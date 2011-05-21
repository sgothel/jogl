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

import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

/** GPU based resolution independent Button impl
 */
public class RIButton extends UIShape {
    private float width, height;
    private Label label;
    private float spacing = 2.0f;
    private float[] scale = new float[]{1.0f,1.0f};
    private float corner = 1.0f;
    private float labelZOffset = -0.05f;
    
    private float[] buttonColor = {0.6f, 0.6f, 0.6f};
    private float[] labelColor = {1.0f, 1.0f, 1.0f};
    
    public RIButton(Factory<? extends Vertex> factory, Font labelFont, String labelText, float width, float height) {
        // w 4.0f, h 3.0f
        // FontFactory.get(FontFactory.UBUNTU).getDefault()
        super(factory);
        
        // FIXME: Determine font size - PMV Matrix relation ?
        // this.label = new Label(factory, labelFont, (int)(height - 2f * spacing), labelText);
        this.label = new Label(factory, labelFont, 10, labelText);
        this.width = width;
        this.height = height;
    }

    public final float getWidth() { return width; }
    public final float getHeight() { return height; }
    public float getCorner() { return corner; }
    public float[] getScale() { return scale; }    
    public Label getLabel() { return label; }

    public void setDimension(int width, int height) {
        this.width = width;
        this.height = height;
        dirty |= DIRTY_SHAPE;
    }

    @Override
    protected void clearImpl() {
        label.clear();
    }
    
    @Override
    protected void createShape() {
        // FIXME: Only possible if all data (color) is 
        //        is incl. in Outline Shape.
        //        Until then - draw each separately!
        //shape.addOutlinShape( label.getShape() );
        label.updateShape();
        
        final AABBox lbox = label.getBounds();
        if(corner == 0.0f) {
            createSharpOutline(lbox);
        } else {
            createCurvedOutline(lbox);
        }
        scale[0] = getWidth() / ( 2f*spacing + lbox.getWidth() );
        scale[1] = getHeight() / ( 2f*spacing + lbox.getHeight() );
    }
    
    
    private void createSharpOutline(AABBox lbox) {
        float th = (2f*spacing) + lbox.getHeight();
        float tw = (2f*spacing) + lbox.getWidth();
        float minX = lbox.getMinX()-spacing;
        float minY = lbox.getMinY()-spacing;
        
        shape.addVertex(minX, minY, labelZOffset,  true);
        shape.addVertex(minX+tw, minY,  labelZOffset, true);
        shape.addVertex(minX+tw, minY + th, labelZOffset,  true);
        shape.addVertex(minX, minY + th, labelZOffset,  true);
        shape.closeLastOutline();
    }
    
    private void createCurvedOutline(AABBox lbox){
        float th = 2.0f*spacing + lbox.getHeight();
        float tw = 2.0f*spacing + lbox.getWidth();
        
        float cw = 0.5f*corner*Math.min(tw, th);
        float ch = 0.5f*corner*Math.min(tw, th);
        
        float minX = lbox.getMinX()-spacing;
        float minY = lbox.getMinY()-spacing;
        
        shape.addVertex(minX, minY + ch, labelZOffset, true);
        shape.addVertex(minX, minY,  labelZOffset, false);
        shape.addVertex(minX + cw, minY, labelZOffset,  true);
        shape.addVertex(minX + tw - cw, minY,  labelZOffset, true);
        shape.addVertex(minX + tw, minY, labelZOffset,  false);
        shape.addVertex(minX + tw, minY + ch, labelZOffset,  true);
        shape.addVertex(minX + tw, minY + th- ch, labelZOffset,  true);
        shape.addVertex(minX + tw, minY + th, labelZOffset,  false);
        shape.addVertex(minX + tw - cw, minY + th, labelZOffset,  true);
        shape.addVertex(minX + cw, minY + th, labelZOffset,  true);
        shape.addVertex(minX, minY + th, labelZOffset,  false);
        shape.addVertex(minX, minY + th - ch, labelZOffset,  true);
        shape.closeLastOutline();
    }


    public void setCorner(float corner) {
        if(corner > 1.0f){
            this.corner = 1.0f;
        }
        else if(corner < 0.01f){
            this.corner = 0.0f;
        }
        else{
            this.corner = corner;
        }
        dirty |= DIRTY_SHAPE;
    }
    
    public float getLabelZOffset() {
        return labelZOffset;
    }

    public void setLabelZOffset(float labelZOffset) {
        this.labelZOffset = -labelZOffset;
        dirty |= DIRTY_SHAPE;
    }
    public float getSpacing() {
        return spacing;
    }

    public void setSpacing(float spacing) {
        if(spacing < 0.0f){
            this.spacing = 0.0f;
        }
        else{
            this.spacing = spacing;
        }
        dirty |= DIRTY_SHAPE;
    }
    
    public float[] getButtonColor() {
        return buttonColor;
    }

    public void setButtonColor(float r, float g, float b) {
        this.buttonColor[0] = r;
        this.buttonColor[1] = g;
        this.buttonColor[2] = b;
    }

    public float[] getLabelColor() {
        return labelColor;
    }

    public void setLabelColor(float r, float g, float b) {
        this.labelColor[0] = r;
        this.labelColor[1] = g;
        this.labelColor[2] = b;
    }
    
    public String toString() {
        return "RIButton [" + getWidth() + "x" + getHeight() + ", "
            + getLabel() + "," + "spacing: " + spacing
            + ", " + "corner: " + corner + ", " + "shapeOffset: " + labelZOffset + " ]";
    }
}
