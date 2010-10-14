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

package com.jogamp.opengl.impl;

import java.util.*;
import javax.media.opengl.*;

/** Encapsulates the implementation of most of the GLAutoDrawable's
    methods to be able to share it between GLCanvas and GLJPanel. */

public class GLDrawableHelper {
  protected static final boolean DEBUG = GLDrawableImpl.DEBUG;
  private static final boolean VERBOSE = Debug.verbose();
  private Object listenersLock = new Object();
  private List listeners;
  private volatile boolean listenersIter; // avoid java.util.ConcurrentModificationException
  private Set listenersToBeInit;
  private boolean autoSwapBufferMode;
  private Object glRunnablesLock = new Object();
  private ArrayList glRunnables;
  private GLAnimatorControl animatorCtrl;

  public GLDrawableHelper() {
    reset();
  }

  public void reset() {
    synchronized(listenersLock) {
        listeners = new ArrayList();
        listenersIter = false;
        listenersToBeInit = new HashSet();
    }
    autoSwapBufferMode = true;
    synchronized(glRunnablesLock) {
        glRunnables = new ArrayList();
    }
    animatorCtrl = null;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("GLAnimatorControl: "+animatorCtrl+", ");
    synchronized(listenersLock) {
        sb.append("GLEventListeners num "+listeners.size()+" [");
        listenersIter = true;
        for (int i=0; i < listeners.size(); i++) {
          Object l = listeners.get(i);
          sb.append(l);
          sb.append("[init ");
          sb.append( !listenersToBeInit.contains(l) );
          sb.append("], ");
        }
        listenersIter = false;
    }
    sb.append("]");
    return sb.toString();
  }

  public void addGLEventListener(GLEventListener listener) {
    addGLEventListener(-1, listener);
  }

  public void addGLEventListener(int index, GLEventListener listener) {
    synchronized(listenersLock) {
        if(0>index) {
            index = listeners.size();
        }
        listenersToBeInit.add(listener);
        if(!listenersIter) {
            // fast path
            listeners.add(index, listener);
        } else {
            // copy mode in case this is issued while iterating, eg via init, display, ..
            List newListeners = (List) ((ArrayList) listeners).clone();
            newListeners.add(index, listener);
            listeners = newListeners;
        }
    }
  }
  
  public void removeGLEventListener(GLEventListener listener) {
    synchronized(listenersLock) {
        if(!listenersIter) {
            // fast path
            listeners.remove(listener);
        } else {
            // copy mode in case this is issued while iterating, eg via init, display, ..
            List newListeners = (List) ((ArrayList) listeners).clone();
            newListeners.remove(listener);
            listeners = newListeners;
        }
        listenersToBeInit.remove(listener);
    }
  }

  public void dispose(GLAutoDrawable drawable) {
    synchronized(listenersLock) {
        listenersIter = true;
        for (int i=0; i < listeners.size(); i++) {
          GLEventListener listener = (GLEventListener) listeners.get(i) ;
          listener.dispose(drawable);
          listenersToBeInit.add(listener);
        }
        listenersIter = false;
    }
  }

  private final boolean init(GLEventListener l, GLAutoDrawable drawable, boolean sendReshape) {
      if(listenersToBeInit.remove(l)) {
          l.init(drawable);
          if(sendReshape) {
              reshape(l, drawable, 0, 0, drawable.getWidth(), drawable.getHeight(), true /* setViewport */);
          }
          return true;
      }
      return false;
  }

  public void init(GLAutoDrawable drawable) {
    synchronized(listenersLock) {
        listenersIter = true;
        for (int i=0; i < listeners.size(); i++) {
          GLEventListener listener = (GLEventListener) listeners.get(i) ;
          if ( ! init( listener, drawable, false ) ) {
            throw new GLException("GLEventListener "+listener+" already initialized: "+drawable);
          }
        }
        listenersIter = false;
    }
  }

  public void display(GLAutoDrawable drawable) {
    synchronized(listenersLock) {
        listenersIter = true;
        for (int i=0; i < listeners.size(); i++) {
          GLEventListener listener = (GLEventListener) listeners.get(i) ;
          // GLEventListener may need to be init, 
          // in case this one is added after the realization of the GLAutoDrawable
          init( listener, drawable, true ) ; 
          listener.display(drawable);
        }
        listenersIter = false;
    }
    execGLRunnables(drawable);
  }

  private final void reshape(GLEventListener listener, GLAutoDrawable drawable,
                             int x, int y, int width, int height, boolean setViewport) {
    if(setViewport) {
        drawable.getGL().glViewport(x, y, width, height);
    }
    listener.reshape(drawable, x, y, width, height);
  }

  public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    synchronized(listenersLock) {
        listenersIter = true;
        for (int i=0; i < listeners.size(); i++) {
          reshape((GLEventListener) listeners.get(i), drawable, x, y, width, height, 0==i);
        }
        listenersIter = false;
    }
  }

  private void execGLRunnables(GLAutoDrawable drawable) {
    if(glRunnables.size()>0) {
        // swap one-shot list asap
        ArrayList _glRunnables = null;
        synchronized(glRunnablesLock) {
            if(glRunnables.size()>0) {
                _glRunnables = glRunnables;
                glRunnables = new ArrayList();
            }
        }
        if(null!=_glRunnables) {
            for (int i=0; i < _glRunnables.size(); i++) {
              ((GLRunnable) _glRunnables.get(i)).run(drawable);
            }
        }
    }
  }

  public void setAnimator(GLAnimatorControl animator) throws GLException {
    synchronized(glRunnablesLock) {
        if(animatorCtrl!=animator && null!=animator && null!=animatorCtrl) {
            throw new GLException("Trying to register GLAnimatorControl "+animator+", where "+animatorCtrl+" is already registered. Unregister first.");
        }
        animatorCtrl = animator;
    }
  }

  public GLAnimatorControl getAnimator() {
    synchronized(glRunnablesLock) {
        return animatorCtrl;
    }
  }

  public final boolean isExternalAnimatorAnimating() {
    return ( null != animatorCtrl ) ? animatorCtrl.isAnimating() && animatorCtrl.getThread() != Thread.currentThread() : false ;
  }

  public void invoke(GLAutoDrawable drawable, boolean wait, GLRunnable glRunnable) {
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

  public void setAutoSwapBufferMode(boolean onOrOff) {
    autoSwapBufferMode = onOrOff;
  }

  public boolean getAutoSwapBufferMode() {
    return autoSwapBufferMode;
  }

  private static final ThreadLocal perThreadInitAction = new ThreadLocal();
  /** Principal helper method which runs a Runnable with the context
      made current. This could have been made part of GLContext, but a
      desired goal is to be able to implement the GLCanvas in terms of
      the GLContext's public APIs, and putting it into a separate
      class helps ensure that we don't inadvertently use private
      methods of the GLContext or its implementing classes. */
  public void invokeGL(GLDrawable drawable,
                       GLContext context,
                       Runnable  runnable,
                       Runnable  initAction) {
    if(null==context) {
        if (DEBUG) {
            Exception e = new GLException(Thread.currentThread().getName()+"Info: GLDrawableHelper " + this + ".invokeGL(): NULL GLContext");
            e.printStackTrace();
        }
        return;
    }
    // Support for recursive makeCurrent() calls as well as calling
    // other drawables' display() methods from within another one's
    GLContext lastContext    = GLContext.getCurrent();
    Runnable  lastInitAction = (Runnable) perThreadInitAction.get();
    if (lastContext != null) {
      lastContext.release();
    }
  
    if(!context.isCreated() && null == initAction) {
        throw new GLException("Context has to be created, but no initAction is given: "+context);
    }
    int res = 0;
    try {
      res = context.makeCurrent();
      if (res != GLContext.CONTEXT_NOT_CURRENT) {
        if(null!=initAction) {
            perThreadInitAction.set(initAction);
            if (res == GLContext.CONTEXT_CURRENT_NEW) {
              if (DEBUG) {
                System.err.println("GLDrawableHelper " + this + ".invokeGL(): Running initAction");
              }
              initAction.run();
            }
        }
        if(null!=runnable) {
            if (DEBUG && VERBOSE) {
              System.err.println("GLDrawableHelper " + this + ".invokeGL(): Running runnable");
            }
            runnable.run();
            if (autoSwapBufferMode && null != initAction) {
              if (drawable != null) {
                drawable.swapBuffers();
              }
            }
        }
      }
    } finally {
      try {
        if (res != GLContext.CONTEXT_NOT_CURRENT) {
          context.release();
        }
      } catch (Exception e) {
      }
      if (lastContext != null) {
        int res2 = lastContext.makeCurrent();
        if (res2 == GLContext.CONTEXT_CURRENT_NEW) {
          lastInitAction.run();
        }
      }
    }
  }

}
