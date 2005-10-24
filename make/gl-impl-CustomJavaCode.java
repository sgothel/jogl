// Tracks glBegin/glEnd calls to determine whether it is legal to
// query Vertex Buffer Object state
private boolean inBeginEndPair;

public GLImpl(GLContextImpl context) {
  this._context = context; 
}

public boolean isFunctionAvailable(String glFunctionName) {
  return _context.isFunctionAvailable(glFunctionName);
}

public boolean isExtensionAvailable(String glExtensionName) {
  return _context.isExtensionAvailable(glExtensionName);
}

public Object getExtension(String extensionName) {
  // At this point we don't expose any extensions using this mechanism
  return null;
}

private GLContextImpl _context;

/**
 * Provides platform-independent access to the wglAllocateMemoryNV /
 * glXAllocateMemoryNV extension.
 */
public java.nio.ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
  return _context.glAllocateMemoryNV(arg0, arg1, arg2, arg3);
}

public void setSwapInterval(int interval) {
  _context.setSwapInterval(interval);
}

public Object getPlatformGLExtensions() {
  return _context.getPlatformGLExtensions();
}

private void checkBufferObject(String extension1,
                               String extension2,
                               boolean enabled,
                               int state,
                               String kind) {
  if (inBeginEndPair) {
    throw new GLException("May not call this between glBegin and glEnd");
  }
  boolean avail = ((extension1 != null && isExtensionAvailable(extension1)) ||
                   (extension2 != null && isExtensionAvailable(extension2)));
  if (!avail) {
    if (!enabled)
      return;
    throw new GLException("Required extensions not available to call this function");
  }
  int[] val = new int[1];
  glGetIntegerv(state, val, 0);
  if (enabled) {
    if (val[0] == 0) {
      throw new GLException(kind + " must be enabled to call this method");
    }
  } else {
    if (val[0] != 0) {
      throw new GLException(kind + " must be disabled to call this method");
    }
  }
}  

private void checkUnpackPBODisabled() { 
  checkBufferObject("GL_ARB_pixel_buffer_object",
                    "GL_EXT_pixel_buffer_object",
                    false,
                    GL.GL_PIXEL_UNPACK_BUFFER_BINDING_ARB,
                    "unpack pixel_buffer_object");
}

private void checkUnpackPBOEnabled() { 
  checkBufferObject("GL_ARB_pixel_buffer_object",
                    "GL_EXT_pixel_buffer_object",
                    true,
                    GL.GL_PIXEL_UNPACK_BUFFER_BINDING_ARB,
                    "unpack pixel_buffer_object");
}

private void checkPackPBODisabled() { 
  checkBufferObject("GL_ARB_pixel_buffer_object",
                    "GL_EXT_pixel_buffer_object",
                    false,
                    GL.GL_PIXEL_PACK_BUFFER_BINDING_ARB,
                    "pack pixel_buffer_object");
}

private void checkPackPBOEnabled() { 
  checkBufferObject("GL_ARB_pixel_buffer_object",
                    "GL_EXT_pixel_buffer_object",
                    true,
                    GL.GL_PIXEL_PACK_BUFFER_BINDING_ARB,
                    "pack pixel_buffer_object");
}


private void checkArrayVBODisabled() { 
  checkBufferObject("GL_VERSION_1_5",
                    "GL_ARB_vertex_buffer_object",
                    false,
                    GL.GL_ARRAY_BUFFER_BINDING,
                    "array vertex_buffer_object");
}

private void checkArrayVBOEnabled() { 
  checkBufferObject("GL_VERSION_1_5",
                    "GL_ARB_vertex_buffer_object",
                    true,
                    GL.GL_ARRAY_BUFFER_BINDING,
                    "array vertex_buffer_object");
}

private void checkElementVBODisabled() { 
  checkBufferObject("GL_VERSION_1_5",
                    "GL_ARB_vertex_buffer_object",
                    false,
                    GL.GL_ELEMENT_ARRAY_BUFFER_BINDING,
                    "element vertex_buffer_object");
}

private void checkElementVBOEnabled() { 
  checkBufferObject("GL_VERSION_1_5",
                    "GL_ARB_vertex_buffer_object",
                    true,
                    GL.GL_ELEMENT_ARRAY_BUFFER_BINDING,
                    "element vertex_buffer_object");
}

// Attempt to return the same ByteBuffer object from glMapBufferARB if
// the vertex buffer object's base address and size haven't changed
private static class ARBVBOKey {
  private long addr;
  private int  capacity;

  ARBVBOKey(long addr, int capacity) {
    this.addr = addr;
    this.capacity = capacity;
  }

  public int hashCode() {
    return (int) addr;
  }

  public boolean equals(Object o) {
    if ((o == null) || (!(o instanceof ARBVBOKey))) {
      return false;
    }

    ARBVBOKey other = (ARBVBOKey) o;
    return ((addr == other.addr) && (capacity == other.capacity));
  }
}

private Map/*<ARBVBOKey, ByteBuffer>*/ arbVBOCache = new HashMap();

/** Entry point to C language function: <br> <code> LPVOID glMapBuffer(GLenum target, GLenum access); </code>    */
public java.nio.ByteBuffer glMapBuffer(int target, int access) {
  final long __addr_ = _context.getGLProcAddressTable()._addressof_glMapBuffer;
  if (__addr_ == 0) {
    throw new GLException("Method \"glMapBuffer\" not available");
  }
  int[] sz = new int[1];
  glGetBufferParameteriv(target, GL_BUFFER_SIZE_ARB, sz, 0);
  long addr;
  addr = dispatch_glMapBuffer(target, access, __addr_);
  if (addr == 0 || sz[0] == 0) {
    return null;
  }
  ARBVBOKey key = new ARBVBOKey(addr, sz[0]);
  ByteBuffer _res = (ByteBuffer) arbVBOCache.get(key);
  if (_res == null) {
    _res = InternalBufferUtils.newDirectByteBuffer(addr, sz[0]);
    _res.order(ByteOrder.nativeOrder());
    arbVBOCache.put(key, _res);
  }
  return _res;
}

/** Entry point to C language function: <br> <code> LPVOID glMapBufferARB(GLenum target, GLenum access); </code>    */
public java.nio.ByteBuffer glMapBufferARB(int target, int access) {
  final long __addr_ = _context.getGLProcAddressTable()._addressof_glMapBufferARB;
  if (__addr_ == 0) {
    throw new GLException("Method \"glMapBufferARB\" not available");
  }
  int[] sz = new int[1];
  glGetBufferParameterivARB(target, GL_BUFFER_SIZE_ARB, sz, 0);
  long addr;
  addr = dispatch_glMapBufferARB(target, access, __addr_);
  if (addr == 0 || sz[0] == 0) {
    return null;
  }
  ARBVBOKey key = new ARBVBOKey(addr, sz[0]);
  ByteBuffer _res = (ByteBuffer) arbVBOCache.get(key);
  if (_res == null) {
    _res = InternalBufferUtils.newDirectByteBuffer(addr, sz[0]);
    _res.order(ByteOrder.nativeOrder());
    arbVBOCache.put(key, _res);
  }
  return _res;
}

/** Encapsulates function pointer for OpenGL function <br>: <code> LPVOID glMapBuffer(GLenum target, GLenum access); </code>    */
native private long dispatch_glMapBuffer(int target, int access, long glProcAddress);

/** Encapsulates function pointer for OpenGL function <br>: <code> LPVOID glMapBufferARB(GLenum target, GLenum access); </code>    */
native private long dispatch_glMapBufferARB(int target, int access, long glProcAddress);
