/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util.texture;

import javax.media.opengl.GL;

public interface TextureSequence {

    public static class TextureFrame {
        public TextureFrame(Texture t) {
            texture = t;
            // stMatrix = new float[4*4];
            // ProjectFloat.makeIdentityf(stMatrix, 0);
        }
        
        public final Texture getTexture() { return texture; }
        // public final float[] getSTMatrix() { return stMatrix; }
        
        public String toString() {
            return "TextureFrame[" + texture + "]";
        }
        protected final Texture texture;
        // protected final float[] stMatrix;
    }

    public interface TexSeqEventListener<T extends TextureSequence> {
        /** 
         * Signaling listeners that {@link TextureSequence#getNextTexture(GL, boolean)} is able to deliver a new frame.
         * @param ts the event source 
         * @param when system time in msec. 
         **/
        public void newFrameAvailable(T ts, long when);
    }
    
    public int getTextureTarget();

    public int getTextureUnit();

    public int[] getTextureMinMagFilter();

    public int[] getTextureWrapST();

    /**
     * Returns the last updated texture. 
     * <p>
     * In case the instance is just initialized, it shall return a <code>TextureFrame</code>
     * object with valid attributes. The texture content may be undefined 
     * until the first call of {@link #getNextTexture(GL, boolean)}.<br>
     * </p> 
     * Not blocking. 
     */
    public TextureFrame getLastTexture();

    /**
     * Returns the next texture to be rendered. 
     * <p>
     * Implementation shall block until next frame is available if <code>blocking</code> is <code>true</code>,
     * otherwise it shall return the last frame in case a new frame is not available.
     * </p>
     * <p>
     * Shall return <code>null</code> in case <i>no</i> frame is available.
     * </p>
     */
    public TextureFrame getNextTexture(GL gl, boolean blocking);
}