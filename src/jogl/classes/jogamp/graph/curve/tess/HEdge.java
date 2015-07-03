/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
package jogamp.graph.curve.tess;

import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Triangle;


public class HEdge {
    public static final int BOUNDARY = 3;
    public static final int INNER = 1;
    public static final int HOLE = 2;

    private GraphVertex vert;
    private HEdge prev = null;
    private HEdge next = null;
    private HEdge sibling = null;
    private int type = BOUNDARY;
    private Triangle triangle = null;

    public HEdge(final GraphVertex vert, final int type) {
        this.vert = vert;
        this.type = type;
    }

    public HEdge(final GraphVertex vert, final HEdge prev, final HEdge next, final HEdge sibling, final int type) {
        this.vert = vert;
        this.prev = prev;
        this.next = next;
        this.sibling = sibling;
        this.type = type;
    }

    public HEdge(final GraphVertex vert, final HEdge prev, final HEdge next, final HEdge sibling, final int type, final Triangle triangle) {
        this.vert = vert;
        this.prev = prev;
        this.next = next;
        this.sibling = sibling;
        this.type = type;
        this.triangle = triangle;
    }

    public GraphVertex getGraphPoint() {
        return vert;
    }

    public void setVert(final GraphVertex vert) {
        this.vert = vert;
    }

    public HEdge getPrev() {
        return prev;
    }

    public void setPrev(final HEdge prev) {
        this.prev = prev;
    }

    public HEdge getNext() {
        return next;
    }

    public void setNext(final HEdge next) {
        this.next = next;
    }

    public HEdge getSibling() {
        return sibling;
    }

    public void setSibling(final HEdge sibling) {
        this.sibling = sibling;
    }

    public int getType() {
        return type;
    }

    public void setType(final int type) {
        this.type = type;
    }

    public Triangle getTriangle() {
        return triangle;
    }

    public void setTriangle(final Triangle triangle) {
        this.triangle = triangle;
    }

    public static <T extends Vertex> void connect(final HEdge first, final HEdge next){
        first.setNext(next);
        next.setPrev(first);
    }

    public static <T extends Vertex> void makeSiblings(final HEdge first, final HEdge second){
        first.setSibling(second);
        second.setSibling(first);
    }

    public boolean vertexOnCurveVertex(){
        return vert.getPoint().isOnCurve();
    }

}
