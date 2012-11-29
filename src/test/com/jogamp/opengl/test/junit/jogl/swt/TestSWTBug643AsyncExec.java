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
import java.lang.reflect.InvocationTargetException;

import org.eclipse.swt.SWT ;

import org.eclipse.swt.layout.FillLayout ;

import org.eclipse.swt.widgets.Composite ;
import org.eclipse.swt.widgets.Display ;
import org.eclipse.swt.widgets.Shell ;
import org.junit.Assume;
import org.junit.Test;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities ;
import javax.media.opengl.GLProfile;

import junit.framework.Assert;

import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.newt.opengl.GLWindow ;
import com.jogamp.newt.swt.NewtCanvasSWT ;
import com.jogamp.opengl.swt.GLCanvas;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;

////////////////////////////////////////////////////////////////////////////////


public class TestSWTBug643AsyncExec extends UITestCase {
    
    static int duration = 500;
    
    ////////////////////////////////////////////////////////////////////////////////
    
    static void resetAsyncExecCount() {
        synchronized(asyncExecCountSync) {
            asyncExecCount=0;
        }
    }
    static int incrAsyncExecCount() {
        synchronized(asyncExecCountSync) {
            asyncExecCount++;
            return asyncExecCount;
        }
    }
    static int getAsyncExecCount() {
        synchronized(asyncExecCountSync) {
            return asyncExecCount;
        }
    }
    static Object asyncExecCountSync = new Object();
    static int asyncExecCount = 0;    
    
    ////////////////////////////////////////////////////////////////////////////////
    
    static class AsyncExecFeederThread extends Thread {
        volatile boolean shallStop = false;
        private Display display ;
        private int n ;
    
        public AsyncExecFeederThread( Display display )
        {
            super();
            this.display = display ;
        }
        
        final Runnable asyncAction = new Runnable() {
            public void run()
            {
                ++n ;
                System.err.println("[A-i shallStop "+shallStop+", disposed "+display.isDisposed()+"]: Counter[loc "+n+", glob: "+incrAsyncExecCount()+"]");
            }  };
        
        public void run()
        {
            System.err.println("[A-0 shallStop "+shallStop+", disposed "+display.isDisposed()+"]");
            
            // final Display d = Display.getDefault();
            final Display d = this.display;
            
            while( !shallStop && !d.isDisposed() )
            {
                try
                {
                    System.err.println("[A-n shallStop "+shallStop+", disposed "+d.isDisposed()+"]");
                    d.asyncExec( asyncAction );
                    d.wake();
                    
                    Thread.sleep( 50L ) ;                    
                } catch( InterruptedException e ) {
                    break ;
                }
            }
            System.err.println("*R-Exit* shallStop "+shallStop+", disposed "+d.isDisposed());
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////
    
    private volatile boolean shallStop = false;
    
    static class SWT_DSC {
        Display display;
        Shell shell;
        Composite composite;
        
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
            catch( Throwable throwable ) {
                throwable.printStackTrace();
                Assume.assumeNoException( throwable );
            }
            display = null;
            shell = null;
            composite = null;            
        }
    }
    
    private void testImpl(boolean useJOGLGLCanvas, boolean useNewtCanvasSWT) throws InterruptedException, AWTException, InvocationTargetException {
        resetAsyncExecCount();
        
        final SWT_DSC dsc = new SWT_DSC();
        dsc.init();
                
        {
            final GLProfile gl2Profile = GLProfile.get( GLProfile.GL2 ) ;
            final GLCapabilities caps = new GLCapabilities( gl2Profile ) ;
            
            final GLAutoDrawable glad;
            if( useJOGLGLCanvas ) {
                glad = GLCanvas.create(dsc.composite, 0, caps, null, null);
            } else if( useNewtCanvasSWT ) {
                final GLWindow glWindow = GLWindow.create( caps ) ;
                glad = glWindow;
                NewtCanvasSWT.create( dsc.composite, 0, glWindow ) ;                
            } else {
                throw new InternalError("XXX");
            }
            glad.addGLEventListener( new GearsES2() ) ;
        }
            
        dsc.display.syncExec( new Runnable() {
            public void run() {
               dsc.shell.setText( "NewtCanvasSWT Resize Bug Demo" ) ;
               dsc.shell.setSize( 400, 450 ) ;
               dsc.shell.open() ;
            } } );

        shallStop = false;
        
        final int[] ayncExecCountBeforeExit = new int[] { 0 };
        
        final AsyncExecFeederThread asyncExecFeeder;
        {
            asyncExecFeeder = new AsyncExecFeederThread( dsc.display) ;
            asyncExecFeeder.start() ;
        }
        
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {}
                    
                    ayncExecCountBeforeExit[0] = getAsyncExecCount();
                    asyncExecFeeder.shallStop = true;
                    try
                    {
                        asyncExecFeeder.join();
                    } catch( InterruptedException e ) { }
                    shallStop = true;
                    dsc.display.wake();
                } } ).start();
        }
                
        try {
            final Display d = dsc.display;            
            while( !shallStop && !d.isDisposed() ) {
                final boolean r = d.readAndDispatch();
                System.err.print(",");
                if( !r ) {
                    dsc.display.sleep();
                }
            }
        } catch (Exception e0) {
            e0.printStackTrace();
            Assert.assertTrue("Deadlock @ dispatch: "+e0, false);
        }
                
        // canvas is disposed implicit, due to it's disposed listener !
        
        dsc.dispose();
        
        System.err.println("AsyncExecCount before exit: " + ayncExecCountBeforeExit[0]);
        Assert.assertTrue("AsyncExecCount not greater zero before dispose!", 0 < ayncExecCountBeforeExit[0]);        
    }

    @Test
    public void test01JOGLGLCanvas() throws InterruptedException, AWTException, InvocationTargetException {
        testImpl(true /* useJOGLGLCanvas */, false /* useNewtCanvasSWT */);
    }
    
    @Test
    public void test02NewtCanvasSWT() throws InterruptedException, AWTException, InvocationTargetException {
        testImpl(false /* useJOGLGLCanvas */, true /* useNewtCanvasSWT */);
    }
    
    public static void main( String[] args ) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = MiscUtils.atoi(args[++i],  duration);
            }
        }
        System.out.println("durationPerTest: "+duration);
        org.junit.runner.JUnitCore.main(TestSWTBug643AsyncExec.class.getName());        
    }
    
}
