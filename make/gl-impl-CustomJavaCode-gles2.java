// Tracks glBegin/glEnd calls to determine whether it is legal to
// query Vertex Buffer Object state
private boolean inBeginEndPair;

public GLES2Impl(GLContextImpl context) {
  this._context = context; 
  this.bufferSizeTracker = context.getBufferSizeTracker();
  this.enableFixedFunctionEmulationMode(FIXED_EMULATION_MATRIX);
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
  case GL_STENCIL_INDEX:
    elements = 1;
    break;
  case GL_ALPHA:
  case GL_LUMINANCE:
  case GL_DEPTH_COMPONENT:
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
  case GL_INT:
  case GL_UNSIGNED_INT:
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
  final long __addr_ = ((GLES2ProcAddressTable)_context.getGLProcAddressTable())._addressof_glMapBuffer;
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

public void glClearDepth(double depth) {
    glClearDepthf((float)depth); 
}

public void glDepthRange(double zNear, double zFar) {
    glDepthRangef((float)zNear, (float)zFar); 
}

protected int fixedFunctionEmulationMode = 0;

protected boolean fixedFunctionShaderActive=false;
protected FixedFuncShader fixedFunction=null;
protected ShaderData shaderData=null;

protected boolean fixedFunctionMatrixEnabled=false;
protected PMVMatrix pmvMatrix = null;

public void enableFixedFunctionEmulationMode(int modes) {
    // clear unsupported modes
    modes = modes & ~FIXED_EMULATION_NORMALLIGHT;

    fixedFunctionEmulationMode|=modes;

    if( 0 != (modes & FIXED_EMULATION_MATRIX ) ) {
      if ( !fixedFunctionMatrixEnabled) {
          // setup ressources
          fixedFunctionMatrixEnabled=true;

          pmvMatrix = new PMVMatrix();
      }
    }

    // currently only for shader type: FIXED_EMULATION_VERTEXCOLOR
    if( 0 != (modes & FIXED_EMULATION_VERTEXCOLOR ) ) {
          fixedFunctionShaderActive=true;
          if(null==fixedFunction) {
            if( 0 != (modes & FIXED_EMULATION_TEXTURE ) ) {
                shaderData = new FixedFuncShaderVertexColorTexture();
            } else {
                shaderData = new FixedFuncShaderVertexColor();
            }
            fixedFunction = new FixedFuncShader(this, pmvMatrix, shaderData);
          }
    }
}

public void disableFixedFunctionEmulationMode(int modes) {
    // clear unsupported modes
    modes = modes & ~FIXED_EMULATION_NORMALLIGHT;

    fixedFunctionEmulationMode&=~modes;

    if( 0 != (modes & FIXED_EMULATION_MATRIX ) ) {
      if ( fixedFunctionMatrixEnabled) {
          // release ressources
          fixedFunctionMatrixEnabled=false;
          pmvMatrix = null;
      }
    }

    // currently only for shader type: FIXED_EMULATION_VERTEXCOLOR
    if( 0 != (modes & FIXED_EMULATION_VERTEXCOLOR ) ) {
          if(null!=fixedFunction) {
            fixedFunction.release(this);
          }
          fixedFunctionShaderActive=false;
    }
}

public int  getEnabledFixedFunctionEmulationModes() {
    return fixedFunctionEmulationMode;
}

public PMVMatrix getPMVMatrix() {
    return pmvMatrix;
}

public int  glGetMatrixMode() {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    return pmvMatrix.glGetMatrixMode();
}
public void glMatrixMode(int mode) {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    pmvMatrix.glMatrixMode(mode);
}
public final FloatBuffer glGetPMVMatrixf() {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    return pmvMatrix.glGetPMVMatrixf();
}
public FloatBuffer glGetMatrixf(int matrixName) {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    return pmvMatrix.glGetMatrixf(matrixName);
}

public FloatBuffer glGetMatrixf() {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    return pmvMatrix.glGetMatrixf();
}

public void glLoadMatrixf(java.nio.FloatBuffer m) {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    pmvMatrix.glLoadMatrixf(m);
}
public void glLoadMatrixf(float[] m, int m_offset) {
    glLoadMatrixf(BufferUtil.newFloatBuffer(m, m_offset));
}
public void glPopMatrix() {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    pmvMatrix.glPopMatrix();
}

public void glPushMatrix() {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    pmvMatrix.glPushMatrix();
}

public void glLoadIdentity() {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    pmvMatrix.glLoadIdentity();
}
public void glMultMatrixf(java.nio.FloatBuffer m) {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    pmvMatrix.glMultMatrixf(m);
}
public void glMultMatrixf(float[] m, int m_offset) {
    glMultMatrixf(BufferUtil.newFloatBuffer(m, m_offset));
}
public void glTranslatef(float x, float y, float z) {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    pmvMatrix.glTranslatef(x, y, z);
}
public void glRotatef(float angdeg, float x, float y, float z) {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    pmvMatrix.glRotatef(angdeg, x, y, z);
}

public void glScalef(float x, float y, float z) {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    pmvMatrix.glScalef(x, y, z);
}
public void glOrthof(float left, float right, float bottom, float top, float zNear, float zFar) {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    pmvMatrix.glOrthof(left, right, bottom, top, zNear, zFar);
}
public void glFrustumf(float left, float right, float bottom, float top, float zNear, float zFar) {
    if(!fixedFunctionMatrixEnabled) {
        throw new GLUnsupportedException("not enabled");
    }
    pmvMatrix.glFrustumf(left, right, bottom, top, zNear, zFar);
}

public void glEnableClientState(int glArrayName) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  fixedFunction.glEnableClientState(this, glArrayName);
}
public void glDisableClientState(int glArrayName) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  fixedFunction.glDisableClientState(this, glArrayName);
}
public void glVertexPointer(int size, int type, int stride, java.nio.Buffer pointer) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  checkArrayVBODisabled();
  BufferFactory.rangeCheck(pointer, 1);
  if (!BufferFactory.isDirect(pointer)) {
    throw new GLException("Argument \"pointer\" was not a direct buffer"); }
  fixedFunction.glVertexPointer(this, size, type, stride, pointer);
}
public void glVertexPointer(int size, int type, int stride, long pointer_buffer_offset) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  checkArrayVBOEnabled();
  fixedFunction.glVertexPointer(this, size, type, stride, pointer_buffer_offset);
}

public void glColorPointer(int size, int type, int stride, java.nio.Buffer pointer) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  checkArrayVBODisabled();
  BufferFactory.rangeCheck(pointer, 1);
  if (!BufferFactory.isDirect(pointer)) {
    throw new GLException("Argument \"pointer\" was not a direct buffer"); }
  fixedFunction.glColorPointer(this, size, type, stride, pointer);
}
public void glColorPointer(int size, int type, int stride, long pointer_buffer_offset) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  checkArrayVBOEnabled();
  fixedFunction.glColorPointer(this, size, type, stride, pointer_buffer_offset);
}
public void glColor4f(float red, float green, float blue, float alpha) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  fixedFunction.glColor4fv(this, BufferUtil.newFloatBuffer(new float[] { red, green, blue, alpha })); 
}

public void glNormalPointer(int type, int stride, java.nio.Buffer pointer) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  checkArrayVBODisabled();
  BufferFactory.rangeCheck(pointer, 1);
  if (!BufferFactory.isDirect(pointer)) {
    throw new GLException("Argument \"pointer\" was not a direct buffer"); }
  fixedFunction.glNormalPointer(this, type, stride, pointer);
}
public void glNormalPointer(int type, int stride, long pointer_buffer_offset) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  checkArrayVBOEnabled();
  fixedFunction.glNormalPointer(this, type, stride, pointer_buffer_offset);
}

public void glTexCoordPointer(int size, int type, int stride, java.nio.Buffer pointer) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  checkArrayVBODisabled();
  BufferFactory.rangeCheck(pointer, 1);
  if (!BufferFactory.isDirect(pointer)) {
    throw new GLException("Argument \"pointer\" was not a direct buffer"); }
  fixedFunction.glTexCoordPointer(this, size, type, stride, pointer);
}
public void glTexCoordPointer(int size, int type, int stride, long pointer_buffer_offset) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  checkArrayVBOEnabled();
  fixedFunction.glTexCoordPointer(this, size, type, stride, pointer_buffer_offset);
}
private final void glDrawArraysPrologue() {
    if(fixedFunctionShaderActive) { 
        fixedFunction.syncUniforms(this); 
    }
}
private final void glDrawArraysEpilogue() {
    if(fixedFunctionShaderActive) { 
        fixedFunction.glUseProgram(this, false);
    }
}
public void glLightfv(int light, int pname, java.nio.FloatBuffer params) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  fixedFunction.glLightfv(this, light, pname, params);
}
public void glLightfv(int light, int pname, float[] params, int params_offset) {
    glLightfv(light, pname, BufferUtil.newFloatBuffer(params, params_offset));
}
public void glShadeModel(int mode) {
  if(!fixedFunctionShaderActive) {
    throw new GLUnsupportedException("not enabled");
  }
  fixedFunction.glShadeModel(this, mode);
}

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
      buf.append(", fixedEmul: [ ");
      if( 0 != (fixedFunctionEmulationMode & FIXED_EMULATION_MATRIX) ) {
          buf.append("FIXED_EMULATION_MATRIX ");
      }
      if( 0 != (fixedFunctionEmulationMode & FIXED_EMULATION_VERTEXCOLOR) ) {
          buf.append("FIXED_EMULATION_VERTEXCOLOR ");
      }
      if( 0 != (fixedFunctionEmulationMode & FIXED_EMULATION_TEXTURE) ) {
          buf.append("FIXED_EMULATION_TEXTURE ");
      }
      if( 0 != (fixedFunctionEmulationMode & FIXED_EMULATION_NORMALLIGHT) ) {
          buf.append("FIXED_EMULATION_NORMALLIGHT ");
      }
      buf.append("], matrixEnabled: "+fixedFunctionMatrixEnabled);
      buf.append(", shaderData: "+shaderData);
      buf.append(", shaderActive: "+fixedFunctionShaderActive);
      if(null!=pmvMatrix) {
          buf.append(", matrixDirty: "+pmvMatrix.isDirty());
      }
      buf.append(" )");

      return buf.toString();
}


