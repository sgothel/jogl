    @Override
    public GLProfile getGLProfile() {
        return this.glProfile;
    }
    private final GLProfile glProfile;

    @Override
    public final int glGetBoundBuffer(int target) {
        return bufferStateTracker.getBoundBufferObject(target, this);
    }

    @Override
    public final long glGetBufferSize(int buffer) {
        return bufferSizeTracker.getDirectStateBufferSize(buffer, this);
    }

    @Override
    public final boolean glIsVBOArrayEnabled() {
        return checkArrayVBOEnabled(false);
    }

    @Override
    public final boolean glIsVBOElementArrayEnabled() {
        return checkElementVBOEnabled(false);
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

