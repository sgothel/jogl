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

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.test.junit.util.NEWTGLContext;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Test;

public class TestGPUMemSec01NEWT extends UITestCase {
    static String hexString(int i) {
        return "0x"+Integer.toHexString(i);
    }
    static String exceptionMsg(String pre, int format, int type, int components, int width, int height, int rl1, int rl4, int rl8) {
        return pre + 
             ": fmt "+hexString(format)+", type "+hexString(type)+", comps "+components+
             ", "+width+"x"+height+
             ", rowlenA1 "+rl1+", rowlenA4 "+rl4+", rowlenA8 "+rl8;        
    }
    
    static NEWTGLContext.WindowContext createCurrentGLOffscreenWindow(int width, int height) throws GLException, InterruptedException {
        final NEWTGLContext.WindowContext winctx = NEWTGLContext.createOffscreenWindow(GLProfile.getGL2ES2(), width, height, true);
        final GL gl = winctx.context.getGL();

        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        // misc GL setup
        gl.glClearColor(1, 1, 1, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        gl.glViewport(0, 0, width, height);        
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        
        return winctx;
    }
    
    static int readPixelsCheck(GL gl, int format, int type, int components, int width, int height) throws InterruptedException {
        int expectedExceptions = 0;
        
        final int rowlenA1 = width * components;    
        
        final int rowlenA4 = ( ( width * components + 3 ) / 4 ) * 4 ;        
        Assert.assertTrue(rowlenA4 % 4 == 0);

        final int rowlenA8 = ( ( width * components + 7 ) / 8 ) * 8 ;
        Assert.assertTrue(rowlenA8 % 8 == 0);
        
        GLPixelStorageModes psm = new GLPixelStorageModes();
        psm.setPackAlignment(gl, 1);
        
        Exception ee = null;
        
        // ok size !
        try {
            ByteBuffer bb = Buffers.newDirectByteBuffer(height*rowlenA1);
            gl.glReadPixels(0, 0, width, height, format, type, bb);
            Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        } catch(IndexOutOfBoundsException e) {
            ee = e;
        }
        Assert.assertNull(
            exceptionMsg("Unexpected IndexOutOfBoundsException (size ok, alignment 1)", 
                         format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8), ee);
        ee = null;
        
        
        // too small -10 !
        try {
            ByteBuffer bb = Buffers.newDirectByteBuffer(height*rowlenA1-10);
            gl.glReadPixels(0, 0, width, height, format, type, bb);
            Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        } catch(IndexOutOfBoundsException e) {
            ee = e;
            System.err.println(
                exceptionMsg("OK Expected IndexOutOfBoundsException (size-10 bytes)", 
                             format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8)+
                             ": "+ee.getMessage());
            expectedExceptions++;            
        }
        Assert.assertNotNull(
            exceptionMsg("Expected IndexOutOfBoundsException (size-10 bytes)", 
                         format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8), ee);
        ee = null;
        
        // too small size/4 !
        try {
            ByteBuffer bb = Buffers.newDirectByteBuffer(height*rowlenA1/4);
            gl.glReadPixels(0, 0, width, height, format, type, bb);
            Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        } catch(IndexOutOfBoundsException e) {
            ee = e;
            System.err.println(
                exceptionMsg("OK Expected IndexOutOfBoundsException (size/4 bytes)", 
                             format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8)+
                             ": "+ee.getMessage());
            expectedExceptions++;            
        }
        Assert.assertNotNull(
            exceptionMsg("Expected IndexOutOfBoundsException (size/4 bytes)", 
                         format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8), ee);
        ee = null;

        //
        // Alignment test
        //         
        psm.setPackAlignment(gl, 4);
        
        // ok size !
        try {
            ByteBuffer bb = Buffers.newDirectByteBuffer(height*rowlenA4);
            gl.glReadPixels(0, 0, width, height, format, type, bb);
            Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        } catch(IndexOutOfBoundsException e) {
            ee = e;
        }
        Assert.assertNull(
            exceptionMsg("Unexpected IndexOutOfBoundsException (size ok, alignment 4)", 
                         format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8), ee);
        ee = null;
        
        // too small if rowlenA1%4 > 0
        try {
            ByteBuffer bb = Buffers.newDirectByteBuffer(height*rowlenA1);
            gl.glReadPixels(0, 0, width, height, format, type, bb);
            Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        } catch(IndexOutOfBoundsException e) {
            ee = e;
            if(rowlenA1%4>0) {
                System.err.println(
                    exceptionMsg("OK Expected IndexOutOfBoundsException (alignment 4)", 
                                 format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8)+
                                 ": "+ee.getMessage());
                expectedExceptions++;
            }
        }
        if(rowlenA1%4>0) {
            Assert.assertNotNull(
                exceptionMsg("Expected IndexOutOfBoundsException (alignment 4)", 
                             format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8), ee);
        } else {
            Assert.assertNull(
                exceptionMsg("Unexpected IndexOutOfBoundsException (alignment 4)", 
                             format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8), ee);            
        }
        ee = null;
        
        psm.setPackAlignment(gl, 8);
        
        // ok size !
        try {
            ByteBuffer bb = Buffers.newDirectByteBuffer(height*rowlenA8);
            gl.glReadPixels(0, 0, width, height, format, type, bb);
            Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        } catch(IndexOutOfBoundsException e) {
            ee = e;
        }
        Assert.assertNull(
            exceptionMsg("Unexpected IndexOutOfBoundsException (size ok, alignment 8)", 
                         format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8), ee);
        ee = null;
        
        // too small if rowlenA1%8 > 0
        try {
            ByteBuffer bb = Buffers.newDirectByteBuffer(height*rowlenA1);
            gl.glReadPixels(0, 0, width, height, format, type, bb);
            Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        } catch(IndexOutOfBoundsException e) {
            ee = e;
            if(rowlenA1%8>0) {
                System.err.println(
                    exceptionMsg("OK Expected IndexOutOfBoundsException (alignment 8)", 
                                 format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8)+
                                 ": "+ee.getMessage());
                expectedExceptions++;
            }
        }
        if(rowlenA1%8>0) {
            Assert.assertNotNull(
                exceptionMsg("Expected IndexOutOfBoundsException (alignment 8)", 
                             format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8), ee);
        } else {
            Assert.assertNull(
                exceptionMsg("Unexpected IndexOutOfBoundsException (alignment 8)", 
                             format, type, components, width, height, rowlenA1, rowlenA4, rowlenA8), ee);            
        }
        ee = null;
        
        psm.restore(gl);        
        
        return expectedExceptions;            
    }
    
    @Test
    public void testReadPixels_640x480xRGBxUB() throws InterruptedException {
        final int width = 640;
        final int height= 480;
        
        // preset ..
        final NEWTGLContext.WindowContext winctx = createCurrentGLOffscreenWindow(width, height);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL gl = winctx.context.getGL();
        
        // 2 x too small - 0 x alignment
        Assert.assertEquals(2, readPixelsCheck(gl, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, 3, width, height));
                        
        drawable.swapBuffers();
        Thread.sleep(50);
        
        NEWTGLContext.destroyWindow(winctx);
    }
    
    @Test
    public void testReadPixels_102x100xRGBxUB() throws InterruptedException {
        final int wwidth = 640;
        final int wheight= 480;
        final int rwidth = 102;
        final int rheight= 100;
        
        // preset ..
        final NEWTGLContext.WindowContext winctx = createCurrentGLOffscreenWindow(wwidth, wheight);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL gl = winctx.context.getGL();
        
        // 2 x too small - 2 x alignment
        Assert.assertEquals(4, readPixelsCheck(gl, GL.GL_RGB, GL.GL_UNSIGNED_BYTE, 3, rwidth, rheight));
                        
        drawable.swapBuffers();
        Thread.sleep(50);
        
        NEWTGLContext.destroyWindow(winctx);
    }
    
    @Test
    public void testReadPixelsGL2GL3_640x480xREDxUB() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2GL3)) {
            System.err.println("GL2GL3 n/a skip test");
            return;
        }
        final int width = 640;
        final int height= 480;

        // preset ..
        final NEWTGLContext.WindowContext winctx = createCurrentGLOffscreenWindow(width, height);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL2GL3 gl = winctx.context.getGL().getGL2GL3();
        
        // 2 x too small - 0 x alignment
        Assert.assertEquals(2, readPixelsCheck(gl, GL2GL3.GL_RED, GL.GL_UNSIGNED_BYTE, 1, width, height));
                        
        drawable.swapBuffers();
        Thread.sleep(50);
        
        NEWTGLContext.destroyWindow(winctx);
    }

    @Test
    public void testReadPixelsGL2GL3_102x100xREDxUB() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2GL3)) {
            System.err.println("GL2GL3 n/a skip test");
            return;
        }
        int wwidth = 640;
        int wheight= 480;
        int rwidth = 102;
        int rheight= 100;

        // preset ..
        final NEWTGLContext.WindowContext winctx = createCurrentGLOffscreenWindow(wwidth, wheight);
        final GLDrawable drawable = winctx.context.getGLDrawable();
        final GL2GL3 gl = winctx.context.getGL().getGL2GL3();
        
        // 2 x too small - 2 x alignment
        Assert.assertEquals(4, readPixelsCheck(gl, GL2GL3.GL_RED, GL.GL_UNSIGNED_BYTE, 1, rwidth, rheight));
                        
        drawable.swapBuffers();
        Thread.sleep(50);
        
        NEWTGLContext.destroyWindow(winctx);
    }
    
    public static void main(String args[]) throws IOException {
        String tstname = TestGPUMemSec01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }    
}

