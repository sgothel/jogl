/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
 * Adds capabilities to set a shared {@link GLContext} directly or via an {@link GLAutoDrawable}.
 * <p>
 * Warning: Don't reference this interface directly, since it may end up in {@link GLAutoDrawable}
 * </p>
 */
public interface GLSharedContextSetter extends GLAutoDrawable {
    /**
     * Specifies an {@link GLContext OpenGL context}, which shall be shared by this {@link GLAutoDrawable}'s {@link GLContext}.
     * <p>
     * Since the {@link GLDrawable drawable} and {@link GLContext context} is created
     * at {@link GLAutoDrawable#initialization GLAutoDrawable initialization}
     * this method shall be called beforehand to have any effect.
     * </p>
     * <p>
     * A set <i>sharedContext</i> will block context creation, i.e. {@link GLAutoDrawable#initialization GLAutoDrawable initialization},
     * as long it is not {@link GLContext#isCreated() created natively}.
     * </p>
     *
     * @param sharedContext The OpenGL context to be shared by this {@link GLAutoDrawable}'s {@link GLContext}.
     * @throws IllegalStateException if a {@link #setSharedContext(GLContext) shared GLContext}
     *                               or {@link #setSharedAutoDrawable(GLAutoDrawable) shared GLAutoDrawable} is already set,
     *                               the given sharedContext is null or equal to this {@link GLAutoDrawable}'s context.
     * @see #setSharedAutoDrawable(GLAutoDrawable)
     */
    void setSharedContext(GLContext sharedContext) throws IllegalStateException;

    /**
     * Specifies an {@link GLAutoDrawable}, which {@link GLContext OpenGL context} shall be shared by this {@link GLAutoDrawable}'s {@link GLContext}.
     * <p>
     * Since the {@link GLDrawable drawable} and {@link GLContext context} is created
     * at {@link GLAutoDrawable#initialization GLAutoDrawable initialization}
     * this method shall be called beforehand to have any effect.
     * </p>
     * <p>
     * A set <i>sharedAutoDrawable</i> will block context creation, i.e. {@link GLAutoDrawable#initialization GLAutoDrawable initialization},
     * as long it's {@link GLContext} is <code>null</code>
     * or has not been {@link GLContext#isCreated() created natively}.
     * </p>
     *
     * @param sharedContext The GLAutoDrawable, which OpenGL context shall be shared by this {@link GLAutoDrawable}'s {@link GLContext}.
     * @throws IllegalStateException if a {@link #setSharedContext(GLContext) shared GLContext}
     *                               or {@link #setSharedAutoDrawable(GLAutoDrawable) shared GLAutoDrawable} is already set,
     *                               the given sharedAutoDrawable is null or equal to this GLAutoDrawable.
     * @see #setSharedContext(GLContext)
     */
    void setSharedAutoDrawable(GLAutoDrawable sharedAutoDrawable) throws IllegalStateException;
}
