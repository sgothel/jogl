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

import javax.media.opengl.GL2ES2;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

public abstract class UIShape {
    private final Factory<? extends Vertex> vertexFactory;
    protected OutlineShape shape;
    
    protected static final int DIRTY_SHAPE  = 1 << 0 ;    
    protected int dirty = DIRTY_SHAPE;
    
    private boolean down = false;

    public UIShape(Factory<? extends Vertex> factory) {
        this.vertexFactory = factory;
        this.shape = new OutlineShape(factory);
    }
    
    public void clear() {
        clearImpl();
        shape.clear();
    }
    
    public abstract void render(GL2ES2 gl, RenderState rs, RegionRenderer renderer, int renderModes, int texSize, boolean selection);
    
    protected boolean positionDirty = false;
    
    private float[] position = new float[]{0,0,0};
    private float[] scale = new float[]{1.0f,1.0f,1.0f};
    public void setScale(float x, float y, float z){
        scale[0] = x;
        scale[1] = y;
        scale[2] = z;
    }
    
    public void setPosition(float x, float y, float z) {
        this.position[0] = x;
        this.position[1] = y;
        this.position[2] = z;
        positionDirty = true;
    }
    
    private void updatePosition () {
        float minX = shape.getBounds().getLow()[0];
        float minY = shape.getBounds().getLow()[1];
        float minZ = shape.getBounds().getLow()[2];
        System.out.println("Position was: " + (position[0]) + " " + (position[1]) + " " + (position[2]));
        System.out.println("Position became: " + (position[0] - minX) + " " + (position[1] - minY) + " " + (position[2] - minZ));
        setPosition(position[0] - minX, position[1] - minY, position[2] - minZ);
        positionDirty = false;
    }
    
    public float[] getScale() { return scale; }   
    public float[] getPosition() { return position; }
    
    protected abstract void clearImpl();
    
    protected abstract void createShape();
    
    public boolean updateShape() {
        if( isShapeDirty() ) {
            shape.clear();
            createShape();
            if(positionDirty){
                updatePosition();
            }
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
    
    public void setPressed(boolean b) {
        this.down  = b;
    }
    
    public boolean isPressed() {
        return this.down;
    }
    
    public abstract void onClick();
    public abstract void onPressed();
    public abstract void onRelease();
}
