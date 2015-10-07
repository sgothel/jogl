/**
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
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
 * <P>The base interface from which all GL profiles derive, providing
 * checked conversion down to concrete profiles, access to the
 * OpenGL context associated with the GL and extension/function
 * availability queries as described below.</P>
 *
 * <P> While the APIs for vendor extensions are unconditionally
 * exposed, the underlying functions may not be present. The method
 * {@link #isFunctionAvailable} should be used to query the
 * availability of any non-core function before it is used for the
 * first time; for example,
 * <code>gl.isFunctionAvailable("glProgramStringARB")</code>. On
 * certain platforms (Windows in particular), the most "core"
 * functionality is only OpenGL 1.1, so in theory any routines first
 * exposed in OpenGL 1.2, 1.3, and 1.4, 1.5, or 2.0 as well as vendor
 * extensions should all be queried. Calling an unavailable function
 * will cause a {@link GLException} to be raised. </P>
 *
 * {@link #isExtensionAvailable} may also be used to determine whether
 * a specific extension is available before calling the routines or
 * using the functionality it exposes: for example,
 * <code>gl.isExtensionAvailable("GL_ARB_vertex_program");</code>.
 * However, in this case it is up to the end user to know which
 * routines or functionality are associated with which OpenGL
 * extensions. It may also be used to test for the availability of a
 * particular version of OpenGL: for example,
 * <code>gl.isExtensionAvailable("GL_VERSION_1_5");</code>.
 *
 * <P> Exceptions to the window system extension naming rules:
 *
 * <UL>
 *
 * <LI> WGL_ARB_pbuffer, WGL_ARB_pixel_format, and other
 * platform-specific pbuffer functionality; the availability of
 * pbuffers can be queried on Windows, X11 and Mac OS X platforms by
 * querying {@link #isExtensionAvailable} with an argument of
 * "GL_ARB_pbuffer" or "GL_ARB_pixel_format".</LI>
 *
 * </UL> <P>
 *
 */
public interface GLBase {

  /**
   * Indicates whether this GL object conforms to any of the OpenGL profiles.
   */
  public boolean isGL();

  /**
   * Indicates whether this GL object conforms to the OpenGL &ge; 4.0 compatibility profile.
   * The GL4 compatibility profile includes the GL2, GL2ES1, GL2ES2, GL3, GL3bc and GL4 profile.
   * @see GLContext#isGL4bc()
   */
  public boolean isGL4bc();

  /**
   * Indicates whether this GL object conforms to the OpenGL &ge; 4.0 core profile.
   * The GL4 core profile includes the GL2ES2, and GL3 profile.
   * @see GLContext#isGL4()
   */
  public boolean isGL4();

  /**
   * Indicates whether this GL object conforms to the OpenGL &ge; 3.1 compatibility profile.
   * The GL3 compatibility profile includes the GL2, GL2ES1, GL2ES2 and GL3 profile.
   * @see GLContext#isGL3bc()
   */
  public boolean isGL3bc();

  /**
   * Indicates whether this GL object conforms to the OpenGL &ge; 3.1 core profile.
   * The GL3 core profile includes the GL2ES2 profile.
   * @see GLContext#isGL3()
   */
  public boolean isGL3();

  /**
   * Indicates whether this GL object conforms to the OpenGL &le; 3.0 profile.
   * The GL2 profile includes the GL2ES1 and GL2ES2 profile.
   * @see GLContext#isGL2()
   */
  public boolean isGL2();

  /**
   * Indicates whether this GL object conforms to the OpenGL ES &ge; 1.0 profile.
   * @see GLContext#isGLES1()
   */
  public boolean isGLES1();

  /**
   * Indicates whether this GL object conforms to the OpenGL ES &ge; 2.0 profile.
   * <p>
   * Remark: ES2 compatible desktop profiles are not included.
   * To query whether core ES2 functionality is provided, use {@link #isGLES2Compatible()}.
   * </p>
   * @see #isGLES2Compatible()
   * @see GLContext#isGLES2()
   */
  public boolean isGLES2();

  /**
   * Indicates whether this GL object conforms to the OpenGL ES &ge; 3.0 profile.
   * <p>
   * Remark: ES3 compatible desktop profiles are not included.
   * To query whether core ES3 functionality is provided, use {@link #isGLES3Compatible()}.
   * </p>
   * @see #isGLES3Compatible()
   * @see GLContext#isGLES3()
   */
  public boolean isGLES3();

  /**
   * Indicates whether this GL object conforms to one of the OpenGL ES profiles,
   * see {@link #isGLES1()}, {@link #isGLES2()} and {@link #isGLES3()}.
   * @see GLContext#isGLES()
   */
  public boolean isGLES();

  /**
   * Indicates whether this GL object conforms to a GL2ES1 compatible profile.
   * @see GLContext#isGL2ES1()
   */
  public boolean isGL2ES1();

  /**
   * Indicates whether this GL object conforms to a GL2ES2 compatible profile.
   * @see GLContext#isGL2ES2()
   */
  public boolean isGL2ES2();

  /**
   * Indicates whether this GL object conforms to a either a GL2GL3 or GL3ES3 compatible profile.
   * @see GLContext#isGL2ES3()
   */
  public boolean isGL2ES3();

  /**
   * Indicates whether this GL object conforms to a GL3ES3 compatible profile.
   * @see GLContext#isGL3ES3()
   */
  public boolean isGL3ES3();

  /**
   * Returns true if this GL object conforms to a GL4ES3 compatible profile, i.e. if {@link #isGLES3Compatible()} returns true.
   * <p>Includes [ GL &ge; 4.3, GL &ge; 3.1 w/ GL_ARB_ES3_compatibility and GLES3 ]</p>
   * @see GLContext#isGL4ES3()
   */
  public boolean isGL4ES3();

  /**
   * Indicates whether this GL object conforms to a GL2GL3 compatible profile.
   * @see GLContext#isGL2GL3()
   */
  public boolean isGL2GL3();

  /**
   * Indicates whether this GL object uses a GL4 core profile. <p>Includes [ GL4 ].</p>
   * @see GLContext#isGL4core()
   */
  public boolean isGL4core();

  /**
   * Indicates whether this GL object uses a GL3 core profile. <p>Includes [ GL4, GL3 ].</p>
   * @see GLContext#isGL3core()
   */
  public boolean isGL3core();

  /**
   * Indicates whether this GL object uses a GL core profile. <p>Includes [ GL4, GL3, GLES3, GL2ES2 ].</p>
   * @see GLContext#isGLcore()
   */
  public boolean isGLcore();

  /**
   * Indicates whether this GL object is compatible with the core OpenGL ES2 functionality.
   * @return true if this context is an ES2 context or implements
   *         the extension <code>GL_ARB_ES2_compatibility</code>, otherwise false
   * @see GLContext#isGLES2Compatible()
   */
  public boolean isGLES2Compatible();

  /**
   * Indicates whether this GL object is compatible with the core OpenGL ES3 functionality.
   * <p>
   * Return true if the underlying context is an ES3 context or implements
   * the extension <code>GL_ARB_ES3_compatibility</code>, otherwise false.
   * </p>
   * <p>
   * Includes [ GL &ge; 4.3, GL &ge; 3.1 w/ GL_ARB_ES3_compatibility and GLES3 ]
   * </p>
   * @see GLContext#isGLES3Compatible()
   */
  public boolean isGLES3Compatible();

  /**
   * Indicates whether this GL object is compatible with the core OpenGL ES3.1 functionality.
   * <p>
   * Return true if the underlying context is an ES3 context &ge; 3.1 or implements
   * the extension <code>GL_ARB_ES3_1_compatibility</code>, otherwise false.
   * </p>
   * <p>
   * Includes [ GL &ge; 4.5, GL &ge; 3.1 w/ GL_ARB_ES3_1_compatibility and GLES3 &ge; 3.1 ]
   * </p>
   * @see GLContext#isGLES31Compatible()
   */
  public boolean isGLES31Compatible();

  /**
   * Indicates whether this GL object is compatible with the core OpenGL ES3.2 functionality.
   * <p>
   * Return true if the underlying context is an ES3 context &ge; 3.2 or implements
   * the extension <code>GL_ARB_ES3_2_compatibility</code>, otherwise false.
   * </p>
   * <p>
   * Includes [ GL &ge; 4.5, GL &ge; 3.1 w/ GL_ARB_ES3_2_compatibility and GLES3 &ge; 3.2 ]
   * </p>
   * @see GLContext#isGLES32Compatible()
   */
  public boolean isGLES32Compatible();

  /**
   * Indicates whether this GL object supports GLSL.
   * @see GLContext#hasGLSL()
   */
  public boolean hasGLSL();

  /**
   * Returns the downstream GL instance in case this is a wrapping pipeline, otherwise <code>null</code>.
   * <p>
   * See {@link #getRootGL()} for retrieving the implementing root instance.
   * </p>
   * @throws GLException if the downstream instance is not null and not a GL implementation
   * @see #getRootGL()
   */
  public GL getDownstreamGL() throws GLException;

  /**
   * Returns the implementing root instance, considering a wrapped pipelined hierarchy, see {@link #getDownstreamGL()}.
   * <p>
   * If this instance is not a wrapping pipeline, i.e. has no downstream instance,
   * this instance is returned.
   * </p>
   * @throws GLException if the root instance is not a GL implementation
   */
  public GL getRootGL() throws GLException;

  /**
   * Casts this object to the GL interface.
   * @throws GLException if this object is not a GL implementation
   */
  public GL getGL() throws GLException;

  /**
   * Casts this object to the GL4bc interface.
   * @throws GLException if this object is not a GL4bc implementation
   */
  public GL4bc getGL4bc() throws GLException;

  /**
   * Casts this object to the GL4 interface.
   * @throws GLException if this object is not a GL4 implementation
   */
  public GL4 getGL4() throws GLException;

  /**
   * Casts this object to the GL3bc interface.
   * @throws GLException if this object is not a GL3bc implementation
   */
  public GL3bc getGL3bc() throws GLException;

  /**
   * Casts this object to the GL3 interface.
   * @throws GLException if this object is not a GL3 implementation
   */
  public GL3 getGL3() throws GLException;

  /**
   * Casts this object to the GL2 interface.
   * @throws GLException if this object is not a GL2 implementation
   */
  public GL2 getGL2() throws GLException;

  /**
   * Casts this object to the GLES1 interface.
   * @throws GLException if this object is not a GLES1 implementation
   */
  public GLES1 getGLES1() throws GLException;

  /**
   * Casts this object to the GLES2 interface.
   * @throws GLException if this object is not a GLES2 implementation
   */
  public GLES2 getGLES2() throws GLException;

  /**
   * Casts this object to the GLES3 interface.
   * @throws GLException if this object is not a GLES3 implementation
   */
  public GLES3 getGLES3() throws GLException;

  /**
   * Casts this object to the GL2ES1 interface.
   * @throws GLException if this object is not a GL2ES1 implementation
   */
  public GL2ES1 getGL2ES1() throws GLException;

  /**
   * Casts this object to the GL2ES2 interface.
   * @throws GLException if this object is not a GL2ES2 implementation
   */
  public GL2ES2 getGL2ES2() throws GLException;

  /**
   * Casts this object to the GL2ES3 interface.
   * @throws GLException if this object is not a GL2ES3 implementation
   */
  public GL2ES3 getGL2ES3() throws GLException;

  /**
   * Casts this object to the GL3ES3 interface.
   * @throws GLException if this object is not a GL3ES3 implementation
   */
  public GL3ES3 getGL3ES3() throws GLException;

  /**
   * Casts this object to the GL4ES3 interface.
   * @throws GLException if this object is not a GL4ES3 implementation
   */
  public GL4ES3 getGL4ES3() throws GLException;

  /**
   * Casts this object to the GL2GL3 interface.
   * @throws GLException if this object is not a GL2GL3 implementation
   */
  public GL2GL3 getGL2GL3() throws GLException;

  /**
   * Returns the GLProfile associated with this GL object.
   */
  public GLProfile getGLProfile();

  /**
   * Returns the GLContext associated which this GL object.
   */
  public GLContext getContext();

   /**
    * Returns true if the specified OpenGL core- or extension-function can be
    * used successfully through this GL instance given the current host (OpenGL
    * <i>client</i>) and display (OpenGL <i>server</i>) configuration.<P>
    * By "successfully" we mean that the function is both <i>callable</i>
    * on the machine running the program and <i>available</i> on the current
    * display.<P>
    *
    * In order to call a function successfully, the function must be both
    * <i>callable</i> on the machine running the program and <i>available</i> on
    * the display device that is rendering the output (note: on non-networked,
    * single-display machines these two conditions are identical; on networked and/or
    * multi-display machines this becomes more complicated). These conditions are
    * met if the function is either part of the core OpenGL version supported by
    * both the host and display, or it is an OpenGL extension function that both
    * the host and display support. <P>
    *
    * A GL function is <i>callable</i> if it is successfully linked at runtime,
    * hence the GLContext must be made current at least once.
    *
    * @param glFunctionName the name of the OpenGL function (e.g., use
    * "glBindRenderbufferEXT" or "glBindRenderbuffer" to check if {@link
    * GL#glBindRenderbuffer(int,int)} is available).
    */
   public boolean isFunctionAvailable(String glFunctionName);

   /**
    * Returns true if the specified OpenGL extension can be
    * used successfully through this GL instance given the current host (OpenGL
    * <i>client</i>) and display (OpenGL <i>server</i>) configuration.<P>
    *
    * @param glExtensionName the name of the OpenGL extension (e.g.,
    * "GL_ARB_vertex_program").
    */
   public boolean isExtensionAvailable(String glExtensionName);

   /**
    * Returns <code>true</code> if basic FBO support is available, otherwise <code>false</code>.
    * <p>
    * Basic FBO is supported if the context is either GL-ES >= 2.0, GL >= 3.0 [core, compat] or implements the extensions
    * <code>GL_ARB_ES2_compatibility</code>, <code>GL_ARB_framebuffer_object</code>, <code>GL_EXT_framebuffer_object</code> or <code>GL_OES_framebuffer_object</code>.
    * </p>
    * <p>
    * Basic FBO support may only include one color attachment and no multisampling,
    * as well as limited internal formats for renderbuffer.
    * </p>
    * @see GLContext#hasBasicFBOSupport()
    */
   public boolean hasBasicFBOSupport();

   /**
    * Returns <code>true</code> if full FBO support is available, otherwise <code>false</code>.
    * <p>
    * Full FBO is supported if the context is either GL >= core 3.0 [ES, core, compat] or implements the extensions
    * <code>ARB_framebuffer_object</code>, or all of
    * <code>EXT_framebuffer_object</code>, <code>EXT_framebuffer_multisample</code>,
    * <code>EXT_framebuffer_blit</code>, <code>GL_EXT_packed_depth_stencil</code>.
    * </p>
    * <p>
    * Full FBO support includes multiple color attachments and multisampling.
    * </p>
    * @see GLContext#hasFullFBOSupport()
    */
   public boolean hasFullFBOSupport();

   /**
    * Returns the maximum number of FBO RENDERBUFFER samples
    * if {@link #hasFullFBOSupport() full FBO is supported}, otherwise false.
    * @see GLContext#getMaxRenderbufferSamples()
    */
   public int getMaxRenderbufferSamples();

   /**
    * Returns true if the GL context supports non power of two (NPOT) textures,
    * otherwise false.
    * <p>
    * NPOT textures are supported in OpenGL >= 3, GLES2 or if the
    * 'GL_ARB_texture_non_power_of_two' extension is available.
    * </p>
    */
   public boolean isNPOTTextureAvailable();

   public boolean isTextureFormatBGRA8888Available();

   /**
    * Set the swap interval of the current context and attached <i>onscreen {@link GLDrawable}</i>.
    * <p>
    * <i>offscreen {@link GLDrawable}</i> are ignored and {@code false} is returned.
    * </p>
    * <p>
    * The {@code interval} semantics:
    * <ul>
    *   <li><i>0</i> disables the vertical synchronization</li>
    *   <li><i>&ge;1</i> is the number of vertical refreshes before a swap buffer occurs</li>
    *   <li><i>&lt;0</i> enables <i>late swaps to occur without synchronization to the video frame</i>, a.k.a <i>EXT_swap_control_tear</i>.
    *              If supported, the absolute value is the minimum number of
    *              video frames between buffer swaps. If not supported, the absolute value is being used, see above.
    *   </li>
    * </ul>
    * </p>
    * @param interval see above
    * @return true if the operation was successful, otherwise false
    * @throws GLException if the context is not current.
    * @see GLContext#setSwapInterval(int)
    * @see #getSwapInterval()
    */
   public void setSwapInterval(int interval) throws GLException;

   /**
    * Return the current swap interval.
    * <p>
    * If the context has not been made current at all,
    * the default value {@code 0} is returned.
    * </p>
    * <p>
    * For a valid context w/ an <o>onscreen {@link GLDrawable}</i> the default value is {@code 1},
    * otherwise the default value is {@code 0}.
    * </p>
    * @see GLContext#getSwapInterval()
    * @see #setSwapInterval(int)
    */
   public int getSwapInterval();

   /**
    * Returns an object through which platform-specific OpenGL extensions
    * (EGL, GLX, WGL, etc.) may be accessed. The data type of the returned
    * object and its associated capabilities are undefined. Most
    * applications will never need to call this method. It is highly
    * recommended that any applications which do call this method perform
    * all accesses on the returned object reflectively to guard
    * themselves against changes to the implementation.
    */
   public Object getPlatformGLExtensions();

   /**
    * Returns an object providing access to the specified OpenGL
    * extension. This is intended to provide a mechanism for vendors who
    * wish to provide access to new OpenGL extensions without changing
    * the public API of the core package. For example, a user may request
    * access to extension "GL_VENDOR_foo" and receive back an object
    * which implements a vendor-specified interface which can call the
    * OpenGL extension functions corresponding to that extension. It is
    * up to the vendor to specify both the extension name and Java API
    * for accessing it, including which class or interface contains the
    * functions.
    *
    * <p>
    * Note: it is the intent to add new extensions as quickly as possible
    * to the core GL API. Therefore it is unlikely that most vendors will
    * use this extension mechanism, but it is being provided for
    * completeness.
    * </p>
    */
   public Object getExtension(String extensionName);

   /** Aliased entrypoint of <code> void {@native glClearDepth}(GLclampd depth); </code> and <code> void {@native glClearDepthf}(GLclampf depth); </code>. */
   public void glClearDepth( double depth );

   /** Aliased entrypoint of <code> void {@native glDepthRange}(GLclampd depth); </code> and <code> void {@native glDepthRangef}(GLclampf depth); </code>. */
   public void glDepthRange(double zNear, double zFar);

   /**
    * @param target a GL buffer (VBO) target as used in {@link GL#glBindBuffer(int, int)}, ie {@link GL#GL_ELEMENT_ARRAY_BUFFER}, {@link GL#GL_ARRAY_BUFFER}, ..
    * @return the GL buffer name bound to a target via {@link GL#glBindBuffer(int, int)} or 0 if unbound.
    * @see #getBufferStorage(int)
    */
   public int getBoundBuffer(int target);

   /**
    * @param bufferName a GL buffer name, generated with e.g. {@link GL#glGenBuffers(int, int[], int)} and used in {@link GL#glBindBuffer(int, int)}, {@link GL#glBufferData(int, long, java.nio.Buffer, int)} or {@link GL2#glNamedBufferDataEXT(int, long, java.nio.Buffer, int)}.
    * @return the size of the given GL buffer storage, see {@link GLBufferStorage}
    * @see #getBoundBuffer(int)
    */
   public GLBufferStorage getBufferStorage(int bufferName);

   /**
    * Returns the {@link GLBufferStorage} instance as mapped via OpenGL's native {@link GL#glMapBuffer(int, int) glMapBuffer(..)} implementation.
    * <p>
    * Throws a {@link GLException} if GL-function constraints are not met.
    * </p>
    * <p>
    * {@link GL#glMapBuffer(int, int)} wrapper calls this method and returns {@link GLBufferStorage#getMappedBuffer()}.
    * </p>
    * <p>
    * A zero {@link GLBufferStorage#getSize()} will avoid a native call and returns the unmapped {@link GLBufferStorage}.
    * </p>
    * <p>
    * A null native mapping result indicating an error will
    * not cause a GLException but returns the unmapped {@link GLBufferStorage}.
    * This allows the user to handle this case.
    * </p>
    * @param target denotes the buffer via it's bound target
    * @param access the mapping access mode
    * @throws GLException if buffer is not bound to target
    * @throws GLException if buffer is not tracked
    * @throws GLException if buffer is already mapped
    * @throws GLException if buffer has invalid store size, i.e. less-than zero
    */
   public GLBufferStorage mapBuffer(int target, int access) throws GLException;

   /**
    * Returns the {@link GLBufferStorage} instance as mapped via OpenGL's native {@link GL#glMapBufferRange(int, long, long, int) glMapBufferRange(..)} implementation.
    * <p>
    * Throws a {@link GLException} if GL-function constraints are not met.
    * </p>
    * <p>
    * {@link GL#glMapBufferRange(int, long, long, int)} wrapper calls this method and returns {@link GLBufferStorage#getMappedBuffer()}.
    * </p>
    * <p>
    * A zero {@link GLBufferStorage#getSize()} will avoid a native call and returns the unmapped {@link GLBufferStorage}.
    * </p>
    * <p>
    * A null native mapping result indicating an error will
    * not cause a GLException but returns the unmapped {@link GLBufferStorage}.
    * This allows the user to handle this case.
    * </p>
    * @param target denotes the buffer via it's bound target
    * @param offset offset of the mapped buffer's storage
    * @param length length of the mapped buffer's storage
    * @param access the mapping access mode
    * @throws GLException if buffer is not bound to target
    * @throws GLException if buffer is not tracked
    * @throws GLException if buffer is already mapped
    * @throws GLException if buffer has invalid store size, i.e. less-than zero
    * @throws GLException if buffer mapping range does not fit, incl. offset
    */
   public GLBufferStorage mapBufferRange(final int target, final long offset, final long length, final int access) throws GLException;

   /**
    * @return true if a VBO is bound to {@link GL#GL_ARRAY_BUFFER} via {@link GL#glBindBuffer(int, int)}, otherwise false
    */
   public boolean isVBOArrayBound();

   /**
    * @return true if a VBO is bound to {@link GL#GL_ELEMENT_ARRAY_BUFFER} via {@link GL#glBindBuffer(int, int)}, otherwise false
    */
   public boolean isVBOElementArrayBound();

   /**
    * Return the framebuffer name bound to this context,
    * see {@link GL#glBindFramebuffer(int, int)}.
    * <p>
    * Calls {@link GLContext#getBoundFramebuffer(int)}.
    * </p>
    */
   public int getBoundFramebuffer(int target);

   /**
    * Return the default draw framebuffer name.
    * <p>
    * May differ from it's default <code>zero</code>
    * in case an framebuffer object ({@link com.jogamp.opengl.FBObject}) based drawable
    * is being used.
    * </p>
    * <p>
    * Calls {@link GLContext#getDefaultDrawFramebuffer()}.
    * </p>
    */
   public int getDefaultDrawFramebuffer();

   /**
    * Return the default read framebuffer name.
    * <p>
    * May differ from it's default <code>zero</code>
    * in case an framebuffer object ({@link com.jogamp.opengl.FBObject}) based drawable
    * is being used.
    * </p>
    * <p>
    * Calls {@link GLContext#getDefaultReadFramebuffer()}.
    * </p>
    */
   public int getDefaultReadFramebuffer();

   /**
    * Returns the default color buffer within the current bound
    * {@link #getDefaultReadFramebuffer()}, i.e. GL_READ_FRAMEBUFFER,
    * which will be used as the source for pixel reading commands,
    * like {@link GL#glReadPixels(int, int, int, int, int, int, java.nio.Buffer) glReadPixels} etc.
    * <p>
    * For offscreen framebuffer objects this is {@link GL#GL_COLOR_ATTACHMENT0},
    * otherwise this is {@link GL#GL_FRONT} for single buffer configurations
    * and {@link GL#GL_BACK} for double buffer configurations.
    * </p>
    * <p>
    * Note-1: Neither ES1 nor ES2 supports selecting the read buffer via glReadBuffer
    * and {@link GL#GL_BACK} is the default.
    * </p>
    * <p>
    * Note-2: ES3 only supports {@link GL#GL_BACK}, {@link GL#GL_NONE} or {@link GL#GL_COLOR_ATTACHMENT0}+i
    * </p>
    * <p>
    * Note-3: See {@link com.jogamp.opengl.util.GLDrawableUtil#swapBuffersBeforeRead(GLCapabilitiesImmutable) swapBuffersBeforeRead}
    * for read-pixels and swap-buffers implications.
    * </p>
    * <p>
    * Calls {@link GLContext#getDefaultReadBuffer()}.
    * </p>
    */
   public int getDefaultReadBuffer();
}

