    public GLProfile getGLProfile() {
        return this.glProfile;
    }
    private GLProfile glProfile;

    public int glGetBoundBuffer(int target) {
        return bufferStateTracker.getBoundBufferObject(target, this);
    }

    public long glGetBufferSize(int buffer) {
        return bufferSizeTracker.getDirectStateBufferSize(buffer, this);
    }

    public boolean glIsVBOArrayEnabled() {
        return checkArrayVBOEnabled(false);
    }

    public boolean glIsVBOElementArrayEnabled() {
        return checkElementVBOEnabled(false);
    }

    public final boolean isGL() {
        return true;
    }
      
    public final GL getGL() throws GLException {
        return this;
    }

    public boolean isFunctionAvailable(String glFunctionName) {
      return _context.isFunctionAvailable(glFunctionName);
    }

    public boolean isExtensionAvailable(String glExtensionName) {
      return _context.isExtensionAvailable(glExtensionName);
    }

    public boolean isNPOTTextureAvailable() {
      return isGL3() || isGLES2() || isExtensionAvailable(GL_ARB_texture_non_power_of_two);
    }
    private static final String GL_ARB_texture_non_power_of_two = "GL_ARB_texture_non_power_of_two";

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

    public void setSwapInterval(int interval) {
      _context.setSwapInterval(interval);
    }

    public int getSwapInterval() {
      return _context.getSwapInterval();
    }

    public Object getPlatformGLExtensions() {
      return _context.getPlatformGLExtensions();
    }

