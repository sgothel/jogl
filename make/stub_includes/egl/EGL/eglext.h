#ifndef __eglext_h_
#define __eglext_h_

#ifdef __cplusplus
extern "C" {
#endif

/*
** Copyright (c) 2007 The Khronos Group Inc.
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

/*
 * Copyright (c) 2008 NVIDIA Corporation.  All rights reserved.
 *
 * NVIDIA Corporation and its licensors retain all intellectual property
 * and proprietary rights in and to this software, related documentation
 * and any modifications thereto.  Any use, reproduction, disclosure or
 * distribution of this software and related documentation without an express
 * license agreement from NVIDIA Corporation is strictly prohibited.
 */

#include <EGL/egl.h>
#include <EGL/eglplatform.h>

/*************************************************************/

/* Header file version number */
/* eglext.h last updated 2007/11/20 */
/* Current version at http://www.khronos.org/registry/egl/ */
#define EGL_EGLEXT_VERSION 1

#ifndef EGL_KHR_config_attribs
#define EGL_KHR_config_attribs 1
#define EGL_CONFORMANT_KHR                  0x3042	/* EGLConfig attribute */
#define EGL_VG_COLORSPACE_LINEAR_BIT_KHR    0x0020	/* EGL_SURFACE_TYPE bitfield */
#define EGL_VG_ALPHA_FORMAT_PRE_BIT_KHR		0x0040	/* EGL_SURFACE_TYPE bitfield */
#endif

#ifndef EGL_KHR_lock_surface
#define EGL_KHR_lock_surface 1
#define EGL_READ_SURFACE_BIT_KHR		0x0001	/* EGL_LOCK_USAGE_HINT_KHR bitfield */
#define EGL_WRITE_SURFACE_BIT_KHR		0x0002	/* EGL_LOCK_USAGE_HINT_KHR bitfield */
#define EGL_LOCK_SURFACE_BIT_KHR		0x0080	/* EGL_SURFACE_TYPE bitfield */
#define EGL_OPTIMAL_FORMAT_BIT_KHR		0x0100	/* EGL_SURFACE_TYPE bitfield */
#define EGL_MATCH_FORMAT_KHR			0x3043	/* EGLConfig attribute */
#define EGL_FORMAT_RGB_565_EXACT_KHR		0x30C0	/* EGL_MATCH_FORMAT_KHR value */
#define EGL_FORMAT_RGB_565_KHR			0x30C1	/* EGL_MATCH_FORMAT_KHR value */
#define EGL_FORMAT_RGBA_8888_EXACT_KHR		0x30C2	/* EGL_MATCH_FORMAT_KHR value */
#define EGL_FORMAT_RGBA_8888_KHR		0x30C3	/* EGL_MATCH_FORMAT_KHR value */
#define EGL_MAP_PRESERVE_PIXELS_KHR		0x30C4	/* eglLockSurfaceKHR attribute */
#define EGL_LOCK_USAGE_HINT_KHR			0x30C5	/* eglLockSurfaceKHR attribute */
#define EGL_BITMAP_POINTER_KHR			0x30C6	/* eglQuerySurface attribute */
#define EGL_BITMAP_PITCH_KHR			0x30C7	/* eglQuerySurface attribute */
#define EGL_BITMAP_ORIGIN_KHR			0x30C8	/* eglQuerySurface attribute */
#define EGL_BITMAP_PIXEL_RED_OFFSET_KHR		0x30C9	/* eglQuerySurface attribute */
#define EGL_BITMAP_PIXEL_GREEN_OFFSET_KHR	0x30CA	/* eglQuerySurface attribute */
#define EGL_BITMAP_PIXEL_BLUE_OFFSET_KHR	0x30CB	/* eglQuerySurface attribute */
#define EGL_BITMAP_PIXEL_ALPHA_OFFSET_KHR	0x30CC	/* eglQuerySurface attribute */
#define EGL_BITMAP_PIXEL_LUMINANCE_OFFSET_KHR	0x30CD	/* eglQuerySurface attribute */
#define EGL_LOWER_LEFT_KHR			0x30CE	/* EGL_BITMAP_ORIGIN_KHR value */
#define EGL_UPPER_LEFT_KHR			0x30CF	/* EGL_BITMAP_ORIGIN_KHR value */
#ifdef EGL_EGLEXT_PROTOTYPES
EGLAPI EGLBoolean EGLAPIENTRY eglLockSurfaceKHR (EGLDisplay display, EGLSurface surface, const EGLint *attrib_list);
EGLAPI EGLBoolean EGLAPIENTRY eglUnlockSurfaceKHR (EGLDisplay display, EGLSurface surface);
#endif /* EGL_EGLEXT_PROTOTYPES */
typedef EGLBoolean (EGLAPIENTRYP PFNEGLLOCKSURFACEKHRPROC) (EGLDisplay display, EGLSurface surface, const EGLint *attrib_list);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLUNLOCKSURFACEKHRPROC) (EGLDisplay display, EGLSurface surface);
#endif

#ifndef EGL_KHR_image
#define EGL_KHR_image 1
#define EGL_NATIVE_PIXMAP_KHR			0x30B0	/* eglCreateImageKHR target */
typedef void *EGLImageKHR;
/* Manual: #define EGL_NO_IMAGE_KHR ((EGLImageKHR)0) */
#ifdef EGL_EGLEXT_PROTOTYPES
EGLAPI EGLImageKHR EGLAPIENTRY eglCreateImageKHR (EGLDisplay dpy, EGLContext ctx, EGLenum target, EGLClientBuffer buffer, EGLint *attr_list);
EGLAPI EGLBoolean EGLAPIENTRY eglDestroyImageKHR (EGLDisplay dpy, EGLImageKHR image);
#endif /* EGL_EGLEXT_PROTOTYPES */
typedef EGLImageKHR (EGLAPIENTRYP PFNEGLCREATEIMAGEKHRPROC) (EGLDisplay dpy, EGLContext ctx, EGLenum target, EGLClientBuffer buffer, EGLint *attr_list);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLDESTROYIMAGEKHRPROC) (EGLDisplay dpy, EGLImageKHR image);
#endif

#ifndef EGL_KHR_vg_parent_image
#define EGL_KHR_vg_parent_image 1
#define EGL_VG_PARENT_IMAGE_KHR			0x30BA	/* eglCreateImageKHR target */
#endif

#ifndef EGL_KHR_gl_texture_2D_image
#define EGL_KHR_gl_texture_2D_image 1
#define EGL_GL_TEXTURE_2D_KHR			0x30B1	/* eglCreateImageKHR target */
#define EGL_GL_TEXTURE_LEVEL_KHR		0x30BC	/* eglCreateImageKHR attribute */
#endif

#ifndef EGL_KHR_gl_texture_cubemap_image
#define EGL_KHR_gl_texture_cubemap_image 1
#define EGL_GL_TEXTURE_CUBE_MAP_POSITIVE_X_KHR	0x30B3	/* eglCreateImageKHR target */
#define EGL_GL_TEXTURE_CUBE_MAP_NEGATIVE_X_KHR	0x30B4	/* eglCreateImageKHR target */
#define EGL_GL_TEXTURE_CUBE_MAP_POSITIVE_Y_KHR	0x30B5	/* eglCreateImageKHR target */
#define EGL_GL_TEXTURE_CUBE_MAP_NEGATIVE_Y_KHR	0x30B6	/* eglCreateImageKHR target */
#define EGL_GL_TEXTURE_CUBE_MAP_POSITIVE_Z_KHR	0x30B7	/* eglCreateImageKHR target */
#define EGL_GL_TEXTURE_CUBE_MAP_NEGATIVE_Z_KHR	0x30B8	/* eglCreateImageKHR target */
#endif

#ifndef EGL_KHR_gl_texture_3D_image
#define EGL_KHR_gl_texture_3D_image 1
#define EGL_GL_TEXTURE_3D_KHR			0x30B2	/* eglCreateImageKHR target */
#define EGL_GL_TEXTURE_ZOFFSET_KHR		0x30BD	/* eglCreateImageKHR attribute */
#endif

#ifndef EGL_KHR_gl_renderbuffer_image
#define EGL_KHR_gl_renderbuffer_image 1
#define EGL_GL_RENDERBUFFER_KHR			0x30B9	/* eglCreateImageKHR target */
#endif

// Supported NV Extensions

//  NOTE: EGL_KHR_bind_client_buffer == EGL_create_pbuffer_from_client_buffer
/* EGL_KHR_bind_client_buffer */
#ifndef EGL_KHR_bind_client_buffer
#define EGL_create_pbuffer_from_client_buffer 1
#define EGL_KHR_bind_client_buffer              1
#define EGL_OPENVG_IMAGE_KHR                EGL_OPENVG_IMAGE
#endif

/* eglCreateImageKHR targets */
/* eglCreateImageKHR target and eglCreatePbufferFromClientBuffer buftype */

/* EGL_NV_perfmon */
#if 0
#ifndef EGL_NV_perfmon
#define EGL_NV_perfmon 1
#define EGL_PERFMONITOR_HARDWARE_COUNTERS_BIT_NV    0x00000001
#define EGL_PERFMONITOR_OPENGL_ES_API_BIT_NV        0x00000010
#define EGL_PERFMONITOR_OPENVG_API_BIT_NV           0x00000020
#define EGL_PERFMONITOR_OPENGL_ES2_API_BIT_NV       0x00000040
#define EGL_COUNTER_NAME_NV            0x1234
#define EGL_COUNTER_DESCRIPTION_NV     0x1235
#define EGL_IS_HARDWARE_COUNTER_NV     0x1236
#define EGL_COUNTER_MAX_NV             0x1237
#define EGL_COUNTER_VALUE_TYPE_NV      0x1238
#define EGL_RAW_VALUE_NV               0x1239
#define EGL_PERCENTAGE_VALUE_NV        0x1240
#define EGL_BAD_CURRENT_PERFMONITOR_NV 0x1241
/* Manual: #define EGL_NO_PERFMONITOR_NV ((EGLPerfMonitorNV)0) */
/* Manual: #define EGL_DEFAULT_PERFMARKER_NV ((EGLPerfMarkerNV)0) */
typedef void *EGLPerfMonitorNV;
typedef void *EGLPerfMarkerNV;
#ifdef USE_GLUEGEN
    /* GlueGen currently needs this form of typedef to produce distinct
       types for each of these pointer types */
    typedef struct {} _EGLPerfCounterNV, *EGLPerfCounterNV;
#else
    typedef void *EGLPerfCounterNV;
#endif
#ifdef EGL_EGLEXT_PROTOTYPES
EGLAPI EGLPerfMonitorNV EGLAPIENTRY eglCreatePerfMonitorNV(EGLDisplay dpy, EGLint mask);
EGLAPI EGLBoolean EGLAPIENTRY eglDestroyPerfMonitorNV(EGLDisplay dpy, EGLPerfMonitorNV monitor);
EGLAPI EGLBoolean EGLAPIENTRY eglMakeCurrentPerfMonitorNV(EGLPerfMonitorNV monitor);
EGLAPI EGLPerfMonitorNV EGLAPIENTRY eglGetCurrentPerfMonitorNV(void);
EGLAPI EGLBoolean EGLAPIENTRY eglGetPerfCountersNV(EGLPerfMonitorNV monitor, EGLPerfCounterNV *counters, EGLint counter_size, EGLint *num_counter);
EGLAPI EGLBoolean EGLAPIENTRY eglGetPerfCounterAttribNV(EGLPerfMonitorNV monitor, EGLPerfCounterNV counter, EGLint pname, EGLint *value);
EGLAPI const char * EGLAPIENTRY eglQueryPerfCounterStringNV(EGLPerfMonitorNV monitor, EGLPerfCounterNV counter, EGLint pname);
EGLAPI EGLBoolean EGLAPIENTRY eglPerfMonitorAddCountersNV(EGLint n, const EGLPerfCounterNV *counters);
EGLAPI EGLBoolean EGLAPIENTRY eglPerfMonitorRemoveCountersNV(EGLint n, const EGLPerfCounterNV *counters);
EGLAPI EGLBoolean EGLAPIENTRY eglPerfMonitorRemoveAllCountersNV(void);
EGLAPI EGLBoolean EGLAPIENTRY eglPerfMonitorBeginExperimentNV(void);
EGLAPI EGLBoolean EGLAPIENTRY eglPerfMonitorEndExperimentNV(void);
EGLAPI EGLBoolean EGLAPIENTRY eglPerfMonitorBeginPassNV(EGLint n);
EGLAPI EGLBoolean EGLAPIENTRY eglPerfMonitorEndPassNV(void);
EGLAPI EGLPerfMarkerNV EGLAPIENTRY eglCreatePerfMarkerNV(void);
EGLAPI EGLBoolean EGLAPIENTRY eglDestroyPerfMarkerNV(EGLPerfMarkerNV marker);
EGLAPI EGLBoolean EGLAPIENTRY eglMakeCurrentPerfMarkerNV(EGLPerfMarkerNV marker);
EGLAPI EGLPerfMarkerNV EGLAPIENTRY eglGetCurrentPerfMarkerNV(void);
EGLAPI EGLBoolean EGLAPIENTRY eglGetPerfMarkerCounterNV(EGLPerfMarkerNV marker, EGLPerfCounterNV counter, EGLuint64NV *value, EGLuint64NV *cycles);
EGLAPI EGLBoolean EGLAPIENTRY eglValidatePerfMonitorNV(EGLint *num_passes);
#endif
typedef EGLPerfMonitorNV (EGLAPIENTRYP PFNEGLCREATEPERFMONITORNVPROC)(EGLDisplay dpy, EGLint mask);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLDESTROYPERFMONITORNVPROC)(EGLDisplay dpy, EGLPerfMonitorNV monitor);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLMAKECURRENTPERFMONITORNVPROC)(EGLPerfMonitorNV monitor);
typedef EGLPerfMonitorNV (EGLAPIENTRYP PFNEGLGETCURRENTPERFMONITORNVPROC)(void);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLGETPERFCOUNTERSNVPROC)(EGLPerfMonitorNV monitor, EGLPerfCounterNV *counters, EGLint counter_size, EGLint *num_counter);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLGETPERFCOUNTERATTRIBNVPROC)(EGLPerfMonitorNV monitor, EGLPerfCounterNV counter, EGLint pname, EGLint *value);
typedef const char * (EGLAPIENTRYP PFNEGLQUERYPERFCOUNTERSTRINGNVPROC)(EGLPerfMonitorNV monitor, EGLPerfCounterNV counter, EGLint pname);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLPERFMONITORADDCOUNTERSNVPROC)(EGLint n, const EGLPerfCounterNV *counters);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLPERFMONITORREMOVECOUNTERSNVPROC)(EGLint n, const EGLPerfCounterNV *counters);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLPERFMONITORREMOVEALLCOUNTERSNVPROC)(void);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLPERFMONITORBEGINEXPERIMENTNVPROC)(void);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLPERFMONITORENDEXPERIMENTNVPROC)(void);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLPERFMONITORBEGINPASSNVPROC)(EGLint n);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLPERFMONITORENDPASSNVPROC)(void);
typedef EGLPerfMarkerNV (EGLAPIENTRYP PFNEGLCREATEPERFMARKERNVPROC)(void);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLDESTROYPERFMARKERNVPROC)(EGLPerfMarkerNV marker);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLMAKECURRENTPERFMARKERNVPROC)(EGLPerfMarkerNV marker);
typedef EGLPerfMarkerNV (EGLAPIENTRYP PFNEGLGETCURRENTPERFMARKERNVPROC)(void);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLGETPERFMARKERCOUNTERNVPROC)(EGLPerfMarkerNV marker, EGLPerfCounterNV counter, EGLuint64NV *value, EGLuint64NV *cycles);
typedef EGLBoolean (EGLAPIENTRYP PFNEGLVALIDATEPERFMONITORNVPROC)(EGLint *num_passes);
#endif
#endif // exclude EGL_NV_perfmon

/* EGL_NV_texture_rectangle (reuse analagous WGL enum) */
#ifndef EGL_NV_texture_rectangle
#define EGL_NV_texture_rectangle 1
#define EGL_GL_TEXTURE_RECTANGLE_NV_KHR           0x30BB
#define EGL_TEXTURE_RECTANGLE_NV       0x20A2
#endif

#ifndef EGL_NV_system_time
#define EGL_NV_system_time 1
#ifdef EGL_EGLEXT_PROTOTYPES
EGLAPI EGLuint64NV EGLAPIENTRY eglGetSystemTimeFrequencyNV(void);
EGLAPI EGLuint64NV EGLAPIENTRY eglGetSystemTimeNV(void);
#endif
typedef EGLuint64NV (EGLAPIENTRYP PFNEGLGETSYSTEMTIMEFREQUENCYNVPROC)(void);
typedef EGLuint64NV (EGLAPIENTRYP PFNEGLGETSYSTEMTIMENVPROC)(void);
#endif

/* EGL_NV_coverage_sample (reuse GLX_VIDEO_OUT_DEPTH_NV and
 * GLX_VIDEO_OUT_COLOR_AND_ALPHA_NV)
 */
#ifndef EGL_NV_coverage_sample
#define EGL_NV_coverage_sample 1
#define EGL_COVERAGE_BUFFERS_NV 0x30E0
#define EGL_COVERAGE_SAMPLES_NV 0x30E1
#endif

/* EGL_NV_depth_nonlinear
 */
#ifndef EGL_NV_depth_nonlinear
#define EGL_NV_depth_nonlinear 1
#define EGL_DEPTH_ENCODING_NV 0x30E2
#define EGL_DEPTH_ENCODING_NONE_NV 0
#define EGL_DEPTH_ENCODING_NONLINEAR_NV 0x30E3
#endif

#ifndef EGL_NV_nvva_image
#define EGL_NV_nvva_image 1
#define EGL_NVVA_OUTPUT_SURFACE_NV                0x30BE
EGLAPI EGLBoolean EGLAPIENTRY eglVideoImageLockNV(EGLDisplay dpy, EGLImageKHR image, EGLenum api);
EGLAPI EGLBoolean EGLAPIENTRY eglVideoImageUnlockNV(EGLDisplay dpy, EGLImageKHR image);
#endif

#ifndef EGL_NV_nvma_image
#define EGL_NV_nvma_image 1
#define EGL_NVMA_OUTPUT_SURFACE_NV                0x30BE /* eglCreateImageKHR target and eglCreatePbufferFromClientBuffer buftype */
#endif

#ifndef EGL_NV_client_api_nvma
#define EGL_NV_client_api_nvma 1
EGLAPI EGLBoolean EGLAPIENTRY eglNvmaOutputSurfacePbufferLock(EGLDisplay display,
                                                          EGLSurface pbuffer);
EGLAPI EGLBoolean EGLAPIENTRY eglNvmaOutputSurfacePbufferUnlock(EGLDisplay display,
                                                            EGLSurface pbuffer);
#endif


/**
 * EGL_KHR_sync
 */
#ifndef EGL_KHR_sync
/* !!!!! TODO: Get correct values for these defines !!!! 
   Do not rely on these values...THEY WILL CHANGE!
   And it WILL BREAK binary compatibility when they do! */
#define EGL_SYNC_PRIOR_COMMANDS_COMPLETE_KHR     0x3100
#define EGL_SYNC_NATIVE_SYNC_KHR                 0x3101
#define EGL_SYNC_STATUS_KHR                      0x3102
#define EGL_SIGNALED_KHR                         0x3103
#define EGL_UNSIGNALED_KHR                       0x3104
#define EGL_SYNC_FLUSH_COMMANDS_BIT_KHR          0x0001
/* Manual: #define EGL_FOREVER_KHR                          0xFFFFFFFFFFFFFFFFull */
#define EGL_ALREADY_SIGNALED_KHR                 0x3105
#define EGL_TIMEOUT_EXPIRED_KHR                  0x3106
#define EGL_CONDITION_SATISFIED_KHR              0x3107
#define EGL_SYNC_TYPE_KHR                        0x3108
#define EGL_SYNC_CONDITION_KHR                   0x3109
#define EGL_SYNC_FENCE_KHR                       0x310A
#define EGL_NO_SYNC_KHR                          0x0000

typedef void* EGLSyncKHR;
typedef void* NativeSyncKHR;
typedef uint64_t EGLTimeKHR;

#endif

#ifndef EGL_KHR_sync
#define EGL_KHR_sync 1
#ifdef EGL_EGLEXT_PROTOTYPES
EGLSyncKHR eglCreateFenceSyncKHR( EGLDisplay dpy, EGLenum condition, const EGLint *attrib_list ); 
NativeSyncKHR eglCreateNativeSyncKHR( EGLSyncKHR sync );
EGLBoolean eglDestroySyncKHR( EGLSyncKHR sync );
EGLBoolean eglFenceKHR( EGLSyncKHR sync );
EGLint eglClientWaitSyncKHR( EGLSyncKHR sync, EGLint flags, EGLTimeKHR timeout );
EGLBoolean eglSignalSyncKHR( EGLSyncKHR sync, EGLenum mode );
EGLBoolean eglGetSyncAttribKHR( EGLSyncKHR sync, EGLint attribute, EGLint *value );
#endif
typedef EGLSyncKHR (EGLAPIENTRYP PFNEGLCREATEFENCESYNCKHRPROC)( EGLDisplay dpy, EGLenum condition, const EGLint *attrib_list );
typedef NativeSyncKHR (EGLAPIENTRYP PFNEGLCREATENATIVESYNCKHRPROC)( EGLSyncKHR sync );
typedef EGLBoolean (EGLAPIENTRYP PFNEGLDESTROYSYNCKHRPROC)( EGLSyncKHR sync );
typedef EGLBoolean (EGLAPIENTRYP PFNEGLFENCEKHRPROC)( EGLSyncKHR sync );
typedef EGLint (EGLAPIENTRYP PFNEGLCLIENTWAITSYNCKHRPROC)( EGLSyncKHR sync, EGLint flags, EGLTimeKHR timeout );
typedef EGLBoolean (EGLAPIENTRYP PFNEGLSIGNALSYNCKHRPROC)( EGLSyncKHR sync, EGLenum mode );
typedef EGLBoolean (EGLAPIENTRYP PFNEGLGETSYNCATTRIBKHRPROC)( EGLSyncKHR sync, EGLint attribute, EGLint *value );
#endif

/* EGL_NV_omx_il_sink
 */
#ifndef EGL_NV_omx_il_sink
#define EGL_NV_omx_il_sink
#define EGL_OPENMAX_IL_BIT_NV  0x0010 /* EGL_RENDERABLE_TYPE bit */
#define EGL_SURFACE_OMX_IL_EVENT_CALLBACK_NV 0x309A /* eglCreate*Surface attribute */
#define EGL_SURFACE_OMX_IL_EMPTY_BUFFER_DONE_CALLBACK_NV 0x309B /* eglCreate*Surface attribute */
#define EGL_SURFACE_OMX_IL_CALLBACK_DATA_NV 0x309C /* eglCreate*Surface attribute */
#define EGL_SURFACE_COMPONENT_HANDLE_NV 0x309D /* eglQuerySurface attribute */
#endif

#define EGL_RMSURFACE_NV 0x30EF /* eglCreateImageKHR target */

#ifdef __cplusplus
}
#endif

#endif
