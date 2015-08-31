
  /** Interface to C language function: <br> <code> XRenderPictFormat *  XRenderFindVisualFormat(Display *  dpy, const Visual *  visual); </code>    */
  public static boolean XRenderFindVisualFormat(long dpy, long visual, XRenderPictFormat dest)  {
    if( dest == null ) {
        throw new RuntimeException("dest is null");
    }
    final ByteBuffer destBuffer = dest.getBuffer();
    if( !Buffers.isDirect(destBuffer) ) {
        throw new RuntimeException("dest buffer is not direct");
    }
    return XRenderFindVisualFormat1(dpy, visual, destBuffer);
  }
  /** Entry point to C language function: <code> XVisualInfo *  XGetVisualInfo(Display * , long, XVisualInfo * , int * ); </code>    */
  private static native boolean XRenderFindVisualFormat1(long dpy, long visual, ByteBuffer xRenderPictFormat);

  
  /** Interface to C language function: <br> <code> XVisualInfo *  XGetVisualInfo(Display * , long, XVisualInfo * , int * ); </code>    */
  public static XVisualInfo[] XGetVisualInfo(long arg0, long arg1, XVisualInfo arg2, int[] arg3, int arg3_offset)
  {
    if(arg3 != null && arg3.length <= arg3_offset)
      throw new RuntimeException("array offset argument \"arg3_offset\" (" + arg3_offset + ") equals or exceeds array length (" + arg3.length + ")");
    java.nio.ByteBuffer _res;
    _res = XGetVisualInfo1(arg0, arg1, ((arg2 == null) ? null : arg2.getBuffer()), arg3, Buffers.SIZEOF_INT * arg3_offset);

    if (_res == null) return null;
    Buffers.nativeOrder(_res);
    final int count = getFirstElement(arg3, arg3_offset);
    if (count <= 0) return null;
    final int esize = _res.capacity() / count;
    if( esize < XVisualInfo.size() ) {
        throw new RuntimeException("element-size "+_res.capacity()+"/"+count+"="+esize+" < "+XVisualInfo.size());
    }
    XVisualInfo[] _retarray = new XVisualInfo[count];
    for (int i = 0; i < count; i++) {
      _res.position(i * esize); // XVisualInfo.size());
      _res.limit   ((1 + i) * esize); // XVisualInfo.size());
      java.nio.ByteBuffer _tmp = _res.slice();
      _res.position(0);
      _res.limit(_res.capacity());
      _retarray[i] = XVisualInfo.create(_tmp);
    }
    return _retarray;
  }

  /** Entry point to C language function: <code> XVisualInfo *  XGetVisualInfo(Display * , long, XVisualInfo * , int * ); </code>    */
  private static native java.nio.ByteBuffer XGetVisualInfo1(long arg0, long arg1, java.nio.ByteBuffer arg2, Object arg3, int arg3_byte_offset);

  public static native int GetVisualIDFromWindow(long display, long window);

  public static native int DefaultVisualID(long display, int screen);

  public static native long CreateWindow(long parent, long display, int screen_index, int visualID, int width, int height, boolean input, boolean visible);
  public static native void DestroyWindow(long display, long window);
  public static native void SetWindowPosSize(long display, long window, int x, int y, int width, int height);

  public static Point GetRelativeLocation(long display, int screen_index, long src_win, long dest_win, int src_x, int src_y) {
    return (Point) GetRelativeLocation0(display, screen_index, src_win, dest_win, src_x, src_y);
  }
  private static native Object GetRelativeLocation0(long display, int screen_index, long src_win, long dest_win, int src_x, int src_y);

  public static boolean QueryExtension(long display, String extensionName) {
    return QueryExtension0(display, extensionName);
  }
  private static native boolean QueryExtension0(long display, String extensionName);

  public static native int XCloseDisplay(long display);
  public static native void XUnlockDisplay(long display);
  public static native void XLockDisplay(long display);

