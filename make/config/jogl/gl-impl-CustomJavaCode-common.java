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

    @Override
    public final int getDefaultReadBuffer() {
      return _context.getDefaultReadBuffer();
    }

    private final HashMap<MemoryObject, MemoryObject> arbMemCache = new HashMap<MemoryObject, MemoryObject>();

    /** Entry point to C language function: <code> void *  {@native glMapBuffer}(GLenum target, GLenum access); </code> <br>Part of <code>GL_VERSION_1_5</code>; <code>GL_OES_mapbuffer</code>   */
    private final java.nio.ByteBuffer glMapBufferImpl(int target, boolean useRange, long offset, long length, int access, long glProcAddress) {
      if (glProcAddress == 0) {
        throw new GLException("Method \""+(useRange?"glMapBufferRange":"glMapBuffer")+"\" not available");
      }
      final long sz = bufferSizeTracker.getBufferSize(bufferStateTracker, target, this);
      if (0 == sz) {
        return null;
      }
      if( !useRange ) {
        length = sz;
        offset = 0;
      } else {
        if( length + offset > sz ) {
            throw new GLException("Out of range: offset "+offset+" + length "+length+" > size "+sz); 
        }
        if( 0 > length || 0 > offset ) {
            throw new GLException("Invalid values: offset "+offset+", length "+length);
        }
        if( 0 == length ) {
            return null;
        }
      }
      final long addr = useRange ? dispatch_glMapBufferRange(target, offset, length, access, glProcAddress) :
                                   dispatch_glMapBuffer(target, access, glProcAddress);
      if (0 == addr) {
        return null;
      }
      ByteBuffer buffer;
      MemoryObject memObj0 = new MemoryObject(addr, length); // object and key
      MemoryObject memObj1 = MemoryObject.getOrAddSafe(arbMemCache, memObj0);
      if(memObj0 == memObj1) {
        // just added ..
        if(null != memObj0.getBuffer()) {
            throw new InternalError();
        }
        buffer = newDirectByteBuffer(addr, length);
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
    private native long dispatch_glMapBuffer(int target, int access, long glProcAddress);
    private native long dispatch_glMapBufferRange(int target, long offset, long length, int access, long procAddress);


    /** Entry point to C language function: <code> GLvoid *  {@native glMapNamedBufferEXT}(GLuint buffer, GLenum access); </code> <br>Part of <code>GL_EXT_direct_state_access</code>   */
    private final java.nio.ByteBuffer glMapNamedBufferImpl(int bufferName, int access, long glProcAddress)  {
      if (glProcAddress == 0) {
        throw new GLException("Method \"glMapNamedBufferEXT\" not available");
      }
      final long sz = bufferSizeTracker.getDirectStateBufferSize(bufferName, this);
      if (0 == sz) {
        return null;
      }
      final long addr = dispatch_glMapNamedBufferEXT(bufferName, access, glProcAddress);
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
    private native long dispatch_glMapNamedBufferEXT(int buffer, int access, long procAddress);

    private native ByteBuffer newDirectByteBuffer(long addr, long capacity);
