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
 
package jogamp.newt;

import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.Rectangle;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.Screen;

public class MonitorDeviceImpl extends MonitorDevice {

    public MonitorDeviceImpl(ScreenImpl screen, int nativeId, DimensionImmutable sizeMM, Rectangle viewport, MonitorMode currentMode, ArrayHashSet<MonitorMode> supportedModes) {
        super(screen, nativeId, sizeMM, viewport, currentMode, supportedModes);
    }
    
    @Override
    public final MonitorMode queryCurrentMode() {
        final ScreenImpl screenImpl = (ScreenImpl)screen;
        final ScreenMonitorState sms = screenImpl.getScreenMonitorStatus(true);
        sms.lock();
        try {
            final MonitorMode mm0 = screenImpl.queryCurrentMonitorModeIntern(this);
            if(null == mm0) {
                throw new InternalError("getCurrentMonitorModeIntern() == null");
            }
            MonitorMode mmU = supportedModes.get(mm0); // unified instance
            if( null == mmU ) {
                // add new mode avoiding exception! 
                mmU = sms.getMonitorModes().getOrAdd(mm0);
                mmU = supportedModes.getOrAdd(mmU);
                if( Screen.DEBUG ) {
                    System.err.println("Adding new mode: "+mm0+" -> "+mmU);
                }
            }
            // if mode has changed somehow, update it ..
            if( getCurrentMode().hashCode() != mmU.hashCode() ) {
                setCurrentModeValue(mmU);
                sms.fireMonitorModeChanged(this, mmU, true);
            }
            return mmU;
        } finally {
            sms.unlock();
        }
    }

    @Override
    public final boolean setCurrentMode(MonitorMode mode) {
        if(Screen.DEBUG) {
            System.err.println("Screen.setCurrentMode.0: "+this+" -> "+mode);
        }
        final ScreenImpl screenImpl = (ScreenImpl)screen;
        final ScreenMonitorState sms = screenImpl.getScreenMonitorStatus(true);
        sms.lock();
        try {
            final MonitorMode mmC = queryCurrentMode();
            final MonitorMode mmU = supportedModes.get(mode); // unify via value hash
            if( null == mmU ) {
                throw new IllegalArgumentException("Given mode not in set of modes. Current mode "+mode+", "+this);
            }
            if( mmU.equals( mmC ) ) {
                if(Screen.DEBUG) {
                    System.err.println("Screen.setCurrentMode: 0.0 is-current (skip) "+mmU+" == "+mmC);
                }            
                return true;
            }
            final long tStart;
            if(Screen.DEBUG) {
                tStart = System.nanoTime();                
            } else {
                tStart = 0;
            }
            
            sms.fireMonitorModeChangeNotify(this, mmU);
            if(Screen.DEBUG) {
                System.err.println("Screen.setCurrentMode ("+(System.nanoTime()-tStart)/1e6+"ms): fireModeChangeNotify() "+mmU);
            }

            boolean success = screenImpl.setCurrentMonitorModeImpl(this, mmU);
            if(success) {
                if(Screen.DEBUG) {
                    System.err.println("Screen.setCurrentMode ("+(System.nanoTime()-tStart)/1e6+"ms): setCurrentModeImpl() "+mmU+", success(1): "+success);
                }
            } else {
                // 2nd attempt validate!
                final MonitorMode queriedCurrent = queryCurrentMode(); // may fireModeChanged(..) if successful and differs!
                success = queriedCurrent.hashCode() == mmU.hashCode() ;
                if(Screen.DEBUG) {
                    System.err.println("Screen.setCurrentMode.2: queried "+queriedCurrent);
                    System.err.println("Screen.setCurrentMode ("+(System.nanoTime()-tStart)/1e6+"ms): setCurrentModeImpl() "+mmU+", success(2): "+success);
                }
            }
            if( success ) {
                setCurrentModeValue(mmU);
                modeChanged = !isOriginalMode();
            }
            sms.fireMonitorModeChanged(this, mmU, success);
            if(Screen.DEBUG) {
                System.err.println("Screen.setCurrentMode ("+(System.nanoTime()-tStart)/1e6+"ms): X.X "+this+", success: "+success);
            }
            return success;
        } finally {
            sms.unlock();
        }
    }

    private final void setCurrentModeValue(MonitorMode currentMode) {
        this.currentMode = currentMode;
    }
    
    /* pp */ final void setViewportValue(Rectangle viewport) {
        this.viewport = viewport;
    }
    
    /* pp */ ArrayHashSet<MonitorMode> getSupportedModesImpl() {
        return supportedModes;
    }
    
}
