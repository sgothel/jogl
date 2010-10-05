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
 
package com.jogamp.test.junit.jogl.acore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

import javax.media.opengl.*;
import com.jogamp.common.os.Platform;

import java.io.IOException;

public class DumpVersion implements GLEventListener {

    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        System.err.println(Thread.currentThread()+" Platform: " + Platform.getOS() + " (os), " + Platform.getArch() + " (arch)");
        System.err.println(Thread.currentThread()+" Platform: littleEndian " + Platform.isLittleEndian() + ", 32Bit "+Platform.is32Bit() + ", a-ptr bit-size "+Platform.getPointerSizeInBits());
        System.err.println(Thread.currentThread()+" Platform: JavaSE " + Platform.isJavaSE());
        System.err.println(Thread.currentThread()+" GL Profile    " + gl.getGLProfile());
        System.err.println(Thread.currentThread()+" CTX VERSION   " + gl.getContext().getGLVersion());
        System.err.println(Thread.currentThread()+" GL            " + gl);
        System.err.println(Thread.currentThread()+" GL_VERSION    " + gl.glGetString(gl.GL_VERSION));
        System.err.println(Thread.currentThread()+" GL_EXTENSIONS ");
        System.err.println(Thread.currentThread()+"               " + gl.glGetString(gl.GL_EXTENSIONS));
        System.err.println(Thread.currentThread()+" swapInterval  " + gl.getSwapInterval());
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    public void display(GLAutoDrawable drawable) {
    }

    public void dispose(GLAutoDrawable drawable) {
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

}
