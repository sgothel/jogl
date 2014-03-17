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
public class RIButton extends UIShape {
    /** {@value} */
    public static final float DEFAULT_SPACING_X = 0.08f;
    /** {@value} */
    public static final float DEFAULT_SPACING_Y = 0.40f;
    /** {@value} */
    public static final float DEFAULT_CORNER = 1f;

    private float width, height;
    private final Label label;
    private float spacingX = DEFAULT_SPACING_X;
    private float spacingY = DEFAULT_SPACING_Y;
    private float corner = DEFAULT_CORNER;
    private float labelZOffset = -0.05f;

    public RIButton(Factory<? extends Vertex> factory, Font labelFont, String labelText, float width, float height) {
        super(factory);

        final float pixelSize = height * ( 1f - spacingY ) ;
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
    public final float getCorner() { return corner; }
    public final Label getLabel() { return label; }

    public void setDimension(float width, float height) {
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
    public void drawShape(GL2ES2 gl, RegionRenderer renderer, int[] sampleCount) {
        gl.glEnable(GL2ES2.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(0.0f, 1f);
        super.drawShape(gl, renderer, sampleCount);
        gl.glDisable(GL2ES2.GL_POLYGON_OFFSET_FILL);

        label.drawShape(gl, renderer, sampleCount);
    }

    @Override
    protected void createShape(GL2ES2 gl, RegionRenderer renderer) {
        label.clear(gl, renderer);

        final OutlineShape shape = new OutlineShape(renderer.getRenderState().getVertexFactory());
        if(corner == 0.0f) {
            createSharpOutline(shape);
        } else {
            createCurvedOutline(shape);
        }
        box.resize(shape.getBounds());

        // Precompute text-box size .. guessing pixelSize
        final float lPixelSize0 = 10f;
        final float lw = width * ( 1f - spacingX ) ;
        final float lh = height * ( 1f - spacingY ) ;
        final AABBox lbox0 = label.font.getStringBounds(label.text, lPixelSize0);
        final float lsx = lw / lbox0.getWidth();
        final float lsy = lh / lbox0.getHeight();
        final float lPixelSize1 = lsx < lsy ? lPixelSize0 * lsx : lPixelSize0 * lsy;
        if( DRAW_DEBUG_BOX ) {
            System.err.println("RIButton: spacing "+spacingX+", "+spacingY);
            System.err.println("RIButton: bbox "+box);
            System.err.println("RIButton: lbox "+lbox0);
            System.err.println("RIButton: net-text "+lw+" x "+lh);
            System.err.println("RIButton: lsx "+lsx+", lsy "+lsy+": pixelSize "+lPixelSize0+" -> "+lPixelSize1);
        }

        // Setting pixelSize based on actual text-box size
        label.setPixelSize(lPixelSize1);
        label.createShape(gl, renderer);
        final AABBox lbox1 = label.getBounds();
        if( DRAW_DEBUG_BOX ) {
            System.err.printf("RIButton: lbox1 %s .... %s%n", lbox1, this.label.text);
        }

        // Center text .. (share same center w/ button)
        final float[] lctr = lbox1.getCenter();
        final float[] ctr = box.getCenter();
        final float[] ltx = new float[] { ctr[0] - lctr[0], ctr[1] - lctr[1], 0f };
        label.translateShape( ltx[0], ltx[1] );
        lbox1.translate( ltx );

        // rotate center of button/label ..
        label.setRotationOrigin( ctr[0], ctr[1], ctr[2]);
        setRotationOrigin( ctr[0], ctr[1], ctr[2]);

        shapes.add(new OutlineShapeXForm(shape, null));

        if( DRAW_DEBUG_BOX ) {
            System.err.println("XXX.UIShape.RIButton: Added Shape: "+shape+", "+box);
        }
    }
    private void createSharpOutline(OutlineShape shape) {
        final float tw = getWidth();
        final float th = getHeight();

        final float minX = 0;
        final float minY = 0;
        final float minZ = labelZOffset;

        shape.addVertex(minX, minY, minZ,  true);
        shape.addVertex(minX+tw, minY,  minZ, true);
        shape.addVertex(minX+tw, minY + th, minZ,  true);
        shape.addVertex(minX, minY + th, minZ,  true);
        shape.closeLastOutline(true);
    }
    private void createCurvedOutline(OutlineShape shape) {
        final float tw = getWidth();
        final float th = getHeight();
        final float dC = 0.5f*corner*Math.min(tw, th);

        final float minX = 0;
        final float minY = 0;
        final float minZ = labelZOffset;

        shape.addVertex(minX, minY + dC, minZ, true);
        shape.addVertex(minX, minY,  minZ, false);

        shape.addVertex(minX + dC, minY, minZ,  true);

        shape.addVertex(minX + tw - dC, minY,           minZ, true);
        shape.addVertex(minX + tw,      minY,           minZ, false);
        shape.addVertex(minX + tw,      minY + dC,      minZ, true);
        shape.addVertex(minX + tw,      minY + th- dC,  minZ, true);
        shape.addVertex(minX + tw,      minY + th,      minZ, false);
        shape.addVertex(minX + tw - dC, minY + th,      minZ, true);
        shape.addVertex(minX + dC,      minY + th,      minZ, true);
        shape.addVertex(minX,           minY + th,      minZ, false);
        shape.addVertex(minX,           minY + th - dC, minZ, true);

        shape.closeLastOutline(true);
    }

    /** Set corner size, default is {@link #DEFAULT_CORNER} */
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
    public final float getSpacingX() { return spacingX; }
    public final float getSpacingY() { return spacingY; }

    /**
     * In percent of text label
     * @param spacingX spacing in percent on X, default is {@link #DEFAULT_SPACING_X}
     * @param spacingY spacing in percent on Y, default is {@link #DEFAULT_SPACING_Y}
     */
    public void setSpacing(float spacingX, float spacingY) {
        if ( spacingX < 0.0f ) {
            this.spacingX = 0.0f;
        } else if ( spacingX > 1.0f ) {
            this.spacingX = 1.0f;
        } else {
            this.spacingX = spacingX;
        }
        if ( spacingY < 0.0f ) {
            this.spacingY = 0.0f;
        } else if ( spacingY > 1.0f ) {
            this.spacingY = 1.0f;
        } else {
            this.spacingY = spacingY;
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
        return "RIButton [" + translate[0]+getWidth()/2f+" / "+translate[1]+getHeight()/2f+" "+getWidth() + "x" + getHeight() + ", "
                + getLabel() + ", " + "spacing: " + spacingX+"/"+spacingY
                + ", " + "corner: " + corner + ", " + "shapeOffset: " + labelZOffset + ", "+box+" ]";
    }
}
