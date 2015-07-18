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
package com.jogamp.opengl.util.stereo;

import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

import jogamp.opengl.GLDrawableHelper;
import jogamp.opengl.GLDrawableHelper.GLEventListenerAction;

import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.FBObject.Attachment.Type;
import com.jogamp.opengl.util.CustomGLEventListener;

/**
 * {@link StereoClientRenderer} utilizing {@link StereoDeviceRenderer}
 * implementing {@link GLEventListener} for convenience.
 * <p>
 * See {@link StereoDeviceRenderer} notes about <a href="StereoDeviceRenderer.html#asymFOVRendering">Correct Asymmetric FOV Rendering</a>.
 * <p>
 * Implementation renders {@link StereoGLEventListener}
 * using one or more {@link FBObject} according to {@link StereoDeviceRenderer#getTextureCount()}.
 * </p>
 */
public class StereoClientRenderer implements GLEventListener {
    private final GLDrawableHelper helper;
    private final StereoDeviceRenderer deviceRenderer;
    private final boolean ownsDevice;
    private final FBObject[] fbos;
    private final int magFilter;
    private final int minFilter;

    private int numSamples;
    private final TextureAttachment[] fboTexs;

    public StereoClientRenderer(final StereoDeviceRenderer deviceRenderer, final boolean ownsDevice,
                                final int magFilter, final int minFilter, final int numSamples) {
        final int fboCount = deviceRenderer.getTextureCount();
        if( 0 > fboCount || 2 < fboCount ) {
            throw new IllegalArgumentException("fboCount must be within [0..2], has "+fboCount+", due to "+deviceRenderer);
        }
        this.helper = new GLDrawableHelper();
        this.deviceRenderer = deviceRenderer;
        this.ownsDevice = ownsDevice;
        this.magFilter = magFilter;
        this.minFilter = minFilter;

        this.numSamples = numSamples;

        this.fbos = new FBObject[fboCount];
        for(int i=0; i<fboCount; i++) {
            this.fbos[i] = new FBObject();
        }
        this.fboTexs = new TextureAttachment[fboCount];
    }

    private void initFBOs(final GL gl, final DimensionImmutable[] sizes) {
        for(int i=0; i<fbos.length; i++) {
            fbos[i].init(gl, sizes[i].getWidth(), sizes[i].getHeight(), numSamples);
            if( i>0 && fbos[i-1].getNumSamples() != fbos[i].getNumSamples()) {
                throw new InternalError("sample size mismatch: \n\t0: "+fbos[i-1]+"\n\t1: "+fbos[i]);
            }
            numSamples = fbos[i].getNumSamples();

            if(numSamples>0) {
                fbos[i].attachColorbuffer(gl, 0, true); // MSAA requires alpha
                fbos[i].attachRenderbuffer(gl, Type.DEPTH, FBObject.DEFAULT_BITS);
                final FBObject ssink = new FBObject();
                {
                    ssink.init(gl, sizes[i].getWidth(), sizes[i].getHeight(), 0);
                    ssink.attachTexture2D(gl, 0, false, magFilter, minFilter, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
                    ssink.attachRenderbuffer(gl, Attachment.Type.DEPTH, FBObject.DEFAULT_BITS);
                }
                fbos[i].setSamplingSink(ssink);
                fbos[i].resetSamplingSink(gl); // validate
                fboTexs[i] = fbos[i].getSamplingSink().getTextureAttachment();
            } else {
                fboTexs[i] = fbos[i].attachTexture2D(gl, 0, false, magFilter, minFilter, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
                fbos[i].attachRenderbuffer(gl, Type.DEPTH, FBObject.DEFAULT_BITS);
            }
            fbos[i].unbind(gl);
            System.err.println("FBO["+i+"]: "+fbos[i]);
        }

    }

    @SuppressWarnings("unused")
    private void resetFBOs(final GL gl, final DimensionImmutable size) {
        for(int i=0; i<fbos.length; i++) {
            fbos[i].reset(gl, size.getWidth(), size.getHeight(), numSamples);
            if( i>0 && fbos[i-1].getNumSamples() != fbos[i].getNumSamples()) {
                throw new InternalError("sample size mismatch: \n\t0: "+fbos[i-1]+"\n\t1: "+fbos[i]);
            }
            numSamples = fbos[i].getNumSamples();
            if(numSamples>0) {
                fboTexs[i] = fbos[i].getSamplingSink().getTextureAttachment();
            } else {
                fboTexs[i] = fbos[i].getColorbuffer(0).getTextureAttachment();
            }
        }
    }

    public final StereoDeviceRenderer getStereoDeviceRenderer() { return deviceRenderer; }

    public final void addGLEventListener(final StereoGLEventListener l) {
        helper.addGLEventListener(l);
    }
    public final void removeGLEventListener(final StereoGLEventListener l) {
        helper.removeGLEventListener(l);
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        deviceRenderer.init(gl);

        // We will do some offscreen rendering, setup FBO...
        final DimensionImmutable[] textureSize = deviceRenderer.getTextureCount() > 1 ?
                                                 deviceRenderer.getEyeSurfaceSize() :
                                                 new DimensionImmutable[] { deviceRenderer.getTotalSurfaceSize() };
        initFBOs(gl, textureSize);
        helper.init(drawable, false);

        gl.setSwapInterval(1);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        helper.disposeAllGLEventListener(drawable, false);
        for(int i=0; i<fbos.length; i++) {
            fbos[i].destroy(gl);
            fboTexs[i] = null;
        }
        if( ownsDevice ) {
            deviceRenderer.dispose(gl);
        }
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        deviceRenderer.beginFrame(gl);

        if(0 < numSamples) {
            gl.glEnable(GL.GL_MULTISAMPLE);
        }

        final int fboCount = fbos.length;
        final int displayRepeatFlags;
        if( 1 >= fboCount ) {
            displayRepeatFlags = CustomGLEventListener.DISPLAY_DONTCLEAR;
        } else {
            displayRepeatFlags = 0;
        }

        final int[] eyeOrder = deviceRenderer.getDevice().getEyeRenderOrder();
        final int eyeCount = eyeOrder.length;

        final ViewerPose viewerPose = deviceRenderer.updateViewerPose();

        if( 1 == fboCount ) {
            fbos[0].bind(gl);
        }

        for(int eyeNum=0; eyeNum<eyeCount; eyeNum++) {
            final int eyeName = eyeOrder[eyeNum];
            if( 1 < fboCount ) {
                fbos[eyeName].bind(gl);
            }

            final StereoDeviceRenderer.Eye eye = deviceRenderer.getEye(eyeName);
            final RectangleImmutable viewport = eye.getViewport();
            gl.glViewport(viewport.getX(), viewport.getY(), viewport.getWidth(), viewport.getHeight());

            final int displayFlags = eyeNum > 0 ? CustomGLEventListener.DISPLAY_REPEAT | displayRepeatFlags : 0;
            final GLEventListenerAction reshapeDisplayAction = new GLEventListenerAction() {
                public void run(final GLAutoDrawable drawable, final GLEventListener listener) {
                    final StereoGLEventListener sl = (StereoGLEventListener) listener;
                    sl.reshapeForEye(drawable, viewport.getX(), viewport.getY(), viewport.getWidth(), viewport.getHeight(),
                                     eye.getEyeParameter(), viewerPose);
                    sl.display(drawable, displayFlags);
                }  };
            helper.runForAllGLEventListener(drawable, reshapeDisplayAction);

            if( 1 < fboCount ) {
                fbos[eyeName].unbind(gl);
            }
        }

        if( 1 == fboCount ) {
            fbos[0].unbind(gl);
        }
        // restore viewport
        gl.glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());

        if( deviceRenderer.ppAvailable() ) {
            deviceRenderer.ppBegin(gl);
            if( 1 == fboCount ) {
                fbos[0].use(gl, fboTexs[0]);
                for(int eyeNum=0; eyeNum<eyeCount; eyeNum++) {
                    deviceRenderer.ppOneEye(gl, eyeOrder[eyeNum]);
                }
                fbos[0].unuse(gl);
            } else {
                for(int eyeNum=0; eyeNum<eyeCount; eyeNum++) {
                    final int eyeName = eyeOrder[eyeNum];
                    fbos[eyeName].use(gl, fboTexs[eyeName]);
                    deviceRenderer.ppOneEye(gl, eyeName);
                    fbos[eyeName].unuse(gl);
                }
            }
            deviceRenderer.ppEnd(gl);
        }

        if( !drawable.getAutoSwapBufferMode() ) {
            drawable.swapBuffers();
        }
        deviceRenderer.endFrame(gl);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        if( !drawable.getAutoSwapBufferMode() ) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            gl.glViewport(0, 0, width, height);
        }
    }
}
