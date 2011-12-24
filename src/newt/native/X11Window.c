/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

#include "X11Common.h"

#ifdef VERBOSE_ON
    #define DUMP_VISUAL_INFO(a,b) _dumpVisualInfo((a),(b))

    static void _dumpVisualInfo(const char * msg, XVisualInfo *pVisualQuery) {
        if(pVisualQuery!=NULL) {
            fprintf(stderr, "%s: screen %d, visual: %p, visual-id: 0x%X, depth: %d, class %d, cmap sz: %d, bpp: 3x%d, rgb 0x%X 0x%X 0x%X\n",
                msg,
                pVisualQuery->screen,
                pVisualQuery->visual,
                (int)pVisualQuery->visualid,
                pVisualQuery->depth,
                pVisualQuery->class,
                pVisualQuery->colormap_size,
                pVisualQuery->bits_per_rgb,
                (int)pVisualQuery->red_mask,
                (int)pVisualQuery->green_mask,
                (int)pVisualQuery->blue_mask
            );
        } else {
            fprintf(stderr, "%s: NULL XVisualInfo\n", msg);
        }
    }

#else

    #define DUMP_VISUAL_INFO(a,b)

#endif

#define X11_MOUSE_EVENT_MASK (ButtonPressMask | ButtonReleaseMask | PointerMotionMask | EnterWindowMask | LeaveWindowMask)

static int putPtrIn32Long(unsigned long * dst, uintptr_t src) {
    int i=0;
        dst[i++] = (unsigned long) ( ( src >>  0 ) & 0xFFFFFFFF ) ;
    if(sizeof(uintptr_t) == 8) {
        dst[i++] = (unsigned long) ( ( src >> 32 ) & 0xFFFFFFFF ) ;
    }
    return i;
}

static uintptr_t getPtrOut32Long(unsigned long * src) {
    uintptr_t  res = ( (uintptr_t) ( src[0] & 0xFFFFFFFF ) )  <<  0 ;
    if(sizeof(uintptr_t) == 8) {
              res |= ( (uintptr_t) ( src[1] & 0xFFFFFFFF ) )  << 32 ;
    }
    return res;
}

static void setJavaWindowProperty(JNIEnv *env, Display *dpy, Window window, jlong javaObjectAtom, jobject jwindow) {
    unsigned long jogl_java_object_data[2]; // X11 is based on 'unsigned long'
    int nitems_32 = putPtrIn32Long( jogl_java_object_data, (uintptr_t) jwindow);

    {
        jobject test = (jobject) getPtrOut32Long(jogl_java_object_data);
        if( ! (jwindow==test) ) {
            NewtCommon_FatalError(env, "Internal Error .. Encoded Window ref not the same %p != %p !", jwindow, test);
        }
    }

    XChangeProperty( dpy, window, (Atom)javaObjectAtom, (Atom)javaObjectAtom, 32, PropModeReplace, 
                                     (unsigned char *)&jogl_java_object_data, nitems_32);
}

jobject getJavaWindowProperty(JNIEnv *env, Display *dpy, Window window, jlong javaObjectAtom, Bool showWarning) {
    Atom actual_type;
    int actual_format;
    int nitems_32 = ( sizeof(uintptr_t) == 8 ) ? 2 : 1 ;
    unsigned char * jogl_java_object_data_pp = NULL;
    jobject jwindow;

    {
        unsigned long nitems= 0;
        unsigned long bytes_after= 0;
        jobject jwindow = NULL;
        int res;

        res = XGetWindowProperty(dpy, window, (Atom)javaObjectAtom, 0, nitems_32, False, 
                                 (Atom)javaObjectAtom, &actual_type, &actual_format, 
                                 &nitems, &bytes_after, &jogl_java_object_data_pp);

        if ( Success != res ) {
            if(True==showWarning) {
                fprintf(stderr, "Warning: NEWT X11Window: Could not fetch Atom NEWT_JAVA_OBJECT window property (res %d) nitems %ld, bytes_after %ld, result 0!\n", res, nitems, bytes_after);
            }
            return NULL;
        }

        if(actual_type!=(Atom)javaObjectAtom || nitems<nitems_32 || NULL==jogl_java_object_data_pp) {
            XFree(jogl_java_object_data_pp);
            if(True==showWarning) {
                fprintf(stderr, "Warning: NEWT X11Window: Fetched invalid Atom NEWT_JAVA_OBJECT window property (res %d) nitems %ld, bytes_after %ld, actual_type %ld, NEWT_JAVA_OBJECT %ld, result 0!\n", 
                res, nitems, bytes_after, (long)actual_type, (long)javaObjectAtom);
            }
            return NULL;
        }
    }

    jwindow = (jobject) getPtrOut32Long( (unsigned long *) jogl_java_object_data_pp ) ;
    XFree(jogl_java_object_data_pp);

#ifdef VERBOSE_ON
    if(JNI_FALSE == (*env)->IsInstanceOf(env, jwindow, X11NewtWindowClazz)) {
        NewtCommon_throwNewRuntimeException(env, "fetched Atom NEWT_JAVA_OBJECT window is not a NEWT Window: javaWindow 0x%X !", jwindow);
    }
#endif
    return jwindow;
}

/** @return zero if fails, non zero if OK */
Status NewtWindows_getRootAndParent (Display *dpy, Window w, Window * root_return, Window * parent_return) {
    Window *children_return=NULL;
    unsigned int nchildren_return=0;

    Status res = XQueryTree(dpy, w, root_return, parent_return, &children_return, &nchildren_return);
    if(NULL!=children_return) {
        XFree(children_return);
    }
    return res; // 0 == res -> Error
}
static Window NewtWindows_getRoot (Display *dpy, Window w) {
    Window root_return;
    Window parent_return;
    if( 0 != NewtWindows_getRootAndParent(dpy, w, &root_return, &parent_return) ) {
        return root_return;
    }
    return 0; // Error
}
static Window NewtWindows_getParent (Display *dpy, Window w) {
    Window root_return;
    Window parent_return;
    if( 0 != NewtWindows_getRootAndParent(dpy, w, &root_return, &parent_return) ) {
        return parent_return;
    }
    return 0; // Error
}
static Status NewtWindows_getParentPosition (Display *dpy, Window w, int *x_return, int *y_return) {
    Window root_return;
    unsigned int width_return, height_return;
    unsigned int border_width_return;
    unsigned int depth_return;
    Window parent = NewtWindows_getParent(dpy, w);

    if(0 != parent) {
        XGetGeometry(dpy, parent, &root_return, x_return, y_return, &width_return, 
                               &height_return, &border_width_return, &depth_return);
        return 1; // OK
    }
    return 0; // Error
}
static Status NewtWindows_getFrameExtends(Display *dpy, Window window, int *left, int *right, int *top, int *bottom) {
    Atom actual_type;
    int actual_format;
    int nitems_32 = 4; // l, r, t, b
    unsigned char * frame_extends_data_pp = NULL;

    {
        Atom _NET_FRAME_EXTENTS = XInternAtom( dpy, "_NET_FRAME_EXTENTS", False );
        unsigned long nitems = 0;
        unsigned long bytes_after = 0;
        int res;

        res = XGetWindowProperty(dpy, window, _NET_FRAME_EXTENTS, 0, nitems_32, False, 
                                 AnyPropertyType, &actual_type, &actual_format, 
                                 &nitems, &bytes_after, &frame_extends_data_pp);

        if ( Success != res ) {
            fprintf(stderr, "Error: NEWT X11Window: Could not fetch Atom _NET_FRAME_EXTENTS window property (res %d) nitems %ld, bytes_after %ld, result 0!\n", res, nitems, bytes_after);
            return 0; // Error
        }

        if(nitems<nitems_32 || NULL==frame_extends_data_pp) {
            XFree(frame_extends_data_pp);
            // DBG_PRINT( "Warning: NEWT X11Window: Fetched invalid Atom _NET_FRAME_EXTENTS window property (res %d) nitems %ld, bytes_after %ld, actual_type %ld, actual_format %d, _NET_FRAME_EXTENTS %ld, result 0!\n", 
            //     res, nitems, bytes_after, (long)actual_type, actual_format, _NET_FRAME_EXTENTS);
            return 0; // Error, but ok - ie window not mapped
        }
    }
    long * extends = (long*) frame_extends_data_pp;
    *left = (int) *(extends + 0);
    *right = (int) *(extends + 1);
    *top = (int) *(extends + 2);
    *bottom = (int) *(extends + 3);

    // DBG_PRINT( "X11: _NET_FRAME_EXTENTS: window %p insets [ l %d, r %d, t %d, b %d ]\n",
    //     (void*)window, *left, *right, *top, *bottom);
        
    XFree(frame_extends_data_pp);

    return 1; // Ok
}
Status NewtWindows_updateInsets(JNIEnv *env, jobject jwindow, Display *dpy, Window window, int *left, int *right, int *top, int *bottom) {
    if(0 != NewtWindows_getFrameExtends(dpy, window, left, right, top, bottom)) {
        DBG_PRINT( "NewtWindows_updateInsets: insets by _NET_FRAME_EXTENTS [ l %d, r %d, t %d, b %d ]\n",
            *left, *right, *top, *bottom);
        (*env)->CallVoidMethod(env, jwindow, insetsChangedID, JNI_FALSE, *left, *right, *top, *bottom);
        return 1; // OK
    } else if(0 != NewtWindows_getParentPosition (dpy, window, left, top)) {
        *right = *left; *bottom = *left;
        DBG_PRINT( "NewtWindows_updateInsets: insets by parent position [ l %d, r %d, t %d, b %d ]\n",
            *left, *right, *top, *bottom);
        (*env)->CallVoidMethod(env, jwindow, insetsChangedID, JNI_FALSE, *left, *right, *top, *bottom);
        return 1; // OK
    }
    return 0; // Error
}

static void NewtWindows_setCWAbove(Display *dpy, Window w) {
    XWindowChanges xwc;
    memset(&xwc, 0, sizeof(XWindowChanges));
    xwc.stack_mode = Above;
    XConfigureWindow(dpy, w, CWStackMode, &xwc);
    XSync(dpy, False);
}

static void NewtWindows_requestFocus (JNIEnv *env, jobject window, Display *dpy, Window w, jboolean force) {
    XWindowAttributes xwa;
    Window focus_return;
    int revert_to_return;

    XGetInputFocus(dpy, &focus_return, &revert_to_return);
    DBG_PRINT( "X11: requestFocus dpy %p,win %p, force %d, hasFocus %d\n", dpy, (void*)w, force, focus_return==w);

    if( JNI_TRUE==force || focus_return!=w) {
        DBG_PRINT( "X11: XRaiseWindow dpy %p, win %p\n", dpy, (void*)w);
        XRaiseWindow(dpy, w);
        NewtWindows_setCWAbove(dpy, w);
        // Avoid 'BadMatch' errors from XSetInputFocus, ie if window is not viewable
        XGetWindowAttributes(dpy, w, &xwa);
        if(xwa.map_state == IsViewable) {
            DBG_PRINT( "X11: XSetInputFocus dpy %p,win %pd\n", dpy, (void*)w);
            XSetInputFocus(dpy, w, RevertToParent, CurrentTime);
        }
    }
    DBG_PRINT( "X11: requestFocus dpy %p,win %p, force %d - FIN\n", dpy, (void*)w, force);
    XSync(dpy, False);
}

#define DECOR_USE_MWM 1     // works for known WMs
// #define DECOR_USE_EWMH 1 // haven't seen this to work (NORMAL->POPUP, never gets undecorated)

/* see <http://tonyobryan.com/index.php?article=9> */
#define MWM_HINTS_DECORATIONS   (1L << 1)
#define PROP_MWM_HINTS_ELEMENTS 5

static void NewtWindows_setDecorations (Display *dpy, Window w, Bool decorated) {

#ifdef DECOR_USE_MWM
    unsigned long mwmhints[PROP_MWM_HINTS_ELEMENTS] = { MWM_HINTS_DECORATIONS, 0, decorated, 0, 0 }; // flags, functions, decorations, input_mode, status
    Atom _MOTIF_WM_HINTS = XInternAtom( dpy, "_MOTIF_WM_HINTS", False );
#endif

#ifdef DECOR_USE_EWMH
    Atom _NET_WM_WINDOW_TYPE = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE", False );
    Atom types[3]={0};
    int ntypes=0;
    if(True==decorated) {
        types[ntypes++] = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE_NORMAL", False );
    } else {
        types[ntypes++] = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE_POPUP_MENU", False );
    }
#endif

#ifdef DECOR_USE_MWM
    XChangeProperty( dpy, w, _MOTIF_WM_HINTS, _MOTIF_WM_HINTS, 32, PropModeReplace, (unsigned char *)&mwmhints, PROP_MWM_HINTS_ELEMENTS);
#endif

#ifdef DECOR_USE_EWMH
    XChangeProperty( dpy, w, _NET_WM_WINDOW_TYPE, XA_ATOM, 32, PropModeReplace, (unsigned char *)&types, ntypes);
#endif

    XSync(dpy, False);
}

static void NewtWindows_setNormalWindowEWMH (Display *dpy, Window w) {
    Atom _NET_WM_WINDOW_TYPE = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE", False );
    Atom types[1]={0};
    types[0] = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE_NORMAL", False );
    XChangeProperty( dpy, w, _NET_WM_WINDOW_TYPE, XA_ATOM, 32, PropModeReplace, (unsigned char *)&types, 1);
    XSync(dpy, False);
}

#define _NET_WM_STATE_REMOVE 0
#define _NET_WM_STATE_ADD 1

#define _NET_WM_FULLSCREEN ( 1 << 0 )
#define _NET_WM_ABOVE      ( 1 << 1 )

/**
 * Set fullscreen using Extended Window Manager Hints (EWMH)
 *
 * Fullscreen on:
 *   Be aware that _NET_WM_STATE_FULLSCREEN requires a mapped window
 *   which shall be on the top of the stack to work reliable.
 *
 * The WM will internally save the size and position when entering FS
 * and resets it when leaving FS.
 * The same is assumed for the decoration state.
 */
static int NewtWindows_isFullscreenEWMHSupported (Display *dpy, Window w) {
    Atom _NET_WM_ALLOWED_ACTIONS = XInternAtom( dpy, "_NET_WM_ALLOWED_ACTIONS", False );
    Atom _NET_WM_ACTION_FULLSCREEN = XInternAtom( dpy, "_NET_WM_ACTION_FULLSCREEN", False );
    Atom _NET_WM_ACTION_ABOVE = XInternAtom( dpy, "_NET_WM_ACTION_ABOVE", False );
    Atom * actions;
    Atom type;
    unsigned long action_len, remain;
    int res = 0, form, i;
    Status s;

    if ( Success == (s = XGetWindowProperty(dpy, w, _NET_WM_ALLOWED_ACTIONS, 0, 1024, False, AnyPropertyType,
                                            &type, &form, &action_len, &remain, (unsigned char**)&actions)) ) {
        for(i=0; i<action_len; i++) {
            if(_NET_WM_ACTION_FULLSCREEN == actions[i]) {
                DBG_PRINT( "**************** X11: FS EWMH CHECK[%d]: _NET_WM_ACTION_FULLSCREEN (*)\n", i);
                res |= _NET_WM_FULLSCREEN ;
            } else if(_NET_WM_ACTION_ABOVE == actions[i]) {
                DBG_PRINT( "**************** X11: FS EWMH CHECK[%d]: _NET_WM_ACTION_ABOVE (*)\n", i);
                res |= _NET_WM_ABOVE ;
            }
#ifdef VERBOSE_ON
            else {
                char * astr = XGetAtomName(dpy, actions[i]);
                DBG_PRINT( "**************** X11: FS EWMH CHECK[%d]: %s (unused)\n", i, astr);
                XFree(astr);
            }
#endif
        }
        DBG_PRINT( "**************** X11: FS EWMH CHECK: 0x%X\n", res);
    } else {
        DBG_PRINT( "**************** X11: FS EWMH CHECK: XGetWindowProperty failed: %d\n", s);
    }
    // above code doesn't work reliable on KDE4 ...
    res = _NET_WM_FULLSCREEN | _NET_WM_ABOVE ;
    return res;
}

static Bool NewtWindows_setFullscreenEWMH (Display *dpy, Window root, Window w, int ewmhFlags, Bool isVisible, Bool enable) {
    Atom _NET_WM_STATE = XInternAtom( dpy, "_NET_WM_STATE", False );
    Atom _NET_WM_STATE_ABOVE = XInternAtom( dpy, "_NET_WM_STATE_ABOVE", False );
    Atom _NET_WM_STATE_FULLSCREEN = XInternAtom( dpy, "_NET_WM_STATE_FULLSCREEN", False );
    int ewmhMask = NewtWindows_isFullscreenEWMHSupported(dpy, w);
    Bool res = False;

    if(0 == ewmhMask) { 
        return res;
    }

    if(!isVisible && True==enable) {
        Atom types[2]={0};
        int ntypes=0;

        if( 0 != ( ( _NET_WM_FULLSCREEN & ewmhMask ) & ewmhFlags ) )  {
            types[ntypes++] = _NET_WM_STATE_FULLSCREEN;
        }
        if( 0 != ( ( _NET_WM_ABOVE & ewmhMask ) & ewmhFlags ) )  {
            types[ntypes++] = _NET_WM_STATE_ABOVE;
        }
        if(ntypes>0) {
            XChangeProperty( dpy, w, _NET_WM_STATE, XA_ATOM, 32, PropModeReplace, (unsigned char *)&types, ntypes);
            XSync(dpy, False);
            res = True;
        }
    } else {
        if(enable) {
            NewtWindows_setCWAbove(dpy, w);
        }
        XEvent xev;
        long mask = SubstructureNotifyMask | SubstructureRedirectMask ;
        int i=0;
        
        memset ( &xev, 0, sizeof(xev) );
        
        xev.type = ClientMessage;
        xev.xclient.window = w;
        xev.xclient.message_type = _NET_WM_STATE;
        xev.xclient.format = 32;
            
        xev.xclient.data.l[i++] = ( True == enable ) ? _NET_WM_STATE_ADD : _NET_WM_STATE_REMOVE ;
        if( 0 != ( ( _NET_WM_FULLSCREEN & ewmhMask ) & ewmhFlags ) )  {
            xev.xclient.data.l[i++] = _NET_WM_STATE_FULLSCREEN;
        }
        if( 0 != ( ( _NET_WM_ABOVE & ewmhMask ) & ewmhFlags ) )  {
            xev.xclient.data.l[i++] = _NET_WM_STATE_ABOVE;
        }
        xev.xclient.data.l[3] = 1; //source indication for normal applications

        if(i>0) {
            XSendEvent ( dpy, root, False, mask, &xev );
            res = True;
        }
    }
    XSync(dpy, False);
    DBG_PRINT( "X11: reconfigureWindow0 FULLSCREEN EWMH ON %d, ewmhMask 0x%X, ewmhFlags 0x%X, visible %d: %d\n", 
        enable, ewmhMask, ewmhFlags, isVisible, res);
    return res;
}

/**
 * Window
 */

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_X11Window_initIDs0
  (JNIEnv *env, jclass clazz)
{
    return JNI_TRUE;
}

static Bool WaitForMapNotify( Display *dpy, XEvent *event, XPointer arg ) {
    return (event->type == MapNotify) && (event->xmap.window == (Window) arg);
}

static Bool WaitForUnmapNotify( Display *dpy, XEvent *event, XPointer arg ) {
    return (event->type == UnmapNotify) && (event->xmap.window == (Window) arg);
}

static void NewtWindows_setPosSize(Display *dpy, Window w, jint x, jint y, jint width, jint height) {
    if(width>0 && height>0 || x>=0 && y>=0) { // resize/position if requested
        XWindowChanges xwc;
        int flags = CWX | CWY;

        memset(&xwc, 0, sizeof(XWindowChanges));
        xwc.x=x;
        xwc.y=y;

        if(0<width && 0<height) {
            flags |= CWWidth | CWHeight;
            xwc.width=width;
            xwc.height=height;
        }
        XConfigureWindow(dpy, w, flags, &xwc);
        XSync(dpy, False);
    }
}

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    CreateWindow
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_X11Window_CreateWindow0
  (JNIEnv *env, jobject obj, jlong parent, jlong display, jint screen_index, 
                             jlong visualID, 
                             jlong javaObjectAtom, jlong windowDeleteAtom, 
                             jint x, jint y, jint width, jint height, jboolean autoPosition, int flags)
{
    Display * dpy = (Display *)(intptr_t)display;
    Atom wm_delete_atom = (Atom)windowDeleteAtom;
    int       scrn_idx = (int)screen_index;
    Window root = RootWindow(dpy, scrn_idx);
    Window  windowParent = (Window) parent;
    Window  window = 0;
    jobject jwindow = 0;

    XVisualInfo visualTemplate;
    XVisualInfo *pVisualQuery = NULL;
    Visual *visual = NULL;
    int depth;

    XSetWindowAttributes xswa;
    unsigned long attrMask;
    int n;

    Screen* scrn;

    if(dpy==NULL) {
        NewtCommon_FatalError(env, "invalid display connection..");
    }

    if(visualID<0) {
        NewtCommon_throwNewRuntimeException(env, "invalid VisualID ..");
        return 0;
    }

    XSync(dpy, False);

    scrn = ScreenOfDisplay(dpy, scrn_idx);
    if(0==windowParent) {
        windowParent = root;
    }
    DBG_PRINT( "X11: CreateWindow dpy %p, parent %p, %d/%d %dx%d, undeco %d, alwaysOnTop %d, autoPosition %d\n", 
        (void*)dpy, (void*)windowParent, x, y, width, height,
        TST_FLAG_IS_UNDECORATED(flags), TST_FLAG_IS_ALWAYSONTOP(flags), autoPosition);

    // try given VisualID on screen
    memset(&visualTemplate, 0, sizeof(XVisualInfo));
    visualTemplate.screen = scrn_idx;
    visualTemplate.visualid = (VisualID)visualID;
    pVisualQuery = XGetVisualInfo(dpy, VisualIDMask|VisualScreenMask, &visualTemplate,&n);
    DUMP_VISUAL_INFO("Given VisualID,ScreenIdx", pVisualQuery);
    if(pVisualQuery!=NULL) {
        visual   = pVisualQuery->visual;
        depth    = pVisualQuery->depth;
        visualID = (jlong)pVisualQuery->visualid;
        XFree(pVisualQuery);
        pVisualQuery=NULL;
    }
    DBG_PRINT( "X11: [CreateWindow] trying given (dpy %p, screen %d, visualID: %d, parent %p) found: %p\n", 
        dpy, scrn_idx, (int)visualID, (void*)windowParent, visual);

    if (visual==NULL)
    { 
        NewtCommon_throwNewRuntimeException(env, "could not query Visual by given VisualID, bail out!");
        return 0;
    } 

    if(pVisualQuery!=NULL) {
        XFree(pVisualQuery);
        pVisualQuery=NULL;
    }

    attrMask  = ( CWBackingStore | CWBackingPlanes | CWBackingPixel | CWBackPixmap |
                  CWBorderPixel | CWColormap | CWOverrideRedirect | CWEventMask ) ;

    memset(&xswa, 0, sizeof(xswa));
    xswa.override_redirect = False; // use the window manager, always (default)
    xswa.border_pixel = 0;
    xswa.background_pixmap = None;
    xswa.backing_store=NotUseful;  /* NotUseful, WhenMapped, Always */
    xswa.backing_planes=0;         /* planes to be preserved if possible */
    xswa.backing_pixel=0;          /* value to use in restoring planes */
    xswa.event_mask  = X11_MOUSE_EVENT_MASK;
    xswa.event_mask |= KeyPressMask | KeyReleaseMask ;
    xswa.event_mask |= FocusChangeMask | SubstructureNotifyMask | StructureNotifyMask | ExposureMask ;

    xswa.colormap = XCreateColormap(dpy,
                                    windowParent,
                                    visual,
                                    AllocNone);

    {
        int _x = x, _y = y; // pos for CreateWindow, might be tweaked
        if(JNI_TRUE == autoPosition) {
            // user didn't requested specific position, use WM default
            _x = 0;
            _y = 0;
        }
        window = XCreateWindow(dpy,
                               windowParent,
                               _x, _y, // only a hint, WM most likely will override
                               width, height,
                               0, // border width
                               depth,
                               InputOutput,
                               visual,
                               attrMask,
                               &xswa);
    }

    if(0==window) {
        NewtCommon_throwNewRuntimeException(env, "could not create Window, bail out!");
        return 0;
    }

    XSetWMProtocols(dpy, window, &wm_delete_atom, 1); // windowDeleteAtom
    jwindow = (*env)->NewGlobalRef(env, obj);
    setJavaWindowProperty(env, dpy, window, javaObjectAtom, jwindow);

    NewtWindows_setNormalWindowEWMH(dpy, window);
    NewtWindows_setDecorations(dpy, window, TST_FLAG_IS_UNDECORATED(flags) ? False : True );

    // since native creation happens at setVisible(true) .. 
    // we can pre-map the window here to be able to gather the insets and position.
    {
        XEvent event;
        int left, right, top, bottom;

        XMapWindow(dpy, window);
        XIfEvent( dpy, &event, WaitForMapNotify, (XPointer) window ); // wait to get proper insets values

        // send insets before visibility, allowing java code a proper sync point!
        NewtWindows_updateInsets(env, jwindow, dpy, window, &left, &right, &top, &bottom);
        (*env)->CallVoidMethod(env, jwindow, visibleChangedID, JNI_FALSE, JNI_TRUE);

        if(JNI_TRUE == autoPosition) {
            // get position from WM
            int dest_x, dest_y;
            Window child;
            XTranslateCoordinates(dpy, window, windowParent, 0, 0, &dest_x, &dest_y, &child);
            x = (int)dest_x; y = (int)dest_y;
        }
        DBG_PRINT("X11: [CreateWindow]: client: %d/%d %dx%d (autoPosition %d)\n", x, y, width, height, autoPosition);

        x -= left; // top-level
        y -= top;  // top-level
        DBG_PRINT("X11: [CreateWindow]: top-level: %d/%d\n", x, y);
        NewtWindows_setPosSize(dpy, window, x, y, width, height);

        if( TST_FLAG_IS_ALWAYSONTOP(flags) ) {
            NewtWindows_setFullscreenEWMH(dpy, root, window, _NET_WM_ABOVE, True, True);
        }
    }

    DBG_PRINT( "X11: [CreateWindow] created window %p on display %p\n", (void*)window, dpy);
    return (jlong) window;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    CloseWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_CloseWindow0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jlong javaObjectAtom, jlong windowDeleteAtom)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    jobject jwindow;

    if(dpy==NULL) {
        NewtCommon_FatalError(env, "invalid display connection..");
    }

    DBG_PRINT( "X11: CloseWindow START dpy %p, win %p\n", (void*)dpy, (void*)w);

    jwindow = getJavaWindowProperty(env, dpy, w, javaObjectAtom, True);
    if(NULL==jwindow) {
        NewtCommon_throwNewRuntimeException(env, "could not fetch Java Window object, bail out!");
        return;
    }
    if ( JNI_FALSE == (*env)->IsSameObject(env, jwindow, obj) ) {
        NewtCommon_throwNewRuntimeException(env, "Internal Error .. Window global ref not the same!");
        return;
    }

    XSync(dpy, False);
    XSelectInput(dpy, w, 0);
    XUnmapWindow(dpy, w);
    XSync(dpy, False);

    // Drain all events related to this window ..
    Java_jogamp_newt_driver_x11_X11Display_DispatchMessages0(env, obj, display, javaObjectAtom, windowDeleteAtom);

    XDestroyWindow(dpy, w);
    XSync(dpy, False);

    (*env)->DeleteGlobalRef(env, jwindow);

    DBG_PRINT( "X11: CloseWindow END\n");
}

#if 0
static Bool WaitForReparentNotify( Display *dpy, XEvent *event, XPointer arg ) {
    return (event->type == ReparentNotify) && (event->xreparent.window == (Window) arg);
}
#endif

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    reconfigureWindow0
 * Signature: (JIJJIIIII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_reconfigureWindow0
  (JNIEnv *env, jobject obj, jlong jdisplay, jint screen_index, jlong jparent, jlong jwindow, 
   jint x, jint y, jint width, jint height, jint flags)
{
    Display * dpy = (Display *) (intptr_t) jdisplay;
    Window w = (Window)jwindow;
    Window root = RootWindow(dpy, screen_index);
    Window parent = (0!=jparent)?(Window)jparent:root;
    XEvent event;
    Bool isVisible = !TST_FLAG_CHANGE_VISIBILITY(flags) && TST_FLAG_IS_VISIBLE(flags) ;
    Bool tempInvisible = ( TST_FLAG_CHANGE_FULLSCREEN(flags) || TST_FLAG_CHANGE_PARENTING(flags) ) && isVisible ;
    int fsEWMHFlags = 0;
    if( TST_FLAG_CHANGE_FULLSCREEN(flags) ) {
        fsEWMHFlags |= _NET_WM_FULLSCREEN;
        if( TST_FLAG_IS_FULLSCREEN(flags) ) {
            fsEWMHFlags |= _NET_WM_ABOVE; // fs & above on
        } else if( !TST_FLAG_IS_ALWAYSONTOP(flags) ) {
            fsEWMHFlags |= _NET_WM_ABOVE; // fs & above off
        } /* else { } */                  // fs off, keep above
    } else if( TST_FLAG_CHANGE_ALWAYSONTOP(flags) ) {
        fsEWMHFlags |= _NET_WM_ABOVE; // toggle above only
    }

    NewtDisplay_displayDispatchErrorHandlerEnable(1, env);

    DBG_PRINT( "X11: reconfigureWindow0 dpy %p, scrn %d, parent %p/%p, win %p, %d/%d %dx%d, parentChange %d, hasParent %d, decorationChange %d, undecorated %d, fullscreenChange %d, fullscreen %d, alwaysOnTopChange %d, alwaysOnTop %d, visibleChange %d, visible %d, tempInvisible %d, fsEWMHFlags %d\n",
        (void*)dpy, screen_index, (void*) jparent, (void*)parent, (void*)w,
        x, y, width, height, 
        TST_FLAG_CHANGE_PARENTING(flags),   TST_FLAG_HAS_PARENT(flags),
        TST_FLAG_CHANGE_DECORATION(flags),  TST_FLAG_IS_UNDECORATED(flags),
        TST_FLAG_CHANGE_FULLSCREEN(flags),  TST_FLAG_IS_FULLSCREEN(flags),
        TST_FLAG_CHANGE_ALWAYSONTOP(flags), TST_FLAG_IS_ALWAYSONTOP(flags),
        TST_FLAG_CHANGE_VISIBILITY(flags),  TST_FLAG_IS_VISIBLE(flags), tempInvisible, fsEWMHFlags);

    // FS Note: To toggle FS, utilizing the _NET_WM_STATE_FULLSCREEN WM state shall be enough.
    //          However, we have to consider other cases like reparenting and WM which don't support it.

    if( fsEWMHFlags && !TST_FLAG_CHANGE_PARENTING(flags) && isVisible &&
        ( TST_FLAG_CHANGE_FULLSCREEN(flags) || TST_FLAG_CHANGE_ALWAYSONTOP(flags) ) ) {
        Bool enable = TST_FLAG_CHANGE_FULLSCREEN(flags) ? TST_FLAG_IS_FULLSCREEN(flags) : TST_FLAG_IS_ALWAYSONTOP(flags) ;
        if( NewtWindows_setFullscreenEWMH(dpy, root, w, fsEWMHFlags, isVisible, enable) ) {
            NewtDisplay_displayDispatchErrorHandlerEnable(0, env);
            return;
        }
    }

    if( tempInvisible ) {
        DBG_PRINT( "X11: reconfigureWindow0 TEMP VISIBLE OFF\n");
        XUnmapWindow(dpy, w);
        XIfEvent( dpy, &event, WaitForUnmapNotify, (XPointer) w );
        // no need to notify the java side .. just temp change
    }

    if( fsEWMHFlags && ( ( TST_FLAG_CHANGE_FULLSCREEN(flags)  && !TST_FLAG_IS_FULLSCREEN(flags) ) || 
                         ( TST_FLAG_CHANGE_ALWAYSONTOP(flags) && !TST_FLAG_IS_ALWAYSONTOP(flags) ) ) ) { // FS off
        NewtWindows_setFullscreenEWMH(dpy, root, w, fsEWMHFlags, isVisible, False);
    }

    if( TST_FLAG_CHANGE_PARENTING(flags) && !TST_FLAG_HAS_PARENT(flags) ) {
        // TOP: in -> out
        DBG_PRINT( "X11: reconfigureWindow0 PARENTING in->out\n");
        XReparentWindow( dpy, w, parent, x, y ); // actual reparent call
        // XIfEvent( dpy, &event, WaitForReparentNotify, (XPointer) w );
        XSync(dpy, False);
    }

    if( TST_FLAG_CHANGE_DECORATION(flags) ) {
        DBG_PRINT( "X11: reconfigureWindow0 DECORATIONS %d\n", !TST_FLAG_IS_UNDECORATED(flags));
        NewtWindows_setDecorations (dpy, w, TST_FLAG_IS_UNDECORATED(flags) ? False : True);
    }

    DBG_PRINT( "X11: reconfigureWindow0 setPosSize %d/%d %dx%d\n", x, y, width, height);
    NewtWindows_setPosSize(dpy, w, x, y, width, height);

    if( TST_FLAG_CHANGE_PARENTING(flags) && TST_FLAG_HAS_PARENT(flags) ) {
        // CHILD: out -> in
        DBG_PRINT( "X11: reconfigureWindow0 PARENTING out->in\n");
        XReparentWindow( dpy, w, parent, x, y ); // actual reparent call
        // XIfEvent( dpy, &event, WaitForReparentNotify, (XPointer) w );
        XSync(dpy, False);
    }

    if( tempInvisible ) {
        DBG_PRINT( "X11: reconfigureWindow0 TEMP VISIBLE ON\n");
        XMapRaised(dpy, w);
        XIfEvent( dpy, &event, WaitForMapNotify, (XPointer) w );
        // no need to notify the java side .. just temp change
    }

    if( TST_FLAG_CHANGE_VISIBILITY(flags) ) {
        if( TST_FLAG_IS_VISIBLE(flags) ) {
            DBG_PRINT( "X11: reconfigureWindow0 VISIBLE ON\n");
            XMapRaised(dpy, w);
        } else {
            DBG_PRINT( "X11: reconfigureWindow0 VISIBLE OFF\n");
            XUnmapWindow(dpy, w);
        }
        XSync(dpy, False);
    }

    if( fsEWMHFlags && ( ( TST_FLAG_CHANGE_FULLSCREEN(flags)  && TST_FLAG_IS_FULLSCREEN(flags) ) || 
                         ( TST_FLAG_CHANGE_ALWAYSONTOP(flags) && TST_FLAG_IS_ALWAYSONTOP(flags) ) ) ) { // FS on
        NewtWindows_setFullscreenEWMH(dpy, root, w, fsEWMHFlags, isVisible, True);
    }

    NewtDisplay_displayDispatchErrorHandlerEnable(0, env);

    DBG_PRINT( "X11: reconfigureWindow0 X\n");
}

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    requestFocus0
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_requestFocus0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jboolean force)
{
    NewtWindows_requestFocus ( env, obj, (Display *) (intptr_t) display, (Window)window, force ) ;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    getParentWindow0
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_X11Window_getParentWindow0
  (JNIEnv *env, jclass clazz, jlong display, jlong window)
{
    return (jlong) NewtWindows_getParent ((Display *) (intptr_t) display, (Window)window);
}

/*
 * Class:     Java_jogamp_newt_driver_x11_X11Window
 * Method:    setTitle0
 * Signature: (JJLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_setTitle0
  (JNIEnv *env, jclass clazz, jlong display, jlong window, jstring title)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;

#if 1
    const char* title_str;
    if (NULL != title) {
        title_str = (*env)->GetStringUTFChars(env, title, NULL);
        if(NULL != title_str) {
            DBG_PRINT( "X11: setTitle: <%s> SET\n", title_str);
            XStoreName(dpy, w, title_str);
            (*env)->ReleaseStringUTFChars(env, title, title_str);
        } else {
            DBG_PRINT( "X11: setTitle: NULL - NOT SET (1)\n");
        }
    } else {
        DBG_PRINT( "X11: setTitle: NULL TITLE\n");
    }
#else
    char *str_list[] = { NULL };
    XTextProperty text_prop;
    if (NULL != title) {
        str_list[0] = (char *) NewtCommon_GetNullTerminatedStringChars(env, title);
        if (str_list[0] != NULL) {
            memset(&text_prop, 0, sizeof(XTextProperty));
            if ( Success != XmbTextListToTextProperty(dpy, str_list, 1, XStringStyle, &text_prop) ) {
                DBG_PRINT( "X11: setTitle.XmbTextListToTextProperty not completly successfull\n");
                fprintf(stderr, "X11: setTitle.XmbTextListToTextProperty not completly successfull\n");
            }
            if(NULL!=text_prop.value) {
                DBG_PRINT( "X11: setTitle: <%s> SET\n", str_list[0]);
                XSetWMName(dpy, w, &text_prop);
                XFree(text_prop.value);
            } else {
                DBG_PRINT( "X11: setTitle: <%s> NOT SET (1)\n", str_list[0]);
            }
            free(str_list[0]);
        } else {
            DBG_PRINT( "X11: setTitle: NULL\n");
        }
    }
#endif
}

/*
 * Class:     Java_jogamp_newt_driver_x11_X11Window
 * Method:    setPointerVisible0
 * Signature: (JJZ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_X11Window_setPointerVisible0
  (JNIEnv *env, jclass clazz, jlong display, jlong window, jboolean mouseVisible)
{
    static char noData[] = { 0,0,0,0,0,0,0,0 };
    static XColor black = { 0 };

    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;

    DBG_PRINT( "X11: setPointerVisible0: %d\n", mouseVisible);

    if(JNI_TRUE == mouseVisible) {
        XUndefineCursor(dpy, w);
    } else {
        Pixmap bitmapNoData;
        Cursor invisibleCursor;

        bitmapNoData = XCreateBitmapFromData(dpy, w, noData, 8, 8);
        if(None == bitmapNoData) {
            return JNI_FALSE;
        }
        invisibleCursor = XCreatePixmapCursor(dpy, bitmapNoData, bitmapNoData, &black, &black, 0, 0);
        XDefineCursor(dpy, w, invisibleCursor);
        XFreeCursor(dpy, invisibleCursor);
        XFreePixmap(dpy, bitmapNoData);
    }
    return JNI_TRUE;
}

/*
 * Class:     Java_jogamp_newt_driver_x11_X11Window
 * Method:    confinePointer0
 * Signature: (JJZ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_X11Window_confinePointer0
  (JNIEnv *env, jclass clazz, jlong display, jlong window, jboolean confine)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;

    DBG_PRINT( "X11: confinePointer0: %d\n", confine);

    if(JNI_TRUE == confine) {
        return GrabSuccess == XGrabPointer(dpy, w, True, 
                                           X11_MOUSE_EVENT_MASK,
                                           GrabModeAsync, GrabModeAsync, w, None, CurrentTime)
               ? JNI_TRUE : JNI_FALSE ;
    }
    XUngrabPointer(dpy, CurrentTime);
    return JNI_TRUE;
}

/*
 * Class:     Java_jogamp_newt_driver_x11_X11Window
 * Method:    warpPointer0
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_warpPointer0
  (JNIEnv *env, jclass clazz, jlong display, jlong window, jint x, jint y)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;

    DBG_PRINT( "X11: warpPointer0: %d/%d\n", x, y);

    XWarpPointer(dpy, None, w, 0, 0, 0, 0, x, y);
}

