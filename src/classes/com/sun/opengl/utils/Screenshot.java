/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.sun.opengl.utils;

import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import javax.imageio.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;

/** Utilities for taking screenshots of OpenGL applications. */

public class Screenshot {
  private static final int TARGA_HEADER_SIZE = 18;

  /** 
   * Takes a fast screenshot of the current OpenGL drawable to a Targa
   * file. Requires the OpenGL context for the desired drawable to be
   * current. This is the fastest mechanism for taking a screenshot of
   * an application. Contributed by Carsten Weisse of Bytonic Software
   * (http://bytonic.de/).
   *
   * @param file the file to write containing the screenshot
   * @param width the width of the current drawable
   * @param height the height of the current drawable
   *
   * @throws GLException if an OpenGL context was not current or
   *   another OpenGL-related error occurred
   * @throws IOException if an I/O error occurred while writing the
   *   file
   */
  public static void writeToTargaFile(File file,
                                      int width,
                                      int height) throws GLException, IOException {
    RandomAccessFile out = new RandomAccessFile(file, "rw");
    FileChannel ch = out.getChannel();
    int fileLength = TARGA_HEADER_SIZE + width * height * 3;
    out.setLength(fileLength);
    MappedByteBuffer image = ch.map(FileChannel.MapMode.READ_WRITE, 0, fileLength);

    // write the TARGA header
    image.put(0, (byte) 0).put(1, (byte) 0);
    image.put(2, (byte) 2); // uncompressed type
    image.put(12, (byte) (width & 0xFF)); // width
    image.put(13, (byte) (width >> 8)); // width
    image.put(14, (byte) (height & 0xFF)); // height
    image.put(15, (byte) (height >> 8)); // height
    image.put(16, (byte) 24); // pixel size
             
    // go to image data position
    image.position(TARGA_HEADER_SIZE);
    // jogl needs a sliced buffer
    ByteBuffer bgr = image.slice();

    GL gl = GLU.getCurrentGL();

    // Set up pixel storage modes
    PixelStorageModes psm = new PixelStorageModes();
    psm.save(gl);

    // read the BGR values into the image buffer
    gl.glReadPixels(0, 0, width, height, GL.GL_BGR,
                    GL.GL_UNSIGNED_BYTE, bgr);

    // Restore pixel storage modes
    psm.restore(gl);

    // close the file channel
    ch.close();
  }

  /**
   * Takes a screenshot of the current OpenGL drawable to a
   * BufferedImage. Requires the OpenGL context for the desired
   * drawable to be current. Note that the scanlines of the resulting
   * image are flipped vertically in order to correctly match the
   * OpenGL contents, which takes time and is therefore not as fast as
   * the Targa screenshot function.
   *
   * @param width the width of the current drawable
   * @param height the height of the current drawable
   *
   * @throws GLException if an OpenGL context was not current or
   *   another OpenGL-related error occurred
   */
  public static BufferedImage readToBufferedImage(int width,
                                                  int height) throws GLException {
    // Allocate necessary storage
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

    GL gl = GLU.getCurrentGL();

    // Set up pixel storage modes
    PixelStorageModes psm = new PixelStorageModes();
    psm.save(gl);

    // read the BGR values into the image
    gl.glReadPixels(0, 0, width, height, GL.GL_BGR,
                    GL.GL_UNSIGNED_BYTE,
                    ByteBuffer.wrap(((DataBufferByte) image.getRaster().getDataBuffer()).getData()));

    // Restore pixel storage modes
    psm.restore(gl);

    // Must flip BufferedImage vertically for correct results
    TextureIO.flipImageVertically(image);
    return image;
  }

  /**
   * Takes a screenshot of the current OpenGL drawable to the
   * specified file on disk using the ImageIO package. Requires the
   * OpenGL context for the desired drawable to be current. This is
   * not the fastest mechanism for taking a screenshot but may be more
   * convenient than others for getting images for consumption by
   * other packages. The file format is inferred from the suffix of
   * the given file.
   *
   * @throws GLException if an OpenGL context was not current or
   *   another OpenGL-related error occurred
   *
   * @throws IOException if an I/O error occurred or if the file could
   *   not be written to disk due to the requested file format being
   *   unsupported by ImageIO
   */
  public static void writeToFile(File file,
                                 int width,
                                 int height) throws IOException, GLException {
    BufferedImage image = readToBufferedImage(width, height);
    if (!ImageIO.write(image, TextureIO.getFileSuffix(file), file)) {
      throw new IOException("Unsupported file format " +
                            TextureIO.getFileSuffix(file));
    }
  }

  private static int glGetInteger(GL gl, int pname, int[] tmp) {
    gl.glGetIntegerv(pname, tmp, 0);
    return tmp[0];
  }

  static class PixelStorageModes {
    int packAlignment;
    int packRowLength;
    int packSkipRows;
    int packSkipPixels;
    int packSwapBytes;
    int[] tmp = new int[1];

    void save(GL gl) {
      packAlignment  = glGetInteger(gl, GL.GL_PACK_ALIGNMENT, tmp);
      packRowLength  = glGetInteger(gl, GL.GL_PACK_ROW_LENGTH, tmp);
      packSkipRows   = glGetInteger(gl, GL.GL_PACK_SKIP_ROWS, tmp);
      packSkipPixels = glGetInteger(gl, GL.GL_PACK_SKIP_PIXELS, tmp);
      packSwapBytes  = glGetInteger(gl, GL.GL_PACK_SWAP_BYTES, tmp);

      gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
      gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH, 0);
      gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS, 0);
      gl.glPixelStorei(GL.GL_PACK_SKIP_PIXELS, 0);
      gl.glPixelStorei(GL.GL_PACK_SWAP_BYTES, 0);
    }

    void restore(GL gl) {
      gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, packAlignment);
      gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH, packRowLength);
      gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS, packSkipRows);
      gl.glPixelStorei(GL.GL_PACK_SKIP_PIXELS, packSkipPixels);
      gl.glPixelStorei(GL.GL_PACK_SWAP_BYTES, packSwapBytes);
    }
  }
}
