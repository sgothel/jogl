/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package jogamp.newt.awt;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.CapabilitiesImmutable;
import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;

import jogamp.nativewindow.awt.AWTMisc;
import jogamp.nativewindow.jawt.JAWTUtil;
import jogamp.nativewindow.jawt.x11.X11SunJDKReflection;
import jogamp.nativewindow.x11.X11Lib;
import jogamp.newt.Debug;

import com.jogamp.nativewindow.awt.AWTGraphicsConfiguration;
import com.jogamp.nativewindow.awt.AWTGraphicsScreen;
import com.jogamp.nativewindow.awt.JAWTWindow;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;

public class NewtFactoryAWT extends NewtFactory {
  public static final boolean DEBUG_IMPLEMENTATION = Debug.debug("Window");

  /**
   * @deprecated Use {@link #getNativeWindow(java.awt.Component, AWTGraphicsConfiguration)}
   *
   * Wraps an AWT component into a {@link com.jogamp.nativewindow.NativeWindow} utilizing the {@link com.jogamp.nativewindow.NativeWindowFactory},<br>
   * using a configuration agnostic dummy {@link com.jogamp.nativewindow.DefaultGraphicsConfiguration}.<br>
   * <p>
   * The actual wrapping implementation is {@link com.jogamp.nativewindow.awt.JAWTWindow}.<br></p>
   * <p>
   * Purpose of this wrapping is to access the AWT window handle,<br>
   * not to actually render into it.<br>
   * Hence the dummy configuration only.</p>
   *
   * @param awtCompObject must be of type java.awt.Component
   */
  public static JAWTWindow getNativeWindow(final Object awtCompObject, final CapabilitiesImmutable capsRequested) {
      if(null==awtCompObject) {
        throw new NativeWindowException("Null AWT Component");
      }
      if( ! (awtCompObject instanceof java.awt.Component) ) {
        throw new NativeWindowException("AWT Component not a java.awt.Component");
      }
      return getNativeWindow( (java.awt.Component) awtCompObject, capsRequested );
  }

  /**
   * @deprecated Use {@link #getNativeWindow(java.awt.Component, AWTGraphicsConfiguration)}
   * @param awtComp
   * @param capsRequested
   * @return
   */
  public static JAWTWindow getNativeWindow(final java.awt.Component awtComp, final CapabilitiesImmutable capsRequested) {
      final AWTGraphicsConfiguration awtConfig = AWTGraphicsConfiguration.create(awtComp, null, capsRequested);
      return getNativeWindow(awtComp, awtConfig);
  }

  /**
   * Wraps an AWT component into a {@link com.jogamp.nativewindow.NativeWindow} utilizing the {@link com.jogamp.nativewindow.NativeWindowFactory},<br>
   * using the given {@link AWTGraphicsConfiguration}.
   * <p>
   * The actual wrapping implementation is {@link com.jogamp.nativewindow.awt.JAWTWindow}.
   * </p>
   * <p>
   * The required {@link AWTGraphicsConfiguration} may be constructed via
   * {@link AWTGraphicsConfiguration#create(java.awt.GraphicsConfiguration, CapabilitiesImmutable, CapabilitiesImmutable)}
   * </p>
   * <p>
   * Purpose of this wrapping is to access the AWT window handle,<br>
   * not to actually render into it.
   * </p>
   *
   * @param awtComp {@link java.awt.Component}
   * @param awtConfig {@link AWTGraphicsConfiguration} reflecting the used {@link java.awt.GraphicsConfiguration}
   */
  public static JAWTWindow getNativeWindow(final java.awt.Component awtComp, final AWTGraphicsConfiguration awtConfig) {
      final NativeWindow nw = NativeWindowFactory.getNativeWindow(awtComp, awtConfig); // a JAWTWindow
      if(! ( nw instanceof JAWTWindow ) ) {
          throw new NativeWindowException("Not an AWT NativeWindow: "+nw);
      }
      if(DEBUG_IMPLEMENTATION) {
        System.err.println("NewtFactoryAWT.getNativeWindow: "+awtComp+" -> "+nw);
      }
      return (JAWTWindow)nw;
  }

  public static void destroyNativeWindow(final JAWTWindow jawtWindow) {
      final AbstractGraphicsConfiguration config = jawtWindow.getGraphicsConfiguration();
      jawtWindow.destroy();
      config.getScreen().getDevice().close();
  }

  /**
   * @param awtComp
   * @throws IllegalArgumentException if {@code awtComp} is not {@link java.awt.Component#isDisplayable() displayable}
   *                                  or has {@code null} {@link java.awt.Component#getGraphicsConfiguration() GraphicsConfiguration}.
   */
  private static java.awt.GraphicsConfiguration checkComponentValid(final java.awt.Component awtComp) throws IllegalArgumentException {
      if( !awtComp.isDisplayable() ) {
          throw new IllegalArgumentException("Given AWT-Component is not displayable: "+awtComp);
      }
      final java.awt.GraphicsConfiguration gc = awtComp.getGraphicsConfiguration();
      if( null == gc ) {
          throw new IllegalArgumentException("Given AWT-Component has no GraphicsConfiguration set: "+awtComp);
      }
      return gc;
  }

  /**
   * @param awtComp must be {@link java.awt.Component#isDisplayable() displayable}
   *        and must have a {@link java.awt.Component#getGraphicsConfiguration() GraphicsConfiguration}
   * @param reuse attempt to reuse an existing {@link Display} with same <code>name</code> if set true, otherwise create a new instance.
   * @return {@link Display} instance reflecting the {@code awtComp}
   * @throws IllegalArgumentException if {@code awtComp} is not {@link java.awt.Component#isDisplayable() displayable}
   *                                  or has {@code null} {@link java.awt.Component#getGraphicsConfiguration() GraphicsConfiguration}.
   * @see #getAbstractGraphicsScreen(java.awt.Component)
   */
  public static Display createDisplay(final java.awt.Component awtComp, final boolean reuse) throws IllegalArgumentException {
      final java.awt.GraphicsConfiguration gc = checkComponentValid(awtComp);
      final java.awt.GraphicsDevice device = gc.getDevice();

      final String displayConnection;
      final String nwt = NativeWindowFactory.getNativeWindowType(true);
      if( NativeWindowFactory.TYPE_X11 == nwt ) {
          final long displayHandleAWT = X11SunJDKReflection.graphicsDeviceGetDisplay(device);
          if( 0 == displayHandleAWT ) {
              displayConnection = null; // default
          } else {
              /**
               * Using the AWT display handle works fine with NVidia.
               * However we experienced different results w/ AMD drivers,
               * some work, but some behave erratic.
               * I.e. hangs in XQueryExtension(..) via X11GraphicsScreen.
               */
              displayConnection = X11Lib.XDisplayString(displayHandleAWT);
          }
      } else {
          displayConnection = null; // default
      }
      return NewtFactory.createDisplay(displayConnection, reuse);
  }

  /**
   * @param awtComp must be {@link java.awt.Component#isDisplayable() displayable}
   *        and must have a {@link java.awt.Component#getGraphicsConfiguration() GraphicsConfiguration}
   * @param reuse attempt to reuse an existing {@link Display} with same <code>name</code> if set true, otherwise create a new instance.
   * @return {@link Screen} instance reflecting the {@code awtComp}
   * @throws IllegalArgumentException if {@code awtComp} is not {@link java.awt.Component#isDisplayable() displayable}
   *                                  or has {@code null} {@link java.awt.Component#getGraphicsConfiguration() GraphicsConfiguration}.
   * @see #createDevice(java.awt.Component)
   */
  public static Screen createScreen(final java.awt.Component awtComp, final boolean reuse) throws IllegalArgumentException {
      final Display display = createDisplay(awtComp, reuse);
      return NewtFactory.createScreen(display, AWTGraphicsScreen.findScreenIndex(awtComp.getGraphicsConfiguration().getDevice()));
  }

  /**
   * Retrieves the {@link MonitorDevice} for the given displayable {@code awtComp}.
   * <p>
   * In case this method shall be called multiple times, it is advised to keep the given {@link Screen} instance
   * natively created during operation. This should be done via the initial {@link Screen#addReference()}.
   * After operation, user shall destroy the instance accordingly via the final {@link Screen#removeReference()}.
   * </p>
   * @param screen the {@link Screen} instance matching {@code awtComp}, i.e. via {@link #createScreen(java.awt.Component, boolean)}.
   * @param awtComp must be {@link java.awt.Component#isDisplayable() displayable}
   *        and must have a {@link java.awt.Component#getGraphicsConfiguration() GraphicsConfiguration}
   * @return {@link MonitorDevice} instance reflecting the {@code awtComp}
   * @throws IllegalArgumentException if {@code awtComp} is not {@link java.awt.Component#isDisplayable() displayable}
   *                                  or has {@code null} {@link java.awt.Component#getGraphicsConfiguration() GraphicsConfiguration}.
   * @see #createScreen(java.awt.Component, boolean)
   */
  public static MonitorDevice getMonitorDevice(final Screen screen, final java.awt.Component awtComp) throws IllegalArgumentException {
      final java.awt.GraphicsConfiguration gc = checkComponentValid(awtComp);
      final String nwt = NativeWindowFactory.getNativeWindowType(true);
      MonitorDevice res = null;
      screen.addReference();
      try {
          if( NativeWindowFactory.TYPE_MACOSX == nwt ) {
              res = screen.getMonitor( JAWTUtil.getMonitorDisplayID( gc.getDevice() ) );
          }
          if( null == res ) {
              // Fallback, use AWT component coverage
              final Point los = AWTMisc.getLocationOnScreenSafe(null, awtComp, false);
              final RectangleImmutable r = new Rectangle(los.getX(), los.getY(), awtComp.getWidth(), awtComp.getHeight());
              res = screen.getMainMonitor(r);
          }
      } finally {
          screen.removeReference();
      }
      return res;
  }
}

