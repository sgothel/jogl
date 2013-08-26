package jogamp.opengl.util.av.impl;

import com.jogamp.common.util.VersionNumber;

class FFMPEGStaticNatives {        
    static VersionNumber getAVVersion(int vers) {
        return new VersionNumber( ( vers >> 16 ) & 0xFF,
                                  ( vers >>  8 ) & 0xFF,
                                  ( vers >>  0 ) & 0xFF );
    }
    static native int getAvUtilVersion0(long func);
    static native int getAvFormatVersion0(long func);
    static native int getAvCodecVersion0(long func);
    static native int getAvResampleVersion0(long func);
}
