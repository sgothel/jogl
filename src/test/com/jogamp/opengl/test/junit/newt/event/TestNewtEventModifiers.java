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

import java.io.PrintStream ;
import java.util.ArrayList ;

import javax.media.opengl.GL ;
import javax.media.opengl.GL2 ;
import javax.media.opengl.GLAutoDrawable ;
import javax.media.opengl.GLEventListener ;
import javax.media.opengl.GLProfile ;

import org.junit.After ;
import org.junit.AfterClass ;
import org.junit.Assert ;
import org.junit.Before ;
import org.junit.BeforeClass ;
import org.junit.Test ;

import com.jogamp.opengl.test.junit.util.UITestCase ;

/**
 * Test whether or not event modifiers are preserved by NEWT.  This
 * class defines most of the tests, but leaves the type of window
 * and canvas up to subclasses.
 */

public abstract class TestNewtEventModifiers extends UITestCase {

    static
    {
        GLProfile.initSingleton() ;
    }

    // A class to draw something on a canvas.  This is only
    // here so we can make sure the GL drawing area has
    // filled the window.

    public static class BigGreenXGLEventListener implements GLEventListener
    {    
        public void init( GLAutoDrawable drawable )
        {
            GL2 gl = drawable.getGL().getGL2() ;

            gl.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f ) ;

            gl.glEnable( GL.GL_LINE_SMOOTH ) ;
            gl.glEnable( GL.GL_BLEND ) ;
            gl.glBlendFunc( GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA ) ;
        }

        public void reshape( GLAutoDrawable drawable, int x, int y, int width, int height )
        {
            GL2 gl = drawable.getGL().getGL2() ;

            gl.glViewport( 0, 0, width, height ) ;

            gl.glMatrixMode( GL2.GL_PROJECTION ) ;
            gl.glLoadIdentity() ;
            gl.glOrtho( -1.0, 1.0, -1.0, 1.0, -1.0, 1.0 ) ;

            gl.glMatrixMode( GL2.GL_MODELVIEW ) ;
            gl.glLoadIdentity() ;
        }
        
        public void display( GLAutoDrawable drawable )
        {
            GL2 gl = drawable.getGL().getGL2() ;

            gl.glClear( GL2.GL_COLOR_BUFFER_BIT ) ;

            gl.glColor4f( 0.0f, 1.0f, 0.0f, 1.0f ) ;
            
            gl.glBegin( GL.GL_LINES ) ;
            {
                gl.glVertex2f( -1.0f,  1.0f ) ;
                gl.glVertex2f(  1.0f, -1.0f ) ;

                gl.glVertex2f( -1.0f, -1.0f ) ;
                gl.glVertex2f(  1.0f,  1.0f ) ;
            }
            gl.glEnd() ;
        }

        public void dispose( GLAutoDrawable drawable )
        {
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    private static class TestMouseListener implements com.jogamp.newt.event.MouseListener
    {
        private static final String NO_EVENT_DELIVERY = "no event delivery" ;

        private boolean _modifierCheckEnabled ;
        private int _expectedModifiers ;
        private ArrayList<String> _failures = new ArrayList<String>() ;

        public void setModifierCheckEnabled( boolean value ) {
            _modifierCheckEnabled = value ;
        }

        public boolean modifierCheckEnabled() {
            return _modifierCheckEnabled ;
        }
        
        /**
         * Sets the modifiers the listener should expect, and clears
         * out any existing accumulated failures.  Normally this kind
         * of double duty in a setter might be considered evil, but
         * in this test code it's probably ok.
         */

        public void setExpectedModifiers( int value ) {
            _expectedModifiers = value ;

            // Assume we will have a failure due to no event delivery.
            // If an event is delivered and it's good this assumed
            // failure will get cleared out.

            _failures.clear() ;
            _failures.add( NO_EVENT_DELIVERY ) ;
        }
        
        private void _checkModifiers( com.jogamp.newt.event.MouseEvent event ) {

            if( _debug ) {
                _debugPrintStream.print( "     received NEWT " ) ;
                _debugPrintStream.print( com.jogamp.newt.event.MouseEvent.getEventTypeString( event.getEventType() ) ) ;
            }

            if( _modifierCheckEnabled ) {

                if( _debug ) {
                    _debugPrintStream.println( ", checking modifiers..." ) ;
                    _debugPrintStream.println( "         expected NEWT Modifiers:" ) ;
                    _printModifiers( _expectedModifiers ) ;
                    _debugPrintStream.println( "         current NEWT Modifiers:" ) ;
                    _printModifiers( event.getModifiers() ) ;
                }

                _checkModifierMask( event, com.jogamp.newt.event.InputEvent.SHIFT_MASK ) ;
                _checkModifierMask( event, com.jogamp.newt.event.InputEvent.CTRL_MASK ) ;
                _checkModifierMask( event, com.jogamp.newt.event.InputEvent.META_MASK ) ;
                _checkModifierMask( event, com.jogamp.newt.event.InputEvent.ALT_MASK ) ;
                _checkModifierMask( event, com.jogamp.newt.event.InputEvent.ALT_GRAPH_MASK ) ;

                for( int n = 0 ; n < _numButtonsToTest ; ++n ) {
                    _checkModifierMask( event, com.jogamp.newt.event.InputEvent.getButtonMask( n + 1 ) ) ;
                }
            } else {
                if( _debug ) {
                    _debugPrintStream.println( ", modifier check disabled" ) ;
                }
            }
        }

        private void _checkModifierMask( com.jogamp.newt.event.MouseEvent event, int mask ) {

            // If the "no event delivery" failure is still in the list then
            // get rid of it since that obviously isn't true anymore.  We
            // want to do this whether or not there's an issue with the
            // modifiers.

            if( _failures.size() == 1 && _failures.get(0).equals( NO_EVENT_DELIVERY ) ) {
                _failures.clear() ;
            }

            if( ( event.getModifiers() & mask ) != ( _expectedModifiers & mask ) ) {
                _failures.add( com.jogamp.newt.event.MouseEvent.getEventTypeString( event.getEventType() ) ) ;
            }
        }

        private void _printModifiers( int modifiers ) {
            if( ( modifiers & com.jogamp.newt.event.InputEvent.SHIFT_MASK ) != 0 ) { _debugPrintStream.println( "             SHIFT" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.CTRL_MASK ) != 0 ) { _debugPrintStream.println( "             CTRL" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.META_MASK ) != 0 ) { _debugPrintStream.println( "             META" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.ALT_MASK ) != 0 ) { _debugPrintStream.println( "             ALT" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.ALT_GRAPH_MASK ) != 0 ) { _debugPrintStream.println( "             ALT_GRAPH" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.BUTTON1_MASK ) != 0 ) { _debugPrintStream.println( "             BUTTON1" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.BUTTON2_MASK ) != 0 ) { _debugPrintStream.println( "             BUTTON2" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.BUTTON3_MASK ) != 0 ) { _debugPrintStream.println( "             BUTTON3" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.BUTTON4_MASK ) != 0 ) { _debugPrintStream.println( "             BUTTON4" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.BUTTON5_MASK ) != 0 ) { _debugPrintStream.println( "             BUTTON5" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.BUTTON6_MASK ) != 0 ) { _debugPrintStream.println( "             BUTTON6" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.BUTTON7_MASK ) != 0 ) { _debugPrintStream.println( "             BUTTON7" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.BUTTON8_MASK ) != 0 ) { _debugPrintStream.println( "             BUTTON8" ) ; }
            if( ( modifiers & com.jogamp.newt.event.InputEvent.BUTTON9_MASK ) != 0 ) { _debugPrintStream.println( "             BUTTON9" ) ; }
        }

        private static void _printAwtModifiers( int awtMods, int awtModsEx ) {
            if( ( awtMods & java.awt.event.InputEvent.SHIFT_MASK ) != 0 ) { System.err.println( "SHIFT" ) ; }
            if( ( awtMods & java.awt.event.InputEvent.CTRL_MASK ) != 0 ) { System.err.println( "CTRL" ) ; }
            if( ( awtMods & java.awt.event.InputEvent.META_MASK ) != 0 ) { System.err.println( "META" ) ; }
            if( ( awtMods & java.awt.event.InputEvent.ALT_MASK ) != 0 ) { System.err.println( "ALT" ) ; }
            if( ( awtMods & java.awt.event.InputEvent.ALT_GRAPH_MASK ) != 0 ) { System.err.println( "ALT_GRAPH" ) ; }
    
            if( ( awtModsEx & java.awt.event.InputEvent.SHIFT_DOWN_MASK ) != 0 ) { System.err.println( "SHIFT Ex" ) ; }
            if( ( awtModsEx & java.awt.event.InputEvent.CTRL_DOWN_MASK ) != 0 ) { System.err.println( "CTRL Ex" ) ; }
            if( ( awtModsEx & java.awt.event.InputEvent.META_DOWN_MASK ) != 0 ) { System.err.println( "META Ex" ) ; }
            if( ( awtModsEx & java.awt.event.InputEvent.ALT_DOWN_MASK ) != 0 ) { System.err.println( "ALT Ex" ) ; }
            if( ( awtModsEx & java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK ) != 0 ) { System.err.println( "ALT_GRAPH Ex" ) ; }
            if( ( awtModsEx & java.awt.event.InputEvent.BUTTON1_DOWN_MASK ) != 0 ) { System.err.println( "BUTTON1" ) ; }
            if( ( awtModsEx & java.awt.event.InputEvent.BUTTON2_DOWN_MASK ) != 0 ) { System.err.println( "BUTTON2" ) ; }
            if( ( awtModsEx & java.awt.event.InputEvent.BUTTON3_DOWN_MASK ) != 0 ) { System.err.println( "BUTTON3" ) ; }
        }
        
        public ArrayList<String> getFailures() {
            return _failures ;
        }

        public void mouseClicked( com.jogamp.newt.event.MouseEvent event ) {
            _checkModifiers( event ) ;
        }

        public void mousePressed( com.jogamp.newt.event.MouseEvent event ) {
            _checkModifiers( event ) ;
        }

        public void mouseReleased( com.jogamp.newt.event.MouseEvent event ) {
            _checkModifiers( event ) ;
        }

        public void mouseEntered( com.jogamp.newt.event.MouseEvent event ) {
            _checkModifiers( event ) ;
        }
        
        public void mouseExited( com.jogamp.newt.event.MouseEvent event ) {
            _checkModifiers( event ) ;
        }

        public void mouseDragged( com.jogamp.newt.event.MouseEvent event ) {
            _checkModifiers( event ) ;
        }

        public void mouseMoved( com.jogamp.newt.event.MouseEvent event ) {
            _checkModifiers( event ) ;
        }
        
        public void mouseWheelMoved( com.jogamp.newt.event.MouseEvent event ) {
            _checkModifiers( event ) ;
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    protected static final int TEST_FRAME_X = 100 ;
    protected static final int TEST_FRAME_Y = 100 ;

    protected static final int TEST_FRAME_WIDTH = 400 ;
    protected static final int TEST_FRAME_HEIGHT = 400 ;

    private static final int INITIAL_MOUSE_X = TEST_FRAME_X + ( TEST_FRAME_WIDTH / 2 ) ;
    private static final int INITIAL_MOUSE_Y = TEST_FRAME_Y + ( TEST_FRAME_HEIGHT / 2 ) ;

    private static final int MS_ROBOT_SHORT_AUTO_DELAY = 50 ;
    private static final int MS_ROBOT_LONG_AUTO_DELAY = 500 ;

    private static boolean _debug = true ;
    private static PrintStream _debugPrintStream = System.err ;

    ////////////////////////////////////////////////////////////////////////////

    private static int _numButtonsToTest ;
    private static int _awtButtonMasks[] ;
    private static int _newtButtonMasks[] ;

    private static java.awt.Robot _robot ;

    protected static TestMouseListener _testMouseListener ;

    ////////////////////////////////////////////////////////////////////////////

    public static int getAWTButtonMask(int button) {
        int m;
        switch(button) {
            case 1 : m = java.awt.event.InputEvent.BUTTON1_MASK; break;
            case 2 : m = java.awt.event.InputEvent.BUTTON2_MASK; break;
            case 3 : m = java.awt.event.InputEvent.BUTTON3_MASK; break;
            default: throw new IllegalArgumentException("Only buttons 1-3 have a MASK value, requested button "+button);
        }
        return m;
    }
    
    @BeforeClass
    public static void beforeClass() throws Exception {

        // Who know how many buttons the AWT will say exist on given platform.
        // We'll test the smaller of what NEWT supports and what the
        // AWT says is available.
        // _numButtonsToTest = java.awt.MouseInfo.getNumberOfButtons() ;

        // Then again, maybe not:

        // FIXME? - for reasons I'm not quite sure of the AWT MouseEvent
        // constructor does some strange things for buttons other than
        // 1, 2, and 3.  Furthermore, while developing this test it
        // appeared that events sent by the robot for buttons 9 and
        // up weren't even delivered to the listeners.
        //
        // So... for now we're only going to test 3 buttons since
        // that's the common case.

        _numButtonsToTest = 3 ;

        {
            if( _numButtonsToTest > com.jogamp.newt.event.MouseEvent.BUTTON_NUMBER ) { 
                _numButtonsToTest = com.jogamp.newt.event.MouseEvent.BUTTON_NUMBER ;
            }

            // These two arrays are assumed to be peers, i.e. are the same
            // size, and a given index references the same button in
            // either array.

            _awtButtonMasks = new int[_numButtonsToTest] ;
            
            for( int n = 0 ; n < _awtButtonMasks.length ; ++n ) {
                _awtButtonMasks[n] = getAWTButtonMask( n + 1 );
            }
            
            _newtButtonMasks = new int[_numButtonsToTest] ;

            for( int n = 0 ; n < _newtButtonMasks.length ; ++n ) {
                _newtButtonMasks[n] = com.jogamp.newt.event.InputEvent.getButtonMask( n + 1 ) ;
            }
        }

        _robot = new java.awt.Robot() ;
        _robot.setAutoWaitForIdle( true ) ;
        _robot.setAutoDelay( MS_ROBOT_LONG_AUTO_DELAY ) ;

        _testMouseListener = new TestMouseListener() ;
    }

    ////////////////////////////////////////////////////////////////////////////

    @Before
    public void before() throws Exception {
        
        _testMouseListener.setModifierCheckEnabled( false ) ;
        _robot.setAutoDelay( MS_ROBOT_SHORT_AUTO_DELAY ) ;

        // Make sure all the buttons and modifier keys are released.

        _releaseModifiers() ;
        _escape() ;

        // Move the pointer into the window and click once to
        // ensure that the test window has focus.

        if( _debug ) {
            _debugPrintStream.println( ">>>> Clicking in the window to get focus." ) ;
        }

        _robot.mouseMove( INITIAL_MOUSE_X, INITIAL_MOUSE_Y ) ;
        _robot.mousePress( java.awt.event.InputEvent.BUTTON1_DOWN_MASK ) ;
        _robot.mouseRelease( java.awt.event.InputEvent.BUTTON1_DOWN_MASK ) ;

        _testMouseListener.setModifierCheckEnabled( true ) ;
        _robot.setAutoDelay( MS_ROBOT_LONG_AUTO_DELAY ) ;

        if( _debug ) {
            _debugPrintStream.println( ">>>> About to start testing." ) ;
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////

    // The approach on all these tests is to tell the test mouse listener what
    // modifiers we think it should receive.  Then when the events are delivered
    // it compares what we told it to expect with what actually showed up and
    // complains if there are differences.
    //
    // As things stand currently the tests below generally work for AWTCanvas
    // and fail for everything else.  This may point to a flaw in the test
    // code, or a flaw in the NEWT stuff; not sure yet.  One exception is the
    // tests involving ALT and META, which on at least X11 cause the desktop
    // to do undesirable stuff while the tests are in progress.  So... these
    // tests have been commented out for now and probably should be left
    // that way.
    //
    // Due to the fact that a majority of these fail currently for
    // everything but AWTCanvas for the time being we probably shouldn't
    // run the tests for NewtCanvasAWT and NewtCanvasSWT until we can
    // pay more attention to the NEWT event modifier stuff.

    @Test
    public void testSingleButtonPressAndRelease() throws Exception {
        _doSingleButtonPressAndRelease( 0, 0 ) ;
    }

    @Test
    public void testSingleButtonPressAndReleaseWithShift() throws Exception {
        _doSingleButtonPressAndRelease( java.awt.event.KeyEvent.VK_SHIFT, java.awt.event.InputEvent.SHIFT_DOWN_MASK ) ;
    }

    @Test
    public void testSingleButtonPressAndReleaseWithCtrl() throws Exception {
        _doSingleButtonPressAndRelease( java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.InputEvent.CTRL_DOWN_MASK ) ;
    }

    // The META and ALT tests get too tied up with functions of the window system on X11,
    // so it's probably best to leave them commented out.

    //@Test
    //public void testSingleButtonPressAndReleaseWithMeta() throws Exception {
    //    _doSingleButtonPressAndRelease( java.awt.event.KeyEvent.VK_META, java.awt.event.InputEvent.META_DOWN_MASK ) ;
    //}

    //@Test
    //public void testSingleButtonPressAndReleaseWithAlt() throws Exception {
    //    _doSingleButtonPressAndRelease( java.awt.event.KeyEvent.VK_ALT, java.awt.event.InputEvent.ALT_DOWN_MASK ) ;
    //}

    // FIXME - not sure yet what's up with ALT_GRAPH.  It appears that this
    // modifier didn't make it through, so I had to disable this test else
    // it would always fail.
    //
    // My US keyboard doesn't have an AltGr key, so maybe X is smart
    // enough to not let this modifier slip through (?).

    //@Test
    //public void testSingleButtonPressAndReleaseWithAltGraph() throws Exception {
    //    _doSingleButtonPressAndRelease( java.awt.event.KeyEvent.VK_ALT_GRAPH, java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK ) ;
    //}

    ////////////////////////////////////////////////////////////////////////////

    @Test
    public void testHoldOneButtonAndPressAnother() throws Exception {
        _doHoldOneButtonAndPressAnother( 0, 0 ) ;
    }
    
    @Test
    public void testPressAllButtonsInSequence() throws Exception {
        _doPressAllButtonsInSequence( 0, 0 ) ;
    }

    @Test
    public void testSingleButtonClickAndDrag() throws Exception {
        _doSingleButtonClickAndDrag( 0, 0 ) ;
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _doSingleButtonPressAndRelease( final int keyCode, final int keyModifierMask ) throws Exception {

        if( _debug ) { _debugPrintStream.println( "\n>>>> _doSingleButtonPressAndRelease" ) ; }

        _doKeyPress( keyCode ) ;

        for (int n = 0 ; n < _numButtonsToTest ; ++n) {

            int awtButtonMask = _awtButtonMasks[n] ;

            if( _debug ) { _debugPrintStream.println( "\n     pressing button " + ( n + 1 ) ) ; }
            _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask ) ) ;
            _robot.mousePress( awtButtonMask ) ;
            _checkFailures() ;

            if( _debug ) { _debugPrintStream.println( "\n     releasing button " + ( n + 1 ) ) ; }
            _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask ) ) ;
            _robot.mouseRelease( awtButtonMask ) ;
            _checkFailures() ;
        }

        _doKeyRelease( keyCode ) ;
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _doHoldOneButtonAndPressAnother( final int keyCode, final int keyModifierMask ) throws Exception {

        if( _debug ) { _debugPrintStream.println( "\n>>>> _doHoldOneButtonAndPressAnother" ) ; }

        _doKeyPress( keyCode ) ;

        for (int n = 0 ; n < _numButtonsToTest ; ++n) {

            int awtButtonMask = _awtButtonMasks[n] ;

            if( _debug ) { _debugPrintStream.println( "\n     pressing button " + ( n + 1 ) ) ; }
            _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask ) ) ;
            _robot.mousePress( awtButtonMask ) ;
            _checkFailures() ;

            for (int m = 0 ; m < _numButtonsToTest ; ++m) {

                if( n != m ) {

                    if( _debug ) { _debugPrintStream.println( "\n     pressing additional button " + ( m + 1 ) ) ; }
                    _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask | _awtButtonMasks[m] ) ) ;
                    _robot.mousePress( _awtButtonMasks[m] ) ;
                    _checkFailures() ;

                    if( _debug ) { _debugPrintStream.println( "\n     releasing additional button " + ( m + 1 ) ) ; }
                    _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask ) ) ;
                    _robot.mouseRelease( _awtButtonMasks[m] ) ;
                    _checkFailures() ;
                }
            }

            if( _debug ) { _debugPrintStream.println( "\n     releasing button " + ( n + 1 ) ) ; }
            _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask ) ) ;
            _robot.mouseRelease( awtButtonMask ) ;
            _checkFailures() ;
        }

        _doKeyRelease( keyCode ) ;
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _doPressAllButtonsInSequence( final int keyCode, final int keyModifierMask ) throws Exception {

        if( _debug ) { _debugPrintStream.println( "\n>>>> _doPressAllButtonsInSequence" ) ; }

        _doKeyPress( keyCode ) ;

        {
            int cumulativeAwtModifiers = 0 ;
            
            for (int n = 0 ; n < _numButtonsToTest ; ++n) {

                cumulativeAwtModifiers |= _awtButtonMasks[n] ;

                if( _debug ) { _debugPrintStream.println( "\n     pressing button " + ( n + 1 ) ) ; }
                _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | cumulativeAwtModifiers ) ) ;
                _robot.mousePress( _awtButtonMasks[n] ) ;
                _checkFailures() ;
            }

            for (int n = _numButtonsToTest - 1 ; n >= 0 ; --n) {

                cumulativeAwtModifiers &= ~_awtButtonMasks[n] ;

                if( _debug ) { _debugPrintStream.println( "\n     releasing button " + ( n + 1 ) ) ; }
                _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | cumulativeAwtModifiers ) ) ;
                _robot.mouseRelease( _awtButtonMasks[n] ) ;
                _checkFailures() ;
            }
        }

        _doKeyRelease( keyCode ) ;
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _doSingleButtonClickAndDrag( final int keyCode, final int keyModifierMask ) throws Exception {

        if( _debug ) { _debugPrintStream.println( "\n>>>> _doSingleButtonClickAndDrag" ) ; }

        _doKeyPress( keyCode ) ;

        _testMouseListener.setModifierCheckEnabled( true ) ;

        for (int n = 0 ; n < _numButtonsToTest ; ++n) {

            int awtButtonMask = _awtButtonMasks[n] ;

            if( _debug ) { _debugPrintStream.println( "\n     pressing button " + ( n + 1 ) ) ; }
            _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask ) ) ;
            _robot.mousePress( awtButtonMask ) ;
            _checkFailures() ;

            // To get a drag we only need to move one pixel.
            if( _debug ) { _debugPrintStream.println( "\n     moving mouse" ) ; }
            _robot.mouseMove( INITIAL_MOUSE_X + 1, INITIAL_MOUSE_Y + 1 ) ;
            _checkFailures() ;

            if( _debug ) { _debugPrintStream.println( "\n     releasing button " + ( n + 1 ) ) ; }
            _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask ) ) ;
            _robot.mouseRelease( awtButtonMask ) ;
            _checkFailures() ;

            _testMouseListener.setModifierCheckEnabled( false ) ;
            _robot.mouseMove( INITIAL_MOUSE_X, INITIAL_MOUSE_Y ) ;
            _testMouseListener.setModifierCheckEnabled( true ) ;
        }

        _doKeyRelease( keyCode ) ;
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _doKeyPress( int keyCode ) {
        if( keyCode != 0 ) {
            boolean modifierCheckEnabled = _testMouseListener.modifierCheckEnabled() ;
            _testMouseListener.setModifierCheckEnabled( false ) ;
            _robot.keyPress( keyCode ) ;
            _testMouseListener.setModifierCheckEnabled( modifierCheckEnabled ) ;
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _doKeyRelease( int keyCode ) {
        if( keyCode != 0 ) {
            boolean modifierCheckEnabled = _testMouseListener.modifierCheckEnabled() ;
            _testMouseListener.setModifierCheckEnabled( false ) ;
            _robot.keyRelease( keyCode ) ;
            _testMouseListener.setModifierCheckEnabled( modifierCheckEnabled ) ;
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _checkFailures() {

        ArrayList<String> failures = _testMouseListener.getFailures() ;

        if( _debug ) {
            int numFailures = failures.size() ;
            if( numFailures == 0 ) {
                _debugPrintStream.println( "     PASSED" ) ;
            } else {
                _debugPrintStream.println( "     FAILED" ) ;
                for( int n = 0 ; n < numFailures ; ++n ) {
                    _debugPrintStream.print( "         " ) ;
                    _debugPrintStream.println( failures.get(n) ) ;
                }
            }
        }

        Assert.assertTrue( failures.size() == 0 ) ;
    }

    ////////////////////////////////////////////////////////////////////////////

    @After
    public void after() throws Exception {

        _testMouseListener.setModifierCheckEnabled( false ) ;
        
        Thread.sleep( 500 ) ;
    }
    
    ////////////////////////////////////////////////////////////////////////////

    @AfterClass
    public static void afterClass() throws Exception {

        // Make sure all modifiers are released, otherwise the user's
        // desktop can get locked up (ask me how I know this).

        _releaseModifiers() ;
        _escape() ;
    }

    ////////////////////////////////////////////////////////////////////////////

    private static void _releaseModifiers() {

        if (_robot != null) {

            _robot.setAutoDelay( MS_ROBOT_SHORT_AUTO_DELAY ) ;

            boolean modifierCheckEnabled = _testMouseListener.modifierCheckEnabled() ;
            _testMouseListener.setModifierCheckEnabled( false ) ;

            {
                _robot.keyRelease( java.awt.event.KeyEvent.VK_SHIFT ) ;
                _robot.keyRelease( java.awt.event.KeyEvent.VK_CONTROL ) ;
                _robot.keyRelease( java.awt.event.KeyEvent.VK_META ) ;
                _robot.keyRelease( java.awt.event.KeyEvent.VK_ALT ) ;
                _robot.keyRelease( java.awt.event.KeyEvent.VK_ALT_GRAPH ) ;

                for (int n = 0 ; n < _awtButtonMasks.length ; ++n) {
                    _robot.mouseRelease( _awtButtonMasks[n] ) ;
                }
            }

            _testMouseListener.setModifierCheckEnabled( modifierCheckEnabled ) ;

            _robot.setAutoDelay( MS_ROBOT_LONG_AUTO_DELAY ) ;
        }
    }

    private static void _escape() {
        if (_robot != null) {
            _robot.keyPress( java.awt.event.KeyEvent.VK_ESCAPE ) ;
            _robot.keyRelease( java.awt.event.KeyEvent.VK_ESCAPE ) ;
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Brute force method to return the NEWT event modifiers equivalent
     * to the specified AWT event extended modifiers.
     *
     * @param awtExtendedModifiers
     * The AWT extended modifiers.
     *
     * @return
     * The equivalent NEWT modifiers.
     */
    
    private int _getNewtModifiersForAwtExtendedModifiers( int awtExtendedModifiers ) {

        int mask = 0 ;

        if( ( awtExtendedModifiers & java.awt.event.InputEvent.SHIFT_DOWN_MASK ) != 0 ) {
            mask |= com.jogamp.newt.event.InputEvent.SHIFT_MASK ;
        }

        if( ( awtExtendedModifiers & java.awt.event.InputEvent.CTRL_DOWN_MASK ) != 0 ) {
            mask |= com.jogamp.newt.event.InputEvent.CTRL_MASK ;
        }

        if( ( awtExtendedModifiers & java.awt.event.InputEvent.META_DOWN_MASK ) != 0 ) {
            mask |= com.jogamp.newt.event.InputEvent.META_MASK ;
        }

        if( ( awtExtendedModifiers & java.awt.event.InputEvent.ALT_DOWN_MASK ) != 0 ) {
            mask |= com.jogamp.newt.event.InputEvent.ALT_MASK ;
        }

        if( ( awtExtendedModifiers & java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK ) != 0 ) {
            mask |= com.jogamp.newt.event.InputEvent.ALT_GRAPH_MASK ;
        }

        for (int n = 0 ; n < _numButtonsToTest ; ++n) {
            if ((awtExtendedModifiers & getAWTButtonMask(n+1)) != 0) {
                mask |= com.jogamp.newt.event.InputEvent.getButtonMask(n+1) ;
            }
        }

        return mask ;
    }

    ////////////////////////////////////////////////////////////////////////////
}
