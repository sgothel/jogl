/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
 */

package javax.media.opengl.sub.fixed;

import javax.media.opengl.*;
import javax.media.opengl.sub.*;

import com.sun.nwi.impl.NWReflection;
import java.lang.reflect.*;

/**
 * Tool to pipeline GL2ES2 into a fixed function emulation,
 * implementing GL2ES1.
 * The implementation is retrieved by reflection.
 */
public class GLFixedFuncUtil {
    static final Constructor fFuncHookCstr;
    static final Constructor fFuncImplCstr;

    static {
        if(NWReflection.isClassAvailable("com.sun.opengl.util.glsl.fixed.FixedFuncHook") &&
           NWReflection.isClassAvailable("com.sun.opengl.util.glsl.fixed.FixedFuncImpl")) {
            Class argsHook[] = { javax.media.opengl.GL2ES2.class };
            Class argsImpl[] = { javax.media.opengl.GL2ES2.class, NWReflection.getClass("com.sun.opengl.util.glsl.fixed.FixedFuncHook") };
            fFuncHookCstr = NWReflection.getConstructor("com.sun.opengl.util.glsl.fixed.FixedFuncHook", argsHook);
            fFuncImplCstr = NWReflection.getConstructor("com.sun.opengl.util.glsl.fixed.FixedFuncImpl", argsImpl);
        } else {
            fFuncHookCstr=null;
            fFuncImplCstr=null;
        }
    }

    /**
     * @return If gl is a GL2ES1, return the type cast object,
     *         otherwise create a FixedFuncImpl pipeline with the GL2ES2 impl.
     * @throws GLException If this GL Object is neither GL2ES1 nor GL2ES2
     */
    public static final GL2ES1 getFixedFuncImpl(GL gl) {
        if(gl instanceof GL2ES1) {
            return (GL2ES1)gl;
        } else if(gl instanceof GL2ES2) {
            if(null!=fFuncImplCstr) {
                try {
                    GL2ES2 es2 = (GL2ES2)gl;
                    Object fFuncHook = fFuncHookCstr.newInstance( new Object[] { es2 } );
                    GL2ES1 fFuncImpl = (GL2ES1) fFuncImplCstr.newInstance( new Object[] { es2, fFuncHook } );
                    gl.getContext().setGL(fFuncImpl);
                    return fFuncImpl;
                } catch (Exception e) {
                    throw new GLException(e);
                }
            } else {
                throw new GLException("GL Object is GL2ES2, but no fixed function impl. available");
            }
        }
        throw new GLException("GL Object is neither GL2ES1 nor GL2ES2");
    }
}

