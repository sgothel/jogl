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
import com.jogamp.graph.curve.OutlineShapeXForm;
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
    /** 20 % to each side default */
    private float spacing = 0.2f;
    private static final float spacingSx = 2f;
    private static final float spacingSy = 2f;
    private float corner = 1.0f;
    private float labelZOffset = -0.05f;

    public RIButton(Factory<? extends Vertex> factory, Font labelFont, String labelText, float width, float height) {
        super(factory);

        final float pixelSize = height * ( 1f - spacingSy*spacing ) ;
        System.err.printf("RIButton: height %f -> pixelSize %f%n", height, pixelSize);
        this.label = new Label(factory, labelFont, pixelSize, labelText);
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
        // Precompute text-box size .. guessing pixelSize
        final float lw = getWidth() * ( 1f - spacingSx*spacing );
        final float lh = getHeight() * ( 1f - spacingSy*spacing );
        final AABBox lbox0 = label.font.getStringBounds(label.text, lh);
        final float lsx = lw / lbox0.getWidth();
        final float lsy = lh / lbox0.getHeight();
        if( DRAW_DEBUG_BOX ) {
            final float sx = getWidth()  / ( ( spacingSx*spacing + 1f ) *  lbox0.getWidth() * lsx );
            final float sy = getHeight() / ( ( spacingSy*spacing + 1f ) *  lbox0.getHeight() * lsy );
            System.err.printf("RIButton: bsize %f x %f, lsize %f x %f, lbox0 %f x %f -> ls %f x %f, bs %f x %f  .... %s%n",
                    getWidth(), getHeight(), lw, lh, lbox0.getWidth(), lbox0.getHeight(), lsx, lsy, sx, sy, this.label.text);
        }

        // Setting pixelSize based on actual text-box size
        final float lPixelSize1 = lh * lsy;
        label.setPixelSize(lPixelSize1);
        label.createShape(gl, renderer);
        final AABBox lbox1 = label.getBounds();
        if( DRAW_DEBUG_BOX ) {
            final float lsx1 = lw / lbox1.getWidth();
            final float lsy1 = lh / lbox1.getHeight();
            System.err.printf("RIButton: ls %f x %f, lbox1 %s .... %s%n",
                    lsx1, lsy1, lbox1, this.label.text);
        }

        final OutlineShape shape = new OutlineShape(renderer.getRenderState().getVertexFactory());
        if(corner == 0.0f) {
            createSharpOutline(shape, lbox1);
        } else {
            createCurvedOutline(shape, lbox1);
        }
        box.resize(shape.getBounds());

        // Center text ..
        final float[] lctr = lbox1.getCenter();
        final float[] ctr = box.getCenter();
        label.translateShape( ctr[0] - lctr[0], ctr[1] - lctr[1] );

        shapes.add(new OutlineShapeXForm(shape, null));
        if( DRAW_DEBUG_BOX ) {
            System.err.println("XXX.UIShape.RIButton: Added Shape: "+shape+", "+box);
        }
    }
    private void createSharpOutline(OutlineShape shape, AABBox lbox) {
        final float tw = getWidth(); // ( spacingSx*spacing + 1f ) * lbox.getWidth();
        final float th = getHeight(); // ( spacingSy*spacing + 1f ) * lbox.getHeight();

        final float minX = lbox.getMinX() - ( spacingSx / 2f * spacing * lbox.getWidth() );
        final float minY = lbox.getMinY() - ( spacingSy / 2f * spacing * lbox.getHeight() );
        final float minZ = labelZOffset;

        shape.addVertex(minX, minY, minZ,  true);
        shape.addVertex(minX+tw, minY,  minZ, true);
        shape.addVertex(minX+tw, minY + th, minZ,  true);
        shape.addVertex(minX, minY + th, minZ,  true);
        shape.closeLastOutline(true);
    }
    private void createCurvedOutline(OutlineShape shape, AABBox lbox){
        final float tw = getWidth(); // ( spacingSx*spacing + 1f ) * lbox.getWidth();
        final float th = getHeight(); // ( spacingSy*spacing + 1f ) * lbox.getHeight();
        final float cw = 0.5f*corner*Math.min(tw, th);
        final float ch = 0.5f*corner*Math.min(tw, th);

        final float minX = lbox.getMinX() - ( spacingSx / 2f * spacing * getWidth() ); // lbox.getWidth() );
        final float minY = lbox.getMinY() - ( spacingSy / 2f * spacing * getHeight() ); // lbox.getHeight() );
        final float minZ = labelZOffset;

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

    /** In percent of text label */
    public void setSpacing(float spacing) {
        if ( spacing < 0.0f ) {
            this.spacing = 0.0f;
        } else if ( spacing > 1.0f ) {
            this.spacing = 1.0f;
        } else {
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

    @Override
    public String toString() {
        return "RIButton [" + getWidth() + "x" + getHeight() + ", "
            + getLabel() + ", " + "spacing: " + spacing
            + ", " + "corner: " + corner + ", " + "shapeOffset: " + labelZOffset + " ]";
    }
}
