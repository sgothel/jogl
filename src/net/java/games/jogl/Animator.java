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

package net.java.games.jogl;

import java.awt.EventQueue;
import java.util.*;

/** <P> An Animator can be attached to one or more {@link
    GLAutoDrawable}s to drive their display() methods in a loop. </P>

    <P> The Animator class creates a background thread in which the
    calls to <code>display()</code> are performed. After each drawable
    has been redrawn, {@link #sync} is called to cause a brief pause.
    The default implementation of {@link #sync} calls
    <code>Thread.sleep(1)</code> to yield the CPU briefly. Subclasses
    may override this behavior to cause different animation behavior.
    </P>
*/

public class Animator {
  private volatile ArrayList/*<GLAutoDrawable>*/ drawables = new ArrayList();
  private Runnable runnable;
  private Thread thread;
  private volatile boolean shouldStop;
  protected boolean ignoreExceptions;
  protected boolean printExceptions;

  /** Creates a new, empty Animator. */
  public Animator() {
  }

  /** Creates a new Animator for a particular drawable. */
  public Animator(GLAutoDrawable drawable) {
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

  /** Called every frame after redrawing all drawables to cause a
      brief pause in animation. Subclasses may override this to cause
      different behavior in animation. The default implementation
      calls <code>Thread.sleep(1)</code>. */
  protected void sync() {
    try {
      Thread.sleep(1);
    } catch (InterruptedException e) {
    }
  }

  class MainLoop implements Runnable {
    public void run() {
      try {
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
          Iterator iter = drawableIterator();
          while (iter.hasNext()) {
            GLAutoDrawable drawable = (GLAutoDrawable) iter.next();
            try {
              drawable.display();
            } catch (RuntimeException e) {
              if (ignoreExceptions) {
                if (printExceptions) {
                  e.printStackTrace();
                }
              } else {
                throw(e);
              }
            }
          }
          sync();
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
    thread = new Thread(runnable);
    thread.start();
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
    if ((Thread.currentThread() == thread) || EventQueue.isDispatchThread()) {
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
