    private final GLProfile glProfile;
    private final GLContextImpl _context;

    @Override
    public GLProfile getGLProfile() {
        return this.glProfile;
    }

    @Override
    public final int getBoundBuffer(int target) {
        return bufferStateTracker.getBoundBufferObject(target, this);
    }

    @Override
    public final GLBufferStorage getBufferStorage(int bufferName) {
        return bufferObjectTracker.getBufferStorage(bufferName);
    }

    @Override
    public final boolean isVBOArrayBound() {
        return checkArrayVBOBound(false);
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

    /**
     * @see com.jogamp.opengl.GLContext#setSwapInterval(int)
     */
    @Override
    public final void setSwapInterval(int interval) {
      _context.setSwapInterval(interval);
    }

    /**
     * @see com.jogamp.opengl.GLContext#getSwapInterval()
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

    @Override
    public final void glBufferData(int target, long size, Buffer data, int usage)  {
        bufferObjectTracker.createBufferStorage(bufferStateTracker, this, 
                                                target, size, data, usage, 0 /* immutableFlags */, 
                                                glBufferDataDispatch);
    }
    private final jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch glBufferDataDispatch = 
        new jogamp.opengl.GLBufferObjectTracker.CreateStorageDispatch() {
            public final void create(final int target, final long size, final Buffer data, final int mutableUsage) {
                glBufferDataDelegate(target, size, data, mutableUsage);
            }
        };

    @Override
    public boolean glUnmapBuffer(int target)  {
        return bufferObjectTracker.unmapBuffer(bufferStateTracker, this, target, glUnmapBufferDispatch);
    }
    private final jogamp.opengl.GLBufferObjectTracker.UnmapBufferDispatch glUnmapBufferDispatch = 
        new jogamp.opengl.GLBufferObjectTracker.UnmapBufferDispatch() {
            public final boolean unmap(final int target) {
                return glUnmapBufferDelegate(target);
            }
        };

    @Override
    public final java.nio.ByteBuffer glMapBuffer(int target, int access) {
      return mapBuffer(target, access).getMappedBuffer();
    }
    @Override
    public final GLBufferStorage mapBuffer(final int target, final int access) {
      return bufferObjectTracker.mapBuffer(bufferStateTracker, this, target, access, glMapBufferDispatch);
    }
    private final jogamp.opengl.GLBufferObjectTracker.MapBufferAllDispatch glMapBufferDispatch = 
        new jogamp.opengl.GLBufferObjectTracker.MapBufferAllDispatch() {
            public final ByteBuffer allocNioByteBuffer(final long addr, final long length) { return newDirectByteBuffer(addr, length); }
            public final long mapBuffer(final int target, final int access) {
                return glMapBufferDelegate(target, access);
            }
        };

    @Override
    public final ByteBuffer glMapBufferRange(int target, long offset, long length, int access)  {
      return mapBufferRange(target, offset, length, access).getMappedBuffer();
    }
    @Override
    public final GLBufferStorage mapBufferRange(final int target, final long offset, final long length, final int access) {
      return bufferObjectTracker.mapBuffer(bufferStateTracker, this, target, offset, length, access, glMapBufferRangeDispatch);
    }
    private final jogamp.opengl.GLBufferObjectTracker.MapBufferRangeDispatch glMapBufferRangeDispatch = 
        new jogamp.opengl.GLBufferObjectTracker.MapBufferRangeDispatch() {
            public final ByteBuffer allocNioByteBuffer(final long addr, final long length) { return newDirectByteBuffer(addr, length); }
            public final long mapBuffer(final int target, final long offset, final long length, final int access) {
                return glMapBufferRangeDelegate(target, offset, length, access);
            }
        };

    private native ByteBuffer newDirectByteBuffer(long addr, long capacity);

