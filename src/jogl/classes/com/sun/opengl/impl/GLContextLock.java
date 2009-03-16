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

package com.sun.opengl.impl;

import javax.media.opengl.*;

/** Implements the makeCurrent / release locking behavior of the
    GLContext class. When "fail fast mode" is enabled, attempts to
    lock the same GLContextLock on more than one thread cause
    GLException to be raised. This lock is not recursive. Attempts to
    lock it more than once on a given thread will cause GLException to
    be raised. */

public class GLContextLock {
  private Object lock = new Object();
  private Thread owner;
  private boolean failFastMode = true;
  private volatile int waiters;

  /** Locks this GLContextLock on the current thread. If fail fast
      mode is enabled and the GLContextLock is already owned by
      another thread, throws GLException. */
  public void lock() throws GLException {
    synchronized(lock) {
      Thread current = Thread.currentThread();
      if (owner == null) {
        owner = current;
      } else if (owner != current) {
        while (owner != null) {
          if (failFastMode) {
            throw new GLException("Attempt to make context current on thread " + current +
                                  " which is already current on thread " + owner);
          } else {
            try {
              ++waiters;
              lock.wait();
            } catch (InterruptedException e) {
              throw new GLException(e);
            } finally {
              --waiters;
            }
          }
        }
        owner = current;
      } else {
        throw new GLException("Attempt to make the same context current twice on thread " + current);
      }
    }
  }

  /** Unlocks this GLContextLock. */
  public void unlock() throws GLException {
    synchronized (lock) {
      Thread current = Thread.currentThread();
      if (owner == current) {
        owner = null;
        lock.notifyAll();
      } else {
        if (owner != null) {
          throw new GLException("Attempt by thread " + current +
                                " to release context owned by thread " + owner);
        } else {
          throw new GLException("Attempt by thread " + current +
                                " to release unowned context");
        }
      }
    }
  }

  /** Indicates whether this lock is held by the current thread. */
  public boolean isHeld() {
    synchronized(lock) {
      Thread current = Thread.currentThread();
      return (owner == current);
    }
  }

  public void setFailFastMode(boolean onOrOff) {
    failFastMode = onOrOff;
  }

  public boolean getFailFastMode() {
    return failFastMode;
  }

  public boolean hasWaiters() {
    return (waiters != 0);
  }
}
