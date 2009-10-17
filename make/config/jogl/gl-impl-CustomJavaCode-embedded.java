private int[] imageSizeTemp = new int[1];

/** Helper for more precise computation of number of bytes that will
    be touched by a pixel pack or unpack operation. */
private int imageSizeInBytes(int bytesPerElement,
                             int width, int height, int depth,
                             int dimensions,
                             boolean pack) {
    int rowLength;
    int alignment = 1;

    if (pack) {
        glGetIntegerv(GL_PACK_ALIGNMENT, imageSizeTemp, 0);
        alignment = imageSizeTemp[0];
    } else {
        glGetIntegerv(GL_UNPACK_ALIGNMENT, imageSizeTemp, 0);
        alignment = imageSizeTemp[0];
    }
    // Try to deal somewhat correctly with potentially invalid values
    height      = Math.max(0, height);
    alignment   = Math.max(1, alignment);

    rowLength   = Math.max(0, width); // > 0 && >= width
    int rowLengthInBytes = rowLength * bytesPerElement;

    if (alignment > 1) {
        int modulus = rowLengthInBytes % alignment;
        if (modulus > 0) {
            rowLengthInBytes += alignment - modulus;
        }
    }

    int size = height * rowLengthInBytes; // height == 1 for 1D
    if(dimensions==3) {
       size *= depth;
    }

    return size;
}
