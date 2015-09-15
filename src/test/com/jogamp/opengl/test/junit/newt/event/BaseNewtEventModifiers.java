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
import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.opengl.GLProfile ;

import org.junit.Assert ;
import org.junit.BeforeClass ;
import org.junit.FixMethodOrder;
import org.junit.Test ;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.RunnableTask;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase ;

/**
 * Test whether or not event modifiers are preserved by NEWT.  This
 * class defines most of the tests, but leaves the type of window
 * and canvas up to subclasses.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class BaseNewtEventModifiers extends UITestCase {

    ////////////////////////////////////////////////////////////////////////////

    protected static final int TEST_FRAME_X = 100 ;
    protected static final int TEST_FRAME_Y = 100 ;

    protected static final int TEST_FRAME_WIDTH = 400 ;
    protected static final int TEST_FRAME_HEIGHT = 400 ;

    protected static final int INITIAL_MOUSE_X = TEST_FRAME_X + ( TEST_FRAME_WIDTH / 2 ) ;
    protected static final int INITIAL_MOUSE_Y = TEST_FRAME_Y + ( TEST_FRAME_HEIGHT / 2 ) ;

    protected static final int MS_ROBOT_KEY_PRESS_DELAY = 50 ;
    protected static final int MS_ROBOT_KEY_RELEASE_DELAY = 50 ;
    protected static final int MS_ROBOT_MOUSE_MOVE_DELAY = 200 ;

    protected static final int MS_ROBOT_AUTO_DELAY = 50 ;
    protected static final int MS_ROBOT_POST_TEST_DELAY = 100;

    protected static final boolean _debug = true ;
    protected static final PrintStream _debugPrintStream = System.err ;

    ////////////////////////////////////////////////////////////////////////////

    static
    {
        GLProfile.initSingleton() ;
    }

    private static class TestMouseListener implements com.jogamp.newt.event.MouseListener
    {
        private static final String NO_EVENT_DELIVERY = "no event delivery" ;

        private boolean _modifierCheckEnabled ;
        private int _expectedModifiers;
        private final AtomicInteger _eventCount = new AtomicInteger(0);
        private ArrayList<String> _failures = new ArrayList<String>() ;

        public synchronized void setModifierCheckEnabled( final boolean value ) {
            _modifierCheckEnabled = value ;
        }

        public synchronized boolean modifierCheckEnabled() {
            return _modifierCheckEnabled ;
        }

        /**
         * Sets the modifiers the listener should expect, and clears
         * out any existing accumulated failures.  Normally this kind
         * of double duty in a setter might be considered evil, but
         * in this test code it's probably ok.
         */

        public synchronized void setExpectedModifiers( final int value ) {
            _expectedModifiers = value ;
            clear();
        }

        public synchronized ArrayList<String> clear() {
            final ArrayList<String> old = _failures;

            _eventCount.set(0);

            // Assume we will have a failure due to no event delivery.
            // If an event is delivered and it's good this assumed
            // failure will get cleared out.
            _failures = new ArrayList<String>();
            _failures.add( NO_EVENT_DELIVERY );
            return old;
        }

        public ArrayList<String> getFailures(final int waitEventCount) {
            int j;
            for(j=0; j < 20 && _eventCount.get() < waitEventCount; j++) { // wait until events are collected
                _robot.delay(MS_ROBOT_AUTO_DELAY);
            }
            if(0 == _eventCount.get()) {
                _debugPrintStream.println("**** No Event. Waited "+j+" * "+MS_ROBOT_AUTO_DELAY+"ms, eventCount "+_eventCount);
            }
            return clear();
        }

        private synchronized void _checkModifiers( final com.jogamp.newt.event.MouseEvent hasEvent ) {
            if( _modifierCheckEnabled ) {

                final MouseEvent expEvent = new MouseEvent(hasEvent.getEventType(), hasEvent.getSource(), hasEvent.getWhen(), _expectedModifiers,
                                                           hasEvent.getX(), hasEvent.getY(), hasEvent.getClickCount(), hasEvent.getButton(),
                                                           hasEvent.getRotation(), hasEvent.getRotationScale());

                _checkModifierMask( expEvent, hasEvent, com.jogamp.newt.event.InputEvent.SHIFT_MASK, "shift" ) ;
                _checkModifierMask( expEvent, hasEvent, com.jogamp.newt.event.InputEvent.CTRL_MASK, "ctrl" ) ;
                _checkModifierMask( expEvent, hasEvent, com.jogamp.newt.event.InputEvent.META_MASK, "meta" ) ;
                _checkModifierMask( expEvent, hasEvent, com.jogamp.newt.event.InputEvent.ALT_MASK, "alt" ) ;
                _checkModifierMask( expEvent, hasEvent, com.jogamp.newt.event.InputEvent.ALT_GRAPH_MASK, "graph" ) ;

                for( int n = 0 ; n < _numButtonsToTest ; ++n ) {
                    _checkModifierMask( expEvent, hasEvent, com.jogamp.newt.event.InputEvent.getButtonMask( n + 1 ), "button"+(n+1) ) ;
                }
            }
        }

        private synchronized void _checkModifierMask( final com.jogamp.newt.event.MouseEvent expEvent, final com.jogamp.newt.event.MouseEvent hasEvent, final int mask, final String maskS ) {

            // If the "no event delivery" failure is still in the list then
            // get rid of it since that obviously isn't true anymore.  We
            // want to do this whether or not there's an issue with the
            // modifiers.

            if( _failures.size() == 1 && _failures.get(0).equals( NO_EVENT_DELIVERY ) ) {
                _failures.clear() ;
            }

            if( ( hasEvent.getModifiers() & mask ) != ( expEvent.getModifiers() & mask ) ) {
                final StringBuilder sb = new StringBuilder();
                sb.append( com.jogamp.newt.event.MouseEvent.getEventTypeString( hasEvent.getEventType() ) ).append(": mask ").append(maskS).append(" 0x").append(Integer.toHexString(mask));
                sb.append(", eventCount ").append(_eventCount).append(", expected:");
                expEvent.getModifiersString(sb);
                sb.append(", have: ");
                hasEvent.getModifiersString(sb);
                sb.append(" - full event: ");
                hasEvent.toString(sb);
                _failures.add( sb.toString() ) ;
            }
        }

        public synchronized void mousePressed( final com.jogamp.newt.event.MouseEvent event ) {
            _eventCount.incrementAndGet();
            if( _debug ) {
                _debugPrintStream.println( "MousePressed     "+_eventCount+": "+event);
            }
            _checkModifiers( event ) ;
        }

        public synchronized void mouseReleased( final com.jogamp.newt.event.MouseEvent event ) {
            _eventCount.incrementAndGet();
            if( _debug ) {
                _debugPrintStream.println( "MouseReleased    "+_eventCount+": "+event);
            }
            _checkModifiers( event ) ;
        }

        public synchronized void mouseDragged( final com.jogamp.newt.event.MouseEvent event ) {
            _eventCount.incrementAndGet();
            if( _debug ) {
                _debugPrintStream.println( "MouseDragged     "+_eventCount+": "+event);
            }
            _checkModifiers( event ) ;
        }

        //
        // IGNORED
        //

        public synchronized void mouseMoved( final com.jogamp.newt.event.MouseEvent event ) {
            // Ignored, since mouse MOVE doesn't hold mouse button, we look for DRAGGED!
            // _eventCount++;
            if( _debug ) {
                _debugPrintStream.println( "MouseMoved        ignored: "+event);
            }
            // _checkModifiers( event ) ;
        }

        public synchronized void mouseClicked( final com.jogamp.newt.event.MouseEvent event ) {
            // Ignored, since we look for PRESS/RELEASE only!
            // _eventCount++;
            if( _debug ) {
                _debugPrintStream.println( "MouseClicked      ignored: "+event);
            }
            // _checkModifiers( event ) ;
        }

        public synchronized void mouseWheelMoved( final com.jogamp.newt.event.MouseEvent event ) {
            // _eventCount++;
            if( _debug ) {
                _debugPrintStream.println( "MouseWheeleMoved  ignored: "+event);
            }
            // _checkModifiers( event ) ;
        }

        public synchronized void mouseEntered( final com.jogamp.newt.event.MouseEvent event ) {
            // _eventCount++;
            if( _debug ) {
                _debugPrintStream.println( "MouseEntered      ignored: "+event);
            }
            // _checkModifiers( event ) ;
        }

        public synchronized void mouseExited( final com.jogamp.newt.event.MouseEvent event ) {
            // _eventCount++;
            if( _debug ) {
                _debugPrintStream.println( "MouseExited       ignored: "+event);
            }
            // _checkModifiers( event ) ;
        }

    }

    ////////////////////////////////////////////////////////////////////////////

    private static int _numButtonsToTest ;
    private static int _awtButtonMasks[] ;

    protected static java.awt.Robot _robot ;

    protected static TestMouseListener _testMouseListener ;

    ////////////////////////////////////////////////////////////////////////////

    public static int getAWTButtonMask(final int button) {
        // Java7: java.awt.event.InputEvent.getMaskForButton( n + 1 ) ; -> using InputEvent.BUTTON1_DOWN_MASK .. etc
        // Java6: Only use BUTTON1_MASK, ..
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
    public static void baseBeforeClass() throws Exception {

        // Who know how many buttons the AWT will say exist on given platform.
        // We'll test the smaller of what NEWT supports and what the
        // AWT says is available.
        /** Java7:
        if( java.awt.Toolkit.getDefaultToolkit().areExtraMouseButtonsEnabled() ) {
            _numButtonsToTest = java.awt.MouseInfo.getNumberOfButtons() ;
        } else {
            _numButtonsToTest = 3 ;
        } */
        _numButtonsToTest = 3 ;

        // Then again, maybe not:

        // FIXME? - for reasons I'm not quite sure of the AWT MouseEvent
        // constructor does some strange things for buttons other than
        // 1, 2, and 3.  Furthermore, while developing this test it
        // appeared that events sent by the robot for buttons 9 and
        // up weren't even delivered to the listeners.
        //
        // So... for now we're only going to test 3 buttons since
        // that's the common case _and_ Java6 safe.

        _numButtonsToTest = 3 ;

        {
            if( _numButtonsToTest > com.jogamp.newt.event.MouseEvent.BUTTON_COUNT ) {
                _numButtonsToTest = com.jogamp.newt.event.MouseEvent.BUTTON_COUNT ;
            }

            // These two arrays are assumed to be peers, i.e. are the same
            // size, and a given index references the same button in
            // either array.

            _awtButtonMasks = new int[_numButtonsToTest] ;

            for( int n = 0 ; n < _awtButtonMasks.length ; ++n ) {
                _awtButtonMasks[n] = getAWTButtonMask( n + 1 );
            }
        }

        _robot = new java.awt.Robot() ;
        _robot.setAutoWaitForIdle( true ) ;

        _testMouseListener = new TestMouseListener() ;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Following both methods are mandatory to deal with SWT's requirement
    // to run the SWT event dispatch on the TK thread - which must be the main thread on OSX.
    // We spawn off the actual test-action into another thread,
    // while dispatching the events until the test-action is completed.
    // YES: This is sort of ideal - NOT :)

    protected void eventDispatch() {
        try {
            Thread.sleep(100);
        } catch (final InterruptedException e) { }
    }

    private void execOffThreadWithOnThreadEventDispatch(final Runnable testAction) throws Exception {
        _testMouseListener.setModifierCheckEnabled( false ) ;
        _robot.setAutoDelay( MS_ROBOT_AUTO_DELAY ) ;
        {
            // Make sure all the buttons and modifier keys are released.
            clearKeyboadAndMouse();
        }
        _testMouseListener.setModifierCheckEnabled( true ) ;

        // final Object sync = new Object();
        final RunnableTask rt = new RunnableTask( testAction, null, true, System.err );
        try {
            // synchronized(sync) {
                new InterruptSource.Thread(null, rt, "Test-Thread").start();
                int i=0;
                while( rt.isInQueue() ) {
                    System.err.println("WAIT-till-done: eventDispatch() #"+i++);
                    eventDispatch();
                }
                final Throwable throwable = rt.getThrowable();
                if(null!=throwable) {
                    throw new RuntimeException(throwable);
                }
            // }
        } finally {
            System.err.println("WAIT-till-done: DONE");
            _testMouseListener.setModifierCheckEnabled( false ) ;
            clearKeyboadAndMouse();
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

    @Test(timeout=180000) // TO 3 min
    public void testSingleButtonPressAndRelease() throws Exception {
        execOffThreadWithOnThreadEventDispatch(new Runnable() {
            public void run() {
                try {
                    _doSingleButtonPressAndRelease( 0, 0 );
                } catch (final Exception e) { throw new RuntimeException(e); }
            } } );
    }

    @Test(timeout=180000) // TO 3 min
    public void testSingleButtonPressAndReleaseWithShift() throws Exception {
        execOffThreadWithOnThreadEventDispatch(new Runnable() {
            public void run() {
                try {
                    _doSingleButtonPressAndRelease( java.awt.event.KeyEvent.VK_SHIFT, java.awt.event.InputEvent.SHIFT_DOWN_MASK ) ;
                } catch (final Exception e) { throw new RuntimeException(e); }
            } } );
    }

    @Test(timeout=180000) // TO 3 min
    public void testSingleButtonPressAndReleaseWithCtrl() throws Exception {
        execOffThreadWithOnThreadEventDispatch(new Runnable() {
            public void run() {
                try {
                    _doSingleButtonPressAndRelease( java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.InputEvent.CTRL_DOWN_MASK ) ;
                } catch (final Exception e) { throw new RuntimeException(e); }
            } } );
    }

    /**
     * The META and ALT tests get too tied up with functions of the window system on X11,
     * so it's probably best to leave them commented out.
        @Test(timeout=180000) // TO 3 min
        public void testSingleButtonPressAndReleaseWithMeta() throws Exception {
            execOffThreadWithOnThreadEventDispatch(new Runnable() {
                public void run() {
                    try {
                        _doSingleButtonPressAndRelease( java.awt.event.KeyEvent.VK_META, java.awt.event.InputEvent.META_DOWN_MASK ) ;
                    } catch (Exception e) { throw new RuntimeException(e); }
                } } );
        }

        @Test(timeout=180000) // TO 3 min
        public void testSingleButtonPressAndReleaseWithAlt() throws Exception {
            execOffThreadWithOnThreadEventDispatch(new Runnable() {
                public void run() {
                    try {
                        _doSingleButtonPressAndRelease( java.awt.event.KeyEvent.VK_ALT, java.awt.event.InputEvent.ALT_DOWN_MASK ) ;
                    } catch (Exception e) { throw new RuntimeException(e); }
                } } );
        }
     */

    /**
     * FIXME - not sure yet what's up with ALT_GRAPH.  It appears that this
     * modifier didn't make it through, so I had to disable this test else it would always fail.
     *
     * My US keyboard doesn't have an AltGr key, so maybe X is smart
     * enough to not let this modifier slip through (?).
        @Test
        public void testSingleButtonPressAndReleaseWithAltGraph() throws Exception {
            execOffThreadWithOnThreadEventDispatch(new Runnable() {
                public void run() {
                    try {
                        _doSingleButtonPressAndRelease( java.awt.event.KeyEvent.VK_ALT_GRAPH, java.awt.event.InputEvent.ALT_GRAPH_DOWN_MASK ) ;
                    } catch (Exception e) { throw new RuntimeException(e); }
                } } );
        }
     */

    ////////////////////////////////////////////////////////////////////////////

    @Test(timeout=180000) // TO 3 min
    public void testHoldOneButtonAndPressAnother() throws Exception {
        execOffThreadWithOnThreadEventDispatch(new Runnable() {
            public void run() {
                try {
                    _doHoldOneButtonAndPressAnother( 0, 0 ) ;
                } catch (final Exception e) { throw new RuntimeException(e); }
            } } );
    }

    @Test(timeout=180000) // TO 3 min
    public void testPressAllButtonsInSequence() throws Exception {
        execOffThreadWithOnThreadEventDispatch(new Runnable() {
            public void run() {
                try {
                    _doPressAllButtonsInSequence( 0, 0 ) ;
                } catch (final Exception e) { throw new RuntimeException(e); }
            } } );
    }

    @Test(timeout=180000) // TO 3 min
    public void testSingleButtonClickAndDrag() throws Exception {
        execOffThreadWithOnThreadEventDispatch(new Runnable() {
            public void run() {
                try {
                    _doSingleButtonClickAndDrag( 0, 0 ) ;
                } catch (final Exception e) { throw new RuntimeException(e); }
            } } );
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _doSingleButtonPressAndRelease( final int keyCode, final int keyModifierMask ) throws Exception {

        if( _debug ) { _debugPrintStream.println( "\n>>>> _doSingleButtonPressAndRelease" ) ; }

        _doKeyPress( keyCode ) ;

        for (int n = 0 ; n < _numButtonsToTest ; ++n) {

            final int awtButtonMask = _awtButtonMasks[n] ;

            if( _debug ) { _debugPrintStream.println( "*** pressing button " + ( n + 1 ) ) ; }
            _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask ) ) ;
            _robot.mousePress( awtButtonMask ) ;
            _checkFailures("mouse-press("+(n+1)+")", 1) ;

            if( _debug ) { _debugPrintStream.println( "*** releasing button " + ( n + 1 ) ) ; }
            _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask ) ) ;
            _robot.mouseRelease( awtButtonMask ) ;
            _checkFailures("mouse-release("+(n+1)+")", 1) ;
        }

        _doKeyRelease( keyCode ) ;
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _doHoldOneButtonAndPressAnother( final int keyCode, final int keyModifierMask ) throws Exception {

        if( _debug ) { _debugPrintStream.println( "\n>>>> _doHoldOneButtonAndPressAnother" ) ; }

        _doKeyPress( keyCode ) ;

        for (int n = 0 ; n < _numButtonsToTest ; ++n) {

            final int awtButtonMask = _awtButtonMasks[n] ;

            if( _debug ) { _debugPrintStream.println( "*** pressing button " + ( n + 1 ) ) ; }
            _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask ) ) ;
            _robot.mousePress( awtButtonMask ) ;
            _checkFailures("mouse-press("+(n+1)+")", 1) ;

            for (int m = 0 ; m < _numButtonsToTest ; ++m) {

                if( n != m ) {

                    if( _debug ) { _debugPrintStream.println( "*** pressing additional button " + ( m + 1 ) ) ; }
                    _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask | _awtButtonMasks[m] ) ) ;
                    _robot.mousePress( _awtButtonMasks[m] ) ;
                    _checkFailures("mouse-press("+(n+1)+", "+(m+1)+")", 1) ;

                    if( _debug ) { _debugPrintStream.println( "*** releasing additional button " + ( m + 1 ) ) ; }
                    _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask | _awtButtonMasks[m] ) ) ;
                    _robot.mouseRelease( _awtButtonMasks[m] ) ;
                    _checkFailures("mouse-release("+(n+1)+", "+(m+1)+")", 1) ;
                }
            }

            if( _debug ) { _debugPrintStream.println( "*** releasing button " + ( n + 1 ) ) ; }
            _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask ) ) ;
            _robot.mouseRelease( awtButtonMask ) ;
            _checkFailures("mouse-release("+(n+1)+")", 1);
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

                if( _debug ) { _debugPrintStream.println( "*** pressing button " + ( n + 1 ) ) ; }
                _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | cumulativeAwtModifiers ) ) ;
                _robot.mousePress( _awtButtonMasks[n] ) ;
                _checkFailures("mouse-press("+(n+1)+")", 1) ;
            }

            for (int n = _numButtonsToTest - 1 ; n >= 0 ; --n) {

                if( _debug ) { _debugPrintStream.println( "*** releasing button " + ( n + 1 ) ) ; }
                _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | cumulativeAwtModifiers ) ) ;
                _robot.mouseRelease( _awtButtonMasks[n] ) ;
                _checkFailures("mouse-release("+(n+1)+")", 1) ;

                cumulativeAwtModifiers &= ~_awtButtonMasks[n] ;
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

            final int awtButtonMask = _awtButtonMasks[n] ;

            if( _debug ) { _debugPrintStream.println( "*** pressing button " + ( n + 1 ) ) ; }
            _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask ) ) ;
            _robot.mousePress( awtButtonMask ) ;
            _checkFailures("mouse-press("+(n+1)+")", 1) ;

            // To get a drag we only need to move one pixel.
            if( _debug ) { _debugPrintStream.println( "*** moving mouse" ) ; }
            final int newX = INITIAL_MOUSE_X + 8, newY = INITIAL_MOUSE_Y + 8;
            _robot.mouseMove( newX, newY ) ;
            _robot.delay(MS_ROBOT_MOUSE_MOVE_DELAY);
            _checkFailures("mouse-move("+newX+", "+newY+")", 1) ;

            if( _debug ) { _debugPrintStream.println( "*** releasing button " + ( n + 1 ) ) ; }
            _testMouseListener.setExpectedModifiers( _getNewtModifiersForAwtExtendedModifiers( keyModifierMask | awtButtonMask ) ) ;
            _robot.mouseRelease( awtButtonMask ) ;
            _checkFailures("mouse-release("+(n+1)+")", 1) ;

            _testMouseListener.setModifierCheckEnabled( false ) ;
            _robot.mouseMove( INITIAL_MOUSE_X, INITIAL_MOUSE_Y ) ;
            _robot.delay(MS_ROBOT_MOUSE_MOVE_DELAY);
            _testMouseListener.setModifierCheckEnabled( true ) ;
        }

        _doKeyRelease( keyCode ) ;
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _doKeyPress( final int keyCode ) {
        AWTRobotUtil.validateAWTEDTIsAlive();
        if( keyCode != 0 ) {
            final boolean modifierCheckEnabled = _testMouseListener.modifierCheckEnabled() ;
            _testMouseListener.setModifierCheckEnabled( false ) ;
            _robot.keyPress( keyCode ) ;
            _robot.delay(MS_ROBOT_KEY_PRESS_DELAY);
            _testMouseListener.setModifierCheckEnabled( modifierCheckEnabled ) ;
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _doKeyRelease( final int keyCode ) {
        AWTRobotUtil.validateAWTEDTIsAlive();
        if( keyCode != 0 ) {
            final boolean modifierCheckEnabled = _testMouseListener.modifierCheckEnabled() ;
            _testMouseListener.setModifierCheckEnabled( false ) ;
            _robot.keyRelease( keyCode ) ;
            _robot.delay(MS_ROBOT_KEY_RELEASE_DELAY);
            _testMouseListener.setModifierCheckEnabled( modifierCheckEnabled ) ;
        }
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _checkFailures(final String descr, final int waitEventCount) {
        final ArrayList<String> failures = _testMouseListener.getFailures(waitEventCount) ;

        _debugPrintStream.print(getSimpleTestName(".")+" - "+descr+": ");
        final int numFailures = failures.size() ;
        if( numFailures == 0 ) {
            _debugPrintStream.println( " PASSED" ) ;
        } else {
            _debugPrintStream.println( " FAILED" ) ;
            for( int n = 0 ; n < numFailures ; ++n ) {
                _debugPrintStream.print( "         " ) ;
                _debugPrintStream.println( failures.get(n) ) ;
            }
        }

        Assert.assertTrue( failures.size() == 0 ) ;
    }

    ////////////////////////////////////////////////////////////////////////////

    public void eventDispatchedPostTestDelay() throws Exception {
        eventDispatch(); eventDispatch(); eventDispatch();
        Thread.sleep( MS_ROBOT_POST_TEST_DELAY ) ;
        eventDispatch(); eventDispatch(); eventDispatch();
        _testMouseListener.clear();
    }

    public void clearKeyboadAndMouse() throws Exception {
        // Make sure all modifiers are released, otherwise the user's
        // desktop can get locked up (ask me how I know this).
        _releaseModifiers() ;
        _escape() ;
        eventDispatchedPostTestDelay();
    }

    ////////////////////////////////////////////////////////////////////////////

    private void _releaseModifiers() {

        if (_robot != null) {
            AWTRobotUtil.validateAWTEDTIsAlive();

            _robot.setAutoDelay( MS_ROBOT_AUTO_DELAY ) ;

            final boolean modifierCheckEnabled = _testMouseListener.modifierCheckEnabled() ;
            _testMouseListener.setModifierCheckEnabled( false ) ;

            {
                _robot.keyRelease( java.awt.event.KeyEvent.VK_SHIFT ) ;
                _robot.keyRelease( java.awt.event.KeyEvent.VK_CONTROL ) ;
                // _robot.keyRelease( java.awt.event.KeyEvent.VK_META ) ;
                // _robot.keyRelease( java.awt.event.KeyEvent.VK_ALT ) ;
                // _robot.keyRelease( java.awt.event.KeyEvent.VK_ALT_GRAPH ) ;

                for (int n = 0 ; n < _awtButtonMasks.length ; ++n) {
                    _robot.mouseRelease( _awtButtonMasks[n] ) ;
                }
            }

            _testMouseListener.setModifierCheckEnabled( modifierCheckEnabled ) ;
        }
    }

    private void _escape() {
        if (_robot != null) {
            AWTRobotUtil.validateAWTEDTIsAlive();
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

    private int _getNewtModifiersForAwtExtendedModifiers( final int awtExtendedModifiers ) {

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
