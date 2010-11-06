#ifndef __gluegen_types_h
#define __gluegen_types_h

/**
 * These are standard include replacement files
 * for gluegen processing only!
 *
 * Don't include this folder to your native compiler!
 *
 * Purpose of all files within this folder is to define a fixed bitsize
 * across all platforms to allow the resulting java type comfort all.
 * IE a 'intptr_t' shall always be 64bit.
 *
 * We use one size fits all.
 */
#if defined(__STDC_VERSION__) || defined(__GNUC__) || defined (__ARMCC_2__) || \
    defined(__VMS) || defined(__sgi) || defined(__sun__) || defined(__digital__) || defined(__unix__) || defined(__SCO__) || defined(OPENSTEP) || \
    defined(BSD) || defined(FREEBSD) || defined(_HPUX) || defined(SOLARIS) || defined(macosx) || \
    defined(_WIN32) || defined(_WIN32_WCE) || defined(WINVER) || defined(_WIN32_WINNT) || defined(__CYGWIN__) || \
    defined(__SCITECH_SNAP__) || defined (__SYMBIAN32__) || \
    defined(__arch64__) || defined(_LP64)

    #error PLATFORM or COMPILER DEFINES FOUND, not allowed within GLUEGEN HEADER

#endif

/**
 * Look in the GlueGen.java API documentation for the build-in types (terminal symbols) 
 * definition.
 * 
 * The following types are build-in:
 *
 * __int32
 * int32_t
 * uint32_t
 * __int64
 * int64_t
 * uint64_t
 * ptrdiff_t
 * size_t
 */

#endif /* __gluegen_types_h */

