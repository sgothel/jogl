/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package jogamp.opengl.egl;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.SurfaceChangeable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;

public class EGLPbufferDrawable extends EGLDrawable {
    private int texFormat;
    protected static final boolean useTexture = false; // No yet ..

    protected EGLPbufferDrawable(EGLDrawableFactory factory, NativeSurface target) {
        super(factory, target);
    }

    protected void destroyImpl() {
        setRealized(false);
    }
    
    protected long createSurface(long eglDpy, long eglNativeCfg, long surfaceHandle) {
        final AbstractGraphicsConfiguration config = getNativeSurface().getGraphicsConfiguration();
        final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) config.getChosenCapabilities();

        if(useTexture) {
            texFormat = caps.getAlphaBits() > 0 ? EGL.EGL_TEXTURE_RGBA : EGL.EGL_TEXTURE_RGB ;
        } else {
            texFormat = EGL.EGL_NO_TEXTURE;
        }

        if (DEBUG) {
          System.out.println("Pbuffer config: " + config);
        }

        NativeSurface nw = getNativeSurface();
        int[] attrs = EGLGraphicsConfiguration.CreatePBufferSurfaceAttribList(nw.getWidth(), nw.getHeight(), texFormat);
        long surf = EGL.eglCreatePbufferSurface(eglDpy, eglNativeCfg, attrs, 0);
        if (EGL.EGL_NO_SURFACE==surf) {
            throw new GLException("Creation of window surface (eglCreatePbufferSurface) failed, dim "+nw.getWidth()+"x"+nw.getHeight()+", error 0x"+Integer.toHexString(EGL.eglGetError()));
        } else if(DEBUG) {
            System.err.println("PBuffer setSurface result: eglSurface 0x"+Long.toHexString(surf));
        }
        ((SurfaceChangeable)nw).setSurfaceHandle(surf);
        return surf;
    }

    public GLContext createContext(GLContext shareWith) {
        return new EGLPbufferContext(this, shareWith);
    }
}

