/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package jogamp.nativewindow.jawt;

import java.awt.EventQueue;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Map;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.ToolkitLock;
import com.jogamp.nativewindow.awt.AWTGraphicsScreen;

import jogamp.common.os.PlatformPropsImpl;
import jogamp.nativewindow.Debug;
import jogamp.nativewindow.NWJNILibLoader;
import jogamp.nativewindow.jawt.x11.X11SunJDKReflection;
import jogamp.nativewindow.macosx.OSXUtil;
import jogamp.nativewindow.x11.X11Lib;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.SecurityUtil;
import com.jogamp.common.util.UnsafeUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

public class JAWTUtil {
  public static final boolean DEBUG = Debug.debug("JAWT");

  private static final boolean SKIP_AWT_HIDPI;

  /** OSX JAWT version option to use CALayer */
  public static final int JAWT_MACOSX_USE_CALAYER = 0x80000000;

  /** OSX JAWT CALayer required with Java >= 1.7.0 (implies OS X >= 10.7  */
  private static final int MacOS_JVM_1_7_COMPARE;
  /** OSX JAWT CALayer availability on Mac OS X >= 10.6 Update 4 (recommended) */
  private static final int MacOS_10_6_4_COMPARE;

  // See whether we're running in headless mode
  private static final boolean headlessMode;
  private static final JAWT jawtLockObject;

  // Java2D magic ..
  private static final Method isQueueFlusherThread;
  private static final boolean j2dExist;

  private static final Method  stkAWTLockMID;
  private static final Method  stkAWTUnlockMID;
  private static final boolean hasSTKAWTLock;
  private static final Method stkDisableBackgroundEraseMID;

  private static final RecursiveLock jawtLock;
  private static final ToolkitLock jawtToolkitLock;

  private static final Method gdGetScaleFactorMID;
  private static final Method gdGetCGDisplayIDMIDOnOSX;

  private static class SunToolkitData {
    SunToolkitData() { }
    // default initialization to null, false
    Method awtLockMID;
    Method awtUnlockMID;
    Method disableBackgroundEraseMID;
    boolean ok;
  }
  private static class GraphicsDeviceData {
    GraphicsDeviceData() { }
    // default initialization to null, false
    Method getScaleFactorMID;
    Method getCGDisplayIDMIDOnOSX;
    boolean ok;
  }

  /**
   * Returns true if this platform's JAWT implementation supports offscreen layer.
   */
  public static boolean isOffscreenLayerSupported() {
    return PlatformPropsImpl.OS_TYPE == Platform.OSType.MACOS && MacOS_10_6_4_COMPARE >= 0;
  }

  /**
   * Returns true if this platform's JAWT implementation requires using offscreen layer.
   */
  public static boolean isOffscreenLayerRequired() {
    return PlatformPropsImpl.OS_TYPE == Platform.OSType.MACOS && MacOS_JVM_1_7_COMPARE >= 0;
  }

  /**
   * CALayer size needs to be set using the AWT component size.
   * <p>
   * AWT's super-calayer, i.e. the AWT's own component CALayer,
   * does not layout our root-calayer in respect to this component's
   * position and size, at least when resizing programmatically.
   * </p>
   * <p>
   * As of today, this flag is enabled for all known AWT versions.
   * </p>
   * <p>
   * Sync w/ NativeWindowProtocols.h
   * </p>
   */
  public static final int JAWT_OSX_CALAYER_QUIRK_SIZE     = 1 << 0;

  /**
   * CALayer position needs to be set to zero.
   * <p>
   * AWT's super-calayer, i.e. the AWT's own component CALayer,
   * has a broken layout and needs it's sub-layers to be located at position 0/0.
   * </p>
   * <p>
   * See <code>http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=7172187</code>.
   * </p>
   * <p>
   * Further more a re-layout seems to be required in this case,
   * i.e. a programmatic forced resize +1 and it's inverted resize -1.
   * </p>
   * <p>
   * This flag is enabled w/ AWT < 1.7.0_40.
   * </p>
   * <p>
   * Sync w/ NativeWindowProtocols.h
   * </p>
   */
  public static final int JAWT_OSX_CALAYER_QUIRK_POSITION = 1 << 1;

  /**
   * CALayer position needs to be derived from AWT position
   * in relation to super CALayer.
   * <p>
   * AWT's super-calayer, i.e. the AWT top-container's CALayer,
   * does not layout our root-calayer in respect to this component's
   * position and size, at least when resizing programmatically.
   * </p>
   * <p>
   * CALayer position has origin 0/0 at bottom/left,
   * where AWT component has origin 0/0 at top/left.
   * </p>
   * <p>
   * The super-calayer bounds exclude the frame's heavyweight border/insets.
   * </p>
   * <p>
   * The super-calayer lies within the AWT top-container client space (content).
   * </p>
   * <p>
   * Component's location in super-calayer:
   * <pre>
      p0 = c.locationOnScreen();
      p0 -= c.getOutterComp.getPos();
      p0 -= c.getOutterComp.getInsets();
   * </pre>
   * Where 'locationOnScreen()' is:
   * <pre>
     p0 = 0/0;
     while( null != c ) {
       p0 += c.getPos();
     }
   * </pre>
   * </p>
   * <p>
   * This flags also sets {@link #JAWT_OSX_CALAYER_QUIRK_SIZE},
   * i.e. they are related.
   * </p>
   * <p>
   * As of today, this flag is enabled for w/ AWT >= 1.7.0_40.
   * </p>
   * <p>
   * Sync w/ NativeWindowProtocols.h
   * </p>
   */
  public static final int JAWT_OSX_CALAYER_QUIRK_LAYOUT = 1 << 2;

  /**
   * Returns bitfield of required JAWT OSX CALayer quirks to mediate AWT impl. bugs.
   * <p>
   * Returns zero, if platform is not {@link Platform.OSType#MACOS}
   * or not supporting CALayer, i.e. OSX < 10.6.4.
   * </p>
   * <p>
   * Otherwise includes
   * <ul>
   *    <li>{@link #JAWT_OSX_CALAYER_QUIRK_SIZE} (always)</li>
   *    <li>{@link #JAWT_OSX_CALAYER_QUIRK_POSITION} if JVM < 1.7.0_40</li>
   *    <li>{@link #JAWT_OSX_CALAYER_QUIRK_LAYOUT} if JVM >= 1.7.0_40</li>
   * </ul>
   * </p>
   */
  public static int getOSXCALayerQuirks() {
    int res = 0;
    if( PlatformPropsImpl.OS_TYPE == Platform.OSType.MACOS && MacOS_10_6_4_COMPARE >= 0 ) {
        /** Knowing impl. all expose the SIZE bug */
        res |= JAWT_OSX_CALAYER_QUIRK_SIZE;

        if( MacOS_JVM_1_7_COMPARE < 0 || MacOS_JVM_1_7_COMPARE == 0 && PlatformPropsImpl.JAVA_VERSION_UPDATE < 40 ) {
            res |= JAWT_OSX_CALAYER_QUIRK_POSITION;
        } else {
            res |= JAWT_OSX_CALAYER_QUIRK_LAYOUT;
        }
    }
    return res;
  }

  /**
   * @param useOffscreenLayerIfAvailable
   * @return
   */
  public static JAWT getJAWT(final boolean useOffscreenLayerIfAvailable) {
    final int jawt_version_flags = JAWTFactory.JAWT_VERSION_1_4;
    final JAWT jawt = JAWT.create();

    // default queries
    boolean tryOffscreenLayer;
    boolean tryOnscreen;
    int jawt_version_flags_offscreen = jawt_version_flags;

    if(isOffscreenLayerRequired()) {
        if(PlatformPropsImpl.OS_TYPE == Platform.OSType.MACOS) {
            if(MacOS_10_6_4_COMPARE >= 0) {
                jawt_version_flags_offscreen |= JAWTUtil.JAWT_MACOSX_USE_CALAYER;
                tryOffscreenLayer = true;
                tryOnscreen = false;
            } else {
                throw new RuntimeException("OSX: Invalid version of Java ("+PlatformPropsImpl.JAVA_VERSION_NUMBER+") / OS X ("+PlatformPropsImpl.OS_VERSION_NUMBER+")");
            }
        } else {
            throw new InternalError("offscreen required, but n/a for: "+PlatformPropsImpl.OS_TYPE);
        }
    } else if(useOffscreenLayerIfAvailable && isOffscreenLayerSupported()) {
        if(PlatformPropsImpl.OS_TYPE == Platform.OSType.MACOS) {
            jawt_version_flags_offscreen |= JAWTUtil.JAWT_MACOSX_USE_CALAYER;
            tryOffscreenLayer = true;
            tryOnscreen = true;
        } else {
            throw new InternalError("offscreen requested and supported, but n/a for: "+PlatformPropsImpl.OS_TYPE);
        }
    } else {
        tryOffscreenLayer = false;
        tryOnscreen = true;
    }
    if(DEBUG) {
        System.err.println("JAWTUtil.getJAWT(tryOffscreenLayer "+tryOffscreenLayer+", tryOnscreen "+tryOnscreen+")");
    }

    final StringBuilder errsb = new StringBuilder();
    if(tryOffscreenLayer) {
        errsb.append("Offscreen 0x").append(Integer.toHexString(jawt_version_flags_offscreen));
        if( JAWT.getJAWT(jawt, jawt_version_flags_offscreen) ) {
            return jawt;
        }
    }
    if(tryOnscreen) {
        if(tryOffscreenLayer) {
            errsb.append(", ");
        }
        errsb.append("Onscreen 0x").append(Integer.toHexString(jawt_version_flags));
        if( JAWT.getJAWT(jawt, jawt_version_flags) ) {
            return jawt;
        }
    }
    throw new RuntimeException("Unable to initialize JAWT, trials: "+errsb.toString());
  }

  public static boolean isJAWTUsingOffscreenLayer(final JAWT jawt) {
      return 0 != ( jawt.getCachedVersion() & JAWTUtil.JAWT_MACOSX_USE_CALAYER );
  }

  static {
    SKIP_AWT_HIDPI = PropertyAccess.isPropertyDefined("nativewindow.awt.nohidpi", true);

    if(DEBUG) {
        System.err.println("JAWTUtil initialization (JAWT/JNI/...); SKIP_AWT_HIDPI "+SKIP_AWT_HIDPI);
        // Thread.dumpStack();
    }

    if( PlatformPropsImpl.OS_TYPE == Platform.OSType.MACOS ) {
        MacOS_JVM_1_7_COMPARE = PlatformPropsImpl.JAVA_VERSION_NUMBER.compareTo(new VersionNumber(1, 7, 0));
        MacOS_10_6_4_COMPARE = PlatformPropsImpl.OS_VERSION_NUMBER.compareTo(new VersionNumber(10,6,4));
    } else {
        MacOS_JVM_1_7_COMPARE = -1;
        MacOS_10_6_4_COMPARE = -1;
    }

    headlessMode = GraphicsEnvironment.isHeadless();

    if( headlessMode ) {
        // Headless case
        jawtLockObject = null;
        isQueueFlusherThread = null;
        j2dExist = false;
        stkAWTLockMID = null;
        stkAWTUnlockMID = null;
        hasSTKAWTLock = false;
        stkDisableBackgroundEraseMID = null;
        gdGetScaleFactorMID = null;
        gdGetCGDisplayIDMIDOnOSX = null;
    } else {
        // Non-headless case
        JAWTJNILibLoader.initSingleton(); // load libjawt.so
        if(!NWJNILibLoader.loadNativeWindow("awt")) { // load libnativewindow_awt.so
            throw new NativeWindowException("NativeWindow AWT native library load error.");
        }
        jawtLockObject = getJAWT(false); // don't care for offscreen layer here

        boolean j2dExistTmp = false;
        Class<?> java2DClass = null;
        Method isQueueFlusherThreadTmp = null;
        try {
            java2DClass = Class.forName("jogamp.opengl.awt.Java2D");
            isQueueFlusherThreadTmp = java2DClass.getMethod("isQueueFlusherThread", (Class[])null);
            j2dExistTmp = true;
        } catch (final Exception e) {
        }
        isQueueFlusherThread = isQueueFlusherThreadTmp;
        j2dExist = j2dExistTmp;

        // Always enforce using sun.awt.SunToolkit's awtLock even on JVM >= Java_9,
        // as we have no other official means to synchronize native UI locks especially for X11
        {
            final SunToolkitData std = SecurityUtil.doPrivileged(new PrivilegedAction<SunToolkitData>() {
                @Override
                public SunToolkitData run() {
                    return UnsafeUtil.doWithoutIllegalAccessLogger(new PrivilegedAction<SunToolkitData>() {
                        @Override
                        public SunToolkitData run() {
                            final SunToolkitData d = new SunToolkitData();
                            try {
                                final Class<?> sunToolkitClass = Class.forName("sun.awt.SunToolkit");
                                d.awtLockMID = sunToolkitClass.getDeclaredMethod("awtLock");
                                d.awtLockMID.setAccessible(true);
                                d.awtUnlockMID = sunToolkitClass.getDeclaredMethod("awtUnlock");
                                d.awtUnlockMID.setAccessible(true);
                                d.disableBackgroundEraseMID = sunToolkitClass.getDeclaredMethod("disableBackgroundErase", java.awt.Component.class);
                                d.disableBackgroundEraseMID.setAccessible(true);
                                d.ok=true;
                            } catch (final Exception e) {
                                // Either not a Sun JDK or the interfaces have changed since [Java 1.4.2 / 1.5 -> Java 11]
                                if(DEBUG) {
                                    System.err.println("JAWTUtil stk.0: "+e.getMessage());
                                }
                            }
                            return d;
                        }}); }});
            stkAWTLockMID = std.awtLockMID;
            stkAWTUnlockMID = std.awtUnlockMID;
            stkDisableBackgroundEraseMID = std.disableBackgroundEraseMID;
            boolean _hasSunToolkitAWTLock = false;
            if ( std.ok ) {
                try {
                    stkAWTLockMID.invoke(null, (Object[])null);
                    stkAWTUnlockMID.invoke(null, (Object[])null);
                    _hasSunToolkitAWTLock = true;
                } catch (final Exception e) {
                    if(DEBUG) {
                        System.err.println("JAWTUtil stk.awtLock.1: "+e.getMessage());
                    }
                }
            }
            hasSTKAWTLock = _hasSunToolkitAWTLock;
        }
        if( PlatformPropsImpl.JAVA_9 ) {
            gdGetScaleFactorMID = null;
            gdGetCGDisplayIDMIDOnOSX = null;
        } else {
            final GraphicsDeviceData gdd = (GraphicsDeviceData) SecurityUtil.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    final GraphicsDeviceData d = new GraphicsDeviceData();
                    try {
                        final GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                        final Class<?> gdClass = gd.getClass();
                        d.getScaleFactorMID = gdClass.getDeclaredMethod("getScaleFactor");
                        d.getScaleFactorMID.setAccessible(true);
                        if( Platform.OSType.MACOS == PlatformPropsImpl.OS_TYPE ) {
                            d.getCGDisplayIDMIDOnOSX = gdClass.getDeclaredMethod("getCGDisplayID");
                            d.getCGDisplayIDMIDOnOSX.setAccessible(true);
                        }
                    } catch (final Throwable t) {
                        if(DEBUG) {
                            System.err.println("JAWTUtil scaleFactor: "+t.getMessage());
                        }
                    }
                    return d;
                }
            });
            gdGetScaleFactorMID = gdd.getScaleFactorMID;
            gdGetCGDisplayIDMIDOnOSX = gdd.getCGDisplayIDMIDOnOSX;
        }
    }

    jawtLock = LockFactory.createRecursiveLock();

    jawtToolkitLock = new ToolkitLock() {
          @Override
          public final void lock() {
              JAWTUtil.lockToolkit();
          }
          @Override
          public final void unlock() {
              JAWTUtil.unlockToolkit();
          }
          @Override
          public final void validateLocked() throws RuntimeException {
              JAWTUtil.validateLocked();
          }
          @Override
          public final void dispose() {
              // nop
          }
          @Override
          public String toString() {
              return "JAWTToolkitLock[obj 0x"+Integer.toHexString(hashCode())+", isOwner "+jawtLock.isOwner(Thread.currentThread())+", "+jawtLock+"]";
          }
      };

    // trigger native AWT toolkit / properties initialization
    Map<?,?> desktophints = null;
    try {
        if(EventQueue.isDispatchThread()) {
            desktophints = (Map<?,?>)(Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints"));
        } else {
            final ArrayList<Map<?,?>> desktophintsBucket = new ArrayList<Map<?,?>>(1);
            EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    final Map<?,?> _desktophints = (Map<?,?>)(Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints"));
                    if(null!=_desktophints) {
                        desktophintsBucket.add(_desktophints);
                    }
                }
            });
            desktophints = ( desktophintsBucket.size() > 0 ) ? desktophintsBucket.get(0) : null ;
        }
    } catch (final InterruptedException ex) {
        ex.printStackTrace();
    } catch (final InvocationTargetException ex) {
        ex.printStackTrace();
    }

    if (DEBUG) {
        System.err.println("JAWTUtil: Has sun.awt.SunToolkit: awtLock/awtUnlock " + hasSTKAWTLock + ", disableBackgroundErase "+(null!=stkDisableBackgroundEraseMID));
        System.err.println("JAWTUtil: Has Java2D " + j2dExist);
        System.err.println("JAWTUtil: Is headless " + headlessMode);
        final int hints = ( null != desktophints ) ? desktophints.size() : 0 ;
        System.err.println("JAWTUtil: AWT Desktop hints " + hints);
        System.err.println("JAWTUtil: OffscreenLayer Supported: "+isOffscreenLayerSupported()+" - Required "+isOffscreenLayerRequired());
    }
  }

  /**
   * Called by {@link NativeWindowFactory#initSingleton()}
   */
  public static void initSingleton() {
      // just exist to ensure static init has been run
  }

  /**
   * Called by {@link NativeWindowFactory#shutdown()}
   */
  public static void shutdown() {
  }

  public static boolean hasJava2D() {
    return j2dExist;
  }

  public static boolean isJava2DQueueFlusherThread() {
    boolean b = false;
    if(j2dExist) {
        try {
            b = ((Boolean)isQueueFlusherThread.invoke(null, (Object[])null)).booleanValue();
        } catch (final Exception e) {}
    }
    return b;
  }

  public static boolean isHeadlessMode() {
    return headlessMode;
  }

  /**
   * Locks the AWT's global ReentrantLock.
   * <p>
   * JAWT's native Lock() function calls SunToolkit.awtLock(),
   * which just uses AWT's global ReentrantLock.
   * </p>
   * <p>
   * AWT locking is wrapped through a recursive lock object.
   * </p>
   */
  public static void lockToolkit() throws NativeWindowException {
    jawtLock.lock();
    if( 1 == jawtLock.getHoldCount() ) {
        if(!headlessMode && !isJava2DQueueFlusherThread()) {
            if(hasSTKAWTLock) {
                try {
                    stkAWTLockMID.invoke(null, (Object[])null);
                } catch (final Exception e) {
                  throw new NativeWindowException("SunToolkit.awtLock failed", e);
                }
            } else {
                jawtLockObject.Lock();
            }
        }
    }
    if(ToolkitLock.TRACE_LOCK) { System.err.println("JAWTUtil-ToolkitLock.lock(): "+jawtLock); }
  }

  /**
   * Unlocks the AWT's global ReentrantLock.
   * <p>
   * JAWT's native Unlock() function calls SunToolkit.awtUnlock(),
   * which just uses AWT's global ReentrantLock.
   * </p>
   * <p>
   * AWT unlocking is wrapped through a recursive lock object.
   * </p>
   */
  public static void unlockToolkit() {
    jawtLock.validateLocked();
    if(ToolkitLock.TRACE_LOCK) { System.err.println("JAWTUtil-ToolkitLock.unlock(): "+jawtLock); }
    if( 1 == jawtLock.getHoldCount() ) {
        if(!headlessMode && !isJava2DQueueFlusherThread()) {
            if(hasSTKAWTLock) {
                try {
                    stkAWTUnlockMID.invoke(null, (Object[])null);
                } catch (final Exception e) {
                  throw new NativeWindowException("SunToolkit.awtUnlock failed", e);
                }
            } else {
                jawtLockObject.Unlock();
            }
        }
    }
    jawtLock.unlock();
  }

  public static final void validateLocked() throws RuntimeException {
    jawtLock.validateLocked();
  }

  public static ToolkitLock getJAWTToolkitLock() {
    return jawtToolkitLock;
  }

  /**
   * Calls {@code sun.awt.SunToolkit.disableBackgroundErase(Component component)} if available.
   * <p>
   * Disables the AWT's erasing of the given native Component's background since Java SE 6.
   * This feature can also be enabled by setting the system property
   * {@code sun.awt.noerasebackground} = {@code true}.
   * </p>
   * @param component
   * @return {@code true} if available and successful, otherwise {@code false}
   */
  public static boolean disableBackgroundErase(final java.awt.Component component) {
      if( null != stkDisableBackgroundEraseMID ) {
          try {
              stkDisableBackgroundEraseMID.invoke(component.getToolkit(), component);
              return true;
          } catch (final Exception e) {
              if( DEBUG ) {
                  ExceptionUtils.dumpThrowable("JAWTUtil", e);
              }
          }
      }
      return false;
  }

  /**
   * Queries the Monitor's display ID of the given device
   * <p>
   * Currently only supported for OSX on Java<9
   * </p>
   * @param device for which the display id is being queried
   * @return {@code null} if not supported (Java9+ or !OSX), otherwise the monitor displayID
   */
  public static final Integer getMonitorDisplayID(final GraphicsDevice device) {
      if( null != gdGetCGDisplayIDMIDOnOSX ) {
          // OSX specific for Java<9
          try {
              final Object res = gdGetCGDisplayIDMIDOnOSX.invoke(device);
              if (res instanceof Integer) {
                  return (Integer)res;
              }
          } catch (final Throwable t) {}
      }
      return null;
  }

  /**
   * Returns the pixel scale factor of the given {@link GraphicsDevice}, if supported.
   * <p>
   * This method is generally supported on Java9+
   * </p>
   * <p>
   * If the component does not support pixel scaling
   * or SKIP_AWT_HIDPI is set, the default value <code>one</code> is returned.
   * </p>
   *
   * @param device the {@link GraphicsDevice} instance used to query the pixel scale
   * @param minScale current and output min scale values
   * @param maxScale current and output max scale values
   * @return {@code true} if the given min and max scale values have changed, otherwise {@code false}.
   */
  public static final boolean getPixelScale(final GraphicsDevice device, final float[] minScale, final float[] maxScale) {
      // Shall we allow ]0..1[ minimum scale?
      boolean changed = minScale[0] != 1f || minScale[1] != 1f;
      minScale[0] = 1f;
      minScale[1] = 1f;
      float sx = 1f;
      float sy = 1f;
      boolean gotSXZ = false;
      if( !SKIP_AWT_HIDPI ) {
          if( null != gdGetCGDisplayIDMIDOnOSX ) {
              // OSX specific for Java<9, preserving double type
              try {
                  final Object res = gdGetCGDisplayIDMIDOnOSX.invoke(device);
                  if (res instanceof Integer) {
                      final int displayID = ((Integer)res).intValue();
                      sx = OSXUtil.GetScreenPixelScaleByDisplayID(displayID);
                      sy = sx;
                      gotSXZ = true;
                  }
              } catch (final Throwable t) {}
          }
          if( !gotSXZ && null != gdGetScaleFactorMID ) {
              // Generic for Java<9
              try {
                  final Object res = gdGetScaleFactorMID.invoke(device);
                  if (res instanceof Integer) {
                      sx = ((Integer)res).floatValue();
                  } else if ( res instanceof Double) {
                      sx = ((Double)res).floatValue();
                  }
                  sy = sx;
                  gotSXZ = true;
              } catch (final Throwable t) {}
          }
          if( !gotSXZ ) {
              final GraphicsConfiguration gc = device.getDefaultConfiguration();
              final AffineTransform tx = gc.getDefaultTransform();
              sx = (float)tx.getScaleX();
              sy = (float)tx.getScaleY();
          }
      }
      changed = maxScale[0] != sx || maxScale[1] != sy;
      maxScale[0] = sx;
      maxScale[1] = sy;
      return changed;
  }

  /**
   * Returns the pixel scale factor of the given {@link GraphicsConfiguration}'s {@link GraphicsDevice}, if supported.
   * <p>
   * This method is generally supported on Java9+
   * </p>
   * <p>
   * If the {@link GraphicsDevice} is <code>null</code>, <code>one</code> is returned.
   * </p>
   * <p>
   * If the component does not support pixel scaling
   * or SKIP_AWT_HIDPI is set, the default value <code>one</code> is returned.
   * </p>
   * @param gc the {@link GraphicsConfiguration} instance used to query the pixel scale
   * @param minScale current and output min scale values
   * @param maxScale current and output max scale values
   * @return {@code true} if the given min and max scale values have changed, otherwise {@code false}.
   */
  public static final boolean getPixelScale(final GraphicsConfiguration gc, final float[] minScale, final float[] maxScale) {
      final GraphicsDevice device = null != gc ? gc.getDevice() : null;
      boolean changed;
      if( null == device ) {
          changed = minScale[0] != 1f || minScale[1] != 1f || maxScale[0] != 1f || maxScale[1] != 1f;
          minScale[0] = 1f;
          minScale[1] = 1f;
          maxScale[0] = 1f;
          maxScale[1] = 1f;
      } else {
          changed = JAWTUtil.getPixelScale(device, minScale, maxScale);
      }
      return changed;
  }

  private static String getThreadName() {
      return Thread.currentThread().getName();
  }
  private static String toHexString(final long val) {
      return "0x" + Long.toHexString(val);
  }

  /**
   * @param awtComp must be {@link java.awt.Component#isDisplayable() displayable}
   *        and must have a {@link java.awt.Component#getGraphicsConfiguration() GraphicsConfiguration}
   * @return AbstractGraphicsDevice instance reflecting the {@code awtComp}
   * @throws IllegalArgumentException if {@code awtComp} is not {@link java.awt.Component#isDisplayable() displayable}
   *                                  or has {@code null} {@link java.awt.Component#getGraphicsConfiguration() GraphicsConfiguration}.
   * @see #getAbstractGraphicsScreen(java.awt.Component)
   */
  public static AbstractGraphicsDevice createDevice(final java.awt.Component awtComp) throws IllegalArgumentException {
      if( !awtComp.isDisplayable() ) {
          throw new IllegalArgumentException("Given AWT-Component is not displayable: "+awtComp);
      }
      final GraphicsDevice device;
      final GraphicsConfiguration gc = awtComp.getGraphicsConfiguration();
      if(null!=gc) {
          device = gc.getDevice();
      } else {
          throw new IllegalArgumentException("Given AWT-Component has no GraphicsConfiguration set: "+awtComp);
      }

      final String displayConnection;
      final String nwt = NativeWindowFactory.getNativeWindowType(true);
      if( NativeWindowFactory.TYPE_X11 == nwt ) {
          final long displayHandleAWT = X11SunJDKReflection.graphicsDeviceGetDisplay(device);
          if( 0 == displayHandleAWT ) {
              displayConnection = null; // default
              if(DEBUG) {
                  System.err.println(getThreadName()+" - JAWTUtil.createDevice: Null AWT dpy, default X11 display");
              }
          } else {
              /**
               * Using the AWT display handle works fine with NVidia.
               * However we experienced different results w/ AMD drivers,
               * some work, but some behave erratic.
               * I.e. hangs in XQueryExtension(..) via X11GraphicsScreen.
               */
              displayConnection = X11Lib.XDisplayString(displayHandleAWT);
              if(DEBUG) {
                  System.err.println(getThreadName()+" - JAWTUtil.createDevice: AWT dpy "+displayConnection+" / "+toHexString(displayHandleAWT));
              }
          }
      } else {
          displayConnection = null; // default
      }
      return NativeWindowFactory.createDevice(displayConnection, true /* own */);
  }

  /**
   * @param awtComp must be {@link java.awt.Component#isDisplayable() displayable}
   *        and must have a {@link java.awt.Component#getGraphicsConfiguration() GraphicsConfiguration}
   * @return AbstractGraphicsScreen instance reflecting the {@code awtComp}
   * @throws IllegalArgumentException if {@code awtComp} is not {@link java.awt.Component#isDisplayable() displayable}
   *                                  or has {@code null} {@link java.awt.Component#getGraphicsConfiguration() GraphicsConfiguration}.
   * @see #createDevice(java.awt.Component)
   */
  public static AbstractGraphicsScreen getAbstractGraphicsScreen(final java.awt.Component awtComp) throws IllegalArgumentException {
      final AbstractGraphicsDevice adevice = createDevice(awtComp);
      return NativeWindowFactory.createScreen(adevice, AWTGraphicsScreen.findScreenIndex(awtComp.getGraphicsConfiguration().getDevice()));
  }

}

