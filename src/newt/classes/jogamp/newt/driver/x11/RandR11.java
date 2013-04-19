/**
 * Copyright 2013 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package jogamp.newt.driver.x11;

import jogamp.newt.ScreenImpl;

import com.jogamp.newt.ScreenMode;
import com.jogamp.newt.util.ScreenModeUtil;

public class RandR11 implements RandR {
    private static final boolean DEBUG = ScreenDriver.DEBUG;
    
    private int[] nrotations;
    private int nrotation_index;
    private int nres_number;
    private int nres_index;
    private int[] nrates;
    private int nrate_index;
    private int nmode_number;
    
    @Override
    public int[] getScreenModeFirstImpl(final long dpy, final int screen_idx) {
        // initialize iterators and static data
        nrotations = getAvailableScreenModeRotations0(dpy, screen_idx);
        if(null==nrotations || 0==nrotations.length) {
            return null;
        }
        nrotation_index = 0;

        nres_number = getNumScreenModeResolutions0(dpy, screen_idx);
        if(0==nres_number) {
            return null;
        }
        nres_index = 0;

        nrates = getScreenModeRates0(dpy, screen_idx, nres_index);
        if(null==nrates || 0==nrates.length) {
            return null;
        }
        nrate_index = 0;

        nmode_number = 0;

        return getScreenModeNextImpl(dpy, screen_idx);
    }

    @Override
    public int[] getScreenModeNextImpl(final long dpy, final int screen_idx) {
        /**
        System.err.println("******** mode: "+nmode_number);
        System.err.println("rot  "+nrotation_index);
        System.err.println("rate "+nrate_index);
        System.err.println("res  "+nres_index); */

        int[] res = getScreenModeResolution0(dpy, screen_idx, nres_index);
        if(null==res || 0==res.length) {
            return null;
        }
        if(0>=res[0] || 0>=res[1]) {
            throw new InternalError("invalid resolution: "+res[0]+"x"+res[1]+" for res idx "+nres_index+"/"+nres_number);
        }
        int rate = nrates[nrate_index];
        if(0>=rate) {
            rate = ScreenImpl.default_sm_rate;
            if(DEBUG) {
                System.err.println("Invalid rate: "+rate+" at index "+nrate_index+"/"+nrates.length+", using default: "+ScreenImpl.default_sm_rate);
            }
        }
        int rotation = nrotations[nrotation_index];

        int[] props = new int[ 1 + ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL ];
        int i = 0;
        props[i++] = nres_index; // use resolution index, not unique for native -> ScreenMode
        props[i++] = 0; // set later for verification of iterator
        props[i++] = res[0]; // width
        props[i++] = res[1]; // height
        props[i++] = ScreenImpl.default_sm_bpp; // FIXME
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

                nrates = getScreenModeRates0(dpy, screen_idx, nres_index);
                if(null==nrates || 0==nrates.length) {
                    return null;
                }
                nrate_index = 0;
            }
        }

        return props;
    }

    @Override
    public ScreenMode getCurrentScreenModeImpl(final long dpy, final int screen_idx) {
        long screenConfigHandle = getScreenConfiguration0(dpy, screen_idx);
        if(0 == screenConfigHandle) {
            return null;
        }
        int[] res;
        int rate, rot;
        try {                
            int resNumber = getNumScreenModeResolutions0(dpy, screen_idx);
            if(0==resNumber) {
                return null;
            }

            int resIdx = getCurrentScreenResolutionIndex0(screenConfigHandle);
            if(0>resIdx) {
                return null;
            }
            if(resIdx>=resNumber) {
                throw new RuntimeException("Invalid resolution index: ! "+resIdx+" < "+resNumber);
            }
            res = getScreenModeResolution0(dpy, screen_idx, resIdx);
            if(null==res || 0==res.length) {
                return null;
            }
            if(0>=res[0] || 0>=res[1]) {
                throw new InternalError("invalid resolution: "+res[0]+"x"+res[1]+" for res idx "+resIdx+"/"+resNumber);
            }
            rate = getCurrentScreenRate0(screenConfigHandle);
            if(0>rate) {
                return null;
            }
            rot = getCurrentScreenRotation0(screenConfigHandle);
            if(0>rot) {
                return null;
            }
        } finally {
             freeScreenConfiguration0(screenConfigHandle);
        }
        int[] props = new int[ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL];
        int i = 0;
        props[i++] = 0; // set later for verification of iterator
        props[i++] = res[0]; // width
        props[i++] = res[1]; // height
        props[i++] = ScreenImpl.default_sm_bpp; // FIXME
        props[i++] = res[2]; // widthmm
        props[i++] = res[3]; // heightmm
        props[i++] = rate;   // rate
        props[i++] = rot;
        props[i - ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL] = i; // count
        return ScreenModeUtil.streamIn(props, 0);
    }

    @Override
    public boolean setCurrentScreenModeImpl(final long dpy, final int screen_idx, final ScreenMode screenMode, final int screenModeIdx, final int resolutionIdx) {
        final long t0 = System.currentTimeMillis();
        boolean done = false;
        long screenConfigHandle = getScreenConfiguration0(dpy, screen_idx);
        if(0 == screenConfigHandle) {
            return Boolean.valueOf(done);
        }
        try {
            int resNumber = getNumScreenModeResolutions0(dpy, screen_idx);
            if(0>resolutionIdx || resolutionIdx>=resNumber) {
                throw new RuntimeException("Invalid resolution index: ! 0 < "+resolutionIdx+" < "+resNumber+", screenMode["+screenModeIdx+"] "+screenMode);
            }
    
            final int f = screenMode.getMonitorMode().getRefreshRate();
            final int r = screenMode.getRotation();

            if( setCurrentScreenModeStart0(dpy, screen_idx, screenConfigHandle, resolutionIdx, f, r) ) {
                while(!done && System.currentTimeMillis()-t0 < ScreenImpl.SCREEN_MODE_CHANGE_TIMEOUT) {
                    done = setCurrentScreenModePollEnd0(dpy, screen_idx, resolutionIdx, f, r);
                    if(!done) {
                        try { Thread.sleep(10); } catch (InterruptedException e) { }
                    }
                }
            }
        } finally {
             freeScreenConfiguration0(screenConfigHandle);
        }
        return done;
    }

    /** @return int[] { rot1, .. } */
    private static native int[] getAvailableScreenModeRotations0(long display, int screen_index);

    private static native int getNumScreenModeResolutions0(long display, int screen_index);

    /** @return int[] { width, height, widthmm, heightmm } */
    private static native int[] getScreenModeResolution0(long display, int screen_index, int mode_index);

    private static native int[] getScreenModeRates0(long display, int screen_index, int mode_index);

    private static native long getScreenConfiguration0(long display, int screen_index);
    private static native void freeScreenConfiguration0(long screenConfiguration);
    
    private static native int getCurrentScreenResolutionIndex0(long screenConfiguration);
    private static native int getCurrentScreenRate0(long screenConfiguration);
    private static native int getCurrentScreenRotation0(long screenConfiguration);

    /** needs own Display connection for XRANDR event handling */
    private static native boolean setCurrentScreenModeStart0(long display, int screen_index, long screenConfiguration, int mode_index, int freq, int rot);
    private static native boolean setCurrentScreenModePollEnd0(long display, int screen_index, int mode_index, int freq, int rot);
    
}
