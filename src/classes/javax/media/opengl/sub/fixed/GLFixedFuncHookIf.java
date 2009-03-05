/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
 */

package javax.media.opengl.sub.fixed;

import java.nio.*;

import javax.media.opengl.*;
import javax.media.opengl.sub.*;

/**
 * Fixed function implementation hook interface<p>
 * 
 * An implementation shall implement the below interface methods
 * and pipeline the call to the underlying GL impl. if necessary. <p>
 *
 * An implementation must implement all extended interface methods. <p>
 */
public interface GLFixedFuncHookIf extends GLLightingIf, GLMatrixIf, GLPointerIf {
    public void glDrawArrays(int mode, int first, int count) ;
    public void glDrawElements(int mode, int count, int type, java.nio.Buffer indices) ;
    public void glDrawElements(int mode, int count, int type, long indices_buffer_offset) ;
    public void glActiveTexture(int texture) ;
    public void glEnable(int cap) ;
    public void glDisable(int cap) ;
    public void glCullFace(int faceName) ;
    public void glGetFloatv(int pname, java.nio.FloatBuffer params) ;
    public void glGetFloatv(int pname, float[] params, int params_offset) ;
    public void glGetIntegerv(int pname, IntBuffer params) ;
    public void glGetIntegerv(int pname, int[] params, int params_offset) ;
}

