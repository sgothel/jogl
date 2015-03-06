
    private static EGLProcAddressTable _table = new EGLProcAddressTable(new GLProcAddressResolver());
    protected static boolean resetProcAddressTable(DynamicLookupHelper lookup) {
        _table.reset(lookup);
        return 0 != _table._addressof_eglGetDisplay && 0 != _table._addressof_eglInitialize;
    }

    // There are some #defines in egl.h that GlueGen and PCPP don't currently handle
    public static final long EGL_DEFAULT_DISPLAY = 0;
    public static final long EGL_NO_CONTEXT = 0;
    public static final long EGL_NO_DISPLAY = 0;
    public static final long EGL_NO_SURFACE = 0;
    public static final int  EGL_DONT_CARE  = -1;
    public static final int  EGL_UNKNOWN    = -1;

    /** 
     * In case of an error on a particualr attribute, the attribute in the attributes-buffer is set to 0.
     * <p>
     * Entry point to C language function: <code> EGLBoolean eglGetConfigAttrib(EGLDisplay dpy, EGLConfig config, EGLint attribute, EGLint *  value); </code> <br>Part of <code>EGL_VERSION_1_0</code>
     * </p>
     */
    public static void eglGetConfigAttributes(long dpy, long config, IntBuffer attributes, IntBuffer values) {
        if( attributes == null || values == null ) {
            throw new RuntimeException("arrays buffers are null");
        }
        if( !Buffers.isDirect(attributes) || !Buffers.isDirect(values) ) {
            throw new RuntimeException("arrays buffers are not direct");
        }
        if( attributes.remaining() > values.remaining() ) {
            throw new RuntimeException("not enough values "+values+" for attributes "+attributes);
        }
        final long __addr = _table._addressof_eglGetConfigAttrib;
        dispatch_eglGetConfigAttributes(dpy, config, attributes.remaining(), attributes, Buffers.getDirectBufferByteOffset(attributes), 
                                        values, Buffers.getDirectBufferByteOffset(values), __addr);
    }
    private static native void dispatch_eglGetConfigAttributes(long dpy, long config, int attributeCount, Object attributes, int attributes_byte_offset, Object values, int valuesOffset, long procAddr);

