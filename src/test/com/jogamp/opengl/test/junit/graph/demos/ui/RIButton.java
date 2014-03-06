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

import jogamp.graph.geom.plane.AffineTransform;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.opengl.math.geom.AABBox;

/**
 * GPU based resolution independent Button impl
 */
public abstract class RIButton extends UIShape {
    private float width, height;
    private final Label label;
    private float spacing = 4.0f;
    private float corner = 1.0f;
    private float labelZOffset = -0.05f;

    public RIButton(Factory<? extends Vertex> factory, Font labelFont, String labelText, float width, float height) {
        super(factory);

        // FIXME: Determine font size - PMV Matrix relation ?
        // this.label = new Label(factory, labelFont, (int)(height - 2f * spacing), labelText);
        this.label = new Label(factory, labelFont, 10, labelText);
        this.label.setSelectedColor(this.color[0], this.color[1], this.color[2]);
        this.label.setColor(0.9f, 0.9f, 0.9f);
        this.label.setSelectedColor(1f, 1f, 1f);

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
        dirty |= DIRTY_SHAPE | DIRTY_REGION;
    }

    @Override
    protected void clearImpl(GL2ES2 gl, RegionRenderer renderer) {
        label.clear(gl, renderer);
    }

    @Override
    protected void destroyImpl(GL2ES2 gl, RegionRenderer renderer) {
        label.destroy(gl, renderer);
    }

    @Override
    public void drawShape(GL2ES2 gl, RegionRenderer renderer, int[] sampleCount, boolean select) {
        gl.glEnable(GL2ES2.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(0.0f, 1f);
        super.drawShape(gl, renderer, sampleCount, select);
        gl.glDisable(GL2ES2.GL_POLYGON_OFFSET_FILL);

        label.drawShape(gl, renderer, sampleCount, select);
    }

    @Override
    protected void createShape(GL2ES2 gl, RegionRenderer renderer) {
        label.createShape(gl, renderer);
        box.resize(label.getBounds());

        final float sx = getWidth() / ( 2f*spacing + box.getWidth() );
        final float sy = getHeight() / ( 2f*spacing + box.getHeight() );
        scale(sx, sy, 1);

        final OutlineShape shape = new OutlineShape(renderer.getRenderState().getVertexFactory());
        if(corner == 0.0f) {
            createSharpOutline(shape, box);
        } else {
            createCurvedOutline(shape, box);
        }
        box.resize(shape.getBounds());
        shapes.add(new TransformedShape(shape, new AffineTransform(renderer.getRenderState().getVertexFactory())));
        System.err.println("XXX.UIShape.RIButton: Added Shape: "+shape+", "+box);
    }
    private void createSharpOutline(OutlineShape shape, AABBox lbox) {
        float th = (2f*spacing) + lbox.getHeight();
        float tw = (2f*spacing) + lbox.getWidth();

        float minX = lbox.getMinX()-spacing;
        float minY = lbox.getMinY()-spacing;
        float minZ = labelZOffset;

        shape.addVertex(minX, minY, minZ,  true);
        shape.addVertex(minX+tw, minY,  minZ, true);
        shape.addVertex(minX+tw, minY + th, minZ,  true);
        shape.addVertex(minX, minY + th, minZ,  true);
        shape.closeLastOutline(true);
    }
    private void createCurvedOutline(OutlineShape shape, AABBox lbox){
        final float th = 2.0f*spacing + lbox.getHeight();
        final float tw = 2.0f*spacing + lbox.getWidth();
        final float cw = 0.5f*corner*Math.min(tw, th);
        final float ch = 0.5f*corner*Math.min(tw, th);

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
        shape.closeLastOutline(true);
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
        dirty |= DIRTY_SHAPE | DIRTY_REGION;
    }

    public float getLabelZOffset() {
        return labelZOffset;
    }

    public void setLabelZOffset(float labelZOffset) {
        this.labelZOffset = -labelZOffset;
        dirty |= DIRTY_SHAPE | DIRTY_REGION;
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
        dirty |= DIRTY_SHAPE | DIRTY_REGION;
    }

    public float[] getLabelColor() {
        return label.getColor();
    }

    public void setLabelColor(float r, float g, float b) {
        label.setColor(r, g, b);
    }

    public void setLabelSelectedColor(float r, float g, float b){
        label.setSelectedColor(r, g, b);
    }

    public String toString() {
        return "RIButton [" + getWidth() + "x" + getHeight() + ", "
            + getLabel() + ", " + "spacing: " + spacing
            + ", " + "corner: " + corner + ", " + "shapeOffset: " + labelZOffset + " ]";
    }
}
