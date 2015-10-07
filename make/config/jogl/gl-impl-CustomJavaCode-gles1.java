private final GLES1ProcAddressTable _pat;

public GLES1Impl(GLProfile glp, GLContextImpl context) {
  this._context = context; 
  this._pat = (GLES1ProcAddressTable)_context.getGLProcAddressTable();
  this.bufferObjectTracker = context.getBufferObjectTracker();
  this.bufferStateTracker = context.getBufferStateTracker();
  this.glStateTracker     = context.getGLStateTracker();
  this.glProfile = glp;
}

public final void finalizeInit() {
}

private int[] imageSizeTemp = new int[1];

private final int imageSizeInBytes(int format, int type, int width, int height, int depth, boolean pack) {
    return GLBuffers.sizeof(this, imageSizeTemp, format, type, width, height, depth, pack) ;                                    
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

@Override
public final boolean isGL2() {
    return false;
}

@Override
public final boolean isGLES1() {
    return true;
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
    return true;
}

@Override
public final boolean isGL2ES1() {
    return true;
}

@Override
public final boolean isGL2ES2() {
    return false;
}

@Override
public final boolean isGL2ES3() {
    return false;
}

@Override
public final boolean isGL3ES3() {
    return false;
}

@Override
public final boolean isGL4ES3() {
    return false;
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
    return false;
}

@Override
public final boolean isGLES2Compatible() {
    return false;
}

@Override
public final boolean isGLES3Compatible() {
    return false;
}

@Override
public final boolean isGLES31Compatible() {
    return false;
}

@Override
public final boolean isGLES32Compatible() {
    return false;
}

@Override
public final boolean isGL2GL3() {
    return false;
}

@Override
public final boolean hasGLSL() {
    return false;
}

@Override
public boolean isNPOTTextureAvailable() {
  return false;
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
    return this;
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
public final GL2ES1 getGL2ES1() throws GLException {
    return this;
}

@Override
public final GL2ES3 getGL2ES3() throws GLException {
    throw new GLException("Not a GL2ES3 implementation");
}

@Override
public final GL2ES2 getGL2ES2() throws GLException {
    throw new GLException("Not a GL2ES2 implementation");
}

@Override
public final GL3ES3 getGL3ES3() throws GLException {
    throw new GLException("Not a GL3ES3 implementation");
}

@Override
public final GL4ES3 getGL4ES3() throws GLException {
    throw new GLException("Not a GL4ES3 implementation");
}

@Override
public final GL2GL3 getGL2GL3() throws GLException {
    throw new GLException("Not a GL2GL3 implementation");
}

//
// Helpers for ensuring the correct amount of texture data
//

private final boolean checkBufferObject(boolean bound,
                                        int state,
                                        String kind, boolean throwException) {
  final int buffer = bufferStateTracker.getBoundBufferObject(state, this);
  if (bound) {
    if (0 == buffer) {
      if(throwException) {
          throw new GLException(kind + " must be bound to call this method");
      }
      return false;
    }
  } else {
    if (0 != buffer) {
      if(throwException) {
          throw new GLException(kind + " must be unbound to call this method");
      }
      return false;
    }
  }
  return true;
}  

private final boolean checkArrayVBOUnbound(boolean throwException) { 
  return checkBufferObject(false, // bound
                           GL.GL_ARRAY_BUFFER,
                           "array vertex_buffer_object", throwException);
}

private final boolean checkArrayVBOBound(boolean throwException) { 
  return checkBufferObject(true, // bound
                           GL.GL_ARRAY_BUFFER,
                           "array vertex_buffer_object", throwException);
}

private final boolean checkElementVBOUnbound(boolean throwException) { 
  return checkBufferObject(false, // bound
                           GL.GL_ELEMENT_ARRAY_BUFFER,
                           "element vertex_buffer_object", throwException);
}

private final boolean checkElementVBOBound(boolean throwException) { 
  return checkBufferObject(true, // bound
                           GL.GL_ELEMENT_ARRAY_BUFFER,
                           "element vertex_buffer_object", throwException);
}

private final boolean checkUnpackPBOUnbound(boolean throwException) { 
    // PBO n/a for ES 1.1 or ES 2.0
    return true;
}

private final boolean checkUnpackPBOBound(boolean throwException) { 
    // PBO n/a for ES 1.1 or ES 2.0
    return false;
}

private final boolean checkPackPBOUnbound(boolean throwException) { 
    // PBO n/a for ES 1.1 or ES 2.0
    return true;
}

private final boolean checkPackPBOBound(boolean throwException) { 
    // PBO n/a for ES 1.1 or ES 2.0
    return false;
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

