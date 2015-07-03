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
    Atom actual_type = 0;
    int actual_format = 0;
    int nitems_32 = ( sizeof(uintptr_t) == 8 ) ? 2 : 1 ;
    unsigned char * jogl_java_object_data_pp = NULL;
    jobject jwindow = 0;

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
            if( NULL != jogl_java_object_data_pp ) {
                XFree(jogl_java_object_data_pp);
            }
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
static void NewtWindows_setCWAbove(Display *dpy, Window w) {
    XWindowChanges xwc;
    memset(&xwc, 0, sizeof(XWindowChanges));
    xwc.stack_mode = Above;
    XConfigureWindow(dpy, w, CWStackMode, &xwc);
    XSync(dpy, False);
}
static Status NewtWindows_getWindowPositionRelative2Parent (Display *dpy, Window w, int *x_return, int *y_return) {
    Window root_return;
    unsigned int width_return, height_return;
    unsigned int border_width_return;
    unsigned int depth_return;

    if(0 !=  XGetGeometry(dpy, w, &root_return, x_return, y_return, &width_return, 
                                  &height_return, &border_width_return, &depth_return)) {
        return 1; // OK
    }
    return 0; // Error
}
static Status NewtWindows_getFrameExtends(Display *dpy, Window window, int *left, int *right, int *top, int *bottom) {
    Atom actual_type = 0;
    int actual_format = 0;
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
            if( NULL != frame_extends_data_pp ) {
                XFree(frame_extends_data_pp);
            }
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

static Bool NewtWindows_hasDecorations (Display *dpy, Window w) {
    Bool decor = False;

#ifdef DECOR_USE_MWM
    Atom _MOTIF_WM_HINTS = XInternAtom( dpy, "_MOTIF_WM_HINTS", False );
    unsigned char *wm_data = NULL;
    Atom wm_type = 0;
    int wm_format = 0;
    unsigned long wm_nitems = 0, wm_bytes_after = 0;
 
    if( Success == XGetWindowProperty(dpy, w, _MOTIF_WM_HINTS, 0, PROP_MWM_HINTS_ELEMENTS, False, AnyPropertyType, 
                                      &wm_type, &wm_format, &wm_nitems, &wm_bytes_after, &wm_data) ) {
        if(wm_type != None && NULL != wm_data && wm_nitems >= PROP_MWM_HINTS_ELEMENTS) {
            // unsigned long mwmhints[PROP_MWM_HINTS_ELEMENTS] = { MWM_HINTS_DECORATIONS, 0, decorated, 0, 0 }; // flags, functions, decorations, input_mode, status
            unsigned long *hints = (unsigned long *) wm_data;
            decor = ( 0 != (hints[0] & MWM_HINTS_DECORATIONS) ) && ( 0 != hints[2] );
        }
        if( NULL != wm_data ) {
            XFree(wm_data);
        }
    }
#endif

    return decor;
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
#define _NET_WM_STATE_FLAG_FULLSCREEN        ( 1 << 0 )
#define _NET_WM_STATE_FLAG_ABOVE             ( 1 << 1 )

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
static int NewtWindows_getSupportedStackingEWMHFlags(Display *dpy, Window w) {
#ifdef VERBOSE_ON
    // Code doesn't work reliable on KDE4 ...
    Atom _NET_WM_ALLOWED_ACTIONS = XInternAtom( dpy, "_NET_WM_ALLOWED_ACTIONS", False );
    Atom _NET_WM_ACTION_FULLSCREEN = XInternAtom( dpy, "_NET_WM_ACTION_FULLSCREEN", False );
    Atom _NET_WM_ACTION_ABOVE = XInternAtom( dpy, "_NET_WM_ACTION_ABOVE", False );
    Atom * actions = NULL;
    Atom type = 0;
    unsigned long action_len = 0, remain = 0;
    int res = 0, form = 0, i = 0;
    Status s;

    if ( Success == (s = XGetWindowProperty(dpy, w, _NET_WM_ALLOWED_ACTIONS, 0, 1024, False, AnyPropertyType,
                                            &type, &form, &action_len, &remain, (unsigned char**)&actions)) ) {
        if( NULL != actions ) {
            for(i=0; i<action_len; i++) {
                if(_NET_WM_ACTION_FULLSCREEN == actions[i]) {
                    DBG_PRINT( "**************** X11: FS EWMH CHECK[%d]: _NET_WM_ACTION_FULLSCREEN (*)\n", i);
                    res |= _NET_WM_STATE_FLAG_FULLSCREEN ;
                } else if(_NET_WM_ACTION_ABOVE == actions[i]) {
                    DBG_PRINT( "**************** X11: FS EWMH CHECK[%d]: _NET_WM_ACTION_ABOVE (*)\n", i);
                    res |= _NET_WM_STATE_FLAG_ABOVE ;
                }
                else {
                    char * astr = XGetAtomName(dpy, actions[i]);
                    DBG_PRINT( "**************** X11: FS EWMH CHECK[%d]: %s (unused)\n", i, astr);
                    XFree(astr);
                }
            }
            XFree(actions);
        }
        DBG_PRINT( "**************** X11: FS EWMH CHECK: 0x%X\n", res);
    } else {
        DBG_PRINT( "**************** X11: FS EWMH CHECK: XGetWindowProperty failed: %d\n", s);
    }
#endif
    return _NET_WM_STATE_FLAG_FULLSCREEN | _NET_WM_STATE_FLAG_ABOVE ;
}

static Bool NewtWindows_setStackingEWMHFlags (Display *dpy, Window root, Window w, int ewmhFlags, Bool isVisible, Bool enable) {
    Atom _NET_WM_STATE = XInternAtom( dpy, "_NET_WM_STATE", False );
    Atom _NET_WM_STATE_ABOVE = XInternAtom( dpy, "_NET_WM_STATE_ABOVE", False );
    Atom _NET_WM_STATE_FULLSCREEN = XInternAtom( dpy, "_NET_WM_STATE_FULLSCREEN", False );
    int ewmhMask = NewtWindows_getSupportedStackingEWMHFlags(dpy, w);
    Bool changeFullscreen = 0 != ( ( _NET_WM_STATE_FLAG_FULLSCREEN & ewmhMask ) & ewmhFlags ) ;
    Bool changeAbove =      0 != ( ( _NET_WM_STATE_FLAG_ABOVE      & ewmhMask ) & ewmhFlags ) ;
    Bool res = False;

    if(0 == ewmhMask) { 
        return res;
    }

    // _NET_WM_STATE: fullscreen and/or above
    if( changeFullscreen || changeAbove ) {
        {
            // _NET_WM_STATE as event to root window
            XEvent xev;
            long mask = SubstructureNotifyMask | SubstructureRedirectMask ;
            int i=0;
            
            memset ( &xev, 0, sizeof(xev) );
            
            xev.type = ClientMessage;
            xev.xclient.window = w;
            xev.xclient.message_type = _NET_WM_STATE;
            xev.xclient.format = 32;
                
            xev.xclient.data.l[i++] = enable ? _NET_WM_STATE_ADD : _NET_WM_STATE_REMOVE ;
            if( changeFullscreen ) {
                xev.xclient.data.l[i++] = _NET_WM_STATE_FULLSCREEN;
            }
            if( changeAbove ) {
                xev.xclient.data.l[i++] = _NET_WM_STATE_ABOVE;
            }
            xev.xclient.data.l[3] = 1; //source indication for normal applications

            XSendEvent ( dpy, root, False, mask, &xev );
        }
        // Also change _NET_WM_BYPASS_COMPOSITOR!
        //   A value of 0 indicates no preference. 
        //   A value of 1 hints the compositor to disabling compositing of this window. 
        //   A value of 2 hints the compositor to not disabling compositing of this window
        {
            Atom _NET_WM_BYPASS_COMPOSITOR = XInternAtom( dpy, "_NET_WM_BYPASS_COMPOSITOR", False );
            unsigned long value = enable ? 1 : 0;
            XChangeProperty( dpy, w, _NET_WM_BYPASS_COMPOSITOR, XA_CARDINAL, 32, PropModeReplace, (unsigned char*)&value, 1); 
        }
        XSync(dpy, False);
        res = True;
    }
    DBG_PRINT( "X11: setStackingEWMHFlags ON %d, changeFullscreen %d, changeAbove %d, visible %d: %d\n", 
        enable, changeFullscreen, changeAbove, isVisible, res);
    return res;
}


Status NewtWindows_updateInsets(JNIEnv *env, jobject jwindow, Display *dpy, Window window, int *left, int *right, int *top, int *bottom) {
    if(0 != NewtWindows_getFrameExtends(dpy, window, left, right, top, bottom)) {
        DBG_PRINT( "NewtWindows_updateInsets: insets by _NET_FRAME_EXTENTS [ l %d, r %d, t %d, b %d ]\n",
            *left, *right, *top, *bottom);
        (*env)->CallVoidMethod(env, jwindow, insetsChangedID, JNI_FALSE, *left, *right, *top, *bottom);
        return 1; // OK
    }

    Bool hasDecor = NewtWindows_hasDecorations (dpy, window);
    if(hasDecor) {
        // The following logic only works if window is top-level _and_ the WM
        // has 'decorated' our client window w/ another parent window _within_ the actual 'framed' window.
        Window parent = NewtWindows_getParent(dpy, window);
        if(0 != NewtWindows_getWindowPositionRelative2Parent (dpy, parent, left, top)) {
            *right = *left; *bottom = *top;
            DBG_PRINT( "NewtWindows_updateInsets: insets by parent position [ l %d, r %d, t %d, b %d ]\n",
                *left, *right, *top, *bottom);
            (*env)->CallVoidMethod(env, jwindow, insetsChangedID, JNI_FALSE, *left, *right, *top, *bottom);
            return 1; // OK
        }
    }
    DBG_PRINT( "NewtWindows_updateInsets: cannot determine insets - hasDecor %d\n", hasDecor);
    return 0; // Error
}

static void NewtWindows_requestFocus (Display *dpy, Window w, Bool force) {
    XWindowAttributes xwa;
    Window focus_return;
    int revert_to_return;

    XSync(dpy, False);
    XGetInputFocus(dpy, &focus_return, &revert_to_return);
    DBG_PRINT( "X11: requestFocus dpy %p,win %p, force %d, hasFocus %d\n", dpy, (void*)w, force, focus_return==w);

    if( True==force || focus_return!=w) {
        DBG_PRINT( "X11: XRaiseWindow dpy %p, win %p\n", dpy, (void*)w);
        XRaiseWindow(dpy, w);
        NewtWindows_setCWAbove(dpy, w);
        // Avoid 'BadMatch' errors from XSetInputFocus, ie if window is not viewable
        XGetWindowAttributes(dpy, w, &xwa);
        DBG_PRINT( "X11: XSetInputFocus dpy %p,win %p, isViewable %d\n", dpy, (void*)w, (xwa.map_state == IsViewable));
        if(xwa.map_state == IsViewable) {
            XSetInputFocus(dpy, w, RevertToParent, CurrentTime);
        }
    }
    DBG_PRINT( "X11: requestFocus dpy %p,win %p, force %d - FIN\n", dpy, (void*)w, force);
    XSync(dpy, False);
}

/**
 * Window
 */

/*
 * Class:     jogamp_newt_driver_x11_WindowDriver
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_WindowDriver_initIDs0
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
    if( ( width>0 && height>0 ) || ( x>=0 && y>=0 ) ) { // resize/position if requested
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

static void NewtWindows_setIcon(Display *dpy, Window w, int data_size, const unsigned char * data_ptr) {
    Atom _NET_WM_ICON = XInternAtom(dpy, "_NET_WM_ICON", False);
    Atom CARDINAL = XInternAtom(dpy, "CARDINAL", False);
    XChangeProperty(dpy, w, _NET_WM_ICON, CARDINAL, 32, PropModeReplace, data_ptr, data_size);
}

/*
 * Class:     jogamp_newt_driver_x11_WindowDriver
 * Method:    CreateWindow
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_WindowDriver_CreateWindow0
  (JNIEnv *env, jobject obj, jlong parent, jlong display, jint screen_index, 
                             jint visualID, 
                             jlong javaObjectAtom, jlong windowDeleteAtom, 
                             jint x, jint y, jint width, jint height, jboolean autoPosition, int flags,
                             jint pixelDataSize, jobject pixels, jint pixels_byte_offset, jboolean pixels_is_direct)
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
    DBG_PRINT( "X11: CreateWindow dpy %p, screen %d, visualID 0x%X, parent %p, %d/%d %dx%d, undeco %d, alwaysOnTop %d, autoPosition %d\n", 
        (void*)dpy, scrn_idx, (int)visualID, (void*)windowParent, x, y, width, height,
        TST_FLAG_IS_UNDECORATED(flags), TST_FLAG_IS_ALWAYSONTOP(flags), autoPosition);

    // try given VisualID on screen
    memset(&visualTemplate, 0, sizeof(XVisualInfo));
    visualTemplate.screen = scrn_idx;
    visualTemplate.visualid = (VisualID)visualID;
    pVisualQuery = XGetVisualInfo(dpy, VisualIDMask|VisualScreenMask, &visualTemplate,&n);
    DUMP_VISUAL_INFO("Given VisualID", pVisualQuery);
    if(pVisualQuery!=NULL) {
        visual   = pVisualQuery->visual;
        depth    = pVisualQuery->depth;
        visualID = (jint)pVisualQuery->visualid;
        XFree(pVisualQuery);
        pVisualQuery=NULL;
    }
    DBG_PRINT( "X11: [CreateWindow] found visual: %p\n", visual);

    if (visual==NULL) { 
        NewtCommon_throwNewRuntimeException(env, "could not query Visual by given VisualID 0x%X, bail out!", (int)visualID);
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
        int left=0, right=0, top=0, bottom=0;
        const unsigned char * pixelPtr = NULL;

        // NOTE: MUST BE DIRECT BUFFER, since _NET_WM_ICON Atom uses buffer directly!
        DBG_PRINT("X11: CreateWindow icon: size %d, pixels %p, offset %d, direct %d\n", pixelDataSize, (void*)pixels, pixels_byte_offset, pixels_is_direct);
        if( 0 < pixelDataSize && NULL != pixels ) {
            pixelPtr = (const unsigned char *) ( JNI_TRUE == pixels_is_direct ? 
                                                    (*env)->GetDirectBufferAddress(env, pixels) : 
                                                    (*env)->GetPrimitiveArrayCritical(env, pixels, NULL) );
            DBG_PRINT("X11: CreateWindow icon: NIO %p\n", pixelPtr);
            NewtWindows_setIcon(dpy, window, (int)pixelDataSize, pixelPtr+pixels_byte_offset);
        }

        XMapWindow(dpy, window);
        XIfEvent( dpy, &event, WaitForMapNotify, (XPointer) window ); // wait to get proper insets values

        XSync(dpy, False);

        if( JNI_FALSE == pixels_is_direct && NULL != pixelPtr ) {
            (*env)->ReleasePrimitiveArrayCritical(env, pixels, (void*)pixelPtr, JNI_ABORT);  
        }

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
            NewtWindows_setStackingEWMHFlags(dpy, root, window, _NET_WM_STATE_FLAG_ABOVE, True, True);
        }
    }

    DBG_PRINT( "X11: [CreateWindow] created window %p on display %p\n", (void*)window, dpy);
    return (jlong) window;
}

/*
 * Class:     jogamp_newt_driver_x11_WindowDriver
 * Method:    CloseWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_WindowDriver_CloseWindow0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jlong javaObjectAtom, jlong windowDeleteAtom /*, jlong kbdHandle*/, // XKB disabled for now
                             jint randr_event_base, jint randr_error_base)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    jobject jwindow;
    XWindowAttributes xwa;

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
    memset(&xwa, 0, sizeof(XWindowAttributes));
    XGetWindowAttributes(dpy, w, &xwa); // prefetch colormap to be destroyed after window destruction
    XSelectInput(dpy, w, 0);
    XUnmapWindow(dpy, w);

    // Drain all events related to this window ..
    Java_jogamp_newt_driver_x11_DisplayDriver_DispatchMessages0(env, obj, display, javaObjectAtom, windowDeleteAtom /*, kbdHandle */, // XKB disabled for now
                                                                          randr_event_base, randr_error_base);

    XDestroyWindow(dpy, w);
    if( None != xwa.colormap ) {
        XFreeColormap(dpy, xwa.colormap);
    }
    XSync(dpy, True); // discard all events now, no more handler

    (*env)->DeleteGlobalRef(env, jwindow);

    DBG_PRINT( "X11: CloseWindow END\n");
}

// #define REPARENT_WAIT_FOR_REPARENT_NOTIFY 1

#ifdef REPARENT_WAIT_FOR_REPARENT_NOTIFY
static Bool WaitForReparentNotify( Display *dpy, XEvent *event, XPointer arg ) {
    Bool res = (event->type == ReparentNotify) && (event->xreparent.window == (Window) arg);
    #ifdef VERBOSE_ON
    if( event->type == ReparentNotify ) {
        DBG_PRINT( "X11.WaitForReparentNotify: Event ReparentNotify: Result %d, exp %p, has %p\n", (int)res, arg, event->xreparent.window);
    } else {
        DBG_PRINT( "X11.WaitForReparentNotify: Event 0x%X: Result %d, exp %p, has %p\n", (int)event->type, (int)res, arg, event->xreparent.window);
    }
    #endif
    return res;
}
#endif

/*
 * Class:     jogamp_newt_driver_x11_WindowDriver
 * Method:    reconfigureWindow0
 * Signature: (JIJJIIIII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_WindowDriver_reconfigureWindow0
  (JNIEnv *env, jobject obj, jlong jdisplay, jint screen_index,
   jlong jparent, jlong jwindow, jlong windowDeleteAtom,
   jint x, jint y, jint width, jint height, jint flags)
{
    Display * dpy = (Display *) (intptr_t) jdisplay;
    Window w = (Window)jwindow;
    Atom wm_delete_atom = (Atom)windowDeleteAtom;
    Window root = RootWindow(dpy, screen_index);
    Window parent = (0!=jparent)?(Window)jparent:root;
    XEvent event;
    Bool isVisible = !TST_FLAG_CHANGE_VISIBILITY(flags) && TST_FLAG_IS_VISIBLE(flags) ;
    Bool tempInvisible = ( TST_FLAG_CHANGE_FULLSCREEN(flags) || TST_FLAG_CHANGE_PARENTING(flags) ) && isVisible ;
    // Bool tempInvisible = TST_FLAG_CHANGE_PARENTING(flags) && isVisible ;
    int fsEWMHFlags = 0;
    if( TST_FLAG_CHANGE_FULLSCREEN(flags) ) {
        if( !TST_FLAG_IS_FULLSCREEN_SPAN(flags) ) {      // doesn't work w/ spanning across monitors. See also Bug 770 & Bug 771
            fsEWMHFlags |= _NET_WM_STATE_FLAG_FULLSCREEN;
        }
        if( TST_FLAG_IS_FULLSCREEN(flags) ) {
            if( TST_FLAG_IS_ALWAYSONTOP(flags) ) {
                fsEWMHFlags |= _NET_WM_STATE_FLAG_ABOVE; // fs on,  above on
            } // else { }                                // fs on,  above off
        } else if( !TST_FLAG_IS_ALWAYSONTOP(flags) ) {
            fsEWMHFlags |= _NET_WM_STATE_FLAG_ABOVE;     // fs off, above off
        } // else { }                                    // fs off, above on
    } else if( TST_FLAG_CHANGE_PARENTING(flags) ) {
        // Fix for Unity WM, i.e. _remove_ persistent previous states
        fsEWMHFlags |= _NET_WM_STATE_FLAG_FULLSCREEN;    // fs off
        if( !TST_FLAG_IS_ALWAYSONTOP(flags) ) {
            fsEWMHFlags |= _NET_WM_STATE_FLAG_ABOVE;     // above off
        }
    } else if( TST_FLAG_CHANGE_ALWAYSONTOP(flags) ) {
        fsEWMHFlags |= _NET_WM_STATE_FLAG_ABOVE;         // toggle above
    }

    DBG_PRINT( "X11: reconfigureWindow0 dpy %p, scrn %d, parent %p/%p, win %p, %d/%d %dx%d, parentChange %d, hasParent %d, decorationChange %d, undecorated %d, fullscreenChange %d, fullscreen %d (span %d), alwaysOnTopChange %d, alwaysOnTop %d, visibleChange %d, visible %d, tempInvisible %d, fsEWMHFlags %d\n",
        (void*)dpy, screen_index, (void*) jparent, (void*)parent, (void*)w,
        x, y, width, height, 
        TST_FLAG_CHANGE_PARENTING(flags),   TST_FLAG_HAS_PARENT(flags),
        TST_FLAG_CHANGE_DECORATION(flags),  TST_FLAG_IS_UNDECORATED(flags),
        TST_FLAG_CHANGE_FULLSCREEN(flags),  TST_FLAG_IS_FULLSCREEN(flags), TST_FLAG_IS_FULLSCREEN_SPAN(flags),
        TST_FLAG_CHANGE_ALWAYSONTOP(flags), TST_FLAG_IS_ALWAYSONTOP(flags),
        TST_FLAG_CHANGE_VISIBILITY(flags),  TST_FLAG_IS_VISIBLE(flags), 
        tempInvisible, fsEWMHFlags);

    // FS Note: To toggle FS, utilizing the _NET_WM_STATE_FULLSCREEN WM state should be enough.
    //          However, we have to consider other cases like reparenting and WM which don't support it.
    #if 0    // Also doesn't work work properly w/ Unity WM
    if( fsEWMHFlags && !TST_FLAG_CHANGE_PARENTING(flags) && isVisible &&
        !TST_FLAG_IS_FULLSCREEN_SPAN(flags) &&
        ( TST_FLAG_CHANGE_FULLSCREEN(flags) || TST_FLAG_CHANGE_ALWAYSONTOP(flags) ) ) {
        Bool enable = TST_FLAG_CHANGE_FULLSCREEN(flags) ? TST_FLAG_IS_FULLSCREEN(flags) : TST_FLAG_IS_ALWAYSONTOP(flags) ;
        if( NewtWindows_setStackingEWMHFlags(dpy, root, w, fsEWMHFlags, isVisible, enable) ) {
            if ( TST_FLAG_CHANGE_FULLSCREEN(flags) && !TST_FLAG_IS_FULLSCREEN(flags) ) { // FS off - restore decoration
                NewtWindows_setDecorations (dpy, w, TST_FLAG_IS_UNDECORATED(flags) ? False : True);
            }
            DBG_PRINT( "X11: reconfigureWindow0 X (fs.atop.fast)\n");
            return;
        }
    }
    #endif
    // Toggle ALWAYSONTOP (only) w/o visibility or window stacking sideffects
    if( isVisible && fsEWMHFlags && TST_FLAG_CHANGE_ALWAYSONTOP(flags) &&
        !TST_FLAG_CHANGE_PARENTING(flags) && !TST_FLAG_CHANGE_FULLSCREEN(flags) ) {
        if( NewtWindows_setStackingEWMHFlags(dpy, root, w, fsEWMHFlags, isVisible, TST_FLAG_IS_ALWAYSONTOP(flags)) ) {
            DBG_PRINT( "X11: reconfigureWindow0 X (atop.fast)\n");
            return;
        }
    }

    if( tempInvisible ) {
        DBG_PRINT( "X11: reconfigureWindow0 TEMP VISIBLE OFF\n");
        XUnmapWindow(dpy, w);
        XIfEvent( dpy, &event, WaitForUnmapNotify, (XPointer) w );
        // no need to notify the java side .. just temp change
    }

    if( fsEWMHFlags && ( ( TST_FLAG_CHANGE_FULLSCREEN(flags)  && !TST_FLAG_IS_FULLSCREEN(flags) ) ||     // FS off
                         ( TST_FLAG_CHANGE_ALWAYSONTOP(flags) && !TST_FLAG_IS_ALWAYSONTOP(flags) ) ) ) { // AlwaysOnTop off
        NewtWindows_setStackingEWMHFlags(dpy, root, w, fsEWMHFlags, isVisible, False);
    }

    if( TST_FLAG_CHANGE_PARENTING(flags) && !TST_FLAG_HAS_PARENT(flags) ) {
        // TOP: in -> out
        DBG_PRINT( "X11: reconfigureWindow0 PARENTING in->out\n");
        XReparentWindow( dpy, w, parent, x, y ); // actual reparent call
        #ifdef REPARENT_WAIT_FOR_REPARENT_NOTIFY
            XIfEvent( dpy, &event, WaitForReparentNotify, (XPointer) w );
        #endif
        XSync(dpy, False);
        XSetWMProtocols(dpy, w, &wm_delete_atom, 1); // windowDeleteAtom
        // Fix for Unity WM, i.e. _remove_ persistent previous states
        NewtWindows_setStackingEWMHFlags(dpy, root, w, fsEWMHFlags, isVisible, False);
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
        #ifdef REPARENT_WAIT_FOR_REPARENT_NOTIFY
            XIfEvent( dpy, &event, WaitForReparentNotify, (XPointer) w );
        #endif
        XSync(dpy, False);
    }

    if( tempInvisible ) {
        DBG_PRINT( "X11: reconfigureWindow0 TEMP VISIBLE ON\n");
        XMapRaised(dpy, w);
        XIfEvent( dpy, &event, WaitForMapNotify, (XPointer) w );
        // no need to notify the java side .. just temp change
    } else if( TST_FLAG_CHANGE_VISIBILITY(flags) ) {
        if( TST_FLAG_IS_VISIBLE(flags) ) {
            DBG_PRINT( "X11: reconfigureWindow0 VISIBLE ON\n");
            XMapRaised(dpy, w);
            XSync(dpy, False);
            // WM may disregard pos/size XConfigureWindow requests for invisible windows!
            DBG_PRINT( "X11: reconfigureWindow0 setPosSize.2 %d/%d %dx%d\n", x, y, width, height);
            NewtWindows_setPosSize(dpy, w, x, y, width, height);
        } else {
            DBG_PRINT( "X11: reconfigureWindow0 VISIBLE OFF\n");
            XUnmapWindow(dpy, w);
            XSync(dpy, False);
        }
    }

    if( fsEWMHFlags && ( ( TST_FLAG_CHANGE_FULLSCREEN(flags)  && TST_FLAG_IS_FULLSCREEN(flags) ) ||     // FS on
                         ( TST_FLAG_CHANGE_ALWAYSONTOP(flags) && TST_FLAG_IS_ALWAYSONTOP(flags) ) ) ) { // AlwaysOnTop on
        NewtWindows_requestFocus (dpy, w, True);
        NewtWindows_setStackingEWMHFlags(dpy, root, w, fsEWMHFlags, isVisible, True);
    }

    DBG_PRINT( "X11: reconfigureWindow0 X (full)\n");
}

/*
 * Class:     jogamp_newt_driver_x11_WindowDriver
 * Method:    requestFocus0
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_WindowDriver_requestFocus0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jboolean force)
{
    NewtWindows_requestFocus ( (Display *) (intptr_t) display, (Window)window, JNI_TRUE==force?True:False ) ;
}

/*
 * Class:     jogamp_newt_driver_x11_WindowDriver
 * Method:    getParentWindow0
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_WindowDriver_getParentWindow0
  (JNIEnv *env, jclass clazz, jlong display, jlong window)
{
    return (jlong) NewtWindows_getParent ((Display *) (intptr_t) display, (Window)window);
}

/*
 * Class:     Java_jogamp_newt_driver_x11_WindowDriver
 * Method:    setTitle0
 * Signature: (JJLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_WindowDriver_setTitle0
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
 * Class:     Java_jogamp_newt_driver_x11_WindowDriver
 * Method:    setPointerIcon0
 * Signature: (JJILjava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_WindowDriver_setPointerIcon0
  (JNIEnv *env, jclass clazz, jlong display, jlong window, jlong handle)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;

    if( 0 == handle ) {
        DBG_PRINT( "X11: setPointerIcon0: reset\n");
        XUndefineCursor(dpy, w);
    } else {
        Cursor c = (Cursor) (intptr_t) handle;
        DBG_PRINT( "X11: setPointerIcon0: %p\n", (void*)c);
        XDefineCursor(dpy, w, c);
    }
}

/*
 * Class:     Java_jogamp_newt_driver_x11_WindowDriver
 * Method:    setPointerVisible0
 * Signature: (JJZ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_WindowDriver_setPointerVisible0
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
 * Class:     Java_jogamp_newt_driver_x11_WindowDriver
 * Method:    confinePointer0
 * Signature: (JJZ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_WindowDriver_confinePointer0
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
 * Class:     Java_jogamp_newt_driver_x11_WindowDriver
 * Method:    warpPointer0
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_WindowDriver_warpPointer0
  (JNIEnv *env, jclass clazz, jlong display, jlong window, jint x, jint y)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;

    DBG_PRINT( "X11: warpPointer0: %d/%d\n", x, y);

    XWarpPointer(dpy, None, w, 0, 0, 0, 0, x, y);
}

