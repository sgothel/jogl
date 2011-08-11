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

package jogamp.newt;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.common.util.IntIntHashMap;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.ScreenMode;
import com.jogamp.newt.event.ScreenModeListener;
import com.jogamp.newt.util.ScreenModeUtil;

import javax.media.nativewindow.*;

import java.security.*;
import java.util.ArrayList;
import java.util.List;

public abstract class ScreenImpl extends Screen implements ScreenModeListener {
    protected static final boolean DEBUG_TEST_SCREENMODE_DISABLED = Debug.isPropertyDefined("newt.test.Screen.disableScreenMode", true);

    protected DisplayImpl display;
    protected int screen_idx;
    protected String fqname;
    protected int hashCode;
    protected AbstractGraphicsScreen aScreen;
    protected int refCount; // number of Screen references by Window
    protected int width=-1, height=-1; // detected values: set using setScreenSize
    protected static int usrWidth=-1, usrHeight=-1; // property values: newt.ws.swidth and newt.ws.sheight
    private static AccessControlContext localACC = AccessController.getContext();
    private List/*<ScreenModeListener>*/ referencedScreenModeListener = new ArrayList();
    long t0; // creationTime

    private static Class getScreenClass(String type) 
    throws ClassNotFoundException 
    {
        Class screenClass = NewtFactory.getCustomClass(type, "Screen");
        if(null==screenClass) {
            if (NativeWindowFactory.TYPE_ANDROID.equals(type)) {
                screenClass = Class.forName("jogamp.newt.driver.android.AndroidScreen");
            } else if (NativeWindowFactory.TYPE_EGL.equals(type)) {
                screenClass = Class.forName("jogamp.newt.driver.kd.KDScreen");
            } else if (NativeWindowFactory.TYPE_WINDOWS.equals(type)) {
                screenClass = Class.forName("jogamp.newt.driver.windows.WindowsScreen");
            } else if (NativeWindowFactory.TYPE_MACOSX.equals(type)) {
                screenClass = Class.forName("jogamp.newt.driver.macosx.MacScreen");
            } else if (NativeWindowFactory.TYPE_X11.equals(type)) {
                screenClass = Class.forName("jogamp.newt.driver.x11.X11Screen");
            } else if (NativeWindowFactory.TYPE_AWT.equals(type)) {
                screenClass = Class.forName("jogamp.newt.driver.awt.AWTScreen");
            } else {
                throw new RuntimeException("Unknown window type \"" + type + "\"");
            }
        }
        return screenClass;
    }

    public static Screen create(Display display, final int idx) {
        try {
            if(usrWidth<0 || usrHeight<0) {
                synchronized (Screen.class) {
                    if(usrWidth<0 || usrHeight<0) {
                        usrWidth  = Debug.getIntProperty("newt.ws.swidth", true, localACC);
                        usrHeight = Debug.getIntProperty("newt.ws.sheight", true, localACC);
                        if(usrWidth>0 || usrHeight>0) {
                            System.err.println("User screen size "+usrWidth+"x"+usrHeight);
                        }
                    }
                }
            }
            synchronized(screenList) {
                {
                    Screen screen0 = ScreenImpl.getLastScreenOf(display, idx, -1);
                    if(null != screen0) {
                        if(DEBUG) {
                            System.err.println("Screen.create() REUSE: "+screen0+" "+Display.getThreadName());
                        }
                        return screen0;
                    }
                }
                Class screenClass = getScreenClass(display.getType());
                ScreenImpl screen  = (ScreenImpl) screenClass.newInstance();
                screen.display = (DisplayImpl) display;
                screen.screen_idx = idx;
                screen.fqname = (display.getFQName()+idx).intern();
                screen.hashCode = screen.fqname.hashCode();
                screenList.add(screen);
                if(DEBUG) {
                    System.err.println("Screen.create() NEW: "+screen+" "+Display.getThreadName());
                }
                return screen;
            }            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ScreenImpl other = (ScreenImpl) obj;
        if (this.display != other.display && (this.display == null || !this.display.equals(other.display))) {
            return false;
        }
        if (this.screen_idx != other.screen_idx) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return hashCode;
    }

    public synchronized final void createNative()
            throws NativeWindowException
    {
        if(null == aScreen) {
            if(DEBUG) {
                System.err.println("Screen.createNative() START ("+DisplayImpl.getThreadName()+", "+this+")");
            }
            t0 = System.currentTimeMillis();
            display.addReference();
            createNativeImpl();
            if(null == aScreen) {
                throw new NativeWindowException("Screen.createNative() failed to instanciate an AbstractGraphicsScreen");
            }
            if(DEBUG) {
                System.err.println("Screen.createNative() END ("+DisplayImpl.getThreadName()+", "+this+")");
            }
            synchronized(screenList) {
                screensActive++;
            }
        }
        initScreenModeStatus();
    }

    public synchronized final void destroy() {
        releaseScreenModeStatus();

        synchronized(screenList) {
            screenList.remove(this);
            if(0 < screensActive) {
                screensActive--;
            }
        }

        if ( null != aScreen ) {
            closeNativeImpl();
            aScreen = null;
        }
        refCount = 0;
        display.removeReference();
    }

    public synchronized final int addReference() throws NativeWindowException {
        if(DEBUG) {
            System.err.println("Screen.addReference() ("+DisplayImpl.getThreadName()+"): "+refCount+" -> "+(refCount+1));
        }
        if ( 0 == refCount ) {
            createNative();
        }
        if(null == aScreen) {
            throw new NativeWindowException("Screen.addReference() (refCount "+refCount+") null AbstractGraphicsScreen");
        }
        return ++refCount;
    }

    public synchronized final int removeReference() {
        if(DEBUG) {
            String msg = "Screen.removeReference() ("+DisplayImpl.getThreadName()+"): "+refCount+" -> "+(refCount-1);
            // Throwable t = new Throwable(msg);
            // t.printStackTrace();
            System.err.println(msg);
        }
        refCount--; // could become < 0, in case of manual destruction without actual creation/addReference
        if(0>=refCount) {
            destroy();
            refCount=0; // fix < 0
        }
        return refCount;
    }

    public synchronized final int getReferenceCount() {
        return refCount;
    }

    protected abstract void createNativeImpl();
    protected abstract void closeNativeImpl();

    public final String getFQName() {
        return fqname;
    }

    /**
     * Set the <b>rotated</b> ScreenSize.
     * @see com.jogamp.newt.ScreenMode#getRotatedWidth()
     * @see com.jogamp.newt.ScreenMode#getRotatedHeight()
     */
    protected void setScreenSize(int w, int h) {
        System.err.println("Detected screen size "+w+"x"+h);
        width=w; height=h;
    }

    public final Display getDisplay() {
        return display;
    }

    public final int getIndex() {
        return screen_idx;
    }

    public final AbstractGraphicsScreen getGraphicsScreen() {
        return aScreen;
    }

    public final boolean isNativeValid() {
        return null != aScreen;
    }

    
    /**
     * @return the <b>rotated</b> width.
     * @see com.jogamp.newt.ScreenMode#getRotatedWidth()
     */
    public final int getWidth() {
        return (usrWidth>0) ? usrWidth : (width>0) ? width : 480;
    }

    /**
     * @return the <b>rotated</b> height
     * @see com.jogamp.newt.ScreenMode#getRotatedHeight()
     */
    public final int getHeight() {
        return (usrHeight>0) ? usrHeight : (height>0) ? height : 480;
    }

    @Override
    public String toString() {
        return "NEWT-Screen["+getFQName()+", idx "+screen_idx+", refCount "+refCount+", "+getWidth()+"x"+getHeight()+", "+aScreen+", "+display+"]";
    }

    public final List/*<ScreenMode>*/ getScreenModes() {
        ArrayHashSet screenModes = getScreenModesOrig();
        if(null != screenModes && 0 < screenModes.size()) {
            return screenModes.toArrayList();
        }
        return null;
    }

    public ScreenMode getOriginalScreenMode() {
        ScreenModeStatus sms = ScreenModeStatus.getScreenModeStatus(this.getFQName());
        return ( null != sms ) ? sms.getOriginalScreenMode() : null ;
    }

    public ScreenMode getCurrentScreenMode() {
        ScreenMode smU = null;
        ScreenModeStatus sms = ScreenModeStatus.getScreenModeStatus(this.getFQName());
        if(null != sms) {
            ScreenMode sm0 = ( DEBUG_TEST_SCREENMODE_DISABLED ) ? null : getCurrentScreenModeImpl();
            if(null == sm0) {
                return null;
            }
            sms.lock();
            try {
                smU = (ScreenMode) sms.getScreenModes().getOrAdd(sm0); // unified instance, maybe new

                // if mode has changed somehow, update it ..
                if( sms.getCurrentScreenMode().hashCode() != smU.hashCode() ) {
                    setScreenSize(smU.getRotatedWidth(), smU.getRotatedHeight());
                    sms.fireScreenModeChanged(smU, true);
                }
            } finally {
                sms.unlock();
            }
        }
        return smU;
    }

    public boolean setCurrentScreenMode(ScreenMode screenMode) {
        ScreenMode smU = (ScreenMode) getScreenModesOrig().get(screenMode); // unify via value hash
        ScreenModeStatus sms = ScreenModeStatus.getScreenModeStatus(this.getFQName());
        if(null!=sms) {
            sms.lock();
            try {
                if(DEBUG) {
                    System.err.println("Screen.setCurrentScreenMode ("+(System.currentTimeMillis()-t0)+"): 0.0 "+screenMode);
                }

                sms.fireScreenModeChangeNotify(smU);

                if(DEBUG) {
                    System.err.println("Screen.setCurrentScreenMode ("+(System.currentTimeMillis()-t0)+"): 0.1 "+screenMode);
                }

                boolean success = setCurrentScreenModeImpl(smU);
                if(success) {
                    setScreenSize(screenMode.getRotatedWidth(), screenMode.getRotatedHeight());
                }

                if(DEBUG) {
                    System.err.println("Screen.setCurrentScreenMode ("+(System.currentTimeMillis()-t0)+"): X.0 "+screenMode+", success: "+success);
                }

                sms.fireScreenModeChanged(smU, success);

                if(DEBUG) {
                    System.err.println("Screen.setCurrentScreenMode ("+(System.currentTimeMillis()-t0)+"): X.X "+screenMode+", success: "+success);
                }

                return success;
            } finally {
                sms.unlock();
            }
        }
        return false;
    }

    public void screenModeChangeNotify(ScreenMode sm) {
        for(int i=0; i<referencedScreenModeListener.size(); i++) {
            ((ScreenModeListener)referencedScreenModeListener.get(i)).screenModeChangeNotify(sm);
        }
    }

    public void screenModeChanged(ScreenMode sm, boolean success) {
        for(int i=0; i<referencedScreenModeListener.size(); i++) {
            ((ScreenModeListener)referencedScreenModeListener.get(i)).screenModeChanged(sm, success);
        }
    }

    public synchronized final void addScreenModeListener(ScreenModeListener sml) {
        referencedScreenModeListener.add(sml);
    }

    public synchronized final void removeScreenModeListener(ScreenModeListener sml) {
        referencedScreenModeListener.remove(sml);
    }

    /** ScreenModeStatus bridge to native implementation */
    protected final ArrayHashSet getScreenModesOrig() {
        ScreenModeStatus sms = ScreenModeStatus.getScreenModeStatus(this.getFQName());
        if(null!=sms) {
            return sms.getScreenModes();
        }
        return null;
    }

    /** ScreenModeStatus bridge to native implementation */
    protected final IntIntHashMap getScreenModesIdx2NativeIdx() {
        ScreenModeStatus sms = ScreenModeStatus.getScreenModeStatus(this.getFQName());
        if(null!=sms) {
            return sms.getScreenModesIdx2NativeIdx();
        }
        return null;
    }

    /**
     * To be implemented by the native specification.<br>
     * Is called within a thread safe environment.<br>
     * Is called only to collect the ScreenModes, usually at startup setting up modes.<br>
     * <br>
     * <b>WARNING</b>: must be synchronized with {@link com.jogamp.newt.util.ScreenModeUtil#NUM_SCREEN_MODE_PROPERTIES},
     * ie {@link com.jogamp.newt.util.ScreenModeUtil#streamIn(com.jogamp.common.util.ArrayHashSet, com.jogamp.common.util.ArrayHashSet, com.jogamp.common.util.ArrayHashSet, com.jogamp.common.util.ArrayHashSet, int[], int)}<br>
     * <br>
     * <b>Note</b>: Additional 1st element is native mode id.
     */
    protected int[] getScreenModeFirstImpl() {
        return null;
    }

    /**
     * To be implemented by the native specification.<br>
     * Is called within a thread safe environment.<br>
     * Is called only to collect the ScreenModes, usually at startup setting up modes.<br>
     * <br>
     * <b>WARNING</b>: must be synchronized with {@link com.jogamp.newt.util.ScreenModeUtil#NUM_SCREEN_MODE_PROPERTIES},
     * ie {@link com.jogamp.newt.util.ScreenModeUtil#streamIn(com.jogamp.common.util.ArrayHashSet, com.jogamp.common.util.ArrayHashSet, com.jogamp.common.util.ArrayHashSet, com.jogamp.common.util.ArrayHashSet, int[], int)}<br>
     * <br>
     * <b>Note</b>: Additional 1st element is native mode id.
     */
    protected int[] getScreenModeNextImpl() {
        return null;
    }

    /**
     * To be implemented by the native specification.<br>
     * Is called within a thread safe environment.<br>
     */
    protected ScreenMode getCurrentScreenModeImpl() {
        return null;
    }

    /**
     * To be implemented by the native specification.<br>
     * Is called within a thread safe environment.<br>
     */
    protected boolean setCurrentScreenModeImpl(ScreenMode screenMode) {
        return false;
    }

    private void initScreenModeStatus() {
        ScreenModeStatus sms;
        ScreenModeStatus.lockScreenModeStatus();
        try {
            sms = ScreenModeStatus.getScreenModeStatus(this.getFQName());
            if(null==sms) {                
                IntIntHashMap screenModesIdx2NativeIdx = new IntIntHashMap();

                ArrayHashSet screenModes = collectNativeScreenModes(screenModesIdx2NativeIdx);
                if(screenModes.size()==0) {
                    ScreenMode sm0 = ( DEBUG_TEST_SCREENMODE_DISABLED ) ? null : getCurrentScreenModeImpl();
                    if(null != sm0) {
                        if(DEBUG) {
                            System.err.println("ScreenImpl.initScreenModeStatus: added current (last resort, collect failed): "+sm0);
                        }
                        screenModes.getOrAdd(sm0);
                    } else if(DEBUG) {
                        System.err.println("ScreenImpl.initScreenModeStatus: Warning: No screen modes added!");
                    }
                }
                sms = new ScreenModeStatus(screenModes, screenModesIdx2NativeIdx);
                if(screenModes.size()>0) {
                    ScreenMode originalScreenMode = ( DEBUG_TEST_SCREENMODE_DISABLED ) ? null : getCurrentScreenModeImpl();
                    if(null != originalScreenMode) {
                        ScreenMode originalScreenMode0 = (ScreenMode) screenModes.get(originalScreenMode); // unify via value hash
                        if(null == originalScreenMode0) {
                            throw new RuntimeException(originalScreenMode+" could not be hashed from ScreenMode list");
                        }
                        sms.setOriginalScreenMode(originalScreenMode0);
                    }
                }
                ScreenModeStatus.mapScreenModeStatus(this.getFQName(), sms);
            }
            sms.addListener(this);
        } finally {
            ScreenModeStatus.unlockScreenModeStatus();
        }
    }

    /** ignores bpp < 15 */
    private ArrayHashSet collectNativeScreenModes(IntIntHashMap screenModesIdx2NativeId) {
        ArrayHashSet resolutionPool  = new ArrayHashSet();
        ArrayHashSet surfaceSizePool = new ArrayHashSet();
        ArrayHashSet screenSizeMMPool = new ArrayHashSet();
        ArrayHashSet monitorModePool = new ArrayHashSet();
        ArrayHashSet screenModePool = null;

        screenModePool = new ArrayHashSet();

        int[] smProps = null;
        int num = 0;
        final int idxBpp =   1 // native mode
                           + 1 // count
                           + ScreenModeUtil.NUM_RESOLUTION_PROPERTIES
                           + ScreenModeUtil.NUM_SURFACE_SIZE_PROPERTIES
                           - 1 ; // index 0 based
        do {
            if(DEBUG_TEST_SCREENMODE_DISABLED) {
                smProps = null;
            } else if(0 == num) {
                smProps = getScreenModeFirstImpl();
            } else {
                smProps = getScreenModeNextImpl();
            }
            if(null != smProps && 0 < smProps.length && smProps[idxBpp] >= 15) {
                int nativeId = smProps[0];
                int screenModeIdx = ScreenModeUtil.streamIn(resolutionPool, surfaceSizePool, screenSizeMMPool,
                                                            monitorModePool, screenModePool, smProps, 1);
                if(screenModeIdx >= 0) {
                    screenModesIdx2NativeId.put(screenModeIdx, nativeId);
                }
            }
            num++;
        } while ( null != smProps && 0 < smProps.length );

        if(DEBUG) {
            System.err.println("ScreenImpl.collectNativeScreenModes: ScreenMode number  : "+screenModePool.size());
            System.err.println("ScreenImpl.collectNativeScreenModes: MonitorMode number : "+monitorModePool.size());
            System.err.println("ScreenImpl.collectNativeScreenModes: ScreenSizeMM number: "+screenSizeMMPool.size());
            System.err.println("ScreenImpl.collectNativeScreenModes: SurfaceSize number : "+surfaceSizePool.size());
            System.err.println("ScreenImpl.collectNativeScreenModes: Resolution number  : "+resolutionPool.size());
        }

        return screenModePool;
    }

    private void releaseScreenModeStatus() {
        ScreenModeStatus sms;
        ScreenModeStatus.lockScreenModeStatus();
        try {
            sms = ScreenModeStatus.getScreenModeStatus(this.getFQName());
            if(null != sms) {
                sms.lock();
                try {
                    if(0 == sms.removeListener(this)) {
                        if(!sms.isOriginalMode()) {
                            setCurrentScreenMode(sms.getOriginalScreenMode());
                        }
                        ScreenModeStatus.unmapScreenModeStatus(this.getFQName());
                    }
                } finally {
                    sms.unlock();
                }
            }            
        } finally {
            ScreenModeStatus.unlockScreenModeStatus();
        }
    }
}

