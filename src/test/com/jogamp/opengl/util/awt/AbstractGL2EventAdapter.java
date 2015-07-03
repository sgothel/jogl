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

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;


/**
 * Skeletal implementation of {@link GLEventListener} for OpenGL 2.
 */
abstract class AbstractGL2EventAdapter extends GLEventAdapter {

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public final void display(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        doDisplay(gl);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public final void dispose(final GLAutoDrawable drawable) {
        final GL2 gl = drawable.getGL().getGL2();
        doDispose(gl);
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public final void reshape(final GLAutoDrawable drawable,
                              final int x, final int y,
                              final int width, final int height) {
        final GL2 gl = drawable.getGL().getGL2();
        doReshape(gl, x, y, width, height);
    }

    //-----------------------------------------------------------------
    // Hooks
    //

    /**
     * Handles initialize events from a drawable.
     *
     * @param gl OpenGL context from drawable
     * @throws NullPointerException if context is <tt>null</tt>
     */
    protected void doInit(GL2 gl) {
        // pass
    }

    /**
     * Handles display events from a drawable.
     *
     * @param gl OpenGL context from drawable
     * @throws NullPointerException if context is <tt>null</tt>
     */
    protected void doDisplay(GL2 gl) {
        // pass
    }

    /**
     * Handles dispose events from a drawable.
     *
     * @param gl OpenGL context from drawable
     * @throws NullPointerException if context is <tt>null</tt>
     */
    protected void doDispose(GL2 gl) {
        // pass
    }

    /**
     * Handles reshape events from a drawable.
     *
     * @param gl OpenGL context from drawable
     * @param x Left side of viewport
     * @param y Top of viewport
     * @param width Width of viewport
     * @param height Height of viewport
     * @throws NullPointerException if context is <tt>null</tt>
     */
    protected void doReshape(GL2 gl, int x, int y, int width, int height) {
        // pass
    }
}
