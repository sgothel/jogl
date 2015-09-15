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

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.SourcedInterruptedException;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;

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
    private ThreadGroup threadGroup;
    private Runnable runnable;
    private boolean runAsFastAsPossible;
    boolean isAnimating;
    volatile boolean pauseIssued;
    volatile boolean stopIssued;

    /**
     * Creates a new, empty Animator.
     */
    public Animator() {
        super();
        if(DEBUG) {
            System.err.println("Animator created");
        }
    }

    /**
     * Creates a new Animator w/ an associated ThreadGroup.
     */
    public Animator(final ThreadGroup tg) {
        super();
        setThreadGroup(tg);
        if(DEBUG) {
            System.err.println("Animator created, ThreadGroup: "+threadGroup);
        }
    }

    /**
     * Creates a new Animator for a particular drawable.
     */
    public Animator(final GLAutoDrawable drawable) {
        super();
        add(drawable);
        if(DEBUG) {
            System.err.println("Animator created, w/ "+drawable);
        }
    }

    /**
     * Creates a new Animator w/ an associated ThreadGroup for a particular drawable.
     */
    public Animator(final ThreadGroup tg, final GLAutoDrawable drawable) {
        super();
        setThreadGroup(tg);
        add(drawable);
        if(DEBUG) {
            System.err.println("Animator created, ThreadGroup: "+threadGroup+" and "+drawable);
        }
    }

    @Override
    protected final String getBaseName(final String prefix) {
        return prefix + "Animator" ;
    }

    /**
     * Sets a flag in this Animator indicating that it is to run as
     * fast as possible. By default there is a brief pause in the
     * animation loop which prevents the CPU from getting swamped.
     * This method may not have an effect on subclasses.
     */
    public final synchronized void setRunAsFastAsPossible(final boolean runFast) {
        runAsFastAsPossible = runFast;
    }

    class MainLoop implements Runnable {
        @Override
        public String toString() {
            return "[started "+isStarted()+", animating "+isAnimating()+", paused "+isPaused()+", drawable "+drawables.size()+", drawablesEmpty "+drawablesEmpty+"]";
        }

        @Override
        public void run() {
            ThreadDeath caughtThreadDeath = null;
            UncaughtAnimatorException caughtException = null;

            try {
                synchronized (Animator.this) {
                    if(DEBUG) {
                        System.err.println("Animator start on " + getThreadName() + ": " + toString());
                    }
                    fpsCounter.resetFPSCounter();
                    animThread = Thread.currentThread();
                    isAnimating = false;
                    // 'waitForStartedCondition' wake-up is handled below!
                }

                while (!stopIssued) {
                    synchronized (Animator.this) {
                        // Pause; Also don't consume CPU unless there is work to be done and not paused
                        boolean ectCleared = false;
                        while ( !stopIssued && ( pauseIssued || drawablesEmpty ) ) {
                            if( drawablesEmpty ) {
                                pauseIssued = true;
                            }
                            final boolean wasPaused = pauseIssued;
                            if (DEBUG) {
                                System.err.println("Animator pause on " + animThread.getName() + ": " + toString());
                            }
                            if ( exclusiveContext && !drawablesEmpty && !ectCleared ) {
                                ectCleared = true;
                                setDrawablesExclCtxState(false);
                                try {
                                    display(); // propagate exclusive context -> off!
                                } catch (final UncaughtAnimatorException dre) {
                                    caughtException = dre;
                                    stopIssued = true;
                                    break; // end pause loop
                                }
                            }
                            isAnimating = false;
                            Animator.this.notifyAll();
                            try {
                                Animator.this.wait();
                            } catch (final InterruptedException e) {
                                caughtException = new UncaughtAnimatorException(null, SourcedInterruptedException.wrap(e));
                                stopIssued = true;
                                break; // end pause loop
                            }
                            if (wasPaused) {
                                // resume from pause -> reset counter
                                fpsCounter.resetFPSCounter();
                                if (DEBUG) {
                                    System.err.println("Animator resume on " + animThread.getName() + ": " + toString());
                                }
                            }
                        }
                        if (!stopIssued && !isAnimating) {
                            // Wakes up 'waitForStartedCondition' sync
                            // - and -
                            // Resume from pause or drawablesEmpty,
                            // implies !pauseIssued and !drawablesEmpty
                            isAnimating = true;
                            setDrawablesExclCtxState(exclusiveContext); // may re-enable exclusive context
                            Animator.this.notifyAll();
                        }
                    } // sync Animator.this
                    if ( !pauseIssued && !stopIssued ) {
                        try {
                            display();
                        } catch (final UncaughtAnimatorException dre) {
                            caughtException = dre;
                            stopIssued = true;
                            break; // end animation loop
                        }
                        if ( !runAsFastAsPossible ) {
                            // Avoid swamping the CPU
                            Thread.yield();
                        }
                    }
                }
            } catch(final ThreadDeath td) {
                if(DEBUG) {
                    ExceptionUtils.dumpThrowable("", td);
                }
                caughtThreadDeath = td;
            }
            if( exclusiveContext && !drawablesEmpty ) {
                setDrawablesExclCtxState(false);
                try {
                    display(); // propagate exclusive context -> off!
                } catch (final UncaughtAnimatorException dre) {
                    if( null == caughtException ) {
                        caughtException = dre;
                    } else {
                        ExceptionUtils.dumpThrowable("(setExclusiveContextThread)", dre);
                    }
                }
            }
            boolean flushGLRunnables = false;
            boolean throwCaughtException = false;
            synchronized (Animator.this) {
                if(DEBUG) {
                    System.err.println("Animator stop on " + animThread.getName() + ": " + toString());
                    if( null != caughtException ) {
                        ExceptionUtils.dumpThrowable("", caughtException);
                    }
                }
                stopIssued = false;
                pauseIssued = false;
                isAnimating = false;
                if( null != caughtException ) {
                    flushGLRunnables = true;
                    throwCaughtException = !handleUncaughtException(caughtException);
                }
                animThread = null;
                Animator.this.notifyAll();
            }
            if( flushGLRunnables ) {
                flushGLRunnables();
            }
            if( throwCaughtException ) {
                throw caughtException;
            }
            if( null != caughtThreadDeath ) {
                throw caughtThreadDeath;
            }
        }
    }

    @Override
    public final synchronized boolean isAnimating() {
        return animThread != null && isAnimating ;
    }

    @Override
    public final synchronized boolean isPaused() {
        return animThread != null && pauseIssued ;
    }

    /**
     * Set a {@link ThreadGroup} for the {@link #getThread() animation thread}.
     *
     * @param tg the {@link ThreadGroup}
     * @throws GLException if the animator has already been started
     */
    public final synchronized void setThreadGroup(final ThreadGroup tg) throws GLException {
        if ( isStarted() ) {
            throw new GLException("Animator already started.");
        }
        threadGroup = tg;
    }

    @Override
    public final synchronized boolean start() {
        if ( isStarted() ) {
            return false;
        }
        if (runnable == null) {
            runnable = new MainLoop();
        }
        fpsCounter.resetFPSCounter();
        final Thread thread = new InterruptSource.Thread(threadGroup, runnable, getThreadName()+"-"+baseName);
        thread.setDaemon(false); // force to be non daemon, regardless of parent thread
        if(DEBUG) {
            final Thread ct = Thread.currentThread();
            System.err.println("Animator "+ct.getName()+"[daemon "+ct.isDaemon()+"]: starting "+thread.getName()+"[daemon "+thread.isDaemon()+"]");
        }
        thread.start();
        return finishLifecycleAction(waitForStartedCondition, 0);
    }
    private final Condition waitForStartedCondition = new Condition() {
        @Override
        public boolean eval() {
            return !isStarted() || (!drawablesEmpty && !isAnimating) ;
        } };

    @Override
    public final synchronized boolean stop() {
        if ( !isStarted() ) {
            return false;
        }
        stopIssued = true;
        return finishLifecycleAction(waitForStoppedCondition, 0);
    }
    private final Condition waitForStoppedCondition = new Condition() {
        @Override
        public boolean eval() {
            return isStarted();
        } };

    @Override
    public final synchronized boolean pause() {
        if ( !isStarted() || pauseIssued ) {
            return false;
        }
        pauseIssued = true;
        return finishLifecycleAction(waitForPausedCondition, 0);
    }
    private final Condition waitForPausedCondition = new Condition() {
        @Override
        public boolean eval() {
            // end waiting if stopped as well
            return isStarted() && isAnimating;
        } };

    @Override
    public final synchronized boolean resume() {
        if ( !isStarted() || !pauseIssued ) {
            return false;
        }
        pauseIssued = false;
        return finishLifecycleAction(waitForResumeCondition, 0);
    }
    private final Condition waitForResumeCondition = new Condition() {
        @Override
        public boolean eval() {
            // end waiting if stopped as well
            return isStarted() && ( !drawablesEmpty && !isAnimating || drawablesEmpty && !pauseIssued ) ;
        } };
}
