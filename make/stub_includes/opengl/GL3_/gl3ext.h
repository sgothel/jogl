#ifndef __gl3ext_h_
#define __gl3ext_h_

#include "gl3-64bit-types.h"

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
  

#ifndef GL_VERSION_3_1
#define GL_SAMPLER_2D_RECT                0x8B63
#define GL_SAMPLER_2D_RECT_SHADOW         0x8B64
#define GL_SAMPLER_BUFFER                 0x8DC2
#define GL_INT_SAMPLER_2D_RECT            0x8DCD
#define GL_INT_SAMPLER_BUFFER             0x8DD0
#define GL_UNSIGNED_INT_SAMPLER_2D_RECT   0x8DD5
#define GL_UNSIGNED_INT_SAMPLER_BUFFER    0x8DD8
#define GL_TEXTURE_BUFFER                 0x8C2A
#define GL_MAX_TEXTURE_BUFFER_SIZE        0x8C2B
#define GL_TEXTURE_BINDING_BUFFER         0x8C2C
#define GL_TEXTURE_BUFFER_DATA_STORE_BINDING 0x8C2D
#define GL_TEXTURE_BUFFER_FORMAT          0x8C2E
#define GL_TEXTURE_RECTANGLE              0x84F5
#define GL_TEXTURE_BINDING_RECTANGLE      0x84F6
#define GL_PROXY_TEXTURE_RECTANGLE        0x84F7
#define GL_MAX_RECTANGLE_TEXTURE_SIZE     0x84F8
#define GL_RED_SNORM                      0x8F90
#define GL_RG_SNORM                       0x8F91
#define GL_RGB_SNORM                      0x8F92
#define GL_RGBA_SNORM                     0x8F93
#define GL_R8_SNORM                       0x8F94
#define GL_RG8_SNORM                      0x8F95
#define GL_RGB8_SNORM                     0x8F96
#define GL_RGBA8_SNORM                    0x8F97
#define GL_R16_SNORM                      0x8F98
#define GL_RG16_SNORM                     0x8F99
#define GL_RGB16_SNORM                    0x8F9A
#define GL_RGBA16_SNORM                   0x8F9B
#define GL_SIGNED_NORMALIZED              0x8F9C
#define GL_PRIMITIVE_RESTART              0x8F9D
#define GL_PRIMITIVE_RESTART_INDEX        0x8F9E
/* Reuse tokens from ARB_copy_buffer */
/* reuse GL_COPY_READ_BUFFER */
/* reuse GL_COPY_WRITE_BUFFER */
/* Would reuse tokens from ARB_draw_instanced, but it has none */
/* Reuse tokens from ARB_uniform_buffer_object */
/* reuse GL_UNIFORM_BUFFER */
/* reuse GL_UNIFORM_BUFFER_BINDING */
/* reuse GL_UNIFORM_BUFFER_START */
/* reuse GL_UNIFORM_BUFFER_SIZE */
/* reuse GL_MAX_VERTEX_UNIFORM_BLOCKS */
/* reuse GL_MAX_FRAGMENT_UNIFORM_BLOCKS */
/* reuse GL_MAX_COMBINED_UNIFORM_BLOCKS */
/* reuse GL_MAX_UNIFORM_BUFFER_BINDINGS */
/* reuse GL_MAX_UNIFORM_BLOCK_SIZE */
/* reuse GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS */
/* reuse GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS */
/* reuse GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT */
/* reuse GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH */
/* reuse GL_ACTIVE_UNIFORM_BLOCKS */
/* reuse GL_UNIFORM_TYPE */
/* reuse GL_UNIFORM_SIZE */
/* reuse GL_UNIFORM_NAME_LENGTH */
/* reuse GL_UNIFORM_BLOCK_INDEX */
/* reuse GL_UNIFORM_OFFSET */
/* reuse GL_UNIFORM_ARRAY_STRIDE */
/* reuse GL_UNIFORM_MATRIX_STRIDE */
/* reuse GL_UNIFORM_IS_ROW_MAJOR */
/* reuse GL_UNIFORM_BLOCK_BINDING */
/* reuse GL_UNIFORM_BLOCK_DATA_SIZE */
/* reuse GL_UNIFORM_BLOCK_NAME_LENGTH */
/* reuse GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS */
/* reuse GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES */
/* reuse GL_UNIFORM_BLOCK_REFERENCED_BY_VERTEX_SHADER */
/* reuse GL_UNIFORM_BLOCK_REFERENCED_BY_FRAGMENT_SHADER */
/* reuse GL_INVALID_INDEX */
#endif
#ifndef GL_VERSION_3_1
#define GL_VERSION_3_1 1
/* OpenGL 3.1 also reuses entry points from these extensions: */
/* ARB_copy_buffer */
/* ARB_uniform_buffer_object */
#ifdef GL3_PROTOTYPES
GLAPI void APIENTRY glDrawArraysInstanced (GLenum, GLint, GLsizei, GLsizei);
GLAPI void APIENTRY glDrawElementsInstanced (GLenum, GLsizei, GLenum, const GLvoid *, GLsizei);
GLAPI void APIENTRY glTexBuffer (GLenum, GLenum, GLuint);
GLAPI void APIENTRY glPrimitiveRestartIndex (GLuint);
#endif /* GL3_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif


#ifndef GL_ARB_copy_buffer
#define GL_COPY_READ_BUFFER               0x8F36
#define GL_COPY_WRITE_BUFFER              0x8F37
#endif
#ifndef GL_ARB_copy_buffer
#define GL_ARB_copy_buffer 1
#ifdef GL3_PROTOTYPES
GLAPI void APIENTRY glCopyBufferSubData (GLenum, GLenum, GLintptr, GLintptr, GLsizeiptr);
#endif /* GL3_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif


#ifndef GL_ARB_uniform_buffer_object
#define GL_UNIFORM_BUFFER                 0x8A11
#define GL_UNIFORM_BUFFER_BINDING         0x8A28
#define GL_UNIFORM_BUFFER_START           0x8A29
#define GL_UNIFORM_BUFFER_SIZE            0x8A2A
#define GL_MAX_VERTEX_UNIFORM_BLOCKS      0x8A2B
#define GL_MAX_GEOMETRY_UNIFORM_BLOCKS    0x8A2C
#define GL_MAX_FRAGMENT_UNIFORM_BLOCKS    0x8A2D
#define GL_MAX_COMBINED_UNIFORM_BLOCKS    0x8A2E
#define GL_MAX_UNIFORM_BUFFER_BINDINGS    0x8A2F
#define GL_MAX_UNIFORM_BLOCK_SIZE         0x8A30
#define GL_MAX_COMBINED_VERTEX_UNIFORM_COMPONENTS 0x8A31
#define GL_MAX_COMBINED_GEOMETRY_UNIFORM_COMPONENTS 0x8A32
#define GL_MAX_COMBINED_FRAGMENT_UNIFORM_COMPONENTS 0x8A33
#define GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT 0x8A34
#define GL_ACTIVE_UNIFORM_BLOCK_MAX_NAME_LENGTH 0x8A35
#define GL_ACTIVE_UNIFORM_BLOCKS          0x8A36
#define GL_UNIFORM_TYPE                   0x8A37
#define GL_UNIFORM_SIZE                   0x8A38
#define GL_UNIFORM_NAME_LENGTH            0x8A39
#define GL_UNIFORM_BLOCK_INDEX            0x8A3A
#define GL_UNIFORM_OFFSET                 0x8A3B
#define GL_UNIFORM_ARRAY_STRIDE           0x8A3C
#define GL_UNIFORM_MATRIX_STRIDE          0x8A3D
#define GL_UNIFORM_IS_ROW_MAJOR           0x8A3E
#define GL_UNIFORM_BLOCK_BINDING          0x8A3F
#define GL_UNIFORM_BLOCK_DATA_SIZE        0x8A40
#define GL_UNIFORM_BLOCK_NAME_LENGTH      0x8A41
#define GL_UNIFORM_BLOCK_ACTIVE_UNIFORMS  0x8A42
#define GL_UNIFORM_BLOCK_ACTIVE_UNIFORM_INDICES 0x8A43
#define GL_UNIFORM_BLOCK_REFERENCED_BY_VERTEX_SHADER 0x8A44
#define GL_UNIFORM_BLOCK_REFERENCED_BY_GEOMETRY_SHADER 0x8A45
#define GL_UNIFORM_BLOCK_REFERENCED_BY_FRAGMENT_SHADER 0x8A46
/** Manual: #define GL_INVALID_INDEX                  -1 == (int) 0xFFFFFFFFu */
#endif
#ifndef GL_ARB_uniform_buffer_object
#define GL_ARB_uniform_buffer_object 1
#ifdef GL3_PROTOTYPES
GLAPI void APIENTRY glGetUniformIndices (GLuint, GLsizei, const GLchar* *, GLuint *);
GLAPI void APIENTRY glGetActiveUniformsiv (GLuint, GLsizei, const GLuint *, GLenum, GLint *);
GLAPI void APIENTRY glGetActiveUniformName (GLuint, GLuint, GLsizei, GLsizei *, GLchar *);
GLAPI GLuint APIENTRY glGetUniformBlockIndex (GLuint, const GLchar *);
GLAPI void APIENTRY glGetActiveUniformBlockiv (GLuint, GLuint, GLenum, GLint *);
GLAPI void APIENTRY glGetActiveUniformBlockName (GLuint, GLuint, GLsizei, GLsizei *, GLchar *);
GLAPI void APIENTRY glUniformBlockBinding (GLuint, GLuint, GLuint);
#endif /* GL3_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif


#ifndef GL_VERSION_3_2
#define GL_VERSION_3_2 1
/* OpenGL 3.2 also reuses entry points from these extensions: */
/* ARB_vertex_array_bgra */
/* ARB_draw_elements_base_vertex */
/* ARB_fragment_coord_conventions */
/* ARB_provoking_vertex */
/* ARB_seamless_cube_map */
/* ARB_texture_multisample */
/* ARB_depth_clamp */
/* ARB_geometry_shader4 */
/* ARB_sync */
#ifdef GL_GL3EXT_PROTOTYPES
#endif /* GL3_PROTOTYPES */
#endif

/**
 * http://www.opengl.org/registry/specs/ARB/vertex_array_bgra.txt
 */
#ifndef GL_ARB_vertex_array_bgra
#define GL_BGRA 0x80E1
#endif
#ifndef GL_ARB_vertex_array_bgra
#define GL_ARB_vertex_array_bgra 1
#ifdef GL_GL3EXT_PROTOTYPES
/* No FUNCTIONS */
#endif
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

/**
 * http://www.opengl.org/registry/specs/ARB/draw_elements_base_vertex.txt
 */
#ifndef GL_ARB_draw_elements_base_vertex
/* No TOKENS */
#endif
#ifndef GL_ARB_draw_elements_base_vertex
#define GL_ARB_draw_elements_base_vertex 1
#ifdef GL_GL3EXT_PROTOTYPES
GLAPI void APIENTRY glDrawElementsBaseVertex(GLenum mode, GLsizei count, GLenum type,
         GLvoid *indices, GLint basevertex);
GLAPI void APIENTRY glDrawRangeElementsBaseVertex(GLenum mode, GLuint start, GLuint end,
         GLsizei count, GLenum type, GLvoid *indices, GLint basevertex);
GLAPI void APIENTRY glDrawElementsInstancedBaseVertex(GLenum mode, GLsizei count,
         GLenum type, const GLvoid *indices, GLsizei primcount, GLint basevertex);
GLAPI void APIENTRY glMultiDrawElementsBaseVertex(GLenum mode, GLsizei *count, GLenum type,
         GLvoid **indices, GLsizei primcount, GLint *basevertex);
#endif
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

/**
 * http://www.opengl.org/registry/specs/ARB/fragment_coord_conventions.txt
 */
#ifndef GL_ARB_fragment_coord_conventions
/* No Tokens */
#endif
#ifndef GL_ARB_fragment_coord_conventions
#define GL_ARB_fragment_coord_conventions 1
#ifdef GL_GL3EXT_PROTOTYPES
/* No Functions */
#endif
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

/**
 * http://www.opengl.org/registry/specs/ARB/provoking_vertex.txt
 */
#ifndef GL_ARB_provoking_vertex
#define GL_FIRST_VERTEX_CONVENTION                   0x8E4D
#define GL_LAST_VERTEX_CONVENTION                    0x8E4E
#define GL_PROVOKING_VERTEX                          0x8E4F
#define GL_QUADS_FOLLOW_PROVOKING_VERTEX_CONVENTION  0x8E4C
#endif
#ifndef GL_ARB_provoking_vertex
#define GL_ARB_provoking_vertex 1
#ifdef GL_GL3EXT_PROTOTYPES
GLAPI void APIENTRY glProvokingVertex(GLenum mode);
#endif
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

/**
 * http://www.opengl.org/registry/specs/ARB/seamless_cube_map.txt
 */
#ifndef GL_ARB_seamless_cube_map
#define GL_TEXTURE_CUBE_MAP_SEAMLESS                   0x884F
#endif
#ifndef GL_ARB_seamless_cube_map
#define GL_ARB_seamless_cube_map 1
#ifdef GL_GL3EXT_PROTOTYPES
/* No Functions */
#endif
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

/**
 * http://www.opengl.org/registry/specs/ARB/texture_multisample.txt
 */
#ifndef GL_ARB_texture_multisample
#define GL_SAMPLE_POSITION                             0x8E50
#define GL_SAMPLE_MASK                                 0x8E51
#define GL_SAMPLE_MASK_VALUE                           0x8E52
#define GL_TEXTURE_2D_MULTISAMPLE                      0x9100
#define GL_PROXY_TEXTURE_2D_MULTISAMPLE                0x9101
#define GL_TEXTURE_2D_MULTISAMPLE_ARRAY                0x9102
#define GL_PROXY_TEXTURE_2D_MULTISAMPLE_ARRAY          0x9103
#define GL_MAX_SAMPLE_MASK_WORDS                       0x8E59
#define GL_MAX_COLOR_TEXTURE_SAMPLES                   0x910E
#define GL_MAX_DEPTH_TEXTURE_SAMPLES                   0x910F
#define GL_MAX_INTEGER_SAMPLES                         0x9110
#define GL_TEXTURE_BINDING_2D_MULTISAMPLE              0x9104
#define GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY        0x9105
#define GL_TEXTURE_SAMPLES                             0x9106
#define GL_TEXTURE_FIXED_SAMPLE_LOCATIONS              0x9107
#define GL_SAMPLER_2D_MULTISAMPLE                      0x9108
#define GL_INT_SAMPLER_2D_MULTISAMPLE                  0x9109
#define GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE         0x910A
#define GL_SAMPLER_2D_MULTISAMPLE_ARRAY                0x910B
#define GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY            0x910C
#define GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY   0x910D
#endif
#ifndef GL_ARB_texture_multisample
#define GL_ARB_texture_multisample 1
#ifdef GL_GL3EXT_PROTOTYPES
GLAPI void APIENTRY glTexImage2DMultisample(GLenum target, GLsizei samples, GLint internalformat,
                               GLsizei width, GLsizei height,
                               GLboolean fixedsamplelocations);
GLAPI void APIENTRY glTexImage3DMultisample(GLenum target, GLsizei samples, GLint internalformat,
                               GLsizei width, GLsizei height, GLsizei depth,
                               GLboolean fixedsamplelocations);
GLAPI void APIENTRY glGetMultisamplefv(GLenum pname, GLuint index, GLfloat *val);
GLAPI void APIENTRY glSampleMaski(GLuint index, GLbitfield mask);
#endif
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

/**
 * http://www.opengl.org/registry/specs/ARB/depth_clamp.txt
 */
#ifndef GL_ARB_depth_clamp
#define GL_DEPTH_CLAMP 0x864F
#endif
#ifndef GL_ARB_depth_clamp
#define GL_ARB_depth_clamp 1
#ifdef GL_GL3EXT_PROTOTYPES
/* No FUNCTIONS */
#endif
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

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
/* reuse GL_MAX_VARYING_COMPONENTS */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER */
#endif
#ifndef GL_ARB_geometry_shader4
#define GL_ARB_geometry_shader4 1
#ifdef GL_GL3EXT_PROTOTYPES
GLAPI void APIENTRY glProgramParameteriARB (GLuint, GLenum, GLint);
GLAPI void APIENTRY glFramebufferTextureARB (GLenum, GLenum, GLuint, GLint);
GLAPI void APIENTRY glFramebufferTextureLayerARB (GLenum, GLenum, GLuint, GLint, GLint);
GLAPI void APIENTRY glFramebufferTextureFaceARB (GLenum, GLenum, GLuint, GLint, GLenum);
#endif
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

/**
 * http://www.opengl.org/registry/specs/ARB/sync.txt
 */
#ifndef GL_ARB_sync
#define GL_MAX_SERVER_WAIT_TIMEOUT    0x9111
#define GL_OBJECT_TYPE                0x9112
#define GL_SYNC_CONDITION             0x9113
#define GL_SYNC_STATUS                0x9114
#define GL_SYNC_FLAGS                 0x9115
#define GL_SYNC_FENCE                 0x9116
#define GL_SYNC_GPU_COMMANDS_COMPLETE 0x9117
#define GL_UNSIGNALED                 0x9118
#define GL_SIGNALED                   0x9119
#define GL_SYNC_FLUSH_COMMANDS_BIT    0x00000001
/* Manual: #define GL_TIMEOUT_IGNORED 0xFFFFFFFFFFFFFFFFul */
#define GL_ALREADY_SIGNALED           0x911A
#define GL_TIMEOUT_EXPIRED            0x911B
#define GL_CONDITION_SATISFIED        0x911C
#define GL_WAIT_FAILED                0x911D
#endif
#ifndef GL_ARB_sync
typedef int64_t GLint64;
typedef uint64_t GLuint64;
typedef struct __GLsync *GLsync;
#endif
#ifndef GL_ARB_sync
#define GL_ARB_sync 1
#ifdef GL_GL3EXT_PROTOTYPES
GLAPI GLsync    APIENTRY glFenceSync(GLenum condition, GLbitfield flags);
GLAPI GLboolean APIENTRY glIsSync(GLsync sync);
GLAPI void      APIENTRY glDeleteSync(GLsync sync);
GLAPI GLenum    APIENTRY glClientWaitSync(GLsync sync, GLbitfield flags, GLuint64 timeout);
GLAPI void      APIENTRY glWaitSync(GLsync sync, GLbitfield flags, GLuint64 timeout);
GLAPI void      APIENTRY glGetInteger64v(GLenum pname, GLint64 *params);
GLAPI void      APIENTRY glGetSynciv(GLsync sync, GLenum pname, GLsizei bufSize, GLsizei *length, GLint *values);
#endif
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

/**
 * Convenient names only .. actually subsumed into core
 */
#ifndef GL_ARB_texture_rectangle
#define GL_TEXTURE_RECTANGLE_ARB          0x84F5
#define GL_TEXTURE_BINDING_RECTANGLE_ARB  0x84F6
#define GL_PROXY_TEXTURE_RECTANGLE_ARB    0x84F7
#define GL_MAX_RECTANGLE_TEXTURE_SIZE_ARB 0x84F8
#endif

#ifndef GL_ARB_texture_rectangle
#define GL_ARB_texture_rectangle 1
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
GLAPI void APIENTRY glMakeBufferResidentNV(GLenum target, GLenum access);
GLAPI void APIENTRY glMakeBufferNonResidentNV(GLenum target);
GLAPI GLboolean APIENTRY glIsBufferResidentNV(GLenum target);
GLAPI void APIENTRY glNamedMakeBufferResidentNV(GLuint buffer, GLenum access);
GLAPI void APIENTRY glNamedMakeBufferNonResidentNV(GLuint buffer);
GLAPI GLboolean APIENTRY glIsNamedBufferResidentNV(GLuint buffer);
GLAPI void APIENTRY glGetBufferParameterui64vNV(GLenum target, GLenum pname, GLuint64 *params);
GLAPI void APIENTRY glGetNamedBufferParameterui64vNV(GLuint buffer, GLenum pname, GLuint64 *params);
GLAPI void APIENTRY glGetIntegerui64vNV(GLenum value, GLuint64 *result);
GLAPI void APIENTRY glUniformui64NV(GLint location, GLuint64 value);
GLAPI void APIENTRY glUniformui64vNV(GLint location, GLsizei count, GLuint64 *value);
GLAPI void APIENTRY glGetUniformui64vNV(GLuint program, GLint location, GLuint64 *params);
GLAPI void APIENTRY glProgramUniformui64NV(GLuint program, GLint location, GLuint64 value);
GLAPI void APIENTRY glProgramUniformui64vNV(GLuint program, GLint location, GLsizei count, GLuint64 *value);
#endif /* GL_GL3EXT_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
#endif

/**
 * http://www.opengl.org/registry/specs/NV/vertex_buffer_unified_memory.txt
 */
#ifndef GL_NV_vertex_buffer_unified_memory
#define GL_VERTEX_ATTRIB_ARRAY_UNIFIED_NV   0x8F1E
#define GL_ELEMENT_ARRAY_UNIFIED_NV         0x8F1F
#define GL_VERTEX_ATTRIB_ARRAY_ADDRESS_NV   0x8F20
#define GL_VERTEX_ARRAY_ADDRESS_NV          0x8F21
#define GL_NORMAL_ARRAY_ADDRESS_NV          0x8F22
#define GL_COLOR_ARRAY_ADDRESS_NV           0x8F23
#define GL_INDEX_ARRAY_ADDRESS_NV           0x8F24
#define GL_TEXTURE_COORD_ARRAY_ADDRESS_NV   0x8F25
#define GL_EDGE_FLAG_ARRAY_ADDRESS_NV       0x8F26
#define GL_SECONDARY_COLOR_ARRAY_ADDRESS_NV 0x8F27
#define GL_FOG_COORD_ARRAY_ADDRESS_NV       0x8F28
#define GL_ELEMENT_ARRAY_ADDRESS_NV         0x8F29
#define GL_VERTEX_ATTRIB_ARRAY_LENGTH_NV    0x8F2A
#define GL_VERTEX_ARRAY_LENGTH_NV           0x8F2B
#define GL_NORMAL_ARRAY_LENGTH_NV           0x8F2C
#define GL_COLOR_ARRAY_LENGTH_NV            0x8F2D
#define GL_INDEX_ARRAY_LENGTH_NV            0x8F2E
#define GL_TEXTURE_COORD_ARRAY_LENGTH_NV    0x8F2F
#define GL_EDGE_FLAG_ARRAY_LENGTH_NV        0x8F30
#define GL_SECONDARY_COLOR_ARRAY_LENGTH_NV  0x8F31
#define GL_FOG_COORD_ARRAY_LENGTH_NV        0x8F32
#define GL_ELEMENT_ARRAY_LENGTH_NV          0x8F33
#endif
#ifndef GL_NV_vertex_buffer_unified_memory
#define GL_NV_vertex_buffer_unified_memory 1
#ifdef GL_GL3EXT_PROTOTYPES
GLAPI GLboolean APIENTRY glIsEnabled( GLenum cap );     // extra requirement in core GL3
GLAPI void APIENTRY glEnableClientState( GLenum cap );  // extra requirement in core GL3
GLAPI void APIENTRY glDisableClientState( GLenum cap ); // extra requirement in core GL3
GLAPI void APIENTRY glBufferAddressRangeNV(GLenum pname, GLuint index, GLuint64 address, GLsizeiptr length);
GLAPI void APIENTRY glVertexFormatNV(GLint size, GLenum type, GLsizei stride);
GLAPI void APIENTRY glNormalFormatNV(GLenum type, GLsizei stride);
GLAPI void APIENTRY glColorFormatNV(GLint size, GLenum type, GLsizei stride);
GLAPI void APIENTRY glIndexFormatNV(GLenum type, GLsizei stride);
GLAPI void APIENTRY glTexCoordFormatNV(GLint size, GLenum type, GLsizei stride);
GLAPI void APIENTRY glEdgeFlagFormatNV(GLsizei stride);
GLAPI void APIENTRY glSecondaryColorFormatNV(GLint size, GLenum type, GLsizei stride);
GLAPI void APIENTRY glFogCoordFormatNV(GLenum type, GLsizei stride);
GLAPI void APIENTRY glVertexAttribFormatNV(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride);
GLAPI void APIENTRY glVertexAttribIFormatNV(GLuint index, GLint size, GLenum type, GLsizei stride);
GLAPI void APIENTRY glGetIntegerui64i_vNV(GLenum value, GLuint index, GLuint64 *result);
#endif /* GL_GL3EXT_PROTOTYPES */
/* No need for explicit function pointer: we force generation of ProcAddress .. */
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

#endif /* __gl3ext_h_ */

