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

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;

import com.jogamp.nativewindow.UpstreamSurfaceHookMutableSize;

/**
 * Generic Surface implementation which wraps an existing window handle.
 *
 * @see ProxySurface
 */
public class WrappedSurface extends ProxySurfaceImpl implements ScalableSurface {
  private final float[] hasPixelScale = new float[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE };
  private long surfaceHandle;

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
  public WrappedSurface(final AbstractGraphicsConfiguration cfg, final long handle, final int initialWidth, final int initialHeight, final boolean ownsDevice) {
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
  public WrappedSurface(final AbstractGraphicsConfiguration cfg, final long handle, final UpstreamSurfaceHook upstream, final boolean ownsDevice) {
      super(cfg, upstream, ownsDevice);
      surfaceHandle=handle;
  }

  @Override
  protected void invalidateImpl() {
    surfaceHandle = 0;
    hasPixelScale[0] = ScalableSurface.IDENTITY_PIXELSCALE;
    hasPixelScale[1] = ScalableSurface.IDENTITY_PIXELSCALE;
  }

  @Override
  public final long getSurfaceHandle() {
    return surfaceHandle;
  }

  @Override
  public final void setSurfaceHandle(final long surfaceHandle) {
    this.surfaceHandle=surfaceHandle;
  }

  @Override
  protected final int lockSurfaceImpl() {
    return LOCK_SUCCESS;
  }

  @Override
  protected final void unlockSurfaceImpl() {
  }

  /**
   * {@inheritDoc}
   * <p>
   * {@link WrappedSurface}'s implementation uses the {@link #setSurfaceScale(float[]) given pixelScale} directly.
   * </p>
   */
  @Override
  public final int[] convertToWindowUnits(final int[] pixelUnitsAndResult) {
      return SurfaceScaleUtils.scaleInv(pixelUnitsAndResult, pixelUnitsAndResult, hasPixelScale);
  }

  /**
   * {@inheritDoc}
   * <p>
   * {@link WrappedSurface}'s implementation uses the {@link #setSurfaceScale(float[]) given pixelScale} directly.
   * </p>
   */
  @Override
  public final int[] convertToPixelUnits(final int[] windowUnitsAndResult) {
      return SurfaceScaleUtils.scale(windowUnitsAndResult, windowUnitsAndResult, hasPixelScale);
  }

  /**
   * {@inheritDoc}
   * <p>
   * {@link WrappedSurface}'s implementation is to simply pass the given pixelScale
   * from the caller <i>down</i> to this instance without validation to be applied in the {@link #convertToPixelUnits(int[]) conversion} {@link #convertToWindowUnits(int[]) methods} <b>only</b>.<br/>
   * This allows the caller to pass down knowledge about window- and pixel-unit conversion and utilize mentioned conversion methods.
   * </p>
   * <p>
   * The given pixelScale will not impact the actual {@link #getSurfaceWidth()} and {@link #getSurfaceHeight()},
   * which is determinated by this instances {@link #getUpstreamSurface() upstream surface}.
   * </p>
   * <p>
   * Implementation uses the default pixelScale {@link ScalableSurface#IDENTITY_PIXELSCALE}
   * and resets to default values on {@link #invalidateImpl()}, i.e. {@link #destroyNotify()}.
   * </p>
   * <p>
   * Implementation returns the given pixelScale array.
   * </p>
   */
  @Override
  public final boolean setSurfaceScale(final float[] pixelScale) {
      final boolean changed = hasPixelScale[0] != pixelScale[0] || hasPixelScale[1] != pixelScale[1];
      System.arraycopy(pixelScale, 0, hasPixelScale, 0, 2);
      return changed;
  }

  @Override
  public final float[] getRequestedSurfaceScale(final float[] result) {
      System.arraycopy(hasPixelScale, 0, result, 0, 2);
      return result;
  }

  @Override
  public final float[] getCurrentSurfaceScale(final float[] result) {
      System.arraycopy(hasPixelScale, 0, result, 0, 2);
      return result;
  }

  @Override
  public float[] getMinimumSurfaceScale(final float[] result) {
      System.arraycopy(hasPixelScale, 0, result, 0, 2);
      return result;
  }

  @Override
  public final float[] getMaximumSurfaceScale(final float[] result) {
      System.arraycopy(hasPixelScale, 0, result, 0, 2);
      return result;
  }

}