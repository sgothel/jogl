#ifndef __gl3ext_h_
#define __gl3ext_h_

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
 *    #ifdef GL_GL3EXT_PROTOTYPES
 *    Add FUNCTION DECLARATIONS here
 *    #endif
 *    FUNCTION POINTER DECLARATIONS NOT NEEDED
 *    #endif
 */
  

/**
 * http://www.opengl.org/registry/specs/ARB/geometry_shader4.txt
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
typedef void (APIENTRYP PFNGLPROGRAMPARAMETERIARBPROC) (GLuint program, GLenum pname, GLint value);
typedef void (APIENTRYP PFNGLFRAMEBUFFERTEXTUREARBPROC) (GLenum target, GLenum attachment, GLuint texture, GLint level);
typedef void (APIENTRYP PFNGLFRAMEBUFFERTEXTURELAYERARBPROC) (GLenum target, GLenum attachment, GLuint texture, GLint level, GLint layer);
typedef void (APIENTRYP PFNGLFRAMEBUFFERTEXTUREFACEARBPROC) (GLenum target, GLenum attachment, GLuint texture, GLint level, GLenum face);
#endif

/**
 * http://www.opengl.org/registry/specs/AMD/vertex_shader_tessellator.txt
 */
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
#ifdef GL_GL3EXT_PROTOTYPES
GLAPI void APIENTRY glTessellationFactorAMD(GLfloat factor);
GLAPI void APIENTRY glTessellationModeAMD(GLenum mode);
#endif
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

/**
 * http://www.opengl.org/registry/specs/NV/shader_buffer_load.txt
 */
#ifndef GL_NV_shader_buffer_load
#define GL_BUFFER_GPU_ADDRESS_NV          0x8F1D
#define GL_GPU_ADDRESS_NV                 0x8F34
#define GL_MAX_SHADER_BUFFER_ADDRESS_NV   0x8F35
#endif
#ifndef GL_NV_shader_buffer_load
#define GL_NV_shader_buffer_load 1
#ifdef GL_GL3EXT_PROTOTYPES
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
#endif /* GL_GL3EXT_PROTOTYPES */
typedef void (APIENTRYP PFNGLMAKEBUFFERRESIDENTNVPROC) (GLenum target, GLenum access);
typedef void (APIENTRYP PFNGLMAKEBUFFERNONRESIDENTNVPROC) (GLenum target);
typedef GLboolean (APIENTRYP PFNGLISBUFFERRESIDENTNVPROC) (GLenum target);
typedef void (APIENTRYP PFNGLMAKENAMEDBUFFERRESIDENTNVPROC) (GLuint buffer, GLenum access);
typedef void (APIENTRYP PFNGLMAKENAMEDBUFFERNONRESIDENTNVPROC) (GLuint buffer);
typedef GLboolean (APIENTRYP PFNGLISNAMEDBUFFERRESIDENTNVPROC) (GLuint buffer);
typedef void (APIENTRYP PFNGLGETBUFFERPARAMETERUI64VNVPROC) (GLenum target, GLenum pname, GLuint64EXT *params);
typedef void (APIENTRYP PFNGLGETNAMEDBUFFERPARAMETERUI64VNVPROC) (GLuint buffer, GLenum pname, GLuint64EXT *params);
typedef void (APIENTRYP PFNGLGETINTEGERUI64VNVPROC) (GLenum value, GLuint64EXT *result);
typedef void (APIENTRYP PFNGLUNIFORMUI64NVPROC) (GLint location, GLuint64EXT value);
typedef void (APIENTRYP PFNGLUNIFORMUI64VNVPROC) (GLint location, GLsizei count, const GLuint64EXT *value);
typedef void (APIENTRYP PFNGLGETUNIFORMUI64VNVPROC) (GLuint program, GLint location, GLuint64EXT *params);
typedef void (APIENTRYP PFNGLPROGRAMUNIFORMUI64NVPROC) (GLuint program, GLint location, GLuint64EXT value);
typedef void (APIENTRYP PFNGLPROGRAMUNIFORMUI64VNVPROC) (GLuint program, GLint location, GLsizei count, const GLuint64EXT *value);
#endif

/**
 * http://www.opengl.org/registry/specs/NV/vertex_buffer_unified_memory.txt
 */
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
#ifdef GL_GL3EXT_PROTOTYPES
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
#endif /* GL_GL3EXT_PROTOTYPES */
typedef void (APIENTRYP PFNGLBUFFERADDRESSRANGENVPROC) (GLenum pname, GLuint index, GLuint64EXT address, GLsizeiptr length);
typedef void (APIENTRYP PFNGLVERTEXFORMATNVPROC) (GLint size, GLenum type, GLsizei stride);
typedef void (APIENTRYP PFNGLNORMALFORMATNVPROC) (GLenum type, GLsizei stride);
typedef void (APIENTRYP PFNGLCOLORFORMATNVPROC) (GLint size, GLenum type, GLsizei stride);
typedef void (APIENTRYP PFNGLINDEXFORMATNVPROC) (GLenum type, GLsizei stride);
typedef void (APIENTRYP PFNGLTEXCOORDFORMATNVPROC) (GLint size, GLenum type, GLsizei stride);
typedef void (APIENTRYP PFNGLEDGEFLAGFORMATNVPROC) (GLsizei stride);
typedef void (APIENTRYP PFNGLSECONDARYCOLORFORMATNVPROC) (GLint size, GLenum type, GLsizei stride);
typedef void (APIENTRYP PFNGLFOGCOORDFORMATNVPROC) (GLenum type, GLsizei stride);
typedef void (APIENTRYP PFNGLVERTEXATTRIBFORMATNVPROC) (GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride);
typedef void (APIENTRYP PFNGLVERTEXATTRIBIFORMATNVPROC) (GLuint index, GLint size, GLenum type, GLsizei stride);
typedef void (APIENTRYP PFNGLGETINTEGERUI64I_VNVPROC) (GLenum value, GLuint index, GLuint64EXT *result);
#endif



#ifndef GL_APPLE_float_pixels
#define GL_HALF_APPLE                      0x140B
#define GL_COLOR_FLOAT_APPLE               0x8A0F
#define GL_RGBA_FLOAT32_APPLE              0x8814
#define GL_RGB_FLOAT32_APPLE               0x8815
#define GL_ALPHA_FLOAT32_APPLE             0x8816
#define GL_INTENSITY_FLOAT32_APPLE         0x8817
#define GL_LUMINANCE_FLOAT32_APPLE         0x8818
#define GL_LUMINANCE_ALPHA_FLOAT32_APPLE   0x8819
#define GL_RGBA_FLOAT16_APPLE              0x881A
#define GL_RGB_FLOAT16_APPLE               0x881B
#define GL_ALPHA_FLOAT16_APPLE             0x881C
#define GL_INTENSITY_FLOAT16_APPLE         0x881D
#define GL_LUMINANCE_FLOAT16_APPLE         0x881E
#define GL_LUMINANCE_ALPHA_FLOAT16_APPLE   0x881F
#endif

#ifndef GL_APPLE_float_pixels
#define GL_APPLE_float_pixels 1
#endif

#ifndef GL_ATI_texture_float
#define GL_RGBA_FLOAT32_ATI               0x8814
#define GL_RGB_FLOAT32_ATI                0x8815
#define GL_ALPHA_FLOAT32_ATI              0x8816
#define GL_INTENSITY_FLOAT32_ATI          0x8817
#define GL_LUMINANCE_FLOAT32_ATI          0x8818
#define GL_LUMINANCE_ALPHA_FLOAT32_ATI    0x8819
#define GL_RGBA_FLOAT16_ATI               0x881A
#define GL_RGB_FLOAT16_ATI                0x881B
#define GL_ALPHA_FLOAT16_ATI              0x881C
#define GL_INTENSITY_FLOAT16_ATI          0x881D
#define GL_LUMINANCE_FLOAT16_ATI          0x881E
#define GL_LUMINANCE_ALPHA_FLOAT16_ATI    0x881F
#endif

#ifndef GL_ATI_texture_float
#define GL_ATI_texture_float 1
#endif

#ifndef GL_NV_float_buffer
#define GL_FLOAT_R_NV                     0x8880
#define GL_FLOAT_RG_NV                    0x8881
#define GL_FLOAT_RGB_NV                   0x8882
#define GL_FLOAT_RGBA_NV                  0x8883
#define GL_FLOAT_R16_NV                   0x8884
#define GL_FLOAT_R32_NV                   0x8885
#define GL_FLOAT_RG16_NV                  0x8886
#define GL_FLOAT_RG32_NV                  0x8887
#define GL_FLOAT_RGB16_NV                 0x8888
#define GL_FLOAT_RGB32_NV                 0x8889
#define GL_FLOAT_RGBA16_NV                0x888A
#define GL_FLOAT_RGBA32_NV                0x888B
#define GL_TEXTURE_FLOAT_COMPONENTS_NV    0x888C
#define GL_FLOAT_CLEAR_COLOR_VALUE_NV     0x888D
#define GL_FLOAT_RGBA_MODE_NV             0x888E
#endif

#ifndef GL_NV_float_buffer
#define GL_NV_float_buffer 1
#endif

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



#endif /* __gl3ext_h_ */

