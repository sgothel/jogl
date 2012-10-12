package com.jogamp.opengl.test.junit.jogl.util;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLPipelineFactory;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.gl2es1.GLUgl2es1;

import com.jogamp.opengl.util.ImmModeSink;
import com.jogamp.opengl.util.glsl.fixedfunc.FixedFuncUtil;
import com.jogamp.opengl.util.glsl.fixedfunc.ShaderSelectionMode;

class DemoGL2ES1ImmModeSink implements GLEventListener {
    private boolean debugFFPEmu = false;
    private boolean verboseFFPEmu = false;
    private boolean traceFFPEmu = false;
    private boolean forceFFPEmu = false;
    final ImmModeSink ims;
    final GLU glu;
    
    DemoGL2ES1ImmModeSink(boolean useVBO) {
        ims = ImmModeSink.createFixed(3*3, 
                                      3, GL.GL_FLOAT, // vertex
                                      3, GL.GL_FLOAT, // color
                                      0, GL.GL_FLOAT, // normal
                                      0, GL.GL_FLOAT, // texCoords 
                                      useVBO ? GL.GL_STATIC_DRAW : 0);
        glu = new GLUgl2es1();
    }
    
    public void setForceFFPEmu(boolean forceFFPEmu, boolean verboseFFPEmu, boolean debugFFPEmu, boolean traceFFPEmu) {
        this.forceFFPEmu = forceFFPEmu;
        this.verboseFFPEmu = verboseFFPEmu;
        this.debugFFPEmu = debugFFPEmu;
        this.traceFFPEmu = traceFFPEmu;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL _gl = drawable.getGL();
        if(debugFFPEmu) {
            // Debug ..
            _gl = _gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Debug", GL2ES2.class, _gl, null) );
        }
        if(traceFFPEmu) {
            // Trace ..
            _gl = _gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Trace", GL2ES2.class, _gl, new Object[] { System.err } ) );
        }
        GL2ES1 gl = FixedFuncUtil.wrapFixedFuncEmul(_gl, ShaderSelectionMode.AUTO, null, forceFFPEmu, verboseFFPEmu);
        
        System.err.println("GL_VENDOR   "+gl.glGetString(GL.GL_VENDOR));
        System.err.println("GL_RENDERER "+gl.glGetString(GL.GL_RENDERER));
        System.err.println("GL_VERSION  "+gl.glGetString(GL.GL_VERSION));
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2ES1 gl = drawable.getGL().getGL2ES1();
        
        gl.glMatrixMode( GL2ES1.GL_PROJECTION );
        gl.glLoadIdentity();

        // coordinate system origin at lower left with width and height same as the window
        glu.gluOrtho2D( 0.0f, width, 0.0f, height );

        gl.glMatrixMode( GL2ES1.GL_MODELVIEW );
        gl.glLoadIdentity();

        gl.glViewport( 0, 0, width, height );        
    }
    
    @Override
    public void display(GLAutoDrawable drawable) {
        GL2ES1 gl = drawable.getGL().getGL2ES1();
        
        gl.glClear( GL.GL_COLOR_BUFFER_BIT );

        // draw a triangle filling the window
        gl.glLoadIdentity();
        
        ims.glBegin(GL.GL_TRIANGLES);
        ims.glColor3f( 1, 0, 0 );
        ims.glVertex2f( 0, 0 );
        ims.glColor3f( 0, 1, 0 );
        ims.glVertex2f( TestImmModeSinkES1NEWT.iWidth, 0 );
        ims.glColor3f( 0, 0, 1 );
        ims.glVertex2f( TestImmModeSinkES1NEWT.iWidth / 2, TestImmModeSinkES1NEWT.iHeight );
        ims.glEnd(gl, true);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
    }        
}