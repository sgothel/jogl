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

package com.jogamp.opengl.impl;

import javax.media.opengl.*;

/** Implements the makeCurrent / release locking behavior of the
    GLContext class. When "fail fast mode" is enabled, attempts to
    lock the same GLContextLock on more than one thread cause
    GLException to be raised. This lock is not recursive. Attempts to
    lock it more than once on a given thread will cause GLException to
    be raised. */

public class GLContextLock {
  protected static final boolean DEBUG = GLContextImpl.DEBUG;

  static class SyncData {
      boolean failFastMode = true;
      Thread owner = null;
      int waiters = 0;
      Exception lockedStack = null; // only enabled if DEBUG
  }
  private SyncData sdata = new SyncData(); // synchronized (flow/mem)  mutable access

  /** Locks this GLContextLock on the current thread. If fail fast
      mode is enabled and the GLContextLock is already owned by
      another thread, throws GLException. */
  public final void lock() throws GLException {
    synchronized(sdata) {
      Thread current = Thread.currentThread();
      if (sdata.owner == null) {
        sdata.owner = current;
        if(DEBUG) {
            sdata.lockedStack = new Exception("Error: Previously made current (1) by "+sdata.owner+", lock: "+this);
        }
      } else if (sdata.owner != current) {
        while (sdata.owner != null) {
          if (sdata.failFastMode) {
            if(null!=sdata.lockedStack) {
                sdata.lockedStack.printStackTrace();
            }
            throw new GLException("Error: Attempt to make context current on thread " + current +
                                  " which is already current on thread " + sdata.owner);
          } else {
            try {
              ++sdata.waiters;
              sdata.wait();
            } catch (InterruptedException e) {
              throw new GLException(e);
            } finally {
              --sdata.waiters;
            }
          }
        }
        sdata.owner = current;
        if(DEBUG) {
            sdata.lockedStack = new Exception("Previously made current (2) by "+sdata.owner+", lock: "+this);
        }
      } else {
        throw new GLException("Attempt to make the same context current twice on thread " + current);
      }
    }
  }

  /** Unlocks this GLContextLock. */
  public final void unlock() throws GLException {
    synchronized (sdata) {
      Thread current = Thread.currentThread();
      if (sdata.owner == current) {
        sdata.owner = null;
        sdata.lockedStack = null;
        // Assuming notify() implementation weaks up the longest waiting thread, to avoid starvation. 
        // Otherwise we would need to have a Thread queue implemented, using sleep(timeout) and interrupt.
        sdata.notify();
      } else {
        if (sdata.owner != null) {
          throw new GLException("Attempt by thread " + current +
                                " to release context owned by thread " + sdata.owner);
        } else {
          throw new GLException("Attempt by thread " + current +
                                " to release unowned context");
        }
      }
    }
  }

  /** Indicates whether this lock is held by the current thread. */
  public final boolean isHeld() {
    synchronized(sdata) {
      return (Thread.currentThread() == sdata.owner);
    }
  }

  public final void setFailFastMode(boolean onOrOff) {
    synchronized(sdata) {
        sdata.failFastMode = onOrOff;
    }
  }

  public final boolean getFailFastMode() {
    synchronized(sdata) {
        return sdata.failFastMode;
    }
  }

  public final boolean hasWaiters() {
    synchronized(sdata) {
        return (0 != sdata.waiters);
    }
  }

  /** holding the owners stack trace when lock is acquired and DEBUG is true */
  public final Exception getLockedStack() {
    synchronized(sdata) {
        return sdata.lockedStack;
    }
  }

}
