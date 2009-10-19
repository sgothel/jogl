private int[] imageSizeTemp = new int[1];

/** Helper for more precise computation of number of bytes that will
    be touched by a pixel pack or unpack operation. */
private int imageSizeInBytes(int bytesPerElement,
                             int rowLength, int imageHeight, int depth, boolean pack) {
    int alignment = 1;

    if (pack) {
        glGetIntegerv(GL_PACK_ALIGNMENT, imageSizeTemp, 0);
        alignment = imageSizeTemp[0];
    } else {
        glGetIntegerv(GL_UNPACK_ALIGNMENT, imageSizeTemp, 0);
        alignment = imageSizeTemp[0];
    }
    // Try to deal somewhat correctly with potentially invalid values
    rowLength   = Math.max(0, rowLength );
    imageHeight = Math.max(1, imageHeight); // min 1D
    depth       = Math.max(1, depth );      // min 1 * imageSize
    alignment   = Math.max(1, alignment);

    int rowLengthInBytes = rowLength * bytesPerElement;

    if (alignment > 1) {
        int padding = rowLengthInBytes % alignment;
        if (padding > 0) {
            rowLengthInBytes += alignment - padding;
        }
    }

    /**
     * depth is in multiples of image size.
     *
     * rowlenght is the actual repeating offset 
     * to go from line n to line n+1 at the same x-axis position.
     */
    return 
        ( depth       - 1 ) * imageHeight * rowLengthInBytes + // whole images
        ( imageHeight - 1 ) * rowLengthInBytes +               // lines with padding
        ( rowLength       ) * bytesPerElement;                 // last line
}

