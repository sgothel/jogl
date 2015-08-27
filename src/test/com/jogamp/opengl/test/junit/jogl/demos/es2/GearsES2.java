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
package com.jogamp.opengl.test.junit.jogl.demos.es2;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.GestureHandler;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.PinchToZoomGesture;
import com.jogamp.newt.event.GestureHandler.GestureEvent;
import com.jogamp.opengl.GLRendererQuirks;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.test.junit.jogl.demos.GearsObject;
import com.jogamp.opengl.util.CustomGLEventListener;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.TileRendererBase;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.stereo.EyeParameter;
import com.jogamp.opengl.util.stereo.ViewerPose;
import com.jogamp.opengl.util.stereo.StereoGLEventListener;

import java.nio.FloatBuffer;

import com.jogamp.nativewindow.NativeWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

/**
 * GearsES2.java <BR>
 * @author Brian Paul (converted to Java by Ron Cemer and Sven Gothel) <P>
 */
public class GearsES2 implements StereoGLEventListener, TileRendererBase.TileRendererListener {
    private final FloatBuffer lightPos = Buffers.newDirectFloatBuffer( new float[] { 5.0f, 5.0f, 10.0f } );

    private ShaderState st = null;
    private PMVMatrix pmvMatrix = null;
    private GLUniformData pmvMatrixUniform = null;
    private GLUniformData colorU = null;
    private float view_rotx = 20.0f, view_roty = 30.0f;
    private boolean flipVerticalInGLOrientation = false;
    private final boolean customRendering = false;

    private final float view_rotz = 0.0f;
    private float panX = 0.0f, panY = 0.0f, panZ=0.0f;
    private volatile GearsObjectES2 gear1=null, gear2=null, gear3=null;
    private GearsES2 sharedGears = null;
    private Object syncObjects = null;
    private boolean useMappedBuffers = false;
    private boolean validateBuffers = false;
    private volatile boolean usesSharedGears = false;
    private FloatBuffer gear1Color=GearsObject.red, gear2Color=GearsObject.green, gear3Color=GearsObject.blue;
    private float angle = 0.0f;
    private int swapInterval = 0;
    // private MouseListener gearsMouse = new TraceMouseAdapter(new GearsMouseAdapter());
    public MouseListener gearsMouse = new GearsMouseAdapter();
    public KeyListener gearsKeys = new GearsKeyAdapter();
    private TileRendererBase tileRendererInUse = null;
    private boolean doRotateBeforePrinting;

    private boolean doRotate = true;
    private boolean ignoreFocus = false;
    private float[] clearColor = null;
    private boolean clearBuffers = true;
    private boolean verbose = true;
    private volatile boolean isInit = false;

    private PinchToZoomGesture pinchToZoomGesture = null;


    public GearsES2(final int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public GearsES2() {
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
        System.err.println("GearsES2.startTileRendering: "+sid()+""+tr);
    }
    @Override
    public void endTileRendering(final TileRendererBase tr) {
        System.err.println("GearsES2.endTileRendering: "+sid()+""+tr);
    }

    public void setIgnoreFocus(final boolean v) { ignoreFocus = v; }
    public void setDoRotation(final boolean rotate) { this.doRotate = rotate; }
    public void setClearBuffers(final boolean v) { clearBuffers = v; }
    public void setVerbose(final boolean v) { verbose = v; }
    public void setFlipVerticalInGLOrientation(final boolean v) { flipVerticalInGLOrientation=v; }

    /** float[4] */
    public void setClearColor(final float[] clearColor) {
        this.clearColor = clearColor;
    }

    public void setGearsColors(final FloatBuffer gear1Color, final FloatBuffer gear2Color, final FloatBuffer gear3Color) {
        this.gear1Color = gear1Color;
        this.gear2Color = gear2Color;
        this.gear3Color = gear3Color;
    }

    public void setSharedGears(final GearsES2 shared) {
        sharedGears = shared;
    }

    public void setSyncObjects(final Object sync) {
        syncObjects = sync;
    }

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

    public boolean usesSharedGears() { return usesSharedGears; }

    public void setUseMappedBuffers(final boolean v) { useMappedBuffers = v; }
    public void setValidateBuffers(final boolean v) { validateBuffers = v; }

    public PMVMatrix getPMVMatrix() {
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
        if(null != sharedGears && !sharedGears.isInit() ) {
            System.err.println(Thread.currentThread()+" GearsES2.init.0 "+sid()+": pending shared Gears .. re-init later XXXXX");
            drawable.setGLEventListenerInitState(this, false);
            return;
        }

        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(verbose) {
            System.err.println(Thread.currentThread()+" GearsES2.init.0 "+sid()+": tileRendererInUse "+tileRendererInUse+", "+this);
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

        pmvMatrix = new PMVMatrix();
        st.attachObject("pmvMatrix", pmvMatrix);
        pmvMatrixUniform = new GLUniformData("pmvMatrix", 4, 4, pmvMatrix.glGetPMvMvitMatrixf()); // P, Mv, Mvi and Mvit
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);

        final GLUniformData lightU = new GLUniformData("lightPos", 3, lightPos);
        st.ownUniform(lightU);
        st.uniform(gl, lightU);

        colorU = new GLUniformData("color", 4, GearsObject.red);
        st.ownUniform(colorU);
        st.uniform(gl, colorU);

        if( null != sharedGears ) {
            gear1 = new GearsObjectES2(sharedGears.getGear1(), st, pmvMatrix, pmvMatrixUniform, colorU);
            gear2 = new GearsObjectES2(sharedGears.getGear2(), st, pmvMatrix, pmvMatrixUniform, colorU);
            gear3 = new GearsObjectES2(sharedGears.getGear3(), st, pmvMatrix, pmvMatrixUniform, colorU);
            usesSharedGears = true;
            if(verbose) {
                System.err.println("gear1 "+sid()+" created w/ share: "+sharedGears.getGear1()+" -> "+gear1);
                System.err.println("gear2 "+sid()+" created w/ share: "+sharedGears.getGear2()+" -> "+gear2);
                System.err.println("gear3 "+sid()+" created w/ share: "+sharedGears.getGear3()+" -> "+gear3);
            }
            if( gl.getContext().hasRendererQuirk(GLRendererQuirks.NeedSharedObjectSync) ) {
                syncObjects = sharedGears;
                System.err.println("Shared GearsES2: Synchronized Objects due to quirk "+GLRendererQuirks.toString(GLRendererQuirks.NeedSharedObjectSync));
            } else if( null == syncObjects ) {
                syncObjects = new Object();
                System.err.println("Shared GearsES2: Unsynchronized Objects");
            }
        } else {
            gear1 = new GearsObjectES2(gl, useMappedBuffers, st, gear1Color, 1.0f, 4.0f, 1.0f, 20, 0.7f, pmvMatrix, pmvMatrixUniform, colorU, validateBuffers);
            if(verbose) {
                System.err.println("gear1 "+sid()+" created: "+gear1);
            }

            gear2 = new GearsObjectES2(gl, useMappedBuffers, st, gear2Color, 0.5f, 2.0f, 2.0f, 10, 0.7f, pmvMatrix, pmvMatrixUniform, colorU, validateBuffers);
            if(verbose) {
                System.err.println("gear2 "+sid()+" created: "+gear2);
            }

            gear3 = new GearsObjectES2(gl, useMappedBuffers, st, gear3Color, 1.3f, 2.0f, 0.5f, 10, 0.7f, pmvMatrix, pmvMatrixUniform, colorU, validateBuffers);
            if(verbose) {
                System.err.println("gear3 "+sid()+" created: "+gear2);
            }
            if( null == syncObjects ) {
                syncObjects = new Object();
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
        } else if (GLProfile.isAWTAvailable() && upstreamWidget instanceof java.awt.Component) {
            final java.awt.Component comp = (java.awt.Component) upstreamWidget;
            new com.jogamp.newt.event.awt.AWTMouseAdapter(gearsMouse, drawable).addTo(comp);
            new com.jogamp.newt.event.awt.AWTKeyAdapter(gearsKeys, drawable).addTo(comp);
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

    @Override
    public void reshape(final GLAutoDrawable glad, final int x, final int y, final int width, final int height) {
        if( !isInit ) { return; }
        final GL2ES2 gl = glad.getGL().getGL2ES2();
        gl.setSwapInterval(swapInterval);
        reshapeImpl(gl, x, y, width, height, width, height);
    }

    @Override
    public void reshapeTile(final TileRendererBase tr,
                            final int tileX, final int tileY, final int tileWidth, final int tileHeight,
                            final int imageWidth, final int imageHeight) {
        if( !isInit ) { return; }
        final GL2ES2 gl = tr.getAttachedDrawable().getGL().getGL2ES2();
        gl.setSwapInterval(0);
        reshapeImpl(gl, tileX, tileY, tileWidth, tileHeight, imageWidth, imageHeight);
    }

    private float zNear = 5f;
    private float zFar = 10000f;
    private float zViewDist = 40.0f;

    public void setZ(final float zNear, final float zFar, final float zViewDist) {
        this.zNear = zNear;
        this.zFar = zFar;
        this.zViewDist = zViewDist;
    }

    void reshapeImpl(final GL2ES2 gl, final int tileX, final int tileY, final int tileWidth, final int tileHeight, final int imageWidth, final int imageHeight) {
        final boolean msaa = gl.getContext().getGLDrawable().getChosenGLCapabilities().getSampleBuffers();
        if(verbose) {
            System.err.println(Thread.currentThread()+" GearsES2.reshape "+sid()+" "+tileX+"/"+tileY+" "+tileWidth+"x"+tileHeight+" of "+imageWidth+"x"+imageHeight+", swapInterval "+swapInterval+", drawable 0x"+Long.toHexString(gl.getContext().getGLDrawable().getHandle())+", msaa "+msaa+", tileRendererInUse "+tileRendererInUse);
        }

        if( !gl.hasGLSL() ) {
            return;
        }

        // compute projection parameters 'normal'
        float left, right, bottom, top;
        if( imageHeight > imageWidth ) {
            final float a = (float)imageHeight / (float)imageWidth;
            left = -1.0f;
            right = 1.0f;
            bottom = -a;
            top = a;
        } else {
            final float a = (float)imageWidth / (float)imageHeight;
            left = -a;
            right = a;
            bottom = -1.0f;
            top = 1.0f;
        }
        final float w = right - left;
        final float h = top - bottom;

        // compute projection parameters 'tiled'
        final float l = left + tileX * w / imageWidth;
        final float r = l + tileWidth * w / imageWidth;
        final float b = bottom + tileY * h / imageHeight;
        final float t = b + tileHeight * h / imageHeight;

        final float _w = r - l;
        final float _h = t - b;
        if(verbose) {
            System.err.println(">> GearsES2 "+sid()+", angle "+angle+", [l "+left+", r "+right+", b "+bottom+", t "+top+"] "+w+"x"+h+" -> [l "+l+", r "+r+", b "+b+", t "+t+"] "+_w+"x"+_h+", v-flip "+flipVerticalInGLOrientation);
        }

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        if( flipVerticalInGLOrientation && gl.getContext().getGLDrawable().isGLOriented() ) {
            pmvMatrix.glScalef(1f, -1f, 1f);
        }
        pmvMatrix.glFrustumf(l, r, b, t, zNear, zFar);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0.0f, 0.0f, -zViewDist);
        st.useProgram(gl, true);
        st.uniform(gl, pmvMatrixUniform);
        st.useProgram(gl, false);
    }
    // private boolean useAndroidDebug = false;

    private final float[] mat4Tmp1 = new float[16];
    private final float[] mat4Tmp2 = new float[16];
    private final float[] vec3Tmp1 = new float[3];
    private final float[] vec3Tmp2 = new float[3];
    private final float[] vec3Tmp3 = new float[3];

    private static final float[] vec3ScalePos = new float[] { 20f, 20f, 20f };

    @Override
    public void reshapeForEye(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height,
                              final EyeParameter eyeParam, final ViewerPose viewerPose) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        final float[] mat4Projection = FloatUtil.makePerspective(mat4Tmp1, 0, true, eyeParam.fovhv, zNear, zFar);
        if( flipVerticalInGLOrientation && gl.getContext().getGLDrawable().isGLOriented() ) {
            pmvMatrix.glLoadIdentity();
            pmvMatrix.glScalef(1f, -1f, 1f);
            pmvMatrix.glMultMatrixf(mat4Projection, 0);
        } else {
            pmvMatrix.glLoadMatrixf(mat4Projection, 0);
        }

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);

        final Quaternion rollPitchYaw = new Quaternion();
        // private final float eyeYaw = FloatUtil.PI; // 180 degrees in radians
        // rollPitchYaw.rotateByAngleY(eyeYaw);
        // final float[] shiftedEyePos = rollPitchYaw.rotateVector(vec3Tmp1, 0, viewerPose.position, 0);
        final float[] shiftedEyePos = VectorUtil.copyVec3(vec3Tmp1, 0, viewerPose.position, 0);
        VectorUtil.scaleVec3(shiftedEyePos, shiftedEyePos, vec3ScalePos); // amplify viewerPose position
        VectorUtil.addVec3(shiftedEyePos, shiftedEyePos, eyeParam.positionOffset);

        rollPitchYaw.mult(viewerPose.orientation);
        final float[] up = rollPitchYaw.rotateVector(vec3Tmp2, 0, VectorUtil.VEC3_UNIT_Y, 0);
        final float[] forward = rollPitchYaw.rotateVector(vec3Tmp3, 0, VectorUtil.VEC3_UNIT_Z_NEG, 0);
        final float[] center = VectorUtil.addVec3(forward, shiftedEyePos, forward);

        final float[] mLookAt = FloatUtil.makeLookAt(mat4Tmp1, 0, shiftedEyePos, 0, center, 0, up, 0, mat4Tmp2);
        final float[] mViewAdjust = FloatUtil.makeTranslation(mat4Tmp2, true, eyeParam.distNoseToPupilX, eyeParam.distMiddleToPupilY, eyeParam.eyeReliefZ);
        final float[] mat4Modelview = FloatUtil.multMatrix(mViewAdjust, mLookAt);

        pmvMatrix.glLoadMatrixf(mat4Modelview, 0);
        pmvMatrix.glTranslatef(0.0f, 0.0f, -zViewDist);
        st.useProgram(gl, true);
        st.uniform(gl, pmvMatrixUniform);
        st.useProgram(gl, false);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        if( !isInit ) { return; }
        isInit = false;
        if(verbose) {
            System.err.println(Thread.currentThread()+" GearsES2.dispose "+sid()+": tileRendererInUse "+tileRendererInUse);
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
        sharedGears = null;
        syncObjects = null;

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
        if(null != sharedGears && !sharedGears.isInit() ) { return; }
        final GLAnimatorControl anim = drawable.getAnimator();
        if( verbose && ( null == anim || !anim.isAnimating() ) ) {
            System.err.println(Thread.currentThread()+" GearsES2.display "+sid()+" "+drawable.getSurfaceWidth()+"x"+drawable.getSurfaceHeight()+", swapInterval "+swapInterval+", drawable 0x"+Long.toHexString(drawable.getHandle()));
        }

        final boolean repeatedFrame = 0 != ( CustomGLEventListener.DISPLAY_REPEAT & flags );
        final boolean dontClear = 0 != ( CustomGLEventListener.DISPLAY_DONTCLEAR & flags );

        // Turn the gears' teeth
        if( doRotate && !repeatedFrame ) {
            angle += 0.5f;
        }

        // Get the GL corresponding to the drawable we are animating
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        final boolean hasFocus;
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if(upstreamWidget instanceof NativeWindow) {
          hasFocus = ((NativeWindow)upstreamWidget).hasFocus();
        } else {
          hasFocus = true;
        }

        if( clearBuffers && !dontClear ) {
            if( null != clearColor ) {
              gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            } else if( null != tileRendererInUse ) {
              gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
            } else if( ignoreFocus || hasFocus ) {
              gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            } else {
              gl.glClearColor(0.2f, 0.2f, 0.2f, 0.0f);
            }
            // Special handling for the case where the GLJPanel is translucent
            // and wants to be composited with other Java 2D content
            if (GLProfile.isAWTAvailable() &&
                (drawable instanceof com.jogamp.opengl.awt.GLJPanel) &&
                !((com.jogamp.opengl.awt.GLJPanel) drawable).isOpaque() &&
                ((com.jogamp.opengl.awt.GLJPanel) drawable).shouldPreserveColorBufferIfTranslucent()) {
              gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
            } else {
              gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            }
        }
        if( !gl.hasGLSL() ) {
            return;
        }

        setGLStates(gl, true);

        st.useProgram(gl, true);
        pmvMatrix.glPushMatrix();
        pmvMatrix.glTranslatef(panX, panY, panZ);
        pmvMatrix.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
        pmvMatrix.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
        pmvMatrix.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);

        synchronized ( syncObjects ) {
            gear1.draw(gl, -3.0f, -2.0f,  1f * angle -    0f);
            gear2.draw(gl,  3.1f, -2.0f, -2f * angle -  9.0f);
            gear3.draw(gl, -3.1f,  4.2f, -2f * angle - 25.0f);
        }
        pmvMatrix.glPopMatrix();
        st.useProgram(gl, false);

        setGLStates(gl, false);
    }

    public void setGLStates(final GL2ES2 gl, final boolean enable) {
        // Culling only possible if we do not flip the projection matrix
        final boolean useCullFace = ! ( flipVerticalInGLOrientation && gl.getContext().getGLDrawable().isGLOriented() || customRendering );
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
        return "GearsES2[obj "+sid()+" isInit "+isInit+", usesShared "+usesSharedGears+", 1 "+gear1+", 2 "+gear2+", 3 "+gear3+", sharedGears "+sharedGears+"]";
    }

    class GearsKeyAdapter extends KeyAdapter {
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
            }
        }

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

        public void mouseReleased(final MouseEvent e) {
        }

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
            } else if (GLProfile.isAWTAvailable() && source instanceof java.awt.Component) {
                final java.awt.Component comp = (java.awt.Component) source;
                width=comp.getWidth(); // FIXME HiDPI: May need to convert window units -> pixel units!
                height=comp.getHeight();
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
}
