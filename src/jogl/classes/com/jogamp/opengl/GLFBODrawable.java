/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl;

import com.jogamp.nativewindow.NativeWindowException;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Colorbuffer;
import com.jogamp.opengl.FBObject.ColorAttachment;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.GLRendererQuirks;

/**
 * Platform-independent {@link GLDrawable} specialization,
 * exposing {@link FBObject} functionality.
 *
 * <p>
 * A {@link GLFBODrawable} is uninitialized until a {@link GLContext} is bound
 * and made current the first time, hence only then it's capabilities <i>fully</i> reflect expectations,
 * i.e. color, depth, stencil and MSAA bits will be <i>valid</i> only after the first {@link GLContext#makeCurrent() makeCurrent()} call.
 * On-/offscreen bits are <i>valid</i> after {@link #setRealized(boolean) setRealized(true)}.
 * </p>
 *
 * <p>
 * MSAA is used if {@link GLCapabilitiesImmutable#getNumSamples() requested}.
 * </p>
 * <p>
 * Double buffering is used if {@link GLCapabilitiesImmutable#getDoubleBuffered() requested}.
 * </p>
 * <p>
 * In MSAA mode, it always uses the implicit 2nd {@link FBObject framebuffer} {@link FBObject#getSamplingSinkFBO() sink}.
 * Hence double buffering is always the case w/ MSAA.
 * </p>
 * <p>
 * In non MSAA a second explicit {@link FBObject framebuffer} is being used.
 * This method allows compliance w/ the spec, i.e. read and draw framebuffer selection
 * and double buffer usage for e.g. {@link GL#glReadPixels(int, int, int, int, int, int, java.nio.Buffer) glReadPixels(..)}.
 * This method also allows usage of both textures seperately.
 * </p>
 * <p>
 * It would be possible to implement double buffering simply using
 * {@link Colorbuffer}s with one {@link FBObject framebuffer}.
 * This would require mode selection and hence complicate the API. Besides, it would
 * not support differentiation of read and write framebuffer and hence not be spec compliant.
 * </p>
 * <p>
 * Actual swapping of the {@link Colorbuffer}s and/or {@link FBObject framebuffer}
 * is performed either in the {@link jogamp.opengl.GLContextImpl#contextMadeCurrent(boolean) context current hook}
 * or when {@link jogamp.opengl.GLDrawableImpl#swapBuffersImpl(boolean) swapping buffers}, whatever comes first.
 * </p>
 */
public interface GLFBODrawable extends GLDrawable {
    // public enum DoubleBufferMode { NONE, TEXTURE, FBO }; // TODO: Add or remove TEXTURE (only) DoubleBufferMode support

    /** FBO Mode Bit: Use a {@link TextureAttachment} for the {@link #getColorbuffer(int) render colorbuffer}, see {@link #setFBOMode(int)}. */
    public static final int FBOMODE_USE_TEXTURE = 1 << 0;

    /**
     * @return <code>true</code> if initialized, i.e. a {@link GLContext} is bound and made current once, otherwise <code>false</code>.
     */
    public boolean isInitialized();

    /**
     * Set the FBO mode bits used for FBO creation.
     * <p>
     * Default value is: {@link #FBOMODE_USE_TEXTURE}.
     * </p>
     * <p>
     * If {@link GLRendererQuirks#BuggyColorRenderbuffer} is set,
     * {@link #FBOMODE_USE_TEXTURE} is always added at initialization.
     * </p>
     *
     * @param modeBits custom FBO mode bits like {@link #FBOMODE_USE_TEXTURE}.
     * @throws IllegalStateException if already initialized, see {@link #isInitialized()}.
     */
    void setFBOMode(final int modeBits) throws IllegalStateException;

    /**
     * @return the used FBO mode bits, mutable via {@link #setFBOMode(int)}
     */
    int getFBOMode();

    /**
     * Notify this instance about upstream size change
     * to reconfigure the {@link FBObject}.
     * @param gl GL context object bound to this drawable, will be made current during operation.
     *           A prev. current context will be make current after operation.
     * @throws GLException if resize operation failed
     */
    void resetSize(final GL gl) throws GLException;

    /**
     * @return the used texture unit
     */
    int getTextureUnit();

    /**
     *
     * @param unit the texture unit to be used
     */
    void setTextureUnit(final int unit);

    /**
     * Set the number of sample buffers if using MSAA
     *
     * @param gl GL context object bound to this drawable, will be made current during operation.
     *           A prev. current context will be make current after operation.
     * @param newSamples new sample size
     * @throws GLException if resetting the FBO failed
     */
    void setNumSamples(final GL gl, final int newSamples) throws GLException;

    /**
     * @return the number of sample buffers if using MSAA, otherwise 0
     */
    int getNumSamples();

    /**
     * Sets the number of buffers (FBO) being used if using {@link GLCapabilities#getDoubleBuffered() double buffering}.
     * <p>
     * If {@link GLCapabilities#getDoubleBuffered() double buffering} is not chosen, this is a NOP.
     * </p>
     * <p>
     * Must be called before {@link #isInitialized() initialization}, otherwise an exception is thrown.
     * </p>
     * @return the new number of buffers (FBO) used, maybe different than the requested <code>bufferCount</code> (see above)
     * @throws IllegalStateException if already initialized, see {@link #isInitialized()}.
     */
    int setNumBuffers(final int bufferCount) throws IllegalStateException, GLException;

    /**
     * @return the number of buffers (FBO) being used. 1 if not using {@link GLCapabilities#getDoubleBuffered() double buffering},
     * otherwise &ge; 2, depending on {@link #setNumBuffers(int)}.
     */
    int getNumBuffers();

    /**
     * @return the used {@link DoubleBufferMode}
     */
    // DoubleBufferMode getDoubleBufferMode(); // TODO: Add or remove TEXTURE (only) DoubleBufferMode support

    /**
     * Sets the {@link DoubleBufferMode}. Must be called before {@link #isInitialized() initialization},
     * otherwise an exception is thrown.
     * <p>
     * This call has no effect is MSAA is selected, since MSAA always forces the mode to {@link DoubleBufferMode#FBO FBO}.
     * Also setting the mode to {@link DoubleBufferMode#NONE NONE} where double buffering is {@link GLCapabilitiesImmutable#getDoubleBuffered() requested}
     * or setting a double buffering mode w/o {@link GLCapabilitiesImmutable#getDoubleBuffered() request} will be ignored.
     * </p>
     * <p>
     * Since {@link DoubleBufferMode#TEXTURE TEXTURE} mode is currently not implemented, this method has no effect.
     * </p>
     * @throws GLException if already initialized, see {@link #isInitialized()}.
     */
    // void setDoubleBufferMode(DoubleBufferMode mode) throws GLException; // TODO: Add or remove TEXTURE (only) DoubleBufferMode support

    /**
     * If MSAA is being used and {@link GL#GL_FRONT} is requested,
     * the internal {@link FBObject} {@link FBObject#getSamplingSinkFBO() sample sink} is being returned.
     *
     * @param bufferName {@link GL#GL_FRONT} and {@link GL#GL_BACK} are valid buffer names
     * @return the named {@link FBObject}
     * @throws IllegalArgumentException if an illegal buffer name is being used
     */
    FBObject getFBObject(final int bufferName) throws IllegalArgumentException;

    /**
     * Returns the named {@link Colorbuffer} instance.
     * <p>
     * If MSAA is being used, only the {@link GL#GL_FRONT} buffer is accessible
     * and an exception is being thrown if {@link GL#GL_BACK} is being requested.
     * </p>
     * <p>
     * Depending on the {@link #setFBOMode(int) fbo mode} the resulting {@link Colorbuffer}
     * is either a {@link TextureAttachment} if {@link #FBOMODE_USE_TEXTURE} is set,
     * otherwise a {@link ColorAttachment}.
     * See {@link Colorbuffer#isTextureAttachment()}.
     * </p>
     * @param bufferName {@link GL#GL_FRONT} and {@link GL#GL_BACK} are valid buffer names
     * @return the named {@link Colorbuffer}
     * @throws IllegalArgumentException if using MSAA and {@link GL#GL_BACK} is requested or an illegal buffer name is being used
     */
    Colorbuffer getColorbuffer(final int bufferName) throws IllegalArgumentException;

    /** Resizeable {@link GLFBODrawable} specialization */
    public interface Resizeable extends GLFBODrawable {
        /**
         * Resize this {@link GLFBODrawable}'s surface.
         * <p>
         * This drawable is being locked during operation.
         * </p>
         * @param context the {@link GLContext} bound to this drawable, will be made current during operation
         *                A prev. current context will be make current after operation.
         * @param newWidth new width in pixel units
         * @param newHeight new width in pixel units
         * @throws NativeWindowException in case the surface could no be locked
         * @throws GLException in case an error during the resize operation occurred
         */
        void setSurfaceSize(GLContext context, int newWidth, int newHeight) throws NativeWindowException, GLException;
    }
}
