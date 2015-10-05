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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLFBODrawable;
import com.jogamp.opengl.GLRunnable;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.InterruptedRuntimeException;
import com.jogamp.common.util.PropertyAccess;

/** Encapsulates the implementation of most of the GLAutoDrawable's
    methods to be able to share it between GLAutoDrawable implementations like GLAutoDrawableBase, GLCanvas and GLJPanel. */
public class GLDrawableHelper {
  /** true if property <code>jogl.debug.GLDrawable.PerfStats</code> is defined. */
  private static final boolean PERF_STATS;

  static {
      Debug.initSingleton();
      PERF_STATS = PropertyAccess.isPropertyDefined("jogl.debug.GLDrawable.PerfStats", true);
  }

  protected static final boolean DEBUG = GLDrawableImpl.DEBUG;
  private static final boolean DEBUG_SETCLEAR = GLContext.DEBUG_GL || DEBUG;

  private final Object listenersLock = new Object();
  private final ArrayList<GLEventListener> listeners = new ArrayList<GLEventListener>();
  private final HashSet<GLEventListener> listenersToBeInit = new HashSet<GLEventListener>();
  private final Object glRunnablesLock = new Object();
  private ArrayList<GLRunnableTask> glRunnables = new ArrayList<GLRunnableTask>();
  private volatile int glRunnableCount = 0;
  private boolean autoSwapBufferMode;
  private volatile Thread exclusiveContextThread;
  /** -1 release, 0 nop, 1 claim */
  private volatile int exclusiveContextSwitch;
  private GLAnimatorControl animatorCtrl;
  private static Runnable nop = new Runnable() { @Override public void run() {} };

  private GLContext sharedContext;
  private GLAutoDrawable sharedAutoDrawable;


  public GLDrawableHelper() {
    reset();
  }

  public final void reset() {
    synchronized(listenersLock) {
        listeners.clear();
        listenersToBeInit.clear();
    }
    autoSwapBufferMode = true;
    exclusiveContextThread = null;
    exclusiveContextSwitch = 0;
    synchronized(glRunnablesLock) {
        glRunnableCount = 0;
        glRunnables.clear();
    }
    animatorCtrl = null;
    sharedContext = null;
    sharedAutoDrawable = null;
  }

  public final void setSharedContext(final GLContext thisContext, final GLContext sharedContext) throws IllegalStateException {
      if( null == sharedContext ) {
          throw new IllegalStateException("Null shared GLContext");
      }
      if( thisContext == sharedContext ) {
          throw new IllegalStateException("Shared GLContext same as local");
      }
      if( null != this.sharedContext ) {
          throw new IllegalStateException("Shared GLContext already set");
      }
      if( null != this.sharedAutoDrawable ) {
          throw new IllegalStateException("Shared GLAutoDrawable already set");
      }
      this.sharedContext = sharedContext;
  }

  public final void setSharedAutoDrawable(final GLAutoDrawable thisAutoDrawable, final GLAutoDrawable sharedAutoDrawable) throws IllegalStateException {
      if( null == sharedAutoDrawable ) {
          throw new IllegalStateException("Null shared GLAutoDrawable");
      }
      if( thisAutoDrawable == sharedAutoDrawable ) {
          throw new IllegalStateException("Shared GLAutoDrawable same as this");
      }
      if( null != this.sharedContext ) {
          throw new IllegalStateException("Shared GLContext already set");
      }
      if( null != this.sharedAutoDrawable ) {
          throw new IllegalStateException("Shared GLAutoDrawable already set");
      }
      this.sharedAutoDrawable = sharedAutoDrawable;
  }

  /**
   * @param shared returns the shared GLContext, based on set shared GLAutoDrawable
   *               or GLContext. Maybe null if none is set.
   * @return true if initialization is pending due to a set shared GLAutoDrawable or GLContext
   *         which is not ready yet. Otherwise false.
   */
  public boolean isSharedGLContextPending(final GLContext[] shared) {
      final GLContext shareWith;
      final boolean pending;
      if ( null != sharedAutoDrawable ) {
          shareWith = sharedAutoDrawable.getContext();
          pending = null == shareWith || !shareWith.isCreated() || !sharedAutoDrawable.areAllGLEventListenerInitialized();
      } else {
          shareWith = sharedContext;
          pending = null != shareWith && !shareWith.isCreated();
      }
      shared[0] = shareWith;
      return pending;
  }

  @Override
  public final String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("GLAnimatorControl: "+animatorCtrl+", ");
    synchronized(listenersLock) {
        sb.append("GLEventListeners num "+listeners.size()+" [");
        for (int i=0; i < listeners.size(); i++) {
          final Object l = listeners.get(i);
          sb.append(l);
          sb.append("[init ");
          sb.append( !listenersToBeInit.contains(l) );
          sb.append("], ");
        }
    }
    sb.append("]");
    return sb.toString();
  }

  /** Limit release calls of {@link #forceNativeRelease(GLContext)} to {@value}. */
  private static final int MAX_RELEASE_ITER = 512;

  /**
   * Since GLContext's {@link GLContext#makeCurrent()} and {@link GLContext#release()}
   * is recursive, a call to {@link GLContext#release()} may not natively release the context.
   * <p>
   * This methods continues calling {@link GLContext#release()} until the context has been natively released.
   * </p>
   * @param ctx
   */
  public static final void forceNativeRelease(final GLContext ctx) {
      int releaseCount = 0;
      do {
          ctx.release();
          releaseCount++;
          if (DEBUG) {
              System.err.println("GLDrawableHelper.forceNativeRelease() #"+releaseCount+" -- currentThread "+Thread.currentThread()+" -> "+GLContext.getCurrent());
          }
      } while( MAX_RELEASE_ITER > releaseCount && ctx.isCurrent() );

      if( ctx.isCurrent() ) {
          throw new GLException("Context still current after "+MAX_RELEASE_ITER+" releases: "+ctx);
      }
  }

  /**
   * Switch {@link GLContext} / {@link GLDrawable} association.
   * <p>
   * Remarks:
   * <ul>
   *   <li>The <code>oldCtx</code> will be destroyed if <code>destroyPrevCtx</code> is <code>true</code>,
   *       otherwise disassociate <code>oldCtx</code> from <code>drawable</code>
   *       via {@link GLContext#setGLDrawable(GLDrawable, boolean) oldCtx.setGLDrawable(null, true);} including {@link GL#glFinish() glFinish()}.</li>
   *   <li>Reassociate <code>newCtx</code> with <code>drawable</code>
   *       via {@link GLContext#setGLDrawable(GLDrawable, boolean) newCtx.setGLDrawable(drawable, true);}.</li>
   *   <li>If the old context was current on this thread, it is being released after disassociating the drawable.</li>
   *   <li>If the new context was current on this thread, it is being released before associating the drawable
   *       and made current afterwards.</li>
   *   <li>Implementation may issue {@link #makeCurrent()} and {@link #release()} while drawable reassociation.</li>
   *   <li>The user shall take extra care of thread synchronization,
   *       i.e. lock the involved {@link GLDrawable#getNativeSurface() drawable's} {@link NativeSurface}s
   *       to avoid a race condition. In case {@link GLAutoDrawable auto-drawable's} are used,
   *       their {@link GLAutoDrawable#getUpstreamLock() upstream-lock} must be locked beforehand
   *       see <a href="../../com/jogamp/opengl/GLAutoDrawable.html#locking">GLAutoDrawable Locking</a>.</li>
   * </ul>
   * </p>
   *
   * @param drawable the drawable which context is changed
   * @param oldCtx the old context, maybe <code>null</code>.
   * @param destroyOldCtx if <code>true</code>, destroy the <code>oldCtx</code>
   * @param newCtx the new context, maybe <code>null</code> for dis-association.
   * @param newCtxCreationFlags additional creation flags if newCtx is not null and not been created yet, see {@link GLContext#setContextCreationFlags(int)}
   *
   * @see GLAutoDrawable#setContext(GLContext, boolean)
   */
  public static final void switchContext(final GLDrawable drawable, final GLContext oldCtx, final boolean destroyOldCtx, final GLContext newCtx, final int newCtxCreationFlags) {
      if( null != oldCtx ) {
          if( destroyOldCtx ) {
              oldCtx.destroy();
          } else {
              oldCtx.setGLDrawable(null, true); // dis-associate old pair
          }
      }

      if(null!=newCtx) {
          newCtx.setContextCreationFlags(newCtxCreationFlags);
          newCtx.setGLDrawable(drawable, true); // re-associate new pair
      }
  }

  /**
   * If the drawable is not realized, OP is a NOP.
   * <ul>
   *  <li>release context if current</li>
   *  <li>destroy old drawable</li>
   *  <li>create new drawable</li>
   *  <li>attach new drawable to context</li>
   *  <li>make context current, if it was current</li>
   * </ul>
   * <p>
   * Locking is performed via {@link GLContext#makeCurrent()} on the passed <code>context</code>.
   * </p>
   *
   * @param drawable
   * @param context maybe null
   * @return the new drawable
   */
  public static final GLDrawableImpl recreateGLDrawable(GLDrawableImpl drawable, final GLContext context) {
      if( ! drawable.isRealized() ) {
          return drawable;
      }
      final GLContext currentContext = GLContext.getCurrent();
      final GLDrawableFactory factory = drawable.getFactory();
      final NativeSurface surface = drawable.getNativeSurface();
      final ProxySurface proxySurface = (surface instanceof ProxySurface) ? (ProxySurface)surface : null;

      if( null != context ) {
          // Ensure to sync GL command stream
          if( currentContext != context ) {
              context.makeCurrent();
          }
          context.setGLDrawable(null, true); // dis-associate
      }

      if(null != proxySurface) {
          proxySurface.enableUpstreamSurfaceHookLifecycle(false);
      }
      try {
          drawable.setRealized(false);
          drawable = (GLDrawableImpl) factory.createGLDrawable(surface); // [2]
          drawable.setRealized(true);
      } finally {
          if(null != proxySurface) {
              proxySurface.enableUpstreamSurfaceHookLifecycle(true);
          }
      }

      if(null != context) {
          context.setGLDrawable(drawable, true); // re-association, implicit glFinish() ctx/drawable sync
      }

      if( null != currentContext ) {
          currentContext.makeCurrent();
      }
      return drawable;
  }

  /**
   * Performs resize operation on the given drawable, assuming it is offscreen.
   * <p>
   * The {@link GLDrawableImpl}'s {@link NativeSurface} is being locked during operation.
   * In case the holder is an auto drawable or similar, it's lock shall be claimed by the caller.
   * </p>
   * <p>
   * May recreate the drawable via {@link #recreateGLDrawable(GLDrawableImpl, GLContext)}
   * in case of a a pbuffer- or pixmap-drawable.
   * </p>
   * <p>
   * FBO drawables are resized w/o drawable destruction.
   * </p>
   * <p>
   * Offscreen resize operation is validated w/ drawable size in the end.
   * An exception is thrown if not successful.
   * </p>
   *
   * @param drawable
   * @param context
   * @param newWidth the new width, it's minimum is capped to 1
   * @param newHeight the new height, it's minimum is capped to 1
   * @return the new drawable in case of an pbuffer/pixmap drawable, otherwise the passed drawable is being returned.
   * @throws NativeWindowException is drawable is not offscreen or it's surface lock couldn't be claimed
   * @throws GLException may be thrown a resize operation
   */
  public static final GLDrawableImpl resizeOffscreenDrawable(GLDrawableImpl drawable, final GLContext context, int newWidth, int newHeight)
          throws NativeWindowException, GLException
  {
      final NativeSurface ns = drawable.getNativeSurface();
      final int lockRes = ns.lockSurface();
      if ( NativeSurface.LOCK_SURFACE_NOT_READY >= lockRes ) {
          throw new NativeWindowException("Could not lock surface of drawable: "+drawable);
      }
      boolean validateSize = true;
      try {
          if( ! drawable.isRealized() ) {
              return drawable;
          }
          if( drawable.getChosenGLCapabilities().isOnscreen() ) {
              throw new NativeWindowException("Drawable is not offscreen: "+drawable);
          }
          if( DEBUG && ( 0>=newWidth || 0>=newHeight) ) {
              System.err.println("WARNING: Odd size detected: "+newWidth+"x"+newHeight+", using safe size 1x1. Drawable "+drawable);
              ExceptionUtils.dumpStack(System.err);
          }
          if( 0 >= newWidth )  { newWidth = 1; validateSize=false; }
          if( 0 >= newHeight ) { newHeight = 1; validateSize=false; }
          // propagate new size
          if( ns instanceof ProxySurface ) {
              final ProxySurface ps = (ProxySurface) ns;
              final UpstreamSurfaceHook ush = ps.getUpstreamSurfaceHook();
              if(ush instanceof UpstreamSurfaceHook.MutableSize) {
                  ((UpstreamSurfaceHook.MutableSize)ush).setSurfaceSize(newWidth, newHeight);
              } else if(DEBUG) { // we have to assume UpstreamSurfaceHook contains the new size already, hence size check @ bottom
                  System.err.println("GLDrawableHelper.resizeOffscreenDrawable: Drawable's offscreen ProxySurface n.a. UpstreamSurfaceHook.MutableSize, but "+ush.getClass().getName()+": "+ush);
              }
          } else if(DEBUG) { // we have to assume surface contains the new size already, hence size check @ bottom
              System.err.println("GLDrawableHelper.resizeOffscreenDrawable: Drawable's offscreen surface n.a. ProxySurface, but "+ns.getClass().getName()+": "+ns);
          }
          if( drawable instanceof GLFBODrawable ) {
              if( null != context && context.isCreated() ) {
                  ((GLFBODrawable) drawable).resetSize(context.getGL());
              }
          } else {
              drawable = GLDrawableHelper.recreateGLDrawable(drawable, context);
          }
      } finally {
          ns.unlockSurface();
      }
      if( validateSize && ( drawable.getSurfaceWidth() != newWidth || drawable.getSurfaceHeight() != newHeight ) ) {
          throw new InternalError("Incomplete resize operation: expected "+newWidth+"x"+newHeight+", has: "+drawable);
      }
      return drawable;
  }

  public final void addGLEventListener(final GLEventListener listener) {
    addGLEventListener(-1, listener);
  }

  public final void addGLEventListener(int index, final GLEventListener listener) {
    synchronized(listenersLock) {
        if(0>index) {
            index = listeners.size();
        }
        // GLEventListener may be added after context is created,
        // hence we earmark initialization for the next display call.
        listenersToBeInit.add(listener);

        listeners.add(index, listener);
    }
  }

  /**
   * Note that no {@link GLEventListener#dispose(GLAutoDrawable)} call is being issued
   * due to the lack of a current context.
   * Consider calling {@link #disposeGLEventListener(GLAutoDrawable, GLDrawable, GLContext, GLEventListener)}.
   * @return the removed listener, or null if listener was not added
   */
  public final GLEventListener removeGLEventListener(final GLEventListener listener) {
    synchronized(listenersLock) {
        listenersToBeInit.remove(listener);
        return listeners.remove(listener) ? listener : null;
    }
  }

  public final GLEventListener removeGLEventListener(int index) throws IndexOutOfBoundsException {
    synchronized(listenersLock) {
        if(0>index) {
            index = listeners.size()-1;
        }
        final GLEventListener listener = listeners.remove(index);
        listenersToBeInit.remove(listener);
        return listener;
    }
  }

  public final int getGLEventListenerCount() {
    synchronized(listenersLock) {
        return listeners.size();
    }
  }

  public final GLEventListener getGLEventListener(int index) throws IndexOutOfBoundsException {
    synchronized(listenersLock) {
        if(0>index) {
            index = listeners.size()-1;
        }
        return listeners.get(index);
    }
  }

  public final boolean areAllGLEventListenerInitialized() {
    synchronized(listenersLock) {
        return 0 == listenersToBeInit.size();
    }
  }

  public final boolean getGLEventListenerInitState(final GLEventListener listener) {
    synchronized(listenersLock) {
        return !listenersToBeInit.contains(listener);
    }
  }

  public final void setGLEventListenerInitState(final GLEventListener listener, final boolean initialized) {
    synchronized(listenersLock) {
        if(initialized) {
            listenersToBeInit.remove(listener);
        } else {
            listenersToBeInit.add(listener);
        }
    }
  }

  /**
   * Disposes the given {@link GLEventListener} via {@link GLEventListener#dispose(GLAutoDrawable)}
   * if it has been initialized and added to this queue.
   * <p>
   * If <code>remove</code> is <code>true</code>, the {@link GLEventListener} is removed from this drawable queue before disposal,
   * otherwise marked uninitialized.
   * </p>
   * <p>
   * Please consider using {@link #disposeGLEventListener(GLAutoDrawable, GLDrawable, GLContext, GLEventListener)}
   * for correctness, i.e. encapsulating all calls w/ makeCurrent etc.
   * </p>
   * @param autoDrawable
   * @param remove if true, the listener gets removed
   * @return the disposed and/or removed listener, otherwise null if neither action is performed
   */
  public final GLEventListener disposeGLEventListener(final GLAutoDrawable autoDrawable, final GLEventListener listener, final boolean remove) {
      synchronized(listenersLock) {
          if( remove ) {
              if( listeners.remove(listener) ) {
                  if( !listenersToBeInit.remove(listener) ) {
                      listener.dispose(autoDrawable);
                  }
                  return listener;
              }
          } else {
              if( listeners.contains(listener) && !listenersToBeInit.contains(listener) ) {
                  listener.dispose(autoDrawable);
                  listenersToBeInit.add(listener);
                  return listener;
              }
          }
      }
      return null;
  }

  /**
   * Disposes all added initialized {@link GLEventListener}s via {@link GLEventListener#dispose(GLAutoDrawable)}.
   * <p>
   * If <code>remove</code> is <code>true</code>, the {@link GLEventListener}s are removed from this drawable queue before disposal,
   * otherwise maked uninitialized.
   * </p>
   * <p>
   * Please consider using {@link #disposeAllGLEventListener(GLAutoDrawable, GLContext, boolean)}
   * or {@link #disposeGL(GLAutoDrawable, GLContext, boolean)}
   * for correctness, i.e. encapsulating all calls w/ makeCurrent etc.
   * </p>
   * @param autoDrawable
   * @return the disposal count
   * @throws GLException caused by {@link GLEventListener#dispose(GLAutoDrawable)}
   */
  public final int disposeAllGLEventListener(final GLAutoDrawable autoDrawable, final boolean remove) throws GLException {
    Throwable firstCaught = null;
    int disposeCount = 0;
    synchronized(listenersLock) {
        if( remove ) {
            for (int count = listeners.size(); 0 < count && 0 < listeners.size(); count--) {
              final GLEventListener listener = listeners.remove(0);
              if( !listenersToBeInit.remove(listener) ) {
                  try {
                      listener.dispose(autoDrawable);
                  } catch (final Throwable t) {
                      if( null == firstCaught ) {
                          firstCaught = t;
                      } else {
                          ExceptionUtils.dumpThrowable("subsequent", t);
                      }
                  }
                  disposeCount++;
              }
            }
        } else {
            for (int i = 0; i < listeners.size(); i++) {
              final GLEventListener listener = listeners.get(i);
              if( !listenersToBeInit.contains(listener) ) {
                  try {
                      listener.dispose(autoDrawable);
                  } catch (final Throwable t) {
                      if( null == firstCaught ) {
                          firstCaught = t;
                      } else {
                          ExceptionUtils.dumpThrowable("subsequent", t);
                      }
                  }
                  listenersToBeInit.add(listener);
                  disposeCount++;
              }
            }
        }
    }
    if( null != firstCaught ) {
        flushGLRunnables();
        throw GLException.newGLException(firstCaught);
    }
    return disposeCount;
  }

  /**
   * Principal helper method which runs {@link #disposeGLEventListener(GLAutoDrawable, GLEventListener, boolean)}
   * with the context made current.
   * <p>
   * If an {@link GLAnimatorControl} is being attached and the current thread is different
   * than {@link GLAnimatorControl#getThread() the animator's thread}, it is paused during the operation.
   * </p>
   *
   * @param autoDrawable
   * @param context
   * @param listener
   * @param initAction
   */
  public final GLEventListener disposeGLEventListener(final GLAutoDrawable autoDrawable,
                                                      final GLDrawable drawable,
                                                      final GLContext context,
                                                      final GLEventListener listener,
                                                      final boolean remove) {
      synchronized(listenersLock) {
          // fast path for uninitialized listener
          if( listenersToBeInit.contains(listener) ) {
             if( remove ) {
                 listenersToBeInit.remove(listener);
                 return listeners.remove(listener) ? listener : null;
             }
             return null;
          }
      }
      final boolean isPaused = isAnimatorAnimatingOnOtherThread() && animatorCtrl.pause();
      final GLEventListener[] res = new GLEventListener[] { null };
      final Runnable action = new Runnable() {
          @Override
          public void run() {
              res[0] = disposeGLEventListener(autoDrawable, listener, remove);
          }
      };
      invokeGL(drawable, context, action, nop);

      if(isPaused) {
          animatorCtrl.resume();
      }
      return res[0];
  }

  /**
   * Principal helper method which runs {@link #disposeAllGLEventListener(GLAutoDrawable, boolean)}
   * with the context made current.
   * <p>
   * If an {@link GLAnimatorControl} is being attached and the current thread is different
   * than {@link GLAnimatorControl#getThread() the animator's thread}, it is paused during the operation.
   * </p>
   *
   * @param autoDrawable
   * @param context
   * @param remove
   */
  public final void disposeAllGLEventListener(final GLAutoDrawable autoDrawable,
                                              final GLDrawable drawable,
                                              final GLContext context,
                                              final boolean remove) {

      final boolean isPaused = isAnimatorAnimatingOnOtherThread() && animatorCtrl.pause();

      final Runnable action = new Runnable() {
          @Override
          public void run() {
              disposeAllGLEventListener(autoDrawable, remove);
          }
      };
      invokeGL(drawable, context, action, nop);

      if(isPaused) {
          animatorCtrl.resume();
      }
  }

  private final void init(final GLEventListener l, final GLAutoDrawable drawable, final boolean sendReshape) {
      l.init(drawable);
      if(sendReshape) {
          l.reshape(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
      }
  }

  /**
   * The default init action to be called once after ctx is being created @ 1st makeCurrent().
   * @param sendReshape set to true if the subsequent display call won't reshape, otherwise false to avoid double reshape.
   **/
  public final void init(final GLAutoDrawable drawable, final boolean sendReshape) {
    setViewportAndClear(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
    synchronized(listenersLock) {
        final ArrayList<GLEventListener> _listeners = listeners;
        final int listenerCount = _listeners.size();
        if( listenerCount > 0 ) {
            for (int i=0; i < listenerCount; i++) {
              final GLEventListener listener = _listeners.get(i) ;

              // If make ctx current, invoked by invokGL(..), results in a new ctx, init gets called.
              // This may happen not just for initial setup, but for ctx recreation due to resource change (drawable/window),
              // hence it must be called unconditional, always.
              listenersToBeInit.remove(listener); // remove if exist, avoiding dbl init
              init(listener, drawable, sendReshape);
            }
        }
    }
  }

  public final void display(final GLAutoDrawable drawable) {
    displayImpl(drawable);
    // runForAllGLEventListener(drawable, displayAction);
    if( glRunnableCount > 0 && !execGLRunnables(drawable) ) { // glRunnableCount volatile OK; execGL.. only executed if size > 0
        displayImpl(drawable);
        // runForAllGLEventListener(drawable, displayAction);
    }
  }
  private final void displayImpl(final GLAutoDrawable drawable) {
      synchronized(listenersLock) {
          final ArrayList<GLEventListener> _listeners = listeners;
          final int listenerCount = _listeners.size();
          for (int i=0; i < listenerCount; i++) {
            final GLEventListener listener = _listeners.get(i) ;
            // GLEventListener may need to be init,
            // in case this one is added after the realization of the GLAutoDrawable
            if( listenersToBeInit.remove(listener) ) {
                init( listener, drawable, true /* sendReshape */ );
            }
            listener.display(drawable);
          }
      }
  }

  public static interface GLEventListenerAction {
      public void run(final GLAutoDrawable drawable, final GLEventListener listener);
  }
  /**
  private static GLEventListenerAction displayAction = new GLEventListenerAction() {
      public void run(final GLAutoDrawable drawable, final GLEventListener listener) {
          listener.display(drawable);
      }  }; */

  public final void runForAllGLEventListener(final GLAutoDrawable drawable, final GLEventListenerAction action) {
      synchronized(listenersLock) {
          final ArrayList<GLEventListener> _listeners = listeners;
          final int listenerCount = _listeners.size();
          for (int i=0; i < listenerCount; i++) {
            final GLEventListener listener = _listeners.get(i) ;
            // GLEventListener may need to be init,
            // in case this one is added after the realization of the GLAutoDrawable
            if( listenersToBeInit.remove(listener) ) {
                init( listener, drawable, true /* sendReshape */ );
            }
            action.run(drawable, listener);
          }
      }
  }

  /**
   * Bug 1206: Security: Clear exposed framebuffer after creation and before visibility
   * - Clear framebuffer after setting viewport
   * - Since we only attempt to help against leaking un-initialized framebuffer content
   *   not against user-app faults, we do not clear a 2nd-buffer (double-buffering).
   */
  private final void setViewportAndClear(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
      final GL gl = drawable.getGL();
      if( DEBUG_SETCLEAR ) {
          final int glerr0 = gl.glGetError();
          if( GL.GL_NO_ERROR != glerr0 ) {
              System.err.println("Info: GLDrawableHelper.reshape: pre-exisiting GL error 0x"+Integer.toHexString(glerr0));
              ExceptionUtils.dumpStack(System.err);
          }
      }
      gl.glViewport(x, y, width, height);
      gl.glClear(GL.GL_COLOR_BUFFER_BIT);
  }

  public final void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
    setViewportAndClear(drawable, x, y, width, height);
    synchronized(listenersLock) {
        for (int i=0; i < listeners.size(); i++) {
            final GLEventListener l = listeners.get(i);
            // GLEventListener may need to be init,
            // in case this one is added after the realization of the GLAutoDrawable
            if( listenersToBeInit.remove(l) ) {
                l.init(drawable);
            }
            l.reshape(drawable, x, y, width, height);
        }
    }
  }

  private final boolean execGLRunnables(final GLAutoDrawable drawable) { // glRunnables.size()>0
    // swap one-shot list asap
    final ArrayList<GLRunnableTask> _glRunnables;
    synchronized(glRunnablesLock) {
        if( glRunnables.size() > 0 ) {
            glRunnableCount = 0;
            _glRunnables = glRunnables;
            glRunnables = new ArrayList<GLRunnableTask>();
        } else {
            return true;
        }
    }
    boolean res = true;
    for (int i=0; i < _glRunnables.size(); i++) {
        res = _glRunnables.get(i).run(drawable) && res;
    }
    return res;
  }

  public final void flushGLRunnables() {
    synchronized(glRunnablesLock) {
        glRunnableCount = 0;
        while( glRunnables.size() > 0 ) {
            glRunnables.remove(0).flush();
        }
    }
  }

  public final void setAnimator(final GLAnimatorControl animator) throws GLException {
    synchronized(glRunnablesLock) {
        if(animatorCtrl!=animator && null!=animator && null!=animatorCtrl) {
            throw new GLException("Trying to register GLAnimatorControl "+animator+", where "+animatorCtrl+" is already registered. Unregister first.");
        }
        animatorCtrl = animator;
    }
  }

  public final GLAnimatorControl getAnimator() {
    synchronized(glRunnablesLock) {
        return animatorCtrl;
    }
  }

  public final boolean isAnimatorStartedOnOtherThread() {
    return ( null != animatorCtrl ) ? animatorCtrl.isStarted() && animatorCtrl.getThread() != Thread.currentThread() : false ;
  }

  public final boolean isAnimatorStarted() {
    return ( null != animatorCtrl ) ? animatorCtrl.isStarted() : false ;
  }

  public final boolean isAnimatorAnimatingOnOtherThread() {
    return ( null != animatorCtrl ) ? animatorCtrl.isAnimating() && animatorCtrl.getThread() != Thread.currentThread() : false ;
  }

  public final boolean isAnimatorAnimating() {
    return ( null != animatorCtrl ) ? animatorCtrl.isAnimating() : false ;
  }

  public static final boolean isLockedByOtherThread(final GLAutoDrawable d) {
      final Thread currentThread = Thread.currentThread();
      final Thread upstreamLockOwner = d.getUpstreamLock().getOwner();
      if( null != upstreamLockOwner && currentThread != upstreamLockOwner ) {
          return true;
      } else {
          final NativeSurface s = d.getNativeSurface();
          final Thread surfaceLockOwner = null != s ? s.getSurfaceLockOwner() : null;
          return null != surfaceLockOwner  && currentThread != surfaceLockOwner;
      }
  }

  public static final boolean isLockedByThisThread(final GLAutoDrawable d) {
      final Thread currentThread = Thread.currentThread();
      final Thread upstreamLockOwner = d.getUpstreamLock().getOwner();
      if( currentThread == upstreamLockOwner ) {
          return true;
      } else {
          final NativeSurface s = d.getNativeSurface();
          final Thread surfaceLockOwner = null != s ? s.getSurfaceLockOwner() : null;
          return currentThread == surfaceLockOwner;
      }
  }

  /**
   * <p>
   * If <code>wait</code> is <code>true</code> the call blocks until the <code>glRunnable</code>
   * has been executed.<p>
   * <p>
   * If <code>wait</code> is <code>true</code> <b>and</b>
   * {@link GLDrawable#isRealized()} returns <code>false</code> <i>or</i> {@link GLAutoDrawable#getContext()} returns <code>null</code>,
   * the call is ignored and returns <code>false</code>.<br>
   * This helps avoiding deadlocking the caller.
   * </p>
   * <p>
   * <pre>
   * 0 == deferredHere && 0 == isGLThread -> display() will issue on GL thread, blocking!
   *
   *         deferredHere wait isGLThread   lockedByThisThread  Note
   *  OK     0            x    1            x
   *  OK     0            x    0            0
   *  ERROR  0            x    0            1                   Will be deferred on GL thread by display() (blocking),
   *                                                            but locked by this thread -> ERROR
   *
   *         1            0    x            x                   All good, due to no wait, non blocking
   *
   *         1            1    1            0
   *         1            1    0            0
   *  SWITCH 1            1    1            1                   Run immediately, don't defer since locked by this thread, but isGLThread
   *  ERROR  1            1    0            1                   Locked by this thread, but _not_ isGLThread -> ERROR
   * </pre>
   * </p>
   *
   * @param drawable the {@link GLAutoDrawable} to be used
   * @param wait if <code>true</code> block until execution of <code>glRunnable</code> is finished, otherwise return immediatly w/o waiting
   * @param glRunnable the {@link GLRunnable} to execute within {@link #display()}
   * @return <code>true</code> if the {@link GLRunnable} has been processed or queued, otherwise <code>false</code>.
   * @throws IllegalStateException in case the drawable is locked by this thread, no animator is running on another thread and <code>wait</code> is <code>true</code>.
   */
  public final boolean invoke(final GLAutoDrawable drawable, boolean wait, final GLRunnable glRunnable) throws IllegalStateException {
    if( null == glRunnable || null == drawable ||
        wait && ( !drawable.isRealized() || null==drawable.getContext() ) ) {
        return false;
    }

    final GLRunnableTask rTask;
    final Object rTaskLock = new Object();
    synchronized(rTaskLock) {
        boolean deferredHere;
        synchronized(glRunnablesLock) {
            final boolean isGLThread = drawable.isThreadGLCapable();
            deferredHere = isAnimatorAnimatingOnOtherThread();
            if( deferredHere ) {
                if( wait && isLockedByThisThread(drawable) ) {
                    if( isGLThread ) {
                        // Run immediately, don't defer since locked by this thread, but isGLThread
                        deferredHere = false;
                        wait = false;
                    } else {
                        // Locked by this thread, but _not_ isGLThread -> ERROR
                        throw new IllegalStateException("Deferred, wait, isLocked on current and not GL-Thread: thread "+Thread.currentThread());
                    }
                }
            } else {
                if( !isGLThread && isLockedByThisThread(drawable) ) {
                    // Will be deferred on GL thread by display() (blocking), but locked by this thread -> ERROR
                    throw new IllegalStateException("Not deferred, isLocked on current and not GL-Thread: thread "+Thread.currentThread());
                }
                wait = false; // don't wait if exec immediately
            }
            rTask = new GLRunnableTask(glRunnable,
                                       wait ? rTaskLock : null,
                                       wait  /* catch Exceptions if waiting for result */);
            glRunnableCount++;
            glRunnables.add(rTask);
        }
        if( !deferredHere ) {
            drawable.display();
        } else if( wait ) {
            try {
                while( rTask.isInQueue() ) {
                    rTaskLock.wait(); // free lock, allow execution of rTask
                }
            } catch (final InterruptedException ie) {
                throw new InterruptedRuntimeException(ie);
            }
            final Throwable throwable = rTask.getThrowable();
            if(null!=throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }
    return true;
  }

  /**
   * @see #invoke(GLAutoDrawable, boolean, GLRunnable)
   *
   * @param drawable
   * @param wait
   * @param newGLRunnables
   * @return
   * @throws IllegalStateException
   */
  public final boolean invoke(final GLAutoDrawable drawable, boolean wait, final List<GLRunnable> newGLRunnables) throws IllegalStateException {
    if( null == newGLRunnables || newGLRunnables.size() == 0 || null == drawable ||
        wait && ( !drawable.isRealized() || null==drawable.getContext() ) ) {
        return false;
    }

    final int count = newGLRunnables.size();
    final GLRunnableTask rTask;
    final Object rTaskLock = new Object();
    synchronized(rTaskLock) {
        boolean deferredHere;
        synchronized(glRunnablesLock) {
            final boolean isGLThread = drawable.isThreadGLCapable();
            deferredHere = isAnimatorAnimatingOnOtherThread();
            if( deferredHere ) {
                if( wait && isLockedByThisThread(drawable) ) {
                    if( isGLThread ) {
                        // Run immediately, don't defer since locked by this thread, but isGLThread
                        deferredHere = false;
                        wait = false;
                    } else {
                        // Locked by this thread, but _not_ isGLThread -> ERROR
                        throw new IllegalStateException("Deferred, wait, isLocked on current and not GL-Thread: thread "+Thread.currentThread());
                    }
                }
            } else {
                if( !isGLThread && isLockedByThisThread(drawable) ) {
                    // Will be deferred on GL thread by display() (blocking), but locked by this thread -> ERROR
                    throw new IllegalStateException("Not deferred, isLocked on current and not GL-Thread: thread "+Thread.currentThread());
                }
                wait = false; // don't wait if exec immediately
            }
            for(int i=0; i<count-1; i++) {
                glRunnableCount++;
                glRunnables.add( new GLRunnableTask(newGLRunnables.get(i), null, false) );
            }
            rTask = new GLRunnableTask(newGLRunnables.get(count-1),
                                       wait ? rTaskLock : null,
                                       wait  /* catch Exceptions if waiting for result */);
            glRunnableCount++;
            glRunnables.add(rTask);
        }
        if( !deferredHere ) {
            drawable.display();
        } else if( wait ) {
            try {
                while( rTask.isInQueue() ) {
                    rTaskLock.wait(); // free lock, allow execution of rTask
                }
            } catch (final InterruptedException ie) {
                throw new InterruptedRuntimeException(ie);
            }
            final Throwable throwable = rTask.getThrowable();
            if(null!=throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }
    return true;
  }

  public final void enqueue(final GLRunnable glRunnable) {
    if( null == glRunnable) {
        return;
    }
    synchronized(glRunnablesLock) {
        glRunnableCount++;
        glRunnables.add( new GLRunnableTask(glRunnable, null, false) );
    }
  }

  public final void setAutoSwapBufferMode(final boolean enable) {
    autoSwapBufferMode = enable;
  }

  public final boolean getAutoSwapBufferMode() {
    return autoSwapBufferMode;
  }

  private final String getExclusiveContextSwitchString() {
      return 0 == exclusiveContextSwitch ? "nop" : ( 0 > exclusiveContextSwitch ? "released" : "claimed" ) ;
  }

  /**
   * Dedicates this instance's {@link GLContext} to the given thread.<br/>
   * The thread will exclusively claim the {@link GLContext} via {@link #display()} and not release it
   * until {@link #destroy()} or <code>setExclusiveContextThread(null)</code> has been called.
   * <p>
   * Default non-exclusive behavior is <i>requested</i> via <code>setExclusiveContextThread(null)</code>,
   * which will cause the next call of {@link #display()} on the exclusive thread to
   * release the {@link GLContext}. Only after it's async release, {@link #getExclusiveContextThread()}
   * will return <code>null</code>.
   * </p>
   * <p>
   * To release a previous made exclusive thread, a user issues <code>setExclusiveContextThread(null)</code>
   * and may poll {@link #getExclusiveContextThread()} until it returns <code>null</code>,
   * <i>while</i> the exclusive thread is still running.
   * </p>
   * <p>
   * Note: Setting a new exclusive thread without properly releasing a previous one
   * will throw an GLException.
   * </p>
   * <p>
   * One scenario could be to dedicate the context to the {@link com.jogamp.opengl.util.AnimatorBase#getThread() animator thread}
   * and spare redundant context switches.
   * </p>
   * @param t the exclusive thread to claim the context, or <code>null</code> for default operation.
   * @return previous exclusive context thread
   * @throws GLException If an exclusive thread is still active but a new one is attempted to be set
   */
  public final Thread setExclusiveContextThread(final Thread t, final GLContext context) throws GLException {
    if (DEBUG) {
      System.err.println("GLDrawableHelper.setExclusiveContextThread(): START switch "+getExclusiveContextSwitchString()+", thread "+exclusiveContextThread+" -> "+t+" -- currentThread "+Thread.currentThread());
    }
    final Thread oldExclusiveContextThread = exclusiveContextThread;
    if( exclusiveContextThread == t ) {
        exclusiveContextSwitch =  0; // keep
    } else if( null == t ) {
        exclusiveContextSwitch = -1; // release
    } else {
        exclusiveContextSwitch =  1; // claim
        if( null != exclusiveContextThread ) {
            throw new GLException("Release current exclusive Context Thread "+exclusiveContextThread+" first");
        }
        if( null != context && context.isCurrent() ) {
            try {
                forceNativeRelease(context);
            } catch (final Throwable ex) {
                flushGLRunnables();
                throw GLException.newGLException(ex);
            }
        }
        exclusiveContextThread = t;
    }
    if (DEBUG) {
      System.err.println("GLDrawableHelper.setExclusiveContextThread(): END switch "+getExclusiveContextSwitchString()+", thread "+exclusiveContextThread+" -- currentThread "+Thread.currentThread());
    }
    return oldExclusiveContextThread;
  }

  /**
   * @see #setExclusiveContextThread(Thread, GLContext)
   */
  public final Thread getExclusiveContextThread() {
    return exclusiveContextThread;
  }

  /**
   * Runs given {@code runnable} outside of a probable claimed exclusive thread,
   * i.e. releases the exclusive thread, executes the runnable and reclaims it.
   * @see #setExclusiveContextThread(Thread, GLContext)
   * @since 2.3.2
   */
  public final void runOutsideOfExclusiveContextThread(final GLContext context, final Runnable runnable) {
      final Thread t = setExclusiveContextThread(null, context);
      try {
          runnable.run();
      } finally {
          setExclusiveContextThread(t, context);
      }
  }

  private static final ThreadLocal<WeakReference<Runnable>> perThreadInitAction = new ThreadLocal<WeakReference<Runnable>>();
  private static final Runnable getLastInitAction() {
      final WeakReference<Runnable> lastInitActionWR = perThreadInitAction.get();
      if( null != lastInitActionWR ) {
          final Runnable lastInitAction = lastInitActionWR.get();
          if( null == lastInitAction ) {
              perThreadInitAction.set(null);
          }
          return lastInitAction;
      }
      return null;
  }
  private static final void setLastInitAction(final Runnable initAction) {
      perThreadInitAction.set(new WeakReference<Runnable>(initAction));
  }

  /** Principal helper method which runs a Runnable with the context
      made current. This could have been made part of GLContext, but a
      desired goal is to be able to implement GLAutoDrawable's in terms of
      the GLContext's public APIs, and putting it into a separate
      class helps ensure that we don't inadvertently use private
      methods of the GLContext or its implementing classes.
      <p>
      Note: Locking of the surface is implicit done by {@link GLContext#makeCurrent()},
      where unlocking is performed by the latter {@link GLContext#release()}.
      </p>
   *
   * @param drawable
   * @param context
   * @param runnable
   * @param initAction
   */
  public final void invokeGL(final GLDrawable drawable,
                             final GLContext context,
                             final Runnable  runnable,
                             final Runnable  initAction) {
    if(null==context) {
        if (DEBUG) {
            ExceptionUtils.dumpThrowable("informal", new GLException("Info: GLDrawableHelper " + this + ".invokeGL(): NULL GLContext"));
        }
        return;
    }

    if(PERF_STATS) {
        invokeGLImplStats(drawable, context, runnable, initAction);
    } else {
        invokeGLImpl(drawable, context, runnable, initAction);
    }
  }

  /**
   * Principal helper method which runs
   * {@link #disposeAllGLEventListener(GLAutoDrawable, boolean) disposeAllGLEventListener(autoDrawable, false)}
   * with the context made current.
   * <p>
   * If <code>destroyContext</code> is <code>true</code> the context is destroyed in the end while holding the lock.
   * </p>
   * <p>
   * If <code>destroyContext</code> is <code>false</code> the context is natively released, i.e. released as often as locked before.
   * </p>
   * @param autoDrawable
   * @param context
   * @param destroyContext destroy context in the end while holding the lock
   * @throws GLException caused by {@link GLEventListener#dispose(GLAutoDrawable)} or context closing
   *
   */
  public final void disposeGL(final GLAutoDrawable autoDrawable,
                              final GLContext context, final boolean destroyContext) throws GLException {
    // Support for recursive makeCurrent() calls as well as calling
    // other drawables' display() methods from within another one's
    GLContext lastContext = GLContext.getCurrent();
    Runnable  lastInitAction = null;
    if (lastContext != null) {
        if (lastContext == context) {
            lastContext = null;
        } else {
            // utilize recursive locking
            lastInitAction = getLastInitAction();
            lastContext.release();
        }
    }

    GLException disposeCaught = null;
    Throwable contextCloseCaught = null;

    int res;
    try {
      res = context.makeCurrent();
      if (GLContext.CONTEXT_NOT_CURRENT != res) {
        if(GLContext.CONTEXT_CURRENT_NEW == res) {
            throw new GLException(GLDrawableHelper.getThreadName()+" GLDrawableHelper " + this + ".invokeGL(): Dispose case (no init action given): Native context was not created (new ctx): "+context);
        }
        if( listeners.size() > 0 && null != autoDrawable ) {
            try {
                disposeAllGLEventListener(autoDrawable, false);
            } catch(final GLException t) {
                disposeCaught = t;
            }
        }
      }
    } finally {
      try {
          if(destroyContext) {
              context.destroy();
          } else {
              forceNativeRelease(context);
          }
      } catch (final Throwable t) {
          contextCloseCaught = t;
      }
      flushGLRunnables(); // always flush GLRunnables at dispose

      if (lastContext != null) {
        final int res2 = lastContext.makeCurrent();
        if (null != lastInitAction && res2 == GLContext.CONTEXT_CURRENT_NEW) {
          lastInitAction.run();
        }
      }
      if( null != disposeCaught ) {
          if( null != contextCloseCaught ) {
              ExceptionUtils.dumpThrowable("subsequent", contextCloseCaught);
          }
          throw disposeCaught;
      }
      if( null != contextCloseCaught ) {
          throw GLException.newGLException(contextCloseCaught);
      }
    }
  }

  private final void invokeGLImpl(final GLDrawable drawable,
          final GLContext context,
          final Runnable  runnable,
          final Runnable  initAction) {
      final Thread currentThread = Thread.currentThread();

      Throwable glEventListenerCaught = null;
      Throwable contextReleaseCaught = null;

      // Exclusive Cases:
      //   1: lock - unlock  : default
      //   2: lock - -       : exclusive,    not locked yet
      //   3: -    - -       : exclusive,    already locked
      //   4: -    - unlock  : ex-exclusive, already locked
      final boolean _isExclusiveThread, _releaseExclusiveThread;
      if( null != exclusiveContextThread) {
          if( currentThread == exclusiveContextThread ) {
              _releaseExclusiveThread = 0 > exclusiveContextSwitch;
              _isExclusiveThread = !_releaseExclusiveThread;
              exclusiveContextSwitch = 0;
          } else {
              // Exclusive thread usage, but on other thread
              return;
          }
      } else {
          _releaseExclusiveThread = false;
          _isExclusiveThread = false;
      }

      // Support for recursive makeCurrent() calls as well as calling
      // other drawables' display() methods from within another one's
      int res = GLContext.CONTEXT_NOT_CURRENT;
      GLContext lastContext = GLContext.getCurrent();
      Runnable  lastInitAction = null;
      if (lastContext != null) {
          if (lastContext == context) {
              res = GLContext.CONTEXT_CURRENT;
              lastContext = null;
          } else {
              // utilize recursive locking
              lastInitAction = getLastInitAction();
              lastContext.release();
          }
      }

      try {
          final boolean releaseContext;
          if( GLContext.CONTEXT_NOT_CURRENT == res ) {
              res = context.makeCurrent();
              releaseContext = !_isExclusiveThread;
          } else {
              releaseContext = _releaseExclusiveThread;
          }
          if (GLContext.CONTEXT_NOT_CURRENT != res) {
              try {
                  setLastInitAction(initAction);
                  if (GLContext.CONTEXT_CURRENT_NEW == res) {
                      if (DEBUG) {
                          System.err.println("GLDrawableHelper " + this + ".invokeGL(): Running initAction");
                      }
                      initAction.run();
                  }
                  runnable.run();
                  if ( autoSwapBufferMode ) {
                      drawable.swapBuffers();
                  }
              } catch (final Throwable t) {
                  glEventListenerCaught = t;
              } finally {
                  if( _releaseExclusiveThread ) {
                      exclusiveContextThread = null;
                      if (DEBUG) {
                          System.err.println("GLDrawableHelper.invokeGL() - Release ExclusiveContextThread -- currentThread "+Thread.currentThread());
                      }
                  }
                  if( releaseContext ) {
                      try {
                          context.release();
                      } catch (final Throwable t) {
                          contextReleaseCaught = t;
                      }
                  }
              }
          }
      } finally {
          if (lastContext != null) {
              final int res2 = lastContext.makeCurrent();
              if (null != lastInitAction && res2 == GLContext.CONTEXT_CURRENT_NEW) {
                  lastInitAction.run();
              }
          }
          if( null != glEventListenerCaught ) {
              flushGLRunnables();
              if( null != contextReleaseCaught ) {
                  ExceptionUtils.dumpThrowable("subsequent", contextReleaseCaught);
              }
              throw GLException.newGLException(glEventListenerCaught);
          }
          if( null != contextReleaseCaught ) {
              flushGLRunnables();
              throw GLException.newGLException(contextReleaseCaught);
          }
      }
  }

  private final void invokeGLImplStats(final GLDrawable drawable,
          final GLContext context,
          final Runnable  runnable,
          final Runnable  initAction) {
      final Thread currentThread = Thread.currentThread();

      Throwable glEventListenerCaught = null;
      Throwable contextReleaseCaught = null;

      // Exclusive Cases:
      //   1: lock - unlock  : default
      //   2: lock - -       : exclusive, not locked yet
      //   3: -    - -       : exclusive, already locked
      //   4: -    - unlock  : ex-exclusive, already locked
      final boolean _isExclusiveThread, _releaseExclusiveThread;
      if( null != exclusiveContextThread) {
          if( currentThread == exclusiveContextThread ) {
              _releaseExclusiveThread = 0 > exclusiveContextSwitch;
              _isExclusiveThread = !_releaseExclusiveThread;
          } else {
              // Exclusive thread usage, but on other thread
              return;
          }
      } else {
          _releaseExclusiveThread = false;
          _isExclusiveThread = false;
      }

      // Support for recursive makeCurrent() calls as well as calling
      // other drawables' display() methods from within another one's
      int res = GLContext.CONTEXT_NOT_CURRENT;
      GLContext lastContext = GLContext.getCurrent();
      Runnable  lastInitAction = null;
      if (lastContext != null) {
          if (lastContext == context) {
              res = GLContext.CONTEXT_CURRENT;
              lastContext = null;
          } else {
              // utilize recursive locking
              lastInitAction = getLastInitAction();
              lastContext.release();
          }
      }

      final long t0 = System.currentTimeMillis();
      long tdA = 0; // makeCurrent
      long tdR = 0; // render time
      long tdS = 0; // swapBuffers
      long tdX = 0; // release
      boolean ctxClaimed = false;
      boolean ctxReleased = false;
      final boolean ctxDestroyed = false;
      try {
          final boolean releaseContext;
          if( GLContext.CONTEXT_NOT_CURRENT == res ) {
              res = context.makeCurrent();
              releaseContext = !_isExclusiveThread;
              ctxClaimed = true;
          } else {
              releaseContext = _releaseExclusiveThread;
          }
          if (GLContext.CONTEXT_NOT_CURRENT != res) {
              try {
                  setLastInitAction(initAction);
                  if (GLContext.CONTEXT_CURRENT_NEW == res) {
                      if (DEBUG) {
                          System.err.println("GLDrawableHelper " + this + ".invokeGL(): Running initAction");
                      }
                      initAction.run();
                  }
                  tdR = System.currentTimeMillis();
                  tdA = tdR - t0; // makeCurrent
                  runnable.run();
                  tdS = System.currentTimeMillis();
                  tdR = tdS - tdR; // render time
                  if ( autoSwapBufferMode ) {
                      drawable.swapBuffers();
                      tdX = System.currentTimeMillis();
                      tdS = tdX - tdS; // swapBuffers
                  }
              } catch (final Throwable t) {
                  glEventListenerCaught = t;
              } finally {
                  if( _releaseExclusiveThread ) {
                      exclusiveContextSwitch = 0;
                      exclusiveContextThread = null;
                      if (DEBUG) {
                          System.err.println("GLDrawableHelper.invokeGL() - Release ExclusiveContextThread -- currentThread "+Thread.currentThread());
                      }
                  }
                  if( releaseContext ) {
                      try {
                          context.release();
                          ctxReleased = true;
                      } catch (final Throwable t) {
                          contextReleaseCaught = t;
                      }
                  }
              }
          }
      } finally {
          tdX = System.currentTimeMillis() - tdX; // release / destroy
          if (lastContext != null) {
              final int res2 = lastContext.makeCurrent();
              if (null != lastInitAction && res2 == GLContext.CONTEXT_CURRENT_NEW) {
                  lastInitAction.run();
              }
          }
          if( null != glEventListenerCaught ) {
              flushGLRunnables();
              if( null != contextReleaseCaught ) {
                  ExceptionUtils.dumpThrowable("subsequent", contextReleaseCaught);
              }
              throw GLException.newGLException(glEventListenerCaught);
          }
          if( null != contextReleaseCaught ) {
              flushGLRunnables();
              throw GLException.newGLException(contextReleaseCaught);
          }
      }
      final long td = System.currentTimeMillis() - t0;
      System.err.println("td0 "+td+"ms, fps "+(1.0/(td/1000.0))+", td-makeCurrent: "+tdA+"ms, td-render "+tdR+"ms, td-swap "+tdS+"ms, td-release "+tdX+"ms, ctx claimed: "+ctxClaimed+", ctx release: "+ctxReleased+", ctx destroyed "+ctxDestroyed);
  }

  protected static String getThreadName() { return Thread.currentThread().getName(); }
}
