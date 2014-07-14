
public GLES3Impl(GLProfile glp, GLContextImpl context) {
  this._context = context; 
  if(null != context) {
      this.bufferObjectTracker  = context.getBufferObjectTracker();
      this.bufferStateTracker = context.getBufferStateTracker();
      this.glStateTracker     = context.getGLStateTracker();
  } else {
      this.bufferObjectTracker  = null;
      this.bufferStateTracker = null;
      this.glStateTracker     = null;
  }
  this.glProfile = glp;
  this._isES3 = glp.getImplName() == GLProfile.GLES3;
}

public final void finalizeInit() {
}

@Override
public final boolean isGL4bc() {
    return false;
}

@Override
public final boolean isGL4() {
    return false;
}

@Override
public final boolean isGL3bc() {
    return false;
}

@Override
public final boolean isGL3() {
    return false;
}

public final boolean isGL2() {
    return false;
}

@Override
public final boolean isGLES1() {
    return false;
}

@Override
public final boolean isGLES2() {
    return true;
}

@Override
public final boolean isGLES3() {
    return _isES3;
}

@Override
public final boolean isGLES() {
    return true;
}

@Override
public final boolean isGL2ES1() {
    return false;
}

@Override
public final boolean isGL2ES2() {
    return true;
}

@Override
public final boolean isGL2ES3() {
    return _isES3;
}

@Override
public final boolean isGL3ES3() {
    return _isES3;
}

@Override
public final boolean isGL4ES3() {
    return _isES3;
}

@Override
public final boolean isGL4core() {
    return false;
}

@Override
public final boolean isGL3core() {
    return false;
}

@Override
public final boolean isGLcore() {
    return true;
}

@Override
public final boolean isGLES2Compatible() {
    return true;
}

@Override
public final boolean isGLES3Compatible() {
    return _isES3;
}


@Override
public final boolean isGL2GL3() {
    return false;
}

@Override
public final boolean hasGLSL() {
    return true;
}

@Override
public boolean isNPOTTextureAvailable() {
  return true;
}

@Override
public final GL4bc getGL4bc() throws GLException {
    throw new GLException("Not a GL4bc implementation");
}

@Override
public final GL4 getGL4() throws GLException {
    throw new GLException("Not a GL4 implementation");
}

@Override
public final GL3bc getGL3bc() throws GLException {
    throw new GLException("Not a GL3bc implementation");
}

@Override
public final GL3 getGL3() throws GLException {
    throw new GLException("Not a GL3 implementation");
}

@Override
public final GL2 getGL2() throws GLException {
    throw new GLException("Not a GL2 implementation");
}

@Override
public final GLES1 getGLES1() throws GLException {
    throw new GLException("Not a GLES1 implementation");
}

@Override
public final GLES2 getGLES2() throws GLException {
    return this;
}

@Override
public final GLES3 getGLES3() throws GLException {
    return this;
}

@Override
public final GL2ES1 getGL2ES1() throws GLException {
    throw new GLException("Not a GL2ES1 implementation");
}

@Override
public final GL2ES2 getGL2ES2() throws GLException {
    return this;
}

@Override
public final GL2ES3 getGL2ES3() throws GLException {
    if(!_isES3) {
        throw new GLException("Not a GL2ES3 implementation");
    }
    return this;
}

@Override
public final GL3ES3 getGL3ES3() throws GLException {
    if(!_isES3) {
        throw new GLException("Not a GL3ES3 implementation");
    }
    return this;
}

@Override
public final GL4ES3 getGL4ES3() throws GLException {
    if(!_isES3) {
        throw new GLException("Not a GL4ES3 implementation");
    }
    return this;
}

@Override
public final GL2GL3 getGL2GL3() throws GLException {
    throw new GLException("Not a GL2GL3 implementation");
}

//
// Helpers for ensuring the correct amount of texture data
//

private final boolean _isES3;

private final boolean checkBufferObject(boolean extensionAvail,
                                        boolean allowVAO,
                                        boolean bound,
                                        int state,
                                        String kind, boolean throwException) {
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
  return checkBufferObject(true,
                           _isES3, // allowVAO
                           false, // bound
                           GL.GL_ARRAY_BUFFER,
                           "array vertex_buffer_object", throwException);
}

private final boolean checkArrayVBOBound(boolean throwException) { 
  return checkBufferObject(true,
                           _isES3, // allowVAO
                           true, // bound
                           GL.GL_ARRAY_BUFFER,
                           "array vertex_buffer_object", throwException);
}

private final boolean checkElementVBOUnbound(boolean throwException) { 
  if(throwException) {
      validateCPUSourcedAvail();
  }
  return checkBufferObject(true,
                           _isES3, // allowVAO
                           false, // bound
                           GL.GL_ELEMENT_ARRAY_BUFFER,
                           "element vertex_buffer_object", throwException);
}

private final boolean checkElementVBOBound(boolean throwException) { 
  return checkBufferObject(true,
                           _isES3, // allowVAO
                           true, // bound
                           GL.GL_ELEMENT_ARRAY_BUFFER,
                           "element vertex_buffer_object", throwException);
}

private final boolean checkUnpackPBOUnbound(boolean throwException) { 
  return checkBufferObject(_isES3,
                           false, // allowVAO
                           false, // bound
                           GL2.GL_PIXEL_UNPACK_BUFFER,
                           "unpack pixel_buffer_object", throwException);
}

private final boolean checkUnpackPBOBound(boolean throwException) { 
  return checkBufferObject(_isES3,
                           false, // allowVAO
                           true, // bound
                           GL2.GL_PIXEL_UNPACK_BUFFER,
                           "unpack pixel_buffer_object", throwException);
}

private final boolean checkPackPBOUnbound(boolean throwException) { 
  return checkBufferObject(_isES3,
                           false, // allowVAO
                           false, // bound
                           GL2.GL_PIXEL_PACK_BUFFER,
                           "pack pixel_buffer_object", throwException);
}

private final boolean checkPackPBOBound(boolean throwException) { 
  return checkBufferObject(_isES3,
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
public final void glClearDepth(double depth) {
    glClearDepthf((float)depth); 
}

@Override
public final void glDepthRange(double zNear, double zFar) {
    glDepthRangef((float)zNear, (float)zFar); 
}

//
// GLBufferObjectTracker Redirects
//

@Override
public final void glBufferData(int target, long size, Buffer data, int usage)  {
    final long glProcAddress = ((GLES3ProcAddressTable)_context.getGLProcAddressTable())._addressof_glBufferData;
    if ( 0 == glProcAddress ) {
      throw new GLException(String.format("Method \"%s\" not available", "glBufferData"));
    }
    bufferObjectTracker.createBufferStorage(bufferStateTracker, this, 
                                            target, size, data, usage, 0 /* immutableFlags */,
                                            createBoundMutableStorageDispatch, glProcAddress);
}

@Override
public boolean glUnmapBuffer(int target)  {
    final long glProcAddress = ((GLES3ProcAddressTable)_context.getGLProcAddressTable())._addressof_glUnmapBuffer;
    if ( 0 == glProcAddress ) {
      throw new GLException(String.format("Method \"%s\" not available", "glUnmapBuffer"));
    }
    return bufferObjectTracker.unmapBuffer(bufferStateTracker, this, target, unmapBoundBufferDispatch, glProcAddress);
}

@Override
public final GLBufferStorage mapBuffer(final int target, final int access) {
  final long glProcAddress = ((GLES3ProcAddressTable)_context.getGLProcAddressTable())._addressof_glMapBuffer;
  if ( 0 == glProcAddress ) {
    throw new GLException("Method \"glMapBuffer\" not available");
  }
  return bufferObjectTracker.mapBuffer(bufferStateTracker, this, target, access, mapBoundBufferAllDispatch, glProcAddress);
}
@Override
public final GLBufferStorage mapBufferRange(final int target, final long offset, final long length, final int access) {
  final long glProcAddress = ((GLES3ProcAddressTable)_context.getGLProcAddressTable())._addressof_glMapBufferRange;
  if ( 0 == glProcAddress ) {
    throw new GLException("Method \"glMapBufferRange\" not available");
  }
  return bufferObjectTracker.mapBuffer(bufferStateTracker, this, target, offset, length, access, mapBoundBufferRangeDispatch, glProcAddress);
}


