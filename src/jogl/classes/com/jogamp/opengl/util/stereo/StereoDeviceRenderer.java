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
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.opengl.GL;

import com.jogamp.opengl.math.FovHVHalves;

/**
 * Stereoscopic device rendering interface.
 * <p>
 * The following pseudo-code describes how to implement a renderer
 * using a {@link StereoDeviceRenderer}.
 * See {@link StereoClientRenderer} which implements the following:
 * <ul>
 *   <li>device.{@link #beginFrame(GL)}</li>
 *   <li>For both eyes:<ul>
 *     <li>device.{@link #updateViewerPose(int)}</li>
 *     <li>if device.{@link #ppAvailable()}: Set the render target, e.g. FBO</li>
 *     <li>Set the viewport using {@link Eye#getViewport()}</li>
 *     <li>{@link StereoGLEventListener#reshapeForEye(com.jogamp.opengl.GLAutoDrawable, int, int, int, int, EyeParameter, ViewerPose) upstream.reshapeEye(..)}</li>
 *     <li>{@link StereoGLEventListener#display(com.jogamp.opengl.GLAutoDrawable, int) upstream.display(..)}.</li>
 *   </ul></li>
 *   <li>Reset the viewport</li>
 *   <li>If device.{@link #ppAvailable()}:<ul>
 *     <li>device.{@link #ppBegin(GL)}</li>
 *     <li>Use render target, e.g. FBO's texture</li>
 *     <li>device.{@link #ppBothEyes(GL)} or device.{@link #ppOneEye(GL, int)} for both eyes</li>
 *     <li>device.{@link #ppEnd(GL)}</li>
 *   </ul></li>
 *   <li>device.{@link #endFrame(GL)}</li>
 * </ul>
 * </p>
 * <a name="asymFOVRendering"><h5>Correct {@link FovHVHalves Asymmetric FOV} Rendering</h5></a>
 * <p>
 * The {@link StereoClientRenderer} shall render both images for each eye correctly <i>Off-axis</i>
 * utilizing an asymmetric camera frustum, i.e. by using {@link StereoDevice StereoDevice}'s {@link StereoDevice#getDefaultFOV() default} {@link FovHVHalves}.<br>
 *
 * Some references:
 * <ul>
 *   <li><a href="https://en.wikipedia.org/wiki/Binocular_vision">Wiki: Binocular Vision</a></li>
 *   <li><a href="http://paulbourke.net/stereographics/stereorender/">Paul Burke: Stereo Graphics - Stereo Renderer</a></li>
 *   <li><a href="https://en.wikipedia.org/wiki/Distortion_%28optics%29">Wiki: Distortion (Optics)</a></li>
 * </ul>
 * </p>
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
     *                 {@link ViewerPose} movement to reduce latency.
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
    }

    /**
     * Returns the {@link Eye} instance for the denoted <code>eyeNum</code>.
     */
    public Eye getEye(final int eyeNum);

    /**
     * Updates the {@link ViewerPose} and returns it.
     */
    public ViewerPose updateViewerPose();

    /**
     * Returns the last {@link ViewerPose}.
     */
    public ViewerPose getLastViewerPose();

    /**
     * Returns used distortion compensation bits, e.g. {@link #DISTORTION_BARREL},
     * in case the stereoscopic display requires such, i.e. in case lenses are utilized.
     * <p>
     * Distortion requires {@link #ppAvailable() post-processing}.
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
     * or {@link #ppAvailable() post-processing} is utilized to merge {@link #getTextureCount() textures}
     * to a <i>side-by-side</i> configuration.
     * </p>
     */
    public boolean usesSideBySideStereo();

    /**
     * Returns the surface size for each eye's a single image in pixel units.
     */
    public DimensionImmutable[] getEyeSurfaceSize();

    /**
     * Returns the total surface size required for the complete images in pixel units.
     * <p>
     * If {@link #usesSideBySideStereo()} the total size spans over both {@link #getEyeSurfaceSize()}, side-by-side.
     * </p>
     * <p>
     * Otherwise the size is equal to {@link #getEyeSurfaceSize()}.
     * </p>
     */
    public DimensionImmutable getTotalSurfaceSize();

    /**
     * Returns the used texture-image count for post-processing, see {@link #ppAvailable()}.
     * <p>
     * In case the renderer does not support multiple textures for post-processing,
     * or no post-processing at all, method returns zero despite the request
     * from {@link StereoDevice#createRenderer(int, int, float[], com.jogamp.opengl.math.FovHVHalves[], float)}.
     * </p>
     */
    public int getTextureCount();

    /** Returns the desired texture-image unit for post-processing, see {@link #ppAvailable()}. */
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
     * Returns <code>true</code> if stereoscopic post-processing is required and available,
     * otherwise <code>false</code>.
     * <p>
     * Stereoscopic post-processing is available if:
     * <ul>
     *   <li>one of the <i>distortion</i> bits are set, see {@link #getDistortionBits()}</li>
     * </ul>
     * </p>
     * <p>
     * If stereoscopic post-processing is used
     * the following post-processing methods must be called to before {@link #endFrame()}:
     * <ul>
     *   <li>{@link #ppBegin(GL)}</li>
     *   <li>{@link #ppOneEye(GL, int)} for both eyes</li>
     *   <li>{@link #ppEnd(GL)}</li>
     * </ul>
     * </p>
     */
    public boolean ppAvailable();

    /**
     * Begin stereoscopic post-processing, see {@link #ppAvailable()}.
     * <p>
     * {@link #updateViewerPose(int)} for both eyes must be called upfront
     * when rendering upstream {@link StereoGLEventListener}.
     * </p>
     *
     * @param gl
     */
    public void ppBegin(final GL gl);

    /**
     * Performs stereoscopic post-processing for one eye, see {@link #ppAvailable()}.
     * @param gl
     * @param eyeNum
     */
    public void ppOneEye(final GL gl, final int eyeNum);

    /**
     * End stereoscopic post-processing, see {@link #ppAvailable()}.
     * @param gl
     */
    public void ppEnd(final GL gl);

}
