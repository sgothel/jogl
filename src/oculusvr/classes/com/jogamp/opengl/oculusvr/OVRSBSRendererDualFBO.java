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
package com.jogamp.opengl.oculusvr;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

import jogamp.opengl.oculusvr.OVRDistortion;

import com.jogamp.oculusvr.OVR;
import com.jogamp.oculusvr.ovrFrameTiming;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.util.CustomRendererListener;
import com.jogamp.opengl.util.stereo.StereoRendererListener;

/**
 * OculusVR (OVR) <i>Side By Side</i> Distortion Renderer utilizing {@link OVRDistortion}
 * implementing {@link GLEventListener} for convenience.
 * <p>
 * Implementation renders an {@link StereoRendererListener} instance
 * side-by-side using two {@link FBObject}s according to {@link OVRDistortion}.
 * </p>
 */
public class OVRSBSRendererDualFBO implements GLEventListener {
    private final OVRDistortion dist;
    private final boolean ownsDist;
    private final StereoRendererListener upstream;
    private final FBObject[] fbos;
    private final int magFilter;
    private final int minFilter;

    private int numSamples;
    private final TextureAttachment[] fboTexs;

    public OVRSBSRendererDualFBO(final OVRDistortion dist, final boolean ownsDist, final StereoRendererListener upstream,
                                 final int magFilter, final int minFilter, final int numSamples) {
        this.dist = dist;
        this.ownsDist = ownsDist;
        this.upstream = upstream;
        this.fbos = new FBObject[2];
        this.fbos[0] = new FBObject();
        this.fbos[1] = new FBObject();
        this.magFilter = magFilter;
        this.minFilter = minFilter;

        this.numSamples = numSamples;
        this.fboTexs = new TextureAttachment[2];
    }

    private void initFBOs(final GL gl, final int width, final int height) {
        // remove all texture attachments, since MSAA uses just color-render-buffer
        // and non-MSAA uses texture2d-buffer
        fbos[0].detachAllColorbuffer(gl);
        fbos[1].detachAllColorbuffer(gl);

        fbos[0].reset(gl, width, height, numSamples, false);
        fbos[1].reset(gl, width, height, numSamples, false);
        if(fbos[0].getNumSamples() != fbos[1].getNumSamples()) {
            throw new InternalError("sample size mismatch: \n\t0: "+fbos[0]+"\n\t1: "+fbos[1]);
        }
        numSamples = fbos[0].getNumSamples();

        if(numSamples>0) {
            fbos[0].attachColorbuffer(gl, 0, true); // MSAA requires alpha
            fbos[0].attachRenderbuffer(gl, Type.DEPTH, 24);
            final FBObject ssink0 = new FBObject();
            {
                ssink0.reset(gl, width, height);
                ssink0.attachTexture2D(gl, 0, false, magFilter, minFilter, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
                ssink0.attachRenderbuffer(gl, Attachment.Type.DEPTH, 24);
            }
            fbos[0].setSamplingSink(ssink0);
            fbos[0].resetSamplingSink(gl); // validate
            fboTexs[0] = fbos[0].getSamplingSink();

            fbos[1].attachColorbuffer(gl, 0, true); // MSAA requires alpha
            fbos[1].attachRenderbuffer(gl, Type.DEPTH, 24);
            final FBObject ssink1 = new FBObject();
            {
                ssink1.reset(gl, width, height);
                ssink1.attachTexture2D(gl, 0, false, magFilter, minFilter, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
                ssink1.attachRenderbuffer(gl, Attachment.Type.DEPTH, 24);
            }
            fbos[1].setSamplingSink(ssink1);
            fbos[1].resetSamplingSink(gl); // validate
            fboTexs[1] = fbos[1].getSamplingSink();
        } else {
            fboTexs[0] = fbos[0].attachTexture2D(gl, 0, false, magFilter, minFilter, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
            fbos[0].attachRenderbuffer(gl, Type.DEPTH, 24);
            fboTexs[1] = fbos[1].attachTexture2D(gl, 0, false, magFilter, minFilter, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
            fbos[1].attachRenderbuffer(gl, Type.DEPTH, 24);
        }
        fbos[0].unbind(gl);
        fbos[1].unbind(gl);
    }

    @SuppressWarnings("unused")
    private void resetFBOs(final GL gl, final int width, final int height) {
        fbos[0].reset(gl, width, height, numSamples, true);
        fbos[1].reset(gl, width, height, numSamples, true);
        if(fbos[0].getNumSamples() != fbos[1].getNumSamples()) {
            throw new InternalError("sample size mismatch: \n\t0: "+fbos[0]+"\n\t1: "+fbos[1]);
        }
        numSamples = fbos[0].getNumSamples();
        if(numSamples>0) {
            fboTexs[0] = fbos[0].getSamplingSink();
            fboTexs[1] = fbos[1].getSamplingSink();
        } else {
            fboTexs[0] = (TextureAttachment) fbos[0].getColorbuffer(0);
            fboTexs[1] = (TextureAttachment) fbos[1].getColorbuffer(0);
        }
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        dist.init(gl);

        // We will do some offscreen rendering, setup FBO...
        if( null != upstream ) {
            final int[] textureSize = dist.textureSize;
            initFBOs(gl, textureSize[0], textureSize[1]);
            upstream.init(drawable);
        }

        gl.setSwapInterval(1);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        // FIXME complete release
        if( null != upstream ) {
            upstream.dispose(drawable);
            fbos[0].destroy(gl);
            fbos[1].destroy(gl);
        }
        if( ownsDist ) {
            dist.dispose(gl);
        }
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final ovrFrameTiming frameTiming = OVR.ovrHmd_BeginFrameTiming(dist.hmdCtx, 0);

        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(0 < numSamples) {
            gl.glEnable(GL.GL_MULTISAMPLE);
        }

        // FIXME: Instead of setting the viewport,
        // it's better to change the projection matrix!
        if( null != upstream ) {
            for(int eyeNum=0; eyeNum<2; eyeNum++) {
                // final ovrPosef eyeRenderPose = OVR.ovrHmd_GetEyePose(hmdCtx, eyeNum);
                // final float[] eyePos = OVRUtil.getVec3f(eyeRenderPose.getPosition());
                fbos[eyeNum].bind(gl);

                final OVRDistortion.EyeData eyeDist = dist.eyes[eyeNum];
                final int[] viewport = eyeDist.viewport;
                gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

                dist.updateEyePose(eyeNum);
                upstream.reshapeEye(drawable, viewport[0], viewport[1], viewport[2], viewport[3],
                                    dist.getEyeParam(eyeNum), dist.updateEyePose(eyeNum));
                upstream.display(drawable, eyeNum > 0 ? CustomRendererListener.DISPLAY_REPEAT : 0);
                fbos[eyeNum].unbind(gl);
            }
            gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        }

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glActiveTexture(GL.GL_TEXTURE0 + dist.texUnit0.intValue());

        if( null != upstream ) {
            dist.displayOneEyePre(gl, frameTiming.getTimewarpPointSeconds());
            fbos[0].use(gl, fboTexs[0]);
            dist.displayOneEye(gl, 0);
            fbos[0].unuse(gl);
            fbos[1].use(gl, fboTexs[1]);
            dist.displayOneEye(gl, 1);
            fbos[1].unuse(gl);
            dist.displayOneEyePost(gl);
        } else {
            dist.display(gl, frameTiming.getTimewarpPointSeconds());
        }

        if( !drawable.getAutoSwapBufferMode() ) {
            drawable.swapBuffers();
        }
        OVR.ovrHmd_EndFrameTiming(dist.hmdCtx);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        if( !drawable.getAutoSwapBufferMode() ) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            gl.glViewport(0, 0, width, height);
        }
    }
}
