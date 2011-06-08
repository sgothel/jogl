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

package com.jogamp.opengl.util;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLException;

/**
 * Utility to safely set and restore the pack and unpack pixel storage mode,
 * regardless of the GLProfile.
 */
public class GLPixelStorageModes {
    private int[] savedGL2GL3Modes = new int[8];
    private int[] savedAlignment = new int[2];
    private boolean saved = false;

    /**
     * Sets the {@link GL2ES2.GL_PACK_ALIGNMENT}. Saves the pixel storage modes if not saved yet.
     */
    public final void setPackAlignment(GL gl, int packAlignment) {
        if(!saved) { save(gl); }
        gl.glPixelStorei(GL2ES2.GL_PACK_ALIGNMENT, packAlignment);        
    }

    /**
     * Sets the {@link GL2ES2.GL_UNPACK_ALIGNMENT}. Saves the pixel storage modes if not saved yet.
     */
    public final void setUnpackAlignment(GL gl, int unpackAlignment) {
        if(!saved) { save(gl); }
        gl.glPixelStorei(GL2ES2.GL_UNPACK_ALIGNMENT, unpackAlignment);        
    }
    
    /**
     * Sets the {@link GL2ES2.GL_PACK_ALIGNMENT} and {@link GL2ES2.GL_UNPACK_ALIGNMENT}. 
     * Saves the pixel storage modes if not saved yet.
     */
    public final void setAlignment(GL gl, int packAlignment, int unpackAlignment) {
        setPackAlignment(gl, packAlignment);
        setUnpackAlignment(gl, unpackAlignment);
    }
    
    private final void save(GL gl) {
        if(gl.isGL2GL3()) {  
            if(gl.isGL2()) {
                gl.getGL2().glPushClientAttrib(GL2.GL_CLIENT_PIXEL_STORE_BIT);
            } else {
                gl.glGetIntegerv(GL2ES2.GL_PACK_ALIGNMENT,     savedAlignment,   0);
                gl.glGetIntegerv(GL2ES2.GL_UNPACK_ALIGNMENT,   savedAlignment,   1);
                gl.glGetIntegerv(GL2GL3.GL_PACK_ROW_LENGTH,    savedGL2GL3Modes, 0);
                gl.glGetIntegerv(GL2GL3.GL_PACK_SKIP_ROWS,     savedGL2GL3Modes, 1);
                gl.glGetIntegerv(GL2GL3.GL_PACK_SKIP_PIXELS,   savedGL2GL3Modes, 2);
                gl.glGetIntegerv(GL2GL3.GL_PACK_SWAP_BYTES,    savedGL2GL3Modes, 3);
                gl.glGetIntegerv(GL2GL3.GL_UNPACK_ROW_LENGTH,  savedGL2GL3Modes, 4);
                gl.glGetIntegerv(GL2GL3.GL_UNPACK_SKIP_ROWS,   savedGL2GL3Modes, 5);
                gl.glGetIntegerv(GL2GL3.GL_UNPACK_SKIP_PIXELS, savedGL2GL3Modes, 6);
                gl.glGetIntegerv(GL2GL3.GL_UNPACK_SWAP_BYTES,  savedGL2GL3Modes, 7);
            }
            gl.glPixelStorei(GL2GL3.GL_PACK_ROW_LENGTH, 0);
            gl.glPixelStorei(GL2GL3.GL_PACK_SKIP_ROWS, 0);
            gl.glPixelStorei(GL2GL3.GL_PACK_SKIP_PIXELS, 0);
            gl.glPixelStorei(GL2GL3.GL_PACK_SWAP_BYTES, 0);
            gl.glPixelStorei(GL2GL3.GL_UNPACK_ROW_LENGTH, 0);
            gl.glPixelStorei(GL2GL3.GL_UNPACK_SKIP_ROWS, 0);
            gl.glPixelStorei(GL2GL3.GL_UNPACK_SKIP_PIXELS, 0);
            gl.glPixelStorei(GL2GL3.GL_UNPACK_SWAP_BYTES, 0);
        } else {
            // embedded deals with pack/unpack alignment only
            gl.glGetIntegerv(GL2ES2.GL_PACK_ALIGNMENT,   savedAlignment, 0);
            gl.glGetIntegerv(GL2ES2.GL_UNPACK_ALIGNMENT, savedAlignment, 1);
        }        
        saved = true;
    }

    /**
     * Restores the pixel storage mode.
     * @throws GLException if not saved via one of the set methods.
     */
    public final void restore(GL gl) throws GLException {
        if(!saved) {
            throw new GLException("pixel storage modes not saved");
        }
        
        if(gl.isGL2GL3()) {  
            if(gl.isGL2()) {
                gl.getGL2().glPopClientAttrib();
            } else {
                gl.glPixelStorei(GL2ES2.GL_PACK_ALIGNMENT,     savedAlignment[0]);
                gl.glPixelStorei(GL2ES2.GL_UNPACK_ALIGNMENT,   savedAlignment[1]);
                gl.glPixelStorei(GL2GL3.GL_PACK_ROW_LENGTH,    savedGL2GL3Modes[0]);
                gl.glPixelStorei(GL2GL3.GL_PACK_SKIP_ROWS,     savedGL2GL3Modes[1]);
                gl.glPixelStorei(GL2GL3.GL_PACK_SKIP_PIXELS,   savedGL2GL3Modes[2]);
                gl.glPixelStorei(GL2GL3.GL_PACK_SWAP_BYTES,    savedGL2GL3Modes[3]);
                gl.glPixelStorei(GL2GL3.GL_UNPACK_ROW_LENGTH,  savedGL2GL3Modes[4]);
                gl.glPixelStorei(GL2GL3.GL_UNPACK_SKIP_ROWS,   savedGL2GL3Modes[5]);
                gl.glPixelStorei(GL2GL3.GL_UNPACK_SKIP_PIXELS, savedGL2GL3Modes[6]);
                gl.glPixelStorei(GL2GL3.GL_UNPACK_SWAP_BYTES,  savedGL2GL3Modes[7]);
            }
        } else {
            // embedded deals with pack/unpack alignment only
            gl.glPixelStorei(GL2ES2.GL_PACK_ALIGNMENT,   savedAlignment[0]);
            gl.glPixelStorei(GL2ES2.GL_UNPACK_ALIGNMENT, savedAlignment[1]);
        }        
        saved = false;
    }      
}


