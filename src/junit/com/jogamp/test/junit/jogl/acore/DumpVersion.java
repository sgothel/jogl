/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name Sven Gothel or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
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
