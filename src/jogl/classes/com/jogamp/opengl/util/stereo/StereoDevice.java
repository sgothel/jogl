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
    public static final boolean DUMP_DATA = Debug.isPropertyDefined("jogl.debug.StereoDevice.DumpData", true);

    /** Merely a class providing a type-tag for extensions */
    public static class Config {
        // NOP
    }

    /** Return the factory used to create this device. */
    public StereoDeviceFactory getFactory();

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
     * Return the device default eye position offset for {@link #createRenderer(int, int, float[], FovHVHalves[], float)}.
     * <p>
     * Result is an array of float values for
     * <ul>
     *   <li><i>right</i> (positive)</li>
     *   <li><i>up</i> (positive)</li>
     *   <li><i>forward</i> (negative)</li>
     * </ul>
     * </p>
     * @return
     */
    public float[] getDefaultEyePositionOffset();

    /**
     * Returns the device default {@link FovHVHalves} for all supported eyes
     * in natural order, i.e. left and right if supported.
     * <p>
     * Monoscopic devices return an array length of one, without the value for the right-eye!
     * </p>
     */
    public FovHVHalves[] getDefaultFOV();

    /** Start or stop sensors. Returns true if action was successful, otherwise false. */
    public boolean startSensors(boolean start);

    /** Return true if sensors have been started, false otherwise */
    public boolean getSensorsStarted();

    /**
     * Returns an array of the preferred eye rendering order.
     * The array length reflects the supported eye count.
     * <p>
     * Monoscopic devices only support one eye, where stereoscopic device two eyes.
     * </p>
     */
    public int[] getEyeRenderOrder();

    /**
     * Returns the supported distortion compensation by the {@link StereoDeviceRenderer},
     * e.g. {@link StereoDeviceRenderer#DISTORTION_BARREL}, {@link StereoDeviceRenderer#DISTORTION_CHROMATIC}, etc.
     * @see StereoDeviceRenderer#getDistortionBits()
     * @see #createRenderer(int, int, float[], FovHVHalves[], float, int)
     * @see #getRecommendedDistortionBits()
     * @see #getMinimumDistortionBits()
     */
    public int getSupportedDistortionBits();

    /**
     * Returns the recommended distortion compensation bits for the {@link StereoDeviceRenderer},
     * e.g. {@link StereoDeviceRenderer#DISTORTION_BARREL}, {@link StereoDeviceRenderer#DISTORTION_CHROMATIC}
     * {@link StereoDeviceRenderer#DISTORTION_VIGNETTE}.
     * <p>
     * User shall use the recommended distortion compensation to achieve a distortion free view.
     * </p>
     * @see StereoDeviceRenderer#getDistortionBits()
     * @see #createRenderer(int, int, float[], FovHVHalves[], float, int)
     * @see #getSupportedDistortionBits()
     * @see #getMinimumDistortionBits()
     */
    public int getRecommendedDistortionBits();

    /**
     * Returns the minimum distortion compensation bits as required by the {@link StereoDeviceRenderer},
     * e.g. {@link StereoDeviceRenderer#DISTORTION_BARREL} in case the stereoscopic display uses [a]spherical lenses.
     * <p>
     * Minimum distortion compensation bits are being enforced by the {@link StereoDeviceRenderer}.
     * </p>
     * @see #getSupportedDistortionBits()
     * @see #getRecommendedDistortionBits()
     * @see StereoDeviceRenderer#getDistortionBits()
     * @see #createRenderer(int, int, float[], FovHVHalves[], float, int)
     */
    public int getMinimumDistortionBits();

    /**
     * Create a new {@link StereoDeviceRenderer} instance.
     *
     * @param distortionBits {@link StereoDeviceRenderer} distortion bits, e.g. {@link StereoDeviceRenderer#DISTORTION_BARREL}, etc,
     *                       see {@link #getRecommendedDistortionBits()}.
     * @param textureCount desired texture count for post-processing, see {@link StereoDeviceRenderer#getTextureCount()} and {@link StereoDeviceRenderer#ppAvailable()}
     * @param eyePositionOffset eye position offset, e.g. {@link #getDefaultEyePositionOffset()}.
     * @param eyeFov FovHVHalves[] field-of-view per eye, e.g. {@link #getDefaultFOV()}. May contain only one value for monoscopic devices,
     *               see {@link #getEyeRenderOrder()}.
     * @param pixelsPerDisplayPixel
     * @param textureUnit
     * @return
     */
    public StereoDeviceRenderer createRenderer(final int distortionBits,
                                               final int textureCount, final float[] eyePositionOffset,
                                               final FovHVHalves[] eyeFov, final float pixelsPerDisplayPixel, final int textureUnit);
}
