/**
 * Copyright 2012-2023 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package jogamp.opengl.util.av.impl;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jogamp.opengl.GLProfile;
import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.os.DynamicLibraryBundle;
import com.jogamp.common.os.DynamicLibraryBundleInfo;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.util.RunnableExecutor;
import com.jogamp.common.util.SecurityUtil;
import com.jogamp.common.util.VersionNumber;

/**
 * See {@link FFMPEGMediaPlayer#compatibility}.
 */
class FFMPEGDynamicLibraryBundleInfo implements DynamicLibraryBundleInfo  {
    private static final boolean DEBUG = FFMPEGMediaPlayer.DEBUG || DynamicLibraryBundleInfo.DEBUG;

    private static final List<String> glueLibNames = new ArrayList<String>(); // none

    private static final int symbolCount = 67;
    private static final String[] symbolNames = {
         "avutil_version",
         "avformat_version",
         "avcodec_version",
         "avdevice_version",   // (opt)
         "swresample_version",
         "swscale_version", // (opt)
         /* 6 */

         // libavcodec
         "avcodec_close",
         "avcodec_string",
         "avcodec_find_decoder",
         "avcodec_alloc_context3",
         "avcodec_free_context",
         "avcodec_parameters_to_context",
         "avcodec_open2",             // 53.6.0
         "av_frame_alloc",            // >= 55.28.1
         "av_frame_free",             // >= 55.28.1
         "avcodec_default_get_buffer2", // 55
         "av_image_fill_linesizes",
         "avcodec_flush_buffers",
         "av_packet_alloc",
         "av_packet_free",
         "av_new_packet",
         "av_packet_unref",
         "avcodec_send_packet",       // 57
         "avcodec_receive_frame",     // 57
         "avcodec_decode_subtitle2",  // 52.23.0
         "avsubtitle_free",           // 52.82.0
         /* +20 = 26 */

         // libavutil
         "av_pix_fmt_desc_get",       // >= lavu 51.45
         "av_frame_unref",            // 55.0.0
         "av_realloc",
         "av_free",
         "av_get_bits_per_pixel",
         "av_samples_get_buffer_size",
         "av_get_bytes_per_sample",   // 51.4.0
         "av_opt_set_int",            // 51.12.0
         "av_dict_iterate",           // 57.42.100
         "av_dict_get",
         "av_dict_count",             // 54.* (opt)
         "av_dict_set",
         "av_dict_free",
         "av_channel_layout_default", // >= 59 (opt)
         "av_channel_layout_uninit", // >= 59 (opt)
         "av_channel_layout_describe", // >= 59 (opt)
         "av_opt_set_chlayout",        // >= 59
         /* +17 = 43 */

         // libavformat
         "avformat_alloc_context",
         "avformat_free_context",     // 52.96.0
         "avformat_close_input",      // 53.17.0
         "av_find_input_format",
         "avformat_open_input",
         "av_dump_format",
         "av_read_frame",
         "av_seek_frame",
         "avformat_seek_file",        // ???       (opt)
         "av_read_play",
         "av_read_pause",
         "avformat_network_init",     // 53.13.0   (opt)
         "avformat_network_deinit",   // 53.13.0   (opt)
         "avformat_find_stream_info", // 53.3.0    (opt)
         /* +14 = 57 */

         // libavdevice
         "avdevice_register_all",     // supported in all versions (opt)
         /* +1  = 58 */

         // libswresample
         "av_opt_set_sample_fmt",     // actually lavu .. but exist only w/ swresample!
         "swr_alloc",
         "swr_init",
         "swr_free",
         "swr_convert",
         "swr_get_out_samples",
         /* +6  = 64 */

         // libswscale
         "sws_getCachedContext",      // opt
         "sws_scale",                 // opt
         "sws_freeContext",           // opt
         /* +3  = 67 */
    };

    // optional symbol names
    private static final String[] optionalSymbolNames = {
         "avformat_seek_file",        // ???       (opt)
         "av_dict_count",             // 54.*   (opt)

         // libavutil
         "av_dict_iterate",           // >= 57.42.100
         "av_channel_layout_default", // >= 59 (opt)
         "av_channel_layout_uninit", // >= 59 (opt)
         "av_channel_layout_describe", // >= 59 (opt)
         "av_opt_set_chlayout",        // >= 59

         // libavdevice
         "avdevice_version",          // (opt)
         "avdevice_register_all",     // 53.0.0 (opt)

         // libswresample
         "av_opt_set_sample_fmt",     // actually lavu .. but exist only w/ swresample!
         "swr_alloc",
         "swr_init",
         "swr_free",
         "swr_convert",
         "swr_get_out_samples",

         // libswscale
         "swscale_version",           // opt
         "sws_getCachedContext",      // opt
         "sws_scale",                 // opt
         "sws_freeContext",           // opt
    };

    /** 6: util, format, codec, device, swresample, swscale */
    private static final int LIB_COUNT = 6;
    private static final int LIB_IDX_UTI = 0;
    private static final int LIB_IDX_FMT = 1;
    private static final int LIB_IDX_COD = 2;
    private static final int LIB_IDX_DEV = 3;
    private static final int LIB_IDX_SWR = 4;
    private static final int LIB_IDX_SWS = 5;

    /** util, format, codec, device, swresample */
    private static final boolean[] libLoaded = new boolean[LIB_COUNT];
    private static final long[] symbolAddr = new long[symbolCount];
    private static final boolean ready;
    private static final boolean libsCFUSLoaded;
    public static class VersionedLib {
        public final NativeLibrary lib;
        public final VersionNumber version;
        public VersionedLib(final NativeLibrary lib, final VersionNumber version) {
            this.lib = lib;
            this.version = version;
        }
        @Override
        public String toString() {
            final String ls;
            if( null != lib ) {
                if( null != lib.getNativeLibraryPath() ) {
                    ls = "nlp '"+lib.getNativeLibraryPath()+"'";
                } else {
                    ls = "vlp '"+lib.getLibraryPath()+"'";
                }
            } else {
                ls = "n/a";
            }
            return ls+", "+version;
        }
    }
    static final VersionedLib avUtil;
    static final VersionedLib avFormat;
    static final VersionedLib avCodec;
    static final VersionedLib avDevice;
    static final VersionedLib swResample;
    static final VersionedLib swScale;
    private static final FFMPEGNatives natives;

    private static final PrivilegedAction<DynamicLibraryBundle> privInitSymbolsAction = new PrivilegedAction<DynamicLibraryBundle>() {
        @Override
        public DynamicLibraryBundle run() {
            final DynamicLibraryBundle dl = new DynamicLibraryBundle(new FFMPEGDynamicLibraryBundleInfo());
            for(int i=0; i<libLoaded.length; i++) {
                libLoaded[i] = dl.isToolLibLoaded(i);
            }
            if( !libLoaded[LIB_IDX_COD] || !libLoaded[LIB_IDX_FMT] || !libLoaded[LIB_IDX_UTI] || !libLoaded[LIB_IDX_SWR]) {
                System.err.println("FFMPEG Tool library incomplete: [ avcodec "+libLoaded[LIB_IDX_COD]+", avformat "+libLoaded[LIB_IDX_FMT]+
                                   ", avutil "+libLoaded[LIB_IDX_UTI]+", swres "+libLoaded[LIB_IDX_SWR]+" ]");
                return null;
            }
            dl.claimAllLinkPermission();
            try {
                for(int i = 0; i<symbolCount; i++) {
                    symbolAddr[i] = dl.dynamicLookupFunction(symbolNames[i]);
                }
            } finally {
                dl.releaseAllLinkPermission();
            }
            return dl;
        } };

    /**
     * @param versions 6: util, format, codec, device, swresample, swscale
     * @return
     */
    private static final boolean initSymbols(final VersionNumber[] versions, final List<NativeLibrary> nativeLibs) {
        for(int i=0; i<libLoaded.length; i++) {
            libLoaded[i] = false;
        }
        if(symbolNames.length != symbolCount) {
            throw new InternalError("XXX0 "+symbolNames.length+" != "+symbolCount);
        }

        final DynamicLibraryBundle dl = SecurityUtil.doPrivileged(privInitSymbolsAction);
        if( null == dl ) {
            return false;
        }
        nativeLibs.clear();
        nativeLibs.addAll(dl.getToolLibraries());

        // optional symbol name set
        final Set<String> optionalSymbolNameSet = new HashSet<String>();
        optionalSymbolNameSet.addAll(Arrays.asList(optionalSymbolNames));

        // validate results
        boolean res = true;
        for(int i = 0; i<symbolCount; i++) {
            if( 0 == symbolAddr[i] ) {
                // no symbol, check optional and alternative symbols
                final String symbol = symbolNames[i];
                if ( !optionalSymbolNameSet.contains(symbol) ) {
                    System.err.println("FFmpeg: Fail: Could not resolve symbol <"+symbolNames[i]+">: not optional, no alternatives.");
                    res = false;
                } else if(DEBUG) {
                    System.err.println("FFmpeg: OK: Unresolved optional symbol <"+symbolNames[i]+">");
                }
            }
        }
        versions[LIB_IDX_UTI] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvVersion0(symbolAddr[LIB_IDX_UTI]));
        versions[LIB_IDX_FMT] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvVersion0(symbolAddr[LIB_IDX_FMT]));
        versions[LIB_IDX_COD] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvVersion0(symbolAddr[LIB_IDX_COD]));
        if( 0 != symbolAddr[LIB_IDX_DEV] ) {
            versions[LIB_IDX_DEV] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvVersion0(symbolAddr[LIB_IDX_DEV]));
        } else {
            versions[LIB_IDX_DEV] = new VersionNumber(0, 0, 0);
        }
        versions[LIB_IDX_SWR] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvVersion0(symbolAddr[LIB_IDX_SWR]));
        if( 0 != symbolAddr[LIB_IDX_SWS] ) {
            versions[LIB_IDX_SWS] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvVersion0(symbolAddr[LIB_IDX_SWS]));
        } else {
            versions[LIB_IDX_SWS] = new VersionNumber(0, 0, 0);
        }
        return res;
    }

    static {
        // native ffmpeg media player implementation is included in jogl_desktop and jogl_mobile
        GLProfile.initSingleton();

        boolean _ready = false;
        /** 6: util, format, codec, device, swresample, swscale */
        final VersionNumber[] _versions = new VersionNumber[LIB_COUNT];
        final List<NativeLibrary> _nativeLibs = new ArrayList<NativeLibrary>();
        try {
            _ready = initSymbols(_versions, _nativeLibs);
        } catch (final Throwable t) {
            ExceptionUtils.dumpThrowable("", t);
        }
        libsCFUSLoaded = libLoaded[LIB_IDX_COD] && libLoaded[LIB_IDX_FMT] && libLoaded[LIB_IDX_UTI] && libLoaded[LIB_IDX_SWR];
        avUtil = new VersionedLib(_nativeLibs.get(LIB_IDX_UTI), _versions[LIB_IDX_UTI]);
        avFormat = new VersionedLib(_nativeLibs.get(LIB_IDX_FMT), _versions[LIB_IDX_FMT]);
        avCodec = new VersionedLib(_nativeLibs.get(LIB_IDX_COD), _versions[LIB_IDX_COD]);
        avDevice = new VersionedLib(_nativeLibs.get(LIB_IDX_DEV), _versions[LIB_IDX_DEV]);
        swResample = new VersionedLib(_nativeLibs.get(LIB_IDX_SWR), _versions[LIB_IDX_SWR]);
        swScale = new VersionedLib(_nativeLibs.get(LIB_IDX_SWS), _versions[LIB_IDX_SWS]);
        if(!libsCFUSLoaded) {
            String missing = "";
            if( !libLoaded[LIB_IDX_COD] ) {
                missing = missing + "avcodec, ";
            }
            if( !libLoaded[LIB_IDX_FMT] ) {
                missing = missing + "avformat, ";
            }
            if( !libLoaded[LIB_IDX_UTI] ) {
                missing = missing + "avutil, ";
            }
            if( !libLoaded[LIB_IDX_SWR] ) {
                missing = missing + "swresample";
            }
            System.err.println("FFmpeg Not Available, missing libs: "+missing);
            natives = null;
            ready = false;
        } else if(!_ready) {
            System.err.println("FFmpeg Symbol Lookup Failed");
            natives = null;
            ready = false;
        } else {
            final int avUtilMajor = avUtil.version.getMajor();
            final int avFormatMajor = avFormat.version.getMajor();
            final int avCodecMajor = avCodec.version.getMajor();
            final int avDeviceMajor = avDevice.version.getMajor();
            final int swResampleMajor = swResample.version.getMajor();
            final int swScaleMajor = swScale.version.getMajor();
            if( avCodecMajor == 58 && avFormatMajor == 58 && ( avDeviceMajor == 58 || avDeviceMajor == 0 ) && avUtilMajor == 56 &&
                swResampleMajor == 3 && ( swScaleMajor == 5 || swScaleMajor == 0 ) )
            {
                // Exact match: ffmpeg 4.x.y
                natives = new FFMPEGv0400Natives();
            } else if( avCodecMajor == 59 && avFormatMajor == 59 && ( avDeviceMajor == 59 || avDeviceMajor == 0 ) && avUtilMajor == 57 &&
                       swResampleMajor == 4 && ( swScaleMajor == 6 || swScaleMajor == 0 ) )
            {
                // Exact match: ffmpeg 5.x.y
                natives = new FFMPEGv0500Natives();
            } else if( avCodecMajor == 60 && avFormatMajor == 60 && ( avDeviceMajor == 60 || avDeviceMajor == 0 ) && avUtilMajor == 58 &&
                       swResampleMajor == 4 && ( swScaleMajor == 7 || swScaleMajor == 0 ) )
            {
                // Exact match: ffmpeg 6.x.y
                natives = new FFMPEGv0600Natives();
            } else {
                natives = null;
            }
            if( null == natives ) {
                System.err.println("FFmpeg Native Class matching runtime-versions not found.");
                ready = false;
            } else if( FFMPEGStaticNatives.initIDs0() ) {
                ready = natives.initSymbols0(symbolAddr, symbolCount);
                if( !ready ) {
                    System.err.println("FFmpeg Native Symbols Initialization Failed");
                }
            } else {
                System.err.println("FFmpeg Native ID Initialization Failed");
                ready = false;
            }
        }
    }

    static boolean libsLoaded() { return libsCFUSLoaded; }
    static boolean avDeviceLoaded() { return libLoaded[LIB_IDX_DEV]; }
    static boolean swResampleLoaded() { return libLoaded[LIB_IDX_SWR]; }
    static boolean swScaleLoaded() { return libLoaded[LIB_IDX_SWS]; }
    static FFMPEGNatives getNatives() { return natives; }
    static boolean initSingleton() { return ready; }

    protected FFMPEGDynamicLibraryBundleInfo() {
    }

    @Override
    public final boolean shallLinkGlobal() { return true; }

    /**
     * {@inheritDoc}
     * <p>
     * Returns <code>true</code>.
     * </p>
     */
    @Override
    public final boolean shallLookupGlobal() {
        return true;
    }

    @Override
    public final List<String> getGlueLibNames() {
        return glueLibNames;
    }

    @Override
    public final boolean searchToolLibInSystemPath() {
        return true;
    }

    @Override
    public final boolean searchToolLibSystemPathFirst() {
        return true;
    }

    @Override
    public final List<List<String>> getToolLibNames() {
        final List<List<String>> libsList = new ArrayList<List<String>>();

        // 6: util, format, codec, device, swresample, swscale

        final List<String> avutil = new ArrayList<String>();
        if( FFMPEGMediaPlayer.PREFER_SYSTEM_LIBS ) {
            avutil.add("avutil");          // system default
        } else {
            avutil.add("internal_avutil"); // internal
        }
        avutil.add("libavutil.so.58");     // ffmpeg 6.[0-x]
        avutil.add("libavutil.so.57");     // ffmpeg 5.[0-x]
        avutil.add("libavutil.so.56");     // ffmpeg 4.[0-x] (Debian-11)

        avutil.add("avutil-58");           // ffmpeg 6.[0-x]
        avutil.add("avutil-57");           // ffmpeg 5.[0-x]
        avutil.add("avutil-56");           // ffmpeg 4.[0-x]
        if( FFMPEGMediaPlayer.PREFER_SYSTEM_LIBS ) {
            avutil.add("internal_avutil"); // internal
        } else {
            avutil.add("avutil");          // system default
        }
        libsList.add(avutil);

        final List<String> avformat = new ArrayList<String>();
        if( FFMPEGMediaPlayer.PREFER_SYSTEM_LIBS ) {
            avformat.add("avformat");          // system default
        } else {
            avformat.add("internal_avformat"); // internal
        }
        avformat.add("libavformat.so.60");     // ffmpeg 6.[0-x]
        avformat.add("libavformat.so.59");     // ffmpeg 5.[0-x]
        avformat.add("libavformat.so.58");     // ffmpeg 4.[0-x] (Debian-11)

        avformat.add("avformat-60");           // ffmpeg 6.[0-x]
        avformat.add("avformat-59");           // ffmpeg 5.[0-x]
        avformat.add("avformat-58");           // ffmpeg 4.[0-x]
        if( FFMPEGMediaPlayer.PREFER_SYSTEM_LIBS ) {
            avformat.add("internal_avformat"); // internal
        } else {
            avformat.add("avformat");          // system default
        }
        libsList.add(avformat);

        final List<String> avcodec = new ArrayList<String>();
        if( FFMPEGMediaPlayer.PREFER_SYSTEM_LIBS ) {
            avcodec.add("avcodec");            // system default
        } else {
            avcodec.add("internal_avcodec");   // internal
        }
        avcodec.add("libavcodec.so.60");       // ffmpeg 6.[0-x]
        avcodec.add("libavcodec.so.59");       // ffmpeg 5.[0-x]
        avcodec.add("libavcodec.so.58");       // ffmpeg 4.[0-x] (Debian-11)

        avcodec.add("avcodec-60");             // ffmpeg 6.[0-x]
        avcodec.add("avcodec-59");             // ffmpeg 5.[0-x]
        avcodec.add("avcodec-58");             // ffmpeg 4.[0-x]
        if( FFMPEGMediaPlayer.PREFER_SYSTEM_LIBS ) {
            avcodec.add("internal_avcodec");   // internal
        } else {
            avcodec.add("avcodec");            // system default
        }
        libsList.add(avcodec);

        final List<String> avdevice = new ArrayList<String>();
        if( FFMPEGMediaPlayer.PREFER_SYSTEM_LIBS ) {
            avdevice.add("avdevice");          // system default
        } else {
            avdevice.add("internal_avdevice"); // internal
        }
        avdevice.add("libavdevice.so.60");     // ffmpeg 6.[0-x]
        avdevice.add("libavdevice.so.59");     // ffmpeg 5.[0-x]
        avdevice.add("libavdevice.so.58");     // ffmpeg 4.[0-x] (Debian-11)

        avdevice.add("avdevice-60");           // ffmpeg 6.[0-x]
        avdevice.add("avdevice-59");           // ffmpeg 5.[0-x]
        avdevice.add("avdevice-58");           // ffmpeg 4.[0-x]
        if( FFMPEGMediaPlayer.PREFER_SYSTEM_LIBS ) {
            avdevice.add("internal_avdevice"); // internal
        } else {
            avdevice.add("avdevice");          // system default
        }
        libsList.add(avdevice);

        final List<String> swresample = new ArrayList<String>();
        if( FFMPEGMediaPlayer.PREFER_SYSTEM_LIBS ) {
            swresample.add("swresample");         // system default
        } else {
            swresample.add("internal_swresample");// internal
        }
        swresample.add("libswresample.so.4");     // ffmpeg 5.[0-x] - 6.[0-x]
        swresample.add("libswresample.so.3");     // ffmpeg 4.[0-x] (Debian-11)

        swresample.add("swresample-4");           // ffmpeg 5.[0-x] - 6.[0-x]
        swresample.add("swresample-3");           // ffmpeg 4.[0-x]
        if( FFMPEGMediaPlayer.PREFER_SYSTEM_LIBS ) {
            swresample.add("internal_swresample");// internal
        } else {
            swresample.add("swresample");         // system default
        }
        libsList.add(swresample);

        final List<String> swscale = new ArrayList<String>();
        if( FFMPEGMediaPlayer.PREFER_SYSTEM_LIBS ) {
            swscale.add("swscale");         // system default
        } else {
            swscale.add("internal_swscale");// internal
        }
        swscale.add("libswscale.so.7");     // ffmpeg 6.[0-x]
        swscale.add("libswscale.so.6");     // ffmpeg 5.[0-x]
        swscale.add("libswscale.so.5");     // ffmpeg 4.[0-x] (Debian-11)

        swscale.add("swscale-7");           // ffmpeg 6.[0-x]
        swscale.add("swscale-6");           // ffmpeg 5.[0-x]
        swscale.add("swscale-5");           // ffmpeg 4.[0-x]
        if( FFMPEGMediaPlayer.PREFER_SYSTEM_LIBS ) {
            swscale.add("internal_swscale");// internal
        } else {
            swscale.add("swscale");         // system default
        }
        libsList.add(swscale);
        return libsList;
    }
    @Override
    public List<String> getSymbolForToolLibPath() {
        // 6: util, format, codec, device, swresample, swscale
        return Arrays.asList("av_free", "av_read_frame", "avcodec_close", "avdevice_register_all", "swr_convert", "swscale_version");
    }

    @Override
    public final List<String> getToolGetProcAddressFuncNameList() {
        return null;
    }

    @Override
    public final long toolGetProcAddress(final long toolGetProcAddressHandle, final String funcName) {
        return 0;
    }

    @Override
    public final boolean useToolGetProcAdressFirst(final String funcName) {
        return false;
    }

    @Override
    public final RunnableExecutor getLibLoaderExecutor() {
        return DynamicLibraryBundle.getDefaultRunnableExecutor();
    }
}
