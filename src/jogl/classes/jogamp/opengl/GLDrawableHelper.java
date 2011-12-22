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

import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLRunnable;

import com.jogamp.opengl.util.Animator;

/** Encapsulates the implementation of most of the GLAutoDrawable's
    methods to be able to share it between GLCanvas and GLJPanel. */

public class GLDrawableHelper {
  protected static final boolean DEBUG = GLDrawableImpl.DEBUG;
  private Object listenersLock = new Object();
  private ArrayList<GLEventListener> listeners;
  private HashSet<GLEventListener> listenersToBeInit;
  private boolean autoSwapBufferMode;
  private Thread skipContextReleaseThread;
  private Object glRunnablesLock = new Object();
  private ArrayList<GLRunnable> glRunnables;
  private GLAnimatorControl animatorCtrl;

  public GLDrawableHelper() {
    reset();
  }

  public final void reset() {
    synchronized(listenersLock) {
        listeners = new ArrayList<GLEventListener>();
        listenersToBeInit = new HashSet<GLEventListener>();
    }
    autoSwapBufferMode = true;
    skipContextReleaseThread = null;
    synchronized(glRunnablesLock) {
        glRunnables = new ArrayList<GLRunnable>();
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

  /**
   * Issues {@link javax.media.opengl.GLEventListener#dispose(javax.media.opengl.GLAutoDrawable)}
   * to all listeners.
   * @param drawable
   */
  public final void dispose(GLAutoDrawable drawable) {
    synchronized(listenersLock) {
        for (int i=0; i < listeners.size(); i++) {
          listeners.get(i).dispose(drawable);
        }
    }
  }

  private boolean init(GLEventListener l, GLAutoDrawable drawable, boolean sendReshape) {
      if(listenersToBeInit.remove(l)) {
          l.init(drawable);
          if(sendReshape) {
              reshape(l, drawable, 0, 0, drawable.getWidth(), drawable.getHeight(), true /* setViewport */, false);
          }
          return true;
      }
      return false;
  }

  public final void init(GLAutoDrawable drawable) {
    synchronized(listenersLock) {
        for (int i=0; i < listeners.size(); i++) {
          final GLEventListener listener = listeners.get(i) ;

          // If make current ctx, invoked by invokGL(..), results in a new ctx, init gets called.
          // This may happen not just for initial setup, but for ctx recreation due to resource change (drawable/window),
          // hence the must always be initialized unconditional.
          listenersToBeInit.add(listener);

          if ( ! init( listener, drawable, false ) ) {
            throw new GLException("GLEventListener "+listener+" already initialized: "+drawable);
          }
        }
    }
  }

  public final void display(GLAutoDrawable drawable) {
    displayImpl(drawable);
    if(!execGLRunnables(drawable)) {
        displayImpl(drawable);  
    }
  }
  private void displayImpl(GLAutoDrawable drawable) {
      synchronized(listenersLock) {
          for (int i=0; i < listeners.size(); i++) {
            final GLEventListener listener = listeners.get(i) ;
            // GLEventListener may need to be init, 
            // in case this one is added after the realization of the GLAutoDrawable
            init( listener, drawable, true ) ; 
            listener.display(drawable);
          }
      }
  }

  private void reshape(GLEventListener listener, GLAutoDrawable drawable,
                       int x, int y, int width, int height, boolean setViewport, boolean checkInit) {
    if(checkInit) {
        // GLEventListener may need to be init, 
        // in case this one is added after the realization of the GLAutoDrawable      
        init( listener, drawable, false ) ;
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

  private boolean execGLRunnables(GLAutoDrawable drawable) {
    boolean res = true;
    if(glRunnables.size()>0) {
        // swap one-shot list asap
        ArrayList<GLRunnable> _glRunnables = null;
        synchronized(glRunnablesLock) {
            if(glRunnables.size()>0) {
                _glRunnables = glRunnables;
                glRunnables = new ArrayList<GLRunnable>();
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

  public final boolean isExternalAnimatorRunning() {
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

  public final void invoke(GLAutoDrawable drawable, boolean wait, GLRunnable glRunnable) {
    if( null == drawable || null == glRunnable ) {
        return;
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
      methods of the GLContext or its implementing classes.<br>
   * <br>
   * Remark: In case this method is called to dispose the GLDrawable/GLAutoDrawable,
   * <code>initAction</code> shall be <code>null</code> to mark this cause.<br>
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

    final boolean isDisposeAction = null==initAction ;
    
    if( isDisposeAction && !context.isCreated() ) {
        throw new GLException(Thread.currentThread().getName()+" GLDrawableHelper " + this + ".invokeGL(): Dispose case (no init action given): Native context is not created: "+context);
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
            lastInitAction = perThreadInitAction.get();
            lastContext.release();
        }
    }
  
    /**
    long t0 = System.currentTimeMillis();
    long td1 = 0; // makeCurrent
    long tdR = 0; // render time
    long td2 = 0; // swapBuffers
    long td3 = 0; // release
    boolean scr = true; */
    
    try {
      if (res == GLContext.CONTEXT_NOT_CURRENT) {
          res = context.makeCurrent();
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
        }
        // tdR = System.currentTimeMillis();        
        // td1 = tdR - t0; // makeCurrent
        if(null!=runnable) {
            runnable.run();
            // td2 = System.currentTimeMillis();
            // tdR = td2 - tdR; // render time
            if (autoSwapBufferMode && !isDisposeAction && drawable != null) {
                drawable.swapBuffers();
                // td3 = System.currentTimeMillis();
                // td2 = td3 - td2; // swapBuffers
            }
        }
      }
    } finally {
      if(res != GLContext.CONTEXT_NOT_CURRENT &&
         (null == skipContextReleaseThread || Thread.currentThread()!=skipContextReleaseThread) ) {
          try {
              context.release();
              // scr = false;
          } catch (Exception e) {
          }
      }
      // td3 = System.currentTimeMillis() - td3; // release
      if (lastContext != null) {
        int res2 = lastContext.makeCurrent();
        if (null != lastInitAction && res2 == GLContext.CONTEXT_CURRENT_NEW) {
          lastInitAction.run();
        }
      }
    }
    // long td0 = System.currentTimeMillis() - t0;
    // System.err.println("td0 "+td0+"ms, fps "+(1.0/(td0/1000.0))+", td-makeCurrent: "+td1+"ms, td-render "+tdR+"ms, td-swap "+td2+"ms, td-release "+td3+"ms, skip ctx release: "+scr);
  }

}
