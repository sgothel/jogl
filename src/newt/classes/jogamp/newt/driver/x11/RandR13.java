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

import java.util.Iterator;

import jogamp.newt.MonitorModeProps;

import com.jogamp.common.util.IntLongHashMap;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;

/**
 * Mapping details:
 * <pre>
 * MonitorMode.id   == XRR mode-id (not index)
 * MonitorDevice.id == XRR monitor-idx (not id)
 * </pre> 
 */
public class RandR13 implements RandR {
    private static final boolean DEBUG = ScreenDriver.DEBUG;
    
    public static VersionNumber version = new VersionNumber(1, 3, 0);

    public static RandR13 createInstance(VersionNumber rAndRVersion) {
        if( rAndRVersion.compareTo(version) >= 0 ) {
            return new RandR13();
        }
        return null;
    }    
    private RandR13() {        
    }
    
    @Override
    public void dumpInfo(final long dpy, final int screen_idx) {
        long screenResources = getScreenResources0(dpy, screen_idx);
        if(0 == screenResources) {
            return;
        }
        try {
            dumpInfo0(dpy, screen_idx, screenResources);
        } finally {
             freeScreenResources0(screenResources);
        }        
    }
    
    long sessionScreenResources = 0;
    IntLongHashMap crtInfoHandleMap = null;
    
    @Override
    public boolean beginInitialQuery(long dpy, ScreenDriver screen) {
        final int screen_idx = screen.getIndex();
        sessionScreenResources = getScreenResources0(dpy, screen_idx);
        if( 0 != sessionScreenResources ) {
            crtInfoHandleMap = new IntLongHashMap();
            crtInfoHandleMap.setKeyNotFoundValue(0);
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public void endInitialQuery(long dpy, ScreenDriver screen) {
        if( null != crtInfoHandleMap ) {
            for(Iterator<IntLongHashMap.Entry> iter = crtInfoHandleMap.iterator(); iter.hasNext(); ) {
                final IntLongHashMap.Entry entry = iter.next();
                freeMonitorInfoHandle0(entry.value);
            }
            crtInfoHandleMap.clear();
            crtInfoHandleMap = null;
        }
        if( 0 != sessionScreenResources ) {
            freeScreenResources0( sessionScreenResources );
            sessionScreenResources = 0;
        }
    }
    
    private final long getScreenResourceHandle(final long dpy, final int screen_idx) {
        if( 0 != sessionScreenResources ) {
            return sessionScreenResources;
        }
        return getScreenResources0(dpy, screen_idx);
    }
    private final void releaseScreenResourceHandle(final long screenResourceHandle) {
        if( 0 == sessionScreenResources ) {
            freeScreenResources0( screenResourceHandle );
        }
    }
    
    private final long getMonitorInfoHandle(final long dpy, final int screen_idx, long screenResources, final int monitor_idx) {
        if( null != crtInfoHandleMap ) {
            long h = crtInfoHandleMap.get(monitor_idx);
            if( 0 == h ) {
                h = getMonitorInfoHandle0(dpy, screen_idx, screenResources, monitor_idx);
                crtInfoHandleMap.put(monitor_idx, h);
            }
            return h;
        } else {
            return getMonitorInfoHandle0(dpy, screen_idx, screenResources, monitor_idx);
        }
    }
    private final void releaseMonitorInfoHandle(final long monitorInfoHandle) {
        if( null == crtInfoHandleMap ) {
            freeMonitorInfoHandle0(monitorInfoHandle);
        }
    }    
    
    @Override
    public int getMonitorDeviceCount(final long dpy, final ScreenDriver screen) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            return getMonitorDeviceCount0(screenResources);
        } finally {
            releaseScreenResourceHandle(screenResources);
        }
    }
    
    @Override
    public int[] getAvailableRotations(final long dpy, final ScreenDriver screen, final int crt_idx) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            final long monitorInfo = getMonitorInfoHandle(dpy, screen_idx, screenResources, crt_idx);
            try {
                final int[] availRotations = getAvailableRotations0(monitorInfo);
                if(null==availRotations || 0==availRotations.length) {
                    return null;
                }        
                return availRotations;
            } finally {
                releaseMonitorInfoHandle(monitorInfo);
            }
        } finally {
            releaseScreenResourceHandle(screenResources);
        }
    }
    
    @Override
    public int[] getMonitorModeProps(final long dpy, final ScreenDriver screen, final int mode_idx) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            return getMonitorMode0(screenResources, mode_idx);
        } finally {
            releaseScreenResourceHandle(screenResources);
        }
    }
    
    @Override
    public int[] getMonitorDeviceProps(final long dpy, final ScreenDriver screen, MonitorModeProps.Cache cache, final int crt_idx) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            final long monitorInfo = getMonitorInfoHandle(dpy, screen_idx, screenResources, crt_idx);
            try {
                return getMonitorDevice0(dpy, screenResources, monitorInfo, crt_idx);
            } finally {
                releaseMonitorInfoHandle(monitorInfo);
            }
        } finally {
            releaseScreenResourceHandle(screenResources);
        }
    }
    
    @Override
    public int[] getMonitorDeviceViewport(final long dpy, final ScreenDriver screen, final int crt_idx) {        
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            final long monitorInfo = getMonitorInfoHandle(dpy, screen_idx, screenResources, crt_idx);
            try {
                return getMonitorViewport0(monitorInfo);
            } finally {
                releaseMonitorInfoHandle(monitorInfo);
            }
        } finally {
            releaseScreenResourceHandle(screenResources);
        }
    }
    
    @Override
    public int[] getCurrentMonitorModeProps(final long dpy, final ScreenDriver screen, final int crt_idx) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            final long monitorInfo = getMonitorInfoHandle(dpy, screen_idx, screenResources, crt_idx);
            try {
                return getMonitorCurrentMode0(screenResources, monitorInfo);
            } finally {
                releaseMonitorInfoHandle(monitorInfo);
            }
        } finally {
            releaseScreenResourceHandle(screenResources);
        }
    }
    
    @Override
    public boolean setCurrentMonitorMode(final long dpy, final ScreenDriver screen, MonitorDevice monitor, final MonitorMode mode) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        final boolean res;
        try {
            final long monitorInfo = getMonitorInfoHandle(dpy, screen_idx, screenResources, monitor.getId());
            try {
                res = setMonitorMode0(dpy, screenResources, monitorInfo, monitor.getId(), mode.getId(), mode.getRotation(), 
                                      -1, -1); // no fixed position!
            } finally {
                releaseMonitorInfoHandle(monitorInfo);
            }
        } finally {
            releaseScreenResourceHandle(screenResources);
        }
        /*** 
         * TODO: Would need a complete re-layout of crt positions,
         *       which is _not_ implicit by XRandR .. sadly.
         *        
        if( res ) {
            updateScreenViewport(dpy, screen, monitor);
        } */
        return res;
    }
    
    /** See above ..
    private final void updateScreenViewport(final long dpy, final ScreenDriver screen, MonitorDevice monitor) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            RectangleImmutable newViewp = null;
            final long monitorInfo = getMonitorInfoHandle(dpy, screen_idx, screenResources, monitor.getId());
            try {
                final int[] vprops = getMonitorViewport0(monitorInfo);
                if( null != vprops ) {
                    newViewp = new Rectangle(vprops[0], vprops[1], vprops[2], vprops[3]);
                }
                System.err.println("XXX setScreenViewport: newVp "+newViewp);
            } finally {
                releaseMonitorInfoHandle(monitorInfo);
            }
            if( null != newViewp ) {
                final List<MonitorDevice> monitors = screen.getMonitorDevices();
                final ArrayList<RectangleImmutable> viewports = new ArrayList<RectangleImmutable>();
                for(int i=0; i<monitors.size(); i++) {
                    final MonitorDevice crt = monitors.get(i);
                    if( crt.getId() != monitor.getId() ) {
                        System.err.println("XXX setScreenViewport: add.pre["+i+"]: "+crt.getViewport());
                        viewports.add( crt.getViewport() ) ;
                    } else {
                        System.err.println("XXX setScreenViewport: add.new["+i+"]: "+newViewp);
                        viewports.add( newViewp );
                    }
                }
                final RectangleImmutable newScrnViewp = new Rectangle().union(viewports);
                System.err.println("XXX setScreenViewport: "+screen.getViewport()+" -> "+newScrnViewp);
                setScreenViewport0(dpy, screen_idx, screenResources, newScrnViewp.getX(), newScrnViewp.getY(), newScrnViewp.getWidth(), newScrnViewp.getHeight()); 
            }
        } finally {
            dumpInfo0(dpy, screen_idx, screenResources);
            releaseScreenResourceHandle(screenResources);
        }        
    } */
        
    private static native long getScreenResources0(long display, int screen_index);
    private static native void freeScreenResources0(long screenResources);
    private static native void dumpInfo0(long display, int screen_index, long screenResources);
    
    private static native int getMonitorDeviceCount0(long screenResources);
    
    private static native long getMonitorInfoHandle0(long display, int screen_index, long screenResources, int monitor_index);
    private static native void freeMonitorInfoHandle0(long monitorInfoHandle);
    
    private static native int[] getAvailableRotations0(long monitorInfo);
    private static native int[] getMonitorViewport0(long monitorInfo);
    private static native int[] getMonitorCurrentMode0(long monitorInfo);
    
    private static native int[] getMonitorMode0(long screenResources, int mode_index);
    private static native int[] getMonitorCurrentMode0(long screenResources, long monitorInfo);
    private static native int[] getMonitorDevice0(long display, long screenResources, long monitorInfo, int monitor_idx);
    
    private static native boolean setMonitorMode0(long display, long screenResources, long monitorInfo, int monitor_idx, int mode_id, int rotation, int x, int y);
    private static native boolean setScreenViewport0(long display, int screen_index, long screenResources, int x, int y, int width, int height);
}
