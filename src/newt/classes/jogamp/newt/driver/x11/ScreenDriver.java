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
package jogamp.newt.driver.x11;

import java.util.List;

import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.Point;

import jogamp.nativewindow.x11.X11Util;
import jogamp.newt.DisplayImpl;
import jogamp.newt.DisplayImpl.DisplayRunnable;
import jogamp.newt.ScreenImpl;

import com.jogamp.common.util.VersionNumber;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.nativewindow.x11.X11GraphicsScreen;
import com.jogamp.newt.ScreenMode;

public class ScreenDriver extends ScreenImpl {

    static {
        DisplayDriver.initSingleton();
    }

    public ScreenDriver() {
    }

    protected void createNativeImpl() {
        // validate screen index
        Long handle = runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Long>() {
            public Long run(long dpy) {        
                return new Long(GetScreen0(dpy, screen_idx));
            } } );        
        if (handle.longValue() == 0) {
            throw new RuntimeException("Error creating screen: " + screen_idx);
        }
        final X11GraphicsDevice x11dev = (X11GraphicsDevice) getDisplay().getGraphicsDevice();        
        final long dpy = x11dev.getHandle(); 
        aScreen = new X11GraphicsScreen(x11dev, screen_idx);
        {
            int v[] = getRandRVersion0(dpy);
            randrVersion = new VersionNumber(v[0], v[1], 0);
        }
        System.err.println("RandR "+randrVersion);
        if( !randrVersion.isZero() ) {
            screenRandR = new ScreenRandR11();
        } else {
            screenRandR = null;
        }
    }

    protected void closeNativeImpl() {
    }

    private VersionNumber randrVersion;
    private ScreenRandR screenRandR;
    
    @Override
    protected int[] getScreenModeFirstImpl() {
        if( null == screenRandR ) { return null; }
        
        return runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<int[]>() {
            public int[] run(long dpy) {
                return screenRandR.getScreenModeFirstImpl(dpy, screen_idx);
            } } );
    }

    @Override
    protected int[] getScreenModeNextImpl() {
        if( null == screenRandR ) { return null; }
        
        // assemble: w x h x bpp x f x r        
        return runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<int[]>() {
            public int[] run(long dpy) {
                return screenRandR.getScreenModeNextImpl(dpy, screen_idx);
            } } );
    }

    @Override
    protected ScreenMode getCurrentScreenModeImpl() {
        if( null == screenRandR ) { return null; }
        
        return runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<ScreenMode>() {
            public ScreenMode run(long dpy) {
                return screenRandR.getCurrentScreenModeImpl(dpy, screen_idx);
            } } );
    }

    @Override
    protected boolean setCurrentScreenModeImpl(final ScreenMode screenMode) {
        if( null == screenRandR ) { return false; }
        
        final List<ScreenMode> screenModes = this.getScreenModesOrig();
        final int screenModeIdx = screenModes.indexOf(screenMode);
        if(0>screenModeIdx) {
            throw new RuntimeException("ScreenMode not element of ScreenMode list: "+screenMode);
        }
        final long t0 = System.currentTimeMillis();
        boolean done = runWithTempDisplayHandle( new DisplayImpl.DisplayRunnable<Boolean>() {
            public Boolean run(long dpy) {
                final int resIdx = getScreenModesIdx2NativeIdx().get(screenModeIdx);
                return Boolean.valueOf( screenRandR.setCurrentScreenModeImpl(dpy, screen_idx, screenMode, screenModeIdx, resIdx) );
            }            
        }).booleanValue();
        
        if(DEBUG || !done) {
            System.err.println("X11Screen.setCurrentScreenModeImpl: TO ("+SCREEN_MODE_CHANGE_TIMEOUT+") reached: "+
                               (System.currentTimeMillis()-t0)+"ms; Current: "+getCurrentScreenMode()+"; Desired: "+screenMode);
        }
        return done;
    }

    private DisplayImpl.DisplayRunnable<Boolean> xineramaEnabledQueryWithTemp = new DisplayImpl.DisplayRunnable<Boolean>() {
        public Boolean run(long dpy) {        
            return new Boolean(X11Util.XineramaIsEnabled(dpy)); 
        } };
    
    protected int validateScreenIndex(final int idx) {
        final DisplayDriver x11Display = (DisplayDriver) getDisplay();
        final Boolean r = x11Display.isXineramaEnabled();
        if( null != r ) {
            return r.booleanValue() ? 0 : idx;
        } else {
            return runWithTempDisplayHandle( xineramaEnabledQueryWithTemp ).booleanValue() ? 0 : idx;
        }
    }
        
    protected void getVirtualScreenOriginAndSize(final Point virtualOrigin, final Dimension virtualSize) {
        runWithLockedDisplayDevice( new DisplayImpl.DisplayRunnable<Object>() {
            public Object run(long dpy) {
                virtualOrigin.setX(0);
                virtualOrigin.setY(0);
                virtualSize.setWidth(getWidth0(dpy, screen_idx));
                virtualSize.setHeight(getHeight0(dpy, screen_idx));
                return null;
            } } );        
    }    
    
    //----------------------------------------------------------------------
    // Internals only
    //    
    private final <T> T runWithLockedDisplayDevice(DisplayRunnable<T> action) {
        return display.runWithLockedDisplayDevice(action);
    }
    
    private final <T> T runWithTempDisplayHandle(DisplayRunnable<T> action) {
        final long displayHandle = X11Util.openDisplay(display.getName());        
        if(0 == displayHandle) {
            throw new RuntimeException("null device");
        }
        T res;
        try {
            res = action.run(displayHandle);
        } finally {
            X11Util.closeDisplay(displayHandle);
        }
        return res;
    }
    
    private static native long GetScreen0(long dpy, int scrn_idx);

    private static native int getWidth0(long display, int scrn_idx);

    private static native int getHeight0(long display, int scrn_idx);
    
    private static native int[] getRandRVersion0(long display);
}
