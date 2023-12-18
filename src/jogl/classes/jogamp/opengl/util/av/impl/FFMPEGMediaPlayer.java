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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.security.PrivilegedAction;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLException;
import com.jogamp.common.av.AudioFormat;
import com.jogamp.common.av.AudioSink;
import com.jogamp.common.av.AudioSinkFactory;
import com.jogamp.common.av.TimeFrameI;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.SecurityUtil;
import com.jogamp.common.util.VersionNumber;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.texture.Texture;

import jogamp.common.os.PlatformPropsImpl;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.util.av.AudioSampleFormat;
import jogamp.opengl.util.av.GLMediaPlayerImpl;
import jogamp.opengl.util.av.VideoPixelFormat;
import jogamp.opengl.util.av.impl.FFMPEGDynamicLibraryBundleInfo.VersionedLib;

/***
 * Implementation utilizes <a href="http://ffmpeg.org/">FFmpeg</a> which is ubiquitous
 * available and usually pre-installed on Unix platforms.
 * <p>
 * Besides the default BSD/Linux/.. repositories and installations,
 * precompiled binaries can be found at the
 * <a href="#ffmpegavail">listed location below</a>.
 * </p>
 *
 * <a name="implspecifics"><h5>Implementation specifics</h5></a>
 * <p>
 * The decoded video frame is written directly into an OpenGL texture
 * on the GPU in it's native format. A custom fragment shader converts
 * the native pixelformat to a usable <i>RGB</i> format if required.
 * Hence only 1 copy is required before bloating the picture
 * from <i>YUV*</i> to <i>RGB</i>, for example.
 * </p>
 * <p>
 * Implements pixel format conversion to <i>RGB</i> via
 * fragment shader texture-lookup functions:
 * <ul>
 *   <li>{@link VideoPixelFormat#YUV420P}</li>
 *   <li>{@link VideoPixelFormat#YUVJ420P}</li>
 *   <li>{@link VideoPixelFormat#YUV422P}</li>
 *   <li>{@link VideoPixelFormat#YUVJ422P}</li>
 *   <li>{@link VideoPixelFormat#YUYV422}</li>
 *   <li>{@link VideoPixelFormat#BGR24}</li>
 * </ul>
 * </p>
 * <p>
 *
 * <a name="ffmpegspecifics"><h5>FFmpeg Specifics</h5></a>
 * <p>
 * Utilizes a slim dynamic and native binding to the FFmpeg libraries:
 * <ul>
 *   <li>avcodec</li>
 *   <li>avformat</li>
 *   <li>avutil</li>
 *   <li>avdevice (optional for video input devices)</li>
 *   <li>swresample</li>
 * </ul>
 * </p>
 *
 * <a name="compatibility"><h5>FFmpeg Compatibility</h5></a>
 * <p>
 * Currently we are binary compatible with the following major versions:
 * <table border="1">
 * <tr><th>ffmpeg</th><th>avcodec</th><th>avformat</th><th>avdevice</th><th>avutil</th><th>swresample</th>  <th>FFMPEG* class</th></tr>
 * <tr><td>4</td>     <td>58</td>     <td>58</td>      <td>58</td>      <td>56</td>    <td>03</td>          <td>FFMPEGv0400</td></tr>
 * <tr><td>5</td>     <td>59</td>     <td>59</td>      <td>59</td>      <td>57</td>    <td>04</td>          <td>FFMPEGv0500</td></tr>
 * <tr><td>6</td>     <td>60</td>     <td>60</td>      <td>60</td>      <td>58</td>    <td>04</td>          <td>FFMPEGv0600</td></tr>
 * </table>
 * </p>
 * <p>
 * See FFmpeg:
 * <ul>
 *  <li>http://ffmpeg.org/documentation.html</li>
 *  <li>http://git.videolan.org/?p=ffmpeg.git;a=blob;f=doc/APIchanges;hb=HEAD</li>
 * </ul>
 * </p>
 * <p>
 * Check tag 'FIXME: Add more planar formats !'
 * here and in the corresponding native code
 * <code>jogl/src/jogl/native/libav/ffmpeg_impl_template.c</code>
 * </p>
 *
 *
 * <a name="todo"><h5>TODO:</h5></a>
 * <p>
 * <ul>
 *   <li>better audio synchronization handling? (video is synchronized)</li>
 * </ul>
 * </p>
 *
 * <a name="ffmpegavail"><h5>FFmpeg Availability</h5></a>
 * <p>
 * <ul>
 *   <li>GNU/Linux: ffmpeg is deployed in most distributions.</li>
 *   <li>Windows:
 *   <ul>
 *     <li>https://ffmpeg.org/download.html#build-windows</li>
 *     <li>http://ffmpeg.zeranoe.com/builds/ (ffmpeg) <i>recommended, works w/ dshow</i></li>
 *   </ul></li>
 *   <li>MacOSX
 *   <ul>
 *     <li>Building using Homebrew *
 *     <ul>
 *       <li>https://github.com/Homebrew/homebrew/wiki/Installation</li>
 *       <li>https://trac.ffmpeg.org/wiki/CompilationGuide/MacOSX<pre>
ruby -e "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)"
brew install ffmpeg
 *       </pre></li>
 *     </ul></li>
 *     <li>Builds
 *     <ul>
 *       <li>https://ffmpeg.org/download.html#build-mac</li>
 *     </ul></li>
 *   </ul></li>
 *   <li>OpenIndiana/Solaris:<pre>
pkg set-publisher -p http://pkg.openindiana.org/sfe-encumbered.
pkt install pkg:/video/ffmpeg
 *       </pre></li>
 * </ul>
 * </p>
 */
public class FFMPEGMediaPlayer extends GLMediaPlayerImpl {

    /**
     * Defaults to {@code true} for now.
     * However, in case we ship our own ffmpeg library this may change.
     * <p>
     * Property {@code jogl.ffmpeg.lib} set to {@code internal}
     * will set {@code PREFER_SYSTEM_LIBS} to {@code false}.
     * </p>
     * <p>
     * Non system internal libraries are named 'internal_<basename>',
     * e.g. 'internal_avutil'.
     * </p>
     * <p>
     * System default libraries are named '<basename>',
     * e.g. 'avutil'.
     * </p>
     * <p>
     * If {@code PREFER_SYSTEM_LIBS} is {@code true} (default),
     * we lookup the default library first,
     * then the versioned library names and last the internal library.
     * </p>
     * <p>
     * If {@code PREFER_SYSTEM_LIBS} is {@code false},
     * we lookup the internal library first,
     * then the versioned library names and last the default library.
     * </p>
     */
    /* pp */ static final boolean PREFER_SYSTEM_LIBS;

    /** POSIX ENOSYS {@value}: Function not implemented. FIXME: Move to GlueGen ?!*/
    private static final int ENOSYS = 38;

    // Instance data
    private static final FFMPEGNatives natives;
    private static final int avUtilMajorVersionCC;
    private static final int avFormatMajorVersionCC;
    private static final int avCodecMajorVersionCC;
    private static final int avDeviceMajorVersionCC;
    private static final int swResampleMajorVersionCC;
    private static final boolean available;

    static {
        // PREFER_SYSTEM_LIBS default on all systems is true for now!
        final String choice = PropertyAccess.getProperty("jogl.ffmpeg.lib", true);
        PREFER_SYSTEM_LIBS = null == choice || !choice.equals("internal");

        final boolean libAVGood = FFMPEGDynamicLibraryBundleInfo.initSingleton();
        final boolean libAVVersionGood;
        if( FFMPEGDynamicLibraryBundleInfo.libsLoaded() ) {
            natives = FFMPEGDynamicLibraryBundleInfo.getNatives();
            if( null != natives ) {
                avCodecMajorVersionCC = natives.getAvCodecMajorVersionCC0();
                avFormatMajorVersionCC = natives.getAvFormatMajorVersionCC0();
                avUtilMajorVersionCC = natives.getAvUtilMajorVersionCC0();
                avDeviceMajorVersionCC = natives.getAvDeviceMajorVersionCC0();
                swResampleMajorVersionCC = natives.getSwResampleMajorVersionCC0();
            } else {
                avUtilMajorVersionCC = 0;
                avFormatMajorVersionCC = 0;
                avCodecMajorVersionCC = 0;
                avDeviceMajorVersionCC = 0;
                swResampleMajorVersionCC = 0;
            }
            final VersionedLib avCodec = FFMPEGDynamicLibraryBundleInfo.avCodec;
            final VersionedLib avFormat = FFMPEGDynamicLibraryBundleInfo.avFormat;
            final VersionedLib avUtil = FFMPEGDynamicLibraryBundleInfo.avUtil;
            final VersionedLib avDevice = FFMPEGDynamicLibraryBundleInfo.avDevice;
            final VersionedLib swResample = FFMPEGDynamicLibraryBundleInfo.swResample;
            final boolean avDeviceLoaded = FFMPEGDynamicLibraryBundleInfo.avDeviceLoaded();
            final boolean swResampleLoaded = FFMPEGDynamicLibraryBundleInfo.swResampleLoaded();
            final int avCodecMajor = avCodec.version.getMajor();
            final int avFormatMajor = avFormat.version.getMajor();
            final int avUtilMajor = avUtil.version.getMajor();
            final int avDeviceMajor = avDevice.version.getMajor();
            final int swResampleMajor = swResample.version.getMajor();
            libAVVersionGood = avCodecMajorVersionCC  == avCodecMajor &&
                               avFormatMajorVersionCC == avFormatMajor &&
                               avUtilMajorVersionCC == avUtilMajor &&
                               ( avDeviceMajorVersionCC == avDeviceMajor || 0 == avDeviceMajor ) &&
                               swResampleMajorVersionCC == swResampleMajor;
            if( !libAVVersionGood ) {
                System.err.println("FFmpeg Not Matching Compile-Time / Runtime Major-Version");
            }
            if( !libAVVersionGood || DEBUG ) {
                printNativeInfoImpl(System.err);
            }
        } else {
            natives = null;
            avUtilMajorVersionCC = 0;
            avFormatMajorVersionCC = 0;
            avCodecMajorVersionCC = 0;
            avDeviceMajorVersionCC = 0;
            swResampleMajorVersionCC = 0;
            libAVVersionGood = false;
        }
        available = libAVGood && libAVVersionGood && null != natives;
    }

    public static final boolean isAvailable() { return available; }

    private static final void printNativeInfoImpl(final PrintStream out) {
        out.println("FFmpeg Codec   : "+FFMPEGDynamicLibraryBundleInfo.avCodec+" [cc "+avCodecMajorVersionCC+"]");
        out.println("FFmpeg Format  : "+FFMPEGDynamicLibraryBundleInfo.avFormat+" [cc "+avFormatMajorVersionCC+"]");
        out.println("FFmpeg Util    : "+FFMPEGDynamicLibraryBundleInfo.avUtil+" [cc "+avUtilMajorVersionCC+"]");
        out.println("FFmpeg Device  : "+FFMPEGDynamicLibraryBundleInfo.avDevice+" [cc "+avDeviceMajorVersionCC+", loaded "+FFMPEGDynamicLibraryBundleInfo.avDeviceLoaded()+"]");
        out.println("FFmpeg Resample: "+FFMPEGDynamicLibraryBundleInfo.swResample+" [cc "+swResampleMajorVersionCC+", loaded "+FFMPEGDynamicLibraryBundleInfo.swResampleLoaded()+"]");
        out.println("FFmpeg Class   : "+(null!= natives ? natives.getClass().getSimpleName() : "n/a"));
    }
    @Override
    public final void printNativeInfo(final PrintStream out) {
        printNativeInfoImpl(out);
    }

    //
    // General
    //

    private long moviePtr = 0;

    //
    // Video
    //

    private VideoPixelFormat vPixelFmt = VideoPixelFormat.RGBA;
    private int vPlanes = 0;
    private int vBitsPerPixel = 0;
    private int vBytesPerPixelPerPlane = 0;
    private int vWidth = 0, vHeight = 0;
    private int texWidth = 0, texHeight = 0; // overall (stuffing planes in one texture)
    private String singleTexComp = "r";
    private final GLPixelStorageModes psm;

    //
    // Audio
    //

    private AudioFormat avChosenAudioFormat;
    private int audioSamplesPerFrameAndChannel = 0;

    public FFMPEGMediaPlayer() {
        if(!available) {
            throw new RuntimeException("FFMPEGMediaPlayer not available");
        }
        psm = new GLPixelStorageModes();
        initSelf();
    }
    private void initSelf() {
        moviePtr = natives.createInstance0(this, DEBUG_NATIVE);
        if(0==moviePtr) {
            throw new GLException("Couldn't create FFMPEGInstance");
        }
        audioSink = null;
    }

    @Override
    protected final void destroyImpl() {
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

    @Override
    protected void stopImpl() {
        destroyImpl();
        initSelf();
    }

    public static final String dev_video_linux = "/dev/video";

    @Override
    protected final void initStreamImpl(final int vid, final int aid) throws IOException {
        if(0==moviePtr) {
            throw new GLException("FFMPEG native instance null");
        }
        if(DEBUG) {
            System.err.println("initStream: p1 "+this);
        }

        final String streamLocS = IOUtil.getUriFilePathOrASCII(getUri());
        destroyAudioSink();
        if( GLMediaPlayer.STREAM_ID_NONE == aid ) {
            audioSink = AudioSinkFactory.createNull();
        } else {
            // audioSink = new jogamp.common.av.JavaSoundAudioSink();
            audioSink = AudioSinkFactory.createDefault(FFMPEGMediaPlayer.class.getClassLoader());
        }
        {
            final int audioChannelLimit = getAudioChannelLimit();
            if( audioChannelLimit >= 1 ) {
                audioSink.setChannelLimit(audioChannelLimit);
            }
        }
        final AudioFormat preferredAudioFormat = audioSink.getPreferredFormat();
        if(DEBUG) {
            System.err.println("initStream: p2 aid "+aid+", preferred "+preferredAudioFormat+" on "+audioSink+", "+this);
        }

        final boolean isCameraInput = null != cameraPath;
        final String resStreamLocS;
        // int rw=640, rh=480, rr=15;
        int rw=-1, rh=-1, rr=-1;
        String sizes = null;
        if( isCameraInput ) {
            switch(PlatformPropsImpl.OS_TYPE) {
                case ANDROID:
                    // ??
                case FREEBSD:
                case HPUX:
                case LINUX:
                case SUNOS:
                    resStreamLocS = dev_video_linux + cameraPath.decode();
                    break;
                case WINDOWS:
                case MACOS:
                case OPENKODE:
                default:
                    resStreamLocS = cameraPath.decode();
                    break;
            }
            if( null != cameraProps ) {
                sizes = cameraProps.get(CameraPropSizeS);
                int v = getPropIntVal(cameraProps, CameraPropWidth);
                if( v > 0 ) { rw = v; }
                v = getPropIntVal(cameraProps, CameraPropHeight);
                if( v > 0 ) { rh = v; }
                v = getPropIntVal(cameraProps, CameraPropRate);
                if( v > 0 ) { rr = v; }
            }
        } else {
            resStreamLocS = streamLocS;
        }
        final int aMaxChannelCount = preferredAudioFormat.channelCount;
        final int aPrefSampleRate = preferredAudioFormat.sampleRate;
         // setStream(..) issues updateAttributes*(..), and defines avChosenAudioFormat, vid, aid, .. etc
        if(DEBUG) {
            System.err.println("initStream: p3 cameraPath "+cameraPath+", isCameraInput "+isCameraInput);
            System.err.println("initStream: p3 stream "+getUri()+" -> "+streamLocS+" -> "+resStreamLocS);
            System.err.println("initStream: p3 vid "+vid+", sizes "+sizes+", reqVideo "+rw+"x"+rh+"@"+rr+", aid "+aid+", aMaxChannelCount "+aMaxChannelCount+", aPrefSampleRate "+aPrefSampleRate);
        }
        natives.setStream0(moviePtr, resStreamLocS, isCameraInput, vid, sizes, rw, rh, rr, aid, aMaxChannelCount, aPrefSampleRate);
    }

    @Override
    protected final void initGLImpl(final GL gl) throws IOException, GLException {
        if(0==moviePtr) {
            throw new GLException("FFMPEG native instance null");
        }
        final int audioQueueSize;
        if( null != gl && STREAM_ID_NONE != getVID() ) {
            final GLContextImpl ctx = (GLContextImpl)gl.getContext();
            SecurityUtil.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    final ProcAddressTable pt = ctx.getGLProcAddressTable();
                    final long procAddrGLTexSubImage2D = pt.getAddressFor("glTexSubImage2D");
                    final long procAddrGLGetError = pt.getAddressFor("glGetError");
                    final long procAddrGLFlush = pt.getAddressFor("glFlush");
                    final long procAddrGLFinish = pt.getAddressFor("glFinish");
                    natives.setGLFuncs0(moviePtr, procAddrGLTexSubImage2D, procAddrGLGetError, procAddrGLFlush, procAddrGLFinish);
                    return null;
            } } );
            audioQueueSize = AudioSink.DefaultQueueSizeWithVideo;
        } else {
            audioQueueSize = AudioSink.DefaultQueueSize;
        }
        if(DEBUG) {
            System.err.println("initGL: p3 aid "+getAID()+", avChosen "+avChosenAudioFormat+" on "+audioSink);
        }

        if( STREAM_ID_NONE == getAID() || null == audioSink ) {
            if(null != audioSink) {
                audioSink.destroy();
            }
            audioSink = AudioSinkFactory.createNull();
            audioSink.init(AudioSink.DefaultFormat, 0, audioQueueSize);
        } else {
            final int frameDuration;
            if( audioSamplesPerFrameAndChannel > 0 ) {
                frameDuration = Math.round( 1000f * avChosenAudioFormat.getSamplesDuration(audioSamplesPerFrameAndChannel) );
            } else {
                frameDuration = AudioSink.DefaultFrameDuration;
            }
            final boolean audioSinkOK = audioSink.init(avChosenAudioFormat, frameDuration, audioQueueSize);
            if( !audioSinkOK ) {
                System.err.println("AudioSink "+audioSink.getClass().getName()+" does not support "+avChosenAudioFormat+", using Null");
                audioSink.destroy();
                audioSink = AudioSinkFactory.createNull();
                audioSink.init(avChosenAudioFormat, 0, audioQueueSize);
            }
        }
        if(DEBUG) {
            System.err.println("initGL: p4 chosen "+avChosenAudioFormat);
            System.err.println("initGL: p4 chosen aid "+getAID()+", "+audioSink);
        }

        if( null != gl && STREAM_ID_NONE != getVID() ) {
            int tf, tif=GL.GL_RGBA; // texture format and internal format
            final int tt = GL.GL_UNSIGNED_BYTE;
            final int texBytesPerPixel;
            switch(vBytesPerPixelPerPlane) {
                case 1: if( gl.isGL3ES3() ) {
                            // RED is supported on ES3 and >= GL3 [core]; ALPHA is deprecated on core
                            tf = GL2ES2.GL_RED;   tif=GL2ES2.GL_RED; singleTexComp = "r";
                        } else {
                            // ALPHA is supported on ES2 and GL2, i.e. <= GL3 [core] or compatibility
                            tf = GL.GL_ALPHA; tif=GL.GL_ALPHA; singleTexComp = "a";
                        }
                        texBytesPerPixel = 1;
                        break;

                case 2: if( vPixelFmt == VideoPixelFormat.YUYV422 || vPixelFmt == VideoPixelFormat.UYVY422 ) {
                            // YUYV422: // < packed YUV 4:2:2, 2x 16bpp, Y0 Cb Y1 Cr
                            // UYVY422: // < packed YUV 4:2:2, 2x 16bpp, Cb Y0 Cr Y1
                            // Both stuffed into RGBA half width texture
                            tf = GL.GL_RGBA; tif=GL.GL_RGBA;
                            texBytesPerPixel = 4;
                        } else {
                            tf = GL2ES2.GL_RG;   tif=GL2ES2.GL_RG;
                            texBytesPerPixel = 2;
                        }
                        break;

                case 3: tf = GL.GL_RGB;   tif=GL.GL_RGB; texBytesPerPixel = 3; break;

                case 4: if( vPixelFmt == VideoPixelFormat.BGRA ) {
                            tf = GL.GL_BGRA;  tif=GL.GL_RGBA;
                        } else {
                            tf = GL.GL_RGBA;  tif=GL.GL_RGBA;
                        }
                        texBytesPerPixel = 4;
                        break;

                default: throw new RuntimeException("Unsupported bytes-per-pixel / plane "+vBytesPerPixelPerPlane);
            }
            setTextureFormat(tif, tf);
            setTextureType(tt);
            setIsGLOriented(false);
            if( DEBUG ) {
                final float texSizeMB = ( texWidth * texHeight * (float)texBytesPerPixel ) / 1000000f;
                System.err.printf("initGL: p5: video %s, bpp %d, %d x %d, %d b/pp, %d planes, tex %d x %d x %d -> %.2fMB%n",
                        vPixelFmt.toString(), vBitsPerPixel, vWidth, vHeight, vBytesPerPixelPerPlane, vPlanes,
                        texWidth, texHeight, texBytesPerPixel, texSizeMB);
            }
        }
    }
    @Override
    protected final TextureFrame createTexImage(final GL gl, final int texName) {
        return new TextureFrame( createTexImageImpl(gl, texName, texWidth, texHeight) );
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
     * Converts the given ffmpeg values to {@link AudioFormat} and returns {@link AudioSink#isSupported(AudioFormat)}.
     * @param audioSampleFmt ffmpeg audio-sample-format, see {@link AudioSampleFormat}.
     * @param audioSampleRate sample rate in Hz (1/s)
     * @param audioChannels number of channels
     */
    final boolean isAudioFormatSupported(final int audioSampleFmt, final int audioSampleRate, final int audioChannels) {
        final AudioSampleFormat avFmt = AudioSampleFormat.valueOf(audioSampleFmt);
        final AudioFormat audioFormat = avAudioFormat2Local(avFmt, audioSampleRate, audioChannels);
        final boolean res = audioSink.isSupported(audioFormat);
        if( DEBUG ) {
            System.err.println("AudioSink.isSupported: "+res+": av[fmt "+avFmt+", rate "+audioSampleRate+", chan "+audioChannels+"] -> "+audioFormat);
        }
        return res;
    }

    /**
     * Returns {@link AudioFormat} as converted from the given ffmpeg values.
     * @param audioSampleFmt ffmpeg audio-sample-format, see {@link AudioSampleFormat}.
     * @param audioSampleRate sample rate in Hz (1/s)
     * @param audioChannels number of channels
     */
    private final AudioFormat avAudioFormat2Local(final AudioSampleFormat audioSampleFmt, final int audioSampleRate, final int audioChannels) {
        final int sampleSize;
        boolean planar = true;
        boolean fixedP = true;
        final boolean signed;
        switch( audioSampleFmt ) {
            case S32:
                planar = false;
            case S32P:
                sampleSize = 32;
                signed = true;
                break;
            case S16:
                planar = false;
            case S16P:
                sampleSize = 16;
                signed = true;
                break;
            case U8:
                planar = false;
            case U8P:
                sampleSize = 8;
                signed = false;
                break;
            case DBL:
                planar = false;
            case DBLP:
                sampleSize = 64;
                signed = true;
                fixedP = false;
                break;
            case FLT:
                planar = false;
            case FLTP:
                sampleSize = 32;
                signed = true;
                fixedP = false;
                break;
            default: // FIXME: Add more formats !
                throw new IllegalArgumentException("Unsupported sampleformat: "+audioSampleFmt);
        }
        return new AudioFormat(audioSampleRate, sampleSize, audioChannels, signed, fixedP, planar, true /* littleEndian */);
    }

    /**
     * Native callback
     * @param vid
     * @param pixFmt
     * @param planes
     * @param bitsPerPixel
     * @param bytesPerPixelPerPlane
     * @param tWd0
     * @param tWd1
     * @param tWd2
     * @param aid
     * @param audioSampleFmt
     * @param audioSampleRate
     * @param audioChannels
     * @param audioSamplesPerFrameAndChannel in audio samples per frame and channel
     */
    void setupFFAttributes(final int vid, final int pixFmt, final int planes, final int bitsPerPixel, final int bytesPerPixelPerPlane,
                          final int tWd0, final int tWd1, final int tWd2, final int vW, final int vH,
                          final int aid, final int audioSampleFmt, final int audioSampleRate,
                          final int audioChannels, final int audioSamplesPerFrameAndChannel) {
        // defaults ..
        vPixelFmt = VideoPixelFormat.RGBA;
        vPlanes = 0;
        vBitsPerPixel = 0;
        vBytesPerPixelPerPlane = 0;
        vWidth = 0; vHeight = 0;
        texWidth = 0; texHeight = 0;

        final int[] vTexWidth = { 0, 0, 0 }; // per plane

        if( STREAM_ID_NONE != vid ) {
            vPixelFmt = VideoPixelFormat.valueOf(pixFmt);
            vPlanes = planes;
            vBitsPerPixel = bitsPerPixel;
            vBytesPerPixelPerPlane = bytesPerPixelPerPlane;
            vWidth = vW; vHeight = vH;
            vTexWidth[0] = tWd0; vTexWidth[1] = tWd1; vTexWidth[2] = tWd2;

            switch(vPixelFmt) {
                case YUVJ420P:
                case YUV420P: // < planar YUV 4:2:0, 12bpp, (1 Cr & Cb sample per 2x2 Y samples)
                    // YUV420P: Adding U+V on right side of fixed height texture,
                    //          since width is already aligned by decoder.
                    //          Splitting texture to 4 quadrants:
                    //            Y covers left top/low quadrant
                    //            U on top-right quadrant.
                    //            V on low-right quadrant.
                    // Y=w*h, U=w/2*h/2, V=w/2*h/2
                    //   w*h + 2 ( w/2 * h/2 )
                    //   w*h + w*h/2
                    texWidth = vTexWidth[0] + vTexWidth[1]; texHeight = vH;
                    break;
                case YUVJ422P:
                case YUV422P:
                    // YUV422P: Adding U+V on right side of fixed height texture,
                    //          since width is already aligned by decoder.
                    //          Splitting texture to 4 columns
                    //            Y covers columns 1+2
                    //            U covers columns 3
                    //            V covers columns 4
                    texWidth = vTexWidth[0] + vTexWidth[1] + vTexWidth[2]; texHeight = vH;
                    break;
                case YUYV422: // < packed YUV 4:2:2, 2x 16bpp, Y0 Cb Y1 Cr - stuffed into RGBA half width texture
                case UYVY422: // < packed YUV 4:2:2, 2x 16bpp, Cb Y0 Cr Y1 - stuffed into RGBA half width texture
                case BGR24:
                    texWidth = vTexWidth[0]; texHeight = vH;
                    break;

                case RGB24:
                case ARGB:
                case RGBA:
                case ABGR:
                case BGRA:
                    texWidth = vTexWidth[0]; texHeight = vH;
                    break;
                default: // FIXME: Add more formats !
                    throw new RuntimeException("Unsupported pixelformat: "+vPixelFmt);
            }
        }

        // defaults ..
        final AudioSampleFormat aSampleFmt;
        avChosenAudioFormat = null;;
        this.audioSamplesPerFrameAndChannel = 0;

        if( STREAM_ID_NONE != aid ) {
            aSampleFmt = AudioSampleFormat.valueOf(audioSampleFmt);
            avChosenAudioFormat = avAudioFormat2Local(aSampleFmt, audioSampleRate, audioChannels);
            this.audioSamplesPerFrameAndChannel = audioSamplesPerFrameAndChannel;
        } else {
            aSampleFmt = null;
        }

        if(DEBUG) {
            System.err.println("audio: id "+aid+", fmt "+aSampleFmt+", "+avChosenAudioFormat+", aFrameSize/fc "+audioSamplesPerFrameAndChannel);
            System.err.printf("video: id %d, %s, bpp %d, %d x %d, %d b/pp, %d planes, tex %d x %d%n",
                    vid, vPixelFmt.toString(), vBitsPerPixel, vWidth, vHeight, vBytesPerPixelPerPlane, vPlanes, texWidth, texHeight);
            for(int i=0; i<3; i++) {
                System.err.println("video: p["+i+"]: "+vTexWidth[i]);
            }
            System.err.println("video: total tex "+texWidth+"x"+texHeight);
            System.err.println(this.toString());
        }
    }

    /**
     * Native callback
     * @param isInGLOrientation
     * @param pixFmt
     * @param planes
     * @param bitsPerPixel
     * @param bytesPerPixelPerPlane
     * @param tWd0
     * @param tWd1
     * @param tWd2
     */
    void updateVidAttributes(final boolean isInGLOrientation, final int pixFmt, final int planes, final int bitsPerPixel, final int bytesPerPixelPerPlane,
                             final int tWd0, final int tWd1, final int tWd2, final int vW, final int vH) {
    }

    /**
     * {@inheritDoc}
     *
     * If this implementation generates a specialized shader,
     * it allows the user to override the default function name <code>ffmpegTexture2D</code>.
     * Otherwise the call is delegated to it's super class.
     */
    @Override
    public String setTextureLookupFunctionName(final String texLookupFuncName) throws IllegalStateException {
        if(null != texLookupFuncName && texLookupFuncName.length()>0) {
            if( texLookupFuncName.equals("texture2D") ) {
                throw new IllegalArgumentException("Build in 'texture2D' lookup-func-name not allowed!");
            }
            textureLookupFunctionName = texLookupFuncName;
        } else {
            textureLookupFunctionName = "ffmpegTexture2D";
        }
        return textureLookupFunctionName;
    }

    /**
     * {@inheritDoc}
     *
     * Depending on the pixelformat, a specific conversion shader is being created,
     * e.g. YUV420P to RGB. Otherwise the call is delegated to it's super class.
     */
    @Override
    public final String getTextureLookupFragmentShaderImpl() {
      final float tc_w_1 = (float)getWidth() / (float)texWidth;
      final String texLookupFuncName = getTextureLookupFunctionName();
      if( texLookupFuncName.equals("texture2D") ) {
          throw new IllegalArgumentException("Build in 'texture2D' lookup-func-name not allowed!");
      }
      switch(vPixelFmt) {
        case YUVJ420P:
        case YUV420P: // < planar YUV 4:2:0, 12bpp, (1 Cr & Cb sample per 2x2 Y samples)
          return
              "// YUV420P: planar YUV 4:2:0, 12bpp, (1 Cr & Cb sample per 2x2 Y samples)\n"+
              "vec4 "+texLookupFuncName+"(in "+getTextureSampler2DType()+" image, in vec2 texCoord) {\n"+
              "  const vec2 u_off = vec2("+tc_w_1+", 0.0);\n"+
              "  const vec2 v_off = vec2("+tc_w_1+", 0.5);\n"+
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

        case YUVJ422P:
        case YUV422P: // < planar YUV 4:2:2, 16bpp, (1 Cr & Cb sample per 2x1 Y samples)
          return
              "// YUV422P: planar YUV 4:2:2, 16bpp, (1 Cr & Cb sample per 2x1 Y samples)\n"+
              "vec4 "+texLookupFuncName+"(in "+getTextureSampler2DType()+" image, in vec2 texCoord) {\n"+
              "  const vec2 u_off = vec2("+tc_w_1+"      , 0.0);\n"+
              "  const vec2 v_off = vec2("+tc_w_1+" * 1.5, 0.0);\n"+
              "  vec2 tc_halfw = vec2(texCoord.x*0.5, texCoord.y);\n"+
              "  float y,u,v,r,g,b;\n"+
              "  y = texture2D(image, texCoord)."+singleTexComp+";\n"+
              "  u = texture2D(image, u_off+tc_halfw)."+singleTexComp+";\n"+
              "  v = texture2D(image, v_off+tc_halfw)."+singleTexComp+";\n"+
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
              "// YUYV422: packed YUV 4:2:2, 2 x 16bpp, [Y0 Cb] [Y1 Cr]\n"+
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
        case UYVY422: // < packed YUV 4:2:2, 2 x 16bpp, Cb Y0 Cr Y1
                      // Stuffed into RGBA half width texture
          return
              "// UYVY422: packed YUV 4:2:2, 2 x 16bpp, Cb Y0 Cr Y1\n"+
              "vec4 "+texLookupFuncName+"(in "+getTextureSampler2DType()+" image, in vec2 texCoord) {\n"+
              "  "+
              "  float y1,u,y2,v,y,r,g,b;\n"+
              "  vec2 tc_halfw = vec2(texCoord.x*0.5, texCoord.y);\n"+
              "  vec4 uyvy = texture2D(image, tc_halfw).rgba;\n"+
              "  u  = uyvy.r;\n"+
              "  y1 = uyvy.g;\n"+
              "  v  = uyvy.b;\n"+
              "  y2 = uyvy.a;\n"+
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

        case BGR24:
          return
              "// BGR24\n"+
              "vec4 "+texLookupFuncName+"(in "+getTextureSampler2DType()+" image, in vec2 texCoord) {\n"+
              "  "+
              "  vec3 bgr = texture2D(image, texCoord).rgb;\n"+
              "  return vec4(bgr.b, bgr.g, bgr.r, 1);\n"+ /* just swizzle */
              "}\n"
          ;
        case RGB24:
          return
              "// RGB24\n"+
              "vec4 "+texLookupFuncName+"(in "+getTextureSampler2DType()+" image, in vec2 texCoord) {\n"+
              "  "+
              "  vec3 t = texture2D(image, texCoord).rgb;\n"+
              "  return vec4(t.rgb, 1);\n"+
              "}\n"
          ;
        case ARGB:
          return
              "// ARGB\n"+
              "vec4 "+texLookupFuncName+"(in "+getTextureSampler2DType()+" image, in vec2 texCoord) {\n"+
              "  "+
              "  vec4 t = texture2D(image, texCoord);\n"+
              "  return vec4(t.g, t.b, t.a, t.r);\n"+ /* just swizzle */
              "}\n"
          ;
        case RGBA:
          return
              "// RGBA\n"+
              "vec4 "+texLookupFuncName+"(in "+getTextureSampler2DType()+" image, in vec2 texCoord) {\n"+
              "  "+
              "  return texture2D(image, texCoord);\n"+
              "}\n"
          ;
        case ABGR:
          return
              "// ABGR\n"+
              "vec4 "+texLookupFuncName+"(in "+getTextureSampler2DType()+" image, in vec2 texCoord) {\n"+
              "  "+
              "  vec4 t = texture2D(image, texCoord);\n"+
              "  return vec4(t.a, t.b, t.g, t.r);\n"+ /* just swizzle */
              "}\n"
          ;
        case BGRA:
          return
              "// BGR24\n"+
              "vec4 "+texLookupFuncName+"(in "+getTextureSampler2DType()+" image, in vec2 texCoord) {\n"+
              "  "+
              "  vec3 bgr = texture2D(image, texCoord).rgb;\n"+
              "  return vec4(bgr.b, bgr.g, bgr.r, 1);\n"+ /* just swizzle */
              "}\n"
          ;

        default: // FIXME: Add more formats !
          throw new InternalError("Add proper mapping of: vPixelFmt "+vPixelFmt);
      }
    }

    @Override
    public final boolean resumeImpl() {
        if(0==moviePtr) {
            return false;
        }
        final int errno = natives.play0(moviePtr);
        if( DEBUG_NATIVE && errno != 0 && errno != -ENOSYS) {
            System.err.println("ffmpeg play err: "+errno);
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
            System.err.println("ffmpeg pause err: "+errno);
        }
        return true;
    }

    @Override
    protected final synchronized int seekImpl(final int msec) {
        if(0==moviePtr) {
            throw new GLException("FFMPEG native instance null");
        }
        return natives.seek0(moviePtr, msec);
    }

    @Override
    protected void preNextTextureImpl(final GL gl) {
        psm.setUnpackAlignment(gl, 1); // RGBA ? 4 : 1
        gl.glActiveTexture(GL.GL_TEXTURE0+getTextureUnit());
    }

    @Override
    protected void postNextTextureImpl(final GL gl) {
        psm.restore(gl);
    }

    @Override
    protected final int getNextTextureImpl(final GL gl, final TextureFrame nextFrame) {
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
           vPTS = natives.readNextPacket0(moviePtr, getTextureTarget(), getTextureFormat(), getTextureType());
        }
        if( null != nextFrame ) {
            nextFrame.setPTS(vPTS);
        }
        return vPTS;
    }

    final void pushSound(final ByteBuffer sampleData, final int data_size, final int audio_pts) {
        if( audioStreamEnabled() ) {
            audioSink.enqueueData( audio_pts, sampleData, data_size);
        }
    }

}

