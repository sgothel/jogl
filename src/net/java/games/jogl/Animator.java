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

  public Animator(GLDrawable drawable) {
    this.drawable = drawable;

    // Workaround for NVidia driver bug 80174
    if (drawable instanceof GLCanvas) {
      ((GLCanvas) drawable).willSetRenderingThread();
    }
  }

  public synchronized void start() {
    if (thread != null) {
      throw new GLException("Already started");
    }
    if (runnable == null) {
      runnable = new Runnable() {
          public void run() {
            drawable.setRenderingThread(Thread.currentThread());
            drawable.setNoAutoRedrawMode(true);
            boolean noException = false;
            try {
              while (!shouldStop) {
                noException = false;
                drawable.display();
                noException = true;
              }
            } finally {
              shouldStop = false;
              drawable.setNoAutoRedrawMode(false);
              if (noException) {
                try {
                  drawable.setRenderingThread(null);
                } finally {
                  thread = null;
                  synchronized (Animator.this) {
                    Animator.this.notify();
                  }
                }
              }
            }
          }
        };
    }
    thread = new Thread(runnable);
    thread.start();
  }

  public synchronized void stop() {
    shouldStop = true;
    while (shouldStop && thread != null) {
      try {
        wait();
      } catch (InterruptedException e) {
      }
    }
  }
}
