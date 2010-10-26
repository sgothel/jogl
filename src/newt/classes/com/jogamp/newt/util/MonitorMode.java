/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

import javax.media.nativewindow.util.*;

/** Immutable MonitorMode Class, consisting of it's read only components:<br>
 * <ul>
 *  <li>{@link javax.media.nativewindow.util.SurfaceSize} surface memory size</li>
 *  <li>{@link javax.media.nativewindow.util.DimensionReadOnly} size in [mm]</li>
 *  <li><code>refresh rate</code></li>
 * </ul>
 */
public class MonitorMode  implements Cloneable {
    SurfaceSize surfaceSize;
    DimensionReadOnly screenSizeMM; // in [mm]
    int refreshRate;

    public MonitorMode(SurfaceSize surfaceSize, DimensionReadOnly screenSizeMM, int refreshRate) {
        if(null==surfaceSize || refreshRate<=0) {
            throw new IllegalArgumentException("surfaceSize must be set and refreshRate greater 0");
        }
        this.surfaceSize=surfaceSize;
        this.screenSizeMM=screenSizeMM;
        this.refreshRate=refreshRate;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new InternalError();
        }
    }

    public final SurfaceSize getSurfaceSize() {
        return surfaceSize;
    }

    public final DimensionReadOnly getScreenSizeMM() {
        return screenSizeMM;
    }

    public final int getRefreshRate() {
        return refreshRate;
    }

    public final String toString() {
        return new String("MonitorMode["+surfaceSize+" x "+refreshRate+" Hz, "+screenSizeMM+" [mm]x[mm]]");
    }

    /**
     * Checks whether two size objects are equal. Two instances
     * of <code>MonitorMode</code> are equal if the three components
     * <code>surfaceSize</code>, <code>screenSizeMM</code> and <code>refreshRate</code>
     * are equal.
     * @return  <code>true</code> if the two dimensions are equal;
     *          otherwise <code>false</code>.
     */
    public final boolean equals(Object obj) {
        if (obj instanceof MonitorMode) {
            MonitorMode p = (MonitorMode)obj;
            return getSurfaceSize().equals(p.getSurfaceSize()) &&
                   getScreenSizeMM().equals(p.getScreenSizeMM()) &&
                   getRefreshRate() == p.getRefreshRate() ;
        }
        return false;
    }

    public final int hashCode() {
        // 31 * x == (x << 5) - x
        int hash = 31 + getSurfaceSize().hashCode();
        hash = ((hash << 5) - hash) + getScreenSizeMM().hashCode();
        hash = ((hash << 5) - hash) + getRefreshRate();
        return hash;
    }
}

