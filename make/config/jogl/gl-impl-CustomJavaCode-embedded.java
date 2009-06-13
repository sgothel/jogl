private int[] imageSizeTemp = new int[1];

/** Helper for more precise computation of number of bytes that will
    be touched by a pixel pack or unpack operation. */
private int imageSizeInBytes(int bytesPerElement,
                             int w, int h, int d,
                             int dimensions,
                             boolean pack) {
    int rowLength = w;
    int alignment = 1;

    if (pack) {
        glGetIntegerv(GL_PACK_ALIGNMENT, imageSizeTemp, 0);
        alignment = imageSizeTemp[0];
    } else {
        glGetIntegerv(GL_UNPACK_ALIGNMENT, imageSizeTemp, 0);
        alignment = imageSizeTemp[0];
    }
    // Try to deal somewhat correctly with potentially invalid values
    rowLength   = Math.max(0, rowLength);
    alignment   = Math.max(1, alignment);
    h           = Math.max(0, h);
    int rowLengthInBytes = rowLength * bytesPerElement;
    if (alignment > 1) {
        int modulus = rowLengthInBytes % alignment;
        if (modulus > 0) {
            rowLengthInBytes += alignment - modulus;
        }
    }

    int size = 0;
    if (dimensions == 1) {
        return rowLengthInBytes;
    } else {
        return h * rowLengthInBytes;
    }
}
