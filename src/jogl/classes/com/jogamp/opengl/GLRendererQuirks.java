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
package com.jogamp.opengl;

import java.util.IdentityHashMap;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.opengl.GLCapabilitiesImmutable;

import com.jogamp.common.os.Platform;
import com.jogamp.opengl.egl.EGL;
import com.jogamp.opengl.egl.EGLExt;

/**
 * GLRendererQuirks contains information of known bugs of various GL renderer.
 * This information allows us to workaround them.
 * <p>
 * Using centralized quirk identifier enables us to
 * locate code dealing w/ it and hence eases it's maintenance.
 * </p>
 * <p>
 * <i>Some</i> <code>GL_VENDOR</code> and <code>GL_RENDERER</code> strings are
 * listed here <http://feedback.wildfiregames.com/report/opengl/feature/GL_VENDOR>.
 * </p>
 */
public class GLRendererQuirks {
    /**
     * Crashes XServer when using double buffered PBuffer with GL_RENDERER:
     * <ul>
     *  <li>Mesa DRI Intel(R) Sandybridge Desktop</li>
     *  <li>Mesa DRI Intel(R) Ivybridge Mobile - 3.0 Mesa 8.0.4</li>
     *  <li>Gallium 0.4 on AMD CYPRESS</li>
     * </ul>
     * For now, it is safe to disable it w/ hw-acceleration.
     */
    public static final int NoDoubleBufferedPBuffer = 0;

    /** On Windows no double buffered bitmaps are guaranteed to be available. */
    public static final int NoDoubleBufferedBitmap  = 1;

    /** Crashes application when trying to set EGL swap interval on Android 4.0.3 / Pandaboard ES / PowerVR SGX 540 */
    public static final int NoSetSwapInterval       = 2;

    /** No offscreen bitmap available, currently true for JOGL's OSX implementation. */
    public static final int NoOffscreenBitmap       = 3;

    /** SIGSEGV on setSwapInterval() after changing the context's drawable w/ 'Mesa 8.0.4' dri2SetSwapInterval/DRI2 (soft & intel) */
    public static final int NoSetSwapIntervalPostRetarget = 4;

    /**
     * GLSL <code>discard</code> command leads to undefined behavior or won't get compiled if being used.
     * <p>
     * Appears to <i>have</i> happened on Nvidia Tegra2, but seems to be fine now.<br/>
     * FIXME: Constrain version.
     * </p>
     */
    public static final int GLSLBuggyDiscard = 5;

    /**
     * Non compliant GL context due to a buggy implementation not suitable for use.
     * <p>
     * Currently, Mesa >= 9.1.3 (may extend back as far as 9.0) OpenGL 3.1 compatibility
     * context is not compliant. Most programs will give completely broken output (or no
     * output at all. For now, this context is not trusted.
     * </p>
     * The above has been confirmed for the following Mesa 9.* GL_RENDERER strings:
     * <ul>
     *   <li>Mesa .* Intel(R) Sandybridge Desktop</li>
     *   <li>Gallium 0.4 on AMD RS880</li>
     * </ul>
     * </p>
     * <p>
     * It still has to be verified whether the AMD OpenGL 3.1 core driver is compliant enought.
     */
    public static final int GLNonCompliant = 6;

    /**
     * The OpenGL context needs a <code>glFlush()</code> before releasing it, otherwise driver may freeze:
     * <ul>
     *   <li>OSX < 10.7.3 - NVidia Driver. Bug 533 and Bug 548 @ https://jogamp.org/bugzilla/.</li>
     * </ul>
     */
    public static final int GLFlushBeforeRelease = 7;

    /**
     * Closing X11 displays may cause JVM crashes or X11 errors with some buggy drivers
     * while being used in concert w/ OpenGL.
     * <p>
     * Some drivers may require X11 displays to be closed in the same order as they were created,
     * some may not allow them to be closed at all while resources are being used somehow.
     * </p>
     * <p>
     * Drivers known exposing such bug:
     * <ul>
     *   <li>Mesa &lt; 8.0 _with_ X11 software renderer <code>Mesa X11</code>, not with GLX/DRI renderer.</li>
     *   <li>ATI proprietary Catalyst X11 driver versions:
     *     <ul>
     *       <li>8.78.6</li>
     *       <li>8.881</li>
     *       <li>8.911</li>
     *       <li>9.01.8</li>
     *     </ul></li>
     * </ul>
     * </p>
     * <p>
     * See Bug 515 - https://jogamp.org/bugzilla/show_bug.cgi?id=515
     * and {@link jogamp.nativewindow.x11.X11Util#ATI_HAS_XCLOSEDISPLAY_BUG}.
     * </p>
     * <p>
     * See Bug 705 - https://jogamp.org/bugzilla/show_bug.cgi?id=705
     * </p>
     */
    public static final int DontCloseX11Display = 8;

    /**
     * Need current GL context when calling new ARB <i>pixel format query</i> functions,
     * otherwise driver crashes the VM.
     * <p>
     * Drivers known exposing such bug:
     * <ul>
     *   <li>ATI proprietary Catalyst driver on Windows version &le; XP.
     *       TODO: Validate if bug actually relates to 'old' ATI Windows drivers for old GPU's like X300
     *             regardless of the Windows version.</li>
     * </ul>
     * <p>
     * See Bug 480 - https://jogamp.org/bugzilla/show_bug.cgi?id=480
     * </p>
     */
    public static final int NeedCurrCtx4ARBPixFmtQueries = 9;

    /**
     * Need current GL context when calling new ARB <i>CreateContext</i> function,
     * otherwise driver crashes the VM.
     * <p>
     * Drivers known exposing such bug:
     * <ul>
     *   <li>ATI proprietary Catalyst Windows driver on laptops with a driver version as reported in <i>GL_VERSION</i>:
     *     <ul>
     *       <li> <i>null</i> </li>
     *       <li> &lt; <code>12.102.3.0</code> ( <i>amd_catalyst_13.5_mobility_beta2</i> ) </li>
     *     </ul></li>
     * </ul>
     * </p>
     * <p>
     * See Bug 706 - https://jogamp.org/bugzilla/show_bug.cgi?id=706<br/>
     * See Bug 520 - https://jogamp.org/bugzilla/show_bug.cgi?id=520
     * </p>
     */
    public static final int NeedCurrCtx4ARBCreateContext = 10;

    /**
     * No full FBO support, i.e. not compliant w/
     * <ul>
     *   <li>GL_ARB_framebuffer_object</li>
     *   <li>EXT_framebuffer_object</li>
     *   <li>EXT_framebuffer_multisample</li>
     *   <li>EXT_framebuffer_blit</li>
     *   <li>EXT_packed_depth_stencil</li>
     * </ul>.
     * Drivers known exposing such bug:
     * <ul>
     *   <li>Mesa <i>7.12-devel</i> on Windows with VMware <i>SVGA3D</i> renderer:
     *     <ul>
     *       <li>GL_VERSION:  <i>2.1 Mesa 7.12-devel (git-d6c318e)</i> </li>
     *       <li>GL_RENDERER: <i>Gallium 0.4 on SVGA3D; build: RELEASE;</i> </li>
     *     </ul></li>
     * </ul>
     * <p>
     * Also enabled via {@link #BuggyColorRenderbuffer}.
     * </p>
     * <p>
     * Quirk can also be enabled via property: <code>jogl.fbo.force.min</code>.
     * </p>
     */
    public static final int NoFullFBOSupport = 11;

    /**
     * GLSL is not compliant or even not stable (crash)
     * <ul>
     *   <li>OSX < 10.7.0 (?) - NVidia Driver. Bug 818 @ https://jogamp.org/bugzilla/.</li>
     * </ul>
     */
    public static final int GLSLNonCompliant = 12;

    /**
     * GL4 context needs to be requested via GL3 profile attribute
     * <ul>
     *   <li>OSX >= 10.9.0 - kCGLOGLPVersion_GL4_Core may not produce hw-accel context. Bug 867 @ https://jogamp.org/bugzilla/.</li>
     * </ul>
     */
    public static final int GL4NeedsGL3Request = 13;

    /**
     * Buggy shared OpenGL context support within a multithreaded use-case, not suitable for stable usage.
     * <p>
     * <i>X11 Mesa DRI Intel(R) driver >= 9.2.1</i> cannot handle multithreaded shared GLContext usage
     * with non-blocking exclusive X11 display connections.
     * References:
     * <ul>
     *    <li>Bug 873: https://jogamp.org/bugzilla/show_bug.cgi?id=873</li>
     *    <li>https://bugs.freedesktop.org/show_bug.cgi?id=41736#c8</li>
     * </ul>
     * <p>
     * However, not all multithreaded use-cases are broken, e.g. our GLMediaPlayer does work.
     * </p>
     * The above has been confirmed for the following Mesa 9.* strings:
     * <ul>
     *    <li>GL_VENDOR      Intel Open Source Technology Center</li>
     *    <li>GL_RENDERER    Mesa DRI Intel(R) Sandybridge Desktop</li>
     *    <li>GL_RENDERER    Mesa DRI Intel(R) Ivybridge Mobile</li>
     *    <li>GL_VERSION     3.1 (Core Profile) Mesa 9.2.1</li>
     * </ul>
     * </p>
     * <p>
     * On Android 4.*, <i>Huawei's Ascend G615 w/ Immersion.16</i> could not make a shared context
     * current, which uses a pbuffer drawable:
     * <ul>
     *    <li>Android 4.*</li>
     *    <li>GL_VENDOR      Hisilicon Technologies</li>
     *    <li>GL_RENDERER    Immersion.16</li>
     *    <li>GL_VERSION     OpenGL ES 2.0</li>
     * </ul>
     * </p>
     * <p>
     * </p>
     */
    public static final int GLSharedContextBuggy = 14;

    /**
     * Bug 925 - Accept an ES3 Context, if reported via GL-Version-String w/o {@link EGLExt#EGL_OPENGL_ES3_BIT_KHR}.
     * <p>
     * The ES3 Context can be used via {@link EGL#EGL_OPENGL_ES2_BIT}.
     * </p>
     * <p>
     * The ES3 Context {@link EGL#eglCreateContext(long, long, long, java.nio.IntBuffer) must be created} with version attributes:
     * <pre>
     *  EGL.EGL_CONTEXT_CLIENT_VERSION, 2, ..
     * </pre>
     * </p>
     * <ul>
     *   <li>Mesa/AMD >= 9.2.1</li>
     *   <li>Some Android ES3 drivers ..</li>
     * </ul>
     */
    public static final int GLES3ViaEGLES2Config = 15;

    /**
     * Bug 948 - NVIDIA 331.38 (Linux X11) EGL impl. only supports _one_ EGL Device via {@link EGL#eglGetDisplay(long)}.
     * <p>
     * Subsequent calls to {@link EGL#eglGetDisplay(long)} fail.
     * </p>
     * <p>
     * Reusing global EGL display works.
     * </p>
     * <p>
     * The quirk is autodetected within EGLDrawableFactory's initial default device setup!
     * </p>
     * <p>
     * Appears on:
     * <ul>
     *   <li>EGL_VENDOR      NVIDIA</li>
     *   <li>EGL_VERSION     1.4</li>
     *   <li>GL_VENDOR       NVIDIA Corporation</li>
     *   <li>GL_VERSION      OpenGL ES 3.0 331.38 (probably w/ 1st NV EGL lib on x86)</li>
     *   <li>GL_VERSION      OpenGL ES 3.1 NVIDIA 355.06 (unstable)</li>
     *   <li>Platform        X11</li>
     *   <li>CPU Family      {@link Platform.CPUFamily#X86}</li>
     * </ul>
     * </p>
     */
    public static final int SingletonEGLDisplayOnly = 16;

    /**
     * No reliable MSAA / FSAA {@link GLCapabilitiesImmutable#getSampleBuffers() multi}
     * {@link GLCapabilitiesImmutable#getNumSamples() sampling} available,
     * i.e. driver <i>may crash</i>.
     * <p>
     * Appears on:
     * <ul>
     *   <li>GL_VENDOR       nouveau</li>
     *   <li>GL_RENDERER     Gallium 0.4 on NV34</li>
     * </ul>
     * TODO: We have to determine the exact version range, i.e. not adding the quirk with fixed driver version!
     * </p>
     * TODO: Since we currently don't handle this quirk internally, a user may need to do the following:
     * <pre>
     * final AbstractGraphicsDevice adevice = GLDrawableFactory.getDesktopFactory(); // or similar
     * if( GLRendererQuirks.existStickyDeviceQuirk(adevice, GLRendererQuirks.NoMultiSamplingBuffers) ) {
     *    // don't use MSAA
     * }
     * </pre>
     */
    public static final int NoMultiSamplingBuffers  = 17;

    /**
     * Buggy FBO color renderbuffer target,
     * i.e. driver <i>may crash</i>.
     * <p>
     * Appears on:
     * <ul>
     *   <li>GL_VENDOR       Brian Paul</li>
     *   <li>GL_RENDERER     Mesa X11</li>
     *   <li>GL_VERSION      2.1 Mesa 7.2</li>
     * </ul>
     * TODO: We have to determine the exact version range, i.e. not adding the quirk with fixed driver version!
     * </p>
     * <p>
     * Note: Also enables {@link #NoFullFBOSupport}.
     * </p>
     * <p>
     * Note: GLFBODrawable always uses texture attachments if set.
     * </p>
     * <p>
     * Quirk can also be enabled via property: <code>jogl.fbo.force.nocolorrenderbuffer</code>.
     * </p>
     */
    public static final int BuggyColorRenderbuffer  = 18;

    /**
     * No pbuffer supporting accumulation buffers available,
     * even if driver claims otherwise.
     * <p>
     * Some drivers wrongly claim to support pbuffers
     * with accumulation buffers. However, the creation of such pbuffer fails:
     * <pre>
     *   com.jogamp.opengl.GLException: pbuffer creation error: Couldn't find a suitable pixel format
     * </pre>
     * </p>
     * <p>
     * Appears on:
     * <ul>
     *   <li>GL_VENDOR       Intel</li>
     *   <li>GL_RENDERER     Intel Bear Lake B</li>
     *   <li>GL_VERSION      1.4.0 - Build 8.14.10.1930</li>
     *   <li>Platform        Windows</li>
     * </ul>
     * </p>
     */
    public static final int NoPBufferWithAccum = 19;

    /**
     * Need GL objects (VBO, ..) to be synchronized when utilized
     * concurrently from multiple threads via a shared GL context,
     * otherwise driver crashes the VM.
     * <p>
     * Usually synchronization should not be required, if the shared GL objects
     * are created and immutable before concurrent usage.<br>
     * However, using drivers exposing this issue always require the user to
     * synchronize access of shared GL objects.
     * </p>
     * <p>
     * Synchronization can be avoided if accessing the shared GL objects
     * exclusively via a queue or {@link com.jogamp.common.util.Ringbuffer Ringbuffer}, see GLMediaPlayerImpl as an example.
     * </p>
     * <p>
     * Appears on:
     * <ul>
     *   <li>Platform        OSX
     *     <ul>
     *       <li>detected on OSX 10.9.5 first</li>
     *       <li>any driver</li>
     *       <li>enabled for all OSX versions</li>
     *     </ul>
     *   </li>
     * </ul>
     * </p>
     * <p>
     * See Bug 1088 - https://jogamp.org/bugzilla/show_bug.cgi?id=1088
     * </p>
     */
    public static final int NeedSharedObjectSync = 20;

    /**
     * No reliable ARB_create_context implementation,
     * even if driver claims otherwise.
     * <p>
     * Some drivers wrongly claim to support ARB_create_context.
     * However, the creation of such context fails:
     * <pre>
     *   com.jogamp.opengl.GLException: AWT-EventQueue-0: WindowsWGLContex.createContextImpl ctx !ARB, profile > GL2
     *   requested (OpenGL >= 3.0.1). Requested: GLProfile[GL3bc/GL3bc.hw], current: 2.1 (Compat profile, FBO, hardware)
     *   - 2.1.8787
     * </pre>
     * </p>
     * <p>
     * Appears on:
     * <ul>
     *   <li>GL_VENDOR       ATI Technologies Inc.</li>
     *   <li>GL_RENDERER     ATI Radeon 3100 Graphics</li>
     *   <li>GL_VERSION      2.1.8787</li>
     *   <li>Platform        Windows</li>
     * </ul>
     * </p>
     */
    public static final int NoARBCreateContext = 21;

    /**
     * No support for ES or desktop GL >= 3.0 current context without surface,
     * i.e. without a default framebuffer as read- and write drawables.
     * <p>
     * See <i>OpenGL spec 3.0, chapter 2.1 OpenGL Fundamentals, page 7</i> or<br>
     * <i>OpenGL ES spec 3.0.2, chapter 2.1 OpenGL Fundamentals, page 6</i>:
     * <pre>
     * It is possible to use a GL context without a default framebuffer, in which case
     * a framebuffer object must be used to perform all rendering. This is useful for
     * applications neeting to perform offscreen rendering.
     * </pre>
     * </p>
     * <p>
     * The feature will be attempted at initialization and this quirk will be set if failing.
     * </p>
     * <p>
     * Known drivers failing the specification:
     * <ul>
     *   <li>GNU/Linux X11 Nvidia proprietary driver
     *   <ul>
     *     <li>GL_VERSION      4.4.0 NVIDIA 340.24</li>
     *     <li>Platform        GNU/Linux X11</li>
     *   </ul></li>
     * </ul>
     * </p>
     */
    public static final int NoSurfacelessCtx = 22;

    /** Return the number of known quirks. */
    public static final int getCount() { return 23; }

    private static final String[] _names = new String[] { "NoDoubleBufferedPBuffer", "NoDoubleBufferedBitmap", "NoSetSwapInterval",
                                                          "NoOffscreenBitmap", "NoSetSwapIntervalPostRetarget", "GLSLBuggyDiscard",
                                                          "GLNonCompliant", "GLFlushBeforeRelease", "DontCloseX11Display",
                                                          "NeedCurrCtx4ARBPixFmtQueries", "NeedCurrCtx4ARBCreateContext",
                                                          "NoFullFBOSupport", "GLSLNonCompliant", "GL4NeedsGL3Request",
                                                          "GLSharedContextBuggy", "GLES3ViaEGLES2Config", "SingletonEGLDisplayOnly",
                                                          "NoMultiSamplingBuffers", "BuggyColorRenderbuffer", "NoPBufferWithAccum",
                                                          "NeedSharedObjectSync", "NoARBCreateContext", "NoSurfacelessCtx"
                                                        };

    private static final IdentityHashMap<String, GLRendererQuirks> stickyDeviceQuirks = new IdentityHashMap<String, GLRendererQuirks>();

    /**
     * Retrieval of sticky {@link AbstractGraphicsDevice}'s {@link GLRendererQuirks}.
     * <p>
     * The {@link AbstractGraphicsDevice}s are mapped via their {@link AbstractGraphicsDevice#getUniqueID()}.
     * </p>
     * <p>
     * Not thread safe.
     * </p>
     * @see #areSameStickyDevice(AbstractGraphicsDevice, AbstractGraphicsDevice)
     */
    public static GLRendererQuirks getStickyDeviceQuirks(final AbstractGraphicsDevice device) {
        final String key = device.getUniqueID();
        final GLRendererQuirks has = stickyDeviceQuirks.get(key);
        final GLRendererQuirks res;
        if( null == has ) {
            res = new GLRendererQuirks();
            stickyDeviceQuirks.put(key, res);
        } else {
            res = has;
        }
        return res;
    }

    /**
     * Returns true if both devices have the same {@link AbstractGraphicsDevice#getUniqueID()},
     * otherwise false.
     */
    public static boolean areSameStickyDevice(final AbstractGraphicsDevice device1, final AbstractGraphicsDevice device2) {
        return device1.getUniqueID() == device2.getUniqueID(); // uses .intern()!
    }

    /**
     * {@link #addQuirk(int) Adding given quirk} of sticky {@link AbstractGraphicsDevice}'s {@link GLRendererQuirks}.
     * <p>
     * Not thread safe.
     * </p>
     * @see #getStickyDeviceQuirks(AbstractGraphicsDevice)
     */
    public static void addStickyDeviceQuirk(final AbstractGraphicsDevice device, final int quirk) throws IllegalArgumentException {
        final GLRendererQuirks sq = getStickyDeviceQuirks(device);
        sq.addQuirk(quirk);
    }
    /**
     * {@link #addQuirks(int[], int, int) Adding given quirks} of sticky {@link AbstractGraphicsDevice}'s {@link GLRendererQuirks}.
     * <p>
     * Not thread safe.
     * </p>
     * @see #getStickyDeviceQuirks(AbstractGraphicsDevice)
     */
    public static void addStickyDeviceQuirks(final AbstractGraphicsDevice device, final int[] quirks, final int offset, final int len) throws IllegalArgumentException {
        final GLRendererQuirks sq = getStickyDeviceQuirks(device);
        sq.addQuirks(quirks, offset, len);
    }
    /**
     * {@link #addQuirks(GLRendererQuirks) Adding given quirks} of sticky {@link AbstractGraphicsDevice}'s {@link GLRendererQuirks}.
     * <p>
     * Not thread safe.
     * </p>
     * @see #getStickyDeviceQuirks(AbstractGraphicsDevice)
     */
    public static void addStickyDeviceQuirks(final AbstractGraphicsDevice device, final GLRendererQuirks quirks) throws IllegalArgumentException {
        final GLRendererQuirks sq = getStickyDeviceQuirks(device);
        sq.addQuirks(quirks);
    }
    /**
     * {@link #exist(int) Query} of sticky {@link AbstractGraphicsDevice}'s {@link GLRendererQuirks}.
     * <p>
     * Not thread safe. However, use after changing the sticky quirks is safe.
     * </p>
     * @see #getStickyDeviceQuirks(AbstractGraphicsDevice)
     */
    public static boolean existStickyDeviceQuirk(final AbstractGraphicsDevice device, final int quirk) {
        return getStickyDeviceQuirks(device).exist(quirk);
    }
    /**
     * {@link #addQuirks(GLRendererQuirks) Pushing} the sticky {@link AbstractGraphicsDevice}'s {@link GLRendererQuirks}
     * to the given {@link GLRendererQuirks destination}.
     * <p>
     * Not thread safe. However, use after changing the sticky quirks is safe.
     * </p>
     * @see #getStickyDeviceQuirks(AbstractGraphicsDevice)
     */
    public static void pushStickyDeviceQuirks(final AbstractGraphicsDevice device, final GLRendererQuirks dest) {
        dest.addQuirks(getStickyDeviceQuirks(device));
    }

    private int _bitmask;

    public GLRendererQuirks() {
        _bitmask = 0;
    }

    /**
     * @param quirks an array of valid quirks
     * @param offset offset in quirks array to start reading
     * @param len number of quirks to read from offset within quirks array
     * @throws IllegalArgumentException if one of the quirks is out of range
     */
    public GLRendererQuirks(final int[] quirks, final int offset, final int len) throws IllegalArgumentException {
        this();
        addQuirks(quirks, offset, len);
    }

    /**
     * @param quirk valid quirk to be added
     * @throws IllegalArgumentException if the quirk is out of range
     */
    public final void addQuirk(final int quirk) throws IllegalArgumentException {
        validateQuirk(quirk);
        _bitmask |= 1 << quirk;
    }

    /**
     * @param quirks an array of valid quirks to be added
     * @param offset offset in quirks array to start reading
     * @param len number of quirks to read from offset within quirks array
     * @throws IllegalArgumentException if one of the quirks is out of range
     */
    public final void addQuirks(final int[] quirks, final int offset, final int len) throws IllegalArgumentException {
        int bitmask = 0;
        if( !( 0 <= offset + len && offset + len <= quirks.length ) ) {
            throw new IllegalArgumentException("offset and len out of bounds: offset "+offset+", len "+len+", array-len "+quirks.length);
        }
        for(int i=offset; i<offset+len; i++) {
            final int quirk = quirks[i];
            validateQuirk(quirk);
            bitmask |= 1 << quirk;
        }
        _bitmask |= bitmask;
    }

    /**
     * @param quirks valid GLRendererQuirks to be added
     */
    public final void addQuirks(final GLRendererQuirks quirks) {
        _bitmask |= quirks._bitmask;
    }

    /**
     * @param quirk the quirk to be tested
     * @return true if quirk exist, otherwise false
     * @throws IllegalArgumentException if quirk is out of range
     */
    public final boolean exist(final int quirk) throws IllegalArgumentException {
        validateQuirk(quirk);
        return 0 != ( ( 1 << quirk )  & _bitmask );
    }

    public final StringBuilder toString(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        sb.append("[");
        boolean first=true;
        for(int i=0; i<getCount(); i++) {
            final int testmask = 1 << i;
            if( 0 != ( _bitmask & testmask ) ) {
                if(!first) { sb.append(", "); }
                sb.append(toString(i));
                first=false;
            }
        }
        sb.append("]");
        return sb;
    }

    @Override
    public final String toString() {
        return toString(null).toString();
    }

    /**
     * @param quirk the quirk to be validated, i.e. whether it is out of range
     * @throws IllegalArgumentException if quirk is out of range
     */
    public static void validateQuirk(final int quirk) throws IllegalArgumentException {
        if( !( 0 <= quirk && quirk < getCount() ) ) {
            throw new IllegalArgumentException("Quirks must be in range [0.."+getCount()+"[, but quirk: "+quirk);
        }
    }

    /**
     * @param quirk the quirk to be converted to String
     * @return the String equivalent of this quirk
     * @throws IllegalArgumentException if quirk is out of range
     */
    public static final String toString(final int quirk) throws IllegalArgumentException {
        validateQuirk(quirk);
        return _names[quirk];
    }
}
