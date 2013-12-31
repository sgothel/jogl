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

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.texture.spi.PNGImage;

public class JoglUtilPNGIcon {

    public static ByteBuffer arrayToX11BGRAImages(IOUtil.ClassResources resources, int[] data_size, int[] elem_bytesize) throws UnsupportedOperationException, InterruptedException, IOException, MalformedURLException {
        final PNGImage[] images = new PNGImage[resources.resourceCount()];
        data_size[0] = 0;
        for(int i=0; i<resources.resourceCount(); i++) {
            final URLConnection urlConn = resources.resolve(i);
            final PNGImage image = PNGImage.read(urlConn.getInputStream());
            data_size[0] += 2 + image.getWidth() * image.getHeight();
            images[i] = image;
        }
        final boolean is64Bit = Platform.is64Bit();
        elem_bytesize[0] =  is64Bit ? Buffers.SIZEOF_LONG : Buffers.SIZEOF_INT;
        final ByteBuffer buffer = Buffers.newDirectByteBuffer( data_size[0] * elem_bytesize[0] );

        for(int i=0; i<images.length; i++) {
            final PNGImage image1 = images[i];
            if( is64Bit ) {
                buffer.putLong(image1.getWidth());
                buffer.putLong(image1.getHeight());
            } else {
                buffer.putInt(image1.getWidth());
                buffer.putInt(image1.getHeight());
            }
            final ByteBuffer bb = image1.getData();
            final int bpp = image1.getBytesPerPixel();
            final int stride = image1.getWidth() * bpp;
            for(int y=0; y<image1.getHeight(); y++) {
                int bbOff = image1.isGLOriented() ? ( image1.getHeight() - 1 - y ) * stride : y * stride;
                for(int x=0; x<image1.getWidth(); x++) {
                    // Source: R G B A
                    // Dest:   B G R A
                    long pixel;
                    pixel  = ( 0xffL & bb.get(bbOff++) ) << 16; // R
                    pixel |= ( 0xffL & bb.get(bbOff++) ) <<  8; // G
                    pixel |= ( 0xffL & bb.get(bbOff++) ); // B
                    if( 4 == bpp ) {
                        pixel |= ( 0xffL & bb.get(bbOff++) ) << 24;
                    } else {
                        pixel |= 0x00000000ff000000L;
                    }
                    if( is64Bit ) {
                        buffer.putLong(pixel);
                    } else {
                        buffer.putInt((int)pixel);
                    }
                }
            }
        }
        buffer.rewind();
        return buffer;
    }

    public static ByteBuffer singleToRGBAImage(IOUtil.ClassResources resources, int resourceIdx, boolean toBGRA, int[] width, int[] height, int[] data_size, int[] elem_bytesize) throws UnsupportedOperationException, InterruptedException, IOException, MalformedURLException {
        width[0] = 0;
        height[0] = 0;
        data_size[0] = 0;
        final URLConnection urlConn = resources.resolve(resourceIdx);
        final PNGImage image = PNGImage.read(urlConn.getInputStream());
        width[0] = image.getWidth();
        height[0] = image.getHeight();
        data_size[0] = image.getWidth() * image.getHeight();

        elem_bytesize[0] = 4; // BGRA
        final ByteBuffer buffer = Buffers.newDirectByteBuffer( data_size[0] * elem_bytesize[0] );

        final ByteBuffer bb = image.getData();
        final int bpp = image.getBytesPerPixel();
        final int stride = image.getWidth() * bpp;
        for(int y=0; y<image.getHeight(); y++) {
            int bbOff = image.isGLOriented() ? ( image.getHeight() - 1 - y ) * stride : y * stride;
            for(int x=0; x<image.getWidth(); x++) {
                // Source: R G B A
                final byte r, g, b, a;
                r = bb.get(bbOff++); // R
                g = bb.get(bbOff++); // G
                b = bb.get(bbOff++); // B
                if( 4 == bpp ) {
                    a = bb.get(bbOff++); // A
                } else {
                    a = (byte)0xff;      // A
                }
                if( toBGRA ) {
                    // Dest:   B G R A
                    buffer.put(b);
                    buffer.put(g);
                    buffer.put(r);
                } else {
                    // Dest:   R G B A
                    buffer.put(r);
                    buffer.put(g);
                    buffer.put(b);
                }
                buffer.put(a);
            }
        }
        buffer.rewind();
        return buffer;
    }
}
