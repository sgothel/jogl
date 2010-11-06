#ifndef __gl_64bit_types_h_
#define __gl_64bit_types_h_

#include <KHR/khrplatform.h>

#ifdef KHRONOS_SUPPORT_INT64

    #ifndef GL_EXT_timer_query
    typedef khronos_int64_t GLint64EXT;
    typedef khronos_uint64_t GLuint64EXT;
    #endif

    #ifndef GL_ARB_sync
    typedef khronos_int64_t GLint64;
    typedef khronos_uint64_t GLuint64;
    typedef struct __GLsync *GLsync;
    #endif

#endif /* KHRONOS_SUPPORT_INT64 */

#endif /* __gl_64bit_types_h_ */
