/**
 * compile with: gcc -o contextRetargetDrawable01 contextRetargetDrawable01.c -lX11 -lGL
 */

#include <stdio.h>
#include <unistd.h>
#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/glx.h>
#include <GL/gl.h>

static PFNGLXSWAPINTERVALSGIPROC _glXSwapIntervalSGI = NULL;

static void testRetarget();

static const char * msg = "contextRetargetDrawable01";

static const useconds_t demodelay = 2 * 1000 * 1000;

int main(int nargs, char **vargs) {
    _glXSwapIntervalSGI = (PFNGLXSWAPINTERVALSGIPROC) glXGetProcAddressARB("glXSwapIntervalSGI");
    if(NULL == _glXSwapIntervalSGI) {
        fprintf(stderr, "No glXSwapIntervalSGI avail, bail out\n");
        return 1;
    }
    testRetarget();
    return 0;
}

static void createGLWin(Display *dpy, int width, int height, Window *rWin, GLXContext *rCtx);
static void useGL(Display *dpy, Window win, GLXContext ctx, int width, int height, float c, int swapInterval);

static void testRetarget() {
    int major, minor;
    Display *disp1;
    Window win1;
    GLXContext ctx1;

    Display *disp2;
    Window win2;
    GLXContext ctx2;

    fprintf(stderr, "%s: Create #1\n", msg);
    disp1 = XOpenDisplay(NULL);
    createGLWin(disp1, 200, 200, &win1, &ctx1);

    fprintf(stderr, "%s: Create #2\n", msg);
    disp2 = disp1;
    // disp2 = XOpenDisplay(NULL);
    createGLWin(disp2, 300, 300, &win2, &ctx2);

    fprintf(stderr, "%s: Use #1.1\n", msg);
    useGL(disp1, win1, ctx1, 200, 200, 0.0f, 1); // OK

    fprintf(stderr, "%s: Use #1.2\n", msg);
    useGL(disp2, win2, ctx2, 300, 300, 1.0f, 1); // OK

    usleep( demodelay );

    fprintf(stderr, "%s: Retarget Drawable\n", msg);
    {
        Window _win = win2;
        win2 = win1;
        win1 = _win;
    }

    fprintf(stderr, "%s: Use #2.1\n", msg);
    useGL(disp1, win1, ctx1, 200, 200, 0.0f, 0); // no setSwapInterval - OK

    fprintf(stderr, "%s: Use #2.2\n", msg);
    useGL(disp2, win2, ctx2, 300, 300, 1.0f, 0); // no setSwapInterval - OK

    usleep( demodelay );

    fprintf(stderr, "%s: Use #3.1\n", msg);
    useGL(disp1, win1, ctx1, 200, 200, 0.1f, 1); // setSwapInterval - crash on Mesa 8.0.4 DRI2

    fprintf(stderr, "%s: Use #3.2\n", msg);
    useGL(disp2, win2, ctx2, 300, 300, 0.9f, 1); // setSwapInterval - crash on Mesa 8.0.4 DRI2

    fprintf(stderr, "%s: Success - no bug\n", msg);

    usleep( demodelay );

    fprintf(stderr, "%s: Destroy #1.0\n", msg);
    glXMakeContextCurrent(disp1, 0, 0, 0);
    glXDestroyContext(disp1, ctx1);
    if( disp1 != disp2 ) {
        XCloseDisplay(disp1);
    }
    fprintf(stderr, "%s: Destroy #1.X\n", msg);

    fprintf(stderr, "%s: Destroy #2.0\n", msg);
    glXMakeContextCurrent(disp2, 0, 0, 0);
    glXDestroyContext(disp2, ctx2);
    XCloseDisplay(disp2);
    fprintf(stderr, "%s: Destroy #2.X\n", msg);

    fprintf(stderr, "%s: Exit - OK\n", msg);
}

static void useGL(Display *dpy, Window win, GLXContext ctx, int width, int height, float c, int swapInterval)
{
    glXMakeContextCurrent(dpy, win, win, ctx);
    glViewport(0, 0, width, height);
    if(0 < swapInterval) {
        fprintf(stderr, "%s: glXSwapIntervalSGI(1)\n", msg);
        _glXSwapIntervalSGI(1); // offending op after retargeting drawable
    }
    fprintf(stderr, "GL_VENDOR: %s\n", glGetString(GL_VENDOR));
    fprintf(stderr, "GL_VERSION: %s\n", glGetString(GL_VERSION));
    fprintf(stderr, "GL_RENDERER: %s\n", glGetString(GL_RENDERER));
    glClearColor(c, c, c, 0.0f);
    glClearDepth(1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glXSwapBuffers(dpy, win);
    glXMakeContextCurrent(dpy, 0, 0, 0);
}

/* attributes for a double buffered visual in RGBA format with at least
 * 4 bits per color and a 16 bit depth buffer */
static int attrListDbl[] = { GLX_RGBA, GLX_DOUBLEBUFFER, 
    GLX_RED_SIZE, 4, 
    GLX_GREEN_SIZE, 4, 
    GLX_BLUE_SIZE, 4, 
    GLX_DEPTH_SIZE, 16,
    None };

static void createGLWin(Display *dpy, int width, int height, Window *rWin, GLXContext *rCtx)
{
    int screen = DefaultScreen(dpy);
    XVisualInfo *vi = glXChooseVisual(dpy, screen, attrListDbl);
    Colormap cmap;
    XSetWindowAttributes attr;

    /* create a GLX context */
    *rCtx = glXCreateContext(dpy, vi, 0, GL_TRUE);

    /* create a color map */
    cmap = XCreateColormap(dpy, RootWindow(dpy, vi->screen), vi->visual, AllocNone);
    attr.colormap = cmap;
    attr.border_pixel = 0;

    /* create a window in window mode*/
    attr.event_mask = ExposureMask | KeyPressMask | ButtonPressMask |
        StructureNotifyMask;
    *rWin = XCreateWindow(dpy, RootWindow(dpy, vi->screen),
        0, 0, width, height, 0, vi->depth, InputOutput, vi->visual,
        CWBorderPixel | CWColormap | CWEventMask, &attr);

    XMapRaised(dpy, *rWin);
}

