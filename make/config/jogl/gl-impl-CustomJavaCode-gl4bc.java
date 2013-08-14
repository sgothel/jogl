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

public final void finalizeInit() {
  if(null != _context) {
      haveARBPixelBufferObject  = isExtensionAvailable("GL_ARB_pixel_buffer_object");
      haveEXTPixelBufferObject  = isExtensionAvailable("GL_EXT_pixel_buffer_object");
      haveGL15                  = isExtensionAvailable("GL_VERSION_1_5");
      haveGL21                  = isExtensionAvailable("GL_VERSION_2_1");
      haveARBVertexBufferObject = isExtensionAvailable("GL_ARB_vertex_buffer_object");
      haveARBVertexArrayObject  = _context.getGLVersionNumber().compareTo(GLContext.Version300) >= 0 ||
                                  isExtensionAvailable("GL_ARB_vertex_array_object");
   } else {
      haveARBPixelBufferObject  = false;
      haveEXTPixelBufferObject  = false;
      haveGL15                  = false;
      haveGL21                  = false;
      haveARBVertexBufferObject = false;
      haveARBVertexArrayObject  = false;
   }
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
@Override
public final java.nio.ByteBuffer glAllocateMemoryNV(int size, float readFrequency, float writeFrequency, float priority) {
  return _context.glAllocateMemoryNV(size, readFrequency, writeFrequency, priority);
}

@Override
public final void glFreeMemoryNV(java.nio.ByteBuffer pointer) {
  _context.glFreeMemoryNV(pointer);
}

//
// Helpers for ensuring the correct amount of texture data
//

private final GLBufferSizeTracker  bufferSizeTracker;
private final GLBufferStateTracker bufferStateTracker;
private final GLStateTracker       glStateTracker;

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
        buffer = bufferStateTracker.getBoundBufferObject(GL2GL3.GL_VERTEX_ARRAY_BINDING, this);
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

private final boolean checkArrayVBOUnbound(boolean throwException) { 
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
public final boolean glIsPBOPackBound() {
    return checkPackPBOBound(false);
}

@Override
public final boolean glIsPBOUnpackBound() {
    return checkUnpackPBOBound(false);
}

/** Entry point to C language function: <code> void *  {@native glMapBuffer}(GLenum target, GLenum access); </code> <br>Part of <code>GL_VERSION_1_5</code>; <code>GL_OES_mapbuffer</code>   */
public final java.nio.ByteBuffer glMapBuffer(int target, int access) {
  return glMapBufferImpl(target, false, 0, 0, access, ((GL4bcProcAddressTable)_context.getGLProcAddressTable())._addressof_glMapBuffer);
}

/** Entry point to C language function: <code> void *  {@native glMapBufferRange}(GLenum target, GLintptr offset, GLsizeiptr length, GLbitfield access); </code> <br>Part of <code>GL_ES_VERSION_3_0</code>, <code>GL_VERSION_3_0</code>; <code>GL_EXT_map_buffer_range</code>   */
public final ByteBuffer glMapBufferRange(int target, long offset, long length, int access)  {
  return glMapBufferImpl(target, true, offset, length, access, ((GL4bcProcAddressTable)_context.getGLProcAddressTable())._addressof_glMapBufferRange);
}

/** Entry point to C language function: <code> GLvoid *  {@native glMapNamedBufferEXT}(GLuint buffer, GLenum access); </code> <br>Part of <code>GL_EXT_direct_state_access</code>   */
public final java.nio.ByteBuffer glMapNamedBufferEXT(int bufferName, int access)  {
  return glMapNamedBufferImpl(bufferName, access, ((GL4bcProcAddressTable)_context.getGLProcAddressTable())._addressof_glMapNamedBufferEXT);
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

