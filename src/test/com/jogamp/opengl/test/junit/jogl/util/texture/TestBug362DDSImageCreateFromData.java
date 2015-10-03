package com.jogamp.opengl.test.junit.jogl.util.texture;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.texture.spi.DDSImage;
import com.jogamp.opengl.util.texture.spi.DDSImage.ImageInfo;

/**
 * This test uses the DDSImage class to read a dds image from file, extract the data,
 * and use the class to create a new DDSImage from the extracted data
 * <br></br>
 * Bug Reference: https://jogamp.org/bugzilla/show_bug.cgi?id=362
 * <br></br>
 * The bug pertains to incorrect size calculation for checking validity of data. Compressed DXT1 has min of 8 bytes, DXT5 has min of 16 bytes.
 * It exists in {@link DDSImage#createFromData(int, int, int, ByteBuffer[])}
 * where an {@link IllegalArgumentException} is thrown for Mipmap level size mismatch.
 * <br></br>
 * <ul>The following cases are tested:
 * <li>Uncompressed 64x32 RGB DDS Image with all mipmap levels (64x32 --> 1x1)</li>
 * <li>DXT1 compressed 64x32 RGB DDS Image with all mipmap levels (64x32 --> 1x1)</li>
 * <li>DXT5 compressed 64x32 RGB DDS Image with all mipmap levels (64x32 --> 1x1)</li>
 * </ul>
 *
 * @author Michael Esemplare
 *
 */
public class TestBug362DDSImageCreateFromData {

	File testDDSImage01Uncompressed;
	File testDDSImage02DXT1;
	File testDDSImage03DXT5;

	@Before
    public void setup() throws Throwable {
		testDDSImage01Uncompressed = initFile("test-64x32_uncompressed.dds");
		testDDSImage02DXT1 = initFile("test-64x32_DXT1.dds");
		testDDSImage03DXT5 = initFile("test-64x32_DXT5.dds");
    }

    @After
    public void teardown() {
    	testDDSImage01Uncompressed = null;
		testDDSImage02DXT1 = null;
		testDDSImage03DXT5 = null;
    }

    private File initFile(final String filename) throws URISyntaxException {
    	final URLConnection connection = IOUtil.getResource(filename, getClass().getClassLoader(), getClass());
    	Assert.assertNotNull(connection);
    	final URL url = connection.getURL();
    	final File file = new File(url.toURI());
    	Assert.assertTrue(file.exists());
    	return file;
    }

    private void testImpl(final File file) throws IOException {
    	final DDSImage ddsImage = DDSImage.read(file);
    	Assert.assertNotNull(ddsImage);
    	final int numMipMaps = ddsImage.getNumMipMaps();
		final ByteBuffer[] mipMapArray = new ByteBuffer[numMipMaps];
		for (int i=0;i<numMipMaps;i++){
			final ImageInfo info = ddsImage.getMipMap(i);
			mipMapArray[i] = info.getData();
		}
		final DDSImage newImage = DDSImage.createFromData(ddsImage.getPixelFormat(), ddsImage.getWidth(), ddsImage.getHeight(), mipMapArray);
		Assert.assertNotNull(newImage);
    }

    @Test
    public void test00_DDSImage_CreateFromData_Uncompressed_RGB () throws IOException {
    	testImpl(testDDSImage01Uncompressed);
    }

    @Test
    public void test01_DDSImage_CreateFromData_DXT1_RGB () throws IOException {
    	testImpl(testDDSImage02DXT1);
    }

    @Test
    public void test02_DDSImage_CreateFromData_DXT5_RGB () throws IOException {
    	testImpl(testDDSImage03DXT5);
    }

	public static void main(final String[] args) {
		org.junit.runner.JUnitCore.main(TestBug362DDSImageCreateFromData.class.getName());
	}
}