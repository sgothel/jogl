#ifndef __gl_types_h_
#define __gl_types_h_

#include <KHR/khrplatform.h>

#define GLEXT_64_TYPES_DEFINED 1

typedef khronos_int64_t GLint64EXT;
typedef khronos_uint64_t GLuint64EXT;

typedef khronos_int64_t GLint64;
typedef khronos_uint64_t GLuint64;
typedef struct __GLsync *GLsync;

#endif /* __gl_types_h_ */
