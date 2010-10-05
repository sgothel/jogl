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
import com.jogamp.common.util.IntIntHashMap;

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

  /** The underlying native OpenGL context */
  protected long contextHandle;

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
   * @return true if this GLContext is current on this thread
   */
  public final boolean isCurrent() {
    return getCurrent() == this ; 
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
    return attachedObjects.get(new Integer(name));
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
  public final Object putAttachedObject(int name, Object obj) {
    return attachedObjects.put(new Integer(name), obj);
  }

  /**
   * Sets the attached user object for the given name to this GLContext.
   * Returns the previously set object or null.
   */
  public final Object putAttachedObject(String name, Object obj) {
    return attachedObjects.put(name, obj);
  }

  /**
   * Classname, GL, GLDrawable
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(getClass().getName());
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
    if(getGLDrawable()!=getGLDrawableRead()) {
        sb.append(",\n\tDrawable Read : ");
        sb.append(getGLDrawableRead());
        sb.append(",\n\tDrawable Write: ");
        sb.append(getGLDrawable());
    } else {
        sb.append(",\n\tDrawable Read/Write: ");
        sb.append(getGLDrawable());
    }
    return sb;
  }

  /** Returns a non-null (but possibly empty) string containing the
      space-separated list of available platform-dependent (e.g., WGL,
      GLX) extensions. Can only be called while this context is
      current. */
  public abstract String getPlatformExtensionsString();

  public final int getGLVersionMajor() { return ctxMajorVersion; }
  public final int getGLVersionMinor() { return ctxMinorVersion; }
  public final boolean isGLCompatibilityProfile() { return ( 0 != ( CTX_PROFILE_COMPAT & ctxOptions ) ); }
  public final boolean isGLCoreProfile()          { return ( 0 != ( CTX_PROFILE_CORE   & ctxOptions ) ); }
  public final boolean isGLEmbeddedProfile()      { return ( 0 != ( CTX_PROFILE_ES     & ctxOptions ) ); }
  public final boolean isGLForwardCompatible()    { return ( 0 != ( CTX_OPTION_FORWARD & ctxOptions ) ); }
  public final boolean isCreatedWithARBMethod()   { return ( 0 != ( CTX_IS_ARB_CREATED & ctxOptions ) ); }

  /** 
   * Returns a valid OpenGL version string, ie<br>
   * <pre>
   *     major.minor ([option]?[options,]*) - gl-version
   * </pre><br>
   *
   * <ul>
   *   <li> options
   *   <ul>
   *     <li> <code>old</code> refers to the non ARB_create_context created context</li>
   *     <li> <code>new</code> refers to the ARB_create_context created context</li>
   *     <li> <code>compatible profile</code></li>
   *     <li> <code>core profile</code></li>
   *     <li> <code>forward compatible</code></li>
   *     <li> <code>any</code> refers to the non forward compatible context</li>
   *     <li> <code>ES</code>  refers to the GLES context variant</li>
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
  /** <code>ARB_create_context</code> related: ES profile */
  protected static final int CTX_PROFILE_ES     = 1 << 3;
  /** <code>ARB_create_context</code> related: flag forward compatible */
  protected static final int CTX_OPTION_FORWARD = 1 << 4;
  /** <code>ARB_create_context</code> related: not flag forward compatible */
  protected static final int CTX_OPTION_ANY     = 1 << 5;
  /** <code>ARB_create_context</code> related: flag debug */
  protected static final int CTX_OPTION_DEBUG   = 1 << 6;

  
  public final boolean isGL4bc() {
      return ctxMajorVersion>=4 && 0!=(ctxOptions & CTX_PROFILE_COMPAT);
  }

  public final boolean isGL4() {
      return ctxMajorVersion>=4 && 0!=(ctxOptions & (CTX_PROFILE_COMPAT|CTX_PROFILE_CORE));
  }

  public final boolean isGL3bc() {
      return ctxMajorVersion>=3 && 0!=(ctxOptions & CTX_PROFILE_COMPAT);
  }

  public final boolean isGL3() {
      return ctxMajorVersion>=3 && 0!=(ctxOptions & (CTX_PROFILE_COMPAT|CTX_PROFILE_CORE));
  }

  public final boolean isGL2() {
      return ctxMajorVersion>=1 && 0!=(ctxOptions & CTX_PROFILE_COMPAT);
  }

  public final boolean isGL2GL3() {
      return isGL2() || isGL3();
  }

  public final boolean isGLES1() {
      return ctxMajorVersion==1 && 0!=(ctxOptions & CTX_PROFILE_ES);
  }

  public final boolean isGLES2() {
      return ctxMajorVersion==2 && 0!=(ctxOptions & CTX_PROFILE_ES);
  }

  public final boolean isGLES() {
      return isGLEmbeddedProfile();
  }

  public final boolean isGL2ES1() {
      return isGL2() || isGLES1() ;
  }

  public final boolean isGL2ES2() {
      return isGL2GL3() || isGLES2() ;
  }

  public final boolean hasGLSL() {
      return isGL2ES2() ;
  }

  public static final int GL_VERSIONS[][] = {
      /* 0.*/ { -1 },
      /* 1.*/ { 0, 1, 2, 3, 4, 5 },
      /* 2.*/ { 0, 1 },
      /* 3.*/ { 0, 1, 2, 3 },
      /* 4.*/ { 0, 1 } };

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

  /**
   * @param major Key Value either 1, 2, 3 or 4
   * @param profile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   */
  public static final String getGLVersionAvailable(int major, int profile) {
    int _major[] = { 0 };
    int _minor[] = { 0 };
    int _ctp[] = { 0 };
    if(getGLVersionAvailable(major, profile, _major, _minor, _ctp)) {
        return getGLVersion(_major[0], _minor[0], _ctp[0], null);
    }
    return null;
  }

  /**
   * @param reqMajor Key Value either 1, 2, 3 or 4
   * @param reqProfile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   * @param major if not null, returns the used major version
   * @param minor if not null, returns the used minor version
   * @param ctp if not null, returns the used context profile
   */
  public static final boolean getGLVersionAvailable(int reqMajor, int reqProfile, int[] major, int minor[], int ctp[]) {
    int key = compose8bit(reqMajor, reqProfile, 0, 0);
    int val = mappedVersionsAvailable.get( key );
    if(val<=0) {
        return false;
    }
    if(null!=major) {
        major[0] = getComposed8bit(val, 1);
    }
    if(null!=minor) {
        minor[0] = getComposed8bit(val, 2);
    }
    if(null!=ctp) {
        ctp[0]   = getComposed8bit(val, 3);
    }
    return true;
  }

  /**
   * @param major Key Value either 1, 2, 3 or 4
   * @param profile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   */
  public static final boolean isGLVersionAvailable(int major, int profile) {
      return getGLVersionAvailable(major, profile, null, null, null);
  }
  public static final boolean isGL4bcAvailable() { return isGLVersionAvailable(4, CTX_PROFILE_COMPAT); }
  public static final boolean isGL4Available()   { return isGLVersionAvailable(4, CTX_PROFILE_CORE); }
  public static final boolean isGL3bcAvailable() { return isGLVersionAvailable(3, CTX_PROFILE_COMPAT); }
  public static final boolean isGL3Available()   { return isGLVersionAvailable(3, CTX_PROFILE_CORE); }
  public static final boolean isGL2Available()   { return isGLVersionAvailable(2, CTX_PROFILE_COMPAT); }

  public static String getGLVersion(int major, int minor, int ctp, String gl_version) {
    boolean needColon = false;
    StringBuffer sb = new StringBuffer();
    sb.append(major);
    sb.append(".");
    sb.append(minor);
    sb.append(" (");
    needColon = appendString(sb, "ES",                    needColon, 0 != ( CTX_PROFILE_ES & ctp ));
    needColon = appendString(sb, "compatibility profile", needColon, 0 != ( CTX_PROFILE_COMPAT & ctp ));
    needColon = appendString(sb, "core profile",          needColon, 0 != ( CTX_PROFILE_CORE & ctp ));
    needColon = appendString(sb, "forward compatible",    needColon, 0 != ( CTX_OPTION_FORWARD & ctp ));
    needColon = appendString(sb, "any",                   needColon, 0 != ( CTX_OPTION_ANY & ctp ));
    needColon = appendString(sb, "new",                   needColon, 0 != ( CTX_IS_ARB_CREATED & ctp ));
    needColon = appendString(sb, "old",                   needColon, 0 == ( CTX_IS_ARB_CREATED & ctp ));
    sb.append(")");
    if(null!=gl_version) {
        sb.append(" - ");
        sb.append(gl_version);
    }
    return sb.toString();
  }

  protected static final IntIntHashMap mappedVersionsAvailable;
  protected static volatile boolean mappedVersionsAvailableSet;
  protected static final Object mappedVersionsAvailableLock;

  static {
      mappedVersionsAvailableLock = new Object();
      mappedVersionsAvailableSet = false;
      mappedVersionsAvailable = new IntIntHashMap();
      mappedVersionsAvailable.setKeyNotFoundValue(-1);
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

  /**
   * Called by {@link GLContextImpl#createContextARBMapVersionsAvailable} not intendet to be used by 
   * implementations. However, if {@link #createContextARB} is not being used within the 
   * {@link GLDrawableImpl} constructor, GLProfile has to map the available versions.
   *
   * @param reqMajor Key Value either 1, 2, 3 or 4
   * @param profile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}

   * @see #createContextARBMapVersionsAvailable
   */
  protected static void mapVersionAvailable(int reqMajor, int profile, int resMajor, int resMinor, int resCtp)
  {
    validateProfileBits(profile, "profile");
    validateProfileBits(resCtp, "resCtp");

    int key = compose8bit(reqMajor, profile, 0, 0);
    int val = compose8bit(resMajor, resMinor, resCtp, 0);
    mappedVersionsAvailable.put( key, val );
  }

  protected static int compose8bit(int one, int two, int three, int four) {
    return  ( ( one   & 0x000000FF ) << 24 ) |
            ( ( two   & 0x000000FF ) << 16 ) |
            ( ( three & 0x000000FF ) <<  8 ) |
            ( ( four  & 0x000000FF )       ) ;
  }

  protected static int getComposed8bit(int bits32, int which ) {
    switch (which) {
        case 1: return ( bits32 & 0xFF000000 ) >> 24 ;
        case 2: return ( bits32 & 0x00FF0000 ) >> 16 ;
        case 3: return ( bits32 & 0x0000FF00 ) >>  8 ;
        case 4: return ( bits32 & 0xFF0000FF )       ;
    }
    throw new GLException("argument which out of range: "+which);
  }

  protected static String composed8BitToString(int bits32, boolean hex1, boolean hex2, boolean hex3, boolean hex4) {
    int a = getComposed8bit(bits32, 1);
    int b = getComposed8bit(bits32, 2);
    int c = getComposed8bit(bits32, 3);
    int d = getComposed8bit(bits32, 4);
    return "["+toString(a, hex1)+", "+toString(b, hex2)+", "+toString(c, hex3)+", "+toString(d, hex4)+"]";
  }

  protected static String toString(int val, boolean hex) {
    if(hex) {
        return "0x" + Integer.toHexString(val);
    }
    return String.valueOf(val);
  }

  protected static String toHexString(int hex) {
    return "0x" + Integer.toHexString(hex);
  }

  protected static String toHexString(long hex) {
    return "0x" + Long.toHexString(hex);
  }

  private static boolean appendString(StringBuffer sb, String string, boolean needColon, boolean condition) {
    if(condition) {
        if(needColon) {
            sb.append(", ");
        }
        sb.append(string);
        needColon=true;
    }
    return needColon;
  }

}

