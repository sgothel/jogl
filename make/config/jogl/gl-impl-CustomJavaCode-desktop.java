private int[] imageSizeTemp = new int[1];

/** Helper for more precise computation of number of bytes that will
    be touched by a pixel pack or unpack operation. */
private int imageSizeInBytes(int bytesPerElement,
                             int w, int h, int d,
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
    rowLength   = Math.max(0, rowLength);
    skipRows    = Math.max(0, skipRows);
    skipPixels  = Math.max(0, skipPixels);
    alignment   = Math.max(1, alignment);
    imageHeight = Math.max(0, imageHeight);
    skipImages  = Math.max(0, skipImages);
    rowLength   = Math.max(rowLength, w + skipPixels);
    int rowLengthInBytes = rowLength * bytesPerElement;
    if (alignment > 1) {
        int modulus = rowLengthInBytes % alignment;
        if (modulus > 0) {
            rowLengthInBytes += alignment - modulus;
        }
    }

    int size = 0;
    if (dimensions == 1) {
        return (w + skipPixels) * bytesPerElement;
    } else {
        int size2D = ((skipRows + h - 1) * rowLengthInBytes) + (w + skipPixels) * bytesPerElement;
        if (dimensions == 2) {
            return size2D;
        }
        int imageSizeInBytes = imageHeight * rowLength;
        return ((skipImages + d - 1) * imageSizeInBytes) + size2D;
    }
}
