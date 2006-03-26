/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.opengl.impl;

import java.lang.reflect.InvocationTargetException;
import java.security.*;
import java.util.*;
import javax.media.opengl.*;

/** Singleton thread upon which all OpenGL work is performed by
    default. Unfortunately many vendors' OpenGL drivers are not really
    thread-safe and stability is much improved by performing OpenGL
    work on at most one thread. This is the default behavior of the
    GLAutoDrawable implementations according to the {@link
    javax.media.opengl.Threading Threading} class. The GLWorkerThread
    replaces the original AWT event queue thread-based mechanism for
    two reasons: first, more than one AWT event queue thread may be
    spawned, for example if a dialog is being shown; second, it avoids
    blocking the AWT event queue thread during OpenGL rendering. */

public class GLWorkerThread {
  private static volatile boolean started;
  private static volatile Thread thread;
  private static Object lock;
  private static volatile boolean shouldTerminate;
  private static volatile Throwable exception;

  // The Runnable to execute immediately on the worker thread
  private static volatile Runnable work;
  // Queue of Runnables to be asynchronously invoked
  private static List queue = new LinkedList();
  
  /** Should only be called by Threading class if creation of the
      GLWorkerThread was requested via the opengl.1thread system
      property. */
  public static void start() {
    if (!started) {
      synchronized (GLWorkerThread.class) {
        if (!started) {
          lock = new Object();
          thread = new Thread(new WorkerRunnable(),
                              "JOGL GLWorkerThread");
          thread.setDaemon(true);
          started = true;
          synchronized (lock) {
            thread.start();
            try {
              lock.wait();
            } catch (InterruptedException e) {
            }
          }

          /*

          // Note: it appears that there is a bug in NVidia's current
          // drivers where if a context was ever made current on a
          // given thread and that thread has exited before program
          // exit, a crash occurs in the drivers. Releasing the
          // context from the given thread does not work around the
          // problem.
          //
          // For the time being, we're going to work around this
          // problem by not terminating the GLWorkerThread. In theory,
          // shutting down the GLWorkerThread cleanly could be a good
          // general solution to the problem of needing to
          // cooperatively terminate all Animators at program exit.
          //
          // It appears that this doesn't even work around all of the
          // kinds of crashes. Causing the context to be unilaterally
          // released from the GLWorkerThread after each invocation
          // seems to work around all of the kinds of crashes seen.
          //
          // These appear to be similar to the kinds of crashes seen
          // when the Java2D/OpenGL pipeline terminates, and those are
          // a known issue being fixed, so presumably these will be
          // fixed in NVidia's next driver set.

          // Install shutdown hook to terminate daemon thread more or
          // less cooperatively
          AccessController.doPrivileged(new PrivilegedAction() {
              public Object run() {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                      Object lockTemp = lock;
                      if (lockTemp == null) {
                        // Already terminating (?)
                        return;
                      }
                      synchronized (lockTemp) {
                        shouldTerminate = true;
                        lockTemp.notifyAll();
                        try {
                          lockTemp.wait(500);
                        } catch (InterruptedException e) {
                        }
                      }
                    }
                  });
                return null;
              }
            });

          */

        } else {
          throw new RuntimeException("Should not start GLWorkerThread twice");
        }
      }
    }
  }

  public static void invokeAndWait(Runnable runnable)
    throws InvocationTargetException, InterruptedException {
    if (!started) {
      throw new RuntimeException("May not invokeAndWait on worker thread without starting it first");
    }

    Object lockTemp = lock;
    if (lockTemp == null) {
      return; // Terminating
    }

    synchronized (lockTemp) {
      if (thread == null) {
        // Terminating
        return;
      }

      work = runnable;
      lockTemp.notifyAll();
      lockTemp.wait();
      if (exception != null) {
        Throwable localException = exception;
        exception = null;
        throw new InvocationTargetException(localException);
      }
    }
  }

  public static void invokeLater(Runnable runnable) {
    if (!started) {
      throw new RuntimeException("May not invokeLater on worker thread without starting it first");
    }

    Object lockTemp = lock;
    if (lockTemp == null) {
      return; // Terminating
    }

    synchronized (lockTemp) {
      if (thread == null) {
        // Terminating
        return;
      }

      queue.add(runnable);
      lockTemp.notifyAll();
    }
  }

  /** Indicates whether the OpenGL worker thread was started, i.e.,
      whether it is currently in use. */
  public static boolean isStarted() {
    return started;
  }

  /** Indicates whether the current thread is the OpenGL worker
      thread. */
  public static boolean isWorkerThread() {
    return (Thread.currentThread() == thread);
  }

  static class WorkerRunnable implements Runnable {
    public void run() {
      // Notify starting thread that we're ready
      synchronized (lock) {
        lock.notifyAll();
      }

      while (!shouldTerminate) {
        synchronized (lock) {
          while (!shouldTerminate &&
                 (work == null) &&
                 queue.isEmpty()) {
            try {
              // Avoid race conditions with wanting to release contexts on this thread
              lock.wait(1000);
            } catch (InterruptedException e) {
            }

            if (GLContext.getCurrent() != null) {
              // Test later to see whether we need to release this context
              break;
            }
          }
          
          if (shouldTerminate) {
            lock.notifyAll();
            thread = null;
            lock = null;
            return;
          }

          if (work != null) {
            try {
              work.run();
            } catch (Throwable t) {
              exception = t;
            } finally {
              work = null;
              lock.notifyAll();
            }
          }

          while (!queue.isEmpty()) {
            try {
              Runnable curAsync = (Runnable) queue.remove(0);
              curAsync.run();
            } catch (Throwable t) {
              System.out.println("Exception occurred on JOGL OpenGL worker thread:");
              t.printStackTrace();
            }
          }

          // See about releasing current context
          GLContext curContext = GLContext.getCurrent();
          if (curContext != null &&
              (curContext instanceof GLContextImpl)) {
            GLContextImpl impl = (GLContextImpl) curContext;
            if (impl.hasWaiters()) {
              impl.release();
            }
          }
        }
      }
    }
  }
}
