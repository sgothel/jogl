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

package jogamp.nativewindow.windows;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.NativeWindowException;

import javax.media.nativewindow.ProxySurface;

/**
 * GDI Surface implementation which wraps an existing window handle
 * allowing the use of HDC via lockSurface()/unlockSurface() protocol.
 * The latter will get and release the HDC.
 * The size via getWidth()/getHeight() is invalid.
 */
public class GDISurface extends ProxySurface {
  protected long windowHandle;
  protected long surfaceHandle;

  public GDISurface(AbstractGraphicsConfiguration cfg, long windowHandle) {
    super(cfg);
    if(0 == windowHandle) {
        throw new NativeWindowException("Error hwnd 0, werr: "+GDI.GetLastError());
    }
    this.windowHandle=windowHandle;
  }

  protected final void invalidateImpl() {
    windowHandle=0;
    surfaceHandle=0;
  }

  protected int lockSurfaceImpl() {
    if (0 != surfaceHandle) {
        throw new InternalError("surface not released");
    }
    surfaceHandle = GDI.GetDC(windowHandle);
    /*
    if(0 == surfaceHandle) {
        System.err.println("****** DC Acquire: 0x"+Long.toHexString(windowHandle)+", isWindow "+GDI.IsWindow(windowHandle)+", isVisible "+GDI.IsWindowVisible(windowHandle)+", GDI LastError: "+GDI.GetLastError()+", 0x"+Long.toHexString(surfaceHandle)+", GDI LastError: "+GDI.GetLastError()+", thread: "+Thread.currentThread().getName());
        Thread.dumpStack();
    }
    */
    return (0 != surfaceHandle) ? LOCK_SUCCESS : LOCK_SURFACE_NOT_READY;
  }

  protected void unlockSurfaceImpl() {
    if (0 == surfaceHandle) {
        throw new InternalError("surface not acquired: "+this+", thread: "+Thread.currentThread().getName());
    }
    if(0 == GDI.ReleaseDC(windowHandle, surfaceHandle)) {
        throw new NativeWindowException("DC not released: "+this+", isWindow "+GDI.IsWindow(windowHandle)+", werr "+GDI.GetLastError()+", thread: "+Thread.currentThread().getName());        
    }
    surfaceHandle=0;
  }

  public long getSurfaceHandle() {
    return surfaceHandle;
  }

  public String toString() {
    return "GDISurface[config "+getPrivateGraphicsConfiguration()+
                ", displayHandle 0x"+Long.toHexString(getDisplayHandle())+
                ", windowHandle 0x"+Long.toHexString(windowHandle)+
                ", surfaceHandle 0x"+Long.toHexString(getSurfaceHandle())+
                ", size "+getWidth()+"x"+getHeight()+"]";
  }

}
