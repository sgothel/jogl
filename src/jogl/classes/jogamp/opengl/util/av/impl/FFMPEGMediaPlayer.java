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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLException;

import com.jogamp.common.os.Platform;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.opengl.util.TimeFrameI;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.av.AudioSink;
import com.jogamp.opengl.util.av.AudioSink.AudioFormat;
import com.jogamp.opengl.util.av.AudioSinkFactory;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.texture.Texture;

import jogamp.opengl.GLContextImpl;
import jogamp.opengl.util.av.GLMediaPlayerImpl;
import jogamp.opengl.util.av.impl.FFMPEGNatives.PixelFormat;
import jogamp.opengl.util.av.impl.FFMPEGNatives.SampleFormat;

/***
 * Implementation utilizes <a href="http://libav.org/">Libav</a>
 * or  <a href="http://ffmpeg.org/">FFmpeg</a> which is ubiquitous
 * available and usually pre-installed on Unix platforms. Due to legal 
 * reasons we cannot deploy binaries of it, which contains patented codecs.
 * Besides the default BSD/Linux/.. repositories and installations,
 * precompiled binaries can be found at the listed location below. 
 * <p>
 * Implements YUV420P to RGB fragment shader conversion 
 * and the usual packed RGB formats.
 * The decoded video frame is written directly into an OpenGL texture 
 * on the GPU in it's native format. A custom fragment shader converts 
 * the native pixelformat to a usable RGB format if required. 
 * Hence only 1 copy is required before bloating the picture 
 * from YUV to RGB, for example.
 * </p> 
 * <p>
 * Utilizes a slim dynamic and native binding to the Lib_av 
 * libraries:
 * <ul>
 *   <li>libavutil</li>
 *   <li>libavformat</li>
 *   <li>libavcodec</li>
 * </ul> 
 * </p>
 * <p>
 * http://libav.org/
 * </p>
 * <p> 
 * Check tag 'FIXME: Add more planar formats !' 
 * here and in the corresponding native code
 * <code>jogl/src/jogl/native/ffmpeg/jogamp_opengl_util_av_impl_FFMPEGMediaPlayer.c</code>
 * </p>
 * <p>
 * TODO:
 * <ul>
 *   <li>Audio Output</li>
 *   <li>Off thread <i>next frame</i> processing using multiple target textures</li>
 *   <li>better pts sync handling</li>
 *   <li>fix seek</li>   
 * </ul> 
 * </p>
 * Pre-compiled Libav / FFmpeg packages:
 * <ul>
 *   <li>Windows: http://ffmpeg.zeranoe.com/builds/</li>
 *   <li>MacOSX: http://www.ffmpegx.com/</li>
 *   <li>OpenIndiana/Solaris:<pre>
 *       pkg set-publisher -p http://pkg.openindiana.org/sfe-encumbered.
 *       pkt install pkg:/video/ffmpeg
 *       </pre></li>
 * </ul> 
 */
public class FFMPEGMediaPlayer extends GLMediaPlayerImpl {

    /** POSIX ENOSYS {@value}: Function not implemented. FIXME: Move to GlueGen ?!*/
    private static final int ENOSYS = 38;
    
    // Instance data
    private static final FFMPEGNatives natives;
    private static final int avUtilMajorVersionCC;
    private static final int avFormatMajorVersionCC;
    private static final int avCodecMajorVersionCC;    
    private static final int avResampleMajorVersionCC;    
    private static final boolean available;
    
    static {
        final boolean libAVGood = FFMPEGDynamicLibraryBundleInfo.initSingleton();
        final boolean libAVVersionGood;
        if( FFMPEGDynamicLibraryBundleInfo.libsLoaded() ) {
            natives = FFMPEGDynamicLibraryBundleInfo.getNatives();
            avUtilMajorVersionCC = natives.getAvUtilMajorVersionCC0();
            avFormatMajorVersionCC = natives.getAvFormatMajorVersionCC0();
            avCodecMajorVersionCC = natives.getAvCodecMajorVersionCC0();
            avResampleMajorVersionCC = natives.getAvResampleMajorVersionCC0();
            System.err.println("LIB_AV Util    : "+FFMPEGDynamicLibraryBundleInfo.avUtilVersion+" [cc "+avUtilMajorVersionCC+"]");
            System.err.println("LIB_AV Format  : "+FFMPEGDynamicLibraryBundleInfo.avFormatVersion+" [cc "+avFormatMajorVersionCC+"]");
            System.err.println("LIB_AV Codec   : "+FFMPEGDynamicLibraryBundleInfo.avCodecVersion+" [cc "+avCodecMajorVersionCC+"]");
            System.err.println("LIB_AV Device  : [loaded "+FFMPEGDynamicLibraryBundleInfo.avDeviceLoaded()+"]");
            System.err.println("LIB_AV Resample: "+FFMPEGDynamicLibraryBundleInfo.avResampleVersion+" [cc "+avResampleMajorVersionCC+", loaded "+FFMPEGDynamicLibraryBundleInfo.avResampleLoaded()+"]");
            libAVVersionGood = avUtilMajorVersionCC   == FFMPEGDynamicLibraryBundleInfo.avUtilVersion.getMajor() &&
                               avFormatMajorVersionCC == FFMPEGDynamicLibraryBundleInfo.avFormatVersion.getMajor() &&
                               avCodecMajorVersionCC  == FFMPEGDynamicLibraryBundleInfo.avCodecVersion.getMajor() &&
                               avResampleMajorVersionCC  == FFMPEGDynamicLibraryBundleInfo.avResampleVersion.getMajor();
            if( !libAVVersionGood ) {
                System.err.println("LIB_AV Not Matching Compile-Time / Runtime Major-Version");
            }
        } else {
            natives = null;
            avUtilMajorVersionCC = 0;
            avFormatMajorVersionCC = 0;
            avCodecMajorVersionCC = 0;
            avResampleMajorVersionCC = 0;
            libAVVersionGood = false;
        }
        available = libAVGood && libAVVersionGood && null != natives ? natives.initIDs0() : false;
    }
    
    public static final boolean isAvailable() { return available; }

    //
    // General
    //
    
    private long moviePtr = 0;    
    
    //
    // Video
    //
    
    private String texLookupFuncName = "ffmpegTexture2D";
    private boolean usesTexLookupShader = false;    
    private PixelFormat vPixelFmt = null;
    private int vPlanes = 0;
    private int vBitsPerPixel = 0;
    private int vBytesPerPixelPerPlane = 0;    
    private int[] vLinesize = { 0, 0, 0 }; // per plane
    private int[] vTexWidth = { 0, 0, 0 }; // per plane
    private int texWidth, texHeight; // overall (stuffing planes in one texture)
    private String singleTexComp = "r";
    private GLPixelStorageModes psm;

    //
    // Audio
    //
    
    private AudioSink.AudioFormat avChosenAudioFormat;
    private int audioSamplesPerFrameAndChannel = 0;
    
    public FFMPEGMediaPlayer() {
        if(!available) {
            throw new RuntimeException("FFMPEGMediaPlayer not available");
        }
        moviePtr = natives.createInstance0(this, DEBUG_NATIVE);
        if(0==moviePtr) {
            throw new GLException("Couldn't create FFMPEGInstance");
        }
        psm = new GLPixelStorageModes();
        audioSink = null;
    }

    @Override
    protected final void destroyImpl(GL gl) {
        if (moviePtr != 0) {
            natives.destroyInstance0(moviePtr);
            moviePtr = 0;
        }
        destroyAudioSink();
    }
    private final void destroyAudioSink() {
        final AudioSink _audioSink = audioSink;
        if( null != _audioSink ) {            
            audioSink = null;
            _audioSink.destroy();
        }
    }
    
    public static final String dev_video_linux = "/dev/video";
    
    @Override
    protected final void initStreamImpl(int vid, int aid) throws IOException {
        if(0==moviePtr) {
            throw new GLException("FFMPEG native instance null");
        }
        if(DEBUG) {
            System.err.println("initStream: p1 "+this);
        }
        
        final String streamLocS=streamLoc.toString().replaceAll("%20", " ");
        destroyAudioSink();
        if( GLMediaPlayer.STREAM_ID_NONE == aid ) {
            audioSink = AudioSinkFactory.createNull();
        } else {
            audioSink = AudioSinkFactory.createDefault();
        }
        final AudioFormat preferredAudioFormat = audioSink.getPreferredFormat();
        if(DEBUG) {
            System.err.println("initStream: p2 preferred "+preferredAudioFormat+", "+this);
        }
        
        final boolean isCameraInput = null != cameraHostPart;
        final String resStreamLocS;
        if( isCameraInput ) {
            switch(Platform.OS_TYPE) {
                case ANDROID:
                    // ??
                case FREEBSD:
                case HPUX:
                case LINUX:
                case SUNOS:
                    resStreamLocS = dev_video_linux + cameraHostPart;
                    break;
                case WINDOWS:
                    resStreamLocS = cameraHostPart;
                    break;
                case MACOS:
                case OPENKODE:
                default:
                    resStreamLocS = streamLocS; // FIXME: ??
                    break;            
            }
        } else {
            resStreamLocS = streamLocS;
        }
        final int aMaxChannelCount = audioSink.getMaxSupportedChannels();
        final int aPrefSampleRate = preferredAudioFormat.sampleRate;
         // setStream(..) issues updateAttributes*(..), and defines avChosenAudioFormat, vid, aid, .. etc
        natives.setStream0(moviePtr, resStreamLocS, isCameraInput, vid, aid, aMaxChannelCount, aPrefSampleRate);
    }

    @Override
    protected final void initGLImpl(GL gl) throws IOException, GLException {
        if(0==moviePtr) {
            throw new GLException("FFMPEG native instance null");
        }
        if(null == audioSink) {
            throw new GLException("AudioSink null");
        }
        final int audioQueueLimit;
        if( null != gl ) {
            final GLContextImpl ctx = (GLContextImpl)gl.getContext();
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                public Object run() {
                    final ProcAddressTable pt = ctx.getGLProcAddressTable();
                    final long procAddrGLTexSubImage2D = pt.getAddressFor("glTexSubImage2D");
                    final long procAddrGLGetError = pt.getAddressFor("glGetError");
                    final long procAddrGLFlush = pt.getAddressFor("glFlush");
                    final long procAddrGLFinish = pt.getAddressFor("glFinish");
                    natives.setGLFuncs0(moviePtr, procAddrGLTexSubImage2D, procAddrGLGetError, procAddrGLFlush, procAddrGLFinish);
                    return null;
            } } );
            audioQueueLimit = AudioSink.DefaultQueueLimitWithVideo;
        } else {
            audioQueueLimit = AudioSink.DefaultQueueLimitAudioOnly;
        }
        final float frameDuration;
        if( audioSamplesPerFrameAndChannel > 0 ) {
            frameDuration= avChosenAudioFormat.getSamplesDuration(audioSamplesPerFrameAndChannel);
        } else {
            frameDuration = AudioSink.DefaultFrameDuration;
        }
        if(DEBUG) {
            System.err.println("initGL: p3 avChosen "+avChosenAudioFormat);
        }
        
        if( STREAM_ID_NONE == aid ) {
            audioSink.destroy();
            audioSink = AudioSinkFactory.createNull();
        }
        final boolean audioSinkOK = audioSink.init(avChosenAudioFormat, frameDuration, AudioSink.DefaultInitialQueueSize, AudioSink.DefaultQueueGrowAmount, audioQueueLimit);
        if( !audioSinkOK ) {
            System.err.println("AudioSink "+audioSink.getClass().getName()+" does not support "+avChosenAudioFormat+", using Null");
            audioSink.destroy();
            audioSink = AudioSinkFactory.createNull();
            audioSink.init(avChosenAudioFormat, 0, AudioSink.DefaultInitialQueueSize, AudioSink.DefaultQueueGrowAmount, audioQueueLimit);
        }
        if(DEBUG) {
            System.err.println("initGL: p4 chosen "+avChosenAudioFormat);
            System.err.println("initGL: p4 chosen "+audioSink);
        }
        
        if( null != gl ) {
            int tf, tif=GL.GL_RGBA; // texture format and internal format
            int tt = GL.GL_UNSIGNED_BYTE;
            switch(vBytesPerPixelPerPlane) {
                case 1:
                    if( gl.isGL3ES3() ) {
                        // RED is supported on ES3 and >= GL3 [core]; ALPHA is deprecated on core
                        tf = GL2ES2.GL_RED;   tif=GL2ES2.GL_RED; singleTexComp = "r";
                    } else {
                        // ALPHA is supported on ES2 and GL2, i.e. <= GL3 [core] or compatibility
                        tf = GL2ES2.GL_ALPHA; tif=GL2ES2.GL_ALPHA; singleTexComp = "a";
                    }
                    break;
                
                case 2: if( vPixelFmt == PixelFormat.YUYV422 ) {
                            // YUYV422: // < packed YUV 4:2:2, 2x 16bpp, Y0 Cb Y1 Cr
                            // Stuffed into RGBA half width texture
                            tf = GL2ES2.GL_RGBA; tif=GL2ES2.GL_RGBA; break;
                        } else {
                            tf = GL2ES2.GL_RG;   tif=GL2ES2.GL_RG; break;
                        }
                case 3: tf = GL2ES2.GL_RGB;   tif=GL.GL_RGB;   break;
                case 4: tf = GL2ES2.GL_RGBA;  tif=GL.GL_RGBA;  break;
                default: throw new RuntimeException("Unsupported bytes-per-pixel / plane "+vBytesPerPixelPerPlane);
            }        
            setTextureFormat(tif, tf);
            setTextureType(tt);
        }
    }    
    @Override
    protected final TextureFrame createTexImage(GL gl, int texName) {
        return new TextureFrame( createTexImageImpl(gl, texName, texWidth, texHeight, true) );
    }
    
    /**
     * @param sampleRate sample rate in Hz (1/s)
     * @param sampleSize sample size in bits
     * @param channelCount number of channels
     * @param signed true if signed number, false for unsigned
     * @param fixedP true for fixed point value, false for unsigned floating point value with a sampleSize of 32 (float) or 64 (double)
     * @param planar true for planar data package (each channel in own data buffer), false for packed data channels interleaved in one buffer.
     * @param littleEndian true for little-endian, false for big endian
     * @return
     */
    
    /**
     * Native callback
     * Converts the given libav/ffmpeg values to {@link AudioFormat} and returns {@link AudioSink#isSupported(AudioFormat)}. 
     * @param audioSampleFmt ffmpeg/libav audio-sample-format, see {@link SampleFormat}.
     * @param audioSampleRate sample rate in Hz (1/s)
     * @param audioChannels number of channels
     */
    final boolean isAudioFormatSupported(int audioSampleFmt, int audioSampleRate, int audioChannels) {
        final AudioFormat audioFormat = avAudioFormat2Local(SampleFormat.valueOf(audioSampleFmt), audioSampleRate, audioChannels);
        final boolean res = audioSink.isSupported(audioFormat);
        if( DEBUG ) {
            System.err.println("AudioSink.isSupported: "+res+": "+audioFormat);
        }
        return res;
    }
    
    /**
     * Returns {@link AudioFormat} as converted from the given libav/ffmpeg values. 
     * @param audioSampleFmt ffmpeg/libav audio-sample-format, see {@link SampleFormat}.
     * @param audioSampleRate sample rate in Hz (1/s)
     * @param audioChannels number of channels
     */
    private final AudioFormat avAudioFormat2Local(SampleFormat audioSampleFmt, int audioSampleRate, int audioChannels) {
        final int sampleSize;
        boolean planar = true;
        final boolean signed, fixedP;
        switch( audioSampleFmt ) {
            case S32:
                planar = false;
            case S32P:
                sampleSize = 32;
                signed = true;
                fixedP = true;
                break;
            case S16:
                planar = false;
            case S16P:
                sampleSize = 16;
                signed = true;
                fixedP = true;
                break;
            case U8:
                planar = false;
            case U8P:
                sampleSize = 8;
                signed = false;
                fixedP = true;
                break;
            case DBL:
                planar = false;
            case DBLP:
                sampleSize = 64;
                signed = true;
                fixedP = true;
                break;
            case FLT:
                planar = false;
            case FLTP:
                sampleSize = 32;
                signed = true;
                fixedP = true;
                break;
            default: // FIXME: Add more formats !
                throw new IllegalArgumentException("Unsupported sampleformat: "+audioSampleFmt);
        }
        return new AudioFormat(audioSampleRate, sampleSize, audioChannels, signed, fixedP, planar, true /* littleEndian */);
    }
    
    /**
     * Native callback
     * @param pixFmt
     * @param planes
     * @param bitsPerPixel
     * @param bytesPerPixelPerPlane
     * @param lSz0
     * @param lSz1
     * @param lSz2
     * @param tWd0
     * @param tWd1
     * @param tWd2
     * @param audioSampleFmt
     * @param audioSampleRate
     * @param audioChannels
     * @param audioSamplesPerFrameAndChannel in audio samples per frame and channel
     */
    void updateAttributes2(int pixFmt, int planes, int bitsPerPixel, int bytesPerPixelPerPlane,
                           int lSz0, int lSz1, int lSz2,
                           int tWd0, int tWd1, int tWd2, int vW, int vH,
                           int audioSampleFmt, int audioSampleRate, 
                           int audioChannels, int audioSamplesPerFrameAndChannel) {
        vPixelFmt = PixelFormat.valueOf(pixFmt);
        vPlanes = planes;
        vBitsPerPixel = bitsPerPixel;
        vBytesPerPixelPerPlane = bytesPerPixelPerPlane;
        vLinesize[0] = lSz0; vLinesize[1] = lSz1; vLinesize[2] = lSz2;
        vTexWidth[0] = tWd0; vTexWidth[1] = tWd1; vTexWidth[2] = tWd2;
        
        switch(vPixelFmt) {
            case YUV420P: // < planar YUV 4:2:0, 12bpp, (1 Cr & Cb sample per 2x2 Y samples)
                usesTexLookupShader = true;
                // YUV420P: Adding U+V on right side of fixed height texture,
                //          since width is already aligned by decoder.
                // Y=w*h, Y=w/2*h/2, U=w/2*h/2
                // w*h + 2 ( w/2 * h/2 ) 
                // w*h + w*h/2
                // 2*w/2 * h 
                texWidth = vTexWidth[0] + vTexWidth[1]; texHeight = vH;
                break;
            case YUYV422: // < packed YUV 4:2:2, 2x 16bpp, Y0 Cb Y1 Cr - stuffed into RGBA half width texture
                usesTexLookupShader = true;
                texWidth = vTexWidth[0]; texHeight = vH; 
                break;
            case RGB24:
            case BGR24:
            case ARGB:
            case RGBA:
            case ABGR:
            case BGRA:
                usesTexLookupShader = false;
                texWidth = vTexWidth[0]; texHeight = vH; 
                break;
            default: // FIXME: Add more formats !
                throw new RuntimeException("Unsupported pixelformat: "+vPixelFmt);
        }
        final SampleFormat aSampleFmt = SampleFormat.valueOf(audioSampleFmt);
        avChosenAudioFormat = avAudioFormat2Local(aSampleFmt, audioSampleRate, audioChannels);

        this.audioSamplesPerFrameAndChannel = audioSamplesPerFrameAndChannel;
        
        if(DEBUG) {
            System.err.println("audio: fmt "+aSampleFmt+", "+avChosenAudioFormat+", aFrameSize/fc "+audioSamplesPerFrameAndChannel);
            System.err.println("video: fmt "+vW+"x"+vH+", "+vPixelFmt+", planes "+vPlanes+", bpp "+vBitsPerPixel+"/"+vBytesPerPixelPerPlane+", usesTexLookupShader "+usesTexLookupShader);
            for(int i=0; i<3; i++) {
                System.err.println("video: "+i+": "+vTexWidth[i]+"/"+vLinesize[i]);
            }
            System.err.println("video: total tex "+texWidth+"x"+texHeight);
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * If this implementation generates a specialized shader,
     * it allows the user to override the default function name <code>ffmpegTexture2D</code>.
     * Otherwise the call is delegated to it's super class.
     */
    @Override
    public final String getTextureLookupFunctionName(String desiredFuncName) throws IllegalStateException {
        if(State.Uninitialized == state) {
            throw new IllegalStateException("Instance not initialized: "+this);
        }
        if( usesTexLookupShader ) {
            if(null != desiredFuncName && desiredFuncName.length()>0) {
                texLookupFuncName = desiredFuncName;
            }
            return texLookupFuncName;
        }
        return super.getTextureLookupFunctionName(desiredFuncName);        
    }
    
    /**
     * {@inheritDoc}
     * 
     * Depending on the pixelformat, a specific conversion shader is being created,
     * e.g. YUV420P to RGB. Otherwise the call is delegated to it's super class.  
     */ 
    @Override
    public final String getTextureLookupFragmentShaderImpl() throws IllegalStateException {
      if(State.Uninitialized == state) {
          throw new IllegalStateException("Instance not initialized: "+this);
      }
      if( !usesTexLookupShader ) {
          return super.getTextureLookupFragmentShaderImpl();
      }
      final float tc_w_1 = (float)getWidth() / (float)texWidth;
      switch(vPixelFmt) {
        case YUV420P: // < planar YUV 4:2:0, 12bpp, (1 Cr & Cb sample per 2x2 Y samples)
          return
              "vec4 "+texLookupFuncName+"(in "+getTextureSampler2DType()+" image, in vec2 texCoord) {\n"+
              "  vec2 u_off = vec2("+tc_w_1+", 0.0);\n"+
              "  vec2 v_off = vec2("+tc_w_1+", 0.5);\n"+
              "  vec2 tc_half = texCoord*0.5;\n"+
              "  float y,u,v,r,g,b;\n"+
              "  y = texture2D(image, texCoord)."+singleTexComp+";\n"+
              "  u = texture2D(image, u_off+tc_half)."+singleTexComp+";\n"+
              "  v = texture2D(image, v_off+tc_half)."+singleTexComp+";\n"+
              "  y = 1.1643*(y-0.0625);\n"+
              "  u = u-0.5;\n"+
              "  v = v-0.5;\n"+
              "  r = y+1.5958*v;\n"+
              "  g = y-0.39173*u-0.81290*v;\n"+
              "  b = y+2.017*u;\n"+
              "  return vec4(r, g, b, 1);\n"+
              "}\n"
          ;
        case YUYV422: // < packed YUV 4:2:2, 2 x 16bpp, [Y0 Cb] [Y1 Cr]
                      // Stuffed into RGBA half width texture
          return
              "vec4 "+texLookupFuncName+"(in "+getTextureSampler2DType()+" image, in vec2 texCoord) {\n"+
              "  "+
              "  float y1,u,y2,v,y,r,g,b;\n"+
              "  vec2 tc_halfw = vec2(texCoord.x*0.5, texCoord.y);\n"+
              "  vec4 yuyv = texture2D(image, tc_halfw).rgba;\n"+
              "  y1 = yuyv.r;\n"+
              "  u  = yuyv.g;\n"+
              "  y2 = yuyv.b;\n"+
              "  v  = yuyv.a;\n"+
              "  y = mix( y1, y2, mod(gl_FragCoord.x, 2) ); /* avoid branching! */\n"+
              "  y = 1.1643*(y-0.0625);\n"+
              "  u = u-0.5;\n"+
              "  v = v-0.5;\n"+
              "  r = y+1.5958*v;\n"+
              "  g = y-0.39173*u-0.81290*v;\n"+
              "  b = y+2.017*u;\n"+
              "  return vec4(r, g, b, 1);\n"+
              "}\n"
          ;
        default: // FIXME: Add more formats !
          throw new InternalError("Add proper mapping of: vPixelFmt "+vPixelFmt+", usesTexLookupShader "+usesTexLookupShader);
      }        
    }
    
    @Override
    public final boolean playImpl() {
        if(0==moviePtr) {
            return false;
        }
        final int errno = natives.play0(moviePtr);
        if( DEBUG_NATIVE && errno != 0 && errno != -ENOSYS) {
            System.err.println("libav play err: "+errno);
        }
        return true;
    }
    
    @Override
    public final boolean pauseImpl() {
        if(0==moviePtr) {
            return false;
        }
        final int errno = natives.pause0(moviePtr);
        if( DEBUG_NATIVE && errno != 0 && errno != -ENOSYS) {
            System.err.println("libav pause err: "+errno);
        }
        return true;
    }

    @Override
    protected final synchronized int seekImpl(int msec) {
        if(0==moviePtr) {
            throw new GLException("FFMPEG native instance null");
        }
        return natives.seek0(moviePtr, msec);
    }

    @Override
    protected void preNextTextureImpl(GL gl) {
        psm.setUnpackAlignment(gl, 1); // RGBA ? 4 : 1
        gl.glActiveTexture(GL.GL_TEXTURE0+getTextureUnit());
    }
    
    @Override
    protected void postNextTextureImpl(GL gl) {
        psm.restore(gl);
    }
    
    @Override
    protected final int getNextTextureImpl(GL gl, TextureFrame nextFrame) {
        if(0==moviePtr) {
            throw new GLException("FFMPEG native instance null");
        }
        int vPTS = TimeFrameI.INVALID_PTS;
        if( null != gl ) {
            final Texture tex = nextFrame.getTexture();
            tex.enable(gl);
            tex.bind(gl);
        }

        /** Try decode up to 10 packets to find one containing video. */
        for(int i=0; TimeFrameI.INVALID_PTS == vPTS && 10 > i; i++) {
           vPTS = natives.readNextPacket0(moviePtr, textureTarget, textureFormat, textureType);
        }
        if( null != nextFrame ) {
            nextFrame.setPTS(vPTS);
        }
        return vPTS;
    }    
    
    final void pushSound(ByteBuffer sampleData, int data_size, int audio_pts) {
        setFirstAudioPTS2SCR( audio_pts );
        if( 1.0f == playSpeed || audioSinkPlaySpeedSet ) {
            audioSink.enqueueData( audio_pts, sampleData, data_size);
        }
    }
    
}

