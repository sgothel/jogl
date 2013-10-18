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

package jogamp.opengl;

import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;
import javax.media.opengl.GLPbuffer;

import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

@SuppressWarnings("deprecation")
public class GLPbufferImpl extends GLAutoDrawableBase implements GLPbuffer {

  public GLPbufferImpl(GLDrawableImpl pbufferDrawable, GLContextImpl pbufferContext) {
    super(pbufferDrawable, pbufferContext, true); // drawable := pbufferDrawable, context := pbufferContext
  }

  //
  // pbuffer specifics
  //

  //
  // GLDrawable delegation
  //

  @Override
  public final void swapBuffers() throws GLException {
      defaultSwapBuffers();
  }

  //
  // GLAutoDrawable completion
  //
  private final RecursiveLock lock = LockFactory.createRecursiveLock();  // instance wide lock

  @Override
  protected final RecursiveLock getLock() { return lock; }

  @Override
  public final Object getUpstreamWidget() {
    return null;
  }

  @Override
  public void destroy() {
    defaultDestroy();
  }

  @Override
  public GLDrawableFactory getFactory() {
    return drawable.getFactory();
  }

  @Override
  public final void display() {
    final RecursiveLock _lock = lock;
    _lock.lock(); // sync: context/drawable could been recreated/destroyed while animating
    try {
        if( null != context ) {
          helper.invokeGL(drawable, context, defaultDisplayAction, defaultInitAction);
        }
    } finally {
        _lock.unlock();
    }
  }
}
