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
package com.jogamp.opengl.test.junit.jogl.util.texture;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.texture.spi.PNGImage;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPNGImage00NEWT extends UITestCase {
    @Test
    public void testPNGReadWriteAndCompare() throws InterruptedException, IOException, MalformedURLException {
        final File out1_f=new File(getSimpleTestName(".")+"-PNGImageTest1.png");
        final File out2_f=new File(getSimpleTestName(".")+"-PNGImageTest2.png");
        final File out2F_f=new File(getSimpleTestName(".")+"-PNGImageTest2Flipped.png");
        final File out2R_f=new File(getSimpleTestName(".")+"-PNGImageTest2Reversed.png");
        final File out2RF_f=new File(getSimpleTestName(".")+"-PNGImageTest2ReversedFlipped.png");
        final String url_s="jogl/util/data/av/test-ntsc01-160x90.png";
        URLConnection urlConn = IOUtil.getResource(url_s, this.getClass().getClassLoader());
        PNGImage image1 = PNGImage.read(urlConn.getInputStream());
        System.err.println("PNGImage - Orig: "+image1);        
        image1.write(out1_f, true); 
        {
            Assert.assertEquals(image1.getData(), PNGImage.read(out1_f.toURI().toURL().openStream()).getData());
        }
        
        final PNGImage image2 = PNGImage.createFromData(image1.getWidth(), image1.getHeight(), 
                                                        image1.getDpi()[0], image1.getDpi()[1], 
                                                        image1.getBytesPerPixel(), false /* reverseChannels */, image1.isGLOriented(), image1.getData());
        image2.write(out2_f, true);
        {
            Assert.assertEquals(image1.getData(), PNGImage.read(out2_f.toURI().toURL().openStream()).getData());
        }
        
        // flipped
        final PNGImage image2F = PNGImage.createFromData(image1.getWidth(), image1.getHeight(), 
                                                         image1.getDpi()[0], image1.getDpi()[1], 
                                                         image1.getBytesPerPixel(), false /* reverseChannels */, !image1.isGLOriented(), image1.getData());
        image2F.write(out2F_f, true);
        
        // reversed channels
        final PNGImage image2R = PNGImage.createFromData(image1.getWidth(), image1.getHeight(), 
                                                         image1.getDpi()[0], image1.getDpi()[1], 
                                                         image1.getBytesPerPixel(), true /* reverseChannels */, image1.isGLOriented(), image1.getData());
        image2R.write(out2R_f, true);
        
        // reversed channels and flipped
        final PNGImage image2RF = PNGImage.createFromData(image1.getWidth(), image1.getHeight(), 
                                                         image1.getDpi()[0], image1.getDpi()[1], 
                                                         image1.getBytesPerPixel(), true /* reverseChannels */, !image1.isGLOriented(), image1.getData());
        image2RF.write(out2RF_f, true);
    }
    
    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestPNGImage00NEWT.class.getName());
    }
}
