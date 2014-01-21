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
 * shared {@link GLContext} w/ this master are referred as slave {@link GLContext}
 * and controls the shared object's lifecycle, i.e. their construction and destruction.
 * </p>
 * <p>
 * Using the nearest or same {@link GLCapabilitiesImmutable#getVisualID(javax.media.nativewindow.VisualIDHolder.VIDType) visual ID}
 * or {@link GLCapabilitiesImmutable caps} across the shared {@link GLDrawable}s will yield best compatibility.
 * </p>
 * <h5><a name="lifecycle">Lifecycle Considerations</a></h5>
 * <p>
 * After shared objects are created on the <i>master</i>, the OpenGL pipeline
 * might need to be synchronized w/ the <i>slaves</i>, e.g. via {@link GL#glFinish()}.
 * At least this has been experienced w/ OSX 10.9.
 * </p>
 * <p>
 * Be aware that the <i>master</i> {@link GLContext} and related resources
 * <i>shall not</i> be destroyed before it's <i>slave</i> {@link GLContext} instances <i>while they are using them</i>.<br>
 * Otherwise the OpenGL driver implementation may crash w/ SIGSEGV, since using already destroyed resources,
 * e.g. OpenGL buffer objects, may not be validated by the driver!<br>
 * </p>
 * <p>
 * Either proper lifecycle synchronization is implemented, e.g. by notifying the <i>slaves</i> about the loss of the shared resources,
 * <i>or</i> the <i>slaves</i> validate whether the resources are still valid.
 * </p>
 * <p>
 * To simplify above lifecycle issues, one may use a {@link GLDrawableFactory#createDummyDrawable(javax.media.nativewindow.AbstractGraphicsDevice, boolean, GLCapabilitiesImmutable, GLCapabilitiesChooser) dummy}
 * {@link GLDrawable} and it's {@link GLContext} as the <i>master</i> of all shared <i>slave</i> {@link GLContext}.
 * Since this <i>dummy instance</i> does not depend on any native windowing system, it can be controlled easily w/o being <i>in sight</i>.<br>
 * Below code creates a {@link GLAutoDrawable} based on a <i>dummy GLDrawable</i>:
 * <pre>
        // GLProfile and GLCapabilities should be equal across all shared GL drawable/context.
        final GLCapabilitiesImmutable caps = ... ;
        final GLProfile glp = caps.getGLProfile();
        ..
        final boolean createNewDevice = true; // use 'own' display device!
        final GLAutoDrawable sharedDrawable = GLDrawableFactory.getFactory(glp).createDummyAutoDrawable(null, createNewDevice, caps, null);
        sharedDrawable.display(); // triggers GLContext object creation and native realization.
        ...
        // Later a shared 'slave' can be created e.g.:
        GLWindow glad = GLWindow.create(caps); // or any other GLAutoDrawable supporting GLSharedContextSetter
        glad.setSharedAutoDrawable(sharedDrawable);
        glad.addGLEventListener(..);
        glad.setVisible(true); // GLWindow creation ..
 * </pre>
 * </p>
 * <h5><a name="driverissues">Known Driver Issues</a></h5>
 * <h7><a name="intelmesa">Intel's Mesa >= 9.1.2 Backend for [Sandybridge/Ivybridge] on GNU/Linux</a></h7>
 * <p>
 * <pre>
 * Error: 'intel_do_flush_locked: No such file or directory'
 * JogAmp: https://jogamp.org/bugzilla/show_bug.cgi?id=873
 * freedesktop.org: https://bugs.freedesktop.org/show_bug.cgi?id=41736#c8
 * </pre>
 * Shared context seems not to be supported w/ lock-free bound X11 display connections
 * per OpenGL drawable/context. The error message above is thrown in this case.
 * Hence the driver bug renders shared context use w/ JOGL impossible.
 * </p>
 * <h7><a name="hisilicon">Hisilicon's Immersion.16 on Android</a></h7>
 * <p>
 * We failed to create a shared ES2 context on another thread.
 * </p>
 */
public interface GLSharedContextSetter extends GLAutoDrawable {
    /**
     * Returns true if all {@link GLEventListener} are initialized, otherwise false.
     * @deprecated Promote method to {@link GLAutoDrawable}
     */
    boolean areAllGLEventListenerInitialized();

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
     * See <a href="#lifecycle">Lifecycle Considerations</a>.
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
     * as long it's
     * <ul>
     *   <li>{@link GLContext} is <code>null</code>, or</li>
     *   <li>{@link GLContext} has not been {@link GLContext#isCreated() created natively}, or</li>
     *   <li>{@link GLEventListener} are <i>not</i> {@link GLSharedContextSetter#areAllGLEventListenerInitialized() completely initialized}</li>
     * </ul>
     * </p>
     * <p>
     * See <a href="#lifecycle">Lifecycle Considerations</a>.
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
