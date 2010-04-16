/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

import java.util.HashMap;

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
  /** Indicates that the context was not made current during the last call to {@link #makeCurrent makeCurrent}. */
  public static final int CONTEXT_NOT_CURRENT = 0;
  /** Indicates that the context was made current during the last call to {@link #makeCurrent makeCurrent}. */
  public static final int CONTEXT_CURRENT     = 1;
  /** Indicates that a newly-created context was made current during the last call to {@link #makeCurrent makeCurrent}. */
  public static final int CONTEXT_CURRENT_NEW = 2;

  private static ThreadLocal currentContext = new ThreadLocal();

  private HashMap/*<int, Object>*/ attachedObjects = new HashMap();

  protected long context;

  /**
   * Returns the GLDrawable to which this context may be used to
   * draw.
   */
  public abstract GLDrawable getGLDrawable();

  /**
   * Set the GLDrawable from which this context may be used to
   * read.<br>
   * If read is null, the default write drawable will be used.
   */
  public abstract void setGLDrawableRead(GLDrawable read);

  /**
   * Returns the GLDrawable from which this context may be used to
   * read.
   */
  public abstract GLDrawable getGLDrawableRead();

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
    return (GLContext) currentContext.get();
  }

  /**
   * Sets the thread-local variable returned by {@link #getCurrent}
   * and has no other side-effects. For use by third parties adding
   * new GLContext implementations; not for use by end users.
   */
  protected static void setCurrent(GLContext cur) {
    currentContext.set(cur);
  }
  
  /**
   * Destroys this OpenGL context and frees its associated
   * resources. The context should have been released before this
   * method is called.
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
   */
  public abstract GL getGL();

  /**
   * Sets the GL pipeline object for this GLContext.
   *
   * @return the set GL pipeline or null if not successful
   */
  public abstract GL setGL(GL gl);

  /**
   * Returns the attached user object for the given name to this GLContext.
   */
  public Object getAttachedObject(int name) {
    return attachedObjects.get(new Integer(name));
  }

  /**
   * Returns the attached user object for the given name to this GLContext.
   */
  public Object getAttachedObject(String name) {
    return attachedObjects.get(name);
  }

  /**
   * Sets the attached user object for the given name to this GLContext.
   * Returns the previously set object or null.
   */
  public Object putAttachedObject(int name, Object obj) {
    return attachedObjects.put(new Integer(name), obj);
  }

  /**
   * Sets the attached user object for the given name to this GLContext.
   * Returns the previously set object or null.
   */
  public Object putAttachedObject(String name, Object obj) {
    return attachedObjects.put(name, obj);
  }

  /**
   * Classname, GL, GLDrawable
   */
  public final String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(getClass().getName());
    sb.append(" [OpenGL ");
    sb.append(getGLVersion());
    sb.append(", ");
    sb.append(getGL());
    if(getGLDrawable()!=getGLDrawableRead()) {
        sb.append(",\n\tDrawable Read : ");
        sb.append(getGLDrawableRead());
        sb.append(",\n\tDrawable Write: ");
        sb.append(getGLDrawable());
    } else {
        sb.append(",\n\tDrawable Read/Write: ");
        sb.append(getGLDrawable());
    }
    sb.append("] ");
    return sb.toString();
  }

  /** Returns a non-null (but possibly empty) string containing the
      space-separated list of available platform-dependent (e.g., WGL,
      GLX) extensions. Can only be called while this context is
      current. */
  public abstract String getPlatformExtensionsString();

  public final int getGLVersionMajor() { return ctxMajorVersion; }
  public final int getGLVersionMinor() { return ctxMajorVersion; }
  public final boolean isGLCompatibilityProfile() { return ( 0 != ( CTX_PROFILE_COMPAT & ctxOptions ) ); }
  public final boolean isGLForwardCompatible()    { return ( 0 != ( CTX_OPTION_FORWARD & ctxOptions ) ); }

  /** 
   * Returns a valid OpenGL version string, ie 
   *     <code>major.minor ([option]?[options,]*) - gl-version</code>
   *
   * <ul>
   *   <li> options
   *   <ul>
   *     <li> <code>old</code> refers to the non ARB_create_context created context
   *     <li> <code>new</code> refers to the ARB_create_context created context
   *     <li> <code>compatible profile</code>
   *     <li> <code>core profile</code>
   *     <li> <code>forward compatible</code>
   *     <li> <code>any</code> refers to the non forward compatible context
   *     <li> <code>ES</code>  refers to the GLES context variant
   *   </ul>
   *   <li> <i>gl-version</i> the GL_VERSION string
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
   *     <tr><td></td>   <td>ES2</td>  <td><code>2.0 (ES, any, new) - 2.0 ES Profile</code></td></tr>
   *     <tr><td>ATI</td><td>GL2</td>  <td><code>3.0 (compatibility profile, any, new) - 3.2.9704 Compatibility Profile Context</code></td></tr>
   *     <tr><td>ATI</td><td>GL3</td>  <td><code>3.3 (core profile, any, new) - 1.4 (3.2.9704 Compatibility Profile Context)</code></td></tr>
   *     <tr><td>ATI</td><td>GL3bc</td><td><code>3.3 (compatibility profile, any, new) - 1.4 (3.2.9704 Compatibility Profile Context)</code></td></tr>
   *     <tr><td>NV</td><td>GL2</td>   <td><code>3.0 (compatibility profile, any, new) - 3.0.0 NVIDIA 195.36.07.03</code></td></tr>
   *     <tr><td>NV</td><td>GL3</td>   <td><code>3.3 (core profile, any, new) - 3.3.0 NVIDIA 195.36.07.03</code></td></tr>
   *     <tr><td>NV</td><td>GL3bc</td> <td><code>3.3 (compatibility profile, any, new) - 3.3.0 NVIDIA 195.36.07.03</code></td></tr>
   * </table> 
   */
  public final String getGLVersion() {
    return ctxVersionString;
  }

  protected int ctxMajorVersion=-1;
  protected int ctxMinorVersion=-1;
  protected int ctxOptions=0;
  protected String ctxVersionString=null;

  /** <code>ARB_create_context</code> related: created via ARB_create_context */
  protected static final int CTX_IS_ARB_CREATED = 1 << 0;
  /** <code>ARB_create_context</code> related: compatibility profile */
  protected static final int CTX_PROFILE_COMPAT = 1 << 1;
  /** <code>ARB_create_context</code> related: core profile */
  protected static final int CTX_PROFILE_CORE   = 1 << 2;
  /** <code>ARB_create_context</code> related: flag forward compatible */
  protected static final int CTX_OPTION_FORWARD = 1 << 3;
  /** <code>ARB_create_context</code> related: not flag forward compatible */
  protected static final int CTX_OPTION_ANY     = 1 << 4;
  /** <code>ARB_create_context</code> related: flag debug */
  protected static final int CTX_OPTION_DEBUG   = 1 << 5;
}
