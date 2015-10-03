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
package jogamp.newt.driver.opengl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.nio.ByteBuffer;

import com.jogamp.nativewindow.util.PixelFormat;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.PNGPixelRect;

public class JoglUtilPNGIcon {

    public static ByteBuffer arrayToX11BGRAImages(final IOUtil.ClassResources resources, final int[] data_size, final int[] elem_bytesize) throws UnsupportedOperationException, InterruptedException, IOException, MalformedURLException {
        final PNGPixelRect[] images = new PNGPixelRect[resources.resourceCount()];
        data_size[0] = 0;
        for(int i=0; i<resources.resourceCount(); i++) {
            final URLConnection urlConn = resources.resolve(i);
            if( null != urlConn ) {
                final PNGPixelRect image = PNGPixelRect.read(urlConn.getInputStream(), PixelFormat.BGRA8888, false /* directBuffer */, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
                data_size[0] += 2 + image.getSize().getWidth() * image.getSize().getHeight();
                images[i] = image;
            } else {
                images[i] = null;
            }
        }
        if( 0 == data_size[0] ) {
            // no image, abort
            return null;
        }
        final boolean is64Bit = Platform.is64Bit();
        elem_bytesize[0] =  is64Bit ? Buffers.SIZEOF_LONG : Buffers.SIZEOF_INT;
        final ByteBuffer buffer = Buffers.newDirectByteBuffer( data_size[0] * elem_bytesize[0] );

        for(int i=0; i<images.length; i++) {
            final PNGPixelRect image1 = images[i];
            if( null != image1 ) {
                final int width = image1.getSize().getWidth();
                final int height = image1.getSize().getHeight();
                if( is64Bit ) {
                    buffer.putLong(width);
                    buffer.putLong(height);
                } else {
                    buffer.putInt(width);
                    buffer.putInt(height);
                }
                final ByteBuffer bb = image1.getPixels();
                final int stride = image1.getStride();
                for(int y=0; y<height; y++) {
                    int bbOff = y * stride;
                    for(int x=0; x<width; x++) {
                        long pixel;
                        pixel  = ( 0xffL & bb.get(bbOff++) );       // B
                        pixel |= ( 0xffL & bb.get(bbOff++) ) <<  8; // G
                        pixel |= ( 0xffL & bb.get(bbOff++) ) << 16; // R
                        pixel |= ( 0xffL & bb.get(bbOff++) ) << 24; // A
                        if( is64Bit ) {
                            buffer.putLong(pixel);
                        } else {
                            buffer.putInt((int)pixel);
                        }
                    }
                }
            }
        }
        buffer.rewind();
        return buffer;
    }
}
