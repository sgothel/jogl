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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLEventListener;

import com.jogamp.common.util.locks.SingletonInstance;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.junit.runners.MethodSorters;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class UITestCase {
    @Rule public TestName _unitTestName = new TestName();

    public static final String SINGLE_INSTANCE_LOCK_FILE = "UITestCase.lock";
    public static final int SINGLE_INSTANCE_LOCK_PORT = 59999;

    public static final long SINGLE_INSTANCE_LOCK_TO   = 6*60*1000; // wait up to 6 mins
    public static final long SINGLE_INSTANCE_LOCK_POLL =      1000; // poll every 1s

    private static volatile SingletonInstance singletonInstance;

    private static volatile boolean testSupported = true;
    private static volatile boolean resetXRandRIfX11AfterClass = false;

    private static volatile int maxMethodNameLen = 0;

    private static final synchronized void initSingletonInstance() {
        if( null == singletonInstance )  {
            // singletonInstance = SingletonInstance.createFileLock(SINGLE_INSTANCE_LOCK_POLL, SINGLE_INSTANCE_LOCK_FILE);
            singletonInstance = SingletonInstance.createServerSocket(SINGLE_INSTANCE_LOCK_POLL, SINGLE_INSTANCE_LOCK_PORT);
            if(!singletonInstance.tryLock(SINGLE_INSTANCE_LOCK_TO)) {
                throw new RuntimeException("Fatal: Could not lock single instance: "+singletonInstance.getName());
            }
        }
    }

    public static boolean isTestSupported() {
        return testSupported;
    }

    public static void setTestSupported(boolean v) {
        System.err.println("setTestSupported: "+v);
        testSupported = v;
    }

    public static void setResetXRandRIfX11AfterClass() {
        resetXRandRIfX11AfterClass = true;
    }

    /**
     * Iterates through all outputs and sets the preferred mode and normal rotation using RandR 1.3.
     * <p>
     * With NV drivers, one need to add the Modes in proper order to the Screen's Subsection "Display",
     * otherwise they are either in unsorted resolution order or even n/a!
     * </p>
     */
    @SuppressWarnings("unused")
    public static void resetXRandRIfX11() {
        if( NativeWindowFactory.isInitialized() && NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(true) ) {
            try {
                final List<String> outputDevices = new ArrayList<String>();
                // final List<String> outputSizes = new ArrayList<String>();
                final Object ioSync = new Object();
                synchronized ( ioSync ) {
                    final StringBuilder out = new StringBuilder();
                    final ProcessBuilder pb = new ProcessBuilder("xrandr", "-q");
                    pb.redirectErrorStream(true);
                    System.err.println("XRandR Query: "+pb.command());
                    final Process p = pb.start();
                    final MiscUtils.StreamDump dump = new MiscUtils.StreamDump( out, p.getInputStream(), ioSync );
                    dump.start();
                    while( !dump.eos() ) {
                        ioSync.wait();
                    }
                    p.waitFor(); // should be fine by now ..
                    final int errorCode = p.exitValue();
                    if( 0 == errorCode ) {
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
                    } else {
                        System.err.println("XRandR Query Error Code "+errorCode);
                        System.err.println(out.toString());
                    }
                }
                for(int i=0; i<outputDevices.size(); i++) {
                    final String outputDevice = outputDevices.get(i);
                    final String outputSize = null; // outputSizes.get(i);
                    final String[] cmdline;
                    if( null != outputSize ) {
                        cmdline = new String[] { "xrandr", "--output", outputDevice, "--mode", outputSize, "--rotate", "normal" };
                    } else {
                        cmdline = new String[] { "xrandr", "--output", outputDevice, "--preferred", "--rotate", "normal" };
                    }
                    System.err.println("XRandR Reset: "+Arrays.asList(cmdline));
                    final int errorCode = processCommand(cmdline, System.err, "xrandr-reset> ");
                    if( 0 != errorCode ) {
                        System.err.println("XRandR Reset Error Code "+errorCode);
                    }
                }
            } catch (Exception e) {
                System.err.println("Catched "+e.getClass().getName()+": "+e.getMessage());
                e.printStackTrace();
            }
        }
    }
    private static String getFirst(String line) {
        final StringTokenizer tok = new StringTokenizer(line);
        if( tok.hasMoreTokens() ) {
            final String s = tok.nextToken().trim();
            if( s.length() > 0 ) {
                return s;
            }
        }
        return null;
    }

    public static int processCommand(String[] cmdline, OutputStream outstream, String outPrefix) {
        int errorCode = 0;
        final Object ioSync = new Object();
        try {
            synchronized ( ioSync ) {
                final ProcessBuilder pb = new ProcessBuilder(cmdline);
                pb.redirectErrorStream(true);
                final Process p = pb.start();
                final MiscUtils.StreamDump dump = new MiscUtils.StreamDump( outstream, outPrefix, p.getInputStream(), ioSync);
                dump.start();
                while( !dump.eos() ) {
                    ioSync.wait();
                }
                p.waitFor(); // should be fine by now ..
                errorCode = p.exitValue();
            }
        } catch (Exception e) {
            System.err.println("Catched "+e.getClass().getName()+": "+e.getMessage());
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
            for(Iterator<FrameworkMethod> iter=testMethods.iterator(); iter.hasNext(); ) {
                final int l = iter.next().getName().length();
                if( ml < l ) { ml = l; }
            }
            maxMethodNameLen = ml;
        }
        return maxMethodNameLen;
    }

    public final String getTestMethodName() {
        return _unitTestName.getMethodName();
    }

    public final String getSimpleTestName(String separator) {
        return getClass().getSimpleName()+separator+getTestMethodName();
    }

    public final String getFullTestName(String separator) {
        return getClass().getName()+separator+getTestMethodName();
    }

    @BeforeClass
    public static void oneTimeSetUp() {
        // one-time initialization code
        initSingletonInstance();
    }

    @AfterClass
    public static void oneTimeTearDown() {
        // one-time cleanup code
        if( resetXRandRIfX11AfterClass ) {
            resetXRandRIfX11();
        }
        System.gc(); // force cleanup
        singletonInstance.unlock();
    }

    @Before
    public void setUp() {
        System.err.print("++++ UITestCase.setUp: "+getFullTestName(" - "));
        if(!testSupported) {
            System.err.println(" - "+unsupportedTestMsg);
            Assume.assumeTrue(testSupported); // abort
        }
        System.err.println();
    }

    @After
    public void tearDown() {
        System.err.println("++++ UITestCase.tearDown: "+getFullTestName(" - "));
    }

    public static void waitForKey(String preMessage) {
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println(preMessage+"> Press enter to continue");
        try {
            System.err.println(stdin.readLine());
        } catch (IOException e) { }
    }

    static final String unsupportedTestMsg = "Test not supported on this platform.";

    public String getSnapshotFilename(int sn, String postSNDetail, GLCapabilitiesImmutable caps, int width, int height, boolean sinkHasAlpha, String fileSuffix, String destPath) {
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
    public void snapshot(int sn, String postSNDetail, GL gl, GLReadBufferUtil readBufferUtil, String fileSuffix, String destPath) {
        final GLDrawable drawable = gl.getContext().getGLReadDrawable();
        final String filename = getSnapshotFilename(sn, postSNDetail,
                                                    drawable.getChosenGLCapabilities(), drawable.getSurfaceWidth(), drawable.getSurfaceHeight(),
                                                    readBufferUtil.hasAlpha(), fileSuffix, destPath);
        System.err.println(Thread.currentThread().getName()+": ** screenshot: "+filename);
        gl.glFinish(); // just make sure rendering finished ..
        if(readBufferUtil.readPixels(gl, false)) {
            readBufferUtil.write(new File(filename));
        }
    }

    public class SnapshotGLEventListener implements GLEventListener {
        private final GLReadBufferUtil screenshot;
        private volatile boolean makeShot = false;
        private volatile boolean makeShotAlways = false;
        private volatile boolean verbose = false;
        private volatile int displayCount=0;
        private volatile int reshapeCount=0;
        private volatile String postSNDetail = null;
        public SnapshotGLEventListener(GLReadBufferUtil screenshot) {
            this.screenshot = screenshot;
        }
        public SnapshotGLEventListener() {
            this.screenshot = new GLReadBufferUtil(false, false);
        }
        public int getDisplayCount() { return displayCount; }
        public int getReshapeCount() { return reshapeCount; }
        public GLReadBufferUtil getGLReadBufferUtil() { return screenshot; }
        public void init(GLAutoDrawable drawable) {}
        public void dispose(GLAutoDrawable drawable) {}
        public void display(GLAutoDrawable drawable) {
            final GL gl = drawable.getGL();
            final boolean _makeShot = makeShot || makeShotAlways;
            if(verbose) {
                System.err.println(Thread.currentThread().getName()+": ** display: "+displayCount+": "+drawable.getSurfaceWidth()+"x"+drawable.getSurfaceHeight()+", makeShot "+_makeShot);
            }
            if(_makeShot) {
                makeShot=false;
                snapshot(displayCount, postSNDetail, gl, screenshot, TextureIO.PNG, null);
            }
            displayCount++;
        }
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            if(verbose) {
                System.err.println(Thread.currentThread().getName()+": ** reshape: "+reshapeCount+": "+width+"x"+height+" - "+drawable.getSurfaceWidth()+"x"+drawable.getSurfaceHeight());
            }
            reshapeCount++;
        }
        public void setMakeSnapshot() {
            makeShot=true;
        }
        public void setMakeSnapshotAlways(boolean v) {
            makeShotAlways=v;
        }
        public void setVerbose(boolean v) {
            verbose=v;
        }
        public void setPostSNDetail(String v) {
            postSNDetail = v;
        }
    };

}

