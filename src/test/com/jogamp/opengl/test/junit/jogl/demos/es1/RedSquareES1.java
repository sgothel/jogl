package com.jogamp.opengl.test.junit.jogl.demos.es1;

import com.jogamp.common.nio.Buffers;

import java.nio.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;

import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.util.TileRendererBase;
import com.jogamp.opengl.util.glsl.fixedfunc.*;

public class RedSquareES1 implements GLEventListener, TileRendererBase.TileRendererListener {

    public static boolean oneThread = false;
    public static boolean useAnimator = false;
    private boolean debugFFPEmu = false;
    private boolean verboseFFPEmu = false;
    private boolean traceFFPEmu = false;
    private boolean forceFFPEmu = false;
    private boolean debug = false ;
    private boolean trace = false ;
    private int swapInterval = 0;
    private final float aspect = 1.0f;
    private boolean doRotate = true;
    private TileRendererBase tileRendererInUse = null;
    private boolean doRotateBeforePrinting;
    private boolean flipVerticalInGLOrientation = false;

    long startTime = 0;
    long curTime = 0;

    public RedSquareES1(final int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public RedSquareES1() {
        this.swapInterval = 1;
    }

    @Override
    public void addTileRendererNotify(final TileRendererBase tr) {
        tileRendererInUse = tr;
        doRotateBeforePrinting = doRotate;
        setDoRotation(false);
    }
    @Override
    public void removeTileRendererNotify(final TileRendererBase tr) {
        tileRendererInUse = null;
        setDoRotation(doRotateBeforePrinting);
    }
    @Override
    public void startTileRendering(final TileRendererBase tr) {
        System.err.println("RedSquareES1.startTileRendering: "+tr);
    }
    @Override
    public void endTileRendering(final TileRendererBase tr) {
        System.err.println("RedSquareES1.endTileRendering: "+tr);
    }

    public void setDoRotation(final boolean rotate) { this.doRotate = rotate; }
    public void setForceFFPEmu(final boolean forceFFPEmu, final boolean verboseFFPEmu, final boolean debugFFPEmu, final boolean traceFFPEmu) {
        this.forceFFPEmu = forceFFPEmu;
        this.verboseFFPEmu = verboseFFPEmu;
        this.debugFFPEmu = debugFFPEmu;
        this.traceFFPEmu = traceFFPEmu;
    }
    public void setFlipVerticalInGLOrientation(final boolean v) { flipVerticalInGLOrientation=v; }

    // FIXME: we must add storage of the pointers in the GL state to
    // the GLImpl classes. The need for this can be seen by making
    // these variables method local instead of instance members. The
    // square will disappear after a second or so due to garbage
    // collection. On desktop OpenGL this implies a stack of
    // references due to the existence of glPush/PopClientAttrib. On
    // OpenGL ES 1/2 it can simply be one set of references.
    private FloatBuffer colors;
    private FloatBuffer vertices;

    @Override
    public void init(final GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" RedSquareES1.init ...");
        GL _gl = drawable.getGL();

        if(debugFFPEmu) {
            // Debug ..
            _gl = _gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", GL2ES2.class, _gl, null) );
            debug = false;
        }
        if(traceFFPEmu) {
            // Trace ..
            _gl = _gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", GL2ES2.class, _gl, new Object[] { System.err } ) );
            trace = false;
        }
        GL2ES1 gl = FixedFuncUtil.wrapFixedFuncEmul(_gl, ShaderSelectionMode.AUTO, null, forceFFPEmu, verboseFFPEmu);

        if(debug) {
            try {
                // Debug ..
                gl = (GL2ES1) gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", GL2ES1.class, gl, null) );
            } catch (final Exception e) {e.printStackTrace();}
        }
        if(trace) {
            try {
                // Trace ..
                gl = (GL2ES1) gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", GL2ES1.class, gl, new Object[] { System.err } ) );
            } catch (final Exception e) {e.printStackTrace();}
        }

        System.err.println("RedSquareES1 init on "+Thread.currentThread());
        System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
        System.err.println("INIT GL IS: " + gl.getClass().getName());
        System.err.println(JoglVersion.getGLStrings(gl, null, false).toString());

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
        gl.glEnable(GL.GL_DEPTH_TEST);

        startTime = System.currentTimeMillis();
        curTime = startTime;
        System.err.println(Thread.currentThread()+" RedSquareES1.init FIN");
    }

    @Override
    public void reshape(final GLAutoDrawable glad, final int x, final int y, final int width, final int height) {
        final GL2ES1 gl = glad.getGL().getGL2ES1();
        gl.setSwapInterval(swapInterval);
        reshapeImpl(gl, x, y, width, height, width, height);
    }

    @Override
    public void reshapeTile(final TileRendererBase tr,
                            final int tileX, final int tileY, final int tileWidth, final int tileHeight,
                            final int imageWidth, final int imageHeight) {
        final GL2ES1 gl = tr.getAttachedDrawable().getGL().getGL2ES1();
        gl.setSwapInterval(0);
        reshapeImpl(gl, tileX, tileY, tileWidth, tileHeight, imageWidth, imageHeight);
    }

    void reshapeImpl(final GL2ES1 gl, final int tileX, final int tileY, final int tileWidth, final int tileHeight, final int imageWidth, final int imageHeight) {
        System.err.println(Thread.currentThread()+" RedSquareES1.reshape "+tileX+"/"+tileY+" "+tileWidth+"x"+tileHeight+" of "+imageWidth+"x"+imageHeight+", swapInterval "+swapInterval+", drawable 0x"+Long.toHexString(gl.getContext().getGLDrawable().getHandle())+", tileRendererInUse "+tileRendererInUse);

        // Set location in front of camera
        gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        gl.glLoadIdentity();
        if( flipVerticalInGLOrientation && gl.getContext().getGLDrawable().isGLOriented() ) {
            gl.glScalef(1f, -1f, 1f);
        }

        // compute projection parameters 'normal' perspective
        final float fovy=45f;
        final float aspect2 = ( (float) imageWidth / (float) imageHeight ) / aspect;
        final float zNear=1f;
        final float zFar=100f;

        // compute projection parameters 'normal' frustum
        final float top=(float)Math.tan(fovy*((float)Math.PI)/360.0f)*zNear;
        final float bottom=-1.0f*top;
        final float left=aspect2*bottom;
        final float right=aspect2*top;
        final float w = right - left;
        final float h = top - bottom;

        // compute projection parameters 'tiled'
        final float l = left + tileX * w / imageWidth;
        final float r = l + tileWidth * w / imageWidth;
        final float b = bottom + tileY * h / imageHeight;
        final float t = b + tileHeight * h / imageHeight;

        gl.glFrustumf(l, r, b, t, zNear, zFar);
        // gl.glOrthof(-4.0f, 4.0f, -4.0f, 4.0f, 1.0f, 100.0f);

        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();

        System.err.println(Thread.currentThread()+" RedSquareES1.reshape FIN");
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        curTime = System.currentTimeMillis();
        final GL2ES1 gl = drawable.getGL().getGL2ES1();
        if( null != tileRendererInUse ) {
            gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        } else {
            gl.glClearColor(0, 0, 0, 0);
        }
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // One rotation every four seconds
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glTranslatef(0, 0, -10);
        final float ang = doRotate ? ((curTime - startTime) * 360.0f) / 4000.0f : 1f;
        gl.glRotatef(ang, 0, 0, 1);
        gl.glRotatef(ang, 0, 1, 0);

        // Draw a square
        gl.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GLPointerFunc.GL_COLOR_ARRAY);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GLPointerFunc.GL_COLOR_ARRAY);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" RedSquareES1.dispose ... ");
        final GL2ES1 gl = drawable.getGL().getGL2ES1();
        gl.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GLPointerFunc.GL_COLOR_ARRAY);
        colors.clear();
        colors   = null;
        vertices.clear();
        vertices = null;
        System.err.println(Thread.currentThread()+" RedSquareES1.dispose FIN");
    }
}
