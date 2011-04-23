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

package com.jogamp.opengl.util;

import javax.media.opengl.GLAutoDrawable;


/** <P> An Animator can be attached to one or more {@link
    GLAutoDrawable}s to drive their display() methods in a loop. </P>

    <P> The Animator class creates a background thread in which the
    calls to <code>display()</code> are performed. After each drawable
    has been redrawn, a brief pause is performed to avoid swamping the
    CPU, unless {@link #setRunAsFastAsPossible} has been called.  </P>

 * <p>
 * The Animator execution thread does not run as a daemon thread,
 * so it is able to keep an application from terminating.<br>
 * Call {@link #stop() } to terminate the animation and it's execution thread.
 * </p>
*/

public class Animator extends AnimatorBase {

    protected ThreadGroup threadGroup;
    private Runnable runnable;
    private boolean runAsFastAsPossible;
    protected boolean isAnimating;
    protected boolean pauseIssued;
    protected volatile boolean stopIssued;

    public Animator() {
        super();
        if(DEBUG) {
            System.err.println("Animator created");
        }
    }

    public Animator(ThreadGroup tg) {
        super();
        threadGroup = tg;

        if(DEBUG) {
            System.err.println("Animator created, ThreadGroup: "+threadGroup);
        }
    }

    /** Creates a new Animator for a particular drawable. */
    public Animator(GLAutoDrawable drawable) {
        super();
        add(drawable);
    }

    /** Creates a new Animator for a particular drawable. */
    public Animator(ThreadGroup tg, GLAutoDrawable drawable) {
        this(tg);
        add(drawable);
    }

    protected String getBaseName(String prefix) {
        return prefix + "Animator" ;
    }

    /**
     * Sets a flag in this Animator indicating that it is to run as
     * fast as possible. By default there is a brief pause in the
     * animation loop which prevents the CPU from getting swamped.
     * This method may not have an effect on subclasses.
     */
    public final void setRunAsFastAsPossible(boolean runFast) {
        stateSync.lock();
        try {
            runAsFastAsPossible = runFast;
        } finally {
            stateSync.unlock();
        }
    }

    private void setIsAnimatingSynced(boolean v) {
        stateSync.lock();
        try {
            isAnimating = v;
        } finally {
            stateSync.unlock();
        }
    }

    class MainLoop implements Runnable {
        public String toString() {
            return "[started "+isStartedImpl()+", animating "+isAnimatingImpl()+", paused "+isPausedImpl()+", drawable "+drawables.size()+"]";
        }

        public void run() {
            try {
                synchronized (Animator.this) {
                    if(DEBUG) {
                        System.err.println("Animator start:" + Thread.currentThread() + ": " + toString());
                    }
                    fpsCounter.resetFPSCounter();
                    animThread = Thread.currentThread();
                    setIsAnimatingSynced(false); // barrier
                    Animator.this.notifyAll();
                }

                while (!stopIssued) {
                    synchronized (Animator.this) {
                        // Don't consume CPU unless there is work to be done and not paused
                        while (!stopIssued && (pauseIssued || drawablesEmpty)) {
                            boolean wasPaused = pauseIssued;
                            if (DEBUG) {
                                System.err.println("Animator pause:" + Thread.currentThread() + ": " + toString());
                            }
                            setIsAnimatingSynced(false); // barrier
                            Animator.this.notifyAll();
                            try {
                                Animator.this.wait();
                            } catch (InterruptedException e) {
                            }

                            if (wasPaused) {
                                // resume from pause -> reset counter
                                fpsCounter.resetFPSCounter();
                                if (DEBUG) {
                                    System.err.println("Animator resume:" + Thread.currentThread() + ": " + toString());
                                }
                            }
                        }
                        if (!stopIssued && !isAnimating) {
                            // resume from pause or drawablesEmpty,
                            // implies !pauseIssued and !drawablesEmpty
                            setIsAnimatingSynced(true);
                            Animator.this.notifyAll();
                        }
                    } // sync Animator.this
                    if (!stopIssued) {
                        display();
                    }
                    if (!stopIssued && !runAsFastAsPossible) {
                        // Avoid swamping the CPU
                        Thread.yield();
                    }
                }
            } finally {
                synchronized (Animator.this) {
                    if(DEBUG) {
                        System.err.println("Animator stop " + Thread.currentThread() + ": " + toString());
                    }
                    stopIssued = false;
                    pauseIssued = false;
                    animThread = null;
                    setIsAnimatingSynced(false); // barrier
                    Animator.this.notifyAll();
                }
            }
        }
    }

    private final boolean isStartedImpl() {
        return animThread != null ;
    }
    public final boolean isStarted() {
        stateSync.lock();
        try {
            return animThread != null ;
        } finally {
            stateSync.unlock();
        }
    }

    private final boolean isAnimatingImpl() {
        return animThread != null && isAnimating ;
    }
    public final boolean isAnimating() {
        stateSync.lock();
        try {
            return animThread != null && isAnimating ;
        } finally {
            stateSync.unlock();
        }
    }

    private final boolean isPausedImpl() {
        return animThread != null && pauseIssued ;
    }
    public final boolean isPaused() {
        stateSync.lock();
        try {
            return animThread != null && pauseIssued ;
        } finally {
            stateSync.unlock();
        }
    }

    interface Condition {
        /**
         * @return true if branching (cont waiting, action), otherwise false
         */
        boolean result();
    }

    private synchronized void finishLifecycleAction(Condition condition) {
        // It's hard to tell whether the thread which changes the lifecycle has
        // dependencies on the Animator's internal thread. Currently we
        // use a couple of heuristics to determine whether we should do
        // the blocking wait().
        boolean doWait = !impl.skipWaitForCompletion(animThread);
        if (doWait) {
            while (condition.result()) {
                try {
                    wait();
                } catch (InterruptedException ie) {  }
            }
        }
        if(DEBUG) {
            System.err.println("finishLifecycleAction(" + condition.getClass().getName() + "): finished - waited " + doWait +
                    ", started: " + isStartedImpl() +", animating: " + isAnimatingImpl() +
                    ", paused: " + isPausedImpl() + ", drawables " + drawables.size());
        }
    }

    public synchronized boolean start() {
        if ( isStartedImpl() ) {
            return false;
        }
        if (runnable == null) {
            runnable = new MainLoop();
        }
        fpsCounter.resetFPSCounter();
        String threadName = Thread.currentThread().getName()+"-"+baseName;
        Thread thread;
        if(null==threadGroup) {
            thread = new Thread(runnable, threadName);
        } else {
            thread = new Thread(threadGroup, runnable, threadName);
        }
        thread.start();
        finishLifecycleAction(waitForStartedCondition);
        return true;
    }

    private class WaitForStartedCondition implements Condition {
        public boolean result() {
            return !isStartedImpl() || (!drawablesEmpty && !isAnimating) ;
        }
    }
    Condition waitForStartedCondition = new WaitForStartedCondition();

    public synchronized boolean stop() {
        if ( !isStartedImpl() ) {
            return false;
        }
        stopIssued = true;
        notifyAll();
        finishLifecycleAction(waitForStoppedCondition);
        return true;
    }
    private class WaitForStoppedCondition implements Condition {
        public boolean result() {
            return isStartedImpl();
        }
    }
    Condition waitForStoppedCondition = new WaitForStoppedCondition();

    public synchronized boolean pause() {
        if ( !isStartedImpl() || pauseIssued ) {
            return false;
        }
        stateSync.lock();
        try {
            pauseIssued = true;
        } finally {
            stateSync.unlock();
        }
        notifyAll();
        finishLifecycleAction(waitForPausedCondition);
        return true;
    }
    private class WaitForPausedCondition implements Condition {
        public boolean result() {
            // end waiting if stopped as well
            return isAnimating && isStartedImpl();
        }
    }
    Condition waitForPausedCondition = new WaitForPausedCondition();

    public synchronized boolean resume() {
        if ( !isStartedImpl() || !pauseIssued ) {
            return false;
        }
        stateSync.lock();
        try {
            pauseIssued = false;
        } finally {
            stateSync.unlock();
        }
        notifyAll();
        finishLifecycleAction(waitForResumeCondition);
        return true;
    }
    private class WaitForResumeCondition implements Condition {
        public boolean result() {
            // end waiting if stopped as well
            return !drawablesEmpty && !isAnimating && isStartedImpl();
        }
    }
    Condition waitForResumeCondition = new WaitForResumeCondition();
}
