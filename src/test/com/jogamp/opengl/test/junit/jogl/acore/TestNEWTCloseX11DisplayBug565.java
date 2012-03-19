package com.jogamp.opengl.test.junit.jogl.acore;

import jogamp.nativewindow.x11.X11Util;
import org.junit.Assert;
import org.junit.Test;

import com.jogamp.newt.opengl.GLWindow;

import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

/**
 * Tests the closing the device of GLWindow and GLPBuffer in JOGL
 */
public class TestNEWTCloseX11DisplayBug565 {

  @Test
  public void testX11WindowMemoryLeak() throws Exception {
    GLProfile.initSingleton(); // ensure shared resource runner is done
    try {
      for ( int j = 0; j < 10; j++ ) {
        final int open0;
        if(NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(false)) {        
            open0 = X11Util.getOpenDisplayConnectionNumber();
        } else {
            open0 = 0;
        }
                
        GLCapabilitiesImmutable caps = new GLCapabilities( GLProfile.getDefault( ) );
        
        GLWindow window = GLWindow.create(caps);
        window.setTitle("NEWT Resource X11 Leak - #" + j );  
        window.setSize( 128, 128 );
        window.setVisible(true);
        window.display();
        window.setVisible(false);
        window.destroy();

        if(NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(false)) {
            final int openD = X11Util.getOpenDisplayConnectionNumber() - open0;
            if(openD>1) {
                X11Util.dumpOpenDisplayConnections();
                X11Util.dumpPendingDisplayConnections();
                Assert.assertTrue("More than 1 new open display connections", false);
            }
        }
      }
    }
    catch ( Exception e ) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }


  @Test
  public void testX11WindowMemoryLeakOffscreenWindow() throws Exception {
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
        GLCapabilitiesImmutable caps = new GLCapabilities( glp );


        GLPbuffer buffer = GLDrawableFactory.getFactory( glp ).createGLPbuffer(
            null,
            caps,
            new DefaultGLCapabilitiesChooser(),
            256,
            256,
            null
        );
        buffer.display();
        buffer.destroy();

        if(NativeWindowFactory.TYPE_X11 == NativeWindowFactory.getNativeWindowType(false)) {
            final int openD = X11Util.getOpenDisplayConnectionNumber() - open0;
            if(openD>1) {
                X11Util.dumpOpenDisplayConnections();
                X11Util.dumpPendingDisplayConnections();
                Assert.assertTrue("More than 1 new open display connections", false);
            }
        }
      }
    }
    catch ( Exception e ) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }

  public static void main(String args[]) {
    org.junit.runner.JUnitCore.main(TestNEWTCloseX11DisplayBug565.class.getName());
  }
  
}

