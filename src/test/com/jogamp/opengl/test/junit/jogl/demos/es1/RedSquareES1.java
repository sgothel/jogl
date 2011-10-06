package com.jogamp.opengl.test.junit.jogl.demos.es1;

import com.jogamp.common.nio.Buffers;
import java.nio.*;
import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.fixedfunc.GLPointerFunc;

import com.jogamp.opengl.util.glsl.fixedfunc.*;

public class RedSquareES1 implements GLEventListener {

    public static boolean glDebugEmu = false;
    public static boolean glDebug = false ;
    public static boolean glTrace = false ;
    public static boolean oneThread = false;
    public static boolean useAnimator = false;
    private int swapInterval = 0;

    long startTime = 0;
    long curTime = 0;

    public RedSquareES1(int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public RedSquareES1() {
        this.swapInterval = 1;
    }
    
    // FIXME: we must add storage of the pointers in the GL state to
    // the GLImpl classes. The need for this can be seen by making
    // these variables method local instead of instance members. The
    // square will disappear after a second or so due to garbage
    // collection. On desktop OpenGL this implies a stack of
    // references due to the existence of glPush/PopClientAttrib. On
    // OpenGL ES 1/2 it can simply be one set of references.
    private FloatBuffer colors;
    private FloatBuffer vertices;

    public void init(GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" RedSquareES1.init ...");
        GL _gl = drawable.getGL();

        if(glDebugEmu) {
            try {
                // Debug ..
                _gl = _gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Debug", GL2ES2.class, _gl, null) );

                if(glTrace) {
                    // Trace ..
                    _gl = _gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Trace", GL2ES2.class, _gl, new Object[] { System.err } ) );
                }
            } catch (Exception e) {e.printStackTrace();} 
            glDebug = false;
            glTrace = false;
        }

        GL2ES1 gl = FixedFuncUtil.wrapFixedFuncEmul(_gl);
        if(glDebug) {
            try {
                // Debug ..
                gl = (GL2ES1) gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Debug", GL2ES1.class, gl, null) );
            } catch (Exception e) {e.printStackTrace();} 
        }

        if(glTrace) {
            try {
                // Trace ..
                gl = (GL2ES1) gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Trace", GL2ES1.class, gl, new Object[] { System.err } ) );
            } catch (Exception e) {e.printStackTrace();}
        }

        System.err.println(Thread.currentThread()+"Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
        System.err.println(Thread.currentThread()+"INIT GL IS: " + gl.getClass().getName());
        System.err.println(Thread.currentThread()+"GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
        System.err.println(Thread.currentThread()+"GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
        System.err.println(Thread.currentThread()+"GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
        
        System.err.println(Thread.currentThread()+" GL Profile: "+gl.getGLProfile());
        System.err.println(Thread.currentThread()+" GL:" + gl);
        System.err.println(Thread.currentThread()+" GL_VERSION=" + gl.glGetString(GL.GL_VERSION));

        // Allocate vertex arrays
        colors   = Buffers.newDirectFloatBuffer(16);
        vertices = Buffers.newDirectFloatBuffer(12);
        // Fill them up
        colors.put( 0, 1);    colors.put( 1, 0);     colors.put( 2, 0);    colors.put( 3, 1);
        colors.put( 4, 0);    colors.put( 5, 0);     colors.put( 6, 1);    colors.put( 7, 1);
        colors.put( 8, 1);    colors.put( 9, 0);     colors.put(10, 0);    colors.put(11, 1);
        colors.put(12, 1);    colors.put(13, 0);     colors.put(14, 0);    colors.put(15, 1);
        vertices.put(0, -2);  vertices.put( 1,  2);  vertices.put( 2,  0);
        vertices.put(3,  2);  vertices.put( 4,  2);  vertices.put( 5,  0);
        vertices.put(6, -2);  vertices.put( 7, -2);  vertices.put( 8,  0);
        vertices.put(9,  2);  vertices.put(10, -2);  vertices.put(11,  0);

        gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertices);
        gl.glColorPointer(4, GL.GL_FLOAT, 0, colors);

        // OpenGL Render Settings
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);

        startTime = System.currentTimeMillis();
        curTime = startTime;
        System.err.println(Thread.currentThread()+" RedSquareES1.init FIN");
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        System.err.println(Thread.currentThread()+" RedSquareES1.reshape "+x+"/"+y+" "+width+"x"+height+", swapInterval "+swapInterval);
        GL2ES1 gl = drawable.getGL().getGL2ES1();
        gl.setSwapInterval(swapInterval);
        
        // Set location in front of camera
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        gluPerspective(gl, 45.0f, (float)width / (float)height, 1.0f, 100.0f);
        // gl.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);
        System.err.println(Thread.currentThread()+" RedSquareES1.reshape FIN");
    }
    
    void gluPerspective(GL2ES1 gl, final float fovy, final float aspect, final float zNear, final float zFar) {
      float top=(float)Math.tan(fovy*((float)Math.PI)/360.0f)*zNear;
      float bottom=-1.0f*top;
      float left=aspect*bottom;
      float right=aspect*top;
      gl.glFrustumf(left, right, bottom, top, zNear, zFar);        
    }

    public void display(GLAutoDrawable drawable) {
        curTime = System.currentTimeMillis();
        GL2ES1 gl = drawable.getGL().getGL2ES1();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // One rotation every four seconds
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -10);
        float ang = ((float) (curTime - startTime) * 360.0f) / 4000.0f;
        gl.glRotatef(ang, 0, 0, 1);
        gl.glRotatef(ang, 0, 1, 0);

        // Draw a square
        gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GLPointerFunc.GL_COLOR_ARRAY);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GLPointerFunc.GL_COLOR_ARRAY);
    }

    public void dispose(GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" RedSquareES1.dispose ... ");
        GL2ES1 gl = drawable.getGL().getGL2ES1();
        gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GLPointerFunc.GL_COLOR_ARRAY);
        colors.clear();
        colors   = null;
        vertices.clear();
        vertices = null;
        System.err.println(Thread.currentThread()+" RedSquareES1.dispose FIN");
    }
}
