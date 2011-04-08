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

public GL4bcImpl(GLProfile glp, GLContextImpl context) {
  this._context = context; 
  this.bufferSizeTracker  = context.getBufferSizeTracker();
  this.bufferStateTracker = context.getBufferStateTracker();
  this.glStateTracker     = context.getGLStateTracker();
  this.glProfile = glp;
}

/**
 * Provides platform-independent access to the wglAllocateMemoryNV /
 * glXAllocateMemoryNV extension.
 */
public java.nio.ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
  return _context.glAllocateMemoryNV(arg0, arg1, arg2, arg3);
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

private HashMap/*<MemoryObject>*/ arbMemCache = new HashMap();

/** Entry point to C language function: <br> <code> LPVOID glMapBuffer(GLenum target, GLenum access); </code>    */
public java.nio.ByteBuffer glMapBuffer(int target, int access) {
  final long __addr_ = ((GL4bcProcAddressTable)_context.getGLProcAddressTable())._addressof_glMapBuffer;
  if (__addr_ == 0) {
    throw new GLException("Method \"glMapBuffer\" not available");
  }
  final long sz = bufferSizeTracker.getBufferSize(bufferStateTracker, target, this);
  if (0 == sz) {
    return null;
  }
  final long addr = dispatch_glMapBuffer(target, access, __addr_);
  if (0 == addr) {
    return null;
  }
  ByteBuffer buffer;
  MemoryObject memObj0 = new MemoryObject(addr, sz); // object and key
  MemoryObject memObj1 = MemoryObject.getOrAddSafe(arbMemCache, memObj0);
  if(memObj0 == memObj1) {
    // just added ..
    if(null != memObj0.getBuffer()) {
        throw new InternalError();
    }
    buffer = newDirectByteBuffer(addr, sz);
    Buffers.nativeOrder(buffer);
    memObj0.setBuffer(buffer);
  } else {
    // already mapped
    buffer = memObj1.getBuffer();
    if(null == buffer) {
        throw new InternalError();
    }
  }
  buffer.position(0);
  return buffer;
}

/** Encapsulates function pointer for OpenGL function <br>: <code> LPVOID glMapBuffer(GLenum target, GLenum access); </code>    */
native private long dispatch_glMapBuffer(int target, int access, long glProcAddress);

/** Entry point to C language function: <code> GLvoid *  {@native glMapNamedBufferEXT}(GLuint buffer, GLenum access); </code> <br>Part of <code>GL_EXT_direct_state_access</code>   */
public java.nio.ByteBuffer glMapNamedBufferEXT(int bufferName, int access)  {
  final long __addr_ = ((GL4bcProcAddressTable)_context.getGLProcAddressTable())._addressof_glMapNamedBufferEXT;
  if (__addr_ == 0) {
    throw new GLException("Method \"glMapNamedBufferEXT\" not available");
  }
  final long sz = bufferSizeTracker.getDirectStateBufferSize(bufferName, this);
  if (0 == sz) {
    return null;
  }
  final long addr = dispatch_glMapNamedBufferEXT(bufferName, access, __addr_);
  if (0 == addr) {
    return null;
  }
  ByteBuffer buffer;
  MemoryObject memObj0 = new MemoryObject(addr, sz); // object and key
  MemoryObject memObj1 = MemoryObject.getOrAddSafe(arbMemCache, memObj0);
  if(memObj0 == memObj1) {
    // just added ..
    if(null != memObj0.getBuffer()) {
        throw new InternalError();
    }
    buffer = newDirectByteBuffer(addr, sz);
    Buffers.nativeOrder(buffer);
    memObj0.setBuffer(buffer);
  } else {
    // already mapped
    buffer = memObj1.getBuffer();
    if(null == buffer) {
        throw new InternalError();
    }
  }
  buffer.position(0);
  return buffer;
}

private native long dispatch_glMapNamedBufferEXT(int buffer, int access, long procAddress);

native private ByteBuffer newDirectByteBuffer(long addr, long capacity);

    public void glVertexPointer(GLArrayData array) {
      if(array.getComponentNumber()==0) return;
      if(array.isVBO()) {
          glVertexPointer(array.getComponentNumber(), array.getComponentType(), array.getStride(), array.getVBOOffset());
      } else {
          glVertexPointer(array.getComponentNumber(), array.getComponentType(), array.getStride(), array.getBuffer());
      }
    }
    public void glColorPointer(GLArrayData array) {
      if(array.getComponentNumber()==0) return;
      if(array.isVBO()) {
          glColorPointer(array.getComponentNumber(), array.getComponentType(), array.getStride(), array.getVBOOffset());
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
          glNormalPointer(array.getComponentType(), array.getStride(), array.getVBOOffset());
      } else {
          glNormalPointer(array.getComponentType(), array.getStride(), array.getBuffer());
      }
    }
    public void glTexCoordPointer(GLArrayData array) {
      if(array.getComponentNumber()==0) return;
      if(array.isVBO()) {
          glTexCoordPointer(array.getComponentNumber(), array.getComponentType(), array.getStride(), array.getVBOOffset());
      } else {
          glTexCoordPointer(array.getComponentNumber(), array.getComponentType(), array.getStride(), array.getBuffer());
      }
    }

