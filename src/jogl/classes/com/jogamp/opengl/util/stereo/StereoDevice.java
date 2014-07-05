/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util.stereo;

import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.PointImmutable;

import jogamp.opengl.Debug;

import com.jogamp.opengl.math.FovHVHalves;

/**
 * Interface describing a native stereoscopic device
 */
public interface StereoDevice {
    public static final boolean DEBUG = Debug.debug("StereoDevice");

    /**
     * Default eye position offset for {@link #createRenderer(int, int, float[], FovHVHalves[], float)}.
     * <p>
     * Default offset is 1.6f <i>up</i> and 5.0f <i>away</i>.
     * </p>
     */
    public static final float[] DEFAULT_EYE_POSITION_OFFSET = { 0.0f, 1.6f, -5.0f };

    /** Disposes this {@link StereoDevice}. */
    public void dispose();

    /**
     * If operation within a device spanning virtual desktop,
     * returns the device position.
     * <p>
     * Otherwise simply 0/0.
     * </p>
     */
    public PointImmutable getPosition();

    /**
     * Returns the required surface size in pixel.
     */
    public DimensionImmutable getSurfaceSize();

    /**
     * Returns the device default {@link FovHVHalves} per eye.
     */
    public FovHVHalves[] getDefaultFOV();

    /** Start or stop sensors. Returns true if action was successful, otherwise false. */
    public boolean startSensors(boolean start);

    /** Return true if sensors have been started, false otherwise */
    public boolean getSensorsStarted();

    /**
     * Create a new {@link StereoDeviceRenderer} instance.
     *
     * @param distortionBits {@link StereoDeviceRenderer} distortion bits, e.g. {@link StereoDeviceRenderer#DISTORTION_BARREL}, etc.
     * @param textureCount desired texture count for post-processing, see {@link StereoDeviceRenderer#getTextureCount()} and {@link StereoDeviceRenderer#ppRequired()}
     * @param eyePositionOffset eye position offset, e.g. {@link #DEFAULT_EYE_POSITION_OFFSET}.
     * @param eyeFov FovHVHalves[2] field-of-view per eye
     * @param pixelsPerDisplayPixel
     * @param textureUnit
     * @return
     */
    public StereoDeviceRenderer createRenderer(final int distortionBits,
                                               final int textureCount, final float[] eyePositionOffset,
                                               final FovHVHalves[] eyeFov, final float pixelsPerDisplayPixel, final int textureUnit);
}
