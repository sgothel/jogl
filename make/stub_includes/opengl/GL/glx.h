/* $Id$ */

/*
 * Mesa 3-D graphics library
 * Version:  4.1
 * 
 * Copyright (C) 1999-2002  Brian Paul   All Rights Reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * BRIAN PAUL BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */


#ifndef GLX_H
#define GLX_H


#ifdef __VMS
#include <GL/vms_x_fix.h>
# ifdef __cplusplus
/* VMS Xlib.h gives problems with C++.
 * this avoids a bunch of trivial warnings */
#pragma message disable nosimpint
#endif
#endif
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#ifdef __VMS
# ifdef __cplusplus
#pragma message enable nosimpint
#endif
#endif

// Due to 'glext-supplement.h', we prefer to include it manually afterwards.
// This gives us the same behavior as the vanilla GLES, GLES2, GLES3 and glcorearb header.
// #include <GL/gl.h>

#if defined(USE_MGL_NAMESPACE)
#include <GL/glx_mangle.h>
#endif


#ifdef __cplusplus
extern "C" {
#endif

#ifndef GLX_VERSION_1_0
#define GLX_VERSION_1_0 1

#define GLX_EXTENSION_NAME   "GLX"

/*
 * Tokens for glXChooseVisual and glXGetConfig:
 */
#define GLX_USE_GL		1
#define GLX_BUFFER_SIZE		2
#define GLX_LEVEL		3
#define GLX_RGBA		4
#define GLX_DOUBLEBUFFER	5
#define GLX_STEREO		6
#define GLX_AUX_BUFFERS		7
#define GLX_RED_SIZE		8
#define GLX_GREEN_SIZE		9
#define GLX_BLUE_SIZE		10
#define GLX_ALPHA_SIZE		11
#define GLX_DEPTH_SIZE		12
#define GLX_STENCIL_SIZE	13
#define GLX_ACCUM_RED_SIZE	14
#define GLX_ACCUM_GREEN_SIZE	15
#define GLX_ACCUM_BLUE_SIZE	16
#define GLX_ACCUM_ALPHA_SIZE	17


/*
 * Error codes returned by glXGetConfig:
 */
#define GLX_BAD_SCREEN		1
#define GLX_BAD_ATTRIBUTE	2
#define GLX_NO_EXTENSION	3
#define GLX_BAD_VISUAL		4
#define GLX_BAD_CONTEXT		5
#define GLX_BAD_VALUE       	6
#define GLX_BAD_ENUM		7

typedef struct __GLXcontextRec *GLXContext;
typedef XID GLXPixmap;
typedef XID GLXDrawable;

extern XVisualInfo* glXChooseVisual( Display *dpy, int screen,
				     int *attribList );

extern GLXContext glXCreateContext( Display *dpy, XVisualInfo *vis,
				    GLXContext shareList, Bool direct );

extern void glXDestroyContext( Display *dpy, GLXContext ctx );

extern Bool glXMakeCurrent( Display *dpy, GLXDrawable drawable,
			    GLXContext ctx);

extern void glXCopyContext( Display *dpy, GLXContext src, GLXContext dst,
			    unsigned long mask );

extern void glXSwapBuffers( Display *dpy, GLXDrawable drawable );

extern GLXPixmap glXCreateGLXPixmap( Display *dpy, XVisualInfo *visual,
				     Pixmap pixmap );

extern void glXDestroyGLXPixmap( Display *dpy, GLXPixmap pixmap );

extern Bool glXQueryExtension( Display *dpy, int *errorb, int *event );

extern Bool glXQueryVersion( Display *dpy, int *maj, int *min );

extern Bool glXIsDirect( Display *dpy, GLXContext ctx );

extern int glXGetConfig( Display *dpy, XVisualInfo *visual,
			 int attrib, int *value );

extern GLXContext glXGetCurrentContext( void );

extern GLXDrawable glXGetCurrentDrawable( void );

extern void glXWaitGL( void );

extern void glXWaitX( void );

extern void glXUseXFont( Font font, int first, int count, int list );


#endif /* GLX_VERSION_1_0 */


#ifndef GLX_VERSION_1_1
#define GLX_VERSION_1_1 1

#define GLX_VENDOR		1
#define GLX_VERSION		2
#define GLX_EXTENSIONS 		3

extern const char *glXQueryExtensionsString( Display *dpy, int screen );

extern const char *glXQueryServerString( Display *dpy, int screen, int name );

extern const char *glXGetClientString( Display *dpy, int name );

#endif  /* GLX_VERSION_1_1 */


#ifndef GLX_VERSION_1_2
#define GLX_VERSION_1_2 1

extern Display *glXGetCurrentDisplay( void );

#endif  /* GLX_VERSION_1_2 */

/** 
 * We require 'glXGetProcAddress' and 'glXGetProcAddressARB'
 * to be available in GLX, rather than GLXExt. 
 */
#ifndef GLX_VERSION_1_4
#define GLX_VERSION_1_4 1

typedef void ( *__GLXextFuncPtr)(void);
#define GLX_SAMPLE_BUFFERS                100000
#define GLX_SAMPLES                       100001
typedef __GLXextFuncPtr ( *PFNGLXGETPROCADDRESSPROC) (const GLubyte *procName);
#ifdef GLX_GLXEXT_PROTOTYPES
__GLXextFuncPtr glXGetProcAddress (const GLubyte *procName);
#endif
#endif /* GLX_VERSION_1_4 */

#ifndef GLX_ARB_get_proc_address
#define GLX_ARB_get_proc_address 1
typedef __GLXextFuncPtr ( *PFNGLXGETPROCADDRESSARBPROC) (const GLubyte *procName);
#ifdef GLX_GLXEXT_PROTOTYPES
__GLXextFuncPtr glXGetProcAddressARB (const GLubyte *procName);
#endif
#endif /* GLX_ARB_get_proc_address */

// Due to 'glext-supplement.h', we prefer to include it manually afterwards.
// This gives us the same behavior as the vanilla GLES, GLES2, GLES3 and glcorearb header.
// #include <GL/glxext.h>

#ifdef __cplusplus
}
#endif

#endif
