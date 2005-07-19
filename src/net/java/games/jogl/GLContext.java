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

public abstract class GLContext {
  public static final int CONTEXT_NOT_CURRENT = 0;
  public static final int CONTEXT_CURRENT     = 1;
  public static final int CONTEXT_CURRENT_NEW = 2;

  private static ThreadLocal currentContext = new ThreadLocal();

  /**
   * Returns the GLDrawable to which this context may be used to
   * draw.
   */
  public abstract GLDrawable getGLDrawable();

  /**
   * Makes this GLContext current on the calling thread.
   *
   * There are two return values that indicate success and one that
   * indicates failure. A return value of CONTEXT_CURRENT_NEW
   * indicates that that context has been made current, and that
   * this is the first time this context has been made current, or
   * that the state of the underlying context or drawable may have
   * changed since the last time this context was made current. In
   * this case, the application may wish to initialize the state.  A
   * return value of CONTEXT_CURRENT indicates that the context has
   * been made currrent, with its previous state restored.
   * 
   * If the context could not be made current (for example, because
   * the underlying drawable has not ben realized on the display) ,
   * a value of CONTEXT_NOT_CURRENT is returned.
   *
   * If the context is in use by another thread at the time of the
   * call, then if isSynchronized() is true the call will
   * block. If isSynchronized() is false, an exception will be
   * thrown and the context will remain current on the other thread.
   *
   * @return CONTEXT_CURRENT if the context was successfully made current
   * @return CONTEXT_CURRENT_NEW if the context was successfully made
   * current, but need to be initialized.
   *
   * @return CONTEXT_NOT_CURRENT if the context could not be made current.
   *
   * @throws GLException if synchronization is disabled and the
   * context is current on another thread, or because the context
   * could not be created or made current due to non-recoverable,
   * window system-specific errors.
   */
  public abstract int makeCurrent() throws GLException;

  /**
   * Releases control of this GLContext from the current thread.
   *
   * @throw GLException if the context had not previously been made
   * current on the current thread
   */
  public abstract void release() throws GLException;

  /**
   * Returns the context which is current on the current thread. If no
   * context is current, returns null.
   *
   * @return the context current on this thread, or null if no context
   * is current.
   */
  public static GLContext getCurrent() {
    return (GLContext) currentContext.get();
  }

  /**
   * Sets the current context object on the current thread. This
   * method is called by GLContext implementations during {@link
   * #makeCurrent} and does not need to be called by end users.
   *
   */
  public static void setCurrent(GLContext cur) {
    currentContext.set(cur);
  }
  
  /**
   * Destroys this OpenGL context and frees its associated resources.
   * <P>
   * For onscreen GLDrawables, should be used to indicate to the
   * GLContext implementation that the underlying window has been
   * destroyed.
   */
  public abstract void destroy();

  /**
   * Returns true if 'makeCurrent' will exhibit synchronized behavior.
   */
  public abstract boolean isSynchronized();
    
  /** 
   * Determines whether 'makeCurrent' will exhibit synchronized behavior.
   */
  public abstract void setSynchronized(boolean isSynchronized);

  // 'GL' and 'GLU' pipelines allow instrumenting the actual
  // pipeline for debugging, tracing, etc.

  public abstract GL getGL();

  public abstract GLU getGLU();

  public abstract void setGL(GL gl);

  public abstract void setGLU(GLU glu);
}
