#ifndef __gl_types_h_
#define __gl_types_h_

#ifdef __cplusplus
extern "C" {
#endif

#include <KHR/khrplatform.h>

typedef khronos_int8_t GLbyte;
typedef khronos_float_t GLclampf;
typedef khronos_int32_t GLfixed;
typedef khronos_int32_t GLclampx;
typedef short GLshort;
typedef unsigned short GLushort;
typedef void GLvoid;
typedef struct __GLsync *GLsync;
typedef khronos_int64_t GLint64;
typedef khronos_int64_t GLint64EXT;
typedef khronos_uint64_t GLuint64;
typedef khronos_uint64_t GLuint64EXT;
typedef unsigned int GLenum;
typedef unsigned int GLuint;
typedef char GLchar;
typedef khronos_float_t GLfloat;
typedef khronos_ssize_t GLsizeiptr;
typedef khronos_intptr_t GLintptr;
typedef unsigned int GLbitfield;
typedef int GLint;
typedef unsigned char GLboolean;
typedef int GLsizei;
typedef khronos_uint8_t GLubyte;
typedef double          GLdouble;       /* double precision float */
typedef double          GLclampd;       /* double precision float in [0,1] */

typedef char GLcharARB;
typedef GLsizeiptr GLsizeiptrARB;
typedef GLintptr  GLintptrARB;
#ifdef __APPLE__
    typedef void *GLhandleARB;
#else
    typedef unsigned int GLhandleARB;
#endif

typedef void* GLeglImageOES;

struct _cl_context;
struct _cl_event;

typedef unsigned short GLhalfNV;
typedef GLintptr GLvdpauSurfaceNV;

#define GLEXT_64_TYPES_DEFINED 1

#ifdef __cplusplus
}
#endif

#endif /* __gl_types_h_ */
