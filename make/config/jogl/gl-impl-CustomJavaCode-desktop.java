    private int[] imageSizeTemp = new int[1];

    /** Helper for more precise computation of number of bytes that will
        be touched by a pixel pack or unpack operation. */
    private int imageSizeInBytes(int bytesPerElement,
                                 int width, int height, int depth, boolean pack) {
        int rowLength = 0;
        int skipRows = 0;
        int skipPixels = 0;
        int alignment = 1;
        int imageHeight = 0;
        int skipImages = 0;

        if (pack) {
            glGetIntegerv(GL_PACK_ROW_LENGTH, imageSizeTemp, 0);
            rowLength = imageSizeTemp[0];
            glGetIntegerv(GL_PACK_SKIP_ROWS, imageSizeTemp, 0);
            skipRows = imageSizeTemp[0];
            glGetIntegerv(GL_PACK_SKIP_PIXELS, imageSizeTemp, 0);
            skipPixels = imageSizeTemp[0];
            glGetIntegerv(GL_PACK_ALIGNMENT, imageSizeTemp, 0);
            alignment = imageSizeTemp[0];
            if (depth > 1) {
                glGetIntegerv(GL_PACK_IMAGE_HEIGHT, imageSizeTemp, 0);
                imageHeight = imageSizeTemp[0];
                glGetIntegerv(GL_PACK_SKIP_IMAGES, imageSizeTemp, 0);
                skipImages = imageSizeTemp[0];
            }
        } else {
            glGetIntegerv(GL_UNPACK_ROW_LENGTH, imageSizeTemp, 0);
            rowLength = imageSizeTemp[0];
            glGetIntegerv(GL_UNPACK_SKIP_ROWS, imageSizeTemp, 0);
            skipRows = imageSizeTemp[0];
            glGetIntegerv(GL_UNPACK_SKIP_PIXELS, imageSizeTemp, 0);
            skipPixels = imageSizeTemp[0];
            glGetIntegerv(GL_UNPACK_ALIGNMENT, imageSizeTemp, 0);
            alignment = imageSizeTemp[0];
            if (depth > 1) {
                glGetIntegerv(GL_UNPACK_IMAGE_HEIGHT, imageSizeTemp, 0);
                imageHeight = imageSizeTemp[0];
                glGetIntegerv(GL_UNPACK_SKIP_IMAGES, imageSizeTemp, 0);
                skipImages = imageSizeTemp[0];
            }
        }
        // Try to deal somewhat correctly with potentially invalid values
        width       = Math.max(0, width );
        height      = Math.max(1, height); // min 1D
        depth       = Math.max(1, depth ); // min 1 * imageSize
        skipRows    = Math.max(0, skipRows);
        skipPixels  = Math.max(0, skipPixels);
        alignment   = Math.max(1, alignment);
        skipImages  = Math.max(0, skipImages);

        imageHeight = ( imageHeight > 0 ) ? imageHeight : height;
        rowLength   = ( rowLength   > 0 ) ? rowLength   : width;

        int rowLengthInBytes = rowLength * bytesPerElement;

        if (alignment > 1) {
            int padding = rowLengthInBytes % alignment;
            if (padding > 0) {
                rowLengthInBytes += alignment - padding;
            }
        }

        /**
         * skipPixels and skipRows is a static one time offset.
         *
         * skipImages and depth are in multiples of image size.
         *
         * rowlenght is the actual repeating offset 
         * to go from line n to line n+1 at the same x-axis position.
         */
        return 
            ( skipImages + depth  - 1 ) * imageHeight * rowLengthInBytes + // whole images
            ( skipRows   + height - 1 ) * rowLengthInBytes +               // lines with padding
            ( skipPixels + width      ) * bytesPerElement;                 // last line
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
        if(!isGL4bc()) {
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

