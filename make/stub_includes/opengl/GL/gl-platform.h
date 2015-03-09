#ifndef __gl_platform_h_
#define __gl_platform_h_

#if defined(__BEOS__)
#include <stdlib.h>     /* to get some BeOS-isms */
#endif

#if !defined(OPENSTEP) && (defined(NeXT) || defined(NeXT_PDO))
#define OPENSTEP
#endif

#if defined(_WIN32) && !defined(__WIN32__) && !defined(__CYGWIN__)
#define __WIN32__
#endif

/*
 * WINDOWS: Include windows.h here to define APIENTRY.
 * It is also useful when applications include this file by
 * including only glut.h, since glut.h depends on windows.h.
 * Applications needing to include windows.h with parms other
 * than "WIN32_LEAN_AND_MEAN" may include windows.h before
 * glut.h or gl.h.
 */
#if defined(_WIN32) && !defined(APIENTRY) && !defined(__CYGWIN__) && !defined(__SCITECH_SNAP__)
#define WIN32_LEAN_AND_MEAN 1
#include <windows.h>
#undef WIN32_LEAN_AND_MEAN
#endif

#if !defined(OPENSTEP) && (defined(__WIN32__) && !defined(__CYGWIN__))
#  if defined(_MSC_VER) && defined(BUILD_GL32) /* tag specify we're building mesa as a DLL */
#    define GLAPI __declspec(dllexport)
#  elif defined(_MSC_VER) && defined(_DLL) /* tag specifying we're building for DLL runtime support */
#    define GLAPI __declspec(dllimport)
#  else /* for use with static link lib build of Win32 edition only */
#    define GLAPI extern
#  endif /* _STATIC_MESA support */
#  ifndef APIENTRY
#    define APIENTRY __stdcall
#  endif /* APIENTRY */
#else
/* non-Windows compilation */
#  ifndef GLAPI
#    define GLAPI extern
#  endif
#  ifndef APIENTRY
#    define APIENTRY
#  endif
#endif /* WIN32 / CYGWIN bracket */

#ifndef GLAPI
#define GLAPI extern
#endif
#ifndef APIENTRYP
#define APIENTRYP APIENTRY *
#endif

#if (defined(__BEOS__) && defined(__POWERPC__)) || defined(__QUICKDRAW__)
#  define PRAGMA_EXPORT_SUPPORTED               1
#endif

#if defined(macintosh) && PRAGMA_IMPORT_SUPPORTED
#pragma import on
#endif

#ifdef CENTERLINE_CLPP
#define signed
#endif

#if defined(PRAGMA_EXPORT_SUPPORTED)
#pragma export on
#endif

#endif /* __gl_platform_h_ */

