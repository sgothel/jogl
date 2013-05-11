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
package com.jogamp.opengl.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.texture.TextureData;

/** 
 * OpenGL pixel data buffer, allowing user to provide buffers via their {@link GLPixelBufferProvider} implementation.
 * <p>
 * {@link GLPixelBufferProvider} produces a {@link GLPixelBuffer}.
 * </p> 
 * <p>
 * You may use {@link #defaultProvider}.
 * </p>
 */
public class GLPixelBuffer {
    
    /** Allows user to interface with another toolkit to define {@link GLPixelAttributes} and memory buffer to produce {@link TextureData}. */ 
    public static interface GLPixelBufferProvider {
        /** Called first to determine {@link GLPixelAttributes}. */
        GLPixelAttributes getAttributes(GL gl, int componentCount);
        
        /** 
         * Allocates a new {@link GLPixelBuffer} object.
         * <p>
         * Being called to gather the initial {@link GLPixelBuffer},
         * or a new replacement {@link GLPixelBuffer} if {@link GLPixelBuffer#requiresNewBuffer(GL, int, int, int)}.
         * </p>
         * <p>
         * The minimum required {@link Buffer#remaining() remaining} byte size equals to <code>minByteSize</code>, if &gt; 0, 
         * otherwise utilize {@link GLBuffers#sizeof(GL, int[], int, int, int, int, int, boolean)}
         * to calculate it.
         * </p>
         * 
         * @param gl the corresponding current GL context object
         * @param pixelAttributes the desired {@link GLPixelAttributes}
         * @param width in pixels
         * @param height in pixels
         * @param depth in pixels
         * @param pack true for read mode GPU -> CPU, otherwise false for write mode CPU -> GPU
         * @param minByteSize if &gt; 0, the pre-calculated minimum byte-size for the resulting buffer, otherwise ignore.   
         */
        GLPixelBuffer allocate(GL gl, GLPixelAttributes pixelAttributes, int width, int height, int depth, boolean pack, int minByteSize);
    }

    /** Single {@link GLPixelBuffer} provider. */ 
    public static interface SingletonGLPixelBufferProvider extends GLPixelBufferProvider {
        /** Return the last {@link #allocate(GL, GLPixelAttributes, int, int, int, boolean, int) allocated} {@link GLPixelBuffer} w/ {@link GLPixelAttributes#componentCount}. */ 
        GLPixelBuffer getSingleBuffer(GLPixelAttributes pixelAttributes);
        /** 
         * Initializes the single {@link GLPixelBuffer} w/ a given size, if not yet {@link #allocate(GL, GLPixelAttributes, int, int, int, boolean, int) allocated}.
         * @return the newly initialized single {@link GLPixelBuffer}, or null if already allocated.
         */
        GLPixelBuffer initSingleton(int componentCount, int width, int height, int depth, boolean pack);        
    }

    /** 
     * Default {@link GLPixelBufferProvider} utilizing best match for {@link GLPixelAttributes}
     * and {@link GLPixelBufferProvider#allocate(GL, GLPixelAttributes, int, int, int, boolean, int) allocating} a {@link ByteBuffer}.
     */
    public static GLPixelBufferProvider defaultProvider = new GLPixelBufferProvider() {
        
        @Override
        public GLPixelAttributes getAttributes(GL gl, int componentCount) {
            final GLContext ctx = gl.getContext();
            final int dFormat, dType;
            
            if(gl.isGL2GL3() && 3 == componentCount) {
                dFormat = GL.GL_RGB;
                dType   = GL.GL_UNSIGNED_BYTE;            
            } else {
                dFormat = ctx.getDefaultPixelDataFormat();
                dType   = ctx.getDefaultPixelDataType();
            }
            return new GLPixelAttributes(componentCount, dFormat, dType);
        }
        
        /**
         * {@inheritDoc}
         * <p>
         * Returns an NIO {@link ByteBuffer}.
         * </p>
         */
        @Override
        public GLPixelBuffer allocate(GL gl, GLPixelAttributes pixelAttributes, int width, int height, int depth, boolean pack, int minByteSize) {
            if( minByteSize > 0 ) {
                return new GLPixelBuffer(pixelAttributes, width, height, depth, pack, Buffers.newDirectByteBuffer(minByteSize));                
            } else {
                int[] tmp = { 0 };
                final int byteSize = GLBuffers.sizeof(gl, tmp, pixelAttributes.bytesPerPixel, width, height, depth, pack);
                return new GLPixelBuffer(pixelAttributes, width, height, depth, pack, Buffers.newDirectByteBuffer(byteSize));
            }
        }
    };
        
    /** Pixel attributes. */ 
    public static class GLPixelAttributes {
        /** Undefined instance of {@link GLPixelAttributes}, having componentCount:=0, format:=0 and type:= 0. */ 
        public static final GLPixelAttributes UNDEF = new GLPixelAttributes(0, 0, 0);
        
        /** Pixel <i>source</i> component count, i.e. number of meaningful components. */
        public final int componentCount;
        /** The OpenGL pixel data format */
        public final int format;
        /** The OpenGL pixel data type  */
        public final int type;
        /** The OpenGL pixel size in bytes  */
        public final int bytesPerPixel;
        
        /**
         * Deriving {@link #componentCount} via GL <code>dataFormat</code>, i.e. {@link GLBuffers#componentCount(int)} if &gt; 0.
         * @param dataFormat GL data format
         * @param dataType GL data type
         */
        public GLPixelAttributes(int dataFormat, int dataType) {
            this(0 < dataFormat ? GLBuffers.componentCount(dataFormat) : 0, dataFormat, dataType);
        }
        /**
         * Using user specified source {@link #componentCount}. 
         * @param componentCount source component count
         * @param dataFormat GL data format
         * @param dataType GL data type
         */
        public GLPixelAttributes(int componentCount, int dataFormat, int dataType) {
            this.componentCount = componentCount;
            this.format = dataFormat;
            this.type = dataType;
            this.bytesPerPixel = ( 0 < dataFormat && 0 < dataType ) ? GLBuffers.bytesPerPixel(dataFormat, dataType) : 0;
        }
        public String toString() {
            return "PixelAttributes[comp "+componentCount+", fmt 0x"+Integer.toHexString(format)+", type 0x"+Integer.toHexString(type)+", bytesPerPixel "+bytesPerPixel+"]";
        }
    }
    
    /** The {@link GLPixelAttributes}. */
    public final GLPixelAttributes pixelAttributes;
    /** Width in pixels. */
    public final int width;
    /** Height in pixels. */
    public final int height;
    /** Depth in pixels. */
    public final int depth;
    /** Data packing direction. If <code>true</code> for read mode GPU -> CPU, <code>false</code> for write mode CPU -> GPU. */
    public final boolean pack;
    /** Byte size of the buffer. Actually the number of {@link Buffer#remaining()} bytes when passed in ctor. */
    public final int byteSize;
    /** 
     * Buffer holding the pixel data. If {@link #rewind()}, it holds <code>byteSize</code> {@link Buffer#remaining()} bytes. 
     * <p>
     * By default the {@link Buffer} is a {@link ByteBuffer}, due to {@link DefProvider#allocate(GL, GLPixelAttributes, int, int, int, boolean, int)}.
     * However, other {@link GLPixelBufferProvider} may utilize different {@link Buffer} types.
     * </p>
     */   
    public final Buffer buffer;
    /** Buffer element size in bytes. */
    public final int bufferElemSize;
    
    private boolean disposed = false;
    
    public StringBuffer toString(StringBuffer sb) {
        if(null == sb) {
            sb = new StringBuffer();
        }
        sb.append(pixelAttributes).append(", dim ").append(width).append("x").append(height).append("x").append(depth).append(", pack ").append(pack)
        .append(", disposed ").append(disposed).append(", valid ").append(isValid()).append(", buffer[sz [bytes ").append(byteSize).append(", elemSize ").append(bufferElemSize).append(", ").append(buffer).append("]");
        return sb;
    }
    public String toString() {
        return "GLPixelBuffer["+toString(null).toString()+"]";
    }
    
    public GLPixelBuffer(GLPixelAttributes pixelAttributes, int width, int height, int depth, boolean pack, Buffer buffer) {
        this.pixelAttributes = pixelAttributes; 
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.pack = pack;
        this.buffer = buffer;
        this.byteSize = Buffers.remainingBytes(buffer);
        this.bufferElemSize = Buffers.sizeOfBufferElem(buffer);
    }
    
    /** Is not {@link #dispose()} and has {@link #byteSize} &gt; 0. */
    public boolean isValid() {
        return !disposed && 0 < byteSize;
    }
    
    public Buffer rewind() {
        return buffer.rewind();
    }

    /** Returns the byte position of the {@link #buffer}. */
    public int position() {
        return buffer.position() * bufferElemSize;
    }
    
    /** Sets the byte position of the {@link #buffer}. */
    public Buffer position(int bytePos) {
        return buffer.position( bytePos / bufferElemSize );
    }
    
    public Buffer flip() {
        return buffer.flip();        
    }
    
    public Buffer clear() {
        return buffer.clear();        
    }
    
    /** 
     * Returns true, if implementation requires a new buffer based on the new size
     * due to pixel alignment or byte size or if {@link #isValid() invalid}, otherwise false.
     * <p>
     * It is assumed that <code>pixelAttributes</code>, <code>depth</code> and <code>pack</code> stays the same!
     * </p>
     * <p>
     * The minimum required byte size equals to <code>minByteSize</code>, if &gt; 0, 
     * otherwise utilize {@link GLBuffers#sizeof(GL, int[], int, int, int, int, int, boolean) GLBuffers.sizeof(..)}
     * to calculate it.
     * </p>
     * @param gl the corresponding current GL context object
     * @param newWidth new width in pixels
     * @param newHeight new height in pixels
     * @param minByteSize if &gt; 0, the pre-calculated minimum byte-size for the resulting buffer, otherwise ignore.   
     * @see GLPixelBufferProvider#allocate(GL, GLPixelAttributes, int, int, int, boolean, int)
     */
    public boolean requiresNewBuffer(GL gl, int newWidth, int newHeight, int minByteSize) {
        return !isValid() || this.byteSize < minByteSize;
    }
    
    /** Dispose resources. */
    public void dispose() {
        buffer.clear();
    }
}

