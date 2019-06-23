/**
 * Copyright 2019 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package jogamp.opengl.ios.eagl;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLException;

import jogamp.opengl.GLDrawableImpl;

public abstract class IOSEAGLDrawable extends GLDrawableImpl {
  public enum GLBackendType {
    /** Default OpenGL Backend */
    CAEAGL_LAYER(0);

    public final int id;

    GLBackendType(final int id){
        this.id = id;
    }
  }

  private boolean haveSetOpenGLMode = false;
  private GLBackendType openGLMode = GLBackendType.CAEAGL_LAYER;

  public IOSEAGLDrawable(final GLDrawableFactory factory, final NativeSurface comp, final boolean realized) {
    super(factory, comp, realized);
    initOpenGLImpl(getOpenGLMode());
  }

  @Override
  protected void setRealizedImpl() {
  }

  @Override
  protected void associateContext(final GLContext ctx, final boolean bound) {
  }

  @Override
  protected final void swapBuffersImpl(final boolean doubleBuffered) {
  }

  // Support for "mode switching" as described in MacOSXCGLDrawable
  public void setOpenGLMode(final GLBackendType mode) {
      if (mode == openGLMode) {
        return;
      }
      if (haveSetOpenGLMode) {
        throw new GLException("Can't switch between using NSOpenGLPixelBuffer and CGLPBufferObj more than once");
      }
      setRealized(false);
      if (DEBUG) {
        System.err.println("MacOSXCGLDrawable: Switching context mode " + openGLMode + " -> " + mode);
      }
      initOpenGLImpl(mode);
      openGLMode = mode;
      haveSetOpenGLMode = true;
  }
  public final GLBackendType getOpenGLMode() { return openGLMode; }

  protected void initOpenGLImpl(final GLBackendType backend) { /* nop */ }

}
