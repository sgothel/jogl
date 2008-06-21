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

package com.sun.opengl.util.texture;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;

public class TextureUtil {
  /** Constant which can be used as a file suffix to indicate a
      DirectDraw Surface file. */
  public static final String DDS     = "dds";

  /** Constant which can be used as a file suffix to indicate an SGI
      RGB file. */
  public static final String SGI     = "sgi";

  /** Constant which can be used as a file suffix to indicate an SGI
      RGB file. */
  public static final String SGI_RGB = "rgb";

  /** Constant which can be used as a file suffix to indicate a GIF
      file. */
  public static final String GIF     = "gif";

  /** Constant which can be used as a file suffix to indicate a JPEG
      file. */
  public static final String JPG     = "jpg";

  /** Constant which can be used as a file suffix to indicate a PNG
      file. */
  public static final String PNG     = "png";

  /** Constant which can be used as a file suffix to indicate a Targa
      file. */
  public static final String TGA     = "tga";

  /** Constant which can be used as a file suffix to indicate a TIFF
      file. */
  public static final String TIFF    = "tiff";

  // For manually disabling the use of the texture rectangle
  // extensions so you know the texture target is GL_TEXTURE_2D; this
  // is useful for shader writers (thanks to Chris Campbell for this
  // observation)
  private static boolean texRectEnabled = true;

  //---------------------------------------------------------------------------
  // Global disabling of texture rectangle extension
  //

  /** Toggles the use of the GL_ARB_texture_rectangle extension by the
      TextureIO classes. By default, on hardware supporting this
      extension, the TextureIO classes may use the
      GL_ARB_texture_rectangle extension for non-power-of-two
      textures. (If the hardware supports the
      GL_ARB_texture_non_power_of_two extension, that one is
      preferred.) In some situations, for example when writing
      shaders, it is advantageous to force the texture target to
      always be GL_TEXTURE_2D in order to have one version of the
      shader, even at the expense of texture memory in the case where
      NPOT textures are not supported. This method allows the use of
      the GL_ARB_texture_rectangle extension to be turned off globally
      for this purpose. The default is that the use of the extension
      is enabled. */
  public static void setTexRectEnabled(boolean enabled) {
    texRectEnabled = enabled;
  }

  /** Indicates whether the GL_ARB_texture_rectangle extension is
      allowed to be used for non-power-of-two textures; see {@link
      #setTexRectEnabled setTexRectEnabled}. */
  public static boolean isTexRectEnabled() {
    return texRectEnabled;
  }

}
