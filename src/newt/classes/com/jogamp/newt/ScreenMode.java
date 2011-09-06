/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

import com.jogamp.newt.util.MonitorMode;

/** Immutable ScreenMode Class, consisting of it's read only components:<br>
 * <ul>
 *  <li>{@link com.jogamp.newt.util.MonitorMode}, non rotated values</li>
 *  <li><code>rotation</code>, measured counter clockwise (CCW)</li>
 * </ul>
 *
 * <i>Aquire and filter ScreenModes</i><br>
 * <ul>
 *  <li>A List of read only ScreenMode's is being returned by {@link com.jogamp.newt.Screen#getScreenModes()}.</li>
 *  <li>You may utilize {@link com.jogamp.newt.util.ScreenModeUtil} to filter and select a desired ScreenMode.</li>
 *  <li>The current ScreenMode can be obtained via {@link com.jogamp.newt.Screen#getCurrentScreenMode()}.</li>
 *  <li>The initial original ScreenMode (at startup) can be obtained via {@link com.jogamp.newt.Screen#getOriginalScreenMode()}.</li>
 * </ul>
 * <br>
 *
 * <i>Changing ScreenModes</i><br>
 * <ul>
 *  <li> Use {@link com.jogamp.newt.Screen#setCurrentScreenMode(com.jogamp.newt.ScreenMode)}</li>
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
        int freq = orig.getMonitorMode().getRefreshRate();

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
public class ScreenMode {
    /** zero rotation, compared to normal settings */
    public static final int ROTATE_0   = 0;

    /**  90 degrees CCW rotation */
    public static final int ROTATE_90  = 90;

    /** 180 degrees CCW rotation */
    public static final int ROTATE_180 = 180;

    /** 270 degrees CCW rotation */
    public static final int ROTATE_270 = 270;

    MonitorMode monitorMode;
    int rotation;

    public static boolean isRotationValid(int rotation) {
        return rotation == ScreenMode.ROTATE_0 || rotation == ScreenMode.ROTATE_90 ||
               rotation == ScreenMode.ROTATE_180 || rotation == ScreenMode.ROTATE_270 ;
    }

    /**
     * @param monitorMode the monitor mode
     * @param rotation the screen rotation, measured counter clockwise (CCW)
     */
    public ScreenMode(MonitorMode monitorMode, int rotation) {
        if ( !isRotationValid(rotation) ) {
            throw new RuntimeException("invalid rotation: "+rotation);
        }
        this.monitorMode = monitorMode;
        this.rotation = rotation;
    }

    /** Returns the unrotated <code>MonitorMode</code> */
    public final MonitorMode getMonitorMode() {
        return monitorMode;
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
        return "[ " +  getMonitorMode() + ", " + rotation + " degr ]";
    }

    /**
     * Tests equality of two <code>ScreenMode</code> objects 
     * by evaluating equality of it's components:<br>
     * <ul>
     *  <li><code>monitorMode</code></li>
     *  <li><code>rotation</code></li>
     * </ul>
     * <br>
     */
    public final boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj instanceof ScreenMode) {
            ScreenMode sm = (ScreenMode)obj;
            return sm.getMonitorMode().equals(getMonitorMode()) &&
                   sm.getRotation() == this.getRotation() ;
        }
        return false;
    }

    /**
     * Returns a combined hash code of it's elements:<br>
     * <ul>
     *  <li><code>monitorMode</code></li>
     *  <li><code>rotation</code></li>
     * </ul>
     */
    public final int hashCode() {
        // 31 * x == (x << 5) - x
        int hash = 31 + getMonitorMode().hashCode();
        hash = ((hash << 5) - hash) + getRotation();
        return hash;
    }
    
    private final int getRotatedWH(boolean width) {
        final DimensionImmutable d = getMonitorMode().getSurfaceSize().getResolution();
        final boolean swap = ScreenMode.ROTATE_90 == rotation || ScreenMode.ROTATE_270 == rotation ;
        if ( (  width &&  swap ) || ( !width && !swap ) ) {
            return d.getHeight();
        }
        return d.getWidth();
    }        
}
