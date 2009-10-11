/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.opengl.util;

import java.util.*;

import javax.media.opengl.*;

import com.sun.opengl.impl.Debug;

/** <P> An Animator can be attached to one or more {@link
    GLAutoDrawable}s to drive their display() methods in a loop. </P>

    <P> The Animator class creates a background thread in which the
    calls to <code>display()</code> are performed. After each drawable
    has been redrawn, a brief pause is performed to avoid swamping the
    CPU, unless {@link #setRunAsFastAsPossible} has been called.  </P>
*/

public class Animator {
    protected static final boolean DEBUG = Debug.debug("Animator");

    private volatile ArrayList/*<GLAutoDrawable>*/ drawables = new ArrayList();
    private AnimatorImpl impl;
    private Runnable runnable;
    private boolean runAsFastAsPossible;
    protected ThreadGroup threadGroup;
    protected Thread thread;
    protected volatile boolean shouldStop;
    protected boolean ignoreExceptions;
    protected boolean printExceptions;

    /** Creates a new, empty Animator. */
    public Animator(ThreadGroup tg) {

        if(GLProfile.isAWTJOGLAvailable()) {
            try {
                impl = (AnimatorImpl) Class.forName("com.sun.opengl.util.awt.AWTAnimatorImpl").newInstance();
            } catch (Exception e) { }
        }
        if(null==impl) {
            impl = new AnimatorImpl();
        }
        threadGroup = tg;

        if(DEBUG) {
            System.out.println("Animator created, ThreadGroup: "+threadGroup);
        }
    }

    public Animator() {
        this((ThreadGroup)null);
    }

    /** Creates a new Animator for a particular drawable. */
    public Animator(GLAutoDrawable drawable) {
        this((ThreadGroup)null);
        add(drawable);
    }

    /** Creates a new Animator for a particular drawable. */
    public Animator(ThreadGroup tg, GLAutoDrawable drawable) {
        this(tg);
        add(drawable);
    }

    /** Adds a drawable to the list managed by this Animator. */
    public synchronized void add(GLAutoDrawable drawable) {
        ArrayList newList = (ArrayList) drawables.clone();
        newList.add(drawable);
        drawables = newList;
        notifyAll();
    }

    /** Removes a drawable from the list managed by this Animator. */
    public synchronized void remove(GLAutoDrawable drawable) {
        ArrayList newList = (ArrayList) drawables.clone();
        newList.remove(drawable);
        drawables = newList;
    }

    /** Returns an iterator over the drawables managed by this
        Animator. */
    public Iterator/*<GLAutoDrawable>*/ drawableIterator() {
        return drawables.iterator();
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

    /** Sets a flag in this Animator indicating that it is to run as
        fast as possible. By default there is a brief pause in the
        animation loop which prevents the CPU from getting swamped.
        This method may not have an effect on subclasses. */
    public final void setRunAsFastAsPossible(boolean runFast) {
        runAsFastAsPossible = runFast;
    }

    /** Called every frame to cause redrawing of all of the
        GLAutoDrawables this Animator manages. Subclasses should call
        this to get the most optimized painting behavior for the set of
        components this Animator manages, in particular when multiple
        lightweight widgets are continually being redrawn. */
    protected void display() {
        impl.display(this, ignoreExceptions, printExceptions);
    }

    class MainLoop implements Runnable {
        public void run() {
            try {
                if(DEBUG) {
                    System.out.println("Animator started: "+Thread.currentThread());
                }
                while (!shouldStop) {
                    // Don't consume CPU unless there is work to be done
                    if (drawables.size() == 0) {
                        synchronized (Animator.this) {
                            while (drawables.size() == 0 && !shouldStop) {
                                try {
                                    Animator.this.wait();
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }
                    display();
                    if (!runAsFastAsPossible) {
                        // Avoid swamping the CPU
                        Thread.yield();
                    }
                }
                if(DEBUG) {
                    System.out.println("Animator stopped: "+Thread.currentThread());
                }
            } finally {
                shouldStop = false;
                synchronized (Animator.this) {
                    thread = null;
                    Animator.this.notify();
                }
            }
        }
    }

    /** Starts this animator. */
    public synchronized void start() {
        if (thread != null) {
            throw new GLException("Already started");
        }
        if (runnable == null) {
            runnable = new MainLoop();
        }
        if(null==threadGroup) {
            thread = new Thread(runnable);
        } else {
            thread = new Thread(threadGroup, runnable);
        }
        thread.start();
    }

    /** Indicates whether this animator is currently running. This
        should only be used as a heuristic to applications because in
        some circumstances the Animator may be in the process of
        shutting down and this method will still return true. */
    public synchronized boolean isAnimating() {
        return (thread != null);
    }

    /** Stops this animator. In most situations this method blocks until
        completion, except when called from the animation thread itself
        or in some cases from an implementation-internal thread like the
        AWT event queue thread. */
    public synchronized void stop() {
        shouldStop = true;
        notifyAll();

        // It's hard to tell whether the thread which calls stop() has
        // dependencies on the Animator's internal thread. Currently we
        // use a couple of heuristics to determine whether we should do
        // the blocking wait().
        if (impl.skipWaitForStop(thread)) {
            return;
        }

        while (shouldStop && thread != null) {
            try {
                wait();
            } catch (InterruptedException ie) {
            }
        }
    }
}
