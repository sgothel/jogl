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
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
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

package net.java.games.jogl;

import java.awt.EventQueue;
import net.java.games.jogl.impl.SingleThreadedWorkaround;

/** <P> An Animator can be attached to a GLDrawable to drive its
    display() method in a loop. For efficiency, it sets up the
    rendering thread for the drawable to be its own internal thread,
    so it can not be combined with manual repaints of the
    surface. </P>

    <P> The Animator currently contains a workaround for a bug in
    NVidia's drivers (80174). The current semantics are that once an
    Animator is created with a given GLDrawable as a target, repaints
    will likely be suspended for that GLDrawable until the Animator is
    started. This prevents multithreaded access to the context (which
    can be problematic) when the application's intent is for
    single-threaded access within the Animator. It is not guaranteed
    that repaints will be prevented during this time and applications
    should not rely on this behavior for correctness. </P>
*/

public class Animator {
  private GLDrawable drawable;
  private Runnable runnable;
  private Thread thread;
  private boolean shouldStop;

  /** Creates a new Animator for a particular drawable. */
  public Animator(GLDrawable drawable) {
    this.drawable = drawable;

    // Workaround for NVidia driver bug 80174
    if (drawable instanceof GLCanvas) {
      ((GLCanvas) drawable).willSetRenderingThread();
    }
  }

  /** Starts this animator. */
  public synchronized void start() {
    if (thread != null) {
      throw new GLException("Already started");
    }
    if (runnable == null) {
      runnable = new Runnable() {
          public void run() {
            boolean noException = false;
            try {
              // Try to get OpenGL context optimization since we know we
              // will be rendering this one drawable continually from
              // this thread; make the context current once instead of
              // making it current and freeing it each frame.
              drawable.setRenderingThread(Thread.currentThread());

              // Since setRenderingThread is currently advisory (because
              // of the poor JAWT implementation in the Motif AWT, which
              // performs excessive locking) we also prevent repaint(),
              // which is called from the AWT thread, from having an
              // effect for better multithreading behavior. This call is
              // not strictly necessary, but if end users write their
              // own animation loops which update multiple drawables per
              // tick then it may be necessary to enforce the order of
              // updates.
              drawable.setNoAutoRedrawMode(true);

              while (!shouldStop) {
                noException = false;
                drawable.display();
                noException = true;
              }
            } finally {
              shouldStop = false;
              drawable.setNoAutoRedrawMode(false);
              try {
                // The surface is already unlocked and rendering
                // thread is already null if an exception occurred
                // during display(), so don't disable the rendering
                // thread again.
                if (noException) {
                  // Destruction of the underlying GLContext may have
                  // disabled the setRenderingThread optimization out
                  // from under us
                  if (drawable.getRenderingThread() != null) {
                    drawable.setRenderingThread(null);
                  }
                }
              } finally {
                synchronized (Animator.this) {
                  thread = null;
                  Animator.this.notify();
                }
              }
            }
          }
        };
    }
    thread = new Thread(runnable);
    thread.start();
  }

  /** Stops this animator, blocking until the animation thread has
      finished. */
  public synchronized void stop() {
    shouldStop = true;
    // It's hard to tell whether the thread which calls stop() has
    // dependencies on the Animator's internal thread. Currently we
    // use a couple of heuristics to determine whether we should do
    // the blocking wait().
    if ((Thread.currentThread() == thread) ||
        (SingleThreadedWorkaround.doWorkaround() && EventQueue.isDispatchThread())) {
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
