/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.javafx;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.util.RunnableTask;
import com.jogamp.nativewindow.javafx.JFXAccessor;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.javafx.NewtCanvasJFX;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.opengl.util.NEWTDemoListener;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.MultisampleDemoES2;
import com.jogamp.opengl.test.junit.newt.parenting.NewtJFXReparentingKeyAdapter;
import com.jogamp.opengl.test.junit.newt.parenting.NewtReparentingKeyAdapter;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NewtTestUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * {@link NewtCanvasJFX} basic functional integration test
 * of its native parented NEWT child {@link GLWindow} attached to JavaFX's {@link Canvas}.
 * <p>
 * {@link NewtCanvasJFX} allows utilizing custom {@link GLCapabilities} settings independent from the JavaFX's window
 * as well as independent rendering from JavaFX's thread.
 * </p>
 * <p>
 * This unit tests also tests {@link NewtCanvasJFX} native parenting operations before and after
 * it's belonging Group's Scene has been attached to the JavaFX {@link javafx.stage.Window Window}'s actual native window,
 * i.e. becoming fully realized and visible.
 * </p>
 * <p>
 * Note that {@link JFXAccessor#runOnJFXThread(boolean, Runnable)} is still used to for certain
 * mandatory JavaFX lifecycle operation on the JavaFX thread.
 * </p>
 * <p>
 * The demo code uses {@link NewtReparentingKeyAdapter} including {@link NEWTDemoListener} functionality.
 * </p>
 * <p>
 * Manual invocation via main allows running a single test, e.g. {@code -test 21}, and setting each tests's duration in milliseconds, e.g.{@code -time 10000}.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNewtCanvasJFXGLn extends UITestCase {

    static int duration = 5000; // 250;
    static int manualTestID = -1;

    com.jogamp.newt.Display jfxNewtDisplay = null;

    public static class JFXApp extends Application {
        static Stage stage;

        final static Object sync = new Object();
        static volatile boolean isLaunched = false;

        public JFXApp() {
        }

        @Override public void init() throws Exception {
            // pre JFX thread
            System.err.println("JFX init ...: "+Thread.currentThread());
        }

        @Override public void start(final Stage stage) {
            System.err.println("JFX start.0 ...: "+Thread.currentThread());
            synchronized(sync) {
                try {
                    // on JFX thread
                    final Scene scene = new Scene(new Group(), defWidth, defHeight);
                    stage.setTitle(TestNewtCanvasJFXGLn.class.getSimpleName());
                    stage.setScene(scene);
                    stage.sizeToScene();
                    {
                        final long h = JFXAccessor.getWindowHandle(stage);
                        System.err.println("t1 - Native window: 0x"+Long.toHexString(h));
                    }
                    stage.show();
                    {
                        final long h = JFXAccessor.getWindowHandle(stage);
                        System.err.println("t2 - Native window: 0x"+Long.toHexString(h));
                    }
                    JFXApp.stage = stage;
                } finally {
                    isLaunched = true;
                    sync.notifyAll();
                }
            }
            System.err.println("JFX start.X ...: "+Thread.currentThread());
        }
        @Override public void stop() throws Exception {
            System.err.println("JFX stop ...: "+Thread.currentThread());
        }
        public static void startup() throws InterruptedException {
            System.out.println( "GLProfile " + GLProfile.glAvailabilityToString() );
            System.err.println("JFX Available: "+JFXAccessor.isJFXAvailable());
            if( JFXAccessor.isJFXAvailable() ) {
                Platform.setImplicitExit(false); // FIXME: Default for all NEWT cases?
                synchronized(sync) {
                    final Thread ct = Thread.currentThread();
                    RunnableTask.invokeOnNewThread(ct.getThreadGroup(), ct.getName()+"JFXLauncher", false,
                                                   new Runnable() {
                       public void run() {
                           Application.launch(JFXApp.class);
                       }
                    });
                    while(!isLaunched) {
                        sync.wait();
                    }
                }
                System.err.println("JFX launched ...");
            }
        }
        public static void shutdown() {
            JFXAccessor.runOnJFXThread(true, new Runnable() {
                    public void run() {
                        if( null != stage ) {
                            stage.close();
                        }
                    } });
        }
    }

    @BeforeClass
    public static void startup() throws InterruptedException {
        JFXApp.startup();
    }

    @AfterClass
    public static void shutdown() {
        JFXApp.shutdown();
        Platform.exit();
    }

    @Before
    public void init() {
        jfxNewtDisplay = NewtFactory.createDisplay(null, false); // no-reuse
    }

    @After
    public void release() {
        jfxNewtDisplay = null;
    }

    class WaitAction implements Runnable {
        private final long sleepMS;

        WaitAction(final long sleepMS) {
            this.sleepMS = sleepMS;
        }
        public void run() {
            // blocks on linux .. display.sleep();
            try {
                Thread.sleep(sleepMS);
            } catch (final InterruptedException e) { }
        }
    }
    final WaitAction awtRobotWaitAction = new WaitAction(AWTRobotUtil.TIME_SLICE);
    final WaitAction generalWaitAction = new WaitAction(10);

    static final int defWidth = 800, defHeight = 600;

    static void populateScene(final Scene scene, final boolean postAttach,
                              final GLWindow glWindow,
                              final int width, final int height, final boolean useBorder,
                              final NewtCanvasJFX[] res) {
        final javafx.stage.Window w = scene.getWindow();
        final boolean isShowing = null != w && w.isShowing();
        final Group g = new Group();

        final int cx, cy, cw, ch, bw, bh;
        if( useBorder ) {
            bw = width/5; bh = height/5;
            cx = bw; cy = bh; cw = width-bw-bw; ch = height-bh-bh;
        } else {
            bw = 0; bh = 0;
            cx = 0; cy = 0; cw = width; ch = height;
        }
        System.err.println("Scene "+width+"x"+height+", isShowing "+isShowing+", postAttach "+postAttach);
        System.err.println("Scene.canvas "+cx+"/"+cy+" "+cw+"x"+ch);
        System.err.println("Scene.border "+bw+"x"+bh);

        if( !postAttach ) {
            if(isShowing) {
                JFXAccessor.runOnJFXThread(true, new Runnable() {
                    @Override
                    public void run() {
                        scene.setRoot(g);
                    }});
            } else {
                scene.setRoot(g);
            }
        }

        final Canvas canvas0;
        if( null == res ) {
            canvas0 = new Canvas();
        } else {
            res[0] = new NewtCanvasJFX( glWindow );
            canvas0 = res[0];
        }
        canvas0.setWidth(cw);
        canvas0.setHeight(ch);
        if( null == res ) {
            final GraphicsContext gc = canvas0.getGraphicsContext2D();
            gc.setFill(Color.BLUE);
            gc.fillRect(0, 0, cw, ch);
        }
        canvas0.relocate(cx, cy);

        final Text text0 = new Text(0, 0, "left");
        {
            text0.setFont(new Font(40));
            text0.relocate(0, height/2);
        }
        final Text text1 = new Text(0, 0, "above");
        {
            text1.setFont(new Font(40));
            text1.relocate(width/2, bh-40);
        }
        final Text text2 = new Text(0, 0, "right");
        {
            text2.setFont(new Font(40));
            text2.relocate(width-bw, height/2);
        }
        final Text text3 = new Text(0, 0, "below");
        {
            text3.setFont(new Font(40));
            text3.relocate(width/2, height-bh);
        }
        final Runnable attach2Group = new Runnable() {
            @Override
            public void run() {
                g.getChildren().add(text0);
                g.getChildren().add(text1);
                g.getChildren().add(canvas0);
                g.getChildren().add(text2);
                g.getChildren().add(text3);
            } };
        if( !postAttach && isShowing ) {
            JFXAccessor.runOnJFXThread(true, attach2Group);
        } else {
            attach2Group.run();
        }
        if( postAttach ) {
            if(isShowing) {
                JFXAccessor.runOnJFXThread(true, new Runnable() {
                    @Override
                    public void run() {
                        scene.setRoot(g);
                    }});
            } else {
                scene.setRoot(g);
            }
        }
    }

    protected void runTestAGL( final GLCapabilitiesImmutable caps, final GLEventListener demo,
                               final boolean postAttachNewtCanvas, final boolean postAttachGLWindow,
                               final boolean useAnimator ) throws InterruptedException {
        if( !JFXAccessor.isJFXAvailable() ) {
            System.err.println("JFX not available");
            return;
        }
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(false, false);
        final GLWindow glWindow1;
        if( null == demo ) {
            glWindow1 = null;
        } else {
            final Screen screen = NewtFactory.createScreen(jfxNewtDisplay, 0);
            glWindow1 = GLWindow.create(screen, caps);
            Assert.assertNotNull(glWindow1);
            Assert.assertEquals(false, glWindow1.isVisible());
            Assert.assertEquals(false, glWindow1.isNativeValid());
            Assert.assertNull(glWindow1.getParent());
            glWindow1.addGLEventListener(demo);
            glWindow1.addGLEventListener(new GLEventListener() {
               int displayCount = 0;
               public void init(final GLAutoDrawable drawable) { }
               public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
               public void display(final GLAutoDrawable drawable) {
                  if(displayCount < 3) {
                      snapshot(displayCount++, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
                  }
               }
               public void dispose(final GLAutoDrawable drawable) { }
            });
        }

        final NewtCanvasJFX[] glCanvas = null==demo? null : new NewtCanvasJFX[]{null};

        final Scene scene = new Scene(new Group(), defWidth, defHeight);
        if(!postAttachNewtCanvas) {
            System.err.println("Stage set.A0");
            JFXAccessor.runOnJFXThread(true, new Runnable() {
                    public void run() {
                        System.err.println("Stage set.A1");
                        JFXApp.stage.setScene(scene);
                        JFXApp.stage.sizeToScene();
                        System.err.println("Stage set.AX");
                    }  });
        }
        populateScene( scene, postAttachNewtCanvas, postAttachGLWindow?null:glWindow1, defWidth, defHeight, true, glCanvas);
        if(postAttachNewtCanvas) {
            System.err.println("Stage set.B0");
            JFXAccessor.runOnJFXThread(true, new Runnable() {
                    public void run() {
                        System.err.println("Stage set.B1");
                        JFXApp.stage.setScene(scene);
                        JFXApp.stage.sizeToScene();
                        System.err.println("Stage set.BX");
                    }  });
        }

        if(postAttachGLWindow && null != demo) {
            glCanvas[0].setNEWTChild(glWindow1);
        }

        if( null != glWindow1 ) {
            Assert.assertTrue("GLWindow didn't become visible natively!", NewtTestUtil.waitForRealized(glWindow1, true, awtRobotWaitAction));
            System.err.println("GLWindow LOS.0: "+glWindow1.getLocationOnScreen(null));
            glWindow1.addWindowListener(new WindowAdapter() {
                public void windowResized(final WindowEvent e) {
                    System.err.println("window resized: "+glWindow1.getX()+"/"+glWindow1.getY()+" "+glWindow1.getSurfaceWidth()+"x"+glWindow1.getSurfaceHeight());
                }
                public void windowMoved(final WindowEvent e) {
                    System.err.println("window moved:   "+glWindow1.getX()+"/"+glWindow1.getY()+" "+glWindow1.getSurfaceWidth()+"x"+glWindow1.getSurfaceHeight());
                }
            });
            final NewtReparentingKeyAdapter newtDemoListener = new NewtJFXReparentingKeyAdapter(JFXApp.stage, glCanvas[0], glWindow1);
            newtDemoListener.quitAdapterEnable(true);
            glWindow1.addKeyListener(newtDemoListener);
            glWindow1.addMouseListener(newtDemoListener);
            glWindow1.addWindowListener(newtDemoListener);

           final ChangeListener<Number> sizeListener = new ChangeListener<Number>() {
            @Override public void changed(final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) {
                newtDemoListener.setTitle();
            } };
           JFXApp.stage.widthProperty().addListener(sizeListener);
           JFXApp.stage.heightProperty().addListener(sizeListener);

        }
        if( null != demo ) {
            System.err.println("NewtCanvasJFX LOS.0: "+glCanvas[0].getNativeWindow().getLocationOnScreen(null));
        }

        Animator anim;
        if(useAnimator && null != demo) {
            anim = new Animator(glWindow1);
            anim.start();
        } else {
            anim = null;
        }

        final long lStartTime = System.currentTimeMillis();
        final long lEndTime = lStartTime + duration;
        try {
            while( (System.currentTimeMillis() < lEndTime) ) {
                generalWaitAction.run();
            }
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        if(null != anim) {
            anim.stop();
        }

        JFXAccessor.runOnJFXThread(true, new Runnable() {
                public void run() {
                    populateScene( JFXApp.stage.getScene(), false, null, defWidth, defHeight, true, null);
                    JFXApp.stage.sizeToScene();
                }  });
    }

    @Test
    public void test00() throws InterruptedException {
        if( 0 > manualTestID || 0 == manualTestID ) {
            runTestAGL( null, null,
                        false /* postAttachNewtCanvas */, false /* postAttach */, false /* animator */);
        }
    }

    @Test
    public void test11_preAttachNewtGL_NoAnim() throws InterruptedException {
        if( 0 > manualTestID || 11 == manualTestID ) {
            runTestAGL( new GLCapabilities(GLProfile.getGL2ES2()), new GearsES2(),
                        false /* postAttachNewtCanvas */, false /* postAttachGLWindow */, false /* animator */);
        }
    }

    @Test
    public void test12_postAttachNewt_NoAnim() throws InterruptedException {
        if( 0 > manualTestID || 12 == manualTestID ) {
            runTestAGL( new GLCapabilities(GLProfile.getGL2ES2()), new GearsES2(),
                        true /* postAttachNewtCanvas */, false /* postAttachGLWindow */, false /* animator */);
        }
    }

    @Test
    public void test13_postAttachGL_NoAnim() throws InterruptedException {
        if( 0 > manualTestID || 13 == manualTestID ) {
            runTestAGL( new GLCapabilities(GLProfile.getGL2ES2()), new GearsES2(),
                        false /* postAttachNewtCanvas */, true /* postAttachGLWindow */, false /* animator */);
        }
    }

    @Test
    public void test14_postAttachNewtGL_NoAnim() throws InterruptedException {
        if( 0 > manualTestID || 14 == manualTestID ) {
            runTestAGL( new GLCapabilities(GLProfile.getGL2ES2()), new GearsES2(),
                        true /* postAttachNewtCanvas */, true /* postAttachGLWindow */, false /* animator */);
        }
    }

    @Test
    public void test21_preAttachNewtGL_DoAnim() throws InterruptedException {
        if( 0 > manualTestID || 21 == manualTestID ) {
            runTestAGL( new GLCapabilities(GLProfile.getGL2ES2()), new GearsES2(),
                        false /* postAttachNewtCanvas */, false /* postAttachGLWindow */, true /* animator */);
        }
    }

    @Test
    public void test22_postAttachNewt_DoAnim() throws InterruptedException {
        if( 0 > manualTestID || 22 == manualTestID ) {
            runTestAGL( new GLCapabilities(GLProfile.getGL2ES2()), new GearsES2(),
                        true /* postAttachNewtCanvas */, false /* postAttachGLWindow */, true /* animator */);
        }
    }

    @Test
    public void test30_MultisampleAndAlpha() throws InterruptedException {
        if( 0 > manualTestID || 30 == manualTestID ) {
            final GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
            caps.setSampleBuffers(true);
            caps.setNumSamples(2);
            runTestAGL( caps, new MultisampleDemoES2(true),
                        false /* postAttachNewtCanvas */, false /* postAttachGLWindow */, false /* animator */);
        }
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                duration = MiscUtils.atoi(args[++i],  duration);
            }
            if(args[i].equals("-test")) {
                manualTestID = MiscUtils.atoi(args[++i], -1);
            }
        }
        System.out.println("durationPerTest: "+duration+", test "+manualTestID);
        org.junit.runner.JUnitCore.main(TestNewtCanvasJFXGLn.class.getName());
    }
}
