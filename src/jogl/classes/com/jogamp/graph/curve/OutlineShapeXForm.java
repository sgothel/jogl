package com.jogamp.graph.curve;

import jogamp.graph.geom.plane.AffineTransform;

public class OutlineShapeXForm {
    public final OutlineShape shape;
    private AffineTransform t;

    public OutlineShapeXForm(final OutlineShape shape, final AffineTransform t) {
        this.shape = shape;
        this.t = t;
    }

    public final AffineTransform getTransform() { return t; }
    public final void setTransform(final AffineTransform t) { this.t = t; }
}