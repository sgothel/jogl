
    private int[] imageSizeTemp = new int[1];

    private final int imageSizeInBytes(int format, int type, int width, int height, int depth, boolean pack) {
        return GLBuffers.sizeof(this, imageSizeTemp, format, type, width, height, depth, pack) ;                                    
    }

