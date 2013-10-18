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

import java.util.Timer;
import java.util.TimerTask;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLException;

/**
 * An Animator subclass which attempts to achieve a target
 * frames-per-second rate to avoid using all CPU time. The target FPS
 * is only an estimate and is not guaranteed.
 * <p>
 * The Animator execution thread does not run as a daemon thread,
 * so it is able to keep an application from terminating.<br>
 * Call {@link #stop() } to terminate the animation and it's execution thread.
 * </p>
 */
public class FPSAnimator extends AnimatorBase {
    private Timer timer = null;
    private MainTask task = null;
    private int fps;
    private boolean scheduleAtFixedRate;
    private boolean isAnimating;         // MainTask feedback
    private volatile boolean shouldRun;  // MainTask trigger
    private volatile boolean shouldStop; // MainTask trigger

    protected String getBaseName(String prefix) {
        return "FPS" + prefix + "Animator" ;
    }

    /** Creates an FPSAnimator with a given target frames-per-second
    value. Equivalent to <code>FPSAnimator(null, fps)</code>. */
    public FPSAnimator(int fps) {
        this(null, fps);
    }

    /** Creates an FPSAnimator with a given target frames-per-second
    value and a flag indicating whether to use fixed-rate
    scheduling. Equivalent to <code>FPSAnimator(null, fps,
    scheduleAtFixedRate)</code>. */
    public FPSAnimator(int fps, boolean scheduleAtFixedRate) {
        this(null, fps, scheduleAtFixedRate);
    }

    /** Creates an FPSAnimator with a given target frames-per-second
    value and an initial drawable to animate. Equivalent to
    <code>FPSAnimator(null, fps, false)</code>. */
    public FPSAnimator(GLAutoDrawable drawable, int fps) {
        this(drawable, fps, false);
    }

    /** Creates an FPSAnimator with a given target frames-per-second
    value, an initial drawable to animate, and a flag indicating
    whether to use fixed-rate scheduling. */
    public FPSAnimator(GLAutoDrawable drawable, int fps, boolean scheduleAtFixedRate) {
        super();
        this.fps = fps;
        if (drawable != null) {
            add(drawable);
        }
        this.scheduleAtFixedRate = scheduleAtFixedRate;
    }

    /**
     * @param fps
     * @throws GLException if the animator has already been started
     */
    public final synchronized void setFPS(int fps) throws GLException {
        if ( isStartedImpl() ) {
            throw new GLException("Animator already started.");
        }
        this.fps = fps;
    }
    public final int getFPS() { return fps; }

    class MainTask extends TimerTask {
        private boolean justStarted;
        private boolean alreadyStopped;
        private boolean alreadyPaused;

        public MainTask() {
        }

        public void start(Timer timer) {
            fpsCounter.resetFPSCounter();
            shouldRun = true;
            shouldStop = false;

            justStarted = true;
            alreadyStopped = false;
            alreadyPaused = false;

            final long period = 0 < fps ? (long) (1000.0f / (float) fps) : 1; // 0 -> 1: IllegalArgumentException: Non-positive period
            if (scheduleAtFixedRate) {
                timer.scheduleAtFixedRate(this, 0, period);
            } else {
                timer.schedule(this, 0, period);
            }
        }

        public boolean isActive() { return !alreadyStopped && !alreadyPaused; }

        public String toString() {
            return "Task[thread "+animThread+", stopped "+alreadyStopped+", paused "+alreadyPaused+" shouldRun "+shouldRun+", shouldStop "+shouldStop+" -- started "+isStartedImpl()+", animating "+isAnimatingImpl()+", paused "+isPausedImpl()+", drawable "+drawables.size()+", drawablesEmpty "+drawablesEmpty+"]";
        }

        public void run() {
            if( justStarted ) {
                justStarted = false;
                synchronized (FPSAnimator.this) {
                    animThread = Thread.currentThread();
                    if(DEBUG) {
                        System.err.println("FPSAnimator start/resume:" + Thread.currentThread() + ": " + toString());
                    }
                    isAnimating = true;
                    if( drawablesEmpty ) {
                        shouldRun = false; // isAnimating:=false @ pause below
                    } else {
                        shouldRun = true;
                        setDrawablesExclCtxState(exclusiveContext);
                        FPSAnimator.this.notifyAll();
                    }
                    System.err.println("FPSAnimator P1:" + Thread.currentThread() + ": " + toString());
                }
            }
            if( shouldRun ) {
                display();
            } else if( shouldStop ) { // STOP
                System.err.println("FPSAnimator P4: "+alreadyStopped+", "+ Thread.currentThread() + ": " + toString());
                this.cancel();

                if( !alreadyStopped ) {
                    alreadyStopped = true;
                    if( exclusiveContext && !drawablesEmpty ) {
                        setDrawablesExclCtxState(false);
                        display(); // propagate exclusive change!
                    }
                    synchronized (FPSAnimator.this) {
                        if(DEBUG) {
                            System.err.println("FPSAnimator stop " + Thread.currentThread() + ": " + toString());
                        }
                        animThread = null;
                        isAnimating = false;
                        FPSAnimator.this.notifyAll();
                    }
                }
            } else {
                System.err.println("FPSAnimator P5: "+alreadyPaused+", "+ Thread.currentThread() + ": " + toString());
                this.cancel();

                if( !alreadyPaused ) { // PAUSE
                    alreadyPaused = true;
                    if( exclusiveContext && !drawablesEmpty ) {
                        setDrawablesExclCtxState(false);
                        display(); // propagate exclusive change!
                    }
                    synchronized (FPSAnimator.this) {
                        if(DEBUG) {
                            System.err.println("FPSAnimator pause " + Thread.currentThread() + ": " + toString());
                        }
                        isAnimating = false;
                        FPSAnimator.this.notifyAll();
                    }
                }
            }
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
        return animThread != null && ( !shouldRun && !shouldStop ) ;
    }
    public final boolean isPaused() {
        stateSync.lock();
        try {
            return animThread != null && ( !shouldRun && !shouldStop ) ;
        } finally {
            stateSync.unlock();
        }
    }

    static int timerNo = 0;

    public synchronized boolean start() {
        if ( null != timer || null != task || isStartedImpl() ) {
            return false;
        }
        timer = new Timer( getThreadName()+"-"+baseName+"-Timer"+(timerNo++) );
        task = new MainTask();
        if(DEBUG) {
            System.err.println("FPSAnimator.start() START: "+task+", "+ Thread.currentThread() + ": " + toString());
        }
        task.start(timer);

        final boolean res = finishLifecycleAction( drawablesEmpty ? waitForStartedEmptyCondition : waitForStartedAddedCondition,
                                                   POLLP_WAIT_FOR_FINISH_LIFECYCLE_ACTION);
        if(DEBUG) {
            System.err.println("FPSAnimator.start() END: "+task+", "+ Thread.currentThread() + ": " + toString());
        }
        if( drawablesEmpty ) {
            task.cancel();
            task = null;
        }
        return res;
    }
    private final Condition waitForStartedAddedCondition = new Condition() {
        public boolean eval() {
            return !isStartedImpl() || !isAnimating ;
        } };
    private final Condition waitForStartedEmptyCondition = new Condition() {
        public boolean eval() {
            return !isStartedImpl() || isAnimating ;
        } };

    /** Stops this FPSAnimator. Due to the implementation of the
    FPSAnimator it is not guaranteed that the FPSAnimator will be
    completely stopped by the time this method returns. */
    public synchronized boolean stop() {
        if ( null == timer || !isStartedImpl() ) {
            return false;
        }
        if(DEBUG) {
            System.err.println("FPSAnimator.stop() START: "+task+", "+ Thread.currentThread() + ": " + toString());
        }
        final boolean res;
        if( null == task ) {
            // start/resume case w/ drawablesEmpty
            res = true;
        } else {
            shouldRun = false;
            shouldStop = true;
            res = finishLifecycleAction(waitForStoppedCondition, POLLP_WAIT_FOR_FINISH_LIFECYCLE_ACTION);
        }

        if(DEBUG) {
            System.err.println("FPSAnimator.stop() END: "+task+", "+ Thread.currentThread() + ": " + toString());
        }
        if(null != task) {
            task.cancel();
            task = null;
        }
        if(null != timer) {
            timer.cancel();
            timer = null;
        }
        animThread = null;
        return res;
    }
    private final Condition waitForStoppedCondition = new Condition() {
        public boolean eval() {
            return isStartedImpl();
        } };

    public synchronized boolean pause() {
        if ( !isStartedImpl() || ( null != task && isPausedImpl() ) ) {
            return false;
        }
        if(DEBUG) {
            System.err.println("FPSAnimator.pause() START: "+task+", "+ Thread.currentThread() + ": " + toString());
        }
        final boolean res;
        if( null == task ) {
            // start/resume case w/ drawablesEmpty
            res = true;
        } else {
            shouldRun = false;
            res = finishLifecycleAction(waitForPausedCondition, POLLP_WAIT_FOR_FINISH_LIFECYCLE_ACTION);
        }

        if(DEBUG) {
            System.err.println("FPSAnimator.pause() END: "+task+", "+ Thread.currentThread() + ": " + toString());
        }
        if(null != task) {
            task.cancel();
            task = null;
        }
        return res;
    }
    private final Condition waitForPausedCondition = new Condition() {
        public boolean eval() {
            // end waiting if stopped as well
            return isAnimating && isStartedImpl();
        } };

    public synchronized boolean resume() {
        if ( null != task || !isStartedImpl() || !isPausedImpl() ) {
            return false;
        }
        if(DEBUG) {
            System.err.println("FPSAnimator.resume() START: "+ Thread.currentThread() + ": " + toString());
        }
        final boolean res;
        if( drawablesEmpty ) {
            res = true;
        } else {
            task = new MainTask();
            task.start(timer);
            res = finishLifecycleAction(waitForResumeCondition, POLLP_WAIT_FOR_FINISH_LIFECYCLE_ACTION);
        }
        if(DEBUG) {
            System.err.println("FPSAnimator.resume() END: "+task+", "+ Thread.currentThread() + ": " + toString());
        }
        return res;
    }
    private final Condition waitForResumeCondition = new Condition() {
        public boolean eval() {
            // end waiting if stopped as well
            return !drawablesEmpty && !isAnimating && isStartedImpl();
        } };
}
