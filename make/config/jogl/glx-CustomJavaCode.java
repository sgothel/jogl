
  /** 
   * Returns the GLX error value, i.e. 0 for no error. In case of an error values.get(values.getPosition()) contains the attributes index causing the error.
   * <p>
   * Entry point to C language function: <code> int glXGetFBConfigAttrib(Display *  dpy, GLXFBConfig config, int attribute, int *  value); </code> <br>Part of <code>GLX_VERSION_1_3</code>
   * </p>
   */
  public static int glXGetFBConfigAttributes(long dpy, long config, IntBuffer attributes, IntBuffer values) {
    if( attributes == null || values == null ) {
        throw new RuntimeException("arrays buffers are null");
    }
    if( !Buffers.isDirect(attributes) || !Buffers.isDirect(values) ) {
        throw new RuntimeException("arrays buffers are not direct");
    }
    if( attributes.remaining() > values.remaining() ) {
        throw new RuntimeException("not enough values "+values+" for attributes "+attributes);
    }
    final long __addr = glxProcAddressTable._addressof_glXGetFBConfigAttrib;
    return dispatch_glXGetFBConfigAttributes(dpy, config, attributes.remaining(), attributes, Buffers.getDirectBufferByteOffset(attributes), 
                                             values, Buffers.getDirectBufferByteOffset(values), __addr);
  }
  private static native int dispatch_glXGetFBConfigAttributes(long dpy, long config, int attributeCount, Object attributes, int attributes_byte_offset, Object values, int valuesOffset, long procAddr);

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

  /** Entry point to C language function: <code> GLXFBConfig *  glXGetFBConfigs(Display *  dpy, int screen, int *  nelements); </code> <br>Part of <code>GLX_VERSION_1_3</code>
      @param nelements a direct only {@link java.nio.IntBuffer}   */
  public static PointerBuffer glXGetFBConfigs(long dpy, int screen, IntBuffer nelements)  {

    if (!Buffers.isDirect(nelements))
      throw new GLException("Argument \"nelements\" is not a direct buffer");
    final long __addr_ = glxProcAddressTable._addressof_glXGetFBConfigs;
    if (__addr_ == 0) {
      throw new GLException(String.format("Method \"%s\" not available", "glXGetFBConfigs"));
    }
    final ByteBuffer _res = dispatch_glXGetFBConfigs(dpy, screen, nelements, Buffers.getDirectBufferByteOffset(nelements), __addr_);
    if (_res == null) return null;
    return PointerBuffer.wrap(Buffers.nativeOrder(_res));
  }

  /** Entry point to C language function: <code> GLXFBConfig *  glXGetFBConfigs(Display *  dpy, int screen, int *  nelements); </code> <br>Part of <code>GLX_VERSION_1_3</code>
      @param nelements a direct only {@link java.nio.IntBuffer}   */
  private static native ByteBuffer dispatch_glXGetFBConfigs(long dpy, int screen, Object nelements, int nelements_byte_offset, long procAddress);


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

