#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/glx.h>
#include <stdio.h>

int main(int nargs, char **vargs) {
    int major, minor;
    Display *disp = XOpenDisplay(NULL);
    glXQueryVersion(disp, &major, &minor);
    fprintf(stderr, "%p: %d.%d\n", disp, major, minor);
    XCloseDisplay(disp);
    disp = XOpenDisplay(NULL);
    glXQueryVersion(disp, &major, &minor);
    fprintf(stderr, "%p: %d.%d\n", disp, major, minor);
    XCloseDisplay(disp);
    return 0;
}
