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
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.util;

import java.io.*;

/** Utilities for dealing with files. */

public class FileUtil {
  private FileUtil() {}

  /**
   * Returns the lowercase suffix of the given file name (the text
   * after the last '.' in the file name). Returns null if the file
   * name has no suffix. Only operates on the given file name;
   * performs no I/O operations.
   *
   * @param file name of the file
   * @return lowercase suffix of the file name
   * @throws NullPointerException if file is null
   */

  public static String getFileSuffix(File file) {
    return getFileSuffix(file.getName());
  }

  /**
   * Returns the lowercase suffix of the given file name (the text
   * after the last '.' in the file name). Returns null if the file
   * name has no suffix. Only operates on the given file name;
   * performs no I/O operations.
   *
   * @param filename name of the file
   * @return lowercase suffix of the file name
   * @throws NullPointerException if filename is null
   */
  public static String getFileSuffix(String filename) {
    int lastDot = filename.lastIndexOf('.');
    if (lastDot < 0) {
      return null;
    }
    return toLowerCase(filename.substring(lastDot + 1));
  }

  private static String toLowerCase(String arg) {
    if (arg == null) {
      return null;
    }

    return arg.toLowerCase();
  }
}
