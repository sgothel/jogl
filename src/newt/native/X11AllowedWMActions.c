
#ifdef VERBOSE_ON
    #define _NET_WM_ACTION_FLAG_RESERVED         ( 1 <<  0 ) /* for the ATOM COMMAND */
    #define _NET_WM_ACTION_FLAG_MOVE             ( 1 <<  1 )
    #define _NET_WM_ACTION_FLAG_RESIZE           ( 1 <<  2 )
    #define _NET_WM_ACTION_FLAG_MINIMIZE         ( 1 <<  3 )
    #define _NET_WM_ACTION_FLAG_SHADE            ( 1 <<  4 )
    #define _NET_WM_ACTION_FLAG_STICK            ( 1 <<  5 )
    #define _NET_WM_ACTION_FLAG_MAXIMIZE_HORZ    ( 1 <<  6 )
    #define _NET_WM_ACTION_FLAG_MAXIMIZE_VERT    ( 1 <<  7 )
    #define _NET_WM_ACTION_FLAG_FULLSCREEN       ( 1 <<  8 )
    #define _NET_WM_ACTION_FLAG_CHANGE_DESKTOP   ( 1 <<  9 )
    #define _NET_WM_ACTION_FLAG_CLOSE            ( 1 << 10 )
    #define _NET_WM_ACTION_FLAG_ABOVE            ( 1 << 11 )
    #define _NET_WM_ACTION_FLAG_BELOW            ( 1 << 12 )
    #define _NET_WM_ACTION_FLAG_ALL              ( ( 1 << 13 ) - 1 )
    static const char * _NET_WM_AACTION_NAMES[] = { 
        "_NET_WM_ALLOWED_ACTIONS",
        "_NET_WM_ACTION_MOVE",
        "_NET_WM_ACTION_RESIZE",
        "_NET_WM_ACTION_MINIMIZE",
        "_NET_WM_ACTION_SHADE",
        "_NET_WM_ACTION_STICK",
        "_NET_WM_ACTION_MAXIMIZE_HORZ",
        "_NET_WM_ACTION_MAXIMIZE_VERT",
        "_NET_WM_ACTION_FULLSCREEN",
        "_NET_WM_ACTION_CHANGE_DESKTOP",
        "_NET_WM_ACTION_CLOSE",
        "_NET_WM_ACTION_ABOVE",
        "_NET_WM_ACTION_BELOW"
    };
    static const int _NET_WM_AACTION_COUNT = sizeof(_NET_WM_AACTION_NAMES)/sizeof(const char *);
    static const int _UNDEF_AACTION_MAX = 16;
    static int NewtWindows_getAllowedWindowActionEWMH(Display *dpy, const Atom * allDefinedActions, const Atom action, const int num) {
        int i;
        for(i=1; i<_NET_WM_AACTION_COUNT; i++) {
            if( action == allDefinedActions[i] ) {
                DBG_PRINT( "...... [%d] -> [%d/%d]: %s\n", num, i, _NET_WM_AACTION_COUNT, _NET_WM_AACTION_NAMES[i]);
                return 1 << i;
            }
        }
    #ifdef VERBOSE_ON
        char * astr = XGetAtomName(dpy, action);
        DBG_PRINT( "...... [%d] -> [_/%d]: %s (undef)\n", num, _NET_WM_AACTION_COUNT, astr);
        XFree(astr);
    #endif
        return 0;
    }
    static int NewtWindows_getAllowedWindowActionsEWMH1(Display *dpy, Window w, Atom * _NET_WM_AACTIONS) {
        Atom * actions = NULL;
        Atom type = 0;
        unsigned long action_len = 0, remain = 0;
        int res = 0, form = 0, i = 0;
        Status s;

        XSync(dpy, False);
        DBG_PRINT( "**************** X11: AACTIONS EWMH CHECK for Window: %p:\n", w);
        if ( Success == (s = XGetWindowProperty(dpy, w, _NET_WM_AACTIONS[0], 0, 1024, False, AnyPropertyType,
                                                &type, &form, &action_len, &remain, (unsigned char**)&actions)) ) {
            if( NULL != actions ) {
                for(i=0; i<action_len; i++) {
                    res |= NewtWindows_getAllowedWindowActionEWMH(dpy, _NET_WM_AACTIONS, actions[i], i);
                }
                XFree(actions);
            }
            DBG_PRINT( "**************** X11: AACTIONS EWMH CHECK: 0x%X\n", res);
        } else {
            DBG_PRINT( "**************** X11: AACTIONS EWMH CHECK: XGetWindowProperty failed: %d\n", s);
            res = _NET_WM_ACTION_FLAG_FULLSCREEN; // default ..
        }
        return res;
    }
    static int NewtWindows_getAllowedWindowActionsEWMH0(Display *dpy, Window w) {
        Atom _NET_WM_AACTIONS[_NET_WM_AACTION_COUNT];
        if( 0 == XInternAtoms( dpy, (char **)_NET_WM_AACTION_NAMES, _NET_WM_AACTION_COUNT, False, _NET_WM_AACTIONS) ) {
            // error
            DBG_PRINT( "**************** X11: AACTIONS EWMH CHECK: XInternAtoms failed\n");
            return _NET_WM_ACTION_FLAG_FULLSCREEN; // default ..
        }
        return NewtWindows_getAllowedWindowActionsEWMH1(dpy, w, _NET_WM_AACTIONS);
    }
    static int NewtWindows_setAllowedWindowActionsEWMH(Display *dpy, Window w, int new_action_mask) {
        Atom _NET_WM_AACTIONS[_NET_WM_AACTION_COUNT];
        if( 0 == XInternAtoms( dpy, (char **)_NET_WM_AACTION_NAMES, _NET_WM_AACTION_COUNT, False, _NET_WM_AACTIONS) ) {
            // error
            DBG_PRINT( "**************** X11: AACTIONS EWMH SET: XInternAtoms failed\n");
            return _NET_WM_ACTION_FLAG_FULLSCREEN; // default ..
        }
        Atom * actions = NULL;
        Atom type = 0;
        unsigned long action_len = 0, remain = 0;
        int res = 0, form = 0, i = 0, j = 0;
        Status s;
        Atom _NET_WM_NEWACTIONS[_NET_WM_AACTION_COUNT+_UNDEF_AACTION_MAX]; // +_UNDEF_AACTION_MAX undefined props

        DBG_PRINT( "**************** X11: AACTIONS EWMH SET for Window: %p:\n", w);
        if ( Success == (s = XGetWindowProperty(dpy, w, _NET_WM_AACTIONS[0], 0, 1024, False, AnyPropertyType,
                                                &type, &form, &action_len, &remain, (unsigned char**)&actions)) ) {
            if( NULL != actions ) {
                for(i=0; i<action_len; i++) {
                    const int r = NewtWindows_getAllowedWindowActionEWMH(dpy, _NET_WM_AACTIONS, actions[i], i);
                    if( 0 == r && j < _UNDEF_AACTION_MAX ) {
                        // conserve undefined action
                        _NET_WM_NEWACTIONS[j++] = actions[i];
                    }
                    res |= r;
                }
                XFree(actions);
            }
            DBG_PRINT( "**************** X11: AACTIONS EWMH SET: Has 0x%X\n", res);
            for(i=1; i<_NET_WM_AACTION_COUNT; i++) {
                const int m = 1 << i;
                if( 0 != ( m & new_action_mask ) ) {
                    // requested
                    _NET_WM_NEWACTIONS[j++] = _NET_WM_AACTIONS[i];
                    res |= m;
                }
            }
            DBG_PRINT( "**************** X11: AACTIONS EWMH SET: New 0x%X\n", res);
            XChangeProperty( dpy, w, _NET_WM_AACTIONS[0], XA_ATOM, 32, PropModeReplace, (unsigned char*)_NET_WM_NEWACTIONS, j); 
            XSync(dpy, False);
            const int res2 = NewtWindows_getAllowedWindowActionsEWMH1(dpy, w, _NET_WM_AACTIONS);
            DBG_PRINT( "**************** X11: AACTIONS EWMH SET: Val 0x%X\n", res2);
        } else {
            DBG_PRINT( "**************** X11: AACTIONS EWMH SET: XGetWindowProperty failed: %d\n", s);
            res = _NET_WM_ACTION_FLAG_FULLSCREEN; // default ..
        }
        return res;
    }
#endif

