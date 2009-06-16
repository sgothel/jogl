#ifndef __gl3_h_
#define __gl3_h_

#ifdef __cplusplus
extern "C" {
#endif

/*
** Copyright (c) 2007-2009 The Khronos Group Inc.
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

/* This is a draft release of gl3.h, a new header intended for use with
 * core OpenGL 3.1 implementations. The current version is always
 * available at http://www.opengl.org/registry/ . Please don't package
 * gl3.h for release with other software until it's out of draft status.
 * The structure of the file may change significantly, and the details
 * will probably change slightly as we make sure exactly the right set
 * of interfaces is included.
 *
 * gl3.h last updated on $Date: 2009-05-05 15:53:39 -0700 (Tue, 05 May 2009) $
 *
 * RELEASE NOTES
 *
 * gl3.h should be placed under a directory 'GL3' and included as
 * '<GL3/gl3.h>'.
 *
 * gl3.h only includes interfaces supported in a core OpenGL 3.1
 * implementation, as well as interfaces for a few ARB extensions which
 * have identical enums and entry points. It does not, and never will
 * include functionality removed from core OpenGL 3.1, such as
 * fixed-function vertex and fragment processing.
 *
 * Implementations of OpenGL 3.1 supporting the optional
 * GL_ARB_compatibility extension continue to provide that functionality,
 * and source code requiring it should use the traditional <GL/gl.h> and
 * <GL/glext.h> headers instead of <GL3/gl3.h>.
 *
 * It is not possible to #include both <GL3/gl3.h> and either of
 * <GL/gl.h> or <GL/glext.h> in the same source file.
 *
 * We welcome feedback on gl3.h. Please register for the Khronos Bugzilla
 * (www.khronos.org/bugzilla) and file issues there under product
 * "OpenGL", category "Registry". Feedback on the opengl.org forums
 * may not be responded to in a timely fashion.
 */

/* Function declaration macros - to move into glplatform.h */

#if defined(_WIN32) && !defined(__WIN32__) && !defined(__CYGWIN__)
#define __WIN32__
#endif

#if !defined(OPENSTEP) && (defined(__WIN32__) && !defined(__CYGWIN__))
#  if defined(_MSC_VER) && defined(BUILD_GL32) /* tag specify we're building mesa as a DLL */
#    define GLAPI __declspec(dllexport)
#  elif defined(_MSC_VER) && defined(_DLL) /* tag specifying we're building for DLL runtime support */
#    define GLAPI __declspec(dllimport)
#  else /* for use with static link lib build of Win32 edition only */
#    define GLAPI extern
#  endif /* _STATIC_MESA support */
#  define GLAPIENTRY __stdcall
#else
/* non-Windows compilation */
#  define GLAPI extern
#  define GLAPIENTRY
#endif /* WIN32 / CYGWIN bracket */

#ifndef GLAPIENTRYP
#   define GLAPIENTRYP GLAPIENTRY*
#endif

#if (defined(__BEOS__) && defined(__POWERPC__)) || defined(__QUICKDRAW__)
#  define PRAGMA_EXPORT_SUPPORTED               1
#endif

/*
 * WINDOWS: Include windows.h here to define APIENTRY.
 * It is also useful when applications include this file by
 * including only glut.h, since glut.h depends on windows.h.
 * Applications needing to include windows.h with parms other
 * than "WIN32_LEAN_AND_MEAN" may include windows.h before
 * glut.h or gl.h.
 */
#if defined(_WIN32) && !defined(APIENTRY) && !defined(__CYGWIN__)
#define WIN32_LEAN_AND_MEAN 1
#include <windows.h>
#endif

#if defined(_WIN32) && !defined(_WINGDI_) && !defined(_GNU_H_WINDOWS32_DEFINES) && !defined(OPENSTEP) && !defined(__CYGWIN__)
#include <gl/mesa_wgl.h>
#endif

#if defined(macintosh) && PRAGMA_IMPORT_SUPPORTED
#pragma import on
#endif

#ifndef APIENTRY
#define APIENTRY GLAPIENTRY
#endif

#ifndef APIENTRY
#define APIENTRY GLAPIENTRY
#endif

#ifdef CENTERLINE_CLPP
#define signed
#endif

#if defined(PRAGMA_EXPORT_SUPPORTED)
#pragma export on
#endif

/*
 * End system-specific stuff.
 **********************************************************************/

/* Base GL types */

typedef unsigned int GLenum;
typedef unsigned char GLboolean;
typedef unsigned int GLbitfield;
typedef signed char GLbyte;
typedef short GLshort;
typedef int GLint;
typedef int GLsizei;
typedef unsigned char GLubyte;
typedef unsigned short GLushort;
typedef unsigned int GLuint;
typedef float GLfloat;
typedef float GLclampf;
typedef double GLdouble;
typedef double GLclampd;
typedef void GLvoid;

/*************************************************************/

#ifndef GL_VERSION_1_1
/* AttribMask */
#define GL_DEPTH_BUFFER_BIT               0x00000100
#define GL_STENCIL_BUFFER_BIT             0x00000400
#define GL_COLOR_BUFFER_BIT               0x00004000
/* Boolean */
#define GL_FALSE                          0
#define GL_TRUE                           1
/* BeginMode */
#define GL_POINTS                         0x0000
#define GL_LINES                          0x0001
#define GL_LINE_LOOP                      0x0002
#define GL_LINE_STRIP                     0x0003
#define GL_TRIANGLES                      0x0004
#define GL_TRIANGLE_STRIP                 0x0005
#define GL_TRIANGLE_FAN                   0x0006
/* AlphaFunction */
#define GL_NEVER                          0x0200
#define GL_LESS                           0x0201
#define GL_EQUAL                          0x0202
#define GL_LEQUAL                         0x0203
#define GL_GREATER                        0x0204
#define GL_NOTEQUAL                       0x0205
#define GL_GEQUAL                         0x0206
#define GL_ALWAYS                         0x0207
/* BlendingFactorDest */
#define GL_ZERO                           0
#define GL_ONE                            1
#define GL_SRC_COLOR                      0x0300
#define GL_ONE_MINUS_SRC_COLOR            0x0301
#define GL_SRC_ALPHA                      0x0302
#define GL_ONE_MINUS_SRC_ALPHA            0x0303
#define GL_DST_ALPHA                      0x0304
#define GL_ONE_MINUS_DST_ALPHA            0x0305
/* BlendingFactorSrc */
#define GL_DST_COLOR                      0x0306
#define GL_ONE_MINUS_DST_COLOR            0x0307
#define GL_SRC_ALPHA_SATURATE             0x0308
/* DrawBufferMode */
#define GL_NONE                           0
#define GL_FRONT_LEFT                     0x0400
#define GL_FRONT_RIGHT                    0x0401
#define GL_BACK_LEFT                      0x0402
#define GL_BACK_RIGHT                     0x0403
#define GL_FRONT                          0x0404
#define GL_BACK                           0x0405
#define GL_LEFT                           0x0406
#define GL_RIGHT                          0x0407
#define GL_FRONT_AND_BACK                 0x0408
/* ErrorCode */
#define GL_NO_ERROR                       0
#define GL_INVALID_ENUM                   0x0500
#define GL_INVALID_VALUE                  0x0501
#define GL_INVALID_OPERATION              0x0502
#define GL_OUT_OF_MEMORY                  0x0505
/* FrontFaceDirection */
#define GL_CW                             0x0900
#define GL_CCW                            0x0901
/* GetPName */
#define GL_POINT_SIZE                     0x0B11
#define GL_POINT_SIZE_RANGE               0x0B12
#define GL_POINT_SIZE_GRANULARITY         0x0B13
#define GL_LINE_SMOOTH                    0x0B20
#define GL_LINE_WIDTH                     0x0B21
#define GL_LINE_WIDTH_RANGE               0x0B22
#define GL_LINE_WIDTH_GRANULARITY         0x0B23
#define GL_POLYGON_SMOOTH                 0x0B41
#define GL_CULL_FACE                      0x0B44
#define GL_CULL_FACE_MODE                 0x0B45
#define GL_FRONT_FACE                     0x0B46
#define GL_DEPTH_RANGE                    0x0B70
#define GL_DEPTH_TEST                     0x0B71
#define GL_DEPTH_WRITEMASK                0x0B72
#define GL_DEPTH_CLEAR_VALUE              0x0B73
#define GL_DEPTH_FUNC                     0x0B74
#define GL_STENCIL_TEST                   0x0B90
#define GL_STENCIL_CLEAR_VALUE            0x0B91
#define GL_STENCIL_FUNC                   0x0B92
#define GL_STENCIL_VALUE_MASK             0x0B93
#define GL_STENCIL_FAIL                   0x0B94
#define GL_STENCIL_PASS_DEPTH_FAIL        0x0B95
#define GL_STENCIL_PASS_DEPTH_PASS        0x0B96
#define GL_STENCIL_REF                    0x0B97
#define GL_STENCIL_WRITEMASK              0x0B98
#define GL_VIEWPORT                       0x0BA2
#define GL_DITHER                         0x0BD0
#define GL_BLEND_DST                      0x0BE0
#define GL_BLEND_SRC                      0x0BE1
#define GL_BLEND                          0x0BE2
#define GL_LOGIC_OP_MODE                  0x0BF0
#define GL_COLOR_LOGIC_OP                 0x0BF2
#define GL_DRAW_BUFFER                    0x0C01
#define GL_READ_BUFFER                    0x0C02
#define GL_SCISSOR_BOX                    0x0C10
#define GL_SCISSOR_TEST                   0x0C11
#define GL_COLOR_CLEAR_VALUE              0x0C22
#define GL_COLOR_WRITEMASK                0x0C23
#define GL_DOUBLEBUFFER                   0x0C32
#define GL_STEREO                         0x0C33
#define GL_LINE_SMOOTH_HINT               0x0C52
#define GL_POLYGON_SMOOTH_HINT            0x0C53
#define GL_UNPACK_SWAP_BYTES              0x0CF0
#define GL_UNPACK_LSB_FIRST               0x0CF1
#define GL_UNPACK_ROW_LENGTH              0x0CF2
#define GL_UNPACK_SKIP_ROWS               0x0CF3
#define GL_UNPACK_SKIP_PIXELS             0x0CF4
#define GL_UNPACK_ALIGNMENT               0x0CF5
#define GL_PACK_SWAP_BYTES                0x0D00
#define GL_PACK_LSB_FIRST                 0x0D01
#define GL_PACK_ROW_LENGTH                0x0D02
#define GL_PACK_SKIP_ROWS                 0x0D03
#define GL_PACK_SKIP_PIXELS               0x0D04
#define GL_PACK_ALIGNMENT                 0x0D05
#define GL_MAX_TEXTURE_SIZE               0x0D33
#define GL_MAX_VIEWPORT_DIMS              0x0D3A
#define GL_SUBPIXEL_BITS                  0x0D50
#define GL_TEXTURE_1D                     0x0DE0
#define GL_TEXTURE_2D                     0x0DE1
#define GL_POLYGON_OFFSET_UNITS           0x2A00
#define GL_POLYGON_OFFSET_POINT           0x2A01
#define GL_POLYGON_OFFSET_LINE            0x2A02
#define GL_POLYGON_OFFSET_FILL            0x8037
#define GL_POLYGON_OFFSET_FACTOR          0x8038
#define GL_TEXTURE_BINDING_1D             0x8068
#define GL_TEXTURE_BINDING_2D             0x8069
/* GetTextureParameter */
#define GL_TEXTURE_WIDTH                  0x1000
#define GL_TEXTURE_HEIGHT                 0x1001
#define GL_TEXTURE_INTERNAL_FORMAT        0x1003
#define GL_TEXTURE_BORDER_COLOR           0x1004
#define GL_TEXTURE_BORDER                 0x1005
#define GL_TEXTURE_RED_SIZE               0x805C
#define GL_TEXTURE_GREEN_SIZE             0x805D
#define GL_TEXTURE_BLUE_SIZE              0x805E
#define GL_TEXTURE_ALPHA_SIZE             0x805F
/* HintMode */
#define GL_DONT_CARE                      0x1100
#define GL_FASTEST                        0x1101
#define GL_NICEST                         0x1102
/* DataType */
#define GL_BYTE                           0x1400
#define GL_UNSIGNED_BYTE                  0x1401
#define GL_SHORT                          0x1402
#define GL_UNSIGNED_SHORT                 0x1403
#define GL_INT                            0x1404
#define GL_UNSIGNED_INT                   0x1405
#define GL_FLOAT                          0x1406
#define GL_DOUBLE                         0x140A
/* LogicOp */
#define GL_CLEAR                          0x1500
#define GL_AND                            0x1501
#define GL_AND_REVERSE                    0x1502
#define GL_COPY                           0x1503
#define GL_AND_INVERTED                   0x1504
#define GL_NOOP                           0x1505
#define GL_XOR                            0x1506
#define GL_OR                             0x1507
#define GL_NOR                            0x1508
#define GL_EQUIV                          0x1509
#define GL_INVERT                         0x150A
#define GL_OR_REVERSE                     0x150B
#define GL_COPY_INVERTED                  0x150C
#define GL_OR_INVERTED                    0x150D
#define GL_NAND                           0x150E
#define GL_SET                            0x150F
/* MatrixMode (for gl3.h, FBO attachment type) */
#define GL_TEXTURE                        0x1702
/* PixelCopyType */
#define GL_COLOR                          0x1800
#define GL_DEPTH                          0x1801
#define GL_STENCIL                        0x1802
/* PixelFormat */
#define GL_STENCIL_INDEX                  0x1901
#define GL_DEPTH_COMPONENT                0x1902
#define GL_RED                            0x1903
#define GL_GREEN                          0x1904
#define GL_BLUE                           0x1905
#define GL_ALPHA                          0x1906
#define GL_RGB                            0x1907
#define GL_RGBA                           0x1908
/* PolygonMode */
#define GL_POINT                          0x1B00
#define GL_LINE                           0x1B01
#define GL_FILL                           0x1B02
/* StencilOp */
#define GL_KEEP                           0x1E00
#define GL_REPLACE                        0x1E01
#define GL_INCR                           0x1E02
#define GL_DECR                           0x1E03
/* StringName */
#define GL_VENDOR                         0x1F00
#define GL_RENDERER                       0x1F01
#define GL_VERSION                        0x1F02
#define GL_EXTENSIONS                     0x1F03
/* TextureMagFilter */
#define GL_NEAREST                        0x2600
#define GL_LINEAR                         0x2601
/* TextureMinFilter */
#define GL_NEAREST_MIPMAP_NEAREST         0x2700
#define GL_LINEAR_MIPMAP_NEAREST          0x2701
#define GL_NEAREST_MIPMAP_LINEAR          0x2702
#define GL_LINEAR_MIPMAP_LINEAR           0x2703
/* TextureParameterName */
#define GL_TEXTURE_MAG_FILTER             0x2800
#define GL_TEXTURE_MIN_FILTER             0x2801
#define GL_TEXTURE_WRAP_S                 0x2802
#define GL_TEXTURE_WRAP_T                 0x2803
/* TextureTarget */
#define GL_PROXY_TEXTURE_1D               0x8063
#define GL_PROXY_TEXTURE_2D               0x8064
/* TextureWrapMode */
#define GL_REPEAT                         0x2901
/* PixelInternalFormat */
#define GL_R3_G3_B2                       0x2A10
#define GL_RGB4                           0x804F
#define GL_RGB5                           0x8050
#define GL_RGB8                           0x8051
#define GL_RGB10                          0x8052
#define GL_RGB12                          0x8053
#define GL_RGB16                          0x8054
#define GL_RGBA2                          0x8055
#define GL_RGBA4                          0x8056
#define GL_RGB5_A1                        0x8057
#define GL_RGBA8                          0x8058
#define GL_RGB10_A2                       0x8059
#define GL_RGBA12                         0x805A
#define GL_RGBA16                         0x805B
#endif

#ifndef GL_VERSION_1_2
#define GL_UNSIGNED_BYTE_3_3_2            0x8032
#define GL_UNSIGNED_SHORT_4_4_4_4         0x8033
#define GL_UNSIGNED_SHORT_5_5_5_1         0x8034
#define GL_UNSIGNED_INT_8_8_8_8           0x8035
#define GL_UNSIGNED_INT_10_10_10_2        0x8036
#define GL_TEXTURE_BINDING_3D             0x806A
#define GL_PACK_SKIP_IMAGES               0x806B
#define GL_PACK_IMAGE_HEIGHT              0x806C
#define GL_UNPACK_SKIP_IMAGES             0x806D
#define GL_UNPACK_IMAGE_HEIGHT            0x806E
#define GL_TEXTURE_3D                     0x806F
#define GL_PROXY_TEXTURE_3D               0x8070
#define GL_TEXTURE_DEPTH                  0x8071
#define GL_TEXTURE_WRAP_R                 0x8072
#define GL_MAX_3D_TEXTURE_SIZE            0x8073
#define GL_UNSIGNED_BYTE_2_3_3_REV        0x8362
#define GL_UNSIGNED_SHORT_5_6_5           0x8363
#define GL_UNSIGNED_SHORT_5_6_5_REV       0x8364
#define GL_UNSIGNED_SHORT_4_4_4_4_REV     0x8365
#define GL_UNSIGNED_SHORT_1_5_5_5_REV     0x8366
#define GL_UNSIGNED_INT_8_8_8_8_REV       0x8367
#define GL_UNSIGNED_INT_2_10_10_10_REV    0x8368
#define GL_BGR                            0x80E0
#define GL_BGRA                           0x80E1
#define GL_MAX_ELEMENTS_VERTICES          0x80E8
#define GL_MAX_ELEMENTS_INDICES           0x80E9
#define GL_CLAMP_TO_EDGE                  0x812F
#define GL_TEXTURE_MIN_LOD                0x813A
#define GL_TEXTURE_MAX_LOD                0x813B
#define GL_TEXTURE_BASE_LEVEL             0x813C
#define GL_TEXTURE_MAX_LEVEL              0x813D
#define GL_SMOOTH_POINT_SIZE_RANGE        0x0B12
#define GL_SMOOTH_POINT_SIZE_GRANULARITY  0x0B13
#define GL_SMOOTH_LINE_WIDTH_RANGE        0x0B22
#define GL_SMOOTH_LINE_WIDTH_GRANULARITY  0x0B23
#define GL_ALIASED_LINE_WIDTH_RANGE       0x846E
#endif

#ifndef GL_ARB_imaging
#define GL_CONSTANT_COLOR                 0x8001
#define GL_ONE_MINUS_CONSTANT_COLOR       0x8002
#define GL_CONSTANT_ALPHA                 0x8003
#define GL_ONE_MINUS_CONSTANT_ALPHA       0x8004
#define GL_BLEND_COLOR                    0x8005
#define GL_FUNC_ADD                       0x8006
#define GL_MIN                            0x8007
#define GL_MAX                            0x8008
#define GL_BLEND_EQUATION                 0x8009
#define GL_FUNC_SUBTRACT                  0x800A
#define GL_FUNC_REVERSE_SUBTRACT          0x800B
#endif

#ifndef GL_VERSION_1_3
#define GL_TEXTURE0                       0x84C0
#define GL_TEXTURE1                       0x84C1
#define GL_TEXTURE2                       0x84C2
#define GL_TEXTURE3                       0x84C3
#define GL_TEXTURE4                       0x84C4
#define GL_TEXTURE5                       0x84C5
#define GL_TEXTURE6                       0x84C6
#define GL_TEXTURE7                       0x84C7
#define GL_TEXTURE8                       0x84C8
#define GL_TEXTURE9                       0x84C9
#define GL_TEXTURE10                      0x84CA
#define GL_TEXTURE11                      0x84CB
#define GL_TEXTURE12                      0x84CC
#define GL_TEXTURE13                      0x84CD
#define GL_TEXTURE14                      0x84CE
#define GL_TEXTURE15                      0x84CF
#define GL_TEXTURE16                      0x84D0
#define GL_TEXTURE17                      0x84D1
#define GL_TEXTURE18                      0x84D2
#define GL_TEXTURE19                      0x84D3
#define GL_TEXTURE20                      0x84D4
#define GL_TEXTURE21                      0x84D5
#define GL_TEXTURE22                      0x84D6
#define GL_TEXTURE23                      0x84D7
#define GL_TEXTURE24                      0x84D8
#define GL_TEXTURE25                      0x84D9
#define GL_TEXTURE26                      0x84DA
#define GL_TEXTURE27                      0x84DB
#define GL_TEXTURE28                      0x84DC
#define GL_TEXTURE29                      0x84DD
#define GL_TEXTURE30                      0x84DE
#define GL_TEXTURE31                      0x84DF
#define GL_ACTIVE_TEXTURE                 0x84E0
#define GL_MULTISAMPLE                    0x809D
#define GL_SAMPLE_ALPHA_TO_COVERAGE       0x809E
#define GL_SAMPLE_ALPHA_TO_ONE            0x809F
#define GL_SAMPLE_COVERAGE                0x80A0
#define GL_SAMPLE_BUFFERS                 0x80A8
#define GL_SAMPLES                        0x80A9
#define GL_SAMPLE_COVERAGE_VALUE          0x80AA
#define GL_SAMPLE_COVERAGE_INVERT         0x80AB
#define GL_TEXTURE_CUBE_MAP               0x8513
#define GL_TEXTURE_BINDING_CUBE_MAP       0x8514
#define GL_TEXTURE_CUBE_MAP_POSITIVE_X    0x8515
#define GL_TEXTURE_CUBE_MAP_NEGATIVE_X    0x8516
#define GL_TEXTURE_CUBE_MAP_POSITIVE_Y    0x8517
#define GL_TEXTURE_CUBE_MAP_NEGATIVE_Y    0x8518
#define GL_TEXTURE_CUBE_MAP_POSITIVE_Z    0x8519
#define GL_TEXTURE_CUBE_MAP_NEGATIVE_Z    0x851A
#define GL_PROXY_TEXTURE_CUBE_MAP         0x851B
#define GL_MAX_CUBE_MAP_TEXTURE_SIZE      0x851C
#define GL_COMPRESSED_RGB                 0x84ED
#define GL_COMPRESSED_RGBA                0x84EE
#define GL_TEXTURE_COMPRESSION_HINT       0x84EF
#define GL_TEXTURE_COMPRESSED_IMAGE_SIZE  0x86A0
#define GL_TEXTURE_COMPRESSED             0x86A1
#define GL_NUM_COMPRESSED_TEXTURE_FORMATS 0x86A2
#define GL_COMPRESSED_TEXTURE_FORMATS     0x86A3
#define GL_CLAMP_TO_BORDER                0x812D
#endif

#ifndef GL_VERSION_1_4
#define GL_BLEND_DST_RGB                  0x80C8
#define GL_BLEND_SRC_RGB                  0x80C9
#define GL_BLEND_DST_ALPHA                0x80CA
#define GL_BLEND_SRC_ALPHA                0x80CB
#define GL_POINT_FADE_THRESHOLD_SIZE      0x8128
#define GL_DEPTH_COMPONENT16              0x81A5
#define GL_DEPTH_COMPONENT24              0x81A6
#define GL_DEPTH_COMPONENT32              0x81A7
#define GL_MIRRORED_REPEAT                0x8370
#define GL_MAX_TEXTURE_LOD_BIAS           0x84FD
#define GL_TEXTURE_LOD_BIAS               0x8501
#define GL_INCR_WRAP                      0x8507
#define GL_DECR_WRAP                      0x8508
#define GL_TEXTURE_DEPTH_SIZE             0x884A
#define GL_TEXTURE_COMPARE_MODE           0x884C
#define GL_TEXTURE_COMPARE_FUNC           0x884D
#endif

#ifndef GL_VERSION_1_5
#define GL_BUFFER_SIZE                    0x8764
#define GL_BUFFER_USAGE                   0x8765
#define GL_QUERY_COUNTER_BITS             0x8864
#define GL_CURRENT_QUERY                  0x8865
#define GL_QUERY_RESULT                   0x8866
#define GL_QUERY_RESULT_AVAILABLE         0x8867
#define GL_ARRAY_BUFFER                   0x8892
#define GL_ELEMENT_ARRAY_BUFFER           0x8893
#define GL_ARRAY_BUFFER_BINDING           0x8894
#define GL_ELEMENT_ARRAY_BUFFER_BINDING   0x8895
#define GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING 0x889F
#define GL_READ_ONLY                      0x88B8
#define GL_WRITE_ONLY                     0x88B9
#define GL_READ_WRITE                     0x88BA
#define GL_BUFFER_ACCESS                  0x88BB
#define GL_BUFFER_MAPPED                  0x88BC
#define GL_BUFFER_MAP_POINTER             0x88BD
#define GL_STREAM_DRAW                    0x88E0
#define GL_STREAM_READ                    0x88E1
#define GL_STREAM_COPY                    0x88E2
#define GL_STATIC_DRAW                    0x88E4
#define GL_STATIC_READ                    0x88E5
#define GL_STATIC_COPY                    0x88E6
#define GL_DYNAMIC_DRAW                   0x88E8
#define GL_DYNAMIC_READ                   0x88E9
#define GL_DYNAMIC_COPY                   0x88EA
#define GL_SAMPLES_PASSED                 0x8914
#endif

#ifndef GL_VERSION_2_0
#define GL_BLEND_EQUATION_RGB             0x8009
#define GL_VERTEX_ATTRIB_ARRAY_ENABLED    0x8622
#define GL_VERTEX_ATTRIB_ARRAY_SIZE       0x8623
#define GL_VERTEX_ATTRIB_ARRAY_STRIDE     0x8624
#define GL_VERTEX_ATTRIB_ARRAY_TYPE       0x8625
#define GL_CURRENT_VERTEX_ATTRIB          0x8626
#define GL_VERTEX_PROGRAM_POINT_SIZE      0x8642
#define GL_VERTEX_ATTRIB_ARRAY_POINTER    0x8645
#define GL_STENCIL_BACK_FUNC              0x8800
#define GL_STENCIL_BACK_FAIL              0x8801
#define GL_STENCIL_BACK_PASS_DEPTH_FAIL   0x8802
#define GL_STENCIL_BACK_PASS_DEPTH_PASS   0x8803
#define GL_MAX_DRAW_BUFFERS               0x8824
#define GL_DRAW_BUFFER0                   0x8825
#define GL_DRAW_BUFFER1                   0x8826
#define GL_DRAW_BUFFER2                   0x8827
#define GL_DRAW_BUFFER3                   0x8828
#define GL_DRAW_BUFFER4                   0x8829
#define GL_DRAW_BUFFER5                   0x882A
#define GL_DRAW_BUFFER6                   0x882B
#define GL_DRAW_BUFFER7                   0x882C
#define GL_DRAW_BUFFER8                   0x882D
#define GL_DRAW_BUFFER9                   0x882E
#define GL_DRAW_BUFFER10                  0x882F
#define GL_DRAW_BUFFER11                  0x8830
#define GL_DRAW_BUFFER12                  0x8831
#define GL_DRAW_BUFFER13                  0x8832
#define GL_DRAW_BUFFER14                  0x8833
#define GL_DRAW_BUFFER15                  0x8834
#define GL_BLEND_EQUATION_ALPHA           0x883D
#define GL_MAX_VERTEX_ATTRIBS             0x8869
#define GL_VERTEX_ATTRIB_ARRAY_NORMALIZED 0x886A
#define GL_MAX_TEXTURE_IMAGE_UNITS        0x8872
#define GL_FRAGMENT_SHADER                0x8B30
#define GL_VERTEX_SHADER                  0x8B31
#define GL_MAX_FRAGMENT_UNIFORM_COMPONENTS 0x8B49
#define GL_MAX_VERTEX_UNIFORM_COMPONENTS  0x8B4A
#define GL_MAX_VARYING_FLOATS             0x8B4B
#define GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS 0x8B4C
#define GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS 0x8B4D
#define GL_SHADER_TYPE                    0x8B4F
#define GL_FLOAT_VEC2                     0x8B50
#define GL_FLOAT_VEC3                     0x8B51
#define GL_FLOAT_VEC4                     0x8B52
#define GL_INT_VEC2                       0x8B53
#define GL_INT_VEC3                       0x8B54
#define GL_INT_VEC4                       0x8B55
#define GL_BOOL                           0x8B56
#define GL_BOOL_VEC2                      0x8B57
#define GL_BOOL_VEC3                      0x8B58
#define GL_BOOL_VEC4                      0x8B59
#define GL_FLOAT_MAT2                     0x8B5A
#define GL_FLOAT_MAT3                     0x8B5B
#define GL_FLOAT_MAT4                     0x8B5C
#define GL_SAMPLER_1D                     0x8B5D
#define GL_SAMPLER_2D                     0x8B5E
#define GL_SAMPLER_3D                     0x8B5F
#define GL_SAMPLER_CUBE                   0x8B60
#define GL_SAMPLER_1D_SHADOW              0x8B61
#define GL_SAMPLER_2D_SHADOW              0x8B62
#define GL_DELETE_STATUS                  0x8B80
#define GL_COMPILE_STATUS                 0x8B81
#define GL_LINK_STATUS                    0x8B82
#define GL_VALIDATE_STATUS                0x8B83
#define GL_INFO_LOG_LENGTH                0x8B84
#define GL_ATTACHED_SHADERS               0x8B85
#define GL_ACTIVE_UNIFORMS                0x8B86
#define GL_ACTIVE_UNIFORM_MAX_LENGTH      0x8B87
#define GL_SHADER_SOURCE_LENGTH           0x8B88
#define GL_ACTIVE_ATTRIBUTES              0x8B89
#define GL_ACTIVE_ATTRIBUTE_MAX_LENGTH    0x8B8A
#define GL_FRAGMENT_SHADER_DERIVATIVE_HINT 0x8B8B
#define GL_SHADING_LANGUAGE_VERSION       0x8B8C
#define GL_CURRENT_PROGRAM                0x8B8D
#define GL_POINT_SPRITE_COORD_ORIGIN      0x8CA0
#define GL_LOWER_LEFT                     0x8CA1
#define GL_UPPER_LEFT                     0x8CA2
#define GL_STENCIL_BACK_REF               0x8CA3
#define GL_STENCIL_BACK_VALUE_MASK        0x8CA4
#define GL_STENCIL_BACK_WRITEMASK         0x8CA5
#endif

#ifndef GL_VERSION_2_1
#define GL_PIXEL_PACK_BUFFER              0x88EB
#define GL_PIXEL_UNPACK_BUFFER            0x88EC
#define GL_PIXEL_PACK_BUFFER_BINDING      0x88ED
#define GL_PIXEL_UNPACK_BUFFER_BINDING    0x88EF
#define GL_FLOAT_MAT2x3                   0x8B65
#define GL_FLOAT_MAT2x4                   0x8B66
#define GL_FLOAT_MAT3x2                   0x8B67
#define GL_FLOAT_MAT3x4                   0x8B68
#define GL_FLOAT_MAT4x2                   0x8B69
#define GL_FLOAT_MAT4x3                   0x8B6A
#define GL_SRGB                           0x8C40
#define GL_SRGB8                          0x8C41
#define GL_SRGB_ALPHA                     0x8C42
#define GL_SRGB8_ALPHA8                   0x8C43
#define GL_COMPRESSED_SRGB                0x8C48
#define GL_COMPRESSED_SRGB_ALPHA          0x8C49
#endif

#ifndef GL_VERSION_3_0
#define GL_COMPARE_REF_TO_TEXTURE         0x884E
#define GL_CLIP_DISTANCE0                 0x3000
#define GL_CLIP_DISTANCE1                 0x3001
#define GL_CLIP_DISTANCE2                 0x3002
#define GL_CLIP_DISTANCE3                 0x3003
#define GL_CLIP_DISTANCE4                 0x3004
#define GL_CLIP_DISTANCE5                 0x3005
#define GL_MAX_CLIP_DISTANCES             0x0D32
#define GL_MAJOR_VERSION                  0x821B
#define GL_MINOR_VERSION                  0x821C
#define GL_NUM_EXTENSIONS                 0x821D
#define GL_CONTEXT_FLAGS                  0x821E
#define GL_DEPTH_BUFFER                   0x8223
#define GL_STENCIL_BUFFER                 0x8224
#define GL_COMPRESSED_RED                 0x8225
#define GL_COMPRESSED_RG                  0x8226
#define GL_CONTEXT_FLAG_FORWARD_COMPATIBLE_BIT 0x0001
#define GL_RGBA32F                        0x8814
#define GL_RGB32F                         0x8815
#define GL_RGBA16F                        0x881A
#define GL_RGB16F                         0x881B
#define GL_VERTEX_ATTRIB_ARRAY_INTEGER    0x88FD
#define GL_MAX_ARRAY_TEXTURE_LAYERS       0x88FF
#define GL_MIN_PROGRAM_TEXEL_OFFSET       0x8904
#define GL_MAX_PROGRAM_TEXEL_OFFSET       0x8905
#define GL_CLAMP_READ_COLOR               0x891C
#define GL_FIXED_ONLY                     0x891D
#define GL_MAX_VARYING_COMPONENTS         0x8B4B
#define GL_TEXTURE_1D_ARRAY               0x8C18
#define GL_PROXY_TEXTURE_1D_ARRAY         0x8C19
#define GL_TEXTURE_2D_ARRAY               0x8C1A
#define GL_PROXY_TEXTURE_2D_ARRAY         0x8C1B
#define GL_TEXTURE_BINDING_1D_ARRAY       0x8C1C
#define GL_TEXTURE_BINDING_2D_ARRAY       0x8C1D
#define GL_R11F_G11F_B10F                 0x8C3A
#define GL_UNSIGNED_INT_10F_11F_11F_REV   0x8C3B
#define GL_RGB9_E5                        0x8C3D
#define GL_UNSIGNED_INT_5_9_9_9_REV       0x8C3E
#define GL_TEXTURE_SHARED_SIZE            0x8C3F
#define GL_TRANSFORM_FEEDBACK_VARYING_MAX_LENGTH 0x8C76
#define GL_TRANSFORM_FEEDBACK_BUFFER_MODE 0x8C7F
#define GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_COMPONENTS 0x8C80
#define GL_TRANSFORM_FEEDBACK_VARYINGS    0x8C83
#define GL_TRANSFORM_FEEDBACK_BUFFER_START 0x8C84
#define GL_TRANSFORM_FEEDBACK_BUFFER_SIZE 0x8C85
#define GL_PRIMITIVES_GENERATED           0x8C87
#define GL_TRANSFORM_FEEDBACK_PRIMITIVES_WRITTEN 0x8C88
#define GL_RASTERIZER_DISCARD             0x8C89
#define GL_MAX_TRANSFORM_FEEDBACK_INTERLEAVED_COMPONENTS 0x8C8A
#define GL_MAX_TRANSFORM_FEEDBACK_SEPARATE_ATTRIBS 0x8C8B
#define GL_INTERLEAVED_ATTRIBS            0x8C8C
#define GL_SEPARATE_ATTRIBS               0x8C8D
#define GL_TRANSFORM_FEEDBACK_BUFFER      0x8C8E
#define GL_TRANSFORM_FEEDBACK_BUFFER_BINDING 0x8C8F
#define GL_RGBA32UI                       0x8D70
#define GL_RGB32UI                        0x8D71
#define GL_RGBA16UI                       0x8D76
#define GL_RGB16UI                        0x8D77
#define GL_RGBA8UI                        0x8D7C
#define GL_RGB8UI                         0x8D7D
#define GL_RGBA32I                        0x8D82
#define GL_RGB32I                         0x8D83
#define GL_RGBA16I                        0x8D88
#define GL_RGB16I                         0x8D89
#define GL_RGBA8I                         0x8D8E
#define GL_RGB8I                          0x8D8F
#define GL_RED_INTEGER                    0x8D94
#define GL_GREEN_INTEGER                  0x8D95
#define GL_BLUE_INTEGER                   0x8D96
#define GL_RGB_INTEGER                    0x8D98
#define GL_RGBA_INTEGER                   0x8D99
#define GL_BGR_INTEGER                    0x8D9A
#define GL_BGRA_INTEGER                   0x8D9B
#define GL_SAMPLER_1D_ARRAY               0x8DC0
#define GL_SAMPLER_2D_ARRAY               0x8DC1
#define GL_SAMPLER_1D_ARRAY_SHADOW        0x8DC3
#define GL_SAMPLER_2D_ARRAY_SHADOW        0x8DC4
#define GL_SAMPLER_CUBE_SHADOW            0x8DC5
#define GL_UNSIGNED_INT_VEC2              0x8DC6
#define GL_UNSIGNED_INT_VEC3              0x8DC7
#define GL_UNSIGNED_INT_VEC4              0x8DC8
#define GL_INT_SAMPLER_1D                 0x8DC9
#define GL_INT_SAMPLER_2D                 0x8DCA
#define GL_INT_SAMPLER_3D                 0x8DCB
#define GL_INT_SAMPLER_CUBE               0x8DCC
#define GL_INT_SAMPLER_1D_ARRAY           0x8DCE
#define GL_INT_SAMPLER_2D_ARRAY           0x8DCF
#define GL_UNSIGNED_INT_SAMPLER_1D        0x8DD1
#define GL_UNSIGNED_INT_SAMPLER_2D        0x8DD2
#define GL_UNSIGNED_INT_SAMPLER_3D        0x8DD3
#define GL_UNSIGNED_INT_SAMPLER_CUBE      0x8DD4
#define GL_UNSIGNED_INT_SAMPLER_1D_ARRAY  0x8DD6
#define GL_UNSIGNED_INT_SAMPLER_2D_ARRAY  0x8DD7
#define GL_QUERY_WAIT                     0x8E13
#define GL_QUERY_NO_WAIT                  0x8E14
#define GL_QUERY_BY_REGION_WAIT           0x8E15
#define GL_QUERY_BY_REGION_NO_WAIT        0x8E16
/* Reuse tokens from ARB_depth_buffer_float */
/* reuse GL_DEPTH_COMPONENT32F */
/* reuse GL_DEPTH32F_STENCIL8 */
/* reuse GL_FLOAT_32_UNSIGNED_INT_24_8_REV */
/* Reuse tokens from ARB_framebuffer_object */
/* reuse GL_INVALID_FRAMEBUFFER_OPERATION */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE */
/* reuse GL_FRAMEBUFFER_DEFAULT */
/* reuse GL_FRAMEBUFFER_UNDEFINED */
/* reuse GL_DEPTH_STENCIL_ATTACHMENT */
/* reuse GL_INDEX */
/* reuse GL_MAX_RENDERBUFFER_SIZE */
/* reuse GL_DEPTH_STENCIL */
/* reuse GL_UNSIGNED_INT_24_8 */
/* reuse GL_DEPTH24_STENCIL8 */
/* reuse GL_TEXTURE_STENCIL_SIZE */
/* reuse GL_TEXTURE_RED_TYPE */
/* reuse GL_TEXTURE_GREEN_TYPE */
/* reuse GL_TEXTURE_BLUE_TYPE */
/* reuse GL_TEXTURE_ALPHA_TYPE */
/* reuse GL_TEXTURE_DEPTH_TYPE */
/* reuse GL_UNSIGNED_NORMALIZED */
/* reuse GL_FRAMEBUFFER_BINDING */
/* reuse GL_DRAW_FRAMEBUFFER_BINDING */
/* reuse GL_RENDERBUFFER_BINDING */
/* reuse GL_READ_FRAMEBUFFER */
/* reuse GL_DRAW_FRAMEBUFFER */
/* reuse GL_READ_FRAMEBUFFER_BINDING */
/* reuse GL_RENDERBUFFER_SAMPLES */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE */
/* reuse GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER */
/* reuse GL_FRAMEBUFFER_COMPLETE */
/* reuse GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT */
/* reuse GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT */
/* reuse GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER */
/* reuse GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER */
/* reuse GL_FRAMEBUFFER_UNSUPPORTED */
/* reuse GL_MAX_COLOR_ATTACHMENTS */
/* reuse GL_COLOR_ATTACHMENT0 */
/* reuse GL_COLOR_ATTACHMENT1 */
/* reuse GL_COLOR_ATTACHMENT2 */
/* reuse GL_COLOR_ATTACHMENT3 */
/* reuse GL_COLOR_ATTACHMENT4 */
/* reuse GL_COLOR_ATTACHMENT5 */
/* reuse GL_COLOR_ATTACHMENT6 */
/* reuse GL_COLOR_ATTACHMENT7 */
/* reuse GL_COLOR_ATTACHMENT8 */
/* reuse GL_COLOR_ATTACHMENT9 */
/* reuse GL_COLOR_ATTACHMENT10 */
/* reuse GL_COLOR_ATTACHMENT11 */
/* reuse GL_COLOR_ATTACHMENT12 */
/* reuse GL_COLOR_ATTACHMENT13 */
/* reuse GL_COLOR_ATTACHMENT14 */
/* reuse GL_COLOR_ATTACHMENT15 */
/* reuse GL_DEPTH_ATTACHMENT */
/* reuse GL_STENCIL_ATTACHMENT */
/* reuse GL_FRAMEBUFFER */
/* reuse GL_RENDERBUFFER */
/* reuse GL_RENDERBUFFER_WIDTH */
/* reuse GL_RENDERBUFFER_HEIGHT */
/* reuse GL_RENDERBUFFER_INTERNAL_FORMAT */
/* reuse GL_STENCIL_INDEX1 */
/* reuse GL_STENCIL_INDEX4 */
/* reuse GL_STENCIL_INDEX8 */
/* reuse GL_STENCIL_INDEX16 */
/* reuse GL_RENDERBUFFER_RED_SIZE */
/* reuse GL_RENDERBUFFER_GREEN_SIZE */
/* reuse GL_RENDERBUFFER_BLUE_SIZE */
/* reuse GL_RENDERBUFFER_ALPHA_SIZE */
/* reuse GL_RENDERBUFFER_DEPTH_SIZE */
/* reuse GL_RENDERBUFFER_STENCIL_SIZE */
/* reuse GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE */
/* reuse GL_MAX_SAMPLES */
/* Reuse tokens from ARB_framebuffer_sRGB */
/* reuse GL_FRAMEBUFFER_SRGB */
/* Reuse tokens from ARB_half_float_vertex */
/* reuse GL_HALF_FLOAT */
/* Reuse tokens from ARB_map_buffer_range */
/* reuse GL_MAP_READ_BIT */
/* reuse GL_MAP_WRITE_BIT */
/* reuse GL_MAP_INVALIDATE_RANGE_BIT */
/* reuse GL_MAP_INVALIDATE_BUFFER_BIT */
/* reuse GL_MAP_FLUSH_EXPLICIT_BIT */
/* reuse GL_MAP_UNSYNCHRONIZED_BIT */
/* Reuse tokens from ARB_texture_compression_rgtc */
/* reuse GL_COMPRESSED_RED_RGTC1 */
/* reuse GL_COMPRESSED_SIGNED_RED_RGTC1 */
/* reuse GL_COMPRESSED_RG_RGTC2 */
/* reuse GL_COMPRESSED_SIGNED_RG_RGTC2 */
/* Reuse tokens from ARB_texture_rg */
/* reuse GL_RG */
/* reuse GL_RG_INTEGER */
/* reuse GL_R8 */
/* reuse GL_R16 */
/* reuse GL_RG8 */
/* reuse GL_RG16 */
/* reuse GL_R16F */
/* reuse GL_R32F */
/* reuse GL_RG16F */
/* reuse GL_RG32F */
/* reuse GL_R8I */
/* reuse GL_R8UI */
/* reuse GL_R16I */
/* reuse GL_R16UI */
/* reuse GL_R32I */
/* reuse GL_R32UI */
/* reuse GL_RG8I */
/* reuse GL_RG8UI */
/* reuse GL_RG16I */
/* reuse GL_RG16UI */
/* reuse GL_RG32I */
/* reuse GL_RG32UI */
/* Reuse tokens from ARB_vertex_array_object */
/* reuse GL_VERTEX_ARRAY_BINDING */
#endif

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

#ifndef GL_ARB_framebuffer_object
#define GL_INVALID_FRAMEBUFFER_OPERATION  0x0506
#define GL_FRAMEBUFFER_ATTACHMENT_COLOR_ENCODING 0x8210
#define GL_FRAMEBUFFER_ATTACHMENT_COMPONENT_TYPE 0x8211
#define GL_FRAMEBUFFER_ATTACHMENT_RED_SIZE 0x8212
#define GL_FRAMEBUFFER_ATTACHMENT_GREEN_SIZE 0x8213
#define GL_FRAMEBUFFER_ATTACHMENT_BLUE_SIZE 0x8214
#define GL_FRAMEBUFFER_ATTACHMENT_ALPHA_SIZE 0x8215
#define GL_FRAMEBUFFER_ATTACHMENT_DEPTH_SIZE 0x8216
#define GL_FRAMEBUFFER_ATTACHMENT_STENCIL_SIZE 0x8217
#define GL_FRAMEBUFFER_DEFAULT            0x8218
#define GL_FRAMEBUFFER_UNDEFINED          0x8219
#define GL_DEPTH_STENCIL_ATTACHMENT       0x821A
#define GL_MAX_RENDERBUFFER_SIZE          0x84E8
#define GL_DEPTH_STENCIL                  0x84F9
#define GL_UNSIGNED_INT_24_8              0x84FA
#define GL_DEPTH24_STENCIL8               0x88F0
#define GL_TEXTURE_STENCIL_SIZE           0x88F1
#define GL_TEXTURE_RED_TYPE               0x8C10
#define GL_TEXTURE_GREEN_TYPE             0x8C11
#define GL_TEXTURE_BLUE_TYPE              0x8C12
#define GL_TEXTURE_ALPHA_TYPE             0x8C13
#define GL_TEXTURE_DEPTH_TYPE             0x8C16
#define GL_UNSIGNED_NORMALIZED            0x8C17
#define GL_FRAMEBUFFER_BINDING            0x8CA6
#define GL_DRAW_FRAMEBUFFER_BINDING       GL_FRAMEBUFFER_BINDING
#define GL_RENDERBUFFER_BINDING           0x8CA7
#define GL_READ_FRAMEBUFFER               0x8CA8
#define GL_DRAW_FRAMEBUFFER               0x8CA9
#define GL_READ_FRAMEBUFFER_BINDING       0x8CAA
#define GL_RENDERBUFFER_SAMPLES           0x8CAB
#define GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE 0x8CD0
#define GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME 0x8CD1
#define GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL 0x8CD2
#define GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE 0x8CD3
#define GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LAYER 0x8CD4
#define GL_FRAMEBUFFER_COMPLETE           0x8CD5
#define GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT 0x8CD6
#define GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT 0x8CD7
#define GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER 0x8CDB
#define GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER 0x8CDC
#define GL_FRAMEBUFFER_UNSUPPORTED        0x8CDD
#define GL_MAX_COLOR_ATTACHMENTS          0x8CDF
#define GL_COLOR_ATTACHMENT0              0x8CE0
#define GL_COLOR_ATTACHMENT1              0x8CE1
#define GL_COLOR_ATTACHMENT2              0x8CE2
#define GL_COLOR_ATTACHMENT3              0x8CE3
#define GL_COLOR_ATTACHMENT4              0x8CE4
#define GL_COLOR_ATTACHMENT5              0x8CE5
#define GL_COLOR_ATTACHMENT6              0x8CE6
#define GL_COLOR_ATTACHMENT7              0x8CE7
#define GL_COLOR_ATTACHMENT8              0x8CE8
#define GL_COLOR_ATTACHMENT9              0x8CE9
#define GL_COLOR_ATTACHMENT10             0x8CEA
#define GL_COLOR_ATTACHMENT11             0x8CEB
#define GL_COLOR_ATTACHMENT12             0x8CEC
#define GL_COLOR_ATTACHMENT13             0x8CED
#define GL_COLOR_ATTACHMENT14             0x8CEE
#define GL_COLOR_ATTACHMENT15             0x8CEF
#define GL_DEPTH_ATTACHMENT               0x8D00
#define GL_STENCIL_ATTACHMENT             0x8D20
#define GL_FRAMEBUFFER                    0x8D40
#define GL_RENDERBUFFER                   0x8D41
#define GL_RENDERBUFFER_WIDTH             0x8D42
#define GL_RENDERBUFFER_HEIGHT            0x8D43
#define GL_RENDERBUFFER_INTERNAL_FORMAT   0x8D44
#define GL_STENCIL_INDEX1                 0x8D46
#define GL_STENCIL_INDEX4                 0x8D47
#define GL_STENCIL_INDEX8                 0x8D48
#define GL_STENCIL_INDEX16                0x8D49
#define GL_RENDERBUFFER_RED_SIZE          0x8D50
#define GL_RENDERBUFFER_GREEN_SIZE        0x8D51
#define GL_RENDERBUFFER_BLUE_SIZE         0x8D52
#define GL_RENDERBUFFER_ALPHA_SIZE        0x8D53
#define GL_RENDERBUFFER_DEPTH_SIZE        0x8D54
#define GL_RENDERBUFFER_STENCIL_SIZE      0x8D55
#define GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE 0x8D56
#define GL_MAX_SAMPLES                    0x8D57
#endif

#ifndef GL_ARB_map_buffer_range
#define GL_MAP_READ_BIT                   0x0001
#define GL_MAP_WRITE_BIT                  0x0002
#define GL_MAP_INVALIDATE_RANGE_BIT       0x0004
#define GL_MAP_INVALIDATE_BUFFER_BIT      0x0008
#define GL_MAP_FLUSH_EXPLICIT_BIT         0x0010
#define GL_MAP_UNSYNCHRONIZED_BIT         0x0020
#endif

#ifndef GL_ARB_vertex_array_object
#define GL_VERTEX_ARRAY_BINDING           0x85B5
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
#define GL_INVALID_INDEX                  0xFFFFFFFFu
#endif

#ifndef GL_ARB_copy_buffer
#define GL_COPY_READ_BUFFER               0x8F36
#define GL_COPY_WRITE_BUFFER              0x8F37
#endif


/*************************************************************/

#include <stddef.h>
#ifndef GL_VERSION_2_0
/* GL type for program/shader text */
typedef char GLchar;
#endif

#ifndef GL_VERSION_1_5
/* GL types for handling large vertex buffer objects */
typedef ptrdiff_t GLintptr;
typedef ptrdiff_t GLsizeiptr;
#endif

#ifndef GL_ARB_vertex_buffer_object
/* GL types for handling large vertex buffer objects */
typedef ptrdiff_t GLintptrARB;
typedef ptrdiff_t GLsizeiptrARB;
#endif

#ifndef GL_ARB_shader_objects
/* GL types for program/shader text and shader object handles */
typedef char GLcharARB;
typedef unsigned int GLhandleARB;
#endif

/* GL type for "half" precision (s10e5) float data in host memory */
#ifndef GL_ARB_half_float_pixel
typedef unsigned short GLhalfARB;
#endif

#ifndef GL_NV_half_float
typedef unsigned short GLhalfNV;
#endif

#include "glext-64bit-types.h"

#ifndef GL_VERSION_1_0
#define GL_VERSION_1_0 1
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glCullFace (GLenum);
GLAPI void GLAPIENTRY glFrontFace (GLenum);
GLAPI void GLAPIENTRY glHint (GLenum, GLenum);
GLAPI void GLAPIENTRY glLineWidth (GLfloat);
GLAPI void GLAPIENTRY glPointSize (GLfloat);
GLAPI void GLAPIENTRY glPolygonMode (GLenum, GLenum);
GLAPI void GLAPIENTRY glScissor (GLint, GLint, GLsizei, GLsizei);
GLAPI void GLAPIENTRY glTexParameterf (GLenum, GLenum, GLfloat);
GLAPI void GLAPIENTRY glTexParameterfv (GLenum, GLenum, const GLfloat *);
GLAPI void GLAPIENTRY glTexParameteri (GLenum, GLenum, GLint);
GLAPI void GLAPIENTRY glTexParameteriv (GLenum, GLenum, const GLint *);
GLAPI void GLAPIENTRY glTexImage1D (GLenum, GLint, GLint, GLsizei, GLint, GLenum, GLenum, const GLvoid *);
GLAPI void GLAPIENTRY glTexImage2D (GLenum, GLint, GLint, GLsizei, GLsizei, GLint, GLenum, GLenum, const GLvoid *);
GLAPI void GLAPIENTRY glDrawBuffer (GLenum);
GLAPI void GLAPIENTRY glClear (GLbitfield);
GLAPI void GLAPIENTRY glClearColor (GLclampf, GLclampf, GLclampf, GLclampf);
GLAPI void GLAPIENTRY glClearStencil (GLint);
GLAPI void GLAPIENTRY glClearDepth (GLclampd);
GLAPI void GLAPIENTRY glStencilMask (GLuint);
GLAPI void GLAPIENTRY glColorMask (GLboolean, GLboolean, GLboolean, GLboolean);
GLAPI void GLAPIENTRY glDepthMask (GLboolean);
GLAPI void GLAPIENTRY glDisable (GLenum);
GLAPI void GLAPIENTRY glEnable (GLenum);
GLAPI void GLAPIENTRY glFinish (void);
GLAPI void GLAPIENTRY glFlush (void);
GLAPI void GLAPIENTRY glBlendFunc (GLenum, GLenum);
GLAPI void GLAPIENTRY glLogicOp (GLenum);
GLAPI void GLAPIENTRY glStencilFunc (GLenum, GLint, GLuint);
GLAPI void GLAPIENTRY glStencilOp (GLenum, GLenum, GLenum);
GLAPI void GLAPIENTRY glDepthFunc (GLenum);
GLAPI void GLAPIENTRY glPixelStoref (GLenum, GLfloat);
GLAPI void GLAPIENTRY glPixelStorei (GLenum, GLint);
GLAPI void GLAPIENTRY glReadBuffer (GLenum);
GLAPI void GLAPIENTRY glReadPixels (GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, GLvoid *);
GLAPI void GLAPIENTRY glGetBooleanv (GLenum, GLboolean *);
GLAPI void GLAPIENTRY glGetDoublev (GLenum, GLdouble *);
GLAPI GLenum GLAPIENTRY glGetError (void);
GLAPI void GLAPIENTRY glGetFloatv (GLenum, GLfloat *);
GLAPI void GLAPIENTRY glGetIntegerv (GLenum, GLint *);
GLAPI const GLubyte * GLAPIENTRY glGetString (GLenum);
GLAPI void GLAPIENTRY glGetTexImage (GLenum, GLint, GLenum, GLenum, GLvoid *);
GLAPI void GLAPIENTRY glGetTexParameterfv (GLenum, GLenum, GLfloat *);
GLAPI void GLAPIENTRY glGetTexParameteriv (GLenum, GLenum, GLint *);
GLAPI void GLAPIENTRY glGetTexLevelParameterfv (GLenum, GLint, GLenum, GLfloat *);
GLAPI void GLAPIENTRY glGetTexLevelParameteriv (GLenum, GLint, GLenum, GLint *);
GLAPI GLboolean GLAPIENTRY glIsEnabled (GLenum);
GLAPI void GLAPIENTRY glDepthRange (GLclampd, GLclampd);
GLAPI void GLAPIENTRY glViewport (GLint, GLint, GLsizei, GLsizei);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLCULLFACEPROC) (GLenum mode);
typedef void (GLAPIENTRYP PFNGLFRONTFACEPROC) (GLenum mode);
typedef void (GLAPIENTRYP PFNGLHINTPROC) (GLenum target, GLenum mode);
typedef void (GLAPIENTRYP PFNGLLINEWIDTHPROC) (GLfloat width);
typedef void (GLAPIENTRYP PFNGLPOINTSIZEPROC) (GLfloat size);
typedef void (GLAPIENTRYP PFNGLPOLYGONMODEPROC) (GLenum face, GLenum mode);
typedef void (GLAPIENTRYP PFNGLSCISSORPROC) (GLint x, GLint y, GLsizei width, GLsizei height);
typedef void (GLAPIENTRYP PFNGLTEXPARAMETERFPROC) (GLenum target, GLenum pname, GLfloat param);
typedef void (GLAPIENTRYP PFNGLTEXPARAMETERFVPROC) (GLenum target, GLenum pname, const GLfloat *params);
typedef void (GLAPIENTRYP PFNGLTEXPARAMETERIPROC) (GLenum target, GLenum pname, GLint param);
typedef void (GLAPIENTRYP PFNGLTEXPARAMETERIVPROC) (GLenum target, GLenum pname, const GLint *params);
typedef void (GLAPIENTRYP PFNGLTEXIMAGE1DPROC) (GLenum target, GLint level, GLint internalformat, GLsizei width, GLint border, GLenum format, GLenum type, const GLvoid *pixels);
typedef void (GLAPIENTRYP PFNGLTEXIMAGE2DPROC) (GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const GLvoid *pixels);
typedef void (GLAPIENTRYP PFNGLDRAWBUFFERPROC) (GLenum mode);
typedef void (GLAPIENTRYP PFNGLCLEARPROC) (GLbitfield mask);
typedef void (GLAPIENTRYP PFNGLCLEARCOLORPROC) (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
typedef void (GLAPIENTRYP PFNGLCLEARSTENCILPROC) (GLint s);
typedef void (GLAPIENTRYP PFNGLCLEARDEPTHPROC) (GLclampd depth);
typedef void (GLAPIENTRYP PFNGLSTENCILMASKPROC) (GLuint mask);
typedef void (GLAPIENTRYP PFNGLCOLORMASKPROC) (GLboolean red, GLboolean green, GLboolean blue, GLboolean alpha);
typedef void (GLAPIENTRYP PFNGLDEPTHMASKPROC) (GLboolean flag);
typedef void (GLAPIENTRYP PFNGLDISABLEPROC) (GLenum cap);
typedef void (GLAPIENTRYP PFNGLENABLEPROC) (GLenum cap);
typedef void (GLAPIENTRYP PFNGLFINISHPROC) (void);
typedef void (GLAPIENTRYP PFNGLFLUSHPROC) (void);
typedef void (GLAPIENTRYP PFNGLBLENDFUNCPROC) (GLenum sfactor, GLenum dfactor);
typedef void (GLAPIENTRYP PFNGLLOGICOPPROC) (GLenum opcode);
typedef void (GLAPIENTRYP PFNGLSTENCILFUNCPROC) (GLenum func, GLint ref, GLuint mask);
typedef void (GLAPIENTRYP PFNGLSTENCILOPPROC) (GLenum fail, GLenum zfail, GLenum zpass);
typedef void (GLAPIENTRYP PFNGLDEPTHFUNCPROC) (GLenum func);
typedef void (GLAPIENTRYP PFNGLPIXELSTOREFPROC) (GLenum pname, GLfloat param);
typedef void (GLAPIENTRYP PFNGLPIXELSTOREIPROC) (GLenum pname, GLint param);
typedef void (GLAPIENTRYP PFNGLREADBUFFERPROC) (GLenum mode);
typedef void (GLAPIENTRYP PFNGLREADPIXELSPROC) (GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, GLvoid *pixels);
typedef void (GLAPIENTRYP PFNGLGETBOOLEANVPROC) (GLenum pname, GLboolean *params);
typedef void (GLAPIENTRYP PFNGLGETDOUBLEVPROC) (GLenum pname, GLdouble *params);
typedef GLenum (GLAPIENTRYP PFNGLGETERRORPROC) (void);
typedef void (GLAPIENTRYP PFNGLGETFLOATVPROC) (GLenum pname, GLfloat *params);
typedef void (GLAPIENTRYP PFNGLGETINTEGERVPROC) (GLenum pname, GLint *params);
typedef const GLubyte * (GLAPIENTRYP PFNGLGETSTRINGPROC) (GLenum name);
typedef void (GLAPIENTRYP PFNGLGETTEXIMAGEPROC) (GLenum target, GLint level, GLenum format, GLenum type, GLvoid *pixels);
typedef void (GLAPIENTRYP PFNGLGETTEXPARAMETERFVPROC) (GLenum target, GLenum pname, GLfloat *params);
typedef void (GLAPIENTRYP PFNGLGETTEXPARAMETERIVPROC) (GLenum target, GLenum pname, GLint *params);
typedef void (GLAPIENTRYP PFNGLGETTEXLEVELPARAMETERFVPROC) (GLenum target, GLint level, GLenum pname, GLfloat *params);
typedef void (GLAPIENTRYP PFNGLGETTEXLEVELPARAMETERIVPROC) (GLenum target, GLint level, GLenum pname, GLint *params);
typedef GLboolean (GLAPIENTRYP PFNGLISENABLEDPROC) (GLenum cap);
typedef void (GLAPIENTRYP PFNGLDEPTHRANGEPROC) (GLclampd near_val, GLclampd far_val); /* MSVC can't handle near or far var names */
typedef void (GLAPIENTRYP PFNGLVIEWPORTPROC) (GLint x, GLint y, GLsizei width, GLsizei height);
#endif

#ifndef GL_VERSION_1_1
#define GL_VERSION_1_1 1
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glDrawArrays (GLenum, GLint, GLsizei);
GLAPI void GLAPIENTRY glDrawElements (GLenum, GLsizei, GLenum, const GLvoid *);
GLAPI void GLAPIENTRY glGetPointerv (GLenum, GLvoid* *);
GLAPI void GLAPIENTRY glPolygonOffset (GLfloat, GLfloat);
GLAPI void GLAPIENTRY glCopyTexImage1D (GLenum, GLint, GLenum, GLint, GLint, GLsizei, GLint);
GLAPI void GLAPIENTRY glCopyTexImage2D (GLenum, GLint, GLenum, GLint, GLint, GLsizei, GLsizei, GLint);
GLAPI void GLAPIENTRY glCopyTexSubImage1D (GLenum, GLint, GLint, GLint, GLint, GLsizei);
GLAPI void GLAPIENTRY glCopyTexSubImage2D (GLenum, GLint, GLint, GLint, GLint, GLint, GLsizei, GLsizei);
GLAPI void GLAPIENTRY glTexSubImage1D (GLenum, GLint, GLint, GLsizei, GLenum, GLenum, const GLvoid *);
GLAPI void GLAPIENTRY glTexSubImage2D (GLenum, GLint, GLint, GLint, GLsizei, GLsizei, GLenum, GLenum, const GLvoid *);
GLAPI void GLAPIENTRY glBindTexture (GLenum, GLuint);
GLAPI void GLAPIENTRY glDeleteTextures (GLsizei, const GLuint *);
GLAPI void GLAPIENTRY glGenTextures (GLsizei, GLuint *);
GLAPI GLboolean GLAPIENTRY glIsTexture (GLuint);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLDRAWARRAYSPROC) (GLenum mode, GLint first, GLsizei count);
typedef void (GLAPIENTRYP PFNGLDRAWELEMENTSPROC) (GLenum mode, GLsizei count, GLenum type, const GLvoid *indices);
typedef void (GLAPIENTRYP PFNGLGETPOINTERVPROC) (GLenum pname, GLvoid* *params);
typedef void (GLAPIENTRYP PFNGLPOLYGONOFFSETPROC) (GLfloat factor, GLfloat units);
typedef void (GLAPIENTRYP PFNGLCOPYTEXIMAGE1DPROC) (GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLint border);
typedef void (GLAPIENTRYP PFNGLCOPYTEXIMAGE2DPROC) (GLenum target, GLint level, GLenum internalformat, GLint x, GLint y, GLsizei width, GLsizei height, GLint border);
typedef void (GLAPIENTRYP PFNGLCOPYTEXSUBIMAGE1DPROC) (GLenum target, GLint level, GLint xoffset, GLint x, GLint y, GLsizei width);
typedef void (GLAPIENTRYP PFNGLCOPYTEXSUBIMAGE2DPROC) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint x, GLint y, GLsizei width, GLsizei height);
typedef void (GLAPIENTRYP PFNGLTEXSUBIMAGE1DPROC) (GLenum target, GLint level, GLint xoffset, GLsizei width, GLenum format, GLenum type, const GLvoid *pixels);
typedef void (GLAPIENTRYP PFNGLTEXSUBIMAGE2DPROC) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLenum type, const GLvoid *pixels);
typedef void (GLAPIENTRYP PFNGLBINDTEXTUREPROC) (GLenum target, GLuint texture);
typedef void (GLAPIENTRYP PFNGLDELETETEXTURESPROC) (GLsizei n, const GLuint *textures);
typedef void (GLAPIENTRYP PFNGLGENTEXTURESPROC) (GLsizei n, GLuint *textures);
typedef GLboolean (GLAPIENTRYP PFNGLISTEXTUREPROC) (GLuint texture);
#endif

#ifndef GL_VERSION_1_2
#define GL_VERSION_1_2 1
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glBlendColor (GLclampf, GLclampf, GLclampf, GLclampf);
GLAPI void GLAPIENTRY glBlendEquation (GLenum);
GLAPI void GLAPIENTRY glDrawRangeElements (GLenum, GLuint, GLuint, GLsizei, GLenum, const GLvoid *);
GLAPI void GLAPIENTRY glTexImage3D (GLenum, GLint, GLint, GLsizei, GLsizei, GLsizei, GLint, GLenum, GLenum, const GLvoid *);
GLAPI void GLAPIENTRY glTexSubImage3D (GLenum, GLint, GLint, GLint, GLint, GLsizei, GLsizei, GLsizei, GLenum, GLenum, const GLvoid *);
GLAPI void GLAPIENTRY glCopyTexSubImage3D (GLenum, GLint, GLint, GLint, GLint, GLint, GLint, GLsizei, GLsizei);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLBLENDCOLORPROC) (GLclampf red, GLclampf green, GLclampf blue, GLclampf alpha);
typedef void (GLAPIENTRYP PFNGLBLENDEQUATIONPROC) (GLenum mode);
typedef void (GLAPIENTRYP PFNGLDRAWRANGEELEMENTSPROC) (GLenum mode, GLuint start, GLuint end, GLsizei count, GLenum type, const GLvoid *indices);
typedef void (GLAPIENTRYP PFNGLTEXIMAGE3DPROC) (GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLenum format, GLenum type, const GLvoid *pixels);
typedef void (GLAPIENTRYP PFNGLTEXSUBIMAGE3DPROC) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const GLvoid *pixels);
typedef void (GLAPIENTRYP PFNGLCOPYTEXSUBIMAGE3DPROC) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLint x, GLint y, GLsizei width, GLsizei height);
#endif

#ifndef GL_VERSION_1_3
#define GL_VERSION_1_3 1
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glActiveTexture (GLenum);
GLAPI void GLAPIENTRY glSampleCoverage (GLclampf, GLboolean);
GLAPI void GLAPIENTRY glCompressedTexImage3D (GLenum, GLint, GLenum, GLsizei, GLsizei, GLsizei, GLint, GLsizei, const GLvoid *);
GLAPI void GLAPIENTRY glCompressedTexImage2D (GLenum, GLint, GLenum, GLsizei, GLsizei, GLint, GLsizei, const GLvoid *);
GLAPI void GLAPIENTRY glCompressedTexImage1D (GLenum, GLint, GLenum, GLsizei, GLint, GLsizei, const GLvoid *);
GLAPI void GLAPIENTRY glCompressedTexSubImage3D (GLenum, GLint, GLint, GLint, GLint, GLsizei, GLsizei, GLsizei, GLenum, GLsizei, const GLvoid *);
GLAPI void GLAPIENTRY glCompressedTexSubImage2D (GLenum, GLint, GLint, GLint, GLsizei, GLsizei, GLenum, GLsizei, const GLvoid *);
GLAPI void GLAPIENTRY glCompressedTexSubImage1D (GLenum, GLint, GLint, GLsizei, GLenum, GLsizei, const GLvoid *);
GLAPI void GLAPIENTRY glGetCompressedTexImage (GLenum, GLint, GLvoid *);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLACTIVETEXTUREPROC) (GLenum texture);
typedef void (GLAPIENTRYP PFNGLSAMPLECOVERAGEPROC) (GLclampf value, GLboolean invert);
typedef void (GLAPIENTRYP PFNGLCOMPRESSEDTEXIMAGE3DPROC) (GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLsizei depth, GLint border, GLsizei imageSize, const GLvoid *data);
typedef void (GLAPIENTRYP PFNGLCOMPRESSEDTEXIMAGE2DPROC) (GLenum target, GLint level, GLenum internalformat, GLsizei width, GLsizei height, GLint border, GLsizei imageSize, const GLvoid *data);
typedef void (GLAPIENTRYP PFNGLCOMPRESSEDTEXIMAGE1DPROC) (GLenum target, GLint level, GLenum internalformat, GLsizei width, GLint border, GLsizei imageSize, const GLvoid *data);
typedef void (GLAPIENTRYP PFNGLCOMPRESSEDTEXSUBIMAGE3DPROC) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLint zoffset, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLsizei imageSize, const GLvoid *data);
typedef void (GLAPIENTRYP PFNGLCOMPRESSEDTEXSUBIMAGE2DPROC) (GLenum target, GLint level, GLint xoffset, GLint yoffset, GLsizei width, GLsizei height, GLenum format, GLsizei imageSize, const GLvoid *data);
typedef void (GLAPIENTRYP PFNGLCOMPRESSEDTEXSUBIMAGE1DPROC) (GLenum target, GLint level, GLint xoffset, GLsizei width, GLenum format, GLsizei imageSize, const GLvoid *data);
typedef void (GLAPIENTRYP PFNGLGETCOMPRESSEDTEXIMAGEPROC) (GLenum target, GLint level, GLvoid *img);
#endif

#ifndef GL_VERSION_1_4
#define GL_VERSION_1_4 1
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glBlendFuncSeparate (GLenum, GLenum, GLenum, GLenum);
GLAPI void GLAPIENTRY glMultiDrawArrays (GLenum, GLint *, GLsizei *, GLsizei);
GLAPI void GLAPIENTRY glMultiDrawElements (GLenum, const GLsizei *, GLenum, const GLvoid* *, GLsizei);
GLAPI void GLAPIENTRY glPointParameterf (GLenum, GLfloat);
GLAPI void GLAPIENTRY glPointParameterfv (GLenum, const GLfloat *);
GLAPI void GLAPIENTRY glPointParameteri (GLenum, GLint);
GLAPI void GLAPIENTRY glPointParameteriv (GLenum, const GLint *);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLBLENDFUNCSEPARATEPROC) (GLenum sfactorRGB, GLenum dfactorRGB, GLenum sfactorAlpha, GLenum dfactorAlpha);
typedef void (GLAPIENTRYP PFNGLMULTIDRAWARRAYSPROC) (GLenum mode, GLint *first, GLsizei *count, GLsizei primcount);
typedef void (GLAPIENTRYP PFNGLMULTIDRAWELEMENTSPROC) (GLenum mode, const GLsizei *count, GLenum type, const GLvoid* *indices, GLsizei primcount);
typedef void (GLAPIENTRYP PFNGLPOINTPARAMETERFPROC) (GLenum pname, GLfloat param);
typedef void (GLAPIENTRYP PFNGLPOINTPARAMETERFVPROC) (GLenum pname, const GLfloat *params);
typedef void (GLAPIENTRYP PFNGLPOINTPARAMETERIPROC) (GLenum pname, GLint param);
typedef void (GLAPIENTRYP PFNGLPOINTPARAMETERIVPROC) (GLenum pname, const GLint *params);
#endif

#ifndef GL_VERSION_1_5
#define GL_VERSION_1_5 1
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glGenQueries (GLsizei, GLuint *);
GLAPI void GLAPIENTRY glDeleteQueries (GLsizei, const GLuint *);
GLAPI GLboolean GLAPIENTRY glIsQuery (GLuint);
GLAPI void GLAPIENTRY glBeginQuery (GLenum, GLuint);
GLAPI void GLAPIENTRY glEndQuery (GLenum);
GLAPI void GLAPIENTRY glGetQueryiv (GLenum, GLenum, GLint *);
GLAPI void GLAPIENTRY glGetQueryObjectiv (GLuint, GLenum, GLint *);
GLAPI void GLAPIENTRY glGetQueryObjectuiv (GLuint, GLenum, GLuint *);
GLAPI void GLAPIENTRY glBindBuffer (GLenum, GLuint);
GLAPI void GLAPIENTRY glDeleteBuffers (GLsizei, const GLuint *);
GLAPI void GLAPIENTRY glGenBuffers (GLsizei, GLuint *);
GLAPI GLboolean GLAPIENTRY glIsBuffer (GLuint);
GLAPI void GLAPIENTRY glBufferData (GLenum, GLsizeiptr, const GLvoid *, GLenum);
GLAPI void GLAPIENTRY glBufferSubData (GLenum, GLintptr, GLsizeiptr, const GLvoid *);
GLAPI void GLAPIENTRY glGetBufferSubData (GLenum, GLintptr, GLsizeiptr, GLvoid *);
GLAPI GLvoid* GLAPIENTRY glMapBuffer (GLenum, GLenum);
GLAPI GLboolean GLAPIENTRY glUnmapBuffer (GLenum);
GLAPI void GLAPIENTRY glGetBufferParameteriv (GLenum, GLenum, GLint *);
GLAPI void GLAPIENTRY glGetBufferPointerv (GLenum, GLenum, GLvoid* *);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLGENQUERIESPROC) (GLsizei n, GLuint *ids);
typedef void (GLAPIENTRYP PFNGLDELETEQUERIESPROC) (GLsizei n, const GLuint *ids);
typedef GLboolean (GLAPIENTRYP PFNGLISQUERYPROC) (GLuint id);
typedef void (GLAPIENTRYP PFNGLBEGINQUERYPROC) (GLenum target, GLuint id);
typedef void (GLAPIENTRYP PFNGLENDQUERYPROC) (GLenum target);
typedef void (GLAPIENTRYP PFNGLGETQUERYIVPROC) (GLenum target, GLenum pname, GLint *params);
typedef void (GLAPIENTRYP PFNGLGETQUERYOBJECTIVPROC) (GLuint id, GLenum pname, GLint *params);
typedef void (GLAPIENTRYP PFNGLGETQUERYOBJECTUIVPROC) (GLuint id, GLenum pname, GLuint *params);
typedef void (GLAPIENTRYP PFNGLBINDBUFFERPROC) (GLenum target, GLuint buffer);
typedef void (GLAPIENTRYP PFNGLDELETEBUFFERSPROC) (GLsizei n, const GLuint *buffers);
typedef void (GLAPIENTRYP PFNGLGENBUFFERSPROC) (GLsizei n, GLuint *buffers);
typedef GLboolean (GLAPIENTRYP PFNGLISBUFFERPROC) (GLuint buffer);
typedef void (GLAPIENTRYP PFNGLBUFFERDATAPROC) (GLenum target, GLsizeiptr size, const GLvoid *data, GLenum usage);
typedef void (GLAPIENTRYP PFNGLBUFFERSUBDATAPROC) (GLenum target, GLintptr offset, GLsizeiptr size, const GLvoid *data);
typedef void (GLAPIENTRYP PFNGLGETBUFFERSUBDATAPROC) (GLenum target, GLintptr offset, GLsizeiptr size, GLvoid *data);
typedef GLvoid* (GLAPIENTRYP PFNGLMAPBUFFERPROC) (GLenum target, GLenum access);
typedef GLboolean (GLAPIENTRYP PFNGLUNMAPBUFFERPROC) (GLenum target);
typedef void (GLAPIENTRYP PFNGLGETBUFFERPARAMETERIVPROC) (GLenum target, GLenum pname, GLint *params);
typedef void (GLAPIENTRYP PFNGLGETBUFFERPOINTERVPROC) (GLenum target, GLenum pname, GLvoid* *params);
#endif

#ifndef GL_VERSION_2_0
#define GL_VERSION_2_0 1
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glBlendEquationSeparate (GLenum, GLenum);
GLAPI void GLAPIENTRY glDrawBuffers (GLsizei, const GLenum *);
GLAPI void GLAPIENTRY glStencilOpSeparate (GLenum, GLenum, GLenum, GLenum);
GLAPI void GLAPIENTRY glStencilFuncSeparate (GLenum, GLenum, GLint, GLuint);
GLAPI void GLAPIENTRY glStencilMaskSeparate (GLenum, GLuint);
GLAPI void GLAPIENTRY glAttachShader (GLuint, GLuint);
GLAPI void GLAPIENTRY glBindAttribLocation (GLuint, GLuint, const GLchar *);
GLAPI void GLAPIENTRY glCompileShader (GLuint);
GLAPI GLuint GLAPIENTRY glCreateProgram (void);
GLAPI GLuint GLAPIENTRY glCreateShader (GLenum);
GLAPI void GLAPIENTRY glDeleteProgram (GLuint);
GLAPI void GLAPIENTRY glDeleteShader (GLuint);
GLAPI void GLAPIENTRY glDetachShader (GLuint, GLuint);
GLAPI void GLAPIENTRY glDisableVertexAttribArray (GLuint);
GLAPI void GLAPIENTRY glEnableVertexAttribArray (GLuint);
GLAPI void GLAPIENTRY glGetActiveAttrib (GLuint, GLuint, GLsizei, GLsizei *, GLint *, GLenum *, GLchar *);
GLAPI void GLAPIENTRY glGetActiveUniform (GLuint, GLuint, GLsizei, GLsizei *, GLint *, GLenum *, GLchar *);
GLAPI void GLAPIENTRY glGetAttachedShaders (GLuint, GLsizei, GLsizei *, GLuint *);
GLAPI GLint GLAPIENTRY glGetAttribLocation (GLuint, const GLchar *);
GLAPI void GLAPIENTRY glGetProgramiv (GLuint, GLenum, GLint *);
GLAPI void GLAPIENTRY glGetProgramInfoLog (GLuint, GLsizei, GLsizei *, GLchar *);
GLAPI void GLAPIENTRY glGetShaderiv (GLuint, GLenum, GLint *);
GLAPI void GLAPIENTRY glGetShaderInfoLog (GLuint, GLsizei, GLsizei *, GLchar *);
GLAPI void GLAPIENTRY glGetShaderSource (GLuint, GLsizei, GLsizei *, GLchar *);
GLAPI GLint GLAPIENTRY glGetUniformLocation (GLuint, const GLchar *);
GLAPI void GLAPIENTRY glGetUniformfv (GLuint, GLint, GLfloat *);
GLAPI void GLAPIENTRY glGetUniformiv (GLuint, GLint, GLint *);
GLAPI void GLAPIENTRY glGetVertexAttribdv (GLuint, GLenum, GLdouble *);
GLAPI void GLAPIENTRY glGetVertexAttribfv (GLuint, GLenum, GLfloat *);
GLAPI void GLAPIENTRY glGetVertexAttribiv (GLuint, GLenum, GLint *);
GLAPI void GLAPIENTRY glGetVertexAttribPointerv (GLuint, GLenum, GLvoid* *);
GLAPI GLboolean GLAPIENTRY glIsProgram (GLuint);
GLAPI GLboolean GLAPIENTRY glIsShader (GLuint);
GLAPI void GLAPIENTRY glLinkProgram (GLuint);
GLAPI void GLAPIENTRY glShaderSource (GLuint, GLsizei, const GLchar* *, const GLint *);
GLAPI void GLAPIENTRY glUseProgram (GLuint);
GLAPI void GLAPIENTRY glUniform1f (GLint, GLfloat);
GLAPI void GLAPIENTRY glUniform2f (GLint, GLfloat, GLfloat);
GLAPI void GLAPIENTRY glUniform3f (GLint, GLfloat, GLfloat, GLfloat);
GLAPI void GLAPIENTRY glUniform4f (GLint, GLfloat, GLfloat, GLfloat, GLfloat);
GLAPI void GLAPIENTRY glUniform1i (GLint, GLint);
GLAPI void GLAPIENTRY glUniform2i (GLint, GLint, GLint);
GLAPI void GLAPIENTRY glUniform3i (GLint, GLint, GLint, GLint);
GLAPI void GLAPIENTRY glUniform4i (GLint, GLint, GLint, GLint, GLint);
GLAPI void GLAPIENTRY glUniform1fv (GLint, GLsizei, const GLfloat *);
GLAPI void GLAPIENTRY glUniform2fv (GLint, GLsizei, const GLfloat *);
GLAPI void GLAPIENTRY glUniform3fv (GLint, GLsizei, const GLfloat *);
GLAPI void GLAPIENTRY glUniform4fv (GLint, GLsizei, const GLfloat *);
GLAPI void GLAPIENTRY glUniform1iv (GLint, GLsizei, const GLint *);
GLAPI void GLAPIENTRY glUniform2iv (GLint, GLsizei, const GLint *);
GLAPI void GLAPIENTRY glUniform3iv (GLint, GLsizei, const GLint *);
GLAPI void GLAPIENTRY glUniform4iv (GLint, GLsizei, const GLint *);
GLAPI void GLAPIENTRY glUniformMatrix2fv (GLint, GLsizei, GLboolean, const GLfloat *);
GLAPI void GLAPIENTRY glUniformMatrix3fv (GLint, GLsizei, GLboolean, const GLfloat *);
GLAPI void GLAPIENTRY glUniformMatrix4fv (GLint, GLsizei, GLboolean, const GLfloat *);
GLAPI void GLAPIENTRY glValidateProgram (GLuint);
GLAPI void GLAPIENTRY glVertexAttrib1d (GLuint, GLdouble);
GLAPI void GLAPIENTRY glVertexAttrib1dv (GLuint, const GLdouble *);
GLAPI void GLAPIENTRY glVertexAttrib1f (GLuint, GLfloat);
GLAPI void GLAPIENTRY glVertexAttrib1fv (GLuint, const GLfloat *);
GLAPI void GLAPIENTRY glVertexAttrib1s (GLuint, GLshort);
GLAPI void GLAPIENTRY glVertexAttrib1sv (GLuint, const GLshort *);
GLAPI void GLAPIENTRY glVertexAttrib2d (GLuint, GLdouble, GLdouble);
GLAPI void GLAPIENTRY glVertexAttrib2dv (GLuint, const GLdouble *);
GLAPI void GLAPIENTRY glVertexAttrib2f (GLuint, GLfloat, GLfloat);
GLAPI void GLAPIENTRY glVertexAttrib2fv (GLuint, const GLfloat *);
GLAPI void GLAPIENTRY glVertexAttrib2s (GLuint, GLshort, GLshort);
GLAPI void GLAPIENTRY glVertexAttrib2sv (GLuint, const GLshort *);
GLAPI void GLAPIENTRY glVertexAttrib3d (GLuint, GLdouble, GLdouble, GLdouble);
GLAPI void GLAPIENTRY glVertexAttrib3dv (GLuint, const GLdouble *);
GLAPI void GLAPIENTRY glVertexAttrib3f (GLuint, GLfloat, GLfloat, GLfloat);
GLAPI void GLAPIENTRY glVertexAttrib3fv (GLuint, const GLfloat *);
GLAPI void GLAPIENTRY glVertexAttrib3s (GLuint, GLshort, GLshort, GLshort);
GLAPI void GLAPIENTRY glVertexAttrib3sv (GLuint, const GLshort *);
GLAPI void GLAPIENTRY glVertexAttrib4Nbv (GLuint, const GLbyte *);
GLAPI void GLAPIENTRY glVertexAttrib4Niv (GLuint, const GLint *);
GLAPI void GLAPIENTRY glVertexAttrib4Nsv (GLuint, const GLshort *);
GLAPI void GLAPIENTRY glVertexAttrib4Nub (GLuint, GLubyte, GLubyte, GLubyte, GLubyte);
GLAPI void GLAPIENTRY glVertexAttrib4Nubv (GLuint, const GLubyte *);
GLAPI void GLAPIENTRY glVertexAttrib4Nuiv (GLuint, const GLuint *);
GLAPI void GLAPIENTRY glVertexAttrib4Nusv (GLuint, const GLushort *);
GLAPI void GLAPIENTRY glVertexAttrib4bv (GLuint, const GLbyte *);
GLAPI void GLAPIENTRY glVertexAttrib4d (GLuint, GLdouble, GLdouble, GLdouble, GLdouble);
GLAPI void GLAPIENTRY glVertexAttrib4dv (GLuint, const GLdouble *);
GLAPI void GLAPIENTRY glVertexAttrib4f (GLuint, GLfloat, GLfloat, GLfloat, GLfloat);
GLAPI void GLAPIENTRY glVertexAttrib4fv (GLuint, const GLfloat *);
GLAPI void GLAPIENTRY glVertexAttrib4iv (GLuint, const GLint *);
GLAPI void GLAPIENTRY glVertexAttrib4s (GLuint, GLshort, GLshort, GLshort, GLshort);
GLAPI void GLAPIENTRY glVertexAttrib4sv (GLuint, const GLshort *);
GLAPI void GLAPIENTRY glVertexAttrib4ubv (GLuint, const GLubyte *);
GLAPI void GLAPIENTRY glVertexAttrib4uiv (GLuint, const GLuint *);
GLAPI void GLAPIENTRY glVertexAttrib4usv (GLuint, const GLushort *);
GLAPI void GLAPIENTRY glVertexAttribPointer (GLuint, GLint, GLenum, GLboolean, GLsizei, const GLvoid *);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLBLENDEQUATIONSEPARATEPROC) (GLenum modeRGB, GLenum modeAlpha);
typedef void (GLAPIENTRYP PFNGLDRAWBUFFERSPROC) (GLsizei n, const GLenum *bufs);
typedef void (GLAPIENTRYP PFNGLSTENCILOPSEPARATEPROC) (GLenum face, GLenum sfail, GLenum dpfail, GLenum dppass);
typedef void (GLAPIENTRYP PFNGLSTENCILFUNCSEPARATEPROC) (GLenum frontfunc, GLenum backfunc, GLint ref, GLuint mask);
typedef void (GLAPIENTRYP PFNGLSTENCILMASKSEPARATEPROC) (GLenum face, GLuint mask);
typedef void (GLAPIENTRYP PFNGLATTACHSHADERPROC) (GLuint program, GLuint shader);
typedef void (GLAPIENTRYP PFNGLBINDATTRIBLOCATIONPROC) (GLuint program, GLuint index, const GLchar *name);
typedef void (GLAPIENTRYP PFNGLCOMPILESHADERPROC) (GLuint shader);
typedef GLuint (GLAPIENTRYP PFNGLCREATEPROGRAMPROC) (void);
typedef GLuint (GLAPIENTRYP PFNGLCREATESHADERPROC) (GLenum type);
typedef void (GLAPIENTRYP PFNGLDELETEPROGRAMPROC) (GLuint program);
typedef void (GLAPIENTRYP PFNGLDELETESHADERPROC) (GLuint shader);
typedef void (GLAPIENTRYP PFNGLDETACHSHADERPROC) (GLuint program, GLuint shader);
typedef void (GLAPIENTRYP PFNGLDISABLEVERTEXATTRIBARRAYPROC) (GLuint index);
typedef void (GLAPIENTRYP PFNGLENABLEVERTEXATTRIBARRAYPROC) (GLuint index);
typedef void (GLAPIENTRYP PFNGLGETACTIVEATTRIBPROC) (GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size, GLenum *type, GLchar *name);
typedef void (GLAPIENTRYP PFNGLGETACTIVEUNIFORMPROC) (GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLint *size, GLenum *type, GLchar *name);
typedef void (GLAPIENTRYP PFNGLGETATTACHEDSHADERSPROC) (GLuint program, GLsizei maxCount, GLsizei *count, GLuint *obj);
typedef GLint (GLAPIENTRYP PFNGLGETATTRIBLOCATIONPROC) (GLuint program, const GLchar *name);
typedef void (GLAPIENTRYP PFNGLGETPROGRAMIVPROC) (GLuint program, GLenum pname, GLint *params);
typedef void (GLAPIENTRYP PFNGLGETPROGRAMINFOLOGPROC) (GLuint program, GLsizei bufSize, GLsizei *length, GLchar *infoLog);
typedef void (GLAPIENTRYP PFNGLGETSHADERIVPROC) (GLuint shader, GLenum pname, GLint *params);
typedef void (GLAPIENTRYP PFNGLGETSHADERINFOLOGPROC) (GLuint shader, GLsizei bufSize, GLsizei *length, GLchar *infoLog);
typedef void (GLAPIENTRYP PFNGLGETSHADERSOURCEPROC) (GLuint shader, GLsizei bufSize, GLsizei *length, GLchar *source);
typedef GLint (GLAPIENTRYP PFNGLGETUNIFORMLOCATIONPROC) (GLuint program, const GLchar *name);
typedef void (GLAPIENTRYP PFNGLGETUNIFORMFVPROC) (GLuint program, GLint location, GLfloat *params);
typedef void (GLAPIENTRYP PFNGLGETUNIFORMIVPROC) (GLuint program, GLint location, GLint *params);
typedef void (GLAPIENTRYP PFNGLGETVERTEXATTRIBDVPROC) (GLuint index, GLenum pname, GLdouble *params);
typedef void (GLAPIENTRYP PFNGLGETVERTEXATTRIBFVPROC) (GLuint index, GLenum pname, GLfloat *params);
typedef void (GLAPIENTRYP PFNGLGETVERTEXATTRIBIVPROC) (GLuint index, GLenum pname, GLint *params);
typedef void (GLAPIENTRYP PFNGLGETVERTEXATTRIBPOINTERVPROC) (GLuint index, GLenum pname, GLvoid* *pointer);
typedef GLboolean (GLAPIENTRYP PFNGLISPROGRAMPROC) (GLuint program);
typedef GLboolean (GLAPIENTRYP PFNGLISSHADERPROC) (GLuint shader);
typedef void (GLAPIENTRYP PFNGLLINKPROGRAMPROC) (GLuint program);
typedef void (GLAPIENTRYP PFNGLSHADERSOURCEPROC) (GLuint shader, GLsizei count, const GLchar* *string, const GLint *length);
typedef void (GLAPIENTRYP PFNGLUSEPROGRAMPROC) (GLuint program);
typedef void (GLAPIENTRYP PFNGLUNIFORM1FPROC) (GLint location, GLfloat v0);
typedef void (GLAPIENTRYP PFNGLUNIFORM2FPROC) (GLint location, GLfloat v0, GLfloat v1);
typedef void (GLAPIENTRYP PFNGLUNIFORM3FPROC) (GLint location, GLfloat v0, GLfloat v1, GLfloat v2);
typedef void (GLAPIENTRYP PFNGLUNIFORM4FPROC) (GLint location, GLfloat v0, GLfloat v1, GLfloat v2, GLfloat v3);
typedef void (GLAPIENTRYP PFNGLUNIFORM1IPROC) (GLint location, GLint v0);
typedef void (GLAPIENTRYP PFNGLUNIFORM2IPROC) (GLint location, GLint v0, GLint v1);
typedef void (GLAPIENTRYP PFNGLUNIFORM3IPROC) (GLint location, GLint v0, GLint v1, GLint v2);
typedef void (GLAPIENTRYP PFNGLUNIFORM4IPROC) (GLint location, GLint v0, GLint v1, GLint v2, GLint v3);
typedef void (GLAPIENTRYP PFNGLUNIFORM1FVPROC) (GLint location, GLsizei count, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLUNIFORM2FVPROC) (GLint location, GLsizei count, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLUNIFORM3FVPROC) (GLint location, GLsizei count, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLUNIFORM4FVPROC) (GLint location, GLsizei count, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLUNIFORM1IVPROC) (GLint location, GLsizei count, const GLint *value);
typedef void (GLAPIENTRYP PFNGLUNIFORM2IVPROC) (GLint location, GLsizei count, const GLint *value);
typedef void (GLAPIENTRYP PFNGLUNIFORM3IVPROC) (GLint location, GLsizei count, const GLint *value);
typedef void (GLAPIENTRYP PFNGLUNIFORM4IVPROC) (GLint location, GLsizei count, const GLint *value);
typedef void (GLAPIENTRYP PFNGLUNIFORMMATRIX2FVPROC) (GLint location, GLsizei count, GLboolean transpose, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLUNIFORMMATRIX3FVPROC) (GLint location, GLsizei count, GLboolean transpose, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLUNIFORMMATRIX4FVPROC) (GLint location, GLsizei count, GLboolean transpose, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLVALIDATEPROGRAMPROC) (GLuint program);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB1DPROC) (GLuint index, GLdouble x);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB1DVPROC) (GLuint index, const GLdouble *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB1FPROC) (GLuint index, GLfloat x);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB1FVPROC) (GLuint index, const GLfloat *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB1SPROC) (GLuint index, GLshort x);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB1SVPROC) (GLuint index, const GLshort *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB2DPROC) (GLuint index, GLdouble x, GLdouble y);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB2DVPROC) (GLuint index, const GLdouble *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB2FPROC) (GLuint index, GLfloat x, GLfloat y);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB2FVPROC) (GLuint index, const GLfloat *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB2SPROC) (GLuint index, GLshort x, GLshort y);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB2SVPROC) (GLuint index, const GLshort *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB3DPROC) (GLuint index, GLdouble x, GLdouble y, GLdouble z);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB3DVPROC) (GLuint index, const GLdouble *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB3FPROC) (GLuint index, GLfloat x, GLfloat y, GLfloat z);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB3FVPROC) (GLuint index, const GLfloat *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB3SPROC) (GLuint index, GLshort x, GLshort y, GLshort z);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB3SVPROC) (GLuint index, const GLshort *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4NBVPROC) (GLuint index, const GLbyte *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4NIVPROC) (GLuint index, const GLint *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4NSVPROC) (GLuint index, const GLshort *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4NUBPROC) (GLuint index, GLubyte x, GLubyte y, GLubyte z, GLubyte w);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4NUBVPROC) (GLuint index, const GLubyte *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4NUIVPROC) (GLuint index, const GLuint *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4NUSVPROC) (GLuint index, const GLushort *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4BVPROC) (GLuint index, const GLbyte *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4DPROC) (GLuint index, GLdouble x, GLdouble y, GLdouble z, GLdouble w);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4DVPROC) (GLuint index, const GLdouble *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4FPROC) (GLuint index, GLfloat x, GLfloat y, GLfloat z, GLfloat w);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4FVPROC) (GLuint index, const GLfloat *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4IVPROC) (GLuint index, const GLint *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4SPROC) (GLuint index, GLshort x, GLshort y, GLshort z, GLshort w);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4SVPROC) (GLuint index, const GLshort *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4UBVPROC) (GLuint index, const GLubyte *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4UIVPROC) (GLuint index, const GLuint *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIB4USVPROC) (GLuint index, const GLushort *v);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIBPOINTERPROC) (GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const GLvoid *pointer);
#endif

#ifndef GL_VERSION_2_1
#define GL_VERSION_2_1 1
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glUniformMatrix2x3fv (GLint, GLsizei, GLboolean, const GLfloat *);
GLAPI void GLAPIENTRY glUniformMatrix3x2fv (GLint, GLsizei, GLboolean, const GLfloat *);
GLAPI void GLAPIENTRY glUniformMatrix2x4fv (GLint, GLsizei, GLboolean, const GLfloat *);
GLAPI void GLAPIENTRY glUniformMatrix4x2fv (GLint, GLsizei, GLboolean, const GLfloat *);
GLAPI void GLAPIENTRY glUniformMatrix3x4fv (GLint, GLsizei, GLboolean, const GLfloat *);
GLAPI void GLAPIENTRY glUniformMatrix4x3fv (GLint, GLsizei, GLboolean, const GLfloat *);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLUNIFORMMATRIX2X3FVPROC) (GLint location, GLsizei count, GLboolean transpose, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLUNIFORMMATRIX3X2FVPROC) (GLint location, GLsizei count, GLboolean transpose, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLUNIFORMMATRIX2X4FVPROC) (GLint location, GLsizei count, GLboolean transpose, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLUNIFORMMATRIX4X2FVPROC) (GLint location, GLsizei count, GLboolean transpose, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLUNIFORMMATRIX3X4FVPROC) (GLint location, GLsizei count, GLboolean transpose, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLUNIFORMMATRIX4X3FVPROC) (GLint location, GLsizei count, GLboolean transpose, const GLfloat *value);
#endif

#ifndef GL_VERSION_3_0
#define GL_VERSION_3_0 1
/* OpenGL 3.0 also reuses entry points from these extensions: */
/* ARB_framebuffer_object */
/* ARB_map_buffer_range */
/* ARB_vertex_array_object */
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glColorMaski (GLuint, GLboolean, GLboolean, GLboolean, GLboolean);
GLAPI void GLAPIENTRY glGetBooleani_v (GLenum, GLuint, GLboolean *);
GLAPI void GLAPIENTRY glGetIntegeri_v (GLenum, GLuint, GLint *);
GLAPI void GLAPIENTRY glEnablei (GLenum, GLuint);
GLAPI void GLAPIENTRY glDisablei (GLenum, GLuint);
GLAPI GLboolean GLAPIENTRY glIsEnabledi (GLenum, GLuint);
GLAPI void GLAPIENTRY glBeginTransformFeedback (GLenum);
GLAPI void GLAPIENTRY glEndTransformFeedback (void);
GLAPI void GLAPIENTRY glBindBufferRange (GLenum, GLuint, GLuint, GLintptr, GLsizeiptr);
GLAPI void GLAPIENTRY glBindBufferBase (GLenum, GLuint, GLuint);
GLAPI void GLAPIENTRY glTransformFeedbackVaryings (GLuint, GLsizei, const GLchar* *, GLenum);
GLAPI void GLAPIENTRY glGetTransformFeedbackVarying (GLuint, GLuint, GLsizei, GLsizei *, GLsizei *, GLenum *, GLchar *);
GLAPI void GLAPIENTRY glClampColor (GLenum, GLenum);
GLAPI void GLAPIENTRY glBeginConditionalRender (GLuint, GLenum);
GLAPI void GLAPIENTRY glEndConditionalRender (void);
GLAPI void GLAPIENTRY glVertexAttribIPointer (GLuint, GLint, GLenum, GLsizei, const GLvoid *);
GLAPI void GLAPIENTRY glGetVertexAttribIiv (GLuint, GLenum, GLint *);
GLAPI void GLAPIENTRY glGetVertexAttribIuiv (GLuint, GLenum, GLuint *);
GLAPI void GLAPIENTRY glGetUniformuiv (GLuint, GLint, GLuint *);
GLAPI void GLAPIENTRY glBindFragDataLocation (GLuint, GLuint, const GLchar *);
GLAPI GLint GLAPIENTRY glGetFragDataLocation (GLuint, const GLchar *);
GLAPI void GLAPIENTRY glUniform1ui (GLint, GLuint);
GLAPI void GLAPIENTRY glUniform2ui (GLint, GLuint, GLuint);
GLAPI void GLAPIENTRY glUniform3ui (GLint, GLuint, GLuint, GLuint);
GLAPI void GLAPIENTRY glUniform4ui (GLint, GLuint, GLuint, GLuint, GLuint);
GLAPI void GLAPIENTRY glUniform1uiv (GLint, GLsizei, const GLuint *);
GLAPI void GLAPIENTRY glUniform2uiv (GLint, GLsizei, const GLuint *);
GLAPI void GLAPIENTRY glUniform3uiv (GLint, GLsizei, const GLuint *);
GLAPI void GLAPIENTRY glUniform4uiv (GLint, GLsizei, const GLuint *);
GLAPI void GLAPIENTRY glTexParameterIiv (GLenum, GLenum, const GLint *);
GLAPI void GLAPIENTRY glTexParameterIuiv (GLenum, GLenum, const GLuint *);
GLAPI void GLAPIENTRY glGetTexParameterIiv (GLenum, GLenum, GLint *);
GLAPI void GLAPIENTRY glGetTexParameterIuiv (GLenum, GLenum, GLuint *);
GLAPI void GLAPIENTRY glClearBufferiv (GLenum, GLint, const GLint *);
GLAPI void GLAPIENTRY glClearBufferuiv (GLenum, GLint, const GLuint *);
GLAPI void GLAPIENTRY glClearBufferfv (GLenum, GLint, const GLfloat *);
GLAPI void GLAPIENTRY glClearBufferfi (GLenum, GLint, GLfloat, GLint);
GLAPI const GLubyte * GLAPIENTRY glGetStringi (GLenum, GLuint);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLCOLORMASKIPROC) (GLuint index, GLboolean r, GLboolean g, GLboolean b, GLboolean a);
typedef void (GLAPIENTRYP PFNGLGETBOOLEANI_VPROC) (GLenum target, GLuint index, GLboolean *data);
typedef void (GLAPIENTRYP PFNGLGETINTEGERI_VPROC) (GLenum target, GLuint index, GLint *data);
typedef void (GLAPIENTRYP PFNGLENABLEIPROC) (GLenum target, GLuint index);
typedef void (GLAPIENTRYP PFNGLDISABLEIPROC) (GLenum target, GLuint index);
typedef GLboolean (GLAPIENTRYP PFNGLISENABLEDIPROC) (GLenum target, GLuint index);
typedef void (GLAPIENTRYP PFNGLBEGINTRANSFORMFEEDBACKPROC) (GLenum primitiveMode);
typedef void (GLAPIENTRYP PFNGLENDTRANSFORMFEEDBACKPROC) (void);
typedef void (GLAPIENTRYP PFNGLBINDBUFFERRANGEPROC) (GLenum target, GLuint index, GLuint buffer, GLintptr offset, GLsizeiptr size);
typedef void (GLAPIENTRYP PFNGLBINDBUFFERBASEPROC) (GLenum target, GLuint index, GLuint buffer);
typedef void (GLAPIENTRYP PFNGLTRANSFORMFEEDBACKVARYINGSPROC) (GLuint program, GLsizei count, const GLchar* *varyings, GLenum bufferMode);
typedef void (GLAPIENTRYP PFNGLGETTRANSFORMFEEDBACKVARYINGPROC) (GLuint program, GLuint index, GLsizei bufSize, GLsizei *length, GLsizei *size, GLenum *type, GLchar *name);
typedef void (GLAPIENTRYP PFNGLCLAMPCOLORPROC) (GLenum target, GLenum clamp);
typedef void (GLAPIENTRYP PFNGLBEGINCONDITIONALRENDERPROC) (GLuint id, GLenum mode);
typedef void (GLAPIENTRYP PFNGLENDCONDITIONALRENDERPROC) (void);
typedef void (GLAPIENTRYP PFNGLVERTEXATTRIBIPOINTERPROC) (GLuint index, GLint size, GLenum type, GLsizei stride, const GLvoid *pointer);
typedef void (GLAPIENTRYP PFNGLGETVERTEXATTRIBIIVPROC) (GLuint index, GLenum pname, GLint *params);
typedef void (GLAPIENTRYP PFNGLGETVERTEXATTRIBIUIVPROC) (GLuint index, GLenum pname, GLuint *params);
typedef void (GLAPIENTRYP PFNGLGETUNIFORMUIVPROC) (GLuint program, GLint location, GLuint *params);
typedef void (GLAPIENTRYP PFNGLBINDFRAGDATALOCATIONPROC) (GLuint program, GLuint color, const GLchar *name);
typedef GLint (GLAPIENTRYP PFNGLGETFRAGDATALOCATIONPROC) (GLuint program, const GLchar *name);
typedef void (GLAPIENTRYP PFNGLUNIFORM1UIPROC) (GLint location, GLuint v0);
typedef void (GLAPIENTRYP PFNGLUNIFORM2UIPROC) (GLint location, GLuint v0, GLuint v1);
typedef void (GLAPIENTRYP PFNGLUNIFORM3UIPROC) (GLint location, GLuint v0, GLuint v1, GLuint v2);
typedef void (GLAPIENTRYP PFNGLUNIFORM4UIPROC) (GLint location, GLuint v0, GLuint v1, GLuint v2, GLuint v3);
typedef void (GLAPIENTRYP PFNGLUNIFORM1UIVPROC) (GLint location, GLsizei count, const GLuint *value);
typedef void (GLAPIENTRYP PFNGLUNIFORM2UIVPROC) (GLint location, GLsizei count, const GLuint *value);
typedef void (GLAPIENTRYP PFNGLUNIFORM3UIVPROC) (GLint location, GLsizei count, const GLuint *value);
typedef void (GLAPIENTRYP PFNGLUNIFORM4UIVPROC) (GLint location, GLsizei count, const GLuint *value);
typedef void (GLAPIENTRYP PFNGLTEXPARAMETERIIVPROC) (GLenum target, GLenum pname, const GLint *params);
typedef void (GLAPIENTRYP PFNGLTEXPARAMETERIUIVPROC) (GLenum target, GLenum pname, const GLuint *params);
typedef void (GLAPIENTRYP PFNGLGETTEXPARAMETERIIVPROC) (GLenum target, GLenum pname, GLint *params);
typedef void (GLAPIENTRYP PFNGLGETTEXPARAMETERIUIVPROC) (GLenum target, GLenum pname, GLuint *params);
typedef void (GLAPIENTRYP PFNGLCLEARBUFFERIVPROC) (GLenum buffer, GLint drawbuffer, const GLint *value);
typedef void (GLAPIENTRYP PFNGLCLEARBUFFERUIVPROC) (GLenum buffer, GLint drawbuffer, const GLuint *value);
typedef void (GLAPIENTRYP PFNGLCLEARBUFFERFVPROC) (GLenum buffer, GLint drawbuffer, const GLfloat *value);
typedef void (GLAPIENTRYP PFNGLCLEARBUFFERFIPROC) (GLenum buffer, GLint drawbuffer, GLfloat depth, GLint stencil);
typedef const GLubyte * (GLAPIENTRYP PFNGLGETSTRINGIPROC) (GLenum name, GLuint index);
#endif

#ifndef GL_VERSION_3_1
#define GL_VERSION_3_1 1
/* OpenGL 3.1 also reuses entry points from these extensions: */
/* ARB_copy_buffer */
/* ARB_uniform_buffer_object */
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glDrawArraysInstanced (GLenum, GLint, GLsizei, GLsizei);
GLAPI void GLAPIENTRY glDrawElementsInstanced (GLenum, GLsizei, GLenum, const GLvoid *, GLsizei);
GLAPI void GLAPIENTRY glTexBuffer (GLenum, GLenum, GLuint);
GLAPI void GLAPIENTRY glPrimitiveRestartIndex (GLuint);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLDRAWARRAYSINSTANCEDPROC) (GLenum mode, GLint first, GLsizei count, GLsizei primcount);
typedef void (GLAPIENTRYP PFNGLDRAWELEMENTSINSTANCEDPROC) (GLenum mode, GLsizei count, GLenum type, const GLvoid *indices, GLsizei primcount);
typedef void (GLAPIENTRYP PFNGLTEXBUFFERPROC) (GLenum target, GLenum internalformat, GLuint buffer);
typedef void (GLAPIENTRYP PFNGLPRIMITIVERESTARTINDEXPROC) (GLuint index);
#endif

#ifndef GL_ARB_framebuffer_object
#define GL_ARB_framebuffer_object 1
#ifdef GL3_PROTOTYPES
GLAPI GLboolean GLAPIENTRY glIsRenderbuffer (GLuint);
GLAPI void GLAPIENTRY glBindRenderbuffer (GLenum, GLuint);
GLAPI void GLAPIENTRY glDeleteRenderbuffers (GLsizei, const GLuint *);
GLAPI void GLAPIENTRY glGenRenderbuffers (GLsizei, GLuint *);
GLAPI void GLAPIENTRY glRenderbufferStorage (GLenum, GLenum, GLsizei, GLsizei);
GLAPI void GLAPIENTRY glGetRenderbufferParameteriv (GLenum, GLenum, GLint *);
GLAPI GLboolean GLAPIENTRY glIsFramebuffer (GLuint);
GLAPI void GLAPIENTRY glBindFramebuffer (GLenum, GLuint);
GLAPI void GLAPIENTRY glDeleteFramebuffers (GLsizei, const GLuint *);
GLAPI void GLAPIENTRY glGenFramebuffers (GLsizei, GLuint *);
GLAPI GLenum GLAPIENTRY glCheckFramebufferStatus (GLenum);
GLAPI void GLAPIENTRY glFramebufferTexture1D (GLenum, GLenum, GLenum, GLuint, GLint);
GLAPI void GLAPIENTRY glFramebufferTexture2D (GLenum, GLenum, GLenum, GLuint, GLint);
GLAPI void GLAPIENTRY glFramebufferTexture3D (GLenum, GLenum, GLenum, GLuint, GLint, GLint);
GLAPI void GLAPIENTRY glFramebufferRenderbuffer (GLenum, GLenum, GLenum, GLuint);
GLAPI void GLAPIENTRY glGetFramebufferAttachmentParameteriv (GLenum, GLenum, GLenum, GLint *);
GLAPI void GLAPIENTRY glGenerateMipmap (GLenum);
GLAPI void GLAPIENTRY glBlitFramebuffer (GLint, GLint, GLint, GLint, GLint, GLint, GLint, GLint, GLbitfield, GLenum);
GLAPI void GLAPIENTRY glRenderbufferStorageMultisample (GLenum, GLsizei, GLenum, GLsizei, GLsizei);
GLAPI void GLAPIENTRY glFramebufferTextureLayer (GLenum, GLenum, GLuint, GLint, GLint);
#endif /* GL3_PROTOTYPES */
typedef GLboolean (GLAPIENTRYP PFNGLISRENDERBUFFERPROC) (GLuint renderbuffer);
typedef void (GLAPIENTRYP PFNGLBINDRENDERBUFFERPROC) (GLenum target, GLuint renderbuffer);
typedef void (GLAPIENTRYP PFNGLDELETERENDERBUFFERSPROC) (GLsizei n, const GLuint *renderbuffers);
typedef void (GLAPIENTRYP PFNGLGENRENDERBUFFERSPROC) (GLsizei n, GLuint *renderbuffers);
typedef void (GLAPIENTRYP PFNGLRENDERBUFFERSTORAGEPROC) (GLenum target, GLenum internalformat, GLsizei width, GLsizei height);
typedef void (GLAPIENTRYP PFNGLGETRENDERBUFFERPARAMETERIVPROC) (GLenum target, GLenum pname, GLint *params);
typedef GLboolean (GLAPIENTRYP PFNGLISFRAMEBUFFERPROC) (GLuint framebuffer);
typedef void (GLAPIENTRYP PFNGLBINDFRAMEBUFFERPROC) (GLenum target, GLuint framebuffer);
typedef void (GLAPIENTRYP PFNGLDELETEFRAMEBUFFERSPROC) (GLsizei n, const GLuint *framebuffers);
typedef void (GLAPIENTRYP PFNGLGENFRAMEBUFFERSPROC) (GLsizei n, GLuint *framebuffers);
typedef GLenum (GLAPIENTRYP PFNGLCHECKFRAMEBUFFERSTATUSPROC) (GLenum target);
typedef void (GLAPIENTRYP PFNGLFRAMEBUFFERTEXTURE1DPROC) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level);
typedef void (GLAPIENTRYP PFNGLFRAMEBUFFERTEXTURE2DPROC) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level);
typedef void (GLAPIENTRYP PFNGLFRAMEBUFFERTEXTURE3DPROC) (GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level, GLint zoffset);
typedef void (GLAPIENTRYP PFNGLFRAMEBUFFERRENDERBUFFERPROC) (GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer);
typedef void (GLAPIENTRYP PFNGLGETFRAMEBUFFERATTACHMENTPARAMETERIVPROC) (GLenum target, GLenum attachment, GLenum pname, GLint *params);
typedef void (GLAPIENTRYP PFNGLGENERATEMIPMAPPROC) (GLenum target);
typedef void (GLAPIENTRYP PFNGLBLITFRAMEBUFFERPROC) (GLint srcX0, GLint srcY0, GLint srcX1, GLint srcY1, GLint dstX0, GLint dstY0, GLint dstX1, GLint dstY1, GLbitfield mask, GLenum filter);
typedef void (GLAPIENTRYP PFNGLRENDERBUFFERSTORAGEMULTISAMPLEPROC) (GLenum target, GLsizei samples, GLenum internalformat, GLsizei width, GLsizei height);
typedef void (GLAPIENTRYP PFNGLFRAMEBUFFERTEXTURELAYERPROC) (GLenum target, GLenum attachment, GLuint texture, GLint level, GLint layer);
#endif

#ifndef GL_ARB_map_buffer_range
#define GL_ARB_map_buffer_range 1
#ifdef GL3_PROTOTYPES
GLAPI GLvoid* GLAPIENTRY glMapBufferRange (GLenum, GLintptr, GLsizeiptr, GLbitfield);
GLAPI void GLAPIENTRY glFlushMappedBufferRange (GLenum, GLintptr, GLsizeiptr);
#endif /* GL3_PROTOTYPES */
typedef GLvoid* (GLAPIENTRYP PFNGLMAPBUFFERRANGEPROC) (GLenum target, GLintptr offset, GLsizeiptr length, GLbitfield access);
typedef void (GLAPIENTRYP PFNGLFLUSHMAPPEDBUFFERRANGEPROC) (GLenum target, GLintptr offset, GLsizeiptr length);
#endif

#ifndef GL_ARB_vertex_array_object
#define GL_ARB_vertex_array_object 1
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glBindVertexArray (GLuint);
GLAPI void GLAPIENTRY glDeleteVertexArrays (GLsizei, const GLuint *);
GLAPI void GLAPIENTRY glGenVertexArrays (GLsizei, GLuint *);
GLAPI GLboolean GLAPIENTRY glIsVertexArray (GLuint);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLBINDVERTEXARRAYPROC) (GLuint array);
typedef void (GLAPIENTRYP PFNGLDELETEVERTEXARRAYSPROC) (GLsizei n, const GLuint *arrays);
typedef void (GLAPIENTRYP PFNGLGENVERTEXARRAYSPROC) (GLsizei n, GLuint *arrays);
typedef GLboolean (GLAPIENTRYP PFNGLISVERTEXARRAYPROC) (GLuint array);
#endif

#ifndef GL_ARB_uniform_buffer_object
#define GL_ARB_uniform_buffer_object 1
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glGetUniformIndices (GLuint, GLsizei, const GLchar* *, GLuint *);
GLAPI void GLAPIENTRY glGetActiveUniformsiv (GLuint, GLsizei, const GLuint *, GLenum, GLint *);
GLAPI void GLAPIENTRY glGetActiveUniformName (GLuint, GLuint, GLsizei, GLsizei *, GLchar *);
GLAPI GLuint GLAPIENTRY glGetUniformBlockIndex (GLuint, const GLchar *);
GLAPI void GLAPIENTRY glGetActiveUniformBlockiv (GLuint, GLuint, GLenum, GLint *);
GLAPI void GLAPIENTRY glGetActiveUniformBlockName (GLuint, GLuint, GLsizei, GLsizei *, GLchar *);
GLAPI void GLAPIENTRY glUniformBlockBinding (GLuint, GLuint, GLuint);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLGETUNIFORMINDICESPROC) (GLuint program, GLsizei uniformCount, const GLchar* *uniformNames, GLuint *uniformIndices);
typedef void (GLAPIENTRYP PFNGLGETACTIVEUNIFORMSIVPROC) (GLuint program, GLsizei uniformCount, const GLuint *uniformIndices, GLenum pname, GLint *params);
typedef void (GLAPIENTRYP PFNGLGETACTIVEUNIFORMNAMEPROC) (GLuint program, GLuint uniformIndex, GLsizei bufSize, GLsizei *length, GLchar *uniformName);
typedef GLuint (GLAPIENTRYP PFNGLGETUNIFORMBLOCKINDEXPROC) (GLuint program, const GLchar *uniformBlockName);
typedef void (GLAPIENTRYP PFNGLGETACTIVEUNIFORMBLOCKIVPROC) (GLuint program, GLuint uniformBlockIndex, GLenum pname, GLint *params);
typedef void (GLAPIENTRYP PFNGLGETACTIVEUNIFORMBLOCKNAMEPROC) (GLuint program, GLuint uniformBlockIndex, GLsizei bufSize, GLsizei *length, GLchar *uniformBlockName);
typedef void (GLAPIENTRYP PFNGLUNIFORMBLOCKBINDINGPROC) (GLuint program, GLuint uniformBlockIndex, GLuint uniformBlockBinding);
#endif

#ifndef GL_ARB_copy_buffer
#define GL_ARB_copy_buffer 1
#ifdef GL3_PROTOTYPES
GLAPI void GLAPIENTRY glCopyBufferSubData (GLenum, GLenum, GLintptr, GLintptr, GLsizeiptr);
#endif /* GL3_PROTOTYPES */
typedef void (GLAPIENTRYP PFNGLCOPYBUFFERSUBDATAPROC) (GLenum readTarget, GLenum writeTarget, GLintptr readOffset, GLintptr writeOffset, GLsizeiptr size);
#endif


#ifdef __cplusplus
}
#endif

#endif
