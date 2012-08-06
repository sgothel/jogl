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

package com.jogamp.opengl;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.GLAutoDrawableDelegate;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;

import jogamp.opengl.GLFBODrawableImpl;

/** 
 * Platform-independent class exposing FBO offscreen functionality to
 * applications.
 * <p>
 * This class distinguishes itself from {@link GLAutoDrawableDelegate}
 * with it's {@link #setSize(int, int)} functionality.
 * </p>
 */
public class OffscreenAutoDrawable extends GLAutoDrawableDelegate {
  
    /**
     * @param drawable a valid {@link GLDrawable}, may not be realized yet.
     * @param context a valid {@link GLContext}, may not be made current (created) yet.
     * @param ownDevice pass <code>true</code> if {@link AbstractGraphicsDevice#close()} shall be issued,
     *                  otherwise pass <code>false</code>. Closing the device is required in case
     *                  the drawable is created w/ it's own new instance, e.g. offscreen drawables,
     *                  and no further lifecycle handling is applied.
     */
    public OffscreenAutoDrawable(GLDrawable drawable, GLContext context, boolean ownDevice) {
        super(drawable, context, null, ownDevice);
    }

    /**
     * Attempts to resize this offscreen auto drawable, if supported
     * by the underlying {@link GLDrawable).
     * @param newWidth
     * @param newHeight
     * @return <code>true</code> if resize was executed, otherwise <code>false</code>.
     * @throws GLException in case of an error during the resize operation
     */
    public boolean setSize(int newWidth, int newHeight) throws GLException {
        boolean done = false;
        if(drawable instanceof GLFBODrawableImpl) {
            context.makeCurrent();
            try {                        
                ((GLFBODrawableImpl)drawable).setSize(context.getGL(), newWidth, newHeight);
                done = true;
            } finally {
                context.release();
            }
        }
        if(done) {
            this.defaultWindowResizedOp();
            return true;
        }
        return false;
    }
    
    /**
     * If the underlying {@link GLDrawable} is an FBO implementation
     * and contains an {#link FBObject}, the same is returned.
     * Otherwise returns <code>null</code>.
     */
    public FBObject getFBObject() { 
        if(drawable instanceof GLFBODrawableImpl) {
            return ((GLFBODrawableImpl)drawable).getFBObject();
        }
        return null;
    }
}
