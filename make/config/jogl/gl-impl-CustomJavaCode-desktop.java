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

