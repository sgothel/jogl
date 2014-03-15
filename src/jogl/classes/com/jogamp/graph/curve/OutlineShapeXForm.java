package com.jogamp.graph.curve;

import jogamp.graph.geom.plane.AffineTransform;

public class OutlineShapeXForm {
    public final OutlineShape shape;
    public final AffineTransform t;

    public OutlineShapeXForm(final OutlineShape shape, final AffineTransform t) {
        this.shape = shape;
        this.t = t;
    }
}