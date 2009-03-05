/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
 */

package javax.media.opengl.sub.fixed;

import javax.media.opengl.*;
import javax.media.opengl.sub.*;

/**
 * Contains values to handle the fixed implementation.
 */
public class GLFixedFuncUtil {

    /**
     * @return The current GLContext's GL object cast as GLFixedFuncIf
     * @throws GLException is this GL Object is not a GLFixedFuncIf implementation,
     *         or no GLContext is current
     */
    public static final GLFixedFuncIf getCurrentGLFixedFuncIf() throws GLException {
        GLContext curContext = GLContext.getCurrent();
        if (curContext == null) {
            throw new GLException("No OpenGL context current on this thread");
        }
        GL gl = curContext.getGL();
        if(gl instanceof GLFixedFuncIf) {
            return (GLFixedFuncIf) gl;
        }
        throw new GLException("Not a GLFixedFuncIf implementation");
    }

    /**
     * @return true if GL object is a GLFixedFuncIf
     */
    public static final boolean isGLFixedFuncIf(GL gl) {
        return (gl instanceof GLFixedFuncIf) ;
    }

    /**
     * @return The object cast as GLFixedFuncIf
     * @throws GLException is this GL Object is not a GLFixedFuncIf implementation
     */
    public static final GLFixedFuncIf getGLFixedFuncIf(GL gl) {
        if(gl instanceof GLFixedFuncIf) {
            return (GLFixedFuncIf) gl;
        }
        throw new GLException("Not a GLFixedFuncIf implementation");
    }
}

