/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
 
package com.jogamp.opengl.test.junit.jogl.awt.text;

import java.awt.Font;
import java.io.OutputStream;
import java.io.PrintStream;

import com.jogamp.opengl.util.awt.TextRenderer;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

import org.junit.Assert;

/*
 * Unit tests for Bug464
 * GLEventListener for unit test TestAWTTextRendererUseVertexArrayBug464. The display
 * method renders the String "ABC123#+?" to the lower left corner of the canvas.
 *  
 * The testNumber variable is used to switch between 2D- and 3D-textrendering in the display
 * method.
 * The disallowedMethodCalls variable is used to log VBO-related glFunction calls during
 * the execution of the test.
 * 
 * Other classes related to this test:
 *   TestAWTTextRendererUseVertexArrayBug464
 *   TextRendererTraceGL2Mock01
 */

public class TextRendererGLEventListener01 implements GLEventListener {
    private GLU glu = new GLU();
    private TextRenderer renderer;
    private String text;
    private String disallowedMethodCalls;
    private int testNumber;
    
    public TextRendererGLEventListener01(int testNumber) {
        this.disallowedMethodCalls = "";
        this.testNumber = testNumber;
    }

    public void init(GLAutoDrawable drawable) {
        renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
        renderer.setUseVertexArrays(false);
        Assert.assertNotNull(renderer);
        Assert.assertFalse(renderer.getUseVertexArrays());
        
        text = "ABC123#+?";
        
        PrintStream nullStream = new PrintStream(new OutputStream(){ public void write(int b){}});
        drawable.setGL(new TextRendererTraceGL2Mock01(drawable.getGL().getGL2(), nullStream, this));
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glMatrixMode(GL2ES1.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, 1, 0, 1);
        gl.glMatrixMode(GL2ES1.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public void dispose(GLAutoDrawable drawable) {
        renderer.dispose();
    }

    public void display(GLAutoDrawable drawable) {
        if (disallowedMethodCalls.equals("")) {
            if (testNumber == 1) {
                renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
                renderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                renderer.draw(text, 0, 0);
                renderer.endRendering();
            }
            if (testNumber == 2) {
                renderer.begin3DRendering();
                renderer.setColor(1.0f, 1.0f, 1.0f, 1.0f);
                renderer.draw3D(text, 0, 0, 0, 0.002f);
                renderer.end3DRendering();
            }
        }
    }
    
    public void disallowedMethodCalled (String method) {
        if (!disallowedMethodCalls.equals("")) {
            disallowedMethodCalls += ", ";
        }
        disallowedMethodCalls += method;
    }
    
    public String getDisallowedMethodCalls() {
        return this.disallowedMethodCalls;
    }
}

