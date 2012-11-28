Version 4.2.0
    Note: Version 4.3.0 M3 201210312000 fails to work on OpenIndiana,
        OS.GTK_WIDGET_WINDOW (handle) returns 0 !
          javax.media.nativewindow.NativeWindowException: Null gtk-window-handle of SWT handle 0x8490000
                at com.jogamp.nativewindow.swt.SWTAccessor.gdk_widget_get_window(SWTAccessor.java:308)
                at com.jogamp.nativewindow.swt.SWTAccessor.getDevice(SWTAccessor.java:335)
                at com.jogamp.newt.swt.NewtCanvasSWT.<init>(NewtCanvasSWT.java:124)
                at com.jogamp.newt.swt.NewtCanvasSWT$1.run(NewtCanvasSWT.java:99)

