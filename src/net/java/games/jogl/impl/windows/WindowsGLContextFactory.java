/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package net.java.games.jogl.impl.windows;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Rectangle;
import java.io.File;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Collection;
import java.util.Iterator;
import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class WindowsGLContextFactory extends GLContextFactory {
  // On Windows we want to be able to use some extension routines like
  // wglChoosePixelFormatARB during the creation of the user's first
  // GLContext. However, this and other routines' function pointers
  // aren't loaded by the driver until the first OpenGL context is
  // created. The standard way of working around this chicken-and-egg
  // problem is to create a dummy window, show it, send it a paint
  // message, create an OpenGL context, fetch the needed function
  // pointers, and then destroy the dummy window and context. It turns
  // out that ATI cards need the dummy context to be current while
  // wglChoosePixelFormatARB is called, so we cache the extension
  // strings the dummy context reports as being available.
  private static Map/*<GraphicsDevice, GL>*/     dummyContextMap    = new HashMap();
  private static Map/*<GraphicsDevice, String>*/ dummyExtensionsMap = new HashMap();
  private static Set/*<GraphicsDevice    >*/     pendingContextSet  = new HashSet();
  
  public WindowsGLContextFactory() {
    AccessController.doPrivileged( new PrivilegedAction() {
      public Object run() {
        Runtime.getRuntime().addShutdownHook( new ShutdownHook() );
        
        // Test for whether we should enable the single-threaded
        // workaround for ATI cards. It appears that if we make any
        // OpenGL context current on more than one thread on ATI cards
        // on Windows then we see random failures like the inability
        // to create more OpenGL contexts, or having just the next
        // OpenGL SetPixelFormat operation fail with a GetNextError()
        // code of 0 (but subsequent ones on subsequently-created
        // windows succeed). These kinds of failures are obviously due
        // to bugs in ATI's OpenGL drivers. Through trial and error it
        // was found that specifying
        // -DJOGL_SINGLE_THREADED_WORKAROUND=true on the command line
        // caused these problems to completely disappear. Therefore at
        // least on Windows we try to enable the single-threaded
        // workaround before creating any OpenGL contexts. In the
        // future, if problems are encountered on other platforms and
        // -DJOGL_SINGLE_THREADED_WORKAROUND=true works around them,
        // we may want to implement a workaround like this on other
        // platforms.
        
        // The algorithm here is to try to find the system directory
        // (assuming it is on the same drive as TMPDIR, exposed
        // through the system property java.io.tmpdir) and see whether
        // a known file in the ATI drivers is present; if it is, we
        // enable the single-threaded workaround.

        // If any path down this code fails, we simply bail out -- we
        // don't go to great lengths to figure out if the ATI drivers
        // are present. We could add more checks here in the future if
        // these appear to be insufficient.

        String tmpDirProp = System.getProperty("java.io.tmpdir");
        if (tmpDirProp != null) {
          File file = new File(tmpDirProp);
          if (file.isAbsolute()) {
            File parent = null;
            do {
              parent = file.getParentFile();
              if (parent != null) {
                file = parent;
              }
            } while (parent != null);
            // Now the file contains just the drive letter
            file = new File(new File(new File(file, "windows"), "system32"), "atioglxx.dll");
            if (file.exists()) {
              SingleThreadedWorkaround.shouldDoWorkaround();
            }
          }
        }

        return( null );
      }
    }); 
  }
  
  public GraphicsConfiguration chooseGraphicsConfiguration(GLCapabilities capabilities,
                                                           GLCapabilitiesChooser chooser,
                                                           GraphicsDevice device) {
    return null;
  }

  public GLContext createGLContext(Component component,
                                   GLCapabilities capabilities,
                                   GLCapabilitiesChooser chooser,
                                   GLContext shareWith) {
    if (component != null) {
      return new WindowsOnscreenGLContext(component, capabilities, chooser, shareWith);
    } else {
      return new WindowsOffscreenGLContext(capabilities, chooser, shareWith);
    }
  }
  
  // Return cached GL context
  public static WindowsGLContext getDummyGLContext( final GraphicsDevice device ) {
    checkForDummyContext( device );
    NativeWindowStruct nws = (NativeWindowStruct) dummyContextMap.get(device);
    return nws.getWindowsContext();
  }

  // Return cached extension string
  public static String getDummyGLExtensions(final GraphicsDevice device) {
    checkForDummyContext( device );
    String exts = (String) dummyExtensionsMap.get(device);
    return (exts == null) ? "" : exts;
  }

  // Return cached GL function pointers
  public static GL getDummyGL(final GraphicsDevice device) {
    checkForDummyContext( device );
    NativeWindowStruct nws = (NativeWindowStruct) dummyContextMap.get(device);
    return( nws.getWindowsContext().getGL() );
  }
    
  /*
   * Locate a cached native window, if one doesn't exist create one amd
   * cache it.
   */
  private static void checkForDummyContext( final GraphicsDevice device ) {
    if (!pendingContextSet.contains(device) && !dummyContextMap.containsKey( device ) ) {
      pendingContextSet.add(device);
      GraphicsConfiguration config = device.getDefaultConfiguration();
      Rectangle rect = config.getBounds();
      GLCapabilities caps = new GLCapabilities();
      caps.setDepthBits( 16 );
      // Create a context that we use to query pixel formats
      WindowsOnscreenGLContext context = new WindowsOnscreenGLContext( null, caps, null, null );
      // Start a native thread and grab native screen resources from the thread
      NativeWindowThread nwt = new NativeWindowThread( rect );
      nwt.start();
      long hWnd = 0;
      long tempHDC = 0;
      while( (hWnd = nwt.getHWND()) == 0 || (tempHDC = nwt.getHDC()) == 0 ) {
        Thread.yield();
      }
      // Choose a hardware accelerated pixel format
      PIXELFORMATDESCRIPTOR pfd = context.glCapabilities2PFD( caps, true );
      int pixelFormat = WGL.ChoosePixelFormat( tempHDC, pfd );
      if( pixelFormat == 0 ) {
        System.err.println("Pixel Format is Zero");
        pendingContextSet.remove(device);
        return;
      }
      // Set the hardware accelerated pixel format
      if (!WGL.SetPixelFormat(tempHDC, pixelFormat, pfd)) {
        System.err.println("SetPixelFormat Failed");
        pendingContextSet.remove( device );
        return;
      }
      // Create a rendering context
      long tempHGLRC = WGL.wglCreateContext( tempHDC );
      if( hWnd == 0 || tempHDC == 0 || tempHGLRC == 0 ) {
        pendingContextSet.remove( device );
        return;
      }
      // Store native handles for later use
      NativeWindowStruct nws = new NativeWindowStruct();
      nws.setHWND( hWnd );
      nws.setWindowsContext( context );
      nws.setWindowThread( nwt );
      long currentHDC = WGL.wglGetCurrentDC();
      long currentHGLRC = WGL.wglGetCurrentContext();
      // Make the new hardware accelerated context current
      if( !WGL.wglMakeCurrent( tempHDC, tempHGLRC ) ) {
        pendingContextSet.remove( device );
        return;
      }
      // Grab function pointers
      context.hdc = tempHDC;
      context.hglrc = tempHGLRC;
      context.resetGLFunctionAvailability();
      context.createGL();
      pendingContextSet.remove( device );
      dummyContextMap.put( device, nws );
      String availableGLExtensions = "";
      String availableWGLExtensions = "";
      String availableEXTExtensions = "";
      try {
        availableWGLExtensions = context.getGL().wglGetExtensionsStringARB( currentHDC );
      } catch( GLException e ) {
      }
      try {
        availableEXTExtensions = context.getGL().wglGetExtensionsStringEXT();
      } catch( GLException e ) {
      }
      availableGLExtensions = context.getGL().glGetString( GL.GL_EXTENSIONS );
      dummyExtensionsMap.put(device, availableGLExtensions + " " + availableEXTExtensions + " " + availableWGLExtensions);
      WGL.wglMakeCurrent( currentHDC, currentHGLRC );
    }
  }
 
  /*
   * This class stores handles to native resources that need to be destroyed
   * at JVM shutdown.
   */
  static class NativeWindowStruct {
    private long                HWND;
    private WindowsGLContext    windowsContext;
    private Thread              windowThread;
    
    public NativeWindowStruct() {
    }
          
    public long getHDC() {
      return( windowsContext.hdc );
    }
          
    public long getHGLRC() {
      return( windowsContext.hglrc );
    }

    public void setHWND( long hwnd ) {
      HWND = hwnd;
    }
    
    public long getHWND() {
      return( HWND );
    }
    
    public void setWindowsContext( WindowsGLContext context ) {
      windowsContext = context;
    }
    
    public WindowsGLContext getWindowsContext() {
      return( windowsContext );
    }
    
    public void setWindowThread( Thread thread ) {
      windowThread = thread;
    }
    
    public Thread getWindowThread() {
      return( windowThread );
    }
  }
  
  /*
   * Native HWDN and HDC handles must be created and destroyed on the same
   * thread.
   */
  
  static class NativeWindowThread extends Thread {
    private long HWND = 0;
    private long HDC = 0;
    private Rectangle rectangle;
    
    public NativeWindowThread( Rectangle rect ) {
      rectangle = rect;
    }
    
    public synchronized long getHWND() {
      return( HWND );
    }
    
    public synchronized long getHDC() {
      return( HDC );
    }
    
    public void run() {
      // Create a native window and device context
      synchronized (WindowsGLContextFactory.class) {
        HWND = WGL.CreateDummyWindow( rectangle.x, rectangle.y, rectangle.width, rectangle.height );
      }
      HDC = WGL.GetDC( HWND );
      
      // Start the message pump at shutdown
      WGL.NativeEventLoop();
    }
  }
  
  /*
   * This class is registered with the JVM to destroy all cached redering 
   * contexts, device contexts, and window handles.
   */
 
  class ShutdownHook extends Thread {
    public void run() {
      // Collect all saved screen resources
      Collection c = dummyContextMap.values();
      Iterator iter = c.iterator();
      while( iter.hasNext() ) {
        // NativeWindowStruct holds refs to native resources that need to be destroyed
        NativeWindowStruct struct = (NativeWindowStruct)iter.next();
        // Restart native window threads to respond to window closing events
        synchronized( struct.getWindowThread() ) {
          struct.getWindowThread().notifyAll();
        }
        // Destroy OpenGL rendering context
        if( !WGL.wglDeleteContext( struct.getHGLRC() ) ) {
          System.err.println( "Error Destroying NativeWindowStruct RC: " + WGL.GetLastError() );
        }
        // Send context handles to native method for deletion
        WGL.DestroyDummyWindow( struct.getHWND(), struct.getHDC() );
      }
    }
  }
}
