
  /** Interface to C language function: <br> - Alias for: <br> <code> XVisualInfo *  glXGetVisualFromFBConfigSGIX, glXGetVisualFromFBConfig(Display *  dpy, GLXFBConfig config); </code>    */
  public static XVisualInfo glXGetVisualFromFBConfigCopied(long dpy, long config)
  {
    java.nio.ByteBuffer _res;
    _res = glXGetVisualFromFBConfigCopied0(dpy, config);
    if (_res == null) return null;
    return XVisualInfo.create(_res);
  }

  /** Entry point to C language function: - Alias for: <br> <code> XVisualInfo *  glXGetVisualFromFBConfigSGIX, glXGetVisualFromFBConfig(Display *  dpy, GLXFBConfig config); </code>    */
  private static native java.nio.ByteBuffer glXGetVisualFromFBConfigCopied0(long dpy, long config);


  /** Interface to C language function: <br> - Alias for: <br> <code> GLXFBConfig *  glXChooseFBConfigSGIX, glXChooseFBConfig(Display *  dpy, int screen, const int *  attribList, int *  nitems); </code>    */
  public static com.sun.gluegen.runtime.PointerBuffer glXChooseFBConfigCopied(long dpy, int screen, int[] attribList, int attribList_offset, int[] nitems, int nitems_offset)
  {
    if(attribList != null && attribList.length <= attribList_offset)
      throw new GLException("array offset argument \"attribList_offset\" (" + attribList_offset + ") equals or exceeds array length (" + attribList.length + ")");
    if(nitems != null && nitems.length <= nitems_offset)
      throw new GLException("array offset argument \"nitems_offset\" (" + nitems_offset + ") equals or exceeds array length (" + nitems.length + ")");
    java.nio.ByteBuffer _res;
    _res = glXChooseFBConfigCopied1(dpy, screen, attribList, BufferFactory.SIZEOF_INT * attribList_offset, nitems, BufferFactory.SIZEOF_INT * nitems_offset);

    if (_res == null) return null;
    return PointerBuffer.wrapNative2Java(_res, false);
  }

  /** Entry point to C language function: - Alias for: <br> <code> GLXFBConfig *  glXChooseFBConfigSGIX, glXChooseFBConfig(Display *  dpy, int screen, const int *  attribList, int *  nitems); </code>    */
  private static native java.nio.ByteBuffer glXChooseFBConfigCopied1(long dpy, int screen, Object attribList, int attribList_byte_offset, Object nitems, int nitems_byte_offset);

  /** Interface to C language function: <br> - Alias for: <br> <code> XVisualInfo *  glXChooseVisual(Display *  dpy, int screen, int *  attribList); </code>    */
  public static XVisualInfo glXChooseVisualCopied(long dpy, int screen, int[] attribList, int attribList_offset)
  {
    if(attribList != null && attribList.length <= attribList_offset)
      throw new GLException("array offset argument \"attribList_offset\" (" + attribList_offset + ") equals or exceeds array length (" + attribList.length + ")");
    java.nio.ByteBuffer _res;
    _res = glXChooseVisualCopied1(dpy, screen, attribList, BufferFactory.SIZEOF_INT * attribList_offset);

    if (_res == null) return null;
    return XVisualInfo.create(_res);
  }

  /** Entry point to C language function: - Alias for: <br> <code> XVisualInfo *  glXChooseVisual(Display *  dpy, int screen, int *  attribList); </code>    */
  private static native java.nio.ByteBuffer glXChooseVisualCopied1(long dpy, int screen, Object attribList, int attribList_byte_offset);

