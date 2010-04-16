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

package com.jogamp.opengl.impl;

import com.jogamp.common.os.DynamicLookupHelper;
import java.nio.*;
import java.util.*;

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import com.jogamp.nativewindow.impl.NWReflection;
import com.jogamp.gluegen.runtime.*;
import com.jogamp.gluegen.runtime.opengl.*;

public abstract class GLContextImpl extends GLContext {
  protected GLContextLock lock = new GLContextLock();
  protected static final boolean DEBUG = Debug.debug("GLContext");
  protected static final boolean VERBOSE = Debug.verbose();
  // NOTE: default sense of GLContext optimization disabled in JSR-231
  // 1.0 beta 5 due to problems on X11 platforms (both Linux and
  // Solaris) when moving and resizing windows. Apparently GLX tokens
  // get sent to the X server under the hood (and out from under the
  // cover of the AWT lock) in these situations. Users requiring
  // multi-screen X11 applications can manually enable this flag. It
  // basically had no tangible effect on the Windows or Mac OS X
  // platforms anyway in particular with the disabling of the
  // GLWorkerThread which we found to be necessary in 1.0 beta 4.
  protected boolean optimizationEnabled = Debug.isPropertyDefined("jogl.GLContext.optimize", true);

  // Cache of the functions that are available to be called at the current
  // moment in time
  protected ExtensionAvailabilityCache extensionAvailability;
  // Table that holds the addresses of the native C-language entry points for
  // OpenGL functions.
  private ProcAddressTable glProcAddressTable;

  // Tracks creation and initialization of buffer objects to avoid
  // repeated glGet calls upon glMapBuffer operations
  private GLBufferSizeTracker bufferSizeTracker; // Singleton - Set by GLContextShareSet
  private GLBufferStateTracker bufferStateTracker = new GLBufferStateTracker();
  private GLStateTracker glStateTracker = new GLStateTracker();

  protected GLDrawableImpl drawable;
  protected GLDrawableImpl drawableRead;

  protected GL gl;

  public GLContextImpl(GLDrawableImpl drawable, GLDrawableImpl drawableRead, GLContext shareWith) {
    extensionAvailability = new ExtensionAvailabilityCache(this);
    if (shareWith != null) {
      GLContextShareSet.registerSharing(this, shareWith);
    }
    GLContextShareSet.registerForBufferObjectSharing(shareWith, this);
    // This must occur after the above calls into the
    // GLContextShareSet, which set up state needed by the GL object
    setGL(createGL(drawable.getGLProfile()));

    this.drawable = drawable;
    setGLDrawableRead(drawableRead);
  }

  public GLContextImpl(GLDrawableImpl drawable, GLContext shareWith) {
    this(drawable, null, shareWith);
  }

  public void setGLDrawableRead(GLDrawable read) {
    boolean lockHeld = lock.isHeld();
    if(lockHeld) {
        release();
    }
    drawableRead = ( null != read ) ? (GLDrawableImpl) read : drawable;
    if(lockHeld) {
        makeCurrent();
    }
  }

  public GLDrawable getGLDrawable() {
    return drawable;
  }

  public GLDrawable getGLDrawableRead() {
    return drawableRead;
  }

  public GLDrawableImpl getDrawableImpl() {
    return (GLDrawableImpl) getGLDrawable();
  }

  /** 
   * Platform dependent but harmonized implementation of the <code>ARB_create_context</code>
   * mechanism to create a context.<br>
   * The implementation shall verify this context, ie issue a
   * <code>MakeCurrent</code> call if necessary.<br>
   *
   * @param share the shared context or null
   * @param direct flag if direct is requested
   * @param ctxOptionFlags <code>ARB_create_context</code> related, see references below
   * @param major major number
   * @param minor minor number
   * @return the valid context if successfull, or null
   *
   * @see #CTX_PROFILE_COMPAT
   * @see #CTX_OPTION_FORWARD
   * @see #CTX_OPTION_DEBUG
   */
  protected abstract long createContextARBImpl(long share, boolean direct, int ctxOptionFlags, 
                                               int major, int minor);

  private long createContextARB(long share, boolean direct, int ctxOptionFlags, 
                                int majorMax, int minorMax, 
                                int majorMin, int minorMin, 
                                int major[], int minor[]) {
    major[0]=majorMax;
    minor[0]=minorMax;
    long _context=0;

    while ( 0==_context &&
            GLProfile.isValidGLVersion(major[0], minor[0]) &&
            ( major[0]>majorMin || major[0]==majorMin && minor[0] >=minorMin ) ) {

        _context = createContextARBImpl(share, direct, ctxOptionFlags, major[0], minor[0]);

        if(0==_context) {
            if(!GLProfile.decrementGLVersion(major, minor)) break;
        }
    }
    return _context;
  }

  /**
   * Platform independent part of using the <code>ARB_create_context</code>
   * mechanism to create a context.<br>
   */
  protected long createContextARB(long share, boolean direct,
                                  int major[], int minor[], int ctp[]) 
  {
    AbstractGraphicsConfiguration config = drawable.getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration();
    GLCapabilities glCaps = (GLCapabilities) config.getChosenCapabilities();
    GLProfile glp = glCaps.getGLProfile();
    long _context = 0;

    ctp[0] = CTX_IS_ARB_CREATED | CTX_PROFILE_CORE | CTX_OPTION_ANY; // default
    boolean isBackwardCompatibility = glp.isGL2() || glp.isGL3bc() || glp.isGL4bc() ;
    int majorMin, minorMin;
    int majorMax, minorMax;
    if( glp.isGL4() ) {
        // ?? majorMax=GLProfile.getMaxMajor(); minorMax=GLProfile.getMaxMinor(majorMax);
        majorMax=4; minorMax=GLProfile.getMaxMinor(majorMax);
        majorMin=4; minorMin=0;
    } else if( glp.isGL3() ) {
        majorMax=3; minorMax=GLProfile.getMaxMinor(majorMax);
        majorMin=3; minorMin=1;
    } else /* if( glp.isGL2() ) */ {
        majorMax=3; minorMax=0;
        majorMin=1; minorMin=1; // our minimum desktop OpenGL runtime requirements
    }
    // Try the requested ..
    if(isBackwardCompatibility) {
        ctp[0] &= ~CTX_PROFILE_CORE ;
        ctp[0] |=  CTX_PROFILE_COMPAT ;
    }
    _context = createContextARB(share, direct, ctp[0], 
                               /* max */ majorMax, minorMax,
                               /* min */ majorMin, minorMin,
                               /* res */ major, minor);

    if(0==_context && !isBackwardCompatibility) {
        ctp[0] &= ~CTX_PROFILE_COMPAT ;
        ctp[0] |=  CTX_PROFILE_CORE ;
        ctp[0] &= ~CTX_OPTION_ANY ;
        ctp[0] |=  CTX_OPTION_FORWARD ;
        _context = createContextARB(share, direct, ctp[0], 
                                   /* max */ majorMax, minorMax,
                                   /* min */ majorMin, minorMin,
                                   /* res */ major, minor);
       if(0==_context) {
            // Try a compatible one .. even though not requested .. last resort
            ctp[0] &= ~CTX_PROFILE_CORE ;
            ctp[0] |=  CTX_PROFILE_COMPAT ;
            ctp[0] &= ~CTX_OPTION_FORWARD ;
            ctp[0] |=  CTX_OPTION_ANY ;
            _context = createContextARB(share, direct, ctp[0], 
                                       /* max */ majorMax, minorMax,
                                       /* min */ majorMin, minorMin,
                                       /* res */ major, minor);
       }
    }
    return _context;
  }

  public int makeCurrent() throws GLException {
    // Support calls to makeCurrent() over and over again with
    // different contexts without releasing them
    // Could implement this more efficiently without explicit
    // releasing of the underlying context; would require more error
    // checking during the makeCurrentImpl phase
    GLContext current = getCurrent();
    if (current != null) {
      if (current == this) {
        // Assume we don't need to make this context current again
        // For Mac OS X, however, we need to update the context to track resizes
        update();
        return CONTEXT_CURRENT;
      } else {
        current.release();
      }
    }

    if (GLWorkerThread.isStarted() &&
        !GLWorkerThread.isWorkerThread()) {
      // Kick the GLWorkerThread off its current context
      GLWorkerThread.invokeLater(new Runnable() { public void run() {} });
    }

    lock.lock();
    int res = 0;
    try {
      res = makeCurrentImpl();

      /* FIXME: refactor dependence on Java 2D / JOGL bridge
      if ((tracker != null) &&
          (res == CONTEXT_CURRENT_NEW)) {
        // Increase reference count of GLObjectTracker
        tracker.ref();
      }
      */
    } catch (GLException e) {
      lock.unlock();
      throw(e);
    }
    if (res == CONTEXT_NOT_CURRENT) {
      lock.unlock();
    } else {
      if(res == CONTEXT_CURRENT_NEW) {
        // check if the drawable's and the GL's GLProfile are equal
        // throws an GLException if not 
        getGLDrawable().getGLProfile().verifyEquality(gl.getGLProfile());
      }
      setCurrent(this);

      /* FIXME: refactor dependence on Java 2D / JOGL bridge

      // Try cleaning up any stale server-side OpenGL objects
      // FIXME: not sure what to do here if this throws
      if (deletedObjectTracker != null) {
        deletedObjectTracker.clean(getGL());
      }
      */
    }
    return res;
  }

  protected abstract int makeCurrentImpl() throws GLException;

  public void release() throws GLException {
    if (!lock.isHeld()) {
      throw new GLException("Context not current on current thread");
    }
    setCurrent(null);
    try {
      releaseImpl();
    } finally {
      lock.unlock();
    }
  }

  protected abstract void releaseImpl() throws GLException;

  public void destroy() {
    if (lock.isHeld()) {
        // release current context 
        release();
    }

    // Must hold the lock around the destroy operation to make sure we
    // don't destroy the context out from under another thread rendering to it
    lock.lock();
    try {
      /* FIXME: refactor dependence on Java 2D / JOGL bridge
      if (tracker != null) {
        // Don't need to do anything for contexts that haven't been
        // created yet
        if (isCreated()) {
          // If we are tracking creation and destruction of server-side
          // OpenGL objects, we must decrement the reference count of the
          // GLObjectTracker upon context destruction.
          //
          // Note that we can only eagerly delete these server-side
          // objects if there is another context currrent right now
          // which shares textures and display lists with this one.
          tracker.unref(deletedObjectTracker);
        }
      }
      */
  
      // Because we don't know how many other contexts we might be
      // sharing with (and it seems too complicated to implement the
      // GLObjectTracker's ref/unref scheme for the buffer-related
      // optimizations), simply clear the cache of known buffers' sizes
      // when we destroy contexts
      if (bufferSizeTracker != null) {
          bufferSizeTracker.clearCachedBufferSizes();
      }

      if (bufferStateTracker != null) {
          bufferStateTracker.clearBufferObjectState();
      }
  
      if (glStateTracker != null) {
          glStateTracker.clearStates(false);
      }
  
      destroyImpl();
    } finally {
      lock.unlock();
    }
  }

  protected abstract void destroyImpl() throws GLException;

  // This is only needed for Mac OS X on-screen contexts
  protected void update() throws GLException {
  }

  public boolean isSynchronized() {
    return !lock.getFailFastMode();
  }

  public void setSynchronized(boolean isSynchronized) {
    lock.setFailFastMode(!isSynchronized);
  }

  public final GL getGL() {
    return gl;
  }

  public GL setGL(GL gl) {
    if(DEBUG) {
        String sgl1 = (null!=this.gl)?this.gl.getClass().toString()+", "+this.gl.toString():new String("<null>");
        String sgl2 = (null!=gl)?gl.getClass().toString()+", "+gl.toString():new String("<null>");
        Exception e = new Exception("setGL (OpenGL "+getGLVersion()+"): "+Thread.currentThread()+", "+sgl1+" -> "+sgl2);
        e.printStackTrace();
    }
    this.gl = gl;
    return gl;
  }

  public abstract Object getPlatformGLExtensions();

  //----------------------------------------------------------------------
  // Managing the actual OpenGL version, usually figured at creation time.
  // As a last resort, the GL_VERSION string may be used ..
  //

  /** 
   * If major > 0 || minor > 0 : Use passed values, determined at creation time 
   * If major==0 && minor == 0 : Use GL_VERSION
   * Otherwise .. don't touch ..
   */
  protected void setContextVersion(int major, int minor, int ctp) {
      if(major>0 || minor>0) {
          ctxMajorVersion = major;
          ctxMinorVersion = minor;
          ctxOptions = ctp;
          ctxVersionString = getGLVersion(gl, ctxMajorVersion, ctxMinorVersion, ctxOptions, getGL().glGetString(GL.GL_VERSION));
          return;
      }

      if(major==0 && minor==0) {
          String versionStr = getGL().glGetString(GL.GL_VERSION);
          if(null==versionStr) {
            throw new GLException("GL_VERSION is NULL: "+this);
          }

          // Set version
          Version version = new Version(versionStr);
          if (version.isValid()) {
            ctxMajorVersion = version.getMajor();
            ctxMinorVersion = version.getMinor();

            ctxVersionString = getGLVersion(gl, ctxMajorVersion, ctxMinorVersion, ctxOptions, versionStr);
            return;
          }
      }
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

  protected static String getGLVersion(GL gl, int major, int minor, int ctp, String gl_version) {
    boolean needColon = false;
    StringBuffer sb = new StringBuffer();
    sb.append(major);
    sb.append(".");
    sb.append(minor);
    sb.append(" (");
    needColon = appendString(sb, "ES", needColon, null!=gl && gl.isGLES());
    needColon = appendString(sb, "compatibility profile", needColon, 0 != ( CTX_PROFILE_COMPAT & ctp ));
    needColon = appendString(sb, "core profile",          needColon, 0 != ( CTX_PROFILE_CORE & ctp ));
    needColon = appendString(sb, "forward compatible",    needColon, 0 != ( CTX_OPTION_FORWARD & ctp ));
    needColon = appendString(sb, "any",                   needColon, 0 != ( CTX_OPTION_ANY & ctp ));
    needColon = appendString(sb, "new",                   needColon, 0 != ( CTX_IS_ARB_CREATED & ctp ));
    needColon = appendString(sb, "old",                   needColon, 0 == ( CTX_IS_ARB_CREATED & ctp ));
    sb.append(") - ");
    if(null!=gl_version) {
        sb.append(gl_version);
    } else {
        sb.append("n/a");
    }
    return sb.toString();
  }

  //----------------------------------------------------------------------
  // Helpers for various context implementations
  //

  private Object createInstance(GLProfile glp, String suffix, Class[] cstrArgTypes, Object[] cstrArgs) {
    return NWReflection.createInstance(glp.getGLImplBaseClassName()+suffix, cstrArgTypes, cstrArgs);
  }

  /** Create the GL for this context. */
  protected GL createGL(GLProfile glp) {
    GL gl = (GL) createInstance(glp, "Impl", new Class[] { GLProfile.class, GLContextImpl.class }, new Object[] { glp, this } );

    /* FIXME: refactor dependence on Java 2D / JOGL bridge
    if (tracker != null) {
      gl.setObjectTracker(tracker);
    }
    */
    return gl;
  }
  
  public final ProcAddressTable getGLProcAddressTable() {
    return glProcAddressTable;
  }
  
  /**
   * Shall return the platform extension ProcAddressTable,
   * ie for GLXExt, EGLExt, ..
   */
  public abstract ProcAddressTable getPlatformExtProcAddressTable();

  /**
   * Pbuffer support; given that this is a GLContext associated with a
   * pbuffer, binds this pbuffer to its texture target.
   */
  public abstract void bindPbufferToTexture();

  /**
   * Pbuffer support; given that this is a GLContext associated with a
   * pbuffer, releases this pbuffer from its texture target.
   */
  public abstract void releasePbufferFromTexture();

  public abstract ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3);

  public void setSwapInterval(final int interval) {
    GLContext current = getCurrent();
    if (current != this) {
        throw new GLException("This context is not current. Current context: "+current+
                              ", this context "+this);
    }
    setSwapIntervalImpl(interval);
  }

  protected int currentSwapInterval = -1; // default: not set yet ..

  public int getSwapInterval() {
    return currentSwapInterval;
  }

  protected void setSwapIntervalImpl(final int interval) {
    // nop per default ..
  }

  /** Maps the given "platform-independent" function name to a real function
      name. Currently this is only used to map "glAllocateMemoryNV" and
      associated routines to wglAllocateMemoryNV / glXAllocateMemoryNV. */
  protected String mapToRealGLFunctionName(String glFunctionName) {
    Map/*<String, String>*/ map = getFunctionNameMap();
    String lookup = ( null != map ) ? (String) map.get(glFunctionName) : null;
    if (lookup != null) {
      return lookup;
    }
    return glFunctionName;
  }
  protected abstract Map/*<String, String>*/ getFunctionNameMap() ;

  /** Maps the given "platform-independent" extension name to a real
      function name. Currently this is only used to map
      "GL_ARB_pbuffer"      to  "WGL_ARB_pbuffer/GLX_SGIX_pbuffer" and 
      "GL_ARB_pixel_format" to  "WGL_ARB_pixel_format/n.a." 
   */
  protected String mapToRealGLExtensionName(String glExtensionName) {
    Map/*<String, String>*/ map = getExtensionNameMap();
    String lookup = ( null != map ) ? (String) map.get(glExtensionName) : null;
    if (lookup != null) {
      return lookup;
    }
    return glExtensionName;
  }
  protected abstract Map/*<String, String>*/ getExtensionNameMap() ;

  /** Helper routine which resets a ProcAddressTable generated by the
      GLEmitter by looking up anew all of its function pointers. */
  protected void resetProcAddressTable(Object table) {
    GLProcAddressHelper.resetProcAddressTable(table, getDrawableImpl().getDynamicLookupHelper() );
  }

  /** Indicates whether the underlying OpenGL context has been
      created. This is used to manage sharing of display lists and
      textures between contexts. */
  public abstract boolean isCreated();

  /**
   * Sets the OpenGL implementation class and
   * the cache of which GL functions are available for calling through this
   * context. See {@link #isFunctionAvailable(String)} for more information on
   * the definition of "available".
   *
   * @param force force the setting, even if is already being set.
   *              This might be usefull if you change the OpenGL implementation.
   *
   * @see #setContextVersion
   */
  protected void setGLFunctionAvailability(boolean force, int major, int minor, int ctp) {
    if(null!=this.gl && null!=glProcAddressTable && !force) {
        return; // already done and not forced
    }
    if(null==this.gl || force) {
        setGL(createGL(getGLDrawable().getGLProfile()));
    }

    updateGLProcAddressTable(major, minor, ctp);
  }

  /**
   * Updates the cache of which GL functions are available for calling through this
   * context. See {@link #isFunctionAvailable(String)} for more information on
   * the definition of "available".
   *
   * @see #setContextVersion
   */
  protected void updateGLProcAddressTable(int major, int minor, int ctp) {
    if(null==this.gl) {
        throw new GLException("setGLFunctionAvailability not called yet");
    }
    if (DEBUG) {
      System.err.println(getThreadName() + ": !!! Initializing OpenGL extension address table for " + this);
    }
    if (glProcAddressTable == null) {
      glProcAddressTable = (ProcAddressTable) createInstance(gl.getGLProfile(), "ProcAddressTable", new Class[0], null);
      // FIXME: cache ProcAddressTables by capability bits so we can
      // share them among contexts with the same capabilities
    }
    resetProcAddressTable(getGLProcAddressTable());

    setContextVersion(major, minor, ctp);

    extensionAvailability.reset();
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
  public boolean isFunctionAvailable(String glFunctionName) {
    if(isCreated()) {
        // Check GL 1st (cached)
        ProcAddressTable pTable = getGLProcAddressTable();
        try {
            if(0!=pTable.getAddressFor(glFunctionName)) {
                return true;
            }
        } catch (Exception e) {}

        // Check platform extensions 2nd (cached)
        pTable = getPlatformExtProcAddressTable();
        try {
            if(0!=pTable.getAddressFor(glFunctionName)) {
                return true;
            }
        } catch (Exception e) {}
    }
    // dynamic function lookup at last incl name aliasing (not cached)
    DynamicLookupHelper dynLookup = getDrawableImpl().getDynamicLookupHelper();
    String tmpBase = GLExtensionNames.normalizeVEN(GLExtensionNames.normalizeARB(glFunctionName, true), true);
    long addr = 0;
    int  variants = GLExtensionNames.getFuncNamePermutationNumber(tmpBase);
    for(int i = 0; 0==addr && i < variants; i++) {
        String tmp = GLExtensionNames.getFuncNamePermutation(tmpBase, i);
        try {
            addr = dynLookup.dynamicLookupFunction(tmp);
        } catch (Exception e) { }
    }
    if(0!=addr) {
        return true;
    }
    return false;
  }

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
  public boolean isExtensionAvailable(String glExtensionName) {
      return extensionAvailability.isExtensionAvailable(mapToRealGLExtensionName(glExtensionName));
  }

  public String getPlatformExtensionsString() {
      return extensionAvailability.getPlatformExtensionsString();
  }

  public String getGLExtensions() {
      return extensionAvailability.getGLExtensions();
  }

  public boolean isExtensionCacheInitialized() {
      return extensionAvailability.isInitialized();
  }

  /** Indicates which floating-point pbuffer implementation is in
      use. Returns one of GLPbuffer.APPLE_FLOAT, GLPbuffer.ATI_FLOAT,
      or GLPbuffer.NV_FLOAT. */
  public int getFloatingPointMode() throws GLException {
    throw new GLException("Not supported on non-pbuffer contexts");
  }

  /** On some platforms the mismatch between OpenGL's coordinate
      system (origin at bottom left) and the window system's
      coordinate system (origin at top left) necessitates a vertical
      flip of pixels read from offscreen contexts. */
  public abstract boolean offscreenImageNeedsVerticalFlip();

  /** Only called for offscreen contexts; needed by glReadPixels */
  public abstract int getOffscreenContextPixelDataType();

  protected static String getThreadName() {
    return Thread.currentThread().getName();
  }

  public static String toHexString(long hex) {
    return "0x" + Long.toHexString(hex);
  }

  //----------------------------------------------------------------------
  // Helpers for buffer object optimizations
  
  public void setBufferSizeTracker(GLBufferSizeTracker bufferSizeTracker) {
    this.bufferSizeTracker = bufferSizeTracker;
  }

  public GLBufferSizeTracker getBufferSizeTracker() {
    return bufferSizeTracker;
  }

  public GLBufferStateTracker getBufferStateTracker() {
    return bufferStateTracker;
  }

  public GLStateTracker getGLStateTracker() {
    return glStateTracker;
  }

  //---------------------------------------------------------------------------
  // Helpers for context optimization where the last context is left
  // current on the OpenGL worker thread
  //

  public boolean isOptimizable() {
    return optimizationEnabled;
  }

  public boolean hasWaiters() {
    return lock.hasWaiters();
  }

  /* FIXME: needed only by the Java 2D / JOGL bridge; refactor

  public GLContextImpl(GLContext shareWith) {
    this(shareWith, false);
  }
  
  public GLContextImpl(GLContext shareWith, boolean dontShareWithJava2D) {
    extensionAvailability = new ExtensionAvailabilityCache(this);
    GLContext shareContext = shareWith;
    if (!dontShareWithJava2D) {
      shareContext = Java2D.filterShareContext(shareWith);
    }
    if (shareContext != null) {
      GLContextShareSet.registerSharing(this, shareContext);
    }
    // Always indicate real behind-the-scenes sharing to track deleted objects
    if (shareContext == null) {
      shareContext = Java2D.filterShareContext(shareWith);
    }
    GLContextShareSet.registerForObjectTracking(shareWith, this, shareContext);
    GLContextShareSet.registerForBufferObjectSharing(shareWith, this);
    // This must occur after the above calls into the
    // GLContextShareSet, which set up state needed by the GL object
    setGL(createGL());
  }

  //---------------------------------------------------------------------------
  // Helpers for integration with Java2D/OpenGL pipeline when FBOs are
  // being used
  //

  public void setObjectTracker(GLObjectTracker tracker) {
    this.tracker = tracker;
  }
  
  public GLObjectTracker getObjectTracker() {
    return tracker;
  }

  public void setDeletedObjectTracker(GLObjectTracker deletedObjectTracker) {
    this.deletedObjectTracker = deletedObjectTracker;
  }

  public GLObjectTracker getDeletedObjectTracker() {
    return deletedObjectTracker;
  }

  // Tracks creation and deletion of server-side OpenGL objects when
  // the Java2D/OpenGL pipeline is active and using FBOs to render
  private GLObjectTracker tracker;
  // Supports deletion of these objects when no other context is
  // current which can support immediate deletion of them
  private GLObjectTracker deletedObjectTracker;

  */

  /**
   * A class for storing and comparing OpenGL version numbers.
   * This only works for desktop OpenGL at the moment.
   */
  private static class Version implements Comparable
  {
    private boolean valid;
    private int major, minor, sub;
    public Version(int majorRev, int minorRev, int subMinorRev)
    {
      major = majorRev;
      minor = minorRev;
      sub = subMinorRev;
    }

    /**
     * @param versionString must be of the form "GL_VERSION_X" or
     * "GL_VERSION_X_Y" or "GL_VERSION_X_Y_Z" or "X.Y", where X, Y,
     * and Z are integers.
     *
     * @exception IllegalArgumentException if the argument is not a valid
     * OpenGL version identifier
     */
    public Version(String versionString)
    {
      try 
      {
        if (versionString.startsWith("GL_VERSION_"))
        {
          StringTokenizer tok = new StringTokenizer(versionString, "_");

          tok.nextToken(); // GL_
          tok.nextToken(); // VERSION_ 
          if (!tok.hasMoreTokens()) { major = 0; return; }
          major = Integer.valueOf(tok.nextToken()).intValue();
          if (!tok.hasMoreTokens()) { minor = 0; return; }
          minor = Integer.valueOf(tok.nextToken()).intValue();
          if (!tok.hasMoreTokens()) { sub = 0; return; }
          sub = Integer.valueOf(tok.nextToken()).intValue();
        }
        else
        {
          int radix = 10;
          if (versionString.length() > 2) {
            if (Character.isDigit(versionString.charAt(0)) &&
                versionString.charAt(1) == '.' &&
                Character.isDigit(versionString.charAt(2))) {
              major = Character.digit(versionString.charAt(0), radix);
              minor = Character.digit(versionString.charAt(2), radix);

              // See if there's version-specific information which might
              // imply a more recent OpenGL version
              StringTokenizer tok = new StringTokenizer(versionString, " ");
              if (tok.hasMoreTokens()) {
                tok.nextToken();
                if (tok.hasMoreTokens()) {
                  String token = tok.nextToken();
                  int i = 0;
                  while (i < token.length() && !Character.isDigit(token.charAt(i))) {
                    i++;
                  }
                  if (i < token.length() - 2 &&
                      Character.isDigit(token.charAt(i)) &&
                      token.charAt(i+1) == '.' &&
                      Character.isDigit(token.charAt(i+2))) {
                    int altMajor = Character.digit(token.charAt(i), radix);
                    int altMinor = Character.digit(token.charAt(i+2), radix);
                    // Avoid possibly confusing situations by putting some
                    // constraints on the upgrades we do to the major and
                    // minor versions
                    if ((altMajor == major && altMinor > minor) ||
                        altMajor == major + 1) {
                      major = altMajor;
                      minor = altMinor;
                    }
                  }
                }
              }
            }
          }
        }
        valid = true;
      }
      catch (Exception e)
      {
        e.printStackTrace();
        // FIXME: refactor desktop OpenGL dependencies and make this
        // class work properly for OpenGL ES
        System.err.println("ExtensionAvailabilityCache: FunctionAvailabilityCache.Version.<init>: "+e);
        major = 1;
        minor = 0;
        /*
        throw (IllegalArgumentException)
          new IllegalArgumentException(
            "Illegally formatted version identifier: \"" + versionString + "\"")
              .initCause(e);
        */
      }
    }

    public boolean isValid() {
      return valid;
    }

    public int compareTo(Object o)
    {
      Version vo = (Version)o;
      if (major > vo.major) return 1; 
      else if (major < vo.major) return -1; 
      else if (minor > vo.minor) return 1; 
      else if (minor < vo.minor) return -1; 
      else if (sub > vo.sub) return 1; 
      else if (sub < vo.sub) return -1; 

      return 0; // they are equal
    }

    public int getMajor() {
      return major;
    }

    public int getMinor() {
      return minor;
    }
    
  } // end class Version

}
