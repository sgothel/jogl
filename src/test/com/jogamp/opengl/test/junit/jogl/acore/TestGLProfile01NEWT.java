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

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import javax.media.opengl.*;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.nativewindow.NativeWindowVersion;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.DumpGLInfo;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLProfile01NEWT extends UITestCase {

    @Test
    public void testVersion() throws InterruptedException {
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(GlueGenVersion.getInstance());
        System.err.println(NativeWindowVersion.getInstance());
        System.err.println(JoglVersion.getInstance());
        System.err.println(NewtVersion.getInstance());

        System.err.println(JoglVersion.getDefaultOpenGLInfo(null, null, true).toString());        
    }

    @Test
    public void testGLProfileDefault() throws InterruptedException {
        System.out.println("GLProfile "+GLProfile.glAvailabilityToString());
        System.out.println("GLProfile.getDefaultDevice(): "+GLProfile.getDefaultDevice());        
        GLProfile glp = GLProfile.getDefault();
        System.out.println("GLProfile.getDefault(): "+glp);
        if(glp.getName().equals(GLProfile.GL4bc)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL4bc));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL3bc));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES1));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        } else if(glp.getName().equals(GLProfile.GL3bc)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL3bc));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES1));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        } else if(glp.getName().equals(GLProfile.GL2)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES1));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        } else if(glp.getName().equals(GLProfile.GL2ES1)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES1));
        }
        dumpVersion(glp);
    }

    @Test
    public void testGLProfileMaxProgrammable() throws InterruptedException {
        // Assuming at least one programmable profile is available
        GLProfile glp = GLProfile.getMaxProgrammable(true);
        System.out.println("GLProfile.getMaxProgrammable(): "+glp);
        if(glp.getName().equals(GLProfile.GL4)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL4));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL3));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        } else if(glp.getName().equals(GLProfile.GL3)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL3));
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        } else if(glp.getName().equals(GLProfile.GL2ES2)) {
            Assert.assertTrue(GLProfile.isAvailable(GLProfile.GL2ES2));
        }
        dumpVersion(glp);
    }

    @Test
    public void testGLProfileGL2ES1() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2ES1)) {
            System.out.println("GLProfile GL2ES1 n/a");
            return;
        }
        GLProfile glp = GLProfile.getGL2ES1();
        System.out.println("GLProfile GL2ES1: "+glp);
        dumpVersion(glp);
    }

    @Test
    public void testGLProfileGL2ES2() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL2ES2)) {
            System.out.println("GLProfile GL2ES2 n/a");
            return;
        }
        GLProfile glp = GLProfile.getGL2ES2();
        System.out.println("GLProfile GL2ES2: "+glp);
        dumpVersion(glp);
    }

    @Test
    public void testGLProfileGL4ES3() throws InterruptedException {
        if(!GLProfile.isAvailable(GLProfile.GL4ES3)) {
            System.out.println("GLProfile GL4ES3 n/a");
            return;
        }
        GLProfile glp = GLProfile.getGL4ES3();
        System.out.println("GLProfile GL4ES3: "+glp);
        dumpVersion(glp);
    }
    
    void testSpecificProfile(String glps) throws InterruptedException {
        if(GLProfile.isAvailable(glps)) {
            GLProfile glp = GLProfile.get(glps);
            dumpVersion(glp);
        } else {
            System.err.println("Profile "+glps+" n/a");
        }
    }
    
    @Test
    public void testGL4bc() throws InterruptedException {
        testSpecificProfile(GLProfile.GL4bc);
    }

    @Test
    public void testGL3bc() throws InterruptedException {
        testSpecificProfile(GLProfile.GL3bc);
    }

    @Test
    public void testGL2() throws InterruptedException {
        testSpecificProfile(GLProfile.GL2);
    }
    
    @Test
    public void testGL4() throws InterruptedException {
        testSpecificProfile(GLProfile.GL4);
    }

    @Test
    public void testGL3() throws InterruptedException {
        testSpecificProfile(GLProfile.GL3);
    }

    @Test
    public void testGLES1() throws InterruptedException {
        testSpecificProfile(GLProfile.GLES1);
    }

    @Test
    public void testGLES2() throws InterruptedException {
        testSpecificProfile(GLProfile.GLES2);
    }
    
    @Test
    public void testGLES3() throws InterruptedException {
        testSpecificProfile(GLProfile.GLES3);
    }
    
    protected void dumpVersion(GLProfile glp) throws InterruptedException {
        GLCapabilities caps = new GLCapabilities(glp);        
        GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("TestGLProfile01NEWT");

        glWindow.addGLEventListener(new DumpGLInfo());
        glWindow.addGLEventListener(new GLEventListener() {
        
            public void init(GLAutoDrawable drawable) {
                final GL gl = drawable.getGL();
                final GLProfile glp = gl.getGLProfile();
                System.err.println("GL impl. class "+gl.getClass().getName());
                if( gl.isGL4() ) {
                    Assert.assertNotNull( gl.getGL4() );
                    System.err.println("GL Mapping "+glp+" -> GL4");
                }
                if( gl.isGL4bc() ) {
                    Assert.assertNotNull( gl.getGL4bc() );
                    System.err.println("GL Mapping "+glp+" -> GL4bc");
                }
                if( gl.isGL3() ) {
                    Assert.assertNotNull( gl.getGL3() );
                    System.err.println("GL Mapping "+glp+" -> GL3");
                }
                if( gl.isGL3bc() ) {
                    Assert.assertNotNull( gl.getGL3bc() );
                    System.err.println("GL Mapping "+glp+" -> GL3bc");
                }
                if( gl.isGLES3() ) {
                    Assert.assertNotNull( gl.getGLES3() );
                    System.err.println("GL Mapping "+glp+" -> GLES3");
                }
                if( gl.isGLES2() ) {
                    Assert.assertNotNull( gl.getGLES2() );
                    System.err.println("GL Mapping "+glp+" -> GLES2");
                }
                if( gl.isGL4ES3() ) {
                    Assert.assertNotNull( gl.getGL4ES3() );
                    System.err.println("GL Mapping "+glp+" -> GL4ES3");
                }
                if( gl.isGL2ES2() ) {
                    Assert.assertNotNull( gl.getGL2ES2() );
                    System.err.println("GL Mapping "+glp+" -> GL2ES2");
                }
                if( gl.isGL2ES1() ) {
                    Assert.assertNotNull( gl.getGL2ES1() );
                    System.err.println("GL Mapping "+glp+" -> GL2ES1");
                }
            }
        
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            }
        
            public void display(GLAutoDrawable drawable) {
            }
        
            public void dispose(GLAutoDrawable drawable) {
            }
        });

        glWindow.setSize(128, 128);
        glWindow.setVisible(true);

        glWindow.display();
        Thread.sleep(100);
        glWindow.destroy();
    }

    public static void main(String args[]) throws IOException {
        String tstname = TestGLProfile01NEWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
