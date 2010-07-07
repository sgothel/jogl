/*
 * Copyright (c) 2005-2007 NVIDIA Corporation.  All rights reserved.
 *
 * NVIDIA Corporation and its licensors retain all intellectual property
 * and proprietary rights in and to this software, related documentation
 * and any modifications thereto.  Any use, reproduction, disclosure or
 * distribution of this software and related documentation without an express
 * license agreement from NVIDIA Corporation is strictly prohibited.
 */

#ifndef __gl2ext_h_
#define __gl2ext_h_

#ifndef __gl2_h_
#   include <GLES2/gl2.h>
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*
** License Applicability. Except to the extent portions of this file are
** made subject to an alternative license as permitted in the SGI Free
** Software License B, Version 1.0 (the "License"), the contents of this
** file are subject only to the provisions of the License. You may not use
** this file except in compliance with the License. You may obtain a copy
** of the License at Silicon Graphics, Inc., attn: Legal Services, 1600
** Amphitheatre Parkway, Mountain View, CA 94043-1351, or at:
**
** http://oss.sgi.com/projects/FreeB
**
** Note that, as provided in the License, the Software is distributed on an
** "AS IS" basis, with ALL EXPRESS AND IMPLIED WARRANTIES AND CONDITIONS
** DISCLAIMED, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES AND
** CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A
** PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
**
** Original Code. The Original Code is: OpenGL Sample Implementation,
** Version 1.2.1, released January 26, 2000, developed by Silicon Graphics,
** Inc. The Original Code is Copyright (c) 1991-2000 Silicon Graphics, Inc.
** Copyright in any portions created by third parties is as indicated
** elsewhere herein. All Rights Reserved.
**
** Additional Notice Provisions: The application programming interfaces
** established by SGI in conjunction with the Original Code are The
** OpenGL(R) Graphics System: A Specification (Version 1.2.1), released
** April 1, 1999; The OpenGL(R) Graphics System Utility Library (Version
** 1.3), released November 4, 1998; and OpenGL(R) Graphics with the X
** Window System(R) (Version 1.3), released October 19, 1998. This software
** was created using the OpenGL(R) version 1.2.1 Sample Implementation
** published by SGI, but has not been independently verified as being
** compliant with the OpenGL(R) version 1.2.1 Specification.
*/

#ifndef GL_APIENTRYP
#   define GL_APIENTRYP GL_APIENTRY*
#endif

#define GL_GLEXT_PROTOTYPES

#define GL_NVIDIA_PLATFORM_BINARY_NV                            0x890B

#ifndef GL_OES_EGL_image
/** sgothel: wrong defines and/or numbers:
 *
#define GL_TEXTURE_2D_OES                     0x1
#define GL_TEXTURE_CUBE_MAP_POSITIVE_X_OES    0x3
#define GL_TEXTURE_CUBE_MAP_NEGATIVE_X_OES    0x4
#define GL_TEXTURE_CUBE_MAP_POSITIVE_Y_OES    0x5
#define GL_TEXTURE_CUBE_MAP_NEGATIVE_Y_OES    0x6
#define GL_TEXTURE_CUBE_MAP_POSITIVE_Z_OES    0x7
#define GL_TEXTURE_CUBE_MAP_NEGATIVE_Z_OES    0x8
#define GL_RENDERBUFFER_OES                   0x9
#define GL_TEXTURE_RECTANGLE_NV_OES           0xb
 */
#endif /*  GL_OES_EGL_image */

#ifndef GL_OES_EGL_image
typedef void *GLeglImageOES;
#endif /*  GL_OES_EGL_image */

#ifndef GL_OES_EGL_image
#define GL_OES_EGL_image                  1
GL_APICALL void GL_APIENTRY glEGLImageTargetTexture2DOES(GLenum target, GLeglImageOES image);
#ifdef GL_GLEXT_PROTOTYPES
typedef void  (GL_APIENTRYP PFNGLEGLIMAGETARGETTEXTURE2DOESPROC) (GLenum target, GLeglImageOES image);
#endif
#endif


/*------------------------------------------------------------------------*
 * OES extension tokens
 *------------------------------------------------------------------------*/

#ifndef GL_OES_texture_half_float
#define GL_OES_texture_half_float 1
#define GL_HALF_FLOAT_OES                                       0x8D61
#endif

/* GL_OES_mapbuffer */
#ifndef GL_OES_mapbuffer
#define GL_BUFFER_MAPPED_OES                                    0x88BC
#define GL_BUFFER_MAP_POINTER_OES                               0x88BD
#endif

/* GL_OES_rgb8_rgba8 */
#ifndef GL_OES_rgb8_rgba8
#define GL_RGB8_OES                                             0x8051
#define GL_RGBA8_OES                                            0x8058
#endif

/* GL_OES_mapbuffer */
#ifndef GL_OES_mapbuffer
#define GL_OES_mapbuffer 1
#ifdef GL_GLEXT_PROTOTYPES
GL_APICALL void* GL_APIENTRY glMapBufferOES (GLenum target, GLenum access);
GL_APICALL GLboolean GL_APIENTRY glUnmapBufferOES (GLenum target);
GL_APICALL void GL_APIENTRY glGetBufferPointerivNV (GLenum target, GLenum pname, void **params );
#endif
typedef void* (GL_APIENTRYP PFNGLMAPBUFFEROESPROC) (GLenum target, GLenum access);
typedef GLboolean (GL_APIENTRYP PFNGLUNMAPBUFFEROESPROC) (GLenum target);
typedef void  (GL_APIENTRYP PFNGLGETBUFFERPOINTERIVNVPROC) (GLenum target, GLenum pname, void **params );
#endif

/* GL_OES_rgb8_rgba8 */
#ifndef GL_OES_rgb8_rgba8
#define GL_OES_rgb8_rgba8 1
#endif

/*------------------------------------------------------------------------*
 * EXT extension tokens
 *------------------------------------------------------------------------*/
 
/* GL_EXT_framebuffer_mixed_formats */
#ifndef GL_EXT_framebuffer_mixed_formats
#define GL_EXT_framebuffer_mixed_formats 1
#endif

/* GL_EXT_packed_float */
#ifndef GL_EXT_packed_float
#define GL_R11F_G11F_B10F_EXT                           0x8C3A
#define GL_UNSIGNED_INT_10F_11F_11F_REV_EXT             0x8C3B
#define GL_RGBA_SIGNED_COMPONENTS_EXT                   0x8C3C
#endif

/* GL_EXT_texture_array */
#ifndef GL_EXT_texture_array
#define GL_TEXTURE_2D_ARRAY_EXT           0x8C1A
#define GL_SAMPLER_2D_ARRAY_EXT           0x8DC1
#define GL_TEXTURE_BINDING_2D_ARRAY_EXT   0x8C1D
#define GL_MAX_ARRAY_TEXTURE_LAYERS_EXT   0x88FF
#define GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER_EXT 0x8CD4
#endif


/* GL_EXT_texture_compression_dxt1 */
#ifndef GL_EXT_texture_compression_dxt1
#define GL_COMPRESSED_RGB_S3TC_DXT1_EXT   0x83F0
#define GL_COMPRESSED_RGBA_S3TC_DXT1_EXT  0x83F1
#endif

/* GL_EXT_texture_compression_s3tc */
#ifndef GL_EXT_texture_compression_s3tc
/* GL_COMPRESSED_RGB_S3TC_DXT1_EXT defined in GL_EXT_texture_compression_dxt1 already. */
/* GL_COMPRESSED_RGBA_S3TC_DXT1_EXT defined in GL_EXT_texture_compression_dxt1 already. */
#define GL_COMPRESSED_RGBA_S3TC_DXT3_EXT  0x83F2
#define GL_COMPRESSED_RGBA_S3TC_DXT5_EXT  0x83F3
#endif

/* GL_EXT_texture_filter_anisotropic */
#ifndef GL_EXT_texture_filter_anisotropic
#define GL_TEXTURE_MAX_ANISOTROPY_EXT                           0x84FE
#define GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT                       0x84FF
#endif


/* GL_EXT_texture_compression_dxt1 */
#ifndef GL_EXT_texture_compression_dxt1
#define GL_EXT_texture_compression_dxt1 1
#endif

/* GL_EXT_texture_compression_s3tc */
#ifndef GL_EXT_texture_compression_s3tc
#define GL_EXT_texture_compression_s3tc 1
#endif

/* GL_EXT_texture_filter_anisotropic */
#ifndef GL_EXT_texture_filter_anisotropic
#define GL_EXT_texture_filter_anisotropic 1
#endif

#define GL_TEXTURE_MAX_LEVEL_NV         0x813D

/* GL_NV_coverage_sample */
#ifndef GL_NV_coverage_sample
#define GL_COVERAGE_COMPONENT_NV          0x8522
#define GL_COVERAGE_COMPONENT4_NV         0x8523
#define GL_COVERAGE_ATTACHMENT_NV         0x8527
#define GL_COVERAGE_BUFFER_BIT_NV         0x8000
#define GL_COVERAGE_BUFFERS_NV            0x8528
#define GL_COVERAGE_SAMPLES_NV            0x8529
#define GL_COVERAGE_ALL_FRAGMENTS_NV      0x8524
#define GL_COVERAGE_EDGE_FRAGMENTS_NV     0x8525
#define GL_COVERAGE_AUTOMATIC_NV          0x8526
#endif


/* GL_NV_framebuffer_vertex_attrib_array */
#ifndef GL_NV_framebuffer_vertex_attrib_array
#define GL_FRAMEBUFFER_ATTACHABLE_NV                                  0x852A
#define GL_VERTEX_ATTRIB_ARRAY_NV                                     0x852B
#define GL_FRAMEBUFFER_ATTACHMENT_VERTEX_ATTRIB_ARRAY_SIZE_NV         0x852C
#define GL_FRAMEBUFFER_ATTACHMENT_VERTEX_ATTRIB_ARRAY_TYPE_NV         0x852D
#define GL_FRAMEBUFFER_ATTACHMENT_VERTEX_ATTRIB_ARRAY_NORMALIZED_NV   0x852E
#define GL_FRAMEBUFFER_ATTACHMENT_VERTEX_ATTRIB_ARRAY_OFFSET_NV       0x852F
#define GL_FRAMEBUFFER_ATTACHMENT_VERTEX_ATTRIB_ARRAY_WIDTH_NV        0x8530
#define GL_FRAMEBUFFER_ATTACHMENT_VERTEX_ATTRIB_ARRAY_STRIDE_NV       0x8531
#define GL_FRAMEBUFFER_ATTACHMENT_VERTEX_ATTRIB_ARRAY_HEIGHT_NV       0x8532
#endif


/* GL_NV_framebuffer_vertex_attrib_array */
#ifndef GL_NV_framebuffer_vertex_attrib_array
#define GL_NV_framebuffer_vertex_attrib_array 1
#ifdef GL_GLEXT_PROTOTYPES
GL_APICALL void GL_APIENTRY glFramebufferVertexAttribArrayNV (GLenum target, GLenum attachment, GLenum buffertarget, GLuint bufferobject, GLint size, GLenum type, GLboolean normalized, GLintptr offset, GLsizeiptr width, GLsizeiptr height, GLsizei stride);
#endif
typedef void (GL_APIENTRYP PFNGLFRAMEBUFFERVERTEXATTRIBARRAYNVPROC) (GLenum target, GLenum attachment, GLenum buffertarget, GLuint bufferobject, GLint size, GLenum type, GLboolean normalized, GLintptr offset, GLsizeiptr width, GLsizeiptr height, GLsizei stride);
#endif

/* GL_NV_coverage_sample */
#ifndef GL_NV_coverage_sample
#define GL_NV_coverage_sample 1
#ifdef GL_GLEXT_PROTOTYPES
GL_APICALL void GL_APIENTRY glCoverageMaskNV (GLboolean mask);
GL_APICALL void GL_APIENTRY glCoverageOperationNV (GLenum operation);
#endif
typedef void (GL_APIENTRYP PFNGLCOVERAGEMASKNVPROC) (GLboolean mask);
typedef void (GL_APIENTRYP PFNGLCOVERAGEOPERATIONNVPROC) (GLenum operation);
#endif



/*------------------------------------------------------------------------*
 * NV extension tokens
 *------------------------------------------------------------------------*/


#ifdef __cplusplus
}
#endif

#endif /* __gl2ext_h_ */
