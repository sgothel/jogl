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

package com.jogamp.opengl.test.junit.newt.event ;

import java.awt.BorderLayout ;

import com.jogamp.opengl.GLCapabilities ;
import com.jogamp.opengl.GLProfile ;

import javax.swing.JFrame ;
import javax.swing.SwingUtilities ;
import javax.swing.WindowConstants ;

import org.junit.AfterClass ;
import org.junit.Assert;
import org.junit.BeforeClass ;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.awt.NewtCanvasAWT ;
import com.jogamp.newt.opengl.GLWindow ;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;

/**
 * Test whether or not event modifiers are preserved by NEWT when
 * the canvas is a NewtCanvasAWT.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNewtEventModifiersNewtCanvasAWT extends BaseNewtEventModifiers {

    private static JFrame _testFrame ;
    private static GLWindow _glWindow ;

    ////////////////////////////////////////////////////////////////////////////

    @BeforeClass
    public static void beforeClass() throws Exception {

        SwingUtilities.invokeAndWait( new Runnable() {
            public void run() {

                _testFrame = new JFrame( "Event Modifier Test NewtCanvasAWT" ) ;
                _testFrame.setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE ) ;

                {
                    final GLCapabilities caps = new GLCapabilities( GLProfile.getGL2ES2() ) ;
                    _glWindow = GLWindow.create( caps ) ;

                    final NewtCanvasAWT canvas = new NewtCanvasAWT( _glWindow ) ;
                    _testFrame.getContentPane().add( canvas, BorderLayout.CENTER ) ;

                    _glWindow.addGLEventListener( new RedSquareES2() ) ;
                }

                _testFrame.setBounds( TEST_FRAME_X, TEST_FRAME_Y, TEST_FRAME_WIDTH, TEST_FRAME_HEIGHT ) ;
                _testFrame.setVisible( true ) ;
            }
        } ) ;
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(_testFrame, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(_glWindow, true));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(_glWindow, true));

        AWTRobotUtil.assertRequestFocusAndWait(null, _glWindow, _glWindow, null, null);  // programmatic
        Assert.assertNotNull(_robot);
        AWTRobotUtil.requestFocus(_robot, _glWindow, false); // within unit framework, prev. tests (TestFocus02SwingAWTRobot) 'confuses' Windows keyboard input

        _glWindow.addMouseListener( _testMouseListener ) ;
    }

    ////////////////////////////////////////////////////////////////////////////

    @AfterClass
    public static void afterClass() throws Exception {
        SwingUtilities.invokeAndWait( new Runnable() {
            public void run() {
                if( null != _testFrame ) {
                    _testFrame.dispose() ;
                }
            }
        } ) ;

        _glWindow.destroy() ;
    }

    ////////////////////////////////////////////////////////////////////////////

    public static void main(final String args[]) throws Exception {
        final String testName = TestNewtEventModifiersNewtCanvasAWT.class.getName() ;
        org.junit.runner.JUnitCore.main( testName ) ;
    }

    ////////////////////////////////////////////////////////////////////////////
}
