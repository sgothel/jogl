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
import javax.media.nativewindow.util.RectangleImmutable;
import javax.media.opengl.GL;

/**
 * Stereoscopic device rendering interface.
 * <p>
 * The following pseudo-code describes how to implement a renderer
 * using a {@link StereoDeviceRenderer}.
 * See {@link StereoClientRenderer} which implements the following:
 * <ul>
 *   <li>device.{@link #beginFrame(GL)}</li>
 *   <li>For both eyes:<ul>
 *     <li>device.{@link #updateEyePose(int)}</li>
 *     <li>if device.{@link #ppRequired()}: Set the render target, e.g. FBO</li>
 *     <li>Set the viewport using {@link Eye#getViewport()}</li>
 *     <li>{@link StereoGLEventListener#reshapeForEye(javax.media.opengl.GLAutoDrawable, int, int, int, int, EyeParameter, EyePose) upstream.reshapeEye(..)}</li>
 *     <li>{@link StereoGLEventListener#display(javax.media.opengl.GLAutoDrawable, int) upstream.display(..)}.</li>
 *   </ul></li>
 *   <li>Reset the viewport</li>
 *   <li>If device.{@link #ppRequired()}:<ul>
 *     <li>device.{@link #ppBegin(GL)}</li>
 *     <li>Use render target, e.g. FBO's texture</li>
 *     <li>device.{@link #ppBothEyes(GL)} or device.{@link #ppOneEye(GL, int)} for both eyes</li>
 *     <li>device.{@link #ppEnd(GL)}</li>
 *   </ul></li>
 *   <li>device.{@link #endFrame(GL)}</li>
 * <ul>
 */
public interface StereoDeviceRenderer {
    /**
     * Distortion Bit: Barrel distortion compensating lens pincushion distortion
     */
    public static final int DISTORTION_BARREL    = 1 << 0;

    /**
     * Distortion Bit: Chromatic distortion compensating lens chromatic aberration.
     */
    public static final int DISTORTION_CHROMATIC = 1 << 1;

    /**
     * Distortion Bit: Vignette distortion compensating lens chromatic aberration.
     */
    public static final int DISTORTION_VIGNETTE  = 1 << 2;

    /**
     * Distortion Bit: Timewarp distortion technique to predict
     *                 {@link EyePose} movement to reduce latency.
     * <p>
     * FIXME: Explanation needs refinement!
     * </p>
     */
    public static final int DISTORTION_TIMEWARP  = 1 << 3;


    /** Returns the {@link StereoDevice} of this {@link StereoDeviceRenderer} instance. */
    public StereoDevice getDevice();

    /**
     * Interface describing one eye of the stereoscopic device,
     * see {@link StereoDeviceRenderer#getEye(int)}.
     */
    public static interface Eye {
        /**
         * Returns the viewport for this eye.
         */
        public RectangleImmutable getViewport();
        /**
         * Returns the {@link EyeParameter} of this eye.
         */
        public EyeParameter getEyeParameter();
        /**
         * Returns the last {@link EyePose} of this eye.
         */
        public EyePose getLastEyePose();
    }

    /**
     * Returns the {@link Eye} instance for the denoted <code>eyeNum</code>.
     */
    public Eye getEye(final int eyeNum);

    /**
     * Updates the {@link Eye#getLastEyePose()}
     * for the denoted <code>eyeNum</code>.
     */
    public EyePose updateEyePose(final int eyeNum);

    /**
     * Returns distortion compensation bits, e.g. {@link #DISTORTION_BARREL},
     * in case the stereoscopic display requires such, i.e. in case lenses are utilized.
     * <p>
     * Distortion requires {@link #ppRequired() post-processing}.
     * </p>
     */
    public int getDistortionBits();

    /**
     * Method returns <code>true</code> if using <i>side-by-side</i> (SBS)
     * stereoscopic images, otherwise <code>false</code>.
     * <p>
     * SBS requires that both eye's images are presented
     * <i>side-by-side</i> in the final framebuffer.
     * </p>
     * <p>
     * Either the renderer presents the images <i>side-by-side</i> according to the {@link Eye#getViewport() eye's viewport},
     * or {@link #ppRequired() post-processing} is utilized to merge {@link #getTextureCount() textures}
     * to a <i>side-by-side</i> configuration.
     * </p>
     */
    public boolean usesSideBySideStereo();

    /**
     * Returns the unified surface size of one eye's a single image in pixel units.
     */
    public DimensionImmutable getSingleSurfaceSize();

    /**
     * Returns the total surface size required for the complete images in pixel units.
     * <p>
     * If {@link #usesSideBySideStereo()} the total size spans over both {@link #getSingleSurfaceSize()}, side-by-side.
     * </p>
     * <p>
     * Otherwise the size is equal to {@link #getSingleSurfaceSize()}.
     * </p>
     */
    public DimensionImmutable getTotalSurfaceSize();

    /**
     * Returns the used texture-image count for post-processing, see {@link #ppRequired()}.
     * <p>
     * In case the renderer does not support multiple textures for post-processing,
     * or no post-processing at all, method returns zero despite the request
     * from {@link StereoDevice#createRenderer(int, int, float[], com.jogamp.opengl.math.FovHVHalves[], float)}.
     * </p>
     */
    public int getTextureCount();

    /** Returns the desired texture-image unit for post-processing, see {@link #ppRequired()}. */
    public int getTextureUnit();

    /** Initialize OpenGL related resources */
    public void init(final GL gl);

    /** Release all OpenGL related resources */
    public void dispose(final GL gl);

    /** Notifying that a new frame is about to start. */
    public void beginFrame(final GL gl);

    /** Notifying that the frame has been rendered completely. */
    public void endFrame(final GL gl);

    /**
     * Returns <code>true</code> if stereoscopic post-processing is required,
     * otherwise <code>false</code>.
     * <p>
     * Stereoscopic post-processing is usually required if:
     * <ul>
     *   <li>one of the <i>distortion</i> modes are set, i.e. {@link #usesBarrelDistortion()}</li>
     *   <li>texture-images are being used, see {@link #getTextureCount()}</li>
     * </ul>
     * </p>
     * <p>
     * If stereoscopic post-processing is used
     * the following post-processing methods must be called to before {@link #endFrame()}:
     * <ul>
     *   <li>{@link #ppBegin(GL)}</li>
     *   <li>{@link #ppBothEyes(GL)} or {@link #ppOneEye(GL, int)} for both eyes</li>
     *   <li>{@link #ppEnd(GL)}</li>
     * </ul>
     * </p>
     */
    public boolean ppRequired();

    /**
     * Begin stereoscopic post-processing, see {@link #ppRequired()}.
     * <p>
     * {@link #updateEyePose(int)} for both eyes must be called upfront
     * when rendering upstream {@link StereoGLEventListener}.
     * </p>
     *
     * @param gl
     */
    public void ppBegin(final GL gl);

    /**
     * Performs stereoscopic post-processing for both eyes, see {@link #ppRequired()}.
     * @param gl
     */
    public void ppBothEyes(final GL gl);

    /**
     * Performs stereoscopic post-processing for one eye, see {@link #ppRequired()}.
     * @param gl
     * @param eyeNum
     */
    public void ppOneEye(final GL gl, final int eyeNum);

    /**
     * End stereoscopic post-processing, see {@link #ppRequired()}.
     * @param gl
     */
    public void ppEnd(final GL gl);

}
