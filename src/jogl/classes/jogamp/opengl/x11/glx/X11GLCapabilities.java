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

package jogamp.opengl.x11.glx;

import jogamp.nativewindow.x11.XVisualInfo;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import java.util.Comparator;

public class X11GLCapabilities extends GLCapabilities {
  final XVisualInfo xVisualInfo; // maybe null if !onscreen
  final long fbcfg;
  final int  fbcfgid;

  /** Comparing xvisual id only */
  public static class XVisualIDComparator implements Comparator {

      public int compare(Object o1, Object o2) {
        if ( ! ( o1 instanceof X11GLCapabilities ) ) {
            Class<?> c = (null != o1) ? o1.getClass() : null ;
            throw new ClassCastException("arg1 not a X11GLCapabilities object: " + c);
        }
        if ( ! ( o2 instanceof X11GLCapabilities ) ) {
            Class<?> c = (null != o2) ? o2.getClass() : null ;
            throw new ClassCastException("arg2 not a X11GLCapabilities object: " + c);
        }

        final X11GLCapabilities caps1 = (X11GLCapabilities) o1;
        final long id1 = caps1.getXVisualID();

        final X11GLCapabilities caps2 = (X11GLCapabilities) o2;
        final long id2 = caps2.getXVisualID();

        if(id1 > id2) {
            return 1;
        } else if(id1 < id2) {
            return -1;
        }
        return 0;
      }
  }

  public X11GLCapabilities(XVisualInfo xVisualInfo, long fbcfg, int fbcfgid, GLProfile glp) {
      super(glp);
      this.xVisualInfo = xVisualInfo;
      this.fbcfg = fbcfg;
      this.fbcfgid = fbcfgid;
  }

  public X11GLCapabilities(XVisualInfo xVisualInfo, GLProfile glp) {
      super(glp);
      this.xVisualInfo = xVisualInfo;
      this.fbcfg = 0;
      this.fbcfgid = -1;
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

  final public XVisualInfo getXVisualInfo() { return xVisualInfo; }
  final public long getXVisualID() { return (null!=xVisualInfo) ? xVisualInfo.getVisualid() : 0; }
  final public boolean hasXVisualInfo() { return null!=xVisualInfo; }

  final public long getFBConfig() { return fbcfg; }
  final public int getFBConfigID() { return fbcfgid; }
  final public boolean hasFBConfig() { return 0!=fbcfg && fbcfgid>0; }

  final static String na_str = "----" ;

  public StringBuffer toString(StringBuffer sink) {
    if(null == sink) {
        sink = new StringBuffer();
    }
    if(hasXVisualInfo()) {
        sink.append("0x").append(Long.toHexString(xVisualInfo.getVisualid()));
    } else {
        sink.append(na_str);
    }
    sink.append(" ");
    if(hasFBConfig()) {
        sink.append("0x").append(Integer.toHexString(fbcfgid));
    } else {
        sink.append(na_str);
    }
    sink.append(": ");
    return super.toString(sink);
  }
}