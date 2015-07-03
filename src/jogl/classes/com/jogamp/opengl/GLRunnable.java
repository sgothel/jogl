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

package com.jogamp.opengl;

/**
 * <p>
 * Declares a one-shot OpenGL command usable for injection
 * via {@link GLAutoDrawable#invoke(boolean, com.jogamp.opengl.GLRunnable)}.<br>
 * {@link GLAutoDrawable} executes the GLRunnables within it's {@link GLAutoDrawable#display() display()}
 * method after all registered {@link GLEventListener}s
 * {@link GLEventListener#display(GLAutoDrawable) display(GLAutoDrawable)}
 * methods has been called.
 * </p>
 * <p>
 * The OpenGL context is current while executing the GLRunnable.
 * </p>
 * <p>
 * This might be useful to inject OpenGL commands from an I/O event listener.
 * </p>
 */
public interface GLRunnable {
    /**
     * @param drawable the associated drawable and current context for this call
     * @return true if the GL [back] framebuffer remains intact by this runnable, otherwise false.
     *         If returning false {@link GLAutoDrawable} will call
     *         {@link GLEventListener#display(GLAutoDrawable) display(GLAutoDrawable)}
     *         of all registered {@link GLEventListener}s once more.
     * @see GLRunnable
     */
    boolean run(GLAutoDrawable drawable);
}

