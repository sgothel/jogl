package com.jogamp.opengl.test.junit.graph.demos;

import java.awt.Component;
import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import javax.swing.SwingUtilities;

import org.junit.Assume;

import com.jogamp.graph.curve.Region;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.util.Animator;

public class GPUUISceneNewtCanvasAWTDemo {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    static int SceneMSAASamples = 0;
    static boolean GraphVBAAMode = false;
    static boolean GraphMSAAMode = false;
    static float GraphAutoMode = GPUUISceneGLListener0A.DefaultNoAADPIThreshold;

    static float[] reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };

    static void setComponentSize(final Component comp, final DimensionImmutable new_sz) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    final java.awt.Dimension d = new java.awt.Dimension(new_sz.getWidth(), new_sz.getHeight());
                    comp.setMinimumSize(d);
                    comp.setPreferredSize(d);
                    comp.setSize(d);
                } } );
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }

    public static void main(final String[] args) throws InterruptedException, InvocationTargetException {
        int width = 800, height = 400;
        int x = 10, y = 10;

        boolean forceES2 = false;
        boolean forceES3 = false;
        boolean forceGL3 = false;
        boolean forceGLDef = false;

        if( 0 != args.length ) {
            for(int i=0; i<args.length; i++) {
                if(args[i].equals("-smsaa")) {
                    i++;
                    SceneMSAASamples = MiscUtils.atoi(args[i], SceneMSAASamples);
                    GraphMSAAMode = false;
                    GraphVBAAMode = false;
                    GraphAutoMode = 0f;
                } else if(args[i].equals("-gmsaa")) {
                    GraphMSAAMode = true;
                    GraphVBAAMode = false;
                    GraphAutoMode = 0f;
                } else if(args[i].equals("-gvbaa")) {
                    GraphMSAAMode = false;
                    GraphVBAAMode = true;
                    GraphAutoMode = 0f;
                } else if(args[i].equals("-gauto")) {
                    GraphMSAAMode = false;
                    GraphVBAAMode = true;
                    i++;
                    GraphAutoMode = MiscUtils.atof(args[i], GraphAutoMode);
                } else if(args[i].equals("-width")) {
                    i++;
                    width = MiscUtils.atoi(args[i], width);
                } else if(args[i].equals("-height")) {
                    i++;
                    height = MiscUtils.atoi(args[i], height);
                } else if(args[i].equals("-x")) {
                    i++;
                    x = MiscUtils.atoi(args[i], x);
                } else if(args[i].equals("-y")) {
                    i++;
                    y = MiscUtils.atoi(args[i], y);
                } else if(args[i].equals("-pixelScale")) {
                    i++;
                    final float pS = MiscUtils.atof(args[i], reqSurfacePixelScale[0]);
                    reqSurfacePixelScale[0] = pS;
                    reqSurfacePixelScale[1] = pS;
                } else if(args[i].equals("-es2")) {
                    forceES2 = true;
                } else if(args[i].equals("-es3")) {
                    forceES3 = true;
                } else if(args[i].equals("-gl3")) {
                    forceGL3 = true;
                } else if(args[i].equals("-gldef")) {
                    forceGLDef = true;
                }
            }
        }
        System.err.println("forceES2   "+forceES2);
        System.err.println("forceES3   "+forceES3);
        System.err.println("forceGL3   "+forceGL3);
        System.err.println("forceGLDef "+forceGLDef);
        System.err.println("Desired win size "+width+"x"+height);
        System.err.println("Desired win pos  "+x+"/"+y);
        System.err.println("Scene MSAA Samples "+SceneMSAASamples);
        System.err.println("Graph MSAA Mode "+GraphMSAAMode);
        System.err.println("Graph VBAA Mode "+GraphVBAAMode);
        System.err.println("Graph Auto Mode "+GraphAutoMode+" no-AA dpi threshold");

        final GLProfile glp;
        if(forceGLDef) {
            glp = GLProfile.getDefault();
        } else if(forceGL3) {
            glp = GLProfile.get(GLProfile.GL3);
        } else if(forceES3) {
            glp = GLProfile.get(GLProfile.GLES3);
        } else if(forceES2) {
            glp = GLProfile.get(GLProfile.GLES2);
        } else {
            glp = GLProfile.getGL2ES2();
        }
        System.err.println("GLProfile: "+glp);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        if( SceneMSAASamples > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(SceneMSAASamples);
        }
        System.out.println("Requested: " + caps);

        final int rmode;
        if( GraphVBAAMode ) {
            rmode = Region.VBAA_RENDERING_BIT;
        } else if( GraphMSAAMode ) {
            rmode = Region.MSAA_RENDERING_BIT;
        } else {
            rmode = 0;
        }

        final GLWindow window = GLWindow.create(caps);
        window.setPosition(x, y);
        window.setSize(width, height);
        window.setTitle("GraphUI Newt/AWT Demo: graph["+Region.getRenderModeString(rmode)+"], msaa "+SceneMSAASamples);
        window.setSurfaceScale(reqSurfacePixelScale);
        final float[] valReqSurfacePixelScale = window.getRequestedSurfaceScale(new float[2]);

        final GPUUISceneGLListener0A sceneGLListener = 0 < GraphAutoMode ? new GPUUISceneGLListener0A(GraphAutoMode, DEBUG, TRACE) :
                                                                           new GPUUISceneGLListener0A(rmode, DEBUG, TRACE);

        window.addGLEventListener(sceneGLListener);
        sceneGLListener.attachInputListenerTo(window);

        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(60, System.err);
        animator.add(window);

        window.addWindowListener(new WindowAdapter() {
            public void windowDestroyed(final WindowEvent e) {
                animator.stop();
            }
        });

        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(window);
        final Frame frame = new Frame("GraphUI Newt/AWT Demo: graph["+Region.getRenderModeString(rmode)+"], msaa "+SceneMSAASamples);

        setComponentSize(newtCanvasAWT, new Dimension(width, height));
        frame.add(newtCanvasAWT);
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.pack();
               frame.setVisible(true);
           }
        });
        animator.start();
    }
}
