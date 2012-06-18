package com.jogamp.opengl.util.texture.spi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.ImageLine;
import jogamp.opengl.util.pngj.PngReader;
import jogamp.opengl.util.pngj.PngWriter;
import jogamp.opengl.util.pngj.chunks.PngChunkTextVar;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.IOUtil;


public class PNGImage {    
    /** Creates a PNGImage from data supplied by the end user. Shares
        data with the passed ByteBuffer. Assumes the data is already in
        the correct byte order for writing to disk, i.e., LUMINANCE, RGB or RGBA bottom-to-top (OpenGL coord). */
    public static PNGImage createFromData(int width, int height, double dpiX, double dpiY,
                                          int bytesPerPixel, boolean reversedChannels, ByteBuffer data) {
        return new PNGImage(width, height, dpiX, dpiY, bytesPerPixel, reversedChannels, data);
    }
    
    /** Reads a PNG image from the specified InputStream. */
    public static PNGImage read(InputStream in) throws IOException {
        return new PNGImage(in);
    }
    
    /** Reverse read and store, implicitly flip image to GL coords. */
    private static final int getPixelRGBA8(ByteBuffer d, int dOff, ImageLine line, int lineOff, boolean hasAlpha) {
        if(hasAlpha) {
            d.put(dOff--, (byte)line.scanline[lineOff + 3]); // A
        }
        d.put(dOff--, (byte)line.scanline[lineOff + 2]); // B
        d.put(dOff--, (byte)line.scanline[lineOff + 1]); // G        
        d.put(dOff--, (byte)line.scanline[lineOff    ]); // R
        return dOff;
    }    
    /** Reverse read and store, implicitly flip image from GL coords. Handle reversed channels (BGR[A])*/
    private static int setPixelRGBA8(ImageLine line, int lineOff, ByteBuffer d, int dOff, boolean hasAlpha, boolean reversedChannels) {
        if(reversedChannels) {
            line.scanline[lineOff    ] = d.get(dOff--); // R, A
            line.scanline[lineOff + 1] = d.get(dOff--); // G, B
            line.scanline[lineOff + 2] = d.get(dOff--); // B, G
            if(hasAlpha) {
                line.scanline[lineOff + 3] = d.get(dOff--);// R
            }
        } else {
            if(hasAlpha) {
                line.scanline[lineOff + 3] = d.get(dOff--); // A
            }
            line.scanline[lineOff + 2] = d.get(dOff--); // B
            line.scanline[lineOff + 1] = d.get(dOff--); // G
            line.scanline[lineOff    ] = d.get(dOff--); // R
        }
        return dOff;
    }

    private PNGImage(int width, int height, double dpiX, double dpiY, int bytesPerPixel, boolean reversedChannels, ByteBuffer data) {
        pixelWidth=width;
        pixelHeight=height;
        dpi = new double[] { dpiX, dpiY };
        if(4 == bytesPerPixel) {
            glFormat = GL.GL_RGBA;
        } else if (3 == bytesPerPixel) {
            glFormat = GL.GL_RGB;
        } else {
            throw new InternalError("XXX: bytesPerPixel "+bytesPerPixel);
        }
        this.bytesPerPixel = bytesPerPixel;
        this.reversedChannels = reversedChannels;
        this.data = data;        
    }
    
    private PNGImage(InputStream in) {
        final PngReader pngr = new PngReader(new BufferedInputStream(in), null);
        final int channels = pngr.imgInfo.channels;
        if ( ! ( 1 == channels || 3 == channels || 4 == channels ) ) {
            throw new RuntimeException("PNGImage can only handle Lum/RGB/RGBA [1/3/4 channels] images for now. Channels "+channels);
        }
        bytesPerPixel=pngr.imgInfo.bytesPixel;
        if ( ! ( 1 == bytesPerPixel || 3 == bytesPerPixel || 4 == bytesPerPixel ) ) {
            throw new RuntimeException("PNGImage can only handle Lum/RGB/RGBA [1/3/4 bpp] images for now. BytesPerPixel "+bytesPerPixel);
        }
        if(channels != bytesPerPixel) {
            throw new RuntimeException("PNGImage currently only handles Channels [1/3/4] == BytePerPixel [1/3/4], channels: "+channels+", bytesPerPixel "+bytesPerPixel);
        }
        pixelWidth=pngr.imgInfo.cols;
        pixelHeight=pngr.imgInfo.rows;
        dpi = new double[2];
        {
            final double[] dpi2 = pngr.getMetadata().getDpi();
            dpi[0]=dpi2[0];
            dpi[1]=dpi2[1];
        }
        switch(channels) {
            case 1: glFormat = GL.GL_LUMINANCE; break;
            case 3: glFormat = GL.GL_RGB; break;
            case 4: glFormat = GL.GL_RGBA; break;
            default: throw new InternalError("XXX: channels: "+channels+", bytesPerPixel "+bytesPerPixel);
        }
        data = Buffers.newDirectByteBuffer(bytesPerPixel * pixelWidth * pixelHeight);
        reversedChannels = false; // RGB[A]
        final boolean hasAlpha = 4 == channels;
        int dataOff = bytesPerPixel * pixelWidth * pixelHeight - 1; // start at end-of-buffer, reverse store
        for (int row = 0; row < pixelHeight; row++) {
            final ImageLine l1 = pngr.readRow(row);
            int lineOff = ( pixelWidth - 1 ) * bytesPerPixel ;      // start w/ last pixel in line, reverse read
            if(1 == channels) {
                for (int j = pixelWidth - 1; j >= 0; j--) {
                    data.put(dataOff--, (byte)l1.scanline[lineOff--]); // Luminance, 1 bytesPerPixel
                }
            } else {
                for (int j = pixelWidth - 1; j >= 0; j--) {
                    dataOff = getPixelRGBA8(data, dataOff, l1, lineOff, hasAlpha);
                    lineOff -= bytesPerPixel;
                }
            }
        }
        pngr.end();
    }
    private final int pixelWidth, pixelHeight, glFormat, bytesPerPixel;
    private boolean reversedChannels;
    private final double[] dpi;
    private final ByteBuffer data;
    
    /** Returns the width of the image. */
    public int getWidth()    { return pixelWidth; }

    /** Returns the height of the image. */
    public int getHeight()   { return pixelHeight; }

    /** Returns true if data has the channels reversed to BGR or BGRA, otherwise RGB or RGBA is expected. */
    public boolean getHasReversedChannels() { return reversedChannels; }
    
    /** Returns the dpi of the image. */
    public double[] getDpi() { return dpi; }
    
    /** Returns the OpenGL format for this texture; e.g. GL.GL_BGR or GL.GL_BGRA. */
    public int getGLFormat() { return glFormat; }
    
    /** Returns the OpenGL data type: GL.GL_UNSIGNED_BYTE. */
    public int getGLType() { return GL.GL_UNSIGNED_BYTE; }

    /** Returns the bytes per pixel */
    public int getBytesPerPixel() { return bytesPerPixel; }

    /** Returns the raw data for this texture in the correct
        (bottom-to-top) order for calls to glTexImage2D. */
    public ByteBuffer getData()  { return data; }

    public void write(File out, boolean allowOverwrite) throws IOException {        
        final ImageInfo imi = new ImageInfo(pixelWidth, pixelHeight, 8, (4 == bytesPerPixel) ? true : false); // 8 bits per channel, no alpha 
        // open image for writing to a output stream
        final OutputStream outs = new BufferedOutputStream(IOUtil.getFileOutputStream(out, allowOverwrite));
        try {
            final PngWriter png = new PngWriter(outs, imi); 
            // add some optional metadata (chunks)
            png.getMetadata().setDpi(dpi[0], dpi[1]);
            png.getMetadata().setTimeNow(0); // 0 seconds fron now = now
            png.getMetadata().setText(PngChunkTextVar.KEY_Title, "JogAmp PNGImage");
            // png.getMetadata().setText("my key", "my text");
            final boolean hasAlpha = 4 == bytesPerPixel;
            final ImageLine l1 = new ImageLine(imi);
            int dataOff = bytesPerPixel * pixelWidth * pixelHeight - 1; // start at end-of-buffer, reverse read
            for (int row = 0; row < pixelHeight; row++) {
                int lineOff = ( pixelWidth - 1 ) * bytesPerPixel ;      // start w/ last pixel in line, reverse store
                if(1 == bytesPerPixel) {
                    for (int j = pixelWidth - 1; j >= 0; j--) {
                        l1.scanline[lineOff--] = data.get(dataOff--); // // Luminance, 1 bytesPerPixel
                    }
                } else {
                    for (int j = pixelWidth - 1; j >= 0; j--) {
                        dataOff = setPixelRGBA8(l1, lineOff, data, dataOff, hasAlpha, reversedChannels);
                        lineOff -= bytesPerPixel;
                    }
                }
                png.writeRow(l1, row);
            }
            png.end();
        } finally {
            IOUtil.close(outs, false);
        }
    }
    
    public String toString() { return "PNGImage["+pixelWidth+"x"+pixelHeight+", dpi "+dpi[0]+" x "+dpi[1]+", bytesPerPixel "+bytesPerPixel+", reversedChannels "+reversedChannels+", "+data+"]"; }       
}
