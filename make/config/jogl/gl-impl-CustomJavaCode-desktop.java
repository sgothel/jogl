private int[] imageSizeTemp = new int[1];

/** Helper for more precise computation of number of bytes that will
    be touched by a pixel pack or unpack operation. */
private int imageSizeInBytes(int bytesPerElement,
                             int width, int height, int depth,
                             int dimensions,
                             boolean pack) {
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
        if (dimensions > 2) {
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
        if (dimensions > 2) {
            glGetIntegerv(GL_UNPACK_IMAGE_HEIGHT, imageSizeTemp, 0);
            imageHeight = imageSizeTemp[0];
            glGetIntegerv(GL_UNPACK_SKIP_IMAGES, imageSizeTemp, 0);
            skipImages = imageSizeTemp[0];
        }
    }
    // Try to deal somewhat correctly with potentially invalid values
    height      = Math.max(0, height);
    skipRows    = Math.max(0, skipRows);
    skipPixels  = Math.max(0, skipPixels);
    alignment   = Math.max(1, alignment);
    imageHeight = Math.max(0, imageHeight);
    skipImages  = Math.max(0, skipImages);

    rowLength   = Math.max(Math.max(0, rowLength), width); // > 0 && >= width
    int rowLengthInBytes = rowLength * bytesPerElement;

    if (alignment > 1) {
        int modulus = rowLengthInBytes % alignment;
        if (modulus > 0) {
            rowLengthInBytes += alignment - modulus;
        }
    }

    /**
     * skipPixels and skipRows is a static one time offset
     *
     * rowlenght is the actual repeating offset 
     * from line-start to the next line-start.
     */
    int size = height * rowLengthInBytes; // height == 1 for 1D
    if(dimensions==3) {
       size *= (skipImages + depth);
    }

    int skipOffset = skipPixels * bytesPerElement +
                     skipRows * rowLengthInBytes;

    return size + skipOffset;
}
