package com.jogamp.opengl.test.junit.jogl.acore;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.Timer;

import org.junit.Test;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.opengl.*;
import com.jogamp.opengl.test.junit.util.DumpGLInfo;

/**
 * Bug 1160.
 * Test for context sharing with an external context. Creates an external GL context, then
 * sets up an offscreen drawable which shares with it. The test contains two cases: one
 * which creates and repaints the offscreen drawable on the EDT, and one which does so on
 * a dedicated thread. On Windows+NVidia, the former fails.
 */
public class TestSharedExternalContextAWT {

  static final int LATCH_COUNT = 5;

  private void doTest(final boolean aUseEDT) throws Exception {
    final CountDownLatch testLatch = new CountDownLatch(1);
    final CountDownLatch masterLatch = new CountDownLatch(1);
    final CountDownLatch slaveLatch = new CountDownLatch(LATCH_COUNT);
    final MyGLEventListener listener = new MyGLEventListener(aUseEDT, slaveLatch);

    /**
     * For the purpose of this test, this offscreen drawable will be used to create
     * an external GL context. In the actual application, the external context
     * represents a GL context which lives outside the JVM.
     */
    final Runnable runnable = new Runnable() {
      public void run() {
        System.err.println("Master Thread Start: "+Thread.currentThread().getName());
        final GLProfile glProfile = GLProfile.getDefault();
        final GLCapabilities caps = new GLCapabilities(glProfile);
        final GLAutoDrawable buffer = GLDrawableFactory.getDesktopFactory().createOffscreenAutoDrawable(
            GLProfile.getDefaultDevice(), caps, null, 512, 512
        );
        // The listener will set up the context sharing in its init() method.
        buffer.addGLEventListener(new DumpGLInfo(Platform.getNewline()+Platform.getNewline()+"Master GLContext", false, false, false));
        buffer.addGLEventListener(listener);
        buffer.display();
        masterLatch.countDown();

        // wait until test has finished
        try {
            testLatch.await();
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        System.err.println("Master Thread End: "+Thread.currentThread().getName());
      }
    };

    // Kick off thread creating the actual external context
    // which is suppose to lie outside of the JVM.
    // The thread is kept alive, since this detail
    // may be required for the OpenGL driver implementation.
    final Thread thread = new InterruptSource.Thread(null, runnable);
    thread.setDaemon(true);
    thread.start();
    masterLatch.await(3, TimeUnit.SECONDS);

    // Wait for slave to finish.
    slaveLatch.await(3, TimeUnit.SECONDS);

    // signal master test has finished
    testLatch.countDown();

    // If exceptions occurred, fail.
    final Exception e = listener.fException;
    if (e != null) {
      throw e;
    }
  }

  @Test
  public void test01OnEDT() throws Exception {
    doTest(true);
  }

  @Test
  public void test02OnExecutorThread() throws Exception {
    doTest(false);
  }

  /**
   * Listener that creates an external drawable and an offscreen drawable, with context
   * sharing between the two.
   */
  private static class MyGLEventListener implements GLEventListener {
    GLOffscreenAutoDrawable fOffscreenDrawable;
    final boolean fUseEDT;
    final CountDownLatch fLatch;
    final RecursiveLock masterLock = LockFactory.createRecursiveLock();

    private Exception fException = null;

    public MyGLEventListener(final boolean aUseEDT, final CountDownLatch aLatch) {
      fUseEDT = aUseEDT;
      fLatch = aLatch;
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
      // FIXME: We actually need to hook into GLContext make-current lock
      masterLock.lock();
      try {
          final GL2 gl = drawable.getGL().getGL2();
          gl.glClearColor(0.5f, 0.5f, 0.5f, 1.0f);
          gl.glClear(GL.GL_COLOR_BUFFER_BIT);

          System.err.println(); System.err.println();
          System.err.println("Master (orig) Ct: "+drawable.getContext());
          // Create the external context on the caller thread.
          final GLContext master = GLDrawableFactory.getDesktopFactory().createExternalGLContext();
          System.err.println(); System.err.println();
          System.err.println("External Context: "+master);

          // This runnable creates an offscreen drawable which shares with the external context.
          final Runnable initializer = new Runnable() {
            public void run() {
                // FIXME: We actually need to hook into GLContext make-current lock
                // masterLock.lock();
                try {
                    fOffscreenDrawable = GLDrawableFactory.getDesktopFactory().createOffscreenAutoDrawable(
                            GLProfile.getDefaultDevice(),
                            new GLCapabilities(GLProfile.getDefault()),
                            null, // new DefaultGLCapabilitiesChooser(),
                            512, 512
                            );
                    fOffscreenDrawable.setSharedContext(master);
                    fOffscreenDrawable.addGLEventListener(new DumpGLInfo(Platform.getNewline()+Platform.getNewline()+"Slave GLContext", false, false, false));

                    try {
                        System.err.println(); System.err.println();
                        System.err.println("Current: "+GLContext.getCurrent());
                        fOffscreenDrawable.display();
                    } catch (final GLException e) {
                        fException = e;
                        throw e;
                    }
                } finally {
                    // masterLock.unlock();
                }
            }
          };

          /**
           * Depending on the test case, invoke the initialization on the EDT or on an
           * executor thread. The test also displays the offscreen drawable a few times
           * before finishing.
           */
          if (fUseEDT) {
            // Initialize using invokeLater().
            try {
              // We cannot use EventQueue.invokeAndWait(..) since it will
              // block this will block the current thread, holding the context!
              // The whole issue w/ an external shared context is make-current
              // synchronization. JOGL attempts to lock the surface/drawable
              // of the master context to avoid concurrent usage.
              // The semantic constraints of a shared context are not well defined,
              // i.e. some driver may allow creating a shared context w/ a master context
              // to be in use - others don't.
              // Hence it is up to the user to sync the external master context in this case,
              // see 'masterLock' of in this code!
              //    EventQueue.invokeAndWait(initializer);
              EventQueue.invokeLater(initializer);
            } catch (final Exception e) {
              fException = e;
            }

            // Display using a Swing timer, i.e. also on the EDT.
            final Timer t = new Timer(200, new ActionListener() {
              int i = 0;

              @Override
              public void actionPerformed(final ActionEvent e) {
                if (++i > LATCH_COUNT) {
                  return;
                }

                System.err.println("Update on EDT");
                fOffscreenDrawable.display();
                fLatch.countDown();
              }
            });
            t.start();
          } else {
            // Initialize and display using a single-threaded executor.
            final ScheduledExecutorService exe = Executors.newSingleThreadScheduledExecutor();
            exe.submit(initializer);
            exe.scheduleAtFixedRate(new Runnable() {
              int i = 0;

              @Override
              public void run() {
                if (++i > LATCH_COUNT) {
                  return;
                }

                System.err.println("Update on Executor thread");
                fOffscreenDrawable.display();
                fLatch.countDown();
              }
            }, 0, 200, TimeUnit.MILLISECONDS);
          }
      } finally {
          masterLock.unlock();
      }
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
      // FIXME: We actually need to hook into GLContext make-current lock
      masterLock.lock();
      try {
      } finally {
          masterLock.unlock();
      }
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
      // FIXME: We actually need to hook into GLContext make-current lock
      masterLock.lock();
      try {
      } finally {
          masterLock.unlock();
      }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
      // FIXME: We actually need to hook into GLContext make-current lock
      masterLock.lock();
      try {
      } finally {
          masterLock.unlock();
      }
    }
  }

  public static void main(final String[] pArgs)
  {
      org.junit.runner.JUnitCore.main(TestSharedExternalContextAWT.class.getName());
  }
}
