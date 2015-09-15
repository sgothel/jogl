/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.swt;

import java.awt.AWTException;
import java.awt.Robot;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.SWT ;
import org.eclipse.swt.layout.FillLayout ;
import org.eclipse.swt.widgets.Composite ;
import org.eclipse.swt.widgets.Display ;
import org.eclipse.swt.widgets.Shell ;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.GL ;
import com.jogamp.opengl.GL2 ;
import com.jogamp.opengl.GLAutoDrawable ;
import com.jogamp.opengl.GLCapabilities ;
import com.jogamp.opengl.GLEventListener ;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.InterruptedRuntimeException;
import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow ;
import com.jogamp.newt.swt.NewtCanvasSWT ;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

////////////////////////////////////////////////////////////////////////////////


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNewtCanvasSWTBug628ResizeDeadlockAWT extends UITestCase {

    static int duration = 500;

    static class BigFlashingX implements GLEventListener
    {
        float r = 0f, g = 0f, b = 0f;

        public void init( final GLAutoDrawable drawable )
        {
            final GL2 gl = drawable.getGL().getGL2() ;

            gl.glClearColor( 0.0f, 0.0f, 0.0f, 1.0f ) ;

            gl.glEnable( GL.GL_LINE_SMOOTH ) ;
            gl.glEnable( GL.GL_BLEND ) ;
            gl.glBlendFunc( GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA ) ;
        }

        public void reshape( final GLAutoDrawable drawable, final int x, final int y, final int width, final int height )
        {
            // System.err.println( ">>>>>>>> reshape " + x + ", " + y + ", " + width + ", " +height ) ;
            final GL2 gl = drawable.getGL().getGL2() ;

            gl.glViewport( 0, 0, width, height ) ;

            gl.glMatrixMode( GLMatrixFunc.GL_PROJECTION ) ;
            gl.glLoadIdentity() ;
            gl.glOrtho( -1.0, 1.0, -1.0, 1.0, -1.0, 1.0 ) ;

            gl.glMatrixMode( GLMatrixFunc.GL_MODELVIEW ) ;
            gl.glLoadIdentity() ;
        }

        public void display( final GLAutoDrawable drawable )
        {
            // System.err.println( ">>>> display" ) ;
            final GL2 gl = drawable.getGL().getGL2() ;

            // Sven: I could have been seeing things, but it seemed that if this
            // glClear is in here twice it seems aggravates the problem.  Not
            // sure why other than it just takes longer, but this is pretty
            // fast operation.
            gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT ) ;
            gl.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT ) ;

            gl.glColor4f( r, g, b, 1.0f ) ;

            gl.glBegin( GL.GL_LINES ) ;
            {
                gl.glVertex2f( -1.0f,  1.0f ) ;
                gl.glVertex2f(  1.0f, -1.0f ) ;

                gl.glVertex2f( -1.0f, -1.0f ) ;
                gl.glVertex2f(  1.0f,  1.0f ) ;
            }
            gl.glEnd() ;

            if(r<1f) {
                r+=0.1f;
            } else if(g<1f) {
                g+=0.1f;
            } else if(b<1f) {
                b+=0.1f;
            } else {
                r = 0f;
                g = 0f;
                b = 0f;
            }
        }

        public void dispose( final GLAutoDrawable drawable )
        {
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    static class ResizeThread extends InterruptSource.Thread {
        volatile boolean shallStop = false;
        private final Shell _shell ;
        private int _n ;

        public ResizeThread( final Shell shell )
        {
            _shell = shell ;
        }

        final Runnable resizeAction = new Runnable() {
            public void run()
            {
                System.err.println("[R-i shallStop "+shallStop+", disposed "+_shell.isDisposed()+"]");
                if( shallStop || _shell.isDisposed() ) {
                    return;
                }
                try {
                    if( _n % 2 == 0 ) {
                        _shell.setSize( 200, 200 ) ;
                    } else {
                        _shell.setSize( 400, 450 ) ;
                    }
                } catch (final Exception e0) {
                    e0.printStackTrace();
                    Assert.assertTrue("Deadlock @ setSize: "+e0, false);
                }
                ++_n ;
            }  };

        public void run()
        {
            // The problem was originally observed by grabbing the lower right
            // corner of the window and moving the mouse around rapidly e.g. in
            // a circle.  Eventually the UI will hang with something similar to
            // the backtrace noted in the bug report.
            //
            // This loop simulates rapid resizing by the user by toggling
            // the shell back-and-forth between two sizes.

            System.err.println("[R-0 shallStop "+shallStop+", disposed "+_shell.isDisposed()+"]");

            final Display display = _shell.getDisplay();

            while( !shallStop && !_shell.isDisposed() )
            {
                try
                {
                    System.err.println("[R-n shallStop "+shallStop+", disposed "+_shell.isDisposed()+"]");
                    display.asyncExec( resizeAction );
                    display.wake();

                    java.lang.Thread.sleep( 50L ) ;
                } catch( final InterruptedException e ) {
                    throw new InterruptedRuntimeException(e);
                }
            }
            System.err.println("*R-Exit* shallStop "+shallStop+", disposed "+_shell.isDisposed());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    static class KeyfireThread extends InterruptSource.Thread
    {
        volatile boolean shallStop = false;
        Display _display;
        Robot _robot;
        int _n = 0;

        public KeyfireThread(final Robot robot, final Display display)
        {
            super();
            _robot = robot;
            _display = display;
        }

        public void run()
        {
            System.err.println("[K-0]");

            while( !shallStop )
            {
                try {
                    System.err.println("[K-"+_n+"]");
                    AWTRobotUtil.waitForIdle(_robot);
                    AWTRobotUtil.newtKeyPress(_n, _robot, true, KeyEvent.VK_0, 10);
                    AWTRobotUtil.newtKeyPress(_n, _robot, false, KeyEvent.VK_0, 0);
                    java.lang.Thread.sleep( 40L ) ;
                    _n++;
                    if(!_display.isDisposed()) {
                        _display.wake();
                    }
                } catch( final InterruptedException e ) {
                    break ;
                }
            }
            System.err.println("*K-Exit*");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    private volatile boolean shallStop = false;

    static class SWT_DSC {
        volatile Display display;
        volatile Shell shell;
        volatile Composite composite;
        volatile com.jogamp.newt.Display swtNewtDisplay = null;

        public void init() {
            SWTAccessor.invoke(true, new Runnable() {
                public void run() {
                    display = new Display();
                    Assert.assertNotNull( display );
                }});

            display.syncExec(new Runnable() {
                public void run() {
                    shell = new Shell( display );
                    Assert.assertNotNull( shell );
                    shell.setLayout( new FillLayout() );
                    composite = new Composite( shell, SWT.NO_BACKGROUND );
                    composite.setLayout( new FillLayout() );
                    Assert.assertNotNull( composite );
                }});
            swtNewtDisplay = NewtFactory.createDisplay(null, false); // no-reuse
        }

        public void dispose() {
            Assert.assertNotNull( display );
            Assert.assertNotNull( shell );
            Assert.assertNotNull( composite );
            try {
                display.syncExec(new Runnable() {
                   public void run() {
                    composite.dispose();
                    shell.dispose();
                   }});
                SWTAccessor.invoke(true, new Runnable() {
                   public void run() {
                    display.dispose();
                   }});
            }
            catch( final Throwable throwable ) {
                throwable.printStackTrace();
                Assume.assumeNoException( throwable );
            }
            swtNewtDisplay = null;
            display = null;
            shell = null;
            composite = null;
        }
        class WaitAction implements Runnable {
            private final long sleepMS;

            WaitAction(final long sleepMS) {
                this.sleepMS = sleepMS;
            }
            public void run() {
                if( !display.readAndDispatch() ) {
                    // blocks on linux .. display.sleep();
                    try {
                        Thread.sleep(sleepMS);
                    } catch (final InterruptedException e) { }
                }
            }
        }
        final WaitAction awtRobotWaitAction = new WaitAction(AWTRobotUtil.TIME_SLICE);
    }

    @Test
    public void test() throws InterruptedException, AWTException, InvocationTargetException {
        final Robot robot = new Robot();

        final SWT_DSC dsc = new SWT_DSC();
        dsc.init();

        final GLWindow glWindow;
        {
            final GLProfile gl2Profile = GLProfile.get( GLProfile.GL2 ) ;
            final GLCapabilities caps = new GLCapabilities( gl2Profile ) ;
            final com.jogamp.newt.Screen screen = NewtFactory.createScreen(dsc.swtNewtDisplay, 0);
            glWindow = GLWindow.create( screen, caps ) ;
            glWindow.addGLEventListener( new BigFlashingX() ) ;
            glWindow.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(final com.jogamp.newt.event.KeyEvent e) {
                    if( !e.isPrintableKey() || e.isAutoRepeat() ) {
                        return;
                    }
                    System.err.print(".");
                    glWindow.display();
                }
            });
            NewtCanvasSWT.create( dsc.composite, 0, glWindow ) ;
        }

        dsc.display.syncExec( new Runnable() {
            public void run() {
               dsc.shell.setText( "NewtCanvasSWT Resize Bug Demo" ) ;
               dsc.shell.setSize( 400, 450 ) ;
               dsc.shell.open() ;
            } } );
        Assert.assertTrue("GLWindow didn't become visible natively!", AWTRobotUtil.waitForRealized(glWindow, dsc.awtRobotWaitAction, true));

        AWTRobotUtil.requestFocus(robot, glWindow, false);
        AWTRobotUtil.setMouseToClientLocation(robot, glWindow, 50, 50);

        shallStop = false;

        final ResizeThread resizer;
        {
            resizer = new ResizeThread( dsc.shell ) ;
            resizer.start() ;
        }

        final KeyfireThread keyfire;
        {
            keyfire = new KeyfireThread( robot, dsc.display ) ;
            keyfire.start() ;
        }

        {
            final Thread t = new InterruptSource.Thread(null, new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(duration);
                    } catch (final InterruptedException e) {}
                    resizer.shallStop = true;
                    keyfire.shallStop = true;
                    try
                    {
                        resizer.join();
                    } catch( final InterruptedException e ) { }
                    try
                    {
                        keyfire.join();
                    } catch( final InterruptedException e ) { }
                    shallStop = true;
                    if( null != dsc.display && !dsc.display.isDisposed() )  {
                        dsc.display.wake();
                    }
                } } );
            t.setDaemon(true);
            t.start();
        }

        try {
            while( !shallStop && !dsc.display.isDisposed() ) {
                dsc.display.syncExec( new Runnable() {
                    public void run() {
                       if( !dsc.display.isDisposed() && !dsc.display.readAndDispatch() && !shallStop ) {
                           // blocks on linux .. dsc.display.sleep();
                           try {
                               Thread.sleep(10);
                           } catch (final InterruptedException ie) { ie.printStackTrace(); }
                       }
                    } } );
            }
        } catch (final Exception e0) {
            e0.printStackTrace();
            Assert.assertTrue("Deadlock @ dispatch: "+e0, false);
        }

        // canvas is disposed implicit, due to it's disposed listener !

        dsc.dispose();
    }

    public static void main( final String[] args ) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = MiscUtils.atoi(args[++i],  duration);
            }
        }
        System.out.println("durationPerTest: "+duration);
        org.junit.runner.JUnitCore.main(TestNewtCanvasSWTBug628ResizeDeadlockAWT.class.getName());
    }

}
