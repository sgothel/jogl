/**
 * compile with: gcc -o displayMultiple02 displayMultiple02.c -lX11 -lGL
 */

#include <stdio.h>
#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/glx.h>
#include <GL/gl.h>

static void testExtensions();

int main(int nargs, char **vargs) {
    testExtensions();
    return 0;
}

static void createGLWin(Display *dpy, int width, int height, Window *rWin, GLXContext *rCtx);
static void useGL(Display *dpy, Window win, GLXContext ctx, int width, int height);

void testExtensions() {
    int major, minor;
    Display *disp1;
    Window win1;
    GLXContext ctx1;

    disp1 = XOpenDisplay(NULL);
    createGLWin(disp1, 200, 200, &win1, &ctx1);
    if(0 != win1 && 0 != ctx1) {
        useGL(disp1, win1, ctx1, 200, 200);

        glXMakeCurrent(disp1, 0, 0);
        glXDestroyContext(disp1, ctx1);
    }
    XCloseDisplay(disp1);
}

/* attributes for a double buffered visual in RGBA format with at least
 * 4 bits per color and a 16 bit depth buffer */
static int attrListDbl[] = { GLX_RGBA, GLX_DOUBLEBUFFER, 
    GLX_RED_SIZE, 4, 
    GLX_GREEN_SIZE, 4, 
    GLX_BLUE_SIZE, 4, 
    GLX_DEPTH_SIZE, 16,
    None };

void createGLWin(Display *dpy, int width, int height, Window *rWin, GLXContext *rCtx)
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

void useGL(Display *dpy, Window win, GLXContext ctx, int width, int height)
{
    PFNGLGETSTRINGIPROC glGetStringi = 0;
    int i, n;

    glXMakeCurrent(dpy, win, ctx);

    fprintf(stderr, "GL_VENDOR: %s\n", glGetString(GL_VENDOR));
    fprintf(stderr, "GL_VERSION: %s\n", glGetString(GL_VERSION));
    fprintf(stderr, "GL_RENDERER: %s\n", glGetString(GL_RENDERER));

    glGetIntegerv(GL_NUM_EXTENSIONS, &n);
    fprintf(stderr, "GL_NUM_EXTENSIONS: %d\n", n);

    glGetStringi = (PFNGLGETSTRINGIPROC)glXGetProcAddressARB("glGetStringi");
    if(NULL==glGetStringi) {
        return;
    }

    for (i=0; i<n; i++) {
      const char* extension = (const char*)glGetStringi(GL_EXTENSIONS, i);
      fprintf(stderr, "GL_EXTENSION %d/%d: %s\n", (i+1), n, extension);
    }

}

