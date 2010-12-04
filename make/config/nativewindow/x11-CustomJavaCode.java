
  /** Interface to C language function: <br> <code> XVisualInfo *  XGetVisualInfo(Display * , long, XVisualInfo * , int * ); </code>    */
  public static XVisualInfo[] XGetVisualInfo(long arg0, long arg1, XVisualInfo arg2, int[] arg3, int arg3_offset)
  {
    if(arg3 != null && arg3.length <= arg3_offset)
      throw new RuntimeException("array offset argument \"arg3_offset\" (" + arg3_offset + ") equals or exceeds array length (" + arg3.length + ")");
    java.nio.ByteBuffer _res;
    _res = XGetVisualInfo1(arg0, arg1, ((arg2 == null) ? null : arg2.getBuffer()), arg3, Buffers.SIZEOF_INT * arg3_offset);

    if (_res == null) return null;
    Buffers.nativeOrder(_res);
    XVisualInfo[] _retarray = new XVisualInfo[getFirstElement(arg3, arg3_offset)];
    for (int _count = 0; _count < getFirstElement(arg3, arg3_offset); _count++) {
      _res.position(_count * XVisualInfo.size());
      _res.limit   ((1 + _count) * XVisualInfo.size());
      java.nio.ByteBuffer _tmp = _res.slice();
      _res.position(0);
      _res.limit(_res.capacity());
      _retarray[_count] = XVisualInfo.create(_tmp);
    }
    return _retarray;
  }

  /** Entry point to C language function: <code> XVisualInfo *  XGetVisualInfo(Display * , long, XVisualInfo * , int * ); </code>    */
  private static native java.nio.ByteBuffer XGetVisualInfo1(long arg0, long arg1, java.nio.ByteBuffer arg2, Object arg3, int arg3_byte_offset);

  public static native long DefaultVisualID(long display, int screen);

  public static native long CreateDummyWindow(long display, int screen_index, long visualID, int width, int height);
  public static native void DestroyDummyWindow(long display, long window);

  public static Point GetRelativeLocation(long display, int screen_index, long src_win, long dest_win, int src_x, int src_y) {
    return (Point) GetRelativeLocation0(display, screen_index, src_win, dest_win, src_x, src_y);
  }
  private static native Object GetRelativeLocation0(long display, int screen_index, long src_win, long dest_win, int src_x, int src_y);

  public static native int XCloseDisplay(long display);
  public static native void XUnlockDisplay(long display);
  public static native void XLockDisplay(long display);

