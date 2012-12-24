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
 
package com.jogamp.opengl.test.junit.newt.event;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import org.junit.AfterClass ;
import org.junit.BeforeClass ;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;

/**
 * Test whether or not event modifiers are properly delivered by NEWT.
 */
public class TestNewtEventModifiersNEWTWindowAWT extends BaseNewtEventModifiers {

    private static GLWindow glWindow;

    ////////////////////////////////////////////////////////////////////////////

    @BeforeClass
    public static void beforeClass() throws Exception {
        glWindow = GLWindow.create( new GLCapabilities( GLProfile.getGL2ES2() ) );
        glWindow.setTitle("Event Modifier Test GLWindow");
        glWindow.addGLEventListener( new RedSquareES2() ) ;
        glWindow.addMouseListener(_testMouseListener);
        glWindow.setSize(TEST_FRAME_WIDTH, TEST_FRAME_HEIGHT);
        glWindow.setPosition(TEST_FRAME_X, TEST_FRAME_Y);
        glWindow.setVisible(true);
    }

    ////////////////////////////////////////////////////////////////////////////

    @AfterClass
    public static void afterClass() throws Exception {
        glWindow.destroy();
    }

    ////////////////////////////////////////////////////////////////////////////

    public static void main(String args[]) throws Exception {
        String testName = TestNewtEventModifiersNEWTWindowAWT.class.getName() ;
        org.junit.runner.JUnitCore.main( testName ) ;
    }

    ////////////////////////////////////////////////////////////////////////////
}
