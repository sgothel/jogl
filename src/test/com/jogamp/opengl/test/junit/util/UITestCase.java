/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLEventListener;

import com.jogamp.junit.util.SingletonJunitCase;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class UITestCase extends SingletonJunitCase {
    private static volatile boolean resetXRandRIfX11AfterClass = false;

    private static volatile int maxMethodNameLen = 0;

    public static void setResetXRandRIfX11AfterClass() {
        resetXRandRIfX11AfterClass = true;
    }

    /**
     * Iterates through all outputs and sets the preferred mode and normal rotation using RandR 1.3.
     * <p>
     * With NV drivers, one need to add the Modes in proper order to the Screen's Subsection "Display",
     * otherwise they are either in unsorted resolution order or even n/a!
     * </p>
     * @return error-code with {@code zero} for no error
     */
    @SuppressWarnings("unused")
    public static int resetXRandRIfX11() {
        int errorCode = 0;
        if( NativeWindowFactory.isInitialized() && NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(true) ) {
            try {
                final List<String> outputDevices = new ArrayList<String>();
                // final List<String> outputSizes = new ArrayList<String>();
                final StringBuilder out = new StringBuilder();
                final String[] cmdlineQuery = new String[] { "xrandr", "-q" };
                errorCode = processCommand(cmdlineQuery, null, out, "xrandr-query> ");
                if( 0 != errorCode ) {
                    System.err.println("XRandR Query Error Code "+errorCode);
                    System.err.println(out.toString());
                } else {
                    // Parse connected output devices !
                    final BufferedReader in = new BufferedReader( new StringReader( out.toString() ) );
                    String line = null;
                    while ( ( line = in.readLine() ) != null) {
                        final String lline = line.toLowerCase();
                        if( lline.contains("connected") && !lline.contains("disconnected") ) {
                            final String od = getFirst(line);
                            if( null != od ) {
                                outputDevices.add( od );
                                /**
                                if ( ( line = in.readLine() ) != null ) {
                                    outputSizes.add( getFirst(line) );
                                } else {
                                    outputSizes.add( null );
                                } */
                            }
                        }
                    }
                    for(int i=0; i<outputDevices.size(); i++) {
                        final String outputDevice = outputDevices.get(i);
                        final String outputSize = null; // outputSizes.get(i)
                        final String[] cmdline;
                        if( null != outputSize ) {
                            cmdline = new String[] { "xrandr", "--output", outputDevice, "--mode", outputSize, "--rotate", "normal" };
                        } else {
                            cmdline = new String[] { "xrandr", "--output", outputDevice, "--preferred", "--rotate", "normal" };
                        }
                        System.err.println("XRandR 1.2 Reset: "+Arrays.asList(cmdline));
                        errorCode = processCommand(cmdline, System.err, null, "xrandr-1.2-reset> ");
                        if( 0 != errorCode ) {
                            System.err.println("XRandR 1.2 Reset Error Code "+errorCode);
                            break;
                        }
                    }
                    /**
                     * RandR 1.1 reset does not work ..
                    if( 0 != errorCode ) {
                        final String[] cmdline = new String[] { "xrandr", "-s", "0", "-o", "normal" };
                        System.err.println("XRandR 1.1 Reset: "+Arrays.asList(cmdline));
                        errorCode = processCommand(cmdline, System.err, null, "xrandr-1.1-reset> ");
                        if( 0 != errorCode ) {
                            System.err.println("XRandR 1.1 Reset Error Code "+errorCode);
                        }
                    } */
                }
            } catch (final Exception e) {
                System.err.println("Caught "+e.getClass().getName()+": "+e.getMessage());
                e.printStackTrace();
                errorCode = -1;
            }
        }
        return errorCode;
    }
    private static String getFirst(final String line) {
        final StringTokenizer tok = new StringTokenizer(line);
        if( tok.hasMoreTokens() ) {
            final String s = tok.nextToken().trim();
            if( s.length() > 0 ) {
                return s;
            }
        }
        return null;
    }

    public static int processCommand(final String[] cmdline, final OutputStream outstream, final StringBuilder outstring, final String outPrefix) {
        int errorCode = 0;
        final Object ioSync = new Object();
        try {
            synchronized ( ioSync ) {
                final ProcessBuilder pb = new ProcessBuilder(cmdline);
                pb.redirectErrorStream(true);
                final Process p = pb.start();
                final MiscUtils.StreamDump dump;
                if( null != outstream ) {
                    dump = new MiscUtils.StreamDump( outstream, outPrefix, p.getInputStream(), ioSync);
                } else if( null != outstring ) {
                    dump = new MiscUtils.StreamDump( outstring, outPrefix, p.getInputStream(), ioSync);
                } else {
                    throw new IllegalArgumentException("Output stream and string are null");
                }
                dump.start();
                while( !dump.eos() ) {
                    ioSync.wait();
                }
                p.waitFor(); // should be fine by now ..
                errorCode = p.exitValue();
            }
        } catch (final Exception e) {
            System.err.println("Caught "+e.getClass().getName()+": "+e.getMessage());
            e.printStackTrace();
            errorCode = Integer.MIN_VALUE;
        }
        return errorCode;
    }

    public int getMaxTestNameLen() {
        if(0 == maxMethodNameLen) {
            int ml = 0;
            final TestClass tc = new TestClass(getClass());
            final List<FrameworkMethod> testMethods = tc.getAnnotatedMethods(org.junit.Test.class);
            for(final Iterator<FrameworkMethod> iter=testMethods.iterator(); iter.hasNext(); ) {
                final int l = iter.next().getName().length();
                if( ml < l ) { ml = l; }
            }
            maxMethodNameLen = ml;
        }
        return maxMethodNameLen;
    }

    @BeforeClass
    public static final void oneTimeSetUpUITest() {
        // one-time initialization code
    }

    @AfterClass
    public static final void oneTimeTearDownUITest() {
        // one-time cleanup code
        if( resetXRandRIfX11AfterClass ) {
            resetXRandRIfX11();
        }
    }

    public String getSnapshotFilename(final int sn, String postSNDetail, final GLCapabilitiesImmutable caps, final int width, final int height, final boolean sinkHasAlpha, String fileSuffix, final String destPath) {
        if(null == fileSuffix) {
            fileSuffix = TextureIO.PNG;
        }
        final int maxSimpleTestNameLen = getMaxTestNameLen()+getClass().getSimpleName().length()+1;
        final String simpleTestName = this.getSimpleTestName(".");
        final String filenameBaseName;
        {
            final String accel = caps.getHardwareAccelerated() ? "hw" : "sw" ;
            final String scrnm;
            if(caps.isOnscreen()) {
                scrnm = "onscreen";
            } else if(caps.isFBO()) {
                scrnm = "fbobject";
            } else if(caps.isPBuffer()) {
                scrnm = "pbuffer_";
            } else if(caps.isBitmap()) {
                scrnm = "bitmap__";
            } else {
                scrnm = "unknown_";
            }
            final String dblb = caps.getDoubleBuffered() ? "dbl" : "one";
            final String F_pfmt = sinkHasAlpha ? "rgba" : "rgb_";
            final String pfmt = "rgba" + caps.getRedBits() + caps.getGreenBits() + caps.getBlueBits() + caps.getAlphaBits();
            final int depthBits = caps.getDepthBits();
            final int stencilBits = caps.getStencilBits();
            final int samples = caps.getNumSamples() ;
            final String aaext = caps.getSampleExtension();
            postSNDetail = null != postSNDetail ? "-"+postSNDetail : "";

            filenameBaseName = String.format("%-"+maxSimpleTestNameLen+"s-n%04d%s-%-6s-%s-%s-B%s-F%s_I%s-D%02d-St%02d-Sa%02d_%s-%04dx%04d.%s",
                    simpleTestName, sn, postSNDetail, caps.getGLProfile().getName(), accel,
                    scrnm, dblb, F_pfmt, pfmt, depthBits, stencilBits, samples, aaext,
                    width, height, fileSuffix).replace(' ', '_');
        }
        return null != destPath ? destPath + File.separator + filenameBaseName : filenameBaseName;
    }

    /**
     * Takes a snapshot of the drawable's current front framebuffer. Example filenames:
     * <pre>
     * TestGLDrawableAutoDelegateOnOffscrnCapsNEWT.testES2OffScreenFBOSglBuf____-n0001-msaa0-GLES2_-sw-fbobject-Bdbl-Frgb__Irgba8888_-D24-St00-Sa00_default-0400x0300.png
     * TestGLDrawableAutoDelegateOnOffscrnCapsNEWT.testES2OffScreenPbufferDblBuf-n0003-msaa0-GLES2_-sw-pbuffer_-Bdbl-Frgb__Irgba8880-D24-St00-Sa00_default-0200x0150.png
     * TestGLDrawableAutoDelegateOnOffscrnCapsNEWT.testGL2OffScreenPbufferSglBuf-n0003-msaa0-GL2___-hw-pbuffer_-Bone-Frgb__Irgba5551-D24-St00-Sa00_default-0200x0150.png
     * </pre>
     * @param sn sequential number
     * @param postSNDetail optional detail to be added to the filename after <code>sn</code>
     * @param gl the current GL context object. It's read drawable is being used as the pixel source and to gather some details which will end up in the filename.
     * @param readBufferUtil the {@link GLReadBufferUtil} to be used to read the pixels for the screenshot.
     * @param fileSuffix Optional file suffix without a <i>dot</i> defining the file type, i.e. <code>"png"</code>.
     *                   If <code>null</code> the <code>"png"</code> as defined in {@link TextureIO#PNG} is being used.
     * @param destPath Optional platform dependent file path. It shall use {@link File#separatorChar} as is directory separator.
     *                 It shall not end with a directory separator, {@link File#separatorChar}.
     *                 If <code>null</code> the current working directory is being used.
     */
    public void snapshot(final int sn, final String postSNDetail, final GL gl, final GLReadBufferUtil readBufferUtil, final String fileSuffix, final String destPath) {

        final GLDrawable drawable = gl.getContext().getGLReadDrawable();
        final String filename = getSnapshotFilename(sn, postSNDetail,
                                                    drawable.getChosenGLCapabilities(), drawable.getSurfaceWidth(), drawable.getSurfaceHeight(),
                                                    readBufferUtil.hasAlpha(), fileSuffix, destPath);
        System.err.println(Thread.currentThread().getName()+": ** screenshot: "+filename);
        gl.glFinish(); // just make sure rendering finished ..
        try {
            snapshot(gl, readBufferUtil, filename);
        } catch (final ClassNotFoundException cnfe) {
            // Texture class belongs to jogl-util.jar which my not be included in test environment!
            System.err.println("Caught ClassNotFoundException: "+cnfe.getMessage());
        } catch (final NoClassDefFoundError cnfe) {
            // Texture class belongs to jogl-util.jar which my not be included in test environment!
            System.err.println("Caught NoClassDefFoundError: "+cnfe.getMessage());
        }
    }
    private void snapshot(final GL gl, final GLReadBufferUtil readBufferUtil, final String filename) throws ClassNotFoundException, NoClassDefFoundError {
        if(readBufferUtil.readPixels(gl, false)) {
            readBufferUtil.write(new File(filename));
        }
    }

    public class SnapshotGLEventListener implements GLEventListener {
        private final GLReadBufferUtil screenshot;
        private volatile boolean makeShot = false;
        private volatile boolean makeShotAlways = false;
        private volatile boolean verbose = false;
        private final AtomicInteger displayCount = new AtomicInteger(0);
        private final AtomicInteger reshapeCount = new AtomicInteger(0);
        private volatile String postSNDetail = null;
        public SnapshotGLEventListener(final GLReadBufferUtil screenshot) {
            this.screenshot = screenshot;
        }
        public SnapshotGLEventListener() {
            this.screenshot = new GLReadBufferUtil(false, false);
        }
        public int getDisplayCount() { return displayCount.get(); }
        public int getReshapeCount() { return reshapeCount.get(); }
        public GLReadBufferUtil getGLReadBufferUtil() { return screenshot; }
        public void init(final GLAutoDrawable drawable) {}
        public void dispose(final GLAutoDrawable drawable) {}
        public void display(final GLAutoDrawable drawable) {
            final GL gl = drawable.getGL();
            final boolean _makeShot = makeShot || makeShotAlways;
            if(verbose) {
                System.err.println(Thread.currentThread().getName()+": ** display: "+displayCount+": "+drawable.getSurfaceWidth()+"x"+drawable.getSurfaceHeight()+", makeShot "+_makeShot);
            }
            if(_makeShot) {
                makeShot=false;
                snapshot(displayCount.get(), postSNDetail, gl, screenshot, TextureIO.PNG, null);
            }
            displayCount.incrementAndGet();
        }
        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
            if(verbose) {
                System.err.println(Thread.currentThread().getName()+": ** reshape: "+reshapeCount+": "+width+"x"+height+" - "+drawable.getSurfaceWidth()+"x"+drawable.getSurfaceHeight());
            }
            reshapeCount.incrementAndGet();
        }
        public void setMakeSnapshot() {
            makeShot=true;
        }
        public void setMakeSnapshotAlways(final boolean v) {
            makeShotAlways=v;
        }
        public void setVerbose(final boolean v) {
            verbose=v;
        }
        public void setPostSNDetail(final String v) {
            postSNDetail = v;
        }
    };

}

