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

package jogamp.newt;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.Screen;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.SurfaceSize;
import com.jogamp.opengl.math.FloatUtil;

import jogamp.newt.MonitorDeviceImpl;
import jogamp.newt.ScreenImpl;

/**
 * Encodes and decodes {@link MonitorMode} and {@link MonitorDevice} properties.
 */
public class MonitorModeProps {
    /**
     * {@value} Elements: width, height
     * <p>
     * WARNING: must be synchronized with ScreenMode.h, native implementation
     * </p>
     */
    public static final int NUM_RESOLUTION_PROPERTIES   = 2;

    /**
     * {@value} Element: bpp
     * <p>
     * WARNING: must be synchronized with ScreenMode.h, native implementation
     * </p>
     */
    public static final int NUM_SURFACE_SIZE_PROPERTIES = 1;

    /**
     * {@value} Elements: refresh-rate (Hz*100), flags
     * <p>
     * WARNING: must be synchronized with ScreenMode.h, native implementation
     * </p>
     */
    public static final int NUM_SIZEANDRATE_PROPERTIES = 2;

    /**
     * {@value} Elements: id, rotation
     * <p>
     * WARNING: must be synchronized with ScreenMode.h, native implementation
     * </p>
     */
    public static final int NUM_MONITOR_MODE_PROPERTIES  = 2;

    /**
     * {@value} Elements:
     * <ul>
     *  <li>count</li>
     *  <li>{@link #NUM_RESOLUTION_PROPERTIES}</li>
     *  <li>{@link #NUM_SURFACE_SIZE_PROPERTIES}</li>
     *  <li>{@link #NUM_SIZEANDRATE_PROPERTIES}</li>
     *  <li>{@link #NUM_MONITOR_MODE_PROPERTIES}</li>
     *  <li>mode-id</li>
     * </ul>
     * <p>
     * WARNING: must be synchronized with ScreenMode.h, native implementation
     * </p>
     */
    public static final int NUM_MONITOR_MODE_PROPERTIES_ALL = 8;

    /**
     * {@value} Elements: count + {@link #NUM_RESOLUTION_PROPERTIES}
     * <p>
     * WARNING: must be synchronized with ScreenMode.h, native implementation
     * </p>
     */
    public static final int IDX_MONITOR_MODE_BPP =   1 // count
                                                   + MonitorModeProps.NUM_RESOLUTION_PROPERTIES
                                                   ;
    /**
     * {@value} Elements:
     * <ul>
     *  <li>count</li>
     *  <li>{@link #NUM_RESOLUTION_PROPERTIES}</li>
     *  <li>{@link #NUM_SURFACE_SIZE_PROPERTIES}</li>
     *  <li>{@link #NUM_SIZEANDRATE_PROPERTIES}</li>
     *  <li>mode-id</li>
     * </ul>
     * <p>
     * WARNING: must be synchronized with ScreenMode.h, native implementation
     * </p>
     */
    public static final int IDX_MONITOR_MODE_ROT =   1 // count
                                                   + MonitorModeProps.NUM_RESOLUTION_PROPERTIES
                                                   + MonitorModeProps.NUM_SURFACE_SIZE_PROPERTIES
                                                   + MonitorModeProps.NUM_SIZEANDRATE_PROPERTIES
                                                   + 1 // id of MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES
                                                   ;

    /**
     * {@value} Elements:
     * <ul>
     *   <li>count</li>
     *   <li>id</li>
     *   <li>IsClone</li>
     *   <li>IsPrimary</li>
     *   <li>ScreenSizeMM[width, height] (2 elements)</li>
     *   <li>Rotated Viewport pixel-units (4 elements)</li>
     *   <li>Rotated Viewport window-units  (4 elements)</li>
     *   <li>CurrentMonitorModeId</li>
     *   <li>Rotation</li>
     *   <li>SupportedModeId+</li>
     * </ul>
     * <p>
     * with Viewport := [x, y, width, height] (4 elements)
     * </p>
     * <p>
     * WARNING: must be synchronized with ScreenMode.h, native implementation
     * </p>
     */
    public static final int MIN_MONITOR_DEVICE_PROPERTIES = 17;

    public static final int IDX_MONITOR_DEVICE_VIEWPORT =   1 // count
                                                          + 1 // native mode
                                                          + 1 // isClone
                                                          + 1 // isPrimary
                                                          + MonitorModeProps.NUM_RESOLUTION_PROPERTIES // sizeMM
                                                          ;

    public static class Cache {
        public final ArrayHashSet<DimensionImmutable>       resolutions  = new ArrayHashSet<DimensionImmutable>(false, ArrayHashSet.DEFAULT_INITIAL_CAPACITY, ArrayHashSet.DEFAULT_LOAD_FACTOR);
        public final ArrayHashSet<SurfaceSize>              surfaceSizes = new ArrayHashSet<SurfaceSize>(false, ArrayHashSet.DEFAULT_INITIAL_CAPACITY, ArrayHashSet.DEFAULT_LOAD_FACTOR);
        public final ArrayHashSet<MonitorMode.SizeAndRRate> sizeAndRates = new ArrayHashSet<MonitorMode.SizeAndRRate>(false, ArrayHashSet.DEFAULT_INITIAL_CAPACITY, ArrayHashSet.DEFAULT_LOAD_FACTOR);
        public final ArrayHashSet<MonitorMode>              monitorModes = new ArrayHashSet<MonitorMode>(false, ArrayHashSet.DEFAULT_INITIAL_CAPACITY, ArrayHashSet.DEFAULT_LOAD_FACTOR);
        public final ArrayHashSet<MonitorDevice>            monitorDevices = new ArrayHashSet<MonitorDevice>(false, ArrayHashSet.DEFAULT_INITIAL_CAPACITY, ArrayHashSet.DEFAULT_LOAD_FACTOR);

        public final void setPrimary(final MonitorDevice p) { primary = p; }
        public final MonitorDevice getPrimary() { return primary;}
        private MonitorDevice primary = null;
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    private static DimensionImmutable streamInResolution(final int[] resolutionProperties, int offset) {
        final Dimension resolution = new Dimension(resolutionProperties[offset++], resolutionProperties[offset++]);
        return resolution;
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    private static SurfaceSize streamInSurfaceSize(final DimensionImmutable resolution, final int[] sizeProperties, final int offset) {
        return new SurfaceSize(resolution, sizeProperties[offset]);
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    private static MonitorMode.SizeAndRRate streamInSizeAndRRate(final SurfaceSize surfaceSize, final int[] sizeAndRRateProperties, int offset) {
        final float refreshRate = sizeAndRRateProperties[offset++]/100.0f;
        final int flags = sizeAndRRateProperties[offset++];
        return new MonitorMode.SizeAndRRate(surfaceSize, refreshRate, flags);
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    private static MonitorMode streamInMonitorMode0(final MonitorMode.SizeAndRRate sizeAndRate, final int[] modeProperties, int offset) {
        final int id = modeProperties[offset++];
        final int rotation = modeProperties[offset++];
        return new MonitorMode(id, sizeAndRate, rotation);
    }

    /**
     *  WARNING: must be synchronized with ScreenMode.h, native implementation
     *
     * @param mode_idx if not null and cache is given, returns the index of resulting {@link MonitorMode} within {@link Cache#monitorModes}.
     * @param cache optional hash arrays of unique {@link MonitorMode} components and {@link MonitorDevice}s, allowing to avoid duplicates
     * @param modeProperties the input data
     * @param offset the offset to the input data
     * @return {@link MonitorMode} of the identical (old or new) element in {@link Cache#monitorModes},
     *         matching the input <code>modeProperties</code>, or null if input could not be processed.
     */
    public static MonitorMode streamInMonitorMode(final int[] mode_idx, final Cache cache,
                                                  final int[] modeProperties, int offset) {
        final int count = modeProperties[offset];
        if(NUM_MONITOR_MODE_PROPERTIES_ALL != count) {
            throw new RuntimeException("property count should be "+NUM_MONITOR_MODE_PROPERTIES_ALL+", but is "+count+", len "+(modeProperties.length-offset));
        }
        if(NUM_MONITOR_MODE_PROPERTIES_ALL > modeProperties.length-offset) {
            throw new RuntimeException("properties array too short, should be >= "+NUM_MONITOR_MODE_PROPERTIES_ALL+", is "+(modeProperties.length-offset));
        }
        offset++;
        DimensionImmutable resolution = MonitorModeProps.streamInResolution(modeProperties, offset);
        offset += MonitorModeProps.NUM_RESOLUTION_PROPERTIES;
        if(null!=cache) {
            resolution = cache.resolutions.getOrAdd(resolution);
        }

        SurfaceSize surfaceSize = MonitorModeProps.streamInSurfaceSize(resolution, modeProperties, offset);
        offset += MonitorModeProps.NUM_SURFACE_SIZE_PROPERTIES;
        if(null!=cache) {
            surfaceSize = cache.surfaceSizes.getOrAdd(surfaceSize);
        }

        MonitorMode.SizeAndRRate sizeAndRate = MonitorModeProps.streamInSizeAndRRate(surfaceSize, modeProperties, offset);
        offset += MonitorModeProps.NUM_SIZEANDRATE_PROPERTIES;
        if(null!=cache) {
            sizeAndRate = cache.sizeAndRates.getOrAdd(sizeAndRate);
        }

        MonitorMode monitorMode = MonitorModeProps.streamInMonitorMode0(sizeAndRate, modeProperties, offset);
        if(null!=cache) {
            monitorMode = cache.monitorModes.getOrAdd(monitorMode);
        }
        if( null != mode_idx && null!=cache) {
            final int _modeIdx  = cache.monitorModes.indexOf(monitorMode);
            if( 0 > _modeIdx ) {
                throw new InternalError("Invalid index of current unified mode "+monitorMode);
            }
            mode_idx[0] = _modeIdx;
        }
        return monitorMode;
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    public static int[] streamOutMonitorMode (final MonitorMode monitorMode) {
        final int[] data = new int[NUM_MONITOR_MODE_PROPERTIES_ALL];
        int idx=0;
        data[idx++] = NUM_MONITOR_MODE_PROPERTIES_ALL;
        data[idx++] = monitorMode.getSurfaceSize().getResolution().getWidth();
        data[idx++] = monitorMode.getSurfaceSize().getResolution().getHeight();
        data[idx++] = monitorMode.getSurfaceSize().getBitsPerPixel();
        data[idx++] = (int)(monitorMode.getRefreshRate()*100.0f); // Hz*100
        data[idx++] = monitorMode.getFlags();
        data[idx++] = monitorMode.getId();
        data[idx++] = monitorMode.getRotation();
        if(NUM_MONITOR_MODE_PROPERTIES_ALL != idx) {
            throw new InternalError("wrong number of attributes: got "+idx+" != should "+NUM_MONITOR_MODE_PROPERTIES_ALL);
        }
        return data;
    }

    /**
     * WARNING: must be synchronized with ScreenMode.h, native implementation
     * <p>
     * Note: This variant only works for impl. w/ a unique mode key pair <i>modeId, rotation</i>.
     * </p>
     * @param cache hash arrays of unique {@link MonitorMode} components and {@link MonitorDevice}s, allowing to avoid duplicates
     * @param screen the associated {@link ScreenImpl}
     * @param pixelScale pre-fetched current pixel-scale, maybe {@code null} for {@link ScalableSurface#IDENTITY_PIXELSCALE}.
     * @param monitorProperties the input data inclusive supported modes.
     * @param offset the offset to the input data
     * @param monitor_idx if not null, returns the index of resulting {@link MonitorDevice} within {@link Cache#monitorDevices}.
     * @return {@link MonitorDevice} of the identical (old or new) element in {@link Cache#monitorDevices},
     *         matching the input <code>modeProperties</code>, or null if input could not be processed.
     */
    public static MonitorDevice streamInMonitorDevice(final Cache cache, final ScreenImpl screen,
                                                      final float[] pixelScale,
                                                      final int[] monitorProperties, int offset,
                                                      final int[] monitor_idx) {
        // min 11: count, id, ScreenSizeMM[width, height], Viewport[x, y, width, height], currentMonitorModeId, rotation, supportedModeId+
        final int count = monitorProperties[offset];
        if(MIN_MONITOR_DEVICE_PROPERTIES > count) {
            throw new RuntimeException("property count should be >= "+MIN_MONITOR_DEVICE_PROPERTIES+", but is "+count+", len "+(monitorProperties.length-offset));
        }
        if(MIN_MONITOR_DEVICE_PROPERTIES > monitorProperties.length-offset) {
            throw new RuntimeException("properties array too short (min), should be >= "+MIN_MONITOR_DEVICE_PROPERTIES+", is "+(monitorProperties.length-offset));
        }
        if(count > monitorProperties.length-offset) {
            throw new RuntimeException("properties array too short (count), should be >= "+count+", is "+(monitorProperties.length-offset));
        }
        final int limit = offset + count;
        offset++;
        final List<MonitorMode> allMonitorModes = cache.monitorModes.getData();
        final int id = monitorProperties[offset++];
        final boolean isClone = 0 == monitorProperties[offset++] ? false : true;
        final boolean isPrimary = 0 == monitorProperties[offset++] ? false : true;
        final DimensionImmutable sizeMM = streamInResolution(monitorProperties, offset); offset+=NUM_RESOLUTION_PROPERTIES;
        final Rectangle viewportPU = new Rectangle(monitorProperties[offset++], monitorProperties[offset++], monitorProperties[offset++], monitorProperties[offset++]);
        final Rectangle viewportWU = new Rectangle(monitorProperties[offset++], monitorProperties[offset++], monitorProperties[offset++], monitorProperties[offset++]);
        final MonitorMode currentMode;
        {
            final int modeId = monitorProperties[offset++];
            final int rotation = monitorProperties[offset++];
            currentMode = getByNativeIdAndRotation(allMonitorModes, modeId, rotation);
        }
        final ArrayHashSet<MonitorMode> supportedModes = new ArrayHashSet<MonitorMode>(false, ArrayHashSet.DEFAULT_INITIAL_CAPACITY, ArrayHashSet.DEFAULT_LOAD_FACTOR);
        while( offset < limit ) {
            final int modeId = monitorProperties[offset++];
            for (int i=0; i<allMonitorModes.size(); i++) {
                final MonitorMode mode = allMonitorModes.get(i);
                if( mode.getId() == modeId ) {
                    supportedModes.add(mode);
                }
            }
        }
        MonitorDevice monitorDevice = new MonitorDeviceImpl(screen, id, isClone, isPrimary,
                                                            sizeMM, currentMode, pixelScale,
                                                            viewportPU, viewportWU, supportedModes);
        if(null!=cache) {
            monitorDevice = cache.monitorDevices.getOrAdd(monitorDevice);
            if( monitorDevice.isPrimary() ) {
                cache.setPrimary(monitorDevice);
            }
        }
        if( null != monitor_idx ) {
            final int _monitorIdx  = cache.monitorDevices.indexOf(monitorDevice);
            if( 0 > _monitorIdx ) {
                throw new InternalError("Invalid index of current unified mode "+monitorDevice);
            }
            monitor_idx[0] = _monitorIdx;
        }
        return monitorDevice;
    }
    private static MonitorMode getByNativeIdAndRotation(final List<MonitorMode> monitorModes, final int modeId, final int rotation) {
        if( null!=monitorModes && monitorModes.size()>0 ) {
            for (int i=0; i<monitorModes.size(); i++) {
                final MonitorMode mode = monitorModes.get(i);
                if( mode.getId() == modeId && mode.getRotation() == rotation ) {
                    return mode;
                }
            }
        }
        return null;
    }

    /**
     * WARNING: must be synchronized with ScreenMode.h, native implementation
     * <p>
     * This variant expects <code>count</code> to be <code>{@link MIN_MONITOR_DEVICE_PROPERTIES} - 1 - {@link NUM_MONITOR_MODE_PROPERTIES}</code>,
     * due to lack of supported mode and current mode.
     * </p>
     * @param cache hash arrays of unique {@link MonitorMode} components and {@link MonitorDevice}s, allowing to avoid duplicates
     * @param screen the associated {@link ScreenImpl}
     * @param currentMode pre-fetched current {@link MonitorMode}s from cache.
     * @param pixelScale pre-fetched current pixel-scale, maybe {@code null} for {@link ScalableSurface#IDENTITY_PIXELSCALE}.
     * @param supportedModes pre-assembled list of supported {@link MonitorMode}s from cache.
     * @param monitorProperties the input data minus supported modes!
     * @param offset the offset to the input data
     * @param monitor_idx if not null, returns the index of resulting {@link MonitorDevice} within {@link Cache#monitorDevices}.
     * @return {@link MonitorDevice} of the identical (old or new) element in {@link Cache#monitorDevices},
     *         matching the input <code>modeProperties</code>, or null if input could not be processed.
     */
    public static MonitorDevice streamInMonitorDevice(final Cache cache, final ScreenImpl screen,
                                                      final MonitorMode currentMode,
                                                      final float[] pixelScale,
                                                      final ArrayHashSet<MonitorMode> supportedModes,
                                                      final int[] monitorProperties, int offset,
                                                      final int[] monitor_idx) {
        // min 11: count, id, ScreenSizeMM[width, height], Viewport[x, y, width, height], currentMonitorModeId, rotation, supportedModeId+
        final int count = monitorProperties[offset];
        if(MIN_MONITOR_DEVICE_PROPERTIES - 1 - NUM_MONITOR_MODE_PROPERTIES != count) {
            throw new RuntimeException("property count should be == "+(MIN_MONITOR_DEVICE_PROPERTIES-1-NUM_MONITOR_MODE_PROPERTIES)+", but is "+count+", len "+(monitorProperties.length-offset));
        }
        if(MIN_MONITOR_DEVICE_PROPERTIES - 1 - NUM_MONITOR_MODE_PROPERTIES > monitorProperties.length-offset) {
            throw new RuntimeException("properties array too short (min), should be >= "+(MIN_MONITOR_DEVICE_PROPERTIES-1-NUM_MONITOR_MODE_PROPERTIES)+", is "+(monitorProperties.length-offset));
        }
        if(count > monitorProperties.length-offset) {
            throw new RuntimeException("properties array too short (count), should be >= "+count+", is "+(monitorProperties.length-offset));
        }
        offset++;
        final int id = monitorProperties[offset++];
        final boolean isClone = 0 == monitorProperties[offset++] ? false : true;
        final boolean isPrimary = 0 == monitorProperties[offset++] ? false : true;
        final DimensionImmutable sizeMM = streamInResolution(monitorProperties, offset); offset+=NUM_RESOLUTION_PROPERTIES;
        final Rectangle viewportPU = new Rectangle(monitorProperties[offset++], monitorProperties[offset++], monitorProperties[offset++], monitorProperties[offset++]);
        final Rectangle viewportWU = new Rectangle(monitorProperties[offset++], monitorProperties[offset++], monitorProperties[offset++], monitorProperties[offset++]);
        MonitorDevice monitorDevice = new MonitorDeviceImpl(screen, id, isClone, isPrimary,
                                                            sizeMM, currentMode, pixelScale,
                                                            viewportPU, viewportWU, supportedModes);
        if(null!=cache) {
            monitorDevice = cache.monitorDevices.getOrAdd(monitorDevice);
            if( monitorDevice.isPrimary() ) {
                cache.setPrimary(monitorDevice);
            }
        }
        if( null != monitor_idx ) {
            final int _monitorIdx  = cache.monitorDevices.indexOf(monitorDevice);
            if( 0 > _monitorIdx ) {
                throw new InternalError("Invalid index of current unified mode "+monitorDevice);
            }
            monitor_idx[0] = _monitorIdx;
        }
        return monitorDevice;
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    public static int[] streamOutMonitorDevice (final MonitorDevice monitorDevice) {
        // min 11: count, id, ScreenSizeMM[width, height], Viewport[x, y, width, height], currentMonitorModeId, rotation, supportedModeId+
        final int supportedModeCount = monitorDevice.getSupportedModes().size();
        if( 0 == supportedModeCount ) {
            throw new RuntimeException("no supported modes: "+monitorDevice);
        }
        final int[] data = new int[MIN_MONITOR_DEVICE_PROPERTIES + supportedModeCount - 1];
        int idx=0;
        data[idx++] = data.length;
        data[idx++] = monitorDevice.getId();
        data[idx++] = monitorDevice.isClone() ? 1 : 0;
        data[idx++] = monitorDevice.isPrimary() ? 1 : 0;
        data[idx++] = monitorDevice.getSizeMM().getWidth();
        data[idx++] = monitorDevice.getSizeMM().getHeight();
        data[idx++] = monitorDevice.getViewport().getX();
        data[idx++] = monitorDevice.getViewport().getY();
        data[idx++] = monitorDevice.getViewport().getWidth();
        data[idx++] = monitorDevice.getViewport().getHeight();
        data[idx++] = monitorDevice.getViewportInWindowUnits().getX();
        data[idx++] = monitorDevice.getViewportInWindowUnits().getY();
        data[idx++] = monitorDevice.getViewportInWindowUnits().getWidth();
        data[idx++] = monitorDevice.getViewportInWindowUnits().getHeight();
        data[idx++] = monitorDevice.getCurrentMode().getId();
        data[idx++] = monitorDevice.getCurrentMode().getRotation();
        final List<MonitorMode> supportedModes = monitorDevice.getSupportedModes();
        for(int i=0; i<supportedModes.size(); i++) {
            data[idx++] = supportedModes.get(i).getId();
        }
        if(data.length != idx) {
            throw new InternalError("wrong number of attributes: got "+idx+" != should "+data.length);
        }
        return data;
    }

    /**
     * Identify monitor devices:
     * <ul>
     *   <li><i>cloned</i> mode, i.e. consecutive devices being 100% covered by preceding devices.</li>
     * </ul>
     */
    /* pp */ static void identifyMonitorDevices(final MonitorModeProps.Cache cache) {
        final ArrayList<MonitorDevice> monitors = cache.monitorDevices.toArrayList();
        final int monitorCount = monitors.size();
        for(int i=0; i<monitorCount; i++) {
            final MonitorDevice a = monitors.get(i);
            if( !a.isClone() ) {
                for(int j=i+1; j<monitorCount; j++) {
                    final MonitorDevice b = monitors.get(j);
                    if( !b.isClone() ) {
                        final float coverage = b.getViewport().coverage( a.getViewport() );
                        if( FloatUtil.isZero( 1f - coverage, FloatUtil.EPSILON ) ) {
                            ((MonitorDeviceImpl)b).setIsClone(true);
                            if( Screen.DEBUG ) {
                                System.err.printf("MonitorCloneTest[%d of %d]: %f -> _is_ covered%n", j, i, coverage);
                                System.err.printf("  Monitor[%d] %s%n", j, b.toString());
                                System.err.printf("  Monitor[%d] %s%n", i, a.toString());
                            }
                        } else if( Screen.DEBUG ) {
                            System.err.printf("MonitorDevice-CloneTest[%d of %d]: %f -> not covered%n", j, i, coverage);
                            System.err.printf("  Monitor[%d] %s%n", j, b.toString());
                            System.err.printf("  Monitor[%d] %s%n", i, a.toString());
                        }
                    }
                }
            }
        }
    }

    public static final void swapRotatePair(final int rotation, final int[] pairs, int offset, final int numPairs) {
        if( MonitorMode.ROTATE_0 == rotation || MonitorMode.ROTATE_180 == rotation ) {
            // nop
            return;
        }
        for(int i=0; i<numPairs; i++, offset+=2) {
            final int tmp = pairs[offset];
            pairs[offset] = pairs[offset+1];
            pairs[offset+1] = tmp;
        }
    }

}
