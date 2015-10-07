/**
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl;

/**
 * Class holding OpenGL extension strings, commonly used by JOGL's implementation.
 */
public class GLExtensions {
  public static final String VERSION_1_2                     = "GL_VERSION_1_2";
  public static final String VERSION_1_4                     = "GL_VERSION_1_4";
  public static final String VERSION_1_5                     = "GL_VERSION_1_5";
  public static final String VERSION_2_0                     = "GL_VERSION_2_0";

  public static final String GL_KHR_debug                    = "GL_KHR_debug";
  public static final String ARB_debug_output                = "GL_ARB_debug_output";
  public static final String AMD_debug_output                = "GL_AMD_debug_output";

  public static final String ARB_framebuffer_object          = "GL_ARB_framebuffer_object";
  public static final String OES_framebuffer_object          = "GL_OES_framebuffer_object";
  public static final String EXT_framebuffer_object          = "GL_EXT_framebuffer_object";
  public static final String EXT_framebuffer_blit            = "GL_EXT_framebuffer_blit";
  public static final String EXT_framebuffer_multisample     = "GL_EXT_framebuffer_multisample";
  public static final String EXT_packed_depth_stencil        = "GL_EXT_packed_depth_stencil";
  public static final String OES_depth24                     = "GL_OES_depth24";
  public static final String OES_depth32                     = "GL_OES_depth32";
  public static final String OES_packed_depth_stencil        = "GL_OES_packed_depth_stencil";
  public static final String NV_fbo_color_attachments        = "GL_NV_fbo_color_attachments";

  public static final String ARB_ES2_compatibility           = "GL_ARB_ES2_compatibility";
  public static final String ARB_ES3_compatibility           = "GL_ARB_ES3_compatibility";
  public static final String ARB_ES3_1_compatibility         = "GL_ARB_ES3_1_compatibility";
  public static final String ARB_ES3_2_compatibility         = "GL_ARB_ES3_2_compatibility";

  public static final String EXT_abgr                        = "GL_EXT_abgr";
  public static final String OES_rgb8_rgba8                  = "GL_OES_rgb8_rgba8";
  public static final String OES_stencil1                    = "GL_OES_stencil1";
  public static final String OES_stencil4                    = "GL_OES_stencil4";
  public static final String OES_stencil8                    = "GL_OES_stencil8";
  public static final String APPLE_float_pixels              = "GL_APPLE_float_pixels";

  public static final String ARB_texture_non_power_of_two    = "GL_ARB_texture_non_power_of_two";
  public static final String ARB_texture_rectangle           = "GL_ARB_texture_rectangle";
  public static final String EXT_texture_rectangle           = "GL_EXT_texture_rectangle";
  public static final String NV_texture_rectangle            = "GL_NV_texture_rectangle";
  public static final String EXT_texture_format_BGRA8888     = "GL_EXT_texture_format_BGRA8888";
  public static final String IMG_texture_format_BGRA8888     = "GL_IMG_texture_format_BGRA8888";
  public static final String EXT_texture_compression_s3tc    = "GL_EXT_texture_compression_s3tc";
  public static final String NV_texture_compression_vtc      = "GL_NV_texture_compression_vtc";
  public static final String SGIS_generate_mipmap            = "GL_SGIS_generate_mipmap";
  public static final String OES_read_format                 = "GL_OES_read_format";
  public static final String OES_single_precision            = "GL_OES_single_precision";
  public static final String OES_EGL_image_external          = "GL_OES_EGL_image_external";
  /** Required to be requested for OpenGL ES 2.0, <i>not</i> ES 3.0! */
  public static final String OES_standard_derivatives        = "GL_OES_standard_derivatives";

  public static final String ARB_gpu_shader_fp64             = "GL_ARB_gpu_shader_fp64";
  public static final String ARB_shader_objects              = "GL_ARB_shader_objects";
  public static final String ARB_geometry_shader4            = "GL_ARB_geometry_shader4";

  //
  // Aliased GLX/WGL/.. extensions
  //

  public static final String ARB_pixel_format                = "GL_ARB_pixel_format";
  public static final String ARB_pbuffer                     = "GL_ARB_pbuffer";
}
