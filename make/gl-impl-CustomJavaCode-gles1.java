public GLES1Impl(GLContextImpl context) {
  this._context = context; 
  this.bufferSizeTracker = context.getBufferSizeTracker();
}

public final boolean isGL2() {
    return false;
}

public final boolean isGLES1() {
    return true;
}

public final boolean isGLES2() {
    return false;
}

public final boolean isGLES() {
    return true;
}

public final boolean isGL2ES1() {
    return true;
}

public final boolean isGL2ES2() {
    return false;
}

public final GL2 getGL2() throws GLException {
    throw new GLException("Not a GL2 implementation");
}

public final GLES1 getGLES1() throws GLException {
    return this;
}

public final GLES2 getGLES2() throws GLException {
    throw new GLException("Not a GLES2 implementation");
}

public final GL2ES1 getGL2ES1() throws GLException {
    return this;
}

public final GL2ES2 getGL2ES2() throws GLException {
    throw new GLException("Not a GL2ES2 implementation");
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

private GLBufferStateTracker bufferStateTracker = new GLBufferStateTracker();
private GLBufferSizeTracker  bufferSizeTracker;

private boolean bufferObjectExtensionsInitialized = false;
private boolean haveOESFramebufferObject;
private boolean haveOESPixelBufferObject;

private void initBufferObjectExtensionChecks() {
  if (bufferObjectExtensionsInitialized)
    return;
  bufferObjectExtensionsInitialized = true;
  haveOESFramebufferObject  = isExtensionAvailable("GL_OES_framebuffer_object");
  haveOESPixelBufferObject  = false; // FIXME: can't find it in ES 1.1 or ES 2.0 spec
}

private void checkBufferObject(boolean avail,
                               boolean enabled,
                               int state,
                               String kind) {
  if (!avail) {
    if (!enabled)
      return;
    throw new GLUnsupportedException("Required extensions not available to call this function");
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
}  

private void checkArrayVBODisabled() { 
  initBufferObjectExtensionChecks();
  checkBufferObject(true,
                    false,
                    GL.GL_ARRAY_BUFFER,
                    "array vertex_buffer_object");
}

private void checkArrayVBOEnabled() { 
  initBufferObjectExtensionChecks();
  checkBufferObject(true,
                    true,
                    GL.GL_ARRAY_BUFFER,
                    "array vertex_buffer_object");
}

private void checkElementVBODisabled() { 
  initBufferObjectExtensionChecks();
  checkBufferObject(true,
                    false,
                    GL.GL_ELEMENT_ARRAY_BUFFER,
                    "element vertex_buffer_object");
}

private void checkElementVBOEnabled() { 
  initBufferObjectExtensionChecks();
  checkBufferObject(true,
                    true,
                    GL.GL_ELEMENT_ARRAY_BUFFER,
                    "element vertex_buffer_object");
}

private void checkUnpackPBODisabled() { 
  initBufferObjectExtensionChecks();
  checkBufferObject(haveOESPixelBufferObject,
                    false,
                    GL2.GL_PIXEL_UNPACK_BUFFER,
                    "unpack pixel_buffer_object");
}

private void checkUnpackPBOEnabled() { 
  initBufferObjectExtensionChecks();
  checkBufferObject(haveOESPixelBufferObject,
                    true,
                    GL2.GL_PIXEL_UNPACK_BUFFER,
                    "unpack pixel_buffer_object");
}

private void checkPackPBODisabled() { 
  initBufferObjectExtensionChecks();
  checkBufferObject(haveOESPixelBufferObject,
                    false,
                    GL2.GL_PIXEL_PACK_BUFFER,
                    "pack pixel_buffer_object");
}

private void checkPackPBOEnabled() { 
  initBufferObjectExtensionChecks();
  checkBufferObject(haveOESPixelBufferObject,
                    true,
                    GL2.GL_PIXEL_PACK_BUFFER,
                    "pack pixel_buffer_object");
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
    throw new GLUnsupportedException("Method \"glMapBuffer\" not available");
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

  public final String toString() {
      StringBuffer buf = new StringBuffer();
      buf.append("GL: ");
      buf.append(getClass().getName());
      buf.append(" (GLContext: ");
      GLContext context = getContext();
      buf.append(context.getClass().getName());
      buf.append(", GLDrawable: ");
      GLDrawable drawable = context.getGLDrawable();
      buf.append(drawable.getClass().getName());
      buf.append(", Factory: ");
      GLDrawableFactory factory = drawable.getFactory();
      buf.append(factory.getClass().getName());
      buf.append(")");
      return buf.toString();
  }

public void glVertexPointer(GLArrayData array) {
  if(array.getComponentNumber()==0) return;
  if(array.isVBO()) {
      glVertexPointer(array.getComponentNumber(), array.getComponentType(), array.getStride(), array.getOffset());
  } else {
      glVertexPointer(array.getComponentNumber(), array.getComponentType(), array.getStride(), array.getBuffer());
  }
}
public void glColorPointer(GLArrayData array) {
  if(array.getComponentNumber()==0) return;
  if(array.isVBO()) {
      glColorPointer(array.getComponentNumber(), array.getComponentType(), array.getStride(), array.getOffset());
  } else {
      glColorPointer(array.getComponentNumber(), array.getComponentType(), array.getStride(), array.getBuffer());
  }

}
public void glNormalPointer(GLArrayData array) {
  if(array.getComponentNumber()==0) return;
  if(array.getComponentNumber()!=3) {
    throw new GLException("Only 3 components per normal allowed");
  }
  if(array.isVBO()) {
      glNormalPointer(array.getComponentType(), array.getStride(), array.getOffset());
  } else {
      glNormalPointer(array.getComponentType(), array.getStride(), array.getBuffer());
  }
}
public void glTexCoordPointer(GLArrayData array) {
  if(array.getComponentNumber()==0) return;
  if(array.isVBO()) {
      glTexCoordPointer(array.getComponentNumber(), array.getComponentType(), array.getStride(), array.getOffset());
  } else {
      glTexCoordPointer(array.getComponentNumber(), array.getComponentType(), array.getStride(), array.getBuffer());
  }
}

