    @Override
    public GLProfile getGLProfile() {
        return this.glProfile;
    }
    private final GLProfile glProfile;

    @Override
    public final int glGetBoundBuffer(int target) {
        return getBoundBuffer(target);
    }
    @Override
    public final int getBoundBuffer(int target) {
        return bufferStateTracker.getBoundBufferObject(target, this);
    }

    @Override
    public final long glGetBufferSize(int bufferName) {
        return bufferObjectTracker.getBufferSize(bufferName);
    }
    @Override
    public final GLBufferStorage getBufferStorage(int bufferName) {
        return bufferObjectTracker.getBufferStorage(bufferName);
    }

    @Override
    public final boolean glIsVBOArrayBound() {
        return isVBOArrayBound();
    }
    @Override
    public final boolean isVBOArrayBound() {
        return checkArrayVBOBound(false);
    }

    @Override
    public final boolean glIsVBOElementArrayBound() {
        return isVBOElementArrayBound();
    }
    @Override
    public final boolean isVBOElementArrayBound() {
        return checkElementVBOBound(false);
    }

    @Override
    public final GL getDownstreamGL() throws GLException {
        return null;
    }

    @Override
    public final GL getRootGL() throws GLException {
        return this;
    }

    @Override
    public final boolean isGL() {
        return true;
    }
      
    @Override
    public final GL getGL() throws GLException {
        return this;
    }

    @Override
    public final boolean isFunctionAvailable(String glFunctionName) {
      return _context.isFunctionAvailable(glFunctionName);
    }

    @Override
    public final boolean isExtensionAvailable(String glExtensionName) {
      return _context.isExtensionAvailable(glExtensionName);
    }

    @Override
    public final Object getExtension(String extensionName) {
      // At this point we don't expose any extensions using this mechanism
      return null;
    }

    @Override
    public final boolean hasBasicFBOSupport() {
      return _context.hasBasicFBOSupport();
    }

    @Override
    public final boolean hasFullFBOSupport() {
      return _context.hasFullFBOSupport();
    }

    @Override
    public final int getMaxRenderbufferSamples() {
      return _context.getMaxRenderbufferSamples();
    }

    @Override
    public final boolean isTextureFormatBGRA8888Available() {
      return _context.isTextureFormatBGRA8888Available();
    }

    @Override
    public final GLContext getContext() {
      return _context;
    }

    private final GLContextImpl _context;

    /**
     * @see javax.media.opengl.GLContext#setSwapInterval(int)
     */
    @Override
    public final void setSwapInterval(int interval) {
      _context.setSwapInterval(interval);
    }

    /**
     * @see javax.media.opengl.GLContext#getSwapInterval()
     */
    @Override
    public final int getSwapInterval() {
      return _context.getSwapInterval();
    }

    @Override
    public final Object getPlatformGLExtensions() {
      return _context.getPlatformGLExtensions();
    }

    @Override
    public final int getBoundFramebuffer(int target) {
      return _context.getBoundFramebuffer(target);
    }

    @Override
    public final int getDefaultDrawFramebuffer() {
      return _context.getDefaultDrawFramebuffer();
    }

    @Override
    public final int getDefaultReadFramebuffer() {
      return _context.getDefaultReadFramebuffer();
    }

    @Override
    public final int getDefaultReadBuffer() {
      return _context.getDefaultReadBuffer();
    }

    private final GLStateTracker       glStateTracker;

    //
    // GLBufferObjectTracker Redirects
    //
    private final GLBufferObjectTracker bufferObjectTracker;
    private final GLBufferStateTracker bufferStateTracker;

    private final jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch createBoundMutableStorageDispatch = 
        new jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch() {
            public final void create(final int target, final long size, final Buffer data, final int mutableUsage, final long glProcAddress) {
                final boolean data_is_direct = Buffers.isDirect(data);
                dispatch_glBufferData(target, size, 
                                      data_is_direct ? data : Buffers.getArray(data), 
                                      data_is_direct ? Buffers.getDirectBufferByteOffset(data) : Buffers.getIndirectBufferByteOffset(data), 
                                      data_is_direct, mutableUsage, glProcAddress);
            }
        };
    private native void dispatch_glBufferData(int target, long size, Object data, int data_byte_offset, boolean data_is_direct, int usage, long procAddress);

    private final jogamp.opengl.GLBufferObjectTracker.UnmapBufferDispatch unmapBoundBufferDispatch = 
        new jogamp.opengl.GLBufferObjectTracker.UnmapBufferDispatch() {
            public final boolean unmap(final int target, final long glProcAddress) {
                return dispatch_glUnmapBuffer(target, glProcAddress);
            }
        };
    private native boolean dispatch_glUnmapBuffer(int target, long procAddress);

    @Override
    public final java.nio.ByteBuffer glMapBuffer(int target, int access) {
      return mapBuffer(target, access).getMappedBuffer();
    }

    @Override
    public final ByteBuffer glMapBufferRange(int target, long offset, long length, int access)  {
      return mapBufferRange(target, offset, length, access).getMappedBuffer();
    }

    private final jogamp.opengl.GLBufferObjectTracker.MapBufferAllDispatch mapBoundBufferAllDispatch = 
        new jogamp.opengl.GLBufferObjectTracker.MapBufferAllDispatch() {
            public final ByteBuffer allocNioByteBuffer(final long addr, final long length) { return newDirectByteBuffer(addr, length); }
            public final long mapBuffer(final int target, final int access, final long glProcAddress) {
                return dispatch_glMapBuffer(target, access, glProcAddress);
            }
        };
    private native long dispatch_glMapBuffer(int target, int access, long glProcAddress);

    private final jogamp.opengl.GLBufferObjectTracker.MapBufferRangeDispatch mapBoundBufferRangeDispatch = 
        new jogamp.opengl.GLBufferObjectTracker.MapBufferRangeDispatch() {
            public final ByteBuffer allocNioByteBuffer(final long addr, final long length) { return newDirectByteBuffer(addr, length); }
            public final long mapBuffer(final int target, final long offset, final long length, final int access, final long glProcAddress) {
                return dispatch_glMapBufferRange(target, offset, length, access, glProcAddress);
            }
        };
    private native long dispatch_glMapBufferRange(int target, long offset, long length, int access, long glProcAddress);

    private native ByteBuffer newDirectByteBuffer(long addr, long capacity);
