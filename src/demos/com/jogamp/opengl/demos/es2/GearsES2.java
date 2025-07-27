/**
 * Copyright (C) 2011 JogAmp Community. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * BRIAN PAUL BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.jogamp.opengl.demos.es2;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.math.Matrix4f;
import com.jogamp.math.Quaternion;
import com.jogamp.math.Vec3f;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.GestureHandler;
import com.jogamp.newt.event.GestureHandler.GestureEvent;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.PinchToZoomGesture;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.demos.GearsObject;
import com.jogamp.opengl.demos.util.CommandlineOptions;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.CustomGLEventListener;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.caps.NonFSAAGLCapsChooser;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.stereo.EyeParameter;
import com.jogamp.opengl.util.stereo.StereoGLEventListener;
import com.jogamp.opengl.util.stereo.ViewerPose;

/**
 * GearsES2.java <BR>
 * @author Brian Paul (converted to Java by Ron Cemer and Sven Gothel) <P>
 */
public class GearsES2 implements StereoGLEventListener {
    private final FloatBuffer lightPos = Buffers.newDirectFloatBuffer( new float[] { 5.0f, 5.0f, 10.0f } );

    private ShaderState st = null;
    private PMVMatrix pmvMatrix = null;
    private GLUniformData pmvMatrixUniform = null;
    private GLUniformData colorU = null;
    private volatile float view_rotx = 20.0f, view_roty = 30.0f;

    private final float view_rotz = 0.0f;
    private float panX = 0.0f, panY = 0.0f, panZ=0.0f;
    private float scalexyz=1.0f;

    private volatile GearsObjectES2 gear1=null, gear2=null, gear3=null;
    private boolean useMappedBuffers = false;
    private boolean validateBuffers = false;
    private float angle = 0.0f;
    private int swapInterval = 0;
    // private MouseListener gearsMouse = new TraceMouseAdapter(new GearsMouseAdapter());
    public MouseListener gearsMouse = new GearsMouseAdapter();
    public KeyListener gearsKeys = new GearsKeyAdapter();

    private boolean doRotate = true;
    private float[] clearColor = null;
    private boolean clearBuffers = true;
    private boolean clearStencilBuffer = false;
    private boolean verbose = true;
    private volatile boolean isInit = false;

    private PinchToZoomGesture pinchToZoomGesture = null;


    public GearsES2(final int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public GearsES2() {
        this.swapInterval = 1;
    }

    public void setDoRotation(final boolean rotate) { this.doRotate = rotate; }
    public void setClearBuffers(final boolean v) { clearBuffers = v; }
    public void setClearStencilBuffer(final boolean v) { clearStencilBuffer = v; }
    public void setVerbose(final boolean v) { verbose = v; }

    /** float[4] */
    public void setClearColor(final float[] clearColor) {
        this.clearColor = clearColor;
    }

    public void setGearsColor(final int idx, final float r, final float g, final float b, final float a) {
        switch(idx) {
            case -1:
                gear1.setColor(r, g, b, a);
                gear2.setColor(r, g, b, a);
                gear3.setColor(r, g, b, a);
                return;
            case 0: gear1.setColor(r, g, b, a); return;
            case 1: gear2.setColor(r, g, b, a); return;
            case 2: gear3.setColor(r, g, b, a); return;
            default: return;
        }
    }
    public void resetGearsColor() {
        gear1.setColor(GearsObject.red.get(0), GearsObject.red.get(1), GearsObject.red.get(2), GearsObject.red.get(3));
        gear2.setColor(GearsObject.green.get(0), GearsObject.green.get(1), GearsObject.green.get(2), GearsObject.green.get(3));
        gear3.setColor(GearsObject.blue.get(0), GearsObject.blue.get(1), GearsObject.blue.get(2), GearsObject.blue.get(3));
    }

    public float getScale() { return scalexyz; }
    public void setScale(final float v) { scalexyz=v; }

    public float getRotX() { return view_rotx; }
    public float getRotY() { return view_roty; }
    public void setRotX(final float v) { view_rotx = v; }
    public void setRotY(final float v) { view_roty = v; }

    public void addPanning(final float x, final float y, final float z) { panX += x; panY += y; panZ += z; }

    /**
     * @return gear1
     */
    public GearsObjectES2 getGear1() { return gear1; }

    /**
     * @return gear2
     */
    public GearsObjectES2 getGear2() { return gear2; }

    /**
     * @return gear3
     */
    public GearsObjectES2 getGear3() { return gear3; }

    public void setUseMappedBuffers(final boolean v) { useMappedBuffers = v; }
    public void setValidateBuffers(final boolean v) { validateBuffers = v; }

    public PMVMatrix4f getPMVMatrix() {
        return pmvMatrix;
    }

    private static final int TIME_OUT     = 2000; // 2s
    private static final int POLL_DIVIDER   = 20; // TO/20
    private static final int TIME_SLICE   = TIME_OUT / POLL_DIVIDER ;

    /**
     * @return True if this GLEventListener became initialized within TIME_OUT 2s
     */
    public boolean waitForInit(final boolean initialized) throws InterruptedException {
        int wait;
        for (wait=0; wait<POLL_DIVIDER && initialized != isInit ; wait++) {
            Thread.sleep(TIME_SLICE);
        }
        return wait<POLL_DIVIDER;
    }

    private final String sid() { return "0x"+Integer.toHexString(hashCode()); }

    @Override
    public void init(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(verbose) {
            System.err.println(Thread.currentThread()+" GearsES2.init.0 "+sid()+", "+this);
            System.err.println("GearsES2 init "+sid()+" on "+Thread.currentThread());
            System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
            System.err.println("INIT GL IS: " + gl.getClass().getName());
            System.err.println(JoglVersion.getGLStrings(gl, null, false).toString());
        }
        if( !gl.hasGLSL() ) {
            System.err.println("No GLSL available, no rendering.");
            return;
        }

        st = new ShaderState();
        // st.setVerbose(true);
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, this.getClass(), "shader",
                "shader/bin", "gears", true);
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, this.getClass(), "shader",
                "shader/bin", "gears", true);
        vp0.defaultShaderCustomization(gl, true, true);
        fp0.defaultShaderCustomization(gl, true, true);
        final ShaderProgram sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);
        st.attachShaderProgram(gl, sp0, true);
        // Use debug pipeline
        // drawable.setGL(new DebugGL(drawable.getGL()));

        pmvMatrix = new PMVMatrix(PMVMatrix4f.INVERSE_MODELVIEW | PMVMatrix4f.INVERSE_TRANSPOSED_MODELVIEW);
        st.attachObject("pmvMatrix", pmvMatrix);
        pmvMatrixUniform = new GLUniformData("pmvMatrix", 4, 4, pmvMatrix.getSyncPMvMviMvit()); // P, Mv, Mvi and Mvit
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);

        final GLUniformData lightU = new GLUniformData("lightPos", 3, lightPos);
        st.ownUniform(lightU);
        st.uniform(gl, lightU);

        colorU = new GLUniformData("color", 4, Buffers.newDirectFloatBuffer(4));
        st.ownUniform(colorU);
        st.uniform(gl, colorU);

        {
            gear1 = new GearsObjectES2(gl, useMappedBuffers, st, GearsObject.red, 1.0f, 4.0f, 1.0f, 20, 0.7f, pmvMatrix, pmvMatrixUniform, colorU, validateBuffers);
            if(verbose) {
                System.err.println("gear1 "+sid()+" created: "+gear1);
            }

            gear2 = new GearsObjectES2(gl, useMappedBuffers, st, GearsObject.green, 0.5f, 2.0f, 2.0f, 10, 0.7f, pmvMatrix, pmvMatrixUniform, colorU, validateBuffers);
            if(verbose) {
                System.err.println("gear2 "+sid()+" created: "+gear2);
            }

            gear3 = new GearsObjectES2(gl, useMappedBuffers, st, GearsObject.blue, 1.3f, 2.0f, 0.5f, 10, 0.7f, pmvMatrix, pmvMatrixUniform, colorU, validateBuffers);
            if(verbose) {
                System.err.println("gear3 "+sid()+" created: "+gear2);
            }
        }

        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.addMouseListener(gearsMouse);
            window.addKeyListener(gearsKeys);
            window.addGestureListener(pinchToZoomListener);
            pinchToZoomGesture = new PinchToZoomGesture(drawable.getNativeSurface(), false);
            window.addGestureHandler(pinchToZoomGesture);
        }

        st.useProgram(gl, false);

        gl.glFinish(); // make sure .. for shared context (impacts OSX 10.9)

        isInit = true;
        if(verbose) {
            System.err.println(Thread.currentThread()+" GearsES2.init.X "+sid()+" FIN "+this);
        }
    }

    public final boolean isInit() { return isInit; }

    private final GestureHandler.GestureListener pinchToZoomListener = new GestureHandler.GestureListener() {
        @Override
        public void gestureDetected(final GestureEvent gh) {
            final PinchToZoomGesture.ZoomEvent ze = (PinchToZoomGesture.ZoomEvent) gh;
            final float zoom = ze.getZoom(); //  * ( ze.getTrigger().getPointerCount() - 1 ); <- too much ..
            panZ = zoom * 30f - 30f; // [0 .. 2] -> [-30f .. 30f]
        }
    };

    private float zNear = 5f;
    private float zFar = 10000f;
    private float zViewDist = 40.0f;

    public void setZ(final float zNear, final float zFar, final float zViewDist) {
        this.zNear = zNear;
        this.zFar = zFar;
        this.zViewDist = zViewDist;
    }
    public float getZNear() { return zNear; }
    public float getZFar() { return zFar; }
    public float getZViewDist() { return zViewDist; }

    @Override
    public void reshape(final GLAutoDrawable glad, final int x, final int y, final int width, final int height) {
        if( !isInit ) { return; }
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        final boolean msaa = gl.getContext().getGLDrawable().getChosenGLCapabilities().getSampleBuffers();
        if(verbose) {
            System.err.println(Thread.currentThread()+" GearsES2.reshape "+sid()+" "+x+"/"+y+" "+width+"x"+height+", swapInterval "+swapInterval+
                               ", drawable 0x"+Long.toHexString(gl.getContext().getGLDrawable().getHandle())+", msaa "+msaa);
        }
        gl.setSwapInterval(swapInterval);

        if( !gl.hasGLSL() ) {
            return;
        }

        // compute projection parameters
        float l, r, b, t;
        if( height > width ) {
            final float a = (float)height / (float)width;
            l = -1.0f;
            r = 1.0f;
            b = -a;
            t = a;
        } else {
            final float a = (float)width / (float)height;
            l = -a;
            r = a;
            b = -1.0f;
            t = 1.0f;
        }
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glFrustumf(l, r, b, t, zNear, zFar);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0.0f, 0.0f, -zViewDist);
        st.useProgram(gl, true);
        st.uniform(gl, pmvMatrixUniform);
        st.useProgram(gl, false);
    }
    // private boolean useAndroidDebug = false;

    private final Matrix4f mat4Tmp1 = new Matrix4f();
    private final Matrix4f mat4Tmp2 = new Matrix4f();
    private final Vec3f vec3Tmp1 = new Vec3f();
    private final Vec3f vec3Tmp2 = new Vec3f();
    private final Vec3f vec3Tmp3 = new Vec3f();

    private static final float scalePos = 20f;

    @Override
    public void reshapeForEye(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height,
                              final EyeParameter eyeParam, final ViewerPose viewerPose) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        {
            //
            // Projection
            //
            final Matrix4f mat4 = new Matrix4f();
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            mat4.setToPerspective(eyeParam.fovhv, zNear, zFar);
            pmvMatrix.glLoadMatrixf(mat4);

            //
            // Modelview
            //
            pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            final Quaternion rollPitchYaw = new Quaternion();
            // private final float eyeYaw = FloatUtil.PI; // 180 degrees in radians
            // rollPitchYaw.rotateByAngleY(eyeYaw);
            // final Vec3f shiftedEyePos = rollPitchYaw.rotateVector(viewerPose.position, vec3Tmp1);
            final Vec3f shiftedEyePos = vec3Tmp1.set(viewerPose.position);
            shiftedEyePos.scale(scalePos); // amplify viewerPose position
            shiftedEyePos.add(eyeParam.positionOffset);

            rollPitchYaw.mult(viewerPose.orientation);
            final Vec3f up = rollPitchYaw.rotateVector(Vec3f.UNIT_Y, vec3Tmp2);
            final Vec3f forward = rollPitchYaw.rotateVector(Vec3f.UNIT_Z_NEG, vec3Tmp3); // -> center
            final Vec3f center = forward.add(shiftedEyePos);

            final Matrix4f mLookAt = mat4Tmp2.setToLookAt(shiftedEyePos, center, up, mat4Tmp1);
            mat4.mul( mat4Tmp1.setToTranslation( eyeParam.distNoseToPupilX,
                                                 eyeParam.distMiddleToPupilY,
                                                 eyeParam.eyeReliefZ ), mLookAt);
            mat4.translate(0, 0, -zViewDist, mat4Tmp1);
            pmvMatrix.glLoadMatrixf(mat4);
        }
        st.useProgram(gl, true);
        st.uniform(gl, pmvMatrixUniform);
        st.useProgram(gl, false);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        if( !isInit ) { return; }
        isInit = false;
        if(verbose) {
            System.err.println(Thread.currentThread()+" GearsES2.dispose "+sid());
        }
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.removeMouseListener(gearsMouse);
            window.removeKeyListener(gearsKeys);
            window.removeGestureHandler(pinchToZoomGesture);
            pinchToZoomGesture = null;
            window.removeGestureListener(pinchToZoomListener);
        }
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if( !gl.hasGLSL() ) {
            return;
        }
        st.useProgram(gl, false);
        gear1.destroy(gl);
        gear1 = null;
        gear2.destroy(gl);
        gear2 = null;
        gear3.destroy(gl);
        gear3 = null;
        pmvMatrix = null;
        colorU = null;
        st.destroy(gl);
        st = null;

        if(verbose) {
            System.err.println(Thread.currentThread()+" GearsES2.dispose "+sid()+" FIN");
        }
    }

    @Override
    public void display(final GLAutoDrawable drawable) {
        display(drawable, 0);
    }

    @Override
    public void display(final GLAutoDrawable drawable, final int flags) {
        if( !isInit ) { return; }

        final boolean repeatedFrame = 0 != ( CustomGLEventListener.DISPLAY_REPEAT & flags );
        final boolean dontClear = 0 != ( CustomGLEventListener.DISPLAY_DONTCLEAR & flags );

        // Turn the gears' teeth
        if( doRotate && !repeatedFrame ) {
            angle += 0.5f;
        }

        // Get the GL corresponding to the drawable we are animating
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if( clearBuffers && !dontClear ) {
            if( null != clearColor ) {
              gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            } else {
              gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            }
            if( clearStencilBuffer ) {
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);
            } else {
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            }
        }
        if( !gl.hasGLSL() ) {
            return;
        }

        setGLStatesImpl(gl, true);

        st.useProgram(gl, true);
        pmvMatrix.glPushMatrix();
        pmvMatrix.glTranslatef(panX, panY, panZ);
        pmvMatrix.glScalef(scalexyz, scalexyz, scalexyz);
        pmvMatrix.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
        pmvMatrix.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
        pmvMatrix.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);
        {
            gear1.draw(gl, -3.0f, -2.0f,  1f * angle -    0f);
            gear2.draw(gl,  3.1f, -2.0f, -2f * angle -  9.0f);
            gear3.draw(gl, -3.1f,  4.2f, -2f * angle - 25.0f);
        }
        pmvMatrix.glPopMatrix();
        st.useProgram(gl, false);

        setGLStatesImpl(gl, false);
    }

    private void setGLStatesImpl(final GL2ES2 gl, final boolean enable) {
        // Culling only possible if we do not flip the projection matrix
        final boolean useCullFace = !gl.getContext().getGLDrawable().isGLOriented();
        if( enable ) {
            gl.glEnable(GL.GL_DEPTH_TEST);
            if( useCullFace ) {
                gl.glEnable(GL.GL_CULL_FACE);
            }
        } else {
            gl.glDisable(GL.GL_DEPTH_TEST);
            if( useCullFace ) {
                gl.glDisable(GL.GL_CULL_FACE);
            }
        }
    }

    @Override
    public String toString() {
        return "GearsES2[obj "+sid()+" isInit "+isInit+", 1 "+gear1+", 2 "+gear2+", 3 "+gear3+"]";
    }

    public KeyListener getKeyListener() { return this.gearsKeys; }
    public MouseListener getMouseListener() { return this.gearsMouse; }

    class GearsKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(final KeyEvent e) {
            final int kc = e.getKeyCode();
            if(KeyEvent.VK_LEFT == kc) {
                view_roty -= 1;
            } else if(KeyEvent.VK_RIGHT == kc) {
                view_roty += 1;
            } else if(KeyEvent.VK_UP == kc) {
                view_rotx -= 1;
            } else if(KeyEvent.VK_DOWN == kc) {
                view_rotx += 1;
            }
        }
    }

    class GearsMouseAdapter implements MouseListener{
        private int prevMouseX, prevMouseY;

        @Override
        public void mouseClicked(final MouseEvent e) {
        }

        @Override
        public void mouseEntered(final MouseEvent e) {
        }

        @Override
        public void mouseExited(final MouseEvent e) {
        }

        @Override
        public void mouseWheelMoved(final MouseEvent e) {
            final float[] rot = e.getRotation();
            if( e.isControlDown() ) {
                // alternative zoom
                final float incr = e.isShiftDown() ? rot[0] : rot[1] * 0.5f ;
                panZ += incr;
                System.err.println("panZ.2: incr "+incr+", dblZoom "+e.isShiftDown()+" -> "+panZ);
            } else {
                // panning
                panX -= rot[0]; // positive -> left
                panY += rot[1]; // positive -> up
                System.err.println("panXY.2: incr ("+rot[0]+", "+rot[1]+") -> ("+panX+", "+panY+")");
            }
        }

        @Override
        public void mousePressed(final MouseEvent e) {
            if( e.getPointerCount()==1 ) {
                prevMouseX = e.getX();
                prevMouseY = e.getY();
            } else if( e.getPointerCount() == 4 ) {
                final Object src = e.getSource();
                if( e.getPressure(0, true) > 0.7f && src instanceof Window) { // show Keyboard
                   ((Window) src).setKeyboardVisible(true);
                }
            }
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
        }

        @Override
        public void mouseMoved(final MouseEvent e) {
            if( e.isConfined() ) {
                navigate(e);
            } else {
                // track prev. position so we don't have 'jumps'
                // in case we move to confined navigation.
                prevMouseX = e.getX();
                prevMouseY = e.getY();
            }
        }

        @Override
        public void mouseDragged(final MouseEvent e) {
            navigate(e);
        }

        private void navigate(final MouseEvent e) {
            final int x = e.getX();
            final int y = e.getY();

            int width, height;
            final Object source = e.getSource();
            Window window = null;
            if(source instanceof Window) {
                window = (Window) source;
                width=window.getSurfaceWidth();
                height=window.getSurfaceHeight();
            } else if (source instanceof GLAutoDrawable) {
                final GLAutoDrawable glad = (GLAutoDrawable) source;
                width = glad.getSurfaceWidth();
                height = glad.getSurfaceHeight();
            } else {
                throw new RuntimeException("Event source neither Window nor Component: "+source);
            }
            final float thetaY = 360.0f * ( (float)(x-prevMouseX)/(float)width);
            final float thetaX = 360.0f * ( (float)(prevMouseY-y)/(float)height);
            view_rotx += thetaX;
            view_roty += thetaY;
            prevMouseX = x;
            prevMouseY = y;
            // System.err.println("rotXY.1: "+view_rotx+"/"+view_roty+", source "+e);
        }
    }

    public static void main(final String[] args) {
        final CommandlineOptions options = new CommandlineOptions(1280, 720, 0);

        System.err.println(options);
        System.err.println(VersionUtil.getPlatformInfo());
        // System.err.println(JoglVersion.getAllAvailableCapabilitiesInfo(dpy.getGraphicsDevice(), null).toString());

        final GLCapabilities reqCaps = options.getGLCaps();
        System.out.println("Requested: " + reqCaps);

        final GLWindow window = GLWindow.create(reqCaps);
        if( 0 == options.sceneMSAASamples ) {
            window.setCapabilitiesChooser(new NonFSAAGLCapsChooser(false));
        }
        window.setSize(options.surface_width, options.surface_height);
        window.setTitle(GearsES2.class.getSimpleName());

        window.addGLEventListener(new GearsES2(1));

        final Animator animator = new Animator(0 /* w/o AWT */);
        animator.setUpdateFPSFrames(5*60, null);
        animator.add(window);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(final WindowEvent e) {
                animator.stop();
            }
        });

        window.setVisible(true);
        animator.start();
    }

}
