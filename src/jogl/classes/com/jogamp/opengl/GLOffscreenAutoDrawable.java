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

/**
 * Platform-independent {@link GLAutoDrawable} specialization,
 * exposing offscreen functionality.
 * <p>
 * This class distinguishes itself from {@link GLAutoDrawable}
 * with it's {@link #setSurfaceSize(int, int)} functionality.
 * </p>
 * <p>
 * <a name="contextSharing"><h5>OpenGL Context Sharing</h5></a>
 * To share a {@link GLContext} see the following note in the documentation overview:
 * <a href="../../../overview-summary.html#SHARING">context sharing</a>
 * as well as {@link GLSharedContextSetter}.
 * </p>
 */
public interface GLOffscreenAutoDrawable extends GLAutoDrawable, GLSharedContextSetter {

    /**
     * Resize this {@link GLAutoDrawable}'s surface
     * @param newWidth new width in pixel units
     * @param newHeight new height in pixel units
     * @throws NativeWindowException in case the surface could no be locked
     * @throws GLException in case of an error during the resize operation
     */
    void setSurfaceSize(int newWidth, int newHeight) throws NativeWindowException, GLException;

    /**
     * Set the upstream UI toolkit object.
     * @see #getUpstreamWidget()
     */
    void setUpstreamWidget(Object newUpstreamWidget);

    /** {@link FBObject} based {@link GLOffscreenAutoDrawable} specialization */
    public interface FBO extends GLOffscreenAutoDrawable, GLFBODrawable {
    }
}
