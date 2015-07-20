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

import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.PointImmutable;

import jogamp.opengl.Debug;

import com.jogamp.opengl.math.FovHVHalves;

/**
 * Interface describing a native stereoscopic device
 */
public interface StereoDevice {
    public static final boolean DEBUG = Debug.debug("StereoDevice");
    public static final boolean DUMP_DATA = Debug.isPropertyDefined("jogl.debug.StereoDevice.DumpData", true);

    /**
     * Sensor Bit: Orientation tracking
     */
    public static final int SENSOR_ORIENTATION    = 1 << 0;

    /**
     * Sensor Bit: Yaw correction
     */
    public static final int SENSOR_YAW_CORRECTION = 1 << 1;

    /**
     * Sensor Bit: Positional tracking
     */
    public static final int SENSOR_POSITION = 1 << 2;

    /** Return the factory used to create this device. */
    public StereoDeviceFactory getFactory();

    /**
     * Disposes this {@link StereoDevice}, if {@link #isValid() valid}.
     * <p>
     * Implementation shall {@link #stopSensors() stop sensors} and free all resources.
     * </p>
     */
    public void dispose();

    /**
     * Returns {@code true}, if instance is created and not {@link #dispose() disposed},
     * otherwise returns {@code false}.
     */
    public boolean isValid();

    /**
     * If operation within a device spanning virtual desktop,
     * returns the device position.
     * <p>
     * Otherwise simply 0/0.
     * </p>
     */
    public PointImmutable getPosition();

    /**
     * Returns the required surface size in pixel
     * in target space.
     */
    public DimensionImmutable getSurfaceSize();

    /**
     * Returns the CCW rotation as required by this display device.
     */
    public int getRequiredRotation();

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

    /**
     * Returns the {@link LocationSensorParameter} of the device
     * if {@link #SENSOR_POSITION} is {@link #getSupportedSensorBits() supported},
     * otherwise returns {@code null}.
     */
    public LocationSensorParameter getLocationSensorParams();


    /**
     * Sets the location sensor's origin of this device to the current position.
     * <p>
     * In case {@link #SENSOR_POSITION} is not {@link #getSupportedSensorBits() supported},
     * this method is a no-op.
     * </p>
     */
    public void resetLocationSensorOrigin();

    /**
     * Start desired and required sensors. Returns true if action was successful, otherwise false.
     * <p>
     * Method fails if required sensors are not {@link #getSupportedSensorBits() supported}.
     * </p>
     * @param desiredSensorBits the desired optional sensors
     * @param requiredSensorBits the required sensors
     * @see #stopSensors()
     * @see #getSensorsStarted()
     * @see #getSupportedSensorBits()
     * @see #getEnabledSensorBits()
     */
    public boolean startSensors(int desiredSensorBits, int requiredSensorBits);

    /**
     * Stop sensors. Returns true if action was successful, otherwise false.
     * @see #startSensors(int, int)
     * @see #getSensorsStarted()
     * @see #getSupportedSensorBits()
     * @see #getEnabledSensorBits()
     */
    public boolean stopSensors();

    /**
     * Return true if sensors have been started, false otherwise.
     * @see #startSensors(int, int)
     * @see #stopSensors()
     * @see #getSupportedSensorBits()
     * @see #getEnabledSensorBits()
     */
    public boolean getSensorsStarted();

    /**
     * Returns the supported sensor capability bits, e.g. {@link #SENSOR_ORIENTATION}, {@link #SENSOR_POSITION}
     * of the {@link StereoDevice}.
     * @see #startSensors(int, int)
     * @see #stopSensors()
     * @see #getSensorsStarted()
     * @see #getEnabledSensorBits()
     */
    public int getSupportedSensorBits();

    /**
     * Returns the actual used sensor capability bits, e.g. {@link #SENSOR_ORIENTATION}, {@link #SENSOR_POSITION}
     * in case the {@link #getSupportedSensorBits() device supports} them and if they are enabled.
     * @see #startSensors(int, int)
     * @see #stopSensors()
     * @see #getSensorsStarted()
     * @see #getSupportedSensorBits()
     */
    public int getEnabledSensorBits();

    /**
     * Returns an array of the preferred eye rendering order.
     * The array length reflects the supported eye count.
     * <p>
     * Monoscopic devices only support one eye, where stereoscopic device two eyes.
     * </p>
     */
    public int[] getEyeRenderOrder();

    /**
     * Returns the supported distortion compensation of the {@link StereoDeviceRenderer},
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
