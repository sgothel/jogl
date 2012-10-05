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

import java.util.ArrayList;
import java.util.HashSet;

import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLFBODrawable;
import javax.media.opengl.GLRunnable;

import com.jogamp.opengl.util.Animator;

/** Encapsulates the implementation of most of the GLAutoDrawable's
    methods to be able to share it between GLCanvas and GLJPanel. */

public class GLDrawableHelper {
  /** true if property <code>jogl.debug.GLDrawable.PerfStats</code> is defined. */
  private static final boolean PERF_STATS = Debug.isPropertyDefined("jogl.debug.GLDrawable.PerfStats", true);
    
  protected static final boolean DEBUG = GLDrawableImpl.DEBUG;
  private final Object listenersLock = new Object();
  private final ArrayList<GLEventListener> listeners = new ArrayList<GLEventListener>();
  private final HashSet<GLEventListener> listenersToBeInit = new HashSet<GLEventListener>();
  private final Object glRunnablesLock = new Object();
  private volatile ArrayList<GLRunnableTask> glRunnables = new ArrayList<GLRunnableTask>();
  private boolean autoSwapBufferMode;
  private Thread skipContextReleaseThread;
  private GLAnimatorControl animatorCtrl;

  public GLDrawableHelper() {
    reset();
  }

  public final void reset() {
    synchronized(listenersLock) {
        listeners.clear();
        listenersToBeInit.clear();
    }
    autoSwapBufferMode = true;
    skipContextReleaseThread = null;
    synchronized(glRunnablesLock) {
        glRunnables.clear();
    }
    animatorCtrl = null;
  }

  @Override
  public final String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("GLAnimatorControl: "+animatorCtrl+", ");
    synchronized(listenersLock) {
        sb.append("GLEventListeners num "+listeners.size()+" [");
        for (int i=0; i < listeners.size(); i++) {
          Object l = listeners.get(i);
          sb.append(l);
          sb.append("[init ");
          sb.append( !listenersToBeInit.contains(l) );
          sb.append("], ");
        }
    }
    sb.append("]");
    return sb.toString();
  }

  /**
   * Associate a new context to the drawable and also propagates the context/drawable switch by 
   * calling {@link GLContext#setGLDrawable(GLDrawable, boolean) newCtx.setGLDrawable(drawable, true);}.
   * <p> 
   * If the old or new context was current on this thread, it is being released before switching the drawable.
   * </p>
   * <p>
   * Be aware that the old context is still bound to the drawable, 
   * and that one context can only bound to one drawable at one time! 
   * </p>
   * <p>
   * No locking is being performed on the drawable, caller is required to take care of it.
   * </p>
   * 
   * @param drawable the drawable which context is changed
   * @param oldCtx the old context
   * @param newCtx the new context
   * @param newCtxCreationFlags additional creation flags if newCtx is not null and not been created yet, see {@link GLContext#setContextCreationFlags(int)}
   * @return true if the new context was current, otherwise false
   *  
   * @see GLAutoDrawable#setContext(GLContext)
   */
  public static final boolean switchContext(GLDrawable drawable, GLContext oldCtx, GLContext newCtx, int newCtxCreationFlags) {
      if( null != oldCtx && oldCtx.isCurrent() ) {
          oldCtx.release();
      }
      final boolean newCtxCurrent;
      if(null!=newCtx) {
          newCtxCurrent = newCtx.isCurrent();
          if(newCtxCurrent) {
              newCtx.release();
          }
          newCtx.setContextCreationFlags(newCtxCreationFlags);
          newCtx.setGLDrawable(drawable, true); // propagate context/drawable switch
      } else {
          newCtxCurrent = false;
      }
      return newCtxCurrent;
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
   * No locking is being performed, caller is required to take care of it.
   * </p>
   * 
   * @param drawable
   * @param context maybe null
   * @return the new drawable
   */
  public static final GLDrawableImpl recreateGLDrawable(GLDrawableImpl drawable, GLContext context) {      
      if( ! drawable.isRealized() ) {
          return drawable;
      }
      final boolean contextCurrent = null != context && context.isCurrent();
      final GLDrawableFactory factory = drawable.getFactory();
      final NativeSurface surface = drawable.getNativeSurface();
      final ProxySurface proxySurface = (surface instanceof ProxySurface) ? (ProxySurface)surface : null;
      
      if(contextCurrent) {
          context.release();
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
          context.setGLDrawable(drawable, true); // re-association
      }
      
      if(contextCurrent) {
          context.makeCurrent();
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
  public static final GLDrawableImpl resizeOffscreenDrawable(GLDrawableImpl drawable, GLContext context, int newWidth, int newHeight)
          throws NativeWindowException, GLException 
  {
      if(drawable.getChosenGLCapabilities().isOnscreen()) {
          throw new NativeWindowException("Drawable is not offscreen: "+drawable);
      }
      final NativeSurface ns = drawable.getNativeSurface();
      final int lockRes = ns.lockSurface();
      if (NativeSurface.LOCK_SURFACE_NOT_READY >= lockRes) {
          throw new NativeWindowException("Could not lock surface of drawable: "+drawable);
      }
      boolean validateSize = true;
      try {
          if(DEBUG && ( 0>=newWidth || 0>=newHeight) ) {
              System.err.println("WARNING: Odd size detected: "+newWidth+"x"+newHeight+", using safe size 1x1. Drawable "+drawable);
              Thread.dumpStack();
          }
          if(0>=newWidth)  { newWidth = 1; validateSize=false; }
          if(0>=newHeight) { newHeight = 1; validateSize=false; }
          // propagate new size 
          if(ns instanceof ProxySurface) {
              final ProxySurface ps = (ProxySurface) ns;
              final UpstreamSurfaceHook ush = ps.getUpstreamSurfaceHook();
              if(ush instanceof UpstreamSurfaceHook.MutableSize) {
                  ((UpstreamSurfaceHook.MutableSize)ush).setSize(newWidth, newHeight);
              } else if(DEBUG) { // we have to assume UpstreamSurfaceHook contains the new size already, hence size check @ bottom
                  System.err.println("GLDrawableHelper.resizeOffscreenDrawable: Drawable's offscreen ProxySurface n.a. UpstreamSurfaceHook.MutableSize, but "+ush.getClass().getName()+": "+ush);
              }
          } else if(DEBUG) { // we have to assume surface contains the new size already, hence size check @ bottom
              System.err.println("GLDrawableHelper.resizeOffscreenDrawable: Drawable's offscreen surface n.a. ProxySurface, but "+ns.getClass().getName()+": "+ns);
          }
          if(drawable instanceof GLFBODrawable) {
              if( null != context && context.isCreated() ) {                      
                  ((GLFBODrawable) drawable).resetSize(context.getGL());
              }
          } else {
              drawable = GLDrawableHelper.recreateGLDrawable(drawable, context);
          }
      } finally {
          ns.unlockSurface();
      }
      if( validateSize && ( drawable.getWidth() != newWidth || drawable.getHeight() != newHeight ) ) {
          throw new InternalError("Incomplete resize operation: expected "+newWidth+"x"+newHeight+", has: "+drawable);
      }
      return drawable;
  }
    
  public final void addGLEventListener(GLEventListener listener) {
    addGLEventListener(-1, listener);
  }

  public final void addGLEventListener(int index, GLEventListener listener) {
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
  
  public final void removeGLEventListener(GLEventListener listener) {
    synchronized(listenersLock) {
        listeners.remove(listener);
        listenersToBeInit.remove(listener);
    }
  }

  public final GLEventListener removeGLEventListener(int index)
    throws IndexOutOfBoundsException {
    synchronized(listenersLock) {
        if(0>index) {
            index = listeners.size()-1;
        }
        return listeners.remove(index);
    }
  }
  
  /**
   * Issues {@link javax.media.opengl.GLEventListener#dispose(javax.media.opengl.GLAutoDrawable)}
   * to all listeners.
   * <p>
   * Please consider using {@link #disposeGL(GLAutoDrawable, GLDrawable, GLContext, Runnable)}
   * for correctness!
   * </p>
   * @param drawable
   */
  public final void dispose(GLAutoDrawable drawable) {
    synchronized(listenersLock) {
        final ArrayList<GLEventListener> _listeners = listeners;
        for (int i=0; i < _listeners.size(); i++) {
          _listeners.get(i).dispose(drawable);
        }
    }
  }

  private final void init(GLEventListener l, GLAutoDrawable drawable, boolean sendReshape) {
      l.init(drawable);
      if(sendReshape) {
          reshape(l, drawable, 0, 0, drawable.getWidth(), drawable.getHeight(), true /* setViewport */, false /* checkInit */);
      }
  }

  /** 
   * The default init action to be called once after ctx is being created @ 1st makeCurrent(). 
   * @param sendReshape set to true if the subsequent display call won't reshape, otherwise false to avoid double reshape.
   **/
  public final void init(GLAutoDrawable drawable, boolean sendReshape) {
    synchronized(listenersLock) {
        final ArrayList<GLEventListener> _listeners = listeners;
        for (int i=0; i < _listeners.size(); i++) {
          final GLEventListener listener = _listeners.get(i) ;

          // If make ctx current, invoked by invokGL(..), results in a new ctx, init gets called.
          // This may happen not just for initial setup, but for ctx recreation due to resource change (drawable/window),
          // hence it must be called unconditional, always.
          listenersToBeInit.remove(listener); // remove if exist, avoiding dbl init
          init( listener, drawable, sendReshape);
        }
    }
  }

  public final void display(GLAutoDrawable drawable) {
    displayImpl(drawable);
    if(!execGLRunnables(drawable)) {
        displayImpl(drawable);  
    }
  }
  private final void displayImpl(GLAutoDrawable drawable) {
      synchronized(listenersLock) {
          final ArrayList<GLEventListener> _listeners = listeners;
          for (int i=0; i < _listeners.size(); i++) {
            final GLEventListener listener = _listeners.get(i) ;
            // GLEventListener may need to be init, 
            // in case this one is added after the realization of the GLAutoDrawable
            if( listenersToBeInit.remove(listener) ) {
                init( listener, drawable, true /* sendReshape */) ;
            }
            listener.display(drawable);
          }
      }
  }

  private final void reshape(GLEventListener listener, GLAutoDrawable drawable,
                             int x, int y, int width, int height, boolean setViewport, boolean checkInit) {
    if(checkInit) {
        // GLEventListener may need to be init, 
        // in case this one is added after the realization of the GLAutoDrawable
        synchronized(listenersLock) {
            if( listenersToBeInit.remove(listener) ) {
                init( listener, drawable, false /* sendReshape */) ;
            }
        }
    }
    if(setViewport) {
        drawable.getGL().glViewport(x, y, width, height);
    }
    listener.reshape(drawable, x, y, width, height);
  }

  public final void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    synchronized(listenersLock) {
        for (int i=0; i < listeners.size(); i++) {
          reshape((GLEventListener) listeners.get(i), drawable, x, y, width, height, 0==i, true);
        }
    }
  }

  private final boolean execGLRunnables(GLAutoDrawable drawable) {
    boolean res = true;
    if(glRunnables.size()>0) { // volatile OK
        // swap one-shot list asap
        final ArrayList<GLRunnableTask> _glRunnables;
        synchronized(glRunnablesLock) {
            if(glRunnables.size()>0) {
                _glRunnables = glRunnables;
                glRunnables = new ArrayList<GLRunnableTask>();
            } else {
                _glRunnables = null;
            }
        }
        
        if(null!=_glRunnables) {
            for (int i=0; i < _glRunnables.size(); i++) {
                res = _glRunnables.get(i).run(drawable) && res;
            }
        }
    }
    return res;
  }

  public final void flushGLRunnables() {
    if(glRunnables.size()>0) { // volatile OK
        // swap one-shot list asap
        final ArrayList<GLRunnableTask> _glRunnables;
        synchronized(glRunnablesLock) {
            if(glRunnables.size()>0) {
                _glRunnables = glRunnables;
                glRunnables = new ArrayList<GLRunnableTask>();
            } else {
                _glRunnables = null;
            }
        }
        
        if(null!=_glRunnables) {
            for (int i=0; i < _glRunnables.size(); i++) {
                _glRunnables.get(i).flush();
            }
        }
    }
  }
  
  public final void setAnimator(GLAnimatorControl animator) throws GLException {
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

  public final boolean isAnimatorRunningOnOtherThread() {
    return ( null != animatorCtrl ) ? animatorCtrl.isStarted() && animatorCtrl.getThread() != Thread.currentThread() : false ;
  }

  public final boolean isAnimatorRunning() {
    return ( null != animatorCtrl ) ? animatorCtrl.isStarted() : false ;
  }

  public final boolean isExternalAnimatorAnimating() {
    return ( null != animatorCtrl ) ? animatorCtrl.isAnimating() && animatorCtrl.getThread() != Thread.currentThread() : false ;
  }

  public final boolean isAnimatorAnimating() {
    return ( null != animatorCtrl ) ? animatorCtrl.isAnimating() : false ;
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
   *
   * @param drawable the {@link GLAutoDrawable} to be used
   * @param wait if <code>true</code> block until execution of <code>glRunnable</code> is finished, otherwise return immediatly w/o waiting
   * @param glRunnable the {@link GLRunnable} to execute within {@link #display()}
   * @return <code>true</code> if the {@link GLRunnable} has been processed or queued, otherwise <code>false</code>.
   */
  public final boolean invoke(GLAutoDrawable drawable, boolean wait, GLRunnable glRunnable) {
    if( null == glRunnable || null == drawable ||
        wait && ( !drawable.isRealized() || null==drawable.getContext() ) ) {
        return false;
    }
    
    Throwable throwable = null;
    GLRunnableTask rTask = null;
    Object rTaskLock = new Object();
    synchronized(rTaskLock) {
        boolean deferred;
        synchronized(glRunnablesLock) {
            deferred = isExternalAnimatorAnimating();
            if(!deferred) {
                wait = false; // don't wait if exec immediatly
            }
            rTask = new GLRunnableTask(glRunnable,
                                       wait ? rTaskLock : null,
                                       wait  /* catch Exceptions if waiting for result */);
            glRunnables.add(rTask);
        }
        if( !deferred ) {
            drawable.display();
        } else if( wait ) {
            try {
                rTaskLock.wait(); // free lock, allow execution of rTask
            } catch (InterruptedException ie) {
                throwable = ie;
            }
            if(null==throwable) {
                throwable = rTask.getThrowable();
            }
            if(null!=throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }
    return true;
  }

  public final void setAutoSwapBufferMode(boolean enable) {
    autoSwapBufferMode = enable;
  }

  public final boolean getAutoSwapBufferMode() {
    return autoSwapBufferMode;
  }

  /**
   * @param t the thread for which context release shall be skipped, usually the animation thread,
   *          ie. {@link Animator#getThread()}.
   * @deprecated this is an experimental feature, 
   *             intended for measuring performance in regards to GL context switch
   *             and only being used if {@link #PERF_STATS} is enabled
   *             by defining property <code>jogl.debug.GLDrawable.PerfStats</code>.
   */
  public final void setSkipContextReleaseThread(Thread t) {
    skipContextReleaseThread = t;
  }

  /**
   * @deprecated see {@link #setSkipContextReleaseThread(Thread)} 
   */
  public final Thread getSkipContextReleaseThread() {
    return skipContextReleaseThread;
  }
  
  private static final ThreadLocal<Runnable> perThreadInitAction = new ThreadLocal<Runnable>();

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
  public final void invokeGL(GLDrawable drawable,
                             GLContext context,
                             Runnable  runnable,
                             Runnable  initAction) {
    if(null==context) {
        if (DEBUG) {
            Exception e = new GLException(Thread.currentThread().getName()+" Info: GLDrawableHelper " + this + ".invokeGL(): NULL GLContext");
            e.printStackTrace();
        }
        return;
    }

    if(PERF_STATS) {
        invokeGLImplStats(drawable, context, runnable, initAction, null);    
    } else {
        invokeGLImpl(drawable, context, runnable, initAction, null);
    }
  }

  /** 
   * Principal helper method which runs {@link #dispose(GLAutoDrawable)} with the context
   * made current and destroys the context afterwards while holding the lock.  
   * 
   * @param autoDrawable
   * @param drawable
   * @param context
   * @param postAction
   */
  public final void disposeGL(GLAutoDrawable autoDrawable,
                              GLDrawable drawable,
                              GLContext context,
                              Runnable  postAction) {
    if(PERF_STATS) {
        invokeGLImplStats(drawable, context, null, null, autoDrawable);    
    } else {
        invokeGLImpl(drawable, context, null, null, autoDrawable);
    }
    if(null != postAction) {
        postAction.run();
    }
  }
  
  private final void invokeGLImpl(GLDrawable drawable,
                                  GLContext context,
                                  Runnable  runnable,
                                  Runnable  initAction,
                                  GLAutoDrawable disposeAutoDrawable) {
    final Thread currentThread = Thread.currentThread();
    
    final boolean isDisposeAction = null==initAction ;
        
    // Support for recursive makeCurrent() calls as well as calling
    // other drawables' display() methods from within another one's
    GLContext lastContext = GLContext.getCurrent();
    Runnable  lastInitAction = null;
    if (lastContext != null) {
        if (lastContext == context) {
            lastContext = null; // utilize recursive locking
        } else {
            lastInitAction = perThreadInitAction.get();
            lastContext.release();
        }
    }
    int res = GLContext.CONTEXT_NOT_CURRENT;
  
    try {
      res = context.makeCurrent();
      if (GLContext.CONTEXT_NOT_CURRENT != res) {
        if(!isDisposeAction) {
            perThreadInitAction.set(initAction);
            if (GLContext.CONTEXT_CURRENT_NEW == res) {
              if (DEBUG) {
                System.err.println("GLDrawableHelper " + this + ".invokeGL(): Running initAction");
              }
              initAction.run();
            }
            runnable.run();
            if (autoSwapBufferMode) {
                drawable.swapBuffers();
            }
        } else {
            if(GLContext.CONTEXT_CURRENT_NEW == res) {
                throw new GLException(currentThread.getName()+" GLDrawableHelper " + this + ".invokeGL(): Dispose case (no init action given): Native context was not created (new ctx): "+context);
            }
            if(listeners.size()>0) {
                dispose(disposeAutoDrawable);
            }
        }
      }
    } finally {
      try {
          if(isDisposeAction) {
              context.destroy();
              flushGLRunnables();
          } else if( GLContext.CONTEXT_NOT_CURRENT != res ) {
              context.release();
          }
      } catch (Exception e) {
          System.err.println("Catched: "+e.getMessage());
          e.printStackTrace();
      }
      if (lastContext != null) {
        final int res2 = lastContext.makeCurrent();
        if (null != lastInitAction && res2 == GLContext.CONTEXT_CURRENT_NEW) {
          lastInitAction.run();
        }
      }
    }
  }
  
  private final void invokeGLImplStats(GLDrawable drawable,
                                       GLContext context,
                                       Runnable  runnable,
                                       Runnable  initAction,
                                       GLAutoDrawable disposeAutoDrawable) {
    final Thread currentThread = Thread.currentThread();
    
    final boolean isDisposeAction = null==initAction ;
    
    // Support for recursive makeCurrent() calls as well as calling
    // other drawables' display() methods from within another one's
    int res = GLContext.CONTEXT_NOT_CURRENT;
    GLContext lastContext = GLContext.getCurrent();
    Runnable  lastInitAction = null;
    if (lastContext != null) {
        if (lastContext == context) {
            if( currentThread == skipContextReleaseThread ) {
                res = GLContext.CONTEXT_CURRENT;
            } // else: utilize recursive locking
            lastContext = null;
        } else {
            lastInitAction = perThreadInitAction.get();
            lastContext.release();
        }
    }
  
    long t0 = System.currentTimeMillis();
    long tdA = 0; // makeCurrent
    long tdR = 0; // render time
    long tdS = 0; // swapBuffers
    long tdX = 0; // release
    boolean ctxClaimed = false;
    boolean ctxReleased = false;
    boolean ctxDestroyed = false;    
    try {
      if (res == GLContext.CONTEXT_NOT_CURRENT) {
          res = context.makeCurrent();
          ctxClaimed = true;
      }
      if (res != GLContext.CONTEXT_NOT_CURRENT) {
        if(!isDisposeAction) {
            perThreadInitAction.set(initAction);
            if (res == GLContext.CONTEXT_CURRENT_NEW) {
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
            if (autoSwapBufferMode) {
                drawable.swapBuffers();
                tdX = System.currentTimeMillis();
                tdS = tdX - tdS; // swapBuffers
            }
        } else {
            if(res == GLContext.CONTEXT_CURRENT_NEW) {
                throw new GLException(currentThread.getName()+" GLDrawableHelper " + this + ".invokeGL(): Dispose case (no init action given): Native context was not created (new ctx): "+context);
            }
            if(listeners.size()>0) {
                dispose(disposeAutoDrawable);
            }
        }
      }
    } finally {
      try {
          if(isDisposeAction) {
              context.destroy();
              flushGLRunnables();
              ctxDestroyed = true;
          } else if( res != GLContext.CONTEXT_NOT_CURRENT &&
                     (null == skipContextReleaseThread || currentThread != skipContextReleaseThread) ) {
              context.release();
              ctxReleased = true;
          }
      } catch (Exception e) {
          System.err.println("Catched: "+e.getMessage());
          e.printStackTrace();
      }

      tdX = System.currentTimeMillis() - tdX; // release / destroy
      if (lastContext != null) {
        final int res2 = lastContext.makeCurrent();
        if (null != lastInitAction && res2 == GLContext.CONTEXT_CURRENT_NEW) {
          lastInitAction.run();
        }
      }
    }
    long td = System.currentTimeMillis() - t0;
    System.err.println("td0 "+td+"ms, fps "+(1.0/(td/1000.0))+", td-makeCurrent: "+tdA+"ms, td-render "+tdR+"ms, td-swap "+tdS+"ms, td-release "+tdX+"ms, ctx claimed: "+ctxClaimed+", ctx release: "+ctxReleased+", ctx destroyed "+ctxDestroyed);
  }
    
}
