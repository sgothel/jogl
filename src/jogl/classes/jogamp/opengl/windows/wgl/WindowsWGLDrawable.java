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

package jogamp.opengl.windows.wgl;

import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLException;

import jogamp.nativewindow.windows.GDI;
import jogamp.opengl.Debug;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;


public abstract class WindowsWGLDrawable extends GLDrawableImpl {
  private static final boolean PROFILING;

  static {
      Debug.initSingleton();
      PROFILING = Debug.isPropertyDefined("jogl.debug.GLDrawable.profiling", true);
  }

  private static final int PROFILING_TICKS = 200;
  private int  profilingSwapBuffersTicks;
  private long profilingSwapBuffersTime;

  public WindowsWGLDrawable(GLDrawableFactory factory, NativeSurface comp, boolean realized) {
    super(factory, comp, realized);
  }

  @Override
  protected void setRealizedImpl() {
    if(realized) {
        NativeSurface ns = getNativeSurface();
        WindowsWGLGraphicsConfiguration config = (WindowsWGLGraphicsConfiguration)ns.getGraphicsConfiguration();
        config.updateGraphicsConfiguration(getFactory(), ns, null);
        if (DEBUG) {
          System.err.println(getThreadName()+": WindowsWGLDrawable.setRealized(true): "+config);
        }
    }
  }

  @Override
  protected final void swapBuffersImpl(boolean doubleBuffered) {
    if(doubleBuffered) {
        final long t0;
        if (PROFILING) {
          t0 = System.currentTimeMillis();
        } else {
          t0 = 0;
        }

        if (!WGLUtil.SwapBuffers(getHandle()) && (GDI.GetLastError() != GDI.ERROR_SUCCESS)) {
          throw new GLException("Error swapping buffers");
        }

        if (PROFILING) {
          profilingSwapBuffersTime += System.currentTimeMillis() - t0;
          if (++profilingSwapBuffersTicks == PROFILING_TICKS) {
            System.err.println("SwapBuffers calls: " + profilingSwapBuffersTime + " ms / " + PROFILING_TICKS + "  calls (" +
                               ((float) profilingSwapBuffersTime / (float) PROFILING_TICKS) + " ms/call)");
            profilingSwapBuffersTime = 0;
            profilingSwapBuffersTicks = 0;
          }
        }
    }
  }

  @Override
  public GLDynamicLookupHelper getGLDynamicLookupHelper() {
    return getFactoryImpl().getGLDynamicLookupHelper(0);
  }
}
