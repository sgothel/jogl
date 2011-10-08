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

package javax.media.opengl;

import javax.media.nativewindow.CapabilitiesImmutable;

/**
 * Specifies an immutable set of OpenGL capabilities.<br>
 *
 * @see javax.media.opengl.GLCapabilities
 * @see javax.media.nativewindow.CapabilitiesImmutable
 */
public interface GLCapabilitiesImmutable extends CapabilitiesImmutable {
    /** 
     * One of the platform's default sample extension 
     * <code>EGL.EGL_SAMPLES, GLX.GLX_SAMPLES, WGLExt.WGL_SAMPLES_ARB</code>
     * if available, or any other <i>known</i> fallback one, ie <code>EGLExt.EGL_COVERAGE_SAMPLES_NV</code>
     */
    public static final String DEFAULT_SAMPLE_EXTENSION = "default" ;
    
    /**
     * Returns the GL profile you desire or used by the drawable.
     */
    GLProfile getGLProfile();

    /**
     * Returns the number of bits requested for the accumulation
     * buffer's alpha component. On some systems only the accumulation
     * buffer depth, which is the sum of the red, green, and blue bits,
     * is considered.
     */
    int getAccumAlphaBits();

    /**
     * Returns the number of bits requested for the accumulation
     * buffer's blue component. On some systems only the accumulation
     * buffer depth, which is the sum of the red, green, and blue bits,
     * is considered.
     */
    int getAccumBlueBits();

    /**
     * Returns the number of bits requested for the accumulation
     * buffer's green component. On some systems only the accumulation
     * buffer depth, which is the sum of the red, green, and blue bits,
     * is considered.
     */
    int getAccumGreenBits();

    /**
     * Returns the number of bits requested for the accumulation
     * buffer's red component. On some systems only the accumulation
     * buffer depth, which is the sum of the red, green, and blue bits,
     * is considered.
     */
    int getAccumRedBits();

    /**
     * Returns the number of bits requested for the depth buffer.
     */
    int getDepthBits();

    /**
     * Indicates whether double-buffering is enabled.
     */
    boolean getDoubleBuffered();

    /**
     * Indicates whether hardware acceleration is enabled.
     */
    boolean getHardwareAccelerated();

    /**
     * Returns the used extension for full-scene antialiasing
     * (FSAA), default is {@link #DEFAULT_SAMPLE_EXTENSION}.
     */
    String getSampleExtension();
    
    /**
     * Returns whether sample buffers for full-scene antialiasing
     * (FSAA) should be allocated for this drawable. Defaults to
     * false.
     */
    boolean getSampleBuffers();

    /**
     * Returns the number of sample buffers to be allocated if sample
     * buffers are enabled. Defaults to 2.
     */
    int getNumSamples();

    /**
     * For pbuffers only, returns whether floating-point buffers should
     * be used if available. Defaults to false.
     */
    boolean getPbufferFloatingPointBuffers();

    /**
     * For pbuffers only, returns whether the render-to-texture
     * extension should be used if available.  Defaults to false.
     */
    boolean getPbufferRenderToTexture();

    /**
     * For pbuffers only, returns whether the render-to-texture
     * extension should be used. Defaults to false.
     */
    boolean getPbufferRenderToTextureRectangle();

    /**
     * Returns the number of bits requested for the stencil buffer.
     */
    int getStencilBits();

    /**
     * Indicates whether stereo is enabled.
     */
    boolean getStereo();

    /**
     * Indicates whether pbuffer is used/requested.
     */
    boolean isPBuffer();

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

    @Override
    String toString();
}
