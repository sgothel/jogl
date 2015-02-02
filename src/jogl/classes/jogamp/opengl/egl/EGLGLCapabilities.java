/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package jogamp.opengl.egl;

import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import com.jogamp.nativewindow.egl.EGLGraphicsDevice;
import com.jogamp.opengl.egl.EGL;
import com.jogamp.opengl.egl.EGLExt;


public class EGLGLCapabilities extends GLCapabilities {
  private long eglcfg;
  final private int  eglcfgid;
  final private int  renderableType;
  final private int  nativeVisualID;

  /**
   *
   * @param eglcfg
   * @param eglcfgid
   * @param visualID native visualID if valid, otherwise VisualIDHolder.VID_UNDEFINED
   * @param glp desired GLProfile
   * @param renderableType actual EGL renderableType
   *
   * May throw GLException if given GLProfile is not compatible w/ renderableType
   */
  public EGLGLCapabilities(final long eglcfg, final int eglcfgid, final int visualID, final GLProfile glp, final int renderableType) {
      super( glp );
      this.eglcfg = eglcfg;
      this.eglcfgid = eglcfgid;
      if(!isCompatible(glp, renderableType)) {
          throw new GLException("Requested GLProfile "+glp+
                                " not compatible with EGL-RenderableType["+renderableTypeToString(null, renderableType)+"]");
      }
      this.renderableType = renderableType;
      this.nativeVisualID = visualID;
  }

  @Override
  public Object cloneMutable() {
    return clone();
  }

  @Override
  public Object clone() {
    try {
      return super.clone();
    } catch (final RuntimeException e) {
      throw new GLException(e);
    }
  }

  final protected void setEGLConfig(final long v) { eglcfg=v; }
  final public long getEGLConfig() { return eglcfg; }
  final public int getEGLConfigID() { return eglcfgid; }
  final public int getRenderableType() { return renderableType; }
  final public int getNativeVisualID() { return nativeVisualID; }

  @Override
  final public int getVisualID(final VIDType type) throws NativeWindowException {
      switch(type) {
          case INTRINSIC:
          case EGL_CONFIG:
              return getEGLConfigID();
          case NATIVE:
              return getNativeVisualID();
          default:
              throw new NativeWindowException("Invalid type <"+type+">");
      }
  }

  public static boolean isCompatible(final GLProfile glp, final int renderableType) {
    if(null == glp) {
        return true;
    }
    if(0 != (renderableType & EGLExt.EGL_OPENGL_ES3_BIT_KHR) && glp.usesNativeGLES3()) {
        return true;
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_ES2_BIT) && glp.usesNativeGLES2()) {
        return true;
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_ES_BIT) && glp.usesNativeGLES1()) {
        return true;
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_BIT) && !glp.usesNativeGLES()) {
        return true;
    }
    return false;
  }

  public static GLProfile getCompatible(final EGLGraphicsDevice device, final int renderableType) {
    if(0 != (renderableType & EGLExt.EGL_OPENGL_ES3_BIT_KHR) && GLProfile.isAvailable(device, GLProfile.GLES3)) {
        return GLProfile.get(device, GLProfile.GLES3);
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_ES2_BIT) && GLProfile.isAvailable(device, GLProfile.GLES2)) {
        return GLProfile.get(device, GLProfile.GLES2);
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_ES_BIT) && GLProfile.isAvailable(device, GLProfile.GLES1)) {
        return GLProfile.get(device, GLProfile.GLES1);
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_BIT)) {
        return GLProfile.getDefault(device);
    }
    return null;
  }

  public static StringBuilder renderableTypeToString(StringBuilder sink, final int renderableType) {
    if(null == sink) {
        sink = new StringBuilder();
    }
    boolean first=true;
    sink.append("0x").append(Integer.toHexString(renderableType)).append(": ");
    if(0 != (renderableType & EGL.EGL_OPENGL_BIT)) {
        sink.append("GL"); first=false;
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_ES_BIT)) {
        if(!first) sink.append(", "); sink.append("GLES1");  first=false;
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_ES2_BIT)) {
        if(!first) sink.append(", "); sink.append("GLES2");  first=false;
    }
    if(0 != (renderableType & EGLExt.EGL_OPENGL_ES3_BIT_KHR)) {
        if(!first) sink.append(", "); sink.append("GLES3");  first=false;
    }
    if(0 != (renderableType & EGL.EGL_OPENVG_API)) {
        if(!first) sink.append(", "); sink.append("VG");  first=false;
    }
    return sink;
  }

  @Override
  public StringBuilder toString(StringBuilder sink) {
    if(null == sink) {
        sink = new StringBuilder();
    }
    sink.append("egl cfg 0x").append(Integer.toHexString(eglcfgid));
    sink.append(", vid 0x").append(Integer.toHexString(nativeVisualID)).append(": ");
    super.toString(sink);
    sink.append(", [");
    renderableTypeToString(sink, renderableType);
    return sink.append("]");
  }
}