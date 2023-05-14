/**
 * Copyright 2014-2023 Gothel Software e.K. All rights reserved.
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

import java.security.PrivilegedAction;
import java.util.Map;

import com.jogamp.common.util.SecurityUtil;
import com.jogamp.nativewindow.ScalableSurface;

/**
 * Basic {@link ScalableSurface} utility to validate and compute pixel-scale values.
 */
public class SurfaceScaleUtils {

    private static final float EPSILON = 1.1920929E-7f; // Float.MIN_VALUE == 1.4e-45f ; double EPSILON 2.220446049250313E-16d

    /** Returns true if `abs(a) < EPSILON`, otherwise false. */
    public static boolean isZero(final float a) {
        return Math.abs(a) < EPSILON;
    }
    /** Returns true if `isZero(f2[0]) && isZero(f2[1])`, otherwise false. */
    public static boolean isZero(final float[] f2) {
        return isZero(f2[0]) && isZero(f2[1]);
    }

    /** Returns true if `abs(a-b) < EPSILON`, otherwise false. */
    public static boolean isEqual(final float a, final float b) {
        return Math.abs(a-b) < EPSILON;
    }

    /** Returns true if `isEqual(f2[0], c) && isEqual(f2[1], c)`, otherwise false. */
    public static boolean isEqual(final float[] f2, final float c) {
        return isEqual(f2[0], c) && isEqual(f2[1], c);
    }

    /** Returns true if `isEqual(f2[0], g2[0]) && isEqual(f2[1], g2[1])`, otherwise false. */
    public static boolean isEqual(final float[] f2, final float[] g2) {
        return isEqual(f2[0], g2[0]) && isEqual(f2[1], g2[1]);
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
    public static int[] scale(final int[] result, final int x, final int y, final float[] pixelScale) {
        result[0] = (int) ( x * pixelScale[0] + 0.5f );
        result[1] = (int) ( y * pixelScale[1] + 0.5f );
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
    public static int[] scaleInv(final int[] result, final int x, final int y, final float[] pixelScale) {
        result[0] = (int) ( x / pixelScale[0] + 0.5f );
        result[1] = (int) ( y / pixelScale[1] + 0.5f );
        return result;
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
        if( isEqual(pixelScale, ScalableSurface.IDENTITY_PIXELSCALE) ) {
            return ScalableSurface.IDENTITY_PIXELSCALE;
        } else if( isEqual(pixelScale, ScalableSurface.AUTOMAX_PIXELSCALE) ||
                   pixelScale > maxPixelScale ||
                   isEqual(pixelScale, maxPixelScale)
                 )
        {
            return maxPixelScale;
        } else if( pixelScale < minPixelScale || isEqual(pixelScale, minPixelScale) )
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

    /**
     * Returns a proper string representation of the monitor-name to float[2] pixel-scale map.
     */
    public static String toString(final Map<String,float[/*2*/]> monitorNameToScale) {
        final StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        for(final String name: monitorNameToScale.keySet()) {
            sb.append("'").append(name).append("'").append(" = ( ");
            final float[] value = monitorNameToScale.get(name);
            if( null != value && 2 == value.length ) {
               sb.append(value[0]).append(" / ").append(value[1]);
            }
            sb.append(" ), ");
        }
        sb.append(" }");
        return sb.toString();
    }

    /**
     * Get global pixel-scale values from environment variables, e.g.:
     * - QT_SCREEN_SCALE_FACTORS
     * - QT_SCALE_FACTOR
     * - GDK_SCALE
     * See https://wiki.archlinux.org/title/HiDPI
     * @param env_var_names array of potential environment variable names, treated as float.
     * @param global_pixel_scale_xy store for resulting scale factors
     * @param monitorNameToScale storage mapping monitor names to their pixel_scale_xy, if variable value is of regular expression '(<string>=<float>;)+',
     *        i.e. QT_SCREEN_SCALE_FACTORS='DP-1=1.25;DP-2=1.25;HDMI-1=1.25;'
     * @return index of first found global variable name within env_var_names, otherwise -1
     */
    public static int getPixelScaleEnv(final String[] env_var_names, final float[] global_pixel_scale_xy, final Map<String,float[/*2*/]> monitorNameToScale) {
        final Map<String, String> env = SecurityUtil.doPrivileged(new PrivilegedAction<Map<String, String>>() {
            @Override
            public Map<String, String> run() {
                return System.getenv();
            }
        });
        float global_value = -1.0f;
        int global_idx = -1;
        int mapping_idx = -1;
        boolean done = false;
        for(int var_idx = 0; var_idx < env_var_names.length && !done; ++var_idx ) {
            final String env_var_name = env_var_names[var_idx];
            final String s_value = env.get(env_var_name);
            if( null == s_value || s_value.isEmpty()) {
                continue; // next
            }
            try {
                final float v = Float.valueOf(s_value);
                if( 0 > global_idx ) { // no overwrite
                    global_value = v;
                    global_idx = var_idx;
                }
            } catch(final NumberFormatException nfe) {
                if( 0 <= mapping_idx ) {
                    continue;
                }
                // Attempt to parse regular expression '(<string>=<float>;)+',
                // i.e. QT_SCREEN_SCALE_FACTORS='DP-1=1.25;DP-2=1.25;HDMI-1=1.25;'
                final String[] pairs = s_value.split(";");
                if( null != pairs ) {
                    for(final String pair : pairs) {
                        if( null == pair || pair.isEmpty() ) {
                            continue; // empty is OK, next
                        }
                        final String[] elems = pair.split("=");
                        if( null == elems || 2 != elems.length ) {
                            // syntax error, bail out
                            monitorNameToScale.clear();
                            break;
                        }
                        if( null == elems[0] || elems[0].isEmpty() ) {
                            // syntax error (empty name), bail out
                            monitorNameToScale.clear();
                            break;
                        }
                        if( null == elems[1] || elems[1].isEmpty() ) {
                            // syntax error (empty value), bail out
                            monitorNameToScale.clear();
                            break;
                        }
                        try {
                            final float pair_value = Float.valueOf(elems[1]);
                            monitorNameToScale.put(elems[0], new float[] { pair_value, pair_value} );
                            mapping_idx = var_idx;
                        } catch(final NumberFormatException nfe2) {
                            // syntax error, bail out
                            monitorNameToScale.clear();
                            break;
                        }
                    }
                }
            }
            done = 0 <= mapping_idx && 0 <= global_idx;
        }
        if( 0 <= global_idx ) {
            global_pixel_scale_xy[0] = global_value;
            global_pixel_scale_xy[1] = global_value;
            return global_idx;
        } else if( 0 <= mapping_idx ) {
            return mapping_idx;
        } else {
            return -1;
        }
    }
}
