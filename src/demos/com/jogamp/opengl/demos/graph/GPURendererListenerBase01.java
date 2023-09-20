/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl.demos.graph;

import java.io.File;
import java.io.IOException;

import com.jogamp.opengl.FPSCounter;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLPipelineFactory;
import com.jogamp.opengl.GLRunnable;

import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.font.FontScale;
import com.jogamp.math.FloatUtil;
import com.jogamp.math.Recti;
import com.jogamp.math.Vec3f;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.util.PMVMatrix4f;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.util.GLReadBufferUtil;

/**
 *
 * Action Keys:
 * - 1/2: zoom in/out
 * - 6/7: 2nd pass texture size
 * - 0/9: rotate
 * - Q/W: change weight
 * - v: toggle v-sync
 * - s: screenshot
 */
public abstract class GPURendererListenerBase01 implements GLEventListener {
    private final RegionRenderer renderer;
    private final int renderModes;
    private final boolean debug;
    private final boolean trace;

    protected GLRegion region;

    private final GLReadBufferUtil screenshot;

    private KeyAction keyAction;

    private volatile GLAutoDrawable autoDrawable = null;

    private final float[] position = new float[] {0,0,0};

    protected final float zNear = 0.1f, zFar = 7000f;
    /** Describing the bounding box in model-coordinates of the near-plane parallel at distance one. */
    protected final AABBox nearPlane1Box;

    private float xTran = -10;
    private float yTran =  10;
    private float ang = 0f;
    private float zTran = -70f;
    private final int[] sampleCount = new int[] { 4 };

    protected volatile float weight = 1.0f;
    boolean ignoreInput = false;

    public GPURendererListenerBase01(final RegionRenderer renderer, final int renderModes, final boolean debug, final boolean trace) {
        this.renderer = renderer;
        this.renderModes = renderModes;
        this.debug = debug;
        this.trace = trace;
        this.screenshot = new GLReadBufferUtil(false, false);
        nearPlane1Box = new AABBox();
    }

    public final RegionRenderer getRenderer() { return renderer; }
    public final int getRenderModes() { return renderModes; }
    public final float getZTran() { return zTran; }
    public final float getXTran() { return xTran; }
    public final float getYTran() { return yTran; }
    public final float getAngleDeg() { return ang; }
    public final float getAngleRad() { return FloatUtil.adegToRad(ang); }
    public final int[] getSampleCount() { return sampleCount; }
    public final float[] getPosition() { return position; }

    public void setMatrix(final float xtrans, final float ytrans, final float zTran, final float angle, final int sampleCount) {
        this.xTran = xtrans;
        this.yTran = ytrans;
        this.zTran = zTran;
        this.ang = angle;
        this.sampleCount[0] = sampleCount;
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        final Object upObj = drawable.getUpstreamWidget();
        if( upObj instanceof Window ) {
            final Window window = (Window) upObj;
            final float[] sPpMM = window.getPixelsPerMM(new float[2]);
            final float[] sDPI = FontScale.ppmmToPPI( new float[] { sPpMM[0], sPpMM[1] } );
            System.err.println("DPI "+sDPI[0]+" x "+sDPI[1]+", "+sPpMM[0]+" x "+sPpMM[1]+" pixel/mm");

            final float[] hasSurfacePixelScale1 = window.getCurrentSurfaceScale(new float[2]);
            System.err.println("HiDPI PixelScale: "+hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
        }
        autoDrawable = drawable;
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(debug) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Debug", null, gl, null) ).getGL2ES2();
        }
        if(trace) {
            gl = gl.getContext().setGL( GLPipelineFactory.create("com.jogamp.opengl.Trace", null, gl, new Object[] { System.err } ) ).getGL2ES2();
        }
        System.err.println("*** "+gl.getContext().getGLVersion());
        System.err.println("*** GLDebugMessage "+gl.getContext().isGLDebugMessageEnabled());
        MSAATool.dump(drawable);
        gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        getRenderer().init(gl);
    }

    public static void mapWin2ObjectCoords(final PMVMatrix4f pmv, final Recti view,
                                           final float zNear, final float zFar,
                                           final float orthoX, final float orthoY, final float orthoDist,
                                           final float[] winZ, final Vec3f objPos) {
        winZ[0] = (1f/zNear-1f/orthoDist)/(1f/zNear-1f/zFar);
        pmv.mapWinToObj(orthoX, orthoY, winZ[0], view, objPos);
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int xstart, final int ystart, final int width, final int height) {
        final PMVMatrix4f pmv = renderer.getMatrix();
        renderer.reshapePerspective(FloatUtil.QUARTER_PI, width, height, zNear, zFar);
        pmv.loadMvIdentity();
        System.err.printf("Reshape: zNear %f,  zFar %f%n", zNear, zFar);
        System.err.printf("Reshape: Frustum: %s%n", pmv.getFrustum());
        {
            final float orthoDist = 1f;
            final Vec3f obj00Coord = new Vec3f();
            final Vec3f obj11Coord = new Vec3f();
            final float[] winZ = new float[1];
            final Recti view = new Recti(0, 0, width, height);

            mapWin2ObjectCoords(pmv, view, zNear, zFar, 0f, 0f, orthoDist, winZ, obj00Coord);
            System.err.printf("Reshape: mapped.00: [%f, %f, %f], winZ %f -> [%s]%n", 0f, 0f, orthoDist, winZ[0], obj00Coord);

            mapWin2ObjectCoords(pmv, view, zNear, zFar, width, height, orthoDist, winZ, obj11Coord);
            System.err.printf("Reshape: mapped.11: [%f, %f, %f], winZ %f -> [%s]%n", (float)width, (float)height, orthoDist, winZ[0], obj11Coord);

            nearPlane1Box.setSize( obj00Coord, obj11Coord );
            System.err.printf("Reshape: dist1Box: %s%n", nearPlane1Box);
        }

        dumpMatrix();
        // System.err.println("Reshape: "+renderer.getRenderState());
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        autoDrawable = null;
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(null != region) {
            region.destroy(gl);
        }
        screenshot.dispose(gl);
        renderer.destroy(gl);
    }

    public void zoom(final int v){
        zTran += v;
        dumpMatrix();
    }

    public void move(final float x, final float y){
        xTran += x;
        yTran += y;
        dumpMatrix();
    }
    public void rotate(final float delta){
        ang += delta;
        ang %= 360.0f;
        dumpMatrix();
    }
    public void editGlobalWeight(final float delta) {
        if( !RenderState.isWeightValid(weight+delta) ) {
            return;
        }
        weight += delta;
        System.err.println("Global Weight: "+ weight);
    }

    void dumpMatrix() {
        System.err.println("Matrix: " + xTran + " / " + yTran + " / "+zTran + " @ "+ang);
    }

    /** Attach the input listener to the window */
    public void attachInputListenerTo(final GLWindow window) {
        if ( null == keyAction ) {
            keyAction = new KeyAction();
            window.addKeyListener(keyAction);
        }
    }

    public void detachInputListenerFrom(final GLWindow window) {
        if ( null == keyAction ) {
            return;
        }
        window.removeKeyListener(keyAction);
    }

    public void printScreen(final GLAutoDrawable drawable, final String dir, final String tech, final String objName, final boolean exportAlpha) throws GLException, IOException {
        final String sw = String.format("_s%02d-%s-Z%04d-snap%02d-%03dx%03d", sampleCount[0], objName, (int)Math.abs(zTran), screenshot_num++, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        final String filename = dir + tech + sw +".png";
        if(screenshot.readPixels(drawable.getGL(), false)) {
            screenshot.write(new File(filename));
        }
    }
    private int screenshot_num = 0;

    public void printScreenOnGLThread(final GLAutoDrawable drawable, final String dir, final String tech, final String objName, final boolean exportAlpha) {
        drawable.invoke(false, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable drawable) {
                try {
                    printScreen(drawable, dir, tech, objName, exportAlpha);
                } catch (final GLException e) {
                    e.printStackTrace();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
    }

    public void setIgnoreInput(final boolean v) {
        ignoreInput = v;
    }
    public boolean getIgnoreInput() {
        return ignoreInput;
    }

    public class KeyAction implements KeyListener {
        @Override
        public void keyPressed(final KeyEvent arg0) {
            if(ignoreInput) {
                return;
            }

            if(arg0.getKeyCode() == KeyEvent.VK_1){
                zoom(10);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_2){
                zoom(-10);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_UP){
                move(0, -1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_DOWN){
                move(0, 1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_LEFT){
                move(-1, 0);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_RIGHT){
                move(1, 0);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_6){
                sampleCount[0] -= 1;
                System.err.println("Sample Count: " + sampleCount[0]);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_7){
                sampleCount[0] += 1;
                System.err.println("Sample Count: " + sampleCount[0]);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_0){
                rotate(1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_9){
                rotate(-1);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_Q){
                editGlobalWeight(-0.1f);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_W){
                editGlobalWeight(0.1f);
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_V) {
                if(null != autoDrawable) {
                    autoDrawable.invoke(false, new GLRunnable() {
                        @Override
                        public boolean run(final GLAutoDrawable drawable) {
                            final GL gl = drawable.getGL();
                            final int _i = gl.getSwapInterval();
                            final int i;
                            switch(_i) {
                                case  0: i = -1; break;
                                case -1: i =  1; break;
                                case  1: i =  0; break;
                                default: i =  1; break;
                            }
                            gl.setSwapInterval(i);

                            final GLAnimatorControl a = drawable.getAnimator();
                            if( null != a ) {
                                a.resetFPSCounter();
                            }
                            if(drawable instanceof FPSCounter) {
                                ((FPSCounter)drawable).resetFPSCounter();
                            }
                            System.err.println("Swap Interval: "+_i+" -> "+i+" -> "+gl.getSwapInterval());
                            return true;
                        }
                    });
                }
            }
            else if(arg0.getKeyCode() == KeyEvent.VK_S){
                if(null != autoDrawable) {
                    final String modeS = Region.getRenderModeString(renderModes);
                    final String type = modeS + ( Region.hasVariableWeight(renderModes) ? "-vc" : "-uc" ) ;
                    printScreenOnGLThread(autoDrawable, "./", "demo-"+type, "", false);
                }
            }
        }
        @Override
        public void keyReleased(final KeyEvent arg0) {}
    }
}
