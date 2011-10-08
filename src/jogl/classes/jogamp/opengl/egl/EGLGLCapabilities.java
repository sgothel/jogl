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

import java.util.Comparator;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

public class EGLGLCapabilities extends GLCapabilities {
  final long eglcfg;
  final int  eglcfgid;
  final int renderableType;  
  int nativeVisualID;
  
  /** Comparing EGLConfig ID only */
  public static class EglCfgIDComparator implements Comparator<EGLGLCapabilities> {

      public int compare(EGLGLCapabilities caps1, EGLGLCapabilities caps2) {
        final long id1 = caps1.getEGLConfigID();

        final long id2 = caps2.getEGLConfigID();

        if(id1 > id2) {
            return 1;
        } else if(id1 < id2) {
            return -1;
        }
        return 0;
      }
  }

  /**
   * 
   * @param eglcfg
   * @param eglcfgid
   * @param glp desired GLProfile, or null if determined by renderableType
   * @param renderableType actual EGL renderableType
   * 
   * May throw GLException if given GLProfile is not compatible w/ renderableType
   */
  public EGLGLCapabilities(long eglcfg, int eglcfgid, GLProfile glp, int renderableType) {
      super( ( null != glp ) ? glp : getCompatible(renderableType) );
      this.eglcfg = eglcfg;
      this.eglcfgid = eglcfgid;
      if(!isCompatible(glp, renderableType)) {
          throw new GLException("Incompatible "+glp+
                                " with EGL-RenderableType["+renderableTypeToString(null, renderableType)+"]");
      }
      this.renderableType = renderableType;
  }

  public Object cloneMutable() {
    return clone();
  }

  public Object clone() {
    try {
      return super.clone();
    } catch (RuntimeException e) {
      throw new GLException(e);
    }
  }

  final public long getEGLConfig() { return eglcfg; }
  final public int getEGLConfigID() { return eglcfgid; }
  final public int getRenderableType() { return renderableType; }
  final public void setNativeVisualID(int vid) { nativeVisualID=vid; }
  final public int getNativeVisualID() { return nativeVisualID; }
  
  public static boolean isCompatible(GLProfile glp, int renderableType) {
    if(null == glp) {
        return true;
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_ES_BIT) && glp.usesNativeGLES1()) {
        return true;
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_ES2_BIT) && glp.usesNativeGLES2()) {
        return true;
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_BIT) && !glp.usesNativeGLES()) {
        return true;
    }
    return false;
  }

  public static GLProfile getCompatible(int renderableType) {
    if(0 != (renderableType & EGL.EGL_OPENGL_ES2_BIT) && GLProfile.isAvailable(GLProfile.GLES2)) {
        return GLProfile.get(GLProfile.GLES2);
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_ES_BIT) && GLProfile.isAvailable(GLProfile.GLES1)) {
        return GLProfile.get(GLProfile.GLES1);
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_BIT)) {
        return GLProfile.getDefault();
    }
    return null;
  }
  
  public static StringBuffer renderableTypeToString(StringBuffer sink, int renderableType) {
    if(null == sink) {
        sink = new StringBuffer();
    }
    boolean first=true;
    if(0 != (renderableType & EGL.EGL_OPENGL_BIT)) {
        sink.append("GL"); first=false;
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_ES_BIT)) {
        if(!first) sink.append(", "); sink.append("GLES1");  first=false;
    }
    if(0 != (renderableType & EGL.EGL_OPENGL_ES2_BIT)) {
        if(!first) sink.append(", "); sink.append("GLES2");  first=false;
    }
    if(0 != (renderableType & EGL.EGL_OPENVG_API)) {
        if(!first) sink.append(", "); sink.append("VG");  first=false;
    }
    return sink;      
  }
  
  public StringBuffer toString(StringBuffer sink) {
    if(null == sink) {
        sink = new StringBuffer();
    }
    sink.append("0x").append(Long.toHexString(eglcfgid)).append(": ");
    sink.append("vid 0x").append(Integer.toHexString(nativeVisualID)).append(", ");
    super.toString(sink);
    sink.append(", [");
    renderableTypeToString(sink, renderableType);
    return sink.append("]");
  }
}