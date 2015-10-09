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

#define _NET_WM_STATE_REMOVE 0
#define _NET_WM_STATE_ADD 1

/** Sync w/ X11Common.h MASK and IDX */
static const char * _ALL_ATOM_NAMES[] = { 
    /*  0 */ "_NET_WM_STATE",
    /*  1 */ "_NET_WM_STATE_MODAL",
    /*  2 */ "_NET_WM_STATE_STICKY",
    /*  3 */ "_NET_WM_STATE_MAXIMIZED_VERT",
    /*  4 */ "_NET_WM_STATE_MAXIMIZED_HORZ",
    /*  5 */ "_NET_WM_STATE_SHADED",
    /*  6 */ "_NET_WM_STATE_SKIP_TASKBAR",
    /*  7 */ "_NET_WM_STATE_SKIP_PAGER",
    /*  8 */ "_NET_WM_STATE_HIDDEN",
    /*  9 */ "_NET_WM_STATE_FULLSCREEN",
    /* 10 */ "_NET_WM_STATE_ABOVE",
    /* 11 */ "_NET_WM_STATE_BELOW",
    /* 12 */ "_NET_WM_STATE_DEMANDS_ATTENTION",
    /* 13 */ "_NET_WM_STATE_FOCUSED",
    /* 14 */ "_NET_WM_BYPASS_COMPOSITOR",
    /* 15 */ "_NET_WM_DESKTOP",
    /* 16 */ "_NET_CURRENT_DESKTOP",
    /* 17 */ "_NET_WM_WINDOW_TYPE",
    /* 18 */ "_NET_WM_WINDOW_TYPE_NORMAL",
    /* 19 */ "_NET_WM_WINDOW_TYPE_POPUP_MENU",
    /* 20 */ "_NET_FRAME_EXTENTS",
    /* 21 */ "_NET_SUPPORTED",
    /* 22 */ "_NET_ACTIVE_WINDOW",
    /* 23 */ "WM_CHANGE_STATE",
    /* 24 */ "_MOTIF_WM_HINTS"
};
static const uint32_t _ALL_ATOM_COUNT = (uint32_t)(sizeof(_ALL_ATOM_NAMES)/sizeof(const char *));

static uint32_t NewtWindows_getSupportedFeatureEWMH(Display *dpy, const Atom * allAtoms, const Atom action, const int num, Bool verbose) {
    uint32_t i;
    for(i=1; i<_ALL_ATOM_COUNT; i++) {
        if( action == allAtoms[i] ) {
            if(verbose) {
                fprintf(stderr, "...... [%d] -> [%d/%d]: %s\n", num, i, _ALL_ATOM_COUNT, _ALL_ATOM_NAMES[i]);
            }
            return 1U << i;
        }
    }
    if(verbose) {
        char * astr = XGetAtomName(dpy, action);
        fprintf(stderr, "...... [%d] -> [_/%d]: %s (undef)\n", num, _ALL_ATOM_COUNT, astr);
        XFree(astr);
    }
    return 0;
}
static uint32_t NewtWindows_getSupportedFeaturesEWMH(Display *dpy, Window root, Atom * allAtoms, Bool verbose) {
    Atom * properties = NULL;
    Atom type = 0;
    unsigned long props_count = 0, remain = 0;
    int form = 0, i = 0;
    uint32_t res = 0;
    Status s;

    if ( Success == (s = XGetWindowProperty(dpy, root, allAtoms[_NET_SUPPORTED_IDX], 0, 1024, False, AnyPropertyType,
                                            &type, &form, &props_count, &remain, (unsigned char**)&properties)) ) {
        if( NULL != properties ) {
            for(i=0; i<props_count; i++) {
                res |= NewtWindows_getSupportedFeatureEWMH(dpy, allAtoms, properties[i], i, verbose);
            }
            XFree(properties);
        }
        if(verbose) {
            fprintf(stderr, "**************** X11: Feature EWMH CHECK: 0x%X\n", res);
        }
    } else if(verbose) {
        fprintf(stderr, "**************** X11: Feature EWMH CHECK: XGetWindowProperty failed: %d\n", s);
    }
    return res;
}
uint32_t NewtWindows_getNET_WM_STATE(Display *dpy, JavaWindow *w) {
    Bool verbose = 
#ifdef VERBOSE_ON
                        True
#else
                        False
#endif
    ;
    Window window = w->window;
    Atom * allAtoms = w->allAtoms;
    Atom * properties = NULL;
    Atom type = 0;
    unsigned long props_count = 0, remain = 0;
    int form = 0, i = 0;
    uint32_t res = 0;
    Status s;

    if ( Success == (s = XGetWindowProperty(dpy, window, allAtoms[_NET_WM_STATE_IDX], 0, 1024, False, AnyPropertyType,
                                            &type, &form, &props_count, &remain, (unsigned char**)&properties)) ) {
        if( NULL != properties ) {
            for(i=0; i<props_count; i++) {
                res |= NewtWindows_getSupportedFeatureEWMH(dpy, allAtoms, properties[i], i, verbose);
            }
            XFree(properties);
        }
        if(verbose) {
            fprintf(stderr, "**************** X11: WM_STATE of %p: %d props -> 0x%X\n", (void*)window, (int)props_count, res);
        }
    } else if(verbose) {
        fprintf(stderr, "**************** X11: WM_STATE of %p: XGetWindowProperty failed: %d\n", (void*)window, s);
    }
    return res;
}

static JavaWindow* createJavaWindowProperty(JNIEnv *env, Display *dpy, Window root, Window window, 
                                            jlong javaObjectAtom, jlong windowDeleteAtom, jobject obj, Bool verbose) {
    jobject jwindow = (*env)->NewGlobalRef(env, obj);
    JavaWindow * res;
    {
        Atom * allAtoms = calloc(_ALL_ATOM_COUNT, sizeof(Atom));
        if( 0 == XInternAtoms( dpy, (char **)_ALL_ATOM_NAMES, _ALL_ATOM_COUNT, False, allAtoms) ) {
            // error
            fprintf(stderr, "**************** X11: XInternAtoms failed\n");
            return NULL;
        }
        res = calloc(1, sizeof(JavaWindow));
        res->window = window;
        res->jwindow = jwindow;
        res->allAtoms = allAtoms;
        res->javaObjectAtom = (Atom)javaObjectAtom;
        res->windowDeleteAtom = (Atom)windowDeleteAtom;
        res->supportedAtoms = NewtWindows_getSupportedFeaturesEWMH(dpy, root, allAtoms, verbose);
        res->lastDesktop = 0; //undef
        res->maxHorz = False;
        res->maxVert = False;
        res->isMapped = False;
    }
    unsigned long jogl_java_object_data[2]; // X11 is based on 'unsigned long'
    int nitems_32 = putPtrIn32Long( jogl_java_object_data, (uintptr_t) res);
    {
        JavaWindow * test = (JavaWindow *) getPtrOut32Long(jogl_java_object_data);
        if( res != test ) {
            NewtCommon_FatalError(env, "Internal Error .. Encoded Window ref not the same %p != %p !", res, test);
        }
    }
    XChangeProperty( dpy, window, (Atom)javaObjectAtom, (Atom)javaObjectAtom, 32, PropModeReplace, 
                                     (unsigned char *)&jogl_java_object_data, nitems_32);
    return res;
}
static void destroyJavaWindow(JNIEnv *env, JavaWindow *w) {
    if( NULL != w ) {
        (*env)->DeleteGlobalRef(env, w->jwindow);
        w->jwindow = 0;
        if( NULL != w->allAtoms ) {
            free(w->allAtoms);
            w->allAtoms = NULL;
        }
        free(w);
    }
}
JavaWindow * getJavaWindowProperty(JNIEnv *env, Display *dpy, Window window, jlong javaObjectAtom, Bool showWarning) {
    Atom actual_type = 0;
    int actual_format = 0;
    int nitems_32 = ( sizeof(uintptr_t) == 8 ) ? 2 : 1 ;
    unsigned char * jogl_java_object_data_pp = NULL;
    JavaWindow * res = NULL;

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

    res = (JavaWindow *) getPtrOut32Long( (unsigned long *) jogl_java_object_data_pp ) ;
    XFree(jogl_java_object_data_pp);

#ifdef VERBOSE_ON
    if(JNI_FALSE == (*env)->IsInstanceOf(env, res->jwindow, X11NewtWindowClazz)) {
        NewtCommon_throwNewRuntimeException(env, "fetched Atom NEWT_JAVA_OBJECT window is not a NEWT Window: javaWindow 0x%X !", res->jwindow);
    }
#endif
    return res;
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
static Status NewtWindows_getFrameExtends(Display *dpy, JavaWindow *w, int *left, int *right, int *top, int *bottom) {
    Atom actual_type = 0;
    int actual_format = 0;
    int nitems_32 = 4; // l, r, t, b
    unsigned char * frame_extends_data_pp = NULL;

    {
        unsigned long nitems = 0;
        unsigned long bytes_after = 0;
        int res;

        res = XGetWindowProperty(dpy, w->window, w->allAtoms[_NET_FRAME_EXTENTS_IDX], 0, nitems_32, False, 
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

static void NewtWindows_setDecorations (Display *dpy, JavaWindow *w, Bool decorated) {

#ifdef DECOR_USE_MWM
    unsigned long mwmhints[PROP_MWM_HINTS_ELEMENTS] = { MWM_HINTS_DECORATIONS, 0, decorated, 0, 0 }; // flags, functions, decorations, input_mode, status
#endif

#ifdef DECOR_USE_EWMH
    Atom types[3]={0};
    int ntypes=0;
    if(True==decorated) {
        types[ntypes++] = w->allAtoms[_NET_WM_WINDOW_TYPE_NORMAL_IDX];
    } else {
        types[ntypes++] = w->allAtoms[_NET_WM_WINDOW_TYPE_POPUP_MENU_IDX];
    }
#endif

#ifdef DECOR_USE_MWM
    XChangeProperty( dpy, w->window, w->allAtoms[_MOTIF_WM_HINTS_IDX], w->allAtoms[_MOTIF_WM_HINTS_IDX], 32, PropModeReplace, 
                     (unsigned char *)&mwmhints, PROP_MWM_HINTS_ELEMENTS);
#endif

#ifdef DECOR_USE_EWMH
    XChangeProperty( dpy, w->window, w->allAtoms[_NET_WM_WINDOW_TYPE_IDX], XA_ATOM, 32, PropModeReplace, (unsigned char *)&types, ntypes);
#endif
    XFlush(dpy);
}

static Bool NewtWindows_hasDecorations (Display *dpy, JavaWindow * w) {
    Bool decor = False;

#ifdef DECOR_USE_MWM
    unsigned char *wm_data = NULL;
    Atom wm_type = 0;
    int wm_format = 0;
    unsigned long wm_nitems = 0, wm_bytes_after = 0;
 
    if( Success == XGetWindowProperty(dpy, w->window, w->allAtoms[_MOTIF_WM_HINTS_IDX], 0, PROP_MWM_HINTS_ELEMENTS, False, AnyPropertyType, 
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

static void NewtWindows_requestFocus (Display *dpy, JavaWindow * jw, Bool force) {
    XWindowAttributes xwa;
    Window focus_return;
    int revert_to_return;

    XGetInputFocus(dpy, &focus_return, &revert_to_return);
    DBG_PRINT( "X11: requestFocus dpy %p,win %p, force %d, hasFocus %d\n", dpy, (void*)jw->window, force, focus_return==jw->window);

    if( True==force || focus_return!=jw->window) {
        DBG_PRINT( "X11: XRaiseWindow dpy %p, win %p\n", dpy, (void*)jw->window);
        XRaiseWindow(dpy, jw->window);
        NewtWindows_setCWAbove(dpy, jw->window);
        // Avoid 'BadMatch' errors from XSetInputFocus, ie if window is not viewable
        XGetWindowAttributes(dpy, jw->window, &xwa);
        DBG_PRINT( "X11: XSetInputFocus dpy %p,win %p, isViewable %d\n", dpy, (void*)jw->window, (xwa.map_state == IsViewable));
        if(xwa.map_state == IsViewable) {
            XSetInputFocus(dpy, jw->window, RevertToParent, CurrentTime);
        }
    }
    XFlush(dpy);
    DBG_PRINT( "X11: requestFocus dpy %p,win %p, force %d - FIN\n", dpy, (void*)jw->window, force);
}

Bool NewtWindows_updateInsets(Display *dpy, JavaWindow * w, int *left, int *right, int *top, int *bottom) {
    if(0 != NewtWindows_getFrameExtends(dpy, w, left, right, top, bottom)) {
        DBG_PRINT( "NewtWindows_updateInsets: insets by _NET_FRAME_EXTENTS [ l %d, r %d, t %d, b %d ]\n", *left, *right, *top, *bottom);
        return True; // OK
    }

    Bool hasDecor = NewtWindows_hasDecorations (dpy, w);
    if(hasDecor) {
        // The following logic only works if window is top-level _and_ the WM
        // has 'decorated' our client window w/ another parent window _within_ the actual 'framed' window.
        Window parent = NewtWindows_getParent(dpy, w->window);
        if(0 != NewtWindows_getWindowPositionRelative2Parent (dpy, parent, left, top)) {
            *right = *left; *bottom = *top;
            DBG_PRINT( "NewtWindows_updateInsets: insets by parent position [ l %d, r %d, t %d, b %d ]\n", *left, *right, *top, *bottom);
            return True; // OK
        }
    }
    DBG_PRINT( "NewtWindows_updateInsets: cannot determine insets - hasDecor %d\n", hasDecor);
    return False; // Error
}

Bool NewtWindows_updateMaximized(Display *dpy, JavaWindow * w, uint32_t netWMState) {
    Bool maxHorz = 0 != ( _MASK_NET_WM_STATE_MAXIMIZED_HORZ & netWMState ) ;
    Bool maxVert = 0 != ( _MASK_NET_WM_STATE_MAXIMIZED_VERT & netWMState ) ;
    if( w->maxHorz != maxHorz || w->maxVert != maxVert ) {
        w->maxHorz = maxHorz;
        w->maxVert = maxVert;
        return True;
    } else {
        return False;
    }
}

static void NewtWindows_setMinMaxSize(Display *dpy, JavaWindow *w, int min_width, int min_height, int max_width, int max_height) {
    XSizeHints * xsh = XAllocSizeHints();
    if( NULL != xsh ) {
        if( -1 != min_width && -1 != min_height && -1 != max_width && -1 != max_height ) {
            xsh->flags = PMinSize | PMaxSize;
            xsh->min_width=min_width;
            xsh->min_height=min_height;
            xsh->max_width=max_width;
            xsh->max_height=max_height;
        }
#if 0
        XSetWMNormalHints(dpy, w->window, xsh);
#else
        XSetWMSizeHints(dpy, w->window, xsh, XA_WM_NORMAL_HINTS);
#endif
        XFree(xsh);
    }
}

static void NewtWindows_setWindowTypeEWMH (Display *dpy, JavaWindow * w, int typeIdx) {
    Atom types[1]={0};
    if( _NET_WM_WINDOW_TYPE_NORMAL_IDX == typeIdx ) {
        types[0] = w->allAtoms[_NET_WM_WINDOW_TYPE_NORMAL_IDX];
    } // else { }
    if( 0 != types[0] ) {
        XChangeProperty( dpy, w->window, w->allAtoms[_NET_WM_WINDOW_TYPE_IDX], XA_ATOM, 32, PropModeReplace, (unsigned char *)&types, 1);
        XFlush(dpy);
    }
}

void NewtWindows_setUrgency(Display *dpy, Window window, Bool enable) {
    XWMHints wmh;
    memset ( &wmh, 0, sizeof(wmh) );
    if( enable ) {
        wmh.flags = XUrgencyHint;
    }
    XSetWMHints(dpy, window, &wmh);
}

void NewtWindows_sendNET_WM_STATE(Display *dpy, Window root, JavaWindow *w, int prop1Idx, int prop2Idx, Bool enable) {
    XEvent xev;
    int i=0;
    
    memset ( &xev, 0, sizeof(xev) );
    
    xev.type = ClientMessage;
    xev.xclient.window = w->window;
    xev.xclient.message_type = w->allAtoms[_NET_WM_STATE_IDX];
    xev.xclient.format = 32;
        
    xev.xclient.data.l[i++] = enable ? _NET_WM_STATE_ADD : _NET_WM_STATE_REMOVE ;
    if( 0 < prop1Idx ) {
        xev.xclient.data.l[i++] = w->allAtoms[prop1Idx];
    }
    if( 0 < prop2Idx ) {
        xev.xclient.data.l[i++] = w->allAtoms[prop2Idx];
    }
    xev.xclient.data.l[3] = 1; //source indication for normal applications

    XSendEvent ( dpy, root, False, SubstructureNotifyMask | SubstructureRedirectMask, &xev );
}
static unsigned long NewtWindows_getDesktopNum(Display *dpy, Window root, JavaWindow * w) {
    unsigned long res = 0;
    unsigned long * data_pp = NULL;

    Atom actual_type = 0;
    int actual_format = 0;
    unsigned long nitems= 0;
    unsigned long bytes_after= 0;

    if( Success == XGetWindowProperty(dpy, w->window, w->allAtoms[_NET_WM_DESKTOP_IDX], 0, 1, False, 
                                      AnyPropertyType, &actual_type, &actual_format, 
                                      &nitems, &bytes_after, (unsigned char **)&data_pp) ) 
    {
        if(XA_CARDINAL==actual_type && 32==actual_format && 1<=nitems && NULL!=data_pp) {
            res = *data_pp;
            DBG_PRINT("Info: NEWT X11Window: _NET_WM_DESKTOP: %ld\n", res);
        } else {
            DBG_PRINT("Warning: NEWT X11Window: Fetch _NET_WM_DESKTOP failed: nitems %ld, bytes_after %ld, actual_type %ld, actual_format %d, data_pp %p\n", 
                nitems, bytes_after, (long)actual_type, actual_format, data_pp);
            if( NULL != data_pp ) {
                DBG_PRINT("Warning:  *data_pp = %ld\n", *data_pp);
            }
        }
        if(NULL!=data_pp) {
            XFree(data_pp);
        }
    } else {
        DBG_PRINT("Warning: NEWT X11Window: Could not fetch _NET_WM_DESKTOP, nitems %ld, bytes_after %ld, result 0!\n", nitems, bytes_after);
    }
    return res;
}
static void NewtWindows_sendNET_WM_DESKTOP(Display *dpy, Window root, JavaWindow * w, unsigned long deskNum) {
    XEvent xev;
    memset ( &xev, 0, sizeof(xev) );

    xev.type = ClientMessage;
    xev.xclient.window = w->window;
    xev.xclient.message_type = w->allAtoms[_NET_WM_DESKTOP_IDX];
    xev.xclient.format = 32;
    xev.xclient.data.l[0] = deskNum;
    xev.xclient.data.l[1] = 1; //source indication for normal applications
    XSendEvent ( dpy, root, False, SubstructureNotifyMask | SubstructureRedirectMask, &xev );
}


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
static void NewtWindows_setStackingEWMHFlags (Display *dpy, Window root, JavaWindow * w, int ewmhFlags, Bool enable) {
    if( 0 == ewmhFlags ) {
        return;
    }
    Bool changeSticky =     0 != ( _MASK_NET_WM_STATE_STICKY         & ewmhFlags ) ;
    Bool changeFullscreen = 0 != ( _MASK_NET_WM_STATE_FULLSCREEN     & ewmhFlags ) ;
    Bool changeAbove =      0 != ( _MASK_NET_WM_STATE_ABOVE          & ewmhFlags ) ;
    Bool changeBelow =      0 != ( _MASK_NET_WM_STATE_BELOW          & ewmhFlags ) ;
    Bool changeMaxVert =    0 != ( _MASK_NET_WM_STATE_MAXIMIZED_VERT & ewmhFlags ) ;
    Bool changeMaxHorz =    0 != ( _MASK_NET_WM_STATE_MAXIMIZED_HORZ & ewmhFlags ) ;

    if( changeSticky ) {
        unsigned long deskNum;
        if( enable ) {
            w->lastDesktop = NewtWindows_getDesktopNum(dpy, root, w);
            deskNum = 0xFFFFFFFFU;
        } else {
            deskNum = w->lastDesktop;
        }
        NewtWindows_sendNET_WM_STATE(dpy, root, w, _NET_WM_STATE_STICKY_IDX, 0, enable);
        NewtWindows_sendNET_WM_DESKTOP(dpy, root, w, deskNum);
    } else if( changeFullscreen || changeAbove || changeBelow ) {
        int prop2Idx;
        if( changeAbove ) {
            prop2Idx = _NET_WM_STATE_ABOVE_IDX;
        } else if( changeBelow ) {
            prop2Idx = _NET_WM_STATE_BELOW_IDX;
        } else {
            prop2Idx = 0;
        }
        NewtWindows_sendNET_WM_STATE(dpy, root, w,
                                     changeFullscreen ? _NET_WM_STATE_FULLSCREEN_IDX : 0, 
                                     prop2Idx, 
                                     enable);
        // Also change _NET_WM_BYPASS_COMPOSITOR!
        //   A value of 0 indicates no preference. 
        //   A value of 1 hints the compositor to disabling compositing of this window. 
        //   A value of 2 hints the compositor to not disabling compositing of this window
        if( changeFullscreen ) {
            unsigned long value = enable ? 1 : 0;
            XChangeProperty( dpy, w->window, w->allAtoms[_NET_WM_BYPASS_COMPOSITOR_IDX], XA_CARDINAL, 32, PropModeReplace, (unsigned char*)&value, 1); 
        }
    } else if( changeMaxVert || changeMaxHorz ) {
        if( changeMaxHorz ) {
            w->maxHorz = enable;
        }
        if( changeMaxVert ) {
            w->maxVert = enable;
        }
        NewtWindows_sendNET_WM_STATE(dpy, root, w,
                                     changeMaxHorz ? _NET_WM_STATE_MAXIMIZED_HORZ_IDX : 0, 
                                     changeMaxVert ? _NET_WM_STATE_MAXIMIZED_VERT_IDX : 0, 
                                     enable);
    }
    XFlush(dpy);
    DBG_PRINT( "X11: setStackingEWMHFlags ON %d, change[Sticky %d, Fullscreen %d, Above %d, Below %d, MaxV %d, MaxH %d]\n", 
        enable, changeSticky, changeFullscreen, changeAbove, changeBelow, changeMaxVert, changeMaxHorz);
}

static Bool WaitForMapNotify( Display *dpy, XEvent *event, XPointer arg ) {
    return (event->type == MapNotify) && (event->xmap.window == (Window) arg);
}

static Bool WaitForUnmapNotify( Display *dpy, XEvent *event, XPointer arg ) {
    return (event->type == UnmapNotify) && (event->xmap.window == (Window) arg);
}

static void NewtWindows_setVisible(Display *dpy, Window root, JavaWindow* jw, Bool visible, Bool useWM, Bool waitForNotify) {
    XEvent event;
    DBG_PRINT( "X11: setVisible -> %d, useWM: %d, wait %d, window %p\n", (int)visible, (int)useWM, (int)waitForNotify, (void*)jw->window);
    if( useWM && jw->isMapped && 0 != ( _MASK_NET_WM_STATE_HIDDEN & jw->supportedAtoms ) ) {
        // It has been experienced that MapNotify/UnmapNotify is not sent for windows when using NormalState/IconicState!
        // See X11Display.c::Java_jogamp_newt_driver_x11_DisplayDriver_DispatchMessages0 case ConfigureNotify
        // NewtWindows_sendNET_WM_STATE(dpy, root, jw, _NET_WM_STATE_DEMANDS_ATTENTION_IDX, 0, True);
        // NewtWindows_setUrgency(dpy, jw->window, True);
        XEvent xev;
        memset ( &xev, 0, sizeof(xev) );
        if( visible ) {
            // NormalState does not work on some WMs (Gnome, KDE)
            xev.type = ClientMessage;
            xev.xclient.window = jw->window;
            xev.xclient.message_type = jw->allAtoms[_NET_ACTIVE_WINDOW_IDX];
            xev.xclient.format = 32;
            xev.xclient.data.l[0] = 1; //source indication for normal applications
            xev.xclient.data.l[1] = CurrentTime;
            XSendEvent ( dpy, root, False, SubstructureNotifyMask | SubstructureRedirectMask, &xev );
        } else {
            xev.type = ClientMessage;
            xev.xclient.window = jw->window;
            xev.xclient.message_type = jw->allAtoms[_WM_CHANGE_STATE_IDX];
            xev.xclient.format = 32;
            xev.xclient.data.l[0] = IconicState;
            XSendEvent ( dpy, root, False, SubstructureNotifyMask | SubstructureRedirectMask, &xev );
        }
    } else {
        if( visible ) {
            XMapRaised(dpy, jw->window);
            if(waitForNotify) {
                XIfEvent( dpy, &event, WaitForMapNotify, (XPointer) jw->window );
            }
            jw->isMapped=True;
        } else {
            XUnmapWindow(dpy, jw->window);
            if(waitForNotify) {
                XIfEvent( dpy, &event, WaitForUnmapNotify, (XPointer) jw->window );
            }
            jw->isMapped=False;
        }
    }
    XFlush(dpy);
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

static void NewtWindows_setPosSize(Display *dpy, JavaWindow* w, jint x, jint y, jint width, jint height) {
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
        XConfigureWindow(dpy, w->window, flags, &xwc);
        XFlush(dpy);
    }
}

static void NewtWindows_setIcon(Display *dpy, Window w, int data_size, const unsigned char * data_ptr) {
    Atom _NET_WM_ICON = XInternAtom( dpy, "_NET_WM_ICON", False );
    XChangeProperty(dpy, w, _NET_WM_ICON, XA_CARDINAL, 32, PropModeReplace, data_ptr, data_size);
}

/*
 * Class:     jogamp_newt_driver_x11_WindowDriver
 * Method:    CreateWindow
 */
JNIEXPORT jlongArray JNICALL Java_jogamp_newt_driver_x11_WindowDriver_CreateWindow0
  (JNIEnv *env, jobject obj, jlong parent, jlong display, jint screen_index, 
                             jint visualID, 
                             jlong javaObjectAtom, jlong windowDeleteAtom, 
                             jint x, jint y, jint width, jint height, int flags,
                             jint pixelDataSize, jobject pixels, jint pixels_byte_offset, jboolean pixels_is_direct, 
                             jboolean verbose)
{
    Display * dpy = (Display *)(intptr_t)display;
    Atom wm_delete_atom = (Atom)windowDeleteAtom;
    int       scrn_idx = (int)screen_index;
    Window root = RootWindow(dpy, scrn_idx);
    Window  windowParent = (Window) parent;
    Window  window = 0;
    JavaWindow * javaWindow = NULL;
    jlong handles[2];
    jlongArray jhandles;

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
    DBG_PRINT( "X11: CreateWindow dpy %p, screen %d, visualID 0x%X, parent %p, %d/%d %dx%d, undeco %d, alwaysOn[Top %d, Bottom %d], autoPos %d, resizable %d\n", 
        (void*)dpy, scrn_idx, (int)visualID, (void*)windowParent, x, y, width, height,
        TST_FLAG_IS_UNDECORATED(flags), TST_FLAG_IS_ALWAYSONTOP(flags), TST_FLAG_IS_ALWAYSONBOTTOM(flags),
        TST_FLAG_IS_AUTOPOSITION(flags), TST_FLAG_IS_RESIZABLE(flags));

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

    attrMask  = ( CWBackingStore | CWBackingPlanes | CWBackingPixel | 
                  CWBackPixmap | CWBackPixel | CWBorderPixel | CWColormap | 
                  CWOverrideRedirect | CWEventMask ) ;

    memset(&xswa, 0, sizeof(xswa));
    xswa.backing_store=NotUseful;  /* NotUseful, WhenMapped, Always */
    xswa.backing_planes=0;         /* planes to be preserved if possible */
    xswa.backing_pixel=0;          /* value to use in restoring planes */
    xswa.background_pixmap = None;
    xswa.background_pixel = BlackPixel(dpy, scrn_idx);
    xswa.border_pixel = 0;
    xswa.colormap = XCreateColormap(dpy, windowParent, visual, AllocNone);
    xswa.override_redirect = False; // use the window manager, always (default)
    xswa.event_mask  = X11_MOUSE_EVENT_MASK;
    xswa.event_mask |= KeyPressMask | KeyReleaseMask ;
    xswa.event_mask |= FocusChangeMask | SubstructureNotifyMask | StructureNotifyMask | ExposureMask;
    // xswa.event_mask |= VisibilityChangeMask;

    {
        int _x = x, _y = y; // pos for CreateWindow, might be tweaked
        if( TST_FLAG_IS_AUTOPOSITION(flags) ) {
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
    // XClearWindow(dpy, window);

    XSetWMProtocols(dpy, window, &wm_delete_atom, 1); // windowDeleteAtom
    javaWindow = createJavaWindowProperty(env, dpy, root, window, javaObjectAtom, windowDeleteAtom, obj, verbose);

    NewtWindows_setWindowTypeEWMH(dpy, javaWindow, _NET_WM_WINDOW_TYPE_NORMAL_IDX);
    NewtWindows_setDecorations(dpy, javaWindow, TST_FLAG_IS_UNDECORATED(flags) ? False : True );

    // since native creation happens at setVisible(true) .. 
    // we can pre-map the window here to be able to gather the insets and position.
    {
        XEvent event;
        // insets: negative values are ignored
        int left=-1, right=-1, top=-1, bottom=-1;
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
        javaWindow->isMapped=True;

        if( JNI_FALSE == pixels_is_direct && NULL != pixelPtr ) {
            (*env)->ReleasePrimitiveArrayCritical(env, pixels, (void*)pixelPtr, JNI_ABORT);  
        }

        // send insets before visibility, allowing java code a proper sync point!
        XSync(dpy, False);
        if( NewtWindows_updateInsets(dpy, javaWindow, &left, &right, &top, &bottom) ) {
            (*env)->CallVoidMethod(env, javaWindow->jwindow, insetsVisibleChangedID, JNI_FALSE, left, right, top, bottom, 1);
        } else {
            (*env)->CallVoidMethod(env, javaWindow->jwindow, visibleChangedID, JNI_FALSE, JNI_TRUE);
            left=0; right=0; top=0; bottom=0;
        }

        if( TST_FLAG_IS_AUTOPOSITION(flags) ) {
            // get position from WM
            int dest_x, dest_y;
            Window child;
            XTranslateCoordinates(dpy, window, windowParent, 0, 0, &dest_x, &dest_y, &child);
            x = (int)dest_x; y = (int)dest_y;
        }
        DBG_PRINT("X11: [CreateWindow]: client: %d/%d %dx%d, autoPos %d\n", x, y, width, height, TST_FLAG_IS_AUTOPOSITION(flags));

        x -= left; // top-level
        y -= top;  // top-level
        DBG_PRINT("X11: [CreateWindow]: top-level: %d/%d\n", x, y);
        NewtWindows_setPosSize(dpy, javaWindow, x, y, width, height);

        if( TST_FLAG_IS_ALWAYSONTOP(flags) ) {
            NewtWindows_setStackingEWMHFlags(dpy, root, javaWindow, _MASK_NET_WM_STATE_ABOVE, True);
        } else if( TST_FLAG_IS_ALWAYSONBOTTOM(flags) ) {
            NewtWindows_setStackingEWMHFlags(dpy, root, javaWindow, _MASK_NET_WM_STATE_BELOW, True);
        }
        if( TST_FLAG_IS_STICKY(flags) ) {
            NewtWindows_setStackingEWMHFlags(dpy, root, javaWindow, _MASK_NET_WM_STATE_STICKY, True);
        }
        if( TST_FLAG_IS_MAXIMIZED_ANY(flags) ) {
            int cmd = 0;
            if( TST_FLAG_IS_MAXIMIZED_VERT(flags) ) {
                cmd = _MASK_NET_WM_STATE_MAXIMIZED_VERT;
            } 
            if( TST_FLAG_IS_MAXIMIZED_HORZ(flags) ) {
                cmd |= _MASK_NET_WM_STATE_MAXIMIZED_HORZ;
            } 
            NewtWindows_setStackingEWMHFlags(dpy, root, javaWindow, cmd, True);
        }
        if( !TST_FLAG_IS_RESIZABLE(flags) ) {
            NewtWindows_setMinMaxSize(dpy, javaWindow, width, height, width, height);
        }
    }
    XFlush(dpy);
    handles[0] = (jlong)(intptr_t)window;
    handles[1] = (jlong)(intptr_t)javaWindow;
    jhandles = (*env)->NewLongArray(env, 2);
    if (jhandles == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate long array of size 2");
    }
    (*env)->SetLongArrayRegion(env, jhandles, 0, 2, handles);
    DBG_PRINT( "X11: [CreateWindow] created window %p -> %p on display %p\n", (void*)window, (void*)javaWindow, dpy);
    return jhandles;
}

/*
 * Class:     jogamp_newt_driver_x11_WindowDriver
 * Method:    GetSupportedReconfigMask0
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_WindowDriver_GetSupportedReconfigMask0
  (JNIEnv *env, jclass clazz, jlong javaWindow)
{
    JavaWindow * jw = (JavaWindow*)(intptr_t)javaWindow;
    uint32_t supported = jw->supportedAtoms;
    return 
        FLAG_IS_VISIBLE |
        FLAG_IS_AUTOPOSITION |
        FLAG_IS_CHILD |
        FLAG_IS_FOCUSED |
        FLAG_IS_UNDECORATED |
        ( ( 0 != ( _MASK_NET_WM_STATE_ABOVE & supported ) ) ? FLAG_IS_ALWAYSONTOP : 0 ) |
        ( ( 0 != ( _MASK_NET_WM_STATE_BELOW & supported ) ) ? FLAG_IS_ALWAYSONBOTTOM : 0 ) |
        ( ( 0 != ( _MASK_NET_WM_DESKTOP & supported ) ) ? FLAG_IS_STICKY : 0 ) |
        FLAG_IS_RESIZABLE |
        ( ( 0 != ( _MASK_NET_WM_STATE_MAXIMIZED_VERT & supported ) ) ? FLAG_IS_MAXIMIZED_VERT : 0 ) |
        ( ( 0 != ( _MASK_NET_WM_STATE_MAXIMIZED_HORZ & supported ) ) ? FLAG_IS_MAXIMIZED_HORZ : 0 ) |
        FLAG_IS_FULLSCREEN |
        FLAG_IS_POINTERVISIBLE |
        FLAG_IS_POINTERCONFINED |
        FLAG_IS_FULLSCREEN_SPAN;
}

/*
 * Class:     jogamp_newt_driver_x11_WindowDriver
 * Method:    CloseWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_WindowDriver_CloseWindow0
  (JNIEnv *env, jobject obj, jlong display, jlong javaWindow /*, jlong kbdHandle*/, // XKB disabled for now
                             jint randr_event_base, jint randr_error_base)
{
    Display * dpy = (Display *) (intptr_t) display;
    JavaWindow * jw, * jw0;
    XWindowAttributes xwa;

    DBG_PRINT( "X11: CloseWindow START dpy %p, win %p\n", (void*)dpy, (void*)javaWindow);
    if(dpy==NULL) {
        NewtCommon_FatalError(env, "invalid display connection..");
    }
    jw = (JavaWindow*)(intptr_t)javaWindow;
    if(jw==NULL) {
        NewtCommon_FatalError(env, "invalid JavaWindow connection..");
    }
    jw0 = getJavaWindowProperty(env, dpy, jw->window, jw->javaObjectAtom, True);
    if(NULL==jw) {
        NewtCommon_throwNewRuntimeException(env, "could not fetch Java Window object, bail out!");
        return;
    }
    if ( jw != jw0 ) {
        NewtCommon_throwNewRuntimeException(env, "Internal Error .. JavaWindow not the same!");
        return;
    }
    if ( JNI_FALSE == (*env)->IsSameObject(env, jw->jwindow, obj) ) {
        NewtCommon_throwNewRuntimeException(env, "Internal Error .. Window global ref not the same!");
        return;
    }

    XSync(dpy, False);
    memset(&xwa, 0, sizeof(XWindowAttributes));
    XGetWindowAttributes(dpy, jw->window, &xwa); // prefetch colormap to be destroyed after window destruction
    XSelectInput(dpy, jw->window, 0);
    XUnmapWindow(dpy, jw->window);
    jw->isMapped=False;

    // Drain all events related to this window ..
    Java_jogamp_newt_driver_x11_DisplayDriver_DispatchMessages0(env, obj, display, 
                                     (jlong)(intptr_t)jw->javaObjectAtom, (jlong)(intptr_t)jw->windowDeleteAtom /*, kbdHandle */, // XKB disabled for now
                                     randr_event_base, randr_error_base);

    XDestroyWindow(dpy, jw->window);
    if( None != xwa.colormap ) {
        XFreeColormap(dpy, xwa.colormap);
    }
    XSync(dpy, True); // discard all events now, no more handler

    destroyJavaWindow(env, jw);

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
  (JNIEnv *env, jclass clazz, jlong jdisplay, jint screen_index,
   jlong jparent, jlong javaWindow,
   jint x, jint y, jint width, jint height, jint flags)
{
    Display * dpy = (Display *) (intptr_t) jdisplay;
    JavaWindow *jw = (JavaWindow*)(intptr_t)javaWindow;
    Atom wm_delete_atom = jw->windowDeleteAtom;
    Window root = RootWindow(dpy, screen_index);
    Window parent = (0!=jparent)?(Window)jparent:root;
    XEvent event;
    Bool isVisible = !TST_FLAG_CHANGE_VISIBILITY(flags) && TST_FLAG_IS_VISIBLE(flags) ;
    Bool tempInvisible = ( TST_FLAG_CHANGE_FULLSCREEN(flags) || TST_FLAG_CHANGE_PARENTING(flags) ) && isVisible ;
    // Bool tempInvisible = TST_FLAG_CHANGE_PARENTING(flags) && isVisible ;
    int fsEWMHFlags = 0;
    if( TST_FLAG_CHANGE_FULLSCREEN(flags) ) {
        if( !TST_FLAG_IS_FULLSCREEN_SPAN(flags) ) {      // doesn't work w/ spanning across monitors. See also Bug 770 & Bug 771
            fsEWMHFlags |= _MASK_NET_WM_STATE_FULLSCREEN;
        }
        if( TST_FLAG_IS_FULLSCREEN(flags) ) {
            if( TST_FLAG_IS_ALWAYSONTOP(flags) ) {
                fsEWMHFlags |= _MASK_NET_WM_STATE_ABOVE; // fs on,  above on
            } else if( TST_FLAG_IS_ALWAYSONBOTTOM(flags) ) {
                fsEWMHFlags |= _MASK_NET_WM_STATE_BELOW; // fs on,  below on
            } // else { }                                // fs on,  above off
        } else if( !TST_FLAG_IS_ALWAYSONTOP(flags) ) {
            fsEWMHFlags |= _MASK_NET_WM_STATE_ABOVE;     // fs off, above off
        } else if( !TST_FLAG_IS_ALWAYSONBOTTOM(flags) ) {
            fsEWMHFlags |= _MASK_NET_WM_STATE_BELOW;     // fs off, below off
        } // else { }                                    // fs off, above/below on
    } else if( TST_FLAG_CHANGE_PARENTING(flags) ) {
        // Fix for Unity WM, i.e. _remove_ persistent previous states
        fsEWMHFlags |= _MASK_NET_WM_STATE_FULLSCREEN;    // fs off
        if( !TST_FLAG_IS_ALWAYSONTOP(flags) ) {
            fsEWMHFlags |= _MASK_NET_WM_STATE_ABOVE;     // above off
        } else if( !TST_FLAG_IS_ALWAYSONBOTTOM(flags) ) {
            fsEWMHFlags |= _MASK_NET_WM_STATE_BELOW;     // below off
        }
    } else if( TST_FLAG_CHANGE_ALWAYSONTOP(flags) ) {
        fsEWMHFlags |= _MASK_NET_WM_STATE_ABOVE;         // toggle above
    } else if( TST_FLAG_CHANGE_ALWAYSONBOTTOM(flags) ) {
        fsEWMHFlags |= _MASK_NET_WM_STATE_BELOW;         // toggle below
    }

    DBG_PRINT( "X11: reconfigureWindow0 dpy %p, scrn %d, parent %p/%p, win %p, %d/%d %dx%d, parentChange %d, isChild %d, undecorated[change %d, val %d], fullscreen[change %d, val %d (span %d)], alwaysOn[Top[change %d, val %d], Bottom[change %d, val %d]], visible[change %d, val %d, tempInvisible %d], resizable[change %d, val %d], sticky[change %d, val %d], fsEWMHFlags %d\n",
        (void*)dpy, screen_index, (void*) jparent, (void*)parent, (void*)jw->window,
        x, y, width, height, 
        TST_FLAG_CHANGE_PARENTING(flags),   TST_FLAG_IS_CHILD(flags),
        TST_FLAG_CHANGE_DECORATION(flags),  TST_FLAG_IS_UNDECORATED(flags),
        TST_FLAG_CHANGE_FULLSCREEN(flags),  TST_FLAG_IS_FULLSCREEN(flags), TST_FLAG_IS_FULLSCREEN_SPAN(flags),
        TST_FLAG_CHANGE_ALWAYSONTOP(flags), TST_FLAG_IS_ALWAYSONTOP(flags), 
        TST_FLAG_CHANGE_ALWAYSONBOTTOM(flags), TST_FLAG_IS_ALWAYSONBOTTOM(flags), 
        TST_FLAG_CHANGE_VISIBILITY(flags),  TST_FLAG_IS_VISIBLE(flags), tempInvisible, 
        TST_FLAG_CHANGE_RESIZABLE(flags), TST_FLAG_IS_RESIZABLE(flags), 
        TST_FLAG_CHANGE_STICKY(flags), TST_FLAG_IS_STICKY(flags),
        fsEWMHFlags);

    XSync(dpy, False);

    // FS Note: To toggle FS, utilizing the _NET_WM_STATE_FULLSCREEN WM state should be enough.
    //          However, we have to consider other cases like reparenting and WM which don't support it.
    #if 0    // Also doesn't work work properly w/ Unity WM
    if( fsEWMHFlags && !TST_FLAG_CHANGE_PARENTING(flags) && isVisible &&
        !TST_FLAG_IS_FULLSCREEN_SPAN(flags) &&
        ( TST_FLAG_CHANGE_FULLSCREEN(flags) || TST_FLAG_CHANGE_ALWAYSONTOP(flags) ) ) {
        Bool enable = TST_FLAG_CHANGE_FULLSCREEN(flags) ? TST_FLAG_IS_FULLSCREEN(flags) : TST_FLAG_IS_ALWAYSONTOP(flags) ;
        NewtWindows_setStackingEWMHFlags(dpy, root, jw, fsEWMHFlags, enable);
        if ( TST_FLAG_CHANGE_FULLSCREEN(flags) && !TST_FLAG_IS_FULLSCREEN(flags) ) { // FS off - restore decoration
            NewtWindows_setDecorations (dpy, jw, TST_FLAG_IS_UNDECORATED(flags) ? False : True);
        }
        DBG_PRINT( "X11: reconfigureWindow0 X (fs.atop.fast)\n");
        return;
    }
    #endif
    // Toggle ALWAYSONTOP/BOTTOM (only) w/o visibility or window stacking sideffects
    if( isVisible && fsEWMHFlags && TST_FLAG_CHANGE_ALWAYSONANY(flags) &&
        !TST_FLAG_CHANGE_PARENTING(flags) && !TST_FLAG_CHANGE_FULLSCREEN(flags) && !TST_FLAG_CHANGE_RESIZABLE(flags) ) {
        NewtWindows_setStackingEWMHFlags(dpy, root, jw, fsEWMHFlags, TST_FLAG_IS_ALWAYSONANY(flags));
        DBG_PRINT( "X11: reconfigureWindow0 X (atop.fast)\n");
        return;
    }

    if( tempInvisible ) {
        DBG_PRINT( "X11: reconfigureWindow0 TEMP VISIBLE OFF\n");
        NewtWindows_setVisible(dpy, root, jw, False /* visible */, False /* useWM */, True /* wait */);
        // no need to notify the java side .. just temp change
    }

    if( fsEWMHFlags && ( ( TST_FLAG_CHANGE_FULLSCREEN(flags)  && !TST_FLAG_IS_FULLSCREEN(flags) ) ||           // FS off
                         ( TST_FLAG_CHANGE_ALWAYSONTOP(flags) && !TST_FLAG_IS_ALWAYSONTOP(flags) ) ||          // AlwaysOnTop off
                         ( TST_FLAG_CHANGE_ALWAYSONBOTTOM(flags) && !TST_FLAG_IS_ALWAYSONBOTTOM(flags) ) ) ) { // AlwaysOnBottom off
        NewtWindows_setStackingEWMHFlags(dpy, root, jw, fsEWMHFlags, False);
    }

    if( TST_FLAG_CHANGE_PARENTING(flags) && !TST_FLAG_IS_CHILD(flags) ) {
        // TOP: in -> out
        DBG_PRINT( "X11: reconfigureWindow0 PARENTING in->out\n");
        XReparentWindow( dpy, jw->window, parent, x, y ); // actual reparent call
        #ifdef REPARENT_WAIT_FOR_REPARENT_NOTIFY
            XIfEvent( dpy, &event, WaitForReparentNotify, (XPointer) jw->window );
        #else
            XSync(dpy, False);
        #endif
        XSetWMProtocols(dpy, jw->window, &wm_delete_atom, 1); // windowDeleteAtom
        // Fix for Unity WM, i.e. _remove_ persistent previous states
        NewtWindows_setStackingEWMHFlags(dpy, root, jw, fsEWMHFlags, False);
    }

    if( TST_FLAG_CHANGE_DECORATION(flags) ) {
        DBG_PRINT( "X11: reconfigureWindow0 DECORATIONS %d\n", !TST_FLAG_IS_UNDECORATED(flags));
        NewtWindows_setDecorations (dpy, jw, TST_FLAG_IS_UNDECORATED(flags) ? False : True);
    }

    if( TST_FLAG_CHANGE_MAXIMIZED_ANY(flags) ) {
        int cmd = 0;
        if( TST_FLAG_CHANGE_MAXIMIZED_VERT(flags) && 
            TST_FLAG_CHANGE_MAXIMIZED_HORZ(flags) &&
            TST_FLAG_IS_MAXIMIZED_VERT(flags) == TST_FLAG_IS_MAXIMIZED_HORZ(flags) ) {
            // max-both on or off
            cmd = _MASK_NET_WM_STATE_MAXIMIZED_VERT | 
                  _MASK_NET_WM_STATE_MAXIMIZED_HORZ;
            NewtWindows_setStackingEWMHFlags(dpy, root, jw, cmd, TST_FLAG_IS_MAXIMIZED_ANY(flags)?True:False);
        } else {
            // max each on or off
            if( TST_FLAG_CHANGE_MAXIMIZED_VERT(flags) ) {
                cmd = _MASK_NET_WM_STATE_MAXIMIZED_VERT;
                NewtWindows_setStackingEWMHFlags(dpy, root, jw, cmd, TST_FLAG_IS_MAXIMIZED_VERT(flags)?True:False);
            } 
            if( TST_FLAG_CHANGE_MAXIMIZED_HORZ(flags) ) {
                cmd = _MASK_NET_WM_STATE_MAXIMIZED_HORZ;
                NewtWindows_setStackingEWMHFlags(dpy, root, jw, cmd, TST_FLAG_IS_MAXIMIZED_HORZ(flags)?True:False);
            } 
        }
    } else {
        if( !TST_FLAG_IS_MAXIMIZED_ANY(flags) ) {
            DBG_PRINT( "X11: reconfigureWindow0 setPosSize %d/%d %dx%d\n", x, y, width, height);
            NewtWindows_setPosSize(dpy, jw, x, y, width, height);
        }
    }

    if( TST_FLAG_CHANGE_PARENTING(flags) && TST_FLAG_IS_CHILD(flags) ) {
        // CHILD: out -> in
        DBG_PRINT( "X11: reconfigureWindow0 PARENTING out->in\n");
        XReparentWindow( dpy, jw->window, parent, x, y ); // actual reparent call
        XFlush(dpy);
        #ifdef REPARENT_WAIT_FOR_REPARENT_NOTIFY
            XIfEvent( dpy, &event, WaitForReparentNotify, (XPointer) jw->window );
        #else
            XSync(dpy, False);
        #endif
    }

    if( tempInvisible ) {
        DBG_PRINT( "X11: reconfigureWindow0 TEMP VISIBLE ON\n");
        NewtWindows_setVisible(dpy, root, jw, True /* visible */, False /* useWM */, True /* wait */);
        // no need to notify the java side .. just temp change
    } else if( TST_FLAG_CHANGE_VISIBILITY(flags) ) {
        Bool useWM = ( TST_FLAG_CHANGE_VISIBILITY_FAST(flags) || TST_FLAG_IS_CHILD(flags) ) ? False : True;
        if( TST_FLAG_IS_VISIBLE(flags) ) {
            DBG_PRINT( "X11: reconfigureWindow0 VISIBLE ON\n");
            NewtWindows_setVisible(dpy, root, jw, True /* visible */, useWM, False /* wait */);
            if( !TST_FLAG_IS_MAXIMIZED_ANY(flags) ) {
                // WM may disregard pos/size XConfigureWindow requests for invisible windows!
                DBG_PRINT( "X11: reconfigureWindow0 setPosSize.2 %d/%d %dx%d\n", x, y, width, height);
                NewtWindows_setPosSize(dpy, jw, x, y, width, height);
            }
        } else {
            DBG_PRINT( "X11: reconfigureWindow0 VISIBLE OFF\n");
            NewtWindows_setVisible(dpy, root, jw, False /* visible */, useWM, False /* wait */);
        }
    }

    if( fsEWMHFlags && ( ( TST_FLAG_CHANGE_FULLSCREEN(flags)  && TST_FLAG_IS_FULLSCREEN(flags) ) ||           // FS on
                         ( TST_FLAG_CHANGE_ALWAYSONTOP(flags) && TST_FLAG_IS_ALWAYSONTOP(flags) ) ||          // AlwaysOnTop on
                         ( TST_FLAG_CHANGE_ALWAYSONBOTTOM(flags) && TST_FLAG_IS_ALWAYSONBOTTOM(flags) ) ) ) { // AlwaysOnBottom on
        NewtWindows_requestFocus (dpy, jw, True);
        NewtWindows_setStackingEWMHFlags(dpy, root, jw, fsEWMHFlags, True);
    }
    if( TST_FLAG_CHANGE_STICKY(flags) ) {
        NewtWindows_setStackingEWMHFlags(dpy, root, jw, _MASK_NET_WM_STATE_STICKY, TST_FLAG_IS_STICKY(flags)?True:False);
    }
    if( TST_FLAG_CHANGE_RESIZABLE(flags) ) {
        if( !TST_FLAG_IS_RESIZABLE(flags) ) {
            NewtWindows_setMinMaxSize(dpy, jw, width, height, width, height);
        } else {
            // NewtWindows_setMinMaxSize(dpy, jw, 0, 0, 32767, 32767); // FIXME: ..
            NewtWindows_setMinMaxSize(dpy, jw, -1, -1, -1, -1); // FIXME: ..
        }
    }
    XFlush(dpy);
    DBG_PRINT( "X11: reconfigureWindow0 X (full)\n");
}

/*
 * Class:     jogamp_newt_driver_x11_WindowDriver
 * Method:    requestFocus0
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_WindowDriver_requestFocus0
  (JNIEnv *env, jclass clazz, jlong display, jlong javaWindow, jboolean force)
{
    Display * dpy = (Display *) (intptr_t) display;
    XSync(dpy, False);
    NewtWindows_requestFocus ( dpy, (JavaWindow*)(intptr_t)javaWindow, JNI_TRUE==force?True:False ) ;
}

/*
 * Class:     Java_jogamp_newt_driver_x11_WindowDriver
 * Method:    setTitle0
 * Signature: (JJLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_WindowDriver_setTitle0
  (JNIEnv *env, jclass clazz, jlong display, jlong javaWindow, jstring title)
{
    Display * dpy = (Display *) (intptr_t) display;
    JavaWindow *jw = (JavaWindow*)(intptr_t)javaWindow;

#if 1
    const char* title_str;
    if (NULL != title) {
        title_str = (*env)->GetStringUTFChars(env, title, NULL);
        if(NULL != title_str) {
            DBG_PRINT( "X11: setTitle: <%s> SET\n", title_str);
            XStoreName(dpy, jw->window, title_str);
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
                XSetWMName(dpy, jw->window, &text_prop);
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
  (JNIEnv *env, jclass clazz, jlong display, jlong javaWindow, jlong handle)
{
    Display * dpy = (Display *) (intptr_t) display;
    JavaWindow *jw = (JavaWindow*)(intptr_t)javaWindow;

    if( 0 == handle ) {
        DBG_PRINT( "X11: setPointerIcon0: reset\n");
        XUndefineCursor(dpy, jw->window);
    } else {
        Cursor c = (Cursor) (intptr_t) handle;
        DBG_PRINT( "X11: setPointerIcon0: %p\n", (void*)c);
        XDefineCursor(dpy, jw->window, c);
    }
}

/*
 * Class:     Java_jogamp_newt_driver_x11_WindowDriver
 * Method:    setPointerVisible0
 * Signature: (JJZ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_WindowDriver_setPointerVisible0
  (JNIEnv *env, jclass clazz, jlong display, jlong javaWindow, jboolean mouseVisible)
{
    static char noData[] = { 0,0,0,0,0,0,0,0 };
    static XColor black = { 0 };

    Display * dpy = (Display *) (intptr_t) display;
    JavaWindow *jw = (JavaWindow*)(intptr_t)javaWindow;

    DBG_PRINT( "X11: setPointerVisible0: %d\n", mouseVisible);

    if(JNI_TRUE == mouseVisible) {
        XUndefineCursor(dpy, jw->window);
    } else {
        Pixmap bitmapNoData;
        Cursor invisibleCursor;

        bitmapNoData = XCreateBitmapFromData(dpy, jw->window, noData, 8, 8);
        if(None == bitmapNoData) {
            return JNI_FALSE;
        }
        invisibleCursor = XCreatePixmapCursor(dpy, bitmapNoData, bitmapNoData, &black, &black, 0, 0);
        XDefineCursor(dpy, jw->window, invisibleCursor);
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
  (JNIEnv *env, jclass clazz, jlong display, jlong javaWindow, jboolean confine)
{
    Display * dpy = (Display *) (intptr_t) display;
    JavaWindow *jw = (JavaWindow*)(intptr_t)javaWindow;

    DBG_PRINT( "X11: confinePointer0: %d\n", confine);

    if(JNI_TRUE == confine) {
        return GrabSuccess == XGrabPointer(dpy, jw->window, True, 
                                           X11_MOUSE_EVENT_MASK,
                                           GrabModeAsync, GrabModeAsync, jw->window, None, CurrentTime)
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
  (JNIEnv *env, jclass clazz, jlong display, jlong javaWindow, jint x, jint y)
{
    Display * dpy = (Display *) (intptr_t) display;
    JavaWindow *jw = (JavaWindow*)(intptr_t)javaWindow;

    DBG_PRINT( "X11: warpPointer0: %d/%d\n", x, y);

    XWarpPointer(dpy, None, jw->window, 0, 0, 0, 0, x, y);
}

