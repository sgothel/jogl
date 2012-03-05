/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

package jogamp.nativewindow.x11;

import jogamp.nativewindow.NativeVisualID;
import java.util.Comparator;

import javax.media.nativewindow.Capabilities;
import javax.media.nativewindow.NativeWindowException;

public class X11Capabilities extends Capabilities implements NativeVisualID {
  final XVisualInfo xVisualInfo; // maybe null if !onscreen

  /** Comparing xvisual id only */
  public static class XVisualIDComparator implements Comparator {
      public int compare(Object o1, Object o2) {
        if ( ! ( o1 instanceof NativeVisualID ) ) {
            Class<?> c = (null != o1) ? o1.getClass() : null ;
            throw new ClassCastException("arg1 not a NativeVisualID object: " + c);
        }
        if ( ! ( o2 instanceof NativeVisualID ) ) {
            Class<?> c = (null != o2) ? o2.getClass() : null ;
            throw new ClassCastException("arg2 not a NativeVisualID object: " + c);
        }

        final NativeVisualID nvid1 = (NativeVisualID) o1;
        final int id1 = nvid1.getVisualID(NativeVisualID.NVIDType.X11_XVisualID);

        final NativeVisualID nvid2 = (NativeVisualID) o2;
        final int id2 = nvid2.getVisualID(NativeVisualID.NVIDType.X11_XVisualID);

        if(id1 > id2) {
            return 1;
        } else if(id1 < id2) {
            return -1;
        }
        return 0;
      }
  }

  public X11Capabilities(XVisualInfo xVisualInfo) {
      super();
      this.xVisualInfo = xVisualInfo;
  }

  public Object cloneMutable() {
    return clone();
  }

  public Object clone() {
    try {
      return super.clone();
    } catch (RuntimeException e) {
      throw new NativeWindowException(e);
    }
  }

  final public XVisualInfo getXVisualInfo() { return xVisualInfo; }
  final public int getXVisualID() { return (null!=xVisualInfo) ? (int) xVisualInfo.getVisualid() : 0; }
  final public boolean hasXVisualInfo() { return null!=xVisualInfo; }

  final public int getVisualID(NVIDType type) {
      switch(type) {
          case GEN_ID:
          case NATIVE_ID:
              // fall through intended
          case X11_XVisualID:
              return getXVisualID();
          case X11_FBConfigID:
              // fall through intended
          default:
              throw new IllegalArgumentException("Invalid type <"+type+">");
      }      
  }
  
  public StringBuffer toString(StringBuffer sink) {
    if(null == sink) {
        sink = new StringBuffer();
    }
    sink.append("x11 vid ");
    if(hasXVisualInfo()) {
        sink.append("0x").append(Long.toHexString(xVisualInfo.getVisualid()));
    } else {
        sink.append(na_str);
    }
    sink.append(": ");
    return super.toString(sink);
  }
}