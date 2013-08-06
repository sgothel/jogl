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

import javax.media.nativewindow.NativeWindowException;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLDebugListener;
import javax.media.opengl.GLDebugMessage;
import javax.media.opengl.GLException;

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
 * the messages are translated to <i>ARB</i> {@link GLDebugMessage}, using {@link GLDebugMessage#translateAMDEvent(javax.media.opengl.GLContext, long, int, int, int, String)}.</p>
 */
public class GLDebugMessageHandler {
    private static final boolean DEBUG = Debug.debug("GLDebugMessageHandler");
    
    private static final int EXT_ARB = 1;
    private static final int EXT_AMD = 2;    
    
    static {
        if ( !initIDs0() ) {
            throw new NativeWindowException("Failed to initialize GLDebugMessageHandler jmethodIDs");
        }        
    }
            
    private final GLContextImpl ctx;    
    private final ListenerSyncedImplStub<GLDebugListener> listenerImpl;
    
    // licefycle: init - EOL
    private String extName;
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
    public GLDebugMessageHandler(GLContextImpl ctx) {        
        this.ctx = ctx;
        this.listenerImpl = new ListenerSyncedImplStub<GLDebugListener>();        
        this.glDebugMessageCallbackProcAddress = 0;
        this.extName = null;
        this.extType = 0;
        this.extAvailable = false; 
        this.handle = 0;
        this.synchronous = true;
    }
    
    public void init(boolean enable) {
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
            public Long run() {
                try {
                    return Long.valueOf( table.getAddressFor(functionName) );
                } catch (IllegalArgumentException iae) { 
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
        if(Platform.OS_TYPE == Platform.OSType.WINDOWS && Platform.is32Bit()) {
            // Currently buggy, ie. throws an exception after leaving the native callback.
            // Probably a 32bit on 64bit JVM / OpenGL-driver issue.
            if(DEBUG) {
                System.err.println("GLDebugMessageHandler: Windows 32bit currently not supported!");
            }
            return;
        }
        if( ctx.isExtensionAvailable(GLExtensions.ARB_debug_output) ) {
            extName = GLExtensions.ARB_debug_output;
            extType = EXT_ARB;
        } else if( ctx.isExtensionAvailable(GLExtensions.AMD_debug_output) ) {
            extName = GLExtensions.AMD_debug_output;
            extType = EXT_AMD;
        }
        if(DEBUG) {
            System.err.println("GLDebugMessageHandler: Using extension: <"+extName+">");
        }
        
        if(0 == extType) {
            if(DEBUG) {
                System.err.println("GLDebugMessageHandler: No extension available! "+ctx.getGLVersion());
                System.err.println("GL_EXTENSIONS  "+ctx.getGLExtensionCount());
                System.err.println(ctx.getGLExtensionsString());                
            }
            return;
        }
                
        final ProcAddressTable procAddressTable = ctx.getGLProcAddressTable();
        if( !ctx.isGLES1() && !ctx.isGLES2() ) {
            switch(extType) {
                case EXT_ARB: 
                    glDebugMessageCallbackProcAddress = getAddressFor(procAddressTable, "glDebugMessageCallbackARB");
                    break;
                case EXT_AMD: 
                    glDebugMessageCallbackProcAddress = getAddressFor(procAddressTable, "glDebugMessageCallbackAMD");
                    break;
            }
        } else {
            glDebugMessageCallbackProcAddress = 0;
            if(DEBUG) {
                System.err.println("Non desktop context not supported");    
            }            
        }
        extAvailable = 0 < extType && null != extName && 0 != glDebugMessageCallbackProcAddress;
        
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
    
    public final boolean isExtensionARB() {
        return extName == GLExtensions.ARB_debug_output;
    }
    
    public final boolean isExtensionAMD() {
        return extName == GLExtensions.AMD_debug_output;
    }
    
    /**
     * @see javax.media.opengl.GLContext#isGLDebugSynchronous() 
     */
    public final boolean isSynchronous() { return synchronous; }
    
    /**
     * @see javax.media.opengl.GLContext#setGLDebugSynchronous(boolean) 
     */
    public final void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
        if( isEnabled() ) {
            setSynchronousImpl();
        }
    }    
    private final void setSynchronousImpl() {
        if(isExtensionARB()) {
            if(synchronous) {
                ctx.getGL().glEnable(GL2GL3.GL_DEBUG_OUTPUT_SYNCHRONOUS);
            } else {
                ctx.getGL().glDisable(GL2GL3.GL_DEBUG_OUTPUT_SYNCHRONOUS);
            }        
            if(DEBUG) {
                System.err.println("GLDebugMessageHandler: synchronous "+synchronous);
            }
        }
    }
    
    /**
     * @see javax.media.opengl.GLContext#enableGLDebugMessage(boolean) 
     */
    public final void enable(boolean enable) throws GLException {
        ctx.validateCurrent();
        if(!isAvailable()) {
            return;
        }
        enableImpl(enable);
    }        
    final void enableImpl(boolean enable) throws GLException {
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
    
    public final void addListener(GLDebugListener listener) {
        listenerImpl.addListener(-1, listener);
    }

    public final void addListener(int index, GLDebugListener listener) {
        listenerImpl.addListener(index, listener);
    }
  
    public final void removeListener(GLDebugListener listener) {
        listenerImpl.removeListener(listener);
    }
    
    private final void sendMessage(GLDebugMessage msg) {
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
        
        public StdErrGLDebugListener(boolean threadDump) {
            this.threadDump = threadDump;
        }
        public void messageSent(GLDebugMessage event) {
            System.err.println(event);
            if(threadDump) {
                Thread.dumpStack();
            }
        }        
    }
    
    //
    // native -> java
    //
    
    protected final void glDebugMessageARB(int source, int type, int id, int severity, String msg) {
        final GLDebugMessage event = new GLDebugMessage(ctx, System.currentTimeMillis(), source, type, id, severity, msg);
        sendMessage(event);
    }

    protected final void glDebugMessageAMD(int id, int category, int severity, String msg) {
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


