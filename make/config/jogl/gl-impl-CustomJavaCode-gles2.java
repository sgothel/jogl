// Tracks glBegin/glEnd calls to determine whether it is legal to
// query Vertex Buffer Object state
private boolean inBeginEndPair;

public GLES2Impl(GLProfile glp, GLContextImpl context) {
  this._context = context; 
  this.bufferSizeTracker  = context.getBufferSizeTracker();
  this.bufferStateTracker = context.getBufferStateTracker();
  this.glStateTracker     = context.getGLStateTracker();
  this.glProfile = glp;
}

public final boolean isGL4bc() {
    return false;
}

public final boolean isGL4() {
    return false;
}

public final boolean isGL3bc() {
    return false;
}

public final boolean isGL3() {
    return false;
}

public final boolean isGL2() {
    return false;
}

public final boolean isGLES1() {
    return false;
}

public final boolean isGLES2() {
    return true;
}

public final boolean isGLES() {
    return true;
}

public final boolean isGL2ES1() {
    return false;
}

public final boolean isGL2ES2() {
    return true;
}

public final boolean isGLES2Compatible() {
    return true;
}

public final boolean isGL2GL3() {
    return false;
}

public final boolean hasGLSL() {
    return true;
}

public final GL4bc getGL4bc() throws GLException {
    throw new GLException("Not a GL4bc implementation");
}

public final GL4 getGL4() throws GLException {
    throw new GLException("Not a GL4 implementation");
}

public final GL3bc getGL3bc() throws GLException {
    throw new GLException("Not a GL3bc implementation");
}

public final GL3 getGL3() throws GLException {
    throw new GLException("Not a GL3 implementation");
}

public final GL2 getGL2() throws GLException {
    throw new GLException("Not a GL2 implementation");
}

public final GLES1 getGLES1() throws GLException {
    throw new GLException("Not a GLES1 implementation");
}

public final GLES2 getGLES2() throws GLException {
    return this;
}

public final GL2ES1 getGL2ES1() throws GLException {
    throw new GLException("Not a GL2ES1 implementation");
}

public final GL2ES2 getGL2ES2() throws GLException {
    return this;
}

public final GL2GL3 getGL2GL3() throws GLException {
    throw new GLException("Not a GL2GL3 implementation");
}

//
// Helpers for ensuring the correct amount of texture data
//

private GLBufferSizeTracker  bufferSizeTracker;
private GLBufferStateTracker bufferStateTracker;
private GLStateTracker       glStateTracker;

private boolean bufferObjectExtensionsInitialized = false;
private boolean haveOESFramebufferObject;

private void initBufferObjectExtensionChecks() {
  if (bufferObjectExtensionsInitialized)
    return;
  bufferObjectExtensionsInitialized = true;
  haveOESFramebufferObject  = isExtensionAvailable("GL_OES_framebuffer_object");
}

private boolean checkBufferObject(boolean avail,
                                  boolean enabled,
                                  int state,
                                  String kind, boolean throwException) {
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
  return checkBufferObject(true,
                    false,
                    GL.GL_ARRAY_BUFFER,
                    "array vertex_buffer_object", throwException);
}

private boolean checkArrayVBOEnabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(true,
                    true,
                    GL.GL_ARRAY_BUFFER,
                    "array vertex_buffer_object", throwException);
}

private boolean checkElementVBODisabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(true,
                    false,
                    GL.GL_ELEMENT_ARRAY_BUFFER,
                    "element vertex_buffer_object", throwException);
}

private boolean checkElementVBOEnabled(boolean throwException) { 
  initBufferObjectExtensionChecks();
  return checkBufferObject(true,
                    true,
                    GL.GL_ELEMENT_ARRAY_BUFFER,
                    "element vertex_buffer_object", throwException);
}

private boolean checkUnpackPBODisabled(boolean throwException) { 
    // PBO n/a for ES 1.1 or ES 2.0
    return true;
}

private boolean checkUnpackPBOEnabled(boolean throwException) { 
    // PBO n/a for ES 1.1 or ES 2.0
    return false;
}

private boolean checkPackPBODisabled(boolean throwException) { 
    // PBO n/a for ES 1.1 or ES 2.0
    return true;
}

private boolean checkPackPBOEnabled(boolean throwException) { 
    // PBO n/a for ES 1.1 or ES 2.0
    return false;
}

private HashMap/*<MemoryObject>*/ arbMemCache = new HashMap();

/** Entry point to C language function: <br> <code> LPVOID glMapBuffer(GLenum target, GLenum access); </code>    */
public java.nio.ByteBuffer glMapBuffer(int target, int access) {
  final long __addr_ = ((GLES2ProcAddressTable)_context.getGLProcAddressTable())._addressof_glMapBuffer;
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

native private ByteBuffer newDirectByteBuffer(long addr, long capacity);

public void glClearDepth(double depth) {
    glClearDepthf((float)depth); 
}

public void glDepthRange(double zNear, double zFar) {
    glDepthRangef((float)zNear, (float)zFar); 
}

