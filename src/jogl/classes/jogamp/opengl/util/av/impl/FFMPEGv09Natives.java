package jogamp.opengl.util.av.impl;

class FFMPEGv09Natives implements FFMPEGNatives {
    @Override
    public native boolean initSymbols0(long[] symbols, int count);

    @Override
    public native int getAvUtilMajorVersionCC0();

    @Override
    public native int getAvFormatMajorVersionCC0();

    @Override
    public native int getAvCodecMajorVersionCC0();

    @Override
    public native int getAvResampleMajorVersionCC0();

    @Override
    public native boolean initIDs0();

    @Override
    public native long createInstance0(FFMPEGMediaPlayer upstream, boolean verbose);

    @Override
    public native void destroyInstance0(long moviePtr);

    @Override
    public native void setStream0(long moviePtr, String url, String inFormat, int vid, int aid, int aMaxChannelCount, int aPrefSampleRate);

    @Override
    public native void setGLFuncs0(long moviePtr, long procAddrGLTexSubImage2D, long procAddrGLGetError, long procAddrGLFlush, long procAddrGLFinish);

    @Override
    public native int getVideoPTS0(long moviePtr);

    @Override
    public native int getAudioPTS0(long moviePtr);

    @Override
    public native int readNextPacket0(long moviePtr, int texTarget, int texFmt, int texType);

    @Override
    public native int play0(long moviePtr);

    @Override
    public native int pause0(long moviePtr);

    @Override
    public native int seek0(long moviePtr, int position);
}
