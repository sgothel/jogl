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

import java.io.*;
import java.nio.*;
import java.nio.channels.*;

/**
 * Utility class which helps take fast screenshots of OpenGL rendering
 * results into Targa-format files. Used by the {@link
 * com.sun.opengl.util.Screenshot Screenshot} class; can also be used
 * in conjunction with the {@link com.sun.opengl.util.TileRenderer
 * TileRenderer} class. <P>
 */

public class TGAWriter {
  private static final int TARGA_HEADER_SIZE = 18;

  private FileChannel ch;
  private ByteBuffer buf;

  /** Constructor for the TGAWriter. */
  public TGAWriter() {
  }

  /**
   * Opens the specified Targa file for writing, overwriting any
   * existing file, and sets up the header of the file expecting the
   * data to be filled in before closing it.
   *
   * @param file the file to write containing the screenshot
   * @param width the width of the current drawable
   * @param height the height of the current drawable
   * @param alpha whether the alpha channel should be saved. If true,
   *   requires GL_EXT_abgr extension to be present.
   *
   * @throws IOException if an I/O error occurred while writing the
   *   file
   */
  public void open(File file,
                   int width,
                   int height,
                   boolean alpha) throws IOException {
    RandomAccessFile out = new RandomAccessFile(file, "rw");
    ch = out.getChannel();
    int pixelSize = (alpha ? 32 : 24);
    int numChannels = (alpha ? 4 : 3);

    int fileLength = TARGA_HEADER_SIZE + width * height * numChannels;
    out.setLength(fileLength);
    MappedByteBuffer image = ch.map(FileChannel.MapMode.READ_WRITE, 0, fileLength);

    // write the TARGA header
    image.put(0, (byte) 0).put(1, (byte) 0);
    image.put(2, (byte) 2); // uncompressed type
    image.put(12, (byte) (width & 0xFF)); // width
    image.put(13, (byte) (width >> 8)); // width
    image.put(14, (byte) (height & 0xFF)); // height
    image.put(15, (byte) (height >> 8)); // height
    image.put(16, (byte) pixelSize); // pixel size
             
    // go to image data position
    image.position(TARGA_HEADER_SIZE);
    // jogl needs a sliced buffer
    buf = image.slice();
  }

  /**
   * Returns the ByteBuffer corresponding to the data for the image.
   * This must be filled in with data in either BGR or BGRA format
   * depending on whether an alpha channel was specified during
   * open().
   */
  public ByteBuffer getImageData() {
    return buf;
  }

  public void close() throws IOException {
    // close the file channel
    ch.close();
    buf = null;
  }
}
