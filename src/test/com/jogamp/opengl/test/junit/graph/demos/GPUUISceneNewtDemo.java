package com.jogamp.opengl.test.junit.graph.demos;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.SVertex;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.util.Animator;

public class GPUUISceneNewtDemo {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;

    static int SceneMSAASamples = 0;
    static boolean GraphVBAAMode = true;
    static boolean GraphMSAAMode = false;

    public static void main(String[] args) {
        int wwidth = 800, wheight = 400;
        if( 0 != args.length ) {
            for(int i=0; i<args.length; i++) {
                if(args[i].equals("-smsaa")) {
                    i++;
                    SceneMSAASamples = MiscUtils.atoi(args[i], SceneMSAASamples);
                    GraphMSAAMode = false;
                    GraphVBAAMode = false;
                } else if(args[i].equals("-gmsaa")) {
                    GraphMSAAMode = true;
                    GraphVBAAMode = false;
                } else if(args[i].equals("-gvbaa")) {
                    GraphMSAAMode = false;
                    GraphVBAAMode = true;
                } else if(args[i].equals("-width")) {
                    i++;
                    wwidth = MiscUtils.atoi(args[i], wwidth);
                } else if(args[i].equals("-height")) {
                    i++;
                    wheight = MiscUtils.atoi(args[i], wheight);
                }
            }
        }
        System.err.println("Scene MSAA Samples "+SceneMSAASamples);
        System.err.println("Graph MSAA Mode "+GraphMSAAMode);
        System.err.println("Graph VBAA Mode "+GraphVBAAMode);

        GLProfile glp = GLProfile.getGL2ES2();
        GLCapabilities caps = new GLCapabilities(glp);
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
        window.setPosition(10, 10);
        window.setSize(wwidth, wheight);
        window.setTitle("GraphUI Newt Demo: graph["+Region.getRenderModeString(rmode)+"], msaa "+SceneMSAASamples);

        final RenderState rs = RenderState.createRenderState(SVertex.factory());
        GPUUISceneGLListener0A sceneGLListener = new GPUUISceneGLListener0A(rs, rmode, DEBUG, TRACE);

        window.addGLEventListener(sceneGLListener);
        sceneGLListener.attachInputListenerTo(window);

        final Animator animator = new Animator();
        animator.setUpdateFPSFrames(60, System.err);
        animator.add(window);

        window.addWindowListener(new WindowAdapter() {
            public void windowDestroyed(WindowEvent e) {
                animator.stop();
            }
        });

        window.setVisible(true);
        animator.start();
    }
}
