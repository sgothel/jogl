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

package com.jogamp.newt.util;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.newt.ScreenMode;
import java.util.ArrayList;
import java.util.List;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.SurfaceSize;

/**
 * Convenient {@link com.jogamp.newt.ScreenMode} utility methods,
 * filters etc.
 */
public class ScreenModeUtil {
    /** WARNING: must be synchronized with ScreenMode.h, native implementation
     * 2: width and height
     */
    public static final int NUM_RESOLUTION_PROPERTIES   = 2;

    /** WARNING: must be synchronized with ScreenMode.h, native implementation
     * 1: bpp
     */
    public static final int NUM_SURFACE_SIZE_PROPERTIES = 1;

    /** WARNING: must be synchronized with ScreenMode.h, native implementation
     * 3: ScreenSizeMM[width, height], refresh-rate
     */
    public static final int NUM_MONITOR_MODE_PROPERTIES = 3;

    /** WARNING: must be synchronized with ScreenMode.h, native implementation
     * 1: rotation, native_mode_id
     */
    public static final int NUM_SCREEN_MODE_PROPERTIES  = 1;

    /** WARNING: must be synchronized with ScreenMode.h, native implementation
     * count + all the above
     */
    public static final int NUM_SCREEN_MODE_PROPERTIES_ALL = 8;

    public static int getIndex(List<ScreenMode> screenModes, ScreenMode search) {
        return screenModes.indexOf(search);
    }

    public static int getIndexByHashCode(List<ScreenMode> screenModes, ScreenMode search) {
        for (int i=0; null!=screenModes && i<screenModes.size(); i++) {
            if ( search.hashCode() == screenModes.get(i).hashCode() ) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @param screenModes
     * @param resolution
     * @return modes with nearest resolution, or matching ones
     */
    public static List<ScreenMode> filterByResolution(List<ScreenMode> screenModes, DimensionImmutable resolution) {
        if(null==screenModes || screenModes.size()==0) {
            return null;
        }
        List<ScreenMode> out = new ArrayList<ScreenMode>();
        int resolution_sq = resolution.getHeight()*resolution.getWidth();
        int sm_dsq=resolution_sq, sm_dsq_idx=0;

        for (int i=0; null!=screenModes && i<screenModes.size(); i++) {
            ScreenMode sm = screenModes.get(i);
            DimensionImmutable res = sm.getMonitorMode().getSurfaceSize().getResolution();
            int dsq = Math.abs(resolution_sq - res.getHeight()*res.getWidth());
            if(dsq<sm_dsq) {
                sm_dsq = dsq;
                sm_dsq_idx = i;
            }
            if(res.equals(resolution)) {
                out.add(sm);
            }
        }
        if(out.size()>0) {
            return out;
        }
        // nearest ..
        resolution = screenModes.get(sm_dsq_idx).getMonitorMode().getSurfaceSize().getResolution();
        return filterByResolution(screenModes, resolution);
    }

    public static List<ScreenMode> filterBySurfaceSize(List<ScreenMode> screenModes, SurfaceSize surfaceSize) {
        if(null==screenModes || screenModes.size()==0) {
            return null;
        }
        List<ScreenMode> out = new ArrayList<ScreenMode>();
        for (int i=0; null!=screenModes && i<screenModes.size(); i++) {
            ScreenMode sm = screenModes.get(i);
            if(sm.getMonitorMode().getSurfaceSize().equals(surfaceSize)) {
                out.add(sm);
            }
        }
        return out.size()>0 ? out : null;
    }

    public static List<ScreenMode> filterByRotation(List<ScreenMode> screenModes, int rotation) {
        if(null==screenModes || screenModes.size()==0) {
            return null;
        }
        List<ScreenMode> out = new ArrayList<ScreenMode>();
        for (int i=0; null!=screenModes && i<screenModes.size(); i++) {
            ScreenMode sm = screenModes.get(i);
            if(sm.getRotation() == rotation) {
                out.add(sm);
            }
        }
        return out.size()>0 ? out : null;
    }

    public static List<ScreenMode> filterByBpp(List<ScreenMode> screenModes, int bitsPerPixel) {
        if(null==screenModes || screenModes.size()==0) {
            return null;
        }
        List<ScreenMode> out = new ArrayList<ScreenMode>();
        for (int i=0; null!=screenModes && i<screenModes.size(); i++) {
            ScreenMode sm = screenModes.get(i);
            if(sm.getMonitorMode().getSurfaceSize().getBitsPerPixel() == bitsPerPixel) {
                out.add(sm);
            }
        }
        return out.size()>0 ? out : null;
    }

    /**
     *
     * @param screenModes
     * @param refreshRate
     * @return modes with nearest refreshRate, or matching ones
     */
    public static List<ScreenMode> filterByRate(List<ScreenMode> screenModes, int refreshRate) {
        if(null==screenModes || screenModes.size()==0) {
            return null;
        }
        int sm_dr = refreshRate;
        int sm_dr_idx = -1;
        List<ScreenMode> out = new ArrayList<ScreenMode>();
        for (int i=0; null!=screenModes && i<screenModes.size(); i++) {
            ScreenMode sm = screenModes.get(i);
            int dr = Math.abs(refreshRate - sm.getMonitorMode().getRefreshRate());
            if(dr<sm_dr) {
                sm_dr = dr;
                sm_dr_idx = i;
            }
            if(0 == dr) {
                out.add(sm);
            }
        }
        if(out.size()>0) {
            return out;
        }
        refreshRate = screenModes.get(sm_dr_idx).getMonitorMode().getRefreshRate();
        return filterByRate(screenModes, refreshRate);
    }

    public static List<ScreenMode> getHighestAvailableBpp(List<ScreenMode> screenModes) {
        if(null==screenModes || screenModes.size()==0) {
            return null;
        }
        int highest = -1;
        for (int i=0; null!=screenModes && i < screenModes.size(); i++) {
            ScreenMode sm = screenModes.get(i);
            int bpp  = sm.getMonitorMode().getSurfaceSize().getBitsPerPixel();
            if (bpp > highest) {
                highest = bpp;
            }
        }
        return filterByBpp(screenModes, highest);
    }

    public static List<ScreenMode> getHighestAvailableRate(List<ScreenMode> screenModes) {
        if(null==screenModes || screenModes.size()==0) {
            return null;
        }
        int highest = -1;
        for (int i=0; null!=screenModes && i < screenModes.size(); i++) {
            ScreenMode sm = screenModes.get(i);
            int rate = sm.getMonitorMode().getRefreshRate();
            if (rate > highest) {
                highest = rate;
            }
        }
        return filterByRate(screenModes, highest);
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    public static DimensionImmutable streamInResolution(int[] resolutionProperties, int offset) {
        Dimension resolution = new Dimension(resolutionProperties[offset++], resolutionProperties[offset++]);
        return resolution;
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    public static SurfaceSize streamInSurfaceSize(DimensionImmutable resolution, int[] sizeProperties, int offset) {
        SurfaceSize surfaceSize = new SurfaceSize(resolution, sizeProperties[offset++]);
        return surfaceSize;
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    public static MonitorMode streamInMonitorMode(SurfaceSize surfaceSize, DimensionImmutable screenSizeMM, int[] monitorProperties, int offset) {
        int refreshRate = monitorProperties[offset++];
        return new MonitorMode(surfaceSize, screenSizeMM, refreshRate);
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    public static ScreenMode streamInScreenMode(MonitorMode monitorMode, int[] modeProperties, int offset) {
        int rotation = modeProperties[offset++];
        return new ScreenMode(monitorMode, rotation);
    }

    /**
     *  WARNING: must be synchronized with ScreenMode.h, native implementation
     *
     * @param modeProperties the input data
     * @param offset the offset to the input data
     * @return ScreenMode element matching the input <code>modeProperties</code>, 
     *         or null if input could not be processed.
     */
    public static ScreenMode streamIn(int[] modeProperties, int offset) {
        return streamInImpl(null, null, null, null, null, modeProperties, offset);
    }

    /**
     *  WARNING: must be synchronized with ScreenMode.h, native implementation
     *
     * @param resolutionPool hash array of unique resolutions, no duplicates
     * @param surfaceSizePool hash array of unique SurfaceSize, no duplicates
     * @param monitorModePool hash array of unique MonitorMode, no duplicates
     * @param screenModePool hash array of unique ScreenMode, no duplicates
     * @param modeProperties the input data
     * @param offset the offset to the input data
     * @return index of the identical (old or new) ScreenMode element in <code>screenModePool</code>,
     *         matching the input <code>modeProperties</code>, or -1 if input could not be processed.
     */
    public static int streamIn(ArrayHashSet<DimensionImmutable> resolutionPool,
                               ArrayHashSet<SurfaceSize>        surfaceSizePool,
                               ArrayHashSet<DimensionImmutable> screenSizeMMPool,
                               ArrayHashSet<MonitorMode>        monitorModePool,
                               ArrayHashSet<ScreenMode>         screenModePool,
                               int[] modeProperties, int offset) {
        ScreenMode screenMode = streamInImpl(resolutionPool, surfaceSizePool, screenSizeMMPool, monitorModePool, screenModePool,
                                         modeProperties, offset);
        return screenModePool.indexOf(screenMode);
    }

                               
    private static ScreenMode streamInImpl(ArrayHashSet<DimensionImmutable> resolutionPool,
                                           ArrayHashSet<SurfaceSize>        surfaceSizePool,
                                           ArrayHashSet<DimensionImmutable> screenSizeMMPool,
                                           ArrayHashSet<MonitorMode>        monitorModePool,
                                           ArrayHashSet<ScreenMode>         screenModePool,
                                           int[] modeProperties, int offset) {
        int count = modeProperties[offset];
        if(NUM_SCREEN_MODE_PROPERTIES_ALL != count) {
            throw new RuntimeException("NUM_SCREEN_MODE_PROPERTIES should be "+NUM_SCREEN_MODE_PROPERTIES_ALL+", is "+count+", len "+(modeProperties.length-offset));
        }
        if(NUM_SCREEN_MODE_PROPERTIES_ALL > modeProperties.length-offset) {
            throw new RuntimeException("properties array too short, should be >= "+NUM_SCREEN_MODE_PROPERTIES_ALL+", is "+(modeProperties.length-offset));
        }
        offset++;
        DimensionImmutable resolution = ScreenModeUtil.streamInResolution(modeProperties, offset);
        offset += ScreenModeUtil.NUM_RESOLUTION_PROPERTIES;
        if(null!=resolutionPool) {
            resolution = resolutionPool.getOrAdd(resolution);
        }

        SurfaceSize surfaceSize = ScreenModeUtil.streamInSurfaceSize(resolution, modeProperties, offset);
        offset += ScreenModeUtil.NUM_SURFACE_SIZE_PROPERTIES;
        if(null!=surfaceSizePool) {
            surfaceSize = surfaceSizePool.getOrAdd(surfaceSize);
        }

        DimensionImmutable screenSizeMM = ScreenModeUtil.streamInResolution(modeProperties, offset);
        offset += ScreenModeUtil.NUM_RESOLUTION_PROPERTIES;
        if(null!=screenSizeMMPool) {
            screenSizeMM = screenSizeMMPool.getOrAdd(screenSizeMM);
        }

        MonitorMode monitorMode = ScreenModeUtil.streamInMonitorMode(surfaceSize, screenSizeMM, modeProperties, offset);
        offset += ScreenModeUtil.NUM_MONITOR_MODE_PROPERTIES - ScreenModeUtil.NUM_RESOLUTION_PROPERTIES;
        if(null!=monitorModePool) {
            monitorMode = monitorModePool.getOrAdd(monitorMode);
        }

        ScreenMode screenMode = ScreenModeUtil.streamInScreenMode(monitorMode, modeProperties, offset);
        if(null!=screenModePool) {
            screenMode = screenModePool.getOrAdd(screenMode);
        }
        return screenMode;
    }

    /** WARNING: must be synchronized with ScreenMode.h, native implementation */
    public static int[] streamOut (ScreenMode screenMode) {
        int[] data = new int[NUM_SCREEN_MODE_PROPERTIES_ALL];
        int idx=0;
        data[idx++] = NUM_SCREEN_MODE_PROPERTIES_ALL;
        data[idx++] = screenMode.getMonitorMode().getSurfaceSize().getResolution().getWidth();
        data[idx++] = screenMode.getMonitorMode().getSurfaceSize().getResolution().getHeight();
        data[idx++] = screenMode.getMonitorMode().getSurfaceSize().getBitsPerPixel();
        data[idx++] = screenMode.getMonitorMode().getScreenSizeMM().getWidth();
        data[idx++] = screenMode.getMonitorMode().getScreenSizeMM().getHeight();
        data[idx++] = screenMode.getMonitorMode().getRefreshRate();
        data[idx++] = screenMode.getRotation();
        if(NUM_SCREEN_MODE_PROPERTIES_ALL != idx) {
            throw new InternalError("wrong number of attributes: got "+idx+" != should "+NUM_SCREEN_MODE_PROPERTIES_ALL);
        }
        return data;
    }

}
