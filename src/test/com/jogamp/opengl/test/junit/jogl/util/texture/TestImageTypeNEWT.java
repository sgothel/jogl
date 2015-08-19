/**
 * Copyright 2015 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.util.texture;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.texture.ImageType;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestImageTypeNEWT extends UITestCase {
    ImageTstFiles imageTstFiles;

    @Before
    public void initTest() throws IOException {
        imageTstFiles = new ImageTstFiles();
        imageTstFiles.init();
    }

    @After
    public void cleanupTest() {
        imageTstFiles.clear();
    }

    public void testImpl(final List<ImageTstFiles.NamedInputStream> streams, final ImageType expImageType) throws InterruptedException, IOException {
        for(int i=0; i<streams.size(); i++) {
            final ImageTstFiles.NamedInputStream s = streams.get(i);
            final ImageType t = new ImageType(s.stream);
            System.err.printf("Test %3d: path %s, exp-type %s, has-type %s%n", i, s.basePath, expImageType, t);
            Assert.assertEquals(expImageType, t);
        }
    }

    @Test
    public void test01AllPNG() throws InterruptedException, IOException {
        testImpl(imageTstFiles.pngStreams, new ImageType(ImageType.T_PNG));
    }

    @Test
    public void test02AllJPG() throws InterruptedException, IOException {
        testImpl(imageTstFiles.jpgStreams, new ImageType(ImageType.T_JPG));
    }

    // TGA cannot be detected
    // @Test
    public void test03AllTGA() throws InterruptedException, IOException {
        testImpl(imageTstFiles.tgaStreams, new ImageType(ImageType.T_TGA));
    }

    @Test
    public void test04AllDDS() throws InterruptedException, IOException {
        testImpl(imageTstFiles.ddsStreams, new ImageType(ImageType.T_DDS));
    }

    public static void main(final String args[]) throws IOException {
        org.junit.runner.JUnitCore.main(TestImageTypeNEWT.class.getName());
    }
}
