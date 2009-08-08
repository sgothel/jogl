/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
 */

package com.sun.opengl.util.glsl.fixedfunc;

import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.*;

import com.sun.opengl.util.glsl.fixedfunc.impl.*;

/**
 * Tool to pipeline GL2ES2 into a fixed function emulation implementing GL2ES1.
 */
public class FixedFuncUtil {
    /**
     * @return If gl is a GL2ES1 and force is false, return the type cast object,
     *         otherwise create a fixed function emulation pipeline with the GL2ES2 impl.
     * @throws GLException if the GL object is neither GL2ES1 nor GL2ES2
     */
    public static final GL2ES1 getFixedFuncImpl(GL gl, boolean force) {
        if(!force && gl.isGL2ES1()) {
            return gl.getGL2ES1();
        } else if(gl.isGL2ES2()) {
            GL2ES2 es2 = gl.getGL2ES2();
            FixedFuncHook hook = new FixedFuncHook(es2);
            FixedFuncImpl impl = new FixedFuncImpl(es2, hook);
            gl.getContext().setGL(impl);
            return impl;
        }
        throw new GLException("GL Object is neither GL2ES1 nor GL2ES2");
    }

    /**
     * @return If gl is a GL2ES1, return the type cast object,
     *         otherwise create a fixed function emulation pipeline with the GL2ES2 impl.
     * @throws GLException if the GL object is neither GL2ES1 nor GL2ES2
     */
    public static final GL2ES1 getFixedFuncImpl(GL gl) {
        return getFixedFuncImpl(gl, false);
    }

    /**
     * Mapping fixed function (client) array indices to 
     * GLSL array attribute names.
     *
     * Useful for uniq mapping of canonical array index names as listed.
     * 
     * @see #mgl_Vertex
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#GL_VERTEX_ARRAY
     * @see #mgl_Normal
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#GL_NORMAL_ARRAY
     * @see #mgl_Color
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#GL_COLOR_ARRAY
     * @see #mgl_MultiTexCoord
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#GL_TEXTURE_COORD_ARRAY
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#glEnableClientState
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#glVertexPointer
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#glColorPointer
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#glNormalPointer
     * @see javax.media.opengl.fixedfunc.GLPointerFunc#glTexCoordPointer
     */
    public static String getPredefinedArrayIndexName(int glArrayIndex) {
        return FixedFuncPipeline.getPredefinedArrayIndexName(glArrayIndex);
    }

    /**
     * String name for
     * @see javax.media.opengl.GL#GL_VERTEX_ARRAY
     */
    public static final String mgl_Vertex = FixedFuncPipeline.mgl_Vertex;

    /**
     * String name for
     * @see javax.media.opengl.GL#GL_NORMAL_ARRAY
     */
    public static final String mgl_Normal = FixedFuncPipeline.mgl_Normal;

    /**
     * String name for
     * @see javax.media.opengl.GL#GL_COLOR_ARRAY
     */
    public static final String mgl_Color = FixedFuncPipeline.mgl_Color;

    /**
     * String name for
     * @see javax.media.opengl.GL#GL_TEXTURE_COORD_ARRAY
     */
    public static final String mgl_MultiTexCoord = FixedFuncPipeline.mgl_MultiTexCoord;
}
