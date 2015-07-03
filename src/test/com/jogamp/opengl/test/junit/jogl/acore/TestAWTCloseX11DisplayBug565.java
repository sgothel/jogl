package com.jogamp.opengl.test.junit.jogl.acore;

import jogamp.nativewindow.x11.X11Util;
import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import java.awt.Frame;

/**
 * Tests the closing the device of GLCanvas in JOGL
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAWTCloseX11DisplayBug565 {

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

        final GLCapabilitiesImmutable caps = new GLCapabilities( GLProfile.getDefault( ) );
        final Frame frame = new Frame( "AWT Resource X11 Leak - #" + j );

        final GLCanvas glCanvas = new GLCanvas( caps );
        frame.add( glCanvas );

        try {
          javax.swing.SwingUtilities.invokeAndWait( new Runnable() {
            public void run() {
              frame.setSize( 128, 128 );
              frame.setVisible( true );
            }
          } );
        }
        catch ( final Throwable t ) {
          t.printStackTrace();
          Assert.fail(t.getMessage());
        }
        glCanvas.display();
        try {
          javax.swing.SwingUtilities.invokeAndWait( new Runnable() {
            public void run() {
              frame.setVisible( false );
              frame.remove( glCanvas );
              frame.dispose();
            }
          } );
        }
        catch ( final Throwable t ) {
          t.printStackTrace();
          Assert.fail(t.getMessage());
        }

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
    catch ( final Exception e ) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
  }


  public static void main(final String args[]) {
    org.junit.runner.JUnitCore.main(TestAWTCloseX11DisplayBug565.class.getName());
  }

}

