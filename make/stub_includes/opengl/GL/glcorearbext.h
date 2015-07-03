#ifndef __glcorearbext_h_
#define __glcorearbext_h_ 1

#ifdef __cplusplus
extern "C" {
#endif

/*
** Copyright (c) 2010 JogAmp Developer Team
** 
** Permission is hereby granted, free of charge, to any person obtaining a
** copy of this software and/or associated documentation files (the
** "Materials"), to deal in the Materials without restriction, including
** without limitation the rights to use, copy, modify, merge, publish,
** distribute, sublicense, and/or sell copies of the Materials, and to
** permit persons to whom the Materials are furnished to do so, subject to
** the following conditions:
** 
** The above copyright notice and this permission notice shall be included
** in all copies or substantial portions of the Materials.
** 
** THE MATERIALS ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
** EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
** MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
** IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
** CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
** TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
** MATERIALS OR THE USE OR OTHER DEALINGS IN THE MATERIALS.
*/

/**
 * This header files contains additional extensions not covered by the 
 * 'official' khronos glcorearb.h
 */

/*
 * ------------------------------------------------
 * Everything here and below was added manually
 * to the version of glext.h obtained from:
 * http://oss.sgi.com/projects/ogl-sample/registry/index.html
 * ------------------------------------------------
 *
 * Structure is:
 *    #ifndef GL_EXTENSION_NAME
 *    Add DEFINES here
 *    #endif
 *    #ifndef GL_EXTENSION_NAME
 *    Add TYPEDEFS here
 *    #endif
 *    #ifndef GL_EXTENSION_NAME
 *    #define GL_EXTENSION_NAME 1
 *    #ifdef GL_GLEXT_PROTOTYPES
 *    Add FUNCTION DECLARATIONS here
 *    #endif
 *    FUNCTION POINTER DECLARATIONS NOT NEEDED
 *    #endif
 */
  
/**
 * 47. http://www.opengl.org/registry/specs/ARB/geometry_shader4.txt
 */
#ifndef GL_ARB_geometry_shader4
#define GL_LINES_ADJACENCY_ARB            0x000A
#define GL_LINE_STRIP_ADJACENCY_ARB       0x000B
#define GL_TRIANGLES_ADJACENCY_ARB        0x000C
#define GL_TRIANGLE_STRIP_ADJACENCY_ARB   0x000D
#define GL_PROGRAM_POINT_SIZE_ARB         0x8642
#define GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS_ARB 0x8C29
#define GL_FRAMEBUFFER_ATTACHMENT_LAYERED_ARB 0x8DA7
#define GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS_ARB 0x8DA8
#define GL_FRAMEBUFFER_INCOMPLETE_LAYER_COUNT_ARB 0x8DA9
#define GL_GEOMETRY_SHADER_ARB            0x8DD9
#define GL_GEOMETRY_VERTICES_OUT_ARB      0x8DDA
#define GL_GEOMETRY_INPUT_TYPE_ARB        0x8DDB
#define GL_GEOMETRY_OUTPUT_TYPE_ARB       0x8DDC
#define GL_MAX_GEOMETRY_VARYING_COMPONENTS_ARB 0x8DDD
#define GL_MAX_VERTEX_VARYING_COMPONENTS_ARB 0x8DDE
#define GL_MAX_GEOMETRY_UNIFORM_COMPONENTS_ARB 0x8DDF
#define GL_MAX_GEOMETRY_OUTPUT_VERTICES_ARB 0x8DE0
#define GL_MAX_GEOMETRY_TOTAL_OUTPUT_COMPONENTS_ARB 0x8DE1
#ifndef GL_MAX_VARYING_COMPONENTS
#define GL_MAX_VARYING_COMPONENTS         0x8B4B
#endif
#ifndef GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER
#define GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER 0x8CD4
#endif
#endif
#ifndef GL_ARB_geometry_shader4
#define GL_ARB_geometry_shader4 1
#ifdef GL_GLEXT_PROTOTYPES
GLAPI void APIENTRY glProgramParameteriARB (GLuint program, GLenum pname, GLint value);
GLAPI void APIENTRY glFramebufferTextureARB (GLenum target, GLenum attachment, GLuint texture, GLint level);
GLAPI void APIENTRY glFramebufferTextureLayerARB (GLenum target, GLenum attachment, GLuint texture, GLint level, GLint layer);
GLAPI void APIENTRY glFramebufferTextureFaceARB (GLenum target, GLenum attachment, GLuint texture, GLint level, GLenum face);
#endif /* GL_GLEXT_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

// #187
#ifndef GL_EXT_texture_filter_anisotropic
#define GL_TEXTURE_MAX_ANISOTROPY_EXT     0x84FE
#define GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT 0x84FF
#endif
#ifndef GL_EXT_texture_filter_anisotropic
#define GL_EXT_texture_filter_anisotropic 1
#endif

// #363 http://www.opengl.org/registry/specs/AMD/vertex_shader_tessellator.txt
#ifndef GL_AMD_vertex_shader_tessellator
#define GL_SAMPLER_BUFFER_AMD                0x9001
#define GL_INT_SAMPLER_BUFFER_AMD            0x9002
#define GL_UNSIGNED_INT_SAMPLER_BUFFER_AMD   0x9003
#define GL_DISCRETE_AMD                      0x9006
#define GL_CONTINUOUS_AMD                    0x9007
#define GL_TESSELLATION_MODE_AMD             0x9004
#define GL_TESSELLATION_FACTOR_AMD           0x9005
#endif
#ifndef GL_AMD_vertex_shader_tessellator
#define GL_AMD_vertex_shader_tessellator 1
#ifdef GL_GLEXT_PROTOTYPES
GLAPI void APIENTRY glTessellationFactorAMD(GLfloat factor);
GLAPI void APIENTRY glTessellationModeAMD(GLenum mode);
#endif /* GL_GLEXT_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

// #379 http://www.opengl.org/registry/specs/NV/shader_buffer_load.txt
#ifndef GL_NV_shader_buffer_load
#define GL_BUFFER_GPU_ADDRESS_NV          0x8F1D
#define GL_GPU_ADDRESS_NV                 0x8F34
#define GL_MAX_SHADER_BUFFER_ADDRESS_NV   0x8F35
#endif
#ifndef GL_NV_shader_buffer_load
#define GL_NV_shader_buffer_load 1
#ifdef GL_GLEXT_PROTOTYPES
GLAPI void APIENTRY glMakeBufferResidentNV (GLenum target, GLenum access);
GLAPI void APIENTRY glMakeBufferNonResidentNV (GLenum target);
GLAPI GLboolean APIENTRY glIsBufferResidentNV (GLenum target);
GLAPI void APIENTRY glMakeNamedBufferResidentNV (GLuint buffer, GLenum access);
GLAPI void APIENTRY glMakeNamedBufferNonResidentNV (GLuint buffer);
GLAPI GLboolean APIENTRY glIsNamedBufferResidentNV (GLuint buffer);
GLAPI void APIENTRY glGetBufferParameterui64vNV (GLenum target, GLenum pname, GLuint64EXT *params);
GLAPI void APIENTRY glGetNamedBufferParameterui64vNV (GLuint buffer, GLenum pname, GLuint64EXT *params);
GLAPI void APIENTRY glGetIntegerui64vNV (GLenum value, GLuint64EXT *result);
GLAPI void APIENTRY glUniformui64NV (GLint location, GLuint64EXT value);
GLAPI void APIENTRY glUniformui64vNV (GLint location, GLsizei count, const GLuint64EXT *value);
GLAPI void APIENTRY glGetUniformui64vNV (GLuint program, GLint location, GLuint64EXT *params);
GLAPI void APIENTRY glProgramUniformui64NV (GLuint program, GLint location, GLuint64EXT value);
GLAPI void APIENTRY glProgramUniformui64vNV (GLuint program, GLint location, GLsizei count, const GLuint64EXT *value);
#endif /* GL_GLEXT_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

// #380 http://www.opengl.org/registry/specs/NV/vertex_buffer_unified_memory.txt
#ifndef GL_NV_vertex_buffer_unified_memory
#define GL_VERTEX_ATTRIB_ARRAY_UNIFIED_NV 0x8F1E
#define GL_ELEMENT_ARRAY_UNIFIED_NV       0x8F1F
#define GL_VERTEX_ATTRIB_ARRAY_ADDRESS_NV 0x8F20
#define GL_VERTEX_ARRAY_ADDRESS_NV        0x8F21
#define GL_NORMAL_ARRAY_ADDRESS_NV        0x8F22
#define GL_COLOR_ARRAY_ADDRESS_NV         0x8F23
#define GL_INDEX_ARRAY_ADDRESS_NV         0x8F24
#define GL_TEXTURE_COORD_ARRAY_ADDRESS_NV 0x8F25
#define GL_EDGE_FLAG_ARRAY_ADDRESS_NV     0x8F26
#define GL_SECONDARY_COLOR_ARRAY_ADDRESS_NV 0x8F27
#define GL_FOG_COORD_ARRAY_ADDRESS_NV     0x8F28
#define GL_ELEMENT_ARRAY_ADDRESS_NV       0x8F29
#define GL_VERTEX_ATTRIB_ARRAY_LENGTH_NV  0x8F2A
#define GL_VERTEX_ARRAY_LENGTH_NV         0x8F2B
#define GL_NORMAL_ARRAY_LENGTH_NV         0x8F2C
#define GL_COLOR_ARRAY_LENGTH_NV          0x8F2D
#define GL_INDEX_ARRAY_LENGTH_NV          0x8F2E
#define GL_TEXTURE_COORD_ARRAY_LENGTH_NV  0x8F2F
#define GL_EDGE_FLAG_ARRAY_LENGTH_NV      0x8F30
#define GL_SECONDARY_COLOR_ARRAY_LENGTH_NV 0x8F31
#define GL_FOG_COORD_ARRAY_LENGTH_NV      0x8F32
#define GL_ELEMENT_ARRAY_LENGTH_NV        0x8F33
#define GL_DRAW_INDIRECT_UNIFIED_NV       0x8F40
#define GL_DRAW_INDIRECT_ADDRESS_NV       0x8F41
#define GL_DRAW_INDIRECT_LENGTH_NV        0x8F42
#endif
#ifndef GL_NV_vertex_buffer_unified_memory
#define GL_NV_vertex_buffer_unified_memory 1
#ifdef GL_GLEXT_PROTOTYPES
GLAPI GLboolean APIENTRY glIsEnabled( GLenum cap );     // extra requirement in core GL3
GLAPI void APIENTRY glEnableClientState( GLenum cap );  // extra requirement in core GL3
GLAPI void APIENTRY glDisableClientState( GLenum cap ); // extra requirement in core GL3
GLAPI void APIENTRY glBufferAddressRangeNV(GLenum pname, GLuint index, GLuint64 address, GLsizeiptr length);
GLAPI void APIENTRY glBufferAddressRangeNV (GLenum pname, GLuint index, GLuint64EXT address, GLsizeiptr length);
GLAPI void APIENTRY glVertexFormatNV (GLint size, GLenum type, GLsizei stride);
GLAPI void APIENTRY glNormalFormatNV (GLenum type, GLsizei stride);
GLAPI void APIENTRY glColorFormatNV (GLint size, GLenum type, GLsizei stride);
GLAPI void APIENTRY glIndexFormatNV (GLenum type, GLsizei stride);
GLAPI void APIENTRY glTexCoordFormatNV (GLint size, GLenum type, GLsizei stride);
GLAPI void APIENTRY glEdgeFlagFormatNV (GLsizei stride);
GLAPI void APIENTRY glSecondaryColorFormatNV (GLint size, GLenum type, GLsizei stride);
GLAPI void APIENTRY glFogCoordFormatNV (GLenum type, GLsizei stride);
GLAPI void APIENTRY glVertexAttribFormatNV (GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride);
GLAPI void APIENTRY glVertexAttribIFormatNV (GLuint index, GLint size, GLenum type, GLsizei stride);
GLAPI void APIENTRY glGetIntegerui64i_vNV (GLenum value, GLuint index, GLuint64EXT *result);
#endif /* GL_GLEXT_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

// #395
#ifndef GL_AMD_debug_output
#define GL_MAX_DEBUG_LOGGED_MESSAGES_AMD  0x9144
#define GL_DEBUG_LOGGED_MESSAGES_AMD      0x9145
#define GL_DEBUG_SEVERITY_HIGH_AMD        0x9146
#define GL_DEBUG_SEVERITY_MEDIUM_AMD      0x9147
#define GL_DEBUG_SEVERITY_LOW_AMD         0x9148
#define GL_DEBUG_CATEGORY_API_ERROR_AMD   0x9149
#define GL_DEBUG_CATEGORY_WINDOW_SYSTEM_AMD 0x914A
#define GL_DEBUG_CATEGORY_DEPRECATION_AMD 0x914B
#define GL_DEBUG_CATEGORY_UNDEFINED_BEHAVIOR_AMD 0x914C
#define GL_DEBUG_CATEGORY_PERFORMANCE_AMD 0x914D
#define GL_DEBUG_CATEGORY_SHADER_COMPILER_AMD 0x914E
#define GL_DEBUG_CATEGORY_APPLICATION_AMD 0x914F
#define GL_DEBUG_CATEGORY_OTHER_AMD       0x9150
#endif
#ifndef GL_AMD_debug_output
typedef void (APIENTRY *GLDEBUGPROCAMD)(GLuint id,GLenum category,GLenum severity,GLsizei length,const GLchar *message,GLvoid *userParam);
#endif
#ifndef GL_AMD_debug_output
#define GL_AMD_debug_output 1
#ifdef GL_GLEXT_PROTOTYPES
GLAPI void APIENTRY glDebugMessageEnableAMD (GLenum category, GLenum severity, GLsizei count, const GLuint *ids, GLboolean enabled);
GLAPI void APIENTRY glDebugMessageInsertAMD (GLenum category, GLenum severity, GLuint id, GLsizei length, const GLchar *buf);
GLAPI void APIENTRY glDebugMessageCallbackAMD (GLDEBUGPROCAMD callback, GLvoid *userParam);
GLAPI GLuint APIENTRY glGetDebugMessageLogAMD (GLuint count, GLsizei bufsize, GLenum *categories, GLuint *severities, GLuint *ids, GLsizei *lengths, GLchar *message);
#endif /* GL_GLEXT_PROTOTYPES */
typedef void (APIENTRYP PFNGLDEBUGMESSAGEENABLEAMDPROC) (GLenum category, GLenum severity, GLsizei count, const GLuint *ids, GLboolean enabled);
typedef void (APIENTRYP PFNGLDEBUGMESSAGEINSERTAMDPROC) (GLenum category, GLenum severity, GLuint id, GLsizei length, const GLchar *buf);
typedef void (APIENTRYP PFNGLDEBUGMESSAGECALLBACKAMDPROC) (GLDEBUGPROCAMD callback, GLvoid *userParam);
typedef GLuint (APIENTRYP PFNGLGETDEBUGMESSAGELOGAMDPROC) (GLuint count, GLsizei bufsize, GLenum *categories, GLuint *severities, GLuint *ids, GLsizei *lengths, GLchar *message);
#endif

// #401
#ifndef GL_AMD_depth_clamp_separate
#define GL_DEPTH_CLAMP_NEAR_AMD           0x901E
#define GL_DEPTH_CLAMP_FAR_AMD            0x901F
#endif
#ifndef GL_AMD_depth_clamp_separate
#define GL_AMD_depth_clamp_separate 1
#endif

// #402
#ifndef GL_EXT_texture_sRGB_decode
#define GL_TEXTURE_SRGB_DECODE_EXT        0x8A48
#define GL_DECODE_EXT                     0x8A49
#define GL_SKIP_DECODE_EXT                0x8A4A
#endif
#ifndef GL_EXT_texture_sRGB_decode
#define GL_EXT_texture_sRGB_decode 1
#endif

// #403
#ifndef GL_NV_texture_multisample
#define GL_TEXTURE_COVERAGE_SAMPLES_NV    0x9045
#define GL_TEXTURE_COLOR_SAMPLES_NV       0x9046
#endif
#ifndef GL_NV_texture_multisample
#define GL_NV_texture_multisample 1
#ifdef GL_GLEXT_PROTOTYPES
GLAPI void APIENTRY glTexImage2DMultisampleCoverageNV (GLenum target, GLsizei coverageSamples, GLsizei colorSamples, GLint internalFormat, GLsizei width, GLsizei height, GLboolean fixedSampleLocations);
GLAPI void APIENTRY glTexImage3DMultisampleCoverageNV (GLenum target, GLsizei coverageSamples, GLsizei colorSamples, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLboolean fixedSampleLocations);
GLAPI void APIENTRY glTextureImage2DMultisampleNV (GLuint texture, GLenum target, GLsizei samples, GLint internalFormat, GLsizei width, GLsizei height, GLboolean fixedSampleLocations);
GLAPI void APIENTRY glTextureImage3DMultisampleNV (GLuint texture, GLenum target, GLsizei samples, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLboolean fixedSampleLocations);
GLAPI void APIENTRY glTextureImage2DMultisampleCoverageNV (GLuint texture, GLenum target, GLsizei coverageSamples, GLsizei colorSamples, GLint internalFormat, GLsizei width, GLsizei height, GLboolean fixedSampleLocations);
GLAPI void APIENTRY glTextureImage3DMultisampleCoverageNV (GLuint texture, GLenum target, GLsizei coverageSamples, GLsizei colorSamples, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLboolean fixedSampleLocations);
#endif /* GL_GLEXT_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

// #404
#ifndef GL_AMD_blend_minmax_factor
#define GL_FACTOR_MIN_AMD                 0x901C
#define GL_FACTOR_MAX_AMD                 0x901D
#endif
#ifndef GL_AMD_blend_minmax_factor
#define GL_AMD_blend_minmax_factor 1
#endif

// #405
#ifndef GL_AMD_sample_positions
#define GL_SUBSAMPLE_DISTANCE_AMD         0x883F
#endif
#ifndef GL_AMD_sample_positions
#define GL_AMD_sample_positions 1
#ifdef GL_GLEXT_PROTOTYPES
GLAPI void APIENTRY glSetMultisamplefvAMD (GLenum pname, GLuint index, const GLfloat *val);
#endif /* GL_GLEXT_PROTOTYPES */
typedef void (APIENTRYP PFNGLSETMULTISAMPLEFVAMDPROC) (GLenum pname, GLuint index, const GLfloat *val);
#endif

// #406
#ifndef GL_EXT_x11_sync_object
#define GL_SYNC_X11_FENCE_EXT             0x90E1
#endif
#ifndef GL_EXT_x11_sync_object
#define GL_EXT_x11_sync_object 1
#ifdef GL_GLEXT_PROTOTYPES
GLAPI GLsync APIENTRY glImportSyncEXT (GLenum external_sync_type, GLintptr external_sync, GLbitfield flags);
#endif /* GL_GLEXT_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

// #408
#ifndef GL_AMD_multi_draw_indirect
#endif
#ifndef GL_AMD_multi_draw_indirect
#define GL_AMD_multi_draw_indirect 1
#ifdef GL_GLEXT_PROTOTYPES
GLAPI void APIENTRY glMultiDrawArraysIndirectAMD (GLenum mode, const GLvoid *indirect, GLsizei primcount, GLsizei stride);
GLAPI void APIENTRY glMultiDrawElementsIndirectAMD (GLenum mode, GLenum type, const GLvoid *indirect, GLsizei primcount, GLsizei stride);
#endif /* GL_GLEXT_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

// #409
#ifndef GL_EXT_framebuffer_multisample_blit_scaled
#define GL_SCALED_RESOLVE_FASTEST_EXT     0x90BA
#define GL_SCALED_RESOLVE_NICEST_EXT      0x90BB
#endif
#ifndef GL_EXT_framebuffer_multisample_blit_scaled
#define GL_EXT_framebuffer_multisample_blit_scaled 1
#endif

// #410 GL_NV_path_rendering ?

// #411
#ifndef GL_AMD_pinned_memory
#define GL_EXTERNAL_VIRTUAL_MEMORY_BUFFER_AMD 0x9160
#endif
#ifndef GL_AMD_pinned_memory
#define GL_AMD_pinned_memory 1
#endif

// #413
#ifndef GL_AMD_stencil_operation_extended
#define GL_SET_AMD                        0x874A
#define GL_REPLACE_VALUE_AMD              0x874B
#define GL_STENCIL_OP_VALUE_AMD           0x874C
#define GL_STENCIL_BACK_OP_VALUE_AMD      0x874D
#endif
#ifndef GL_AMD_stencil_operation_extended
#define GL_AMD_stencil_operation_extended 1
#ifdef GL_GLEXT_PROTOTYPES
GLAPI void APIENTRY glStencilOpValueAMD (GLenum face, GLuint value);
#endif /* GL_GLEXT_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

#ifdef __cplusplus
}
#endif

#endif /* __glcorearbext_h_ */

