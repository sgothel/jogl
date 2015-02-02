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

import java.util.Comparator;

import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.nativewindow.util.SurfaceSize;

import com.jogamp.newt.util.MonitorModeUtil;


/**
 * Immutable MonitorMode Class, consisting of it's read only components:<br>
 * <ul>
 *  <li>nativeId</li>
 *  <li>{@link SizeAndRRate}, consist out of non rotated {@link #getSurfaceSize() surface size}, {@link #getRefreshRate() refresh rate} and {@link #getFlags() flags}.</li>
 *  <li><code>rotation</code>, measured counter clockwise (CCW)</li>
 * </ul>
 *
 * <i>Aquire and filter MonitorMode</i><br>
 * <ul>
 *  <li>{@link MonitorDevice} Selection:
 *  <ul>
 *    <li>A List of all {@link MonitorDevice}s is accessible via {@link Screen#getMonitorDevices()}.</li>
 *    <li>The main monitor used by a windows is accessible via {@link Window#getMainMonitor()}.</li>
 *    <li>The main monitor covering an arbitrary rectangle is accessible via {@link Screen#getMainMonitor(RectangleImmutable)}.</li>
 *  </ul></li>
 *  <li>The current MonitorMode can be obtained via {@link MonitorDevice#getCurrentMode()}.</li>
 *  <li>The original MonitorMode can be obtained via {@link MonitorDevice#getOriginalMode()}.</li>
 *  <li>{@link MonitorMode} Filtering:
 *  <ul>
 *    <li>A {@link MonitorDevice}'s MonitorModes is accessible via {@link MonitorDevice#getSupportedModes()}.</li>
 *    <li>You may utilize {@link MonitorModeUtil} to filter and select a desired MonitorMode.</li>
 *  </ul></li>
 * </ul>
 * <br>
 *
 * <i>Changing MonitorMode</i><br>
 * <ul>
 *  <li> Use {@link MonitorDevice#setCurrentMode(MonitorMode)}
 *       to change the current MonitorMode for all {@link Screen}s referenced via the {@link Screen#getFQName() full qualified name (FQN)}.</li>
 *  <li> The {@link MonitorDevice#getOriginalMode() original mode} is restored when
 *  <ul>
 *    <li>the last FQN referenced Screen closes.</li>
 *    <li>the JVM shuts down.</li>
 *  </ul></li>
 * </ul>
 * <br>
 * Example for changing the MonitorMode:
 * <pre>
        // Pick the monitor:
        // Either the one used by a window ..
        MonitorDevice monitor = window.getMainMonitor();

        // Or arbitrary from the list ..
        List<MonitorDevice> allMonitor = getMonitorDevices();
        MonitorDevice monitor = allMonitor.get(0);

        // Current and original modes ..
        MonitorMode mmCurrent = monitor.queryCurrentMode();
        MonitorMode mmOrig = monitor.getOriginalMode();

        // Target resolution in pixel units
        DimensionImmutable res = new Dimension(800, 600);

        // Target refresh rate shall be similar to current one ..
        float freq = mmCurrent.getRefreshRate();

        // Target rotation shall be similar to current one
        int rot = mmCurrent.getRotation();

        // Filter criterias sequential out of all available MonitorMode of the chosen MonitorDevice
        List<MonitorMode> monitorModes = monitor.getSupportedModes();
        monitorModes = MonitorModeUtil.filterByFlags(monitorModes, 0); // no interlace, double-scan etc
        monitorModes = MonitorModeUtil.filterByRotation(monitorModes, rot);
        monitorModes = MonitorModeUtil.filterByResolution(monitorModes, res);
        monitorModes = MonitorModeUtil.filterByRate(monitorModes, freq);
        monitorModes = MonitorModeUtil.getHighestAvailableBpp(monitorModes);

        // pick 1st one and set to current ..
        MonitorMode mm = monitorModes.get(0);
        monitor.setCurrentMode(mm);
 * </pre>
 */
public class MonitorMode implements Comparable<MonitorMode> {

    /** Comparator for 2 {@link MonitorMode}s, following comparison order as described in {@link MonitorMode#compareTo(MonitorMode)}, returning the ascending order. */
    public static final Comparator<MonitorMode> monitorModeComparator = new Comparator<MonitorMode>() {
        @Override
        public int compare(final MonitorMode mm1, final MonitorMode mm2) {
            return mm1.compareTo(mm2);
        } };

    /** Comparator for 2 {@link MonitorMode}s, following comparison order as described in {@link MonitorMode#compareTo(MonitorMode)}, returning the descending order. */
    public static final Comparator<MonitorMode> monitorModeComparatorInv = new Comparator<MonitorMode>() {
        @Override
        public int compare(final MonitorMode mm1, final MonitorMode mm2) {
            return mm2.compareTo(mm1);
        } };

    /**
     * Immutable <i>surfaceSize, flags and refreshRate</i> Class, consisting of it's read only components:<br>
     * <ul>
     *  <li>nativeId</li>
     *  <li>{@link SurfaceSize} surface memory size</li>
     *  <li><code>flags</code></li>
     *  <li><code>refresh rate</code></li>
     * </ul>
     */
    public static class SizeAndRRate implements Comparable<SizeAndRRate> {
        /** Non rotated surface size in pixel units */
        public final SurfaceSize surfaceSize;
        /** Mode bitfield flags, i.e. {@link #FLAG_DOUBLESCAN}, {@link #FLAG_INTERLACE}, .. */
        public final int flags;
        /** Vertical refresh rate */
        public final float refreshRate;
        public final int hashCode;

        public SizeAndRRate(final SurfaceSize surfaceSize, final float refreshRate, final int flags) {
            if(null==surfaceSize) {
                throw new IllegalArgumentException("surfaceSize must be set ("+surfaceSize+")");
            }
            this.surfaceSize=surfaceSize;
            this.flags = flags;
            this.refreshRate=refreshRate;
            this.hashCode = getHashCode();
        }

        private final static String STR_INTERLACE = "Interlace";
        private final static String STR_DOUBLESCAN = "DoubleScan";
        private final static String STR_SEP = ", ";

        public static final StringBuilder flags2String(final int flags) {
            final StringBuilder sb = new StringBuilder();
            boolean sp = false;
            if( 0 != ( flags & FLAG_INTERLACE ) ) {
                sb.append(STR_INTERLACE);
                sp = true;
            }
            if( 0 != ( flags & FLAG_DOUBLESCAN ) ) {
                if( sp ) {
                    sb.append(STR_SEP);
                }
                sb.append(STR_DOUBLESCAN);
                sp = true;
            }
            return sb;
        }
        @Override
        public final String toString() {
            return surfaceSize+" @ "+refreshRate+" Hz, flags ["+flags2String(flags).toString()+"]";
        }

        /**
         * <p>
         * Compares {@link SurfaceSize#compareTo(SurfaceSize) surfaceSize} 1st, then {@link #flags}, then {@link #refreshRate}.
         * </p>
         * <p>
         * Flags are compared as follows:
         * <pre>
         *   NONE > DOUBLESCAN > INTERLACE
         * </pre>
         * </p>
         * <p>
         * Refresh rate differences of &lt; 0.01 are considered equal (epsilon).
         * </p>
         * {@inheritDoc}
         */
        @Override
        public int compareTo(final SizeAndRRate sszr) {
            final int rssz = surfaceSize.compareTo(sszr.surfaceSize);
            if( 0 != rssz ) {
                return rssz;
            }
            final int tflags = 0 == flags ? Integer.MAX_VALUE : flags; // normalize NONE
            final int xflags = 0 == sszr.flags ? Integer.MAX_VALUE : sszr.flags; // normalize NONE
            if( tflags == xflags ) {
                final float refreshEpsilon = 0.01f; // reasonable sorting granularity of refresh rate
                final float drate = refreshRate - sszr.refreshRate;
                if( Math.abs(drate) < refreshEpsilon ) {
                    return 0;
                } else if( drate > refreshEpsilon ) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                if(tflags > xflags) {
                    return 1;
                } else if(tflags < xflags) {
                    return -1;
                }
                return 0;
            }
        }

        /**
         * Tests equality of two {@link SizeAndRRate} objects
         * by evaluating equality of it's components:<br/>
         * <ul>
         *  <li><code>surfaceSize</code></li>
         *  <li><code>refreshRate</code></li>
         *  <li><code>flags</code></li>
         * </ul>
         */
        @Override
        public final boolean equals(final Object obj) {
            if (this == obj) { return true; }
            if (obj instanceof SizeAndRRate) {
                final SizeAndRRate p = (SizeAndRRate)obj;
                return surfaceSize.equals(p.surfaceSize) &&
                       flags == p.flags &&
                       refreshRate == p.refreshRate ;
            }
            return false;
        }

        /**
         * Returns a combined hash code of it's elements:<br/>
         * <ul>
         *  <li><code>surfaceSize</code></li>
         *  <li><code>flags</code></li>
         *  <li><code>refreshRate</code></li>
         * </ul>
         */
        @Override
        public final int hashCode() {
            return hashCode;
        }
        private final int getHashCode() {
            // 31 * x == (x << 5) - x
            int hash = 31 + surfaceSize.hashCode();
            hash = ((hash << 5) - hash) + flags;
            hash = ((hash << 5) - hash) + (int)(refreshRate*100.0f);
            return hash;
        }
    }

    /** zero rotation, compared to normal settings */
    public static final int ROTATE_0   = 0;

    /**  90 degrees CCW rotation */
    public static final int ROTATE_90  = 90;

    /** 180 degrees CCW rotation */
    public static final int ROTATE_180 = 180;

    /** 270 degrees CCW rotation */
    public static final int ROTATE_270 = 270;

    /** Frame is split into two fields. See {@link #getFlags()}. */
    public static final int FLAG_INTERLACE = 1 << 0;

    /** Lines are doubled. See {@link #getFlags()}. */
    public static final int FLAG_DOUBLESCAN = 1 << 1;

    /** The immutable native Id of this instance, which may not be unique. */
    private final int nativeId;
    private final SizeAndRRate sizeAndRRate;
    private final int rotation;
    private final int hashCode;

    public static boolean isRotationValid(final int rotation) {
        return rotation == MonitorMode.ROTATE_0 || rotation == MonitorMode.ROTATE_90 ||
               rotation == MonitorMode.ROTATE_180 || rotation == MonitorMode.ROTATE_270 ;
    }

    /**
     * @param sizeAndRRate the surface size and refresh rate mode
     * @param rotation the screen rotation, measured counter clockwise (CCW)
     */
    public MonitorMode(final int nativeId, final SizeAndRRate sizeAndRRate, final int rotation) {
        if ( !isRotationValid(rotation) ) {
            throw new RuntimeException("invalid rotation: "+rotation);
        }
        this.nativeId = nativeId;
        this.sizeAndRRate = sizeAndRRate;
        this.rotation = rotation;
        this.hashCode = getHashCode();
    }

    /**
     * Creates a user instance w/o {@link #getId() identity} to filter our matching modes w/ identity.
     * <p>
     * See {@link com.jogamp.newt.util.MonitorModeUtil} for filter utilities.
     * </p>
     * @param surfaceSize
     * @param refreshRate
     * @param flags
     * @param rotation
     */
    public MonitorMode(final SurfaceSize surfaceSize, final float refreshRate, final int flags, final int rotation) {
        this(0, new SizeAndRRate(surfaceSize, refreshRate, flags), rotation);
    }

    /** @return the immutable native Id of this mode, may not be unique, may be 0. */
    public final int getId() { return nativeId; }

    /** Returns the <i>surfaceSize and refreshRate</i> instance. */
    public final SizeAndRRate getSizeAndRRate() {
        return sizeAndRRate;
    }

    /** Returns the unrotated {@link SurfaceSize} */
    public final SurfaceSize getSurfaceSize() {
        return sizeAndRRate.surfaceSize;
    }

    /** Returns the vertical refresh rate. */
    public final float getRefreshRate() {
        return sizeAndRRate.refreshRate;
    }

    /** Returns bitfield w/ flags, i.e. {@link #FLAG_DOUBLESCAN}, {@link #FLAG_INTERLACE}, .. */
    public final int getFlags() {
        return sizeAndRRate.flags;
    }

    /** Returns the CCW rotation of this mode */
    public final int getRotation() {
        return rotation;
    }

    /** Returns the rotated screen width in pixel units,
     *  derived from <code>getMonitorMode().getSurfaceSize().getResolution()</code>
     *  and <code>getRotation()</code>
     */
    public final int getRotatedWidth() {
        return getRotatedWH(true);
    }

    /** Returns the rotated screen height in pixel units,
     *  derived from <code>getMonitorMode().getSurfaceSize().getResolution()</code>
     *  and <code>getRotation()</code>
     */
    public final int getRotatedHeight() {
        return getRotatedWH(false);
    }

    @Override
    public final String toString() {
        return "[Id "+Display.toHexString(nativeId)+", " +  sizeAndRRate + ", " + rotation + " degr]";
    }

    /**
     * <p>
     * Compares {@link SizeAndRRate#compareTo(SizeAndRRate) sizeAndRRate} 1st, then {@link #rotation}.
     * </p>
     * <p>
     * Rotation is compared inverted, i.e. <code>360 - rotation</code>,
     * so the lowest rotation reflects a higher value.
     * </p>
     * <p>
     * Order of comparing MonitorMode:
     * <ul>
     *   <li>resolution</li>
     *   <li>bits per pixel</li>
     *   <li>flags</li>
     *   <li>refresh rate</li>
     *   <li>rotation</li>
     * </ul>
     * </p>
     * {@inheritDoc}
     */
    @Override
    public int compareTo(final MonitorMode mm) {
        final int c = sizeAndRRate.compareTo(mm.sizeAndRRate);
        if( 0 != c ) {
            return c;
        }
        final int trot = 360 - rotation; // normalize rotation
        final int xrot = 360 - mm.rotation; // normalize rotation
        if(trot > xrot) {
            return 1;
        } else if(trot < xrot) {
            return -1;
        }
        return 0;
    }

    /**
     * Tests equality of two {@link MonitorMode} objects
     * by evaluating equality of it's components:<br/>
     * <ul>
     *  <li><code>nativeId</code></li>
     *  <li><code>sizeAndRRate</code></li>
     *  <li><code>rotation</code></li>
     * </ul>
     */
    @Override
    public final boolean equals(final Object obj) {
        if (this == obj) { return true; }
        if (obj instanceof MonitorMode) {
            final MonitorMode sm = (MonitorMode)obj;
            return sm.nativeId == this.nativeId &&
                   sm.sizeAndRRate.equals(sizeAndRRate) &&
                   sm.rotation == this.rotation ;
        }
        return false;
    }

    /**
     * Returns a combined hash code of it's elements:<br/>
     * <ul>
     *  <li><code>nativeId</code></li>
     *  <li><code>sizeAndRRate</code></li>
     *  <li><code>rotation</code></li>
     * </ul>
     */
    @Override
    public final int hashCode() {
        return hashCode;
    }
    private final int getHashCode() {
        // 31 * x == (x << 5) - x
        int hash = 31 + getId();
        hash = ((hash << 5) - hash) + sizeAndRRate.hashCode();
        hash = ((hash << 5) - hash) + getRotation();
        return hash;
    }

    private final int getRotatedWH(final boolean width) {
        final DimensionImmutable d = sizeAndRRate.surfaceSize.getResolution();
        final boolean swap = MonitorMode.ROTATE_90 == rotation || MonitorMode.ROTATE_270 == rotation ;
        if ( (  width &&  swap ) || ( !width && !swap ) ) {
            return d.getHeight();
        }
        return d.getWidth();
    }
}
