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

/**
 * FIXME: We need native structure access methods to deal with API changes
 *        in the libav headers, which break binary compatibility!
 *        Currently we are binary compatible w/ [0.6 ?, ] 0.7 and 0.8 but not w/ trunk.
 *        
 *        ChangeList for trunk:
 *          Thu Jan 12 11:21:02 2012 a17479dfce67fbea2d0a1bf303010dce1e79059f major 53 -> 54
 *          Mon Feb 27 22:40:11 2012 ee42df8a35c2b795f524c856834d0823dbd4e75d reorder AVStream and AVFormatContext
 *          Tue Feb 28 12:07:53 2012 322537478b63c6bc01e640643550ff539864d790 minor  1 ->  2
 */
class FFMPEGDynamicLibraryBundleInfo implements DynamicLibraryBundleInfo  {
    private static final List<String> glueLibNames = new ArrayList<String>(); // none
    
    private static final int symbolCount = 32;
    private static final String[] symbolNames = {
         "avcodec_version",
         "avformat_version",
/* 3 */  "avutil_version",
        
         // libavcodec
         "avcodec_close", 
         "avcodec_string", 
         "avcodec_find_decoder", 
         "avcodec_open2",             // 53.6.0    (opt) 
         "avcodec_open", 
         "avcodec_alloc_frame", 
         "avcodec_default_get_buffer", 
         "avcodec_default_release_buffer", 
         "av_free_packet", 
         "avcodec_decode_audio4",     // 53.25.0   (opt)
         "avcodec_decode_audio3",     // 52.23.0
/* 15 */ "avcodec_decode_video2",     // 52.23.0
        
         // libavutil
         "av_pix_fmt_descriptors", 
         "av_free", 
         "av_get_bits_per_pixel",
/* 19 */ "av_samples_get_buffer_size",
        
         // libavformat
         "avformat_alloc_context",
         "avformat_free_context",     // 52.96.0   (opt)
         "avformat_close_input",      // 53.17.0   (opt)
         "av_close_input_file",
         "av_register_all", 
         "avformat_open_input", 
         "av_dump_format", 
         "av_read_frame",
         "av_seek_frame",
         "avformat_network_init",     // 53.13.0   (opt)
         "avformat_network_deinit",   // 53.13.0   (opt)
         "avformat_find_stream_info", // 53.3.0    (opt)
/* 32 */ "av_find_stream_info",
    };
    
    // alternate symbol names
    private static final String[][] altSymbolNames = {
        { "avcodec_open",          "avcodec_open2" },              // old, 53.6.0
        { "avcodec_decode_audio3", "avcodec_decode_audio4" },      // old, 53.25.0
        { "av_close_input_file",   "avformat_close_input" },       // old, 53.17.0
        { "av_find_stream_info",   "avformat_find_stream_info" },  // old, 53.3.0       
    };
    
    // optional symbol names
    private static final String[] optionalSymbolNames = {
         "avformat_free_context",     // 52.96.0   (opt)
         "avformat_network_init",     // 53.13.0   (opt)
         "avformat_network_deinit",   // 53.13.0   (opt)
    };
    
    private static long[] symbolAddr;
    private static final boolean ready;
    
    static {
        // native ffmpeg media player implementation is included in jogl_desktop and jogl_mobile     
        GLProfile.initSingleton();
        boolean _ready = false;
        try {
            _ready = initSymbols();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        ready = _ready;
        if(!ready) {
            System.err.println("FFMPEG: Not Available");
        }
    }
    
    static boolean initSingleton() { return ready; }
    
    private static final boolean initSymbols() {
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
        if(!dl.isToolLibComplete()) {
            throw new RuntimeException("FFMPEG Tool libraries incomplete");
        }
        if(symbolNames.length != symbolCount) {
            throw new InternalError("XXX0 "+symbolNames.length+" != "+symbolCount);
        }
        symbolAddr = new long[symbolCount];        
        
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
                            if(ok && (true || DEBUG )) { // keep it verbose per default for now ..
                                System.err.println("OK: Unresolved symbol <"+symbol+">, but has alternative <"+symbolNames[si]+">");
                            }
                        }
                    }
                    if(!ok) {
                        System.err.println("Fail: Could not resolve symbol <"+symbolNames[i]+">: not optional, no alternatives.");
                        return false;
                    }
                } else if(true || DEBUG ) { // keep it verbose per default for now ..
                    System.err.println("OK: Unresolved optional symbol <"+symbolNames[i]+">");
                }
            }
        }
        return initSymbols0(symbolAddr, symbolCount);
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

        avutil.add("libavutil.so.52");     // dummy future proof
        avutil.add("libavutil.so.51");     // 0.8
        avutil.add("libavutil.so.50");     // 0.7
        
        avutil.add("avutil-52");     // dummy future proof
        avutil.add("avutil-51");     // 0.8
        avutil.add("avutil-50");     // 0.7
        libsList.add(avutil);
        
        final List<String> avformat = new ArrayList<String>();
        avformat.add("avformat");    // default

        avformat.add("libavformat.so.55"); // dummy future proof
        avformat.add("libavformat.so.54"); // 0.?
        avformat.add("libavformat.so.53"); // 0.8
        avformat.add("libavformat.so.52"); // 0.7
        
        avformat.add("avformat-55"); // dummy future proof
        avformat.add("avformat-54"); // 0.?
        avformat.add("avformat-53"); // 0.8
        avformat.add("avformat-52"); // 0.7
        libsList.add(avformat);
        
        final List<String> avcodec = new ArrayList<String>();
        avcodec.add("avcodec");      // default

        avcodec.add("libavcodec.so.55");   // dummy future proof
        avcodec.add("libavcodec.so.54");   // 0.?
        avcodec.add("libavcodec.so.53");   // 0.8
        avcodec.add("libavcodec.so.52");   // 0.7        
        
        avcodec.add("avcodec-55");   // dummy future proof
        avcodec.add("avcodec-54");   // 0.?
        avcodec.add("avcodec-53");   // 0.8
        avcodec.add("avcodec-52");   // 0.7
        libsList.add(avcodec);
                
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
    
    private static native boolean initSymbols0(long[] symbols, int count);
}
