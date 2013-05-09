/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.util.awt;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.Buffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.GLPixelBuffer;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;

/** 
 * AWT {@link GLPixelBuffer} backed by an {@link BufferedImage} of type 
 * {@link BufferedImage#TYPE_INT_ARGB} or {@link BufferedImage#TYPE_INT_RGB}.
 * <p>
 * Implementation uses an array backed  {@link IntBuffer}.
 * </p>
 * <p>
 * {@link AWTGLPixelBuffer} can be produced via {@link AWTGLPixelBufferProvider}'s 
 * {@link AWTGLPixelBufferProvider#allocate(GL, GLPixelAttributes, int, int, int, boolean, int) allocate(..)}. 
 * </p>
 * <p>
 * See {@link AWTGLPixelBuffer#requiresNewBuffer(GL, int, int, int)} for {@link #allowRowStride} details.
 * </p>
 */
public class AWTGLPixelBuffer extends GLPixelBuffer {
    public static final GLPixelAttributes awtPixelAttributesIntRGBA4 = new GLPixelAttributes(4, GL.GL_BGRA, GL.GL_UNSIGNED_BYTE);
    public static final GLPixelAttributes awtPixelAttributesIntRGB3 = new GLPixelAttributes(3, GL.GL_BGRA, GL.GL_UNSIGNED_BYTE);

    /** Allow {@link GL2GL3#GL_PACK_ROW_LENGTH}, or {@link GL2GL3#GL_UNPACK_ROW_LENGTH}. See {@link #requiresNewBuffer(GL, int, int, int)}. */
    public final boolean allowRowStride;
    /** The underlying {@link BufferedImage}. */
    public final BufferedImage image;
    
    /**
     * @param pixelAttributes the desired {@link GLPixelAttributes}
     * @param width in pixels
     * @param height in pixels
     * @param depth in pixels
     * @param pack true for read mode GPU -> CPU, otherwise false for write mode CPU -> GPU
     * @param image the AWT image
     * @param buffer the backing array
     * @param allowRowStride If <code>true</code>, allow row-stride, otherwise not. See {@link #requiresNewBuffer(GL, int, int, int)}.
     */
    public AWTGLPixelBuffer(GLPixelAttributes pixelAttributes, int width, int height, int depth, boolean pack, BufferedImage image, 
                            Buffer buffer, boolean allowRowStride) {
        super(pixelAttributes, width, height, depth, pack, buffer);
        this.allowRowStride = allowRowStride;
        this.image = image;
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * If <code>{@link #allowRowStride} = false</code>,
     * method returns <code>true</code> if the new size &ne; current size.
     * </p> 
     * <p>
     * If <code>{@link #allowRowStride} = true</code>, see {@link AWTGLPixelBufferProvider#AWTGLPixelBufferProvider(boolean)},
     * method returns <code>true</code> only if the new size &gt; current size. Assuming user utilizes the row-stride
     * when dealing w/ the data, i.e. {@link GL2GL3#GL_PACK_ROW_LENGTH}.
     * </p>
     */
    @Override
    public boolean requiresNewBuffer(GL gl, int newWidth, int newHeight, int minByteSize) {
        if( !isValid() ) {
            return true;
        }
        if( allowRowStride && gl.isGL2GL3() ) {
            return width < newWidth || height < newHeight;
        } else {
            return width != newWidth || height != newHeight;
        }
    }
    
    @Override
    public void dispose() {
        image.flush();
        super.dispose();
    }
    
    public StringBuffer toString(StringBuffer sb) {
        sb = super.toString(sb);
        sb.append(", allowRowStride ").append(allowRowStride).append(", image [").append(image.getWidth()).append("x").append(image.getHeight()).append(", ").append(image.toString()).append("]");
        return sb;
    }
    public String toString() {
        return "AWTGLPixelBuffer["+toString(null).toString()+"]";
    }
    
    /**
     * Provider for {@link AWTGLPixelBuffer} instances.
     */
    public static class AWTGLPixelBufferProvider implements GLPixelBufferProvider {    
        /** Allow {@link GL2GL3#GL_PACK_ROW_LENGTH}, or {@link GL2GL3#GL_UNPACK_ROW_LENGTH}. */
        public final boolean allowRowStride;
        
        /**
         * @param allowRowStride If <code>true</code>, allow row-stride, otherwise not. See {@link AWTGLPixelBuffer#requiresNewBuffer(GL, int, int, int)}.
         */
        public AWTGLPixelBufferProvider(boolean allowRowStride) {
            this.allowRowStride = allowRowStride;
        }
        @Override
        public GLPixelAttributes getAttributes(GL gl, int componentCount) {
            return 4 == componentCount ? awtPixelAttributesIntRGBA4 : awtPixelAttributesIntRGB3;
        }
            
        /**
         * {@inheritDoc}
         * <p>
         * Returns an array backed {@link IntBuffer} of size <pre>width*height*{@link Buffers#SIZEOF_INT SIZEOF_INT}</code>.
         * </p>
         */
        @Override
        public AWTGLPixelBuffer allocate(GL gl, GLPixelAttributes pixelAttributes, int width, int height, int depth, boolean pack, int minByteSize) {
            final BufferedImage image = new BufferedImage(width, height, 4 == pixelAttributes.componentCount ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            final int[] readBackIntBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            final Buffer ibuffer = IntBuffer.wrap( readBackIntBuffer );
            return new AWTGLPixelBuffer(pixelAttributes, width, height, depth, pack, image, ibuffer, allowRowStride);
        }
    }
    
    /**
     * Provider for singleton {@link AWTGLPixelBuffer} instances.
     * <p>
     * Provider instance holds the last {@link AWTGLPixelBuffer} instance
     * {@link #allocate(GL, GLPixelAttributes, int, int, int, boolean, int) allocated}.
     * A new {@link #allocate(GL, GLPixelAttributes, int, int, int, boolean, int) allocation} 
     * will return same instance, if a new buffer is not {@link AWTGLPixelBuffer#requiresNewBuffer(GL, int, int, int) required}.
     * The latter is true if size are compatible, hence <code>allowRowStride</code> should be enabled, if possible. 
     * </p>
     */    
    public static class SingleAWTGLPixelBufferProvider extends AWTGLPixelBufferProvider implements SingletonGLPixelBufferProvider {
        private AWTGLPixelBuffer singleRGBA4 = null;
        private AWTGLPixelBuffer singleRGB3 = null;
        
        /**
         * @param allowRowStride If <code>true</code>, allow row-stride, otherwise not. See {@link AWTGLPixelBuffer#requiresNewBuffer(GL, int, int, int)}.
         */
        public SingleAWTGLPixelBufferProvider(boolean allowRowStride) {
            super(allowRowStride);
        }
        
        /**
         * {@inheritDoc}
         * <p>
         * Returns an array backed {@link IntBuffer} of size <pre>width*height*{@link Buffers#SIZEOF_INT SIZEOF_INT}</code>.
         * </p>
         */
        @Override
        public AWTGLPixelBuffer allocate(GL gl, GLPixelAttributes pixelAttributes, int width, int height, int depth, boolean pack, int minByteSize) {
            if( 4 == pixelAttributes.componentCount ) {
                if( null == singleRGBA4 || singleRGBA4.requiresNewBuffer(gl, width, height, minByteSize) ) {
                    singleRGBA4 = allocateImpl(pixelAttributes, width, height, depth, pack, minByteSize); 
                }
                return singleRGBA4;
            } else {
                if( null == singleRGB3 || singleRGB3.requiresNewBuffer(gl, width, height, minByteSize) ) {
                    singleRGB3 = allocateImpl(pixelAttributes, width, height, depth, pack, minByteSize);
                }
                return singleRGB3;
            }
        }
        
        private AWTGLPixelBuffer allocateImpl(GLPixelAttributes pixelAttributes, int width, int height, int depth, boolean pack, int minByteSize) {
            final BufferedImage image = new BufferedImage(width, height, 4 == pixelAttributes.componentCount ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            final int[] readBackIntBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
            final Buffer ibuffer = IntBuffer.wrap( readBackIntBuffer );
            return new AWTGLPixelBuffer(pixelAttributes, width, height, depth, pack, image, ibuffer, allowRowStride);
        }
        
        /** Return the last {@link #allocate(GL, GLPixelAttributes, int, int, int, boolean, int) allocated} {@link AWTGLPixelBuffer} w/ {@link GLPixelAttributes#componentCount}. */ 
        public AWTGLPixelBuffer getSingleBuffer(GLPixelAttributes pixelAttributes) {
            return 4 == pixelAttributes.componentCount ? singleRGBA4 : singleRGB3;
        }
        
        /** 
         * Initializes the single {@link AWTGLPixelBuffer} w/ a given size, if not yet {@link #allocate(GL, GLPixelAttributes, int, int, int, boolean, int) allocated}.
         * @return the newly initialized single {@link AWTGLPixelBuffer}, or null if already allocated.
         */
        public AWTGLPixelBuffer initSingleton(int componentCount, int width, int height, int depth, boolean pack) {
            if( 4 == componentCount ) {
                if( null != singleRGBA4 ) {
                    return null; 
                }
                singleRGBA4 = allocateImpl(AWTGLPixelBuffer.awtPixelAttributesIntRGBA4, width, height, depth, pack, 0);
                return singleRGBA4;
            } else {
                if( null != singleRGB3 ) {
                    return null; 
                }
                singleRGB3 = allocateImpl(AWTGLPixelBuffer.awtPixelAttributesIntRGB3, width, height, depth, pack, 0);
                return singleRGB3;
            }
        }
    }
}