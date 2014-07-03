/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2013 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.util.awt;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.TGAWriter;

/**
 * Utilities for taking screenshots of OpenGL applications.
 * @deprecated Please consider using {@link com.jogamp.opengl.util.GLReadBufferUtil},
 *             which is AWT independent and does not require a CPU based vertical image flip
 *             in case drawable {@link GLDrawable#isGLOriented() is in OpenGL orientation}.
 *             Further more you may use {@link AWTGLReadBufferUtil} to read out
 *             the framebuffer into a BufferedImage for further AWT processing.
 */
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
  public static void writeToTargaFile(final File file,
                                      final int width,
                                      final int height) throws GLException, IOException {
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
  public static void writeToTargaFile(final File file,
                                      final int width,
                                      final int height,
                                      final boolean alpha) throws GLException, IOException {
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
  public static void writeToTargaFile(final File file,
                                      final int x,
                                      final int y,
                                      final int width,
                                      final int height,
                                      final boolean alpha) throws GLException, IOException {
    if (alpha) {
      checkExtABGR();
    }

    final TGAWriter writer = new TGAWriter();
    writer.open(file, width, height, alpha);
    final ByteBuffer bgr = writer.getImageData();

    final GL gl = GLContext.getCurrentGL();

    // Set up pixel storage modes
    final GLPixelStorageModes psm = new GLPixelStorageModes();
    psm.setPackAlignment(gl, 1);

    final int readbackType = (alpha ? GL2.GL_ABGR_EXT : GL2GL3.GL_BGR);

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
  public static BufferedImage readToBufferedImage(final int width,
                                                  final int height) throws GLException {
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
  public static BufferedImage readToBufferedImage(final int width,
                                                  final int height,
                                                  final boolean alpha) throws GLException {
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
  public static BufferedImage readToBufferedImage(final int x,
                                                  final int y,
                                                  final int width,
                                                  final int height,
                                                  final boolean alpha) throws GLException {
    final int bufImgType = (alpha ? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_3BYTE_BGR);
    final int readbackType = (alpha ? GL2.GL_ABGR_EXT : GL2GL3.GL_BGR);

    if (alpha) {
      checkExtABGR();
    }

    // Allocate necessary storage
    final BufferedImage image = new BufferedImage(width, height, bufImgType);

    final GLContext glc = GLContext.getCurrent();
    final GL gl = glc.getGL();

    // Set up pixel storage modes
    final GLPixelStorageModes psm = new GLPixelStorageModes();
    psm.setPackAlignment(gl, 1);

    // read the BGR values into the image
    gl.glReadPixels(x, y, width, height, readbackType,
                    GL.GL_UNSIGNED_BYTE,
                    ByteBuffer.wrap(((DataBufferByte) image.getRaster().getDataBuffer()).getData()));

    // Restore pixel storage modes
    psm.restore(gl);

    if( glc.getGLDrawable().isGLOriented() ) {
        // Must flip BufferedImage vertically for correct results
        ImageUtil.flipImageVertically(image);
    }
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
  public static void writeToFile(final File file,
                                 final int width,
                                 final int height) throws IOException, GLException {
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
  public static void writeToFile(final File file,
                                 final int width,
                                 final int height,
                                 final boolean alpha) throws IOException, GLException {
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
  public static void writeToFile(final File file,
                                 final int x,
                                 final int y,
                                 final int width,
                                 final int height,
                                 boolean alpha) throws IOException, GLException {
    final String fileSuffix = IOUtil.getFileSuffix(file);
    if (alpha && (fileSuffix.equals("jpg") || fileSuffix.equals("jpeg"))) {
      // JPEGs can't deal properly with alpha channels
      alpha = false;
    }

    final BufferedImage image = readToBufferedImage(x, y, width, height, alpha);
    if (!ImageIO.write(image, fileSuffix, file)) {
      throw new IOException("Unsupported file format " + fileSuffix);
    }
  }

  private static void checkExtABGR() {
    final GL gl = GLContext.getCurrentGL();

    if (!gl.isExtensionAvailable(GLExtensions.EXT_abgr)) {
      throw new IllegalArgumentException("Saving alpha channel requires GL_EXT_abgr");
    }
  }
}
