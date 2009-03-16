#include <inttypes.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/glx.h>
/* Linux headers don't work properly */
#define __USE_GNU
#include <dlfcn.h>
#undef __USE_GNU

/* HP-UX doesn't define RTLD_DEFAULT. */
#if defined(_HPUX) && !defined(RTLD_DEFAULT)
#define RTLD_DEFAULT NULL
#endif

/* We expect glXGetProcAddressARB to be defined */
extern __GLXextFuncPtr glXGetProcAddressARB (const GLubyte *);

