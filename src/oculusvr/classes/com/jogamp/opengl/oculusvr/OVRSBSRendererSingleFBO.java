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
import javax.media.opengl.GLEventListener2;

import jogamp.opengl.oculusvr.OVRDistortion;

import com.jogamp.oculusvr.OVR;
import com.jogamp.oculusvr.ovrFrameTiming;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.math.FloatUtil;

/**
 * OculusVR (OVR) <i>Side By Side</i> Distortion Renderer utilizing {@link OVRDistortion}
 * implementing {@link GLEventListener} for convenience.
 * <p>
 * Implementation renders an {@link GLEventListener2} instance
 * side-by-side within one {@link FBObject} according to {@link OVRDistortion}.
 * </p>
 */
public class OVRSBSRendererSingleFBO implements GLEventListener {

    private final float[] mat4Projection = new float[16];
    private final float[] mat4Modelview = new float[16];
    private final OVRDistortion dist;
    private final boolean ownsDist;
    private final GLEventListener2 upstream;
    private final FBObject fbo0;

    // final float[] eyePos = { 0.0f, 1.6f, -5.0f };
    private final float[] eyePos = { 0.0f, 0.0f, -20.0f };
    // EyePos.y = ovrHmd_GetFloat(HMD, OVR_KEY_EYE_HEIGHT, EyePos.y);
    private float eyeYaw = FloatUtil.PI; // 180 degrees in radians
    private float frustumNear = 0.1f;
    private float frustumFar = 7000f;
    private int numSamples;
    private TextureAttachment fbo0Tex;

    public OVRSBSRendererSingleFBO(final OVRDistortion dist, final boolean ownsDist, final GLEventListener2 upstream, final int numSamples) {
        this.dist = dist;
        this.ownsDist = ownsDist;
        this.upstream = upstream;
        this.numSamples = numSamples;
        fbo0 = new FBObject();
    }

    /**
     *
     * @param eyePos
     * @param eyeYaw
     * @param frustumNear
     * @param frustumFar
     */
    public void setUpstreamPMVParams(final float[] eyePos, final float eyeYaw, final float frustumNear, final float frustumFar) {
        System.arraycopy(eyePos, 0, this.eyePos, 0, 3);
        this.eyeYaw = eyeYaw;
        this.frustumNear = frustumNear;
        this.frustumFar = frustumFar;
    }

    private void initFBOs(final GL gl, final int width, final int height) {
        // remove all texture attachments, since MSAA uses just color-render-buffer
        // and non-MSAA uses texture2d-buffer
        fbo0.detachAllColorbuffer(gl);

        fbo0.reset(gl, width, height, numSamples, false);
        numSamples = fbo0.getNumSamples();

        if(numSamples>0) {
            fbo0.attachColorbuffer(gl, 0, true);
            fbo0.resetSamplingSink(gl);
            fbo0Tex = fbo0.getSamplingSink();
        } else {
            fbo0Tex = fbo0.attachTexture2D(gl, 0, true);
        }
        numSamples=fbo0.getNumSamples();
        fbo0.attachRenderbuffer(gl, Type.DEPTH, 24);
        fbo0.unbind(gl);
    }

    @SuppressWarnings("unused")
    private void resetFBOs(final GL gl, final int width, final int height) {
        fbo0.reset(gl, width, height, numSamples, true);
        numSamples = fbo0.getNumSamples();
        if(numSamples>0) {
            fbo0Tex = fbo0.getSamplingSink();
        } else {
            fbo0Tex = (TextureAttachment) fbo0.getColorbuffer(0);
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
            fbo0.destroy(gl);
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
            fbo0.bind(gl);

            for(int eyeNum=0; eyeNum<2; eyeNum++) {
                // final ovrPosef eyeRenderPose = OVR.ovrHmd_GetEyePose(hmdCtx, eyeNum);
                // final float[] eyePos = OVRUtil.getVec3f(eyeRenderPose.getPosition());
                final OVRDistortion.EyeData eyeDist = dist.eyes[eyeNum];
                final int[] viewport = eyeDist.viewport;
                gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

                dist.getSBSUpstreamPMV(eyeNum, eyePos, eyeYaw, frustumNear, frustumFar, mat4Projection, mat4Modelview);
                upstream.setProjectionModelview(drawable, mat4Projection, mat4Modelview);
                upstream.display(drawable, eyeNum > 0 ? GLEventListener2.DISPLAY_REPEAT | GLEventListener2.DISPLAY_DONTCLEAR : 0);
            }
            fbo0.unbind(gl);
            gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        }

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glClear(GL.GL_COLOR_BUFFER_BIT);
        gl.glActiveTexture(GL.GL_TEXTURE0 + dist.texUnit0.intValue());

        if( null != upstream ) {
            fbo0.use(gl, fbo0Tex);
            dist.display(gl, frameTiming.getTimewarpPointSeconds());
            fbo0.unuse(gl);
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
