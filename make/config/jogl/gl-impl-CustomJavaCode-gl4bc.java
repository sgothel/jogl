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
  if(null != context) {
      this.bufferSizeTracker  = context.getBufferSizeTracker();
      this.bufferStateTracker = context.getBufferStateTracker();
      this.glStateTracker     = context.getGLStateTracker();
  } else {
      this.bufferSizeTracker  = null;
      this.bufferStateTracker = null;
      this.glStateTracker     = null;
  }
  this.glProfile = glp;
}

/**
 * Provides platform-independent access to the wglAllocateMemoryNV /
 * glXAllocateMemoryNV extension.
 */
public final java.nio.ByteBuffer glAllocateMemoryNV(int arg0, float arg1, float arg2, float arg3) {
  return _context.glAllocateMemoryNV(arg0, arg1, arg2, arg3);
}

//
// Helpers for ensuring the correct amount of texture data
//

private final GLBufferSizeTracker  bufferSizeTracker;
private final GLBufferStateTracker bufferStateTracker;
private final GLStateTracker       glStateTracker;

private boolean bufferObjectExtensionsInitialized = false;
private boolean haveARBPixelBufferObject;
private boolean haveEXTPixelBufferObject;
private boolean haveGL15;
private boolean haveGL21;
private boolean haveARBVertexBufferObject;
private boolean haveARBVertexArrayObject;

private final void initBufferObjectExtensionChecks() {
  if ( bufferObjectExtensionsInitialized ) {
    return;
  }
  bufferObjectExtensionsInitialized = true;
  haveARBPixelBufferObject  = isExtensionAvailable("GL_ARB_pixel_buffer_object");
  haveEXTPixelBufferObject  = isExtensionAvailable("GL_EXT_pixel_buffer_object");
  haveGL15                  = isExtensionAvailable("GL_VERSION_1_5");
  haveGL21                  = isExtensionAvailable("GL_VERSION_2_1");
  haveARBVertexBufferObject = isExtensionAvailable("GL_ARB_vertex_buffer_object");
  haveARBVertexArrayObject  = _context.getGLVersionNumber().compareTo(GLContext.Version300) >= 0 ||
                              isExtensionAvailable("GL_ARB_vertex_array_object");
}

private final boolean checkBufferObject(boolean extensionAvail,
                                        boolean allowVAO,
                                        boolean enabled,
                                        int state,
                                        String kind, boolean throwException) {
  if ( inBeginEndPair ) {
    throw new GLException("May not call this between glBegin and glEnd");
  }
  if ( !extensionAvail ) {
    if ( !enabled ) {
      return true;
    }
    if(throwException) {
        throw new GLException("Required extensions not available to call this function");
    }
    return false;
  }
  int buffer = bufferStateTracker.getBoundBufferObject(state, this);
  if ( enabled ) {
    if ( 0 != buffer ) {
        return true;
    }
    if ( allowVAO ) {
        buffer = bufferStateTracker.getBoundBufferObject(GL2GL3.GL_VERTEX_ARRAY_BINDING, this);
        if( 0 != buffer && !_context.isDefaultVAO(buffer) ) {
            return true;
        }
    }
    if ( throwException ) {
        throw new GLException(kind + " must be enabled to call this method");
    }
    return false;
  } else {
    if ( 0 == buffer ) {
        return true;
    }
    if ( throwException ) {
        throw new GLException(kind + " must be disabled to call this method");
    }
    return false;
  }
}  

private final boolean checkArrayVBODisabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveGL15 || haveARBVertexBufferObject,
                           haveARBVertexArrayObject, // allowVAO
                           false, // enable
                           GL.GL_ARRAY_BUFFER,
                           "array vertex_buffer_object", throwException);
}

private final boolean checkArrayVBOEnabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveGL15 || haveARBVertexBufferObject,
                           haveARBVertexArrayObject, // allowVAO
                           true, // enable
                           GL.GL_ARRAY_BUFFER,
                           "array vertex_buffer_object", throwException);
}

private final boolean checkElementVBODisabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveGL15 || haveARBVertexBufferObject,
                           haveARBVertexArrayObject, // allowVAO
                           false, // enable
                           GL.GL_ELEMENT_ARRAY_BUFFER,
                           "element vertex_buffer_object", throwException);
}

private final boolean checkElementVBOEnabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveGL15 || haveARBVertexBufferObject,
                           haveARBVertexArrayObject, // allowVAO
                           true, // enable
                           GL.GL_ELEMENT_ARRAY_BUFFER,
                           "element vertex_buffer_object", throwException);
}

private final boolean checkUnpackPBODisabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveGL21 || haveARBPixelBufferObject || haveEXTPixelBufferObject,
                           false, // allowVAO
                           false, // enable
                           GL2.GL_PIXEL_UNPACK_BUFFER,
                           "unpack pixel_buffer_object", throwException);
}

private final boolean checkUnpackPBOEnabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveGL21 || haveARBPixelBufferObject || haveEXTPixelBufferObject,
                           false, // allowVAO
                           true, // enable
                           GL2.GL_PIXEL_UNPACK_BUFFER,
                           "unpack pixel_buffer_object", throwException);
}

private final boolean checkPackPBODisabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveGL21 || haveARBPixelBufferObject || haveEXTPixelBufferObject,
                           false, // allowVAO
                           false, // enable
                           GL2.GL_PIXEL_PACK_BUFFER,
                           "pack pixel_buffer_object", throwException);
}

private final boolean checkPackPBOEnabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(haveGL21 || haveARBPixelBufferObject || haveEXTPixelBufferObject,
                           false, // allowVAO
                           true, // enable
                           GL2.GL_PIXEL_PACK_BUFFER,
                           "pack pixel_buffer_object", throwException);
}

@Override
public final boolean glIsPBOPackEnabled() {
    return checkPackPBOEnabled(false);
}

@Override
public final boolean glIsPBOUnpackEnabled() {
    return checkUnpackPBOEnabled(false);
}

private final HashMap<MemoryObject, MemoryObject> arbMemCache = new HashMap<MemoryObject, MemoryObject>();

/** Entry point to C language function: <br> <code> LPVOID glMapBuffer(GLenum target, GLenum access); </code>    */
public final java.nio.ByteBuffer glMapBuffer(int target, int access) {
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
public final java.nio.ByteBuffer glMapNamedBufferEXT(int bufferName, int access)  {
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

    @Override
    public final void glVertexPointer(GLArrayData array) {
      if(array.getComponentCount()==0) return;
      if(array.isVBO()) {
          glVertexPointer(array.getComponentCount(), array.getComponentType(), array.getStride(), array.getVBOOffset());
      } else {
          glVertexPointer(array.getComponentCount(), array.getComponentType(), array.getStride(), array.getBuffer());
      }
    }
    @Override
    public final void glColorPointer(GLArrayData array) {
      if(array.getComponentCount()==0) return;
      if(array.isVBO()) {
          glColorPointer(array.getComponentCount(), array.getComponentType(), array.getStride(), array.getVBOOffset());
      } else {
          glColorPointer(array.getComponentCount(), array.getComponentType(), array.getStride(), array.getBuffer());
      }

    }
    @Override
    public final void glNormalPointer(GLArrayData array) {
      if(array.getComponentCount()==0) return;
      if(array.getComponentCount()!=3) {
        throw new GLException("Only 3 components per normal allowed");
      }
      if(array.isVBO()) {
          glNormalPointer(array.getComponentType(), array.getStride(), array.getVBOOffset());
      } else {
          glNormalPointer(array.getComponentType(), array.getStride(), array.getBuffer());
      }
    }
    @Override
    public final void glTexCoordPointer(GLArrayData array) {
      if(array.getComponentCount()==0) return;
      if(array.isVBO()) {
          glTexCoordPointer(array.getComponentCount(), array.getComponentType(), array.getStride(), array.getVBOOffset());
      } else {
          glTexCoordPointer(array.getComponentCount(), array.getComponentType(), array.getStride(), array.getBuffer());
      }
    }

