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
package com.jogamp.opengl.test.junit.graph.demos.ui;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

public abstract class UIShape {
    private final Factory<? extends Vertex> vertexFactory;
    protected OutlineShape shape;
    
    protected static final int DIRTY_SHAPE  = 1 << 0 ;    
    protected int dirty = DIRTY_SHAPE;

    public UIShape(Factory<? extends Vertex> factory) {
        this.vertexFactory = factory;
        this.shape = new OutlineShape(factory);
    }
    
    public void clear() {
        clearImpl();
        shape.clear();
    }
    protected abstract void clearImpl();
    
    protected abstract void createShape();
    
    public boolean updateShape() {
        if( isShapeDirty() ) {
            shape.clear();
            createShape();
            dirty &= ~DIRTY_SHAPE;
            return true;
        }
        return false;
    }
    
    public final Vertex.Factory<? extends Vertex> getVertexFactory() { return vertexFactory; }    
    public AABBox getBounds() { return shape.getBounds(); }
    
    public OutlineShape getShape() { 
        updateShape(); 
        return shape; 
    }
    
    public boolean isShapeDirty() {
        return 0 != ( dirty & DIRTY_SHAPE ) ;
    }    
}
