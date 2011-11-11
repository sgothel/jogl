/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import jogamp.opengl.Debug;
import jogamp.opengl.FPSCounterImpl;

import java.io.PrintStream;
import java.util.ArrayList;

import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLProfile;

/**
 * Base implementation of GLAnimatorControl<br>
 * <p>
 * The change synchronization is done via synchronized blocks on the AnimatorBase instance.<br>
 * Status get / set activity is synced with a RecursiveLock, used as a memory barrier.<br>
 * This is suitable, since all change requests are allowed to be expensive
 * as they are not expected to be called at every frame.
 * </p>
 */
public abstract class AnimatorBase implements GLAnimatorControl {
    protected static final boolean DEBUG = Debug.debug("Animator");

    private static int animatorCount = 0;

    public interface AnimatorImpl {
        void display(ArrayList<GLAutoDrawable> drawables, boolean ignoreExceptions, boolean printExceptions);
        boolean blockUntilDone(Thread thread);
    }

    protected ArrayList<GLAutoDrawable> drawables = new ArrayList<GLAutoDrawable>();
    protected boolean drawablesEmpty;
    protected AnimatorImpl impl;
    protected String baseName;
    protected Thread animThread;
    protected boolean ignoreExceptions;
    protected boolean printExceptions;
    protected FPSCounterImpl fpsCounter = new FPSCounterImpl();    
    protected RecursiveLock stateSync = LockFactory.createRecursiveLock();

    /** Creates a new, empty Animator. */
    public AnimatorBase() {
        if(GLProfile.isAWTAvailable()) {
            try {
                impl = (AnimatorImpl) Class.forName("com.jogamp.opengl.util.AWTAnimatorImpl").newInstance();
                baseName = "AWTAnimator";
            } catch (Exception e) { e.printStackTrace(); }
        }
        if(null==impl) {
            impl = new DefaultAnimatorImpl();
            baseName = "Animator";
        }
        synchronized (Animator.class) {
            animatorCount++;
            baseName = baseName.concat("-"+animatorCount);
            drawablesEmpty = true;
        }
    }

    protected abstract String getBaseName(String prefix);

    public synchronized void add(GLAutoDrawable drawable) {
        if(DEBUG) {
            System.err.println("Animator add: "+drawable.hashCode()+" - "+Thread.currentThread());
        }
        boolean paused = pause();
        drawables.add(drawable);
        drawablesEmpty = drawables.size() == 0;
        drawable.setAnimator(this);
        if(paused) {
            resume();
        }
        if(impl.blockUntilDone(animThread)) {
            while(isStarted() && !isPaused() && !isAnimating()) {
                try {
                    wait();
                } catch (InterruptedException ie) { }
            }
        }
        notifyAll();
    }

    public synchronized void remove(GLAutoDrawable drawable) {
        if(DEBUG) {
            System.err.println("Animator remove: "+drawable.hashCode()+" - "+Thread.currentThread() + ": "+toString());
        }

        boolean paused = pause();
        drawables.remove(drawable);
        drawablesEmpty = drawables.size() == 0;
        drawable.setAnimator(null);
        if(paused) {
            resume();
        }
        if(impl.blockUntilDone(animThread)) {
            while(isStarted() && drawablesEmpty && isAnimating()) {
                try {
                    wait();
                } catch (InterruptedException ie) { }
            }
        }
        notifyAll();
    }

    /** Called every frame to cause redrawing of all of the
        GLAutoDrawables this Animator manages. Subclasses should call
        this to get the most optimized painting behavior for the set of
        components this Animator manages, in particular when multiple
        lightweight widgets are continually being redrawn. */
    protected void display() {
        impl.display(drawables, ignoreExceptions, printExceptions);
        fpsCounter.tickFPS();
    }

    public final void setUpdateFPSFrames(int frames, PrintStream out) {
        fpsCounter.setUpdateFPSFrames(frames, out);
    }
    
    public final void resetFPSCounter() {
        fpsCounter.resetFPSCounter();
    }

    public final int getUpdateFPSFrames() {
        return fpsCounter.getUpdateFPSFrames();
    }
    
    public final long getFPSStartTime()   {
        return fpsCounter.getFPSStartTime();
    }

    public final long getLastFPSUpdateTime() {
        return fpsCounter.getLastFPSUpdateTime();
    }

    public final long getLastFPSPeriod() {
        return fpsCounter.getLastFPSPeriod();
    }
    
    public final float getLastFPS() {
        return fpsCounter.getLastFPS();
    }
    
    public final int getTotalFPSFrames() {
        return fpsCounter.getTotalFPSFrames();
    }

    public final long getTotalFPSDuration() {
        return fpsCounter.getTotalFPSDuration();
    }
    
    public final float getTotalFPS() {
        return fpsCounter.getTotalFPS();
    }        

    public final Thread getThread() {
        stateSync.lock();
        try {
            return animThread;
        } finally {
            stateSync.unlock();
        }
    }

    /** Sets a flag causing this Animator to ignore exceptions produced
    while redrawing the drawables. By default this flag is set to
    false, causing any exception thrown to halt the Animator. */
    public void setIgnoreExceptions(boolean ignoreExceptions) {
        this.ignoreExceptions = ignoreExceptions;
    }

    /** Sets a flag indicating that when exceptions are being ignored by
    this Animator (see {@link #setIgnoreExceptions}), to print the
    exceptions' stack traces for diagnostic information. Defaults to
    false. */
    public void setPrintExceptions(boolean printExceptions) {
        this.printExceptions = printExceptions;
    }

    public String toString() {
        return getClass().getName()+"[started "+isStarted()+", animating "+isAnimating()+", paused "+isPaused()+", drawable "+drawables.size()+"]";
    }
}
