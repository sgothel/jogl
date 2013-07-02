package com.jogamp.opengl.test.junit.jogl.util.texture;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.texture.spi.PNGImage;

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
