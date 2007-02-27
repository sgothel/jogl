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

package com.sun.opengl.util;

import java.awt.image.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import javax.imageio.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;

/** Utilities for taking screenshots of OpenGL applications. */

public class Screenshot {
  private Screenshot() {}

  /** 
   * Takes a fast screenshot of the current OpenGL drawable to a Targa
   * file. Requires the OpenGL context for the desired drawable to be
   * current. Takes the screenshot from the last assigned read buffer,
   * or the OpenGL default read buffer if none has been specified by
   * the user (GL_FRONT for single-buffered configurations and GL_BACK
   * for double-buffered configurations). This is the fastest
   * mechanism for taking a screenshot of an application. Contributed
   * by Carsten Weisse of Bytonic Software (http://bytonic.de/). <p>
   *
   * No alpha channel is written with this variant.
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
    writeToTargaFile(file, width, height, false);
  }

  /** 
   * Takes a fast screenshot of the current OpenGL drawable to a Targa
   * file. Requires the OpenGL context for the desired drawable to be
   * current. Takes the screenshot from the last assigned read buffer,
   * or the OpenGL default read buffer if none has been specified by
   * the user (GL_FRONT for single-buffered configurations and GL_BACK
   * for double-buffered configurations). This is the fastest
   * mechanism for taking a screenshot of an application. Contributed
   * by Carsten Weisse of Bytonic Software (http://bytonic.de/).
   *
   * @param file the file to write containing the screenshot
   * @param width the width of the current drawable
   * @param height the height of the current drawable
   * @param alpha whether the alpha channel should be saved. If true,
   *   requires GL_EXT_abgr extension to be present.
   *
   * @throws GLException if an OpenGL context was not current or
   *   another OpenGL-related error occurred
   * @throws IOException if an I/O error occurred while writing the
   *   file
   */
  public static void writeToTargaFile(File file,
                                      int width,
                                      int height,
                                      boolean alpha) throws GLException, IOException {
    writeToTargaFile(file, 0, 0, width, height, alpha);
  }

  /** 
   * Takes a fast screenshot of the current OpenGL drawable to a Targa
   * file. Requires the OpenGL context for the desired drawable to be
   * current. Takes the screenshot from the last assigned read buffer,
   * or the OpenGL default read buffer if none has been specified by
   * the user (GL_FRONT for single-buffered configurations and GL_BACK
   * for double-buffered configurations). This is the fastest
   * mechanism for taking a screenshot of an application. Contributed
   * by Carsten Weisse of Bytonic Software (http://bytonic.de/).
   *
   * @param file the file to write containing the screenshot
   * @param x the starting x coordinate of the screenshot, measured from the lower-left
   * @param y the starting y coordinate of the screenshot, measured from the lower-left
   * @param width the width of the desired screenshot area
   * @param height the height of the desired screenshot area
   * @param alpha whether the alpha channel should be saved. If true,
   *   requires GL_EXT_abgr extension to be present.
   *
   * @throws GLException if an OpenGL context was not current or
   *   another OpenGL-related error occurred
   * @throws IOException if an I/O error occurred while writing the
   *   file
   */
  public static void writeToTargaFile(File file,
                                      int x,
                                      int y,
                                      int width,
                                      int height,
                                      boolean alpha) throws GLException, IOException {
    if (alpha) {
      checkExtABGR();
    }

    TGAWriter writer = new TGAWriter();
    writer.open(file, width, height, alpha);
    ByteBuffer bgr = writer.getImageData();

    GL gl = GLU.getCurrentGL();

    // Set up pixel storage modes
    PixelStorageModes psm = new PixelStorageModes();
    psm.save(gl);

    int readbackType = (alpha ? GL.GL_ABGR_EXT : GL.GL_BGR);

    // read the BGR values into the image buffer
    gl.glReadPixels(x, y, width, height, readbackType,
                    GL.GL_UNSIGNED_BYTE, bgr);

    // Restore pixel storage modes
    psm.restore(gl);

    // close the file
    writer.close();
  }

  /**
   * Takes a screenshot of the current OpenGL drawable to a
   * BufferedImage. Requires the OpenGL context for the desired
   * drawable to be current. Takes the screenshot from the last
   * assigned read buffer, or the OpenGL default read buffer if none
   * has been specified by the user (GL_FRONT for single-buffered
   * configurations and GL_BACK for double-buffered configurations).
   * Note that the scanlines of the resulting image are flipped
   * vertically in order to correctly match the OpenGL contents, which
   * takes time and is therefore not as fast as the Targa screenshot
   * function. <P>
   *
   * No alpha channel is read back with this variant.
   *
   * @param width the width of the current drawable
   * @param height the height of the current drawable
   *
   * @throws GLException if an OpenGL context was not current or
   *   another OpenGL-related error occurred
   */
  public static BufferedImage readToBufferedImage(int width,
                                                  int height) throws GLException {
    return readToBufferedImage(width, height, false);
  }

  /**
   * Takes a screenshot of the current OpenGL drawable to a
   * BufferedImage. Requires the OpenGL context for the desired
   * drawable to be current. Takes the screenshot from the last
   * assigned read buffer, or the OpenGL default read buffer if none
   * has been specified by the user (GL_FRONT for single-buffered
   * configurations and GL_BACK for double-buffered configurations).
   * Note that the scanlines of the resulting image are flipped
   * vertically in order to correctly match the OpenGL contents, which
   * takes time and is therefore not as fast as the Targa screenshot
   * function.
   *
   * @param width the width of the current drawable
   * @param height the height of the current drawable
   * @param alpha whether the alpha channel should be read back. If
   *   true, requires GL_EXT_abgr extension to be present.
   *
   * @throws GLException if an OpenGL context was not current or
   *   another OpenGL-related error occurred
   */
  public static BufferedImage readToBufferedImage(int width,
                                                  int height,
                                                  boolean alpha) throws GLException {
    return readToBufferedImage(0, 0, width, height, alpha);
  }

  /**
   * Takes a screenshot of the current OpenGL drawable to a
   * BufferedImage. Requires the OpenGL context for the desired
   * drawable to be current. Takes the screenshot from the last
   * assigned read buffer, or the OpenGL default read buffer if none
   * has been specified by the user (GL_FRONT for single-buffered
   * configurations and GL_BACK for double-buffered configurations).
   * Note that the scanlines of the resulting image are flipped
   * vertically in order to correctly match the OpenGL contents, which
   * takes time and is therefore not as fast as the Targa screenshot
   * function.
   *
   * @param x the starting x coordinate of the screenshot, measured from the lower-left
   * @param y the starting y coordinate of the screenshot, measured from the lower-left
   * @param width the width of the desired screenshot area
   * @param height the height of the desired screenshot area
   * @param alpha whether the alpha channel should be read back. If
   *   true, requires GL_EXT_abgr extension to be present.
   *
   * @throws GLException if an OpenGL context was not current or
   *   another OpenGL-related error occurred
   */
  public static BufferedImage readToBufferedImage(int x,
                                                  int y,
                                                  int width,
                                                  int height,
                                                  boolean alpha) throws GLException {
    int bufImgType = (alpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);
    int readbackType = (alpha ? GL.GL_ABGR_EXT : GL.GL_BGR);

    if (alpha) {
      checkExtABGR();
    }

    // Allocate necessary storage
    BufferedImage image = new BufferedImage(width, height, bufImgType);

    GL gl = GLU.getCurrentGL();

    // Set up pixel storage modes
    PixelStorageModes psm = new PixelStorageModes();
    psm.save(gl);

    // read the BGR values into the image
    gl.glReadPixels(x, y, width, height, readbackType,
                    GL.GL_UNSIGNED_BYTE,
                    ByteBuffer.wrap(((DataBufferByte) image.getRaster().getDataBuffer()).getData()));

    // Restore pixel storage modes
    psm.restore(gl);

    // Must flip BufferedImage vertically for correct results
    ImageUtil.flipImageVertically(image);
    return image;
  }

  /**
   * Takes a screenshot of the current OpenGL drawable to the
   * specified file on disk using the ImageIO package. Requires the
   * OpenGL context for the desired drawable to be current. Takes the
   * screenshot from the last assigned read buffer, or the OpenGL
   * default read buffer if none has been specified by the user
   * (GL_FRONT for single-buffered configurations and GL_BACK for
   * double-buffered configurations). This is not the fastest
   * mechanism for taking a screenshot but may be more convenient than
   * others for getting images for consumption by other packages. The
   * file format is inferred from the suffix of the given file. <P>
   *
   * No alpha channel is saved with this variant.
   *
   * @param file the file to write containing the screenshot
   * @param width the width of the current drawable
   * @param height the height of the current drawable
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
    writeToFile(file, width, height, false);
  }

  /**
   * Takes a screenshot of the current OpenGL drawable to the
   * specified file on disk using the ImageIO package. Requires the
   * OpenGL context for the desired drawable to be current. Takes the
   * screenshot from the last assigned read buffer, or the OpenGL
   * default read buffer if none has been specified by the user
   * (GL_FRONT for single-buffered configurations and GL_BACK for
   * double-buffered configurations). This is not the fastest
   * mechanism for taking a screenshot but may be more convenient than
   * others for getting images for consumption by other packages. The
   * file format is inferred from the suffix of the given file. <P>
   *
   * Note that some file formats, in particular JPEG, can not handle
   * an alpha channel properly. If the "alpha" argument is specified
   * as true for such a file format it will be silently ignored.
   *
   * @param file the file to write containing the screenshot
   * @param width the width of the current drawable
   * @param height the height of the current drawable
   * @param alpha whether an alpha channel should be saved. If true,
   *   requires GL_EXT_abgr extension to be present.
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
                                 int height,
                                 boolean alpha) throws IOException, GLException {
    writeToFile(file, 0, 0, width, height, alpha);
  }

  /**
   * Takes a screenshot of the current OpenGL drawable to the
   * specified file on disk using the ImageIO package. Requires the
   * OpenGL context for the desired drawable to be current. Takes the
   * screenshot from the last assigned read buffer, or the OpenGL
   * default read buffer if none has been specified by the user
   * (GL_FRONT for single-buffered configurations and GL_BACK for
   * double-buffered configurations). This is not the fastest
   * mechanism for taking a screenshot but may be more convenient than
   * others for getting images for consumption by other packages. The
   * file format is inferred from the suffix of the given file. <P>
   *
   * Note that some file formats, in particular JPEG, can not handle
   * an alpha channel properly. If the "alpha" argument is specified
   * as true for such a file format it will be silently ignored.
   *
   * @param file the file to write containing the screenshot
   * @param x the starting x coordinate of the screenshot, measured from the lower-left
   * @param y the starting y coordinate of the screenshot, measured from the lower-left
   * @param width the width of the current drawable
   * @param height the height of the current drawable
   * @param alpha whether an alpha channel should be saved. If true,
   *   requires GL_EXT_abgr extension to be present.
   *
   * @throws GLException if an OpenGL context was not current or
   *   another OpenGL-related error occurred
   *
   * @throws IOException if an I/O error occurred or if the file could
   *   not be written to disk due to the requested file format being
   *   unsupported by ImageIO
   */
  public static void writeToFile(File file,
                                 int x,
                                 int y,
                                 int width,
                                 int height,
                                 boolean alpha) throws IOException, GLException {
    String fileSuffix = FileUtil.getFileSuffix(file);
    if (alpha && (fileSuffix.equals("jpg") || fileSuffix.equals("jpeg"))) {
      // JPEGs can't deal properly with alpha channels
      alpha = false;
    }

    BufferedImage image = readToBufferedImage(x, y, width, height, alpha);
    if (!ImageIO.write(image, fileSuffix, file)) {
      throw new IOException("Unsupported file format " + fileSuffix);
    }
  }

  private static int glGetInteger(GL gl, int pname, int[] tmp) {
    gl.glGetIntegerv(pname, tmp, 0);
    return tmp[0];
  }

  private static void checkExtABGR() {
    GL gl = GLU.getCurrentGL();
    if (!gl.isExtensionAvailable("GL_EXT_abgr")) {
      throw new IllegalArgumentException("Saving alpha channel requires GL_EXT_abgr");
    }
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
