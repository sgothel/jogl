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
import javax.media.opengl.GL2ES3;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDebugListener;
import javax.media.opengl.GLDebugMessage;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
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

  private boolean pixelDataEvaluated;
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

      if ( !isInit && null != boundFBOTarget ) { // <init>: boundFBOTarget is not written yet
          boundFBOTarget[0] = 0; // draw
          boundFBOTarget[1] = 0; // read
      }

      pixelDataEvaluated = false;

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
      final GLDrawableImpl old = drawable;
      if( isCreated() && null != old && old.isRealized() ) {
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
          makeCurrent(true); // implicit: associateDrawable(true)
          if( !lockHeld ) {
              release(false);
          }
      }
      return old;
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
        Thread.dumpStack();
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

    if (res != CONTEXT_NOT_CURRENT) { // still locked!
      setCurrent(this);
      if(res == CONTEXT_CURRENT_NEW) {
        // check if the drawable's and the GL's GLProfile are equal
        // throws an GLException if not
        drawable.getGLProfile().verifyEquality(gl.getGLProfile());

        glDebugHandler.init( isGL2GL3() && isGLDebugEnabled() );

        if(DEBUG_GL) {
            setGL( GLPipelineFactory.create("javax.media.opengl.Debug", null, gl, null) );
            if(glDebugHandler.isEnabled()) {
                glDebugHandler.addListener(new GLDebugMessageHandler.StdErrGLDebugListener(true));
            }
        }
        if(TRACE_GL) {
            setGL( GLPipelineFactory.create("javax.media.opengl.Trace", null, gl, new Object[] { System.err } ) );
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
                if( rootGL.isGL2ES3() ) { // FIXME remove if ES2 == ES3 later
                    final GL2ES3 gl2es3 = rootGL.getGL2ES3();
                    gl2es3.glGenVertexArrays(1, tmp, 0);
                    defaultVAO = tmp[0];
                    gl2es3.glBindVertexArray(defaultVAO);
                }
            }
        } finally {
            if ( null != sharedMaster ) {
                sharedMaster.drawable.unlockSurface();
            }
        }
        if ( DEBUG_TRACE_SWITCH ) {
            System.err.println(getThreadName() + ": Create GL context "+(created?"OK":"FAILED")+": For " + getClass().getName()+" - "+getGLVersion()+" - "+getTraceSwitchMsg());
            // Thread.dumpStack();
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
                    GLContext.mapAvailableGLVersion(device, reqMajor, reqProfile, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                    // Perform all required profile mappings
                    if( isCompat ) {
                        // COMPAT via non ARB
                        GLContext.mapAvailableGLVersion(device, reqMajor, GLContext.CTX_PROFILE_CORE, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                        if( reqMajor >= 4 ) {
                            GLContext.mapAvailableGLVersion(device, 3, reqProfile, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                            GLContext.mapAvailableGLVersion(device, 3, GLContext.CTX_PROFILE_CORE, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                        }
                        if( reqMajor >= 3 ) {
                            GLContext.mapAvailableGLVersion(device, 2, reqProfile, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                        }
                    } else {
                        // CORE via non ARB, unlikely, however ..
                        if( reqMajor >= 4 ) {
                            GLContext.mapAvailableGLVersion(device, 3, reqProfile, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                        }
                    }
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
   * Platform dependent entry point for context creation.<br>
   *
   * This method is called from {@link #makeCurrentWithinLock()} .. {@link #makeCurrent()} .<br>
   *
   * The implementation shall verify this context with a
   * <code>MakeContextCurrent</code> call.<br>
   *
   * The implementation <b>must</b> leave the context current.<br>
   *
   * @param sharedWithHandle the shared context handle or 0
   * @return true if successful, or false
   * @throws GLException
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
    if( GLProfile.disableOpenGLARBContext ) {
        return 0;
    }
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
    GLContext.getRequestMajorAndCompat(glCaps.getGLProfile(), reqMajorCTP);

    if(DEBUG) {
        System.err.println(getThreadName() + ": createContextARB: Requested "+GLContext.getGLVersion(reqMajorCTP[0], 0, reqMajorCTP[0], null));
    }
    final int _major[] = { 0 };
    final int _minor[] = { 0 };
    final int _ctp[] = { 0 };
    long _ctx = 0;
    if( GLContext.getAvailableGLVersion(device, reqMajorCTP[0], reqMajorCTP[1],
                                        _major, _minor, _ctp)) {
        _ctp[0] |= additionalCtxCreationFlags;
        if(DEBUG) {
            System.err.println(getThreadName() + ": createContextARB: Mapped "+GLContext.getGLVersion(_major[0], _minor[0], _ctp[0], null));
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

  private final boolean mapGLVersions(final AbstractGraphicsDevice device) {
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
        if( Platform.OSType.MACOS == Platform.getOSType() &&
            Platform.getOSVersionNumber().compareTo(Platform.OSXVersion.Mavericks) >= 0 ) {
            /**
             * OSX 10.9 GLRendererQuirks.GL4NeedsGL3Request, quirk is added as usual @ setRendererQuirks(..)
             */
            if( !GLProfile.disableOpenGLCore && !hasGL4 && !hasGL3 ) {
                hasGL3   = createContextARBMapVersionsAvailable(3, CTX_PROFILE_CORE);    // GL3
                success |= hasGL3;
                if( hasGL3 ) {
                    final boolean isHWAccel = 0 == ( CTX_IMPL_ACCEL_SOFT & ctxOptions );
                    if( isHWAccel && ctxVersion.getMajor() >= 4 ) {
                        // Gotcha: Creating a '3.2' ctx delivers a >= 4 ctx.
                        GLContext.mapAvailableGLVersion(device, 4, CTX_PROFILE_CORE, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                        hasGL4   = true;
                        if(DEBUG) {
                            System.err.println("Quirk Triggerd: "+GLRendererQuirks.toString(GLRendererQuirks.GL4NeedsGL3Request)+": cause: OS "+Platform.getOSType()+", OS Version "+Platform.getOSVersionNumber());
                        }
                    }
                    resetStates(false); // clean this context states, since creation was temporary
                }
            }
        }
        if( !GLProfile.disableOpenGLCore ) {
            if( !hasGL4 ) {
                hasGL4   = createContextARBMapVersionsAvailable(4, CTX_PROFILE_CORE);    // GL4
                success |= hasGL4;
                if( hasGL4 ) {
                    if( 0 == ( CTX_IMPL_ACCEL_SOFT & ctxOptions ) ) {
                        // Map hw-accel GL4 to all lower core profiles: GL3
                        GLContext.mapAvailableGLVersion(device, 3, CTX_PROFILE_CORE, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                        if( PROFILE_ALIASING ) {
                            hasGL3   = true;
                        }
                    }
                    resetStates(false); // clean context states, since creation was temporary
                }
            }
            if( !hasGL3 ) {
                hasGL3   = createContextARBMapVersionsAvailable(3, CTX_PROFILE_CORE);    // GL3
                success |= hasGL3;
                if( hasGL3 ) {
                    resetStates(false); // clean this context states, since creation was temporary
                }
            }
        }
        if( !hasGL4bc ) {
            hasGL4bc = createContextARBMapVersionsAvailable(4, CTX_PROFILE_COMPAT);  // GL4bc
            success |= hasGL4bc;
            if( hasGL4bc ) {
                if( !hasGL4 ) { // last chance .. ignore hw-accel
                    GLContext.mapAvailableGLVersion(device, 4, CTX_PROFILE_CORE, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                    hasGL4   = true;
                }
                if( !hasGL3 ) { // last chance .. ignore hw-accel
                    GLContext.mapAvailableGLVersion(device, 3, CTX_PROFILE_CORE, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                    hasGL3   = true;
                }
                if( 0 == ( CTX_IMPL_ACCEL_SOFT & ctxOptions ) ) {
                    // Map hw-accel GL4bc to all lower compatible profiles: GL3bc, GL2
                    GLContext.mapAvailableGLVersion(device, 3, CTX_PROFILE_COMPAT, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                    GLContext.mapAvailableGLVersion(device, 2, CTX_PROFILE_COMPAT, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                    if(PROFILE_ALIASING) {
                        hasGL3bc = true;
                        hasGL2   = true;
                    }
                }
                resetStates(false); // clean this context states, since creation was temporary
            }
        }
        if( !hasGL3bc ) {
            hasGL3bc = createContextARBMapVersionsAvailable(3, CTX_PROFILE_COMPAT);  // GL3bc
            success |= hasGL3bc;
            if( hasGL3bc ) {
                if(!hasGL3) {  // last chance .. ignore hw-accel
                    GLContext.mapAvailableGLVersion(device, 3, CTX_PROFILE_CORE, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                    hasGL3   = true;
                }
                if( 0 == ( CTX_IMPL_ACCEL_SOFT & ctxOptions ) ) {
                    // Map hw-accel GL3bc to all lower compatible profiles: GL2
                    GLContext.mapAvailableGLVersion(device, 2, CTX_PROFILE_COMPAT, ctxVersion.getMajor(), ctxVersion.getMinor(), ctxOptions);
                    if(PROFILE_ALIASING) {
                        hasGL2   = true;
                    }
                }
                resetStates(false); // clean this context states, since creation was temporary
            }
        }
        if( !hasGL2 ) {
            hasGL2   = createContextARBMapVersionsAvailable(2, CTX_PROFILE_COMPAT);  // GL2
            success |= hasGL2;
            if( hasGL2 ) {
                resetStates(false); // clean this context states, since creation was temporary
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
   * Note: Since context creation is temporary, caller need to issue {@link #resetStates(boolean)}, if creation was successful, i.e. returns true.
   * This method does not reset the states, allowing the caller to utilize the state variables.
   **/
  private final boolean createContextARBMapVersionsAvailable(final int reqMajor, final int reqProfile) {
    long _context;
    int ctp = CTX_IS_ARB_CREATED | reqProfile;

    // To ensure GL profile compatibility within the JOGL application
    // we always try to map against the highest GL version,
    // so the user can always cast to the highest available one.
    int majorMax, minorMax;
    int majorMin, minorMin;
    final int major[] = new int[1];
    final int minor[] = new int[1];
    if( 4 == reqMajor ) {
        majorMax=4; minorMax=GLContext.getMaxMinor(ctp, majorMax);
        majorMin=4; minorMin=0;
    } else if( 3 == reqMajor ) {
        majorMax=3; minorMax=GLContext.getMaxMinor(ctp, majorMax);
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
        final AbstractGraphicsDevice device = drawable.getNativeSurface().getGraphicsConfiguration().getScreen().getDevice();
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

  private final long createContextARBVersions(final long share, final boolean direct, final int ctxOptionFlags,
                                              final int majorMax, final int minorMax,
                                              final int majorMin, final int minorMin,
                                              final int major[], final int minor[]) {
    major[0]=majorMax;
    minor[0]=minorMax;
    long _context=0;
    int i=0;

    do {
        if (DEBUG) {
            i++;
            System.err.println(getThreadName() + ": createContextARBVersions."+i+": share "+share+", direct "+direct+
                    ", version "+major[0]+"."+minor[0]+", major["+majorMin+".."+majorMax+"], minor["+minorMin+".."+minorMax+"]");
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

    } while ( ( major[0]>majorMin || major[0]==majorMin && minor[0] >minorMin ) &&  // #1 check whether version is above lower limit
              GLContext.decrementGLVersion(ctxOptionFlags, major, minor)            // #2 decrement version
            );
    if (DEBUG) {
        System.err.println(getThreadName() + ": createContextARBVersions.X: ctx "+toHexString(_context)+", share "+share+", direct "+direct+
                ", version "+major[0]+"."+minor[0]+", major["+majorMin+".."+majorMax+"], minor["+minorMin+".."+minorMax+"]");
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

  private Object createInstance(final GLProfile glp, final boolean glObject, final Object[] cstrArgs) {
      return ReflectionUtil.createInstance(glp.getGLCtor(glObject), cstrArgs);
  }

  private boolean verifyInstance(final GLProfile glp, final String suffix, final Object instance) {
      return ReflectionUtil.instanceOf(instance, glp.getGLImplBaseClassName()+suffix);
  }

  /** Create the GL for this context. */
  protected GL createGL(final GLProfile glp) {
    final GL gl = (GL) createInstance(glp, true, new Object[] { glp, this } );

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

  /**
   * Part of <code>GL_NV_vertex_array_range</code>.
   * <p>
   * Provides platform-independent access to the <code>wglAllocateMemoryNV</code> /
   * <code>glXAllocateMemoryNV</code>.
   * </p>
   */
  public abstract ByteBuffer glAllocateMemoryNV(int size, float readFrequency, float writeFrequency, float priority);

  /**
   * Part of <code>GL_NV_vertex_array_range</code>.
   * <p>
   * Provides platform-independent access to the <code>wglFreeMemoryNV</code> /
   * <code>glXFreeMemoryNV</code>.
   * </p>
   */
  public abstract void glFreeMemoryNV(ByteBuffer pointer);

  /** Maps the given "platform-independent" function name to a real function
      name. Currently this is only used to map "glAllocateMemoryNV" and
      associated routines to wglAllocateMemoryNV / glXAllocateMemoryNV. */
  protected final String mapToRealGLFunctionName(final String glFunctionName) {
    final Map<String, String> map = getFunctionNameMap();
    final String lookup = ( null != map ) ? map.get(glFunctionName) : null;
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
  protected final String mapToRealGLExtensionName(final String glExtensionName) {
    final Map<String, String> map = getExtensionNameMap();
    final String lookup = ( null != map ) ? map.get(glExtensionName) : null;
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
        @Override
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

  /**
   * Returns false if <code>glGetIntegerv</code> is inaccessible, otherwise queries major.minor
   * version for given arrays.
   * <p>
   * If the GL query fails, major will be zero.
   * </p>
   */
  private final boolean getGLIntVersion(final int[] glIntMajor, final int[] glIntMinor)  {
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
        glGetIntegervInt(GL2ES3.GL_MAJOR_VERSION, glIntMajor, 0, _glGetIntegerv);
        glGetIntegervInt(GL2ES3.GL_MINOR_VERSION, glIntMinor, 0, _glGetIntegerv);
        return true;
    }
  }

  protected final int getCtxOptions() {
      return ctxOptions;
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
   * @param major requested OpenGL major version
   * @param minor requested OpenGL minor version
   * @param ctxProfileBits OpenGL context profile and option bits, see {@link javax.media.opengl.GLContext#CTX_OPTION_ANY}
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
   * @see #setContextVersion
   * @see javax.media.opengl.GLContext#CTX_OPTION_ANY
   * @see javax.media.opengl.GLContext#CTX_PROFILE_COMPAT
   * @see javax.media.opengl.GLContext#CTX_IMPL_ES2_COMPAT
   */
  protected final boolean setGLFunctionAvailability(final boolean force, int major, int minor, int ctxProfileBits,
                                                    final boolean strictMatch, final boolean withinGLVersionsMapping) {
    if(null!=this.gl && null!=glProcAddressTable && !force) {
        return true; // already done and not forced
    }

    if ( 0 < major && !GLContext.isValidGLVersion(ctxProfileBits, major, minor) ) {
        throw new GLException("Invalid GL Version Request "+GLContext.getGLVersion(major, minor, ctxProfileBits, null));
    }

    if(null==this.gl || !verifyInstance(gl.getGLProfile(), "Impl", this.gl)) {
        setGL( createGL( drawable.getGLProfile() ) );
    }
    updateGLXProcAddressTable();

    final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration();
    final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
    final int reqCtxProfileBits = ctxProfileBits;
    final VersionNumber reqGLVersion = new VersionNumber(major, minor, 0);
    final VersionNumber hasGLVersionByString;
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
        final VersionNumber hasGLVersionByInt;
        {
            final int[] glIntMajor = new int[] { 0 }, glIntMinor = new int[] { 0 };
            final boolean getGLIntVersionOK = getGLIntVersion(glIntMajor, glIntMinor);
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
            hasGLVersionByInt = new VersionNumber(glIntMajor[0], glIntMinor[0], 0);
        }
        if (DEBUG) {
            System.err.println(getThreadName() + ": GLContext.setGLFuncAvail: Version verification (Int): String "+glVersion+", Number(Int) "+hasGLVersionByInt);
        }

        // Only validate integer based version if:
        //    - ctx >= 3.0 is requested _or_ string-version >= 3.0
        //    - _and_ a valid int version was fetched,
        // otherwise cont. w/ version-string method -> 3.0 > Version || Version > MAX!
        //
        if ( ( major >= 3 || hasGLVersionByString.compareTo(Version3_0) >= 0 ) &&
             GLContext.isValidGLVersion(ctxProfileBits, hasGLVersionByInt.getMajor(), hasGLVersionByInt.getMinor()) ) {
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
        ctxProfileBits &= ~ ( GLContext.CTX_IMPL_ES2_COMPAT | GLContext.CTX_IMPL_ES3_COMPAT ) ;
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
        glProcAddressTable = (ProcAddressTable) createInstance(gl.getGLProfile(), false,
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

    if( isES ) {
        if( major >= 3 ) {
            ctxProfileBits |= CTX_IMPL_ES3_COMPAT | CTX_IMPL_ES2_COMPAT ;
            ctxProfileBits |= CTX_IMPL_FBO;
        } else if( major >= 2 ) {
            ctxProfileBits |= CTX_IMPL_ES2_COMPAT;
            ctxProfileBits |= CTX_IMPL_FBO;
        }
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

  private final void setRendererQuirks(final AbstractGraphicsDevice adevice, final GLDrawableFactoryImpl factory,
                                       final int reqMajor, final int reqMinor, final int reqCTP,
                                       final int major, final int minor, final int ctp, final VersionNumberString vendorVersion,
                                       final boolean withinGLVersionsMapping) {
    final String MesaSP = "Mesa ";
    // final String MesaRendererAMDsp = " AMD ";
    final String MesaRendererIntelsp = "Intel(R)";
    final boolean hwAccel = 0 == ( ctp & GLContext.CTX_IMPL_ACCEL_SOFT );
    final boolean compatCtx = 0 != ( ctp & GLContext.CTX_PROFILE_COMPAT );
    final boolean esCtx = 0 != ( ctp & GLContext.CTX_PROFILE_ES );
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
    if( esCtx ) {
        if( 2 == reqMajor && 2 < major ) {
            final int quirk = GLRendererQuirks.GLES3ViaEGLES2Config;
            if(DEBUG) {
                System.err.println("Quirk: "+GLRendererQuirks.toString(quirk)+": cause: ES req "+reqMajor+" and 2 < "+major);
            }
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
            quirks.addQuirk( quirk );
            if( withinGLVersionsMapping ) {
                // Thread safe due to single threaded initialization!
                GLRendererQuirks.addStickyDeviceQuirk(adevice, quirk);
            }
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
            quirks.addQuirk( quirk );
            if( withinGLVersionsMapping ) {
                // Thread safe due to single threaded initialization!
                GLRendererQuirks.addStickyDeviceQuirk(adevice, quirk);
            }
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
        if( esCtx ) {
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

  /**
   * Updates the platform's 'GLX' function cache
   */
  protected abstract void updateGLXProcAddressTable();

  protected abstract StringBuilder getPlatformExtensionsStringImpl();

  @Override
  public final boolean isFunctionAvailable(final String glFunctionName) {
    // Check GL 1st (cached)
    if(null!=glProcAddressTable) { // null if this context wasn't not created
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
    final DynamicLookupHelper dynLookup = getDrawableImpl().getGLDynamicLookupHelper();
    final String tmpBase = GLNameResolver.normalizeVEN(GLNameResolver.normalizeARB(glFunctionName, true), true);
    boolean res = false;
    final int  variants = GLNameResolver.getFuncNamePermutationNumber(tmpBase);
    for(int i = 0; !res && i < variants; i++) {
        final String tmp = GLNameResolver.getFuncNamePermutation(tmpBase, i);
        try {
            res = dynLookup.isFunctionAvailable(tmp);
        } catch (final Exception e) { }
    }
    return res;
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
              boundFBOTarget[0] = framebufferName; // draw
              boundFBOTarget[1] = framebufferName; // read
              break;
          case GL2ES3.GL_DRAW_FRAMEBUFFER:
              boundFBOTarget[0] = framebufferName; // draw
              break;
          case GL2ES3.GL_READ_FRAMEBUFFER:
              boundFBOTarget[1] = framebufferName; // read
              break;
          default: // ignore untracked target
      }
  }
  @Override
  public final int getBoundFramebuffer(final int target) {
      switch(target) {
          case GL.GL_FRAMEBUFFER:
          case GL2ES3.GL_DRAW_FRAMEBUFFER:
              return boundFBOTarget[0]; // draw
          case GL2ES3.GL_READ_FRAMEBUFFER:
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
      if(glDebugHandler.isExtensionARB()) {
          gl.getGL2GL3().glDebugMessageControl(source, type, severity, count, ids, enabled);
      } else if(glDebugHandler.isExtensionAMD()) {
          gl.getGL2GL3().glDebugMessageEnableAMD(GLDebugMessage.translateARB2AMDCategory(source, type), severity, count, ids, enabled);
      }
  }

  @Override
  public final void glDebugMessageControl(final int source, final int type, final int severity, final int count, final int[] ids, final int ids_offset, final boolean enabled) {
      if(glDebugHandler.isExtensionARB()) {
          gl.getGL2GL3().glDebugMessageControl(source, type, severity, count, ids, ids_offset, enabled);
      } else if(glDebugHandler.isExtensionAMD()) {
          gl.getGL2GL3().glDebugMessageEnableAMD(GLDebugMessage.translateARB2AMDCategory(source, type), severity, count, ids, ids_offset, enabled);
      }
  }

  @Override
  public final void glDebugMessageInsert(final int source, final int type, final int id, final int severity, final String buf) {
      final int len = (null != buf) ? buf.length() : 0;
      if(glDebugHandler.isExtensionARB()) {
          gl.getGL2GL3().glDebugMessageInsert(source, type, id, severity, len, buf);
      } else if(glDebugHandler.isExtensionAMD()) {
          gl.getGL2GL3().glDebugMessageInsertAMD(GLDebugMessage.translateARB2AMDCategory(source, type), severity, id, len, buf);
      }
  }

  /** Internal bootstraping glGetString(GL_RENDERER) */
  private static native String glGetStringInt(int name, long procAddress);

  /** Internal bootstraping glGetIntegerv(..) for version */
  private static native void glGetIntegervInt(int pname, int[] params, int params_offset, long procAddress);
}
