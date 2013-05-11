/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2011 JogAmp Community. All rights reserved.
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

package jogamp.newt.driver.macosx;

import javax.media.nativewindow.DefaultGraphicsScreen;

import jogamp.newt.MonitorModeProps;
import jogamp.newt.ScreenImpl;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;

public class ScreenDriver extends ScreenImpl {
    
    static {
        DisplayDriver.initSingleton();
    }

    public ScreenDriver() {
    }

    protected void createNativeImpl() {
        aScreen = new DefaultGraphicsScreen(getDisplay().getGraphicsDevice(), screen_idx);
    }

    protected void closeNativeImpl() { }

    private MonitorMode getMonitorModeImpl(MonitorModeProps.Cache cache, int crt_idx, int mode_idx) {
        final int[] modeProps = getMonitorMode0(crt_idx, mode_idx);
        final MonitorMode res;
        if (null == modeProps  || 0 >= modeProps.length) {
            res = null;
        } else {
            res = MonitorModeProps.streamInMonitorMode(null, cache, modeProps, 0);
        }
        return res;
    }
    
    @Override
    protected final void collectNativeMonitorModesAndDevicesImpl(MonitorModeProps.Cache cache) {
        int crtIdx = 0;
        int modeIdx = 0;
        ArrayHashSet<MonitorMode> supportedModes = new ArrayHashSet<MonitorMode>();
        do {
            final MonitorMode mode = getMonitorModeImpl(cache, crtIdx, modeIdx);
            if( null != mode ) {
                supportedModes.getOrAdd(mode);
                // next mode on same monitor
                modeIdx++;
            } else if( 0 < modeIdx ) {
                // end of monitor modes - got at least one mode
                final MonitorMode currentMode = getMonitorModeImpl(cache, crtIdx, -1);
                if ( null == currentMode ) {
                    throw new InternalError("Could not gather current mode of device "+crtIdx+", but gathered "+modeIdx+" modes");
                }                
                final int[] monitorProps = getMonitorProps0(crtIdx);
                if ( null == monitorProps ) {
                    throw new InternalError("Could not gather device "+crtIdx+", but gathered "+modeIdx+" modes");
                }                
                // merge monitor-props + supported modes
                MonitorModeProps.streamInMonitorDevice(null, cache, this, supportedModes, currentMode, monitorProps, 0);
                
                // next monitor, 1st mode
                supportedModes= new ArrayHashSet<MonitorMode>();                
                crtIdx++;
                modeIdx=0;
            } else {
                // end of monitor
                break;
            }
        } while ( true );
    }

    @Override
    protected MonitorMode queryCurrentMonitorModeImpl(MonitorDevice monitor) {
        return getMonitorModeImpl(null, monitor.getId(), -1);
    }
    
    @Override
    protected boolean setCurrentMonitorModeImpl(MonitorDevice monitor, MonitorMode mode)  {
        return setMonitorMode0(monitor.getId(), mode.getId(), mode.getRotation());
    }
    
    protected int validateScreenIndex(int idx) {
        return 0; // big-desktop w/ multiple monitor attached, only one screen available 
    }
        
    private native int[] getMonitorProps0(int crt_idx);
    private native int[] getMonitorMode0(int crt_index, int mode_idx);
    private native boolean setMonitorMode0(int crt_index, int nativeId, int rot);
}
