/*
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
package com.jogamp.opengl.util.awt;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import java.awt.Dimension;


/**
 * Utility for making OpenGL canvases.
 */
class GLCanvasFactory {

    // Whether or not to make double-buffered canvases
    private boolean doubleBuffered = true;

    // Width of canvases that are made
    private int width = 512;

    // Height of canvases that are made
    private int height = 512;

    /**
     * Changes whether to make double-buffered canvases.
     *
     * @param doubleBuffered <tt>true</tt> to make double-buffered canvases
     */
    public void setDoubleBuffered(final boolean doubleBuffered) {
        this.doubleBuffered = doubleBuffered;
    }

    /**
     * Changes the width of canvases that are made.
     *
     * @param width Width of canvases that are made
     * @throws IllegalArgumentException if width is zero or negative
     */
    public void setWidth(final int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("Width is zero or negative!");
        }
        this.width = width;
    }

    /**
     * Changes the height of canvases that are made.
     *
     * @param height Height of canvases that are made
     * @throws IllegalArgumentException if height is zero or negative
     */
    public void setHeight(final int height) {
        if (height <= 0) {
            throw new IllegalArgumentException("Height is zero or negative!");
        }
        this.height = height;
    }

    /**
     * Creates a canvas with profile options.
     *
     * @param profile GLProfile to use, e.g. "GL2" or "GL3"
     * @return Canvas supporting requested profile
     * @throws NullPointerException if profile is <tt>null</tt>
     */
    public GLCanvas createGLCanvas(final String profile) {

        // Make capabilities
        final GLProfile glProfile = GLProfile.get(profile);
        final GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setDoubleBuffered(doubleBuffered);

        // Make canvas
        final GLCanvas canvas = new GLCanvas(glCapabilities);
        canvas.setPreferredSize(new Dimension(width, height));
        return canvas;
    }
}
