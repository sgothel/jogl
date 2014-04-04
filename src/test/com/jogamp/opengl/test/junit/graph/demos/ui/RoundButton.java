package com.jogamp.opengl.test.junit.graph.demos.ui;

import javax.media.opengl.GL2ES2;

import jogamp.graph.geom.plane.AffineTransform;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;

public abstract class RoundButton extends UIShape {

    /** {@value} */
    public static final float DEFAULT_CORNER = 1f;
    protected float width;
    protected float height;
    protected float corner = DEFAULT_CORNER;
    protected final AffineTransform tempT1 = new AffineTransform();
    protected final AffineTransform tempT2 = new AffineTransform();

    protected RoundButton(final Factory<? extends Vertex> factory, final int renderModes, final float width, final float height) {
        super(factory, renderModes);
        this.width = width;
        this.height = height;
    }

    @Override
    protected void clearImpl(GL2ES2 gl, RegionRenderer renderer) {
    }

    @Override
    protected void destroyImpl(GL2ES2 gl, RegionRenderer renderer) {
    }

    public final float getWidth() { return width; }

    public final float getHeight() { return height; }

    public final float getCorner() { return corner; }

    public void setDimension(float width, float height) {
        this.width = width;
        this.height = height;
        markShapeDirty();
    }

    protected void createSharpOutline(final OutlineShape shape, final float zOffset) {
        final float tw = getWidth();
        final float th = getHeight();

        final float minX = 0;
        final float minY = 0;
        final float minZ = zOffset;

        shape.addVertex(minX, minY, minZ,  true);
        shape.addVertex(minX+tw, minY,  minZ, true);
        shape.addVertex(minX+tw, minY + th, minZ,  true);
        shape.addVertex(minX, minY + th, minZ,  true);
        shape.closeLastOutline(true);
    }

    protected void createCurvedOutline(final OutlineShape shape, final float zOffset) {
        final float tw = getWidth();
        final float th = getHeight();
        final float dC = 0.5f*corner*Math.min(tw, th);

        final float minX = 0;
        final float minY = 0;
        final float minZ = zOffset;

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
        markShapeDirty();
    }

    @Override
    public String getSubString() {
        return super.getSubString()+", dim "+getWidth() + "x" + getHeight() + ", corner " + corner;
    }
}
