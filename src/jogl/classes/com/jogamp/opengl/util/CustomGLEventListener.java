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
package com.jogamp.opengl.util;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

/**
 * Extended {@link GLEventListener} interface
 * supporting more fine grained control over the implementation.
 */
public interface CustomGLEventListener extends GLEventListener {
    /**
     * {@link #display(GLAutoDrawable, int) display flag}: Repeat last produced image.
     * <p>
     * While a repeated frame shall produce the same artifacts as the last <code>display</code> call,
     * e.g. not change animated objects, it shall reflect the {@link #setProjectionModelview(GLAutoDrawable, float[], float[]) current matrix}.
     * </p>
     */
    public static final int DISPLAY_REPEAT    = 1 << 0;

    /**
     * {@link #display(GLAutoDrawable, int) display flag}: Do not clear any target buffer, e.g. color-, depth- or stencil-buffers.
     */
    public static final int DISPLAY_DONTCLEAR = 1 << 1;

    /**
     * Extended {@link #display(GLAutoDrawable) display} method,
     * allowing to pass a display flag, e.g. {@link #DISPLAY_REPEAT} or {@link #DISPLAY_DONTCLEAR}.
     * <p>
     * Method is usually called by a custom rendering loop,
     * e.g. for manual stereo rendering or the like.
     * </p>
     * @param drawable
     * @param flags
     */
    public void display(final GLAutoDrawable drawable, final int flags);
}
