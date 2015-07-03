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

package com.jogamp.opengl.test.junit.jogl.util;

import java.io.IOException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

/**
 * Testing the ImmModeSink w/ GL2ES1 context
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestImmModeSinkES1NEWT extends UITestCase {
    static int duration = 100;
    static final int iWidth = 400;
    static final int iHeight = 400;

    static GLCapabilities getCaps(final String profile) {
        if( !GLProfile.isAvailable(profile) )  {
            System.err.println("Profile "+profile+" n/a");
            return null;
        }
        return new GLCapabilities(GLProfile.get(profile));
    }

    void doTest(final GLCapabilitiesImmutable reqGLCaps, final GLEventListener demo) throws InterruptedException {
        System.out.println("Requested  GL Caps: "+reqGLCaps);

        //
        // Create native windowing resources .. X11/Win/OSX
        //
        final GLWindow glad = GLWindow.create(reqGLCaps);
        glad.addGLEventListener(demo);

        final SnapshotGLEventListener snapshotGLEventListener = new SnapshotGLEventListener();
        glad.addGLEventListener(snapshotGLEventListener);
        glad.setSize(iWidth, iHeight);
        glad.setVisible(true);

        snapshotGLEventListener.setMakeSnapshot();
        glad.display(); // initial resize/display

        Thread.sleep(duration);

        glad.destroy();
    }

    @Test
    public void test01Plain__GL2ES1_VBOOffUsePlain() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1Plain(false, false));
    }

    @Test
    public void test02Plain__GL2ES1_VBOOffUseArrayData() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1Plain(true, false));
    }

    @Test
    public void test03Plain__GL2ES1_VBOOnUsePlain() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1Plain(false, true));
    }

    @Test
    public void test04Plain__GL2ES1_VBOOnUseArrayData() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1Plain(true, true));
    }

    @Test
    public void test05ImmSinkGL2ES1_VBOOff() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1ImmModeSink(false));
    }

    @Test
    public void test06ImmSinkGL2ES1_VBOOn() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1ImmModeSink(true));
    }

    @Test
    public void test07ImmSinkGL2ES1_VBOOnTexture() throws InterruptedException {
        final GLCapabilities reqGLCaps = getCaps(GLProfile.GL2ES1);
        if(null == reqGLCaps) return;
        doTest(reqGLCaps, new DemoGL2ES1TextureImmModeSink());
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = MiscUtils.atoi(args[++i], duration);
            }
        }
        org.junit.runner.JUnitCore.main(TestImmModeSinkES1NEWT.class.getName());
    }

}
