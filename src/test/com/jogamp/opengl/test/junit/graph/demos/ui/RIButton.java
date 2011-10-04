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

import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.AABBox;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.opengl.test.junit.graph.demos.ui.opengl.UIRegion;

/** GPU based resolution independent Button impl
 */
public abstract class RIButton extends UIShape {
    private float width, height;
    private Label label;
    private float spacing = 4.0f;
    private float corner = 1.0f;
    private float labelZOffset = -0.05f;
    
    private float[] buttonColor = {0.6f, 0.6f, 0.6f};
    private float[] buttonSelectedColor = {0.8f,0.8f,0.8f};
    private float[] labelColor = {1.0f, 1.0f, 1.0f};
    private float[] labelSelectedColor = {0.1f, 0.1f, 0.1f};
 
    
    public RIButton(Factory<? extends Vertex> factory, Font labelFont, String labelText, float width, float height) {
        super(factory);
        
        // FIXME: Determine font size - PMV Matrix relation ?
        // this.label = new Label(factory, labelFont, (int)(height - 2f * spacing), labelText);
        this.label = new Label(factory, labelFont, 10, labelText){
            public void onClick() { }
            public void onPressed() { }
            public void onRelease() { }
        };
        
        this.width = width;
        this.height = height;
    }

    public final float getWidth() { return width; }
    public final float getHeight() { return height; }
    public float getCorner() { return corner; }
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
        float sx = getWidth() / ( 2f*spacing + lbox.getWidth() );
        float sy = getHeight() / ( 2f*spacing + lbox.getHeight() );
        
        setScale(sx, sy, 1);
    }
    
    
    private void createSharpOutline(AABBox lbox) {
        float th = (2f*spacing) + lbox.getHeight();
        float tw = (2f*spacing) + lbox.getWidth();
        
        float minX = lbox.getMinX()-spacing;
        float minY = lbox.getMinY()-spacing;
        float minZ = labelZOffset;
        
        shape.addVertex(minX, minY, minZ,  true);
        shape.addVertex(minX+tw, minY,  minZ, true);
        shape.addVertex(minX+tw, minY + th, minZ,  true);
        shape.addVertex(minX, minY + th, minZ,  true);
        shape.closeLastOutline();
    }
    
    private void createCurvedOutline(AABBox lbox){
        float th = 2.0f*spacing + lbox.getHeight();
        float tw = 2.0f*spacing + lbox.getWidth();
        
        float cw = 0.5f*corner*Math.min(tw, th);
        float ch = 0.5f*corner*Math.min(tw, th);
        
        float minX = lbox.getMinX()-spacing;
        float minY = lbox.getMinY()-spacing;
        float minZ = labelZOffset;
        shape.addVertex(minX, minY + ch, minZ, true);
        shape.addVertex(minX, minY,  minZ, false);
        shape.addVertex(minX + cw, minY, minZ,  true);
        shape.addVertex(minX + tw - cw, minY,  minZ, true);
        shape.addVertex(minX + tw, minY, minZ,  false);
        shape.addVertex(minX + tw, minY + ch, minZ,  true);
        shape.addVertex(minX + tw, minY + th- ch, minZ,  true);
        shape.addVertex(minX + tw, minY + th, minZ,  false);
        shape.addVertex(minX + tw - cw, minY + th, minZ,  true);
        shape.addVertex(minX + cw, minY + th, minZ,  true);
        shape.addVertex(minX, minY + th, minZ,  false);
        shape.addVertex(minX, minY + th - ch, minZ,  true);
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
        this.buttonColor = new float[3];
        this.buttonColor[0] = r;
        this.buttonColor[1] = g;
        this.buttonColor[2] = b;
    }

    public float[] getLabelColor() {
        return labelColor;
    }
    
    private UIRegion buttonRegion = null;
    private UIRegion labelRegion = null;
    private boolean toggle =false;
    private boolean toggleable = false;

    public void render(GL2ES2 gl, RenderState rs, RegionRenderer renderer, int renderModes, int texSize, boolean selection) {
        if(null == buttonRegion) {
            buttonRegion = new UIRegion(this);
            labelRegion = new UIRegion(getLabel());
        }  
        
        gl.glEnable(GL2ES2.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(0.0f, 1f);
        
        float[] bColor = buttonColor;
        if(isPressed() || toggle){
            bColor = buttonSelectedColor;
        }
        if(!selection){
            renderer.setColorStatic(gl, bColor[0], bColor[1], bColor[2]);
        }
        renderer.draw(gl, buttonRegion.getRegion(gl, rs, renderModes), getPosition(), texSize);
        gl.glDisable(GL2ES2.GL_POLYGON_OFFSET_FILL);
        
        float[] lColor = labelColor;
        if(isPressed() || toggle ){
            lColor = labelSelectedColor;
        }
        if(!selection){
            renderer.setColorStatic(gl, lColor[0], lColor[1], lColor[2]);
        }
        renderer.draw(gl, labelRegion.getRegion(gl, rs, renderModes), getPosition(), texSize);
    }
    public void setPressed(boolean b) {
        super.setPressed(b);
        if(isToggleable() && b) {
            toggle = !toggle;
        }
    }
    
    public void setLabelColor(float r, float g, float b) {
        this.labelColor = new float[3];
        this.labelColor[0] = r;
        this.labelColor[1] = g;
        this.labelColor[2] = b;
    }
    
    public void setButtonSelectedColor(float r, float g, float b){
        this.buttonSelectedColor = new float[3];
        this.buttonSelectedColor[0] = r;
        this.buttonSelectedColor[1] = g;
        this.buttonSelectedColor[2] = b;
    }
    
    public void setLabelSelectedColor(float r, float g, float b){
        this.labelSelectedColor = new float[3];
        this.labelSelectedColor[0] = r;
        this.labelSelectedColor[1] = g;
        this.labelSelectedColor[2] = b;
    }

    public boolean isToggleable() {
        return toggleable;
    }
    
    public void setToggleable(boolean toggleable) {
        this.toggleable = toggleable;
    }

    public String toString() {
        return "RIButton [" + getWidth() + "x" + getHeight() + ", "
            + getLabel() + ", " + "spacing: " + spacing
            + ", " + "corner: " + corner + ", " + "shapeOffset: " + labelZOffset + " ]";
    }
}
