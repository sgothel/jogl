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

package com.sun.opengl.impl.egl;

import java.util.*;
import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.nativewindow.impl.*;
import com.sun.gluegen.runtime.NativeLibrary;

public class EGLDrawableFactory extends GLDrawableFactoryImpl {
  
    static {
        // Register our GraphicsConfigurationFactory implementations
        // The act of constructing them causes them to be registered
        new EGLGraphicsConfigurationFactory();

        // Check for other underlying stuff ..
        if(NativeWindowFactory.TYPE_X11.equals(NativeWindowFactory.getNativeWindowType(false))) {
            try {
                NWReflection.createInstance("com.sun.opengl.impl.x11.glx.X11GLXGraphicsConfigurationFactory");
            } catch (Throwable t) {}
        }
    }

    public EGLDrawableFactory() {
        super();
    }

    public GLDrawable createGLDrawable(NativeWindow target) {
        target = NativeWindowFactory.getNativeWindow(target, null);
        return new EGLOnscreenDrawable(this, target);
    }

    public GLDrawableImpl createOffscreenDrawable(GLCapabilities capabilities,
                                                  GLCapabilitiesChooser chooser,
                                                  int width,
                                                  int height) {
        throw new GLException("Not yet implemented");
    }

    public boolean canCreateGLPbuffer() {
        return true;
    }
    public GLPbuffer createGLPbuffer(final GLCapabilities capabilities,
                                     final GLCapabilitiesChooser chooser,
                                     final int initialWidth,
                                     final int initialHeight,
                                     final GLContext shareWith) {
        if (!canCreateGLPbuffer()) {
          throw new GLException("Pbuffer support not available with this EGL implementation");
        }
        EGLPbufferDrawable pbufferDrawable = new EGLPbufferDrawable(this, capabilities, chooser,
                                                                  initialWidth,
                                                                  initialHeight);
        return new GLPbufferImpl(pbufferDrawable, shareWith);
    }

    public GLContext createExternalGLContext() {
        AbstractGraphicsScreen absScreen = DefaultGraphicsScreen.createScreenDevice(0);
        return new EGLExternalContext(absScreen);
    }

    public boolean canCreateExternalGLDrawable() {
        return false;
    }

    public GLDrawable createExternalGLDrawable() {
        throw new GLException("Not yet implemented");
    }

    public void loadGLULibrary() {
    }

    public boolean canCreateContextOnJava2DSurface() {
        return false;
    }

    public GLContext createContextOnJava2DSurface(Object graphics, GLContext shareWith)
        throws GLException {
        throw new GLException("Unimplemented on this platform");
    }
}
