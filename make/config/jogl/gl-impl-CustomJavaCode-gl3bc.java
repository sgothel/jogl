// Tracks glBegin/glEnd calls to determine whether it is legal to
// query Vertex Buffer Object state
private boolean inBeginEndPair;

/* FIXME: refactor dependence on Java 2D / JOGL bridge

// Tracks creation and destruction of server-side OpenGL objects when
// the Java2D/OpenGL pipeline is enabled and it is using frame buffer
// objects (FBOs) to do its rendering
private GLObjectTracker tracker;

public void setObjectTracker(GLObjectTracker tracker) {
  this.tracker = tracker;
}

*/


public GL3bcImpl(GLProfile glp, GLContextImpl context) {
  this._context = context; 
  this.bufferSizeTracker  = context.getBufferSizeTracker();
  this.bufferStateTracker = context.getBufferStateTracker();
  this.glStateTracker     = context.getGLStateTracker();
  this.glProfile = glp;
}

public final boolean isGL() {
    return true;
}
  
public final boolean isGL3bc() {
    return true;
}

public final boolean isGL3() {
    return true;
}

public final boolean isGL2() {
    return true;
}
  
public final boolean isGLES1() {
    return false;
}

public final boolean isGLES2() {
    return false;
}

public final boolean isGLES() {
    return false;
}

public final boolean isGL2ES1() {
    return true;
}

public final boolean isGL2ES2() {
    return true;
}

public final boolean isGL2GL3() {
    return true;
}

public final boolean hasGLSL() {
    return true;
}

public final GL getGL() throws GLException {
    return this;
}

public final GL3bc getGL3bc() throws GLException {
    return this;
}

public final GL3 getGL3() throws GLException {
    return this;
}

public final GL2 getGL2() throws GLException {
    return this;
}

public final GLES1 getGLES1() throws GLException {
    throw new GLException("Not a GLES1 implementation");
}

public final GLES2 getGLES2() throws GLException {
    throw new GLException("Not a GLES2 implementation");
}

public final GL2ES1 getGL2ES1() throws GLException {
    return this;
}

public final GL2ES2 getGL2ES2() throws GLException {
    return this;
}

public final GL2GL3 getGL2GL3() throws GLException {
    return this;
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

public int getSwapInterval() {
  return _context.getSwapInterval();
}

public Object getPlatformGLExtensions() {
  return _context.getPlatformGLExtensions();
}

//
// Helpers for ensuring the correct amount of texture data
//

/** Returns the number of bytes required to fill in the appropriate
    texture. This is computed as closely as possible based on the
    pixel pack or unpack parameters. The logic in this routine is
    based on code in the SGI OpenGL sample implementation. */

private int imageSizeInBytes(int format, int type, int w, int h, int d,
                             boolean pack) {
  int elements = 0;
  int esize = 0;
  
  if (w < 0) return 0;
  if (h < 0) return 0;
  if (d < 0) return 0;
  switch (format) {
  case GL_COLOR_INDEX:
  case GL_STENCIL_INDEX:
    elements = 1;
    break;
  case GL_RED:
  case GL_GREEN:
  case GL_BLUE:
  case GL_ALPHA:
  case GL_LUMINANCE:
  case GL_DEPTH_COMPONENT:
    elements = 1;
    break;
  case GL_LUMINANCE_ALPHA:
    elements = 2;
    break;
  case GL_RGB:
  case GL_BGR:
    elements = 3;
    break;
  case GL_RGBA:
  case GL_BGRA:
  case GL_ABGR_EXT:
    elements = 4;
    break;
  /* FIXME ?? 
   case GL_HILO_NV:
    elements = 2;
    break; */
  default:
    return 0;
  }
  switch (type) {
  case GL_BITMAP:
    if (format == GL_COLOR_INDEX) {
      return (d * (h * ((w+7)/8)));
    } else {
      return 0;
    }
  case GL_BYTE:
  case GL_UNSIGNED_BYTE:
    esize = 1;
    break;
  case GL_UNSIGNED_BYTE_3_3_2:
  case GL_UNSIGNED_BYTE_2_3_3_REV:
    esize = 1;
    elements = 1;
    break;
  case GL_SHORT:
  case GL_UNSIGNED_SHORT:
    esize = 2;
    break;
  case GL_UNSIGNED_SHORT_5_6_5:
  case GL_UNSIGNED_SHORT_5_6_5_REV:
  case GL_UNSIGNED_SHORT_4_4_4_4:
  case GL_UNSIGNED_SHORT_4_4_4_4_REV:
  case GL_UNSIGNED_SHORT_5_5_5_1:
  case GL_UNSIGNED_SHORT_1_5_5_5_REV:
    esize = 2;
    elements = 1;
    break;
  case GL_INT:
  case GL_UNSIGNED_INT:
  case GL_FLOAT:
    esize = 4;
    break;
  case GL_UNSIGNED_INT_8_8_8_8:
  case GL_UNSIGNED_INT_8_8_8_8_REV:
  case GL_UNSIGNED_INT_10_10_10_2:
  case GL_UNSIGNED_INT_2_10_10_10_REV:
    esize = 4;
    elements = 1;
    break;
  default:
    return 0;
  }
  return imageSizeInBytes(elements * esize, w, h, d, pack);
}

private GLBufferSizeTracker  bufferSizeTracker;
private GLBufferStateTracker bufferStateTracker;
private GLStateTracker       glStateTracker;

private boolean bufferObjectExtensionsInitialized = false;
private boolean haveARBPixelBufferObject;
private boolean haveEXTPixelBufferObject;
private boolean haveGL15;
private boolean haveGL21;
private boolean haveARBVertexBufferObject;

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

private boolean checkBufferObject(boolean extension1,
                                  boolean extension2,
                                  boolean extension3,
                                  boolean enabled,
                                  int state,
                                  String kind, boolean throwException) {
  if (inBeginEndPair) {
    throw new GLException("May not call this between glBegin and glEnd");
  }
  boolean avail = (extension1 || extension2 || extension3);
  if (!avail) {
    if (!enabled)
      return true;
    if(throwException) {
        throw new GLException("Required extensions not available to call this function");
    }
    return false;
  }
  int buffer = bufferStateTracker.getBoundBufferObject(state, this);
  if (enabled) {
    if (buffer == 0) {
      if(throwException) {
          throw new GLException(kind + " must be enabled to call this method");
      }
      return false;
    }
  } else {
    if (buffer != 0) {
      if(throwException) {
          throw new GLException(kind + " must be disabled to call this method");
      }
      return false;
    }
  }
  return true;
}  

private boolean checkArrayVBODisabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveGL15,
                    haveARBVertexBufferObject,
                    false,
                    false,
                    GL.GL_ARRAY_BUFFER,
                    "array vertex_buffer_object", throwException);
}

private boolean checkArrayVBOEnabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveGL15,
                    haveARBVertexBufferObject,
                    false,
                    true,
                    GL.GL_ARRAY_BUFFER,
                    "array vertex_buffer_object", throwException);
}

private boolean checkElementVBODisabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveGL15,
                    haveARBVertexBufferObject,
                    false,
                    false,
                    GL.GL_ELEMENT_ARRAY_BUFFER,
                    "element vertex_buffer_object", throwException);
}

private boolean checkElementVBOEnabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveGL15,
                    haveARBVertexBufferObject,
                    false,
                    true,
                    GL.GL_ELEMENT_ARRAY_BUFFER,
                    "element vertex_buffer_object", throwException);
}

private boolean checkUnpackPBODisabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveARBPixelBufferObject,
                    haveEXTPixelBufferObject,
                    haveGL21,
                    false,
                    GL2.GL_PIXEL_UNPACK_BUFFER,
                    "unpack pixel_buffer_object", throwException);
}

private boolean checkUnpackPBOEnabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveARBPixelBufferObject,
                    haveEXTPixelBufferObject,
                    haveGL21,
                    true,
                    GL2.GL_PIXEL_UNPACK_BUFFER,
                    "unpack pixel_buffer_object", throwException);
}

private boolean checkPackPBODisabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveARBPixelBufferObject,
                    haveEXTPixelBufferObject,
                    haveGL21,
                    false,
                    GL2.GL_PIXEL_PACK_BUFFER,
                    "pack pixel_buffer_object", throwException);
}

private boolean checkPackPBOEnabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveARBPixelBufferObject,
                    haveEXTPixelBufferObject,
                    haveGL21,
                    true,
                    GL2.GL_PIXEL_PACK_BUFFER,
                    "pack pixel_buffer_object", throwException);
}

public boolean glIsPBOPackEnabled() {
    return checkPackPBOEnabled(false);
}

public boolean glIsPBOUnpackEnabled() {
    return checkUnpackPBOEnabled(false);
}

// Attempt to return the same ByteBuffer object from glMapBuffer if
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
  final long __addr_ = ((GL3bcProcAddressTable)_context.getGLProcAddressTable())._addressof_glMapBuffer;
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
  ByteBuffer _res = (ByteBuffer) arbVBOCache.get(key);
  if (_res == null) {
    _res = newDirectByteBuffer(addr, sz);
    InternalBufferUtil.nativeOrder(_res);
    arbVBOCache.put(key, _res);
  }
  _res.position(0);
  return _res;
}

/** Encapsulates function pointer for OpenGL function <br>: <code> LPVOID glMapBuffer(GLenum target, GLenum access); </code>    */
native private long dispatch_glMapBuffer(int target, int access, long glProcAddress);

native private ByteBuffer newDirectByteBuffer(long addr, int capacity);

  /** Dummy implementation for the ES 2.0 function: <br> <code> void {@native glShaderBinary}(GLint n, const GLuint *  shaders, GLenum binaryformat, const void *  binary, GLint length); </code>  <br> Always throws a GLException!  */
  public void glShaderBinary(int n, java.nio.IntBuffer shaders, int binaryformat, java.nio.Buffer binary, int length) {
    throw new GLException("Method \"glShaderBinary\" not available");
  }

  /** Dummy implementation for the ES 2.0 function: <br> <code> void {@native glShaderBinary}(GLint n, const GLuint *  shaders, GLenum binaryformat, const void *  binary, GLint length); </code>  <br> Always throws a GLException!  */
  public void glShaderBinary(int n, int[] shaders, int shaders_offset, int binaryformat, java.nio.Buffer binary, int length) {
    throw new GLException("Method \"glShaderBinary\" not available");
  }

  public void glReleaseShaderCompiler() {
    // nothing to do 
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

