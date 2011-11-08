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

package javax.media.opengl;

import java.nio.*;

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
 * <LI> The memory allocators for the NVidia vertex_array_range (VAR)
 * extension, in particular <code>wglAllocateMemoryNV</code> /
 * <code>glXAllocateMemoryNV</code> and associated routines. {@link
 * #glAllocateMemoryNV} has been provided for window system-independent
 * access to VAR. {@link #isFunctionAvailable} will translate an argument
 * of "glAllocateMemoryNV" or "glFreeMemoryNV" into the appropriate
 * window system-specific name. </P>
 *
 * <LI> WGL_ARB_pbuffer, WGL_ARB_pixel_format, and other
 * platform-specific pbuffer functionality; the availability of
 * pbuffers can be queried on Windows, X11 and Mac OS X platforms by
 * querying {@link #isExtensionAvailable} with an argument of
 * "GL_ARB_pbuffer" or "GL_ARB_pixel_format".
 *
 * </UL> <P>
 *
 */
public interface GLBase {
    
  /**
   * Indicates whether this GL object conforms to any of the common GL profiles.
   * @return whether this GL object conforms to any of the common GL profiles
   */
  public boolean isGL();

  /**
   * Indicates whether this GL object conforms to the GL4 compatibility profile.
   * The GL4 compatibility profile merges the GL2 profile and GL4 core profile.
   * @return whether this GL object conforms to the GL4 compatibility profile
   */
  public boolean isGL4bc();

  /**
   * Indicates whether this GL object conforms to the GL4 core profile.
   * The GL4 core profile reflects OpenGL versions greater or equal 3.1
   * @return whether this GL object conforms to the GL4 core profile
   */
  public boolean isGL4();

  /**
   * Indicates whether this GL object conforms to the GL3 compatibility profile.
   * The GL3 compatibility profile merges the GL2 profile and GL3 core profile.
   * @return whether this GL object conforms to the GL3 compatibility profile
   */
  public boolean isGL3bc();

  /**
   * Indicates whether this GL object conforms to the GL3 core profile.
   * The GL3 core profile reflects OpenGL versions greater or equal 3.1
   * @return whether this GL object conforms to the GL3 core profile
   */
  public boolean isGL3();

  /**
   * Indicates whether this GL object conforms to the GL2 profile.
   * The GL2 profile reflects OpenGL versions greater or equal 1.5
   * @return whether this GL object conforms to the GL2 profile
   */
  public boolean isGL2();

  /**
   * Indicates whether this GL object conforms to the GLES1 profile.
   * @return whether this GL object conforms to the GLES1 profile
   */
  public boolean isGLES1();

  /**
   * Indicates whether this GL object conforms to the GLES2 profile.
   * @return whether this GL object conforms to the GLES2 profile
   */
  public boolean isGLES2();

  /**
   * Indicates whether this GL object conforms to one of the OpenGL ES compatible profiles.
   * @return whether this GL object conforms to one of the OpenGL ES profiles
   */
  public boolean isGLES();

  /**
   * Indicates whether this GL object conforms to the GL2ES1 compatible profile.
   * @return whether this GL object conforms to the GL2ES1 profile
   */
  public boolean isGL2ES1();

  /**
   * Indicates whether this GL object conforms to the GL2ES2 compatible profile.
   * @return whether this GL object conforms to the GL2ES2 profile
   */
  public boolean isGL2ES2();

  /**
   * Indicates whether this GL object is compatible with OpenGL ES2.
   * @return true if this context is an ES2 context or implements 
   *         the extension <code>GL_ARB_ES2_compatibility</code>, otherwise false 
   */
  public boolean isGLES2Compatible();

  /**
   * Indicates whether this GL object conforms to the GL2GL3 compatible profile.
   * @return whether this GL object conforms to the GL2GL3 profile
   */
  public boolean isGL2GL3();

  /** Indicates whether this GL object supports GLSL. */
  public boolean hasGLSL();

  /**
   * Casts this object to the GL interface.
   * @return this object cast to the GL interface
   * @throws GLException if this GLObject is not a GL implementation
   */
  public GL getGL() throws GLException;

  /**
   * Casts this object to the GL4bc interface.
   * @return this object cast to the GL4bc interface
   * @throws GLException if this GLObject is not a GL4bc implementation
   */
  public GL4bc getGL4bc() throws GLException;

  /**
   * Casts this object to the GL4 interface.
   * @return this object cast to the GL4 interface
   * @throws GLException if this GLObject is not a GL4 implementation
   */
  public GL4 getGL4() throws GLException;

  /**
   * Casts this object to the GL3bc interface.
   * @return this object cast to the GL3bc interface
   * @throws GLException if this GLObject is not a GL3bc implementation
   */
  public GL3bc getGL3bc() throws GLException;

  /**
   * Casts this object to the GL3 interface.
   * @return this object cast to the GL3 interface
   * @throws GLException if this GLObject is not a GL3 implementation
   */
  public GL3 getGL3() throws GLException;

  /**
   * Casts this object to the GL2 interface.
   * @return this object cast to the GL2 interface
   * @throws GLException if this GLObject is not a GL2 implementation
   */
  public GL2 getGL2() throws GLException;

  /**
   * Casts this object to the GLES1 interface.
   * @return this object cast to the GLES1 interface
   * @throws GLException if this GLObject is not a GLES1 implementation
   */
  public GLES1 getGLES1() throws GLException;

  /**
   * Casts this object to the GLES2 interface.
   * @return this object cast to the GLES2 interface
   * @throws GLException if this GLObject is not a GLES2 implementation
   */
  public GLES2 getGLES2() throws GLException;

  /**
   * Casts this object to the GL2ES1 interface.
   * @return this object cast to the GL2ES1 interface
   * @throws GLException if this GLObject is not a GL2ES1 implementation
   */
  public GL2ES1 getGL2ES1() throws GLException;

  /**
   * Casts this object to the GL2ES2 interface.
   * @return this object cast to the GL2ES2 interface
   * @throws GLException if this GLObject is not a GL2ES2 implementation
   */
  public GL2ES2 getGL2ES2() throws GLException;

  /**
   * Casts this object to the GL2GL3 interface.
   * @return this object cast to the GL2GL3 interface
   * @throws GLException if this GLObject is not a GL2GL3 implementation
   */
  public GL2GL3 getGL2GL3() throws GLException;

  /**
   * Returns the GLProfile with which this GL object is associated.
   * @return the GLProfile with which this GL object is associated
   */
  public GLProfile getGLProfile();

  /**
   * Returns the GLContext with which this GL object is associated.
   * @return the GLContext with which this GL object is associated
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
    * Returns true if the GL context supports non power of two (NPOT) textures,
    * otherwise false.
    * <p>
    * NPOT textures are supported in OpenGL >= 3, GLES2 or if the
    * 'GL_ARB_texture_non_power_of_two' extension is available.
    * </p>
    * @return
    */
   public boolean isNPOTTextureAvailable();

   /** Provides a platform-independent way to specify the minimum swap
       interval for buffer swaps. An argument of 0 disables
       sync-to-vertical-refresh completely, while an argument of 1
       causes the application to wait until the next vertical refresh
       until swapping buffers. The default, which is platform-specific,
       is usually either 0 or 1. This function is not guaranteed to
       have an effect, and in particular only affects heavyweight
       onscreen components.
       
       @see #getSwapInterval
       @throws GLException if this context is not the current
    */
   public void setSwapInterval(int interval);

   /** Provides a platform-independent way to get the swap
       interval set by {@link #setSwapInterval}. <br>

       If the interval is not set by {@link #setSwapInterval} yet, 
       -1 is returned, indicating that the platforms default 
       is being used.

       @see #setSwapInterval
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
    * <P>
    *
    * Note: it is the intent to add new extensions as quickly as possible
    * to the core GL API. Therefore it is unlikely that most vendors will
    * use this extension mechanism, but it is being provided for
    * completeness.
    */
   public Object getExtension(String extensionName);
}

