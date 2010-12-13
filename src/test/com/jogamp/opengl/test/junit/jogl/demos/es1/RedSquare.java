package com.jogamp.opengl.test.junit.jogl.demos.es1;

import com.jogamp.common.nio.Buffers;
import java.nio.*;
import java.util.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import javax.media.nativewindow.*;

import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.glsl.fixedfunc.*;

public class RedSquare implements GLEventListener {

    public static boolean glDebugEmu = false;
    public static boolean glDebug = false ;
    public static boolean glTrace = false ;
    public static boolean oneThread = false;
    public static boolean useAnimator = false;
    public static int swapInterval = -1;

    boolean debug = false;
    long startTime = 0;
    long curTime = 0;

    GLU glu = null;

    public RedSquare() {
        this(false);
    }

    public RedSquare(boolean debug) {
        this.debug = debug;
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
        System.out.println("RedSquare: Init");
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

        GL2ES1 gl = FixedFuncUtil.getFixedFuncImpl(_gl);
        if(swapInterval>=0) {
            gl.setSwapInterval(swapInterval);
        }

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

        glu = GLU.createGLU(gl);

        if(debug) {
            System.err.println(Thread.currentThread()+" Entering initialization");
            System.err.println(Thread.currentThread()+" GL Profile: "+gl.getGLProfile());
            System.err.println(Thread.currentThread()+" GL:" + gl);
            System.err.println(Thread.currentThread()+" GL_VERSION=" + gl.glGetString(gl.GL_VERSION));
            System.err.println(Thread.currentThread()+" GL_EXTENSIONS:");
            System.err.println(Thread.currentThread()+"   " + gl.glGetString(gl.GL_EXTENSIONS));
            System.err.println(Thread.currentThread()+" swapInterval: " + swapInterval + " (GL: "+gl.getSwapInterval()+")");
            System.err.println(Thread.currentThread()+" GLU: " + glu);
        }

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

        gl.glEnableClientState(gl.GL_VERTEX_ARRAY);
        gl.glEnableClientState(gl.GL_COLOR_ARRAY);
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertices);
        gl.glColorPointer(4, GL.GL_FLOAT, 0, colors);

        // OpenGL Render Settings
        gl.glClearColor(0, 0, 0, 1);
        gl.glEnable(GL.GL_DEPTH_TEST);

        startTime = System.currentTimeMillis();
        curTime = startTime;
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        System.out.println("RedSquare: Reshape");
        GL2ES1 gl = drawable.getGL().getGL2ES1();
        // Set location in front of camera
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0f, (float)width / (float)height, 1.0f, 100.0f);
        //gl.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);
        //glu.gluLookAt(0, 0, -20, 0, 0, 0, 0, 1, 0);
    }

    public void display(GLAutoDrawable drawable) {
        curTime = System.currentTimeMillis();
        GL2ES1 gl = drawable.getGL().getGL2ES1();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // One rotation every four seconds
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -10);
        float ang = ((float) (curTime - startTime) * 360.0f) / 4000.0f;
        gl.glRotatef(ang, 0, 0, 1);
        gl.glRotatef(ang, 0, 1, 0);


        // Draw a square
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
    }

    public void dispose(GLAutoDrawable drawable) {
        System.out.println("RedSquare: Dispose");
        GL2ES1 gl = drawable.getGL().getGL2ES1();
        if(debug) {
            System.out.println(Thread.currentThread()+" RedSquare.dispose: "+gl.getContext());
        }
        gl.glDisableClientState(gl.GL_VERTEX_ARRAY);
        gl.glDisableClientState(gl.GL_COLOR_ARRAY);
        glu.destroy();
        glu = null;
        colors.clear();
        colors   = null;
        vertices.clear();
        vertices = null;
        if(debug) {
            System.out.println(Thread.currentThread()+" RedSquare.dispose: FIN");
        }
    }
}
