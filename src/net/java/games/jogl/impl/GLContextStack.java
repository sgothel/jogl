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

package net.java.games.jogl.impl;

import java.util.*;

/** Implements a stack of GLContext objects along with the initActions
    that need to be run if their creation is necessary. This is used
    to detect redundant makeCurrent() calls and to allow one drawable
    to call display() of another from within the first drawable's
    display() method. */

public class GLContextStack {
  private ArrayList data = new ArrayList();

  /** Pushes this GLContext on the stack. The passed context must be non-null. */
  public void push(GLContext ctx, Runnable initAction) {
    if (ctx == null) {
      throw new IllegalArgumentException("Null contexts are not allowed here");
    }

    data.add(new GLContextInitActionPair(ctx, initAction));
  }

  /** Removes and returns the top GLContext and associated
      initialization action, or null if there is none. */
  public GLContextInitActionPair pop() {
    if (data.size() == 0) {
      return null;
    }
    
    return (GLContextInitActionPair) data.remove(data.size() - 1);
  }

  /** Returns the top GLContext and associated initialization action
      without removing it, or null if there is none. */
  public GLContextInitActionPair peek() {
    return peek(0);
  }

  /** Returns the <i>i</i>th GLContext and associated initialization
      action from the top without removing it, or null if there is
      none. */
  public GLContextInitActionPair peek(int i) {
    if (data.size() - i <= 0) {
      return null;
    }

    return (GLContextInitActionPair) data.get(data.size() - i - 1);
  }

  /** Returns the top GLContext without removing it, or null if there
      is none. */
  public GLContext peekContext() {
    return peekContext(0);
  }

  /** Returns the <i>i</i>th GLContext from the top without removing
      it, or null if there is none. */
  public GLContext peekContext(int i) {
    GLContextInitActionPair pair = peek(i);
    if (pair == null) {
      return null;
    }

    return pair.getContext();
  }

  /** Returns the top initialization action without removing it, or
      null if there is none. */
  public Runnable peekInitAction() {
    return peekInitAction(0);
  }

  /** Returns the <i>i</i>th initialization action from the top
      without removing it, or null if there is none. */
  public Runnable peekInitAction(int i) {
    GLContextInitActionPair pair = peek(i);
    if (pair == null) {
      return null;
    }

    return pair.getInitAction();
  }

  /** Returns the number of entries on the GLContext stack. */
  public int size() {
    return data.size();
  }
}
