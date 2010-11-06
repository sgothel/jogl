#ifndef __egl_64bit_types_h_
#define __egl_64bit_types_h_

#include <KHR/khrplatform.h>

#ifdef KHRONOS_SUPPORT_INT64
    typedef khronos_int64_t EGLint64;
    typedef khronos_uint64_t EGLuint64;
#endif /* KHRONOS_SUPPORT_INT64 */

#endif /* __egl_64bit_types_h_ */
