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
 * Sharing of server-side OpenGL objects such as buffer objects, e.g. VBOs,
 * and textures among OpenGL contexts is supported with this interface.
 * </p>
 * <p>
 * A <i>master</i> {@link GLContext} is the {@link GLContext} which is created first,
 * shared {@link GLContext} w/ this master are referred as slave {@link GLContext}.
 * </p>
 * <h5><a name="driverstabilityconstraints">Driver stability constraints</a></h5>
 * <p>
 * Be aware that the <i>master</i> {@link GLContext} and related resources, i.e. the {@link GLAutoDrawable},
 * <i>shall not</i> be destroyed before it's <i>slave</i> {@link GLContext} instances.<br>
 * Otherwise the OpenGL driver implementation may crash w/ SIGSEGV if shared resources are still used!<br>
 * Note that this is not specified within OpenGL and <i>should work</i>, however, some drivers
 * do not seem to handle this situation well, i.e. they do not postpone resource destruction
 * until the last reference is removed.<br>
 * Since pending destruction of {@link GLContext} and it's {@link GLDrawable} is complex and nearly impossible
 * for us at the top level, considering the different windowing systems and {@link GLAutoDrawable} types,
 * the user shall take care of proper destruction order.
 * </p>
 * <p>
 * Users may use a {@link GLDrawableFactory#createDummyDrawable(javax.media.nativewindow.AbstractGraphicsDevice, boolean, GLProfile) dummy}
 * {@link GLDrawable} and it's {@link GLContext} as the <i>master</i> of all shared <i>slave</i> {@link GLContext}.
 * Same constraints as above apply, i.e. it shall be destroyed <i>after</i> all shared slaves.
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
     * <p>
     * See <a href="#driverstabilityconstraints">driver stability constraints</a>.
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
     * <p>
     * See <a href="#driverstabilityconstraints">driver stability constraints</a>.
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
