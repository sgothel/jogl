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

import java.util.List;

import com.jogamp.nativewindow.util.RectangleImmutable;

import jogamp.newt.MonitorModeProps;

import com.jogamp.common.util.VersionNumber;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;

public interface RandR {
    public static final VersionNumber version110 = new VersionNumber(1, 1, 0);
    public static final VersionNumber version130 = new VersionNumber(1, 3, 0);
    public static final VersionNumber version140 = new VersionNumber(1, 4, 0);

    VersionNumber getVersion();
    @Override
    String toString();

    void dumpInfo(final long dpy, final int screen_idx);

    /**
     * Encapsulate initial device query allowing caching of internal data structures.
     * Methods covered:
     * <ul>
     *   <li>{@link #getMonitorDeviceIds(long, ScreenDriver)}</li>
     *   <li>{@link #getAvailableRotations(long, ScreenDriver, int)}</li>
     *   <li>{@link #getMonitorModeProps(long, ScreenDriver, int)}</li>
     *   <li>{@link #getCurrentMonitorModeProps(long, ScreenDriver, int)}</li>
     *   <li>{@link #getMonitorDeviceProps(long, ScreenDriver, List, int, MonitorMode)}</li>
     * </ul>
     * <p>
     * Above methods may be called w/o begin/end, in which case no
     * internal data structures can be cached:
     * </p>
     * @param dpy TODO
     * @param screen TODO
     * @return TODO
     */
    boolean beginInitialQuery(long dpy, ScreenDriver screen);
    void endInitialQuery(long dpy, ScreenDriver screen);

    int[] getMonitorDeviceIds(final long dpy, final ScreenDriver screen);
    int[] getAvailableRotations(final long dpy, final ScreenDriver screen, final int crt_id);
    /**
     *
     * @param dpy
     * @param screen
     * @param mode_idx w/o indexing rotation
     * @return props w/o actual rotation
     */
    int[] getMonitorModeProps(final long dpy, final ScreenDriver screen, final int mode_idx);
    int[] getMonitorDeviceProps(final long dpy, final ScreenDriver screen, MonitorModeProps.Cache cache, final int crt_id);
    int[] getMonitorDeviceViewport(final long dpy, final ScreenDriver screen, final int crt_id);
    int[] getCurrentMonitorModeProps(final long dpy, final ScreenDriver screen, final int crt_id);
    /** The device shall be locked, blocking message handling.  */
    boolean setCurrentMonitorModeStart(final long dpy, final ScreenDriver screen, MonitorDevice monitor, final MonitorMode mode);
    /** The device must not be locked, allowing message handling. */
    boolean setCurrentMonitorModeWait(final ScreenDriver screen);
    /** Invoked from Display's EDT thread for message handling. */
    void sendRRScreenChangeNotify(final long dpy, final long event);

    void updateScreenViewport(final long dpy, final ScreenDriver screen, RectangleImmutable viewport);
}
