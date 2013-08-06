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

import javax.media.nativewindow.util.RectangleImmutable;

import jogamp.newt.MonitorModeProps;
import jogamp.newt.ScreenImpl;

import com.jogamp.common.util.VersionNumber;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;

class RandR11 implements RandR {
    private static final boolean DEBUG = ScreenDriver.DEBUG;
    
    RandR11() {        
    }

    @Override
    public final VersionNumber getVersion() {
        return version110;
    }

    @Override
    public void dumpInfo(final long dpy, final int screen_idx) {
        // NOP
    }
    
    private int widthMM=0, heightMM=0;
    private int modeCount = 0;
    private int resolutionCount = 0;
    private int[][] nrates = null; // [nres_number][nrate_number]
    private int[] idx_rate = null, idx_res = null;
    
    @Override
    public boolean beginInitialQuery(long dpy, ScreenDriver screen) {
        // initialize iterators and static data
        final int screen_idx = screen.getIndex();
        resolutionCount = getNumScreenResolutions0(dpy, screen_idx);
        if(0==resolutionCount) {
            endInitialQuery(dpy, screen);
            return false;
        }

        nrates = new int[resolutionCount][];
        for(int i=0; i<resolutionCount; i++) {
            nrates[i] = getScreenRates0(dpy, screen_idx, i);
            if(null==nrates[i] || 0==nrates[i].length) {
                endInitialQuery(dpy, screen);
                return false;
            }
        }
        
        for(int nresIdx=0; nresIdx < resolutionCount; nresIdx++) {
            modeCount += nrates[nresIdx].length;
        }
        
        idx_rate = new int[modeCount];
        idx_res = new int[modeCount];

        int modeIdx=0;
        for(int nresIdx=0; nresIdx < resolutionCount; nresIdx++) {
            for(int nrateIdx=0; nrateIdx < nrates[nresIdx].length; nrateIdx++) {
                idx_rate[modeIdx] = nrateIdx;
                idx_res[modeIdx] = nresIdx;
                modeIdx++;
            }
        }
        return true;
    }
    
    @Override
    public void endInitialQuery(long dpy, ScreenDriver screen) {
        idx_rate=null;
        idx_res=null;            
        nrates=null;        
    }
    
    @Override
    public int getMonitorDeviceCount(final long dpy, final ScreenDriver screen) {
        return 1;
    }
    
    @Override
    public int[] getAvailableRotations(final long dpy, final ScreenDriver screen, final int crt_idx) {
        if( 0 < crt_idx ) {
            // RandR11 only supports 1 CRT 
            return null;
        }
        final int screen_idx = screen.getIndex();
        final int[] availRotations = getAvailableScreenRotations0(dpy, screen_idx);
        if(null==availRotations || 0==availRotations.length) {
            return null;
        }
        return availRotations;
    }
    
    @Override
    public int[] getMonitorModeProps(final long dpy, final ScreenDriver screen, final int mode_idx) {
        if( mode_idx >= modeCount ) {
            return null;
        }        
        final int screen_idx = screen.getIndex();
        
        final int nres_index = idx_res[mode_idx];
        final int nrate_index = idx_rate[mode_idx];
        
        final int[] res = getScreenResolution0(dpy, screen_idx, nres_index);
        if(null==res || 0==res.length) {
            return null;
        }
        if(0>=res[0] || 0>=res[1]) {
            throw new InternalError("invalid resolution: "+res[0]+"x"+res[1]+" for res idx "+nres_index+"/"+resolutionCount);
        }
        if( res[2] > widthMM ) {
            widthMM = res[2];
        }
        if( res[3] > heightMM ) {
            heightMM = res[3];
        }
        
        int rate = nrates[nres_index][nrate_index];
        if(0>=rate) {
            rate = ScreenImpl.default_sm_rate;
            if(DEBUG) {
                System.err.println("Invalid rate: "+rate+" at index "+nrate_index+"/"+nrates.length+", using default: "+ScreenImpl.default_sm_rate);
            }
        }

        int[] props = new int[ MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL ];
        int i = 0;
        props[i++] = MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL;
        props[i++] = res[0]; // width
        props[i++] = res[1]; // height
        props[i++] = ScreenImpl.default_sm_bpp; // bpp n/a in RandR11
        props[i++] = rate*100;  // rate (Hz*100)
        props[i++] = 0; // flags;
        props[i++] = nres_index;
        props[i++] = -1; // rotation placeholder;
        if( MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL != i ) {
            throw new InternalError("XX");
        }
        return props;        
    }
    
    @Override
    public int[] getMonitorDeviceProps(final long dpy, final ScreenDriver screen, final MonitorModeProps.Cache cache, final int crt_idx) {
        if( 0 < crt_idx ) {
            // RandR11 only supports 1 CRT 
            return null;
        }
        final int[] currentModeProps = getCurrentMonitorModeProps(dpy, screen, crt_idx);
        if( null == currentModeProps) { // disabled
            return null;
        }
        final MonitorMode currentMode = MonitorModeProps.streamInMonitorMode(null, cache, currentModeProps, 0);
        final int allModesCount = cache.monitorModes.size();
        final int[] props = new int[MonitorModeProps.MIN_MONITOR_DEVICE_PROPERTIES - 1 + allModesCount];
        int i = 0;
        props[i++] = props.length;
        props[i++] = crt_idx;
        props[i++] = widthMM;
        props[i++] = heightMM;
        props[i++] = 0; // rotated viewport x
        props[i++] = 0; // rotated viewport y
        props[i++] = currentMode.getRotatedWidth();  // rotated viewport width
        props[i++] = currentMode.getRotatedHeight(); // rotated viewport height
        props[i++] = currentMode.getId(); // current mode id
        props[i++] = currentMode.getRotation();
        for(int j=0; j<allModesCount; j++) {
            props[i++] = cache.monitorModes.get(j).getId(); 
        }
        return props;
    }
    
    @Override
    public int[] getMonitorDeviceViewport(final long dpy, final ScreenDriver screen, final int crt_idx) {
        if( 0 < crt_idx ) {
            // RandR11 only supports 1 CRT 
            return null;
        }
        final int screen_idx = screen.getIndex();
        long screenConfigHandle = getScreenConfiguration0(dpy, screen_idx);
        if(0 == screenConfigHandle) {
            return null;
        }
        int[] res;
        final int nres_idx;
        try {                
            int resNumber = getNumScreenResolutions0(dpy, screen_idx);
            if(0==resNumber) {
                return null;
            }

            nres_idx = getCurrentScreenResolutionIndex0(screenConfigHandle);
            if(0>nres_idx) {
                return null;
            }
            if(nres_idx>=resNumber) {
                throw new RuntimeException("Invalid resolution index: ! "+nres_idx+" < "+resNumber);
            }
            res = getScreenResolution0(dpy, screen_idx, nres_idx);
            if(null==res || 0==res.length) {
                return null;
            }
            if(0>=res[0] || 0>=res[1]) {
                throw new InternalError("invalid resolution: "+res[0]+"x"+res[1]+" for res idx "+nres_idx+"/"+resNumber);
            }
        } finally {
             freeScreenConfiguration0(screenConfigHandle);
        }
        int[] props = new int[4];
        int i = 0;
        props[i++] = 0;
        props[i++] = 0;
        props[i++] = res[0]; // width
        props[i++] = res[1]; // height
        return props;        
    }
    
    @Override
    public int[] getCurrentMonitorModeProps(final long dpy, final ScreenDriver screen, final int crt_idx) {
        if( 0 < crt_idx ) {
            // RandR11 only supports 1 CRT 
            return null;
        }
        final int screen_idx = screen.getIndex();
        long screenConfigHandle = getScreenConfiguration0(dpy, screen_idx);
        if(0 == screenConfigHandle) {
            return null;
        }
        int[] res;
        int rate, rot;
        final int nres_idx;
        try {                
            int resNumber = getNumScreenResolutions0(dpy, screen_idx);
            if(0==resNumber) {
                return null;
            }

            nres_idx = getCurrentScreenResolutionIndex0(screenConfigHandle);
            if(0>nres_idx) {
                return null;
            }
            if(nres_idx>=resNumber) {
                throw new RuntimeException("Invalid resolution index: ! "+nres_idx+" < "+resNumber);
            }
            res = getScreenResolution0(dpy, screen_idx, nres_idx);
            if(null==res || 0==res.length) {
                return null;
            }
            if(0>=res[0] || 0>=res[1]) {
                throw new InternalError("invalid resolution: "+res[0]+"x"+res[1]+" for res idx "+nres_idx+"/"+resNumber);
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
        int[] props = new int[ MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL ];
        int i = 0;
        props[i++] = MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL;
        props[i++] = res[0]; // width
        props[i++] = res[1]; // height
        props[i++] = ScreenImpl.default_sm_bpp;
        props[i++] = rate*100;  // rate (Hz*100)
        props[i++] = 0; // flags;
        props[i++] = nres_idx; // mode_idx;
        props[i++] = rot;
        if( MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL != i ) {
            throw new InternalError("XX");
        }
        return props;
    }

    @Override
    public boolean setCurrentMonitorMode(final long dpy, final ScreenDriver screen, MonitorDevice monitor, final MonitorMode mode) {
        final long t0 = System.currentTimeMillis();
        boolean done = false;
        final int screen_idx = screen.getIndex();
        long screenConfigHandle = getScreenConfiguration0(dpy, screen_idx);
        if(0 == screenConfigHandle) {
            return Boolean.valueOf(done);
        }
        try {
            final int resId = mode.getId();
            if(0>resId || resId>=resolutionCount) {
                throw new RuntimeException("Invalid resolution index: ! 0 < "+resId+" < "+resolutionCount+", "+monitor+", "+mode);
            }    
            final int f = (int)mode.getRefreshRate(); // simply cut-off, orig is int
            final int r = mode.getRotation();

            if( setCurrentScreenModeStart0(dpy, screen_idx, screenConfigHandle, resId, f, r) ) {
                while(!done && System.currentTimeMillis()-t0 < ScreenImpl.SCREEN_MODE_CHANGE_TIMEOUT) {
                    done = setCurrentScreenModePollEnd0(dpy, screen_idx, resId, f, r);
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
    
    @Override
    public final void updateScreenViewport(final long dpy, final ScreenDriver screen, RectangleImmutable viewport) {
        // nop
    }    

    /** @return int[] { rot1, .. } */
    private static native int[] getAvailableScreenRotations0(long display, int screen_index);

    private static native int getNumScreenResolutions0(long display, int screen_index);

    /** @return int[] { width, height, widthmm, heightmm } */
    private static native int[] getScreenResolution0(long display, int screen_index, int mode_index);

    private static native int[] getScreenRates0(long display, int screen_index, int mode_index);

    private static native long getScreenConfiguration0(long display, int screen_index);
    private static native void freeScreenConfiguration0(long screenConfiguration);
    
    private static native int getCurrentScreenResolutionIndex0(long screenConfiguration);
    private static native int getCurrentScreenRate0(long screenConfiguration);
    private static native int getCurrentScreenRotation0(long screenConfiguration);

    /** needs own Display connection for XRANDR event handling */
    private static native boolean setCurrentScreenModeStart0(long display, int screen_index, long screenConfiguration, int mode_index, int freq, int rot);
    private static native boolean setCurrentScreenModePollEnd0(long display, int screen_index, int mode_index, int freq, int rot);
    
}
