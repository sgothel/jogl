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

package jogamp.nativewindow;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;

import com.jogamp.nativewindow.UpstreamSurfaceHookMutableSize;

/**
 * Generic Surface implementation which wraps an existing window handle.
 *
 * @see ProxySurface
 */
public class WrappedSurface extends ProxySurfaceImpl {
  protected long surfaceHandle;

  /**
   * Utilizes a {@link UpstreamSurfaceHook.MutableSize} to hold the size information,
   * which is being passed to the {@link ProxySurface} instance.
   *
   * @param cfg the {@link AbstractGraphicsConfiguration} to be used
   * @param handle the wrapped pre-existing native surface handle, maybe 0 if not yet determined
   * @param initialWidth
   * @param initialHeight
   * @param ownsDevice <code>true</code> if this {@link ProxySurface} instance
   *                  owns the {@link AbstractGraphicsConfiguration}'s {@link AbstractGraphicsDevice},
   *                  otherwise <code>false</code>. Owning the device implies closing it at {@link #destroyNotify()}.
   */
  public WrappedSurface(AbstractGraphicsConfiguration cfg, long handle, int initialWidth, int initialHeight, boolean ownsDevice) {
      super(cfg, new UpstreamSurfaceHookMutableSize(initialWidth, initialHeight), ownsDevice);
      surfaceHandle=handle;
  }

  /**
   * @param cfg the {@link AbstractGraphicsConfiguration} to be used
   * @param handle the wrapped pre-existing native surface handle, maybe 0 if not yet determined
   * @param upstream the {@link UpstreamSurfaceHook} to be used
   * @param ownsDevice <code>true</code> if this {@link ProxySurface} instance
   *                  owns the {@link AbstractGraphicsConfiguration}'s {@link AbstractGraphicsDevice},
   *                  otherwise <code>false</code>.
   */
  public WrappedSurface(AbstractGraphicsConfiguration cfg, long handle, UpstreamSurfaceHook upstream, boolean ownsDevice) {
      super(cfg, upstream, ownsDevice);
      surfaceHandle=handle;
  }

  @Override
  protected void invalidateImpl() {
    surfaceHandle = 0;
  }

  @Override
  public final long getSurfaceHandle() {
    return surfaceHandle;
  }

  @Override
  public final void setSurfaceHandle(long surfaceHandle) {
    this.surfaceHandle=surfaceHandle;
  }

  @Override
  protected final int lockSurfaceImpl() {
    return LOCK_SUCCESS;
  }

  @Override
  protected final void unlockSurfaceImpl() {
  }

  @Override
  public final int[] getWindowUnitXY(int[] result, final int[] pixelUnitXY) {
      final int scale = 1; // FIXME: Use 'scale' ..
      result[0] = pixelUnitXY[0] / scale;
      result[1] = pixelUnitXY[1] / scale;
      return result;
  }

  @Override
  public final int[] getPixelUnitXY(int[] result, final int[] windowUnitXY) {
      final int scale = 1; // FIXME: Use 'scale' ..
      result[0] = windowUnitXY[0] * scale;
      result[1] = windowUnitXY[1] * scale;
      return result;
  }


}
