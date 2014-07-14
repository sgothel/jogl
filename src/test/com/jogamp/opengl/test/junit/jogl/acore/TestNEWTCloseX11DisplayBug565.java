package com.jogamp.opengl.test.junit.jogl.acore;

import jogamp.nativewindow.x11.X11Util;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;

import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLOffscreenAutoDrawable;
import javax.media.opengl.GLProfile;

/**
 * Tests the closing the device of GLWindow and off-screen GLAutoDrawable using FBO and PBuffer in JOGL
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestNEWTCloseX11DisplayBug565 {

  @Test
  public void test01X11WindowMemoryLeak() throws Exception {
    GLProfile.initSingleton(); // ensure shared resource runner is done
    try {
      for ( int j = 0; j < 10; j++ ) {
        final int open0;
        if(NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(false)) {
            open0 = X11Util.getOpenDisplayConnectionNumber();
        } else {
            open0 = 0;
        }

        final GLCapabilitiesImmutable caps = new GLCapabilities( GLProfile.getDefault( ) );

        final GLWindow window = GLWindow.create(caps);
        window.setTitle("NEWT Resource X11 Leak - #" + j );
        window.setSize( 128, 128 );
        window.setVisible(true);
        window.display();
        window.setVisible(false);
        window.destroy();

        if(NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(false)) {
            final int openD = X11Util.getOpenDisplayConnectionNumber() - open0;
            if( openD > 0) {
                X11Util.dumpOpenDisplayConnections();
                X11Util.dumpPendingDisplayConnections();
                Assert.assertEquals("New display connection didn't close", 0, openD);
            }
        }
      }
    }
    catch ( final Exception e ) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }


  @Test
  public void test02X11WindowMemoryLeakPBufferAutoDrawable() throws Exception {
    GLProfile.initSingleton(); // ensure shared resource runner is done
    try {
      for ( int j = 0; j < 10; j++ ) {
        final int open0;
        if(NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(false)) {
            open0 = X11Util.getOpenDisplayConnectionNumber();
        } else {
            open0 = 0;
        }
        final GLProfile glp = GLProfile.getDefault( );
        final GLCapabilities caps = new GLCapabilities( glp );
        caps.setPBuffer(true);
        final GLAutoDrawable buffer = GLDrawableFactory.getFactory( glp ).createOffscreenAutoDrawable(null, caps, null, 256, 256);
        buffer.display();
        buffer.destroy();

        if(NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(false)) {
            final int openD = X11Util.getOpenDisplayConnectionNumber() - open0;
            if(openD > 0) {
                X11Util.dumpOpenDisplayConnections();
                X11Util.dumpPendingDisplayConnections();
                Assert.assertEquals("New display connection didn't close", 0, openD);
            }
        }
      }
    }
    catch ( final Exception e ) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  @Test
  public void test03X11WindowMemoryLeakFBOAutoDrawable() throws Exception {
    GLProfile.initSingleton(); // ensure shared resource runner is done
    try {
      for ( int j = 0; j < 10; j++ ) {
        final int open0;
        if(NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(false)) {
            open0 = X11Util.getOpenDisplayConnectionNumber();
        } else {
            open0 = 0;
        }
        final GLProfile glp = GLProfile.getDefault( );
        final GLCapabilitiesImmutable caps = new GLCapabilities( glp );


        final GLOffscreenAutoDrawable buffer = GLDrawableFactory.getFactory( glp ).createOffscreenAutoDrawable(
            null, caps, new DefaultGLCapabilitiesChooser(), 256, 256);
        buffer.display();
        buffer.destroy();

        if(NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(false)) {
            final int openD = X11Util.getOpenDisplayConnectionNumber() - open0;
            if(openD > 0) {
                X11Util.dumpOpenDisplayConnections();
                X11Util.dumpPendingDisplayConnections();
                Assert.assertEquals("New display connection didn't close", 0, openD);
            }
        }
      }
    }
    catch ( final Exception e ) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  public static void main(final String args[]) {
    org.junit.runner.JUnitCore.main(TestNEWTCloseX11DisplayBug565.class.getName());
  }

}

