
  //
  // GLBufferObjectTracker Redirects
  //

  /**
   * Returns the {@link GLBufferStorage} instance as mapped via OpenGL's native {@link GL2#glMapNamedBufferEXT(int, int) glMapNamedBufferEXT(..)} implementation.
   * <p>
   * Throws a {@link GLException} if GL-function constraints are not met.
   * </p>
   * <p>
   * Depends on <code>GL_EXT_direct_state_access</code>.
   * </p>
   * <p>
   * {@link GL2#glMapNamedBufferEXT(int, int)} wrapper calls this method and returns {@link GLBufferStorage#getMappedBuffer()}.
   * </p>
   * @param bufferName denotes the buffer
   * @param access the mapping access mode
   * @throws GLException if buffer is not tracked
   * @throws GLException if buffer is already mapped
   * @throws GLException if buffer has invalid store size, i.e. less-than zero
   */
  public GLBufferStorage mapNamedBufferEXT(int bufferName, int access) throws GLException;

  /**
   * Returns the {@link GLBufferStorage} instance as mapped via OpenGL's native {@link GL2#glMapNamedBufferRangeEXT(int, long, long, int) glMapNamedBufferRangeEXT(..)} implementation.
   * <p>
   * Throws a {@link GLException} if GL-function constraints are not met.
   * </p>
   * <p>
   * Depends on <code>GL_EXT_direct_state_access</code>.
   * </p>
   * <p>
   * {@link GL2#glMapNamedBufferRangeEXT(int, long, long, int)} wrapper calls this method and returns {@link GLBufferStorage#getMappedBuffer()}.
   * </p>
   * @param bufferName denotes the buffer
   * @param offset offset of the mapped buffer's storage
   * @param length length of the mapped buffer's storage
   * @param access the mapping access mode
   * @throws GLException if buffer is not tracked
   * @throws GLException if buffer is already mapped
   * @throws GLException if buffer has invalid store size, i.e. less-than zero
   * @throws GLException if buffer mapping range does not fit, incl. offset
   */
  public GLBufferStorage mapNamedBufferRangeEXT(final int bufferName, final long offset, final long length, final int access) throws GLException;

