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

import com.jogamp.newt.MonitorMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.SurfaceSize;

/**
 * Convenient {@link com.jogamp.newt.MonitorMode} utility methods,
 * filters etc.
 */
public class MonitorModeUtil {

    public static int getIndex(final List<MonitorMode> monitorModes, final MonitorMode search) {
        return monitorModes.indexOf(search);
    }

    public static int getIndexByHashCode(final List<MonitorMode> monitorModes, final MonitorMode search) {
        if( null!=monitorModes && monitorModes.size()>0 ) {
            for (int i=0; i<monitorModes.size(); i++) {
                if ( search.hashCode() == monitorModes.get(i).hashCode() ) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static MonitorMode getByNativeSizeRateIdAndRotation(final List<MonitorMode> monitorModes, final MonitorMode.SizeAndRRate sizeAndRate, final int modeId, final int rotation) {
        if( null!=monitorModes && monitorModes.size()>0 ) {
            for (int i=0; i<monitorModes.size(); i++) {
                final MonitorMode mode = monitorModes.get(i);
                if( mode.getSizeAndRRate().equals(sizeAndRate) && mode.getId() == modeId && mode.getRotation() == rotation ) {
                    return mode;
                }
            }
        }
        return null;
    }

    /** Sort the given {@link MonitorMode} collection w/ {@link MonitorMode#compareTo(MonitorMode)} function. */
    public static void sort(final List<MonitorMode> monitorModes, final boolean ascendingOrder) {
        if( ascendingOrder ) {
            Collections.sort(monitorModes);
        } else {
            Collections.sort(monitorModes, MonitorMode.monitorModeComparatorInv);
        }
    }

    /**
     *
     * @param monitorModes
     * @param surfaceSize
     * @return modes with exact {@link SurfaceSize}. May return zero sized list for non.
     */
    public static List<MonitorMode> filterBySurfaceSize(final List<MonitorMode> monitorModes, final SurfaceSize surfaceSize) {
        final List<MonitorMode> out = new ArrayList<MonitorMode>();
        if( null!=monitorModes && monitorModes.size()>0 ) {
            for (int i=0; null!=monitorModes && i<monitorModes.size(); i++) {
                final MonitorMode mode = monitorModes.get(i);
                if(mode.getSurfaceSize().equals(surfaceSize)) {
                    out.add(mode);
                }
            }
        }
        return out;
    }

    /**
     *
     * @param monitorModes
     * @param rotation
     * @return modes with exact rotation. May return zero sized list for non.
     */
    public static List<MonitorMode> filterByRotation(final List<MonitorMode> monitorModes, final int rotation) {
        final List<MonitorMode> out = new ArrayList<MonitorMode>();
        if( null!=monitorModes && monitorModes.size()>0 ) {
            for (int i=0; null!=monitorModes && i<monitorModes.size(); i++) {
                final MonitorMode mode = monitorModes.get(i);
                if(mode.getRotation() == rotation) {
                    out.add(mode);
                }
            }
        }
        return out;
    }

    /**
     *
     * @param monitorModes
     * @param bitsPerPixel
     * @return modes with exact bpp. May return zero sized list for non.
     */
    public static List<MonitorMode> filterByBpp(final List<MonitorMode> monitorModes, final int bitsPerPixel) {
        final List<MonitorMode> out = new ArrayList<MonitorMode>();
        if( null!=monitorModes && monitorModes.size()>0 ) {
            for (int i=0; null!=monitorModes && i<monitorModes.size(); i++) {
                final MonitorMode mode = monitorModes.get(i);
                if(mode.getSurfaceSize().getBitsPerPixel() == bitsPerPixel) {
                    out.add(mode);
                }
            }
        }
        return out;
    }

    /**
     *
     * @param monitorModes
     * @param flags
     * @return modes with exact flags. May return zero sized list for non.
     */
    public static List<MonitorMode> filterByFlags(final List<MonitorMode> monitorModes, final int flags) {
        final List<MonitorMode> out = new ArrayList<MonitorMode>();
        if( null!=monitorModes && monitorModes.size()>0 ) {
            for (int i=0; null!=monitorModes && i<monitorModes.size(); i++) {
                final MonitorMode mode = monitorModes.get(i);
                if(mode.getFlags() == flags) {
                    out.add(mode);
                }
            }
        }
        return out;
    }

    /**
     * @param monitorModes
     * @param resolution in pixel units
     * @return modes with nearest resolution, or matching ones. May return zero sized list for non.
     */
    public static List<MonitorMode> filterByResolution(final List<MonitorMode> monitorModes, final DimensionImmutable resolution) {
        final List<MonitorMode> out = new ArrayList<MonitorMode>();
        if( null!=monitorModes && monitorModes.size()>0 ) {
            final int resolution_sq = resolution.getHeight()*resolution.getWidth();
            int mode_dsq=Integer.MAX_VALUE, mode_dsq_idx=0;

            for (int i=0; null!=monitorModes && i<monitorModes.size(); i++) {
                final MonitorMode mode = monitorModes.get(i);
                final DimensionImmutable res = mode.getSurfaceSize().getResolution();
                final int dsq = Math.abs(resolution_sq - res.getHeight()*res.getWidth());
                if(dsq<mode_dsq) {
                    mode_dsq = dsq;
                    mode_dsq_idx = i;
                }
                if(res.equals(resolution)) {
                    out.add(mode);
                }
            }
            if(out.size() == 0 && 0 <= mode_dsq_idx ) {
                // nearest ..
                out.add(monitorModes.get(mode_dsq_idx));
            }
        }
        return out;
    }

    /**
     *
     * @param monitorModes
     * @param refreshRate
     * @return modes with nearest refreshRate, or matching ones. May return zero sized list for non.
     */
    public static List<MonitorMode> filterByRate(final List<MonitorMode> monitorModes, final float refreshRate) {
        final List<MonitorMode> out = new ArrayList<MonitorMode>();
        if( null!=monitorModes && monitorModes.size()>0 ) {
            float mode_dr = Float.MAX_VALUE;
            int mode_dr_idx = -1;
            for (int i=0; null!=monitorModes && i<monitorModes.size(); i++) {
                final MonitorMode mode = monitorModes.get(i);
                final float dr = Math.abs(refreshRate - mode.getRefreshRate());
                if(dr<mode_dr) {
                    mode_dr = dr;
                    mode_dr_idx = i;
                }
                if(0 == dr) {
                    out.add(mode);
                }
            }
            if(out.size() == 0 && 0 <= mode_dr_idx ) {
                // nearest ..
                out.add(monitorModes.get(mode_dr_idx));
            }
        }
        return out;
    }

    /**
     * @param monitorModes
     * @return modes with highest available bpp (color depth). May return zero sized list for non.
     */
    public static List<MonitorMode> getHighestAvailableBpp(final List<MonitorMode> monitorModes) {
        if( null!=monitorModes && monitorModes.size()>0 ) {
            int highest = -1;
            for (int i=0; null!=monitorModes && i < monitorModes.size(); i++) {
                final MonitorMode mode = monitorModes.get(i);
                final int bpp  = mode.getSurfaceSize().getBitsPerPixel();
                if (bpp > highest) {
                    highest = bpp;
                }
            }
            return filterByBpp(monitorModes, highest);
        }
        return new ArrayList<MonitorMode>();
    }

    /**
     *
     * @param monitorModes
     * @return modes with highest available refresh rate. May return zero sized list for non.
     */
    public static List<MonitorMode> getHighestAvailableRate(final List<MonitorMode> monitorModes) {
        if( null!=monitorModes && monitorModes.size()>0 ) {
            float highest = -1;
            for (int i=0; null!=monitorModes && i < monitorModes.size(); i++) {
                final MonitorMode mode = monitorModes.get(i);
                final float rate = mode.getRefreshRate();
                if (rate > highest) {
                    highest = rate;
                }
            }
            return filterByRate(monitorModes, highest);
        }
        return new ArrayList<MonitorMode>();
    }

}
