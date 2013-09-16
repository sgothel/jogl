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
package com.jogamp.nativewindow.awt;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.SampleModel;
import java.awt.image.SinglePixelPackedSampleModel;
import java.awt.image.WritableRaster;
import java.nio.IntBuffer;

import com.jogamp.common.nio.Buffers;

/**
 * {@link DataBuffer} specialization using NIO direct buffer of type {@link DataBuffer#TYPE_INT} as storage.
 */
public final class DirectDataBufferInt extends DataBuffer {
    
    public static class DirectWritableRaster extends WritableRaster {
        protected DirectWritableRaster(SampleModel sampleModel, DirectDataBufferInt dataBuffer, Point origin) {
            super(sampleModel, dataBuffer, origin);
        }
    }
    
    /**
     * Creates a {@link BufferedImage} using an RGB[A] {@link DirectColorModel} 
     * with an {@link DirectWritableRaster} utilizing a {@link DirectDataBufferInt} as storage.
     *  
     * @param width
     * @param height
     * @param numComponents 3 or 4 components, i.e. {@link BufferedImage#TYPE_INT_RGB} or {@link BufferedImage#TYPE_INT_RGBA}.
     * @param location origin, if <code>null</code> 0/0 is assumed.
     * @return
     */
    public static BufferedImage createBufferedImage(int width, int height, int numComponents, Point location) {
        final int[] bandOffsets = new int[numComponents];
        for (int i=0; i < numComponents; i++) {
            bandOffsets[i] = i;
        }
        final int bpp, rmask, gmask, bmask, amask;
        final ColorModel cm;
        if( 4 == numComponents ) {
            bpp = 32;
            rmask = 0x00ff0000;
            gmask = 0x0000ff00;
            bmask = 0x000000ff;
            amask = 0xff000000;
            cm = ColorModel.getRGBdefault();
        } else if( 3 == numComponents ) {
            bpp = 24;
            rmask = 0x00ff0000;
            gmask = 0x0000ff00;
            bmask = 0x000000ff;
            amask = 0x0;
            cm = new DirectColorModel(bpp, rmask, gmask, bmask, amask);
        } else {
            throw new IllegalArgumentException("numComponents must be [3..4], has "+numComponents);
        }
        final int[] bandMasks;
        if ( 0 != amask ) {
            bandMasks = new int[4];
            bandMasks[3] = amask;
        }
        else {
            bandMasks = new int[3];
        }
        bandMasks[0] = rmask;
        bandMasks[1] = gmask;
        bandMasks[2] = bmask;
        
        final DirectDataBufferInt dataBuffer = new DirectDataBufferInt(width*height);
        if( null == location ) {
            location = new Point(0,0);
        }
        final SinglePixelPackedSampleModel sppsm = new SinglePixelPackedSampleModel(dataBuffer.getDataType(), 
                    width, height, width /* scanLineStride */, bandMasks);
        // IntegerComponentRasters must haveinteger DataBuffers:
        //    final WritableRaster raster = new IntegerInterleavedRaster(sppsm, dataBuffer, location);
        // Not public:
        //    final WritableRaster raster = new SunWritableRaster(sppsm, dataBuffer, location);
        final WritableRaster raster = new DirectWritableRaster(sppsm, dataBuffer, location);
        
        return new BufferedImage (cm, raster, false /* isRasterPremultiplied */, null /* properties */);
    }
    
    /** Default data bank. */
    private IntBuffer data;

    /** All data banks */
    private IntBuffer bankdata[];
    
    /**
     * Constructs an nio integer-based {@link DataBuffer} with a single bank 
     * and the specified size.
     *
     * @param size The size of the {@link DataBuffer}.
     */
    public DirectDataBufferInt(int size) {
        super(TYPE_INT, size);
        data = Buffers.newDirectIntBuffer(size);
        bankdata = new IntBuffer[1];
        bankdata[0] = data;
    }

    /**
     * Constructs an nio integer-based {@link DataBuffer} with the specified number of
     * banks, all of which are the specified size.
     *
     * @param size The size of the banks in the {@link DataBuffer}.
     * @param numBanks The number of banks in the a{@link DataBuffer}.
     */
    public DirectDataBufferInt(int size, int numBanks) {
        super(TYPE_INT,size,numBanks);
        bankdata = new IntBuffer[numBanks];
        for (int i= 0; i < numBanks; i++) {
            bankdata[i] = Buffers.newDirectIntBuffer(size);
        }
        data = bankdata[0];
    }

    /**
     * Constructs an nio integer-based {@link DataBuffer} with a single bank using the
     * specified array.
     * <p>
     * Only the first <code>size</code> elements should be used by accessors of
     * this {@link DataBuffer}. <code>dataArray</code> must be large enough to
     * hold <code>size</code> elements.
     * </p>
     *
     * @param dataArray The integer array for the {@link DataBuffer}.
     * @param size The size of the {@link DataBuffer} bank.
     */
    public DirectDataBufferInt(IntBuffer dataArray, int size) {
        super(TYPE_INT,size);
        data = dataArray;
        bankdata = new IntBuffer[1];
        bankdata[0] = data;
    }

    /**
     * Returns the default (first) int data array in {@link DataBuffer}.
     *    
     * @return The first integer data array.
     */
    public IntBuffer getData() {
        return data;
    }

    /** 
     * Returns the data array for the specified bank.    
     *   
     * @param bank The bank whose data array you want to get. 
     * @return The data array for the specified bank.
     */
    public IntBuffer getData(int bank) {
        return bankdata[bank];
    }

    /**
     * Returns the requested data array element from the first (default) bank.
     * 
     * @param i The data array element you want to get.
     * @return The requested data array element as an integer.
     * @see #setElem(int, int)
     * @see #setElem(int, int, int)
     */
    public int getElem(int i) {
        return data.get(i+offset);
    }

    /**
     * Returns the requested data array element from the specified bank.
     * 
     * @param bank The bank from which you want to get a data array element.
     * @param i The data array element you want to get.
     * @return The requested data array element as an integer.
     * @see #setElem(int, int)
     * @see #setElem(int, int, int)
     */
    public int getElem(int bank, int i) {
        return bankdata[bank].get(i+offsets[bank]);
    }

    /**
     * Sets the requested data array element in the first (default) bank
     * to the specified value.
     *
     * @param i The data array element you want to set.
     * @param val The integer value to which you want to set the data array element.
     * @see #getElem(int)
     * @see #getElem(int, int)
     */
    public void setElem(int i, int val) {
        data.put(i+offset, val);
    }

    /**
     * Sets the requested data array element in the specified bank
     * to the integer value <code>i</code>.
     * @param bank The bank in which you want to set the data array element.
     * @param i The data array element you want to set.
     * @param val The integer value to which you want to set the specified data array element.
     * @see #getElem(int)
     * @see #getElem(int, int)
     */
    public void setElem(int bank, int i, int val) {
        bankdata[bank].put(i+offsets[bank], val);
    }
}

