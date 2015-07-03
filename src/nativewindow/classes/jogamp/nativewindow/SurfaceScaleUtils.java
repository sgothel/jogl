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
package jogamp.nativewindow;

import com.jogamp.nativewindow.ScalableSurface;

/**
 * Basic {@link ScalableSurface} utility to validate and compute pixel-scale values.
 */
public class SurfaceScaleUtils {

    private static final float EPSILON = 1.1920929E-7f; // Float.MIN_VALUE == 1.4e-45f ; double EPSILON 2.220446049250313E-16d

    private static boolean isZero(final float a) {
        return Math.abs(a) < EPSILON;
    }

    /**
     * Returns integer rounded product, i.e. {@code (int) ( a * pixelScale + 0.5f )}
     *
     * @param a the int value
     * @param pixelScale the float scale factor
     * @return the integer rounded product
     */
    public static int scale(final int a, final float pixelScale) {
        return (int) ( a * pixelScale + 0.5f );
    }

    /**
     * Returns integer rounded product, i.e. {@code (int) ( a / pixelScale + 0.5f )}
     *
     * @param a the int value
     * @param pixelScale the float scale factor
     * @return the integer rounded product
     */
    public static int scaleInv(final int a, final float pixelScale) {
        return (int) ( a / pixelScale + 0.5f );
    }

    /**
     * Returns integer rounded product, i.e. {@code (int) ( a * pixelScale + 0.5f )}
     *
     * @param result the int[2] result, may be {@code a} for in-place operation
     * @param a the int[2] values
     * @param pixelScale the float[2] scale factors
     * @return the result for chaining
     */
    public static int[] scale(final int[] result, final int[] a, final float[] pixelScale) {
        result[0] = (int) ( a[0] * pixelScale[0] + 0.5f );
        result[1] = (int) ( a[1] * pixelScale[1] + 0.5f );
        return result;
    }
    /**
     * Returns integer rounded product, i.e. {@code (int) ( a / pixelScale + 0.5f )}
     *
     * @param result the int[2] result, may be {@code a} for in-place operation
     * @param a the int[2] values
     * @param pixelScale the float[2] scale factors
     * @return the result for chaining
     */
    public static int[] scaleInv(final int[] result, final int[] a, final float[] pixelScale) {
        result[0] = (int) ( a[0] / pixelScale[0] + 0.5f );
        result[1] = (int) ( a[1] / pixelScale[1] + 0.5f );
        return result;
    }

    /**
     * Method constrains the given pixel-scale within ]0..{@code maxPixelScale}], as described below.
     * <p>
     * Method returns {@link ScalableSurface#IDENTITY_PIXELSCALE IDENTITY_PIXELSCALE} if:
     * <ul>
     *   <li>{@code pixelScale} ~= {@link ScalableSurface#IDENTITY_PIXELSCALE IDENTITY_PIXELSCALE}</li>
     * </ul>
     * </p>
     * <p>
     * Method returns {@code maxPixelScale} if
     * <ul>
     *   <li>{@code pixelScale} ~= {@link ScalableSurface#AUTOMAX_PIXELSCALE AUTOMAX_PIXELSCALE}</li>
     *   <li>{@code pixelScale} &gt; {@code maxPixelScale}</li>
     *   <li>{@code pixelScale} ~= {@code maxPixelScale}</li>
     * </ul>
     * </p>
     * <p>
     * Method returns {@code minPixelScale} if
     * <ul>
     *   <li>{@code pixelScale} &lt; {@code minPixelScale}</li>
     *   <li>{@code pixelScale} ~= {@code minPixelScale}</li>
     * </ul>
     * </p>
     * <p>
     * Otherwise method returns the given {@code pixelScale}.
     * </p>
     * <p>
     * <i>~=</i> denominates a delta &le; {@link FloatUtil#EPSILON}.
     * </p>
     * @param pixelScale pixel-scale to be constrained
     * @param minPixelScale minimum pixel-scale
     * @param maxPixelScale maximum pixel-scale
     * @return the constrained pixel-scale
     */
    public static float clampPixelScale(final float pixelScale, final float minPixelScale, final float maxPixelScale) {
        if( isZero(pixelScale-ScalableSurface.IDENTITY_PIXELSCALE) ) {
            return ScalableSurface.IDENTITY_PIXELSCALE;
        } else if( isZero(pixelScale-ScalableSurface.AUTOMAX_PIXELSCALE) ||
                   pixelScale > maxPixelScale ||
                   isZero(pixelScale-maxPixelScale)
                 )
        {
            return maxPixelScale;
        } else if( pixelScale < minPixelScale || isZero(pixelScale-minPixelScale) )
        {
            return minPixelScale;
        } else {
            return pixelScale;
        }
    }

    /**
     * Method {@link #clampPixelScale(float, float, float) constrains} the given float[2] pixel-scale
     * within ]0..{@code maxPixelScale}], as described in {@link #clampPixelScale(float, float, float)}.
     *
     * @param result float[2] storage for result, maybe same as <code>s</code> for in-place
     * @param pixelScale float[2] pixelScale to be constrained
     * @param minPixelScale float[2] minimum pixel-scale
     * @param maxPixelScale float[2] maximum pixel-scale
     * @return the constrained result for chaining
     */
    public static float[] clampPixelScale(final float[] result, final float[] pixelScale,
                                          final float[] minPixelScale, final float[] maxPixelScale) {
        result[0] = clampPixelScale(pixelScale[0], minPixelScale[0], maxPixelScale[0]);
        result[1] = clampPixelScale(pixelScale[1], minPixelScale[1], maxPixelScale[1]);
        return result;
    }

    /**
     * Method writes the given float[2] requested pixel-scale {@code reqPixelScale}
     * into {@code result} within its constraints ]0..{@code maxPixelScale}], as described in {@link #clampPixelScale(float, float, float)}.
     * <p>
     * Method only differs from {@link #clampPixelScale(float[], float[], float[], float[])}
     * by returning the whether the value has changed, i.e. different from the given {@code prePixelScale}.
     * </p>
     *
     * @param result int[2] storage for result, maybe same as <code>prePixelScale</code> for in-place
     * @param prePixelScale float[2] previous pixel-scale
     * @param reqPixelScale float[2] requested pixel-scale, validated via {@link #validateReqPixelScale(float[], float[], String)}.
     * @param minPixelScale float[2] minimum pixel-scale
     * @param maxPixelScale float[2] maximum pixel-scale
     * @param DEBUG_PREFIX if set, dumps debug info on stderr using this prefix
     * @param newPixelScaleRaw new raw surface pixel-scale
     * @return {@code true} if pixel-scale has changed, otherwise {@code false}.
     */
    public static boolean setNewPixelScale(final float[] result,
                                           final float[] prePixelScale, final float[] reqPixelScale,
                                           final float[] minPixelScale, final float[] maxPixelScale,
                                           final String DEBUG_PREFIX) {
        final float resultX = clampPixelScale(reqPixelScale[0], minPixelScale[0], maxPixelScale[0]);
        final float resultY = clampPixelScale(reqPixelScale[1], minPixelScale[1], maxPixelScale[1]);
        final boolean changed = resultX != prePixelScale[0] || resultY != prePixelScale[1];
        if( null != DEBUG_PREFIX ) {
            System.err.println(DEBUG_PREFIX+".setNewPixelScale: pre["+prePixelScale[0]+", "+prePixelScale[1]+"], req["+
                    reqPixelScale[0]+", "+reqPixelScale[1]+"], min["+
                    minPixelScale[0]+", "+minPixelScale[1]+"], max["+
                    maxPixelScale[0]+", "+maxPixelScale[1]+"] -> result["+
                    resultX+", "+resultY+"], changed "+changed);
        }
        result[0] = resultX;
        result[1] = resultY;
        return changed;
    }
}
