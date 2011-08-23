    private int[] imageSizeTemp = new int[1];

    private final int imageSizeInBytes(int format, int type, int width, int height, int depth, boolean pack) {
        return GLBuffers.sizeof(this, imageSizeTemp, format, type, width, height, depth, pack) ;                                    
    }

    public final boolean isGL4bc() {
        return _context.isGL4bc();
    }

    public final boolean isGL4() {
        return _context.isGL4();
    }

    public final boolean isGL3bc() {
        return _context.isGL3bc();
    }

    public final boolean isGL3() {
        return _context.isGL3();
    }

    public final boolean isGL2() {
        return _context.isGL2();
    }
      
    public final boolean isGL2ES1() {
        return _context.isGL2ES1();
    }

    public final boolean isGL2ES2() {
        return _context.isGL2ES2();
    }

    public final boolean isGLES2Compatible() {
        return _context.isGLES2Compatible();
    }

    public final boolean isGL2GL3() {
        return _context.isGL2GL3();
    }

    public final boolean hasGLSL() {
        return _context.hasGLSL();
    }

    public final GL4bc getGL4bc() throws GLException {
        if(!isGL4bc()) {
            throw new GLException("Not a GL4bc implementation");
        }
        return this;
    }

    public final GL4 getGL4() throws GLException {
        if(!isGL4()) {
            throw new GLException("Not a GL4 implementation");
        }
        return this;
    }

    public final GL3bc getGL3bc() throws GLException {
        if(!isGL3bc()) {
            throw new GLException("Not a GL3bc implementation");
        }
        return this;
    }

    public final GL3 getGL3() throws GLException {
        if(!isGL3()) {
            throw new GLException("Not a GL3 implementation");
        }
        return this;
    }

    public final GL2 getGL2() throws GLException {
        if(!isGL2()) {
            throw new GLException("Not a GL2 implementation");
        }
        return this;
    }

    public final GL2ES1 getGL2ES1() throws GLException {
        if(!isGL2ES1()) {
            throw new GLException("Not a GL2ES1 implementation");
        }
        return this;
    }

    public final GL2ES2 getGL2ES2() throws GLException {
        if(!isGL2ES2()) {
            throw new GLException("Not a GL2ES2 implementation");
        }
        return this;
    }

    public final GL2GL3 getGL2GL3() throws GLException {
        if(!isGL2GL3()) {
            throw new GLException("Not a GL2GL3 implementation");
        }
        return this;
    }

    public final boolean isGLES1() {
        return false;
    }

    public final boolean isGLES2() {
        return false;
    }

    public final boolean isGLES() {
        return false;
    }

    public final GLES1 getGLES1() throws GLException {
        throw new GLException("Not a GLES1 implementation");
    }

    public final GLES2 getGLES2() throws GLException {
        throw new GLException("Not a GLES2 implementation");
    }

