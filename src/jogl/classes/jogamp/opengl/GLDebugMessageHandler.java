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

import java.util.ArrayList;

import javax.media.nativewindow.NativeWindowException;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLDebugListener;
import javax.media.opengl.GLDebugMessage;
import javax.media.opengl.GLException;

import com.jogamp.gluegen.runtime.ProcAddressTable;
import jogamp.opengl.gl4.GL4bcProcAddressTable;

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
    /** Extension <i>GL_ARB_debug_output</i> implementing GLDebugMessage */
    public static final String GL_ARB_debug_output = "GL_ARB_debug_output".intern();
    
    /** Extension <i>GL_AMD_debug_output</i> implementing GLDebugMessage */
    public static final String GL_AMD_debug_output = "GL_AMD_debug_output".intern();
    
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
    }
    
    public void init(boolean enable) {
        init();
        if(isAvailable()) {
            enableImpl(enable);
        }
    }
    
    public void init() {
        ctx.validateCurrent();
        if( isAvailable()) {
            return;
        }
        
        if( ctx.isExtensionAvailable(GL_ARB_debug_output) ) {
            extName = GL_ARB_debug_output;
            extType = EXT_ARB;
        } else if( ctx.isExtensionAvailable(GL_AMD_debug_output) ) {
            extName = GL_AMD_debug_output;
            extType = EXT_AMD;
        }
        if(DEBUG) {
            System.err.println("GLDebugMessageHandler: Using extension: <"+extName+">");
        }
        
        if(0 == extType) {
            if(DEBUG) {
                System.err.println("GLDebugMessageHandler: No extension available!");
            }
            return;
        }
                
        final ProcAddressTable procAddressTable = ctx.getGLProcAddressTable();
        if( procAddressTable instanceof GL4bcProcAddressTable) {
            final GL4bcProcAddressTable desktopProcAddressTable = (GL4bcProcAddressTable)procAddressTable;
            switch(extType) {
                case EXT_ARB: 
                    glDebugMessageCallbackProcAddress = desktopProcAddressTable._addressof_glDebugMessageCallbackARB;
                    break;
                case EXT_AMD: 
                    glDebugMessageCallbackProcAddress = desktopProcAddressTable._addressof_glDebugMessageCallbackAMD;
                    break;
            }
        } else {
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
        return extName == GL_ARB_debug_output;
    }
    
    public final boolean isExtensionAMD() {
        return extName == GL_AMD_debug_output;
    }
    
    /**
     * @throws GLException if context not current or callback registration failed (enable) 
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
        public void messageSent(GLDebugMessage event) {
            System.err.println(event);            
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


