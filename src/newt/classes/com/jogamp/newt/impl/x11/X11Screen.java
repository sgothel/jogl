/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
package com.jogamp.newt.impl.x11;

import com.jogamp.nativewindow.impl.x11.X11Util;
import com.jogamp.newt.impl.ScreenImpl;
import com.jogamp.newt.ScreenMode;
import com.jogamp.newt.util.ScreenModeUtil;
import java.util.List;

import javax.media.nativewindow.x11.*;

public class X11Screen extends ScreenImpl {

    static {
        X11Display.initSingleton();
    }

    public X11Screen() {
    }

    protected void createNativeImpl() {
        long handle = GetScreen0(display.getHandle(), screen_idx);
        if (handle == 0) {
            throw new RuntimeException("Error creating screen: " + screen_idx);
        }
        aScreen = new X11GraphicsScreen((X11GraphicsDevice) getDisplay().getGraphicsDevice(), screen_idx);
        setScreenSize(getWidth0(display.getHandle(), screen_idx),
                getHeight0(display.getHandle(), screen_idx));
    }

    protected void closeNativeImpl() {
    }

    private int[] nrotations;
    private int nrotation_index;
    private int nres_number;
    private int nres_index;
    private int[] nrates;
    private int nrate_index;
    private int nmode_number;

    protected int[] getScreenModeFirstImpl() {
        // initialize iterators and static data
        nrotations = getAvailableScreenModeRotations0(display.getHandle(), screen_idx);
        if(null==nrotations || 0==nrotations.length) {
            return null;
        }
        nrotation_index = 0;

        nres_number = getNumScreenModeResolutions0(display.getHandle(), screen_idx);
        if(0==nres_number) {
            return null;
        }
        nres_index = 0;

        nrates = getScreenModeRates0(display.getHandle(), screen_idx, nres_index);
        if(null==nrates || 0==nrates.length) {
            return null;
        }
        nrate_index = 0;

        nmode_number = 0;

        return getScreenModeNextImpl();
    }

    protected int[] getScreenModeNextImpl() {
        // assemble: w x h x bpp x f x r        

        /**
        System.err.println("******** mode: "+nmode_number);
        System.err.println("rot  "+nrotation_index);
        System.err.println("rate "+nrate_index);
        System.err.println("res  "+nres_index); */

        int[] res = getScreenModeResolution0(display.getHandle(), screen_idx, nres_index);
        if(null==res || 0==res.length) {
            return null;
        }
        if(0>=res[0] || 0>=res[1]) {
            throw new InternalError("invalid resolution: "+res[0]+"x"+res[1]+" for res idx "+nres_index+"/"+nres_number);
        }
        int bpp = 32; // FIXME
        int rate = nrates[nrate_index];
        if(0>=rate) {
            throw new InternalError("invalid rate: "+rate+" at index "+nrate_index+"/"+nrates.length);
        }
        int rotation = nrotations[nrotation_index];

        int[] props = new int[ 1 + ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL ];
        int i = 0;
        props[i++] = nres_index; // use resolution index, not unique for native -> ScreenMode
        props[i++] = 0; // set later for verification of iterator
        props[i++] = res[0]; // width
        props[i++] = res[1]; // height
        props[i++] = bpp;    // bpp
        props[i++] = res[2]; // widthmm
        props[i++] = res[3]; // heightmm
        props[i++] = rate;   // rate
        props[i++] = rotation;
        props[i - ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL] = i - 1; // count without extra element

        nmode_number++;

        // iteration: r -> f -> bpp -> [w x h]
        nrotation_index++;
        if(nrotation_index == nrotations.length) {
            nrotation_index=0;
            nrate_index++;
            if(null == nrates || nrate_index == nrates.length){
                nres_index++;
                if(nres_index == nres_number) {
                    // done
                    nrates=null;
                    nrotations=null;
                    return null;
                }

                nrates = getScreenModeRates0(display.getHandle(), screen_idx, nres_index);
                if(null==nrates || 0==nrates.length) {
                    return null;
                }
                nrate_index = 0;
            }
        }

        return props;
    }

    protected ScreenMode getCurrentScreenModeImpl() {
        int resNumber = getNumScreenModeResolutions0(display.getHandle(), screen_idx);
        if(0==resNumber) {
            return null;
        }
        int resIdx = getCurrentScreenResolutionIndex0(display.getHandle(), screen_idx);
        if(0>resIdx) {
            return null;
        }
        if(resIdx>=resNumber) {
            throw new RuntimeException("Invalid resolution index: ! "+resIdx+" < "+resNumber);
        }
        int[] res = getScreenModeResolution0(display.getHandle(), screen_idx, resIdx);
        if(null==res || 0==res.length) {
            return null;
        }
        if(0>=res[0] || 0>=res[1]) {
            throw new InternalError("invalid resolution: "+res[0]+"x"+res[1]+" for res idx "+resIdx+"/"+resNumber);
        }
        int rate = getCurrentScreenRate0(display.getHandle(), screen_idx);
        if(0>rate) {
            return null;
        }
        int rot = getCurrentScreenRotation0(display.getHandle(), screen_idx);
        if(0>rot) {
            return null;
        }

        int[] props = new int[ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL];
        int i = 0;
        props[i++] = 0; // set later for verification of iterator
        props[i++] = res[0]; // width
        props[i++] = res[1]; // height
        props[i++] = 32;     // FIXME: bpp
        props[i++] = res[2]; // widthmm
        props[i++] = res[3]; // heightmm
        props[i++] = rate;   // rate
        props[i++] = rot;
        props[i - ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL] = i; // count
        return ScreenModeUtil.streamIn(props, 0);
    }

    protected boolean setCurrentScreenModeImpl(ScreenMode screenMode) {
        List screenModes = this.getScreenModesOrig();
        int screenModeIdx = screenModes.indexOf(screenMode);
        if(0>screenModeIdx) {
            throw new RuntimeException("ScreenMode not element of ScreenMode list: "+screenMode);
        }
        int resNumber = getNumScreenModeResolutions0(display.getHandle(), screen_idx);
        int resIdx = getScreenModesIdx2NativeIdx().get(screenModeIdx);
        if(0>resIdx || resIdx>=resNumber) {
            throw new RuntimeException("Invalid resolution index: ! 0 < "+resIdx+" < "+resNumber+", screenMode["+screenModeIdx+"] "+screenMode);
        }
        String tname = Thread.currentThread()+"-X11Screen.setCurrentScreenModeImpl()";
        SetScreenModeAction setScreenModeAction = new SetScreenModeAction(screenMode, resIdx);
        Thread randrTask = new Thread(setScreenModeAction, tname);
        randrTask.start();
        int to = SCREEN_MODE_CHANGE_TIMEOUT / 100 ;
        while(to>0 && randrTask.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) { }
            to--;
        }
        if(0==to && DEBUG) {
            System.err.println("X11Screen.setCurrentScreenModeImpl: TO ("+SCREEN_MODE_CHANGE_TIMEOUT+") reached: "+
                               randrTask+", alive "+randrTask.isAlive());
        }
        return setScreenModeAction.getResult();
    }

    class SetScreenModeAction implements Runnable {
        boolean res;
        ScreenMode screenMode;
        int resIdx;

        public SetScreenModeAction(ScreenMode screenMode, int resIdx) {
            this.screenMode = screenMode;
            this.resIdx = resIdx;
            this.res = false;
        }

        public boolean getResult() {
            return res;
        }

        public void run() {
            long dpy = X11Util.createDisplay(display.getName());
            if( 0 == dpy ) {
                throw new RuntimeException("Error creating display: "+display.getName());
            }
            try {
                res = setCurrentScreenMode0(dpy, screen_idx, resIdx,
                                            screenMode.getMonitorMode().getRefreshRate(), screenMode.getRotation());
            } finally {
                X11Util.closeDisplay(dpy);
            }
        }
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    private static native long GetScreen0(long dpy, int scrn_idx);

    private static native int getWidth0(long display, int scrn_idx);

    private static native int getHeight0(long display, int scrn_idx);

    /** @return int[] { rot1, .. } */
    private static native int[] getAvailableScreenModeRotations0(long display, int screen_index);

    private static native int getNumScreenModeResolutions0(long display, int screen_index);

    /** @return int[] { width, height, widthmm, heightmm } */
    private static native int[] getScreenModeResolution0(long display, int screen_index, int mode_index);

    private static native int[] getScreenModeRates0(long display, int screen_index, int mode_index);

    private static native int getCurrentScreenResolutionIndex0(long display, int screen_index);
    private static native int getCurrentScreenRate0(long display, int screen_index);
    private static native int getCurrentScreenRotation0(long display, int screen_index);

    /** needs own Display connection for XRANDR event handling */
    private static native boolean setCurrentScreenMode0(long display, int screen_index, int mode_index, int freq, int rot);
}
