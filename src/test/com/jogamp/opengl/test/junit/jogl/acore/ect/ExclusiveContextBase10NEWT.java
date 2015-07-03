/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.acore.ect;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilitiesImmutable;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * ExclusiveContextThread base implementation to test performance impact of the ExclusiveContext feature with AnimatorBase and NEWT.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class ExclusiveContextBase10NEWT extends ExclusiveContextBase10 {

    static Display dpy;
    static Screen screen;

    @BeforeClass
    public static void initClass00NEWT() {
        dpy = NewtFactory.createDisplay(null);
        screen = NewtFactory.createScreen(dpy, 0);
    }

    @AfterClass
    public static void releaseClass00NEWT() {
        screen = null;
        dpy = null;
    }

    @Override
    protected boolean isAWTTestCase() { return false; }

    @Override
    protected Thread getAWTRenderThread() {
        return null;
    }

    @Override
    protected GLAutoDrawable createGLAutoDrawable(final String title, final int x, final int y, final int width, final int height, final GLCapabilitiesImmutable caps) {
        final GLWindow glWindow = GLWindow.create(screen, caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle(title);
        glWindow.setSize(width, height);
        glWindow.setPosition(x, y);
        return glWindow;
    }

    @Override
    protected void setGLAutoDrawableVisible(final GLAutoDrawable[] glads) {
        final int count = glads.length;
        for(int i=0; i<count; i++) {
            final GLAutoDrawable glad = glads[i];
            ((GLWindow)glad).setVisible(true);
        }
    }

    @Override
    protected void destroyGLAutoDrawableVisible(final GLAutoDrawable glad) {
        ((GLWindow)glad).destroy();
    }
}
