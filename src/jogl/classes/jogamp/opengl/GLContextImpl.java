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

import java.lang.reflect.Method;
import java.nio.IntBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.os.DynamicLookupHelper;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.common.util.VersionNumberString;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLNameResolver;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.GLRendererQuirks;
import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDebugListener;
import com.jogamp.opengl.GLDebugMessage;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLProfile;

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
  private boolean glGetPtrInit = false;
  private long glGetStringPtr = 0;
  private long glGetIntegervPtr = 0;

  // Tracks lifecycle of buffer objects to avoid
  // repeated glGet calls upon glMapBuffer operations
  private final GLBufferObjectTracker bufferObjectTracker;
  private final GLBufferStateTracker bufferStateTracker;
  private final GLStateTracker glStateTracker = new GLStateTracker();
  private GLDebugMessageHandler glDebugHandler = null;
  private final int[] boundFBOTarget = new int[] { 0, 0 }; // { draw, read }
  private int defaultVAO = 0;

  /**
   * <ul>
   *   <li>[GLAutoDrawable.upstreamLock].lock()</li>
   *   <li>drawable.surface.lock()</li>
   *   <li>contextLock.lock()</li>
   * </ul>
   */
  protected GLDrawableImpl drawable;
  protected GLDrawableImpl drawableRead;

  /**
   * If GL >= 3.0 (ES or desktop) and not having {@link GLRendererQuirks#NoSurfacelessCtx},
   * being evaluated if not surface-handle is null and not yet set at makeCurrent(..).
   */
  private boolean isSurfaceless = false;

  private boolean pixelDataEvaluated;
  private int /* pixelDataInternalFormat, */ pixelDataFormat, pixelDataType;

  private int currentSwapInterval;

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

  public GLContextImpl(final GLDrawableImpl drawable, final GLContext shareWith) {
    super();

    if( null == drawable ) {
        throw new IllegalArgumentException("Null drawable");
    }
    bufferStateTracker = new GLBufferStateTracker();
    if ( null != shareWith ) {
      GLContextShareSet.registerSharing(this, shareWith);
      bufferObjectTracker = ((GLContextImpl)shareWith).getBufferObjectTracker();
      if( null == bufferObjectTracker ) {
          throw new InternalError("shared-master context hash null GLBufferObjectTracker: "+toHexString(shareWith.hashCode()));
      }
    } else {
      bufferObjectTracker = new GLBufferObjectTracker();
    }

    this.drawable = drawable;
    this.drawableRead = drawable;

    this.glDebugHandler = new GLDebugMessageHandler(this);
  }

  private final void clearStates() {
      if( !GLContextShareSet.hasCreatedSharedLeft(this) ) {
        bufferObjectTracker.clear();
      }
      bufferStateTracker.clear();
      glStateTracker.setEnabled(false);
      glStateTracker.clearStates();
  }

  @Override
  protected void resetStates(final boolean isInit) {
      if( !isInit ) {
          clearStates();
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
      glGetPtrInit = false;
      glGetStringPtr = 0;
      glGetIntegervPtr = 0;

      if ( !isInit && null != boundFBOTarget ) { // <init>: boundFBOTarget is not written yet
          boundFBOTarget[0] = 0; // draw
          boundFBOTarget[1] = 0; // read
      }

      isSurfaceless = false;
      pixelDataEvaluated = false;
      currentSwapInterval = 0;

      super.resetStates(isInit);
  }

  @Override
  public final GLDrawable setGLReadDrawable(final GLDrawable read) {
      // Validate constraints first!
      if(!isGLReadDrawableAvailable()) {
          throw new GLException("Setting read drawable feature not available");
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
      if(lockHeld) {
          release(false);
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
  public final GLDrawable setGLDrawable(final GLDrawable readWrite, final boolean setWriteOnly) {
      // Validate constraints first!
      final Thread currentThread = Thread.currentThread();
      if( lock.isLockedByOtherThread() ) {
          throw new GLException("GLContext current by other thread "+lock.getOwner().getName()+", operation not allowed on this thread "+currentThread.getName());
      }
      final boolean lockHeld = lock.isOwner(currentThread);
      if( lockHeld && lock.getHoldCount() > 1 ) {
          // would need to makeCurrent * holdCount
          throw new GLException("GLContext is recursively locked - unsupported for setGLDrawable(..)");
      }
      if( drawable == readWrite && ( setWriteOnly || drawableRead == readWrite ) ) {
          return drawable; // no change.
      }
      final GLDrawableImpl oldDrawableWrite = drawable;
      final GLDrawableImpl oldDrawableRead = drawableRead;
      if( isCreated() && null != oldDrawableWrite && oldDrawableWrite.isRealized() ) {
          if(!lockHeld) {
              makeCurrent();
          }
          // sync GL ctx w/ drawable's framebuffer before de-association
          gl.glFinish();
          associateDrawable(false);
          if(!lockHeld) {
              release(false);
          }
      }
      if(lockHeld) {
          release(false);
      }
      if( !setWriteOnly || drawableRead == drawable ) { // if !setWriteOnly || !explicitReadDrawable
          drawableRead = (GLDrawableImpl) readWrite;
      }
      drawableRetargeted |= null != drawable && readWrite != drawable;
      drawable = (GLDrawableImpl) readWrite ;
      if( isCreated() && null != drawable && drawable.isRealized() ) {
          int res = CONTEXT_NOT_CURRENT;
          Throwable gle = null;
          try {
              res = makeCurrent(true); // implicit: associateDrawable(true)
          } catch ( final Throwable t ) {
              gle = t;
          } finally {
              if( CONTEXT_NOT_CURRENT == res ) {
                  // Failure, recover and bail out w/ GLException
                  drawableRead = oldDrawableRead;
                  drawable     = oldDrawableWrite;
                  if( drawable.isRealized() ) {
                      makeCurrent(true); // implicit: associateDrawable(true)
                  }
                  if( !lockHeld ) {
                      release(false);
                  }
                  final String msg = "Error: makeCurrent() failed with new drawable "+readWrite;
                  if( null != gle ) {
                      throw new GLException(msg, gle);
                  } else {
                      throw new GLException(msg);
                  }
              }
          }
          if( !lockHeld ) {
              release(false);
          }
      }
      return oldDrawableWrite;
  }

  @Override
  public final GLDrawable getGLDrawable() {
    return drawable;
  }

  public final GLDrawableImpl getDrawableImpl() {
    return drawable;
  }

  @Override
  public final GL getRootGL() {
      GL _gl = gl;
      GL _parent = _gl.getDownstreamGL();
      while ( null != _parent ) {
          _gl = _parent;
          _parent = _gl.getDownstreamGL();
      }
      return _gl;
  }

  @Override
  public final GL getGL() {
    return gl;
  }

  @Override
  public GL setGL(final GL gl) {
    if( DEBUG ) {
        final String sgl1 = (null!=this.gl)?this.gl.getClass().getSimpleName()+", "+this.gl.toString():"<null>";
        final String sgl2 = (null!=gl)?gl.getClass().getSimpleName()+", "+gl.toString():"<null>";
        System.err.println("Info: setGL (OpenGL "+getGLVersion()+"): "+getThreadName()+", "+sgl1+" -> "+sgl2);
        ExceptionUtils.dumpStack(System.err);
    }
    this.gl = gl;
    return gl;
  }

  @Override
  public final int getDefaultVAO() {
      return defaultVAO;
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
  private String getTraceSwitchMsg() {
      final long drawH = null != drawable ? drawable.getHandle() : 0;
      return "obj " + toHexString(hashCode()) + ", ctx "+toHexString(contextHandle)+", isShared "+GLContextShareSet.isShared(this)+", surf "+(null!=drawable)+" "+toHexString(drawH)+", "+lock;
  }
  private void release(final boolean inDestruction) throws GLException {
      if( TRACE_SWITCH ) {
          System.err.println(getThreadName() +": GLContext.ContextSwitch[release.0, inDestruction: "+inDestruction+"]: "+getTraceSwitchMsg());
      }
      if ( !lock.isOwner(Thread.currentThread()) ) {
          final String msg = getThreadName() +": Context not current on thread, inDestruction: "+inDestruction+", "+getTraceSwitchMsg();
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
                  } catch (final Throwable t) {
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
          lock.unlock();
          drawable.unlockSurface();
          if( DEBUG_TRACE_SWITCH ) {
              final String msg = getThreadName() +": GLContext.ContextSwitch[release.X]: "+(actualRelease?"switch":"keep  ")+" - "+getTraceSwitchMsg();
              lastCtxReleaseStack = new Throwable(msg);
              if( TRACE_SWITCH ) {
                  System.err.println(msg);
                  // ExceptionUtils.dumpStack(System.err, 0, 10);
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
          System.err.println(getThreadName() + ": GLContextImpl.destroy.0: "+getTraceSwitchMsg());
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
              if ( DEBUG_TRACE_SWITCH ) {
                  if ( lock.getHoldCount() > 2 ) {
                      System.err.println(getThreadName() + ": GLContextImpl.destroy: Lock was hold more than once - makeCurrent/release imbalance: "+getTraceSwitchMsg());
                      ExceptionUtils.dumpStack(System.err);
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
                  } catch (final Throwable t) {
                      associateDrawableException = t;
                  }
                  if ( 0 != defaultVAO ) {
                      final int[] tmp = new int[] { defaultVAO };
                      final GL2ES3 gl2es3 = gl.getRootGL().getGL2ES3();
                      gl2es3.glBindVertexArray(0);
                      gl2es3.glDeleteVertexArrays(1, tmp, 0);
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
                  if( GLContextShareSet.contextDestroyed(this) && !GLContextShareSet.hasCreatedSharedLeft(this) ) {
                      GLContextShareSet.unregisterSharing(this);
                  }
                  resetStates(false);
              } finally {
                  lock.unlock();
                  if ( DEBUG_TRACE_SWITCH ) {
                      System.err.println(getThreadName() + ": GLContextImpl.destroy.X: "+getTraceSwitchMsg());
                  }
              }
          } finally {
              drawable.unlockSurface();
          }
          if( null != associateDrawableException ) {
              throw new GLException("Exception @ destroy's associateDrawable(false)", associateDrawableException);
          }
      } else {
          resetStates(false);
      }
  }
  protected abstract void destroyImpl() throws GLException;

  @Override
  public final void copy(final GLContext source, final int mask) throws GLException {
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

  protected final boolean isSurfaceless() { return isSurfaceless; }

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
    final boolean hasDrawable = null != drawable;
    if( TRACE_SWITCH ) {
        System.err.println(getThreadName() +": GLContext.ContextSwitch[makeCurrent.0]: "+getTraceSwitchMsg());
    }
    if( !hasDrawable ) {
        if( DEBUG_TRACE_SWITCH ) {
            System.err.println(getThreadName() +": GLContext.ContextSwitch[makeCurrent.X0]: NULL Drawable - CONTEXT_NOT_CURRENT - "+getTraceSwitchMsg());
        }
        return CONTEXT_NOT_CURRENT;
    }

    // Note: the surface is locked within [makeCurrent .. swap .. release]
    final int lockRes = drawable.lockSurface();
    if (NativeSurface.LOCK_SURFACE_NOT_READY >= lockRes) {
        if( DEBUG_TRACE_SWITCH ) {
            System.err.println(getThreadName() +": GLContext.ContextSwitch[makeCurrent.X1]: Surface Not Ready - CONTEXT_NOT_CURRENT - "+getTraceSwitchMsg());
        }
        return CONTEXT_NOT_CURRENT;
    }

    boolean unlockResources = true; // Must be cleared if successful, otherwise finally block will release context and/or surface!
    int res = CONTEXT_NOT_CURRENT;
    try {
        if ( drawable.isRealized() ) {
            lock.lock();
            try {
                if ( 0 == drawable.getHandle() && !isSurfaceless ) {
                    if( DEBUG ) {
                        System.err.println(getThreadName() +": GLContext.makeCurrent: Surfaceless evaluate");
                    }
                    if( hasRendererQuirk(GLRendererQuirks.NoSurfacelessCtx) ) {
                        throw new GLException(String.format("Surfaceless not supported due to quirk %s: %s",
                                GLRendererQuirks.toString(GLRendererQuirks.NoSurfacelessCtx), toString()));
                    }
                    // Allow probing if ProxySurface && OPT_UPSTREAM_SURFACELESS
                    final NativeSurface surface = drawable.getNativeSurface();
                    if( !(surface instanceof ProxySurface) ||
                        !((ProxySurface)surface).containsUpstreamOptionBits( ProxySurface.OPT_UPSTREAM_SURFACELESS ) ) {
                        throw new GLException(String.format("non-surfaceless drawable has zero-handle: %s", drawable.toString()));
                    }
                }
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
                            System.err.println(getThreadName() +": GLContext.ContextSwitch[makeCurrent.X2]: KEEP - CONTEXT_CURRENT - "+getTraceSwitchMsg());
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
            } catch (final RuntimeException e) {
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
    } catch (final RuntimeException e) {
      unlockResources = true;
      throw e;
    } finally {
      if (unlockResources) {
        drawable.unlockSurface();
      }
    }

    if ( CONTEXT_NOT_CURRENT != res ) { // still locked!
      if( 0 == drawable.getHandle() && !isSurfaceless ) {
          if( hasRendererQuirk(GLRendererQuirks.NoSurfacelessCtx) ) {
              throw new GLException(String.format("Surfaceless not supported due to quirk %s: %s",
                      GLRendererQuirks.toString(GLRendererQuirks.NoSurfacelessCtx), toString()));
          }
          if( DEBUG ) {
              System.err.println(getThreadName() +": GLContext.makeCurrent: Surfaceless OK - validated");
          }
          isSurfaceless = true;
      }
      setCurrent(this);
      if( CONTEXT_CURRENT_NEW == res ) {
        // check if the drawable's and the GL's GLProfile are equal
        // throws an GLException if not
        // FIXME: drawable.getGLProfile().verifyEquality(gl.getGLProfile());

        glDebugHandler.init( isGLDebugEnabled() );

        if(DEBUG_GL) {
            setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, gl, null) );
            if(glDebugHandler.isEnabled()) {
                glDebugHandler.addListener(new GLDebugMessageHandler.StdErrGLDebugListener(true));
            }
        }
        if(TRACE_GL) {
            setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err } ) );
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
        System.err.println(getThreadName() +": GLContext.ContextSwitch[makeCurrent.X3]: SWITCH - "+makeCurrentResultToString(res)+" - stateTracker.on "+glStateTracker.isEnabled()+" - "+getTraceSwitchMsg());
    }
    return res;
  }

  private final GLContextImpl getOtherSharedMaster() {
      final GLContextImpl sharedMaster = (GLContextImpl) GLContextShareSet.getSharedMaster(this);
      return this != sharedMaster ? sharedMaster : null;
  }
  private final int makeCurrentWithinLock(final int surfaceLockRes) throws GLException {
      if (!isCreated()) {
        if( 0 >= drawable.getSurfaceWidth() || 0 >= drawable.getSurfaceHeight() ) {
            if ( DEBUG_TRACE_SWITCH ) {
                System.err.println(getThreadName() + ": Create GL context REJECTED (zero surface size) for " + getClass().getName()+" - "+getTraceSwitchMsg());
                System.err.println(drawable.toString());
            }
            return CONTEXT_NOT_CURRENT;
        }
        if(DEBUG_GL) {
            // only impacts w/ createContextARB(..)
            additionalCtxCreationFlags |= GLContext.CTX_OPTION_DEBUG ;
        }

        final boolean created;
        final GLContextImpl sharedMaster = getOtherSharedMaster();
        if ( null != sharedMaster ) {
            if ( NativeSurface.LOCK_SURFACE_NOT_READY >= sharedMaster.drawable.lockSurface() ) {
                throw new GLException("GLContextShareSet could not lock sharedMaster surface: "+sharedMaster.drawable);
            }
        }
        try {
            if ( null != sharedMaster ) {
                final long sharedMasterHandle = sharedMaster.getHandle();
                if ( 0 == sharedMasterHandle ) {
                    throw new GLException("GLContextShareSet returned an invalid sharedMaster context: "+sharedMaster);
                }
                created = createImpl(sharedMasterHandle); // may throws exception if fails
            } else {
                created = createImpl(0); // may throws exception if fails
            }
            if( created && hasNoDefaultVAO() ) {
                final int[] tmp = new int[1];
                final GL rootGL = gl.getRootGL();
                final GL2ES3 gl2es3 = rootGL.getGL2ES3();
                gl2es3.glGenVertexArrays(1, tmp, 0);
                defaultVAO = tmp[0];
                gl2es3.glBindVertexArray(defaultVAO);
            }
        } finally {
            if ( null != sharedMaster ) {
                sharedMaster.drawable.unlockSurface();
            }
        }
        if ( DEBUG_TRACE_SWITCH ) {
            System.err.println(getThreadName() + ": Create GL context "+(created?"OK":"FAILED")+": For " + getClass().getName()+" - "+getGLVersion()+" - "+getTraceSwitchMsg());
            // ExceptionUtils.dumpStack(System.err, 0, 10);
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
                    if( ctxVersion.compareTo(Version3_0) <= 0 ) {
                        reqMajor = 2;
                    } else {
                        reqMajor = ctxVersion.getMajor();
                    }
                    final boolean isCompat;
                    if( 0 != ( ctxOptions & GLContext.CTX_PROFILE_CORE) ) {
                        reqProfile = GLContext.CTX_PROFILE_CORE;
                        isCompat = false;
                    } else {
                        reqProfile = GLContext.CTX_PROFILE_COMPAT;
                        isCompat = true;
                    }
                    final MappedGLVersion me = mapAvailableGLVersion(device, reqMajor, reqProfile, ctxVersion, ctxOptions, glRendererQuirks);
                    // Perform all required profile mappings
                    if( isCompat ) {
                        // COMPAT via non ARB
                        mapAvailableGLVersion(device, reqMajor, GLContext.CTX_PROFILE_CORE, ctxVersion, ctxOptions, glRendererQuirks);
                        if( reqMajor >= 4 ) {
                            mapAvailableGLVersion(device, 3, reqProfile, ctxVersion, ctxOptions, glRendererQuirks);
                            mapAvailableGLVersion(device, 3, GLContext.CTX_PROFILE_CORE, ctxVersion, ctxOptions, glRendererQuirks);
                        }
                        if( reqMajor >= 3 ) {
                            mapAvailableGLVersion(device, 2, reqProfile, ctxVersion, ctxOptions, glRendererQuirks);
                        }
                    } else {
                        // CORE via non ARB, unlikely, however ..
                        if( reqMajor >= 4 ) {
                            mapAvailableGLVersion(device, 3, reqProfile, ctxVersion, ctxOptions, glRendererQuirks);
                        }
                    }
                    GLContext.setAvailableGLVersionsSet(device, true);

                    if (DEBUG) {
                      System.err.println(getThreadName() + ": createContextOLD-MapGLVersions HAVE: " + me);
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
  protected void associateDrawable(final boolean bound) {
      drawable.associateContext(this, bound);
  }

  /**
   * Calls {@link GLDrawableImpl#contextMadeCurrent(GLContext, boolean)}
   */
  protected void contextMadeCurrent(final boolean current) {
      drawable.contextMadeCurrent(this, current);
  }

  /**
   * Platform dependent entry point for context creation.
   * <p>
   * This method is called from {@link #makeCurrentWithinLock()} .. {@link #makeCurrent()} .
   * </p>
   * <p>
   * The implementation shall verify this context with a
   * <code>MakeContextCurrent</code> call.
   * </p>
   * <p>
   * The implementation <b>must</b> leave the context current.
   * </p>
   * <p>
   * Non fatal context creation failure via return {@code false}
   * is currently implemented for: {@code MacOSXCGLContext}.
   * </p>
   * @param sharedWithHandle the shared context handle or 0
   * @return {@code true} if successful. Method returns {@code false} if the context creation failed non fatally,
   * hence it may be created at a later time. Otherwise method throws {@link GLException}.
   * @throws GLException if method fatally fails creating the context and no attempt shall be made at a later time.
   */
  protected abstract boolean createImpl(long sharedWithHandle) throws GLException ;

  /**
   * Platform dependent but harmonized implementation of the <code>ARB_create_context</code>
   * mechanism to create a context.<br>
   *
   * This method is called from {@link #createContextARB}, {@link #createImpl(long)} .. {@link #makeCurrent()} .<br>
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

  protected final boolean isCreateContextARBAvail(final AbstractGraphicsDevice device) {
    return !GLProfile.disableOpenGLARBContext &&
           !GLRendererQuirks.existStickyDeviceQuirk(device, GLRendererQuirks.NoARBCreateContext);
  }
  protected final String getCreateContextARBAvailStr(final AbstractGraphicsDevice device) {
    final boolean noARBCreateContext = GLRendererQuirks.existStickyDeviceQuirk(device, GLRendererQuirks.NoARBCreateContext);
    return "disabled "+GLProfile.disableOpenGLARBContext+", quirk "+noARBCreateContext;
  }

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
    final GLCapabilitiesImmutable glCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    final GLProfile glp = glCaps.getGLProfile();

    if (DEBUG) {
      System.err.println(getThreadName() + ": createContextARB-MapGLVersions is SET ("+device.getConnection()+"): "+
               GLContext.getAvailableGLVersionsSet(device));
    }
    if ( !GLContext.getAvailableGLVersionsSet(device) ) {
        if( !mapGLVersions(device) ) {
            // none of the ARB context creation calls was successful, bail out
            return 0;
        }
    }

    final int[] reqMajorCTP = new int[] { 0, 0 };
    GLContext.getRequestMajorAndCompat(glp, reqMajorCTP);

    if(DEBUG) {
        System.err.println(getThreadName() + ": createContextARB-MapGLVersions Requested "+glp+" -> "+GLContext.getGLVersion(reqMajorCTP[0], 0, reqMajorCTP[1], null));
    }
    final int _major[] = { 0 };
    final int _minor[] = { 0 };
    final int _ctp[] = { 0 };
    long _ctx = 0;
    if( GLContext.getAvailableGLVersion(device, reqMajorCTP[0], reqMajorCTP[1],
                                        _major, _minor, _ctp)) {
        _ctp[0] |= additionalCtxCreationFlags;
        if(DEBUG) {
            System.err.println(getThreadName() + ": createContextARB-MapGLVersions Mapped "+GLContext.getGLVersion(_major[0], _minor[0], _ctp[0], null));
        }
        _ctx = createContextARBImpl(share, direct, _ctp[0], _major[0], _minor[0]);
        if(0!=_ctx) {
            if( !setGLFunctionAvailability(true, _major[0], _minor[0], _ctp[0], false /* strictMatch */, false /* withinGLVersionsMapping */) ) {
                throw new InternalError("setGLFunctionAvailability !strictMatch failed");
            }
        }
    }
    return _ctx;
  }

  //----------------------------------------------------------------------
  //

  public static class MappedGLVersion {
      public final AbstractGraphicsDevice device;
      public final int reqMajorVersion;
      public final int reqProfile;
      public final VersionNumber ctxVersion;
      public final int ctxOptions;
      public final GLRendererQuirks quirks;
      public final VersionNumber preCtxVersion;
      public final int preCtxOptions;
      public MappedGLVersion(final AbstractGraphicsDevice device, final int reqMajorVersion, final int reqProfile,
                             final VersionNumber ctxVersion, final int ctxOptions, final GLRendererQuirks quirks,
                             final VersionNumber preCtxVersion, final int preCtxOptions) {
          this.device = device;
          this.reqMajorVersion = reqMajorVersion;
          this.reqProfile = reqProfile;
          this.ctxVersion = ctxVersion;
          this.ctxOptions = ctxOptions;
          this.quirks = quirks;
          this.preCtxVersion = preCtxVersion;
          this.preCtxOptions = preCtxOptions;
      }
      public final String toString() {
          return toString(new StringBuilder(), -1, -1, -1, -1).toString();
      }
      public final StringBuilder toString(final StringBuilder sb, final int minMajor, final int minMinor, final int maxMajor, final int maxMinor) {
          sb.append(device.toString()).append(" ").append(reqMajorVersion).append(" (");
          GLContext.getGLProfile(sb, reqProfile).append(")");
          if( minMajor >=0 && minMinor >=0 && maxMajor >= 0 && maxMinor >= 0) {
              sb.append("[").append(minMajor).append(".").append(minMinor).append(" .. ").append(maxMajor).append(".").append(maxMinor).append("]");
          }
          sb.append(": [");
          if( null != preCtxVersion ) {
              GLContext.getGLVersion(sb, preCtxVersion, preCtxOptions, null);
          } else {
              sb.append("None");
          }
          sb.append("] -> [");
          GLContext.getGLVersion(sb, ctxVersion, ctxOptions, null).append("]");
          return sb;
      }
  }
  public static interface MappedGLVersionListener {
      void glVersionMapped(final MappedGLVersion e);
  }
  private static MappedGLVersionListener mapGLVersionListener = null;
  protected static synchronized void setMappedGLVersionListener(final MappedGLVersionListener mvl) {
      mapGLVersionListener = mvl;
  }

  /**
   * Called by {@link jogamp.opengl.GLContextImpl#createContextARBMapVersionsAvailable(int,int)} not intended to be used by
   * implementations. However, if {@link jogamp.opengl.GLContextImpl#createContextARB(long, boolean)} is not being used within
   * {@link com.jogamp.opengl.GLDrawableFactory#getOrCreateSharedContext(com.jogamp.nativewindow.AbstractGraphicsDevice)},
   * GLProfile has to map the available versions.
   *
   * @param reqMajor Key Value either 1, 2, 3 or 4
   * @param profile Key Value either {@link #CTX_PROFILE_COMPAT}, {@link #CTX_PROFILE_CORE} or {@link #CTX_PROFILE_ES}
   * @param resVersion the resulting version number
   * @param resCtp the resulting context options
   * @return the old mapped value
   *
   * @see #createContextARBMapVersionsAvailable
   */
  protected static MappedGLVersion mapAvailableGLVersion(final AbstractGraphicsDevice device,
                                                           final int reqMajor, final int profile,
                                                           final VersionNumber resVersion, final int resCtp, final GLRendererQuirks resQuirks)
  {
      @SuppressWarnings("deprecation")
      final Integer preVal = mapAvailableGLVersion(device, reqMajor, profile, resVersion.getMajor(), resVersion.getMinor(), resCtp);
      final int[] preCtp = { 0 };
      final VersionNumber preVersion = null != preVal ? decomposeBits(preVal.intValue(), preCtp) : null;
      final MappedGLVersion res = new MappedGLVersion(device, reqMajor, profile, resVersion, resCtp, resQuirks, preVersion, preCtp[0]);
      if( null != mapGLVersionListener ) {
          mapGLVersionListener.glVersionMapped(res);
      }
      return res;
  }

  protected static void remapAvailableGLVersions(final AbstractGraphicsDevice fromDevice, final AbstractGraphicsDevice toDevice) {
    if( fromDevice == toDevice || fromDevice.getUniqueID() == toDevice.getUniqueID() ) {
        return; // NOP
    }
    synchronized(deviceVersionAvailable) {
        if(DEBUG) {
            System.err.println(getThreadName() + ": createContextARB-MapGLVersions REMAP "+fromDevice+" -> "+toDevice);
        }
        final IdentityHashMap<String, Integer> newDeviceVersionAvailable = new IdentityHashMap<String, Integer>();
        final Set<String> keys = deviceVersionAvailable.keySet();
        for(final Iterator<String> keyI = keys.iterator(); keyI.hasNext(); ) {
            final String origKey = keyI.next();
            final Integer valI = deviceVersionAvailable.get(origKey);
            if( null != valI ) {
                if(DEBUG) {
                    final int[] ctp = { 0 };
                    final VersionNumber version = decomposeBits(valI.intValue(), ctp);
                    System.err.println(" MapGLVersions REMAP OLD "+origKey+" -> "+GLContext.getGLVersion(new StringBuilder(), version, ctp[0], null).toString());
                }
                newDeviceVersionAvailable.put(origKey, valI);
                final int devSepIdx = origKey.lastIndexOf('-');
                if( 0 >= devSepIdx ) {
                    throw new InternalError("device-separator '-' at "+devSepIdx+" of "+origKey);
                }
                final String devUniqueID = origKey.substring(0, devSepIdx);
                if( fromDevice.getUniqueID().equals(devUniqueID) ) {
                    final String profileReq = origKey.substring(devSepIdx);
                    final String newKey = (toDevice.getUniqueID()+profileReq).intern();
                    if(DEBUG) {
                        System.err.println(" MapGLVersions REMAP NEW "+newKey+" -> (ditto)");
                    }
                    newDeviceVersionAvailable.put(newKey, valI);
                }
            }
        }
        deviceVersionAvailable.clear();
        deviceVersionAvailable.putAll(newDeviceVersionAvailable);
        GLContext.setAvailableGLVersionsSet(toDevice, true);
    }
  }

  private final boolean mapGLVersions(final AbstractGraphicsDevice device) {
    synchronized (GLContext.deviceVersionAvailable) {
        final boolean hasOpenGLESSupport = drawable.getFactory().hasOpenGLESSupport();
        final boolean hasOpenGLDesktopSupport = drawable.getFactory().hasOpenGLDesktopSupport();
        final boolean hasMinorVersionSupport = drawable.getFactoryImpl().hasMajorMinorCreateContextARB();
        if (DEBUG) {
            System.err.println(getThreadName() + ": createContextARB-MapGLVersions START (GLDesktop "+hasOpenGLDesktopSupport+", GLES "+hasOpenGLESSupport+", minorVersion "+hasMinorVersionSupport+") on "+device);
        }
        final long t0 = ( DEBUG ) ? System.nanoTime() : 0;
        boolean success = false;
        // Following GLProfile.GL_PROFILE_LIST_ALL order of profile detection { GL4bc, GL3bc, GL2, GL4, GL3, GL2GL3, GLES2, GL2ES2, GLES1, GL2ES1 }
        boolean hasGL4bc = false;
        boolean hasGL3bc = false;
        boolean hasGL2   = false;
        boolean hasGL4   = false;
        boolean hasGL3   = false;
        boolean hasES3   = false;
        boolean hasES2   = false;
        boolean hasES1   = false;

        if( hasOpenGLESSupport && !GLProfile.disableOpenGLES ) {
            if( !hasES3) {
                hasES3   = createContextARBMapVersionsAvailable(device, 3, CTX_PROFILE_ES, hasMinorVersionSupport);    // ES3
                success |= hasES3;
                if( hasES3 ) {
                    if( 0 == ( CTX_IMPL_ACCEL_SOFT & ctxOptions ) ) {
                        // Map hw-accel ES3 to all lower core profiles: ES2
                        mapAvailableGLVersion(device, 2, CTX_PROFILE_ES, ctxVersion, ctxOptions, glRendererQuirks);
                        if( PROFILE_ALIASING ) {
                            hasES2   = true;
                        }
                    }
                    resetStates(false); // clean context states, since creation was temporary
                }
            }
            if( !hasES2) {
                hasES2   = createContextARBMapVersionsAvailable(device, 2, CTX_PROFILE_ES, hasMinorVersionSupport);    // ES2
                success |= hasES2;
                if( hasES2 ) {
                    if( ctxVersion.getMajor() >= 3 && hasRendererQuirk(GLRendererQuirks.GLES3ViaEGLES2Config)) {
                        mapAvailableGLVersion(device, 3, CTX_PROFILE_ES, ctxVersion, ctxOptions, glRendererQuirks);
                    }
                    resetStates(false); // clean context states, since creation was temporary
                }
            }
            if( !hasES1) {
                hasES1   = createContextARBMapVersionsAvailable(device, 1, CTX_PROFILE_ES, hasMinorVersionSupport);    // ES1
                success |= hasES1;
                if( hasES1 ) {
                    resetStates(false); // clean context states, since creation was temporary
                }
            }
        }

        // Even w/ PROFILE_ALIASING, try to use true core GL profiles
        // ensuring proper user behavior across platforms due to different feature sets!
        //
        if( Platform.OSType.MACOS == Platform.getOSType() &&
            Platform.getOSVersionNumber().compareTo(Platform.OSXVersion.Mavericks) >= 0 ) {
            /**
             * OSX 10.9 GLRendererQuirks.GL4NeedsGL3Request, quirk is added as usual @ setRendererQuirks(..)
             */
            if( hasOpenGLDesktopSupport && !GLProfile.disableOpenGLDesktop && !GLProfile.disableOpenGLCore && !hasGL4 && !hasGL3 ) {
                hasGL3   = createContextARBMapVersionsAvailable(device, 3, CTX_PROFILE_CORE, hasMinorVersionSupport);    // GL3
                success |= hasGL3;
                if( hasGL3 ) {
                    final boolean isHWAccel = 0 == ( CTX_IMPL_ACCEL_SOFT & ctxOptions );
                    if( isHWAccel && ctxVersion.getMajor() >= 4 ) {
                        // Gotcha: Creating a '3.2' ctx delivers a >= 4 ctx.
                        mapAvailableGLVersion(device, 4, CTX_PROFILE_CORE, ctxVersion, ctxOptions, glRendererQuirks);
                        hasGL4   = true;
                        if(DEBUG) {
                            System.err.println(getThreadName() + ": createContextARB-MapGLVersions: Quirk Triggerd: "+GLRendererQuirks.toString(GLRendererQuirks.GL4NeedsGL3Request)+": cause: OS "+Platform.getOSType()+", OS Version "+Platform.getOSVersionNumber());
                        }
                    }
                    resetStates(false); // clean the context states, since creation was temporary
                }
            }
        }
        if( hasOpenGLDesktopSupport && !GLProfile.disableOpenGLDesktop && !GLProfile.disableOpenGLCore ) {
            if( !hasGL4 ) {
                hasGL4   = createContextARBMapVersionsAvailable(device, 4, CTX_PROFILE_CORE, hasMinorVersionSupport);    // GL4
                success |= hasGL4;
                if( hasGL4 ) {
                    if( 0 == ( CTX_IMPL_ACCEL_SOFT & ctxOptions ) ) {
                        // Map hw-accel GL4 to all lower core profiles: GL3
                        mapAvailableGLVersion(device, 3, CTX_PROFILE_CORE, ctxVersion, ctxOptions, glRendererQuirks);
                        if( PROFILE_ALIASING ) {
                            hasGL3   = true;
                        }
                    }
                    resetStates(false); // clean context states, since creation was temporary
                }
            }
            if( !hasGL3 ) {
                hasGL3   = createContextARBMapVersionsAvailable(device, 3, CTX_PROFILE_CORE, hasMinorVersionSupport);    // GL3
                success |= hasGL3;
                if( hasGL3 ) {
                    resetStates(false); // clean this context states, since creation was temporary
                }
            }
        }
        if( hasOpenGLDesktopSupport && !GLProfile.disableOpenGLDesktop ) {
            if( !hasGL4bc ) {
                hasGL4bc = createContextARBMapVersionsAvailable(device, 4, CTX_PROFILE_COMPAT, hasMinorVersionSupport);  // GL4bc
                success |= hasGL4bc;
                if( hasGL4bc ) {
                    if( !hasGL4 ) { // last chance .. ignore hw-accel
                        mapAvailableGLVersion(device, 4, CTX_PROFILE_CORE, ctxVersion, ctxOptions, glRendererQuirks);
                        hasGL4   = true;
                    }
                    if( !hasGL3 ) { // last chance .. ignore hw-accel
                        mapAvailableGLVersion(device, 3, CTX_PROFILE_CORE, ctxVersion, ctxOptions, glRendererQuirks);
                        hasGL3   = true;
                    }
                    if( 0 == ( CTX_IMPL_ACCEL_SOFT & ctxOptions ) ) {
                        // Map hw-accel GL4bc to all lower compatible profiles: GL3bc, GL2
                        mapAvailableGLVersion(device, 3, CTX_PROFILE_COMPAT, ctxVersion, ctxOptions, glRendererQuirks);
                        mapAvailableGLVersion(device, 2, CTX_PROFILE_COMPAT, ctxVersion, ctxOptions, glRendererQuirks);
                        if(PROFILE_ALIASING) {
                            hasGL3bc = true;
                            hasGL2   = true;
                        }
                    }
                    resetStates(false); // clean this context states, since creation was temporary
                }
            }
            if( !hasGL3bc ) {
                hasGL3bc = createContextARBMapVersionsAvailable(device, 3, CTX_PROFILE_COMPAT, hasMinorVersionSupport);  // GL3bc
                success |= hasGL3bc;
                if( hasGL3bc ) {
                    if(!hasGL3) {  // last chance .. ignore hw-accel
                        mapAvailableGLVersion(device, 3, CTX_PROFILE_CORE, ctxVersion, ctxOptions, glRendererQuirks);
                        hasGL3   = true;
                    }
                    if( 0 == ( CTX_IMPL_ACCEL_SOFT & ctxOptions ) ) {
                        // Map hw-accel GL3bc to all lower compatible profiles: GL2
                        mapAvailableGLVersion(device, 2, CTX_PROFILE_COMPAT, ctxVersion, ctxOptions, glRendererQuirks);
                        if(PROFILE_ALIASING) {
                            hasGL2   = true;
                        }
                    }
                    resetStates(false); // clean this context states, since creation was temporary
                }
            }
            if( !hasGL2 ) {
                hasGL2   = createContextARBMapVersionsAvailable(device, 2, CTX_PROFILE_COMPAT, hasMinorVersionSupport);  // GL2
                success |= hasGL2;
                if( hasGL2 ) {
                    resetStates(false); // clean this context states, since creation was temporary
                }
            }
        }
        if(success) {
            // only claim GL versions set [and hence detected] if ARB context creation was successful
            GLContext.setAvailableGLVersionsSet(device, true);
        }
        if(DEBUG) {
            final long t1 = System.nanoTime();
            System.err.println(getThreadName() + ": createContextARB-MapGLVersions END (success "+success+") on "+device+", profileAliasing: "+PROFILE_ALIASING+", total "+(t1-t0)/1e6 +"ms");
            if( success ) {
                System.err.println(GLContext.dumpAvailableGLVersions(null).toString());
            }
        }
        return success;
    }
  }

  /**
   * Note: Since context creation is temporary, caller need to issue {@link #resetStates(boolean)}, if creation was successful, i.e. returns true.
   * This method does not reset the states, allowing the caller to utilize the state variables.
   **/
  private final boolean createContextARBMapVersionsAvailable(final AbstractGraphicsDevice device, final int reqMajor, final int reqProfile,
                                                             final boolean hasMinorVersionSupport) {
    long _context;
    int ctp = CTX_IS_ARB_CREATED | reqProfile;

    // To ensure GL profile compatibility within the JOGL application
    // we always try to map against the highest GL version,
    // so the user can always cast to the highest available one.
    int maxMajor, maxMinor;
    int minMajor, minMinor;
    final int major[] = new int[1];
    final int minor[] = new int[1];

    if( hasMinorVersionSupport ) {
        if( CTX_PROFILE_ES == reqProfile ) {
            // ES3, ES2 or ES1
            maxMajor=reqMajor; maxMinor=GLContext.getMaxMinor(ctp, maxMajor);
            minMajor=reqMajor; minMinor=0;
        } else {
            if( 4 == reqMajor ) {
                maxMajor=4; maxMinor=GLContext.getMaxMinor(ctp, maxMajor);
                minMajor=4; minMinor=0;
            } else if( 3 == reqMajor ) {
                maxMajor=3; maxMinor=GLContext.getMaxMinor(ctp, maxMajor);
                minMajor=3; minMinor=1;
            } else /* if( glp.isGL2() ) */ {
                // our minimum desktop OpenGL runtime requirements are 1.1,
                // nevertheless we restrict ARB context creation to 2.0 to spare us futile attempts
                maxMajor=3; maxMinor=0;
                minMajor=2; minMinor=0;
            }
        }
    } else {
        if( CTX_PROFILE_ES == reqProfile ) {
            // ES3, ES2 or ES1
            maxMajor=reqMajor; maxMinor=0;
            minMajor=reqMajor; minMinor=0;
        } else {
            if( 4 == reqMajor ) {
                maxMajor=4; maxMinor=0;
                minMajor=4; minMinor=0;
            } else if( 3 == reqMajor ) {
                maxMajor=3; maxMinor=1;
                minMajor=3; minMinor=1;
            } else /* if( glp.isGL2() ) */ {
                // our minimum desktop OpenGL runtime requirements are 1.1,
                // nevertheless we restrict ARB context creation to 2.0 to spare us futile attempts
                maxMajor=2; maxMinor=0;
                minMajor=2; minMinor=0;
            }
        }
    }
    _context = createContextARBVersions(0, true, ctp,
                                        /* max */ maxMajor, maxMinor,
                                        /* min */ minMajor, minMinor,
                                        /* res */ major, minor);

    if( 0 == _context && CTX_PROFILE_CORE == reqProfile && !PROFILE_ALIASING ) {
        // try w/ FORWARD instead of CORE
        ctp &= ~CTX_PROFILE_CORE ;
        ctp |=  CTX_OPTION_FORWARD ;
        _context = createContextARBVersions(0, true, ctp,
                                            /* max */ maxMajor, maxMinor,
                                            /* min */ minMajor, minMinor,
                                            /* res */ major, minor);
       if( 0 == _context ) {
            // Try a compatible one .. even though not requested .. last resort
            ctp &= ~CTX_PROFILE_CORE ;
            ctp &= ~CTX_OPTION_FORWARD ;
            ctp |=  CTX_PROFILE_COMPAT ;
            _context = createContextARBVersions(0, true, ctp,
                                       /* max */ maxMajor, maxMinor,
                                       /* min */ minMajor, minMinor,
                                       /* res */ major, minor);
       }
    }
    final boolean res;
    if( 0 != _context ) {
        // ctxMajorVersion, ctxMinorVersion, ctxOptions is being set by
        //   createContextARBVersions(..) -> setGLFunctionAvailbility(..) -> setContextVersion(..)
        final MappedGLVersion me = mapAvailableGLVersion(device, reqMajor, reqProfile, ctxVersion, ctxOptions, glRendererQuirks);
        destroyContextARBImpl(_context);
        if (DEBUG) {
          System.err.println(getThreadName() + ": createContextARB-MapGLVersions HAVE "+me.toString(new StringBuilder(), minMajor, minMinor, maxMajor, maxMinor).toString());
        }
        res = true;
    } else {
        if (DEBUG) {
          System.err.println(getThreadName() + ": createContextARB-MapGLVersions NOPE "+device+", "+reqMajor+" ("+GLContext.getGLProfile(new StringBuilder(), reqProfile).toString()+ ") ["+maxMajor+"."+maxMinor+" .. "+minMajor+"."+minMinor+"]");
        }
        res = false;
    }
    return res;
  }

  private final long createContextARBVersions(final long share, final boolean direct, final int ctxOptionFlags,
                                              final int maxMajor, final int maxMinor,
                                              final int minMajor, final int minMinor,
                                              final int major[], final int minor[]) {
    major[0]=maxMajor;
    minor[0]=maxMinor;
    long _context=0;
    int i=0;

    do {
        if (DEBUG) {
            i++;
            System.err.println(getThreadName() + ": createContextARBVersions."+i+": share "+share+", direct "+direct+
                    ", version "+major[0]+"."+minor[0]+" ["+maxMajor+"."+maxMinor+" .. "+minMajor+"."+minMinor+"]");
        }
        _context = createContextARBImpl(share, direct, ctxOptionFlags, major[0], minor[0]);

        if(0 != _context) {
            if( setGLFunctionAvailability(true, major[0], minor[0], ctxOptionFlags, true /* strictMatch */, true /* withinGLVersionsMapping */) ) {
                break;
            } else {
                destroyContextARBImpl(_context);
                _context = 0;
            }
        }

    } while ( ( major[0]>minMajor || major[0]==minMajor && minor[0] >minMinor ) &&  // #1 check whether version is above lower limit
              GLContext.decrementGLVersion(ctxOptionFlags, major, minor)            // #2 decrement version
            );
    if (DEBUG) {
        System.err.println(getThreadName() + ": createContextARBVersions.X: ctx "+toHexString(_context)+", share "+share+", direct "+direct+
                ", version "+major[0]+"."+minor[0]+" ["+maxMajor+"."+maxMinor+" .. "+minMajor+"."+minMinor+"]");
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
  private final void setContextVersion(final int major, final int minor, final int ctp, final VersionNumberString glVendorVersion, final boolean useGL) {
      if ( 0 == ctp ) {
        throw new GLException("Invalid GL Version "+major+"."+minor+", ctp "+toHexString(ctp));
      }
      ctxVersion = new VersionNumber(major, minor, 0);
      ctxVersionString = getGLVersion(major, minor, ctp, glVersion);
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

  private final boolean verifyInstance(final GLProfile glp, final String suffix, final Object instance) {
      return ReflectionUtil.instanceOf(instance, glp.getGLImplBaseClassName()+suffix);
  }
  private final Object createInstance(final AbstractGraphicsDevice adevice, final int majorVersion, final int minorVersion, final int contextOption,
                                      final boolean glObject, final Object[] cstrArgs) {
      final String profileString = GLContext.getGLProfile(majorVersion, minorVersion, contextOption);
      final GLProfile glp = GLProfile.get(adevice, profileString) ;
      return ReflectionUtil.createInstance(glp.getGLCtor(glObject), cstrArgs);
  }
  private final boolean verifyInstance(final AbstractGraphicsDevice adevice, final int majorVersion, final int minorVersion, final int contextOption,
                                       final String suffix, final Object instance) {
      final String profileString = GLContext.getGLProfile(majorVersion, minorVersion, contextOption);
      final GLProfile glp = GLProfile.get(adevice, profileString) ;
      return ReflectionUtil.instanceOf(instance, glp.getGLImplBaseClassName()+suffix);
  }

  /**
   * Create the GL instance for this context,
   * requires valid {@link #getGLProcAddressTable()} result!
   */
  private final GL createGL(final AbstractGraphicsDevice adevice, final int majorVersion, final int minorVersion, final int contextOption) {
    final String profileString = GLContext.getGLProfile(majorVersion, minorVersion, contextOption);
    final GLProfile glp = GLProfile.get(adevice, profileString);
    final GL gl = (GL) ReflectionUtil.createInstance(glp.getGLCtor(true), new Object[] { glp, this });
    //nal GL gl = (GL) createInstance(glp, true, new Object[] { glp, this } );

    /* FIXME: refactor dependence on Java 2D / JOGL bridge
    if (tracker != null) {
      gl.setObjectTracker(tracker);
    }
    */
    return gl;
  }

  /**
   * Finalizes GL instance initialization after this context has been initialized.
   * <p>
   * Method calls 'void finalizeInit()' of instance 'gl' as retrieved by reflection, if exist.
   * </p>
   */
  private void finalizeInit(final GL gl) {
      Method finalizeInit = null;
      try {
          finalizeInit = ReflectionUtil.getMethod(gl.getClass(), "finalizeInit", new Class<?>[]{ });
      } catch ( final Throwable t ) {
          if(DEBUG) {
              System.err.println("Caught "+t.getClass().getName()+": "+t.getMessage());
              t.printStackTrace();
          }
      }
      if( null != finalizeInit ) {
          ReflectionUtil.callMethod(gl, finalizeInit, new Object[]{ });
      } else {
          throw new InternalError("Missing 'void finalizeInit(ProcAddressTable)' in "+gl.getClass().getName());
      }
  }

  public final ProcAddressTable getGLProcAddressTable() {
    return glProcAddressTable;
  }

  /**
   * Shall return the platform extension ProcAddressTable,
   * ie for GLXExt, EGLExt, ..
   */
  public abstract ProcAddressTable getPlatformExtProcAddressTable();

  /** Maps the given "platform-independent" function name to a real function
      name. Currently not used. */
  protected final String mapToRealGLFunctionName(final String glFunctionName) {
    final Map<String, String> map = getFunctionNameMap();
    if( null != map ) {
        final String lookup = map.get(glFunctionName);
        if (lookup != null) {
          return lookup;
        }
    }
    return glFunctionName;
  }
  protected abstract Map<String, String> getFunctionNameMap() ;

  /** Maps the given "platform-independent" extension name to a real
      function name. Currently this is only used to map
      "GL_ARB_pbuffer"      to  "WGL_ARB_pbuffer/GLX_SGIX_pbuffer" and
      "GL_ARB_pixel_format" to  "WGL_ARB_pixel_format/n.a."
   */
  protected final String mapToRealGLExtensionName(final String glExtensionName) {
    final Map<String, String> map = getExtensionNameMap();
    if( null != map ) {
        final String lookup = map.get(glExtensionName);
        if (lookup != null) {
          return lookup;
        }
    }
    return glExtensionName;
  }
  protected abstract Map<String, String> getExtensionNameMap() ;

  /**
   * Returns the DynamicLookupHelper
   */
  public final GLDynamicLookupHelper getGLDynamicLookupHelper() {
      return drawable.getFactoryImpl().getGLDynamicLookupHelper( ctxVersion.getMajor(), ctxOptions );
  }
  public final GLDynamicLookupHelper getGLDynamicLookupHelper(final int majorVersion, final int contextOptions) {
      return drawable.getFactoryImpl().getGLDynamicLookupHelper( majorVersion, contextOptions );
  }

  /** Helper routine which resets a ProcAddressTable generated by the
      GLEmitter by looking up anew all of its function pointers
      using the given {@link GLDynamicLookupHelper}. */
  protected final void resetProcAddressTable(final ProcAddressTable table, final GLDynamicLookupHelper dlh) {
    AccessController.doPrivileged(new PrivilegedAction<Object>() {
        @Override
        public Object run() {
            table.reset( dlh );
            return null;
        }
    } );
  }

  /**
   * Updates the platform's 'GLX' function cache
   * @param contextFQN provides a fully qualified key of the context including device and GL profile
   * @param dlh {@link GLDynamicLookupHelper} used to {@link #resetProcAddressTable(ProcAddressTable, GLDynamicLookupHelper)} instance.
   */
  protected abstract void updateGLXProcAddressTable(final String contextFQN, final GLDynamicLookupHelper dlh);

  private final boolean initGLRendererAndGLVersionStrings(final int majorVersion, final int contextOptions)  {
    if( !glGetPtrInit ) {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                final GLDynamicLookupHelper glDynLookupHelper = getGLDynamicLookupHelper(majorVersion, contextOptions);
                if( null != glDynLookupHelper ) {
                    glDynLookupHelper.claimAllLinkPermission();
                    try {
                        glGetStringPtr = glDynLookupHelper.dynamicLookupFunction("glGetString");
                        glGetIntegervPtr = glDynLookupHelper.dynamicLookupFunction("glGetIntegerv");
                    } finally {
                        glDynLookupHelper.releaseAllLinkPermission();
                    }
                }
                return null;
            } } );
        glGetPtrInit = true;
    }
    if( 0 == glGetStringPtr || 0 == glGetIntegervPtr ) {
        System.err.println("Error: Could not lookup: glGetString "+toHexString(glGetStringPtr)+", glGetIntegerv "+toHexString(glGetIntegervPtr));
        if(DEBUG) {
            ExceptionUtils.dumpStack(System.err);
        }
        return false;
    } else {
        final String _glVendor = glGetStringInt(GL.GL_VENDOR, glGetStringPtr);
        if(null == _glVendor) {
            if(DEBUG) {
                System.err.println("Warning: GL_VENDOR is NULL.");
                ExceptionUtils.dumpStack(System.err);
            }
            return false;
        }
        glVendor = _glVendor;

        final String _glRenderer = glGetStringInt(GL.GL_RENDERER, glGetStringPtr);
        if(null == _glRenderer) {
            if(DEBUG) {
                System.err.println("Warning: GL_RENDERER is NULL.");
                ExceptionUtils.dumpStack(System.err);
            }
            return false;
        }
        glRenderer = _glRenderer;
        glRendererLowerCase = glRenderer.toLowerCase();

        final String _glVersion = glGetStringInt(GL.GL_VERSION, glGetStringPtr);
        if(null == _glVersion) {
            // FIXME
            if(DEBUG) {
                System.err.println("Warning: GL_VERSION is NULL.");
                ExceptionUtils.dumpStack(System.err);
            }
            return false;
        }
        glVersion = _glVersion;

        return true;
    }
  }

  /**
   * Returns false if <code>glGetIntegerv</code> is inaccessible, otherwise queries major.minor
   * version for given arrays.
   * <p>
   * If the GL query fails, major will be zero.
   * </p>
   */
  private final void getGLIntVersion(final int[] glIntMajor, final int[] glIntMinor)  {
    glIntMajor[0] = 0; // clear
    glIntMinor[0] = 0; // clear
    if( 0 == glGetIntegervPtr ) {
        // should not be reached, since initGLRendererAndGLVersionStrings(..)'s failure should abort caller!
        throw new InternalError("Not initialized: glGetString "+toHexString(glGetStringPtr)+", glGetIntegerv "+toHexString(glGetIntegervPtr));
    } else {
        glGetIntegervInt(GL2ES3.GL_MAJOR_VERSION, glIntMajor, 0, glGetIntegervPtr);
        glGetIntegervInt(GL2ES3.GL_MINOR_VERSION, glIntMinor, 0, glGetIntegervPtr);
    }
  }


  /**
   * Returns null if version string is invalid, otherwise a valid instance.
   * <p>
   * Note: Non ARB ctx is limited to GL 3.0.
   * </p>
   */
  private static final VersionNumber getGLVersionNumber(final int ctp, final String glVersionStr) {
      if( null != glVersionStr ) {
          final GLVersionNumber version = GLVersionNumber.create(glVersionStr);
          if ( version.isValid() ) {
              final int[] major = new int[] { version.getMajor() };
              final int[] minor = new int[] { version.getMinor() };
              if ( GLContext.isValidGLVersion(ctp, major[0], minor[0]) ) {
                  return new VersionNumber(major[0], minor[0], 0);
              }
          }
      }
      return null;
  }

  protected final int getCtxOptions() {
      return ctxOptions;
  }


  /**
   * Sets the OpenGL implementation class and
   * the cache of which GL functions are available for calling through this
   * context. See {@link #isFunctionAvailable(String)} for more information on
   * the definition of "available".
   * <p>
   * All ProcaddressTables are being determined and cached, the GL version is being set
   * and the extension cache is determined as well.
   * </p>
   * <p>
   * It is the callers responsibility to issue {@link #resetStates(boolean)}
   * in case this method returns {@code false} or throws a {@link GLException}.
   * </p>
   *
   * @param force force the setting, even if is already being set.
   *              This might be useful if you change the OpenGL implementation.
   * @param major requested OpenGL major version
   * @param minor requested OpenGL minor version
   * @param ctxProfileBits OpenGL context profile and option bits, see {@link com.jogamp.opengl.GLContext#CTX_OPTION_ANY}
   * @param strictMatch if <code>true</code> the ctx must
   *                    <ul>
   *                      <li>be greater or equal than the requested <code>major.minor</code> version, and</li>
   *                      <li>match the ctxProfileBits</li>
   *                      <li>match ES major versions</li>
   *                    </ul>, otherwise method aborts and returns <code>false</code>.<br>
   *                    if <code>false</code> no version check is performed.
   * @param withinGLVersionsMapping if <code>true</code> GL version mapping is in process, i.e. querying avail versions.
   *                                Otherwise normal user context creation.
   * @return returns <code>true</code> if successful, otherwise <code>false</code>.<br>
   *                 If <code>strictMatch</code> is <code>false</code> method shall always return <code>true</code> or throw an exception.
   *                 If <code>false</code> is returned, no data has been cached or mapped, i.e. ProcAddressTable, Extensions, Version, etc.
   * @throws GLException in case of an unexpected OpenGL related issue, e.g. missing expected GL function pointer.
   * @see #setContextVersion
   * @see com.jogamp.opengl.GLContext#CTX_OPTION_ANY
   * @see com.jogamp.opengl.GLContext#CTX_PROFILE_COMPAT
   * @see com.jogamp.opengl.GLContext#CTX_IMPL_ES2_COMPAT
   */
  protected final boolean setGLFunctionAvailability(final boolean force, int major, int minor, int ctxProfileBits,
                                                    final boolean strictMatch, final boolean withinGLVersionsMapping)
                                                    throws GLException
  {
    if( null != this.gl && null != glProcAddressTable && !force ) {
        return true; // already done and not forced
    }

    if ( 0 < major && !GLContext.isValidGLVersion(ctxProfileBits, major, minor) ) {
        throw new GLException("Invalid GL Version Request "+GLContext.getGLVersion(major, minor, ctxProfileBits, null));
    }

    final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration();
    final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
    final int reqCtxProfileBits = ctxProfileBits;
    final VersionNumber reqGLVersion = new VersionNumber(major, minor, 0);
    final VersionNumber hasGLVersionByString;
    {
        final boolean initGLRendererAndGLVersionStringsOK = initGLRendererAndGLVersionStrings(major, ctxProfileBits);
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
        } else {
            hasGLVersionByString = getGLVersionNumber(ctxProfileBits, glVersion);
            if(DEBUG) {
                System.err.println(getThreadName() + ": GLContext.setGLFuncAvail: Given "+adevice+
                                   " - "+GLContext.getGLVersion(major, minor, ctxProfileBits, glVersion)+
                                   ", Number(Str) "+hasGLVersionByString);
            }
        }
    }

    final boolean isES = 0 != ( CTX_PROFILE_ES & ctxProfileBits );

    //
    // Validate GL version either by GL-Integer or GL-String
    //
    if (DEBUG) {
        System.err.println(getThreadName() + ": GLContext.setGLFuncAvail: Pre version verification - expected "+GLContext.getGLVersion(major, minor, ctxProfileBits, null)+", strictMatch "+strictMatch+", glVersionsMapping " +withinGLVersionsMapping);
    }

    final boolean versionGL3IntOK;
    {
        // Validate the requested version w/ the GL-version from an integer query,
        // as supported by GL [ES] >= 3.0 implementation.
        //
        // Only validate integer based version if:
        //    - ctx >= 3.0 is requested _or_ string-version >= 3.0
        //    - _and_ a valid int version was fetched,
        // otherwise cont. w/ version-string method -> 3.0 > Version || Version > MAX!
        //
        final VersionNumber hasGLVersionByInt;
        if ( major >= 3 || hasGLVersionByString.compareTo(Version3_0) >= 0 ) {
            final int[] glIntMajor = new int[] { 0 }, glIntMinor = new int[] { 0 };
            getGLIntVersion(glIntMajor, glIntMinor);
            hasGLVersionByInt = new VersionNumber(glIntMajor[0], glIntMinor[0], 0);
            if (DEBUG) {
                System.err.println(getThreadName() + ": GLContext.setGLFuncAvail: Version verification (Int): String "+glVersion+", Number(Int) "+hasGLVersionByInt);
            }
            if ( GLContext.isValidGLVersion(ctxProfileBits, hasGLVersionByInt.getMajor(), hasGLVersionByInt.getMinor()) ) {
                // Strict Match (GLVersionMapping):
                //   Relaxed match for versions ( !isES && major < 3 ) requests, last resort!
                //   Otherwise:
                //     - fail if hasVersion < reqVersion (desktop and ES)
                //     - fail if ES major-version mismatch:
                //       - request 1, >= 3 must be equal
                //       - request 2 must be [2..3]
                //
                final int hasMajor = hasGLVersionByInt.getMajor();
                if( strictMatch &&
                    ( ( ( isES || major >= 3 ) && hasGLVersionByInt.compareTo(reqGLVersion) < 0 ) ||
                      ( isES &&
                        (
                          ( 2 == major && ( 2 > hasMajor || hasMajor > 3 ) ) ||  // 2      -> [2..3]
                          ( ( 1 == major || 3 <= major ) && major != hasMajor )  // 1,3,.. -> equal
                        )
                      )
                    ) ) {
                    if(DEBUG) {
                        System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: FAIL, GL version mismatch (Int): "+GLContext.getGLVersion(major, minor, ctxProfileBits, null)+" -> "+glVersion+", "+hasGLVersionByInt);
                    }
                    return false;
                }
                // Use returned GL version!
                major = hasGLVersionByInt.getMajor();
                minor = hasGLVersionByInt.getMinor();
                versionGL3IntOK = true;
            } else {
                versionGL3IntOK = false;
            }
        } else {
            versionGL3IntOK = false;
        }
    }
    final boolean versionValidated;

    if( versionGL3IntOK ) {
        versionValidated = true;
    } else {
        // Validate the requested version w/ the GL-version from the version string.
        if (DEBUG) {
            System.err.println(getThreadName() + ": GLContext.setGLFuncAvail: Version verification (String): String "+glVersion+", Number(Str) "+hasGLVersionByString);
        }

        // Only validate if a valid string version was fetched -> MIN > Version || Version > MAX!
        if( null != hasGLVersionByString ) {
            // Strict Match (GLVersionMapping):
            //   Relaxed match for versions ( !isES && major < 3 ) requests, last resort!
            //   Otherwise:
            //     - fail if hasVersion < reqVersion (desktop and ES)
            //     - fail if ES major-version mismatch:
            //       - request 1, >= 3 must be equal
            //       - request 2 must be [2..3]
            //
            final int hasMajor = hasGLVersionByString.getMajor();
            if( strictMatch &&
                ( ( ( isES || major >= 3 ) && hasGLVersionByString.compareTo(reqGLVersion) < 0 ) ||
                  ( isES &&
                    (
                      ( 2 == major && ( 2 > hasMajor || hasMajor > 3 ) ) ||  // 2      -> [2..3]
                      ( ( 1 == major || 3 <= major ) && major != hasMajor )  // 1,3,.. -> equal
                    )
                  )
                ) ) {
                if(DEBUG) {
                    System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: FAIL, GL version mismatch (String): "+GLContext.getGLVersion(major, minor, ctxProfileBits, null)+" -> "+glVersion+", "+hasGLVersionByString);
                }
                return false;
            }
            if( strictMatch && !versionGL3IntOK && major >= 3 ) {
                if(DEBUG) {
                    System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: FAIL, GL3/ES3 version Int failed, String: "+GLContext.getGLVersion(major, minor, ctxProfileBits, null)+" -> "+glVersion+", "+hasGLVersionByString);
                }
                return false;
            }
            // Use returned GL version!
            major = hasGLVersionByString.getMajor();
            minor = hasGLVersionByString.getMinor();
            versionValidated = true;
        } else {
            versionValidated = false;
        }
    }
    if( strictMatch && !versionValidated ) {
        if(DEBUG) {
            System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: FAIL, No GL version validation possible: "+GLContext.getGLVersion(major, minor, ctxProfileBits, null)+" -> "+glVersion);
        }
        return false;
    }
    if (DEBUG) {
        System.err.println(getThreadName() + ": GLContext.setGLFuncAvail: Post version verification req "+
                GLContext.getGLVersion(reqGLVersion.getMajor(), reqGLVersion.getMinor(), reqCtxProfileBits, null)+" -> has "+
                GLContext.getGLVersion(major, minor, ctxProfileBits, null)+
                ", strictMatch "+strictMatch+", versionValidated "+versionValidated+", versionGL3IntOK "+versionGL3IntOK);
    }

    if( major < 2 ) { // there is no ES2/3-compat for a profile w/ major < 2
        ctxProfileBits &= ~ ( GLContext.CTX_IMPL_ES2_COMPAT | GLContext.CTX_IMPL_ES3_COMPAT |
                              GLContext.CTX_IMPL_ES31_COMPAT | GLContext.CTX_IMPL_ES32_COMPAT ) ;
    }

    if(!isCurrentContextHardwareRasterizer()) {
        ctxProfileBits |= GLContext.CTX_IMPL_ACCEL_SOFT;
    }

    final VersionNumberString vendorVersion = GLVersionNumber.createVendorVersion(glVersion);

    setRendererQuirks(adevice, getDrawableImpl().getFactoryImpl(),
                      reqGLVersion.getMajor(), reqGLVersion.getMinor(), reqCtxProfileBits,
                      major, minor, ctxProfileBits, vendorVersion, withinGLVersionsMapping);

    if( strictMatch && glRendererQuirks.exist(GLRendererQuirks.GLNonCompliant) ) {
        if(DEBUG) {
            System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: FAIL, GL is not compliant: "+GLContext.getGLVersion(major, minor, ctxProfileBits, glVersion)+", "+glRenderer);
        }
        return false;
    }

    contextFQN = getContextFQN(adevice, major, minor, ctxProfileBits);
    if (DEBUG) {
        System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.0 validated FQN: "+contextFQN+" - "+GLContext.getGLVersion(major, minor, ctxProfileBits, glVersion));
    }
    final GLDynamicLookupHelper dynamicLookup = getGLDynamicLookupHelper(major, ctxProfileBits);
    if( null == dynamicLookup ) {
        if(DEBUG) {
            System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: FAIL, No GLDynamicLookupHelper for request: "+GLContext.getGLVersion(major, minor, ctxProfileBits, null));
        }
        return false;
    }
    updateGLXProcAddressTable(contextFQN, dynamicLookup);

    //
    // UpdateGLProcAddressTable functionality
    // _and_ setup GL instance, which ctor requires valid getGLProcAddressTable() result!
    //
    {
        final GLProfile glp = drawable.getGLProfile(); // !withinGLVersionsMapping

        ProcAddressTable table = null;
        synchronized(mappedContextTypeObjectLock) {
            table = mappedGLProcAddress.get( contextFQN );
            if(null != table) {
                if( !verifyInstance(adevice, major, minor, ctxProfileBits, "ProcAddressTable", table) ) {
                    throw new GLException("GLContext GL ProcAddressTable mapped key("+contextFQN+" - " + GLContext.getGLVersion(major, minor, ctxProfileBits, null)+
                          ") -> "+ toHexString(table.hashCode()) +" not matching "+table.getClass().getName());
                }
                if( !withinGLVersionsMapping && !verifyInstance(glp, "ProcAddressTable", table) ) {
                    throw new GLException("GLContext GL ProcAddressTable mapped key("+contextFQN+" - " + GLContext.getGLVersion(major, minor, ctxProfileBits, null)+
                          ") -> "+ toHexString(table.hashCode()) +": "+table.getClass().getName()+" not matching "+glp.getGLImplBaseClassName()+"/"+glp);
                }
            }
        }
        if(null != table) {
            glProcAddressTable = table;
            if(DEBUG) {
                if( withinGLVersionsMapping ) {
                    System.err.println(getThreadName() + ": GLContext GL ProcAddressTable reusing key("+contextFQN+" - " + GLContext.getGLVersion(major, minor, ctxProfileBits, null)+
                          ") -> "+ toHexString(table.hashCode()) +": "+table.getClass().getName());
                } else {
                    System.err.println(getThreadName() + ": GLContext GL ProcAddressTable reusing key("+contextFQN+" - " + GLContext.getGLVersion(major, minor, ctxProfileBits, null)+
                          ") -> "+ toHexString(table.hashCode()) +": "+table.getClass().getName()+" -> "+glp.getGLImplBaseClassName());
                }
            }
        } else {
            glProcAddressTable = (ProcAddressTable) createInstance(adevice, major, minor, ctxProfileBits, false,
                                                                   new Object[] { new GLProcAddressResolver() } );
            resetProcAddressTable(glProcAddressTable, dynamicLookup);

            synchronized(mappedContextTypeObjectLock) {
                mappedGLProcAddress.put(contextFQN, glProcAddressTable);
                if(DEBUG) {
                    if( withinGLVersionsMapping ) {
                        System.err.println(getThreadName() + ": GLContext GL ProcAddressTable mapping key("+contextFQN+" - " + GLContext.getGLVersion(major, minor, ctxProfileBits, null)+
                          ") -> "+toHexString(glProcAddressTable.hashCode()) +": "+glProcAddressTable.getClass().getName());
                    } else {
                        System.err.println(getThreadName() + ": GLContext GL ProcAddressTable mapping key("+contextFQN+" - " + GLContext.getGLVersion(major, minor, ctxProfileBits, null)+
                          ") -> "+toHexString(glProcAddressTable.hashCode()) +": "+glProcAddressTable.getClass().getName()+" -> "+glp.getGLImplBaseClassName());
                    }
                }
            }
        }

        if( null == this.gl || !verifyInstance(adevice, major, minor, ctxProfileBits, "Impl", this.gl) ) {
            setGL( createGL( adevice, major, minor, ctxProfileBits ) );
        }
        if( !withinGLVersionsMapping && !verifyInstance(glp, "Impl", this.gl) ) {
            throw new GLException("GLContext GL Object mismatch: "+GLContext.getGLVersion(major, minor, ctxProfileBits, null)+
                    ") -> "+": "+this.gl.getClass().getName()+" not matching "+glp.getGLImplBaseClassName()+"/"+glp);
        }
    }

    //
    // Update ExtensionAvailabilityCache
    //
    {
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
    }

    if( isES ) {
        if( major >= 3 ) {
            ctxProfileBits |= CTX_IMPL_ES3_COMPAT | CTX_IMPL_ES2_COMPAT ;
            ctxProfileBits |= CTX_IMPL_FBO;
            if( minor >= 2 ) {
                ctxProfileBits |= CTX_IMPL_ES32_COMPAT | CTX_IMPL_ES31_COMPAT;
            } else if( minor >= 1 ) {
                ctxProfileBits |= CTX_IMPL_ES31_COMPAT;
            }
        } else if( major >= 2 ) {
            ctxProfileBits |= CTX_IMPL_ES2_COMPAT;
            ctxProfileBits |= CTX_IMPL_FBO;
        }
    } else if( ( major > 4 || major == 4 && minor >= 5 ) ||
               ( major > 3 || major == 3 && minor >= 1 ) ) {
        // See GLContext.isGLES31CompatibleAvailable(..)/isGLES3[12]Compatible()
        //   Includes [ GL &ge; 4.5, GL &ge; 3.1 w/ GL_ARB_ES3_[12]_compatibility and GLES &ge; 3.[12] ]
        if( isExtensionAvailable( GLExtensions.ARB_ES3_2_compatibility ) ) {
            ctxProfileBits |= CTX_IMPL_ES32_COMPAT | CTX_IMPL_ES31_COMPAT;
        } else if( isExtensionAvailable( GLExtensions.ARB_ES3_1_compatibility ) ) {
            ctxProfileBits |= CTX_IMPL_ES31_COMPAT;
        }
        ctxProfileBits |= CTX_IMPL_ES3_COMPAT | CTX_IMPL_ES2_COMPAT;
        ctxProfileBits |= CTX_IMPL_FBO;
    } else if( ( major > 4 || major == 4 && minor >= 3 ) ||
               ( ( major > 3 || major == 3 && minor >= 1 ) && isExtensionAvailable( GLExtensions.ARB_ES3_compatibility ) ) ) {
        // See GLContext.isGLES3CompatibleAvailable(..)/isGLES3Compatible()
        //   Includes [ GL &ge; 4.3, GL &ge; 3.1 w/ GL_ARB_ES3_compatibility and GLES3 ]
        ctxProfileBits |= CTX_IMPL_ES3_COMPAT | CTX_IMPL_ES2_COMPAT ;
        ctxProfileBits |= CTX_IMPL_FBO;
    } else if( isExtensionAvailable( GLExtensions.ARB_ES2_compatibility ) ) {
        ctxProfileBits |= CTX_IMPL_ES2_COMPAT;
        ctxProfileBits |= CTX_IMPL_FBO;
    } else if( hasFBOImpl(major, ctxProfileBits, extensionAvailability) ) {
        ctxProfileBits |= CTX_IMPL_FBO;
    }

    if( ( isES && major == 1 ) ||  isExtensionAvailable(GLExtensions.OES_single_precision) ) {
        ctxProfileBits |= CTX_IMPL_FP32_COMPAT_API;
    }

    if(FORCE_NO_FBO_SUPPORT) {
        ctxProfileBits &= ~CTX_IMPL_FBO ;
    }

    //
    // Set GL Version (complete w/ version string)
    //
    setContextVersion(major, minor, ctxProfileBits, vendorVersion, true);

    finalizeInit(gl);

    setDefaultSwapInterval();

    final int glErrX = gl.glGetError(); // clear GL error, maybe caused by above operations

    if(DEBUG) {
        System.err.println(getThreadName() + ": GLContext.setGLFuncAvail.X: OK "+contextFQN+" - "+GLContext.getGLVersion(ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions, null)+" - glErr "+toHexString(glErrX));
    }
    return true;
  }

  private static final void addStickyQuirkAlways(final AbstractGraphicsDevice adevice,
                                                 final GLRendererQuirks quirks,
                                                 final int quirk,
                                                 final boolean withinGLVersionsMapping) {
        quirks.addQuirk( quirk );
        if( withinGLVersionsMapping ) {
            // Thread safe due to single threaded initialization!
            GLRendererQuirks.addStickyDeviceQuirk(adevice, quirk);
        } else {
            // FIXME: Remove when moving EGL/ES to ARB ctx creation
            synchronized(GLContextImpl.class) {
                GLRendererQuirks.addStickyDeviceQuirk(adevice, quirk);
            }
        }
  }
  private static final void addStickyQuirkAtMapping(final AbstractGraphicsDevice adevice,
                                                    final GLRendererQuirks quirks,
                                                    final int quirk,
                                                    final boolean withinGLVersionsMapping) {
        quirks.addQuirk( quirk );
        if( withinGLVersionsMapping ) {
            // Thread safe due to single threaded initialization!
            GLRendererQuirks.addStickyDeviceQuirk(adevice, quirk);
        }
  }
  private final void setRendererQuirks(final AbstractGraphicsDevice adevice, final GLDrawableFactoryImpl factory,
                                       final int reqMajor, final int reqMinor, final int reqCTP,
                                       final int major, final int minor, final int ctp, final VersionNumberString vendorVersion,
                                       final boolean withinGLVersionsMapping) {
    final String MesaSP = "Mesa ";
    // final String MesaRendererAMDsp = " AMD ";
    final String MesaRendererIntelsp = "Intel(R)";
    final boolean hwAccel = 0 == ( ctp & GLContext.CTX_IMPL_ACCEL_SOFT );
    final boolean compatCtx = 0 != ( ctp & GLContext.CTX_PROFILE_COMPAT );
    final boolean isES = 0 != ( ctp & GLContext.CTX_PROFILE_ES );
    final boolean isX11 = NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(true);
    final boolean isWindows = Platform.getOSType() == Platform.OSType.WINDOWS;
    final boolean isDriverMesa = glRenderer.contains(MesaSP) || glRenderer.contains("Gallium ");

    final boolean isDriverATICatalyst;
    final boolean isDriverNVIDIAGeForce;
    final boolean isDriverIntel;
    if( !isDriverMesa ) {
        isDriverATICatalyst = glVendor.contains("ATI Technologies") || glRenderer.startsWith("ATI ");
        isDriverNVIDIAGeForce = glVendor.contains("NVIDIA Corporation") || glRenderer.contains("NVIDIA ");
        isDriverIntel = glVendor.startsWith("Intel");
    } else {
        isDriverATICatalyst = false;
        isDriverNVIDIAGeForce = false;
        isDriverIntel = false;
    }

    final GLRendererQuirks quirks = new GLRendererQuirks();

    //
    // General Quirks
    //
    if( isES ) {
        if( 2 == reqMajor && 2 < major ) {
            final int quirk = GLRendererQuirks.GLES3ViaEGLES2Config;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: ES req "+reqMajor+" and 2 < "+major);
            }
            addStickyQuirkAlways(adevice, quirks, quirk, withinGLVersionsMapping);
        }
    }
    if( GLProfile.disableSurfacelessContext ) {
        final int quirk = GLRendererQuirks.NoSurfacelessCtx;
        if(DEBUG) {
            System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: disabled");
        }
        addStickyQuirkAlways(adevice, quirks, quirk, withinGLVersionsMapping);
    }
    if( GLProfile.disableOpenGLARBContext ) {
        final int quirk = GLRendererQuirks.NoARBCreateContext;
        if(DEBUG) {
            System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: disabled");
        }
        addStickyQuirkAlways(adevice, quirks, quirk, withinGLVersionsMapping);
    }

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
            quirks.addQuirk( quirk );
        }
        {
            final int quirk = GLRendererQuirks.NeedSharedObjectSync;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType());
            }
            quirks.addQuirk( quirk );
        }
        if( Platform.getOSVersionNumber().compareTo(Platform.OSXVersion.Mavericks) >= 0 && 3==reqMajor && 4==major ) {
            final int quirk = GLRendererQuirks.GL4NeedsGL3Request;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType()+", OS Version "+Platform.getOSVersionNumber()+", req "+reqMajor+"."+reqMinor);
            }
            addStickyQuirkAtMapping(adevice, quirks, quirk, withinGLVersionsMapping);
        }
        if( isDriverNVIDIAGeForce ) {
            final VersionNumber osxVersionNVFlushClean = new VersionNumber(10,7,3); // < OSX 10.7.3 w/ NV needs glFlush
            if( Platform.getOSVersionNumber().compareTo(osxVersionNVFlushClean) < 0 ) {
                final int quirk = GLRendererQuirks.GLFlushBeforeRelease;
                if(DEBUG) {
                    System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType()+", OS Version "+Platform.getOSVersionNumber()+", Renderer "+glRenderer);
                }
                quirks.addQuirk( quirk );
            }
            if( Platform.getOSVersionNumber().compareTo(Platform.OSXVersion.Lion) < 0 ) { // < OSX 10.7.0 w/ NV has unstable GLSL
                final int quirk = GLRendererQuirks.GLSLNonCompliant;
                if(DEBUG) {
                    System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType()+", OS Version "+Platform.getOSVersionNumber()+", Renderer "+glRenderer);
                }
                quirks.addQuirk( quirk );
            }
        }
    } else if( isWindows ) {
        //
        // WINDOWS
        //
        {
            final int quirk = GLRendererQuirks.NoDoubleBufferedBitmap;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType());
            }
            quirks.addQuirk( quirk );
        }

        if( isDriverATICatalyst ) {
            final VersionNumber winXPVersionNumber = new VersionNumber ( 5, 1, 0);
            final VersionNumber amdSafeMobilityVersion = new VersionNumber(12, 102, 3);

            if ( vendorVersion.compareTo(amdSafeMobilityVersion) < 0 ) { // includes: vendorVersion.isZero()
                final int quirk = GLRendererQuirks.NeedCurrCtx4ARBCreateContext;
                if(DEBUG) {
                    System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType()+", [Vendor "+glVendor+" or Renderer "+glRenderer+"], driverVersion "+vendorVersion);
                }
                quirks.addQuirk( quirk );
            }

            if( Platform.getOSVersionNumber().compareTo(winXPVersionNumber) <= 0 ) {
                final int quirk = GLRendererQuirks.NeedCurrCtx4ARBPixFmtQueries;
                if(DEBUG) {
                    System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS-Version "+Platform.getOSType()+" "+Platform.getOSVersionNumber()+", [Vendor "+glVendor+" or Renderer "+glRenderer+"]");
                }
                quirks.addQuirk( quirk );
            }

            if (  vendorVersion.compareTo(VersionNumberString.zeroVersion) == 0 ) {
                final VersionNumber glVersionNumber = new VersionNumber(glVersion);
                if ( glVersionNumber.getSub() <= 8787 && glRenderer.equals("ATI Radeon 3100 Graphics") ) { // "old" driver -> sub-minor = vendor version
                    final int quirk = GLRendererQuirks.NoARBCreateContext;
                    if(DEBUG) {
                        System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType()+", [Vendor "+glVendor+", Renderer "+glRenderer+" and Version "+glVersion+"]");
                    }
                    addStickyQuirkAtMapping(adevice, quirks, quirk, withinGLVersionsMapping);
                }
            }
        } else if( isDriverIntel && glRenderer.equals("Intel Bear Lake B") ) {
          	final int quirk = GLRendererQuirks.NoPBufferWithAccum;
          	if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType()+", [Vendor "+glVendor+" and Renderer "+glRenderer+"]");
            }
           	quirks.addQuirk( quirk );
        }
    } else if( Platform.OSType.ANDROID == Platform.getOSType() ) {
        //
        // ANDROID
        //
        // Renderer related quirks, may also involve OS
        if( glRenderer.contains("PowerVR") ) {
            final int quirk = GLRendererQuirks.NoSetSwapInterval;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType() + ", Renderer " + glRenderer);
            }
            quirks.addQuirk( quirk );
        }
        if( glRenderer.contains("Immersion.16") ) {
            final int quirk = GLRendererQuirks.GLSharedContextBuggy;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType() + ", Renderer " + glRenderer);
            }
            quirks.addQuirk( quirk );
        }
    }

    //
    // Windowing Toolkit related quirks
    //
    if( isX11 ) {
        //
        // X11
        //
        {
            //
            // Quirk: DontCloseX11Display
            //
            final int quirk = GLRendererQuirks.DontCloseX11Display;
            if( glRenderer.contains(MesaSP) ) {
                if ( glRenderer.contains("X11") && vendorVersion.compareTo(Version8_0) < 0 ) {
                    if(DEBUG) {
                        System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: X11 Renderer=" + glRenderer + ", Version=[vendor " + vendorVersion + ", GL " + glVersion+"]");
                    }
                    quirks.addQuirk( quirk );
                }
            } else if( isDriverATICatalyst ) {
                {
                    if(DEBUG) {
                        System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: X11 Renderer=" + glRenderer);
                    }
                    quirks.addQuirk( quirk );
                }
            } else if( jogamp.nativewindow.x11.X11Util.getMarkAllDisplaysUnclosable() ) {
                {
                    if(DEBUG) {
                        System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: X11Util Downstream");
                    }
                    quirks.addQuirk( quirk );
                }
            }
        }
        if( isDriverNVIDIAGeForce ) {
            // Bug 1200: Crash on GNU/Linux x86_64 'NVidia beta driver 355.06' @ probeSurfacelessCtx
            // final VersionNumber nvSafeVersion = new VersionNumber(356, 0, 0); // FIXME: Add safe version!
            if( !isES && !(adevice instanceof EGLGraphicsDevice) /* &&  vendorVersion.compareTo(nvSafeVersion) < 0 */ ) {
                final int quirk = GLRendererQuirks.NoSurfacelessCtx;
                if(DEBUG) {
                    System.err.print("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: !ES, !EGL, Vendor " + glVendor +", X11 Renderer " + glRenderer+", Version=[vendor " + vendorVersion + ", GL " + glVersion+"]");
                }
                addStickyQuirkAtMapping(adevice, quirks, quirk, withinGLVersionsMapping);
            }
        }
    }


    //
    // RENDERER related quirks
    //
    if( isDriverMesa ) {
        final VersionNumber mesaSafeFBOVersion = new VersionNumber(8, 0, 0);
        final VersionNumber mesaIntelBuggySharedCtx921 = new VersionNumber(9, 2, 1);

        {
            final int quirk = GLRendererQuirks.NoSetSwapIntervalPostRetarget;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: Renderer " + glRenderer);
            }
            quirks.addQuirk( quirk );
        }
        if( hwAccel ) {
            // hardware-acceleration
            final int quirk = GLRendererQuirks.NoDoubleBufferedPBuffer;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: Renderer " + glRenderer);
            }
            quirks.addQuirk( quirk );
        } else {
            // software
            if( vendorVersion.compareTo(mesaSafeFBOVersion) < 0 ) { // FIXME: Is it fixed in >= 8.0.0 ?
                final int quirk = GLRendererQuirks.BuggyColorRenderbuffer;
                if(DEBUG) {
                    System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: Renderer " + glRenderer + " / Mesa-Version "+vendorVersion);
                }
                quirks.addQuirk( quirk );
            }
        }
        if (compatCtx && (major > 3 || (major == 3 && minor >= 1))) {
            // FIXME: Apply vendor version constraints!
            final int quirk = GLRendererQuirks.GLNonCompliant;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: Renderer " + glRenderer);
            }
            quirks.addQuirk( quirk );
        }
        if( glRenderer.contains( MesaRendererIntelsp ) &&
            vendorVersion.compareTo(mesaIntelBuggySharedCtx921) >= 0 && isX11 ) { // FIXME: When is it fixed ?
            final int quirk = GLRendererQuirks.GLSharedContextBuggy;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: X11 / Renderer " + glRenderer + " / Mesa-Version "+vendorVersion);
            }
            quirks.addQuirk( quirk );
        }
        if( glVendor.contains( "nouveau" )
            // FIXME: && vendorVersion.compareTo(nouveauBuggyMSAAFixed) < 0
          ) {
            final int quirk = GLRendererQuirks.NoMultiSamplingBuffers;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: X11 / Renderer " + glRenderer + " / Vendor "+glVendor);
            }
            addStickyQuirkAtMapping(adevice, quirks, quirk, withinGLVersionsMapping);
        }
        if( isWindows && glRenderer.contains("SVGA3D") && vendorVersion.compareTo(mesaSafeFBOVersion) < 0 ) {
            final int quirk = GLRendererQuirks.NoFullFBOSupport;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: OS "+Platform.getOSType() + " / Renderer " + glRenderer + " / Mesa-Version "+vendorVersion);
            }
            quirks.addQuirk( quirk );
        }
    }

    //
    // Property related quirks
    //
    if( FORCE_NO_COLOR_RENDERBUFFER ) {
        final int quirk = GLRendererQuirks.BuggyColorRenderbuffer;
        if(DEBUG) {
            System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: property");
        }
        quirks.addQuirk( quirk );
    }
    if( FORCE_MIN_FBO_SUPPORT || quirks.exist(GLRendererQuirks.BuggyColorRenderbuffer) ) {
        final int quirk = GLRendererQuirks.NoFullFBOSupport;
        if(DEBUG) {
            final String causeProps = FORCE_MIN_FBO_SUPPORT ? "property, " : "";
            final String causeQuirk = quirks.exist(GLRendererQuirks.BuggyColorRenderbuffer) ? "BuggyColorRenderbuffer" : "";
            System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: "+causeProps+causeQuirk);
        }
        quirks.addQuirk( quirk );
    }

    if(DEBUG) {
        System.err.println("Quirks local.0: "+quirks);
    }
    {
        // Merge sticky quirks, thread safe due to single threaded initialization!
        GLRendererQuirks.pushStickyDeviceQuirks(adevice, quirks);

        final AbstractGraphicsDevice factoryDefaultDevice = factory.getDefaultDevice();
        if( !GLRendererQuirks.areSameStickyDevice(factoryDefaultDevice, adevice) ) {
            GLRendererQuirks.pushStickyDeviceQuirks(factoryDefaultDevice, quirks);
        }
        if( isES ) {
            final AbstractGraphicsDevice eglFactoryDefaultDevice = GLDrawableFactory.getEGLFactory().getDefaultDevice();
            if( !GLRendererQuirks.areSameStickyDevice(eglFactoryDefaultDevice, adevice) &&
                !GLRendererQuirks.areSameStickyDevice(eglFactoryDefaultDevice, factoryDefaultDevice) ) {
                GLRendererQuirks.pushStickyDeviceQuirks(eglFactoryDefaultDevice, quirks);
            }
        }
    }
    glRendererQuirks = quirks;
    if(DEBUG) {
        System.err.println("Quirks local.X: "+glRendererQuirks);
        System.err.println("Quirks sticky on "+adevice+": "+GLRendererQuirks.getStickyDeviceQuirks(adevice));
    }
  }

  private static final boolean hasFBOImpl(final int major, final int ctp, final ExtensionAvailabilityCache extCache) {
    return ( 0 != (ctp & CTX_PROFILE_ES) && major >= 2 ) ||                           // ES >= 2.0

           major >= 3 ||                                                              // any >= 3.0 GL ctx (core, compat and ES)

           ( null != extCache &&
             (
               extCache.isExtensionAvailable(GLExtensions.ARB_ES2_compatibility)  ||  // ES 2.0 compatible

               extCache.isExtensionAvailable(GLExtensions.ARB_framebuffer_object) ||  // ARB_framebuffer_object

               extCache.isExtensionAvailable(GLExtensions.EXT_framebuffer_object) ||  // EXT_framebuffer_object

               extCache.isExtensionAvailable(GLExtensions.OES_framebuffer_object)     // OES_framebuffer_object
             ) );
  }

  private final void removeCachedVersion(final int major, final int minor, int ctxProfileBits) {
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
        isHardwareRasterizer = ! ( glRendererLowerCase.contains("software") /* Mesa3D, Apple */ ||
                                   glRendererLowerCase.contains("mesa x11") /* Mesa3D  */ ||
                                   glRendererLowerCase.contains("softpipe") /* Gallium */ ||
                                   glRendererLowerCase.contains("llvmpipe") /* Gallium */
                                 );
    }
    return isHardwareRasterizer;
  }

  protected abstract StringBuilder getPlatformExtensionsStringImpl();

  @Override
  public final boolean isFunctionAvailable(final String glFunctionName) {
    // Check GL 1st (cached)
    if( null != glProcAddressTable ) { // null if this context wasn't not created
        try {
            if( glProcAddressTable.isFunctionAvailable( glFunctionName ) ) {
                return true;
            }
        } catch (final Exception e) {}
    }

    // Check platform extensions 2nd (cached) - context had to be enabled once
    final ProcAddressTable pTable = getPlatformExtProcAddressTable();
    if(null!=pTable) {
        try {
            if( pTable.isFunctionAvailable( glFunctionName ) ) {
                return true;
            }
        } catch (final Exception e) {}
    }

    // dynamic function lookup at last incl name aliasing (not cached)
    final DynamicLookupHelper dynLookup = getGLDynamicLookupHelper(ctxVersion.getMajor(), ctxOptions);
    if( null == dynLookup ) {
        throw new GLException("No GLDynamicLookupHelper for "+this);
    }
    final String tmpBase = GLNameResolver.normalizeVEN(GLNameResolver.normalizeARB(glFunctionName, true), true);
    return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
        @Override
        public Boolean run() {
            boolean res = false;
            dynLookup.claimAllLinkPermission();
            try {
                final int  variants = GLNameResolver.getFuncNamePermutationNumber(tmpBase);
                for(int i = 0; !res && i < variants; i++) {
                    final String tmp = GLNameResolver.getFuncNamePermutation(tmpBase, i);
                    try {
                        res = dynLookup.isFunctionAvailable(tmp);
                    } catch (final Exception e) { }
                }
            } finally {
                dynLookup.releaseAllLinkPermission();
            }
            return Boolean.valueOf(res);
        } } ).booleanValue();
  }

  @Override
  public final boolean isExtensionAvailable(final String glExtensionName) {
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

  protected static String getContextFQN(final AbstractGraphicsDevice device, final int major, final int minor, int ctxProfileBits) {
      // remove non-key values
      ctxProfileBits &= CTX_IMPL_CACHE_MASK;

      return device.getUniqueID() + "-" + toHexString(composeBits(major, minor, ctxProfileBits));
  }

  protected final String getContextFQN() {
      return contextFQN;
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
    if(!pixelDataEvaluated) { // only valid while context is made current
        boolean ok = false;
        /* if(isGL2GL3() && 3 == components) {
            pixelDataInternalFormat=GL.GL_RGB;
            pixelDataFormat=GL.GL_RGB;
            pixelDataType = GL.GL_UNSIGNED_BYTE;
            ok = true;
        } else */ if( isGLES2Compatible() || isExtensionAvailable(GLExtensions.OES_read_format) ) {
            final int[] glImplColorReadVals = new int[] { 0, 0 };
            gl.glGetIntegerv(GL.GL_IMPLEMENTATION_COLOR_READ_FORMAT, glImplColorReadVals, 0);
            gl.glGetIntegerv(GL.GL_IMPLEMENTATION_COLOR_READ_TYPE, glImplColorReadVals, 1);
            // pixelDataInternalFormat = (4 == components) ? GL.GL_RGBA : GL.GL_RGB;
            pixelDataFormat = glImplColorReadVals[0];
            pixelDataType = glImplColorReadVals[1];
            ok = 0 != pixelDataFormat && 0 != pixelDataType;
        }
        if( !ok ) {
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

  //----------------------------------------------------------------------
  // SwapBuffer

  @Override
  public final boolean setSwapInterval(final int interval) throws GLException {
    validateCurrent();
    return setSwapIntervalNC(interval);
  }
  protected final boolean setSwapIntervalNC(final int interval) throws GLException {
    if( !drawableRetargeted ||
        !hasRendererQuirk(GLRendererQuirks.NoSetSwapIntervalPostRetarget)
      )
    {
        final Integer usedInterval = setSwapIntervalImpl2(interval);
        if( null != usedInterval ) {
            currentSwapInterval = usedInterval.intValue();
            return true;
        }
    }
    return false;
  }
  protected abstract Integer setSwapIntervalImpl2(final int interval);

  @Override
  public final int getSwapInterval() {
    return currentSwapInterval;
  }
  @Override
  protected final void setDefaultSwapInterval() {
    currentSwapInterval = 0;
    setSwapIntervalNC(1);
  }


  //----------------------------------------------------------------------
  // Helpers for buffer object optimizations

  public final GLBufferObjectTracker getBufferObjectTracker() {
    return bufferObjectTracker;
  }

  public final GLBufferStateTracker getBufferStateTracker() {
    return bufferStateTracker;
  }

  public final GLStateTracker getGLStateTracker() {
    return glStateTracker;
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
  public final boolean isOwner(final Thread thread) {
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
  public final void setBoundFramebuffer(final int target, final int framebufferName) {
      if(0 > framebufferName) {
          return; // ignore invalid name
      }
      switch(target) {
          case GL.GL_FRAMEBUFFER:
          case GL.GL_DRAW_FRAMEBUFFER:
              boundFBOTarget[0] = framebufferName; // draw
              break;
          case GL.GL_READ_FRAMEBUFFER:
              boundFBOTarget[1] = framebufferName; // read
              break;
          default: // ignore untracked target
      }
  }
  @Override
  public final int getBoundFramebuffer(final int target) {
      switch(target) {
          case GL.GL_FRAMEBUFFER:
          case GL.GL_DRAW_FRAMEBUFFER:
              return boundFBOTarget[0]; // draw
          case GL.GL_READ_FRAMEBUFFER:
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
  public final int getDefaultReadBuffer() { return drawable.getDefaultReadBuffer(gl, drawableRead != drawable); }

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
  public final void setContextCreationFlags(final int flags) {
      if(!isCreated()) {
          additionalCtxCreationFlags = flags & GLContext.CTX_OPTION_DEBUG;
      }
  }

  @Override
  public final boolean isGLDebugSynchronous() { return glDebugHandler.isSynchronous(); }

  @Override
  public final void setGLDebugSynchronous(final boolean synchronous) {
      glDebugHandler.setSynchronous(synchronous);
  }

  @Override
  public final void enableGLDebugMessage(final boolean enable) throws GLException {
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
  public final void addGLDebugListener(final GLDebugListener listener) {
      glDebugHandler.addListener(listener);
  }

  @Override
  public final void removeGLDebugListener(final GLDebugListener listener) {
      glDebugHandler.removeListener(listener);
  }

  @Override
  public final void glDebugMessageControl(final int source, final int type, final int severity, final int count, final IntBuffer ids, final boolean enabled) {
      if(glDebugHandler.isExtensionKHRARB()) {
          gl.getGL2ES2().glDebugMessageControl(source, type, severity, count, ids, enabled);
      } else if(glDebugHandler.isExtensionAMD()) {
          gl.getGL2GL3().glDebugMessageEnableAMD(GLDebugMessage.translateARB2AMDCategory(source, type), severity, count, ids, enabled);
      }
  }

  @Override
  public final void glDebugMessageControl(final int source, final int type, final int severity, final int count, final int[] ids, final int ids_offset, final boolean enabled) {
      if(glDebugHandler.isExtensionKHRARB()) {
          gl.getGL2ES2().glDebugMessageControl(source, type, severity, count, ids, ids_offset, enabled);
      } else if(glDebugHandler.isExtensionAMD()) {
          gl.getGL2GL3().glDebugMessageEnableAMD(GLDebugMessage.translateARB2AMDCategory(source, type), severity, count, ids, ids_offset, enabled);
      }
  }

  @Override
  public final void glDebugMessageInsert(final int source, final int type, final int id, final int severity, final String buf) {
      final int len = (null != buf) ? buf.length() : 0;
      if(glDebugHandler.isExtensionKHRARB()) {
          gl.getGL2ES2().glDebugMessageInsert(source, type, id, severity, len, buf);
      } else if(glDebugHandler.isExtensionAMD()) {
          gl.getGL2GL3().glDebugMessageInsertAMD(GLDebugMessage.translateARB2AMDCategory(source, type), severity, id, len, buf);
      }
  }

  /** Internal bootstraping glGetString(GL_RENDERER) */
  private static native String glGetStringInt(int name, long procAddress);

  /** Internal bootstraping glGetIntegerv(..) for version */
  private static native void glGetIntegervInt(int pname, int[] params, int params_offset, long procAddress);
}
