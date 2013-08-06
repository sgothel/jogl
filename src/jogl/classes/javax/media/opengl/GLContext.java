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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;

import javax.media.nativewindow.AbstractGraphicsDevice;

import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.common.util.VersionNumberString;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.GLRendererQuirks;

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
  public static final boolean TRACE_SWITCH = Debug.isPropertyDefined("jogl.debug.GLContext.TraceSwitch", true);
  public static final boolean DEBUG_TRACE_SWITCH = DEBUG || TRACE_SWITCH;  

  /** 
   * If <code>true</code> (default), bootstrapping the available GL profiles 
   * will use the highest compatible GL context for each profile, 
   * hence skipping querying lower profiles if a compatible higher one is found:
   * <ul>
   *   <li>4.2-core -> 4.2-core, 3.3-core</li>
   *   <li>4.2-comp -> 4.2-comp, 3.3-comp, 2</li>
   * </ul>
   * Otherwise the dedicated GL context would be queried and used:
   * <ul>
   *   <li>4.2-core -> 4.2-core</li>
   *   <li>3.3-core -> 3.3-core</li>
   *   <li>4.2-comp -> 4.2-comp</li>
   *   <li>3.3-comp -> 3.3-comp</li>
   *   <li>3.0-comp -> 2</li>
   * </ul>
   * Using aliasing speeds up initialization about:
   * <ul>
   *   <li>Linux x86_64 - Nvidia: 28%,  700ms down to 500ms</li> 
   *   <li>Linux x86_64 - AMD   : 40%, 1500ms down to 900ms</li> 
   * <p>
   * Can be turned off with property <code>jogl.debug.GLContext.NoProfileAliasing</code>.
   * </p>
   */
  public static final boolean PROFILE_ALIASING = !Debug.isPropertyDefined("jogl.debug.GLContext.NoProfileAliasing", true);
  
  protected static final boolean FORCE_NO_FBO_SUPPORT = Debug.isPropertyDefined("jogl.fbo.force.none", true);
  protected static final boolean FORCE_MIN_FBO_SUPPORT = Debug.isPropertyDefined("jogl.fbo.force.min", true);
  
  /** Reflects property jogl.debug.DebugGL. If true, the debug pipeline is enabled at context creation. */
  public static final boolean DEBUG_GL = Debug.isPropertyDefined("jogl.debug.DebugGL", true);
  /** Reflects property jogl.debug.TraceGL. If true, the trace pipeline is enabled at context creation. */
  public static final boolean TRACE_GL = Debug.isPropertyDefined("jogl.debug.TraceGL", true);

  /** Indicates that the context was not made current during the last call to {@link #makeCurrent makeCurrent}. */
  public static final int CONTEXT_NOT_CURRENT = 0;
  /** Indicates that the context was made current during the last call to {@link #makeCurrent makeCurrent}. */
  public static final int CONTEXT_CURRENT     = 1;
  /** Indicates that a newly-created context was made current during the last call to {@link #makeCurrent makeCurrent}. */
  public static final int CONTEXT_CURRENT_NEW = 2;

  /* Version 1.00, i.e. GLSL 1.00 for ES 2.0. */
  public static final VersionNumber Version100 = new VersionNumber(1,  0, 0);
  /* Version 1.10, i.e. GLSL 1.10 for GL 2.0. */
  public static final VersionNumber Version110 = new VersionNumber(1, 10, 0);
  /* Version 1.20, i.e. GLSL 1.20 for GL 2.1. */
  public static final VersionNumber Version120 = new VersionNumber(1, 20, 0);
  /* Version 1.30, i.e. GLSL 1.30 for GL 3.0. */
  public static final VersionNumber Version130 = new VersionNumber(1, 30, 0);
  /* Version 1.40, i.e. GLSL 1.40 for GL 3.1. */
  public static final VersionNumber Version140 = new VersionNumber(1, 40, 0);
  /* Version 1.50, i.e. GLSL 1.50 for GL 3.2. */
  public static final VersionNumber Version150 = new VersionNumber(1, 50, 0);
  
  /** Version 3.0. As an OpenGL version, it qualifies for desktop {@link #isGL2()} only, or ES 3.0. */
  public static final VersionNumber Version300 = new VersionNumber(3, 0, 0);
  
  /** Version 3.1. As an OpenGL version, it qualifies for {@link #isGL3core()}, {@link #isGL3bc()} and {@link #isGL3()} */
  public static final VersionNumber Version310 = new VersionNumber(3, 1, 0);
  
  /** Version 3.2. As an OpenGL version, it qualifies for geometry shader */
  public static final VersionNumber Version320 = new VersionNumber(3, 2, 0);
  
  /** Version 4.3. As an OpenGL version, it qualifies for <code>GL_ARB_ES3_compatibility</code> */
  public static final VersionNumber Version430 = new VersionNumber(4, 3, 0);
  
  protected static final VersionNumber Version800 = new VersionNumber(8, 0, 0);

  //
  // Cached keys, bits [0..15]
  //
  
  /** Context option bits, full bit mask covering bits [0..15], i.e. <code>0x0000FFFF</code>, {@value}. */
  protected static final int CTX_IMPL_FULL_MASK = 0x0000FFFF;
  
  /** Context option bits, cached bit mask covering 9 bits [0..8], i.e. <code>0x000001FF</code>, {@value}. Leaving 7 bits for non cached options, i.e. 9:7. */
  protected static final int CTX_IMPL_CACHE_MASK = 0x000001FF;
  
  /** <code>ARB_create_context</code> related: created via ARB_create_context. Cache key value. See {@link #getAvailableContextProperties(AbstractGraphicsDevice, GLProfile)}. */
  protected static final int CTX_IS_ARB_CREATED  = 1 <<  0;
  /** <code>ARB_create_context</code> related: desktop compatibility profile. Cache key value. See {@link #isGLCompatibilityProfile()}, {@link #getAvailableContextProperties(AbstractGraphicsDevice, GLProfile)}. */
  protected static final int CTX_PROFILE_COMPAT  = 1 <<  1;
  /** <code>ARB_create_context</code> related: desktop core profile. Cache key value. See {@link #isGLCoreProfile()}, {@link #getAvailableContextProperties(AbstractGraphicsDevice, GLProfile)}. */
  protected static final int CTX_PROFILE_CORE    = 1 <<  2;
  /** <code>ARB_create_context</code> related: ES profile. Cache key value. See {@link #isGLES()}, {@link #getAvailableContextProperties(AbstractGraphicsDevice, GLProfile)}. */
  protected static final int CTX_PROFILE_ES      = 1 <<  3;
  /** <code>ARB_create_context</code> related: flag forward compatible. Cache key value. See {@link #getAvailableContextProperties(AbstractGraphicsDevice, GLProfile)}. */
  protected static final int CTX_OPTION_FORWARD  = 1 <<  4;
  /** <code>ARB_create_context</code> related: flag debug. Cache key value. See {@link #setContextCreationFlags(int)}, {@link GLAutoDrawable#setContextCreationFlags(int)}, {@link #getAvailableContextProperties(AbstractGraphicsDevice, GLProfile)}. */
  public static final int CTX_OPTION_DEBUG       = 1 <<  5;
  /** Context uses software rasterizer, otherwise hardware rasterizer. Cache key value. See {@link #isHardwareRasterizer()}, {@link #getAvailableContextProperties(AbstractGraphicsDevice, GLProfile)}. */
  protected static final int CTX_IMPL_ACCEL_SOFT = 1 <<  6;

  //
  // Non cached keys, bits [9..15]
  //
  
  /** <code>GL_ARB_ES2_compatibility</code> implementation related: Context is compatible w/ ES2. Not a cache key. See {@link #isGLES2Compatible()}, {@link #getAvailableContextProperties(AbstractGraphicsDevice, GLProfile)}. */
  protected static final int CTX_IMPL_ES2_COMPAT = 1 <<  9;

  /** <code>GL_ARB_ES3_compatibility</code> implementation related: Context is compatible w/ ES3. Not a cache key. See {@link #isGLES3Compatible()}, {@link #getAvailableContextProperties(AbstractGraphicsDevice, GLProfile)}. */
  protected static final int CTX_IMPL_ES3_COMPAT = 1 << 10;
  
  /** 
   * Context supports basic FBO, details see {@link #hasBasicFBOSupport()}.
   * Not a cache key.
   * @see #hasBasicFBOSupport()
   * @see #getAvailableContextProperties(AbstractGraphicsDevice, GLProfile)
   */
  protected static final int CTX_IMPL_FBO        = 1 << 11;

  /** 
   * Context supports <code>OES_single_precision</code>, fp32, fixed function point (FFP) compatibility entry points, 
   * see {@link #hasFP32CompatAPI()}.
   * Not a cache key.
   * @see #hasFP32CompatAPI()
   * @see #getAvailableContextProperties(AbstractGraphicsDevice, GLProfile)
   */
  protected static final int CTX_IMPL_FP32_COMPAT_API = 1 << 12;
  
  private static final ThreadLocal<GLContext> currentContext = new ThreadLocal<GLContext>();

  private final HashMap<String, Object> attachedObjects = new HashMap<String, Object>();

  // RecursiveLock maintains a queue of waiting Threads, ensuring the longest waiting thread will be notified at unlock.
  protected final RecursiveLock lock = LockFactory.createRecursiveLock();

  /** The underlying native OpenGL context */
  protected long contextHandle;

  protected GLContext() {
      resetStates();
  }

  protected VersionNumber ctxVersion;
  protected int ctxOptions;
  protected String ctxVersionString;
  protected VersionNumberString ctxVendorVersion;
  protected VersionNumber ctxGLSLVersion;
  private int currentSwapInterval;
  protected GLRendererQuirks glRendererQuirks;

  /** Did the drawable association changed ? see {@link GLRendererQuirks#NoSetSwapIntervalPostRetarget} */ 
  protected boolean drawableRetargeted; 
    
  protected void resetStates() {
      if (DEBUG) {
        System.err.println(getThreadName() + ": GLContext.resetStates()");
        // Thread.dumpStack();
      }
      ctxVersion = VersionNumberString.zeroVersion;
      ctxVendorVersion = VersionNumberString.zeroVersion;
      ctxOptions=0;
      ctxVersionString=null;
      ctxGLSLVersion = VersionNumber.zeroVersion;
      attachedObjects.clear();
      contextHandle=0;
      currentSwapInterval = -1;
      glRendererQuirks = null;
      drawableRetargeted = false;
  }

  /** 
   * Returns the instance of {@link GLRendererQuirks}, allowing one to determine workarounds.
   * @return instance of {@link GLRendererQuirks} if context was made current once, otherwise <code>null</code>.
   */
  public final GLRendererQuirks getRendererQuirks() { return glRendererQuirks; }
  
  /**
   * Returns true if the <code>quirk</code> exist in {@link #getRendererQuirks()}, otherwise false.
   * <p>
   * Convenience method for:
   * <pre>
   *    final GLRendererQuirks glrq = ctx.getRendererQuirks();
   *    boolean hasQuirk = null != glrq ? glrq.exist(quirk) : false ;
   * </pre>
   * </p>
   * @param quirk the quirk to be tested, e.g. {@link GLRendererQuirks#NoDoubleBufferedPBuffer}.
   * @throws IllegalArgumentException if the quirk is out of range
   */
  public final boolean hasRendererQuirk(int quirk) throws IllegalArgumentException { 
      return null != glRendererQuirks ? glRendererQuirks.exist(quirk) : false ; 
  }
  
  /**
   * Sets the read/write drawable for framebuffer operations.
   * <p>
   * If the arguments reflect the current state of this context
   * this method is a no-operation and returns the old and current {@link GLDrawable}.
   * </p>
   * <p>
   * If the context was current on this thread, it is being released before switching the drawable
   * and made current afterwards. However the user shall take extra care that not other thread
   * attempts to make this context current. Otherwise a race condition may happen.
   * </p>
   * @param readWrite The read/write drawable for framebuffer operations, maybe <code>null</code> to remove association.
   * @param setWriteOnly Only change the write-drawable, if <code>setWriteOnly</code> is <code>true</code> and 
   *                     if the {@link #getGLReadDrawable() read-drawable} differs 
   *                     from the {@link #getGLDrawable() write-drawable}. 
   *                     Otherwise set both drawables, read and write.
   * @return The previous read/write drawable
   *
   * @throws GLException in case <code>null</code> is being passed or 
   *                     this context is made current on another thread.
   *
   * @see #isGLReadDrawableAvailable()
   * @see #getGLReadDrawable()
   * @see #setGLReadDrawable()
   * @see #getGLDrawable()
   */
  public abstract GLDrawable setGLDrawable(GLDrawable readWrite, boolean setWriteOnly);
  
  /**
   * Returns the write-drawable this context uses for framebuffer operations.
   * <p>
   * If the read-drawable has not been changed manually via {@link #setGLReadDrawable(GLDrawable)},
   * it equals to the write-drawable (default).
   * </p> 
   * @see #setGLDrawable(GLDrawable, boolean)
   * @see #setGLReadDrawable(GLDrawable)
   */
  public abstract GLDrawable getGLDrawable();

  /**
   * Query whether using a distinguished read-drawable is supported.
   * @return true if using a read-drawable is supported with your driver/OS, otherwise false.
   */
  public abstract boolean isGLReadDrawableAvailable();

  /**
   * Set the read-Drawable for read framebuffer operations.<br>
   * The caller should query if this feature is supported via {@link #isGLReadDrawableAvailable()}.
   * <p>
   * If the context was current on this thread, it is being released before switching the drawable
   * and made current afterwards. However the user shall take extra care that not other thread
   * attempts to make this context current. Otherwise a race condition may happen.
   * </p>
   *
   * @param read the read-drawable for read framebuffer operations.
   *             If null is passed, the default write drawable will be set.
   * @return the previous read-drawable
   *
   * @throws GLException in case a read drawable is not supported or
   *                     this context is made current on another thread.
   *
   * @see #isGLReadDrawableAvailable()
   * @see #getGLReadDrawable()
   */
  public abstract GLDrawable setGLReadDrawable(GLDrawable read);

  /**
   * Returns the read-Drawable this context uses for read framebuffer operations.
   * <p>
   * If the read-drawable has not been changed manually via {@link #setGLReadDrawable(GLDrawable)},
   * it equals to the write-drawable (default).
   * </p> 
   * @see #isGLReadDrawableAvailable()
   * @see #setGLReadDrawable(javax.media.opengl.GLDrawable)
   * @see #getGLDrawable()
   */
  public abstract GLDrawable getGLReadDrawable();

  /**
   * Makes this GLContext current on the calling thread.
   * <p>
   * Recursive call to {@link #makeCurrent()} and hence {@link #release()} are supported.
   * </p>
   * <p>
   * There are two return values that indicate success and one that
   * indicates failure.
   * </p>
   * <p>
   * A return value of {@link #CONTEXT_CURRENT_NEW}
   * indicates that that context has been made current for the 1st time, 
   * or that the state of the underlying context or drawable has
   * changed since the last time this context was current. 
   * In this case, the application may wish to initialize the render state.
   * </p>
   * <p>
   * A return value of {@link #CONTEXT_CURRENT} indicates that the context has
   * been made current, with its previous state restored.
   * </p>
   * <p>
   * If the context could not be made current (for example, because
   * the underlying drawable has not ben realized on the display) ,
   * a value of {@link #CONTEXT_NOT_CURRENT} is returned.
   * </p>
   * <p>
   * This method is blocking, i.e. waits until another thread has
   * released the context.
   * </p>
   * <p>
   * The drawable's surface is being locked at entry
   * and unlocked at {@link #release()}
   * </p>
   *
   * @return <ul>
   *           <li>{@link #CONTEXT_CURRENT_NEW} if the context was successfully made current the 1st time,</li> 
   *           <li>{@link #CONTEXT_CURRENT} if the context was successfully made current,</li>
   *           <li>{@link #CONTEXT_NOT_CURRENT} if the context could not be made current.</li>
   *         </ul>
   *
   * @throws GLException if synchronization is disabled and the
   * context is current on another thread, or because the context
   * could not be created or made current due to non-recoverable,
   * window system-specific errors.
   */
  public abstract int makeCurrent() throws GLException;

  /**
   * Releases control of this GLContext from the current thread.
   * <p>
   * Recursive call to {@link #release()} and hence {@link #makeCurrent()} are supported.
   * </p>
   * <p>
   * The drawable's surface is being unlocked at exit,
   * assumed to be locked by {@link #makeCurrent()}.
   * </p>
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
        throw new GLException(getThreadName()+": No OpenGL context current on this thread");
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
        throw new GLException(getThreadName()+": This context is not current. Current context: "+getCurrent()+", this context "+this);
    }
  }

  /** Returns a String representation of the {@link #makeCurrent()} result. */
  public static final String makeCurrentResultToString(int res) {
      switch(res) {
          case CONTEXT_NOT_CURRENT:   return "CONTEXT_NOT_CURRENT";
          case CONTEXT_CURRENT:       return "CONTEXT_CURRENT";
          case CONTEXT_CURRENT_NEW:   return "CONTEXT_NOT_CURRENT";
          default: return "INVALID_VALUE";
      }
  }

  /**
   * Sets the thread-local variable returned by {@link #getCurrent}
   * and has no other side-effects. For use by third parties adding
   * new GLContext implementations; not for use by end users.
   */
  protected static void setCurrent(GLContext cur) {
    if( TRACE_SWITCH ) {
       if(null == cur) {
           System.err.println(getThreadName()+": GLContext.ContextSwitch: - setCurrent() - NULL");
       } else {
           System.err.println(getThreadName()+": GLContext.ContextSwitch: - setCurrent() - obj " + toHexString(cur.hashCode()) + ", ctx " + toHexString(cur.getHandle()));
       }
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
   * Returns the implementing root GL instance of this GLContext's GL object, 
   * considering a wrapped pipelined hierarchy, see {@link GLBase#getDownstreamGL()}.
   * @throws GLException if the root instance is not a GL implementation
   * @see GLBase#getRootGL()
   * @see GLBase#getDownstreamGL()
   * @see #getGL()
   * @see #setGL(GL)
   */
  public abstract GL getRootGL();
  
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
   * Returns the underlying native OpenGL context handle
   */
  public final long getHandle() { return contextHandle; }

  /**
   * Indicates whether the underlying native OpenGL context has been created.
   */
  public final boolean isCreated() {
    return 0 != contextHandle;
  }

  /**
   * Returns the attached user object for the given name to this GLContext.
   */
  public final Object getAttachedObject(String name) {
    return attachedObjects.get(name);
  }

  /**
   * Sets the attached user object for the given name to this GLContext.
   * Returns the previously set object or null.
   */
  public final Object attachObject(String name, Object obj) {
    return attachedObjects.put(name, obj);
  }

  public final Object detachObject(String name) {
      return attachedObjects.remove(name);
  }

  /**
   * Classname, GL, GLDrawable
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append(" [");
    this.append(sb);
    sb.append("] ");
    return sb.toString();
  }

  public final StringBuilder append(StringBuilder sb) {
    sb.append("Version ").append(getGLVersion()).append(" [GL ").append(getGLVersionNumber()).append(", vendor ").append(getGLVendorVersionNumber());
    sb.append("], options 0x");
    sb.append(Integer.toHexString(ctxOptions));
    sb.append(", this ");
    sb.append(toHexString(hashCode()));
    sb.append(", handle ");
    sb.append(toHexString(contextHandle));
    sb.append(", ");
    sb.append(getGL());
    sb.append(",\n\t quirks: ");
    if(null != glRendererQuirks) {
        glRendererQuirks.toString(sb);
    } else {
        sb.append("n/a");
    }
    if(getGLDrawable()!=getGLReadDrawable()) {
        sb.append(",\n\tRead Drawable : ");
        sb.append(getGLReadDrawable());
        sb.append(",\n\tWrite Drawable: ");
        sb.append(getGLDrawable());
    } else {
        sb.append(",\n\tDrawable: ");
        sb.append(getGLDrawable());
    }
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

  /**
   * Returns this context OpenGL version. 
   * @see #getGLSLVersionNumber() 
   **/
  public final VersionNumber getGLVersionNumber() { return ctxVersion; }
  /** 
   * Returns the vendor's version, i.e. version number at the end of <code>GL_VERSION</code> not being the GL version.
   * <p>
   * In case no such version exists within <code>GL_VERSION</code>, 
   * the {@link VersionNumberString#zeroVersion zero version} instance is returned.
   * </p> 
   * <p>
   * The vendor's version is usually the vendor's OpenGL driver version.
   * </p>
   */
  public final VersionNumberString getGLVendorVersionNumber() { return ctxVendorVersion; }
  public final boolean isGLCompatibilityProfile() { return ( 0 != ( CTX_PROFILE_COMPAT & ctxOptions ) ); }
  public final boolean isGLCoreProfile()          { return ( 0 != ( CTX_PROFILE_CORE   & ctxOptions ) ); }
  public final boolean isGLForwardCompatible()    { return ( 0 != ( CTX_OPTION_FORWARD & ctxOptions ) ); }
  public final boolean isGLDebugEnabled()         { return ( 0 != ( CTX_OPTION_DEBUG   & ctxOptions ) ); }
  public final boolean isCreatedWithARBMethod()   { return ( 0 != ( CTX_IS_ARB_CREATED & ctxOptions ) ); }

  /**
   * Returns the matching GLSL version number, queried by this context GL
   * via {@link GL2ES2#GL_SHADING_LANGUAGE_VERSION} if &ge; ES2.0 or GL2.0,
   * otherwise a static match is being utilized.
   * <p>
   * The context must have been current once, 
   * otherwise the {@link VersionNumberString#zeroVersion zero version} instance is returned. 
   * </p>
   * <p>
   * Examples w/ <code>major.minor</code>: 
   * <pre>
   *    1.00 (ES 2.0), 1.10 (GL 2.0), 1.20 (GL 2.1), 1.50 (GL 3.2), 
   *    3.30 (GL 3.3), 4.00 (GL 4.0), 4.10 (GL 4.1), 4.20 (GL 4.2)
   * </pre >
   * </p>
   * <p>
   * <i>Matching</i> could also refer to the maximum GLSL version usable by this context
   * since <i>normal</i> GL implementations are capable of using a lower GLSL version as well.
   * The latter is not true on OSX w/ a GL3 context. 
   * </p>
   * 
   * @return GLSL version number if context has been made current at least once, 
   *         otherwise the {@link VersionNumberString#zeroVersion zero version} instance is returned.
   *            
   * @see #getGLVersionNumber()
   */
  public final VersionNumber getGLSLVersionNumber() {
      return ctxGLSLVersion;
  }
  
  /**
   * Returns the GLSL version string as to be used in a shader program, including a terminating newline '\n',
   * i.e. for desktop
   * <pre>
   *    #version 110
   *    ..
   *    #version 150
   *    #version 330
   *    ...
   * </pre>
   * And for ES:
   * <pre>
   *    #version 100
   *    #version 300 es
   *    ..
   * </pre>
   * <p>
   * If context has not been made current yet, a string of zero length is returned.
   * </p>
   * @see #getGLSLVersionNumber()
   */
  public final String getGLSLVersionString() {
      if( ctxGLSLVersion.isZero() ) {
          return "";
      }
      final int minor = ctxGLSLVersion.getMinor();
      final String esSuffix = isGLES() && ctxGLSLVersion.compareTo(Version300) >= 0 ? " es" : ""; 
      return "#version " + ctxGLSLVersion.getMajor() + ( minor < 10 ? "0"+minor : minor ) + esSuffix + "\n" ;
  }
  
  protected static final VersionNumber getStaticGLSLVersionNumber(int glMajorVersion, int glMinorVersion, int ctxOptions) {
      if( 0 != ( CTX_PROFILE_ES & ctxOptions ) ) {
          if( 3 > glMajorVersion ) {
              return Version100;           // ES 2.0  ->  GLSL 1.00
          }
      } else if( 1 == glMajorVersion ) {
          return Version110;               // GL 1.x  ->  GLSL 1.10
      } else if( 2 == glMajorVersion ) {
          switch ( glMinorVersion ) {
              case 0:  return Version110;  // GL 2.0  ->  GLSL 1.10
              default: return Version120;  // GL 2.1  ->  GLSL 1.20
          }
      } else if( 3 == glMajorVersion && 2 >= glMinorVersion ) {
          switch ( glMinorVersion ) {
              case 0:  return Version130;  // GL 3.0  ->  GLSL 1.30
              case 1:  return Version140;  // GL 3.1  ->  GLSL 1.40
              default: return Version150;  // GL 3.2  ->  GLSL 1.50 
          }
      }
      // The new default: GL >= 3.3, ES >= 3.0
      return new VersionNumber(glMajorVersion, glMinorVersion * 10, 0); // GL M.N  ->  GLSL M.N
  }
  
  /**
   * @return true if this context is an ES2 context or implements
   *         the extension <code>GL_ARB_ES3_compatibility</code> or <code>GL_ARB_ES2_compatibility</code>, otherwise false
   */
  public final boolean isGLES2Compatible() {
      return 0 != ( ctxOptions & ( CTX_IMPL_ES3_COMPAT | CTX_IMPL_ES2_COMPAT ) ) ;
  }

  /**
   * Return true if this context is an ES3 context or implements
   * the extension <code>GL_ARB_ES3_compatibility</code>, otherwise false.
   * <p>
   * Includes [ GL &ge; 4.3, GL &ge; 3.1 w/ GL_ARB_ES3_compatibility and GLES3 ]
   * </p>
   */
  public final boolean isGLES3Compatible() {
      return 0 != ( ctxOptions & CTX_IMPL_ES3_COMPAT ) ;
  }

  /** 
   * @return true if impl. is a hardware rasterizer, otherwise false.
   * @see #isHardwareRasterizer(AbstractGraphicsDevice, GLProfile)
   * @see GLProfile#isHardwareRasterizer() 
   */
  public final boolean isHardwareRasterizer() {
      return 0 == ( ctxOptions & CTX_IMPL_ACCEL_SOFT ) ;
  }
  
  /**
   * @return true if context supports GLSL, i.e. is either {@link #isGLES2()}, {@link #isGL3()} or {@link #isGL2()} <i>and</i> major-version > 1.
   * @see GLProfile#hasGLSL() 
   */
  public final boolean hasGLSL() {
      return isGLES2() ||
             isGL3() ||
             isGL2() && ctxVersion.getMajor()>1 ;
  }

  /** 
   * Returns <code>true</code> if basic FBO support is available, otherwise <code>false</code>.
   * <p>
   * Basic FBO is supported if the context is either GL-ES >= 2.0, GL >= core 3.0 or implements the extensions
   * <code>GL_ARB_ES2_compatibility</code>, <code>GL_ARB_framebuffer_object</code>, <code>GL_EXT_framebuffer_object</code> or <code>GL_OES_framebuffer_object</code>.
   * </p>
   * <p>
   * Basic FBO support may only include one color attachment and no multisampling,
   * as well as limited internal formats for renderbuffer.
   * </p>
   * @see #CTX_IMPL_FBO
   */
  public final boolean hasBasicFBOSupport() {
      return 0 != ( ctxOptions & CTX_IMPL_FBO ) ;
  }

  /** 
   * Returns <code>true</code> if <code>OES_single_precision</code>, fp32, fixed function point (FFP) compatibility entry points available, 
   * otherwise <code>false</code>.
   * @see #CTX_IMPL_FP32_COMPAT_API
   */
  public final boolean hasFP32CompatAPI() {
      return 0 != ( ctxOptions & CTX_IMPL_FP32_COMPAT_API ) ;
  }
  
  /** 
   * Returns <code>true</code> if full FBO support is available, otherwise <code>false</code>.
   * <p>
   * Full FBO is supported if the context is either GL >= core 3.0 or implements the extensions
   * <code>ARB_framebuffer_object</code>, or all of
   * <code>EXT_framebuffer_object</code>, <code>EXT_framebuffer_multisample</code>, 
   * <code>EXT_framebuffer_blit</code>, <code>GL_EXT_packed_depth_stencil</code>.
   * </p>
   * <p>
   * Full FBO support includes multiple color attachments and multisampling.
   * </p>
   */
  public final boolean hasFullFBOSupport() {        
      return hasBasicFBOSupport() && !hasRendererQuirk(GLRendererQuirks.NoFullFBOSupport) &&
             ( isGL3() ||                                                         // GL >= 3.0                
               isExtensionAvailable(GLExtensions.ARB_framebuffer_object) ||       // ARB_framebuffer_object
               ( isExtensionAvailable(GLExtensions.EXT_framebuffer_object) &&     // All EXT_framebuffer_object*
                 isExtensionAvailable(GLExtensions.EXT_framebuffer_multisample) &&
                 isExtensionAvailable(GLExtensions.EXT_framebuffer_blit) &&
                 isExtensionAvailable(GLExtensions.EXT_packed_depth_stencil)
               )
             ) ;
  }
  
  /**
   * Returns the maximum number of FBO RENDERBUFFER samples
   * if {@link #hasFullFBOSupport() full FBO is supported}, otherwise false. 
   */
  public final int getMaxRenderbufferSamples() {
      if( hasFullFBOSupport() ) {
          final GL gl = getGL();
          final int[] val = new int[] { 0 } ;
          try {
              gl.glGetIntegerv(GL2GL3.GL_MAX_SAMPLES, val, 0);
              final int glerr = gl.glGetError();
              if(GL.GL_NO_ERROR == glerr) {
                  return val[0];
              } else if(DEBUG) {
                  System.err.println("GLContext.getMaxRenderbufferSamples: GL_MAX_SAMPLES query GL Error 0x"+Integer.toHexString(glerr));
              }
          } catch (GLException gle) { gle.printStackTrace(); }
      }
      return 0;
  }
  
  /** Note: The GL impl. may return a const value, ie {@link GLES2#isNPOTTextureAvailable()} always returns <code>true</code>. */
  public boolean isNPOTTextureAvailable() {
      return isGL3() || isGLES2Compatible() || isExtensionAvailable(GLExtensions.ARB_texture_non_power_of_two);
  }

  public boolean isTextureFormatBGRA8888Available() {
      return isGL2GL3() ||
             isExtensionAvailable(GLExtensions.EXT_texture_format_BGRA8888) ||
             isExtensionAvailable(GLExtensions.IMG_texture_format_BGRA8888) ;
  }

  /** 
   * Indicates whether this GLContext is capable of GL4bc.  <p>Includes [ GL4bc ].</p>
   * @see GLProfile#isGL4bc() 
   */
  public final boolean isGL4bc() {
      return 0 != (ctxOptions & CTX_IS_ARB_CREATED) &&
             0 != (ctxOptions & CTX_PROFILE_COMPAT) &&
             ctxVersion.getMajor() >= 4;
  }

  /** 
   * Indicates whether this GLContext is capable of GL4.    <p>Includes [ GL4bc, GL4 ].</p>
   * @see GLProfile#isGL4() 
   */
  public final boolean isGL4() {
      return 0 != (ctxOptions & CTX_IS_ARB_CREATED) &&
             0 != (ctxOptions & (CTX_PROFILE_COMPAT|CTX_PROFILE_CORE)) &&
             ctxVersion.getMajor() >= 4;
  }

  /** 
   * Indicates whether this GLContext uses a GL4 core profile. <p>Includes [ GL4 ].</p>
   */
  public final boolean isGL4core() {
      return 0 != ( ctxOptions & CTX_IS_ARB_CREATED ) &&
             0 != ( ctxOptions & CTX_PROFILE_CORE ) &&
             ctxVersion.getMajor() >= 4;
  }
  
  /** 
   * Indicates whether this GLContext is capable of GL3bc.  <p>Includes [ GL4bc, GL3bc ].</p>
   * @see GLProfile#isGL3bc() 
   */
  public final boolean isGL3bc() {
      return 0 != (ctxOptions & CTX_IS_ARB_CREATED) &&
             0 != (ctxOptions & CTX_PROFILE_COMPAT) &&
             ctxVersion.compareTo(Version310) >= 0 ;
  }

  /** 
   * Indicates whether this GLContext is capable of GL3.    <p>Includes [ GL4bc, GL4, GL3bc, GL3 ].</p>
   * @see GLProfile#isGL3() 
   */
  public final boolean isGL3() {
      return 0 != (ctxOptions & CTX_IS_ARB_CREATED) &&
             0 != (ctxOptions & (CTX_PROFILE_COMPAT|CTX_PROFILE_CORE)) &&
             ctxVersion.compareTo(Version310) >= 0 ;
  }  
  
  /** 
   * Indicates whether this GLContext uses a GL3 core profile. <p>Includes [ GL4, GL3 ].</p>
   */
  public final boolean isGL3core() {
      return 0 != ( ctxOptions & CTX_IS_ARB_CREATED ) &&
             0 != ( ctxOptions & CTX_PROFILE_CORE ) &&
             ctxVersion.compareTo(Version310) >= 0;
  }
  
  /** 
   * Indicates whether this GLContext uses a GL core profile. <p>Includes [ GL4, GL3, GLES3, GL2ES2 ].</p>
   */
  public final boolean isGLcore() {
      return ( 0 != ( ctxOptions & CTX_PROFILE_ES ) && ctxVersion.getMajor() >= 2 ) ||
             ( 0 != ( ctxOptions & CTX_IS_ARB_CREATED ) &&
               0 != ( ctxOptions & CTX_PROFILE_CORE ) &&
               ctxVersion.compareTo(Version310) >= 0
             ) ;
  }
  
  /** 
   * Indicates whether this GLContext's native profile does not implement a default <i>vertex array object</i> (VAO), 
   * starting w/ OpenGL 3.1 core and GLES3.
   * <p>Includes [ GL4, GL3, GLES3 ].</p>
   * <pre>
     Due to GL 3.1 core spec: E.1. DEPRECATED AND REMOVED FEATURES (p 296),
            GL 3.2 core spec: E.2. DEPRECATED AND REMOVED FEATURES (p 331)
     there is no more default VAO buffer 0 bound, hence generating and binding one
     to avoid INVALID_OPERATION at VertexAttribPointer. 
     More clear is GL 4.3 core spec: 10.4 (p 307).
   * </pre>
   * <pre>
     GLES3 is included, since upcoming ES releases &gt; 3.0 may behave the same:
            GL ES 3.0 spec F.1. Legacy Features (p 322).
   * </pre>
   * <p>
   * If no default VAO is implemented in the native OpenGL profile,
   * an own default VAO is being used, see {@link #getDefaultVAO()}.
   * </p>
   * @see #getDefaultVAO()
   */
  public final boolean hasNoDefaultVAO() {
      return ( 0 != ( ctxOptions & CTX_PROFILE_ES ) && ctxVersion.getMajor() >= 3 ) ||
             ( 0 != ( ctxOptions & CTX_IS_ARB_CREATED ) &&
               0 != ( ctxOptions & CTX_PROFILE_CORE ) &&
               ctxVersion.compareTo(Version310) >= 0
             ) ;
  }
  
  /**
   * If this GLContext does not implement a default VAO, see {@link #hasNoDefaultVAO()},
   * an <i>own default VAO</i> will be created and bound at context creation.
   * <p>
   * If this GLContext does implement a default VAO, i.e. {@link #hasNoDefaultVAO()}
   * returns <code>false</code>, this method returns <code>0</code>.
   * </p> 
   * <p>
   * Otherwise this method returns the VAO object name
   * representing this GLContext's <i>own default VAO</i>.  
   * </p> 
   * @see #hasNoDefaultVAO()
   */
  public abstract int getDefaultVAO();

  /** 
   * Indicates whether this GLContext is capable of GL2.    <p>Includes [ GL4bc, GL3bc, GL2  ].</p>
   * @see GLProfile#isGL2() 
   */
  public final boolean isGL2() {
      return 0 != ( ctxOptions & CTX_PROFILE_COMPAT ) && ctxVersion.getMajor()>=1 ;
  }

  /** 
   * Indicates whether this GLContext is capable of GL2GL3. <p>Includes [ GL4bc, GL4, GL3bc, GL3, GL2, GL2GL3 ].</p>
   * @see GLProfile#isGL2GL3() 
   */  
  public final boolean isGL2GL3() {
      return isGL2() || isGL3();
  }

  /** 
   * Indicates whether this GLContext is capable of GLES1.  <p>Includes [ GLES1 ].</p>
   * @see GLProfile#isGLES1() 
   */
  public final boolean isGLES1() {
      return 0 != ( ctxOptions & CTX_PROFILE_ES ) && ctxVersion.getMajor() == 1 ;
  }

  /**
   * Indicates whether this GLContext is capable of GLES2.  <p>Includes [ GLES3, GLES2 ].</p> 
   * @see GLProfile#isGLES2() 
   */
  public final boolean isGLES2() {
      return 0 != ( ctxOptions & CTX_PROFILE_ES ) && ctxVersion.getMajor() >= 2 ;
  }

  /** 
   * Indicates whether this GLContext is capable of GLES3.  <p>Includes [ GLES3 ].</p>
   * @see GLProfile#isGLES3() 
   */
  public final boolean isGLES3() {
      return 0 != ( ctxOptions & CTX_PROFILE_ES ) && ctxVersion.getMajor() >= 3 ;
  }

  /** 
   * Indicates whether this GLContext is capable of GLES.  <p>Includes [ GLES3, GLES1, GLES2 ].</p>
   * @see GLProfile#isGLES() 
   */
  public final boolean isGLES() {
      return 0 != ( CTX_PROFILE_ES & ctxOptions ) ;
  }

  /** 
   * Indicates whether this GLContext is capable of GL2ES1. <p>Includes [ GL4bc, GL3bc, GL2, GLES1, GL2ES1 ].</p>
   * @see GLProfile#isGL2ES1() 
   */
  public final boolean isGL2ES1() {
      return isGLES1() || isGL2();
  }

  /** 
   * Indicates whether this GLContext is capable of GL2ES2. <p>Includes [ GL4bc, GL4, GL3bc, GL3, GLES3, GL2, GL2GL3, GL2ES2, GLES2 ].</p>
   * @see GLProfile#isGL2ES2() 
   */
  public final boolean isGL2ES2() {
      return isGLES2() || isGL2GL3();
  }

  /** 
   * Indicates whether this GLContext is capable of GL3ES3. <p>Includes [ GL4bc, GL4, GL3bc, GL3, GLES3 ].</p>
   * @see GLProfile#isGL3ES3() 
   */
  public final boolean isGL3ES3() {
      return isGL4ES3() || isGL3();
  }

  /** 
   * Returns true if this profile is capable of GL4ES3, i.e. if {@link #isGLES3Compatible()} returns true. 
   * <p>Includes [ GL &ge; 4.3, GL &ge; 3.1 w/ GL_ARB_ES3_compatibility and GLES3 ]</p>
   * @see GLProfile#isGL4ES3()
   */
  public final boolean isGL4ES3() {
      return isGLES3Compatible() ;
  }

  /**
   * Set the swap interval of the current context and attached drawable.
   * @param interval Should be &ge; 0. 0 disables the vertical synchronization,
   *                 where &ge; 1 is the number of vertical refreshes before a swap buffer occurs.
   *                 A value &lt; 0 is ignored.
   * @return true if the operation was successful, otherwise false
   *
   * @throws GLException if the context is not current.
   */
  public final boolean setSwapInterval(int interval) throws GLException {
    validateCurrent();
    if(0<=interval) {
        if( !drawableRetargeted || !hasRendererQuirk(GLRendererQuirks.NoSetSwapIntervalPostRetarget) ) {
            if( setSwapIntervalImpl(interval) ) {
                currentSwapInterval = interval;
                return true;
            }
        }
    }
    return false;
  }
  protected boolean setSwapIntervalImpl(int interval) {
      return false;
  }
  /** Return the current swap interval.
   * <p>
   * If the context has not been made current at all,
   * the default value <code>-1</code> is returned.
   * </p>
   * <p>
   * For a valid context the default value is <code>1</code>
   * in case of an EGL based profile (ES1 or ES2) and <code>-1</code> 
   * (undefined) for desktop.
   * </p>
   */
  public final int getSwapInterval() {
    return currentSwapInterval;
  }
  protected final void setDefaultSwapInterval() {
    if(this.isGLES()) {
        currentSwapInterval = 1;
    } else {
        currentSwapInterval = -1;
    }
  }

  public final boolean queryMaxSwapGroups(int[] maxGroups, int maxGroups_offset,
                                          int[] maxBarriers, int maxBarriers_offset) {
    validateCurrent();
    return queryMaxSwapGroupsImpl(maxGroups, maxGroups_offset, maxBarriers, maxBarriers_offset);
  }
  protected boolean queryMaxSwapGroupsImpl(int[] maxGroups, int maxGroups_offset,
                                          int[] maxBarriers, int maxBarriers_offset) { return false; }
  public final boolean joinSwapGroup(int group) {
    validateCurrent();
    return joinSwapGroupImpl(group);
  }
  protected boolean joinSwapGroupImpl(int group) { /** nop per default .. **/  return false; }
  protected int currentSwapGroup = -1; // default: not set yet ..
  public int getSwapGroup() {
      return currentSwapGroup;
  }
  public final boolean bindSwapBarrier(int group, int barrier) {
    validateCurrent();
    return bindSwapBarrierImpl(group, barrier);
  }
  protected boolean bindSwapBarrierImpl(int group, int barrier) { /** nop per default .. **/  return false; }

  /** 
   * Return the framebuffer name bound to this context, 
   * see {@link GL#glBindFramebuffer(int, int)}.
   */
  public abstract int getBoundFramebuffer(int target);
  
  /** 
   * Return the default draw framebuffer name.
   * <p> 
   * May differ from it's default <code>zero</code>
   * in case an framebuffer object ({@link FBObject}) based drawable
   * is being used.
   * </p> 
   */
  public abstract int getDefaultDrawFramebuffer();
  
  /** 
   * Return the default read framebuffer name.
   * <p> 
   * May differ from it's default <code>zero</code>
   * in case an framebuffer object ({@link FBObject}) based drawable
   * is being used.
   * </p> 
   */
  public abstract int getDefaultReadFramebuffer();
  
  /** 
   * Returns the default color buffer within the current bound 
   * {@link #getDefaultReadFramebuffer()}, i.e. GL_READ_FRAMEBUFFER​,  
   * which will be used as the source for pixel reading commands, 
   * like {@link GL#glReadPixels(int, int, int, int, int, int, java.nio.Buffer)} etc.
   * <p>
   * For offscreen framebuffer objects this is {@link GL#GL_COLOR_ATTACHMENT0},
   * otherwise this is {@link GL#GL_FRONT} for single buffer configurations 
   * and {@link GL#GL_BACK} for double buffer configurations.
   * </p> 
   */
  public abstract int getDefaultReadBuffer();
  
  /** Get the default pixel data type, as required by e.g. {@link GL#glReadPixels(int, int, int, int, int, int, java.nio.Buffer)}. */
  public abstract int getDefaultPixelDataType();
  
  /** Get the default pixel data format, as required by e.g. {@link GL#glReadPixels(int, int, int, int, int, int, java.nio.Buffer)}. */
  public abstract int getDefaultPixelDataFormat();
  
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
      /* 4.*/ { 0, 1, 2, 3 } };

  public static final int ES_VERSIONS[][] = {
      /* 0.*/ { -1 },
      /* 1.*/ { 0, 1 },
      /* 2.*/ { 0 },
      /* 3.*/ { 0 } };

  public static final int getMaxMajor(int ctxProfile) {
      return ( 0 != ( CTX_PROFILE_ES & ctxProfile ) ) ? ES_VERSIONS.length-1 : GL_VERSIONS.length-1;
  }

  public static final int getMaxMinor(int ctxProfile, int major) {
      if( 1>major ) {
          return -1;
      }
      if( ( 0 != ( CTX_PROFILE_ES & ctxProfile ) ) ) {
          if( major>=ES_VERSIONS.length ) return -1;
          return ES_VERSIONS[major].length-1;
      } else {
          if( major>=GL_VERSIONS.length ) return -1;
          return GL_VERSIONS[major].length-1;
      }
  }

  public static final boolean isValidGLVersion(int ctxProfile, int major, int minor) {
      if( 1>major || 0>minor ) {
          return false;
      }
      if( ( 0 != ( CTX_PROFILE_ES & ctxProfile ) ) ) {
          if( major>=ES_VERSIONS.length) return false;
          if( minor>=ES_VERSIONS[major].length) return false;
      } else {
          if( major>=GL_VERSIONS.length) return false;
          if( minor>=GL_VERSIONS[major].length) return false;
      }
      return true;
  }

  public static final boolean decrementGLVersion(int ctxProfile, int major[], int minor[]) {
      if(null==major || major.length<1 ||null==minor || minor.length<1) {
          throw new GLException("invalid array arguments");
      }
      int m = major[0];
      int n = minor[0];
      if( !isValidGLVersion(ctxProfile, m, n) ) {
          return false;
      }

      // decrement ..
      n -= 1;
      if(n < 0) {
          if( ( 0 != ( CTX_PROFILE_ES & ctxProfile ) ) ) {
              if( m >= 3) {
                  m -= 1;
              } else {
                  m = 0; // major decr [1,2] -> 0
              }
              n = ES_VERSIONS[m].length-1;
          } else {
              m -= 1;
              n = GL_VERSIONS[m].length-1;
          }
      }
      if( !isValidGLVersion(ctxProfile, m, n) ) {
          return false;
      }
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
  protected static final IdentityHashMap<String, Integer> deviceVersionAvailable = new IdentityHashMap<String, Integer>();

  /**
   * @see #getUniqueDeviceString(javax.media.nativewindow.AbstractGraphicsDevice)
   */
  private static final IdentityHashMap<String, String> deviceVersionsAvailableSet = new IdentityHashMap<String, String>();

  /** clears the device/context mappings as well as the GL/GLX proc address tables. */
  protected static void shutdown() {
      deviceVersionAvailable.clear();
      deviceVersionsAvailableSet.clear();
      GLContextImpl.shutdownImpl(); // well ..
  }

  protected static boolean getAvailableGLVersionsSet(AbstractGraphicsDevice device) {
      synchronized ( deviceVersionsAvailableSet ) {
        return deviceVersionsAvailableSet.containsKey(device.getUniqueID());
      }
  }

  protected static void setAvailableGLVersionsSet(AbstractGraphicsDevice device) {
      synchronized ( deviceVersionsAvailableSet ) {
          final String devKey = device.getUniqueID();
          if( null != deviceVersionsAvailableSet.put(devKey, devKey) ) {
              throw new InternalError("Already set: "+devKey);
          }
          if (DEBUG) {
            System.err.println(getThreadName() + ": createContextARB: SET mappedVersionsAvailableSet "+devKey);
            System.err.println(GLContext.dumpAvailableGLVersions(null).toString());            
          }
      }
  }

  /** 
   * Returns a unique String object using {@link String#intern()} for the given arguments, 
   * which object reference itself can be used as a key.
   */
  protected static String getDeviceVersionAvailableKey(AbstractGraphicsDevice device, int major, int profile) {
      final String r = device.getUniqueID() + "-" + toHexString(composeBits(major, profile, 0));
      return r.intern();
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

    if(FORCE_NO_FBO_SUPPORT) {
        resCtp &= ~CTX_IMPL_FBO ;
    }
    if(DEBUG) {
        System.err.println("GLContext.mapAvailableGLVersion: "+device+": "+getGLVersion(reqMajor, 0, profile, null)+" -> "+getGLVersion(resMajor, resMinor, resCtp, null));
        // Thread.dumpStack();
    }
    final String objectKey = getDeviceVersionAvailableKey(device, reqMajor, profile);
    final Integer val = new Integer(composeBits(resMajor, resMinor, resCtp));
    synchronized(deviceVersionAvailable) {
        return deviceVersionAvailable.put( objectKey, val );
    }
  }

  protected static StringBuilder dumpAvailableGLVersions(StringBuilder sb) {
    if(null == sb) {
        sb = new StringBuilder();
    }
    synchronized(deviceVersionAvailable) {
        final Set<String> keys = deviceVersionAvailable.keySet();
        boolean needsSeparator = false;
        for(Iterator<String> i = keys.iterator(); i.hasNext(); ) {
            if(needsSeparator) {
                sb.append(Platform.getNewline());
            }
            final String key = i.next();
            sb.append(key).append(": ");
            final Integer valI = deviceVersionAvailable.get(key);
            if(null != valI) {
                final int bits32 = valI.intValue();
                final int major = ( bits32 & 0xFF000000 ) >> 24 ;
                final int minor = ( bits32 & 0x00FF0000 ) >> 16 ;
                final int ctp   = ( bits32 & 0x0000FFFF )       ;
                sb.append(GLContext.getGLVersion(major, minor, ctp, null));
            } else {
                sb.append("n/a");
            }
            needsSeparator = true;
        }
    }
    return sb;
  }

  /**
   * @param device the device to request whether the profile is available for
   * @param reqMajor Key Value either 1, 2, 3 or 4
   * @param reqProfile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   * @return the available GL version as encoded with {@link #composeBits(int, int, int), otherwise <code>null</code>
   */
  protected static Integer getAvailableGLVersion(AbstractGraphicsDevice device, int reqMajor, int reqProfile)  {
    final String objectKey = getDeviceVersionAvailableKey(device, reqMajor, reqProfile);
    Integer val;
    synchronized(deviceVersionAvailable) {
        val = deviceVersionAvailable.get( objectKey );
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

    final Integer valI = getAvailableGLVersion(device, reqMajor, reqProfile);
    if(null==valI) {
        return false;
    }

    final int bits32 = valI.intValue();

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
   * Returns the GLProfile's major version number at reqMajorCTP[0] and it's context property (CTP) at reqMajorCTP[1] for availability mapping request.
   */
  protected static final void getRequestMajorAndCompat(final GLProfile glp, int[/*2*/] reqMajorCTP) {
    final GLProfile glpImpl = glp.getImpl();
    if( glpImpl.isGL4() ) {
        reqMajorCTP[0]=4;
    } else if ( glpImpl.isGL3() || glpImpl.isGLES3() ) {
        reqMajorCTP[0]=3;
    } else if (glpImpl.isGLES1()) {
        reqMajorCTP[0]=1;
    } else /* if (glpImpl.isGL2() || glpImpl.isGLES2()) */ {
        reqMajorCTP[0]=2;
    }
    if( glpImpl.isGLES() ) {
        reqMajorCTP[1]=CTX_PROFILE_ES;
    } else if( glpImpl.isGL2() ) { // incl GL3bc and GL4bc
        reqMajorCTP[1]=CTX_PROFILE_COMPAT;
    } else {
        reqMajorCTP[1]=CTX_PROFILE_CORE;
    }
  }
  
  /**
   * @param device the device the context profile is being requested for
   * @param GLProfile the GLProfile the context profile is being requested for
   * @return the GLProfile's context property (CTP) if available, otherwise <code>0</code>
   */
  protected static final int getAvailableContextProperties(final AbstractGraphicsDevice device, final GLProfile glp) {
    final int[] reqMajorCTP = new int[] { 0, 0 };
    getRequestMajorAndCompat(glp, reqMajorCTP);
    
    int _major[] = { 0 };
    int _minor[] = { 0 };
    int _ctp[] = { 0 };
    if( GLContext.getAvailableGLVersion(device, reqMajorCTP[0], reqMajorCTP[1], _major, _minor, _ctp)) {
      return _ctp[0];
    }
    return 0; // n/a
  }

  /**
   * @param device the device the profile is being requested
   * @param major Key Value either 1, 2, 3 or 4
   * @param profile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   * @return the highest GLProfile regarding availability, version and profile bits.
   */
  protected static GLProfile getAvailableGLProfile(AbstractGraphicsDevice device, int reqMajor, int reqProfile)
          throws GLException {
    int major[] = { 0 };
    int minor[] = { 0 };
    int ctp[] = { 0 };
    if(GLContext.getAvailableGLVersion(device, reqMajor, reqProfile, major, minor, ctp)) {
        return GLProfile.get(GLContext.getGLProfile(major[0], minor[0], ctp[0]));
    }
    return null;
  }

  /**
   * @param device the device the profile is being requested
   * @param major Key Value either 1, 2, 3 or 4
   * @param profile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   */
  protected static String getAvailableGLVersionAsString(AbstractGraphicsDevice device, int major, int profile) {
    int _major[] = { 0 };
    int _minor[] = { 0 };
    int _ctp[] = { 0 };
    if(getAvailableGLVersion(device, major, profile, _major, _minor, _ctp)) {
        return getGLVersion(_major[0], _minor[0], _ctp[0], null);
    }
    return null;
  }

  /**
   * Returns true if it is possible to create an <i>framebuffer object</i> (FBO).
   * <p>
   * FBO feature is implemented in OpenGL, hence it is {@link GLProfile} dependent.
   * </p> 
   * <p>
   * FBO support is queried as described in {@link #hasBasicFBOSupport()}.
   * </p>
   *
   * @param device the device to request whether FBO is available for
   * @param glp {@link GLProfile} to check for FBO capabilities
   * @see GLContext#hasBasicFBOSupport()
   */
  public static final boolean isFBOAvailable(AbstractGraphicsDevice device, GLProfile glp) {
      return 0 != ( CTX_IMPL_FBO & getAvailableContextProperties(device, glp) );
  }
  
  /**
   * @return <code>1</code> if using a hardware rasterizer, <code>0</code> if using a software rasterizer and <code>-1</code> if not determined yet. 
   * @see GLContext#isHardwareRasterizer()
   * @see GLProfile#isHardwareRasterizer() 
   */
  public static final int isHardwareRasterizer(AbstractGraphicsDevice device, GLProfile glp) {
      final int r;
      final int ctp = getAvailableContextProperties(device, glp);
      if(0 == ctp) { 
          r = -1;
      } else if( 0 == ( CTX_IMPL_ACCEL_SOFT & ctp ) ) {
          r = 1;
      } else {
          r = 0;
      }
      return r;
  }
  
  /**
   * @param device the device to request whether the profile is available for
   * @param reqMajor Key Value either 1, 2, 3 or 4
   * @param reqProfile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   * @param isHardware return value of one boolean, whether the profile is a hardware rasterizer or not
   * @return true if the requested GL version is available regardless of a software or hardware rasterizer, otherwise false.
   */
  protected static boolean isGLVersionAvailable(AbstractGraphicsDevice device, int reqMajor, int reqProfile, boolean isHardware[]) {
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

  public static boolean isGLES3Available(AbstractGraphicsDevice device, boolean isHardware[]) {
      return isGLVersionAvailable(device, 3, GLContext.CTX_PROFILE_ES, isHardware);
  }

  /**
   * Returns true if a ES3 compatible profile is available,
   * i.e. either a &ge; 4.3 context or a &ge; 3.1 context supporting <code>GL_ARB_ES3_compatibility</code>,
   * otherwise false.
   * <p>
   * Includes [ GL &ge; 4.3, GL &ge; 3.1 w/ GL_ARB_ES3_compatibility and GLES3 ]
   * </p>
   */
  public static final boolean isGLES3CompatibleAvailable(AbstractGraphicsDevice device) {
      int major[] = { 0 };
      int minor[] = { 0 };
      int ctp[] = { 0 };
      boolean ok;
      
      ok = GLContext.getAvailableGLVersion(device, 3, GLContext.CTX_PROFILE_ES, major, minor, ctp);
      if( !ok ) {
          ok = GLContext.getAvailableGLVersion(device, 3, GLContext.CTX_PROFILE_CORE, major, minor, ctp);
      }
      if( !ok ) {
          ok = GLContext.getAvailableGLVersion(device, 3, GLContext.CTX_PROFILE_COMPAT, major, minor, ctp);
      }
      return 0 != ( ctp[0] & CTX_IMPL_ES3_COMPAT );
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

  protected static String getGLVersion(int major, int minor, int ctp, String gl_version) {
    boolean needColon = false;
    StringBuilder sb = new StringBuilder();
    sb.append(major);
    sb.append(".");
    sb.append(minor);
    sb.append(" (");
    needColon = appendString(sb, "ES profile",            needColon, 0 != ( CTX_PROFILE_ES & ctp ));
    needColon = appendString(sb, "Compat profile",        needColon, 0 != ( CTX_PROFILE_COMPAT & ctp ));
    needColon = appendString(sb, "Core profile",          needColon, 0 != ( CTX_PROFILE_CORE & ctp ));
    needColon = appendString(sb, "forward",               needColon, 0 != ( CTX_OPTION_FORWARD & ctp ));
    needColon = appendString(sb, "arb",                   needColon, 0 != ( CTX_IS_ARB_CREATED & ctp ));
    needColon = appendString(sb, "debug",                 needColon, 0 != ( CTX_OPTION_DEBUG & ctp ));
    needColon = appendString(sb, "ES2 compat",            needColon, 0 != ( CTX_IMPL_ES2_COMPAT & ctp ));
    needColon = appendString(sb, "ES3 compat",            needColon, 0 != ( CTX_IMPL_ES3_COMPAT & ctp ));
    needColon = appendString(sb, "FBO",                   needColon, 0 != ( CTX_IMPL_FBO & ctp ));
    needColon = appendString(sb, "FP32 compat-api",       needColon, 0 != ( CTX_IMPL_FP32_COMPAT_API & ctp ));
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

  protected static String getThreadName() { return Thread.currentThread().getName(); }
  
}

