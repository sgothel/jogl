/**
 * compile with: gcc -o displayMultiple02 displayMultiple02.c -lX11 -lGL
 */

#include <stdio.h>
#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/glx.h>
#include <GL/gl.h>

static void testOrder(int reverseDestroyOrder, const char * msg);

int main(int nargs, char **vargs) {
    testOrder(0, "Normal order");
    testOrder(1, "Reverse order");
    return 0;
}

static void createGLWin(Display *dpy, int width, int height, Window *rWin, GLXContext *rCtx);
static void useGL(Display *dpy, Window win, GLXContext ctx, int width, int height);

void testOrder(int reverseDestroyOrder, const char * msg) {
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
    useGL(disp1, win1, ctx1, 200, 200);

    fprintf(stderr, "%s: Create #2\n", msg);
    disp2 = XOpenDisplay(NULL);
    createGLWin(disp2, 300, 300, &win2, &ctx2);
    useGL(disp2, win2, ctx2, 300, 300);

    if(reverseDestroyOrder) {
        fprintf(stderr, "%s: Destroy #2\n", msg);
        glXMakeCurrent(disp2, 0, 0);
        glXDestroyContext(disp2, ctx2);
        XCloseDisplay(disp2);

        fprintf(stderr, "%s: Destroy #1\n", msg);
        glXMakeCurrent(disp1, 0, 0);
        glXDestroyContext(disp1, ctx1);
        XCloseDisplay(disp1);
    } else {
        fprintf(stderr, "%s: Destroy #1\n", msg);
        glXMakeCurrent(disp1, 0, 0);
        glXDestroyContext(disp1, ctx1);
        XCloseDisplay(disp1);

        fprintf(stderr, "%s: Destroy #2\n", msg);
        glXMakeCurrent(disp2, 0, 0);
        glXDestroyContext(disp2, ctx2);
        XCloseDisplay(disp2);

    }

    fprintf(stderr, "%s: Success - no bug\n", msg);
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
    glXMakeCurrent(dpy, win, ctx);
    glShadeModel(GL_SMOOTH);
    glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
    glClearDepth(1.0f);
    glViewport(0, 0, width, height);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    glXSwapBuffers(dpy, win);
    glXMakeCurrent(dpy, 0, 0);
}

