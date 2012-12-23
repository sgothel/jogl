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

import org.eclipse.swt.SWT ;
import org.eclipse.swt.layout.FillLayout ;
import org.eclipse.swt.widgets.Display ;
import org.eclipse.swt.widgets.Shell ;

import javax.media.opengl.GLCapabilities ;
import javax.media.opengl.GLProfile ;

import org.junit.AfterClass ;
import org.junit.BeforeClass ;

import com.jogamp.newt.opengl.GLWindow ;
import com.jogamp.newt.swt.NewtCanvasSWT ;

/**
 * Test whether or not event modifiers preserved by NEWT when
 * the canvas is a NewtCanvasSWT.
 */

public class TestNewtEventModifiersNewtCanvasSWT extends BaseNewtEventModifiers {

    private static Shell _testShell ;
    private static GLWindow _glWindow ;
    private static DisplayThread _displayThread ;

    ////////////////////////////////////////////////////////////////////////////

    private static class DisplayThread extends Thread
    {
        private Display _display ;

        public DisplayThread()
        {
            super( "SWT Display Thread" ) ;
        }

        public Display getDisplay() {
            return _display ;
        }

        public void run() {

            _display = new Display() ;

            while( isInterrupted() == false ) {
                if( !_display.readAndDispatch() ) {
                    _display.sleep() ;
                }
            }

            _display.dispose() ;
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    @BeforeClass
    public static void beforeClass() throws Exception {

        BaseNewtEventModifiers.beforeClass() ;

        _displayThread = new DisplayThread() ;
        _displayThread.start() ;

        // Wait for the display thread's runnable to get set.

        while( _displayThread.getDisplay() == null ) {
            try { Thread.sleep( 10 ) ; } catch( InterruptedException e ) {}
        }

        _displayThread.getDisplay().syncExec( new Runnable() {
            public void run() {

                _testShell = new Shell( _displayThread.getDisplay() ) ;
                _testShell.setText( "Event Modifier Test NewtCanvasSWT" ) ;
                _testShell.setLayout( new FillLayout() ) ;

                {
                    GLCapabilities caps = new GLCapabilities( GLProfile.get( GLProfile.GL2ES2 ) ) ;
                    _glWindow = GLWindow.create( caps ) ;

                    NewtCanvasSWT.create( _testShell, SWT.NO_BACKGROUND, _glWindow ) ;

                    _glWindow.addGLEventListener( new BigGreenXGLEventListener() ) ;
                }

                _testShell.setBounds( TEST_FRAME_X, TEST_FRAME_Y, TEST_FRAME_WIDTH, TEST_FRAME_HEIGHT ) ;
                _testShell.open() ;
            }
        } ) ;

        _glWindow.addMouseListener( _testMouseListener ) ;
    }

    ////////////////////////////////////////////////////////////////////////////

    @AfterClass
    public static void afterClass() throws Exception {

        BaseNewtEventModifiers.afterClass() ;

        _glWindow.destroy() ;

        _displayThread.getDisplay().syncExec( new Runnable() {
            public void run() {
                _testShell.dispose() ;
            }
        } ) ;

        _displayThread.interrupt() ;

        try {
            _displayThread.join() ;
        } catch( InterruptedException e ) {
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    public static void main(String args[]) throws Exception {
        String testName = TestNewtEventModifiersNewtCanvasSWT.class.getName() ;
        org.junit.runner.JUnitCore.main( testName ) ;
    }

    ////////////////////////////////////////////////////////////////////////////
}
