package com.jogamp.graph.font;

/**
 * Simple static font scale methods for unit conversion.
 */
public class FontScale {
    /**
     * Converts typical font size in points (per-inch) and screen resolution in dpi to font size in pixels (per-inch),
     * which can be used for pixel-size font scaling operations.
     * <p>
     * Note that 1em == size of the selected font.<br/>
     * In case the pixel per em size is required (advance etc),
     * the resulting pixel-size (per-inch) of this method shall be used if rendering directly into the screen resolution!
     * </p>
     * <pre>
        Font Scale Formula:
         1 inch = 25.4 mm

         1 inch = 72 points
         pointSize: [point] = [1/72 inch]

         [1]      Scale := pointSize * resolution / ( 72 points per inch * units_per_em )
         [2]  PixelSize := pointSize * resolution / ( 72 points per inch )
         [3]      Scale := PixelSize / units_per_em
     * </pre>
     * @param font_sz_pt in points (per-inch)
     * @param res_dpi display resolution in dots-per-inch
     * @return pixel-per-inch, pixelSize scale factor for font operations.
     * @see #toPixels2(float, float)
     */
    public static float toPixels(final float font_sz_pt /* points per inch */, final float res_dpi /* dots per inch */) {
        return ( font_sz_pt / 72f /* points per inch */ ) * res_dpi;
    }

    /**
     * Converts typical font size in points-per-inch and screen resolution in points-per-mm to font size in pixels (per-inch),
     * which can be used for pixel-size font scaling operations.
     *
     * @param font_sz_pt in points (per-inch)
     * @param res_ppmm display resolution in dots-per-mm
     * @return pixel-per-inch, pixelSize scale factor for font operations.
     * @see #toPixels(float, float)
     */
    public static float toPixels2(final float font_sz_pt /* points per inch */, final float res_ppmm /* pixels per mm */) {
        return ( font_sz_pt / 72f /* points per inch */ ) * ( res_ppmm * 25.4f /* mm per inch */ ) ;
    }

    /**
     * Converts [1/mm] to [1/inch] in place
     * @param ppmm float[2] [1/mm] value
     * @return return [1/inch] value
     */
    public static float[/*2*/] perMMToPerInch(final float[/*2*/] ppmm) {
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
}
