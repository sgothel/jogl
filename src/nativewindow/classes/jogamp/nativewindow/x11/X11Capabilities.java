/**
 * Copyright 2012-2023 JogAmp Community. All rights reserved.
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

import com.jogamp.nativewindow.Capabilities;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.VisualIDHolder;

public class X11Capabilities extends Capabilities {
  final private XVisualInfo xVisualInfo; // maybe null if !onscreen

  public X11Capabilities(final XVisualInfo xVisualInfo) {
      super();
      this.xVisualInfo = xVisualInfo;
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
      throw new NativeWindowException(e);
    }
  }

  final public XVisualInfo getXVisualInfo() { return xVisualInfo; }
  final public int getXVisualID() { return (null!=xVisualInfo) ? (int) xVisualInfo.getVisualid() : VisualIDHolder.VID_UNDEFINED; }
  final public boolean hasXVisualInfo() { return null!=xVisualInfo; }

  @Override
  final public int getVisualID(final VIDType type) throws NativeWindowException {
      switch(type) {
          case INTRINSIC:
          case NATIVE:
              // fall through intended
          case X11_XVISUAL:
              return getXVisualID();
          case X11_FBCONFIG:
              return VisualIDHolder.VID_UNDEFINED;
          default:
              throw new NativeWindowException("Invalid type <"+type+">");
      }
  }

  @Override
  final public boolean isVisualIDSupported(final VIDType type) {
      switch(type) {
          case INTRINSIC:
          case NATIVE:
          case X11_XVISUAL:
          case X11_FBCONFIG:
              return true;
          default:
              return false;
      }
  }

  @Override
  public StringBuilder toString(StringBuilder sink) {
    if(null == sink) {
        sink = new StringBuilder();
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