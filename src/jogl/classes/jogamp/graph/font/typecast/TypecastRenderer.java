/**
 * Copyright 2011-2023 JogAmp Community. All rights reserved.
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
package jogamp.graph.font.typecast;

import jogamp.graph.font.typecast.ot.Glyph;
import jogamp.graph.font.typecast.ot.Point;
import jogamp.graph.font.typecast.ot.T2Glyph;
import jogamp.opengl.Debug;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.math.geom.AABBox;

/**
 * Factory to build an {@link OutlineShape} from
 * {@link jogamp.graph.font.typecast.ot.OTGlyph Glyph}s.
 *
 * <p>
 * TTF Glyph's have Winding.CW, hence we add the OutlineShape in reverse order,
 * i.e. each new vertex at position 0.
 * </p>
 * <p>
 * Outer TTF glyph winding is expected Winding.CW,
 * moved into OutlineShape in reverse as Winding.CCW.
 * </p>
 * <p>
 * Inner TTF glyph winding is expected Winding.CCW
 * moved into OutlineShape in reverse as Winding.CW.
 * </p>
 * http://www.freetype.org/freetype2/docs/glyphs/glyphs-3.html
 * http://walon.org/pub/ttf/ttf_glyphs.htm
 */
public class TypecastRenderer {
    private static final boolean DEBUG = Debug.debug("graph.font.Renderer");
    private static final boolean PRINT_CODE = Debug.debug("graph.font.Renderer.Code");

    private static void addShapeMoveTo(final float unitsPerEM, final OutlineShape shape, final Point p1) {
        if( PRINT_CODE ) {
            System.err.println("// Shape.MoveTo:");
            System.err.printf("shape.closeLastOutline(false);%n");
            System.err.printf("shape.addEmptyOutline();%n");
            System.err.printf("shape.addVertex(%d, %ff, %ff, %b);%n", 0, p1.x/unitsPerEM, p1.y/unitsPerEM, true);
        }
        shape.closeLastOutline(false);
        shape.addEmptyOutline();
        shape.addVertex(0, p1.x/unitsPerEM,  p1.y/unitsPerEM, true);
    }
    private static void addShapeMoveTo(final float unitsPerEM, final OutlineShape shape, final float p1x, final float p1y) {
        if( PRINT_CODE ) {
            System.err.println("// Shape.MoveTo:");
            System.err.printf("shape.closeLastOutline(false);%n");
            System.err.printf("shape.addEmptyOutline();%n");
            System.err.printf("shape.addVertex(%d, %ff, %ff, %b);%n", 0, p1x/unitsPerEM, p1y/unitsPerEM, true);
        }
        shape.closeLastOutline(false);
        shape.addEmptyOutline();
        shape.addVertex(0, p1x/unitsPerEM,  p1y/unitsPerEM, true);
    }
    private static void addShapeMoveTo(final OutlineShape shape, final float p1x, final float p1y) {
        if( PRINT_CODE ) {
            System.err.println("// Shape.MoveTo:");
            System.err.printf("shape.closeLastOutline(false);%n");
            System.err.printf("shape.addEmptyOutline();%n");
            System.err.printf("shape.addVertex(%d, %ff, %ff, %b);%n", 0, p1x, p1y, true);
        }
        shape.closeLastOutline(false);
        shape.addEmptyOutline();
        shape.addVertex(0, p1x,  p1y, true);
    }
    private static void addShapeLineTo(final float unitsPerEM, final OutlineShape shape, final Point p1) {
        if( PRINT_CODE ) {
            System.err.println("// Shape.LineTo:");
            System.err.printf("shape.addVertex(%d, %ff, %ff, %b);%n", 0, p1.x/unitsPerEM, p1.y/unitsPerEM, true);
        }
        shape.addVertex(0, p1.x/unitsPerEM,  p1.y/unitsPerEM, true);
    }
    private static void addShapeLineTo(final OutlineShape shape, final float p1x, final float p1y) {
        if( PRINT_CODE ) {
            System.err.println("// Shape.LineTo:");
            System.err.printf("shape.addVertex(%d, %ff, %ff, %b);%n", 0, p1x, p1y, true);
        }
        shape.addVertex(0, p1x,  p1y, true);
    }
    private static void addShapeQuadTo(final float unitsPerEM, final OutlineShape shape, final Point p1, final Point p2) {
        if( PRINT_CODE ) {
            System.err.println("// Shape.QuadTo:");
            System.err.printf("shape.addVertex(%d, %ff, %ff, %b);%n", 0, p1.x/unitsPerEM, p1.y/unitsPerEM, false);
            System.err.printf("shape.addVertex(%d, %ff, %ff, %b);%n", 0, p2.x/unitsPerEM, p2.y/unitsPerEM, true);
        }
        shape.addVertex(0, p1.x/unitsPerEM,  p1.y/unitsPerEM, false);
        shape.addVertex(0, p2.x/unitsPerEM,  p2.y/unitsPerEM, true);
    }
    private static void addShapeQuadTo(final float unitsPerEM, final OutlineShape shape, final Point p1, final float p2x, final float p2y) {
        if( PRINT_CODE ) {
            System.err.println("// Shape.QuadTo:");
            System.err.printf("shape.addVertex(%d, %ff, %ff, %b);%n", 0, p1.x/unitsPerEM, p1.y/unitsPerEM, false);
            System.err.printf("shape.addVertex(%d, %ff, %ff, %b);%n", 0, p2x/unitsPerEM, p2y/unitsPerEM, true);
        }
        shape.addVertex(0, p1.x/unitsPerEM,  p1.y/unitsPerEM, false);
        shape.addVertex(0, p2x/unitsPerEM,   p2y/unitsPerEM, true);
    }
    private static void addShapeCubicTo(final float unitsPerEM, final OutlineShape shape, final Point p1, final Point p2, final Point p3) {
        if( PRINT_CODE ) {
            System.err.println("// Shape.CubicTo:");
            System.err.printf("shape.addVertex(%d, %ff, %ff, %b);%n", 0, p1.x/unitsPerEM, p1.y/unitsPerEM, false);
            System.err.printf("shape.addVertex(%d, %ff, %ff, %b);%n", 0, p2.x/unitsPerEM, p2.y/unitsPerEM, false);
            System.err.printf("shape.addVertex(%d, %ff, %ff, %b);%n", 0, p3.x/unitsPerEM, p3.y/unitsPerEM, true);
        }
        shape.addVertex(0, p1.x/unitsPerEM,  p1.y/unitsPerEM, false);
        shape.addVertex(0, p2.x/unitsPerEM,  p2.y/unitsPerEM, false);
        shape.addVertex(0, p3.x/unitsPerEM,  p3.y/unitsPerEM, true);
    }

    public static OutlineShape buildEmptyShape(final int unitsPerEM, final AABBox box) {
        final OutlineShape shape = new OutlineShape();
        if( PRINT_CODE ) { System.err.printf("%n// Start Empty Shape%n"); }
        final float x1 = box.getMinX() / unitsPerEM;
        final float x2 = box.getMaxX() / unitsPerEM;
        final float y1 = box.getMinY() / unitsPerEM;
        final float y2 = box.getMaxY() / unitsPerEM;
        {
            // Outer TTF glyph winding is expected Winding.CW, moved into OutlineShape in reverse as Winding.CCW.
            addShapeMoveTo(shape, x1, y1);
            addShapeLineTo(shape, x1, y2);
            addShapeLineTo(shape, x2, y2);
            addShapeLineTo(shape, x2, y1);
            addShapeLineTo(shape, x1, y1);
            shape.closeLastOutline(false);
        }
        {
            // Inner TTF glyph winding is expected Winding.CCW, moved into OutlineShape in reverse as Winding.CW.
            final float dxy_FU = box.getWidth() < box.getHeight() ? box.getWidth() : box.getHeight();
            final float dxy = dxy_FU / unitsPerEM / 20f;
            addShapeMoveTo(shape, x1+dxy, y1+dxy);
            addShapeLineTo(shape, x2-dxy, y1+dxy);
            addShapeLineTo(shape, x2-dxy, y2-dxy);
            addShapeLineTo(shape, x1+dxy, y2-dxy);
            addShapeLineTo(shape, x1+dxy, y1+dxy);
            shape.closeLastOutline(false);
        }
        shape.setIsQuadraticNurbs();
        if( PRINT_CODE ) { System.err.printf("// End Empty Shape%n%n"); }
        return shape;
    }

    public static OutlineShape buildShape(final int unitsPerEM, final Glyph glyph) {
        if (glyph == null) {
            return null;
        }
        final OutlineShape shape = new OutlineShape();
        if (glyph instanceof T2Glyph) {
            // Type 1/2: Cubic
            if( PRINT_CODE ) { System.err.printf("%n// Start Type-2 Shape for Glyph %d%n", glyph.getID()); }
            buildShapeType2(unitsPerEM, shape, (T2Glyph)glyph);
        } else {
            // TTF: quadratic only
            if( PRINT_CODE ) { System.err.printf("%n// Start TTF Shape for Glyph %d%n", glyph.getID()); }
            buildShapeTTF(unitsPerEM, shape, glyph);
            shape.setIsQuadraticNurbs();
        }
        if( PRINT_CODE ) { System.err.printf("// End Shape for Glyph %d%n%n", glyph.getID()); }
        return shape;
    }

    private static int cmod(final int start, final int i, final int count) {
        if( i >= 0 ) {
            return start + ( i % count );
        } else {
            return start + ( i + count );
        }
    }
    private static void buildShapeTTF(final float unitsPerEM, final OutlineShape shape, final Glyph glyph) {
        // Iterate through all of the points in the glyph.  Each time we find a
        // contour end point, add the point range to the path.
        int startIndex = 0;
        int count = 0;
        final int totalPoints = glyph.getPointCount();
        for (int i = 0; i < totalPoints; i++) {
            count++;
            if ( glyph.getPoint(i).endOfContour ) {
                final int modulus = count;
                int offset = 0;
                while ( offset < count ) {
                    final int point_0_idx = cmod(startIndex, offset, modulus);
                    final Point point_m = glyph.getPoint( cmod(startIndex, offset-1, modulus) );
                    final Point point_0 = glyph.getPoint(point_0_idx);
                    final Point point_1 = glyph.getPoint( cmod(startIndex, offset+1, modulus) );
                    final Point point_2 = glyph.getPoint( cmod(startIndex, offset+2, modulus) );

                    // final Point point_3 = offset+3 < count ? glyph.getPoint(startIndex + offset+3) : null;
                    if( DEBUG ) {
                        System.err.println("// GlyphShape<"+glyph.getID()+">: offset "+offset+" of "+count+"/"+totalPoints+" points");
                        final int point_m_idx= cmod(startIndex, offset-1, modulus);
                        final int point_1_idx = cmod(startIndex, offset+1, modulus);
                        final int point_2_idx = cmod(startIndex, offset+2, modulus);
                        // final int point_3_idx = startIndex + (offset+3)%count;
                        System.err.printf("//  pM[%03d] %s%n", point_m_idx, point_m);
                        System.err.printf("//  p0[%03d] %s%n", point_0_idx, point_0);
                        System.err.printf("//  p1[%03d] %s%n", point_1_idx, point_1);
                        System.err.printf("//  p2[%03d] %s%n", point_2_idx, point_2);
                        // System.err.printf("\t p3[%03d] %s%n", point_3_idx, point_3);
                    }
                    if(offset == 0) {
                        if (point_0.onCurve) {
                            if( PRINT_CODE ) { System.err.printf("// %03d: B0a: move-to p0%n", point_0_idx); }
                            addShapeMoveTo(unitsPerEM, shape, point_0);
                        } else if (point_m.onCurve) {
                            if( PRINT_CODE ) { System.err.printf("// %03d: B0b: move-to pM%n", cmod(startIndex, offset-1, modulus)); }
                            addShapeMoveTo(unitsPerEM, shape, point_m);
                            offset--;
                            count--;
                            continue;
                        } else {
                            if( PRINT_CODE ) { System.err.printf("// %03d: B0c: move-to pMh%n", point_0_idx); }
                            addShapeMoveTo(unitsPerEM, shape, midValue(point_m.x, point_0.x), midValue(point_m.y, point_0.y));
                        }
                    }
                    if (point_0.onCurve) {
                        if (point_1.onCurve) {
                            // Branch-1: point.onCurve && p1.onCurve
                            if( PRINT_CODE ) { System.err.printf("// %03d: B1: line-to p0-p1%n", point_0_idx); }
                            addShapeLineTo(unitsPerEM, shape, point_1);
                            offset++;
                        } else {
                            if (point_2.onCurve) {
                                // Branch-2: point.onCurve && !p1.onCurve && p2.onCurve
                                if( PRINT_CODE ) { System.err.printf("// %03d: B2: quad-to p0-p1-p2%n", point_0_idx); }
                                addShapeQuadTo(unitsPerEM, shape, point_1, point_2);
                                offset+=2;
                            } else {
                                /** if (null != point_3 && point_3.onCurve) {
                                    // Not required, handled via B4 and subsequent B6!
                                    // Branch-3: point.onCurve && !p1.onCurve && !p2.onCurve && p3.onCurve
                                    if( PRINT_CODE ) { System.err.printf("// %03d: B3: p0-p1-p1_2, p1_2-p2-p3 **** 2QUAD%n", point_0_idx); }
                                    addShapeQuadTo(unitsPerEM, shape, point_1, midValue(point_1.x, point_2.x),
                                                   midValue(point_1.y, point_2.y));
                                    addShapeQuadTo(unitsPerEM, shape, point_2, point_3);
                                    offset+=3;
                                } else */ {
                                    // Branch-4: point.onCurve && !p1.onCurve && !p2.onCurve && !p3.onCurve
                                    if( PRINT_CODE ) { System.err.printf("// %03d: B4: quad-to p0-p1-p2h **** MID%n", point_0_idx); }
                                    addShapeQuadTo(unitsPerEM, shape, point_1,
                                                   midValue(point_1.x, point_2.x), midValue(point_1.y, point_2.y));
                                    offset+=2; // Skip p2 as done in Typecast
                                }
                            }
                        }
                    } else {
                        if (!point_1.onCurve) {
                            // Branch-5: !point.onCurve && !p1.onCurve
                            if( PRINT_CODE ) { System.err.printf("// %03d: B5: quad-to pMh-p0-p1h ***** MID%n", point_0_idx); }
                            addShapeQuadTo(unitsPerEM, shape, point_0,
                                           midValue(point_0.x, point_1.x), midValue(point_0.y, point_1.y) );
                            offset++;
                        } else {
                            // Branch-6: !point.onCurve && p1.onCurve
                            if( PRINT_CODE ) { System.err.printf("// %03d: B6: quad-to pMh-p0-p1%n", point_0_idx); }
                            addShapeQuadTo(unitsPerEM, shape, point_0, point_1);
                            offset++;
                        }
                    }
                }
                if( PRINT_CODE ) { System.err.printf("shape.closeLastOutline(false);%n%n"); }
                shape.closeLastOutline(false);
                startIndex = i + 1;
                count = 0;
            }
        }
    }

    private static void buildShapeType2(final float unitsPerEM, final OutlineShape shape, final T2Glyph glyph) {
        // Iterate through all of the points in the glyph.  Each time we find a
        // contour end point, add the point range to the path.
        int startIndex = 0;
        int count = 0;
        final int totalPoints = glyph.getPointCount();
        for (int i = 0; i < totalPoints; i++) {
            count++;
            if ( glyph.getPoint(i).endOfContour ) {
                int offset = 0;
                while ( offset < count ) {
                    final int point_0_idx = startIndex + offset%count;
                    final Point point_0 = glyph.getPoint(point_0_idx);
                    final Point point_1 = glyph.getPoint(startIndex + (offset+1)%count);
                    final Point point_2 = glyph.getPoint(startIndex + (offset+2)%count);
                    final Point point_3 = glyph.getPoint(startIndex + (offset+3)%count);
                    if( DEBUG ) {
                        System.err.println("// GlyphShape<"+glyph.getID()+">: offset "+offset+" of "+count+"/"+totalPoints+" points");
                        final int point_1_idx = startIndex + (offset+1)%count;
                        final int point_2_idx = startIndex + (offset+2)%count;
                        final int point_3_idx = startIndex + (offset+3)%count;
                        System.err.println("//  p0["+point_0_idx+"] "+point_0);
                        System.err.println("//  p1["+point_1_idx+"] "+point_1);
                        System.err.println("//  p2["+point_2_idx+"] "+point_2);
                        System.err.println("//  p3["+point_3_idx+"] "+point_3);
                    }
                    if(offset == 0) {
                        addShapeMoveTo(unitsPerEM, shape, point_0);
                    }
                    if (point_0.onCurve && point_1.onCurve) {
                        // Branch-1: point.onCurve && p1.onCurve
                        if( PRINT_CODE ) { System.err.printf("// %03d: C1: line-to p0-p1%n", point_0_idx); }
                        addShapeLineTo(unitsPerEM, shape, point_1);
                        offset++;
                    } else if (point_0.onCurve && !point_1.onCurve && !point_2.onCurve && point_3.onCurve) {
                        if( PRINT_CODE ) { System.err.printf("// %03d: C2: cubic-to p0-p1-p2%n", point_0_idx); }
                        addShapeCubicTo(unitsPerEM, shape, point_0, point_2, point_3);
                        offset+=3;
                    } else {
                        System.out.println("addContourToPath case not catered for!!");
                        break;
                    }
                }
                if( PRINT_CODE ) { System.err.printf("shape.closeLastOutline(false);%n%n"); }
                shape.closeLastOutline(false);
                startIndex = i + 1;
                count = 0;
            }
        }
    }

    /**
     * Returns the mid-value of two.
     * <p>
     * Intentionally using integer arithmetic on unitPerEM sized values w/o rounding.
     * </p>
     */
    private static int midValue(final int a, final int b) {
        return a + (b - a)/2;
    }

    //
    // Leaving Typecast's orig rendering loop in here, transformed to using our OutlineShape.
    // This is now actually the same since ours has been re-aligned on 2023-02-15.
    //

    @SuppressWarnings("unused")
    private static void buildShapeImplX(final float unitsPerEM, final OutlineShape shape, final Glyph glyph) {
        // Iterate through all of the points in the glyph.  Each time we find a
        // contour end point, add the point range to the path.
        int firstIndex = 0;
        int count = 0;
        final int totalPoints = glyph.getPointCount();
        if (glyph instanceof T2Glyph) {
            // addContourToPath(unitsPerEM, shape, (T2Glyph) glyph, firstIndex, count);
            throw new RuntimeException("T2Glyph Not Yet Supported: "+glyph);
        }
        if( PRINT_CODE ) { System.err.printf("%n// Start Shape for Glyph %d%n", glyph.getID()); }
        for (int i = 0; i < totalPoints; i++) {
            count++;
            if ( glyph.getPoint(i).endOfContour ) {
                addContourToPathX1(unitsPerEM, shape, glyph, firstIndex, count);
                firstIndex = i + 1;
                count = 0;
            }
        }
        if( PRINT_CODE ) { System.err.printf("// End Shape for Glyph %d%n%n", glyph.getID()); }
    }
    private static void addContourToPathX1(final float unitsPerEM, final OutlineShape shape, final Glyph glyph, final int startIndex, final int count) {
        int offset = 0;
        while ( offset < count ) {
            final int point_0_idx = startIndex + offset%count;
            final Point point_m = glyph.getPoint((offset==0) ? startIndex+count-1 : startIndex+(offset-1)%count);
            final Point point_0 = glyph.getPoint(point_0_idx);
            final Point point_1 = glyph.getPoint(startIndex + (offset+1)%count);
            final Point point_2 = glyph.getPoint(startIndex + (offset+2)%count);
            // final Point point_3 = offset+3 < count ? glyph.getPoint(startIndex + offset+3) : null;
            if( DEBUG ) {
                System.err.println("// GlyphShape<"+glyph.getID()+">: offset "+offset+" of "+count+" points");
                final int point_m_idx= (offset==0) ? startIndex+count-1 : startIndex+(offset-1)%count;
                final int point_1_idx = startIndex + (offset+1)%count;
                final int point_2_idx = startIndex + (offset+2)%count;
                // final int p3Idx = startIndex + (offset+3)%count;
                System.err.println("//  pM["+point_m_idx+"] "+point_m);
                System.err.println("//  p0["+point_0_idx+"] "+point_0);
                System.err.println("//  p1["+point_1_idx+"] "+point_1);
                System.err.println("//  p2["+point_2_idx+"] "+point_2);
                // System.err.println("\t p3["+p3Idx+"] "+point_3);
            }
            if(offset == 0) {
                addShapeMoveTo(unitsPerEM, shape, point_0);
            }
            if (point_0.onCurve && point_1.onCurve) {
                // Branch-1: point.onCurve && p1.onCurve
                if( PRINT_CODE ) { System.err.printf("// %03d: B1: line-to p0-p1%n", point_0_idx); }
                addShapeLineTo(unitsPerEM, shape, point_1);
                offset++;
            } else if (point_0.onCurve && !point_1.onCurve && point_2.onCurve) {
                // Branch-2: point.onCurve && !p1.onCurve && p2.onCurve
                if( PRINT_CODE ) { System.err.printf("// %03d: B2: quad-to p0-p1-p2%n", point_0_idx); }
                addShapeQuadTo(unitsPerEM, shape, point_1, point_2);
                offset+=2;
            } else if (point_0.onCurve && !point_1.onCurve && !point_2.onCurve) {
                // Branch-4: point.onCurve && !p1.onCurve && !p2.onCurve && !p3.onCurve
                if( PRINT_CODE ) { System.err.printf("// %03d: B4: quad-to p0-p1-p2h **** MID%n", point_0_idx); }
                addShapeQuadTo(unitsPerEM, shape, point_1,
                        midValue(point_1.x, point_2.x),
                        midValue(point_1.y, point_2.y));

                offset+=2;
            } else if (!point_0.onCurve && !point_1.onCurve) {
                // Branch-5: !point.onCurve && !p1.onCurve
                if( PRINT_CODE ) { System.err.printf("// %03d: B5: quad-to pMh-p0-p1h ***** MID%n", point_0_idx); }
                addShapeQuadTo(unitsPerEM, shape, point_0,
                        midValue(point_0.x, point_1.x),
                        midValue(point_0.y, point_1.y) );
                offset++;
            } else if (!point_0.onCurve && point_1.onCurve) {
                // Branch-6: !point.onCurve && p1.onCurve
                if( PRINT_CODE ) { System.err.printf("// %03d: B6: quad-to pMh-p0-p1%n", point_0_idx); }
                addShapeQuadTo(unitsPerEM, shape, point_0, point_1);
                offset++;
            } else {
                System.out.println("addContourToPath case not catered for!!");
                break;
            }
        }
        if( PRINT_CODE ) { System.err.printf("shape.closeLastOutline(false);%n%n"); }
        shape.closeLastOutline(false);
    }
}
