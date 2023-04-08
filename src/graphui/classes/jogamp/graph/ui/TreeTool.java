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

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.jogamp.graph.ui.Container;
import com.jogamp.graph.ui.Scene;
import com.jogamp.graph.ui.Shape;
import com.jogamp.graph.ui.Shape.Visitor1;
import com.jogamp.graph.ui.Shape.Visitor2;
import com.jogamp.opengl.util.PMVMatrix;

/** Generic static {@link Shape} tree traversal tools, utilized by {@link Scene} and {@link Container} implementations. */
public class TreeTool {

    /**
     * Traverses through the graph up until {@code shape} and apply {@code action} on it.
     * @param pmv
     * @param shape
     * @param action
     * @return true to signal operation complete, i.e. {@code shape} found, otherwise false
     */
    public static boolean forOne(final List<Shape> shapes, final PMVMatrix pmv, final Shape shape, final Runnable action) {
        for(int i=shapes.size()-1; i>=0; i--) {
            final Shape s = shapes.get(i);
            if( s instanceof Container ) {
                final Container c = (Container)s;
                if( !c.contains(shape) ) { // fast-path: skip container
                    continue;
                }
                pmv.glPushMatrix();
                s.setTransform(pmv);
                final boolean res = c.forOne(pmv, shape, action);
                pmv.glPopMatrix();
                if( !res ) { throw new InternalError("Not found "+shape+" in "+c+", but contained"); }
                return true;
            } else {
                if( s.equals(shape) ) {
                    pmv.glPushMatrix();
                    s.setTransform(pmv);
                    action.run();
                    pmv.glPopMatrix();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Traverses through the graph and apply {@link Visitor1#visit(Shape)} for each, stop if it returns true.
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor1#visit(Shape)} returned true, otherwise false
     */
    public static boolean forAll(final List<Shape> shapes, final Visitor1 v) {
        for(int i=shapes.size()-1; i>=0; i--) {
            final Shape s = shapes.get(i);
            boolean res;
            if( s instanceof Container ) {
                final Container c = (Container)s;
                res = c.forAll(v);
            } else {
                res = v.visit(s);
            }
            if( res ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Traverses through the graph and apply {@link Visitor2#visit(Shape, PMVMatrix)} for each, stop if it returns true.
     * @param pmv
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor2#visit(Shape, PMVMatrix)} returned true, otherwise false
     */
    public static boolean forAll(final List<Shape> shapes, final PMVMatrix pmv, final Visitor2 v) {
        for(int i=shapes.size()-1; i>=0; i--) {
            final Shape s = shapes.get(i);
            pmv.glPushMatrix();
            s.setTransform(pmv);
            boolean res;
            if( s instanceof Container ) {
                final Container c = (Container)s;
                res = c.forAll(pmv, v);
            } else {
                res = v.visit(s, pmv);
            }
            pmv.glPopMatrix();
            if( res ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Traverses through the graph and apply {@link Visitor#visit(Shape, PMVMatrix)} for each, stop if it returns true.
     *
     * Each {@link Container} level is sorted using {@code sortComp}
     * @param sortComp
     * @param pmv
     * @param v
     * @return true to signal operation complete and to stop traversal, i.e. {@link Visitor2#visit(Shape, PMVMatrix)} returned true, otherwise false
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static boolean forSortedAll(final Comparator<Shape> sortComp, final List<Shape> shapes, final PMVMatrix pmv, final Visitor2 v) {
        final Object[] shapesS = shapes.toArray();
        Arrays.sort(shapesS, (Comparator)sortComp);

        for(int i=shapesS.length-1; i>=0; i--) {
            final Shape s = (Shape)shapesS[i];
            pmv.glPushMatrix();
            s.setTransform(pmv);
            boolean res;
            if( s instanceof Container ) {
                final Container c = (Container)s;
                res = c.forAll(pmv, v);
            } else {
                res = v.visit(s, pmv);
            }
            pmv.glPopMatrix();
            if( res ) {
                return true;
            }
        }
        return false;
    }

}
