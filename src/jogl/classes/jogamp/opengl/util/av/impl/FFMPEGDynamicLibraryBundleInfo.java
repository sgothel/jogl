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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
    
    private static final int symbolCount = 54;
    private static final String[] symbolNames = {
         "avcodec_version",
         "avformat_version",
         "avutil_version",
/* 4 */  "avresample_version",

         // libavcodec
         "avcodec_register_all",
         "avcodec_close", 
         "avcodec_string", 
         "avcodec_find_decoder", 
         "avcodec_open2",             // 53.6.0    (opt) 
         "avcodec_alloc_frame",
         "avcodec_get_frame_defaults",
         "avcodec_free_frame",        // 54.28.0   (opt)
         "avcodec_default_get_buffer", 
         "avcodec_default_release_buffer",
         "avcodec_flush_buffers",
         "av_init_packet",
         "av_new_packet",
         "av_destruct_packet",
         "av_free_packet", 
         "avcodec_decode_audio4",     // 53.25.0   (opt)
/* 21 */ "avcodec_decode_video2",     // 52.23.0
        
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
/* 33 */ "av_dict_free",

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
/* 48 */ "avformat_find_stream_info", // 53.3.0    (opt)

         // libavdevice
/* 49 */ "avdevice_register_all",     // ??? 
         
         // libavresample
         "avresample_alloc_context",  //  1.0.1
         "avresample_open",
         "avresample_close",
         "avresample_free",
/* 54 */ "avresample_convert"
    };
    
    // alternate symbol names
    private static final String[][] altSymbolNames = {
        // { "av_find_stream_info",   "avformat_find_stream_info" },  // old, 53.3.0       
    };
    
    // optional symbol names
    private static final String[] optionalSymbolNames = {
         "avformat_seek_file",        // ???       (opt)
         "avcodec_free_frame",        // 54.28.0   (opt)
         "av_frame_unref",            // 55.0.0 (opt)
         "av_dict_count",             // 54.*   (opt)
         // libavdevice
         "avdevice_register_all",     // 53.0.0 (opt)
         // libavresample
         "avresample_version",        //  1.0.1
         "avresample_alloc_context",  //  1.0.1
         "avresample_open",
         "avresample_close",
         "avresample_free",
         "avresample_convert",
    };
    
    private static final long[] symbolAddr = new long[symbolCount];
    private static final boolean ready;
    private static final boolean libsLoaded;
    private static final boolean avresampleLoaded;  // optional
    private static final boolean avdeviceLoaded; // optional
    static final VersionNumber avCodecVersion;    
    static final VersionNumber avFormatVersion;
    static final VersionNumber avUtilVersion;
    static final VersionNumber avResampleVersion;
    private static final FFMPEGNatives natives;
    
    static {
        // native ffmpeg media player implementation is included in jogl_desktop and jogl_mobile     
        GLProfile.initSingleton();
        boolean _ready = false;
        boolean[] _libsLoaded= { false };
        boolean[] _avdeviceLoaded= { false };
        boolean[] _avresampleLoaded= { false };
        VersionNumber[] _versions = new VersionNumber[4];
        try {
            _ready = initSymbols(_libsLoaded, _avdeviceLoaded, _avresampleLoaded, _versions);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        libsLoaded = _libsLoaded[0];
        avdeviceLoaded = _avdeviceLoaded[0];
        avresampleLoaded = _avresampleLoaded[0];
        avCodecVersion = _versions[0];    
        avFormatVersion = _versions[1];
        avUtilVersion = _versions[2];
        avResampleVersion = _versions[3];
        if(!libsLoaded) {
            System.err.println("LIB_AV Not Available");
            natives = null;
            ready = false;
        } else if(!_ready) {
            System.err.println("LIB_AV Not Matching");
            natives = null;
            ready = false;
        } else {
            if( avCodecVersion.getMajor() <= 53 && avFormatVersion.getMajor() <= 53 && avUtilVersion.getMajor() <= 51 ) {
                // lavc53.lavf53.lavu51
                natives = new FFMPEGv08Natives();
            } else if( avCodecVersion.getMajor() == 54 && avFormatVersion.getMajor() <= 54 && avUtilVersion.getMajor() <= 52 ) {
                // lavc54.lavf54.lavu52.lavr01
                natives = new FFMPEGv09Natives();
            } else {
                System.err.println("LIB_AV No Version/Native-Impl Match");
                natives = null;
            }
            if( null != natives ) {
                ready = natives.initSymbols0(symbolAddr, symbolCount);
            } else {
                ready = false;
            }
        }
    }
    
    static boolean libsLoaded() { return libsLoaded; }
    static boolean avDeviceLoaded() { return avdeviceLoaded; }
    static boolean avResampleLoaded() { return avresampleLoaded; }
    static FFMPEGNatives getNatives() { return natives; }
    static boolean initSingleton() { return ready; }
    
    private static final boolean initSymbols(boolean[] libsLoaded, boolean[] avdeviceLoaded, boolean[] avresampleLoaded,
                                             VersionNumber[] versions) {
        libsLoaded[0] = false;
        final DynamicLibraryBundle dl = AccessController.doPrivileged(new PrivilegedAction<DynamicLibraryBundle>() {
                                          public DynamicLibraryBundle run() {
                                              return new DynamicLibraryBundle(new FFMPEGDynamicLibraryBundleInfo());
                                          } } );
        final boolean avutilLoaded = dl.isToolLibLoaded(0); 
        final boolean avformatLoaded = dl.isToolLibLoaded(1);
        final boolean avcodecLoaded = dl.isToolLibLoaded(2);
        if(!avutilLoaded || !avformatLoaded || !avcodecLoaded) {
            throw new RuntimeException("FFMPEG Tool library incomplete: [ avutil "+avutilLoaded+", avformat "+avformatLoaded+", avcodec "+avcodecLoaded+"]");
        }
        avdeviceLoaded[0] = dl.isToolLibLoaded(3);
        avresampleLoaded[0] = dl.isToolLibLoaded(4);
        libsLoaded[0] = true;
        
        if(symbolNames.length != symbolCount) {
            throw new InternalError("XXX0 "+symbolNames.length+" != "+symbolCount);
        }
        
        // optional symbol name set
        final Set<String> optionalSymbolNameSet = new HashSet<String>();
        optionalSymbolNameSet.addAll(Arrays.asList(optionalSymbolNames));
        
        // alternate symbol name mapping to indexed array
        final Map<String, Integer> mAltSymbolNames = new HashMap<String, Integer>();
        final int[][] iAltSymbolNames = new int[altSymbolNames.length][];
        {
            final List<String> symbolNameList = Arrays.asList(symbolNames);
            for(int i=0; i<altSymbolNames.length; i++) {
                iAltSymbolNames[i] = new int[altSymbolNames[i].length];        
                for(int j=0; j<altSymbolNames[i].length; j++) {
                    mAltSymbolNames.put(altSymbolNames[i][j], new Integer(i));
                    iAltSymbolNames[i][j] = symbolNameList.indexOf(altSymbolNames[i][j]); 
                }            
            }
        }
        
        // lookup
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
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
                    // check for API changed symbols
                    boolean ok = false;                    
                    final Integer cI = mAltSymbolNames.get(symbol);
                    if ( null != cI ) {
                        // check whether alternative symbol is available
                        final int ci = cI.intValue();
                        for(int j=0; !ok && j<iAltSymbolNames[ci].length; j++) {
                            final int si = iAltSymbolNames[ci][j];
                            ok = 0 != symbolAddr[si];
                            if(ok && DEBUG) {
                                System.err.println("OK: Unresolved symbol <"+symbol+">, but has alternative <"+symbolNames[si]+">");
                            }
                        }
                    }
                    if(!ok) {
                        System.err.println("Fail: Could not resolve symbol <"+symbolNames[i]+">: not optional, no alternatives.");
                        res = false;
                    }
                } else if(DEBUG) {
                    System.err.println("OK: Unresolved optional symbol <"+symbolNames[i]+">");
                }
            }
        }
        versions[0] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvCodecVersion0(symbolAddr[0]));
        versions[1] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvFormatVersion0(symbolAddr[1]));
        versions[2] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvUtilVersion0(symbolAddr[2]));
        versions[3] = FFMPEGStaticNatives.getAVVersion(FFMPEGStaticNatives.getAvResampleVersion0(symbolAddr[3]));
        
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

        final List<String> avutil = new ArrayList<String>();
        avutil.add("avutil");        // default

        avutil.add("libavutil.so.53");     // dummy future proof
        avutil.add("libavutil.so.52");     // 9
        avutil.add("libavutil.so.51");     // 0.8
        avutil.add("libavutil.so.50");     // 0.7
        
        avutil.add("avutil-53");     // dummy future proof
        avutil.add("avutil-52");     // 9
        avutil.add("avutil-51");     // 0.8
        avutil.add("avutil-50");     // 0.7
        libsList.add(avutil);
        
        final List<String> avformat = new ArrayList<String>();
        avformat.add("avformat");    // default

        avformat.add("libavformat.so.55"); // dummy future proof
        avformat.add("libavformat.so.54"); // 9
        avformat.add("libavformat.so.53"); // 0.8
        avformat.add("libavformat.so.52"); // 0.7
        
        avformat.add("avformat-55"); // dummy future proof
        avformat.add("avformat-54"); // 9
        avformat.add("avformat-53"); // 0.8
        avformat.add("avformat-52"); // 0.7
        libsList.add(avformat);
        
        final List<String> avcodec = new ArrayList<String>();
        avcodec.add("avcodec");      // default

        avcodec.add("libavcodec.so.55");   // dummy future proof
        avcodec.add("libavcodec.so.54");   // 9
        avcodec.add("libavcodec.so.53");   // 0.8
        avcodec.add("libavcodec.so.52");   // 0.7        
        
        avcodec.add("avcodec-55");   // dummy future proof
        avcodec.add("avcodec-54");   // 9
        avcodec.add("avcodec-53");   // 0.8
        avcodec.add("avcodec-52");   // 0.7
        libsList.add(avcodec);
        
        final List<String> avdevice = new ArrayList<String>();
        avdevice.add("avdevice");        // default

        avdevice.add("libavdevice.so.54");     // dummy future proof
        avdevice.add("libavdevice.so.53");     // 0.8 && 9
        
        avdevice.add("avdevice-54");     // dummy future proof
        avdevice.add("avdevice-53");     // 0.8 && 9
        libsList.add(avdevice);
        
        final List<String> avresample = new ArrayList<String>();
        avresample.add("avresample");        // default

        avresample.add("libavresample.so.2");     // dummy future proof
        avresample.add("libavresample.so.1");     // 9
        
        avresample.add("avresample-2");     // dummy future proof
        avresample.add("avresample-1");     // 9
        libsList.add(avresample);

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
