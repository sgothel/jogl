/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.opengl.impl.egl;

import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.jogamp.common.JogampRuntimeException;
import com.jogamp.common.util.*;
import com.jogamp.opengl.impl.*;
import com.jogamp.nativewindow.impl.NullWindow;

public class EGLDrawableFactory extends GLDrawableFactoryImpl {
  
    private static final GLDynamicLookupHelper eglES1DynamicLookupHelper;
    private static final GLDynamicLookupHelper eglES2DynamicLookupHelper;

    static {
        // Register our GraphicsConfigurationFactory implementations
        // The act of constructing them causes them to be registered
        new EGLGraphicsConfigurationFactory();

        // Check for other underlying stuff ..
        if(NativeWindowFactory.TYPE_X11.equals(NativeWindowFactory.getNativeWindowType(true))) {
            try {
                ReflectionUtil.createInstance("com.jogamp.opengl.impl.x11.glx.X11GLXGraphicsConfigurationFactory", EGLDrawableFactory.class.getClassLoader());
            } catch (JogampRuntimeException jre) { /* n/a .. */ }
        }

        // FIXME: Probably need to move EGL from a static model 
        // to a dynamic one, where there can be 2 instances 
        // for each ES profile with their own ProcAddressTable.

        GLDynamicLookupHelper tmp=null;
        try {
            tmp = new GLDynamicLookupHelper(new EGLES1DynamicLibraryBundleInfo());
        } catch (GLException gle) {
            if(DEBUG) {
                gle.printStackTrace();
            }
        }
        eglES1DynamicLookupHelper = tmp;
        if(null!=eglES1DynamicLookupHelper && eglES1DynamicLookupHelper.isLibComplete()) {
            EGL.resetProcAddressTable(eglES1DynamicLookupHelper);
        }

        tmp=null;
        try {
            tmp = new GLDynamicLookupHelper(new EGLES2DynamicLibraryBundleInfo());
        } catch (GLException gle) {
            if(DEBUG) {
                gle.printStackTrace();
            }
        }
        eglES2DynamicLookupHelper = tmp;
        if(null!=eglES2DynamicLookupHelper && eglES2DynamicLookupHelper.isLibComplete()) {
            EGL.resetProcAddressTable(eglES2DynamicLookupHelper);
        }
    }

    public EGLDrawableFactory() {
        super();
    }

    public GLDynamicLookupHelper getGLDynamicLookupHelper(int esProfile) {
        if (2==esProfile) {
            if(null==eglES2DynamicLookupHelper) {
                throw new GLException("GLDynamicLookupHelper for ES2 not available");
            }
            return eglES2DynamicLookupHelper;
        } else if (1==esProfile) {
            if(null==eglES1DynamicLookupHelper) {
                throw new GLException("GLDynamicLookupHelper for ES1 not available");
            }
            return eglES1DynamicLookupHelper;
        } else {
            throw new GLException("Unsupported: ES"+esProfile);
        }
    }

    protected void shutdown() {}
    protected final GLDrawableImpl getSharedDrawable() { return null; }
    protected final GLContextImpl getSharedContext() { return null; }

    public GLDrawableImpl createOnscreenDrawable(NativeWindow target) {
        if (target == null) {
          throw new IllegalArgumentException("Null target");
        }
        return new EGLOnscreenDrawable(this, target);
    }

    protected GLDrawableImpl createOffscreenDrawable(NativeWindow target) {
        throw new GLException("Not yet implemented");
    }

    public boolean canCreateGLPbuffer(AbstractGraphicsDevice device) {
        return true;
    }

    protected GLDrawableImpl createGLPbufferDrawableImpl(NativeWindow target) {
        return new EGLPbufferDrawable(this, target);
    }

    protected NativeWindow createOffscreenWindow(GLCapabilities capabilities, GLCapabilitiesChooser chooser, int width, int height) {
        NullWindow nw = new NullWindow(EGLGraphicsConfigurationFactory.createOffscreenGraphicsConfiguration(capabilities, chooser));
        nw.setSize(width, height);
        return nw;
    }

    public GLContext createExternalGLContext() {
        AbstractGraphicsScreen absScreen = DefaultGraphicsScreen.createScreenDevice(0);
        return new EGLExternalContext(absScreen);
    }

    public boolean canCreateExternalGLDrawable(AbstractGraphicsDevice device) {
        return false;
    }

    public GLDrawable createExternalGLDrawable() {
        throw new GLException("Not yet implemented");
    }

    public boolean canCreateContextOnJava2DSurface(AbstractGraphicsDevice device) {
        return false;
    }

    public GLContext createContextOnJava2DSurface(Object graphics, GLContext shareWith)
        throws GLException {
        throw new GLException("Unimplemented on this platform");
    }
}
