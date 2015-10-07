private final GL4bcProcAddressTable _pat;

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
  this._pat = (GL4bcProcAddressTable)_context.getGLProcAddressTable();
  this.bufferObjectTracker = context.getBufferObjectTracker();
  this.bufferStateTracker = context.getBufferStateTracker();
  this.glStateTracker     = context.getGLStateTracker();
  this.glProfile = glp;
}

public final void finalizeInit() {
  haveARBPixelBufferObject  = isExtensionAvailable("GL_ARB_pixel_buffer_object");
  haveEXTPixelBufferObject  = isExtensionAvailable("GL_EXT_pixel_buffer_object");
  haveGL15                  = isExtensionAvailable("GL_VERSION_1_5");
  haveGL21                  = isExtensionAvailable("GL_VERSION_2_1");
  haveARBVertexBufferObject = isExtensionAvailable("GL_ARB_vertex_buffer_object");
  haveARBVertexArrayObject  = _context.getGLVersionNumber().compareTo(GLContext.Version3_0) >= 0 ||
                              isExtensionAvailable("GL_ARB_vertex_array_object");
}

private int[] imageSizeTemp = new int[1];

private final int imageSizeInBytes(int format, int type, int width, int height, int depth, boolean pack) {
    return GLBuffers.sizeof(this, imageSizeTemp, format, type, width, height, depth, pack) ;                                    
}

@Override
public final boolean isGL4bc() {
    return _context.isGL4bc();
}

@Override
public final boolean isGL4() {
    return _context.isGL4();
}

@Override
public final boolean isGL3bc() {
    return _context.isGL3bc();
}

@Override
public final boolean isGL3() {
    return _context.isGL3();
}

@Override
public final boolean isGL2() {
    return _context.isGL2();
}
  
@Override
public final boolean isGL2ES1() {
    return _context.isGL2ES1();
}

@Override
public final boolean isGL2ES2() {
    return _context.isGL2ES2();
}

@Override
public final boolean isGL2ES3() {
    return _context.isGL2ES3();
}

@Override
public final boolean isGL3ES3() {
    return _context.isGL3ES3();
}

@Override
public final boolean isGL4ES3() {
    return _context.isGL4ES3();
}

@Override
public final boolean isGL4core() {
    return _context.isGL4core();
}

@Override
public final boolean isGL3core() {
    return _context.isGL3core();
}

@Override
public final boolean isGLcore() {
    return _context.isGLcore();
}

@Override
public final boolean isGLES2Compatible() {
    return _context.isGLES2Compatible();
}

@Override
public final boolean isGLES3Compatible() {
    return _context.isGLES3Compatible();
}

@Override
public final boolean isGLES31Compatible() {
    return _context.isGLES31Compatible();
}

@Override
public final boolean isGLES32Compatible() {
    return _context.isGLES32Compatible();
}

@Override
public final boolean isGL2GL3() {
    return _context.isGL2GL3();
}

@Override
public final boolean hasGLSL() {
    return _context.hasGLSL();
}

@Override
public final GL4bc getGL4bc() throws GLException {
    if(!isGL4bc()) {
        throw new GLException("Not a GL4bc implementation");
    }
    return this;
}

@Override
public final GL4 getGL4() throws GLException {
    if(!isGL4()) {
        throw new GLException("Not a GL4 implementation");
    }
    return this;
}

@Override
public final GL3bc getGL3bc() throws GLException {
    if(!isGL3bc()) {
        throw new GLException("Not a GL3bc implementation");
    }
    return this;
}

@Override
public final GL3 getGL3() throws GLException {
    if(!isGL3()) {
        throw new GLException("Not a GL3 implementation");
    }
    return this;
}

@Override
public final GL2 getGL2() throws GLException {
    if(!isGL2()) {
        throw new GLException("Not a GL2 implementation");
    }
    return this;
}

@Override
public final GL2ES1 getGL2ES1() throws GLException {
    if(!isGL2ES1()) {
        throw new GLException("Not a GL2ES1 implementation");
    }
    return this;
}

@Override
public final GL2ES2 getGL2ES2() throws GLException {
    if(!isGL2ES2()) {
        throw new GLException("Not a GL2ES2 implementation");
    }
    return this;
}

@Override
public final GL2ES3 getGL2ES3() throws GLException {
    if(!isGL2ES3()) {
        throw new GLException("Not a GL2ES3 implementation");
    }
    return this;
}

@Override
public final GL3ES3 getGL3ES3() throws GLException {
    if(!isGL3ES3()) {
        throw new GLException("Not a GL3ES3 implementation");
    }
    return this;
}

@Override
public final GL4ES3 getGL4ES3() throws GLException {
    if(!isGL4ES3()) {
        throw new GLException("Not a GL4ES3 implementation");
    }
    return this;
}

@Override
public final GL2GL3 getGL2GL3() throws GLException {
    if(!isGL2GL3()) {
        throw new GLException("Not a GL2GL3 implementation");
    }
    return this;
}

@Override
public final boolean isGLES1() {
    return false;
}

@Override
public final boolean isGLES2() {
    return false;
}

@Override
public final boolean isGLES3() {
    return false;
}

@Override
public final boolean isGLES() {
    return false;
}

@Override
public final GLES1 getGLES1() throws GLException {
    throw new GLException("Not a GLES1 implementation");
}

@Override
public final GLES2 getGLES2() throws GLException {
    throw new GLException("Not a GLES2 implementation");
}

@Override
public final GLES3 getGLES3() throws GLException {
    throw new GLException("Not a GLES3 implementation");
}

@Override
public final boolean isNPOTTextureAvailable() {
  return _context.isNPOTTextureAvailable();
}

//
// Helpers for ensuring the correct amount of texture data
//

private boolean haveARBPixelBufferObject;
private boolean haveEXTPixelBufferObject;
private boolean haveGL15;
private boolean haveGL21;
private boolean haveARBVertexBufferObject;
private boolean haveARBVertexArrayObject;

private final boolean checkBufferObject(boolean extensionAvail,
                                        boolean allowVAO,
                                        boolean bound,
                                        int state,
                                        String kind, boolean throwException) {
  if ( inBeginEndPair ) {
    throw new GLException("May not call this between glBegin and glEnd");
  }
  if ( !extensionAvail ) {
    if ( !bound ) {
      return true;
    }
    if(throwException) {
        throw new GLException("Required extensions not available to call this function");
    }
    return false;
  }
  int buffer = bufferStateTracker.getBoundBufferObject(state, this);
  if ( bound ) {
    if ( 0 != buffer ) {
        return true;
    }
    if ( allowVAO ) {
        buffer = bufferStateTracker.getBoundBufferObject(GL2ES3.GL_VERTEX_ARRAY_BINDING, this);
        if( 0 != buffer && _context.getDefaultVAO() != buffer ) {
            return true;
        }
    }
    if ( throwException ) {
        throw new GLException(kind + " must be bound to call this method");
    }
    return false;
  } else {
    if ( 0 == buffer ) {
        return true;
    }
    if ( throwException ) {
        throw new GLException(kind + " must be unbound to call this method");
    }
    return false;
  }
}  

private final void validateCPUSourcedAvail() {
    if(!_context.isCPUDataSourcingAvail()) {
        throw new GLException("CPU data sourcing n/a w/ "+_context);
    }
}

private final boolean checkArrayVBOUnbound(boolean throwException) { 
  if(throwException) {
      validateCPUSourcedAvail();
  }
  return checkBufferObject(haveGL15 || haveARBVertexBufferObject,
                           haveARBVertexArrayObject, // allowVAO
                           false, // bound
                           GL.GL_ARRAY_BUFFER,
                           "array vertex_buffer_object", throwException);
}

private final boolean checkArrayVBOBound(boolean throwException) { 
  return checkBufferObject(haveGL15 || haveARBVertexBufferObject,
                           haveARBVertexArrayObject, // allowVAO
                           true, // bound
                           GL.GL_ARRAY_BUFFER,
                           "array vertex_buffer_object", throwException);
}

private final boolean checkElementVBOUnbound(boolean throwException) { 
  if(throwException) {
      validateCPUSourcedAvail();
  }
  return checkBufferObject(haveGL15 || haveARBVertexBufferObject,
                           haveARBVertexArrayObject, // allowVAO
                           false, // bound
                           GL.GL_ELEMENT_ARRAY_BUFFER,
                           "element vertex_buffer_object", throwException);
}

private final boolean checkElementVBOBound(boolean throwException) { 
  return checkBufferObject(haveGL15 || haveARBVertexBufferObject,
                           haveARBVertexArrayObject, // allowVAO
                           true, // bound
                           GL.GL_ELEMENT_ARRAY_BUFFER,
                           "element vertex_buffer_object", throwException);
}

private final boolean checkIndirectVBOUnbound(boolean throwException) { 
  if(throwException) {
      validateCPUSourcedAvail();
  }
  return checkBufferObject(haveGL15 || haveARBVertexBufferObject,
                           haveARBVertexArrayObject, // allowVAO
                           false, // bound
                           GL4ES3.GL_DRAW_INDIRECT_BUFFER,
                           "indirect vertex_buffer_object", throwException);
}

private final boolean checkIndirectVBOBound(boolean throwException) { 
  return checkBufferObject(haveGL15 || haveARBVertexBufferObject,
                           haveARBVertexArrayObject, // allowVAO
                           true, // bound
                           GL4ES3.GL_DRAW_INDIRECT_BUFFER,
                           "indirect vertex_buffer_object", throwException);
}

private final boolean checkUnpackPBOUnbound(boolean throwException) { 
  return checkBufferObject(haveGL21 || haveARBPixelBufferObject || haveEXTPixelBufferObject,
                           false, // allowVAO
                           false, // bound
                           GL2.GL_PIXEL_UNPACK_BUFFER,
                           "unpack pixel_buffer_object", throwException);
}

private final boolean checkUnpackPBOBound(boolean throwException) { 
  return checkBufferObject(haveGL21 || haveARBPixelBufferObject || haveEXTPixelBufferObject,
                           false, // allowVAO
                           true, // bound
                           GL2.GL_PIXEL_UNPACK_BUFFER,
                           "unpack pixel_buffer_object", throwException);
}

private final boolean checkPackPBOUnbound(boolean throwException) { 
  return checkBufferObject(haveGL21 || haveARBPixelBufferObject || haveEXTPixelBufferObject,
                           false, // allowVAO
                           false, // bound
                           GL2.GL_PIXEL_PACK_BUFFER,
                           "pack pixel_buffer_object", throwException);
}

private final boolean checkPackPBOBound(boolean throwException) { 
  return checkBufferObject(haveGL21 || haveARBPixelBufferObject || haveEXTPixelBufferObject,
                           false, // allowVAO
                           true, // bound
                           GL2.GL_PIXEL_PACK_BUFFER,
                           "pack pixel_buffer_object", throwException);
}

@Override
public final boolean isPBOPackBound() {
    return checkPackPBOBound(false);
}

@Override
public final boolean isPBOUnpackBound() {
    return checkUnpackPBOBound(false);
}

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

//
// GLBufferObjectTracker Redirects
//

@Override
public final void glNamedBufferData(int buffer, long size, Buffer data, int usage)  {
    bufferObjectTracker.createBufferStorage(this, 
                                            buffer, size, data, usage, 0 /* immutableFlags */,
                                            glNamedBufferDataDispatch);
}
private final jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch glNamedBufferDataDispatch = 
    new jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch() {
        public final void create(final int buffer, final long size, final Buffer data, final int mutableUsage) {
            glNamedBufferDataDelegate(buffer, size, data, mutableUsage);
        }
    };

@Override
public final void glNamedBufferDataEXT(int buffer, long size, Buffer data, int usage)  {
    bufferObjectTracker.createBufferStorage(this, 
                                            buffer, size, data, usage, 0 /* immutableFlags */,
                                            glNamedBufferDataEXTDispatch);
}
private final jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch glNamedBufferDataEXTDispatch = 
    new jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch() {
        public final void create(final int buffer, final long size, final Buffer data, final int mutableUsage) {
            glNamedBufferDataEXTDelegate(buffer, size, data, mutableUsage);
        }
    };

@Override
public final void glBufferStorage(int target, long size, Buffer data, int flags)  {
    bufferObjectTracker.createBufferStorage(bufferStateTracker, this, 
                                            target, size, data, 0 /* mutableUsage */, flags, 
                                            glBufferStorageDispatch);
}
private final jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch glBufferStorageDispatch = 
    new jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch() {
        public final void create(final int target, final long size, final Buffer data, final int immutableFlags) {
            glBufferStorageDelegate(target, size, data, immutableFlags);
        }
    };

@Override
public final void glNamedBufferStorage(int buffer, long size, Buffer data, int flags)  {
    bufferObjectTracker.createBufferStorage(this, 
                                            buffer, size, data, 0 /* mutableUsage */, flags,
                                            glNamedBufferStorageDispatch);
}
private final jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch glNamedBufferStorageDispatch = 
    new jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch() {
        public final void create(final int buffer, final long size, final Buffer data, final int immutableFlags) {
            glNamedBufferStorageDelegate(buffer, size, data, immutableFlags);
        }
    };
@Override
public final void glNamedBufferStorageEXT(int buffer, long size, Buffer data, int flags)  {
    bufferObjectTracker.createBufferStorage(this, 
                                            buffer, size, data, 0 /* mutableUsage */, flags,
                                            glNamedBufferStorageEXTDispatch);
}
private final jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch glNamedBufferStorageEXTDispatch = 
    new jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch() {
        public final void create(final int buffer, final long size, final Buffer data, final int immutableFlags) {
            glNamedBufferStorageEXTDelegate(buffer, size, data, immutableFlags);
        }
    };

@Override
public boolean glUnmapNamedBuffer(int buffer)  {
    return bufferObjectTracker.unmapBuffer(buffer, glUnmapNamedBufferDispatch);
}
private final jogamp.opengl.GLBufferObjectTracker.UnmapBufferDispatch glUnmapNamedBufferDispatch = 
    new jogamp.opengl.GLBufferObjectTracker.UnmapBufferDispatch() {
        public final boolean unmap(final int buffer) {
            return glUnmapNamedBufferDelegate(buffer);
        }
    };

@Override
public final java.nio.ByteBuffer glMapNamedBuffer(int bufferName, int access)  {
  return mapNamedBuffer(bufferName, access).getMappedBuffer();
}
@Override
public final GLBufferStorage mapNamedBuffer(final int bufferName, final int access) {
  return bufferObjectTracker.mapBuffer(bufferName, access, glMapNamedBufferDispatch);
}
private final jogamp.opengl.GLBufferObjectTracker.MapBufferAllDispatch glMapNamedBufferDispatch = 
    new jogamp.opengl.GLBufferObjectTracker.MapBufferAllDispatch() {
        public final ByteBuffer allocNioByteBuffer(final long addr, final long length) { return newDirectByteBuffer(addr, length); }
        public final long mapBuffer(final int bufferName, final int access) {
            return glMapNamedBufferDelegate(bufferName, access);
        }
    };

@Override
public final java.nio.ByteBuffer glMapNamedBufferEXT(int bufferName, int access)  {
  return mapNamedBufferEXT(bufferName, access).getMappedBuffer();
}
@Override
public final GLBufferStorage mapNamedBufferEXT(final int bufferName, final int access) {
  return bufferObjectTracker.mapBuffer(bufferName, access, glMapNamedBufferEXTDispatch);
}
private final jogamp.opengl.GLBufferObjectTracker.MapBufferAllDispatch glMapNamedBufferEXTDispatch = 
    new jogamp.opengl.GLBufferObjectTracker.MapBufferAllDispatch() {
        public final ByteBuffer allocNioByteBuffer(final long addr, final long length) { return newDirectByteBuffer(addr, length); }
        public final long mapBuffer(final int bufferName, final int access) {
            return glMapNamedBufferEXTDelegate(bufferName, access);
        }
    };

@Override
public final ByteBuffer glMapNamedBufferRange(int bufferName, long offset, long length, int access)  {
  return mapNamedBufferRange(bufferName, offset, length, access).getMappedBuffer();
}
@Override
public final GLBufferStorage mapNamedBufferRange(final int bufferName, final long offset, final long length, final int access) {
  return bufferObjectTracker.mapBuffer(bufferName, offset, length, access, glMapNamedBufferRangeDispatch);
}
private final jogamp.opengl.GLBufferObjectTracker.MapBufferRangeDispatch glMapNamedBufferRangeDispatch = 
    new jogamp.opengl.GLBufferObjectTracker.MapBufferRangeDispatch() {
        public final ByteBuffer allocNioByteBuffer(final long addr, final long length) { return newDirectByteBuffer(addr, length); }
        public final long mapBuffer(final int bufferName, final long offset, final long length, final int access) {
            return glMapNamedBufferRangeDelegate(bufferName, offset, length, access);
        }
    };

@Override
public final ByteBuffer glMapNamedBufferRangeEXT(int bufferName, long offset, long length, int access)  {
  return mapNamedBufferRangeEXT(bufferName, offset, length, access).getMappedBuffer();
}
@Override
public final GLBufferStorage mapNamedBufferRangeEXT(final int bufferName, final long offset, final long length, final int access) {
  return bufferObjectTracker.mapBuffer(bufferName, offset, length, access, glMapNamedBufferRangeEXTDispatch);
}
private final jogamp.opengl.GLBufferObjectTracker.MapBufferRangeDispatch glMapNamedBufferRangeEXTDispatch = 
    new jogamp.opengl.GLBufferObjectTracker.MapBufferRangeDispatch() {
        public final ByteBuffer allocNioByteBuffer(final long addr, final long length) { return newDirectByteBuffer(addr, length); }
        public final long mapBuffer(final int bufferName, final long offset, final long length, final int access) {
            return glMapNamedBufferRangeEXTDelegate(bufferName, offset, length, access);
        }
    };

