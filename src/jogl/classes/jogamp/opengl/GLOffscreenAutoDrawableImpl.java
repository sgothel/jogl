/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import javax.media.nativewindow.NativeWindowException;
import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLOffscreenAutoDrawable;

import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.GLAutoDrawableDelegate;

import jogamp.opengl.GLFBODrawableImpl;

public class GLOffscreenAutoDrawableImpl extends GLAutoDrawableDelegate implements GLOffscreenAutoDrawable {

    /**
     * @param drawable a valid {@link GLDrawable}, may not be {@link GLDrawable#isRealized() realized} yet.
     * @param context a valid {@link GLContext},
     *                may not have been made current (created) yet,
     *                may not be associated w/ <code>drawable<code> yet,
     *                may be <code>null</code> for lazy initialization at 1st {@link #display()}.
     * @param upstreamWidget optional UI element holding this instance, see {@link #getUpstreamWidget()}.
     * @param lock optional upstream lock, may be null
     */
    public GLOffscreenAutoDrawableImpl(GLDrawable drawable, GLContext context, Object upstreamWidget, RecursiveLock lock) {
        super(drawable, context, upstreamWidget, true, lock);
    }

    @Override
    public void setSize(int newWidth, int newHeight) throws NativeWindowException, GLException {
        this.defaultWindowResizedOp(newWidth, newHeight);
    }

    public static class FBOImpl extends GLOffscreenAutoDrawableImpl implements GLOffscreenAutoDrawable.FBO {
        /**
         * @param drawable a valid {@link GLDrawable}, may not be {@link GLDrawable#isRealized() realized} yet.
         * @param context a valid {@link GLContext},
         *                may not have been made current (created) yet,
         *                may not be associated w/ <code>drawable<code> yet,
         *                may be <code>null</code> for lazy initialization
         * @param upstreamWidget optional UI element holding this instance, see {@link #getUpstreamWidget()}.
         * @param lock optional upstream lock, may be null
         */
        public FBOImpl(GLFBODrawableImpl drawable, GLContext context, Object upstreamWidget, RecursiveLock lock) {
            super(drawable, context, upstreamWidget, lock);
        }

        @Override
        public boolean isInitialized() {
            return ((GLFBODrawableImpl)drawable).isInitialized();
        }

        @Override
        public final int getTextureUnit() {
            return ((GLFBODrawableImpl)drawable).getTextureUnit();
        }

        @Override
        public final void setTextureUnit(int unit) {
            ((GLFBODrawableImpl)drawable).setTextureUnit(unit);
        }

        @Override
        public final int getNumSamples() {
            return ((GLFBODrawableImpl)drawable).getNumSamples();
        }

        @Override
        public final void setNumSamples(GL gl, int newSamples) throws GLException {
            ((GLFBODrawableImpl)drawable).setNumSamples(gl, newSamples);
            windowRepaintOp();
        }

        @Override
        public final int setNumBuffers(int bufferCount) throws GLException {
            return ((GLFBODrawableImpl)drawable).setNumBuffers(bufferCount);
        }

        @Override
        public final int getNumBuffers() {
            return ((GLFBODrawableImpl)drawable).getNumBuffers();
        }

        /** // TODO: Add or remove TEXTURE (only) DoubleBufferMode support
        @Override
        public DoubleBufferMode getDoubleBufferMode() {
            return ((GLFBODrawableImpl)drawable).getDoubleBufferMode();
        }

        @Override
        public void setDoubleBufferMode(DoubleBufferMode mode) throws GLException {
            ((GLFBODrawableImpl)drawable).setDoubleBufferMode(mode);
        } */

        @Override
        public final FBObject getFBObject(int bufferName) {
            return ((GLFBODrawableImpl)drawable).getFBObject(bufferName);
        }

        @Override
        public final FBObject.TextureAttachment getTextureBuffer(int bufferName) {
            return ((GLFBODrawableImpl)drawable).getTextureBuffer(bufferName);
        }

        @Override
        public void resetSize(GL gl) throws GLException {
            ((GLFBODrawableImpl)drawable).resetSize(gl);
        }
    }
}
