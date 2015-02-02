package com.jogamp.opengl.glu;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.util.ImmModeSink;

/**
 * Wrapper for a GLU quadric object.
 */

public interface GLUquadric {
    // enable/disables the Immediate Mode Sink module.
    // This defaults to false for GLUgl2,
    // and is always true for GLUes1.
    public void enableImmModeSink(boolean val);

    public boolean isImmModeSinkEnabled();

    // set Immediate Mode usage.
    // This defaults to false at GLU creation time.
    // If enabled rendering will happen immediately,
    // otherwise rendering will be hold in the ImmModeSink
    // object, to be rendered deferred.
    public void setImmMode(boolean val);

    public boolean getImmMode();

    // creates a new ImmModeSink (VBO Buffers) and
    // returns the old vbo buffer with it's rendering result
    public ImmModeSink replaceImmModeSink();

    // gl may be null, then the GL client states are not disabled
    public void resetImmModeSink(GL gl);
}
