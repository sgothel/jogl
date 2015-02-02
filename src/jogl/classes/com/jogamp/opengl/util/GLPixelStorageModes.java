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

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLException;

/**
 * Utility to safely set and restore the PACK and UNPACK pixel storage mode,
 * regardless of the GLProfile.
 * <p>
 * PACK for GPU to CPU transfers, e.g. {@link GL#glReadPixels(int, int, int, int, int, int, java.nio.Buffer) ReadPixels}, etc.
 * </p>
 * <p>
 * UNPACK for CPU o GPU transfers, e.g. {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, long) TexImage2D}, etc
 * </p>
 */
public class GLPixelStorageModes {
    private final int[] cachePack = new int[8];
    private final int[] cacheUnpack = new int[8];
    private boolean savedPack = false;
    private boolean savedUnpack = false;

    /** Create instance w/o {@link #saveAll(GL)} */
    public GLPixelStorageModes() {}

    /** Create instance w/ {@link #saveAll(GL)} */
    public GLPixelStorageModes(final GL gl) { saveAll(gl); }

    /**
     * Sets the {@link GL#GL_PACK_ALIGNMENT}.
     * <p>
     * Saves the PACK pixel storage modes and {@link #resetPack(GL) resets} them if not saved yet, see {@link #savePack(GL)}.
     * </p>
     */
    public final void setPackAlignment(final GL gl, final int packAlignment) {
        savePack(gl);
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, packAlignment);
    }

    /**
     * Sets the {@link GL#GL_UNPACK_ALIGNMENT}.
     * <p>
     * Saves the UNPACK pixel storage modes and {@link #resetUnpack(GL) resets} them if not saved yet, see {@link #saveUnpack(GL)}.
     * </p>
     */
    public final void setUnpackAlignment(final GL gl, final int unpackAlignment) {
        saveUnpack(gl);
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, unpackAlignment);
    }

    /**
     * Sets the {@link GL#GL_PACK_ALIGNMENT} and {@link GL#GL_UNPACK_ALIGNMENT}.
     * <p>
     * Saves the PACK and UNPACK pixel storage modes and resets them if not saved yet, see {@link #saveAll(GL)}.
     * </p>
     */
    public final void setAlignment(final GL gl, final int packAlignment, final int unpackAlignment) {
        setPackAlignment(gl, packAlignment);
        setUnpackAlignment(gl, unpackAlignment);
    }

    /**
     * Sets the {@link GL2ES3#GL_PACK_ROW_LENGTH}.
     * <p>
     * Saves the PACK pixel storage modes and {@link #resetPack(GL) resets} them if not saved yet, see {@link #savePack(GL)}.
     * </p>
     */
    public final void setPackRowLength(final GL2ES3 gl, final int packRowLength) {
        savePack(gl);
        gl.glPixelStorei(GL2ES3.GL_PACK_ROW_LENGTH, packRowLength);
    }

    /**
     * Sets the {@link GL2ES2#GL_UNPACK_ROW_LENGTH}.
     * <p>
     * Saves the UNPACK pixel storage modes and {@link #resetUnpack(GL) resets} them if not saved yet, see {@link #saveUnpack(GL)}.
     * </p>
     */
    public final void setUnpackRowLength(final GL2ES3 gl, final int unpackRowLength) {
        saveUnpack(gl);
        gl.glPixelStorei(GL2ES2.GL_UNPACK_ROW_LENGTH, unpackRowLength);
    }

    /**
     * Sets the {@link GL2ES3#GL_PACK_ROW_LENGTH} and {@link GL2ES2#GL_UNPACK_ROW_LENGTH} if {@link GL#isGL2ES3()}.
     * <p>
     * Saves the PACK and UNPACK pixel storage modes and resets them if not saved yet, see {@link #saveAll(GL)}.
     * </p>
     */
    public final void setRowLength(final GL2ES3 gl, final int packRowLength, final int unpackRowLength) {
        setPackRowLength(gl, packRowLength);
        setUnpackRowLength(gl, unpackRowLength);
    }

    /**
     * Saves PACK and UNPACK pixel storage modes and {@link #resetAll(GL) resets} them,
     * i.e. issues {@link #savePack(GL)} and {@link #saveUnpack(GL)}.
     * <p>
     * Operation is skipped, if the modes were already saved.
     * </p>
     * <p>
     * Restore via {@link #restore(GL)}
     * </p>
     */
    public final void saveAll(final GL gl) {
        savePack(gl);
        saveUnpack(gl);
    }

    /**
     * Resets PACK and UNPACK pixel storage modes to their default value,
     * i.e. issues {@link #resetPack(GL)} and {@link #resetUnpack(GL)}.
     */
    public final void resetAll(final GL gl) {
        resetPack(gl);
        resetUnpack(gl);
    }

    /**
     * Restores PACK and UNPACK pixel storage mode previously saved w/ {@link #saveAll(GL)}
     * or {@link #savePack(GL)} and {@link #saveUnpack(GL)}.
     * @throws GLException if neither PACK nor UNPACK modes were saved.
     */
    public final void restore(final GL gl) throws GLException {
        if(!savedPack && !savedUnpack) {
            throw new GLException("Neither PACK nor UNPACK pixel storage modes were saved");
        }
        if( savedPack ) {
            restorePack(gl);
            savedPack = false;
        }
        if( savedUnpack ) {
            restoreUnpack(gl);
            savedUnpack = false;
        }
    }

    /**
     * Resets PACK pixel storage modes to their default value.
     */
    public final void resetPack(final GL gl) {
        // Compared w/ ES2, ES3 and GL3-core spec
        gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 4);                            // es2, es3, gl3
        if( gl.isGL2ES3() ) {
            gl.glPixelStorei(GL2ES3.GL_PACK_ROW_LENGTH, 0);                   // es3, gl3
            gl.glPixelStorei(GL2ES3.GL_PACK_SKIP_ROWS, 0);                    // es3, gl3
            gl.glPixelStorei(GL2ES3.GL_PACK_SKIP_PIXELS, 0);                  // es3, gl3
            if( gl.isGL2GL3() ) {
                gl.glPixelStorei(GL2GL3.GL_PACK_SWAP_BYTES,     GL.GL_FALSE); // gl3
                gl.glPixelStorei(GL2GL3.GL_PACK_LSB_FIRST,      GL.GL_FALSE); // gl3
                if( gl.getContext().getGLVersionNumber().compareTo(GLContext.Version1_2) >= 0 ) {
                    gl.glPixelStorei(GL2GL3.GL_PACK_IMAGE_HEIGHT,   0);       // gl3, GL_VERSION_1_2
                    gl.glPixelStorei(GL2GL3.GL_PACK_SKIP_IMAGES,    0);       // gl3, GL_VERSION_1_2
                }
            }
        }
    }
    /**
     * Saves PACK pixel storage modes and {@link #resetPack(GL) resets} them.
     * <p>
     * Operation is skipped, if the modes were already saved.
     * </p>
     * <p>
     * Restore via {@link #restore(GL)}
     * </p>
     */
    public final void savePack(final GL gl) {
        if(savedPack) {
            return;
        }
        if( gl.isGL2() ) {
            // See GLStateTracker.pushAttrib(GL2.GL_CLIENT_PIXEL_STORE_BIT)
            gl.getGL2().glPushClientAttrib(GL2.GL_CLIENT_PIXEL_STORE_BIT);
        } else {
            // ES1 or ES2 deals with pack/unpack alignment only
            gl.glGetIntegerv(GL.GL_PACK_ALIGNMENT,   cachePack, 0);
            if( gl.isGL2ES3() ) {
                gl.glGetIntegerv(GL2ES3.GL_PACK_ROW_LENGTH,     cachePack, 1);
                gl.glGetIntegerv(GL2ES3.GL_PACK_SKIP_ROWS,      cachePack, 2);
                gl.glGetIntegerv(GL2ES3.GL_PACK_SKIP_PIXELS,    cachePack, 3);
                if( gl.isGL2GL3() ) {
                    gl.glGetIntegerv(GL2GL3.GL_PACK_SWAP_BYTES,    cachePack, 4);
                    gl.glGetIntegerv(GL2GL3.GL_PACK_LSB_FIRST,     cachePack, 5);
                    gl.glGetIntegerv(GL2GL3.GL_PACK_IMAGE_HEIGHT,  cachePack, 6);
                    gl.glGetIntegerv(GL2GL3.GL_PACK_SKIP_IMAGES,   cachePack, 7);
                }
            }
        }
        savedPack = true;
        resetPack(gl);
    }
    private final void restorePack(final GL gl) {
        if( gl.isGL2() ) {
            gl.getGL2().glPopClientAttrib();
        } else {
            gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, cachePack[0]);
            if( gl.isGL2ES3() ) {
                gl.glPixelStorei(GL2ES3.GL_PACK_ROW_LENGTH, cachePack[1]);
                gl.glPixelStorei(GL2ES3.GL_PACK_SKIP_ROWS, cachePack[2]);
                gl.glPixelStorei(GL2ES3.GL_PACK_SKIP_PIXELS, cachePack[3]);
                if( gl.isGL2GL3() ) {
                    gl.glPixelStorei(GL2GL3.GL_PACK_SWAP_BYTES,     cachePack[4]);
                    gl.glPixelStorei(GL2GL3.GL_PACK_LSB_FIRST,      cachePack[5]);
                    gl.glPixelStorei(GL2GL3.GL_PACK_IMAGE_HEIGHT,   cachePack[6]);
                    gl.glPixelStorei(GL2GL3.GL_PACK_SKIP_IMAGES,    cachePack[7]);
                }
            }
        }
    }

    /**
     * Resets UNPACK pixel storage modes to their default value.
     */
    public final void resetUnpack(final GL gl) {
        // Compared w/ ES2, ES3 and GL3-core spec
        gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 4);                          // es2, es3, gl3
        if( gl.isGL2ES3() ) {
            gl.glPixelStorei(GL2ES2.GL_UNPACK_ROW_LENGTH, 0);                 // es3, gl3
            gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_ROWS, 0);                  // es3, gl3
            gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_PIXELS, 0);                // es3, gl3
            if( gl.isGL2GL3() ) {
                if( gl.getContext().getGLVersionNumber().compareTo(GLContext.Version1_2) >= 0 ) {
                    gl.glPixelStorei(GL2ES3.GL_UNPACK_IMAGE_HEIGHT, 0);       // es3, gl3, GL_VERSION_1_2
                    gl.glPixelStorei(GL2ES3.GL_UNPACK_SKIP_IMAGES,  0);       // es3, gl3, GL_VERSION_1_2
                }
                gl.glPixelStorei(GL2GL3.GL_UNPACK_SWAP_BYTES,   GL.GL_FALSE); // gl3
                gl.glPixelStorei(GL2GL3.GL_UNPACK_LSB_FIRST,    GL.GL_FALSE); // gl3
            } else {
                gl.glPixelStorei(GL2ES3.GL_UNPACK_IMAGE_HEIGHT, 0);           // es3, gl3, GL_VERSION_1_2
                gl.glPixelStorei(GL2ES3.GL_UNPACK_SKIP_IMAGES,  0);           // es3, gl3, GL_VERSION_1_2
            }
        }
    }
    /**
     * Saves UNPACK pixel storage modes and {@link #resetUnpack(GL) resets} them.
     * <p>
     * Operation is skipped, if the modes were already saved.
     * </p>
     * <p>
     * Restore via {@link #restore(GL)}
     * </p>
     */
    public final void saveUnpack(final GL gl) {
        if(savedUnpack) {
            return;
        }
        if( gl.isGL2() ) {
            // See GLStateTracker.pushAttrib(GL2.GL_CLIENT_PIXEL_STORE_BIT)
            gl.getGL2().glPushClientAttrib(GL2.GL_CLIENT_PIXEL_STORE_BIT);
        } else {
            // ES1 or ES2 deals with pack/unpack alignment only
            gl.glGetIntegerv(GL.GL_UNPACK_ALIGNMENT, cacheUnpack, 0);
            if( gl.isGL2ES3() ) {
                gl.glGetIntegerv(GL2ES2.GL_UNPACK_ROW_LENGTH,   cacheUnpack, 1);
                gl.glGetIntegerv(GL2ES2.GL_UNPACK_SKIP_ROWS,    cacheUnpack, 2);
                gl.glGetIntegerv(GL2ES2.GL_UNPACK_SKIP_PIXELS,  cacheUnpack, 3);
                gl.glGetIntegerv(GL2ES3.GL_UNPACK_IMAGE_HEIGHT, cacheUnpack, 4);
                gl.glGetIntegerv(GL2ES3.GL_UNPACK_SKIP_IMAGES,  cacheUnpack, 5);
                if( gl.isGL2GL3() ) {
                    gl.glGetIntegerv(GL2GL3.GL_UNPACK_SWAP_BYTES,  cacheUnpack, 6);
                    gl.glGetIntegerv(GL2GL3.GL_UNPACK_LSB_FIRST,   cacheUnpack, 7);
                }
            }
        }
        savedUnpack = true;
        resetUnpack(gl);
    }
    private final void restoreUnpack(final GL gl) {
        if( gl.isGL2() ) {
            gl.getGL2().glPopClientAttrib();
        } else {
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, cacheUnpack[0]);
            if( gl.isGL2ES3() ) {
                gl.glPixelStorei(GL2ES2.GL_UNPACK_ROW_LENGTH, cacheUnpack[1]);
                gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_ROWS, cacheUnpack[2]);
                gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_PIXELS, cacheUnpack[3]);
                gl.glPixelStorei(GL2ES3.GL_UNPACK_IMAGE_HEIGHT, cacheUnpack[4]);
                gl.glPixelStorei(GL2ES3.GL_UNPACK_SKIP_IMAGES,  cacheUnpack[5]);
                if( gl.isGL2GL3() ) {
                    gl.glPixelStorei(GL2GL3.GL_UNPACK_SWAP_BYTES,   cacheUnpack[6]);
                    gl.glPixelStorei(GL2GL3.GL_UNPACK_LSB_FIRST,    cacheUnpack[7]);
                }
            }
        }
    }
}


