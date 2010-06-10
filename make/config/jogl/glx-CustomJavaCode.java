
  /** Interface to C language function: <br> - Alias for: <br> <code> XVisualInfo *  glXGetVisualFromFBConfigSGIX, glXGetVisualFromFBConfig(Display *  dpy, GLXFBConfig config); </code>    */
  public static XVisualInfo glXGetVisualFromFBConfig(long dpy, long config)
  {
    final long __addr_ = glxProcAddressTable._addressof_glXGetVisualFromFBConfig;
    if (__addr_ == 0) {
        throw new GLException("Method \"glXGetVisualFromFBConfig\" not available");
    }
    java.nio.ByteBuffer _res;
    _res = dispatch_glXGetVisualFromFBConfig(dpy, config, __addr_);
    if (_res == null) return null;
    return XVisualInfo.create(_res);
  }

  /** Entry point to C language function: - Alias for: <br> <code> XVisualInfo *  glXGetVisualFromFBConfigSGIX, glXGetVisualFromFBConfig(Display *  dpy, GLXFBConfig config); </code>    */
  private static native java.nio.ByteBuffer dispatch_glXGetVisualFromFBConfig(long dpy, long config, long procAddr);


  /** Interface to C language function: <br> - Alias for: <br> <code> GLXFBConfig *  glXChooseFBConfigSGIX, glXChooseFBConfig(Display *  dpy, int screen, const int *  attribList, int *  nitems); </code>    */
  public static com.jogamp.common.nio.PointerBuffer glXChooseFBConfig(long dpy, int screen, int[] attribList, int attribList_offset, int[] nitems, int nitems_offset)
  {
    final long __addr_ = glxProcAddressTable._addressof_glXChooseFBConfig;
    if (__addr_ == 0) {
        throw new GLException("Method \"glXGetVisualFromFBConfig\" not available");
    }
    if(attribList != null && attribList.length <= attribList_offset)
      throw new GLException("array offset argument \"attribList_offset\" (" + attribList_offset + ") equals or exceeds array length (" + attribList.length + ")");
    if(nitems != null && nitems.length <= nitems_offset)
      throw new GLException("array offset argument \"nitems_offset\" (" + nitems_offset + ") equals or exceeds array length (" + nitems.length + ")");
    java.nio.ByteBuffer _res;
    _res = dispatch_glXChooseFBConfig(dpy, screen, attribList, Buffers.SIZEOF_INT * attribList_offset, nitems, Buffers.SIZEOF_INT * nitems_offset, __addr_);

    if (_res == null) return null;
    return PointerBuffer.wrap(_res);
  }

  /** Entry point to C language function: - Alias for: <br> <code> GLXFBConfig *  glXChooseFBConfigSGIX, glXChooseFBConfig(Display *  dpy, int screen, const int *  attribList, int *  nitems); </code>    */
  private static native java.nio.ByteBuffer dispatch_glXChooseFBConfig(long dpy, int screen, Object attribList, int attribList_byte_offset, Object nitems, int nitems_byte_offset, long procAddr);

  /** Interface to C language function: <br> - Alias for: <br> <code> XVisualInfo *  glXChooseVisual(Display *  dpy, int screen, int *  attribList); </code>    */
  public static XVisualInfo glXChooseVisual(long dpy, int screen, int[] attribList, int attribList_offset)
  {
    final long __addr_ = glxProcAddressTable._addressof_glXChooseVisual;
    if (__addr_ == 0) {
        throw new GLException("Method \"glXChooseVisual\" not available");
    }
    if(attribList != null && attribList.length <= attribList_offset)
      throw new GLException("array offset argument \"attribList_offset\" (" + attribList_offset + ") equals or exceeds array length (" + attribList.length + ")");
    java.nio.ByteBuffer _res;
    _res = dispatch_glXChooseVisual(dpy, screen, attribList, Buffers.SIZEOF_INT * attribList_offset, __addr_);

    if (_res == null) return null;
    return XVisualInfo.create(_res);
  }

  /** Entry point to C language function: - Alias for: <br> <code> XVisualInfo *  glXChooseVisual(Display *  dpy, int screen, int *  attribList); </code>    */
  private static native java.nio.ByteBuffer dispatch_glXChooseVisual(long dpy, int screen, Object attribList, int attribList_byte_offset, long procAddr);

