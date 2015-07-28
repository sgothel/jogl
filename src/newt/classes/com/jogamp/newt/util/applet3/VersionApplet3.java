package com.jogamp.newt.util.applet3;

import com.jogamp.plugin.applet.Applet3;
import com.jogamp.plugin.applet.Applet3Context;
import com.jogamp.plugin.ui.NativeWindowDownstream;
import com.jogamp.plugin.ui.NativeWindowUpstream;

import java.util.List;
import java.util.Locale;

import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.JoglVersion;

public class VersionApplet3 implements Applet3 {

    public static void main(final String[] args) {
        final VersionApplet3 va = new VersionApplet3();

        final NativeWindowDownstream nwc = va.createNativeWindow(null, new NativeWindowUpstream() {
            @Override
            public long getWindowHandle() {
                return 0;
            }
            @Override
            public int getWidth() {
                return 64;
            }
            @Override
            public int getHeight() {
                return 64;
            }
            @Override
            public String getDisplayConnection() {
                return null; // default
            }
            @Override
            public int getScreenIndex() {
                return 0; // default
            }
            @Override
            public void notifySurfaceUpdated(final NativeWindowDownstream swappedWin) {
                // NOP
            }
            @Override
            public int getX() {
                return 0;
            }
            @Override
            public int getY() {
                return 0;
            }
        });
        va.init(null);
        va.start();
        va.stop();
        va.destroy();
        nwc.destroy();
    }

    GLWindow canvas;

    @Override
    public NativeWindowDownstream createNativeWindow(final Applet3Context ctx, final NativeWindowUpstream parentWin) {
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        final Window w = NewtFactory.createWindow(parentWin.getDisplayConnection(), parentWin.getScreenIndex(), parentWin.getWindowHandle(), caps);
        canvas = GLWindow.create(w);
        canvas.setSize(parentWin.getWidth(), parentWin.getHeight());

        return new NativeWindowDownstream() {
            @Override
            public void setVisible(final boolean v) {
                if( null != canvas ) {
                    canvas.setVisible(v);
                }
            }

            @Override
            public void setSize(final int width, final int height) {
                if( null != canvas ) {
                    canvas.setSize(width, height);
                }
            }

            @Override
            public void requestFocus() {
                if( null != canvas ) {
                    canvas.requestFocus();
                }
            }

            @Override
            public void destroy() {
                if( null != canvas ) {
                    canvas.destroy();
                }
            }

            @Override
            public NativeWindowUpstream getParent() {
                return parentWin;
            }

            @Override
            public long getWindowHandle() {
                if( null != canvas ) {
                    return canvas.getWindowHandle();
                } else {
                    return 0;
                }
            }

            @Override
            public void display() {
                if( null != canvas ) {
                    canvas.display();
                }
            }

            @Override
            public void notifyPositionChanged(final NativeWindowUpstream nw) {
                if( null != canvas ) {
                    canvas.setPosition(nw.getX(), nw.getY());
                }
            }
        };
    }

    @Override
    public void init(final Applet3Context ctx) {
        System.err.println("VersionApplet: init() - begin");
        canvas.addGLEventListener(new GLInfo());
        System.err.println("VersionApplet: init() - end");
    }

    @Override
    public void start() {
        System.err.println("VersionApplet: start() - begin");

        String s;

        s = VersionUtil.getPlatformInfo().toString();
        System.err.println(s);

        s = GlueGenVersion.getInstance().toString();
        System.err.println(s);

        /*
            s = NativeWindowVersion.getInstance().toString();
            System.err.println(s);
        */

        s = JoglVersion.getInstance().toString();
        System.err.println(s);

        final GLDrawableFactory factory = GLDrawableFactory.getFactory(canvas.getGLProfile());
        final List<GLCapabilitiesImmutable> availCaps = factory.getAvailableCapabilities(null);
        for(int i=0; i<availCaps.size(); i++) {
            s = availCaps.get(i).toString();
            System.err.println(s);
        }
        canvas.display();
        System.err.println("VersionApplet: start() - end");
    }

    @Override
    public void stop() {
        System.err.println("VersionApplet: stop() - begin");
        canvas.setVisible(false);
        System.err.println("VersionApplet: stop() - end");
    }

    @Override
    public void destroy() {
        System.err.println("VersionApplet: destroy() - start");
        if(null!=canvas) {
            canvas.destroy();
            canvas = null;
        }
        System.err.println("VersionApplet: destroy() - end");
    }

    @Override
    public String getAppletInfo() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return null;
    }

    @Override
    public String[][] getParameterInfo() {
        return null;
    }

    static class GLInfo implements GLEventListener {
        @Override
        public void init(final GLAutoDrawable drawable) {
            final GL gl = drawable.getGL();
            final String s = JoglVersion.getGLInfo(gl, null).toString();
            System.err.println(s);
        }
        @Override
        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        }
        @Override
        public void display(final GLAutoDrawable drawable) {
        }
        @Override
        public void dispose(final GLAutoDrawable drawable) {
        }
    }
}
