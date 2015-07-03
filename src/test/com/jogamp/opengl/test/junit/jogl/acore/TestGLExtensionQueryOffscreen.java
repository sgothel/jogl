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

package com.jogamp.opengl.test.junit.jogl.acore;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLExtensionQueryOffscreen {

    public static void main(final String[] args) {
        final TestGLExtensionQueryOffscreen instance = new TestGLExtensionQueryOffscreen();
        instance.testJogl2ExtensionCheck();
    }

    @Test
    public void testJogl2ExtensionCheck() {
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        final GLDrawableFactory factory = GLDrawableFactory.getFactory(caps.getGLProfile());
        final GLOffscreenAutoDrawable drawable = factory.createOffscreenAutoDrawable(null, caps, null, 256, 256);
        drawable.display(); // trigger context creation ..
        final GLContext context = drawable.getContext();
        context.makeCurrent();
        String extensions;
        try {
            extensions = context.getGL().glGetString(GL.GL_EXTENSIONS);
        } finally {
            context.release();
        }
        final String[] tabExtensions = extensions.split(" ");
        final SortedSet<String> setExtensions = new TreeSet<String>();
        Collections.addAll(setExtensions, tabExtensions);
        System.out.println("DefaultContext: "+context);
        System.out.println("DefaultContext: "+setExtensions);
    }
}

