
  /** Interface to C language function: <br> <code> XVisualInfo *  XGetVisualInfo(Display * , long, XVisualInfo * , int * ); </code>    */
  public static XVisualInfo[] XGetVisualInfoCopied(long arg0, long arg1, XVisualInfo arg2, int[] arg3, int arg3_offset)
  {
    if(arg3 != null && arg3.length <= arg3_offset)
      throw new RuntimeException("array offset argument \"arg3_offset\" (" + arg3_offset + ") equals or exceeds array length (" + arg3.length + ")");
    java.nio.ByteBuffer _res;
    _res = XGetVisualInfoCopied1(arg0, arg1, ((arg2 == null) ? null : arg2.getBuffer()), arg3, BufferFactory.SIZEOF_INT * arg3_offset);

    if (_res == null) return null;
    BufferFactory.nativeOrder(_res);
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
  private static native java.nio.ByteBuffer XGetVisualInfoCopied1(long arg0, long arg1, java.nio.ByteBuffer arg2, Object arg3, int arg3_byte_offset);

