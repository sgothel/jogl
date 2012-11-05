/**
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl.util;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLRunnable;

import jogamp.opengl.Debug;

/**
 * Providing utility functions dealing w/ {@link GLDrawable}s, {@link GLAutoDrawable} and their {@link GLEventListener}.
 */
public class GLDrawableUtil {
  protected static final boolean DEBUG = Debug.debug("GLDrawable");
  
  public static final boolean isAnimatorStartedOnOtherThread(GLAnimatorControl animatorCtrl) {
    return ( null != animatorCtrl ) ? animatorCtrl.isStarted() && animatorCtrl.getThread() != Thread.currentThread() : false ;
  }

  public static final boolean isAnimatorStarted(GLAnimatorControl animatorCtrl) {
    return ( null != animatorCtrl ) ? animatorCtrl.isStarted() : false ;
  }

  public static final boolean isAnimatorAnimatingOnOtherThread(GLAnimatorControl animatorCtrl) {
    return ( null != animatorCtrl ) ? animatorCtrl.isAnimating() && animatorCtrl.getThread() != Thread.currentThread() : false ;
  }
  
  public static final boolean isAnimatorAnimating(GLAnimatorControl animatorCtrl) {
    return ( null != animatorCtrl ) ? animatorCtrl.isAnimating() : false ;
  }
  
  /**
   * Moves the designated {@link GLEventListener} from {@link GLAutoDrawable} <code>src</code> to <code>dest</code>.
   * If <code>preserveInitState</code> is <code>true</code>, it's initialized state is preserved
   * and {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int) reshape(..)} issued w/ the next {@link GLAutoDrawable#display()} call.
   * <p>
   * Note that it is only legal to pass <code>preserveInitState := true</code>,
   * if the {@link GLContext} of both <code>src</code> and <code>dest</code> are shared, or has itself moved from <code>src</code> to <code>dest</code>.
   * </p>
   * <p>
   * Also note that the caller is encouraged to pause an attached {@link GLAnimatorControl}.
   * </p>
   * @param src
   * @param dest
   * @param listener
   * @param preserveInitState
   */
  public static final void moveGLEventListener(GLAutoDrawable src, GLAutoDrawable dest, GLEventListener listener, boolean preserveInitState) {
    final boolean initialized = src.getGLEventListenerInitState(listener);
    src.removeGLEventListener(listener);
    dest.addGLEventListener(listener);
    if(preserveInitState && initialized) {
        dest.setGLEventListenerInitState(listener, true);
        dest.invoke(false, new ReshapeGLEventListener(listener));
    } // else .. !init state is default
  }
  
  /**
   * Moves all {@link GLEventListener} from {@link GLAutoDrawable} <code>src</code> to <code>dest</code>.
   * If <code>preserveInitState</code> is <code>true</code>, it's initialized state is preserved
   * and {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int) reshape(..)} issued w/ the next {@link GLAutoDrawable#display()} call.
   * <p>
   * Note that it is only legal to pass <code>preserveInitState := true</code>,
   * if the {@link GLContext} of both <code>src</code> and <code>dest</code> are shared, or has itself moved from <code>src</code> to <code>dest</code>.
   * </p>
   * <p>
   * Also note that the caller is encouraged to pause an attached {@link GLAnimatorControl}.
   * </p>
   * @param src
   * @param dest
   * @param listener
   * @param preserveInitState
   */
  public static final void moveAllGLEventListener(GLAutoDrawable src, GLAutoDrawable dest, boolean preserveInitState) {
    for(int count = src.getGLEventListenerCount(); 0<count; count--) {
        final GLEventListener listener = src.getGLEventListener(0);
        moveGLEventListener(src, dest, listener, preserveInitState);
    }
  }

  /**
   * Swaps the {@link GLContext} and all {@link GLEventListener} between {@link GLAutoDrawable} <code>a</code> and <code>b</code>,
   * while preserving it's initialized state, resets the GL-Viewport and issuing {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int) reshape(..)}.
   * <p>
   * If an {@link GLAnimatorControl} is being attached to {@link GLAutoDrawable} <code>a</code> or <code>b</code> 
   * and the current thread is different than {@link GLAnimatorControl#getThread() the animator's thread}, it is paused during the operation.
   * </p>
   * @param a
   * @param b
   */
  public static final void swapGLContextAndAllGLEventListener(GLAutoDrawable a, GLAutoDrawable b) {
    final List<GLRunnable> aGLCmds = new ArrayList<GLRunnable>();
    final List<GLRunnable> bGLCmds = new ArrayList<GLRunnable>();
    final GLAnimatorControl aAnim = a.getAnimator();
    final GLAnimatorControl bAnim = b.getAnimator();    
    final boolean aIsPaused = isAnimatorAnimatingOnOtherThread(aAnim) && aAnim.pause();
    final boolean bIsPaused = isAnimatorAnimatingOnOtherThread(bAnim) && bAnim.pause();
    
    //
    // remove and cache all GLEventListener and their init-state
    //
    final int aSz = a.getGLEventListenerCount();
    final GLEventListener[] aGLE = new GLEventListener[aSz];
    final boolean[] aInit = new boolean[aSz];
    for(int i=0; i<aSz; i++) {
        final GLEventListener l = a.getGLEventListener(0);
        aInit[i] = a.getGLEventListenerInitState(l);
        aGLE[i] = a.removeGLEventListener( l );
    }    
    final int bSz = b.getGLEventListenerCount();
    final GLEventListener[] bGLE = new GLEventListener[bSz];
    final boolean[] bInit = new boolean[bSz];
    for(int i=0; i<bSz; i++) {
        final GLEventListener l = b.getGLEventListener(0);
        bInit[i] = b.getGLEventListenerInitState(l);
        bGLE[i] = b.removeGLEventListener( l );
    }
    
    //
    // trigger glFinish to sync GL ctx
    //
    a.invoke(true, glFinish);
    b.invoke(true, glFinish);
    
    //
    // switch context and
    // trigger GL-Viewport reset and reshape of all initialized GLEventListeners
    //
    b.setContext( a.setContext( b.getContext() ) );
    aGLCmds.add(setViewport);
    bGLCmds.add(setViewport);
    for(int i=0; i<aSz; i++) {
        if( aInit[i] ) {
            bGLCmds.add(new ReshapeGLEventListener(aGLE[i]));
        }
    }    
    for(int i=0; i<bSz; i++) {
        if( bInit[i] ) {
            aGLCmds.add(new ReshapeGLEventListener(bGLE[i]));
        }
    }
    aGLCmds.add(glFinish);
    bGLCmds.add(glFinish);
    a.invoke(true, aGLCmds);
    b.invoke(true, bGLCmds);
    
    // add all cached GLEventListener to their destination and fix their init-state
    for(int i=0; i<bSz; i++) {
        final GLEventListener l = bGLE[i];
        a.addGLEventListener( l );
        if( bInit[i] ) {
            a.setGLEventListenerInitState(l, true);
        } // else uninitialized is default after add
    }    
    for(int i=0; i<aSz; i++) {
        final GLEventListener l = aGLE[i];
        b.addGLEventListener( l );
        if( aInit[i] ) {
            b.setGLEventListenerInitState(l, true);
        } // else uninitialized is default after add
    }
    
    if(aIsPaused) { aAnim.resume(); }
    if(bIsPaused) { bAnim.resume(); }
  }
  
  static GLRunnable setViewport = new GLRunnable() {
    @Override
    public boolean run(GLAutoDrawable drawable) {
        drawable.getGL().glViewport(0, 0, drawable.getWidth(), drawable.getHeight());
        return true;
    }            
  };
  static GLRunnable glFinish = new GLRunnable() {
    @Override
    public boolean run(GLAutoDrawable drawable) {
        drawable.getGL().glFinish();
        return true;
    }            
  };
  
  private static class ReshapeGLEventListener implements GLRunnable {
    private GLEventListener listener;
    ReshapeGLEventListener(GLEventListener listener) {
        this.listener = listener;
    }
    @Override
    public boolean run(GLAutoDrawable drawable) {
        listener.reshape(drawable, 0, 0, drawable.getWidth(), drawable.getHeight());
        return true;
    }      
  }

  /** 
   * Swaps the {@link GLContext} of given {@link GLAutoDrawable} 
   * and {@link GLAutoDrawable#disposeGLEventListener(GLEventListener, boolean) disposes} 
   * each {@link GLEventListener} w/o removing it.
   * <p>
   * The GL-Viewport is reset and {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int) reshape(..)} issued implicit.
   * </p> 
   * <p>
   * If an {@link GLAnimatorControl} is being attached to GLAutoDrawable src or dest and the current thread is different 
   * than {@link GLAnimatorControl#getThread() the animator's thread}, it is paused during the operation.
   * </p>
   * @param src
   * @param dest
   */
  public static final void swapGLContext(GLAutoDrawable src, GLAutoDrawable dest) {    
    final GLAnimatorControl aAnim = src.getAnimator();
    final GLAnimatorControl bAnim = dest.getAnimator();    
    final boolean aIsPaused = isAnimatorAnimatingOnOtherThread(aAnim) && aAnim.pause();
    final boolean bIsPaused = isAnimatorAnimatingOnOtherThread(bAnim) && bAnim.pause();
    
    for(int i = src.getGLEventListenerCount() - 1; 0 <= i; i--) {
        src.disposeGLEventListener(src.getGLEventListener(i), false);
    }
    for(int i = dest.getGLEventListenerCount() - 1; 0 <= i; i--) {
        dest.disposeGLEventListener(dest.getGLEventListener(i), false);
    }
    dest.setContext( src.setContext( dest.getContext() ) );
    
    src.invoke(true, setViewport);
    dest.invoke(true, setViewport);
    
    if(aIsPaused) { aAnim.resume(); }
    if(bIsPaused) { bAnim.resume(); }
  }

}
