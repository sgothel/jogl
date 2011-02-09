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
  long eglcfg;
  int  eglcfgid;

  /** Comparing EGLConfig ID only */
  public static class EglCfgIDComparator implements Comparator {

      public int compare(Object o1, Object o2) {
        if ( ! ( o1 instanceof EGLGLCapabilities ) ) {
            Class c = (null != o1) ? o1.getClass() : null ;
            throw new ClassCastException("arg1 not a EGLGLCapabilities object: " + c);
        }
        if ( ! ( o2 instanceof EGLGLCapabilities ) ) {
            Class c = (null != o2) ? o2.getClass() : null ;
            throw new ClassCastException("arg2 not a EGLGLCapabilities object: " + c);
        }

        final EGLGLCapabilities caps1 = (EGLGLCapabilities) o1;
        final long id1 = caps1.getEGLConfigID();

        final EGLGLCapabilities caps2 = (EGLGLCapabilities) o2;
        final long id2 = caps2.getEGLConfigID();

        if(id1 > id2) {
            return 1;
        } else if(id1 < id2) {
            return -1;
        }
        return 0;
      }
  }

  public EGLGLCapabilities(long eglcfg, int eglcfgid, GLProfile glp) {
      super(glp);
      this.eglcfg = eglcfg;
      this.eglcfgid = eglcfgid;
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
  
  public StringBuffer toString(StringBuffer sink) {
    if(null == sink) {
        sink = new StringBuffer();
    }
    sink.append("0x").append(Long.toHexString(eglcfgid)).append(": ");
    return super.toString(sink);
  }
}