/** Entry point to C language function: <br> <code> LPVOID glMapBufferARB(GLenum target, GLenum access); </code>    */
public java.nio.ByteBuffer glMapBufferARB(int target, int access) {
  final long __addr_ = context.getGLProcAddressTable()._addressof_glMapBufferARB;
  if (__addr_ == 0) {
    throw new GLException("Method \"glMapBufferARB\" not available");
  }
  int[] sz = new int[1];
  glGetBufferParameterivARB(target, GL_BUFFER_SIZE_ARB, sz);
  ByteBuffer _res;
  _res = dispatch_glMapBufferARB(target, access, sz[0], __addr_);
  if (_res == null) return null;
  return _res.order(ByteOrder.nativeOrder());
}

/** Encapsulates function pointer for OpenGL function <br>: <code> LPVOID glMapBufferARB(GLenum target, GLenum access); </code>    */
native private java.nio.ByteBuffer dispatch_glMapBufferARB(int target, int access, int size, long glProcAddress);
