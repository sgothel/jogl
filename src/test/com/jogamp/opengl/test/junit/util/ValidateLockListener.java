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

package com.jogamp.opengl.test.junit.util;


import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;


public class ValidateLockListener implements GLEventListener {

    private void validateLock(final GLAutoDrawable drawable) {
        final NativeSurface ns = drawable.getNativeSurface();
        ns.getGraphicsConfiguration().getScreen().getDevice().validateLocked();

        final Thread current = Thread.currentThread();
        final Thread owner = ns.getSurfaceLockOwner();
        if( ns.isSurfaceLockedByOtherThread() ) {
            throw new RuntimeException(current.getName()+": Locked by another thread(1), owner "+owner+", "+ns);
        }
        if( current != owner ) {
            if( null == owner ) {
                throw new RuntimeException(current.getName()+": Not locked: "+ns);
            }
            throw new RuntimeException(current.getName()+": Locked by another thread(2), owner "+owner+", "+ns);
        }
    }

    public void init(final GLAutoDrawable drawable) {
        validateLock(drawable);
    }

    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        validateLock(drawable);
    }

    public void display(final GLAutoDrawable drawable) {
        validateLock(drawable);
    }

    public void dispose(final GLAutoDrawable drawable) {
        validateLock(drawable);
    }
}
