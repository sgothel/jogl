/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.font;

/**
 * Simple static font scale methods for unit conversions.
 *
 * PostScript - current DTP point system used e.g in CSS (Cascading Style Sheets).
 * - 1 point = 1pt = 1/72in (cala) = 0.3528 mm
 * - 1 pica = 1pc = 12pt= 1/6in (cala) = 4.233(3) mm
 */
public class FontScale {
    /**
     * Converts the the given points size to inch, dividing by {@code 72} points per inch.
     * <pre>
         1 points = 1/72 inch
     *
     * </pre>
     */
    public static float ptToInch(final float points) {
        return points / 72f /* points per inch */;
    }

    /**
     * Converts the the given points size to mm, dividing by {@code 72 * 25.4} points per inch.
     * <pre>
         1 inch = 25.4 mm
         1 points = 1/72 inch
         1 points = 1/72 * 25.4 mm
     *
     * </pre>
     */
    public static float ptToMM(final float points) {
        return points / 72f /* points per inch */ * 25.4f /* mm_per_inch */;
    }

    /**
     * Converts typical font size in points and screen resolution in dpi (pixels-per-inch) to font size in pixels,
     * which can be used for pixel-size font scaling operations.
     * <pre>
        Font Scale Formula:
         1 points = 1/72 inch

         pixels = points / 72 * res_dpi
     * </pre>
     * @param points in points
     * @param res_dpi display resolution in pixels-per-inch
     * @return pixelSize scale factor for font operations.
     * @see #toPixels2(float, float)
     */
    public static float toPixels(final float points /* points */, final float res_dpi /* pixels per inch */) {
        return ptToInch( points ) * res_dpi;
    }

    /**
     * Converts typical font size in points and screen resolution in pixels (pixels-per-mm) to font size in pixels,
     * which can be used for pixel-size font scaling operations.
     * <pre>
        Font Scale Formula:
         1 inch = 25.4 mm
         1 points = 1/72 inch
         1 points = 1/72 * 25.4 mm

         pixels = points / 72 * 25.4 * res_ppmm
     * </pre>
     * @param points in points
     * @param res_ppmm display resolution in pixels-per-mm
     * @return pixelSize scale factor for font operations.
     * @see #toPixels(float, float)
     */
    public static float toPixels2(final float points /* points */, final float res_ppmm /* pixels per mm */) {
        return ptToMM( points ) * res_ppmm;
    }

    /**
     * Converts [1/mm] to [1/inch] in place
     * @param ppmm float[2] [1/mm] value
     * @return return [1/inch] value
     */
    public static float[/*2*/] ppmmToPPI(final float[/*2*/] ppmm) {
        ppmm[0] *= 25.4f;
        ppmm[1] *= 25.4f;
        return ppmm;
    }

    /**
     * Converts [1/mm] to [1/inch] into res storage
     * @param ppmm float[2] [1/mm] value
     * @param res the float[2] result storage
     * @return return [1/inch] value, i.e. the given res storage
     */
    public static float[/*2*/] ppmmToPPI(final float[/*2*/] ppmm, final float[/*2*/] res) {
        res[0] = ppmm[0] * 25.4f;
        res[1] = ppmm[1] * 25.4f;
        return res;
    }

    /**
     * Converts [1/inch] to [1/mm] in place
     * @param ppi float[2] [1/inch] value
     * @return return [1/mm] value
     */
    public static float[/*2*/] ppiToPPMM(final float[/*2*/] ppi) {
        ppi[0] /= 25.4f;
        ppi[1] /= 25.4f;
        return ppi;
    }

    /**
     * Converts [1/inch] to [1/mm] into res storage
     * @param ppi float[2] [1/inch] value
     * @param res the float[2] result storage
     * @return return [1/mm] value, i.e. the given res storage
     */
    public static float[/*2*/] ppiToPPMM(final float[/*2*/] ppi, final float[/*2*/] res) {
        res[0] = ppi[0] / 25.4f;
        res[1] = ppi[1] / 25.4f;
        return res;
    }
}
