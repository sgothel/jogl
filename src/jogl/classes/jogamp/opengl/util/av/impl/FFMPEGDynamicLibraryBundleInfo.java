/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.opengl.GLProfile;

import com.jogamp.common.os.DynamicLibraryBundle;
import com.jogamp.common.os.DynamicLibraryBundleInfo;
import com.jogamp.common.util.RunnableExecutor;
import com.jogamp.common.util.VersionNumber;

/**
 * See {@link FFMPEGMediaPlayer#compatibility}.
 */
class FFMPEGDynamicLibraryBundleInfo implements DynamicLibraryBundleInfo  {
    private static final boolean DEBUG = FFMPEGMediaPlayer.DEBUG || DynamicLibraryBundleInfo.DEBUG;

    private static final List<String> glueLibNames = new ArrayList<String>(); // none

    private static final int symbolCount = 65;
    private static final String[] symbolNames = {
         "avutil_version",
         "avformat_version",
         "avcodec_version",
         "avresample_version",
/* 5 */  "swresample_version",

         // libavcodec
         "avcodec_register_all",
         "avcodec_close",
         "avcodec_string",
         "avcodec_find_decoder",
         "avcodec_open2",             // 53.6.0    (opt)
         "avcodec_alloc_frame",
         "avcodec_get_frame_defaults",
         "avcodec_free_frame",        // 54.28.0   (opt)
         "avcodec_default_get_buffer",     // <= 54 (opt), else sp_avcodec_default_get_buffer2
         "avcodec_default_release_buffer", // <= 54 (opt), else sp_av_frame_unref
         "avcodec_default_get_buffer2",    // 55 (opt)
         "avcodec_get_edge_width",
         "av_image_fill_linesizes",
         "avcodec_align_dimensions",
         "avcodec_align_dimensions2",
         "avcodec_flush_buffers",
         "av_init_packet",
         "av_new_packet",
         "av_destruct_packet",
         "av_free_packet",
         "avcodec_decode_audio4",     // 53.25.0   (opt)
/* 27 */ "avcodec_decode_video2",     // 52.23.0

         // libavutil
         "av_pix_fmt_descriptors",
         "av_frame_unref",            // 55.0.0 (opt)
         "av_realloc",
         "av_free",
         "av_get_bits_per_pixel",
         "av_samples_get_buffer_size",
         "av_get_bytes_per_sample",   // 51.4.0
         "av_opt_set_int",            // 51.12.0
         "av_dict_get",
         "av_dict_count",             // 54.*      (opt)
         "av_dict_set",
/* 28 */ "av_dict_free",

         // libavformat
         "avformat_alloc_context",
         "avformat_free_context",     // 52.96.0   (opt)
         "avformat_close_input",      // 53.17.0   (opt)
         "av_register_all",
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
/* 54 */ "avformat_find_stream_info", // 53.3.0    (opt)

         // libavdevice
/* 55 */ "avdevice_register_all",     // ???

         // libavresample
         "avresample_alloc_context",  //  1.0.1
         "avresample_open",
         "avresample_close",
         "avresample_free",
/* 60 */ "avresample_convert",

         // libavresample
         "av_opt_set_sample_fmt",     // actually lavu .. but exist only w/ swresample!
         "swr_alloc",
         "swr_init",
         "swr_free",
/* 65 */ "swr_convert",

    };

    // optional symbol names
    private static final String[] optionalSymbolNames = {
         "avformat_seek_file",        // ???       (opt)
         "avcodec_free_frame",        // 54.28.0   (opt)
         "av_frame_unref",            // 55.0.0 (opt)
         "av_dict_count",             // 54.*   (opt)
         "avcodec_default_get_buffer",     // <= 54 (opt), else sp_avcodec_default_get_buffer2
         "avcodec_default_release_buffer", // <= 54 (opt), else sp_av_frame_unref
         "avcodec_default_get_buffer2",    // 55 (opt)

         // libavdevice
         "avdevice_register_all",     // 53.0.0 (opt)

         // libavresample
         "avresample_version",        //  1.0.1
         "avresample_alloc_context",  //  1.0.1
         "avresample_open",
         "avresample_close",
         "avresample_free",
         "avresample_convert",

         // libavresample
         "av_opt_set_sample_fmt",     // actually lavu .. but exist only w/ swresample!
         "swresample_version",        //  0
         "swr_alloc",
         "swr_init",
         "swr_free",
         "swr_convert",
    };

    private static final long[] symbolAddr = new long[symbolCount];
    private static final boolean ready;
    private static final boolean libsUFCLoaded;
    private static final boolean avresampleLoaded;  // optional
    private static final boolean swresampleLoaded;  // optional
    private static final boolean avdeviceLoaded; // optional
    static final VersionNumber avCodecVersion;
    static final VersionNumber avFormatVersion;
    static final VersionNumber avUtilVersion;
    static final VersionNumber avResampleVersion;
    static final VersionNumber swResampleVersion;
    private static final FFMPEGNatives natives;

    private static final int LIB_IDX_UTI = 0;
    private static final int LIB_IDX_FMT = 1;
    private static final int LIB_IDX_COD = 2;
    private static final int LIB_IDX_DEV = 3;
    private static final int LIB_IDX_AVR = 4;
    private static final int LIB_IDX_SWR = 5;

    static {
        // native ffmpeg media player implementation is included in jogl_desktop and jogl_mobile
        GLProfile.initSingleton();
        boolean _ready = false;
        /** util, format, codec, device, avresample, swresample */
        boolean[] _loaded= new boolean[6];
        /** util, format, codec, avresample, swresample */
        VersionNumber[] _versions = new VersionNumber[5];
        try {
            _ready = initSymbols(_loaded, _versions);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        libsUFCLoaded = _loaded[LIB_IDX_UTI] && _loaded[LIB_IDX_FMT] && _loaded[LIB_IDX_COD];
        avdeviceLoaded = _loaded[LIB_IDX_DEV];
        avresampleLoaded = _loaded[LIB_IDX_AVR];
        swresampleLoaded = _loaded[LIB_IDX_SWR];
        avUtilVersion = _versions[0];
        avFormatVersion = _versions[1];
        avCodecVersion = _versions[2];
        avResampleVersion = _versions[3];
        swResampleVersion = _versions[4];
        if(!libsUFCLoaded) {
            System.err.println("LIB_AV Not Available: lavu, lavc, lavu");
            natives = null;
            ready = false;
        } else if(!_ready) {
            System.err.println("LIB_AV Not Matching");
            natives = null;
            ready = false;
        } else {
            if( avCodecVersion.getMajor() == 53 && avFormatVersion.getMajor() == 53 && avUtilVersion.getMajor() == 51 ) {
                // lavc53.lavf53.lavu51
                natives = new FFMPEGv08Natives();
            } else if( avCodecVersion.getMajor() == 54 && avFormatVersion.getMajor() == 54 && avUtilVersion.getMajor() == 52 ) {
                // lavc54.lavf54.lavu52.lavr01
                natives = new FFMPEGv09Natives();
            } else if( avCodecVersion.getMajor() == 55 && avFormatVersion.getMajor() == 55 && avUtilVersion.getMajor() == 52 ) {
                // lavc55.lavf55.lavu52.lavr01
                natives = new FFMPEGv10Natives();
            } else {
                System.err.println("LIB_AV No Version/Native-Impl Match");
                natives = null;
            }
            if( null != natives && FFMPEGStaticNatives.initIDs0() ) {
                ready = natives.initSymbols0(symbolAddr, symbolCount);
            } else {
                ready = false;
            }
        }
    }

    static boolean libsLoaded() { return libsUFCLoaded; }
    static boolean avDeviceLoaded() { return avdeviceLoaded; }
    static boolean avResampleLoaded() { return avresampleLoaded; }
    static boolean swResampleLoaded() { return swresampleLoaded; }
    static FFMPEGNatives getNatives() { return natives; }
    static boolean initSingleton() { return ready; }

    /**
     * @param loaded 6: util, format, codec, device, avresample, swresample
     * @param versions 5: util, format, codec, avresample, swresample
     * @return
     */
    private static final boolean initSymbols(boolean[] loaded, VersionNumber[] versions) {
        for(int i=0; i<6; i++) {
            loaded[i] = false;
        }
        final DynamicLibraryBundle dl = AccessController.doPrivileged(new PrivilegedAction<DynamicLibraryBundle>() {
                                          @Override
                                          public DynamicLibraryBundle run() {
                                              return new DynamicLibraryBundle(new FFMPEGDynamicLibraryBundleInfo());
                                          } } );
        dl.toString();
        for(int i=0; i<6; i++) {
            loaded[i] = dl.isToolLibLoaded(i);
        }
        if( !loaded[LIB_IDX_UTI] || !loaded[LIB_IDX_FMT] || !loaded[LIB_IDX_COD] ) {
            throw new RuntimeException("FFMPEG Tool library incomplete: [ avutil "+loaded[LIB_IDX_UTI]+", avformat "+loaded[LIB_IDX_FMT]+", avcodec "+loaded[LIB_IDX_COD]+"]");
        }
        if(symbolNames.length != symbolCount) {
            throw new InternalError("XXX0 "+symbolNames.length+" != "+symbolCount);
        }

        // optional symbol name set
        final Set<String> optionalSymbolNameSet = new HashSet<String>();
        optionalSymbolNameSet.addAll(Arrays.asList(optionalSymbolNames));

        // lookup
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                for(int i = 0; i<symbolCount; i++) {
                    symbolAddr[i] = dl.dynamicLookupFunction(symbolNames[i]);
                }
                return null;
            } } );

        // validate results
        boolean res = true;
        for(int i = 0; i<symbolCount; i++) {
            if( 0 == symbolAddr[i] ) {
                // no symbol, check optional and alternative symbols
                final String symbol = symbolNames[i];
                if ( !optionalSymbolNameSet.contains(symbol) ) {
                    System.err.println("Fail: Could not resolve symbol <"+symbolNames[i]+">: not optional, no alternatives.");
                    res = false;
                } else if(DEBUG) {
                    System.err.println("OK: Unresolved optional symbol <"+symbolNames[i]+">");
                }
            }
        }
        versions[0] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvVersion0(symbolAddr[0]));
        versions[1] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvVersion0(symbolAddr[1]));
        versions[2] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvVersion0(symbolAddr[2]));
        versions[3] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvVersion0(symbolAddr[3]));
        versions[4] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvVersion0(symbolAddr[4]));

        return res;
    }

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
    public final List<List<String>> getToolLibNames() {
        List<List<String>> libsList = new ArrayList<List<String>>();

        // 6: util, format, codec, device, avresample, swresample

        final List<String> avutil = new ArrayList<String>();
        avutil.add("avutil");        // default

        avutil.add("libavutil.so.53");     // dummy future proof
        avutil.add("libavutil.so.52");     // ffmpeg 1.2 + 2 / libav 9 + 10
        avutil.add("libavutil.so.51");     // 0.8
        avutil.add("libavutil.so.50");     // 0.7

        avutil.add("avutil-53");     // dummy future proof
        avutil.add("avutil-52");     // ffmpeg 1.2 + 2 / libav 9 + 10
        avutil.add("avutil-51");     // 0.8
        avutil.add("avutil-50");     // 0.7
        libsList.add(avutil);

        final List<String> avformat = new ArrayList<String>();
        avformat.add("avformat");    // default

        avformat.add("libavformat.so.56"); // dummy future proof
        avformat.add("libavformat.so.55"); // ffmpeg 2 / libav 10
        avformat.add("libavformat.so.54"); // ffmpeg 1.2 / libav 9
        avformat.add("libavformat.so.53"); // 0.8
        avformat.add("libavformat.so.52"); // 0.7

        avformat.add("avformat-56"); // dummy future proof
        avformat.add("avformat-55"); // ffmpeg 2 / libav 10
        avformat.add("avformat-54"); // ffmpeg 1.2 / libav 9
        avformat.add("avformat-53"); // 0.8
        avformat.add("avformat-52"); // 0.7
        libsList.add(avformat);

        final List<String> avcodec = new ArrayList<String>();
        avcodec.add("avcodec");      // default

        avcodec.add("libavcodec.so.56");   // dummy future proof
        avcodec.add("libavcodec.so.55");   // ffmpeg 2/ libav 10
        avcodec.add("libavcodec.so.54");   // ffmpeg 1.2 / libav 9
        avcodec.add("libavcodec.so.53");   // 0.8
        avcodec.add("libavcodec.so.52");   // 0.7

        avcodec.add("avcodec-56");   // dummy future proof
        avcodec.add("avcodec-55");   // ffmpeg 2/ libav 10
        avcodec.add("avcodec-54");   // ffmpeg 1.2 / libav 9
        avcodec.add("avcodec-53");   // 0.8
        avcodec.add("avcodec-52");   // 0.7
        libsList.add(avcodec);

        final List<String> avdevice = new ArrayList<String>();
        avdevice.add("avdevice");        // default

        avdevice.add("libavdevice.so.56");     // dummy future proof
        avdevice.add("libavdevice.so.55");     // ffmpeg 2
        avdevice.add("libavdevice.so.54");     // ffmpeg 1.2 / libav 10
        avdevice.add("libavdevice.so.53");     // 0.8 && libav 9

        avdevice.add("avdevice-56");     // dummy future proof
        avdevice.add("avdevice-55");     // ffmpeg 2
        avdevice.add("avdevice-54");     // ffmpeg 1.2 / libav 10
        avdevice.add("avdevice-53");     // 0.8 && libav 9
        libsList.add(avdevice);

        final List<String> avresample = new ArrayList<String>();
        avresample.add("avresample");        // default

        avresample.add("libavresample.so.2");     // dummy future proof
        avresample.add("libavresample.so.1");     // libav 9 + 10

        avresample.add("avresample-2");     // dummy future proof
        avresample.add("avresample-1");     // libav 9 + 10
        libsList.add(avresample);

        final List<String> swresample = new ArrayList<String>();
        swresample.add("swresample");        // default

        swresample.add("libswresample.so.1");     // dummy future proof
        swresample.add("libswresample.so.0");     // ffmpeg 1.2 + 2.x

        swresample.add("swresample-1");     // dummy future proof
        swresample.add("swresample-0");     // ffmpeg 1.2 + 2.x
        libsList.add(swresample);

        return libsList;
    }

    @Override
    public final List<String> getToolGetProcAddressFuncNameList() {
        return null;
    }

    @Override
    public final long toolGetProcAddress(long toolGetProcAddressHandle, String funcName) {
        return 0;
    }

    @Override
    public final boolean useToolGetProcAdressFirst(String funcName) {
        return false;
    }

    @Override
    public final RunnableExecutor getLibLoaderExecutor() {
        return DynamicLibraryBundle.getDefaultRunnableExecutor();
    }
}
