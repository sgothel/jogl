package com.jogamp.opengl.test.junit.jogl.glu;

import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLOffscreenAutoDrawable;
import javax.media.opengl.GLProfile;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import jogamp.opengl.glu.mipmap.Mipmap;
import jogamp.opengl.glu.mipmap.ScaleInternal;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * This test creates a {@link Texture} from {@link TextureData} of various pixel formats
 * and pixel types with auto generate mipmaps set to {@code true}.
 * <br></br>
 * Bug Reference: https://jogamp.org/bugzilla/show_bug.cgi?id=365
 * <br></br>
 * The bug pertains to mipmap generation from a Texture and exists in {@link ScaleInternal}
 * where a {@link java.nio.BufferUnderflowException} is thrown.
 * <br></br>
 * <ul>This suite of test cases test:
 * <li>{@link ScaleInternal#scale_internal_ubyte(int, int, int, ByteBuffer, int, int, ByteBuffer, int, int, int)}</li>
 * <li>{@link ScaleInternal#scale_internal_byte(int, int, int, ByteBuffer, int, int, ByteBuffer, int, int, int)}</li>
 * <li>{@link ScaleInternal#scale_internal_ushort(int, int, int, ByteBuffer, int, int, java.nio.ShortBuffer, int, int, int, boolean)}</li>
 * <li>{@link ScaleInternal#scale_internal_short(int, int, int, ByteBuffer, int, int, java.nio.ShortBuffer, int, int, int, boolean)}</li>
 * <li>{@link ScaleInternal#scale_internal_uint(int, int, int, ByteBuffer, int, int, java.nio.IntBuffer, int, int, int, boolean)}</li>
 * <li>{@link ScaleInternal#scale_internal_int(int, int, int, ByteBuffer, int, int, java.nio.IntBuffer, int, int, int, boolean)}</li>
 * <li>{@link ScaleInternal#scale_internal_float(int, int, int, ByteBuffer, int, int, java.nio.FloatBuffer, int, int, int, boolean)}</li>
 * </ul>
 *
 * @author Michael Esemplare, et.al.
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug365TextureGenerateMipMaps extends UITestCase {
	static GLOffscreenAutoDrawable drawable;

	@BeforeClass
    public static void setup() throws Throwable {
		// disableNPOT
		System.setProperty("jogl.texture.nonpot", "true");
        try {
        	setUpOffscreenAutoDrawable();
        } catch (final Throwable t) {
            throw t;
        }
    }

    @AfterClass
    public static void teardown() {
        tearDownOffscreenAutoDrawable();
    }

    private static void setUpOffscreenAutoDrawable() throws Throwable {
    	final GLProfile glp = GLProfile.getDefault();
		final GLCapabilities caps = new GLCapabilities(glp);

		final GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);

		// Make a drawable to get an offscreen context
	    drawable = factory.createOffscreenAutoDrawable(null, caps, null, 2, 2);
	    drawable.display(); // trigger context creation
	    final GLContext glContext = drawable.getContext();
	    try {
	    	Assert.assertTrue("Could not make context current", GLContext.CONTEXT_NOT_CURRENT < glContext.makeCurrent());
	    } catch (final Throwable t) {
	    	tearDownOffscreenAutoDrawable();
            throw t;
        }
    }

    private static void tearDownOffscreenAutoDrawable() {
        if(drawable != null) {
        	drawable.getContext().release();
        	drawable.destroy();
	    	drawable = null;
        }
    }

    private static void testTextureMipMapGeneration(final int width, final int height, final int pixelFormat, final int pixelType) {
		final int internalFormat = pixelFormat;
		final int border = 0;
		final boolean mipmap = true;
		final boolean dataIsCompressed = false;
		final boolean mustFlipVertically = false;

		final int memReq = Mipmap.image_size( width, height, pixelFormat, pixelType );
		ByteBuffer buffer = Buffers.newDirectByteBuffer( memReq );

		final TextureData data = new TextureData(drawable.getGLProfile(),
				internalFormat,
				width,
				height,
				border,
				pixelFormat,
				pixelType,
				mipmap,
				dataIsCompressed,
				mustFlipVertically,
				buffer,
				null);

		final Texture texture = TextureIO.newTexture(drawable.getGL(), data);
		// Cleanup
		texture.destroy(drawable.getGL());
		data.destroy();
		buffer.clear();
		buffer = null;
    }

    @Test
    public void test00_MipMap_ScaleInternal_RGB_UBYTE () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGB;
    	final int pixelType = GL.GL_UNSIGNED_BYTE;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test01_MipMap_ScaleInternal_RGBA_UBYTE () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGBA;
    	final int pixelType = GL.GL_UNSIGNED_BYTE;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test02_MipMap_ScaleInternal_RGB_BYTE () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGB;
    	final int pixelType = GL.GL_BYTE;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test03_MipMap_ScaleInternal_RGBA_BYTE () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGBA;
    	final int pixelType = GL.GL_BYTE;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test04_MipMap_ScaleInternal_RGB_USHORT () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGB;
    	final int pixelType = GL.GL_UNSIGNED_SHORT;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test05_MipMap_ScaleInternal_RGBA_USHORT () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGBA;
    	final int pixelType = GL.GL_UNSIGNED_SHORT;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test06_MipMap_ScaleInternal_RGB_SHORT () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGB;
    	final int pixelType = GL.GL_SHORT;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test07_MipMap_ScaleInternal_RGBA_SHORT () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGBA;
    	final int pixelType = GL.GL_SHORT;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test08_MipMap_ScaleInternal_RGB_UINT () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGB;
    	final int pixelType = GL.GL_UNSIGNED_INT;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test09_MipMap_ScaleInternal_RGBA_UINT () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGBA;
    	final int pixelType = GL.GL_UNSIGNED_INT;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test10_MipMap_ScaleInternal_RGB_INT () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGB;
    	final int pixelType = GL2ES2.GL_INT;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test11_MipMap_ScaleInternal_RGBA_INT () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGBA;
    	final int pixelType = GL2ES2.GL_INT;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test12_MipMap_ScaleInternal_RGB_FLOAT () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGB;
    	final int pixelType = GL.GL_FLOAT;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

    @Test
    public void test13_MipMap_ScaleInternal_RGBA_FLOAT () {
    	final int width = 1;
    	final int height = 7;
    	final int pixelFormat = GL.GL_RGBA;
    	final int pixelType = GL.GL_FLOAT;

    	testTextureMipMapGeneration(width, height, pixelFormat, pixelType);
    }

	public static void main(final String[] args) {
		org.junit.runner.JUnitCore.main(TestBug365TextureGenerateMipMaps.class.getName());
	}
}