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
import javax.media.opengl.GLException;




/** <P> An Animator can be attached to one or more {@link
    GLAutoDrawable}s to drive their display() methods in a loop. </P>

    <P> The Animator class creates a background thread in which the
    calls to <code>display()</code> are performed. After each drawable
    has been redrawn, a brief pause is performed to avoid swamping the
    CPU, unless {@link #setRunAsFastAsPossible} has been called.  </P>
*/

public class Animator extends AnimatorBase {

    protected ThreadGroup threadGroup;
    private Runnable runnable;
    private boolean runAsFastAsPossible;
    protected boolean isAnimating;
    protected boolean isPaused;
    protected volatile boolean shouldPause;
    protected volatile boolean shouldStop;

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
        runAsFastAsPossible = runFast;
    }

    class MainLoop implements Runnable {
        public void run() {
            try {
                if(DEBUG) {
                    System.err.println("Animator started: "+Thread.currentThread());
                }
                startTime = System.currentTimeMillis();
                curTime   = startTime;
                totalFrames = 0;

                synchronized (Animator.this) {
                    isAnimating = true;
                    isPaused = false;
                    Animator.this.notifyAll();
                }

                while (!shouldStop) {
                    // Don't consume CPU unless there is work to be done and not paused
                    if ( !shouldStop && ( shouldPause || drawables.size() == 0 ) ) {
                        synchronized (Animator.this) {
                            boolean wasPaused = false;
                            while ( !shouldStop && ( shouldPause || drawables.size() == 0 )  ) {
                                isAnimating = false;
                                isPaused = true;
                                wasPaused = true;
                                if(DEBUG) {
                                    System.err.println("Animator paused: "+Thread.currentThread());
                                }
                                Animator.this.notifyAll();
                                try {
                                    Animator.this.wait();
                                } catch (InterruptedException e) {
                                }
                            }
                            if ( wasPaused ) {
                                startTime = System.currentTimeMillis();
                                curTime   = startTime;
                                totalFrames = 0;
                                isAnimating = true;
                                isPaused = false;
                                if(DEBUG) {
                                    System.err.println("Animator resumed: "+Thread.currentThread());
                                }
                                Animator.this.notifyAll();
                            }
                        }
                    }
                    if ( !shouldStop && !shouldPause) {
                        display();
                        if ( !runAsFastAsPossible) {
                            // Avoid swamping the CPU
                            Thread.yield();
                        }
                    }
                }
            } finally {
                synchronized (Animator.this) {
                    if(DEBUG) {
                        System.err.println("Animator stopped: "+Thread.currentThread());
                    }
                    shouldStop = false;
                    shouldPause = false;
                    thread = null;
                    isAnimating = false;
                    isPaused = false;
                    Animator.this.notifyAll();
                }
            }
        }
    }

    public final synchronized boolean isStarted() {
        return (thread != null);
    }

    public final synchronized boolean isAnimating() {
        return (thread != null) && isAnimating;
    }

    public final synchronized boolean isPaused() {
        return (thread != null) && isPaused;
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
        boolean doWait = !impl.skipWaitForCompletion(thread);
        if (doWait) {
            while (condition.result()) {
                try {
                    wait();
                } catch (InterruptedException ie) {
                }
            }
        }
        if(DEBUG) {
            System.err.println("finishLifecycleAction(" + condition.getClass().getName() + "): finished - waited " + doWait +
                    ", started: " + isStarted() +", animating: " + isAnimating() +
                    ", paused: " + isPaused());
        }
    }

    public synchronized void start() {
        boolean started = null != thread;
        if ( started || isAnimating ) {
            throw new GLException("Already running (started "+started+" (false), animating "+isAnimating+" (false))");
        }
        if (runnable == null) {
            runnable = new MainLoop();
        }
        resetCounter();
        int id;
        String threadName = Thread.currentThread().getName()+"-"+baseName;
        if(null==threadGroup) {
            thread = new Thread(runnable, threadName);
        } else {
            thread = new Thread(threadGroup, runnable, threadName);
        }
        thread.start();

        finishLifecycleAction(waitForStartedCondition);
    }
    private class WaitForStartedCondition implements Condition {
        public boolean result() {
            // cont waiting until actually is animating
            return !isAnimating || thread == null;
        }
    }
    Condition waitForStartedCondition = new WaitForStartedCondition();

    public synchronized void stop() {
        boolean started = null != thread;
        if ( !started ) {
            throw new GLException("Not started");
        }
        shouldStop = true;
        notifyAll();

        finishLifecycleAction(waitForStoppedCondition);
    }
    private class WaitForStoppedCondition implements Condition {
        public boolean result() {
            return thread != null;
        }
    }
    Condition waitForStoppedCondition = new WaitForStoppedCondition();

    public synchronized void pause() {
        boolean started = null != thread;
        if ( !started || !isAnimating || shouldPause ) {
            throw new GLException("Invalid state (started "+started+" (true), animating "+isAnimating+" (true), paused "+shouldPause+" (false) )");
        }
        shouldPause = true;
        notifyAll();

        finishLifecycleAction(waitForPausedCondition);
    }
    private class WaitForPausedCondition implements Condition {
        public boolean result() {
            // end waiting if stopped as well
            return (!isPaused || isAnimating) && thread != null;
        }
    }
    Condition waitForPausedCondition = new WaitForPausedCondition();

    public synchronized void resume() {
        boolean started = null != thread;
        if ( !started || !shouldPause ) {
            throw new GLException("Invalid state (started "+started+" (true), paused "+shouldPause+" (true) )");
        }        
        shouldPause = false;
        notifyAll();

        finishLifecycleAction(waitForResumedCondition);
    }
    private class WaitForResumedCondition implements Condition {
        public boolean result() {
            // end waiting if stopped as well
            return (isPaused || !isAnimating) && thread != null;
        }
    }
    Condition waitForResumedCondition = new WaitForResumedCondition();

    protected final boolean getShouldPause() {
        return shouldPause;
    }

    protected final boolean getShouldStop() {
        return shouldStop;
    }

}
