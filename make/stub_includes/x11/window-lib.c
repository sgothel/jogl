#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/extensions/Xrender.h>

// Routines needed from Xlib.h and Xutil.h (placed here to avoid having
// XVisualInfo generated multiple times)
#ifndef _Xconst
#define _Xconst const
#endif /* _Xconst */

extern intptr_t XSynchronize(Display *display, Bool onoff);

extern int XFlush(Display *display);
extern int XSync(Display *display, Bool discard);

extern char *XDisplayString(Display *display);

extern Display *XOpenDisplay(
    _Xconst char*	/* display_name */
);

extern int XCloseDisplay(Display *display);

extern void XLockDisplay(Display *display);

extern void XUnlockDisplay(Display *display);

extern Window RootWindow(Display *display, int screen_number);
extern int DefaultScreen(Display *display);

extern XVisualInfo *XGetVisualInfo(
    Display*		/* display */,
    long		/* vinfo_mask */,
    XVisualInfo*	/* vinfo_template */,
    int*		/* nitems_return */
);

extern Pixmap XCreatePixmap(
    Display*		/* display */,
    Drawable		/* d */,
    unsigned int	/* width */,
    unsigned int	/* height */,
    unsigned int	/* depth */		        
);

extern int XFreePixmap(
    Display*		/* display */,
    Pixmap		/* pixmap */
);

extern int XFree(
    void*		/* data */
);

// Helper routine for querying whether Xinerama is enabled.
Bool XineramaEnabled(Display* display);

// Routines for changing gamma settings.
// Note that these are not currently supported on Solaris.
Bool XF86VidModeGetGammaRampSize(
    Display *display,
    int screen,
    int* size);

Bool XF86VidModeGetGammaRamp(
    Display *display,
    int screen,
    int size,
    unsigned short *red_array,
    unsigned short *green_array,
    unsigned short *blue_array);

Bool XF86VidModeSetGammaRamp(
    Display *display,
    int screen,
    int size,
    unsigned short *red_array,
    unsigned short *green_array,
    unsigned short *blue_array);

XRenderPictFormat * XRenderFindVisualFormat (Display *dpy, _Xconst Visual *visual);

