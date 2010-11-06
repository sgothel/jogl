/* Sample KD/kdplatform.h for OpenKODE Core 1.0.3  */
#ifndef __kdplatform_h_
#define __kdplatform_h_

#include <KHR/khrplatform.h>

#define KD_API
#define KD_APIENTRY

typedef khronos_int32_t KDint32;
typedef khronos_uint32_t KDuint32;
typedef khronos_int64_t KDint64;
typedef khronos_uint64_t KDuint64;
typedef khronos_int16_t KDint16;
typedef khronos_uint16_t KDuint16;
typedef khronos_uintptr_t KDuintptr;
typedef khronos_usize_t KDsize;
typedef khronos_ssize_t KDssize;
#define KDINT_MIN (-0x7fffffff-1)
#define KDINT_MAX 0x7fffffff
#define KDUINT_MAX 0xffffffffU
#define KDINT64_MIN (-0x7fffffffffffffffLL-1)
#define KDINT64_MAX 0x7fffffffffffffffLL
#define KDUINT64_MAX 0xffffffffffffffffULL
#define KDSSIZE_MIN (-0x7fffffff-1)
#define KDSSIZE_MAX 0x7fffffff
#define KDSIZE_MAX 0xffffffffU
#define KDUINTPTR_MAX 0xffffffffU
#define KD_NORETURN
#define KD_INFINITY (1.0F/0.0F)
#define KD_WINDOW_SUPPORTED
#ifdef KD_NDEBUG
#define kdAssert(c)
#else
#define kdAssert(c) ((void)( (c) ? 0 : (kdHandleAssertion(#c, __FILE__, __LINE__), 0)))
#endif

#endif /* __kdplatform_h_ */

