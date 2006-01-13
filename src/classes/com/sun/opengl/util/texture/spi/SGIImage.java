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

package com.sun.opengl.util.texture.spi;

import java.io.*;
import javax.media.opengl.*;
import com.sun.opengl.util.*;

// Test harness
import java.awt.image.*;
import javax.swing.*;

/** <p> Reads and writes SGI RGB/RGBA images. </p>

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

    Header() {
      magic = MAGIC;
    }

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

  /** Writes this SGIImage to the specified file name. If
      flipVertically is set, outputs the scanlines from top to bottom
      rather than the default bottom to top order. */
  public void write(String filename, boolean flipVertically) throws IOException {
    write(new File(filename), flipVertically);
  }

  /** Writes this SGIImage to the specified file. If flipVertically is
      set, outputs the scanlines from top to bottom rather than the
      default bottom to top order. */
  public void write(File file, boolean flipVertically) throws IOException {
    writeImage(file, data, header.xsize, header.ysize, header.zsize, flipVertically);
  }

  /** Creates an SGIImage from the specified data in either RGB or
      RGBA format. */
  public static SGIImage createFromData(int width,
                                        int height,
                                        boolean hasAlpha,
                                        byte[] data) {
    Header header = new Header();
    header.xsize = (short) width;
    header.ysize = (short) height;
    header.zsize = (short) (hasAlpha ? 4 : 3);
    SGIImage image = new SGIImage(header);
    image.data = data;
    return image;
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
    header.zsize = 4;
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

  private static byte imgref(byte[] i,
                             int x,
                             int y,
                             int z,
                             int xs,
                             int ys,
                             int zs) {
    return i[(xs*ys*z)+(xs*y)+x];
  }


  private void writeHeader(DataOutputStream stream,
                           int xsize, int ysize, int zsize, boolean rle) throws IOException {
    // effects: outputs the 512-byte IRIS RGB header to STREAM, using xsize,
    //          ysize, and depth as the dimensions of the image. NOTE that
    //          the following defaults are used:
    //              STORAGE = 1     (storage format = RLE)
    //              BPC = 1         (# bytes/channel)
    //              DIMENSION = 3
    //              PIXMIN = 0
    //              PIXMAX = 255
    //              IMAGENAME = <80 nulls>
    //              COLORMAP = 0
    //          See ftp://ftp.sgi.com/pub/sgi/SGIIMAGESPEC for more details.

    // write out MAGIC, STORAGE, BPC
    stream.writeShort(474);
    stream.write((rle ? 1 : 0));
    stream.write(1);

    // write out DIMENSION
    stream.writeShort(3);

    // write XSIZE, YSIZE, ZSIZE
    stream.writeShort(xsize);
    stream.writeShort(ysize);
    stream.writeShort(zsize);

    // write PIXMIN, PIXMAX
    stream.writeInt(0);
    stream.writeInt(255);

    // write DUMMY
    stream.writeInt(0);

    // write IMAGENAME
    for (int i = 0; i < 80; i++)
      stream.write(0);

    // write COLORMAP
    stream.writeInt(0);

    // write DUMMY (404 bytes)
    for (int i = 0; i < 404; i++)
      stream.write(0);
  }

  private void writeImage(File file,
                          byte[] data,
                          int xsize,
                          int ysize,
                          int zsize,
                          boolean yflip) throws IOException {
    // Input data is in RGBRGBRGB or RGBARGBARGBA format; first unswizzle it
    byte[] tmpData = new byte[xsize * ysize * zsize];
    int dest = 0;
    for (int i = 0; i < zsize; i++) {
      for (int j = i; j < (xsize * ysize * zsize); j += zsize) {
        tmpData[dest++] = data[j];
      }
    }
    data = tmpData;

    // requires: DATA must be an array of size XSIZE * YSIZE * ZSIZE,
    //           indexed in the following manner:
    //             data[0]    ...data[xsize-1] == first row of first channel
    //             data[xsize]...data[2*xsize-1]   == second row of first channel
    //         ... data[(ysize - 1) * xsize]...data[(ysize * xsize) - 1] ==
    //                                            last row of first channel
    //           Later channels follow the same format.
    //           *** NOTE that "first row" is defined by the BOTTOM ROW of
    //           the image. That is, the origin is in the lower left corner.
    // effects: writes out an SGI image to FILE, RLE-compressed, INCLUDING
    //          header, of dimensions (xsize, ysize, zsize), and containing
    //          the data in DATA. If YFLIP is set, outputs the data in DATA
    //          in reverse order vertically (equivalent to a flip about the
    //          x axis).

    // Build the offset tables
    int[] starttab  = new int[ysize * zsize];
    int[] lengthtab = new int[ysize * zsize];

    // Temporary buffer for holding RLE data.
    // Note that this makes the assumption that RLE-compressed data will
    // never exceed twice the size of the input data.
    // There are surely formal proofs about how big the RLE buffer should
    // be, as well as what the optimal look-ahead size is (i.e. don't switch
    // copy/repeat modes for less than N repeats). However, I'm going from
    // empirical evidence here; the break-even point seems to be a look-
    // ahead of 3. (That is, if the three values following this one are all
    // the same as the current value, switch to repeat mode.)
    int lookahead = 3;
    byte[] rlebuf = new byte[2 * xsize * ysize * zsize];

    int cur_loc = 0;   // current offset location.
    int ptr = 0;
    int total_size = 0;
    int ystart = 0;
    int yincr = 1;
    int yend = ysize;

    if (yflip) {
      ystart = ysize - 1;
      yend = -1;
      yincr = -1;
    }

    boolean DEBUG = false;

    for (int z = 0; z < zsize; z++) {
      for (int y = ystart; y != yend; y += yincr) {
        // RLE-compress each row.
	  
        int x = 0;
        byte count = 0;
        boolean repeat_mode = false;
        boolean should_switch = false;
        int start_ptr = ptr;
        int num_ptr = ptr++;
        byte repeat_val = 0;
	  
        while (x < xsize) {
          // see if we should switch modes
          should_switch = false;
          if (repeat_mode) {
            if (imgref(data, x, y, z, xsize, ysize, zsize) != repeat_val) {
              should_switch = true;
            }
          } else {
            // look ahead to see if we should switch to repeat mode.
            // stay within the scanline for the lookahead
            if ((x + lookahead) < xsize) {
              should_switch = true;
              for (int i = 1; i <= lookahead; i++) {
                if (DEBUG)
                  System.err.println("left side was " + ((int) imgref(data, x, y, z, xsize, ysize, zsize)) +
                                     ", right side was " + (int)imgref(data, x+i, y, z, xsize, ysize, zsize));
			  
                if (imgref(data, x, y, z, xsize, ysize, zsize) !=
                    imgref(data, x+i, y, z, xsize, ysize, zsize))
                  should_switch = false;
              }
            }
          }

          if (should_switch || (count == 127)) {
            // update the number of elements we repeated/copied
            if (x > 0) {
              if (repeat_mode)
                rlebuf[num_ptr] = count;
              else
                rlebuf[num_ptr] = (byte) (count | 0x80);
            }
            // perform mode switch if necessary; output repeat_val if
            // switching FROM repeat mode, and set it if switching
            // TO repeat mode.
            if (repeat_mode) {
              if (should_switch)
                repeat_mode = false;
              rlebuf[ptr++] = repeat_val;
            } else {
              if (should_switch)
                repeat_mode = true;
              repeat_val = imgref(data, x, y, z, xsize, ysize, zsize);
            }
		  
            if (x > 0) {
              // reset the number pointer
              num_ptr = ptr++;
              // reset number of bytes copied
              count = 0;
            }
          }
		    
          // if not in repeat mode, copy element to ptr
          if (!repeat_mode) {
            rlebuf[ptr++] = imgref(data, x, y, z, xsize, ysize, zsize);
          }
          count++;

          if (x == xsize - 1) {
            // Need to store the number of pixels we copied/repeated.
            if (repeat_mode) {
              rlebuf[num_ptr] = count;
              // If we ended the row in repeat mode, store the
              // repeated value
              rlebuf[ptr++] = repeat_val;
            }
            else
              rlebuf[num_ptr] = (byte) (count | 0x80);

            // output zero counter for the last value in the row
            rlebuf[ptr++] = 0;
          }

          x++;
        }
        // output this row's length into the length table
        int rowlen = ptr - start_ptr;
        if (yflip)
          lengthtab[ysize*z+(ysize-y-1)] = rowlen;
        else
          lengthtab[ysize*z+y] = rowlen;
        // add to the start table, and update the current offset
        if (yflip)
          starttab[ysize*z+(ysize-y-1)] = cur_loc;
        else
          starttab[ysize*z+y] = cur_loc;
        cur_loc += rowlen;
      }
    }

    // Now we have the offset tables computed, as well as the RLE data.
    // Output this information to the file.
    total_size = ptr;
  
    if (DEBUG) 
      System.err.println("total_size was " + total_size);

    DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)));

    writeHeader(stream, xsize, ysize, zsize, true);

    int SIZEOF_INT = 4;
    for (int i = 0; i < (ysize * zsize); i++)
      stream.writeInt(starttab[i] + 512 + (2 * ysize * zsize * SIZEOF_INT));
    for (int i = 0; i < (ysize * zsize); i++)
      stream.writeInt(lengthtab[i]);
    for (int i = 0; i < total_size; i++)
      stream.write(rlebuf[i]);

    stream.close();
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

  // Test case
  /*
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
  */
}
