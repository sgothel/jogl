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

/**
 * <p>
 * Declares one-shot OpenGL commands usable for injection into a {@link GLAutoDrawable},<br>
 * via {@link GLAutoDrawable#invoke(boolean, javax.media.opengl.GLRunnable)}.<br>
 * {@link GLAutoDrawable} executes these commands within it's {@link GLAutoDrawable#display()}
 * method while the OpenGL context is current.<br>
 * <p>
 * This might be useful to inject OpenGL commands from an I/O event listener.
 */
public interface GLRunnable { 
    /**
     * Called by the drawable to initiate one-shot OpenGL commands by the
     * client, like {@link GLEventListener#display(GLAutoDrawable)}.
     * 
     * @param drawable the associated drawable the implementation shall use
     * @return false if impl invalidates the back buffers, hence {@link GLAutoDrawable#display()} will 
     *         issue another {@link GLEventListener#display(GLAutoDrawable)} call. Otherwise true.
     */
    boolean run(GLAutoDrawable drawable);
}

