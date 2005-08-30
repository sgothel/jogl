/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.gluegen.opengl;

import java.io.*;

public class ConvertFromGL4Java {
  public static void main(String[] args) throws IOException {
    for (int i = 0; i < args.length; i++) {
      convert(new File(args[i]));
    }
  }

  private static void convert(File src) throws IOException {
    File orig = new File(src.getAbsolutePath() + ".orig");
    if (!src.renameTo(orig)) {
      throw new IOException("Error renaming original file to " + orig);
    }
    File dest = src;
    BufferedReader reader = new BufferedReader(new FileReader(orig));
    BufferedWriter writer = new BufferedWriter(new FileWriter(dest));
    boolean handledImports = false;
    String line = null;
    while ((line = reader.readLine()) != null) {
      String trimmed = line.trim();
      boolean isImport = false;
      if (trimmed.startsWith("import gl4java")) {
        line = "import javax.media.opengl.*;";
        isImport = true;
      }
      if (!isImport ||
          (isImport && !handledImports)) {
        line = line.replaceAll("GLFunc14",  "GL");
        line = line.replaceAll("GLUFunc14", "GLU");
        line = line.replaceAll("GLFunc",    "GL");
        line = line.replaceAll("GLUFunc",   "GLU");
        line = line.replaceAll("implements GLEnum,", "implements ");
        line = line.replaceAll(", GLEnum\\s", " ");
        line = line.replaceAll("GLEnum,", "");
        line = line.replaceAll("GLEnum.", "");
        line = line.replaceAll("GLEnum",  "");
        line = line.replaceAll("GL_", "GL.GL_");
        writer.write(line);
        writer.newLine();
        if (isImport) {
          handledImports = true;
        }
      }
    }
    writer.flush();
    reader.close();
    writer.close();
  }
}
