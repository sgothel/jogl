// Tracks glBegin/glEnd calls to determine whether it is legal to
// query Vertex Buffer Object state
private boolean inBeginEndPair;

public GLES1Impl(GLContextImpl context) {
  this._context = context; 
  this.bufferSizeTracker = context.getBufferSizeTracker();
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

/** Returns the context this GL object is associated with for better
    error checking by DebugGL. */
public GLContext getContext() {
  return _context;
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

//
// Helpers for ensuring the correct amount of texture data
//

/** Returns the number of bytes required to fill in the appropriate
    texture. This is regrettably a lower bound as in certain
    circumstances OpenGL state such as unpack alignment can cause more
    data to be required. However this should be close enough that it
    should catch most crashes. The logic in this routine is based on
    code in the SGI OpenGL sample implementation. */

private int imageSizeInBytes(int format, int type, int w, int h, int d) {
  int elements = 0;
  int esize = 0;
  
  if (w < 0) return 0;
  if (h < 0) return 0;
  if (d < 0) return 0;
  switch (format) {
  case GL_ALPHA:
  case GL_LUMINANCE:
    elements = 1;
    break;
  case GL_LUMINANCE_ALPHA:
    elements = 2;
    break;
  case GL_RGB:
    elements = 3;
    break;
  case GL_RGBA:
    elements = 4;
    break;
  default:
    return 0;
  }
  switch (type) {
  case GL_BYTE:
  case GL_UNSIGNED_BYTE:
    esize = 1;
    break;
  case GL_SHORT:
  case GL_UNSIGNED_SHORT:
    esize = 2;
    break;
  case GL_UNSIGNED_SHORT_5_6_5:
  case GL_UNSIGNED_SHORT_4_4_4_4:
  case GL_UNSIGNED_SHORT_5_5_5_1:
    esize = 2;
    elements = 1;
    break;
  case GL_FLOAT:
    esize = 4;
    break;
  default:
    return 0;
  }
  return (elements * esize * w * h * d);
}

private boolean bufferObjectExtensionsInitialized = false;
private boolean haveARBPixelBufferObject;
private boolean haveEXTPixelBufferObject;
private boolean haveGL15;
private boolean haveGL21;
private boolean haveARBVertexBufferObject;
private GLBufferStateTracker bufferStateTracker = new GLBufferStateTracker();
private GLBufferSizeTracker  bufferSizeTracker;

private void initBufferObjectExtensionChecks() {
  if (bufferObjectExtensionsInitialized)
    return;
  bufferObjectExtensionsInitialized = true;
  haveARBPixelBufferObject  = isExtensionAvailable("GL_ARB_pixel_buffer_object");
  haveEXTPixelBufferObject  = isExtensionAvailable("GL_EXT_pixel_buffer_object");
  haveGL15                  = isExtensionAvailable("GL_VERSION_1_5");
  haveGL21                  = isExtensionAvailable("GL_VERSION_2_1");
  haveARBVertexBufferObject = isExtensionAvailable("GL_ARB_vertex_buffer_object");
}

//
// FIXME: all of these error checking routines had to be disabled
// because our version and extension querying mechanisms aren't
// working on the device yet
//


private void checkBufferObject(boolean extension1,
                               boolean extension2,
                               boolean extension3,
                               boolean enabled,
                               int state,
                               String kind) {
    /*
  if (inBeginEndPair) {
    throw new GLException("May not call this between glBegin and glEnd");
  }
  boolean avail = (extension1 || extension2 || extension3);
  if (!avail) {
    if (!enabled)
      return;
    throw new GLException("Required extensions not available to call this function");
  }
  int buffer = bufferStateTracker.getBoundBufferObject(state, this);
  if (enabled) {
    if (buffer == 0) {
      throw new GLException(kind + " must be enabled to call this method");
    }
  } else {
    if (buffer != 0) {
      throw new GLException(kind + " must be disabled to call this method");
    }
  }
    */
}  

private void checkArrayVBODisabled() { 
    /*
  initBufferObjectExtensionChecks();
  checkBufferObject(haveGL15,
                    haveARBVertexBufferObject,
                    false,
                    false,
                    GL.GL_ARRAY_BUFFER,
                    "array vertex_buffer_object");
    */
}

private void checkArrayVBOEnabled() { 
    /*
  initBufferObjectExtensionChecks();
  checkBufferObject(haveGL15,
                    haveARBVertexBufferObject,
                    false,
                    true,
                    GL.GL_ARRAY_BUFFER,
                    "array vertex_buffer_object");
    */
}

private void checkElementVBODisabled() { 
    /*
  initBufferObjectExtensionChecks();
  checkBufferObject(haveGL15,
                    haveARBVertexBufferObject,
                    false,
                    false,
                    GL.GL_ELEMENT_ARRAY_BUFFER,
                    "element vertex_buffer_object");
    */
}

private void checkElementVBOEnabled() { 
    /*
  initBufferObjectExtensionChecks();
  checkBufferObject(haveGL15,
                    haveARBVertexBufferObject,
                    false,
                    true,
                    GL.GL_ELEMENT_ARRAY_BUFFER,
                    "element vertex_buffer_object");
    */
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
  final long __addr_ = ((GLES1ProcAddressTable)_context.getGLProcAddressTable())._addressof_glMapBuffer;
  if (__addr_ == 0) {
    throw new GLException("Method \"glMapBuffer\" not available");
  }
  int sz = bufferSizeTracker.getBufferSize(bufferStateTracker,
                                           target,
                                           this);
  long addr;
  addr = dispatch_glMapBuffer(target, access, __addr_);
  if (addr == 0 || sz == 0) {
    return null;
  }
  ARBVBOKey key = new ARBVBOKey(addr, sz);
  java.nio.ByteBuffer _res = (java.nio.ByteBuffer) arbVBOCache.get(key);
  if (_res == null) {
    _res = InternalBufferUtils.newDirectByteBuffer(addr, sz);
    BufferUtil.nativeOrder(_res);
    arbVBOCache.put(key, _res);
  }
  _res.position(0);
  return _res;
}

/** Encapsulates function pointer for OpenGL function <br>: <code> LPVOID glMapBuffer(GLenum target, GLenum access); </code>    */
native private long dispatch_glMapBuffer(int target, int access, long glProcAddress);
