/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package jogamp.opengl;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;

import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLDebugListener;
import com.jogamp.opengl.GLDebugMessage;
import com.jogamp.opengl.GLException;

import jogamp.common.os.PlatformPropsImpl;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.os.Platform;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.opengl.GLExtensions;

/**
 * The GLDebugMessageHandler, handling <i>GL_ARB_debug_output</i> or <i>GL_AMD_debug_output</i>
 * debug messages.<br>
 *
 * <p>An instance must be bound to the current thread's GLContext to achieve thread safety.</p>
 *
 * <p>A native callback function is registered at {@link #enable(boolean) enable(true)},
 * which forwards received messages to the added {@link GLDebugListener} directly.
 * Hence the {@link GLDebugListener#messageSent(GLDebugMessage)} implementation shall
 * return as fast as possible.</p>
 *
 * <p>In case no <i>GL_ARB_debug_output</i> is available, but <i>GL_AMD_debug_output</i>,
 * the messages are translated to <i>ARB</i> {@link GLDebugMessage}, using {@link GLDebugMessage#translateAMDEvent(com.jogamp.opengl.GLContext, long, int, int, int, String)}.</p>
 */
public class GLDebugMessageHandler {
    private static final boolean DEBUG = Debug.debug("GLDebugMessageHandler");

    private static final int EXT_KHR = 1;
    private static final int EXT_ARB = 2;
    private static final int EXT_AMD = 3;

    static {
        if ( !initIDs0() ) {
            throw new NativeWindowException("Failed to initialize GLDebugMessageHandler jmethodIDs");
        }
    }

    private final GLContextImpl ctx;
    private final ListenerSyncedImplStub<GLDebugListener> listenerImpl;

    // licefycle: init - EOL
    private String extName;
    private String extSuffix;
    private int extType;
    private long glDebugMessageCallbackProcAddress;
    private boolean extAvailable;
    private boolean synchronous;

    // licefycle: enable - disable/EOL
    private long handle;

    /**
     * @param ctx the associated GLContext
     * @param glDebugExtension chosen extension to use
     */
    public GLDebugMessageHandler(final GLContextImpl ctx) {
        this.ctx = ctx;
        this.listenerImpl = new ListenerSyncedImplStub<GLDebugListener>();
        this.glDebugMessageCallbackProcAddress = 0;
        this.extName = null;
        this.extSuffix = null;
        this.extType = 0;
        this.extAvailable = false;
        this.handle = 0;
        this.synchronous = true;
    }

    public void init(final boolean enable) {
        if(DEBUG) {
            System.err.println("GLDebugMessageHandler.init("+enable+")");
        }
        init();
        if(isAvailable()) {
            enableImpl(enable);
        } else if(DEBUG) {
            System.err.println("GLDebugMessageHandler.init("+enable+") .. n/a");
        }
    }

    private final long getAddressFor(final ProcAddressTable table, final String functionName) {
        return AccessController.doPrivileged(new PrivilegedAction<Long>() {
            @Override
            public Long run() {
                try {
                    return Long.valueOf( table.getAddressFor(functionName) );
                } catch (final IllegalArgumentException iae) {
                    return Long.valueOf(0);
                }
            }
        } ).longValue();
    }

    public void init() {
        ctx.validateCurrent();
        if( isAvailable()) {
            return;
        }

        if( !ctx.isGLDebugEnabled() ) {
            if(DEBUG) {
                System.err.println("GLDebugMessageHandler: GL DEBUG not set in ARB ctx options: "+ctx.getGLVersion());
            }
            return;
        }
        if(PlatformPropsImpl.OS_TYPE == Platform.OSType.WINDOWS && Platform.is32Bit()) {
            // Currently buggy, ie. throws an exception after leaving the native callback.
            // Probably a 32bit on 64bit JVM / OpenGL-driver issue.
            if(DEBUG) {
                System.err.println("GLDebugMessageHandler: Windows 32bit currently not supported!");
            }
            return;
        }
        if( ctx.isExtensionAvailable(GLExtensions.GL_KHR_debug) ) {
            extName = GLExtensions.GL_KHR_debug;
            extSuffix = ctx.isGLES() ? "KHR" : ""; // See SPEC!
            extType = EXT_KHR;
        } else if( ctx.isExtensionAvailable(GLExtensions.ARB_debug_output) ) {
            extName = GLExtensions.ARB_debug_output;
            extSuffix = "ARB";
            extType = EXT_ARB;
        } else if( ctx.isExtensionAvailable(GLExtensions.AMD_debug_output) ) {
            extName = GLExtensions.AMD_debug_output;
            extSuffix = "AMD";
            extType = EXT_AMD;
        }

        // Validate GL Profile, just to be sure
        switch(extType) {
            case EXT_KHR:
                if( !ctx.isGL2ES2() ) {
                    if(DEBUG) {
                        System.err.println("Non GL2ES2 context not supported, has "+ctx.getGLVersion());
                    }
                    extType = 0;
                }
                break;
            case EXT_ARB:
                // fall through intended
            case EXT_AMD:
                if( !ctx.isGL2GL3() ) {
                    if(DEBUG) {
                        System.err.println("Non GL2GL3 context not supported, has "+ctx.getGLVersion());
                    }
                    extType = 0;
                }
                break;
        }

        if(0 == extType) {
            extName = null;
            extSuffix = null;
            if(DEBUG) {
                System.err.println("GLDebugMessageHandler: No extension available! "+ctx.getGLVersion());
                System.err.println("GL_EXTENSIONS  "+ctx.getGLExtensionCount());
                System.err.println(ctx.getGLExtensionsString());
            }
            return;
        } else  if(DEBUG) {
            System.err.println("GLDebugMessageHandler: Using extension: <"+extName+"> with suffix <"+extSuffix+">");
        }

        final ProcAddressTable procAddressTable = ctx.getGLProcAddressTable();
        switch(extType) {
            case EXT_KHR:
                glDebugMessageCallbackProcAddress = getAddressFor(procAddressTable, "glDebugMessageCallback"+extSuffix);
                break;
            case EXT_ARB:
                glDebugMessageCallbackProcAddress = getAddressFor(procAddressTable, "glDebugMessageCallback"+extSuffix);
                break;
            case EXT_AMD:
                glDebugMessageCallbackProcAddress = getAddressFor(procAddressTable, "glDebugMessageCallback"+extSuffix);
                break;
        }
        extAvailable = 0 < extType && null != extName && null != extSuffix && 0 != glDebugMessageCallbackProcAddress;

        if(DEBUG) {
            System.err.println("GLDebugMessageHandler: extAvailable: "+extAvailable+", glDebugMessageCallback* : 0x"+Long.toHexString(glDebugMessageCallbackProcAddress));
        }

        if(!extAvailable) {
            glDebugMessageCallbackProcAddress = 0;
        }

        handle = 0;
    }

    public final boolean isAvailable() { return extAvailable; }

    /**
     * @return The extension implementing the GLDebugMessage feature,
     *         either {@link #GL_ARB_debug_output} or {@link #GL_AMD_debug_output}.
     *         If unavailable <i>null</i> is returned.
     */
    public final String getExtension() {
        return extName;
    }

    public final boolean isExtensionKHRARB() {
        return EXT_KHR == extType || EXT_ARB == extType;
    }

    public final boolean isExtensionKHR() {
        return EXT_KHR == extType;
    }

    public final boolean isExtensionARB() {
        return EXT_ARB == extType;
    }

    public final boolean isExtensionAMD() {
        return EXT_AMD == extType;
    }

    /**
     * @see com.jogamp.opengl.GLContext#isGLDebugSynchronous()
     */
    public final boolean isSynchronous() { return synchronous; }

    /**
     * @see com.jogamp.opengl.GLContext#setGLDebugSynchronous(boolean)
     */
    public final void setSynchronous(final boolean synchronous) {
        this.synchronous = synchronous;
        if( isEnabled() ) {
            setSynchronousImpl();
        }
    }
    private final void setSynchronousImpl() {
        if(isExtensionKHRARB()) {
            if(synchronous) {
                ctx.getGL().glEnable(GL2ES2.GL_DEBUG_OUTPUT_SYNCHRONOUS);
            } else {
                ctx.getGL().glDisable(GL2ES2.GL_DEBUG_OUTPUT_SYNCHRONOUS);
            }
            if(DEBUG) {
                System.err.println("GLDebugMessageHandler: synchronous "+synchronous);
            }
        }
    }

    /**
     * @see com.jogamp.opengl.GLContext#enableGLDebugMessage(boolean)
     */
    public final void enable(final boolean enable) throws GLException {
        ctx.validateCurrent();
        if(!isAvailable()) {
            return;
        }
        enableImpl(enable);
    }
    final void enableImpl(final boolean enable) throws GLException {
        if(enable) {
            if(0 == handle) {
                setSynchronousImpl();
                handle = register0(glDebugMessageCallbackProcAddress, extType);
                if(0 == handle) {
                    throw new GLException("Failed to register via \"glDebugMessageCallback*\" using "+extName);
                }
            }
        } else {
            if(0 != handle) {
                unregister0(glDebugMessageCallbackProcAddress, handle);
                handle = 0;
            }
        }
        if(DEBUG) {
            System.err.println("GLDebugMessageHandler: enable("+enable+") -> 0x" + Long.toHexString(handle));
        }
    }

    public final boolean isEnabled() { return 0 != handle; }

    public final int listenerSize() {
        return listenerImpl.size();
    }

    public final void addListener(final GLDebugListener listener) {
        listenerImpl.addListener(-1, listener);
    }

    public final void addListener(final int index, final GLDebugListener listener) {
        listenerImpl.addListener(index, listener);
    }

    public final void removeListener(final GLDebugListener listener) {
        listenerImpl.removeListener(listener);
    }

    private final void sendMessage(final GLDebugMessage msg) {
        synchronized(listenerImpl) {
            if(DEBUG) {
                System.err.println("GLDebugMessageHandler: "+msg);
            }
            final ArrayList<GLDebugListener> listeners = listenerImpl.getListeners();
            for(int i=0; i<listeners.size(); i++) {
                listeners.get(i).messageSent(msg);
            }
        }
    }

    public static class StdErrGLDebugListener implements GLDebugListener {
        boolean threadDump;

        public StdErrGLDebugListener(final boolean threadDump) {
            this.threadDump = threadDump;
        }
        @Override
        public void messageSent(final GLDebugMessage event) {
            System.err.println(event);
            if(threadDump) {
                ExceptionUtils.dumpStack(System.err);
            }
        }
    }

    //
    // native -> java
    //

    protected final void glDebugMessageARB(final int source, final int type, final int id, final int severity, final String msg) {
        final GLDebugMessage event = new GLDebugMessage(ctx, System.currentTimeMillis(), source, type, id, severity, msg);
        sendMessage(event);
    }

    protected final void glDebugMessageAMD(final int id, final int category, final int severity, final String msg) {
        final GLDebugMessage event = GLDebugMessage.translateAMDEvent(ctx, System.currentTimeMillis(), id, category, severity, msg);
        sendMessage(event);
    }

    //
    // java -> native
    //

    private static native boolean initIDs0();
    private native long register0(long glDebugMessageCallbackProcAddress, int extType);
    private native void unregister0(long glDebugMessageCallbackProcAddress, long handle);
}


