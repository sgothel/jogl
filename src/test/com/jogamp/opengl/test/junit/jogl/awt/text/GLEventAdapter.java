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
package com.jogamp.opengl.test.junit.jogl.awt.text;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;


/**
 * Skeletal implementation of {@link GLEventListener}.
 */
abstract class GLEventAdapter implements GLEventListener  {

    /**
     * Handles initialize events.
     *
     * @param drawable Surface being initialized
     * @throws NullPointerException if drawable is <tt>null</tt>
     */
    @Override
    public void init(final GLAutoDrawable drawable) {
        // pass
    }

    /**
     * Handles display events.
     *
     * @param drawable Surface being drawn to
     * @throws NullPointerException if drawable is <tt>null</tt>
     */
    @Override
    public void display(final GLAutoDrawable drawable) {
        // pass
    }

    /**
     * Handles dispose events.
     *
     * @param drawable Surface being disposed of
     * @throws NullPointerException if drawable is <tt>null</tt>
     */
    @Override
    public void dispose(final GLAutoDrawable drawable) {
        // pass
    }

    /**
     * Handles window resizing events and invokes {@link #doReshape}.
     *
     * @param drawable Surface being reshaped
     * @param x Left side of viewport
     * @param y Top of viewport
     * @param width Width of viewport
     * @param height Height of viewport
     * @throws NullPointerException if drawable is <tt>null</tt>
     */
    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, int y, final int width, int height) {
        // pass
    }
}
