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

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.CustomGLEventListener;

/**
 * Extended {@link GLEventListener} and {@link CustomGLEventListener} interface
 * supporting stereoscopic client rendering.
 */
public interface StereoGLEventListener extends CustomGLEventListener {
    /**
     * Stereo capable specialization of {@link #reshape(GLAutoDrawable, int, int, int, int)}
     * for one {@link StereoDeviceRenderer.Eye}.
     * <p>
     * Called by the stereo renderer before each {@link #display(GLAutoDrawable)}
     * or {@link #display(GLAutoDrawable, int)} call.
     * </p>
     * <p>
     * The client can update it's viewport associated data
     * and view volume of the window appropriately.
     * </p>
     * <p>
     * The client shall also update it's projection- and modelview matrices according
     * to the given {@link EyeParameter} and {@link ViewerPose}.
     * </p>
     * <p>
     * For efficiency the GL viewport has already been updated
     * via <code>glViewport(x, y, width, height)</code> when this method is called.
     * </p>
     *
     * @param drawable the triggering {@link GLAutoDrawable}
     * @param x viewport x-coord in pixel units
     * @param y viewport y-coord in pixel units
     * @param width viewport width in pixel units
     * @param height viewport height in pixel units
     * @param eyeParam constant eye parameter, i.e. FOV and IPD
     * @param viewerPose current viewer position and orientation
     * @see FloatUtil#makePerspective(float[], int, boolean, com.jogamp.opengl.math.FloatUtil.FovHVHalves, float, float)
     */
    public void reshapeForEye(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height,
                              final EyeParameter eyeParam, final ViewerPose viewerPose);


}
