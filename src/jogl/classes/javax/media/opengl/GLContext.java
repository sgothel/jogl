/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package javax.media.opengl;

import java.nio.IntBuffer;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;

import javax.media.nativewindow.AbstractGraphicsDevice;

import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;

/** Abstraction for an OpenGL rendering context. In order to perform
    OpenGL rendering, a context must be "made current" on the current
    thread. OpenGL rendering semantics specify that only one context
    may be current on the current thread at any given time, and also
    that a given context may be current on only one thread at any
    given time. Because components can be added to and removed from
    the component hierarchy at any time, it is possible that the
    underlying OpenGL context may need to be destroyed and recreated
    multiple times over the lifetime of a given component. This
    process is handled by the implementation, and the GLContext
    abstraction provides a stable object which clients can use to
    refer to a given context. */
public abstract class GLContext {
  public static final boolean DEBUG = Debug.debug("GLContext");
  
  public static final boolean TRACE_SWITCH;       
  /** Reflects property jogl.debug.DebugGL. If true, the debug pipeline is enabled at context creation. */
  public final static boolean DEBUG_GL;
  /** Reflects property jogl.debug.TraceGL. If true, the trace pipeline is enabled at context creation. */
  public final static boolean TRACE_GL;
  
  static { 
      final AccessControlContext acl = AccessController.getContext();
      DEBUG_GL = Debug.isPropertyDefined("jogl.debug.DebugGL", true, acl);
      TRACE_GL = Debug.isPropertyDefined("jogl.debug.TraceGL", true, acl);
      TRACE_SWITCH = Debug.isPropertyDefined("jogl.debug.GLContext.TraceSwitch", true, acl);
  }
  
  /** Indicates that the context was not made current during the last call to {@link #makeCurrent makeCurrent}. */
  public static final int CONTEXT_NOT_CURRENT = 0;
  /** Indicates that the context was made current during the last call to {@link #makeCurrent makeCurrent}. */
  public static final int CONTEXT_CURRENT     = 1;
  /** Indicates that a newly-created context was made current during the last call to {@link #makeCurrent makeCurrent}. */
  public static final int CONTEXT_CURRENT_NEW = 2;

  /** <code>ARB_create_context</code> related: created via ARB_create_context. Cache key value. */
  protected static final int CTX_IS_ARB_CREATED  = 1 <<  0;
  /** <code>ARB_create_context</code> related: compatibility profile. Cache key value. */
  protected static final int CTX_PROFILE_COMPAT  = 1 <<  1;
  /** <code>ARB_create_context</code> related: core profile. Cache key value. */
  protected static final int CTX_PROFILE_CORE    = 1 <<  2;
  /** <code>ARB_create_context</code> related: ES profile. Cache key value. */
  protected static final int CTX_PROFILE_ES      = 1 <<  3;
  /** <code>ARB_create_context</code> related: flag forward compatible. Cache key value. */
  protected static final int CTX_OPTION_FORWARD  = 1 <<  4;
  /** <code>ARB_create_context</code> related: flag debug. Not a cache key. */
  public static final int CTX_OPTION_DEBUG       = 1 <<  5;
  
  /** <code>GL_ARB_ES2_compatibility</code> implementation related: Context is compatible w/ ES2. Not a cache key. */
  protected static final int CTX_IMPL_ES2_COMPAT = 1 <<  8;

  /** Context uses software rasterizer, otherwise hardware rasterizer. Cache key value. */
  protected static final int CTX_IMPL_ACCEL_SOFT = 1 << 15;
  
  private static ThreadLocal<GLContext> currentContext = new ThreadLocal<GLContext>();

  private HashMap<String, Object> attachedObjectsByString = new HashMap<String, Object>();
  private IntObjectHashMap attachedObjectsByInt = new IntObjectHashMap();

  // RecursiveLock maintains a queue of waiting Threads, ensuring the longest waiting thread will be notified at unlock.  
  protected RecursiveLock lock = LockFactory.createRecursiveLock();

  /** The underlying native OpenGL context */
  protected long contextHandle;

  protected GLContext() {
      resetStates();
  }
  
  protected int ctxMajorVersion;
  protected int ctxMinorVersion;
  protected int ctxOptions;
  protected String ctxVersionString;

  protected void resetStates() {
      ctxMajorVersion=-1;
      ctxMinorVersion=-1;
      ctxOptions=0;
      ctxVersionString=null;
      attachedObjectsByString.clear();
      attachedObjectsByInt.clear();
      contextHandle=0;
  }

  /**
   * Returns the GLDrawable to which this context may be used to
   * draw.
   */
  public abstract GLDrawable getGLDrawable();

  /**
   * Return availability of GL read drawable.
   * @return true if a GL read drawable is supported with your driver, otherwise false.
   */
  public abstract boolean isGLReadDrawableAvailable();

  /**
   * Set the read GLDrawable for read framebuffer operations.<br>
   * The caller should query if this feature is supported via {@link #isGLReadDrawableAvailable()}.
   *
   * @param read the read GLDrawable for read framebuffer operations.
   * If null is passed, the default write drawable will be set.
   *
   * @throws GLException in case a read drawable is not supported
   *         and the given drawable is not null and not equal to the internal write drawable.
   *
   * @see #isGLReadDrawableAvailable()
   * @see #getGLReadDrawable()
   */
  public abstract void setGLReadDrawable(GLDrawable read);

  /**
   * Returns the read GLDrawable this context uses for read framebuffer operations.
   * @see #isGLReadDrawableAvailable()
   * @see #setGLReadDrawable(javax.media.opengl.GLDrawable)
   */
  public abstract GLDrawable getGLReadDrawable();

  /**
   * Makes this GLContext current on the calling thread.
   *
   * There are two return values that indicate success and one that
   * indicates failure. A return value of CONTEXT_CURRENT_NEW
   * indicates that that context has been made current, and that
   * this is the first time this context has been made current, or
   * that the state of the underlying context or drawable may have
   * changed since the last time this context was made current. In
   * this case, the application may wish to initialize the state.  A
   * return value of CONTEXT_CURRENT indicates that the context has
   * been made currrent, with its previous state restored.
   * 
   * If the context could not be made current (for example, because
   * the underlying drawable has not ben realized on the display) ,
   * a value of CONTEXT_NOT_CURRENT is returned.
   *
   * If the context is in use by another thread at the time of the
   * call, then if isSynchronized() is true the call will
   * block. If isSynchronized() is false, an exception will be
   * thrown and the context will remain current on the other thread.
   *
   * @return CONTEXT_CURRENT if the context was successfully made current
   * @return CONTEXT_CURRENT_NEW if the context was successfully made
   * current, but need to be initialized.
   *
   * @return CONTEXT_NOT_CURRENT if the context could not be made current.
   *
   * @throws GLException if synchronization is disabled and the
   * context is current on another thread, or because the context
   * could not be created or made current due to non-recoverable,
   * window system-specific errors.
   */
  public abstract int makeCurrent() throws GLException;

  /**
   * Releases control of this GLContext from the current thread.
   *
   * @throws GLException if the context had not previously been made
   * current on the current thread
   */
  public abstract void release() throws GLException;

  /**
   * Copies selected groups of OpenGL state variables from the
   * supplied source context into this one. The <code>mask</code>
   * parameter indicates which groups of state variables are to be
   * copied. <code>mask</code> contains the bitwise OR of the same
   * symbolic names that are passed to the GL command {@link
   * GL#glPushAttrib glPushAttrib}. The single symbolic constant
   * {@link GL2#GL_ALL_ATTRIB_BITS GL_ALL_ATTRIB_BITS} can be used to
   * copy the maximum possible portion of rendering state. <P>
   *
   * Not all values for GL state can be copied. For example, pixel
   * pack and unpack state, render mode state, and select and feedback
   * state are not copied. The state that can be copied is exactly the
   * state that is manipulated by the GL command {@link
   * GL2#glPushAttrib glPushAttrib}. <P>
   *
   * On most platforms, this context may not be current to any thread,
   * including the calling thread, when this method is called. Some
   * platforms have additional requirements such as whether this
   * context or the source context must occasionally be made current
   * in order for the results of the copy to be seen; these
   * requirements are beyond the scope of this specification.
   *
   * @param source the source OpenGL context from which to copy state
   * @param mask a mask of symbolic names indicating which groups of state to copy

   * @throws GLException if an OpenGL-related error occurred
   */
  public abstract void copy(GLContext source, int mask) throws GLException;

  /**
   * Returns the GL object bound to this thread current context.
   * If no context is current, throw an GLException
   *
   * @return the current context's GL object on this thread
   * @throws GLException if no context is current
   */
  public static GL getCurrentGL() throws GLException {
    GLContext glc = getCurrent();
    if(null==glc) {
        throw new GLException("No OpenGL context current on this thread");
    }
    return glc.getGL();
  }

  /**
   * Returns this thread current context.
   * If no context is current, returns null.
   *
   * @return the context current on this thread, or null if no context
   * is current.
   */
  public static GLContext getCurrent() {
    return currentContext.get();
  }

  /**
   * @return true if this GLContext is current on this thread
   */
  public final boolean isCurrent() {
    return getCurrent() == this ; 
  }

  /**
   * @throws GLException if this GLContext is not current on this thread
   */
  public final void validateCurrent() throws GLException {  
    if(getCurrent() != this) {
        throw new GLException("Given GL context not current");
    }
  }
  
  /**
   * Sets the thread-local variable returned by {@link #getCurrent}
   * and has no other side-effects. For use by third parties adding
   * new GLContext implementations; not for use by end users.
   */
  protected static void setCurrent(GLContext cur) {
    if(TRACE_SWITCH) {
       System.err.println("GLContext.ContextSwitch: - setCurrent() - "+Thread.currentThread().getName()+": "+cur);
    }
    currentContext.set(cur);
  }
  
  /**
   * Destroys this OpenGL context and frees its associated
   * resources.
   * <p>
   * The context may be current w/o recursion when calling <code>destroy()</code>,
   * in which case this method destroys the context and releases the lock.
   * </p> 
   */
  public abstract void destroy();

  /**
   * Returns true if 'makeCurrent' will exhibit synchronized behavior.
   */
  public abstract boolean isSynchronized();
    
  /** 
   * Determines whether 'makeCurrent' will exhibit synchronized behavior.
   */
  public abstract void setSynchronized(boolean isSynchronized);

  /**
   * Returns the GL pipeline object for this GLContext.
   *
   * @return the aggregated GL instance, or null if this context was not yet made current.
   */
  public abstract GL getGL();

  /**
   * Sets the GL pipeline object for this GLContext.
   *
   * @return the set GL pipeline or null if not successful
   */
  public abstract GL setGL(GL gl);

  /**
   * Returns the native GL context handle
   */
  public final long getHandle() { return contextHandle; }

  /** 
   * Indicates whether the underlying OpenGL context has been created. 
   */
  public final boolean isCreated() {
    return 0 != contextHandle;
  }

  /**
   * Returns the attached user object for the given name to this GLContext.
   */
  public final Object getAttachedObject(int name) {
    return attachedObjectsByInt.get(name);
  }

  /**
   * Returns the attached user object for the given name to this GLContext.
   */
  public final Object getAttachedObject(String name) {
    return attachedObjectsByString.get(name);
  }

  /**
   * Sets the attached user object for the given name to this GLContext.
   * Returns the previously set object or null.
   */
  public final Object attachObject(int name, Object obj) {
    return attachedObjectsByInt.put(name, obj);
  }

  public final Object detachObject(int name) {
      return attachedObjectsByInt.remove(name);
  }
  
  /**
   * Sets the attached user object for the given name to this GLContext.
   * Returns the previously set object or null.
   */
  public final Object attachObject(String name, Object obj) {
    return attachedObjectsByString.put(name, obj);
  }

  public final Object detachObject(String name) {
      return attachedObjectsByString.remove(name);
  }
  
  /**
   * Classname, GL, GLDrawable
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(getClass().getSimpleName());
    sb.append(" [");
    this.append(sb);
    sb.append("] ");
    return sb.toString();
  }

  public final StringBuffer append(StringBuffer sb) {
    sb.append("OpenGL ");
    sb.append(getGLVersionMajor());
    sb.append(".");
    sb.append(getGLVersionMinor());
    sb.append(", options 0x");
    sb.append(Integer.toHexString(ctxOptions));
    sb.append(", ");
    sb.append(getGLVersion());
    sb.append(", handle ");
    sb.append(toHexString(contextHandle));
    sb.append(", ");
    sb.append(getGL());
    if(getGLDrawable()!=getGLReadDrawable()) {
        sb.append(",\n\tRead Drawable : ");
        sb.append(getGLReadDrawable());
        sb.append(",\n\tWrite Drawable: ");
        sb.append(getGLDrawable());
    } else {
        sb.append(",\n\tDrawable: ");
        sb.append(getGLDrawable());
    }
    sb.append(", lock ");
    sb.append(lock.toString());
    return sb;
  }

  /**
   * Returns true if the specified OpenGL core- or extension-function can be
   * successfully called using this GL context given the current host (OpenGL
   * <i>client</i>) and display (OpenGL <i>server</i>) configuration.
   *
   * See {@link GL#isFunctionAvailable(String)} for more details.
   *
   * @param glFunctionName the name of the OpenGL function (e.g., use
   * "glPolygonOffsetEXT" or "glPolygonOffset" to check if the {@link
   * javax.media.opengl.GL#glPolygonOffset(float,float)} is available).
   */
  public abstract boolean isFunctionAvailable(String glFunctionName);
  
  /**
   * Returns true if the specified OpenGL extension can be
   * successfully called using this GL context given the current host (OpenGL
   * <i>client</i>) and display (OpenGL <i>server</i>) configuration.
   *
   * See {@link GL#isExtensionAvailable(String)} for more details.
   *
   * @param glExtensionName the name of the OpenGL extension (e.g.,
   * "GL_VERTEX_PROGRAM_ARB").
   */
  public abstract boolean isExtensionAvailable(String glExtensionName);
  
  /** Returns the number of platform extensions */
  public abstract int getPlatformExtensionCount();
  
  /** Returns a non-null (but possibly empty) string containing the
      space-separated list of available platform-dependent (e.g., WGL,
      GLX) extensions. Can only be called while this context is
      current. */
  public abstract String getPlatformExtensionsString();

  /** Returns the number of OpenGL extensions */
  public abstract int getGLExtensionCount();
  
  /** Returns a non-null (but possibly empty) string containing the
      space-separated list of available extensions.
      Can only be called while this context is current.
      This is equivalent to
      {@link javax.media.opengl.GL#glGetString(int) glGetString}({@link javax.media.opengl.GL#GL_EXTENSIONS GL_EXTENSIONS})
   */
  public abstract String getGLExtensionsString();

  /**
   * @return Additional context creation flags, supported: {@link GLContext#CTX_OPTION_DEBUG}.
   */
  public abstract int getContextCreationFlags();

  /**
   * @param flags Additional context creation flags, supported: {@link GLContext#CTX_OPTION_DEBUG}.
   *              Unsupported flags are masked out.
   *              Only affects this context state if not created yet via {@link #makeCurrent()}.
   * @see #enableGLDebugMessage(boolean)
   * @see GLAutoDrawable#setContextCreationFlags(int)
   */
  public abstract void setContextCreationFlags(int flags);
    
  /** 
   * Returns a valid OpenGL version string, ie<br>
   * <pre>
   *     major.minor ([option]?[options,]*) - gl-version
   * </pre><br>
   *
   * <ul>
   *   <li> options
   *   <ul>
   *     <li> <code>ES profile</code> ES profile</li>
   *     <li> <code>Compatibility profile</code> Compatibility profile including fixed function pipeline and deprecated functionality</li>
   *     <li> <code>Core profile</code> Core profile</li>
   *     <li> <code>forward</code> Forward profile excluding deprecated functionality</li>
   *     <li> <code>arb</code> refers to an ARB_create_context created context</li>
   *     <li> <code>debug</code> refers to a debug context</li>
   *     <li> <code>ES2 compatible</code> refers to an ES2 compatible implementation</li>
   *     <li> <code>software</code> refers to a software implementation of the rasterizer</li>
   *     <li> <code>hardware</code> refers to a hardware implementation of the rasterizer</li>
   *   </ul></li>
   *   <li> <i>gl-version</i> the GL_VERSION string</li>
   * </ul>
   *
   * e.g.:
   * <table border="0">
   * <tr> <td></td> <td></td> </tr>
   * <tr>
   * <td>row 2, cell 1</td>
   * <td>row 2, cell 2</td>
   * </tr>
   * </table> 
   *
   * <table border="0">
   *     <tr><td></td>   <td>ES2</td>  <td><code>2.0 (ES profile, ES2 compatible, hardware) - 2.0 ES Profile</code></td></tr>
   *     <tr><td>ATI</td><td>GL2</td>  <td><code>3.0 (Compatibility profile, arb, hardware) - 3.2.9704 Compatibility Profile Context</code></td></tr>
   *     <tr><td>ATI</td><td>GL3</td>  <td><code>3.3 (Core profile, any, new, hardware) - 1.4 (3.2.9704 Compatibility Profile Context)</code></td></tr>
   *     <tr><td>ATI</td><td>GL3bc</td><td><code>3.3 (Compatibility profile, arb, hardware) - 1.4 (3.2.9704 Compatibility Profile Context)</code></td></tr>
   *     <tr><td>NV</td><td>GL2</td>   <td><code>3.0 (Compatibility profile, arb, hardware) - 3.0.0 NVIDIA 195.36.07.03</code></td></tr>
   *     <tr><td>NV</td><td>GL3</td>   <td><code>3.3 (Core profile, arb, hardware) - 3.3.0 NVIDIA 195.36.07.03</code></td></tr>
   *     <tr><td>NV</td><td>GL3bc</td> <td><code>3.3 (Compatibility profile, arb, hardware) - 3.3.0 NVIDIA 195.36.07.03</code></td></tr>
   *     <tr><td>NV</td><td>GL2</td>   <td><code>3.0 (Compatibility profile, arb, ES2 compatible, hardware) - 3.0.0 NVIDIA 290.10</code></td></tr>
   * </table> 
   */
  public final String getGLVersion() {
    return ctxVersionString;
  }

  public final int getGLVersionMajor() { return ctxMajorVersion; }
  public final int getGLVersionMinor() { return ctxMinorVersion; }
  public final boolean isGLCompatibilityProfile() { return ( 0 != ( CTX_PROFILE_COMPAT & ctxOptions ) ); }
  public final boolean isGLCoreProfile()          { return ( 0 != ( CTX_PROFILE_CORE   & ctxOptions ) ); }
  public final boolean isGLForwardCompatible()    { return ( 0 != ( CTX_OPTION_FORWARD & ctxOptions ) ); }
  public final boolean isGLDebugEnabled()         { return ( 0 != ( CTX_OPTION_DEBUG   & ctxOptions ) ); }
  public final boolean isCreatedWithARBMethod()   { return ( 0 != ( CTX_IS_ARB_CREATED & ctxOptions ) ); }
  
  /**
   * @return true if this context is an ES2 context or implements 
   *         the extension <code>GL_ARB_ES2_compatibility</code>, otherwise false 
   */
  public final boolean isGLES2Compatible() {
      return 0 != ( ctxOptions & CTX_IMPL_ES2_COMPAT ) ;
  }
  
  public final boolean hasGLSL() {
      return isGL2ES2() ;
  }


  public final boolean isGL4bc() {
      return ctxMajorVersion>=4 && 0 != (ctxOptions & CTX_IS_ARB_CREATED)
                                && 0 != (ctxOptions & CTX_PROFILE_COMPAT);
  }

  public final boolean isGL4() {
      return ctxMajorVersion>=4 && 0 != (ctxOptions & CTX_IS_ARB_CREATED)
                                && 0 != (ctxOptions & (CTX_PROFILE_COMPAT|CTX_PROFILE_CORE));
  }

  public final boolean isGL3bc() {
      return ( ctxMajorVersion>3 || ctxMajorVersion==3 && ctxMinorVersion>=1 )
             && 0 != (ctxOptions & CTX_IS_ARB_CREATED)
             && 0 != (ctxOptions & CTX_PROFILE_COMPAT);
  }

  public final boolean isGL3() {
      return ( ctxMajorVersion>3 || ctxMajorVersion==3 && ctxMinorVersion>=1 )
             && 0 != (ctxOptions & CTX_IS_ARB_CREATED)
             && 0 != (ctxOptions & (CTX_PROFILE_COMPAT|CTX_PROFILE_CORE));
  }

  public final boolean isGL2() {
      return ctxMajorVersion>=1 && 0!=(ctxOptions & CTX_PROFILE_COMPAT);
  }

  public final boolean isGL2GL3() {
      return isGL2() || isGL3();
  }

  public final boolean isGLES1() {
      return ctxMajorVersion==1 && 0 != ( ctxOptions & CTX_PROFILE_ES ) ;
  }

  public final boolean isGLES2() {
      return ctxMajorVersion==2 && 0 != ( ctxOptions & CTX_PROFILE_ES ) ;
  }

  public final boolean isGLES() {
      return 0 != ( CTX_PROFILE_ES & ctxOptions ) ;
  }

  public final boolean isGL2ES1() {
      return isGL2() || isGLES1() ;
  }

  public final boolean isGL2ES2() {
      return isGL2GL3() || isGLES2() ;
  }

  public final void setSwapInterval(int interval) {
    if (!isCurrent()) {
        throw new GLException("This context is not current. Current context: "+getCurrent()+", this context "+this);
    }
    setSwapIntervalImpl(interval);
  }
  protected void setSwapIntervalImpl(int interval) { /** nop per default .. **/  }
  protected int currentSwapInterval = -1; // default: not set yet ..
  public int getSwapInterval() {
    return currentSwapInterval;
  }
  
  public final boolean queryMaxSwapGroups(int[] maxGroups, int maxGroups_offset,
                                          int[] maxBarriers, int maxBarriers_offset) {
      
    if (!isCurrent()) {
        throw new GLException("This context is not current. Current context: "+getCurrent()+", this context "+this);
    }
    return queryMaxSwapGroupsImpl(maxGroups, maxGroups_offset, maxBarriers, maxBarriers_offset);
  }
  protected boolean queryMaxSwapGroupsImpl(int[] maxGroups, int maxGroups_offset,
                                          int[] maxBarriers, int maxBarriers_offset) { return false; }
  public final boolean joinSwapGroup(int group) {
    if (!isCurrent()) {
        throw new GLException("This context is not current. Current context: "+getCurrent()+", this context "+this);
    }
    return joinSwapGroupImpl(group);
  }
  protected boolean joinSwapGroupImpl(int group) { /** nop per default .. **/  return false; }
  protected int currentSwapGroup = -1; // default: not set yet ..  
  public int getSwapGroup() {
      return currentSwapGroup;
  }
  public final boolean bindSwapBarrier(int group, int barrier) {
    if (!isCurrent()) {
        throw new GLException("This context is not current. Current context: "+getCurrent()+", this context "+this);
    }
    return bindSwapBarrierImpl(group, barrier);    
  }
  protected boolean bindSwapBarrierImpl(int group, int barrier) { /** nop per default .. **/  return false; }

  
  /**
   * @return The extension implementing the GLDebugOutput feature, 
   *         either <i>GL_ARB_debug_output</i> or <i>GL_AMD_debug_output</i>. 
   *         If unavailable or called before initialized via {@link #makeCurrent()}, <i>null</i> is returned. 
   */
  public abstract String getGLDebugMessageExtension();

  /**
   * @return the current synchronous debug behavior via
   * @see #setSynchronous(boolean)
   */
  public abstract boolean isGLDebugSynchronous();
    
  /**
   * Enables or disables the synchronous debug behavior via
   * {@link GL2GL3#GL_DEBUG_OUTPUT_SYNCHRONOUS_ARB glEnable/glDisable(GL_DEBUG_OUTPUT_SYNCHRONOUS_ARB)}, 
   * if extension is {@link #GL_ARB_debug_output}.
   * There is no equivalent for {@link #GL_AMD_debug_output}.
   * <p> The default is <code>true</code>, ie {@link GL2GL3#GL_DEBUG_OUTPUT_SYNCHRONOUS_ARB}.</p> 
   */
  public abstract void setGLDebugSynchronous(boolean synchronous);
  
  /**
   * @return true if the GLDebugOutput feature is enabled or not.
   */
  public abstract boolean isGLDebugMessageEnabled();
  
  /**
   * Enables or disables the GLDebugOutput feature of extension <i>GL_ARB_debug_output</i> 
   * or <i>GL_AMD_debug_output</i>, if available.
   * 
   * <p>To enable the GLDebugOutput feature {@link #enableGLDebugMessage(boolean) enableGLDebugMessage(true)}
   * or {@link #setContextCreationFlags(int) setContextCreationFlags}({@link GLContext#CTX_OPTION_DEBUG})
   * shall be called <b>before</b> context creation via {@link #makeCurrent()}!</p>
   * 
   * <p>In case {@link GLAutoDrawable} are being used, 
   * {@link GLAutoDrawable#setContextCreationFlags(int) glAutoDrawable.setContextCreationFlags}({@link GLContext#CTX_OPTION_DEBUG})
   * shall be issued before context creation via {@link #makeCurrent()}!</p>
   * 
   * <p>After context creation, the GLDebugOutput feature may be enabled or disabled at any time using this method.</p>
   * 
   * @param enable If true enables, otherwise disables the GLDebugOutput feature.
   * 
   * @throws GLException if this context is not current or GLDebugOutput registration failed (enable)
   * 
   * @see #setContextCreationFlags(int)
   * @see #addGLDebugListener(GLDebugListener)
   * @see GLAutoDrawable#setContextCreationFlags(int)
   */
  public abstract void enableGLDebugMessage(boolean enable) throws GLException;
  
  /**
   * Add {@link GLDebugListener}.<br>
   * 
   * @param listener {@link GLDebugListener} handling {@GLDebugMessage}s
   * @see #enableGLDebugMessage(boolean) 
   * @see #removeGLDebugListener(GLDebugListener)
   */
  public abstract void addGLDebugListener(GLDebugListener listener);
  
  /**
   * Remove {@link GLDebugListener}.<br>
   * 
   * @param listener {@link GLDebugListener} handling {@GLDebugMessage}s
   * @see #enableGLDebugMessage(boolean) 
   * @see #addGLDebugListener(GLDebugListener)
   */
  public abstract void removeGLDebugListener(GLDebugListener listener);
  
  /**
   * Generic entry for {@link GL2GL3#glDebugMessageControlARB(int, int, int, int, IntBuffer, boolean)} 
   * and {@link GL2GL3#glDebugMessageEnableAMD(int, int, int, IntBuffer, boolean)} of the GLDebugOutput feature.
   * @see #enableGLDebugMessage(boolean) 
   */
  public abstract void glDebugMessageControl(int source, int type, int severity, int count, IntBuffer ids, boolean enabled);
  
  /**
   * Generic entry for {@link GL2GL3#glDebugMessageControlARB(int, int, int, int, int[], int, boolean)} 
   * and {@link GL2GL3#glDebugMessageEnableAMD(int, int, int, int[], int, boolean)} of the GLDebugOutput feature. 
   * @see #enableGLDebugMessage(boolean) 
   */
  public abstract void glDebugMessageControl(int source, int type, int severity, int count, int[] ids, int ids_offset, boolean enabled);
  
  /**
   * Generic entry for {@link GL2GL3#glDebugMessageInsertARB(int, int, int, int, int, String)} 
   * and {@link GL2GL3#glDebugMessageInsertAMD(int, int, int, int, String)} of the GLDebugOutput feature.
   * @see #enableGLDebugMessage(boolean) 
   */
  public abstract void glDebugMessageInsert(int source, int type, int id, int severity, String buf);
  
  public static final int GL_VERSIONS[][] = {
      /* 0.*/ { -1 },
      /* 1.*/ { 0, 1, 2, 3, 4, 5 },
      /* 2.*/ { 0, 1 },
      /* 3.*/ { 0, 1, 2, 3 },
      /* 4.*/ { 0, 1, 2 } };

  public static final int getMaxMajor() {
      return GL_VERSIONS.length-1;
  }

  public static final int getMaxMinor(int major) {
      if(1>major || major>=GL_VERSIONS.length) return -1;
      return GL_VERSIONS[major].length-1;
  }

  public static final boolean isValidGLVersion(int major, int minor) {
      if(1>major || major>=GL_VERSIONS.length) return false;
      if(0>minor || minor>=GL_VERSIONS[major].length) return false;
      return true;
  }

  public static final boolean decrementGLVersion(int major[], int minor[]) {
      if(null==major || major.length<1 ||null==minor || minor.length<1) {
          throw new GLException("invalid array arguments");
      }
      int m = major[0];
      int n = minor[0];
      if(!isValidGLVersion(m, n)) return false;

      // decrement ..
      n -= 1;
      if(n < 0) {
          m -= 1;
          n = GL_VERSIONS[m].length-1;
      }
      if(!isValidGLVersion(m, n)) return false;
      major[0]=m;
      minor[0]=n;

      return true;
  }

  protected static int composeBits(int a8, int b8, int c16) {
    return  ( ( a8    & 0x000000FF ) << 24 ) |
            ( ( b8    & 0x000000FF ) << 16 ) |
            ( ( c16   & 0x0000FFFF )       ) ;
  }

  private static void validateProfileBits(int bits, String argName) {
    int num = 0;
    if( 0 != ( CTX_PROFILE_COMPAT & bits ) ) { num++; }
    if( 0 != ( CTX_PROFILE_CORE   & bits ) ) { num++; }
    if( 0 != ( CTX_PROFILE_ES     & bits ) ) { num++; }
    if(1!=num) {
        throw new GLException("Internal Error: "+argName+": 1 != num-profiles: "+num);
    }
  }

  //
  // version mapping
  //

  /**
   * @see #getDeviceVersionAvailableKey(javax.media.nativewindow.AbstractGraphicsDevice, int, int) 
   */
  protected static /*final*/ HashMap<String, Integer> deviceVersionAvailable = new HashMap<String, Integer>();

  /**
   * @see #getUniqueDeviceString(javax.media.nativewindow.AbstractGraphicsDevice)
   */
  private static /*final*/ HashSet<String> deviceVersionsAvailableSet = new HashSet<String>();

  protected static String getDeviceVersionAvailableKey(AbstractGraphicsDevice device, int major, int profile) {
      return device.getUniqueID() + "-" + toHexString(composeBits(major, profile, 0));
  }

  protected static boolean getAvailableGLVersionsSet(AbstractGraphicsDevice device) {
      synchronized ( deviceVersionsAvailableSet ) {
        return deviceVersionsAvailableSet.contains(device.getUniqueID());
      }
  }

  protected static void setAvailableGLVersionsSet(AbstractGraphicsDevice device) {
      synchronized ( deviceVersionsAvailableSet ) {
          String devKey = device.getUniqueID();
          if ( deviceVersionsAvailableSet.contains(devKey) ) {
              throw new InternalError("Already set: "+devKey);
          }
          deviceVersionsAvailableSet.add(devKey);
          if (DEBUG) {
            System.err.println(getThreadName() + ": !!! createContextARB: SET mappedVersionsAvailableSet "+devKey);
            // Thread.dumpStack();
          }
      }
  }
  
  /** clears the device/context mappings as well as the GL/GLX proc address tables. */
  protected static void shutdown() {
      deviceVersionAvailable.clear();
      deviceVersionsAvailableSet.clear();      
      GLContextImpl.shutdownImpl(); // well ..
  }

  /**
   * Called by {@link jogamp.opengl.GLContextImpl#createContextARBMapVersionsAvailable} not intended to be used by
   * implementations. However, if {@link #createContextARB} is not being used within
   * {@link javax.media.opengl.GLDrawableFactory#getOrCreateSharedContext(javax.media.nativewindow.AbstractGraphicsDevice)},
   * GLProfile has to map the available versions.
   *
   * @param reqMajor Key Value either 1, 2, 3 or 4
   * @param profile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   * @return the old mapped value
   *
   * @see #createContextARBMapVersionsAvailable
   */
  protected static Integer mapAvailableGLVersion(AbstractGraphicsDevice device,
                                                 int reqMajor, int profile, int resMajor, int resMinor, int resCtp)
  {
    validateProfileBits(profile, "profile");
    validateProfileBits(resCtp, "resCtp");

    String key = getDeviceVersionAvailableKey(device, reqMajor, profile);
    Integer val = new Integer(composeBits(resMajor, resMinor, resCtp));
    synchronized(deviceVersionAvailable) {
        val = deviceVersionAvailable.put( key, val );
    }
    return val;
  }

  /**
   * @param device the device to request whether the profile is available for
   * @param reqMajor Key Value either 1, 2, 3 or 4
   * @param reqProfile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   * @return the available GL version as encoded with {@link #composeBits(int, int, int), otherwise <code>null</code>
   */
  protected static Integer getAvailableGLVersion(AbstractGraphicsDevice device, int reqMajor, int reqProfile)  {
    String key = getDeviceVersionAvailableKey(device, reqMajor, reqProfile);
    Integer val;
    synchronized(deviceVersionAvailable) {
        val = deviceVersionAvailable.get( key );
    }
    return val;
  }

  /**
   * @param reqMajor Key Value either 1, 2, 3 or 4
   * @param reqProfile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   * @param major if not null, returns the used major version
   * @param minor if not null, returns the used minor version
   * @param ctp if not null, returns the used context profile
   */
  protected static boolean getAvailableGLVersion(AbstractGraphicsDevice device, int reqMajor, int reqProfile, 
                                                 int[] major, int minor[], int ctp[]) {

    Integer valI = getAvailableGLVersion(device, reqMajor, reqProfile);
    if(null==valI) {
        return false;
    }

    int bits32 = valI.intValue();

    if(null!=major) {
        major[0] = ( bits32 & 0xFF000000 ) >> 24 ;
    }
    if(null!=minor) {
        minor[0] = ( bits32 & 0x00FF0000 ) >> 16 ;
    }
    if(null!=ctp) {
        ctp[0]   = ( bits32 & 0x0000FFFF )       ;
    }
    return true;
  }
  
  /** 
   * returns the highest GLProfile string regarding the implementation version and context profile bits.
   * @throws GLException if version and context profile bits could not be mapped to a GLProfile
   */
  protected static String getGLProfile(int major, int minor, int ctp) 
          throws GLException {
    if(0 != ( CTX_PROFILE_COMPAT & ctp )) {        
        if(major >= 4)                            { return GLProfile.GL4bc; }
        else if(major == 3 && minor >= 1)         { return GLProfile.GL3bc; }
        else                                      { return GLProfile.GL2; }        
    } else if(0 != ( CTX_PROFILE_CORE & ctp )) {
        if(major >= 4)                            { return GLProfile.GL4; }
        else if(major == 3 && minor >= 1)         { return GLProfile.GL3; }        
    } else if(0 != ( CTX_PROFILE_ES & ctp )) {
        if(major == 2)                            { return GLProfile.GLES2; }
        else if(major == 1)                       { return GLProfile.GLES1; }                
    }
    throw new GLException("Unhandled OpenGL version/profile: "+GLContext.getGLVersion(major, minor, ctp, null));
  }
  
  /** 
   * @param major Key Value either 1, 2, 3 or 4
   * @param profile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   * @return the highest GLProfile string regarding the version and profile bits.
   * @throws GLException if version and context profile bits could not be mapped to a GLProfile
   */  
  public static String getAvailableGLProfile(AbstractGraphicsDevice device, int reqMajor, int reqProfile) 
          throws GLException {
    int major[] = { 0 };
    int minor[] = { 0 };
    int ctp[] = { 0 };
    if(GLContext.getAvailableGLVersion(device, reqMajor, reqProfile, major, minor, ctp)) {
        return GLContext.getGLProfile(major[0], minor[0], ctp[0]);
    }
    return null;
  }

  /**
   * @param device the device to request whether the profile is available for
   * @param reqMajor Key Value either 1, 2, 3 or 4
   * @param reqProfile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   * @param isHardware return value of one boolean, whether the profile is a hardware rasterizer or not
   * @return true if the requested GL version is available regardless of a software or hardware rasterizer, otherwise false.
   */
  public static boolean isGLVersionAvailable(AbstractGraphicsDevice device, int reqMajor, int reqProfile, boolean isHardware[]) {
      Integer valI = getAvailableGLVersion(device, reqMajor, reqProfile);
      if(null==valI) {
          return false;
      }
      isHardware[0] = 0 == ( valI.intValue() & GLContext.CTX_IMPL_ACCEL_SOFT ) ;
      return true;
  }

  public static boolean isGLES1Available(AbstractGraphicsDevice device, boolean isHardware[]) {
      return isGLVersionAvailable(device, 1, GLContext.CTX_PROFILE_ES, isHardware);
  }

  public static boolean isGLES2Available(AbstractGraphicsDevice device, boolean isHardware[]) {
      return isGLVersionAvailable(device, 2, GLContext.CTX_PROFILE_ES, isHardware);
  }

  public static boolean isGL4bcAvailable(AbstractGraphicsDevice device, boolean isHardware[]) {
      return isGLVersionAvailable(device, 4, CTX_PROFILE_COMPAT, isHardware);
  }

  public static boolean isGL4Available(AbstractGraphicsDevice device, boolean isHardware[]) {
      return isGLVersionAvailable(device, 4, CTX_PROFILE_CORE, isHardware);
  }

  public static boolean isGL3bcAvailable(AbstractGraphicsDevice device, boolean isHardware[]) {
      return isGLVersionAvailable(device, 3, CTX_PROFILE_COMPAT, isHardware);
  }

  public static boolean isGL3Available(AbstractGraphicsDevice device, boolean isHardware[]) {
      return isGLVersionAvailable(device, 3, CTX_PROFILE_CORE, isHardware);
  }

  public static boolean isGL2Available(AbstractGraphicsDevice device, boolean isHardware[]) {
    return isGLVersionAvailable(device, 2, CTX_PROFILE_COMPAT, isHardware);
  }

  /**
   * @param major Key Value either 1, 2, 3 or 4
   * @param profile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   */
  public static String getAvailableGLVersionAsString(AbstractGraphicsDevice device, int major, int profile) {
    int _major[] = { 0 };
    int _minor[] = { 0 };
    int _ctp[] = { 0 };
    if(getAvailableGLVersion(device, major, profile, _major, _minor, _ctp)) {
        return getGLVersion(_major[0], _minor[0], _ctp[0], null);
    }
    return null;
  }

  public static String getGLVersion(int major, int minor, int ctp, String gl_version) {
    boolean needColon = false;
    StringBuilder sb = new StringBuilder();
    sb.append(major);
    sb.append(".");
    sb.append(minor);
    sb.append(" (");
    needColon = appendString(sb, "ES profile",            needColon, 0 != ( CTX_PROFILE_ES & ctp ));
    needColon = appendString(sb, "Compatibility profile", needColon, 0 != ( CTX_PROFILE_COMPAT & ctp ));
    needColon = appendString(sb, "Core profile",          needColon, 0 != ( CTX_PROFILE_CORE & ctp ));
    needColon = appendString(sb, "forward",               needColon, 0 != ( CTX_OPTION_FORWARD & ctp )); 
    needColon = appendString(sb, "arb",                   needColon, 0 != ( CTX_IS_ARB_CREATED & ctp ));
    needColon = appendString(sb, "debug",                 needColon, 0 != ( CTX_OPTION_DEBUG & ctp ));    
    needColon = appendString(sb, "ES2 compatible",        needColon, 0 != ( CTX_IMPL_ES2_COMPAT & ctp ));
    if( 0 != ( CTX_IMPL_ACCEL_SOFT & ctp ) ) {
        needColon = appendString(sb, "software",          needColon, true);
    } else {
        needColon = appendString(sb, "hardware",          needColon, true);
    }
    sb.append(")");
    if(null!=gl_version) {
        sb.append(" - ");
        sb.append(gl_version);
    }
    return sb.toString();
  }

  //
  // internal string utils 
  //
  
  protected static String toHexString(int hex) {
    return "0x" + Integer.toHexString(hex);
  }

  protected static String toHexString(long hex) {
    return "0x" + Long.toHexString(hex);
  }

  private static boolean appendString(StringBuilder sb, String string, boolean needColon, boolean condition) {
    if(condition) {
        if(needColon) {
            sb.append(", ");
        }
        sb.append(string);
        needColon=true;
    }
    return needColon;
  }
  
  protected static String getThreadName() {
    return Thread.currentThread().getName();
  }

}

