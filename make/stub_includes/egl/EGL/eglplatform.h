/*
 * Copyright (c) 2007 NVIDIA Corporation.  All rights reserved.
 *
 * NVIDIA Corporation and its licensors retain all intellectual property
 * and proprietary rights in and to this software, related documentation
 * and any modifications thereto.  Any use, reproduction, disclosure or
 * distribution of this software and related documentation without an express
 * license agreement from NVIDIA Corporation is strictly prohibited.
 */

#ifndef EGLPLATFORM_H
#define EGLPLATFORM_H

/*
** eglplatform.h is platform dependent. It defines:
**
**     - Native types
**     - EGL and native handle values
**
** EGLNativeDisplayType, EGLNativeWindowType and EGLNativePixmapType are to be
** replaced with corresponding types of the native window system in egl.h.
**
** EGL and native handle values must match their types.
*/

#ifdef __cplusplus
extern "C" {
#endif

#if defined(_WIN32) && !defined(__GNUC__)
    typedef signed   __int32 int32_t;
    typedef unsigned __int32 uint32_t;
    typedef signed   __int64 int64_t;
    typedef unsigned __int64 uint64_t;
#else
    // Building on obsolete platform on SPARC right now
    #ifdef __sparc
        #include <inttypes.h>
    #else
        #include <stdint.h>
    #endif
#endif

// Define storage class specifiers
#ifndef APIENTRY
#define APIENTRY
#endif

#ifndef EGLAPIENTRY
#define EGLAPIENTRY 
#endif
#ifndef EGLAPIENTRYP
#define EGLAPIENTRYP EGLAPIENTRY *
#endif

#define EGLAPI

// Define native window system types
typedef int   EGLNativeDisplayType;
typedef void* EGLNativePointerType;
typedef void* EGLNativeWindowType;
typedef void* EGLNativePixmapType;

// Define 64-bit integer extensions
typedef int64_t  EGLint64NV;
typedef uint64_t EGLuint64NV;

// Define the pre-EGL 1.3 Native handle types for backwards compatibility
typedef EGLNativeDisplayType NativeDisplayType;
typedef EGLNativePixmapType  NativePixmapType;
typedef EGLNativeWindowType  NativeWindowType;

#ifdef __cplusplus
}
#endif

#endif /* EGLPLATFORM_H */
