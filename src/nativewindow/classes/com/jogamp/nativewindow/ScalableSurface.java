/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

package com.jogamp.nativewindow;

/**
 * Adding mutable surface pixel scale property to implementing class, usually to a {@link NativeSurface} implementation,
 * see {@link #setSurfaceScale(float[])}.
 */
public interface ScalableSurface {
  /** Setting surface-pixel-scale of {@value}, results in same pixel- and window-units. */
  public static final float IDENTITY_PIXELSCALE = 1f;
  /** Setting surface-pixel-scale of {@value}, results in maximum platform dependent pixel-scale, i.e. pixel-units >> window-units where available. */
  public static final float AUTOMAX_PIXELSCALE = 0f;

  /**
   * Request a pixel scale in x- and y-direction for the associated {@link NativeSurface},
   * where {@code size_in_pixel_units = pixel_scale * size_in_window_units}.
   * <p>
   * Default pixel scale request for both directions is {@link #AUTOMAX_PIXELSCALE}.
   * </p>
   * <p>
   * In case platform only supports uniform pixel scale, i.e. one scale for both directions,
   * either {@link #AUTOMAX_PIXELSCALE} or the maximum requested pixel scale component is used.
   * </p>
   * <p>
   * The <i>requested</i> pixel scale will be validated against platform limits before native scale-setup,
   * i.e. clipped to {@link #IDENTITY_PIXELSCALE} if not supported or clipped to the platform maximum.
   * It can be queried via {@link #getRequestedSurfaceScale(float[])}.
   * </p>
   * <p>
   * The actual <i>realized</i> pixel scale values of the {@link NativeSurface}
   * can be queried via {@link #getCurrentSurfaceScale(float[])} or
   * computed via <code>surface.{@link NativeSurface#convertToPixelUnits(int[]) convertToPixelUnits}(new int[] { 1, 1 })</code>
   * </p>
   * @param pixelScale <i>requested</i> surface pixel scale float[2] values for x- and y-direction.
   * @return {@code true} if the {@link #getCurrentSurfaceScale(float[]) current pixel scale} has changed, otherwise {@code false}.
   * @see #getRequestedSurfaceScale(float[])
   */
  public boolean setSurfaceScale(final float[] pixelScale);

  /**
   * Returns the {@link #setSurfaceScale(float[]) requested} pixel scale of the associated {@link NativeSurface}.
   *
   * @param result float[2] storage for the result
   * @return the passed storage containing the current pixelScale for chaining
   * @see #setSurfaceScale(float[])
   */
  public float[] getRequestedSurfaceScale(final float[] result);

  /**
   * Returns the current pixel scale of the associated {@link NativeSurface}.
   *
   * @param result float[2] storage for the result
   * @return the passed storage containing the current pixelScale for chaining
   */
  public float[] getCurrentSurfaceScale(final float[] result);

  /**
   * Returns the minimum pixel scale of the associated {@link NativeSurface}.
   * @param result float[2] storage for the result
   * @return the passed storage containing the minimum pixelScale for chaining
   */
  public float[] getMinimumSurfaceScale(final float[] result);

  /**
   * Returns the maximum pixel scale of the associated {@link NativeSurface}.
   * <p>
   * The maximum pixel scale maybe used to determine the proper <i>dpi</i>
   * value of the monitor displaying this {@link NativeSurface}.
   * <pre>
   *    surfacePpMM = monitorPpMM * currentSurfaceScale / nativeSurfaceScale,
   *    with PpMM == pixel per millimeter
   * </pre>
   * </p>
   *
   * @param result float[2] storage for the result
   * @return the passed storage containing the maximum pixelScale for chaining
   */
  public float[] getMaximumSurfaceScale(final float[] result);
}

