/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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

package jogamp.nativewindow.drm;

import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;

import jogamp.nativewindow.Debug;
import jogamp.nativewindow.NWJNILibLoader;
import jogamp.nativewindow.ToolkitProperties;

import java.io.File;

import com.jogamp.common.ExceptionUtils;

/**
 * DRM and GBM utility
 */
public class DRMUtil implements ToolkitProperties {
    /* pp */ static final boolean DEBUG = Debug.debug("DRMUtil");

    /** FIXME: Add support for other OS implementing DRM/GBM, e.g. FreeBSD, OpenBSD, ..? */
    private static final String driXLinux = "/dev/dri/card";

    private static volatile boolean isInit = false;
    /** DRM file descriptor, valid if >= 0 */
    private static int drmFd = -1;

    private static int openDrmDevice(final String[] lastFilename) {
        for(int i=0; i<100; i++) {
            final String driXFilename = driXLinux + i;
            lastFilename[0] = driXFilename;
            final File driXFile = new File(driXFilename);
            if( !driXFile.exists() ) {
                if(DEBUG) {
                    System.err.println("DRMUtil.initSingleton(): drmDevice["+driXFilename+"]: not existing");
                }
                // end of search, failure
                return -1;
            }
            final int fd = DRMLib.drmOpenFile(driXFilename);
            if( 0 <= fd ) {
                // test ..
                final drmModeRes resources = DRMLib.drmModeGetResources(fd);
                if(DEBUG) {
                    System.err.println("DRMUtil.initSingleton(): drmDevice["+driXFilename+"]: fd "+fd+": has resources: "+(null!=resources));
                }
                if( null == resources ) {
                    // nope, not working - continue testing
                    DRMLib.drmClose(fd);
                    continue;
                } else {
                    // OK
                    DRMLib.drmModeFreeResources(resources);
                    return fd;
                }
            }
        }
        return -1;
    }

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static void initSingleton() {
        if(!isInit) {
            synchronized(DRMUtil.class) {
                if(!isInit) {
                    isInit = true;
                    if(DEBUG) {
                        System.out.println("DRMUtil.initSingleton()");
                    }
                    if(!NWJNILibLoader.loadNativeWindow("drm")) {
                        throw new NativeWindowException("NativeWindow DRM native library load error.");
                    }
                    final String[] lastFilename = new String[] { null };
                    if( initialize0(DEBUG) ) {
                        drmFd = openDrmDevice(lastFilename);
                    }
                    if(DEBUG) {
                        System.err.println("DRMUtil.initSingleton(): OK "+(0 <= drmFd)+", drmFd "+drmFd+"]");
                        if( 0 <= drmFd ) {
                            final DrmMode d = DrmMode.create(drmFd, true);
                            d.print(System.err);
                            d.destroy();
                        }
                        // Thread.dumpStack();
                    }
                    if( 0 > drmFd ) {
                        throw new NativeWindowException("drmOpenFile("+lastFilename[0]+") failed");
                    }
                }
            }
        }
    }

    /** Return the global DRM file descriptor */
    public static int getDrmFd() { return drmFd; }

    /**
     * Cleanup resources.
     * <p>
     * Called by {@link NativeWindowFactory#shutdown()}
     * </p>
     * @see ToolkitProperties
     */
    public static void shutdown() {
        if(isInit) {
            synchronized(DRMUtil.class) {
                if(isInit) {
                    final boolean isJVMShuttingDown = NativeWindowFactory.isJVMShuttingDown() ;
                    if( DEBUG ) {
                        System.err.println("DRMUtil.Display: Shutdown (JVM shutdown: "+isJVMShuttingDown+")");
                        if(DEBUG) {
                            ExceptionUtils.dumpStack(System.err);
                        }
                    }

                    // Only at JVM shutdown time, since AWT impl. seems to
                    // dislike closing of X11 Display's (w/ ATI driver).
                    if( isJVMShuttingDown ) {
                        if( 0 <= drmFd ) {
                            DRMLib.drmClose(drmFd);
                            drmFd = -1;
                        }
                        isInit = false;
                        shutdown0();
                    }
                }
            }
        }
    }

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static final boolean requiresToolkitLock() {
        return true;
    }

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     * @see ToolkitProperties
     */
    public static final boolean hasThreadingIssues() {
        return false;
    }

    static int fourcc_code(final char a, final char b, final char c, final char d) {
        // return ( (int)(a) | ((int)(b) << 8) | ((int)(c) << 16) | ((int)(d) << 24) );
        return ( (a) | ((b) << 8) | ((c) << 16) | ((d) << 24) );
    }
    /** [31:0] x:R:G:B 8:8:8:8 little endian */
    public static final int GBM_FORMAT_XRGB8888 = fourcc_code('X', 'R', '2', '4');
    /** [31:0] A:R:G:B 8:8:8:8 little endian */
    public static final int GBM_FORMAT_ARGB8888 = fourcc_code('A', 'R', '2', '4');

    private DRMUtil() {}

    private static final String getCurrentThreadName() { return Thread.currentThread().getName(); } // Callback for JNI
    private static final void dumpStack() { ExceptionUtils.dumpStack(System.err); } // Callback for JNI

    private static native boolean initialize0(boolean debug);
    private static native void shutdown0();
}
