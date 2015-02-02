/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.graph.demos.ui;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLOffscreenAutoDrawable;

import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Vertex.Factory;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.util.texture.ImageSequence;
import com.jogamp.opengl.util.texture.Texture;

/**
 * GPU based resolution independent Button impl,
 * rendering {@link GLEventListener} content via FBO as an {@link ImageSequence}.
 */
public class GLEventListenerButton extends TextureSeqButton {
    private final GLEventListener glel;
    private final boolean useAlpha;
    private volatile int fboWidth = 200;
    private volatile int fboHeight = 200;
    private volatile GLOffscreenAutoDrawable.FBO fboGLAD = null;
    private boolean animateGLEL = false;

    public GLEventListenerButton(final Factory<? extends Vertex> factory, final int renderModes,
                                 final float width, final float height, final int textureUnit,
                                 final GLEventListener glel, final boolean useAlpha, final int fboWidth, final int fboHeight) {
        super(factory, renderModes, width, height, new ImageSequence(textureUnit, true));
        this.glel = glel;
        this.useAlpha = useAlpha;

        setColor(0.95f, 0.95f, 0.95f, 1.0f);
        setPressedColorMod(1f, 1f, 1f, 0.9f);
        setToggleOffColorMod(0.8f, 0.8f, 0.8f, 1.0f);
        setToggleOnColorMod(1.0f, 1.0f, 1.0f, 1.0f);

        this.fboWidth = fboWidth;
        this.fboHeight = fboHeight;
    }

    public final void setAnimate(final boolean v) { animateGLEL = v; }
    public final boolean getAnimate() { return animateGLEL; }

    public final void setFBOSize(final int fboWidth, final int fboHeight) {
        this.fboWidth = fboWidth;
        this.fboHeight = fboHeight;
    }

    public final GLOffscreenAutoDrawable.FBO getFBOAutoDrawable() { return fboGLAD; }

    @Override
    public void drawShape(final GL2ES2 gl, final RegionRenderer renderer, final int[] sampleCount) {
        if( null == fboGLAD ) {
            final ImageSequence imgSeq = (ImageSequence)texSeq;

            final GLContext ctx = gl.getContext();
            final GLDrawable drawable = ctx.getGLDrawable();
            final GLCapabilitiesImmutable reqCaps = drawable.getRequestedGLCapabilities();
            final GLCapabilities caps = (GLCapabilities) reqCaps.cloneMutable();
            caps.setFBO(true);
            caps.setDoubleBuffered(false);
            if( !useAlpha ) {
                caps.setAlphaBits(0);
            }
            final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());

            fboGLAD = (GLOffscreenAutoDrawable.FBO) factory.createOffscreenAutoDrawable(
                            drawable.getNativeSurface().getGraphicsConfiguration().getScreen().getDevice(),
                            caps, null, fboWidth, fboHeight);
            fboWidth = 0;
            fboHeight = 0;
            fboGLAD.setSharedContext(ctx);
            fboGLAD.setTextureUnit(imgSeq.getTextureUnit());
            fboGLAD.addGLEventListener(glel);
            fboGLAD.display(); // 1st init!

            final FBObject.TextureAttachment texA01 = fboGLAD.getColorbuffer(GL.GL_FRONT).getTextureAttachment();
            final Texture tex = new Texture(texA01.getName(), imgSeq.getTextureTarget(),
                                    fboGLAD.getSurfaceWidth(), fboGLAD.getSurfaceHeight(), fboGLAD.getSurfaceWidth(), fboGLAD.getSurfaceHeight(),
                                    false /* mustFlipVertically */);
            imgSeq.addFrame(gl, tex);
            markStateDirty();
        } else if( 0 != fboWidth*fboHeight ) {
            fboGLAD.setSurfaceSize(fboWidth, fboHeight);
            fboWidth = 0;
            fboHeight = 0;
            markStateDirty();
        } else if( animateGLEL ) {
            fboGLAD.display();
        }

        super.drawShape(gl, renderer, sampleCount);

        if( animateGLEL ) {
            markStateDirty(); // keep on going
        }
    }
}
