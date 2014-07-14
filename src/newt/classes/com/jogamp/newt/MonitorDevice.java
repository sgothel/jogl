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
 * <p>
 * All values of this interface are represented in pixel units, if not stated otherwise.
 * </p>
 */
public abstract class MonitorDevice {
    protected final Screen screen; // backref
    protected final int nativeId; // unique monitor device ID
    protected final DimensionImmutable sizeMM; // in [mm]
    protected final MonitorMode originalMode;
    protected final ArrayHashSet<MonitorMode> supportedModes; // FIXME: May need to support mutable mode, i.e. adding modes on the fly!
    protected MonitorMode currentMode;
    protected boolean modeChanged;
    protected Rectangle viewportPU; // in pixel units
    protected Rectangle viewportWU; // in window units

    protected MonitorDevice(final Screen screen, final int nativeId, final DimensionImmutable sizeMM, final Rectangle viewportPU, final Rectangle viewportWU, final MonitorMode currentMode, final ArrayHashSet<MonitorMode> supportedModes) {
        this.screen = screen;
        this.nativeId = nativeId;
        this.sizeMM = sizeMM;
        this.originalMode = currentMode;
        this.supportedModes = supportedModes;
        this.currentMode = currentMode;
        this.viewportPU = viewportPU;
        this.viewportWU = viewportWU;
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
    public final boolean equals(final Object obj) {
        if (this == obj) { return true; }
        if (obj instanceof MonitorDevice) {
            final MonitorDevice md = (MonitorDevice)obj;
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
     * Returns the <i>pixels per millimeter</i> value according to the <i>current</i> {@link MonitorMode mode}'s
     * {@link SurfaceSize#getResolution() surface resolution}.
     * <p>
     * To convert the result to <i>dpi</i>, i.e. dots-per-inch, multiply both components with <code>25.4f</code>.
     * </p>
     * @param ppmmStore float[2] storage for the ppmm result
     * @return the passed storage containing the ppmm for chaining
     */
    public final float[] getPixelsPerMM(final float[] ppmmStore) {
        return getPixelsPerMM(getCurrentMode(), ppmmStore);
    }

    /**
     * Returns the <i>pixels per millimeter</i> value according to the given {@link MonitorMode mode}'s
     * {@link SurfaceSize#getResolution() surface resolution}.
     * <p>
     * To convert the result to <i>dpi</i>, i.e. dots-per-inch, multiply both components with <code>25.4f</code>.
     * </p>
     * @param mode
     * @param ppmmStore float[2] storage for the ppmm result
     * @return the passed storage containing the ppmm for chaining
     */
    public final float[] getPixelsPerMM(final MonitorMode mode, final float[] ppmmStore) {
        final DimensionImmutable sdim = getSizeMM();
        final DimensionImmutable spix = mode.getSurfaceSize().getResolution();
        ppmmStore[0] = (float)spix.getWidth() / (float)sdim.getWidth();
        ppmmStore[1] = (float)spix.getHeight() / (float)sdim.getHeight();
        return ppmmStore;
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

    /**
     * Returns the {@link RectangleImmutable rectangular} portion
     * of the <b>rotated</b> virtual {@link Screen} size in pixel units
     * represented by this monitor, i.e. top-left origin and size.
     * @see Screen
     */
    public final RectangleImmutable getViewport() {
        return viewportPU;
    }

    /**
     * Returns the {@link RectangleImmutable rectangular} portion
     * of the <b>rotated</b> virtual {@link Screen} size in window units
     * represented by this monitor, i.e. top-left origin and size.
     * @see Screen
     */
    public final RectangleImmutable getViewportInWindowUnits() {
        return viewportWU;
    }

    /**
     * Returns <code>true</code> if given screen coordinates in pixel units
     * are contained by this {@link #getViewport() viewport}, otherwise <code>false</code>.
     * @param x x-coord in pixel units
     * @param y y-coord in pixel units
     */
    public final boolean contains(final int x, final int y) {
        return x >= viewportPU.getX() &&
               x <  viewportPU.getX() + viewportPU.getWidth() &&
               y >= viewportPU.getY() &&
               y <  viewportPU.getY() + viewportPU.getHeight() ;
    }

    /**
     * Calculates the union of the given monitor's {@link #getViewport() viewport} in pixel- and window units.
     * @param viewport storage for result in pixel units, maybe null
     * @param viewportInWindowUnits storage for result in window units, maybe null
     * @param monitors given list of monitors
     */
    public static void unionOfViewports(final Rectangle viewport, final Rectangle viewportInWindowUnits, final List<MonitorDevice> monitors) {
        int x1PU=Integer.MAX_VALUE, y1PU=Integer.MAX_VALUE;
        int x2PU=Integer.MIN_VALUE, y2PU=Integer.MIN_VALUE;
        int x1WU=Integer.MAX_VALUE, y1WU=Integer.MAX_VALUE;
        int x2WU=Integer.MIN_VALUE, y2WU=Integer.MIN_VALUE;
        for(int i=monitors.size()-1; i>=0; i--) {
            if( null != viewport ) {
                final RectangleImmutable viewPU = monitors.get(i).getViewport();
                x1PU = Math.min(x1PU, viewPU.getX());
                x2PU = Math.max(x2PU, viewPU.getX() + viewPU.getWidth());
                y1PU = Math.min(y1PU, viewPU.getY());
                y2PU = Math.max(y2PU, viewPU.getY() + viewPU.getHeight());
            }
            if( null != viewportInWindowUnits ) {
                final RectangleImmutable viewWU = monitors.get(i).getViewportInWindowUnits();
                x1WU = Math.min(x1WU, viewWU.getX());
                x2WU = Math.max(x2WU, viewWU.getX() + viewWU.getWidth());
                y1WU = Math.min(y1WU, viewWU.getY());
                y2WU = Math.max(y2WU, viewWU.getY() + viewWU.getHeight());
            }
        }
        if( null != viewport ) {
            viewport.set(x1PU, y1PU, x2PU - x1PU, y2PU - y1PU);
        }
        if( null != viewportInWindowUnits ) {
            viewportInWindowUnits.set(x1WU, y1WU, x2WU - x1WU, y2WU - y1WU);
        }
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
     * @see #queryCurrentMode()
     */
    public final MonitorMode getCurrentMode() {
        return currentMode;
    }

    /**
     * Returns the current {@link MonitorMode} resulting from a native query.
     * <p>
     * The returned {@link MonitorMode} is element of the lists {@link #getSupportedModes()} and {@link Screen#getMonitorModes()}.
     * </p>
     * @see #getCurrentMode()
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
        return "Monitor[Id "+Display.toHexString(nativeId)+", "+sizeMM+" mm, viewport "+viewportPU+ " [pixels], "+viewportWU+" [window], orig "+originalMode+", curr "+currentMode+
               ", modeChanged "+modeChanged+", modeCount "+supportedModes.size()+"]";
    }
}

