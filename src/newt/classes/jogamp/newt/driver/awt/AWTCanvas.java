/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package jogamp.newt.driver.awt;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.CapabilitiesChooser;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.VisualIDHolder;

import com.jogamp.nativewindow.awt.AWTGraphicsConfiguration;
import com.jogamp.nativewindow.awt.AWTGraphicsDevice;
import com.jogamp.nativewindow.awt.AWTGraphicsScreen;
import com.jogamp.nativewindow.awt.JAWTWindow;
import com.jogamp.newt.Window;

@SuppressWarnings("serial")
public class AWTCanvas extends Canvas {
  private final WindowDriver driver;
  private final CapabilitiesImmutable capabilities;
  private final CapabilitiesChooser chooser;
  private final UpstreamScalable upstreamScale;
  private GraphicsConfiguration chosen;
  private volatile GraphicsDevice device;
  private volatile AWTGraphicsConfiguration awtConfig;
  private volatile JAWTWindow jawtWindow=null; // the JAWTWindow presentation of this AWT Canvas, bound to the 'drawable' lifecycle

  public static interface UpstreamScalable {
      float[] getReqPixelScale();
      void setHasPixelScale(final float[] pixelScale);
  }

  private boolean displayConfigChanged=false;

  public AWTCanvas(final WindowDriver driver, final CapabilitiesImmutable capabilities, final CapabilitiesChooser chooser, final UpstreamScalable upstreamScale) {
    super();
    if(null==capabilities) {
        throw new NativeWindowException("Capabilities null");
    }
    if(null==driver) {
        throw new NativeWindowException("driver null");
    }
    this.driver = driver;
    this.capabilities=capabilities;
    this.chooser=chooser;
    this.upstreamScale = upstreamScale;
  }

  public AWTGraphicsConfiguration getAWTGraphicsConfiguration() {
    return awtConfig;
  }

  /**
   * Overridden from Canvas to prevent the AWT's clearing of the
   * canvas from interfering with the OpenGL rendering.
   */
  @Override
  public void update(final Graphics g) {
    // paint(g);
  }

  /** Overridden to cause OpenGL rendering to be performed during
      repaint cycles. Subclasses which override this method must call
      super.paint() in their paint() method in order to function
      properly.
   */
  @Override
  public void paint(final Graphics g) {
  }

  public boolean hasDeviceChanged() {
    final boolean res = displayConfigChanged;
    displayConfigChanged=false;
    return res;
  }

  @Override
  public void addNotify() {

    // before native peer is valid: X11
    disableBackgroundErase();

    /**
     * 'super.addNotify()' determines the GraphicsConfiguration,
     * while calling this class's overriden 'getGraphicsConfiguration()' method
     * after which it creates the native peer.
     * Hence we have to set the 'awtConfig' before since it's GraphicsConfiguration
     * is being used in getGraphicsConfiguration().
     * This code order also allows recreation, ie re-adding the GLCanvas.
     */
    awtConfig = chooseGraphicsConfiguration(capabilities, capabilities, chooser, device);
    if(Window.DEBUG_IMPLEMENTATION) {
        System.err.println(getThreadName()+": AWTCanvas.addNotify.0: Created Config: "+awtConfig);
    }
    if(null==awtConfig) {
        throw new NativeWindowException("Error: NULL AWTGraphicsConfiguration");
    }
    chosen = awtConfig.getAWTGraphicsConfiguration();

    setAWTGraphicsConfiguration(awtConfig);

    // issues getGraphicsConfiguration() and creates the native peer
    super.addNotify();

    // after native peer is valid: Windows
    disableBackgroundErase();

    {
        jawtWindow = (JAWTWindow) NativeWindowFactory.getNativeWindow(this, awtConfig);
        // trigger initialization cycle
        jawtWindow.lockSurface();
        try {
            jawtWindow.setSurfaceScale(upstreamScale.getReqPixelScale() );
            upstreamScale.setHasPixelScale(jawtWindow.getCurrentSurfaceScale(new float[2]));
        } finally {
            jawtWindow.unlockSurface();
        }
    }

    final GraphicsConfiguration gc = super.getGraphicsConfiguration();
    if(null!=gc) {
        device = gc.getDevice();
    }
    driver.localCreate();
    if(Window.DEBUG_IMPLEMENTATION) {
        System.err.println(getThreadName()+": AWTCanvas.addNotify.X");
    }
  }

  public NativeWindow getNativeWindow() {
    final JAWTWindow _jawtWindow = jawtWindow;
    return (null != _jawtWindow) ? _jawtWindow : null;
  }

  public boolean isOffscreenLayerSurfaceEnabled() {
      return null != jawtWindow ? jawtWindow.isOffscreenLayerSurfaceEnabled() : false;
  }

  private void setAWTGraphicsConfiguration(final AWTGraphicsConfiguration config) {
    // Cache awtConfig
    awtConfig = config;
    if( null != jawtWindow ) {
        // Notify JAWTWindow ..
        jawtWindow.setAWTGraphicsConfiguration(config);
    }
  }

  @Override
  public void removeNotify() {
      if(Window.DEBUG_IMPLEMENTATION) {
          System.err.println(getThreadName()+": AWTCanvas.removeNotify.0: Created Config: "+awtConfig);
      }
      try {
        driver.localDestroy();
      } finally {
        super.removeNotify();
      }
  }

  void dispose() {
    if( null != jawtWindow ) {
        jawtWindow.destroy();
        if(Window.DEBUG_IMPLEMENTATION) {
            System.err.println(getThreadName()+": AWTCanvas.disposeJAWTWindowAndAWTDeviceOnEDT(): post JAWTWindow: "+jawtWindow);
        }
        jawtWindow=null;
    }
    if(null != awtConfig) {
        final AbstractGraphicsDevice adevice = awtConfig.getNativeGraphicsConfiguration().getScreen().getDevice();
        String adeviceMsg=null;
        if(Window.DEBUG_IMPLEMENTATION) {
            adeviceMsg = adevice.toString();
        }
        final boolean closed = adevice.close();
        if(Window.DEBUG_IMPLEMENTATION) {
            System.err.println(getThreadName()+": AWTCanvas.dispose(): closed GraphicsDevice: "+adeviceMsg+", result: "+closed);
        }
    }
    awtConfig = null;
  }

  private String getThreadName() { return Thread.currentThread().getName(); }

  /**
   * Overridden to choose a GraphicsConfiguration on a parent container's
   * GraphicsDevice because both devices
   */
  @Override
  public GraphicsConfiguration getGraphicsConfiguration() {
    /*
     * Workaround for problems with Xinerama and java.awt.Component.checkGD
     * when adding to a container on a different graphics device than the
     * one that this Canvas is associated with.
     *
     * GC will be null unless:
     *   - A native peer has assigned it. This means we have a native
     *     peer, and are already comitted to a graphics configuration.
     *   - This canvas has been added to a component hierarchy and has
     *     an ancestor with a non-null GC, but the native peer has not
     *     yet been created. This means we can still choose the GC on
     *     all platforms since the peer hasn't been created.
     */
    final GraphicsConfiguration gc = super.getGraphicsConfiguration();
    /*
     * chosen is only non-null on platforms where the GLDrawableFactory
     * returns a non-null GraphicsConfiguration (in the GLCanvas
     * constructor).
     *
     * if gc is from this Canvas' native peer then it should equal chosen,
     * otherwise it is from an ancestor component that this Canvas is being
     * added to, and we go into this block.
     */
    if (gc != null && chosen != null && !chosen.equals(gc)) {
      /*
       * Check for compatibility with gc. If they differ by only the
       * device then return a new GCconfig with the super-class' GDevice
       * (and presumably the same visual ID in Xinerama).
       *
       */
      if (!chosen.getDevice().getIDstring().equals(gc.getDevice().getIDstring())) {
        /*
         * Here we select a GraphicsConfiguration on the alternate
         * device that is presumably identical to the chosen
         * configuration, but on the other device.
         *
         * Should really check to ensure that we select a configuration
         * with the same X visual ID for Xinerama screens, otherwise the
         * GLDrawable may have the wrong visual ID (I don't think this
         * ever gets updated). May need to add a method to
         * X11GLDrawableFactory to do this in a platform specific
         * manner.
         *
         * However, on platforms where we can actually get into this
         * block, both devices should have the same visual list, and the
         * same configuration should be selected here.
         */
        final AWTGraphicsConfiguration newConfig = chooseGraphicsConfiguration(
                awtConfig.getChosenCapabilities(), awtConfig.getRequestedCapabilities(), chooser, gc.getDevice());
        final GraphicsConfiguration compatible = (null!=newConfig)?newConfig.getAWTGraphicsConfiguration():null;
        if(Window.DEBUG_IMPLEMENTATION) {
            final Exception e = new Exception("Info: Call Stack: "+Thread.currentThread().getName());
            e.printStackTrace();
            System.err.println("Created Config (n): HAVE    GC "+chosen);
            System.err.println("Created Config (n): THIS    GC "+gc);
            System.err.println("Created Config (n): Choosen GC "+compatible);
            System.err.println("Created Config (n): HAVE    CF "+awtConfig);
            System.err.println("Created Config (n): Choosen CF "+newConfig);
            System.err.println("Created Config (n): EQUALS CAPS "+newConfig.getChosenCapabilities().equals(awtConfig.getChosenCapabilities()));
        }

        if (compatible != null) {
          /*
           * Save the new GC for equals test above, and to return to
           * any outside callers of this method.
           */
          chosen = compatible;
          if( !newConfig.getChosenCapabilities().equals(awtConfig.getChosenCapabilities())) {
              displayConfigChanged=true;
          }
          setAWTGraphicsConfiguration(newConfig);
        }
      }

      /*
       * If a compatible GC was not found in the block above, this will
       * return the GC that was selected in the constructor (and might
       * cause an exception in Component.checkGD when adding to a
       * container, but in this case that would be the desired behavior).
       *
       */
      return chosen;
    } else if (gc == null) {
      /*
       * The GC is null, which means we have no native peer, and are not
       * part of a (realized) component hierarchy. So we return the
       * desired visual that was selected in the constructor (possibly
       * null).
       */
      return chosen;
    }

    /*
     * Otherwise we have not explicitly selected a GC in the constructor, so
     * just return what Canvas would have.
     */
    return gc;
  }

  private static AWTGraphicsConfiguration chooseGraphicsConfiguration(final CapabilitiesImmutable capsChosen,
                                                                      final CapabilitiesImmutable capsRequested,
                                                                      final CapabilitiesChooser chooser,
                                                                      final GraphicsDevice device) {
    final AbstractGraphicsScreen aScreen = null != device ?
            AWTGraphicsScreen.createScreenDevice(device, AbstractGraphicsDevice.DEFAULT_UNIT):
            AWTGraphicsScreen.createDefault();
    final AWTGraphicsConfiguration config = (AWTGraphicsConfiguration)
      GraphicsConfigurationFactory.getFactory(AWTGraphicsDevice.class, capsChosen.getClass()).chooseGraphicsConfiguration(capsChosen,
                                                                                                   capsRequested,
                                                                                                   chooser, aScreen, VisualIDHolder.VID_UNDEFINED);
    if (config == null) {
      throw new NativeWindowException("Error: Couldn't fetch AWTGraphicsConfiguration");
    }

    return config;
  }

  // Disables the AWT's erasing of this Canvas's background on Windows
  // in Java SE 6. This internal API is not available in previous
  // releases, but the system property
  // -Dsun.awt.noerasebackground=true can be specified to get similar
  // results globally in previous releases.
  private static boolean disableBackgroundEraseInitialized;
  private static Method  disableBackgroundEraseMethod;
  private void disableBackgroundErase() {
    if (!disableBackgroundEraseInitialized) {
      try {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
              try {
                Class<?> clazz = getToolkit().getClass();
                while (clazz != null && disableBackgroundEraseMethod == null) {
                  try {
                    disableBackgroundEraseMethod =
                      clazz.getDeclaredMethod("disableBackgroundErase",
                                              new Class[] { Canvas.class });
                    disableBackgroundEraseMethod.setAccessible(true);
                  } catch (final Exception e) {
                    clazz = clazz.getSuperclass();
                  }
                }
              } catch (final Exception e) {
              }
              return null;
            }
          });
      } catch (final Exception e) {
      }
      disableBackgroundEraseInitialized = true;
      if(Window.DEBUG_IMPLEMENTATION) {
        System.err.println("AWTCanvas: TK disableBackgroundErase method found: "+
                (null!=disableBackgroundEraseMethod));
      }
    }
    if (disableBackgroundEraseMethod != null) {
      Throwable t=null;
      try {
        disableBackgroundEraseMethod.invoke(getToolkit(), new Object[] { this });
      } catch (final Exception e) {
        // FIXME: workaround for 6504460 (incorrect backport of 6333613 in 5.0u10)
        // throw new GLException(e);
        t = e;
      }
      if(Window.DEBUG_IMPLEMENTATION) {
        System.err.println("AWTCanvas: TK disableBackgroundErase error: "+t);
      }
    }
  }
}
