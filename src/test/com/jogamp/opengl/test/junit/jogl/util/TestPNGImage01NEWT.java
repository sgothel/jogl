package com.jogamp.opengl.test.junit.jogl.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;

import org.junit.Assert;
import org.junit.Test;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.texture.spi.PNGImage;

public class TestPNGImage01NEWT extends UITestCase {
    @Test
    public void testPNGReadWriteAndCompare() throws InterruptedException, IOException, MalformedURLException {
        final File out1_f=new File(getSimpleTestName(".")+"-PNGImageTest1.png");
        final File out2_f=new File(getSimpleTestName(".")+"-PNGImageTest2.png");
        final String url_s="jogl/util/data/av/test-ntsc01-160x90.png";
        URLConnection urlConn = IOUtil.getResource(url_s, PNGImage.class.getClassLoader());
        PNGImage image0 = PNGImage.read(urlConn.getInputStream());
        System.err.println("PNGImage - Orig: "+image0);        
        image0.write(out1_f, true); 
        {
            Assert.assertEquals(image0.getData(), PNGImage.read(IOUtil.toURL(out1_f).openStream()).getData());
        }
        
        final PNGImage image1 = PNGImage.createFromData(image0.getWidth(), image0.getHeight(), 
                                                        image0.getDpi()[0], image0.getDpi()[1], 
                                                        image0.getBytesPerPixel(), false, image0.getData());
        image1.write(out2_f, true);
        {
            Assert.assertEquals(image0.getData(), PNGImage.read(IOUtil.toURL(out2_f).openStream()).getData());
        }                
    }
    
    public static void main(String args[]) {
        org.junit.runner.JUnitCore.main(TestPNGImage01NEWT.class.getName());
    }
}
