/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

import jogamp.graph.font.typecast.ot.OTGlyph;
import jogamp.graph.font.typecast.ot.Point;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

/**
 * Factory to build an {@link OutlineShape} from
 * {@link jogamp.graph.font.typecast.ot.OTGlyph Glyph}s.
 *
 * http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6.html
 */
public class TypecastRenderer {
    private static final boolean DEBUG = false;

    private static void addShapeMoveTo(final OutlineShape shape, Factory<? extends Vertex> vertexFactory, Point p1) {
        shape.closeLastOutline();
        shape.addEmptyOutline();
        shape.addVertex(0, vertexFactory.create(p1.x,  p1.y, 0, true)); // p1.onCurve));
    }
    private static void addShapeLineTo(final OutlineShape shape, Factory<? extends Vertex> vertexFactory, Point p1) {
        shape.addVertex(0, vertexFactory.create(p1.x,  p1.y, 0, true)); // p1.onCurve));
    }
    private static void addShapeQuadTo(final OutlineShape shape, Factory<? extends Vertex> vertexFactory, Point p1, Point p2) {
        shape.addVertex(0, vertexFactory.create(p1.x,  p1.y, 0, false)); // p1.onCurve));
        shape.addVertex(0, vertexFactory.create(p2.x,  p2.y, 0, true)); // p2.onCurve));
    }
    private static void addShapeQuadTo(final OutlineShape shape, Factory<? extends Vertex> vertexFactory, Point p1, float p2x, float p2y, boolean p2OnCurve) {
        shape.addVertex(0, vertexFactory.create(p1.x,  p1.y, 0, false)); // p1.onCurve));
        shape.addVertex(0, vertexFactory.create(p2x,    p2y, 0, true)); // p2OnCurve));
    }
    /**
    private static void addShapeCubicTo(final OutlineShape shape, Factory<? extends Vertex> vertexFactory, Point p1, Point p2, Point p3) {
        shape.addVertex(0, vertexFactory.create(p1.x,  p1.y, 0, false)); // p1.onCurve));
        shape.addVertex(0, vertexFactory.create(p2.x,  p2.y, 0, false)); // p2.onCurve));
        shape.addVertex(0, vertexFactory.create(p3.x,  p3.y, 0, true)); // p3.onCurve));
        // shape.addVertex(0, vertexFactory.create(coords, 0, 2, false));
        // shape.addVertex(0, vertexFactory.create(coords, 2, 2, false));
        // shape.addVertex(0, vertexFactory.create(coords, 4, 2, true));
    }
    private static void addShapeClose(final OutlineShape shape, Factory<? extends Vertex> vertexFactory) {
        shape.closeLastOutline();
    }
    */

    public static OutlineShape buildShape(char symbol, OTGlyph glyph, Factory<? extends Vertex> vertexFactory) {
        //
        // See Typecast: GlyphPathFactory.addContourToPath(..)
        //

        if (glyph == null) {
            return null;
        }

        final OutlineShape shape = new OutlineShape(vertexFactory);

        // Iterate through all of the points in the glyph.  Each time we find a
        // contour end point, add the point range to the path.
        int startIndex = 0;
        int count = 0;
        for (int i = 0; i < glyph.getPointCount(); i++) {
            count++;
            if ( glyph.getPoint(i).endOfContour ) {
                int offset = 0;
                while (offset < count) {
                    final Point point = glyph.getPoint(startIndex + offset%count);
                    final Point point_plus1 = glyph.getPoint(startIndex + (offset+1)%count);
                    final Point point_plus2 = glyph.getPoint(startIndex + (offset+2)%count);
                    if( DEBUG  ) {
                        final Point point_minus1 = glyph.getPoint((offset==0) ? startIndex+count-1 : startIndex+(offset-1)%count);
                        System.err.println("GlyphShape<"+symbol+">: "+count+" points, offset "+offset);
                        System.err.println("\t p-1 "+point_minus1);
                        System.err.println("\t p0  "+point);
                        System.err.println("\t p1  "+point_plus1);
                        System.err.println("\t p2  "+point_plus2);
                    }
                    if(offset == 0)
                    {
                        addShapeMoveTo(shape, vertexFactory, point);
                        // gp.moveTo(point.x, point.y);
                    }

                    if (point.onCurve) {
                        if (point_plus1.onCurve) {
                            // Branch-1: point.onCurve && point_plus1.onCurve
                            if( DEBUG ) { System.err.println("B1 .. line-to p0-p1"); }

                            // s = new Line2D.Float(point.x, point.y, point_plus1.x, point_plus1.y);
                            // gp.lineTo( point_plus1.x, point_plus1.y );
                            addShapeLineTo(shape, vertexFactory, point_plus1);
                            offset++;
                        } else {
                            if (point_plus2.onCurve) {
                                // Branch-2: point.onCurve && !point_plus1.onCurve && point_plus2.onCurve
                                if( DEBUG ) { System.err.println("B2 .. quad-to p0-p1-p2"); }

                                // s = new QuadCurve2D.Float( point.x, point.y, point_plus1.x, point_plus1.y, point_plus2.x, point_plus2.y);
                                // gp.quadTo(point_plus1.x, point_plus1.y, point_plus2.x, point_plus2.y);
                                addShapeQuadTo(shape, vertexFactory, point_plus1, point_plus2);
                                offset+=2;
                            } else {
                                // Branch-3: point.onCurve && !point_plus1.onCurve && !point_plus2.onCurve
                                if( DEBUG ) { System.err.println("B3 .. quad-to p0-p1-p2h **** MID"); }

                                // s = new QuadCurve2D.Float(point.x,point.y,point_plus1.x,point_plus1.y,
                                //                           midValue(point_plus1.x, point_plus2.x), midValue(point_plus1.y, point_plus2.y));
                                // gp.quadTo(point_plus1.x, point_plus1.y, midValue(point_plus1.x, point_plus2.x), midValue(point_plus1.y, point_plus2.y));
                                addShapeQuadTo(shape, vertexFactory, point_plus1,
                                               midValue(point_plus1.x, point_plus2.x),
                                               midValue(point_plus1.y, point_plus2.y), false);
                                // offset++; // Don't skip point_plus2, not completed yet FIXME ?
                                offset+=2; // As done in Typecast
                            }
                        }
                    } else {
                        if (!point_plus1.onCurve) {
                            // Branch-4: !point.onCurve && !point_plus1.onCurve
                            if( DEBUG ) { System.err.println("B4 .. quad-to pM-p0-p1h ***** MID"); }
                            // s = new QuadCurve2D.Float(midValue(point_minus1.x, point.x), midValue(point_minus1.y, point.y),
                            //                           point.x, point.y,
                            //                           midValue(point.x, point_plus1.x), midValue(point.y, point_plus1.y));
                            addShapeQuadTo(shape, vertexFactory, point,
                                           midValue(point.x, point_plus1.x), midValue(point.y, point_plus1.y), false);
                            offset++;
                        } else {
                            // Branch-5: !point.onCurve && point_plus1.onCurve
                            if( DEBUG ) { System.err.println("B5 .. quad-to pM-p0-p1"); }
                            // s = new QuadCurve2D.Float(midValue(point_minus1.x, point.x), midValue(point_minus1.y, point.y),
                            //                           point.x, point.y, point_plus1.x, point_plus1.y);
                            // gp.quadTo(point.x, point.y, point_plus1.x, point_plus1.y);
                            addShapeQuadTo(shape, vertexFactory, point, point_plus1);
                            offset++;
                        }
                    }
                }
                startIndex = i + 1;
                count = 0;
            }
        }
        shape.closeLastOutline();
        return shape;
    }

    private static float midValue(float a, float b) {
        return a + (b - a)/2f;
    }
    /**
    private static Point midPoint(Point a, Point b, boolean onCurve) {
        return new Point(midValue(a.x, b.x), midValue(a.y, b.y), onCurve, false);
    } */
}
