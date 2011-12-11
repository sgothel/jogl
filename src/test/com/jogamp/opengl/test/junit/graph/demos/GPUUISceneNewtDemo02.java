package com.jogamp.opengl.test.junit.graph.demos;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.glsl.ShaderState;

public class GPUUISceneNewtDemo02 {
    static final boolean DEBUG = false;
    static final boolean TRACE = false;
    
    public static void main(String[] args) {
        GLProfile glp = GLProfile.getGL2ES2();
        GLCapabilities caps = new GLCapabilities(glp);
        caps.setAlphaBits(4);
        caps.setSampleBuffers(true);
        caps.setNumSamples(4);
        
        final GLWindow window = GLWindow.create(caps);        
        window.setPosition(10, 10);
        window.setSize(680, 480);
        window.setTitle("GraphUI Newt Demo");
        
        final RenderState rs = RenderState.createRenderState(new ShaderState(), SVertex.factory());
        GPUUISceneGLListener0A textGLListener = new GPUUISceneGLListener0A(rs, Region.VBAA_RENDERING_BIT, DEBUG, TRACE);
        window.addGLEventListener(textGLListener);
        textGLListener.attachInputListenerTo(window);
        
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
