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

package jogamp.opengl.awt;

import java.awt.EventQueue;

import com.jogamp.opengl.GLException;

import com.jogamp.common.util.awt.AWTEDTExecutor;

import jogamp.opengl.GLWorkerThread;
import jogamp.opengl.ThreadingImpl;
import jogamp.opengl.ToolkitThreadingPlugin;

public class AWTThreadingPlugin implements ToolkitThreadingPlugin {

  public AWTThreadingPlugin() {}

  @Override
  public final boolean isToolkitThread() throws GLException {
      return EventQueue.isDispatchThread();
  }

  @Override
  public final boolean isOpenGLThread() throws GLException {
    switch (ThreadingImpl.getMode()) {
      case ST_AWT:
        // FIXME: See the FIXME below in 'invokeOnOpenGLThread'
        if (Java2D.isOGLPipelineActive() && !ThreadingImpl.isX11()) {
          return Java2D.isQueueFlusherThread();
        } else {
          return EventQueue.isDispatchThread();
        }
      case ST_WORKER:
        if (Java2D.isOGLPipelineActive()) {
          // FIXME: ideally only the QFT would be considered to be the
          // "OpenGL thread", but we can not currently run all of
          // JOGL's OpenGL work on that thread. See the FIXME in
          // invokeOnOpenGLThread.
          return (Java2D.isQueueFlusherThread() ||
                  (ThreadingImpl.isX11() && GLWorkerThread.isWorkerThread()));
        } else {
          return GLWorkerThread.isWorkerThread();
        }
      default:
        throw new InternalError("Illegal single-threading mode " + ThreadingImpl.getMode());
    }
  }

  @Override
  public final void invokeOnOpenGLThread(final boolean wait, final Runnable r) throws GLException {
    switch (ThreadingImpl.getMode()) {
      case ST_AWT:
        // FIXME: ideally should run all OpenGL work on the Java2D QFT
        // thread when it's enabled, but unfortunately there are
        // deadlock issues on X11 platforms when making our
        // heavyweight OpenGL contexts current on the QFT because we
        // perform the JAWT lock inside the makeCurrent()
        // implementation, which attempts to grab the AWT lock on the
        // QFT which is not allowed. For now, on X11 platforms,
        // continue to perform this work on the EDT.
        if (wait && Java2D.isOGLPipelineActive() && !ThreadingImpl.isX11()) {
          Java2D.invokeWithOGLContextCurrent(null, r);
        } else {
          AWTEDTExecutor.singleton.invoke(wait, r);
        }
        break;

      case ST_WORKER:
        ThreadingImpl.invokeOnWorkerThread(wait, r);
        break;

      case MT:
        r.run();
        break;

      default:
        throw new InternalError("Illegal single-threading mode " + ThreadingImpl.getMode());
    }
  }
}
