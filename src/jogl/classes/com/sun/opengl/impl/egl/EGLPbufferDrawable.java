/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.impl.egl;

import javax.media.opengl.*;
import javax.media.nativewindow.*;
import javax.media.nativewindow.egl.*;
import com.sun.opengl.impl.*;
import com.sun.nativewindow.impl.NullWindow;

import java.nio.LongBuffer;

public class EGLPbufferDrawable extends EGLDrawable {
    private int width, height;
    private int texFormat;
    protected static final boolean useTexture = false; // No yet ..

    protected EGLPbufferDrawable(EGLDrawableFactory factory,
                               GLCapabilities caps, 
                               GLCapabilitiesChooser chooser,
                               int width, int height) {
        super(factory, new NullWindow(createEGLGraphicsConfiguration(caps, chooser)));
        if (width <= 0 || height <= 0) {
          throw new GLException("Width and height of pbuffer must be positive (were (" +
                    width + ", " + height + "))");
        }

        // get choosen ones ..
        caps = (GLCapabilities) getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration().getChosenCapabilities();

        this.width=width;
        this.height=height;
        if(useTexture) {
            this.texFormat = caps.getAlphaBits() > 0 ? EGL.EGL_TEXTURE_RGBA : EGL.EGL_TEXTURE_RGB ;
        } else {
            this.texFormat = EGL.EGL_NO_TEXTURE;
        }

        NullWindow nw = (NullWindow) getNativeWindow();
        nw.setSize(width, height);

        ownEGLDisplay = true;

        if (DEBUG) {
          System.out.println("Pbuffer config: " + getNativeWindow().getGraphicsConfiguration().getNativeGraphicsConfiguration());
        }
    }

    protected static EGLGraphicsConfiguration createEGLGraphicsConfiguration(GLCapabilities caps, GLCapabilitiesChooser chooser) {
        long eglDisplay = EGL.eglGetDisplay(EGL.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL.EGL_NO_DISPLAY) {
            throw new GLException("Failed to created EGL default display: error 0x"+Integer.toHexString(EGL.eglGetError()));
        } else if(DEBUG) {
            System.err.println("eglDisplay(EGL_DEFAULT_DISPLAY): 0x"+Long.toHexString(eglDisplay));
        }
        if (!EGL.eglInitialize(eglDisplay, null, null)) {
            throw new GLException("eglInitialize failed"+", error 0x"+Integer.toHexString(EGL.eglGetError()));
        }
        EGLGraphicsDevice e = new EGLGraphicsDevice(eglDisplay);
        DefaultGraphicsScreen s = new DefaultGraphicsScreen(e, 0);
        EGLGraphicsConfiguration eglConfig = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(caps, chooser, s, EGL.EGL_PBUFFER_BIT);
        if (null == eglConfig) {
            EGL.eglTerminate(eglDisplay);
            throw new GLException("Couldn't create EGLGraphicsConfiguration from "+s);
        } else if(DEBUG) {
            System.err.println("Chosen eglConfig: "+eglConfig);
        }
        return eglConfig;
    }

    protected long createSurface(long eglDpy, _EGLConfig eglNativeCfg) {
        int[] attrs = EGLGraphicsConfiguration.CreatePBufferSurfaceAttribList(width, height, texFormat);
        long surf = EGL.eglCreatePbufferSurface(eglDpy, eglNativeCfg, attrs, 0);
        if (EGL.EGL_NO_SURFACE==surf) {
            throw new GLException("Creation of window surface (eglCreatePbufferSurface) failed, dim "+width+"x"+height+", error 0x"+Integer.toHexString(EGL.eglGetError()));
        } else if(DEBUG) {
            System.err.println("setSurface result: eglSurface 0x"+Long.toHexString(surf));
        }
        return surf;
    }

    public GLContext createContext(GLContext shareWith) {
        return new EGLPbufferContext(this, shareWith);
    }

}

