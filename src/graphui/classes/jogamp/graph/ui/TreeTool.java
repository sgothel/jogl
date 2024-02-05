/**
 * Copyright 2023 JogAmp Community. All rights reserved.
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
package jogamp.graph.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.jogamp.graph.ui.Container;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.Shape.Visitor1;
import com.jogamp.graph.ui.Shape.Visitor2;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;

/** Generic static {@link Shape} tree traversal tools, utilized by {@link Scene} and {@link Container} implementations. */
public class TreeTool {

    /**
     * Traverses through the graph up until {@code shape} of {@link Container#getShapes()} and apply {@code action} on it.
     * @param pmv
     * @param shape
     * @param action
     * @return true to signal operation complete, i.e. {@code shape} found, otherwise false
     */
    public static boolean forOne(final Container cont, final PMVMatrix4f pmv, final Shape shape, final Runnable action) {
        final List<Shape> shapes = cont.getShapes();
        final int shapeCount = shapes.size();
        for(int i=0; i<shapeCount; ++i) {
            final Shape s = shapes.get(i);
            if( s.equals(shape) ) {
                pmv.pushMv();
                s.applyMatToMv(pmv);
                action.run();
                pmv.popMv();
                return true;
            } else if( s instanceof Container ) {
                final Container c = (Container)s;
                if( !c.contains(shape) ) { // fast-path: skip container
                    continue;
                }
                pmv.pushMv();
                s.applyMatToMv(pmv);
                final boolean res = forOne(c, pmv, shape, action);
                pmv.popMv();
                if( !res ) { throw new InternalError("Not found "+shape+" in "+c+", but contained"); }
                return true;
            }
        }
        return false;
    }

    /**
     * Traverses through the graph and apply {@link Visitor1#visit(Shape)} for each {@link Shape} of {@link Container#getShapes()}, stop if it returns true.
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor1#visit(Shape)} returned true, otherwise false
     */
    public static boolean forAll(final Container cont, final Visitor1 v) {
        final List<Shape> shapes = cont.getShapes();
        final int shapeCount = shapes.size();
        boolean res = false;
        for(int i=0; !res && i<shapeCount; ++i) {
            final Shape s = shapes.get(i);
            res = v.visit(s);
            if( !res && s instanceof Container ) {
                final Container c = (Container)s;
                res = forAll(c, v);
            }
        }
        return res;
    }

    /**
     * Traverses through the graph and apply {@link Visitor2#visit(Shape, PMVMatrix4f)} for each {@link Shape} of {@link Container#getShapes()}, stop if it returns true.
     * @param pmv
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor2#visit(Shape, PMVMatrix4f)} returned true, otherwise false
     */
    public static boolean forAll(final Container cont, final PMVMatrix4f pmv, final Visitor2 v) {
        final List<Shape> shapes = cont.getShapes();
        final int shapeCount = shapes.size();
        boolean res = false;
        for(int i=0; !res && i<shapeCount; ++i) {
            final Shape s = shapes.get(i);
            pmv.pushMv();
            s.applyMatToMv(pmv);
            res = v.visit(s, pmv);
            if( !res && s instanceof Container ) {
                final Container c = (Container)s;
                res = forAll(c, pmv, v);
            }
            pmv.popMv();
        }
        return res;
    }

    /**
     * Traverses through the graph and apply {@link Visitor2#visit(Shape, PMVMatrix4f)} for each {@link Shape} of {@link Container#getShapes()},
     * stops if it returns true.
     * <p>
     * Each {@link Container} level is sorted using {@code sortComp}
     * </p>
     * @param cont container of the shapes
     * @param sortComp
     * @param pmv
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor2#visit(Shape, PMVMatrix4f)} returned true, otherwise false
     */
    public static boolean forSortedAll(final Container cont, final Comparator<Shape> sortComp, final PMVMatrix4f pmv, final Visitor2 v) {
        final List<Shape> shapes = cont.getShapes();
        final int shapeCount = shapes.size();
        final Shape[] shapesS = shapes.toArray(new Shape[shapeCount]);
        Arrays.sort(shapesS, sortComp);
        boolean res = false;

        for(int i=0; !res && i<shapeCount; ++i) {
            final Shape s = shapesS[i];
            pmv.pushMv();
            s.applyMatToMv(pmv);
            res = v.visit(s, pmv);
            if( !res && s instanceof Container ) {
                final Container c = (Container)s;
                res = forSortedAll(c, sortComp, pmv, v);
            }
            pmv.popMv();
        }
        return res;
    }

    /**
     * Traverses through the graph and apply {@link Visitor2#visit(Shape, PMVMatrix4f)} for each {@link Shape} of {@link Container#getRenderedShapes()},
     * stops if it returns true.
     * <p>
     * Each {@link Container} level is sorted using {@code sortComp}
     * </p>
     * @param cont container of the shapes
     * @param ascnZOrder if {@code true}, traverse through {@link Container#getRenderedShapes()} in ascending z-axis order (bottom-up), otherwise descending (top-down)
     * @param pmv
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor2#visit(Shape, PMVMatrix4f)} returned true, otherwise false
     */
    public static boolean forAllRendered(final Container cont, final boolean ascnZOrder, final PMVMatrix4f pmv, final Visitor2 v) {
        if( ascnZOrder ) {
            return forAllRenderedAscn(cont, pmv, v);
        } else {
            return forAllRenderedDesc(cont, pmv, v);
        }
    }
    private static boolean forAllRenderedAscn(final Container cont, final PMVMatrix4f pmv, final Visitor2 v) {
        final List<Shape> shapes = cont.getRenderedShapes();
        boolean res = false;

        for(int i=0; !res && i<shapes.size(); ++i) {
            final Shape s = shapes.get(i);
            pmv.pushMv();
            s.applyMatToMv(pmv);
            res = v.visit(s, pmv);
            if( !res && s instanceof Container ) {
                final Container c = (Container)s;
                res = forAllRenderedAscn(c, pmv, v);
            }
            pmv.popMv();
        }
        return res;
    }
    private static boolean forAllRenderedDesc(final Container cont, final PMVMatrix4f pmv, final Visitor2 v) {
        final List<Shape> shapes = cont.getRenderedShapes();
        boolean res = false;

        for(int i=shapes.size()-1; !res && i>=0; --i) {
            final Shape s = shapes.get(i);
            pmv.pushMv();
            s.applyMatToMv(pmv);
            res = v.visit(s, pmv);
            if( !res && s instanceof Container ) {
                final Container c = (Container)s;
                res = forAllRenderedDesc(c, pmv, v);
            }
            pmv.popMv();
        }
        return res;
    }

    public static boolean contains(final Container cont, final Shape s) {
        final List<Shape> shapes = cont.getShapes();
        if( shapes.contains(s) ) {
            return true;
        }
        for(final Shape shape : shapes) {
            if( shape instanceof Container ) {
                if( contains((Container)shape, s) ) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Shape getShapeByID(final Container cont, final int id) {
        final Shape[] res = { null };
        forAll(cont, (final Shape s) -> {
            if( s.getID() == id ) {
                res[0] = s;
                return true;
            } else {
                return false;
            }
        });
        return res[0];
    }
    public static Shape getShapeByName(final Container cont, final String name) {
        final Shape[] res = { null };
        forAll(cont, (final Shape s) -> {
            if( s.getName().equals(name) ) {
                res[0] = s;
                return true;
            } else {
                return false;
            }
        });
        return res[0];
    }

    public static boolean isCovered(final List<AABBox> coverage, final AABBox box) {
        for(final AABBox b : coverage) {
            if( b.contains(box) ) {
                return true;
            }
        }
        return false;
    }

    public static void cullShapes(final Shape[] shapesZAsc, final int shapeCount) {
        final List<AABBox> coverage = new ArrayList<AABBox>();
        for(int i=shapeCount-1; i>=0; --i) {
            final Shape s = shapesZAsc[i];
            if( coverage.size() == 0 ) {
                coverage.add(s.getBounds());
                s.setDiscarded(false);
            } else {
                final AABBox b = s.getBounds();
                if( isCovered(coverage, b) ) {
                    s.setDiscarded(true);
                } else {
                    coverage.add(b);
                    s.setDiscarded(false);
                }
            }
        }
    }
}
