/**
 * compile with: gcc -o contextRetargetDrawable02 contextRetargetDrawable02.c -lX11 -lGL
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <X11/X.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/glx.h>
#include <GL/gl.h>

typedef int bool;
#define true 1
#define false 0

static PFNGLXSWAPINTERVALSGIPROC _glXSwapIntervalSGI = NULL;

static void testRetarget(bool reverse);

static const char * msg = "contextRetargetDrawable01";

static const useconds_t demodelay = 2 * 1000 * 1000;

int main(int nargs, char **vargs) {
    _glXSwapIntervalSGI = (PFNGLXSWAPINTERVALSGIPROC) glXGetProcAddressARB("glXSwapIntervalSGI");
    if(NULL == _glXSwapIntervalSGI) {
        fprintf(stderr, "No glXSwapIntervalSGI avail, bail out\n");
        return 1;
    }
    testRetarget(false);
    return 0;
}

static void createGLWin(Display *dpy, int width, int height, Window *rWin, GLXContext *rCtx);
static void useGL(Display *dpy, Window win, GLXContext ctx, int width, int height, float c, int swapInterval);

static void testRetarget(bool reverse) {
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
        GLXContext _ctx = ctx2;
        ctx2 = ctx1;
        ctx1 = _ctx;
    }

    /**
    if(reverse) {
        fprintf(stderr, "%s: Use #2.2\n", msg);
        useGL(disp2, win2, ctx2, 300, 300, 1.0f, 0); // no setSwapInterval - OK

        fprintf(stderr, "%s: Use #2.1\n", msg);
        useGL(disp1, win1, ctx1, 200, 200, 0.0f, 0); // no setSwapInterval - OK
    } else {
        fprintf(stderr, "%s: Use #2.1\n", msg);
        useGL(disp1, win1, ctx1, 200, 200, 0.0f, 0); // no setSwapInterval - OK

        fprintf(stderr, "%s: Use #2.2\n", msg);
        useGL(disp2, win2, ctx2, 300, 300, 1.0f, 0); // no setSwapInterval - OK
    }
    usleep( demodelay ); */

    if(reverse) {
        fprintf(stderr, "%s: Use #3.2\n", msg);
        useGL(disp2, win2, ctx2, 300, 300, 0.9f, 1); // setSwapInterval - crash on Mesa 8.0.4 DRI2

        fprintf(stderr, "%s: Use #3.1\n", msg);
        useGL(disp1, win1, ctx1, 200, 200, 0.1f, 1); // setSwapInterval - crash on Mesa 8.0.4 DRI2
    } else {
        fprintf(stderr, "%s: Use #3.1\n", msg);
        useGL(disp1, win1, ctx1, 200, 200, 0.1f, 1); // setSwapInterval - crash on Mesa 8.0.4 DRI2

        fprintf(stderr, "%s: Use #3.2\n", msg);
        useGL(disp2, win2, ctx2, 300, 300, 0.9f, 1); // setSwapInterval - crash on Mesa 8.0.4 DRI2
    }
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

static volatile bool ctxErrorOccurred = false;
static int ctxErrorHandler( Display *dpy, XErrorEvent *e )
{
    const char * errnoStr = strerror(errno);
    char errCodeStr[80];
    char reqCodeStr[80];

    snprintf(errCodeStr, sizeof(errCodeStr), "%d", e->request_code);
    XGetErrorDatabaseText(dpy, "XRequest", errCodeStr, "Unknown", reqCodeStr, sizeof(reqCodeStr));
    XGetErrorText(dpy, e->error_code, errCodeStr, sizeof(errCodeStr));

    fprintf(stderr, "X11 Error: %d - %s, dpy %p, id %x, # %d: %d:%d %s\n",
        e->error_code, errCodeStr, e->display, (int)e->resourceid, (int)e->serial,
        (int)e->request_code, (int)e->minor_code, reqCodeStr);
    fflush(stderr);

    ctxErrorOccurred = true;
    return 0;
}

/* attributes for a double buffered visual in RGBA format with at least
 * 8 bits per color and a 16 bit depth buffer */
static int visual_attribs[] = {
  GLX_X_RENDERABLE    , True,
  GLX_DRAWABLE_TYPE   , GLX_WINDOW_BIT,
  GLX_RENDER_TYPE     , GLX_RGBA_BIT,
  GLX_RED_SIZE        , 8,
  GLX_GREEN_SIZE      , 8,
  GLX_BLUE_SIZE       , 8,
  GLX_DEPTH_SIZE      , 16,
  GLX_DOUBLEBUFFER    , True,
  GLX_STEREO          , False,
  GLX_TRANSPARENT_TYPE, GLX_NONE,
  //GLX_SAMPLE_BUFFERS  , 1,
  //GLX_SAMPLES         , 4,
  None };

static int context_attribs[] = {
    GLX_CONTEXT_MAJOR_VERSION_ARB, 3,
    GLX_CONTEXT_MINOR_VERSION_ARB, 0,
    GLX_RENDER_TYPE              , GLX_RGBA_TYPE,
    GLX_CONTEXT_FLAGS_ARB        , 0,
    // GLX_CONTEXT_FLAGS_ARB        , GLX_CONTEXT_FORWARD_COMPATIBLE_BIT_ARB,
    // GLX_CONTEXT_PROFILE_MASK_ARB , GLX_CONTEXT_COMPATIBILITY_PROFILE_BIT_ARB,
    None };

static bool isExtensionSupported(const char *extList, const char *extension);

static void createGLWin(Display *dpy, int width, int height, Window *rWin, GLXContext *rCtx)
{
    int glx_major, glx_minor;
 
    // FBConfigs were added in GLX version 1.3.
    if ( !glXQueryVersion( dpy, &glx_major, &glx_minor ) || 
       ( ( glx_major == 1 ) && ( glx_minor < 3 ) ) || ( glx_major < 1 ) )
    {
        printf( "Invalid GLX version" );
        exit(1);
    }

    int fbcount;
    GLXFBConfig *fbc = glXChooseFBConfig( dpy, DefaultScreen( dpy ), 
                                        visual_attribs, &fbcount );
    if ( !fbc || 0 == fbcount )
    {
        printf( "Failed to retrieve a framebuffer config\n" );
        exit(1);
    }
    printf( "Found %d matching FB configs.\n", fbcount );

    GLXFBConfig bestFbc = fbc[ 0 ];
    int bestFbcID = 0;
    if( 0 != glXGetFBConfigAttrib( dpy, bestFbc, GLX_FBCONFIG_ID, &bestFbcID ) ) {
        printf( "Invalid FBConfigID\n" );
        exit(1);
    }
    printf( "Chosen FBConfigID = 0x%x\n", bestFbcID);

    XVisualInfo *vi = glXGetVisualFromFBConfig( dpy, bestFbc );
    printf( "Chosen visual ID = 0x%x\n", (int) vi->visualid );

    XSetWindowAttributes swa;
    Colormap cmap;
    swa.colormap = cmap = XCreateColormap( dpy,
                                         RootWindow( dpy, vi->screen ), 
                                         vi->visual, AllocNone );
    swa.background_pixmap = None ;
    swa.border_pixel      = 0;
    swa.event_mask        = StructureNotifyMask;

    printf( "Creating window\n" );
    Window win = XCreateWindow( dpy, RootWindow( dpy, vi->screen ), 
                                0, 0, width, height, 0, vi->depth, InputOutput,
                                vi->visual, 
                                CWBorderPixel|CWColormap|CWEventMask, &swa );
    if ( !win )
    {
        printf( "Failed to create window.\n" );
        exit(1);
    }

    // Done with the visual info data
    XFree( vi );

    XStoreName( dpy, win, "GL Window" );

    XMapWindow( dpy, win );

    *rWin = win;

    GLXContext ctx0 = glXCreateNewContext( dpy, bestFbc, GLX_RGBA_TYPE, 0, True );
    if( !ctx0 ) {
        printf( "Failed to create intermediate old OpenGL context\n" );
        exit(1);
    }
    glXMakeContextCurrent(dpy, win, win, ctx0);


    // Get the default screen's GLX extension list
    const char *glxExts01 = glXQueryExtensionsString( dpy,
                                                  DefaultScreen( dpy ) );
    const char *glxExts02 = glXGetClientString( dpy, GLX_EXTENSIONS);
    const char *glxExts03 = glXQueryServerString( dpy, DefaultScreen( dpy ), GLX_EXTENSIONS);

    // NOTE: It is not necessary to create or make current to a context before
    // calling glXGetProcAddressARB
    PFNGLXCREATECONTEXTATTRIBSARBPROC _glXCreateContextAttribsARB = 0;
    _glXCreateContextAttribsARB = (PFNGLXCREATECONTEXTATTRIBSARBPROC)
           glXGetProcAddressARB( (const GLubyte *) "glXCreateContextAttribsARB" );

    // Check for the GLX_ARB_create_context extension string and the function.
    // If either is not present, use GLX 1.3 context creation method.
    bool isGLX_ARB_create_contextAvail = isExtensionSupported( glxExts01, "GLX_ARB_create_context" ) || 
                                         isExtensionSupported( glxExts02, "GLX_ARB_create_context" ) ||
                                         isExtensionSupported( glxExts03, "GLX_ARB_create_context" );

    glXMakeContextCurrent(dpy, 0, 0, 0);

    GLXContext ctx = 0;

    // Install an X error handler so the application won't exit if GL 3.0
    // context allocation fails.
    //
    // Note this error handler is global.  All display connections in all threads
    // of a process use the same error handler, so be sure to guard against other
    // threads issuing X commands while this code is running.
    int (*oldHandler)(Display*, XErrorEvent*) =
      XSetErrorHandler(&ctxErrorHandler);

    if ( !isGLX_ARB_create_contextAvail || !_glXCreateContextAttribsARB )
    {
        printf( "glXCreateContextAttribsARB() not found (ext %d, func %p)"
                " ... using old-style GLX context\n", isGLX_ARB_create_contextAvail, _glXCreateContextAttribsARB );
        printf( "extensions 01: %s\n", glxExts01);
        printf( "extensions 02: %s\n", glxExts02);
        printf( "extensions 03: %s\n", glxExts03);
        ctx = ctx0;
    }

    // If it does, try to get a GL 3.0 context!
    else
    {
        printf( "Creating context\n" );
        XSync( dpy, False );
        ctxErrorOccurred = false;
        ctx = _glXCreateContextAttribsARB( dpy, bestFbc, 0, True, context_attribs );
        XSync( dpy, False );

        if ( !ctxErrorOccurred && ctx ) {
          printf( "Created GL 3.0 context\n" );
          glXDestroyContext(dpy, ctx0); // get rid of old ctx
        } else
        {
          // Couldn't create GL 3.0 context.  Fall back to old-style 2.x context.
          // When a context version below 3.0 is requested, implementations will
          // return the newest context version compatible with OpenGL versions less
          // than version 3.0.
          // GLX_CONTEXT_MAJOR_VERSION_ARB = 1
          context_attribs[1] = 1;
          // GLX_CONTEXT_MINOR_VERSION_ARB = 0
          context_attribs[3] = 0;

          printf( "Failed to create GL 3.0 context (err %d, ctx %p)"
                  " ... using old-style GLX context\n", ctxErrorOccurred, (void*)ctx );
          ctx = ctx0;

          ctxErrorOccurred = false;
        }
    }

    // Sync to ensure any errors generated are processed.
    XSync( dpy, False );

    // Restore the original error handler
    XSetErrorHandler( oldHandler );

    if ( ctxErrorOccurred || !ctx )
    {
        printf( "Failed to create an OpenGL context\n" );
        exit(1);
    }

    XFree( fbc );

    *rCtx = ctx;
}

// Helper to check for extension string presence.  Adapted from:
//   http://www.opengl.org/resources/features/OGLextensions/
static bool isExtensionSupported(const char *extList, const char *extension)
{

  const char *start;
  const char *where, *terminator;
  
  /* Extension names should not have spaces. */
  where = strchr(extension, ' ');
  if ( where || *extension == '\0' )
    return false;

  /* It takes a bit of care to be fool-proof about parsing the
     OpenGL extensions string. Don't be fooled by sub-strings,
     etc. */
  for ( start = extList; ; ) {
    where = strstr( start, extension );

    if ( !where )
      break;

    terminator = where + strlen( extension );

    if ( where == start || *(where - 1) == ' ' )
      if ( *terminator == ' ' || *terminator == '\0' )
        return true;

    start = terminator;
  }

  return false;
}

