
  /** Interface to C language function: <br> - Alias for: <br> <code> XVisualInfo *  glXGetVisualFromFBConfigSGIX, glXGetVisualFromFBConfig(Display *  dpy, GLXFBConfig config); </code>    */
  public static XVisualInfo glXGetVisualFromFBConfig(long dpy, long config)
  {
    final long __addr_ = glxProcAddressTable._addressof_glXGetVisualFromFBConfig;
    if (__addr_ == 0) {
        throw new GLException("Method \"glXGetVisualFromFBConfig\" not available");
    }
    final java.nio.ByteBuffer _res = dispatch_glXGetVisualFromFBConfig(dpy, config, __addr_);
    if (_res == null) return null;
    return XVisualInfo.create(Buffers.nativeOrder(_res));
  }

  /** Entry point to C language function: - Alias for: <br> <code> XVisualInfo *  glXGetVisualFromFBConfigSGIX, glXGetVisualFromFBConfig(Display *  dpy, GLXFBConfig config); </code>    */
  private static native java.nio.ByteBuffer dispatch_glXGetVisualFromFBConfig(long dpy, long config, long procAddr);


  /** Entry point to C language function: <code> GLXFBConfig *  glXChooseFBConfig(Display *  dpy, int screen, const int *  attribList, int *  nitems); </code> <br>Part of CORE FUNC
      @param attribList a direct only {@link java.nio.IntBuffer}
      @param nitems a direct only {@link java.nio.IntBuffer}   */
  public static PointerBuffer glXChooseFBConfig(long dpy, int screen, IntBuffer attribList, IntBuffer nitems)  {

    if (!Buffers.isDirect(attribList))
      throw new GLException("Argument \"attribList\" is not a direct buffer");
    if (!Buffers.isDirect(nitems))
      throw new GLException("Argument \"nitems\" is not a direct buffer");
    final long __addr_ = glxProcAddressTable._addressof_glXChooseFBConfig;
    if (__addr_ == 0) {
      throw new GLException("Method \"glXChooseFBConfig\" not available");
    }
    final ByteBuffer _res = dispatch_glXChooseFBConfig(dpy, screen, attribList, Buffers.getDirectBufferByteOffset(attribList), nitems, Buffers.getDirectBufferByteOffset(nitems), __addr_);
    if (_res == null) return null;
    return PointerBuffer.wrap(Buffers.nativeOrder(_res));
  }

  /** Entry point to C language function: <code> GLXFBConfig *  glXChooseFBConfig(Display *  dpy, int screen, const int *  attribList, int *  nitems); </code> <br>Part of CORE FUNC
      @param attribList a direct only {@link java.nio.IntBuffer}
      @param nitems a direct only {@link java.nio.IntBuffer}   */
  private static native ByteBuffer dispatch_glXChooseFBConfig(long dpy, int screen, Object attribList, int attribList_byte_offset, Object nitems, int nitems_byte_offset, long procAddress);


  /** Entry point to C language function: <code> XVisualInfo *  glXChooseVisual(Display *  dpy, int screen, int *  attribList); </code> <br>Part of <code>GLX_VERSION_1_X</code>
      @param attribList a direct only {@link java.nio.IntBuffer}   */
  public static XVisualInfo glXChooseVisual(long dpy, int screen, IntBuffer attribList)  {

    if (!Buffers.isDirect(attribList))
      throw new GLException("Argument \"attribList\" is not a direct buffer");
    final long __addr_ = glxProcAddressTable._addressof_glXChooseVisual;
    if (__addr_ == 0) {
      throw new GLException("Method \"glXChooseVisual\" not available");
    }
    final ByteBuffer _res = dispatch_glXChooseVisual(dpy, screen, attribList, Buffers.getDirectBufferByteOffset(attribList), __addr_);
    if (_res == null) return null;
    return XVisualInfo.create(Buffers.nativeOrder(_res));
  }

  /** Entry point to C language function: <code> XVisualInfo *  glXChooseVisual(Display *  dpy, int screen, int *  attribList); </code> <br>Part of <code>GLX_VERSION_1_X</code>
      @param attribList a direct only {@link java.nio.IntBuffer}   */
  private static native ByteBuffer dispatch_glXChooseVisual(long dpy, int screen, Object attribList, int attribList_byte_offset, long procAddress);

