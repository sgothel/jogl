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

import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ScalableSurface;

/**
 * Basic {@link ScalableSurface} utility to validate and compute pixel-scale values.
 */
public class SurfaceScaleUtils {

    private static final int[] PlatformMaxPixelScale;
    private static final boolean PlatformUniformPixelScale;
    private static final boolean PlatformPixelScaleSupported;

    static {
      if( NativeWindowFactory.TYPE_MACOSX == NativeWindowFactory.getNativeWindowType(true) ) {
          PlatformMaxPixelScale = new int[] { jogamp.nativewindow.macosx.OSXUtil.MAX_PIXELSCALE, jogamp.nativewindow.macosx.OSXUtil.MAX_PIXELSCALE };
          PlatformUniformPixelScale = true;
          PlatformPixelScaleSupported = true;
      } else {
          PlatformMaxPixelScale = new int[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE };
          PlatformUniformPixelScale = false;
          PlatformPixelScaleSupported = false;
      }
    }

    /**
     * Compute a new valid pixelScale to be used by {@link NativeSurface} implementations,
     * based on the given request and surface's pixelScale
     *
     * @param result int[2] storage for result, maybe same as <code>prePixelScale</code> for in-place
     * @param prePixelScale previous pixelScale
     * @param reqPixelScale requested pixelScale, validated via {@link #validateReqPixelScale(int[], int, String)}.
     * @param newPixelScaleRaw new raw surface pixelScale
     * @param DEBUG_PREFIX if set, dumps debug info on stderr using this prefix
     * @return true if pixelScale has changed, otherwise false
     */
    public static boolean computePixelScale(int[] result, final int[] prePixelScale, final int[] reqPixelScale, final int[] newPixelScaleRaw, final String DEBUG_PREFIX) {
        final int newPixelScaleSafeX = 0 < newPixelScaleRaw[0] ? newPixelScaleRaw[0] : ScalableSurface.IDENTITY_PIXELSCALE;
        final int newPixelScaleSafeY = 0 < newPixelScaleRaw[1] ? newPixelScaleRaw[1] : ScalableSurface.IDENTITY_PIXELSCALE;
        final boolean useHiDPI = ScalableSurface.IDENTITY_PIXELSCALE != reqPixelScale[0] || ScalableSurface.IDENTITY_PIXELSCALE != reqPixelScale[1];
        final int prePixelScaleX = prePixelScale[0];
        final int prePixelScaleY = prePixelScale[1];

        if( useHiDPI ) {
            result[0] = newPixelScaleSafeX;
            result[1] = newPixelScaleSafeY;
        } else {
            result[0] = ScalableSurface.IDENTITY_PIXELSCALE;
            result[1] = ScalableSurface.IDENTITY_PIXELSCALE;
        }

        final boolean changed = result[0] != prePixelScaleX || result[1] != prePixelScaleY;
        if( null != DEBUG_PREFIX ) {
            System.err.println(DEBUG_PREFIX+".computePixelScale: useHiDPI "+useHiDPI+", ["+prePixelScaleX+"x"+prePixelScaleY+" (pre), "+
                    reqPixelScale[0]+"x"+reqPixelScale[1]+" (req)] -> "+
                    newPixelScaleRaw[0]+"x"+newPixelScaleRaw[1]+" (raw) -> "+
                    newPixelScaleSafeX+"x"+newPixelScaleSafeY+" (safe) -> "+
                    result[0]+"x"+result[1]+" (use), changed "+changed);
        }
        return changed;
    }

    /**
     * Validate the given requested pixelScale value pair, i.e. clip it to the
     * limits of {@link ScalableSurface#AUTOMAX_PIXELSCALE} and {@link #getPlatformMaxPixelScale(int[])}
     * <p>
     * To be used by {@link ScalableSurface#setSurfaceScale(int[])} implementations.
     * </p>
     *
     * @param result int[2] storage for result
     * @param reqPixelScale requested pixelScale
     * @param DEBUG_PREFIX if set, dumps debug info on stderr using this prefix
     */
    public static void validateReqPixelScale(final int[] result, final int[] reqPixelScale, final String DEBUG_PREFIX) {
        final int minPS = Math.min(reqPixelScale[0], reqPixelScale[1]);
        if( ScalableSurface.AUTOMAX_PIXELSCALE >= minPS ) {
            result[0] = ScalableSurface.AUTOMAX_PIXELSCALE;
            result[1] = ScalableSurface.AUTOMAX_PIXELSCALE;
        } else if( PlatformUniformPixelScale ) {
            final int maxPS = Math.max(reqPixelScale[0], reqPixelScale[1]);
            if( maxPS >= PlatformMaxPixelScale[0] ) {
                result[0] = PlatformMaxPixelScale[0];
                result[1] = PlatformMaxPixelScale[1];
            } else {
                result[0] = maxPS;
                result[1] = maxPS;
            }
        } else {
            if( reqPixelScale[0] >= PlatformMaxPixelScale[0] ) {
                result[0] = PlatformMaxPixelScale[0];
            } else {
                result[0] = reqPixelScale[0];
            }
            if( reqPixelScale[1] >= PlatformMaxPixelScale[1] ) {
                result[1] = PlatformMaxPixelScale[1];
            } else {
                result[1] = reqPixelScale[1];
            }
        }
        if( null != DEBUG_PREFIX ) {
            System.err.println(DEBUG_PREFIX+".validateReqPixelScale: ["+reqPixelScale[0]+"x"+reqPixelScale[1]+" (req), "+
                    PlatformMaxPixelScale[0]+"x"+PlatformMaxPixelScale[1]+" (max)] -> "+
                    result[0]+"x"+result[1]+" (valid)");
        }
    }

    /**
     * Replaces {@link ScalableSurface#AUTOMAX_PIXELSCALE} with {@link #getPlatformMaxPixelScale(int[])},
     * for each component.
     *
     * @param pixelScale int[2] value array to be tested and replaced
     */
    public static void replaceAutoMaxWithPlatformMax(final int[] pixelScale) {
        if( ScalableSurface.AUTOMAX_PIXELSCALE == pixelScale[0] ) {
            pixelScale[0] = PlatformMaxPixelScale[0];
        }
        if( ScalableSurface.AUTOMAX_PIXELSCALE == pixelScale[1] ) {
            pixelScale[1] = PlatformMaxPixelScale[1];
        }
    }

    /**
     * Returns the maximum platform pixelScale
     */
    public static int[] getPlatformMaxPixelScale(final int[] result) {
        System.arraycopy(PlatformMaxPixelScale, 0, result, 0, 2);
        return result;
    }

    /**
     * Returns true if platform pixelScale is uniform, i.e. same scale factor for x- and y-direction, otherwise false.
     */
    public static boolean isPlatformPixelScaleUniform() {
        return PlatformUniformPixelScale;
    }

    /**
     * Returns whether the platform supports pixelScale
     */
    public static boolean isPlatformPixelScaleSupported() {
        return PlatformPixelScaleSupported;
    }

}
