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

package jogamp.opengl;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import com.jogamp.common.os.DynamicLookupHelper;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.common.util.VersionNumberString;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.gluegen.runtime.FunctionAddressResolver;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLNameResolver;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.GLRendererQuirks;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDebugListener;
import javax.media.opengl.GLDebugMessage;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLPipelineFactory;
import javax.media.opengl.GLProfile;

public abstract class GLContextImpl extends GLContext {
  /**
   * Context full qualified name: display_type + display_connection + major + minor + ctp.
   * This is the key for all cached GL ProcAddressTables, etc, to support multi display/device setups.
   */
  private String contextFQN;

  private int additionalCtxCreationFlags;

  // Cache of the functions that are available to be called at the current
  // moment in time
  protected ExtensionAvailabilityCache extensionAvailability;
  // Table that holds the addresses of the native C-language entry points for
  // OpenGL functions.
  private ProcAddressTable glProcAddressTable;

  private String glVendor;
  private String glRenderer;
  private String glRendererLowerCase;
  private String glVersion;

  // Tracks creation and initialization of buffer objects to avoid
  // repeated glGet calls upon glMapBuffer operations
  private GLBufferSizeTracker bufferSizeTracker; // Singleton - Set by GLContextShareSet
  private final GLBufferStateTracker bufferStateTracker = new GLBufferStateTracker();
  private final GLStateTracker glStateTracker = new GLStateTracker();
  private GLDebugMessageHandler glDebugHandler = null;
  private final int[] boundFBOTarget = new int[] { 0, 0 }; // { draw, read }
  private int defaultVAO = 0; 
  
  protected GLDrawableImpl drawable;
  protected GLDrawableImpl drawableRead;
  
  private volatile boolean pixelDataEvaluated;
  private int /* pixelDataInternalFormat, */ pixelDataFormat, pixelDataType;
  
  protected GL gl;

  protected static final Object mappedContextTypeObjectLock;
  protected static final HashMap<String, ExtensionAvailabilityCache> mappedExtensionAvailabilityCache;
  protected static final HashMap<String, ProcAddressTable> mappedGLProcAddress;
  protected static final HashMap<String, ProcAddressTable> mappedGLXProcAddress;

  static {
      mappedContextTypeObjectLock = new Object();
      mappedExtensionAvailabilityCache = new HashMap<String, ExtensionAvailabilityCache>();
      mappedGLProcAddress = new HashMap<String, ProcAddressTable>();
      mappedGLXProcAddress = new HashMap<String, ProcAddressTable>();
  }

  public static void shutdownImpl() {
      mappedExtensionAvailabilityCache.clear();
      mappedGLProcAddress.clear();
      mappedGLXProcAddress.clear();
  }

  public GLContextImpl(GLDrawableImpl drawable, GLContext shareWith) {
    super();

    if (shareWith != null) {
      GLContextShareSet.registerSharing(this, shareWith);
    }
    GLContextShareSet.synchronizeBufferObjectSharing(shareWith, this);

    this.drawable = drawable;
    this.drawableRead = drawable;

    this.glDebugHandler = new GLDebugMessageHandler(this);
  }

  @Override
  protected void resetStates() {
      // Because we don't know how many other contexts we might be
      // sharing with (and it seems too complicated to implement the
      // GLObjectTracker's ref/unref scheme for the buffer-related
      // optimizations), simply clear the cache of known buffers' sizes
      // when we destroy contexts
      if (bufferSizeTracker != null) {
          bufferSizeTracker.clearCachedBufferSizes();
      }

      if (bufferStateTracker != null) { // <init>
          bufferStateTracker.clearBufferObjectState();
      }

      if (glStateTracker != null) { // <init>
          glStateTracker.clearStates(false);
      }

      extensionAvailability = null;
      glProcAddressTable = null;
      gl = null;
      contextFQN = null;
      additionalCtxCreationFlags = 0;

      glVendor = "";
      glRenderer = glVendor;
      glRendererLowerCase = glRenderer;
      glVersion = glVendor;
      
      if (boundFBOTarget != null) { // <init>
          boundFBOTarget[0] = 0; // draw
          boundFBOTarget[1] = 0; // read
      }
      
      pixelDataEvaluated = false;

      super.resetStates();
  }

  @Override
  public final GLDrawable setGLReadDrawable(GLDrawable read) {
    if(!isGLReadDrawableAvailable()) { 
        throw new GLException("Setting read drawable feature not available");
    }
    final boolean lockHeld = lock.isOwner(Thread.currentThread());
    if(lockHeld) {
        release();
    } else if(lock.isLockedByOtherThread()) { // still could glitch ..
        throw new GLException("GLContext current by other thread ("+lock.getOwner()+"), operation not allowed.");
    }
    final GLDrawable old = drawableRead;
    drawableRead = ( null != read ) ? (GLDrawableImpl) read : drawable;
    if(lockHeld) {
        makeCurrent();
    }
    return old;
  }

  @Override
  public final GLDrawable getGLReadDrawable() {
    return drawableRead;
  }

  @Override
  public final GLDrawable setGLDrawable(GLDrawable readWrite, boolean setWriteOnly) {
    if( drawable == readWrite && ( setWriteOnly || drawableRead == readWrite ) ) {
        return drawable; // no change.
    }
    final Thread currentThread = Thread.currentThread();
    if( lock.isLockedByOtherThread() ) {
        throw new GLException("GLContext current by other thread "+lock.getOwner().getName()+", operation not allowed on this thread "+currentThread.getName());
    }    
    final boolean lockHeld = lock.isOwner(currentThread);
    if( lockHeld && lock.getHoldCount() > 1 ) {
        // would need to makeCurrent * holdCount
        throw new GLException("GLContext is recursively locked - unsupported for setGLDrawable(..)");
    }
    final GLDrawableImpl old = drawable;
    if( isCreated() && null != old && old.isRealized() ) {
        if(!lockHeld) {
            makeCurrent();
        }
        associateDrawable(false);
        if(!lockHeld) {
            release();
        }
    }
    if(lockHeld) {
        release();
    }
    if( !setWriteOnly || drawableRead == drawable ) { // if !setWriteOnly || !explicitReadDrawable
        drawableRead = (GLDrawableImpl) readWrite;
    }
    drawableRetargeted |= null != drawable && readWrite != drawable;
    drawable = (GLDrawableImpl) readWrite ;
    if( isCreated() && null != drawable && drawable.isRealized() ) {
        makeCurrent(true); // implicit: associateDrawable(true)
        if( !lockHeld ) {
            release();
        }
    }
    return old;
  }

  @Override
  public final GLDrawable getGLDrawable() {
    return drawable;
  }

  public final GLDrawableImpl getDrawableImpl() {
    return (GLDrawableImpl) getGLDrawable();
  }

  @Override
  public final GL getGL() {
    return gl;
  }

  @Override
  public GL setGL(GL gl) {
    if(DEBUG) {
        String sgl1 = (null!=this.gl)?this.gl.getClass().getSimpleName()+", "+this.gl.toString():"<null>";
        String sgl2 = (null!=gl)?gl.getClass().getSimpleName()+", "+gl.toString():"<null>";
        Exception e = new Exception("Info: setGL (OpenGL "+getGLVersion()+"): "+getThreadName()+", "+sgl1+" -> "+sgl2);
        e.printStackTrace();
    }
    this.gl = gl;
    return gl;
  }

  /**
   * Call this method to notify the OpenGL context
   * that the drawable has changed (size or position).
   *
   * <p>
   * This is currently being used and overridden by Mac OSX,
   * which issues the {@link jogamp.opengl.macosx.cgl.CGL#updateContext(long) NSOpenGLContext update()} call.
   * </p>
   *
   * @throws GLException
   */
  protected void drawableUpdatedNotify() throws GLException { }

  public abstract Object getPlatformGLExtensions();

  // Note: the surface is locked within [makeCurrent .. swap .. release]
  @Override
  public void release() throws GLException {
    release(false);
  }  
  private void release(boolean inDestruction) throws GLException {
    if( TRACE_SWITCH ) {
        System.err.println(getThreadName() +": GLContext.ContextSwitch[release.0]: obj " + toHexString(hashCode()) + ", ctx "+toHexString(contextHandle)+", surf "+toHexString(drawable.getHandle())+", inDestruction: "+inDestruction+", "+lock);
    }
    if ( !lock.isOwner(Thread.currentThread()) ) {
        final String msg = getThreadName() +": Context not current on current thread, obj " + toHexString(hashCode())+", ctx "+toHexString(contextHandle)+", surf "+toHexString(drawable.getHandle())+", inDestruction: "+inDestruction+", "+lock;
        if( DEBUG_TRACE_SWITCH ) {
            System.err.println(msg);
            if( null != lastCtxReleaseStack ) {
                System.err.print("Last release call: ");
                lastCtxReleaseStack.printStackTrace();
            } else {
                System.err.println("Last release call: NONE");
            }
        }
        throw new GLException(msg);
    }
    
    Throwable drawableContextMadeCurrentException = null;
    final boolean actualRelease = ( inDestruction || lock.getHoldCount() == 1 ) && 0 != contextHandle;
    try {
        if( actualRelease ) {
            if( !inDestruction ) {
                try {
                    contextMadeCurrent(false);
                } catch (Throwable t) {
                    drawableContextMadeCurrentException = t;
                }
            }
            releaseImpl();
        }
    } finally {
      // exception prone ..
      if( actualRelease ) {
          setCurrent(null);
      }
      drawable.unlockSurface();
      lock.unlock();
      if( DEBUG_TRACE_SWITCH ) {
          final String msg = getThreadName() +": GLContext.ContextSwitch[release.X]: obj " + toHexString(hashCode()) + ", ctx "+toHexString(contextHandle)+", surf "+toHexString(drawable.getHandle())+" - "+(actualRelease?"switch":"keep  ")+" - "+lock;
          lastCtxReleaseStack = new Throwable(msg);
          if( TRACE_SWITCH ) {
              System.err.println(msg);
              // Thread.dumpStack();
          }
      }
    }
    if(null != drawableContextMadeCurrentException) {
      throw new GLException("GLContext.release(false) during GLDrawableImpl.contextMadeCurrent(this, false)", drawableContextMadeCurrentException);
    }
    
  }
  private Throwable lastCtxReleaseStack = null;
  protected abstract void releaseImpl() throws GLException;

  @Override
  public final void destroy() {
      if ( DEBUG_TRACE_SWITCH ) {
          final long drawH = null != drawable ? drawable.getHandle() : 0;
          System.err.println(getThreadName() + ": GLContextImpl.destroy.0: obj " + toHexString(hashCode()) + ", ctx " + toHexString(contextHandle) +
                  ", surf "+toHexString(drawH)+", isShared "+GLContextShareSet.isShared(this)+" - "+lock);
      }
      if ( 0 != contextHandle ) { // isCreated() ?
          if ( null == drawable ) {
              throw new GLException("GLContext created but drawable is null: "+toString());
          }
          final int lockRes = drawable.lockSurface();
          if ( NativeSurface.LOCK_SURFACE_NOT_READY >= lockRes ) {
                // this would be odd ..
                throw new GLException("Surface not ready to lock: "+drawable);
          }
          Throwable associateDrawableException = null;
          try {
              if ( !drawable.isRealized() ) {
                  throw new GLException("GLContext created but drawable not realized: "+toString());
              }
              // Must hold the lock around the destroy operation to make sure we
              // don't destroy the context while another thread renders to it.
              lock.lock(); // holdCount++ -> 1 - n (1: not locked, 2-n: destroy while rendering)
              if ( lock.getHoldCount() > 2 ) {
                  final String msg = getThreadName() + ": GLContextImpl.destroy: obj " + toHexString(hashCode()) + ", ctx " + toHexString(contextHandle);
                  if ( DEBUG_TRACE_SWITCH ) {
                      System.err.println(msg+" - Lock was hold more than once - makeCurrent/release imbalance: "+lock);
                      Thread.dumpStack();
                  }
              }
              try {
                  // if not current, makeCurrent(), to call associateDrawable(..) and to disable debug handler
                  if ( lock.getHoldCount() == 1 ) {
                      if ( GLContext.CONTEXT_NOT_CURRENT == makeCurrent() ) {
                          throw new GLException("GLContext.makeCurrent() failed: "+toString());
                      }
                  }
                  try {
                      associateDrawable(false);
                  } catch (Throwable t) {
                      associateDrawableException = t;
                  }
                  if ( 0 != defaultVAO ) {
                      int[] tmp = new int[] { defaultVAO };
                      gl.getGL2GL3().glBindVertexArray(0);
                      gl.getGL2GL3().glDeleteVertexArrays(1, tmp, 0);
                      defaultVAO = 0;
                  }
                  glDebugHandler.enable(false);
                  if(lock.getHoldCount() > 1) {
                      // pending release() after makeCurrent()
                      release(true);
                  }
                  destroyImpl();
                  contextHandle = 0;
                  glDebugHandler = null;
                  // this maybe impl. in a platform specific way to release remaining shared ctx.
                  if(GLContextShareSet.contextDestroyed(this) && !GLContextShareSet.hasCreatedSharedLeft(this)) {
                      GLContextShareSet.unregisterSharing(this);
                  }
              } finally {
                  lock.unlock();
                  if ( DEBUG_TRACE_SWITCH ) {
                      System.err.println(getThreadName() + ": GLContextImpl.destroy.X: obj " + toHexString(hashCode()) + ", ctx " + toHexString(contextHandle) +
                              ", isShared "+GLContextShareSet.isShared(this)+" - "+lock);
                  }
              }
          } finally {
              drawable.unlockSurface();
          }
          if( null != associateDrawableException ) {
              throw new GLException("GLContext.destroy() during associateDrawable(false)", associateDrawableException);
          }
      }
      resetStates();
  }
  protected abstract void destroyImpl() throws GLException;

  @Override
  public final void copy(GLContext source, int mask) throws GLException {
    if (source.getHandle() == 0) {
      throw new GLException("Source OpenGL context has not been created");
    }
    if (getHandle() == 0) {
      throw new GLException("Destination OpenGL context has not been created");
    }

    final int lockRes = drawable.lockSurface();
    if (NativeSurface.LOCK_SURFACE_NOT_READY >= lockRes) {
        // this would be odd ..
        throw new GLException("Surface not ready to lock");
    }
    try {
        copyImpl(source, mask);
    } finally {
      drawable.unlockSurface();
    }
  }
  protected abstract void copyImpl(GLContext source, int mask) throws GLException;

  //----------------------------------------------------------------------
  //

  /**
   * {@inheritDoc}
   * <p>
   * MakeCurrent functionality, which also issues the creation of the actual OpenGL context.
   * </p>
   * The complete callgraph for general OpenGL context creation is:<br>
   * <ul>
   *    <li> {@link #makeCurrent} <i>GLContextImpl</i></li>
   *    <li> {@link #makeCurrentImpl} <i>Platform Implementation</i></li>
   *    <li> {@link #create} <i>Platform Implementation</i></li>
   *    <li> If <code>ARB_create_context</code> is supported:
   *    <ul>
   *        <li> {@link #createContextARB} <i>GLContextImpl</i></li>
   *        <li> {@link #createContextARBImpl} <i>Platform Implementation</i></li>
   *    </ul></li>
   * </ul><br>
   *
   * Once at startup, ie triggered by the singleton constructor of a {@link GLDrawableFactoryImpl} specialization,
   * calling {@link #createContextARB} will query all available OpenGL versions:<br>
   * <ul>
   *    <li> <code>FOR ALL GL* DO</code>:
   *    <ul>
   *        <li> {@link #createContextARBMapVersionsAvailable}
   *        <ul>
   *            <li> {@link #createContextARBVersions}</li>
   *        </ul></li>
   *        <li> {@link #mapVersionAvailable}</li>
   *    </ul></li>
   * </ul><br>
   *
   * @see #makeCurrentImpl
   * @see #create
   * @see #createContextARB
   * @see #createContextARBImpl
   * @see #mapVersionAvailable
   * @see #destroyContextARBImpl
   */
  @Override
  public final int makeCurrent() throws GLException {
      return makeCurrent(false);
  }
  
  protected final int makeCurrent(boolean forceDrawableAssociation) throws GLException {
    if( TRACE_SWITCH ) {
        System.err.println(getThreadName() +": GLContext.ContextSwitch[makeCurrent.0]: obj " + toHexString(hashCode()) + ", ctx "+toHexString(contextHandle)+", surf "+toHexString(drawable.getHandle())+" - "+lock);
    }      

    // Note: the surface is locked within [makeCurrent .. swap .. release]
    final int lockRes = drawable.lockSurface();
    if (NativeSurface.LOCK_SURFACE_NOT_READY >= lockRes) {
        if( DEBUG_TRACE_SWITCH ) {
            System.err.println(getThreadName() +": GLContext.ContextSwitch[makeCurrent.X1]: obj " + toHexString(hashCode()) + ", ctx "+toHexString(contextHandle)+", surf "+toHexString(drawable.getHandle())+" - Surface Not Ready - CONTEXT_NOT_CURRENT - "+lock);                        
        }
        return CONTEXT_NOT_CURRENT;
    }
    
    boolean unlockResources = true; // Must be cleared if successful, otherwise finally block will release context and/or surface!
    int res = CONTEXT_NOT_CURRENT;
    try {
        if ( drawable.isRealized() ) {
            if ( 0 == drawable.getHandle() ) {
                throw new GLException("drawable has invalid handle: "+drawable);
            }
            lock.lock();
            try {
                // One context can only be current by one thread,
                // and one thread can only have one context current!
                final GLContext current = getCurrent();
                if (current != null) {
                    if (current == this) { // implicit recursive locking!
                        // Assume we don't need to make this context current again
                        // For Mac OS X, however, we need to update the context to track resizes
                        drawableUpdatedNotify();
                        unlockResources = false; // success
                        if( TRACE_SWITCH ) {
                            System.err.println(getThreadName() +": GLContext.ContextSwitch[makeCurrent.X2]: obj " + toHexString(hashCode()) + ", ctx "+toHexString(contextHandle)+", surf "+toHexString(drawable.getHandle())+" - keep   - CONTEXT_CURRENT - "+lock);                        
                        }
                        return CONTEXT_CURRENT;
                    } else {
                        current.release();
                    }
                }
                res = makeCurrentWithinLock(lockRes);
                unlockResources = CONTEXT_NOT_CURRENT == res; // success ?
                
                /**
                 * FIXME: refactor dependence on Java 2D / JOGL bridge
                    if ( tracker != null && res == CONTEXT_CURRENT_NEW ) {
                        // Increase reference count of GLObjectTracker
                        tracker.ref();
                    }
                 */
            } catch (RuntimeException e) {
              unlockResources = true;
              throw e;
            } finally {
              if (unlockResources) {
                if( DEBUG_TRACE_SWITCH ) {
                  System.err.println(getThreadName() +": GLContext.ContextSwitch[makeCurrent.1]: Context lock.unlock() due to error, res "+makeCurrentResultToString(res)+", "+lock);
                }
                lock.unlock();
              }
            }
        } /* if ( drawable.isRealized() ) */
    } catch (RuntimeException e) {
      unlockResources = true;
      throw e;
    } finally {
      if (unlockResources) {
        drawable.unlockSurface();
      }
    }

    if (res != CONTEXT_NOT_CURRENT) {
      setCurrent(this);
      if(res == CONTEXT_CURRENT_NEW) {
        // check if the drawable's and the GL's GLProfile are equal
        // throws an GLException if not
        getGLDrawable().getGLProfile().verifyEquality(gl.getGLProfile());

        glDebugHandler.init( isGL2GL3() && isGLDebugEnabled() );

        if(DEBUG_GL) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Debug", null, gl, null) );
            if(glDebugHandler.isEnabled()) {
                glDebugHandler.addListener(new GLDebugMessageHandler.StdErrGLDebugListener(true));
            }
        }
        if(TRACE_GL) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Trace", null, gl, new Object[] { System.err } ) );
        }
        
        forceDrawableAssociation = true;
      }
      
      if( forceDrawableAssociation ) {
          associateDrawable(true);
      }
      
      contextMadeCurrent(true);
      
      /* FIXME: refactor dependence on Java 2D / JOGL bridge

      // Try cleaning up any stale server-side OpenGL objects
      // FIXME: not sure what to do here if this throws
      if (deletedObjectTracker != null) {
        deletedObjectTracker.clean(getGL());
      }
      */
    }
    if( TRACE_SWITCH ) {
        System.err.println(getThreadName() +": GLContext.ContextSwitch[makeCurrent.X3]: obj " + toHexString(hashCode()) + ", ctx "+toHexString(contextHandle)+", surf "+toHexString(drawable.getHandle())+" - switch - "+makeCurrentResultToString(res)+" - "+lock);
    }      
    return res;
  }
  
  private final int makeCurrentWithinLock(int surfaceLockRes) throws GLException {
      if (!isCreated()) {
        if( 0 >= drawable.getWidth() || 0 >= drawable.getHeight() ) {
            if ( DEBUG_TRACE_SWITCH ) {
                System.err.println(getThreadName() + ": Create GL context REJECTED (zero surface size) obj " + toHexString(hashCode()) + ", surf "+toHexString(drawable.getHandle())+" for " + getClass().getName());
                System.err.println(drawable.toString());
            }
            return CONTEXT_NOT_CURRENT;
        }
        if(DEBUG_GL) {
            // only impacts w/ createContextARB(..)
            additionalCtxCreationFlags |= GLContext.CTX_OPTION_DEBUG ;
        }

        final GLContextImpl shareWith = (GLContextImpl) GLContextShareSet.getShareContext(this);
        if (null != shareWith) {
            shareWith.getDrawableImpl().lockSurface();
        }
        final boolean created;
        try {
            created = createImpl(shareWith); // may throws exception if fails!
            if( created && isGL3core() ) {
                // Due to GL 3.1 core spec: E.1. DEPRECATED AND REMOVED FEATURES (p 296),
                //        GL 3.2 core spec: E.2. DEPRECATED AND REMOVED FEATURES (p 331)
                // there is no more default VAO buffer 0 bound, hence generating and binding one
                // to avoid INVALID_OPERATION at VertexAttribPointer. 
                // More clear is GL 4.3 core spec: 10.4 (p 307).
                final int[] tmp = new int[1];
                gl.getGL2GL3().glGenVertexArrays(1, tmp, 0);
                defaultVAO = tmp[0];
                gl.getGL2GL3().glBindVertexArray(defaultVAO);
            }
        } finally {
            if (null != shareWith) {
                shareWith.getDrawableImpl().unlockSurface();
            }
        }
        if ( DEBUG_TRACE_SWITCH ) {
            if(created) {
                System.err.println(getThreadName() + ": Create GL context OK: obj " + toHexString(hashCode()) + ", ctx " + toHexString(contextHandle) + ", surf "+toHexString(drawable.getHandle())+" for " + getClass().getName()+" - "+getGLVersion());
                // Thread.dumpStack();
            } else {
                System.err.println(getThreadName() + ": Create GL context FAILED obj " + toHexString(hashCode()) + ", surf "+toHexString(drawable.getHandle())+" for " + getClass().getName());
            }
        }
        if(!created) {
            return CONTEXT_NOT_CURRENT;
        }

        // finalize mapping the available GLVersions, in case it's not done yet
        {
            final AbstractGraphicsConfiguration config = drawable.getNativeSurface().getGraphicsConfiguration();
            final AbstractGraphicsDevice device = config.getScreen().getDevice();

            // Non ARB desktop profiles may not have been registered 
            if( !GLContext.getAvailableGLVersionsSet(device) ) {        // not yet set
                if( 0 == ( ctxOptions & GLContext.CTX_PROFILE_ES) ) {   // not ES profile
                    final int reqMajor;
                    final int reqProfile;
                    if( ctxVersion.compareTo(Version30) <= 0 ) {
                        reqMajor = 2;
                    } else {
                        reqMajor = ctxVersion.getMajor();
                    }
                    if( 0 != ( ctxOptions & GLContext.CTX_PROFILE_CORE) ) {
                        reqProfile = GLContext.CTX_PROFILE_CORE;
                    } else {
                        reqProfile = GLContext.CTX_PROFILE_COMPAT;
                    }
                    GLContext.mapAvailableGLVersion(device, reqMajor, reqProfile,
                                                    ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                    GLContext.setAvailableGLVersionsSet(device);
                    
                    if (DEBUG) {
                      System.err.println(getThreadName() + ": createContextOLD-MapVersionsAvailable HAVE: " + device+" -> "+reqMajor+"."+reqProfile+ " -> "+getGLVersion());
                    }                    
                }
            }
        }
        GLContextShareSet.contextCreated(this);
        return CONTEXT_CURRENT_NEW;
      }
      makeCurrentImpl();
      return CONTEXT_CURRENT;
  }
  protected abstract void makeCurrentImpl() throws GLException;

  /**
   * Calls {@link GLDrawableImpl#associateContext(GLContext, boolean)} 
   */ 
  protected void associateDrawable(boolean bound) { 
      drawable.associateContext(this, bound);      
  }
  
  /**
   * Calls {@link GLDrawableImpl#contextMadeCurrent(GLContext, boolean)} 
   */ 
  protected void contextMadeCurrent(boolean current) {
      drawable.contextMadeCurrent(this, current);      
  }

  /**
   * Platform dependent entry point for context creation.<br>
   *
   * This method is called from {@link #makeCurrentWithinLock()} .. {@link #makeCurrent()} .<br>
   *
   * The implementation shall verify this context with a
   * <code>MakeContextCurrent</code> call.<br>
   *
   * The implementation <b>must</b> leave the context current.<br>
   *
   * @param share the shared context or null
   * @return the valid and current context if successful, or null
   * @throws GLException
   */
  protected abstract boolean createImpl(GLContextImpl sharedWith) throws GLException ;

  /**
   * Platform dependent but harmonized implementation of the <code>ARB_create_context</code>
   * mechanism to create a context.<br>
   *
   * This method is called from {@link #createContextARB}, {@link #createImpl(GLContextImpl)} .. {@link #makeCurrent()} .<br>
   *
   * The implementation shall verify this context with a
   * <code>MakeContextCurrent</code> call.<br>
   *
   * The implementation <b>must</b> leave the context current.<br>
   *
   * @param share the shared context or null
   * @param direct flag if direct is requested
   * @param ctxOptionFlags <code>ARB_create_context</code> related, see references below
   * @param major major number
   * @param minor minor number
   * @return the valid and current context if successful, or null
   *
   * @see #makeCurrent
   * @see #CTX_PROFILE_COMPAT
   * @see #CTX_OPTION_FORWARD
   * @see #CTX_OPTION_DEBUG
   * @see #makeCurrentImpl
   * @see #create
   * @see #createContextARB
   * @see #createContextARBImpl
   * @see #destroyContextARBImpl
   */
  protected abstract long createContextARBImpl(long share, boolean direct, int ctxOptionFlags, int major, int minor);

  /**
   * Destroy the context created by {@link #createContextARBImpl}.
   *
   * @see #makeCurrent
   * @see #makeCurrentImpl
   * @see #create
   * @see #createContextARB
   * @see #createContextARBImpl
   * @see #destroyContextARBImpl
   */
  protected abstract void destroyContextARBImpl(long context);

  /**
   * Platform independent part of using the <code>ARB_create_context</code>
   * mechanism to create a context.<br>
   *
   * The implementation of {@link #create} shall use this protocol in case the platform supports <code>ARB_create_context</code>.<br>
   *
   * This method may call {@link #createContextARBImpl} and {@link #destroyContextARBImpl}. <br>
   *
   * This method will also query all available native OpenGL context when first called,<br>
   * usually the first call should happen with the shared GLContext of the DrawableFactory.<br>
   *
   * The implementation makes the context current, if successful<br>
   *
   * @see #makeCurrentImpl
   * @see #create
   * @see #createContextARB
   * @see #createContextARBImpl
   * @see #destroyContextARBImpl
   */
  protected final long createContextARB(final long share, final boolean direct)
  {
    final AbstractGraphicsConfiguration config = drawable.getNativeSurface().getGraphicsConfiguration();
    final AbstractGraphicsDevice device = config.getScreen().getDevice();

    if (DEBUG) {
      System.err.println(getThreadName() + ": createContextARB: mappedVersionsAvailableSet("+device.getConnection()+"): "+
               GLContext.getAvailableGLVersionsSet(device));
    }

    if ( !GLContext.getAvailableGLVersionsSet(device) ) {
        if(!mapGLVersions(device)) {
            // none of the ARB context creation calls was successful, bail out
            return 0;
        }
    }

    final GLCapabilitiesImmutable glCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    final int[] reqMajorCTP = new int[] { 0, 0 };
    getRequestMajorAndCompat(glCaps.getGLProfile(), reqMajorCTP);
    
    int _major[] = { 0 };
    int _minor[] = { 0 };
    int _ctp[] = { 0 };
    long _ctx = 0;
    if( GLContext.getAvailableGLVersion(device, reqMajorCTP[0], reqMajorCTP[1],
                                        _major, _minor, _ctp)) {
        _ctp[0] |= additionalCtxCreationFlags;
        _ctx = createContextARBImpl(share, direct, _ctp[0], _major[0], _minor[0]);
        if(0!=_ctx) {
            setGLFunctionAvailability(true, _major[0], _minor[0], _ctp[0], false);
        }
    }
    return _ctx;
  }
  
  private final boolean mapGLVersions(AbstractGraphicsDevice device) {
    synchronized (GLContext.deviceVersionAvailable) {
        final long t0 = ( DEBUG ) ? System.nanoTime() : 0;
        boolean success = false;
        // Following GLProfile.GL_PROFILE_LIST_ALL order of profile detection { GL4bc, GL3bc, GL2, GL4, GL3, GL2GL3, GLES2, GL2ES2, GLES1, GL2ES1 }
        boolean hasGL4bc = false;
        boolean hasGL3bc = false;
        boolean hasGL2   = false;
        boolean hasGL4   = false;
        boolean hasGL3   = false;
        
        // Even w/ PROFILE_ALIASING, try to use true core GL profiles
        // ensuring proper user behavior across platforms due to different feature sets!
        //
        if(!hasGL4) {
            hasGL4   = createContextARBMapVersionsAvailable(4, CTX_PROFILE_CORE);    // GL4
            success |= hasGL4;
            if(hasGL4) {
                // Map all lower compatible profiles: GL3
                GLContext.mapAvailableGLVersion(device, 3, CTX_PROFILE_CORE, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);                
                if(PROFILE_ALIASING) {
                    hasGL3   = true;
                }
                resetStates(); // clean context states, since creation was temporary
            }
        }
        if(!hasGL3) {
            hasGL3   = createContextARBMapVersionsAvailable(3, CTX_PROFILE_CORE);    // GL3
            success |= hasGL3;
            if(hasGL3) {
                resetStates(); // clean this context states, since creation was temporary                
            }
        }
        if(!hasGL4bc) {
            hasGL4bc = createContextARBMapVersionsAvailable(4, CTX_PROFILE_COMPAT);  // GL4bc
            success |= hasGL4bc;
            if(hasGL4bc) {
                // Map all lower compatible profiles: GL3bc, GL2, GL4, GL3
                GLContext.mapAvailableGLVersion(device, 3, CTX_PROFILE_COMPAT, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                GLContext.mapAvailableGLVersion(device, 2, CTX_PROFILE_COMPAT, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                if(!hasGL4) {
                    GLContext.mapAvailableGLVersion(device, 4, CTX_PROFILE_CORE, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                }
                if(!hasGL3) {
                    GLContext.mapAvailableGLVersion(device, 3, CTX_PROFILE_CORE, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                }
                if(PROFILE_ALIASING) {
                    hasGL3bc = true;
                    hasGL2   = true;
                    hasGL4   = true;
                    hasGL3   = true;
                }
                resetStates(); // clean this context states, since creation was temporary
            }
        }
        if(!hasGL3bc) {
            hasGL3bc = createContextARBMapVersionsAvailable(3, CTX_PROFILE_COMPAT);  // GL3bc
            success |= hasGL3bc;
            if(hasGL3bc) {
                // Map all lower compatible profiles: GL2 and GL3
                GLContext.mapAvailableGLVersion(device, 2, CTX_PROFILE_COMPAT, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                if(!hasGL3) {
                    GLContext.mapAvailableGLVersion(device, 3, CTX_PROFILE_CORE, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                }
                if(PROFILE_ALIASING) {
                    hasGL2   = true;
                    hasGL3   = true;
                }
                resetStates(); // clean this context states, since creation was temporary
            }
        }
        if(!hasGL2) {
            hasGL2   = createContextARBMapVersionsAvailable(2, CTX_PROFILE_COMPAT);  // GL2
            success |= hasGL2;
            if(hasGL2) {
                resetStates(); // clean this context states, since creation was temporary                
            }
        }
        if(success) {
            // only claim GL versions set [and hence detected] if ARB context creation was successful
            GLContext.setAvailableGLVersionsSet(device);
            if(DEBUG) {
                final long t1 = System.nanoTime();
                System.err.println("GLContextImpl.mapGLVersions: "+device+", profileAliasing: "+PROFILE_ALIASING+", total "+(t1-t0)/1e6 +"ms");
                System.err.println(GLContext.dumpAvailableGLVersions(null).toString());                
            }
        } else if (DEBUG) {
            System.err.println(getThreadName() + ": createContextARB-MapVersions NONE for :"+device);
        }
        return success;
    }
  }

  /** 
   * Note: Since context creation is temporary, caller need to issue {@link #resetStates()}, if creation was successful, i.e. returns true.
   * This method does not reset the states, allowing the caller to utilize the state variables. 
   **/
  private final boolean createContextARBMapVersionsAvailable(int reqMajor, int reqProfile) {
    long _context;
    int ctp = CTX_IS_ARB_CREATED;
    if(CTX_PROFILE_COMPAT == reqProfile) {
        ctp |= CTX_PROFILE_COMPAT ;
    } else {
        ctp |= CTX_PROFILE_CORE ;
    }

    // To ensure GL profile compatibility within the JOGL application
    // we always try to map against the highest GL version,
    // so the user can always cast to the highest available one.
    int majorMax, minorMax;
    int majorMin, minorMin;
    int major[] = new int[1];
    int minor[] = new int[1];
    if( 4 == reqMajor ) {
        majorMax=4; minorMax=GLContext.getMaxMinor(majorMax);
        majorMin=4; minorMin=0;
    } else if( 3 == reqMajor ) {
        majorMax=3; minorMax=GLContext.getMaxMinor(majorMax);
        majorMin=3; minorMin=1;
    } else /* if( glp.isGL2() ) */ {
        // our minimum desktop OpenGL runtime requirements are 1.1,
        // nevertheless we restrict ARB context creation to 2.0 to spare us futile attempts
        majorMax=3; minorMax=0;
        majorMin=2; minorMin=0;
    }
    _context = createContextARBVersions(0, true, ctp,
                                        /* max */ majorMax, minorMax,
                                        /* min */ majorMin, minorMin,
                                        /* res */ major, minor);

    if( 0 == _context && CTX_PROFILE_CORE == reqProfile && !PROFILE_ALIASING ) {
        // try w/ FORWARD instead of CORE
        ctp &= ~CTX_PROFILE_CORE ;
        ctp |=  CTX_OPTION_FORWARD ;
        _context = createContextARBVersions(0, true, ctp,
                                            /* max */ majorMax, minorMax,
                                            /* min */ majorMin, minorMin,
                                            /* res */ major, minor);
       if( 0 == _context ) {
            // Try a compatible one .. even though not requested .. last resort
            ctp &= ~CTX_PROFILE_CORE ;
            ctp &= ~CTX_OPTION_FORWARD ;
            ctp |=  CTX_PROFILE_COMPAT ;
            _context = createContextARBVersions(0, true, ctp,
                                       /* max */ majorMax, minorMax,
                                       /* min */ majorMin, minorMin,
                                       /* res */ major, minor);
       }
    }
    final boolean res;
    if( 0 != _context ) {
        AbstractGraphicsDevice device = drawable.getNativeSurface().getGraphicsConfiguration().getScreen().getDevice();
        // ctxMajorVersion, ctxMinorVersion, ctxOptions is being set by
        //   createContextARBVersions(..) -> setGLFunctionAvailbility(..) -> setContextVersion(..)
        GLContext.mapAvailableGLVersion(device, reqMajor, reqProfile, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
        destroyContextARBImpl(_context);
        if (DEBUG) {
          System.err.println(getThreadName() + ": createContextARB-MapVersionsAvailable HAVE: " +reqMajor+"."+reqProfile+ " -> "+getGLVersion());
        }
        res = true;
    } else {
        if (DEBUG) {
          System.err.println(getThreadName() + ": createContextARB-MapVersionsAvailable NOPE: "+reqMajor+"."+reqProfile);
        }
        res = false;
    }
    return res;
  }

  private final long createContextARBVersions(long share, boolean direct, int ctxOptionFlags,
                                              int majorMax, int minorMax,
                                              int majorMin, int minorMin,
                                              int major[], int minor[]) {
    major[0]=majorMax;
    minor[0]=minorMax;
    long _context=0;

    while ( GLContext.isValidGLVersion(major[0], minor[0]) &&
            ( major[0]>majorMin || major[0]==majorMin && minor[0] >=minorMin ) ) {
        if (DEBUG) {
            System.err.println(getThreadName() + ": createContextARBVersions: share "+share+", direct "+direct+", version "+major[0]+"."+minor[0]);
        }
        _context = createContextARBImpl(share, direct, ctxOptionFlags, major[0], minor[0]);

        if(0 != _context) {
            if( setGLFunctionAvailability(true, major[0], minor[0], ctxOptionFlags, true) ) {
                break;
            } else {
                destroyContextARBImpl(_context);
                _context = 0;
            }
        }

        if(!GLContext.decrementGLVersion(major, minor)) {
            break;
        }
    }
    return _context;
  }

  //----------------------------------------------------------------------
  // Managing the actual OpenGL version, usually figured at creation time.
  // As a last resort, the GL_VERSION string may be used ..
  //

  /**
   * If major > 0 || minor > 0 : Use passed values, determined at creation time
   * Otherwise .. don't touch ..
   */
  private final void setContextVersion(int major, int minor, int ctp, VersionNumberString glVendorVersion, boolean useGL) {
      if ( 0 == ctp ) {
        throw new GLException("Invalid GL Version "+major+"."+minor+", ctp "+toHexString(ctp));
      }
      
      if (!GLContext.isValidGLVersion(major, minor)) {
        throw new GLException("Invalid GL Version "+major+"."+minor+", ctp "+toHexString(ctp));
      }
      ctxVersion = new VersionNumber(major, minor, 0);
      ctxVersionString = getGLVersion(major, minor, ctxOptions, glVersion);
      ctxVendorVersion = glVendorVersion;
      ctxOptions = ctp;
      if(useGL) {
          ctxGLSLVersion = VersionNumber.zeroVersion;
          if( hasGLSL() ) { // >= ES2 || GL2.0
              final String glslVersion = isGLES() ? null : gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION) ; // Use static GLSL version for ES to be safe!
              if( null != glslVersion ) {
                  ctxGLSLVersion = new VersionNumber(glslVersion);
                  if( ctxGLSLVersion.getMajor() < 1 ) {
                      ctxGLSLVersion = VersionNumber.zeroVersion; // failed ..
                  }
              }
              if( ctxGLSLVersion.isZero() ) {
                  ctxGLSLVersion = getStaticGLSLVersionNumber(major, minor, ctxOptions);
              }
          } 
      }
  }
  
  //----------------------------------------------------------------------
  // Helpers for various context implementations
  //

  private Object createInstance(GLProfile glp, String suffix, Class<?>[] cstrArgTypes, Object[] cstrArgs) {
    return ReflectionUtil.createInstance(glp.getGLImplBaseClassName()+suffix, cstrArgTypes, cstrArgs, getClass().getClassLoader());
  }

  private boolean verifyInstance(GLProfile glp, String suffix, Object instance) {
    return ReflectionUtil.instanceOf(instance, glp.getGLImplBaseClassName()+suffix);
  }

  /** Create the GL for this context. */
  protected GL createGL(GLProfile glp) {
    final GL gl = (GL) createInstance(glp, "Impl", new Class[] { GLProfile.class, GLContextImpl.class }, new Object[] { glp, this } );

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
   * @throws GLException if not implemented (default)
   * @deprecated use FBO/GLOffscreenAutoDrawable instead of pbuffer
   */
  public void bindPbufferToTexture() { throw new GLException("not implemented"); }

  /**
   * Pbuffer support; given that this is a GLContext associated with a
   * pbuffer, releases this pbuffer from its texture target.
   * @throws GLException if not implemented (default)
   * @deprecated use FBO/GLOffscreenAutoDrawable instead of pbuffer
   */
  public void releasePbufferFromTexture() { throw new GLException("not implemented"); }

  public abstract ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3);

  /** Maps the given "platform-independent" function name to a real function
      name. Currently this is only used to map "glAllocateMemoryNV" and
      associated routines to wglAllocateMemoryNV / glXAllocateMemoryNV. */
  protected final String mapToRealGLFunctionName(String glFunctionName) {
    Map<String, String> map = getFunctionNameMap();
    String lookup = ( null != map ) ? map.get(glFunctionName) : null;
    if (lookup != null) {
      return lookup;
    }
    return glFunctionName;
  }
  protected abstract Map<String, String> getFunctionNameMap() ;

  /** Maps the given "platform-independent" extension name to a real
      function name. Currently this is only used to map
      "GL_ARB_pbuffer"      to  "WGL_ARB_pbuffer/GLX_SGIX_pbuffer" and
      "GL_ARB_pixel_format" to  "WGL_ARB_pixel_format/n.a."
   */
  protected final String mapToRealGLExtensionName(String glExtensionName) {
    Map<String, String> map = getExtensionNameMap();
    String lookup = ( null != map ) ? map.get(glExtensionName) : null;
    if (lookup != null) {
      return lookup;
    }
    return glExtensionName;
  }
  protected abstract Map<String, String> getExtensionNameMap() ;

  /** Helper routine which resets a ProcAddressTable generated by the
      GLEmitter by looking up anew all of its function pointers. */
  protected final void resetProcAddressTable(final ProcAddressTable table) {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
        public Object run() {
            table.reset(getDrawableImpl().getGLDynamicLookupHelper() );
            return null;
        }
    } );
  }
  
  private final boolean initGLRendererAndGLVersionStrings()  {
    final GLDynamicLookupHelper glDynLookupHelper = getDrawableImpl().getGLDynamicLookupHelper();
    final long _glGetString = glDynLookupHelper.dynamicLookupFunction("glGetString");
    if(0 == _glGetString) {
        System.err.println("Error: Entry point to 'glGetString' is NULL.");
        if(DEBUG) {
            Thread.dumpStack();
        }
        return false;
    } else {
        final String _glVendor = glGetStringInt(GL.GL_VENDOR, _glGetString);
        if(null == _glVendor) {
            if(DEBUG) {
                System.err.println("Warning: GL_VENDOR is NULL.");
                Thread.dumpStack();
            }
            return false;
        }
        glVendor = _glVendor;
        
        final String _glRenderer = glGetStringInt(GL.GL_RENDERER, _glGetString);
        if(null == _glRenderer) {
            if(DEBUG) {
                System.err.println("Warning: GL_RENDERER is NULL.");
                Thread.dumpStack();
            }
            return false;
        }
        glRenderer = _glRenderer;
        glRendererLowerCase = glRenderer.toLowerCase();
        
        final String _glVersion = glGetStringInt(GL.GL_VERSION, _glGetString);
        if(null == _glVersion) {
            // FIXME
            if(DEBUG) {
                System.err.println("Warning: GL_VERSION is NULL.");
                Thread.dumpStack();
            }
            return false;
        }
        glVersion = _glVersion;
        
        return true;
    }
  }

  /**
   * We cannot promote a non ARB context to >= 3.1, reduce it to 3.0 then.
   */
  private static void limitNonARBContextVersion(int[] major, int[] minor, int ctp) {
      if ( 0 == (ctp & CTX_IS_ARB_CREATED) && ( major[0] > 3 || major[0] == 3 && minor[0] >= 1 ) ) {
          major[0] = 3;
          minor[0] = 0;
      }
  }
  
  /**
   * Returns null if version string is invalid, otherwise a valid instance.
   * <p>
   * Note: Non ARB ctx is limited to GL 3.0.
   * </p>
   */
  private static final VersionNumber getGLVersionNumber(int ctp, String glVersionStr) {
      if( null != glVersionStr ) {
          final GLVersionNumber version = GLVersionNumber.create(glVersionStr);
          if ( version.isValid() ) {
              int[] major = new int[] { version.getMajor() };
              int[] minor = new int[] { version.getMinor() };
              limitNonARBContextVersion(major, minor, ctp);
              if ( GLContext.isValidGLVersion(major[0], minor[0]) ) {
                  return new VersionNumber(major[0], minor[0], 0);
              }
          }
      }
      return null;
  }

  /**
   * Returns false if <code>glGetIntegerv</code> is inaccessible, otherwise queries major.minor
   * version for given arrays.
   * <p>
   * If the GL query fails, major will be zero.
   * </p> 
   * <p>
   * Note: Non ARB ctx is limited to GL 3.0.
   * </p>
   */
  private final boolean getGLIntVersion(int[] glIntMajor, int[] glIntMinor, int ctp)  {
    glIntMajor[0] = 0; // clear
    final GLDynamicLookupHelper glDynLookupHelper = getDrawableImpl().getGLDynamicLookupHelper();
    final long _glGetIntegerv = glDynLookupHelper.dynamicLookupFunction("glGetIntegerv");
    if( 0 == _glGetIntegerv ) {
        System.err.println("Error: Entry point to 'glGetIntegerv' is NULL.");
        if(DEBUG) {
            Thread.dumpStack();
        }
        return false;        
    } else {
        glGetIntegervInt(GL2GL3.GL_MAJOR_VERSION, glIntMajor, 0, _glGetIntegerv);
        glGetIntegervInt(GL2GL3.GL_MINOR_VERSION, glIntMinor, 0, _glGetIntegerv);
        limitNonARBContextVersion(glIntMajor, glIntMinor, ctp);
        return true;        
    }
  }
  
  /**
   * Sets the OpenGL implementation class and
   * the cache of which GL functions are available for calling through this
   * context. See {@link #isFunctionAvailable(String)} for more information on
   * the definition of "available".
   * <br>
   * All ProcaddressTables are being determined and cached, the GL version is being set
   * and the extension cache is determined as well.
   *
   * @param force force the setting, even if is already being set.
   *              This might be useful if you change the OpenGL implementation.
   * @param major OpenGL major version
   * @param minor OpenGL minor version
   * @param ctxProfileBits OpenGL context profile and option bits, see {@link javax.media.opengl.GLContext#CTX_OPTION_ANY}
   * @param strictMatch if <code>true</code> the ctx must
   *                    <ul>
   *                      <li>be greater or equal than the requested <code>major.minor</code> version, and</li>
   *                      <li>match the ctxProfileBits</li>
   *                    </ul>, otherwise method aborts and returns <code>false</code>.
   * @return returns <code>true</code> if successful, otherwise <code>false</code>. See <code>strictMatch</code>. 
   *                 If <code>false</code> is returned, no data has been cached or mapped, i.e. ProcAddressTable, Extensions, Version, etc. 
   * @see #setContextVersion
   * @see javax.media.opengl.GLContext#CTX_OPTION_ANY
   * @see javax.media.opengl.GLContext#CTX_PROFILE_COMPAT
   * @see javax.media.opengl.GLContext#CTX_IMPL_ES2_COMPAT
   */
  protected final boolean setGLFunctionAvailability(boolean force, int major, int minor, int ctxProfileBits, boolean strictMatch) {
    if(null!=this.gl && null!=glProcAddressTable && !force) {
        return true; // already done and not forced
    }

    if ( 0 < major && !GLContext.isValidGLVersion(major, minor) ) {
        throw new GLException("Invalid GL Version Request "+GLContext.getGLVersion(major, minor, ctxProfileBits, null));
    }
    
    if(null==this.gl || !verifyInstance(gl.getGLProfile(), "Impl", this.gl)) {
        setGL( createGL( getGLDrawable().getGLProfile() ) );
    }
    updateGLXProcAddressTable();

    final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration();
    final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
    
    {
        final boolean initGLRendererAndGLVersionStringsOK = initGLRendererAndGLVersionStrings();
        if( !initGLRendererAndGLVersionStringsOK ) {
            final String errMsg = "Intialization of GL renderer strings failed. "+adevice+" - "+GLContext.getGLVersion(major, minor, ctxProfileBits, null);
            if( strictMatch ) {
                // query mode .. simply fail
                if(DEBUG) {
                    System.err.println("Warning: setGLFunctionAvailability: "+errMsg);
                }
                return false;
            } else {
                // unusable GL context - non query mode - hard fail!
                throw new GLException(errMsg);
            }
        } else if(DEBUG) {
            System.err.println(getThreadName() + ": GLContext.setGLFuncAvail: Given "+adevice+" - "+GLContext.getGLVersion(major, minor, ctxProfileBits, glVersion));
        }
    }
    
    //
    // Validate GL version either by GL-Integer or GL-String
    //
    if (DEBUG) {
        System.err.println(getThreadName() + ": GLContext.setGLFuncAvail: Pre version verification - expected "+GLContext.getGLVersion(major, minor, ctxProfileBits, null)+", strictMatch "+strictMatch);
    }
    boolean versionValidated = false;
    boolean versionGL3IntFailed = false;
    {
        // Validate the requested version w/ the GL-version from an integer query. 
        final int[] glIntMajor = new int[] { 0 }, glIntMinor = new int[] { 0 }; 
        final boolean getGLIntVersionOK = getGLIntVersion(glIntMajor, glIntMinor, ctxProfileBits);
        if( !getGLIntVersionOK ) {
            final String errMsg = "Fetching GL Integer Version failed. "+adevice+" - "+GLContext.getGLVersion(major, minor, ctxProfileBits, null);
            if( strictMatch ) {
                // query mode .. simply fail
                if(DEBUG) {
                    System.err.println("Warning: setGLFunctionAvailability: "+errMsg);
                }
                return false;
            } else {
                // unusable GL context - non query mode - hard fail!
                throw new GLException(errMsg);
            }
        }        
        if (DEBUG) {
            System.err.println(getThreadName() + ": GLContext.setGLFuncAvail: version verification (Int): "+glVersion+", "+glIntMajor[0]+"."+glIntMinor[0]);
        }
        
        // Only validate if a valid int version was fetched, otherwise cont. w/ version-string method -> 3.0 > Version || Version > MAX!
        if ( GLContext.isValidGLVersion(glIntMajor[0], glIntMinor[0]) ) {
            if( glIntMajor[0]<major || ( glIntMajor[0]==major && glIntMinor[0]<minor ) || 0 == major ) {        
                if( strictMatch && 2 < major ) { // relaxed match for versions major < 3 requests, last resort!
                    if(DEBUG) {
                        System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: FAIL, GL version mismatch (Int): "+GLContext.getGLVersion(major, minor, ctxProfileBits, null)+" -> "+glVersion+", "+glIntMajor[0]+"."+glIntMinor[0]);
                    }
                    return false;
                }
                major = glIntMajor[0];
                minor = glIntMinor[0];
            }
            versionValidated = true;
        } else {
            versionGL3IntFailed = true;
        }
    }
    if( !versionValidated ) {
        // Validate the requested version w/ the GL-version from the version string. 
        final VersionNumber setGLVersionNumber = new VersionNumber(major, minor, 0);
        final VersionNumber strGLVersionNumber = getGLVersionNumber(ctxProfileBits, glVersion);
        if (DEBUG) {
            System.err.println(getThreadName() + ": GLContext.setGLFuncAvail: version verification (String): "+glVersion+", "+strGLVersionNumber);
        }
        
        // Only validate if a valid string version was fetched -> MIN > Version || Version > MAX!
        if( null != strGLVersionNumber ) {
            if( strGLVersionNumber.compareTo(setGLVersionNumber) < 0 || 0 == major ) {
                if( strictMatch && 2 < major ) { // relaxed match for versions major < 3 requests, last resort!
                    if(DEBUG) {
                        System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: FAIL, GL version mismatch (String): "+GLContext.getGLVersion(major, minor, ctxProfileBits, null)+" -> "+glVersion+", "+strGLVersionNumber);
                    }
                    return false;
                }
                major = strGLVersionNumber.getMajor();
                minor = strGLVersionNumber.getMinor();
            }
            if( strictMatch && versionGL3IntFailed && major >= 3 ) {
                if(DEBUG) {
                    System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: FAIL, GL3 version Int failed, String: "+GLContext.getGLVersion(major, minor, ctxProfileBits, null)+" -> "+glVersion+", "+strGLVersionNumber);
                }
                return false;            
            }
            versionValidated = true;
        }
    }
    if( strictMatch && !versionValidated && 0 < major ) {
        if(DEBUG) {
            System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: FAIL, No GL version validation possible: "+GLContext.getGLVersion(major, minor, ctxProfileBits, null)+" -> "+glVersion);
        }
        return false;                    
    }
    if (DEBUG) {
        System.err.println(getThreadName() + ": GLContext.setGLFuncAvail: post version verification "+GLContext.getGLVersion(major, minor, ctxProfileBits, null)+", strictMatch "+strictMatch+", versionValidated "+versionValidated+", versionGL3IntFailed "+versionGL3IntFailed);
    }
    
    if( 2 > major ) { // there is no ES2-compat for a profile w/ major < 2
        ctxProfileBits &= ~GLContext.CTX_IMPL_ES2_COMPAT;
    }
    
    final VersionNumberString vendorVersion = GLVersionNumber.createVendorVersion(glVersion);
    
    setRendererQuirks(adevice, major, minor, ctxProfileBits, vendorVersion);
    
    if( strictMatch && glRendererQuirks.exist(GLRendererQuirks.GLNonCompliant) ) {
        if(DEBUG) {
            System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: FAIL, GL is not compliant: "+GLContext.getGLVersion(major, minor, ctxProfileBits, glVersion)+", "+glRenderer);
        }
        return false;
    }
    
    if(!isCurrentContextHardwareRasterizer()) {
        ctxProfileBits |= GLContext.CTX_IMPL_ACCEL_SOFT;
    }

    contextFQN = getContextFQN(adevice, major, minor, ctxProfileBits);
    if (DEBUG) {
        System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.0 validated FQN: "+contextFQN+" - "+GLContext.getGLVersion(major, minor, ctxProfileBits, glVersion));
    }

    //
    // UpdateGLProcAddressTable functionality
    //
    ProcAddressTable table = null;
    synchronized(mappedContextTypeObjectLock) {
        table = mappedGLProcAddress.get( contextFQN );
        if(null != table && !verifyInstance(gl.getGLProfile(), "ProcAddressTable", table)) {
            throw new InternalError("GLContext GL ProcAddressTable mapped key("+contextFQN+" - " + GLContext.getGLVersion(major, minor, ctxProfileBits, null)+
                  ") -> "+ table.getClass().getName()+" not matching "+gl.getGLProfile().getGLImplBaseClassName());
        }
    }
    if(null != table) {
        glProcAddressTable = table;
        if(DEBUG) {
            System.err.println(getThreadName() + ": GLContext GL ProcAddressTable reusing key("+contextFQN+") -> "+toHexString(table.hashCode()));
        }
    } else {
        glProcAddressTable = (ProcAddressTable) createInstance(gl.getGLProfile(), "ProcAddressTable",
                                                               new Class[] { FunctionAddressResolver.class } ,
                                                               new Object[] { new GLProcAddressResolver() } );
        resetProcAddressTable(getGLProcAddressTable());
        synchronized(mappedContextTypeObjectLock) {
            mappedGLProcAddress.put(contextFQN, getGLProcAddressTable());
            if(DEBUG) {
                System.err.println(getThreadName() + ": GLContext GL ProcAddressTable mapping key("+contextFQN+") -> "+toHexString(getGLProcAddressTable().hashCode()));
            }
        }
    }

    //
    // Update ExtensionAvailabilityCache
    //
    ExtensionAvailabilityCache eCache;
    synchronized(mappedContextTypeObjectLock) {
        eCache = mappedExtensionAvailabilityCache.get( contextFQN );
    }
    if(null !=  eCache) {
        extensionAvailability = eCache;
        if(DEBUG) {
            System.err.println(getThreadName() + ": GLContext GL ExtensionAvailabilityCache reusing key("+contextFQN+") -> "+toHexString(eCache.hashCode()) + " - entries: "+eCache.getTotalExtensionCount());
        }
    } else {
        extensionAvailability = new ExtensionAvailabilityCache();
        setContextVersion(major, minor, ctxProfileBits, vendorVersion, false); // pre-set of GL version, required for extension cache usage
        extensionAvailability.reset(this);
        synchronized(mappedContextTypeObjectLock) {
            mappedExtensionAvailabilityCache.put(contextFQN, extensionAvailability);
            if(DEBUG) {
                System.err.println(getThreadName() + ": GLContext GL ExtensionAvailabilityCache mapping key("+contextFQN+") -> "+toHexString(extensionAvailability.hashCode()) + " - entries: "+extensionAvailability.getTotalExtensionCount());
            }
        }
    }
    
    if( ( 0 != ( CTX_PROFILE_ES & ctxProfileBits ) && major >= 2 ) || isExtensionAvailable(GLExtensions.ARB_ES2_compatibility) ) {
        ctxProfileBits |= CTX_IMPL_ES2_COMPAT;
        ctxProfileBits |= CTX_IMPL_FBO;
    } else if( hasFBOImpl(major, ctxProfileBits, extensionAvailability) ) {
        ctxProfileBits |= CTX_IMPL_FBO;
    }
    
    if(FORCE_NO_FBO_SUPPORT) {
        ctxProfileBits &= ~CTX_IMPL_FBO ;
    }      
    
    //
    // Set GL Version (complete w/ version string)
    //
    setContextVersion(major, minor, ctxProfileBits, vendorVersion, true);
    
    setDefaultSwapInterval();
    
    final int glErrX = gl.glGetError(); // clear GL error, maybe caused by above operations
    
    if(DEBUG) {
        System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: OK "+contextFQN+" - "+GLContext.getGLVersion(ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions, null)+" - glErr "+toHexString(glErrX));
    }
    return true;
  }
  
  private final void setRendererQuirks(final AbstractGraphicsDevice adevice, int major, int minor, int ctp, final VersionNumberString vendorVersion) {
    int[] quirks = new int[GLRendererQuirks.COUNT + 1]; // + 1 ( NoFullFBOSupport )
    int i = 0;
    
    final String MesaSP = "Mesa ";
    final String MesaRendererAMDsp = " AMD "; 
    final String MesaRendererIntelsp = "Intel(R)"; 
    final boolean hwAccel = 0 == ( ctp & GLContext.CTX_IMPL_ACCEL_SOFT );
    final boolean compatCtx = 0 != ( ctp & GLContext.CTX_PROFILE_COMPAT );
    final boolean isDriverMesa = glRenderer.contains(MesaSP) || glRenderer.contains("Gallium ");
    final boolean isDriverATICatalyst = !isDriverMesa && ( glVendor.contains("ATI Technologies") || glRenderer.startsWith("ATI ") );
    final boolean isDriverNVIDIAGeForce = !isDriverMesa && ( glVendor.contains("NVIDIA Corporation") || glRenderer.contains("NVIDIA ") );
    
    //
    // OS related quirks
    //
    if( Platform.getOSType() == Platform.OSType.MACOS ) {
        //
        // OSX
        //
        {
            final int quirk = GLRendererQuirks.NoOffscreenBitmap;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType());
            }
            quirks[i++] = quirk;
        }
        
        final VersionNumber OSXVersion173 = new VersionNumber(1,7,3);
        if( Platform.getOSVersionNumber().compareTo(OSXVersion173) < 0 && isDriverNVIDIAGeForce ) {
            final int quirk = GLRendererQuirks.GLFlushBeforeRelease;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType()+", OS Version "+Platform.getOSVersionNumber()+", Renderer "+glRenderer);
            }
            quirks[i++] = quirk;
        }
    } else if( Platform.getOSType() == Platform.OSType.WINDOWS ) {        
        //
        // WINDOWS
        //
        {
            final int quirk = GLRendererQuirks.NoDoubleBufferedBitmap;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType());
            }
            quirks[i++] = quirk;
        }
        
        if( isDriverATICatalyst ) {
            final VersionNumber winXPVersionNumber = new VersionNumber ( 5, 1, 0);          
            final VersionNumber amdSafeMobilityVersion = new VersionNumber(12, 102, 3);  
            
            if ( vendorVersion.compareTo(amdSafeMobilityVersion) < 0 ) { // includes: vendorVersion.isZero()
                final int quirk = GLRendererQuirks.NeedCurrCtx4ARBCreateContext;
                if(DEBUG) {
                    System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType()+", [Vendor "+glVendor+" or Renderer "+glRenderer+"], driverVersion "+vendorVersion);
                }
                quirks[i++] = quirk;                
            }
            
            if( Platform.getOSVersionNumber().compareTo(winXPVersionNumber) <= 0 ) {
                final int quirk = GLRendererQuirks.NeedCurrCtx4ARBPixFmtQueries;
                if(DEBUG) {
                    System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS-Version "+Platform.getOSType()+" "+Platform.getOSVersionNumber()+", [Vendor "+glVendor+" or Renderer "+glRenderer+"]");
                }
                quirks[i++] = quirk;                
            }
        }
    } else if( Platform.OSType.ANDROID == Platform.getOSType() ) {    
        //
        // ANDROID
        //
        // Renderer related quirks, may also involve OS
        if( glRenderer.contains("PowerVR") ) {
            final int quirk = GLRendererQuirks.NoSetSwapInterval;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType() + " / Renderer " + glRenderer);
            }
            quirks[i++] = quirk;
        }
    }
    
    //
    // Windowing Toolkit related quirks
    //
    if( NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(true) ) {
        //
        // X11
        //
        {
            //
            // Quirk: DontCloseX11Display
            //
            final int quirk = GLRendererQuirks.DontCloseX11Display;
            if( glRenderer.contains(MesaSP) ) {
                if ( glRenderer.contains("X11") && vendorVersion.compareTo(Version80) < 0 ) {
                    if(DEBUG) {
                        System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: X11 Renderer=" + glRenderer + ", Version=[vendor " + vendorVersion + ", GL " + glVersion+"]");
                    }
                    quirks[i++] = quirk;
                }
            } else if( isDriverATICatalyst ) {
                {
                    if(DEBUG) {
                        System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: X11 Renderer=" + glRenderer);
                    }
                    quirks[i++] = quirk;
                }
            } else if( jogamp.nativewindow.x11.X11Util.getMarkAllDisplaysUnclosable() ) {
                {
                    if(DEBUG) {
                        System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: X11Util Downstream");
                    }
                    quirks[i++] = quirk;
                }
            }
        }
    }
    
    
    //
    // RENDERER related quirks
    //
    if( isDriverMesa ) {
        {
            final int quirk = GLRendererQuirks.NoSetSwapIntervalPostRetarget;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: Renderer " + glRenderer);
            }
            quirks[i++] = quirk;
        }
        if( hwAccel /* glRenderer.contains( MesaRendererIntelsp ) || glRenderer.contains( MesaRendererAMDsp ) */ )
        {
            final int quirk = GLRendererQuirks.NoDoubleBufferedPBuffer;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: Renderer " + glRenderer);
            }
            quirks[i++] = quirk;
        }
        if( ( (glRenderer.contains( MesaRendererIntelsp ) && compatCtx) || glRenderer.contains( MesaRendererAMDsp ) ) && 
            ( major > 3 || major == 3 && minor >= 1 ) 
          )
        {
            // FIXME: Apply vendor version constraints!
            final int quirk = GLRendererQuirks.GLNonCompliant;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: Renderer " + glRenderer);
            }
            quirks[i++] = quirk;
        }
        if( Platform.getOSType() == Platform.OSType.WINDOWS && glRenderer.contains("SVGA3D") )
        {
            final VersionNumber mesaSafeFBOVersion = new VersionNumber(8, 0, 0);              
            if ( vendorVersion.compareTo(mesaSafeFBOVersion) < 0 ) { // includes: vendorVersion.isZero()            
                final int quirk = GLRendererQuirks.NoFullFBOSupport;
                if(DEBUG) {
                    System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType() + " / Renderer " + glRenderer + " / Mesa-Version "+vendorVersion);
                }
                quirks[i++] = quirk;
            }
        }
    }
    
    //
    // Property related quirks
    //
    if( FORCE_MIN_FBO_SUPPORT ) {
        final int quirk = GLRendererQuirks.NoFullFBOSupport;
        if(DEBUG) {
            System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: property");
        }
        quirks[i++] = quirk;        
    }
    
    glRendererQuirks = new GLRendererQuirks(quirks, 0, i);
  }
    
  private static final boolean hasFBOImpl(int major, int ctp, ExtensionAvailabilityCache extCache) {
    return ( 0 != (ctp & CTX_PROFILE_ES) && major >= 2 ) ||   // ES >= 2.0
            
           major >= 3 ||                                                 // any >= 3.0 GL ctx                       
           
           ( null != extCache &&
           
               extCache.isExtensionAvailable(GLExtensions.ARB_ES2_compatibility)  ||         // ES 2.0 compatible
               
               extCache.isExtensionAvailable(GLExtensions.ARB_framebuffer_object) ||         // ARB_framebuffer_object
               
               extCache.isExtensionAvailable(GLExtensions.EXT_framebuffer_object) ||         // EXT_framebuffer_object
               
               extCache.isExtensionAvailable(GLExtensions.OES_framebuffer_object) ) ;        // OES_framebuffer_object excluded               
  }
  
  private final void removeCachedVersion(int major, int minor, int ctxProfileBits) {
    if(!isCurrentContextHardwareRasterizer()) {
        ctxProfileBits |= GLContext.CTX_IMPL_ACCEL_SOFT;
    }
    final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration();
    final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();

    contextFQN = getContextFQN(adevice, major, minor, ctxProfileBits);
    if (DEBUG) {
      System.err.println(getThreadName() + ": RM Context FQN: "+contextFQN+" - "+GLContext.getGLVersion(major, minor, ctxProfileBits, null));
    }

    synchronized(mappedContextTypeObjectLock) {
        final ProcAddressTable table = mappedGLProcAddress.remove( contextFQN );
        if(DEBUG) {
            final int hc = null != table ? table.hashCode() : 0;
            System.err.println(getThreadName() + ": RM GLContext GL ProcAddressTable mapping key("+contextFQN+") -> "+toHexString(hc));
        }
    }

    synchronized(mappedContextTypeObjectLock) {
        final ExtensionAvailabilityCache  eCache = mappedExtensionAvailabilityCache.remove( contextFQN );
        if(DEBUG) {
            final int hc = null != eCache ? eCache.hashCode() : 0;
            System.err.println(getThreadName() + ": RM GLContext GL ExtensionAvailabilityCache mapping key("+contextFQN+") -> "+toHexString(hc));
        }
    }
  }

  private final boolean isCurrentContextHardwareRasterizer()  {
    boolean isHardwareRasterizer = true;

    if(!drawable.getChosenGLCapabilities().getHardwareAccelerated()) {
        isHardwareRasterizer = false;
    } else {
        isHardwareRasterizer = ! ( glRendererLowerCase.contains("software") /* Mesa3D  */ ||
                                   glRendererLowerCase.contains("mesa x11") /* Mesa3D  */ ||
                                   glRendererLowerCase.contains("softpipe") /* Gallium */ ||
                                   glRendererLowerCase.contains("llvmpipe") /* Gallium */
                                 );
    }
    return isHardwareRasterizer;
  }

  /**
   * Updates the platform's 'GLX' function cache
   */
  protected abstract void updateGLXProcAddressTable();

  protected abstract StringBuilder getPlatformExtensionsStringImpl();

  @Override
  public final boolean isFunctionAvailable(String glFunctionName) {
    // Check GL 1st (cached)
    if(null!=glProcAddressTable) { // null if this context wasn't not created
        try {
            if( glProcAddressTable.isFunctionAvailable( glFunctionName ) ) {
                return true;
            }
        } catch (Exception e) {}
    }

    // Check platform extensions 2nd (cached) - context had to be enabled once
    final ProcAddressTable pTable = getPlatformExtProcAddressTable();
    if(null!=pTable) {
        try {
            if( pTable.isFunctionAvailable( glFunctionName ) ) {
                return true;
            }
        } catch (Exception e) {}
    }

    // dynamic function lookup at last incl name aliasing (not cached)
    final DynamicLookupHelper dynLookup = getDrawableImpl().getGLDynamicLookupHelper();
    final String tmpBase = GLNameResolver.normalizeVEN(GLNameResolver.normalizeARB(glFunctionName, true), true);
    boolean res = false;
    int  variants = GLNameResolver.getFuncNamePermutationNumber(tmpBase);
    for(int i = 0; !res && i < variants; i++) {
        final String tmp = GLNameResolver.getFuncNamePermutation(tmpBase, i);
        try {
            res = dynLookup.isFunctionAvailable(tmp);
        } catch (Exception e) { }
    }
    return res;
  }

  @Override
  public boolean isExtensionAvailable(String glExtensionName) {
      if(null!=extensionAvailability) {
        return extensionAvailability.isExtensionAvailable(mapToRealGLExtensionName(glExtensionName));
      }
      return false;
  }

  @Override
  public final int getPlatformExtensionCount() {
      return null != extensionAvailability ? extensionAvailability.getPlatformExtensionCount() : 0;
  }

  @Override
  public final String getPlatformExtensionsString() {
      if(null!=extensionAvailability) {
        return extensionAvailability.getPlatformExtensionsString();
      }
      return null;
  }

  @Override
  public final int getGLExtensionCount() {
      return null != extensionAvailability ? extensionAvailability.getGLExtensionCount() : 0;
  }

  @Override
  public final String getGLExtensionsString() {
      if(null!=extensionAvailability) {
        return extensionAvailability.getGLExtensionsString();
      }
      return null;
  }

  public final boolean isExtensionCacheInitialized() {
      if(null!=extensionAvailability) {
        return extensionAvailability.isInitialized();
      }
      return false;
  }

  protected static String getContextFQN(AbstractGraphicsDevice device, int major, int minor, int ctxProfileBits) {
      // remove non-key values
      ctxProfileBits &= ~( GLContext.CTX_IMPL_ES2_COMPAT | GLContext.CTX_IMPL_FBO ) ;

      return device.getUniqueID() + "-" + toHexString(composeBits(major, minor, ctxProfileBits));
  }

  protected final String getContextFQN() {
      return contextFQN;
  }

  /** Indicates which floating-point pbuffer implementation is in
      use. Returns one of GLPbuffer.APPLE_FLOAT, GLPbuffer.ATI_FLOAT,
      or GLPbuffer.NV_FLOAT. */
  public int getFloatingPointMode() throws GLException {
    throw new GLException("Not supported on non-pbuffer contexts");
  }

  @Override
  public int getDefaultPixelDataType() {
      evalPixelDataType();
      return pixelDataType;
  }

  @Override
  public int getDefaultPixelDataFormat() {
      evalPixelDataType();
      return pixelDataFormat;
  }
  
  private final void evalPixelDataType() {
    if(!pixelDataEvaluated) {
        synchronized(this) {
            if(!pixelDataEvaluated) {
                /* if(isGL2GL3() && 3 == components) {
                    pixelDataInternalFormat=GL.GL_RGB;
                    pixelDataFormat=GL.GL_RGB;
                    pixelDataType = GL.GL_UNSIGNED_BYTE;            
                } else */ if(isGLES2Compatible() || isExtensionAvailable(GLExtensions.OES_read_format)) {
                    final int[] glImplColorReadVals = new int[] { 0, 0 };
                    gl.glGetIntegerv(GL.GL_IMPLEMENTATION_COLOR_READ_FORMAT, glImplColorReadVals, 0);
                    gl.glGetIntegerv(GL.GL_IMPLEMENTATION_COLOR_READ_TYPE, glImplColorReadVals, 1);            
                    // pixelDataInternalFormat = (4 == components) ? GL.GL_RGBA : GL.GL_RGB;
                    pixelDataFormat = glImplColorReadVals[0];
                    pixelDataType = glImplColorReadVals[1];
                } else {
                    // RGBA read is safe for all GL profiles 
                    // pixelDataInternalFormat = (4 == components) ? GL.GL_RGBA : GL.GL_RGB;
                    pixelDataFormat=GL.GL_RGBA;
                    pixelDataType = GL.GL_UNSIGNED_BYTE;
                }            
                // TODO: Consider:
                // return gl.isGL2GL3()?GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:GL.GL_UNSIGNED_SHORT_5_5_5_1;
                pixelDataEvaluated = true;
            }
        }
    }
  }

  //----------------------------------------------------------------------
  // Helpers for buffer object optimizations

  public final void setBufferSizeTracker(GLBufferSizeTracker bufferSizeTracker) {
    this.bufferSizeTracker = bufferSizeTracker;
  }

  public final GLBufferSizeTracker getBufferSizeTracker() {
    return bufferSizeTracker;
  }

  public final GLBufferStateTracker getBufferStateTracker() {
    return bufferStateTracker;
  }

  public final GLStateTracker getGLStateTracker() {
    return glStateTracker;
  }
  
  public final boolean isDefaultVAO(int vao) {
      return defaultVAO == vao;
  }

  //---------------------------------------------------------------------------
  // Helpers for context optimization where the last context is left
  // current on the OpenGL worker thread
  //

  /** 
   * Returns true if the given thread is owner, otherwise false.
   * <p>
   * Method exists merely for code validation of {@link #isCurrent()}.
   * </p> 
   */
  public final boolean isOwner(Thread thread) {
      return lock.isOwner(thread);
  }
  
  /** 
   * Returns true if there are other threads waiting for this GLContext to {@link #makeCurrent()}, otherwise false.
   * <p>
   * Since method does not perform any synchronization, accurate result are returned if lock is hold - only.
   * </p> 
   */
  public final boolean hasWaiters() {
    return lock.getQueueLength()>0;
  }
  
  /** 
   * Returns the number of hold locks. See {@link RecursiveLock#getHoldCount()} for semantics.
   * <p>
   * Since method does not perform any synchronization, accurate result are returned if lock is hold - only.
   * </p> 
   */
  public final int getLockCount() {
      return lock.getHoldCount();
  }
  
  //---------------------------------------------------------------------------
  // Special FBO hook
  //
  
  /**
   * Tracks {@link GL#GL_FRAMEBUFFER}, {@link GL2GL3#GL_DRAW_FRAMEBUFFER} and {@link GL2GL3#GL_READ_FRAMEBUFFER}
   * to be returned via {@link #getBoundFramebuffer(int)}.
   * 
   * <p>Invoked by {@link GL#glBindFramebuffer(int, int)}. </p>
   * 
   * <p>Assumes valid <code>framebufferName</code> range of [0..{@link Integer#MAX_VALUE}]</p> 
   * 
   * <p>Does not throw an exception if <code>target</code> is unknown or <code>framebufferName</code> invalid.</p>
   */
  public final void setBoundFramebuffer(int target, int framebufferName) {
      if(0 > framebufferName) {
          return; // ignore invalid name
      }
      switch(target) {
          case GL.GL_FRAMEBUFFER:
              boundFBOTarget[0] = framebufferName; // draw
              boundFBOTarget[1] = framebufferName; // read
              break;
          case GL2GL3.GL_DRAW_FRAMEBUFFER:
              boundFBOTarget[0] = framebufferName; // draw
              break;
          case GL2GL3.GL_READ_FRAMEBUFFER:
              boundFBOTarget[1] = framebufferName; // read
              break;
          default: // ignore untracked target
      }
  }
  @Override
  public final int getBoundFramebuffer(int target) {
      switch(target) {
          case GL.GL_FRAMEBUFFER:
          case GL2GL3.GL_DRAW_FRAMEBUFFER:
              return boundFBOTarget[0]; // draw
          case GL2GL3.GL_READ_FRAMEBUFFER:
              return boundFBOTarget[1]; // read
          default:
              throw new InternalError("Invalid FBO target name: "+toHexString(target));
      }
  }
  
  @Override
  public final int getDefaultDrawFramebuffer() { return drawable.getDefaultDrawFramebuffer(); }  
  @Override
  public final int getDefaultReadFramebuffer() { return drawable.getDefaultReadFramebuffer(); }  
  @Override
  public final int getDefaultReadBuffer() { return drawable.getDefaultReadBuffer(gl); }
    
  //---------------------------------------------------------------------------
  // GL_ARB_debug_output, GL_AMD_debug_output helpers
  //

  @Override
  public final String getGLDebugMessageExtension() {
      return glDebugHandler.getExtension();
  }

  @Override
  public final boolean isGLDebugMessageEnabled() {
      return glDebugHandler.isEnabled();
  }

  @Override
  public final int getContextCreationFlags() {
      return additionalCtxCreationFlags;
  }

  @Override
  public final void setContextCreationFlags(int flags) {
      if(!isCreated()) {
          additionalCtxCreationFlags = flags & GLContext.CTX_OPTION_DEBUG;
      }
  }

  @Override
  public final boolean isGLDebugSynchronous() { return glDebugHandler.isSynchronous(); }

  @Override
  public final void setGLDebugSynchronous(boolean synchronous) {
      glDebugHandler.setSynchronous(synchronous);
  }

  @Override
  public final void enableGLDebugMessage(boolean enable) throws GLException {
      if(!isCreated()) {
          if(enable) {
              additionalCtxCreationFlags |=  GLContext.CTX_OPTION_DEBUG;
          } else {
              additionalCtxCreationFlags &= ~GLContext.CTX_OPTION_DEBUG;
          }
      } else if(0 != (additionalCtxCreationFlags & GLContext.CTX_OPTION_DEBUG) &&
                null != getGLDebugMessageExtension()) {
          glDebugHandler.enable(enable);
      }
  }

  @Override
  public final void addGLDebugListener(GLDebugListener listener) {
      glDebugHandler.addListener(listener);
  }

  @Override
  public final void removeGLDebugListener(GLDebugListener listener) {
      glDebugHandler.removeListener(listener);
  }

  @Override
  public final void glDebugMessageControl(int source, int type, int severity, int count, IntBuffer ids, boolean enabled) {
      if(glDebugHandler.isExtensionARB()) {
          gl.getGL2GL3().glDebugMessageControlARB(source, type, severity, count, ids, enabled);
      } else if(glDebugHandler.isExtensionAMD()) {
          gl.getGL2GL3().glDebugMessageEnableAMD(GLDebugMessage.translateARB2AMDCategory(source, type), severity, count, ids, enabled);
      }
  }

  @Override
  public final void glDebugMessageControl(int source, int type, int severity, int count, int[] ids, int ids_offset, boolean enabled) {
      if(glDebugHandler.isExtensionARB()) {
          gl.getGL2GL3().glDebugMessageControlARB(source, type, severity, count, ids, ids_offset, enabled);
      } else if(glDebugHandler.isExtensionAMD()) {
          gl.getGL2GL3().glDebugMessageEnableAMD(GLDebugMessage.translateARB2AMDCategory(source, type), severity, count, ids, ids_offset, enabled);
      }
  }

  @Override
  public final void glDebugMessageInsert(int source, int type, int id, int severity, String buf) {
      final int len = (null != buf) ? buf.length() : 0;
      if(glDebugHandler.isExtensionARB()) {
          gl.getGL2GL3().glDebugMessageInsertARB(source, type, id, severity, len, buf);
      } else if(glDebugHandler.isExtensionAMD()) {
          gl.getGL2GL3().glDebugMessageInsertAMD(GLDebugMessage.translateARB2AMDCategory(source, type), severity, id, len, buf);
      }
  }

  /** Internal bootstraping glGetString(GL_RENDERER) */
  private static native String glGetStringInt(int name, long procAddress);
  
  /** Internal bootstraping glGetIntegerv(..) for version */
  private static native void glGetIntegervInt(int pname, int[] params, int params_offset, long procAddress);
}
