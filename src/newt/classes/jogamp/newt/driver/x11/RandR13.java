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

import com.jogamp.nativewindow.util.RectangleImmutable;

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
class RandR13 implements RandR {
    private static final boolean DEBUG = ScreenDriver.DEBUG;
    private final VersionNumber version;

    RandR13(final VersionNumber version) {
        this.version = version;
    }

    @Override
    public final VersionNumber getVersion() { return version; }
    @Override
    public String toString() {
        return "RandR13[version "+version+"]";
    }

    @Override
    public void dumpInfo(final long dpy, final int screen_idx) {
        final long screenResources = getScreenResources0(dpy, screen_idx);
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
    public boolean beginInitialQuery(final long dpy, final ScreenDriver screen) {
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
    public void endInitialQuery(final long dpy, final ScreenDriver screen) {
        if( null != crtInfoHandleMap ) {
            for(final Iterator<IntLongHashMap.Entry> iter = crtInfoHandleMap.iterator(); iter.hasNext(); ) {
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

    private final long getMonitorInfoHandle(final long dpy, final int screen_idx, final long screenResources, final int crt_id) {
        if( null != crtInfoHandleMap ) {
            long h = crtInfoHandleMap.get(crt_id);
            if( 0 == h ) {
                h = getMonitorInfoHandle0(dpy, screen_idx, screenResources, crt_id);
                crtInfoHandleMap.put(crt_id, h);
            }
            return h;
        } else {
            return getMonitorInfoHandle0(dpy, screen_idx, screenResources, crt_id);
        }
    }
    private final void releaseMonitorInfoHandle(final long monitorInfoHandle) {
        if( null == crtInfoHandleMap ) {
            freeMonitorInfoHandle0(monitorInfoHandle);
        }
    }

    @Override
    public int[] getMonitorDeviceIds(final long dpy, final ScreenDriver screen) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            return getMonitorDeviceIds0(screenResources);
        } finally {
            releaseScreenResourceHandle(screenResources);
        }
    }

    @Override
    public int[] getAvailableRotations(final long dpy, final ScreenDriver screen, final int crt_id) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            final long monitorInfo = getMonitorInfoHandle(dpy, screen_idx, screenResources, crt_id);
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
    public int[] getMonitorDeviceProps(final long dpy, final ScreenDriver screen, final MonitorModeProps.Cache cache, final int crt_id) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            final long monitorInfo = getMonitorInfoHandle(dpy, screen_idx, screenResources, crt_id);
            try {
                return getMonitorDevice0(dpy, screenResources, monitorInfo, crt_id);
            } finally {
                releaseMonitorInfoHandle(monitorInfo);
            }
        } finally {
            releaseScreenResourceHandle(screenResources);
        }
    }

    @Override
    public int[] getMonitorDeviceViewport(final long dpy, final ScreenDriver screen, final int crt_id) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            final long monitorInfo = getMonitorInfoHandle(dpy, screen_idx, screenResources, crt_id);
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
    public int[] getCurrentMonitorModeProps(final long dpy, final ScreenDriver screen, final int crt_id) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            final long monitorInfo = getMonitorInfoHandle(dpy, screen_idx, screenResources, crt_id);
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
    public boolean setCurrentMonitorModeStart(final long dpy, final ScreenDriver screen, final MonitorDevice monitor, final MonitorMode mode) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        final boolean res;
        try {
            final long monitorInfo = getMonitorInfoHandle(dpy, screen_idx, screenResources, monitor.getId());
            try {
                res = setMonitorMode0(dpy, screen_idx, screenResources, monitorInfo, monitor.getId(),
                                      mode.getId(), mode.getRotation(), -1, -1); // no fixed position!
            } finally {
                releaseMonitorInfoHandle(monitorInfo);
            }
        } finally {
            releaseScreenResourceHandle(screenResources);
        }
        return res;
    }
    @Override
    public void sendRRScreenChangeNotify(final long dpy, final long event) {
        sendRRScreenChangeNotify0(dpy, event);
    }
    @Override
    public boolean setCurrentMonitorModeWait(final ScreenDriver screen) {
        // RandR13 set command waits until done ..
        return true;
    }

    @Override
    public final void updateScreenViewport(final long dpy, final ScreenDriver screen, final RectangleImmutable viewport) {
        final int screen_idx = screen.getIndex();
        final long screenResources = getScreenResourceHandle(dpy, screen_idx);
        try {
            setScreenViewport0(dpy, screen_idx, screenResources, viewport.getX(), viewport.getY(), viewport.getWidth(), viewport.getHeight());
        } finally {
            releaseScreenResourceHandle(screenResources);
        }
    }

    private static native long getScreenResources0(long display, int screen_index);
    private static native void freeScreenResources0(long screenResources);
    private static native void dumpInfo0(long display, int screen_index, long screenResources);

    private static native int[] getMonitorDeviceIds0(long screenResources);

    private static native long getMonitorInfoHandle0(long display, int screen_index, long screenResources, int crtc_id);
    private static native void freeMonitorInfoHandle0(long monitorInfoHandle);

    private static native int[] getAvailableRotations0(long monitorInfo);
    private static native int[] getMonitorViewport0(long monitorInfo);

    private static native int[] getMonitorMode0(long screenResources, int mode_index);
    private static native int[] getMonitorCurrentMode0(long screenResources, long monitorInfo);
    private static native int[] getMonitorDevice0(long display, long screenResources, long monitorInfo, int crtc_id);

    private static native boolean setMonitorMode0(long display, int screen_index, long screenResources,
                                                  long monitorInfo, int crtc_id,
                                                  int mode_id, int rotation, int x, int y);
    private static native boolean setScreenViewport0(long display, int screen_index, long screenResources,
                                                     int x, int y, int width, int height);
    private static native void sendRRScreenChangeNotify0(long display, final long event);
}
