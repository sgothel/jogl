/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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


import com.jogamp.opengl.*;
import com.jogamp.opengl.JoglVersion;


public class DumpGLInfo implements GLEventListener {
    final String header;
    final boolean withGLAvailability;
    final boolean withCapabilities;
    final boolean withExtensions;

    public DumpGLInfo(final String header, final boolean withGLAvailability, final boolean withCapabilities, final boolean withExtensions) {
        this.header = header;
        this.withGLAvailability = withGLAvailability;
        this.withCapabilities = withCapabilities;
        this.withExtensions = withExtensions;
    }
    public DumpGLInfo() {
        this.header = null;
        this.withGLAvailability = true;
        this.withCapabilities = true;
        this.withExtensions = true;
    }

    public void init(final GLAutoDrawable drawable) {
        final GL gl = drawable.getGL();
        if( null != header ) {
            System.err.println(header);
        }
        System.err.println(JoglVersion.getGLInfo(gl, null, withGLAvailability, withCapabilities, withExtensions));
        System.err.println("Drawable: "+drawable.getDelegatedDrawable().getClass().getSimpleName());
        System.err.println(drawable.getChosenGLCapabilities());
    }

    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
    }

    public void display(final GLAutoDrawable drawable) {
    }

    public void dispose(final GLAutoDrawable drawable) {
    }
}
