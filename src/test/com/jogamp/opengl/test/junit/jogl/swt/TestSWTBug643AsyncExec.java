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

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities ;
import com.jogamp.opengl.GLProfile;

import jogamp.newt.swt.SWTEDTUtil;
import jogamp.newt.swt.event.SWTNewtEventFactory;

import com.jogamp.common.util.InterruptSource;
import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.opengl.GLWindow ;
import com.jogamp.newt.swt.NewtCanvasSWT ;
import com.jogamp.opengl.swt.GLCanvas;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

////////////////////////////////////////////////////////////////////////////////


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSWTBug643AsyncExec extends UITestCase {

    static int duration = 500;
    static boolean useAnimator = false;

    ////////////////////////////////////////////////////////////////////////////////

    static void resetSWTAndNEWTEDTCounter() {
        synchronized(swtCountSync) {
            swtCount=0;
        }
        synchronized(edtCountSync) {
            edtCount=0;
        }
    }
    static int incrSWTCount() {
        synchronized(swtCountSync) {
            swtCount++;
            return swtCount;
        }
    }
    static int getSWTCount() {
        synchronized(swtCountSync) {
            return swtCount;
        }
    }
    static int incrNEWTCount() {
        synchronized(edtCountSync) {
            edtCount++;
            return edtCount;
        }
    }
    static int getNEWTCount() {
        synchronized(edtCountSync) {
            return edtCount;
        }
    }
    static Object swtCountSync = new Object();
    static int swtCount = 0;
    static Object edtCountSync = new Object();
    static int edtCount = 0;

    ////////////////////////////////////////////////////////////////////////////////

    static class AsyncExecEDTFeederThread extends InterruptSource.Thread {
        volatile boolean shallStop = false;
        private final Display swtDisplay ;
        private final jogamp.newt.DisplayImpl newtDisplay;
        private int swtN, newtN ;

        public AsyncExecEDTFeederThread( final Display swtDisplay, final com.jogamp.newt.Display newtDisplay )
        {
            this.swtDisplay = swtDisplay ;
            this.newtDisplay = (jogamp.newt.DisplayImpl)newtDisplay;
        }

        final Runnable swtAsyncAction = new Runnable() {
            public void run()
            {
                ++swtN ; incrSWTCount();
                System.err.println("[SWT A-i shallStop "+shallStop+"]: Counter[loc "+swtN+", glob: "+getSWTCount()+"]");
            }  };

        final Runnable newtAsyncAction = new Runnable() {
            public void run()
            {
                ++newtN ; incrNEWTCount();
                System.err.println("[NEWT A-i shallStop "+shallStop+"]: Counter[loc "+newtN+", glob: "+getNEWTCount()+"]");
            }  };

        public void run()
        {
            System.err.println("[A-0 shallStop "+shallStop+"]");

            while( !shallStop && !swtDisplay.isDisposed() )
            {
                try
                {
                    if( !swtDisplay.isDisposed() ) {
                        swtDisplay.asyncExec( swtAsyncAction );
                    }
                    if(null != newtDisplay && newtDisplay.isNativeValid() && newtDisplay.getEDTUtil().isRunning()) {
                        // only perform async exec on valid and already running NEWT EDT!
                        newtDisplay.runOnEDTIfAvail(false, newtAsyncAction);
                    }
                    java.lang.Thread.sleep( 50L ) ;
                } catch( final InterruptedException e ) {
                    break ;
                }
            }
            System.err.println("*R-Exit* shallStop "+shallStop);
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
            catch( final Throwable throwable ) {
                throwable.printStackTrace();
                Assume.assumeNoException( throwable );
            }
            display = null;
            shell = null;
            composite = null;
        }
    }

    private void testImpl(final boolean useJOGLGLCanvas, final boolean useNewtCanvasSWT, final boolean glWindowPreVisible) throws InterruptedException, InvocationTargetException {
        resetSWTAndNEWTEDTCounter();

        final SWT_DSC dsc = new SWT_DSC();
        dsc.init();

        final com.jogamp.newt.Display newtDisplay;
        {
            final GLProfile gl2Profile = GLProfile.get( GLProfile.GL2 ) ;
            final GLCapabilities caps = new GLCapabilities( gl2Profile ) ;

            final GLAutoDrawable glad;
            if( useJOGLGLCanvas ) {
                final GearsES2 demo = new GearsES2();
                final GLCanvas glc = GLCanvas.create(dsc.composite, 0, caps, null);
                final SWTNewtEventFactory swtNewtEventFactory = new SWTNewtEventFactory();
                swtNewtEventFactory.attachDispatchListener(glc, glc, demo.gearsMouse, demo.gearsKeys);
                glc.addGLEventListener( demo ) ;
                glad = glc;
                newtDisplay = null;
            } else if( useNewtCanvasSWT ) {
                newtDisplay = NewtFactory.createDisplay(null, false); // no-reuse
                final com.jogamp.newt.Screen screen = NewtFactory.createScreen(newtDisplay, 0);
                final GLWindow glWindow = GLWindow.create( screen, caps ) ;
                glWindow.addGLEventListener( new GearsES2() ) ;
                if( glWindowPreVisible ) {
                    newtDisplay.setEDTUtil(new SWTEDTUtil(newtDisplay, dsc.display)); // Especially Windows requires creation access via same thread!
                    glWindow.setVisible(true);
                    AWTRobotUtil.waitForRealized(glWindow, true);
                    Thread.sleep(120); // let it render a bit, before consumed by SWT
                }
                glad = glWindow;
                NewtCanvasSWT.create( dsc.composite, 0, glWindow ) ;
            } else {
                throw new InternalError("XXX");
            }
            if(useAnimator) {
                final Animator animator = new Animator(glad);
                animator.start();
            }
        }

        System.err.println("**** Pre Shell Open");
        dsc.display.syncExec( new Runnable() {
            public void run() {
               dsc.shell.setText( "NewtCanvasSWT Resize Bug Demo" ) ;
               dsc.shell.setSize( 400, 450 ) ;
               dsc.shell.open() ;
            } } );
        System.err.println("**** Post Shell Open");

        shallStop = false;

        final int[] counterBeforeExit = new int[] { 0 /* SWT */, 0 /* NEWT */ };

        final AsyncExecEDTFeederThread asyncExecFeeder;
        {
            asyncExecFeeder = new AsyncExecEDTFeederThread(dsc.display, newtDisplay) ;
            asyncExecFeeder.start() ;
        }

        {
            final Thread t = new InterruptSource.Thread(null, new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(duration);
                    } catch (final InterruptedException e) {}

                    counterBeforeExit[0] = getSWTCount();
                    counterBeforeExit[1] = getNEWTCount();
                    asyncExecFeeder.shallStop = true;
                    try
                    {
                        asyncExecFeeder.join();
                    } catch( final InterruptedException e ) { }
                    shallStop = true;
                    dsc.display.wake();
                } } );
            t.setDaemon(true);
            t.start();
        }

        try {
            final Display d = dsc.display;
            while( !shallStop && !d.isDisposed() ) {
                if( !d.readAndDispatch() && !shallStop ) {
                    // blocks on linux .. dsc.display.sleep();
                    Thread.sleep(10);
                }
            }
        } catch (final Exception e0) {
            e0.printStackTrace();
            Assert.assertTrue("Deadlock @ dispatch: "+e0, false);
        }

        // canvas is disposed implicit, due to it's disposed listener !

        dsc.dispose();

        System.err.println("EDT Counter before exit: SWT " + counterBeforeExit[0] + ", NEWT "+counterBeforeExit[1]);
        Assert.assertTrue("SWT EDT Counter not greater zero before dispose!", 0 < counterBeforeExit[0]);
        if( null != newtDisplay ) {
            Assert.assertTrue("NEWT EDT Counter not greater zero before dispose!", 0 < counterBeforeExit[1]);
        }
    }

    @Test
    public void test01JOGLGLCanvas() throws InterruptedException, InvocationTargetException {
        testImpl(true /* useJOGLGLCanvas */, false /* useNewtCanvasSWT */, false /* glWindowPreVisible */);
    }

    @Test
    public void test02NewtCanvasSWTSimple() throws InterruptedException, InvocationTargetException {
        testImpl(false /* useJOGLGLCanvas */, true /* useNewtCanvasSWT */, false /* glWindowPreVisible */);
    }

    @Test
    public void test02NewtCanvasSWTPreVisible() throws InterruptedException, InvocationTargetException {
        testImpl(false /* useJOGLGLCanvas */, true /* useNewtCanvasSWT */, true /* glWindowPreVisible */);
    }

    public static void main( final String[] args ) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = MiscUtils.atoi(args[++i],  duration);
            } else if(args[i].equals("-anim")) {
                useAnimator = true;
            }
        }
        System.out.println("durationPerTest: "+duration);
        org.junit.runner.JUnitCore.main(TestSWTBug643AsyncExec.class.getName());
    }

}
