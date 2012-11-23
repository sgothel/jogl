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
import org.eclipse.swt.layout.GridData ;
import org.eclipse.swt.layout.GridLayout ;

import org.eclipse.swt.widgets.Composite ;
import org.eclipse.swt.widgets.Display ;
import org.eclipse.swt.widgets.Shell ;
import org.junit.Test;

import javax.media.opengl.GL ;
import javax.media.opengl.GL2 ;
import javax.media.opengl.GLAutoDrawable ;
import javax.media.opengl.GLCapabilities ;
import javax.media.opengl.GLEventListener ;
import javax.media.opengl.GLProfile;

import junit.framework.Assert;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.opengl.GLWindow ;
import com.jogamp.newt.swt.NewtCanvasSWT ;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

////////////////////////////////////////////////////////////////////////////////


public class TestNewtCanvasSWTBug628ResizeDeadlock extends UITestCase {
    
    static int duration = 1000;
    
    static class BigFlashingX implements GLEventListener
    {    
        float r = 0f, g = 0f, b = 0f;
        
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
            // System.err.println( ">>>>>>>> reshape " + x + ", " + y + ", " + width + ", " +height ) ;            
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
            // System.err.println( ">>>> display" ) ;            
            GL2 gl = drawable.getGL().getGL2() ;
    
            // Sven: I could have been seeing things, but it seemed that if this
            // glClear is in here twice it seems aggravates the problem.  Not
            // sure why other than it just takes longer, but this is pretty
            // fast operation.
            gl.glClear( GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT ) ;
            gl.glClear( GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT ) ;
    
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
    
        public void dispose( GLAutoDrawable drawable )
        {
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////
    
    static class ResizeThread extends Thread {
        boolean shallStop = false;
        private Shell _shell ;
        private int _n ;
    
        public ResizeThread( Shell shell )
        {
            super();
            _shell = shell ;
        }
        
        public void run()
        {
            // The problem was originally observed by grabbing the lower right
            // corner of the window and moving the mouse around rapidly e.g. in
            // a circle.  Eventually the UI will hang with something similar to
            // the backtrace noted in the bug report.
            //
            // This loop simulates rapid resizing by the user by toggling
            // the shell back-and-forth between two sizes.
            
            while( !shallStop )
            {
                try
                {
                    _shell.getDisplay().asyncExec( new Runnable()
                    {
                        public void run()
                        {
                            try {
                                if( _n % 2 == 0 ) {
                                    _shell.setSize( 200, 200 ) ;
                                } else {
                                    _shell.setSize( 400, 450 ) ;
                                }
                            } catch (Exception e0) {
                                e0.printStackTrace();
                                Assert.assertTrue("Deadlock @ setSize: "+e0, false);
                            }                            
                            ++_n ;
                        }
                    } ) ;
                    
                    Thread.sleep( 50L ) ;
                } catch( InterruptedException e ) {
                    break ;
                }
            }
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////
    
    static class KeyfireThread extends Thread
    {
        boolean shallStop = false;
        Robot _robot;
        int _n = 0;
    
        public KeyfireThread(Robot robot)
        {
            _robot = robot;
        }
        
        public void run()
        {
            while( !shallStop )
            {
                try {
                    AWTRobotUtil.keyPress(_n, _robot, true, KeyEvent.VK_0, 10);
                    AWTRobotUtil.keyPress(_n, _robot, false, KeyEvent.VK_0, 0);
                    Thread.sleep( 40L ) ;
                } catch( InterruptedException e ) {
                    break ;
                }
            }
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////
    
    @Test
    public void test() throws InterruptedException, AWTException, InvocationTargetException {
        final int columnCount = 1;
        final Display display = new Display() ;

        final Shell shell = new Shell( display ) ;
        shell.setLayout( new FillLayout() ) ;
        
        final Robot robot = new Robot();
        
        Composite composite = new Composite( shell, SWT.NONE ) ;
        {
            GridLayout layout = new GridLayout() ;
            layout.numColumns = columnCount ;
            composite.setLayout( layout ) ;
        }

        final GLWindow glWindow;
        {
            final GLProfile gl2Profile = GLProfile.get( GLProfile.GL2 ) ;
            GLCapabilities caps = new GLCapabilities( gl2Profile ) ;
            glWindow = GLWindow.create( caps ) ;
            glWindow.addGLEventListener( new BigFlashingX() ) ;
            glWindow.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(com.jogamp.newt.event.KeyEvent e) {
                    System.err.print(".");
                    glWindow.display();                    
                }                
            });
            NewtCanvasSWT canvas = NewtCanvasSWT.create( composite, SWT.NO_BACKGROUND, glWindow ) ;
            canvas.setLayoutData( new GridData( SWT.FILL, SWT.FILL, true, true, columnCount, 1 ) ) ;
        }
            
        shell.setText( "NewtCanvasSWT Resize Bug Demo" ) ;
        shell.setSize( 400, 450 ) ;
        shell.open() ;
        
        AWTRobotUtil.requestFocus(robot, glWindow, false);
        AWTRobotUtil.setMouseToClientLocation(robot, glWindow, 50, 50);

        final ResizeThread resizer;
        {
            resizer = new ResizeThread( shell ) ;
            resizer.start() ;
        }
        
        final KeyfireThread keyfire;
        {
            keyfire = new KeyfireThread( robot ) ;
            keyfire.start() ;
        }
        
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {}
                    resizer.shallStop = true;
                    keyfire.shallStop = true;
                    try
                    {
                        resizer.join();
                    } catch( InterruptedException e ) { }
                    try
                    {
                        keyfire.join();
                    } catch( InterruptedException e ) { }
                    display.syncExec( new Runnable() {
                        @Override
                        public void run() {
                            shell.dispose();                            
                        } } );
                } } ).start();
        }
        
        try {
            while( !shell.isDisposed() ) {
                if( !display.readAndDispatch() ) {
                    display.sleep();
                }
            }
        } catch (Exception e0) {
            e0.printStackTrace();
            Assert.assertTrue("Deadlock @ dispatch: "+e0, false);
        }
        
        display.dispose() ;
    }
    
    public static void main( String[] args ) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = MiscUtils.atoi(args[++i],  duration);
            }
        }
        System.out.println("durationPerTest: "+duration);
        org.junit.runner.JUnitCore.main(TestNewtCanvasSWTBug628ResizeDeadlock.class.getName());        
    }
    
}
