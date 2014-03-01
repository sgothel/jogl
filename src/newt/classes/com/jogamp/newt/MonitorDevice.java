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

package com.jogamp.newt;

import java.util.List;

import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.Rectangle;
import javax.media.nativewindow.util.RectangleImmutable;
import javax.media.nativewindow.util.SurfaceSize;
import com.jogamp.common.util.ArrayHashSet;

/**
 * Visual output device, i.e. a CRT, LED ..consisting of it's components:<br>
 * <ui>
 *   <li>Immutable
 *   <ul>
 *     <li>nativeId</li>
 *     <li>{@link DimensionImmutable} size in [mm]</li>
 *     <li>{@link MonitorMode} original mode</li>
 *     <li><code>List&lt;MonitorMode&gt;</code> supportedModes</li>
 *   </ul></li>
 *   <li>Mutable
 *   <ul>
 *     <li>{@link MonitorMode} current mode</li>
 *     <li>{@link RectangleImmutable} viewport (rotated)</li>
 *   </ul></li>
 * </ul>
 */
public abstract class MonitorDevice {
    protected final Screen screen; // backref
    protected final int nativeId; // unique monitor device ID
    protected final DimensionImmutable sizeMM; // in [mm]
    protected final MonitorMode originalMode;
    protected final ArrayHashSet<MonitorMode> supportedModes; // FIXME: May need to support mutable mode, i.e. adding modes on the fly!
    protected MonitorMode currentMode;
    protected boolean modeChanged;
    protected Rectangle viewport;

    protected MonitorDevice(Screen screen, int nativeId, DimensionImmutable sizeMM, Rectangle viewport, MonitorMode currentMode, ArrayHashSet<MonitorMode> supportedModes) {
        this.screen = screen;
        this.nativeId = nativeId;
        this.sizeMM = sizeMM;
        this.originalMode = currentMode;
        this.supportedModes = supportedModes;
        this.currentMode = currentMode;
        this.viewport = viewport;
        this.modeChanged = false;
    }

    /** Returns the {@link Screen} owning this monitor. */
    public final Screen getScreen() {
        return screen;
    }

    /**
     * Tests equality of two <code>MonitorDevice</code> objects
     * by evaluating equality of it's components:<br>
     * <ul>
     *  <li><code>nativeID</code></li>
     * </ul>
     * <br>
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj instanceof MonitorDevice) {
            MonitorDevice md = (MonitorDevice)obj;
            return md.nativeId == nativeId;
        }
        return false;
    }

    /**
     * Returns a combined hash code of it's elements:<br>
     * <ul>
     *  <li><code>nativeID</code></li>
     * </ul>
     */
    @Override
    public final int hashCode() {
        return nativeId;
    }

    /** @return the immutable unique native Id of this monitor device. */
    public final int getId() { return nativeId; }

    /**
     * @return the immutable monitor size in millimeters.
     */
    public final DimensionImmutable getSizeMM() {
        return sizeMM;
    }

    /**
     * Stores the <i>pixels per millimeter</i> value according to <i>current</i> {@link MonitorMode}
     * {@link SurfaceSize#getResolution() SurfaceSize's resolution} in the given storage <code>ppmmStore</code>.
     * <p>
     * To convert the result to <i>dpi</i>, i.e. dots-per-inch, multiply both components with <code>25.4f</code>.
     * </p>
     */
    public final void getPixelsPerMM(final float[] ppmmStore) {
        final MonitorMode mode = getCurrentMode();
        getPixelsPerMM(mode, ppmmStore);
    }

    /**
     * Stores the <i>pixels per millimeter</i> value according to the given {@link MonitorMode}
     * {@link SurfaceSize#getResolution() SurfaceSize's resolution} in the given storage <code>ppmmStore</code>.
     * <p>
     * To convert the result to <i>dpi</i>, i.e. dots-per-inch, multiply both components with <code>25.4f</code>.
     * </p>
     */
    public final void getPixelsPerMM(final MonitorMode mode, final float[] ppmmStore) {
        final DimensionImmutable sdim = getSizeMM();
        final DimensionImmutable spix = mode.getSurfaceSize().getResolution();
        ppmmStore[0] = (float)spix.getWidth() / (float)sdim.getWidth();
        ppmmStore[1] = (float)spix.getHeight() / (float)sdim.getHeight();
    }

    /**
     * Returns the immutable original {@link com.jogamp.newt.MonitorMode}, as used at NEWT initialization.
     * <p>
     * The returned {@link MonitorMode} is element of the lists {@link #getSupportedModes()} and {@link Screen#getMonitorModes()}.
     * </p>
     */
    public final MonitorMode getOriginalMode() {
        return originalMode;
    }

    /**
     * Returns a list of immutable {@link MonitorMode}s supported by this monitor.
     * <p>
     * The list is ordered in descending order,
     * see {@link MonitorMode#compareTo(MonitorMode)}.
     * </p>
     * <p>
     * Use w/ care, it's not a copy!
     * </p>
     */
    public final List<MonitorMode> getSupportedModes() {
        return supportedModes.getData();
    }

    /** Returns the {@link RectangleImmutable rectangular} portion of the rotated virtual {@link Screen} size represented by this monitor. */
    public final RectangleImmutable getViewport() {
        return viewport;
    }

    /** Returns <code>true</code> if given coordinates are contained by this {@link #getViewport() viewport}, otherwise <code>false</code>. */
    public final boolean contains(int x, int y) {
        return x >= viewport.getX() &&
               x <  viewport.getX() + viewport.getWidth() &&
               y >= viewport.getY() &&
               y <  viewport.getY() + viewport.getHeight() ;
    }

    /**
     * Returns the coverage of given rectangle w/ this this {@link #getViewport() viewport}, i.e. between <code>0.0</code> and <code>1.0</code>.
     * <p>
     * Coverage is computed by:
     * <pre>
     *    isect = viewport.intersection(r);
     *    coverage = area( isect ) / area( viewport ) ;
     * </pre>
     * </p>
     */
    public final float coverage(RectangleImmutable r) {
        return viewport.coverage(r);
    }

    /**
     * Returns the union of the given monitor's {@link #getViewport() viewport}.
     * @param result storage for result, will be returned
     * @param monitors given list of monitors
     * @return viewport representing the union of given monitor's viewport.
     */
    public static Rectangle unionOfViewports(final Rectangle result, final List<MonitorDevice> monitors) {
        int x1=Integer.MAX_VALUE, y1=Integer.MAX_VALUE;
        int x2=Integer.MIN_VALUE, y2=Integer.MIN_VALUE;
        for(int i=monitors.size()-1; i>=0; i--) {
            final RectangleImmutable vp = monitors.get(i).getViewport();
            x1 = Math.min(x1, vp.getX());
            x2 = Math.max(x2, vp.getX() + vp.getWidth());
            y1 = Math.min(y1, vp.getY());
            y2 = Math.max(y2, vp.getY() + vp.getHeight());
        }
        result.set(x1, y1, x2 - x1, y2 - y1);
        return result;
    }

    public final boolean isOriginalMode() {
        return currentMode.hashCode() == originalMode.hashCode();
    }

    /**
     * Returns <code>true</true> if the {@link MonitorMode}
     * has been changed programmatic via this API <i>only</i>, otherwise <code>false</code>.
     * <p>
     * Note: We cannot guarantee that we won't interfere w/ another running
     * application's screen mode change or vice versa.
     * </p>
     */
    public final boolean isModeChangedByUs() {
        return modeChanged && !isOriginalMode();
    }

    /**
     * Returns the cached current {@link MonitorMode} w/o native query.
     * <p>
     * The returned {@link MonitorMode} is element of the lists {@link #getSupportedModes()} and {@link Screen#getMonitorModes()}.
     * </p>
     */
    public final MonitorMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Returns the current {@link MonitorMode} resulting from a native query.
     * <p>
     * The returned {@link MonitorMode} is element of the lists {@link #getSupportedModes()} and {@link Screen#getMonitorModes()}.
     * </p>
     */
    public abstract MonitorMode queryCurrentMode();

    /**
     * Set the current {@link com.jogamp.newt.MonitorMode}.
     * @param mode to be made current, must be element of the list {@link #getSupportedModes()} and {@link Screen#getMonitorModes()}.
     * @return true if successful, otherwise false
     */
    public abstract boolean setCurrentMode(MonitorMode mode);

    @Override
    public String toString() {
        return "Monitor[Id "+Display.toHexString(nativeId)+", "+sizeMM+" mm, viewport "+viewport+ ", orig "+originalMode+", curr "+currentMode+
               ", modeChanged "+modeChanged+", modeCount "+supportedModes.size()+"]";
    }
}

