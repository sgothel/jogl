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
**
** This version of eglplatform.h is used to generate the glue code for the Java-side
** EGL class, and is intended to be platform-independent.
**
*/

#ifdef __cplusplus
extern "C" {
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
typedef void* EGLNativeWindowType;
typedef void* EGLNativePixmapType;

#ifdef __cplusplus
}
#endif

#endif /* EGLPLATFORM_H */
