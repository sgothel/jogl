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

import java.util.List;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.Rectangle;
import javax.media.nativewindow.util.SurfaceSize;

import jogamp.newt.MonitorDeviceImpl;
import jogamp.newt.ScreenImpl;

/**
 * Encodes and decodes {@link MonitorMode} and {@link MonitorDevice} properties.
 */
public class MonitorModeProps {
    /** WARNING: must be synchronized with ScreenMode.h, native implementation
     * 2: width, height
     */
    public static final int NUM_RESOLUTION_PROPERTIES   = 2;

    /** WARNING: must be synchronized with ScreenMode.h, native implementation
     * 1: bpp
     */
    public static final int NUM_SURFACE_SIZE_PROPERTIES = 1;
    
    /** WARNING: must be synchronized with ScreenMode.h, native implementation
     * 2: refresh-rate (Hz*100), flags
     */
    public static final int NUM_SIZEANDRATE_PROPERTIES = 2;
    
    /** WARNING: must be synchronized with ScreenMode.h, native implementation
     * 2: id, rotation
     */
    public static final int NUM_MONITOR_MODE_PROPERTIES  = 2;

    /** WARNING: must be synchronized with ScreenMode.h, native implementation
     * count + all the above
     */
    public static final int NUM_MONITOR_MODE_PROPERTIES_ALL = 8;
    
    public static final int IDX_MONITOR_MODE_BPP =   1 // count
                                                   + MonitorModeProps.NUM_RESOLUTION_PROPERTIES
                                                   ;
    public static final int IDX_MONITOR_MODE_ROT =   1 // count
                                                   + MonitorModeProps.NUM_RESOLUTION_PROPERTIES
                                                   + MonitorModeProps.NUM_SURFACE_SIZE_PROPERTIES
                                                   + MonitorModeProps.NUM_SIZEANDRATE_PROPERTIES
                                                   + 1 // id of MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES
                                                   ;
    
    /** WARNING: must be synchronized with ScreenMode.h, native implementation
     * 10: count + id, ScreenSizeMM[width, height], rotated Viewport[x, y, width, height], currentMonitorModeId, rotation, supportedModeId+
     */
    public static final int MIN_MONITOR_DEVICE_PROPERTIES = 11;

    public static final int IDX_MONITOR_DEVICE_VIEWPORT =   1 // count
                                                          + 1 // native mode
                                                          + MonitorModeProps.NUM_RESOLUTION_PROPERTIES // sizeMM
                                                          ;
    
    public static class Cache {
        public final ArrayHashSet<DimensionImmutable>       resolutions  = new ArrayHashSet<DimensionImmutable>();
        public final ArrayHashSet<SurfaceSize>              surfaceSizes = new ArrayHashSet<SurfaceSize>();
        public final ArrayHashSet<MonitorMode.SizeAndRRate> sizeAndRates = new ArrayHashSet<MonitorMode.SizeAndRRate>(); 
        public final ArrayHashSet<MonitorMode>              monitorModes = new ArrayHashSet<MonitorMode>();
        public final ArrayHashSet<MonitorDevice>            monitorDevices = new ArrayHashSet<MonitorDevice>();
    }
    
    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    private static DimensionImmutable streamInResolution(int[] resolutionProperties, int offset) {
        Dimension resolution = new Dimension(resolutionProperties[offset++], resolutionProperties[offset++]);
        return resolution;
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    private static SurfaceSize streamInSurfaceSize(DimensionImmutable resolution, int[] sizeProperties, int offset) {
        SurfaceSize surfaceSize = new SurfaceSize(resolution, sizeProperties[offset++]);
        return surfaceSize;
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    private static MonitorMode.SizeAndRRate streamInSizeAndRRate(SurfaceSize surfaceSize, int[] sizeAndRRateProperties, int offset) {
        final float refreshRate = sizeAndRRateProperties[offset++]/100.0f;
        final int flags = sizeAndRRateProperties[offset++];
        return new MonitorMode.SizeAndRRate(surfaceSize, refreshRate, flags);
    }
    
    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    private static MonitorMode streamInMonitorMode0(MonitorMode.SizeAndRRate sizeAndRate, int[] modeProperties, int offset) {
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
    public static MonitorMode streamInMonitorMode(int[] mode_idx, Cache cache,
                                                  int[] modeProperties, int offset) {
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
            int _modeIdx  = cache.monitorModes.indexOf(monitorMode);
            if( 0 > _modeIdx ) {
                throw new InternalError("Invalid index of current unified mode "+monitorMode);
            }
            mode_idx[0] = _modeIdx;
        }
        return monitorMode;
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    public static int[] streamOutMonitorMode (MonitorMode monitorMode) {
        int[] data = new int[NUM_MONITOR_MODE_PROPERTIES_ALL];
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
     * @param mode_idx if not null, returns the index of resulting {@link MonitorDevice} within {@link Cache#monitorDevices}.
     * @param cache hash arrays of unique {@link MonitorMode} components and {@link MonitorDevice}s, allowing to avoid duplicates
     * @param modeProperties the input data
     * @param offset the offset to the input data
     * @return {@link MonitorDevice} of the identical (old or new) element in {@link Cache#monitorDevices},
     *         matching the input <code>modeProperties</code>, or null if input could not be processed.
     */
    public static MonitorDevice streamInMonitorDevice(int[] monitor_idx, Cache cache, ScreenImpl screen, int[] monitorProperties, int offset) {
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
        final DimensionImmutable sizeMM = streamInResolution(monitorProperties, offset); offset+=NUM_RESOLUTION_PROPERTIES;
        final Rectangle viewport = new Rectangle(monitorProperties[offset++], monitorProperties[offset++], monitorProperties[offset++], monitorProperties[offset++]);
        final MonitorMode currentMode;
        {
            final int modeId = monitorProperties[offset++];
            final int rotation = monitorProperties[offset++];
            currentMode = getByNativeIdAndRotation(allMonitorModes, modeId, rotation);
        }
        final ArrayHashSet<MonitorMode> supportedModes = new ArrayHashSet<MonitorMode>();
        while( offset < limit ) {
            final int modeId = monitorProperties[offset++];
            for (int i=0; i<allMonitorModes.size(); i++) {
                final MonitorMode mode = allMonitorModes.get(i);
                if( mode.getId() == modeId ) {
                    supportedModes.add(mode);
                }
            }
        }
        MonitorDevice monitorDevice = new MonitorDeviceImpl(screen, id, sizeMM, viewport, currentMode, supportedModes);
        if(null!=cache) {
            monitorDevice = cache.monitorDevices.getOrAdd(monitorDevice);
        }
        if( null != monitor_idx ) {
            int _monitorIdx  = cache.monitorDevices.indexOf(monitorDevice);
            if( 0 > _monitorIdx ) {
                throw new InternalError("Invalid index of current unified mode "+monitorDevice);
            }
            monitor_idx[0] = _monitorIdx;
        }
        return monitorDevice;
    }    
    private static MonitorMode getByNativeIdAndRotation(List<MonitorMode> monitorModes, int modeId, int rotation) {
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
     *
     * @param mode_idx if not null, returns the index of resulting {@link MonitorDevice} within {@link Cache#monitorDevices}.
     * @param cache hash arrays of unique {@link MonitorMode} components and {@link MonitorDevice}s, allowing to avoid duplicates
     * @param supportedModes pre-assembled list of supported {@link MonitorMode}s from cache.
     * @param currentMode pre-fetched current {@link MonitorMode}s from cache.
     * @param modeProperties the input data minus supported modes!
     * @param offset the offset to the input data
     * @return {@link MonitorDevice} of the identical (old or new) element in {@link Cache#monitorDevices},
     *         matching the input <code>modeProperties</code>, or null if input could not be processed.
     */
    public static MonitorDevice streamInMonitorDevice(int[] monitor_idx, Cache cache, ScreenImpl screen, ArrayHashSet<MonitorMode> supportedModes, MonitorMode currentMode, int[] monitorProperties, int offset) {
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
        final DimensionImmutable sizeMM = streamInResolution(monitorProperties, offset); offset+=NUM_RESOLUTION_PROPERTIES;
        final Rectangle viewport = new Rectangle(monitorProperties[offset++], monitorProperties[offset++], monitorProperties[offset++], monitorProperties[offset++]);
        MonitorDevice monitorDevice = new MonitorDeviceImpl(screen, id, sizeMM, viewport, currentMode, supportedModes);
        if(null!=cache) {
            monitorDevice = cache.monitorDevices.getOrAdd(monitorDevice);
        }
        if( null != monitor_idx ) {
            int _monitorIdx  = cache.monitorDevices.indexOf(monitorDevice);
            if( 0 > _monitorIdx ) {
                throw new InternalError("Invalid index of current unified mode "+monitorDevice);
            }
            monitor_idx[0] = _monitorIdx;
        }
        return monitorDevice;
    }
    
    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    public static int[] streamOutMonitorDevice (MonitorDevice monitorDevice) {
        // min 11: count, id, ScreenSizeMM[width, height], Viewport[x, y, width, height], currentMonitorModeId, rotation, supportedModeId+
        int supportedModeCount = monitorDevice.getSupportedModes().size();
        if( 0 == supportedModeCount ) {
            throw new RuntimeException("no supported modes: "+monitorDevice);
        }
        int[] data = new int[MIN_MONITOR_DEVICE_PROPERTIES + supportedModeCount - 1];
        int idx=0;
        data[idx++] = data.length;
        data[idx++] = monitorDevice.getId();
        data[idx++] = monitorDevice.getSizeMM().getWidth();
        data[idx++] = monitorDevice.getSizeMM().getHeight();
        data[idx++] = monitorDevice.getViewport().getX();
        data[idx++] = monitorDevice.getViewport().getY();
        data[idx++] = monitorDevice.getViewport().getWidth();
        data[idx++] = monitorDevice.getViewport().getHeight();
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
    
    public final void swapRotatePair(int rotation, int[] pairs, int offset, int numPairs) {
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
