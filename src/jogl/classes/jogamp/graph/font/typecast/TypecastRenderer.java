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
import jogamp.opengl.Debug;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

/**
 * Factory to build an {@link OutlineShape} from
 * {@link jogamp.graph.font.typecast.ot.OTGlyph Glyph}s.
 *
 * http://www.freetype.org/freetype2/docs/glyphs/glyphs-3.html
 * http://walon.org/pub/ttf/ttf_glyphs.htm
 */
public class TypecastRenderer {
    private static final boolean DEBUG = Debug.debug("graph.font.Renderer");

    private static void addShapeMoveTo(final OutlineShape shape, final Factory<? extends Vertex> vertexFactory, final Point p1) {
        if( DEBUG ) { System.err.println("Shape.MoveTo: "+p1); }
        shape.closeLastOutline(false);
        shape.addEmptyOutline();
        shape.addVertex(0, vertexFactory.create(p1.x,  p1.y, 0, p1.onCurve));
    }
    private static void addShapeLineTo(final OutlineShape shape, final Factory<? extends Vertex> vertexFactory, final Point p1) {
        if( DEBUG ) { System.err.println("Shape.LineTo: "+p1); }
        shape.addVertex(0, vertexFactory.create(p1.x,  p1.y, 0, p1.onCurve));
    }
    private static void addShapeQuadTo(final OutlineShape shape, final Factory<? extends Vertex> vertexFactory, final Point p1, final Point p2) {
        if( DEBUG ) { System.err.println("Shape.QuadTo: "+p1+", "+p2); }
        shape.addVertex(0, vertexFactory.create(p1.x,  p1.y, 0, p1.onCurve));
        shape.addVertex(0, vertexFactory.create(p2.x,  p2.y, 0, p2.onCurve));
    }
    private static void addShapeQuadTo(final OutlineShape shape, final Factory<? extends Vertex> vertexFactory, final Point p1,
                                       final float p2x, final float p2y, final boolean p2OnCurve) {
        if( DEBUG ) { System.err.println("Shape.QuadTo: "+p1+", p2 "+p2x+", "+p2y+", onCurve "+p2OnCurve); }
        shape.addVertex(0, vertexFactory.create(p1.x,  p1.y, 0, p1.onCurve));
        shape.addVertex(0, vertexFactory.create(p2x,    p2y, 0, p2OnCurve));
    }
    /**
    private static void addShapeCubicTo(final OutlineShape shape, Factory<? extends Vertex> vertexFactory, Point p1, Point p2, Point p3) {
        shape.addVertex(0, vertexFactory.create(p1.x,  p1.y, 0, p1.onCurve));
        shape.addVertex(0, vertexFactory.create(p2.x,  p2.y, 0, p2.onCurve));
        shape.addVertex(0, vertexFactory.create(p3.x,  p3.y, 0, p3.onCurve));
    } */

    public static OutlineShape buildShape(final char symbol, final OTGlyph glyph, final Factory<? extends Vertex> vertexFactory) {
        //
        // See Typecast: GlyphPathFactory.addContourToPath(..)
        //

        if (glyph == null) {
            return null;
        }

        final OutlineShape shape = new OutlineShape(vertexFactory);
        buildShapeImpl(shape, symbol, glyph, vertexFactory);
        shape.setIsQuadraticNurbs();
        return shape;
    }

    /**
    private static void buildShapeImpl02(final OutlineShape shape, char symbol, OTGlyph glyph, Factory<? extends Vertex> vertexFactory) {
        // Iterate through all of the points in the glyph.  Each time we find a
        // contour end point, add the point range to the path.
        int startIndex = 0;
        int count = 0;
        for (int i = 0; i < glyph.getPointCount(); i++) {
            count++;
            if ( glyph.getPoint(i).endOfContour ) {
                for(int j=0; j<count; j++) {
                    final Point p = glyph.getPoint(startIndex + j);
                    shape.addVertex(0, vertexFactory.create(p.x,  p.y, 0, p.onCurve));
                }
                shape.closeLastOutline(false);
                startIndex = i + 1;
                count = 0;
            }
        }
    } */

    private static void buildShapeImpl(final OutlineShape shape, final char symbol, final OTGlyph glyph, final Factory<? extends Vertex> vertexFactory) {
        // Iterate through all of the points in the glyph.  Each time we find a
        // contour end point, add the point range to the path.
        int startIndex = 0;
        int count = 0;
        final int totalPoints = glyph.getPointCount();
        for (int i = 0; i < totalPoints; i++) {
            count++;
            if ( glyph.getPoint(i).endOfContour ) {
                int offset = 0;
                while ( offset < count - 1 ) { // require at least +1 point (last one is end-of-contour)
                    final Point p0 = glyph.getPoint(startIndex + offset%count);
                    final Point p1 = glyph.getPoint(startIndex + (offset+1)%count);
                    final Point p2 = glyph.getPoint(startIndex + (offset+2)%count);
                    final Point p3 = offset+3 < count ? glyph.getPoint(startIndex + offset+3) : null;
                    if( DEBUG  ) {
                        System.err.println("GlyphShape<"+symbol+">: offset "+offset+" of "+count+"/"+totalPoints+" points");
                        final int pMIdx= (offset==0) ? startIndex+count-1 : startIndex+(offset-1)%count;
                        final Point pM = glyph.getPoint(pMIdx);
                        final int p0Idx = startIndex + offset%count;
                        final int p1Idx = startIndex + (offset+1)%count;
                        final int p2Idx = startIndex + (offset+2)%count;
                        final int p3Idx = startIndex + (offset+3)%count;
                        System.err.println("\t pM["+pMIdx+"] "+pM);
                        System.err.println("\t p0["+p0Idx+"] "+p0);
                        System.err.println("\t p1["+p1Idx+"] "+p1);
                        System.err.println("\t p2["+p2Idx+"] "+p2);
                        System.err.println("\t p3["+p3Idx+"] "+p3);
                    }
                    if(offset == 0) {
                        addShapeMoveTo(shape, vertexFactory, p0);
                        // gp.moveTo(point.x, point.y);
                    }

                    if( p0.endOfContour ) {
                        // Branch-0: EOC ** SHALL NEVER HAPPEN **
                        if( DEBUG ) { System.err.println("B0 .. end-of-contour **** EOC"); }
                        shape.closeLastOutline(false);
                        break;
                    } else if (p0.onCurve) {
                        if (p1.onCurve) {
                            // Branch-1: point.onCurve && p1.onCurve
                            if( DEBUG ) { System.err.println("B1 .. line-to p0-p1"); }

                            // s = new Line2D.Float(point.x, point.y, p1.x, p1.y);
                            // gp.lineTo( p1.x, p1.y );
                            addShapeLineTo(shape, vertexFactory, p1);
                            offset++;
                        } else {
                            if (p2.onCurve) {
                                // Branch-2: point.onCurve && !p1.onCurve && p2.onCurve
                                if( DEBUG ) { System.err.println("B2 .. quad-to p0-p1-p2"); }

                                // s = new QuadCurve2D.Float( point.x, point.y, p1.x, p1.y, p2.x, p2.y);
                                // gp.quadTo(p1.x, p1.y, p2.x, p2.y);
                                addShapeQuadTo(shape, vertexFactory, p1, p2);
                                offset+=2;
                            } else {
                                if (null != p3 && p3.onCurve) {
                                    // Branch-3: point.onCurve && !p1.onCurve && !p2.onCurve && p3.onCurve
                                    if( DEBUG ) { System.err.println("B3 .. 2-quad p0-p1-p1_2, p1_2-p2-p3 **** 2QUAD"); }
                                    // addShapeCubicTo(shape, vertexFactory, p1, p2, p3);
                                    addShapeQuadTo(shape, vertexFactory, p1,
                                                   midValue(p1.x, p2.x),
                                                   midValue(p1.y, p2.y), true);
                                    addShapeQuadTo(shape, vertexFactory, p2, p3);
                                    offset+=3;
                                } else {
                                    // Branch-4: point.onCurve && !p1.onCurve && !p2.onCurve && !p3.onCurve
                                    if( DEBUG ) { System.err.println("B4 .. quad-to p0-p1-p2h **** MID"); }

                                    // s = new QuadCurve2D.Float(point.x,point.y,p1.x,p1.y,
                                    //                           midValue(p1.x, p2.x), midValue(p1.y, p2.y));
                                    // gp.quadTo(p1.x, p1.y, midValue(p1.x, p2.x), midValue(p1.y, p2.y));
                                    addShapeQuadTo(shape, vertexFactory, p1,
                                                   midValue(p1.x, p2.x),
                                                   midValue(p1.y, p2.y), true);
                                    offset+=2; // Skip p2 as done in Typecast
                                }
                            }
                        }
                    } else {
                        if (!p1.onCurve) {
                            // Branch-5: !point.onCurve && !p1.onCurve
                            if( DEBUG ) { System.err.println("B5 .. quad-to pMh-p0-p1h ***** MID"); }
                            // s = new QuadCurve2D.Float(midValue(pM.x, point.x), midValue(pM.y, point.y),
                            //                           point.x, point.y,
                            //                           midValue(point.x, p1.x), midValue(point.y, p1.y));
                            addShapeQuadTo(shape, vertexFactory, p0,
                                           midValue(p0.x, p1.x), midValue(p0.y, p1.y), true);
                            offset++;
                        } else {
                            // Branch-6: !point.onCurve && p1.onCurve
                            if( DEBUG ) { System.err.println("B6 .. quad-to pMh-p0-p1"); }
                            // s = new QuadCurve2D.Float(midValue(pM.x, point.x), midValue(pM.y, point.y),
                            //                           point.x, point.y, p1.x, p1.y);
                            // gp.quadTo(point.x, point.y, p1.x, p1.y);
                            addShapeQuadTo(shape, vertexFactory, p0, p1);
                            offset++;
                        }
                    }
                }
                shape.closeLastOutline(false);
                startIndex = i + 1;
                count = 0;
            }
        }
    }

    private static float midValue(final float a, final float b) {
        return a + (b - a)/2f;
    }
}
