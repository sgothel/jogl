/**
 * OlympicGL2
 */

package com.jogamp.opengl.test.junit.jogl.demos.es1;

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

import java.lang.Math;

public class OlympicES1 implements GLEventListener
{
    private boolean debugFFPEmu = false;
    private boolean verboseFFPEmu = false;
    private boolean traceFFPEmu = false;
    private boolean forceFFPEmu = false;
    private boolean debug = false ;
    private boolean trace = false ;

    // private static final double M_PI= 3.141592654;
    private static final double M_2PI= 2*3.141592654;

    private static final int
    // XSIZE=   100,
    // YSIZE=   75,
    RINGS= 5,
    BLUERING= 0,
    BLACKRING= 1,
    REDRING= 2,
    YELLOWRING =3,
    GREENRING =4,
    // BACKGROUND =8,
    BLACK = 0,
    RED=1,
    GREEN=2,
    YELLOW=3,
    BLUE=4
    // ,MAGENTA=5,
    // CYAN=6,
    // WHITE=7
    ;

    private byte rgb_colors[][];
    private int mapped_colors[];
    private float dests[][];
    private float offsets[][];
    private float angs[];
    private float rotAxis[][];
    private int iters[];
    private ImmModeSink theTorus;

    private final float lmodel_ambient[] = {0.0f, 0.0f, 0.0f, 0.0f};
    private final float lmodel_twoside[] = {0.0f, 0.0f, 0.0f, 0.0f};
    // private float lmodel_local[] = {0.0f, 0.0f, 0.0f, 0.0f};
    private final float light0_ambient[] = {0.1f, 0.1f, 0.1f, 1.0f};
    private final float light0_diffuse[] = {1.0f, 1.0f, 1.0f, 0.0f};
    private final float light0_position[] = {0.8660254f, 0.5f, 1f, 0f};
    private final float light0_specular[] = {1.0f, 1.0f, 1.0f, 0.0f};
    private final float bevel_mat_ambient[] = {0.0f, 0.0f, 0.0f, 1.0f};
    private final float bevel_mat_shininess[] = {40.0f, 0f, 0f, 0f};
    private final float bevel_mat_specular[] = {1.0f, 1.0f, 1.0f, 0.0f};
    private final float bevel_mat_diffuse[] = {1.0f, 0.0f, 0.0f, 0.0f};
    private final int swapInterval;
    private GLU glu;

    public OlympicES1() {
        swapInterval = 1;
    }

    public OlympicES1(int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public void setForceFFPEmu(boolean forceFFPEmu, boolean verboseFFPEmu, boolean debugFFPEmu, boolean traceFFPEmu) {
        this.forceFFPEmu = forceFFPEmu;
        this.verboseFFPEmu = verboseFFPEmu;
        this.debugFFPEmu = debugFFPEmu;
        this.traceFFPEmu = traceFFPEmu;
    }

    public void init(GLAutoDrawable drawable)
    {
        GL _gl = drawable.getGL();

        if(debugFFPEmu) {
            // Debug ..
            _gl = _gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Debug", GL2ES2.class, _gl, null) );
            debug = false;
        }
        if(traceFFPEmu) {
            // Trace ..
            _gl = _gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Trace", GL2ES2.class, _gl, new Object[] { System.err } ) );
            trace = false;
        }
        GL2ES1 gl = FixedFuncUtil.wrapFixedFuncEmul(_gl, ShaderSelectionMode.AUTO, null, forceFFPEmu, verboseFFPEmu);

        if(debug) {
            try {
                // Debug ..
                gl = (GL2ES1) gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Debug", GL2ES1.class, gl, null) );
            } catch (Exception e) {e.printStackTrace();}
        }
        if(trace) {
            try {
                // Trace ..
                gl = (GL2ES1) gl.getContext().setGL( GLPipelineFactory.create("javax.media.opengl.Trace", GL2ES1.class, gl, new Object[] { System.err } ) );
            } catch (Exception e) {e.printStackTrace();}
        }

        System.err.println("OlympicES1 init on "+Thread.currentThread());
        System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
        System.err.println("INIT GL IS: " + gl.getClass().getName());
        System.err.println("GL_VENDOR: " + gl.glGetString(GL.GL_VENDOR));
        System.err.println("GL_RENDERER: " + gl.glGetString(GL.GL_RENDERER));
        System.err.println("GL_VERSION: " + gl.glGetString(GL.GL_VERSION));
        System.err.println("GL GLSL: "+gl.hasGLSL()+", has-compiler-func: "+gl.isFunctionAvailable("glCompileShader")+", version "+(gl.hasGLSL() ? gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION) : "none"));
        System.err.println("GL FBO: basic "+ gl.hasBasicFBOSupport()+", full "+gl.hasFullFBOSupport());
        System.err.println("GL Profile: "+gl.getGLProfile());
        System.err.println("GL:" + gl + ", " + gl.getContext().getGLVersion());

        glu = GLUgl2es1.createGLU(gl);
        System.err.println("GLU:" + glu.getClass().getName());

        rgb_colors=new byte[RINGS][3];
        mapped_colors=new int [RINGS];
        dests=new float [RINGS][3];
        offsets=new float[RINGS][3];
        angs=new float[RINGS];
        rotAxis=new float[RINGS][3];
        iters=new int[RINGS];

        int i;
        float top_y = 1.0f;
        float bottom_y = 0.0f;
        float top_z = 0.15f;
        float bottom_z = 0.69f;
        float spacing = 2.5f;

        for (i = 0; i < RINGS; i++) {
            rgb_colors[i][0] = rgb_colors[i][1] = rgb_colors[i][2] = (byte)0;
        }
        rgb_colors[BLUERING][2] = (byte)255;
        rgb_colors[REDRING][0] = (byte)255;
        rgb_colors[GREENRING][1] = (byte)255;
        rgb_colors[YELLOWRING][0] = (byte)255;
        rgb_colors[YELLOWRING][1] = (byte)255;
        mapped_colors[BLUERING] = BLUE;
        mapped_colors[REDRING] = RED;
        mapped_colors[GREENRING] = GREEN;
        mapped_colors[YELLOWRING] = YELLOW;
        mapped_colors[BLACKRING] = BLACK;

        dests[BLUERING][0] = -spacing;
        dests[BLUERING][1] = top_y;
        dests[BLUERING][2] = top_z;

        dests[BLACKRING][0] = 0.0f;
        dests[BLACKRING][1] = top_y;
        dests[BLACKRING][2] = top_z;

        dests[REDRING][0] = spacing;
        dests[REDRING][1] = top_y;
        dests[REDRING][2] = top_z;

        dests[YELLOWRING][0] = -spacing / 2.0f;
        dests[YELLOWRING][1] = bottom_y;
        dests[YELLOWRING][2] = bottom_z;

        dests[GREENRING][0] = spacing / 2.0f;
        dests[GREENRING][1] = bottom_y;
        dests[GREENRING][2] = bottom_z;

        theTorus = ImmModeSink.createFixed(40,
                3, GL.GL_FLOAT, // vertex
                0, GL.GL_FLOAT, // color
                3, GL.GL_FLOAT, // normal
                0, GL.GL_FLOAT, // texCoords
                GL.GL_STATIC_DRAW);
        FillTorus(gl, theTorus, 0.1f, 8, 1.0f, 25);

        gl.glEnable(GL.GL_CULL_FACE);
        gl.glCullFace(GL.GL_BACK);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glClearDepth(1.0);

        gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_AMBIENT, light0_ambient, 0);
        gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_DIFFUSE, light0_diffuse, 0);
        gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_SPECULAR, light0_specular, 0);
        gl.glLightfv(GL2ES1.GL_LIGHT0, GL2ES1.GL_POSITION, light0_position, 0);
        gl.glEnable(GL2ES1.GL_LIGHT0);

        // gl.glLightModelfv(GL2.GL_LIGHT_MODEL_LOCAL_VIEWER, lmodel_local, 0);
        gl.glLightModelfv(GL2ES1.GL_LIGHT_MODEL_TWO_SIDE, lmodel_twoside, 0);
        gl.glLightModelfv(GL2ES1.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);
        gl.glEnable(GL2ES1.GL_LIGHTING);

        gl.glClearColor(0.5f, 0.5f, 0.5f, 0.0f);

        gl.glMaterialfv(GL.GL_FRONT, GL2ES1.GL_AMBIENT, bevel_mat_ambient, 0);
        gl.glMaterialfv(GL.GL_FRONT, GL2ES1.GL_SHININESS, bevel_mat_shininess, 0);
        gl.glMaterialfv(GL.GL_FRONT, GL2ES1.GL_SPECULAR, bevel_mat_specular, 0);
        gl.glMaterialfv(GL.GL_FRONT, GL2ES1.GL_DIFFUSE, bevel_mat_diffuse, 0);

        // gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL2ES1.GL_DIFFUSE);
        gl.glEnable(GL2ES1.GL_COLOR_MATERIAL);
        gl.glShadeModel(GL2ES1.GL_SMOOTH);

        ReInit();
        t0 = System.currentTimeMillis();
        tL = t0;
    }


    @Override
    public void dispose(GLAutoDrawable glad) {
        glu = null;
        theTorus.destroy(glad.getGL());
        theTorus = null;
    }


    @Override
    public void reshape(GLAutoDrawable glad, int x, int y, int width, int height) {
        final GL2ES1 gl = glad.getGL().getGL2ES1();
        gl.setSwapInterval(swapInterval);

        gl.glMatrixMode(GL2ES1.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45f, (float) width / (float) height, 0.1f, 100.0f);
        gl.glMatrixMode(GL2ES1.GL_MODELVIEW);
        gl.glLoadIdentity();
        glu.gluLookAt(0, 0, 10, 0, 0, 0, 0, 1, 0);
    }

    @Override
    public void display(GLAutoDrawable glad) {
        final GL2ES1 gl = glad.getGL().getGL2ES1();
        int i;

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glPushMatrix();
        for (i = 0; i < RINGS; i++) {
            gl.glColor4ub(rgb_colors[i][0], rgb_colors[i][1], rgb_colors[i][2], (byte)1);
            gl.glPushMatrix();
            gl.glTranslatef(dests[i][0] + offsets[i][0],
                    dests[i][1] + offsets[i][1],
                    dests[i][2] + offsets[i][2]);
            gl.glRotatef(angs[i], rotAxis[i][0], rotAxis[i][1], rotAxis[i][2]);
            theTorus.draw(gl, true);
            gl.glPopMatrix();
        }
        gl.glPopMatrix();
        animationCalc();
    }

    long t0, tL;

    protected void animationCalc()
    {
        int i, j;

        long t1 = System.currentTimeMillis();
        if( t1 - tL < 50 ) {
            return;
        }

        for (i = 0; i < RINGS; i++) {
            if (iters[i]!=0) {
                for (j = 0; j < 3; j++) {
                    offsets[i][j] = Clamp(iters[i], offsets[i][j]);
                }
                angs[i] = Clamp(iters[i], angs[i]);
                iters[i]--;
            }
        }
        if (iters[0]==0)
        {
            ReInit();
        }

        tL = t1;
    }

    protected void ReInit() {
        int i;
        float deviation;

        deviation = MyRand() / 2;
        deviation = deviation * deviation;
        for (i = 0; i < RINGS; i++) {
            offsets[i][0] = MyRand();
            offsets[i][1] = MyRand();
            offsets[i][2] = MyRand();
            angs[i] = (float) (260.0 * MyRand());
            rotAxis[i][0] = MyRand();
            rotAxis[i][1] = MyRand();
            rotAxis[i][2] = MyRand();
            iters[i] = ( int ) (deviation * MyRand() + 60.0);
        }
    }

    protected static void FillTorus(GL gl, ImmModeSink immModeSink, float rc, int numc, float rt, int numt)
    {
        int i, j, k;
        double s, t;
        float x, y, z;

        for (i = 0; i < numc; i++) {
            immModeSink.glBegin(ImmModeSink.GL_QUAD_STRIP);
            for (j = 0; j <= numt; j++) {
                for (k = 1; k >= 0; k--) {
                    s = (i + k) % numc + 0.5;
                    t = j % numt;

                    x = (float) Math.cos(t * M_2PI / numt) * (float) Math.cos(s * M_2PI / numc);
                    y = (float) Math.sin(t * M_2PI / numt) * (float) Math.cos(s * M_2PI / numc);
                    z = (float) Math.sin(s * M_2PI / numc);
                    immModeSink.glNormal3f(x, y, z);

                    x = (rt + rc * (float) Math.cos(s * M_2PI / numc)) * (float) Math.cos(t * M_2PI / numt);
                    y = (rt + rc * (float) Math.cos(s * M_2PI / numc)) * (float) Math.sin(t * M_2PI / numt);
                    z = rc * (float) Math.sin(s * M_2PI / numc);
                    immModeSink.glVertex3f(x, y, z);
                }
            }
            immModeSink.glEnd(gl, false);
        }
    }

    protected float Clamp(int iters_left, float t)
    {
        if (iters_left < 3) {
            return 0.0f;
        }
        return (iters_left - 2) * t / iters_left;
    }

    protected float MyRand()
    {
        // return 10.0 * (drand48() - 0.5);
        return (float) ( 10.0 * (Math.random() - 0.5) );
    }

}

