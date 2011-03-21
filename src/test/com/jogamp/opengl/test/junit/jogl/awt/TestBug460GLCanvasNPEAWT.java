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
 
package com.jogamp.opengl.test.junit.jogl.awt;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import org.junit.Test;

public class TestBug460GLCanvasNPEAWT {

    public static void main(String[] args) {
        TestBug460GLCanvasNPEAWT instance = new TestBug460GLCanvasNPEAWT();
        instance.testJogl2ExtensionCheck();
    }

    @Test
    public void testJogl2ExtensionCheck() {
        GLProfile.initSingleton(false);
//      GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        GLCanvas glc = new GLCanvas(caps);
        GLDrawableFactory usine = glc.getFactory();
        GLCapabilitiesImmutable glci = glc.getChosenGLCapabilities();
        GLCapabilitiesChooser glcc = new DefaultGLCapabilitiesChooser();
        AbstractGraphicsDevice agd = usine.getDefaultDevice();
        
        GLPbuffer pbuffer = usine.createGLPbuffer(agd, glci, glcc, 256, 256, null);
        GLContext context = pbuffer.getContext();
        context.makeCurrent();
        GL2 gl = pbuffer.getContext().getGL().getGL2();
        
        String extensions = gl.glGetString(GL.GL_EXTENSIONS);
        String[] tabExtensions = extensions.split(" ");
        SortedSet<String> setExtensions = new TreeSet<String>();
        Collections.addAll(setExtensions, tabExtensions);
        System.out.println(setExtensions);
    }
}

