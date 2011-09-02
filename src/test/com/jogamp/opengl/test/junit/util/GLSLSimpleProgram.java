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

package com.jogamp.opengl.test.junit.util;

import com.jogamp.opengl.util.glsl.ShaderUtil;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import org.junit.Assert;

public class GLSLSimpleProgram {
    private int shaderProgram;
    private int vertShader;
    private int fragShader;
    private boolean isValid;

    private GLSLSimpleProgram(int shaderProgram, int vertShader, int fragShader) {
        this.shaderProgram = shaderProgram;
        this.vertShader = vertShader;
        this.fragShader = fragShader;
        this.isValid = true;
    }

    public static GLSLSimpleProgram create(GL2ES2 gl, String vertShaderCode, String fragShaderCode, boolean link) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream pbaos = new PrintStream(baos);

        int vertShader = gl.glCreateShader(GL2ES2.GL_VERTEX_SHADER);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        int fragShader = gl.glCreateShader(GL2ES2.GL_FRAGMENT_SHADER);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        String[] vlines = new String[] { vertShaderCode };
        int[] vlengths = new int[] { vlines[0].length() };
        gl.glShaderSource(vertShader, vlines.length, vlines, vlengths, 0);
        gl.glCompileShader(vertShader);
        if(!ShaderUtil.isShaderStatusValid(gl, vertShader, gl.GL_COMPILE_STATUS, pbaos)) {
            System.out.println("getShader:postCompile vertShader: "+baos.toString());
            Assert.assertTrue(false);
        }
        pbaos.flush(); baos.reset();
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        
        String[] flines = new String[] { fragShaderCode };
        int[] flengths = new int[] { flines[0].length() };
        gl.glShaderSource(fragShader, flines.length, flines, flengths, 0);
        gl.glCompileShader(fragShader);
        if(!ShaderUtil.isShaderStatusValid(gl, fragShader, gl.GL_COMPILE_STATUS, pbaos)) {
            System.out.println("getShader:postCompile fragShader: "+baos.toString());
            Assert.assertTrue(false);
        }
        pbaos.flush(); baos.reset();
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        int shaderProgram = gl.glCreateProgram();
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        gl.glAttachShader(shaderProgram, vertShader);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        gl.glAttachShader(shaderProgram, fragShader);
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());

        if(link) {
            gl.glLinkProgram(shaderProgram);
            if(!ShaderUtil.isProgramValid(gl, shaderProgram, pbaos)) {
                System.out.println("Error (GLSL link error):  "+baos.toString());
                Assert.assertTrue(false);
            }
        }
        Assert.assertEquals(GL.GL_NO_ERROR, gl.glGetError());
        GLSLSimpleProgram res = new GLSLSimpleProgram(shaderProgram, vertShader, fragShader);
        return res;
    }

    public void release(GL2ES2 gl) {
        gl.glUseProgram(0);
        gl.glDetachShader(shaderProgram, vertShader);
        gl.glDeleteShader(vertShader);
        gl.glDetachShader(shaderProgram, fragShader);
        gl.glDeleteShader(fragShader);
        gl.glDeleteProgram(shaderProgram);
        isValid = false;
        shaderProgram = 0;
        vertShader = 0;
        fragShader = 0;
    }

    public int getFragShader() {
        return fragShader;
    }

    public int getShaderProgram() {
        return shaderProgram;
    }

    public int getVertShader() {
        return vertShader;
    }
    
    public boolean isValid() {
        return isValid;
    }
}
