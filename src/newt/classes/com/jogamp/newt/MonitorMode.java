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

import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.SurfaceSize;


/** Immutable MonitorMode Class, consisting of it's read only components:<br>
 * <ul>
 *  <li>nativeId</li>
 *  <li>{@link SizeAndRRate}, non rotated surfaceSize and refreshRate</li>
 *  <li><code>rotation</code>, measured counter clockwise (CCW)</li>
 * </ul>
 *
 * <i>Aquire and filter MonitorMode</i><br>
 * <ul>
 *  <li>A List of read only MonitorModes is being returned by {@link com.jogamp.newt.Screen#getMonitorModes()}.</li>
 *  <li>You may utilize {@link com.jogamp.newt.util.MonitorModeUtil} to filter and select a desired ScreenMode.</li>
 *  <li>The current ScreenMode can be obtained via {@link com.jogamp.newt.Screen#getCurrentScreenMode()}.</li>
 *  <li>The initial original ScreenMode (at startup) can be obtained via {@link com.jogamp.newt.Screen#getOriginalScreenMode()}.</li>
 * </ul>
 * <br>
 *
 * <i>Changing ScreenModes</i><br>
 * FIXME!!!!!
 * <ul>
 *  <li> Use {@link com.jogamp.newt.Screen#setCurrentScreenMode(com.jogamp.newt.MonitorMode)}</li>
 *       to change the current ScreenMode of all Screen's referenced via the full qualified name (FQN)
 *       {@link com.jogamp.newt.Screen#getFQName()}.</li>
 *  <li> When the last FQN referenced Screen closes, the original ScreenMode ({@link com.jogamp.newt.Screen#getOriginalScreenMode()})
 * is restored.</li>
 * </ul>
 * <br>
 * Example for changing the ScreenMode:
 * <pre>
        // determine target refresh rate
        ScreenMode orig = screen.getOriginalScreenMode();
        int freq = orig.getOutputMode().getRefreshRate();

        // target resolution
        Dimension res = new Dimension(800, 600);

        // target rotation
        int rot = 0;

        // filter available ScreenModes
        List screenModes = screen.getScreenModes();
        screenModes = ScreenModeUtil.filterByRate(screenModes, freq); // get the nearest ones
        screenModes = ScreenModeUtil.filterByRotation(screenModes, rot);
        screenModes = ScreenModeUtil.filterByResolution(screenModes, res); // get the nearest ones
        screenModes = ScreenModeUtil.getHighestAvailableBpp(screenModes);

        // pick 1st one ..
        screen.setCurrentScreenMode((ScreenMode) screenModes.get(0)); 
 * </pre>
 *
 * X11 / AMD just works<br>
 * <br>
 * X11 / NVidia difficulties
 * <pre>
    NVidia RANDR RefreshRate Bug
        If NVidia's 'DynamicTwinView' is enabled, all refresh rates are
        unique, ie consequent numbers starting with the default refresh, ie 50, 51, ..
        The only way to workaround it is to disable 'DynamicTwinView'.
        Read: http://us.download.nvidia.com/XFree86/Linux-x86/260.19.12/README/configtwinview.html

        Check to see if 'DynamicTwinView' is enable:
            nvidia-settings -q :0/DynamicTwinview

        To disable it (workaround), add the following option to your xorg.conf device section:
            Option "DynamicTwinView" "False"

    NVidia RANDR Rotation:
        To enable it, add the following option to your xorg.conf device section:
            Option "RandRRotation" "on"
 * </pre>
 *
 */
public class MonitorMode {
    /** 
     * Immutable <i>surfaceSize and refreshRate</i> Class, consisting of it's read only components:<br>
     * <ul>
     *  <li>nativeId</li>
     *  <li>{@link SurfaceSize} surface memory size</li>
     *  <li><code>refresh rate</code></li>
     * </ul>
     */
    public static class SizeAndRRate {
        public final SurfaceSize surfaceSize;
        public final float refreshRate;
        public final int flags;
        public final int hashCode;
    
        public SizeAndRRate(SurfaceSize surfaceSize, float refreshRate, int flags) {
            if(null==surfaceSize) {
                throw new IllegalArgumentException("surfaceSize must be set ("+surfaceSize+")");
            }
            this.surfaceSize=surfaceSize;
            this.refreshRate=refreshRate;
            this.flags = flags;
            this.hashCode = getHashCode();
        }
    
        private final static String STR_INTERLACE = "Interlace";
        private final static String STR_DOUBLESCAN = "DoubleScan";
        private final static String STR_SEP = ", ";
        
        public static final StringBuffer flags2String(int flags) {
            final StringBuffer sb = new StringBuffer();
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
        public final String toString() {
            return new String(surfaceSize+" @ "+refreshRate+" Hz, flags ["+flags2String(flags).toString()+"]");
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
        public final boolean equals(Object obj) {
            if (this == obj) { return true; }
            if (obj instanceof SizeAndRRate) {
                final SizeAndRRate p = (SizeAndRRate)obj;
                return surfaceSize.equals(p.surfaceSize) &&
                       refreshRate == p.refreshRate &&
                       flags == p.flags ;
            }
            return false;
        }
    
        /**
         * Returns a combined hash code of it's elements:<br/>
         * <ul>
         *  <li><code>surfaceSize</code></li>
         *  <li><code>refreshRate</code></li>
         *  <li><code>flags</code></li>
         * </ul>
         */
        public final int hashCode() {
            return hashCode;
        }        
        private final int getHashCode() {
            // 31 * x == (x << 5) - x
            int hash = 31 + surfaceSize.hashCode();
            hash = ((hash << 5) - hash) + (int)(refreshRate*100.0f);
            hash = ((hash << 5) - hash) + flags;
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

    public static boolean isRotationValid(int rotation) {
        return rotation == MonitorMode.ROTATE_0 || rotation == MonitorMode.ROTATE_90 ||
               rotation == MonitorMode.ROTATE_180 || rotation == MonitorMode.ROTATE_270 ;
    }

    /**
     * @param sizeAndRRate the surface size and refresh rate mode
     * @param rotation the screen rotation, measured counter clockwise (CCW)
     */
    public MonitorMode(int nativeId, SizeAndRRate sizeAndRRate, int rotation) {
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
    public MonitorMode(SurfaceSize surfaceSize, float refreshRate, int flags, int rotation) {
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
    
    /** Returns the rotated screen width, 
     *  derived from <code>getMonitorMode().getSurfaceSize().getResolution()</code>
     *  and <code>getRotation()</code> 
     */
    public final int getRotatedWidth() {
        return getRotatedWH(true);
    }
    
    /** Returns the rotated screen height, 
     *  derived from <code>getMonitorMode().getSurfaceSize().getResolution()</code>
     *  and <code>getRotation()</code> 
     */
    public final int getRotatedHeight() {
        return getRotatedWH(false);
    }

    public final String toString() {
        return "[Id "+Display.toHexString(nativeId)+", " +  sizeAndRRate + ", " + rotation + " degr]";
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
    public final boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj instanceof MonitorMode) {
            MonitorMode sm = (MonitorMode)obj;
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
    
    private final int getRotatedWH(boolean width) {
        final DimensionImmutable d = sizeAndRRate.surfaceSize.getResolution();
        final boolean swap = MonitorMode.ROTATE_90 == rotation || MonitorMode.ROTATE_270 == rotation ;
        if ( (  width &&  swap ) || ( !width && !swap ) ) {
            return d.getHeight();
        }
        return d.getWidth();
    }    
}
