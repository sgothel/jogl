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

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;

import jogamp.nativewindow.ProxySurfaceImpl;
import jogamp.nativewindow.windows.GDI;


/**
 * GDI Surface implementation which wraps an existing window handle
 * allowing the use of HDC via lockSurface()/unlockSurface() protocol.
 * The latter will get and release the HDC.
 * The size via getWidth()/getHeight() is invalid.
 *
 * @see ProxySurface
 */
public class GDISurface extends ProxySurfaceImpl {
  private long windowHandle;
  private long surfaceHandle;

  /**
   * @param cfg the {@link AbstractGraphicsConfiguration} to be used
   * @param windowHandle the wrapped pre-existing native window handle, maybe 0 if not yet determined
   * @param upstream the {@link UpstreamSurfaceHook} to be used
   * @param ownsDevice <code>true</code> if this {@link ProxySurface} instance
   *                  owns the {@link AbstractGraphicsConfiguration}'s {@link AbstractGraphicsDevice},
   *                  otherwise <code>false</code>. Owning the device implies closing it at {@link #destroyNotify()}.
   */
  public GDISurface(final AbstractGraphicsConfiguration cfg, final long windowHandle, final UpstreamSurfaceHook upstream, final boolean ownsDevice) {
    super(cfg, upstream, ownsDevice);
    this.windowHandle=windowHandle;
    this.surfaceHandle=0;
  }

  @Override
  protected void invalidateImpl() {
    if(0 != surfaceHandle) {
        throw new NativeWindowException("didn't release surface Handle: "+this);
    }
    windowHandle = 0;
    // surfaceHandle = 0;
  }

  /**
   * {@inheritDoc}
   * <p>
   * Actually the window handle (HWND), since the surfaceHandle (HDC) is derived
   * from it at {@link #lockSurface()}.
   * </p>
   */
  @Override
  public final void setSurfaceHandle(final long surfaceHandle) {
      this.windowHandle = surfaceHandle;
  }

  /**
   * Sets the window handle (HWND).
   */
  public final void setWindowHandle(final long windowHandle) {
      this.windowHandle = windowHandle;
  }

  public final long getWindowHandle() {
      return windowHandle;
  }

  @Override
  final protected int lockSurfaceImpl() {
    if (0 == windowHandle) {
        throw new NativeWindowException("null window handle: "+this);
    }
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

  @Override
  final protected void unlockSurfaceImpl() {
    if (0 != surfaceHandle) {
        if(0 == GDI.ReleaseDC(windowHandle, surfaceHandle)) {
            throw new NativeWindowException("DC not released: "+this+", isWindow "+GDI.IsWindow(windowHandle)+", werr "+GDI.GetLastError()+", thread: "+Thread.currentThread().getName());
        }
        surfaceHandle=0;
    }
  }

  @Override
  final public long getSurfaceHandle() {
    return surfaceHandle;
  }

  @Override
  public final int[] convertToWindowUnits(final int[] pixelUnitsAndResult) {
      return pixelUnitsAndResult; // no pixelScale factor
  }

  @Override
  public final int[] convertToPixelUnits(final int[] windowUnitsAndResult) {
      return windowUnitsAndResult; // no pixelScale factor
  }

}
