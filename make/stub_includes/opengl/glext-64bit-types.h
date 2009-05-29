#ifndef __glext_64bit_types_h_
#define __glext_64bit_types_h_

#ifndef GLEXT_64_TYPES_DEFINED
    /* This code block is duplicated in glext.h, so must be protected */
    #define GLEXT_64_TYPES_DEFINED
    /* Define int32_t, int64_t, and uint64_t types for UST/MSC */
    /* (as used in the GL_EXT_timer_query extension). */
    #if defined(__STDC_VERSION__) && __STDC_VERSION__ >= 199901L
        #include <inttypes.h>
    #elif defined(__sun__)
        #include <inttypes.h>
        #if defined(__STDC__)
            #if defined(__arch64__)
                typedef long int int64_t;
                typedef unsigned long int uint64_t;
            #else
                typedef long long int int64_t;
                typedef unsigned long long int uint64_t;
            #endif /* __arch64__ */
        #endif /* __STDC__ */
    #elif defined( __VMS )
        #include <inttypes.h>
    #elif defined(__SCO__) || defined(__USLC__)
        #include <stdint.h>
    #elif defined(__UNIXOS2__) || defined(__SOL64__)
        typedef long int int32_t;
        typedef long long int int64_t;
        typedef unsigned long long int uint64_t;
    #elif defined(WIN32) && defined(__GNUC__)
        #include <stdint.h>
    #elif defined(_WIN32)
        typedef __int32 int32_t;
        typedef __int64 int64_t;
        typedef unsigned __int64 uint64_t;
    #else
        #include <inttypes.h>     /* Fallback option */
    #endif
#endif

#ifndef GL_EXT_timer_query
    typedef int64_t GLint64EXT;
    typedef uint64_t GLuint64EXT;
#endif

#endif /* __glext_64bit_types_h_ */
