package javax.media.opengl.glu;

import javax.media.opengl.GL;
import com.sun.opengl.util.ImmModeSink;

/**
 * Wrapper for a GLU quadric object.
 */

public interface GLUquadric {
    // creates a new ImmModeSink (VBO Buffers) and
    // returns the old vbo buffer with it's rendering result
    public ImmModeSink replaceVBOBuffer();

    public void setVBOImmediateMode(boolean val);

    // gl may be null, then the GL client states are not disabled
    public void resetVBOBuffer(GL gl);
}
