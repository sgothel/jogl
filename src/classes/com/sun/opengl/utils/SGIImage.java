/*
 * Portions Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.utils;

import java.io.*;
import javax.media.opengl.*;
import com.sun.opengl.utils.*;

// Test harness
import java.awt.image.*;
import javax.swing.*;

/** <p> Reads SGI RGB/RGBA images. </p>

    <p> Written from <a href =
    "http://astronomy.swin.edu.au/~pbourke/dataformats/sgirgb/">Paul
    Bourke's adaptation</a> of the <a href =
    "http://astronomy.swin.edu.au/~pbourke/dataformats/sgirgb/sgiversion.html">SGI
    specification</a>. </p>
*/

public class SGIImage {
  private Header header;
  private int    format;
  private byte[] data;
  // Used for decoding RLE-compressed images
  private int[]  rowStart;
  private int[]  rowSize;
  private int    rleEnd;
  private byte[] tmpData;
  private byte[] tmpRead;

  private static final int MAGIC = 474;

  static class Header {
    short magic;        // IRIS image file magic number
                        // This should be decimal 474
    byte  storage;      // Storage format
                        // 0 for uncompressed
                        // 1 for RLE compression
    byte  bpc;          // Number of bytes per pixel channel 
                        // Legally 1 or 2
    short dimension;    // Number of dimensions
                        // Legally 1, 2, or 3
                        // 1 means a single row, XSIZE long
                        // 2 means a single 2D image
                        // 3 means multiple 2D images
    short xsize;        // X size in pixels 
    short ysize;        // Y size in pixels 
    short zsize;        // Number of channels
                        // 1 indicates greyscale
                        // 3 indicates RGB
                        // 4 indicates RGB and Alpha
    int pixmin;         // Minimum pixel value
                        // This is the lowest pixel value in the image
    int pixmax;         // Maximum pixel value
                        // This is the highest pixel value in the image
    int dummy;          // Ignored
                        // Normally set to 0
    String imagename;   // Image name; 80 bytes long
                        // Must be null terminated, therefore at most 79 bytes
    int colormap;       // Colormap ID
                        // 0 - normal mode
                        // 1 - dithered, 3 mits for red and green, 2 for blue, obsolete
                        // 2 - index colour, obsolete
                        // 3 - not an image but a colourmap
    // 404 bytes  char    DUMMY      Ignored
    // Should be set to 0, makes the header 512 bytes.

    Header(DataInputStream in) throws IOException {
      magic      = in.readShort();
      storage    = in.readByte();
      bpc        = in.readByte();
      dimension  = in.readShort();
      xsize      = in.readShort();
      ysize      = in.readShort();
      zsize      = in.readShort();
      pixmin     = in.readInt();
      pixmax     = in.readInt();
      dummy      = in.readInt();
      byte[] tmpname = new byte[80];
      in.read(tmpname);
      int numChars = 0;
      while (tmpname[numChars++] != 0);
      imagename  = new String(tmpname, 0, numChars);
      colormap   = in.readInt();
      byte[] tmp = new byte[404];
      in.read(tmp);
    }

    public String toString() {
      return ("magic: " + magic +
              " storage: " + (int) storage +
              " bpc: " + (int) bpc +
              " dimension: " + dimension +
              " xsize: " + xsize +
              " ysize: " + ysize +
              " zsize: " + zsize +
              " pixmin: " + pixmin +
              " pixmax: " + pixmax +
              " imagename: " + imagename +
              " colormap: " + colormap);
    }
  }

  private SGIImage(Header header) {
    this.header = header;
  }

  /** Reads an SGI image from the specified file. */
  public static SGIImage read(String filename) throws IOException {
    return read(new FileInputStream(filename));
  }

  /** Reads an SGI image from the specified InputStream. */
  public static SGIImage read(InputStream in) throws IOException {
    DataInputStream dIn = new DataInputStream(new BufferedInputStream(in));

    Header header = new Header(dIn);
    SGIImage res = new SGIImage(header);
    res.decodeImage(dIn);
    return res;
  }

  /** Determines from the magic number whether the given InputStream
      points to an SGI RGB image. The given InputStream must return
      true from markSupported() and support a minimum of two bytes
      of read-ahead. */
  public static boolean isSGIImage(InputStream in) throws IOException {
    if (!(in instanceof BufferedInputStream)) {
      in = new BufferedInputStream(in);
    }
    if (!in.markSupported()) {
      throw new IOException("Can not test non-destructively whether given InputStream is an SGI RGB image");
    }
    DataInputStream dIn = new DataInputStream(in);
    dIn.mark(4);
    short magic = dIn.readShort();
    dIn.reset();
    return (magic == MAGIC);
  }

  /** Returns the width of the image. */
  public int getWidth() {
    return header.xsize;
  }

  /** Returns the height of the image. */
  public int getHeight() {
    return header.ysize;
  }

  /** Returns the OpenGL format for this texture; e.g. GL.GL_RGB or GL.GL_RGBA. */
  public int getFormat() {
    return format;
  }

  /** Returns the raw data for this texture in the correct
      (bottom-to-top) order for calls to glTexImage2D. */
  public byte[] getData()  { return data; }

  public String toString() {
    return header.toString();
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //
  
  private void decodeImage(DataInputStream in) throws IOException {
    if (header.storage == 1) {
      // Read RLE compression data; row starts and sizes
      int x = header.ysize * header.zsize;
      rowStart = new int[x];
      rowSize  = new int[x];
      rleEnd   = 4 * 2 * x + 512;
      for (int i = 0; i < x; i++) {
        rowStart[i] = in.readInt();
      }
      for (int i = 0; i < x; i++) {
        rowSize[i] = in.readInt();
      }
      tmpRead = new byte[header.xsize * 256];
    }
    tmpData = readAll(in);

    int xsize = header.xsize;
    int ysize = header.ysize;
    int zsize = header.zsize;
    int lptr  = 0;

    data = new byte[xsize * ysize * 4];
    byte[] rbuf = new byte[xsize];
    byte[] gbuf = new byte[xsize];
    byte[] bbuf = new byte[xsize];
    byte[] abuf = new byte[xsize];
    for (int y = 0; y < ysize; y++) {
      if (zsize >= 4) {
        getRow(rbuf, y, 0);
        getRow(gbuf, y, 1);
        getRow(bbuf, y, 2);
        getRow(abuf, y, 3);
        rgbatorgba(rbuf, gbuf, bbuf, abuf, data, lptr);
      } else if (zsize == 3) {
        getRow(rbuf, y, 0);
        getRow(gbuf, y, 1);
        getRow(bbuf, y, 2);
        rgbtorgba(rbuf, gbuf, bbuf, data, lptr);
      } else if (zsize == 2) {
        getRow(rbuf, y, 0);
        getRow(abuf, y, 1);
        latorgba(rbuf, abuf, data, lptr);
      } else {
        getRow(rbuf, y, 0);
        bwtorgba(rbuf, data, lptr);
      }
      lptr += 4 * xsize;
    }
    rowStart = null;
    rowSize  = null;
    tmpData  = null;
    tmpRead  = null;
    format   = GL.GL_RGBA;
  }

  private void getRow(byte[] buf, int y, int z) {
    if (header.storage == 1) {
      int offs = rowStart[y + z * header.ysize] - rleEnd;
      System.arraycopy(tmpData, offs, tmpRead, 0, rowSize[y + z * header.ysize]);
      int iPtr = 0;
      int oPtr = 0;
      for (;;) {
        byte pixel = tmpRead[iPtr++];
        int count = (int) (pixel & 0x7F);
        if (count == 0) {
          return;
        }
        if ((pixel & 0x80) != 0) {
          while ((count--) > 0) {
            buf[oPtr++] = tmpRead[iPtr++];
          }
        } else {
          pixel = tmpRead[iPtr++];
          while ((count--) > 0) {
            buf[oPtr++] = pixel;
          }
        }
      }
    } else {
      int offs = (y * header.xsize) + (z * header.xsize * header.ysize);
      System.arraycopy(tmpData, offs, buf, 0, header.xsize);
    }
  }

  private void bwtorgba(byte[] b, byte[] dest, int lptr) {
    for (int i = 0; i < b.length; i++) {
      dest[4 * i + lptr + 0] = b[i];
      dest[4 * i + lptr + 1] = b[i];
      dest[4 * i + lptr + 2] = b[i];
      dest[4 * i + lptr + 3] = (byte) 0xFF;
    }
  }

  private void latorgba(byte[] b, byte[] a, byte[] dest, int lptr) {
    for (int i = 0; i < b.length; i++) {
      dest[4 * i + lptr + 0] = b[i];
      dest[4 * i + lptr + 1] = b[i];
      dest[4 * i + lptr + 2] = b[i];
      dest[4 * i + lptr + 3] = a[i];
    }
  }

  private void rgbtorgba(byte[] r, byte[] g, byte[] b, byte[] dest, int lptr) {
    for (int i = 0; i < b.length; i++) {
      dest[4 * i + lptr + 0] = r[i];
      dest[4 * i + lptr + 1] = g[i];
      dest[4 * i + lptr + 2] = b[i];
      dest[4 * i + lptr + 3] = (byte) 0xFF;
    }
  }

  private void rgbatorgba(byte[] r, byte[] g, byte[] b, byte[] a, byte[] dest, int lptr) {
    for (int i = 0; i < b.length; i++) {
      dest[4 * i + lptr + 0] = r[i];
      dest[4 * i + lptr + 1] = g[i];
      dest[4 * i + lptr + 2] = b[i];
      dest[4 * i + lptr + 3] = a[i];
    }
  }

  private byte[] readAll(DataInputStream in) throws IOException {
    byte[] dest = new byte[16384];
    int pos = 0;
    int numRead = 0;
    
    boolean done = false;

    do {
      numRead = in.read(dest, pos, dest.length - pos);
      if (pos == dest.length) {
        // Resize destination buffer
        byte[] newDest = new byte[2 * dest.length];
        System.arraycopy(dest, 0, newDest, 0, pos);
        dest = newDest;
      }
      if (numRead > 0) {
        pos += numRead;
      }

      done = ((numRead == -1) || (in.available() == 0));
    } while (!done);

    // Trim destination buffer
    if (pos != dest.length) {
      byte[] finalDest = new byte[pos];
      System.arraycopy(dest, 0, finalDest, 0, pos);
      dest = finalDest;
    }

    return dest;
  }

  public static void main(String[] args) {
    for (int i = 0; i < args.length; i++) {
      try {
        System.out.println(args[i] + ":");
        SGIImage image = SGIImage.read(args[i]);
        System.out.println(image);
        BufferedImage img = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        WritableRaster raster = img.getRaster();
        DataBufferByte db = (DataBufferByte) raster.getDataBuffer();
        byte[] src  = image.getData();
        byte[] dest = db.getData();
        for (int j = 0; j < src.length; j += 4) {
          dest[j + 0] = src[j + 3];
          dest[j + 1] = src[j + 2];
          dest[j + 2] = src[j + 1];
          dest[j + 3] = src[j + 0];
        }
        // System.arraycopy(src, 0, dest, 0, src.length);
        ImageIcon icon = new ImageIcon(img);
        JLabel label = new JLabel();
        label.setIcon(icon);
        JFrame frame = new JFrame(args[i]);
        frame.getContentPane().add(label);
        frame.pack();
        frame.show();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }
}
