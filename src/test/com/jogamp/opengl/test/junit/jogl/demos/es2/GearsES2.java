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
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.jogl.demos.GearsObject;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import java.nio.FloatBuffer;

import javax.media.nativewindow.NativeWindow;
import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLUniformData;

/**
 * GearsES2.java <BR>
 * @author Brian Paul (converted to Java by Ron Cemer and Sven Gothel) <P>
 */
public class GearsES2 implements GLEventListener {
    private final FloatBuffer lightPos = Buffers.newDirectFloatBuffer( new float[] { 5.0f, 5.0f, 10.0f } );
    
    private ShaderState st = null;
    private PMVMatrix pmvMatrix = null;
    private GLUniformData pmvMatrixUniform = null;
    private GLUniformData colorU = null;
    private float view_rotx = 20.0f, view_roty = 30.0f, view_rotz = 0.0f;
    private float panX = 0.0f, panY = 0.0f, panZ=0.0f;
    private int drawableHeight = 1;
    private GearsObjectES2 gear1=null, gear2=null, gear3=null;
    private FloatBuffer gear1Color=GearsObject.red, gear2Color=GearsObject.green, gear3Color=GearsObject.blue;
    private float angle = 0.0f;
    private int swapInterval = 0;
    private boolean pmvUseBackingArray = true; // the default for PMVMatrix now, since it's faster
    // private MouseListener gearsMouse = new TraceMouseAdapter(new GearsMouseAdapter());
    public MouseListener gearsMouse = new GearsMouseAdapter();    
    public KeyListener gearsKeys = new GearsKeyAdapter();

    private boolean doRotate = true;
    private boolean ignoreFocus = false;
    private float[] clearColor = null;
    private boolean clearBuffers = true;
    private boolean verbose = true;

    public GearsES2(int swapInterval) {
        this.swapInterval = swapInterval;
    }

    public GearsES2() {
        this.swapInterval = 1;
    }

    public void setIgnoreFocus(boolean v) { ignoreFocus = v; }
    public void setDoRotation(boolean rotate) { this.doRotate = rotate; }
    public void setClearBuffers(boolean v) { clearBuffers = v; }
    public void setVerbose(boolean v) { verbose = v; }
    
    public void setPMVUseBackingArray(boolean pmvUseBackingArray) {
        this.pmvUseBackingArray = pmvUseBackingArray;
    }
    
    /** float[4] */
    public void setClearColor(float[] clearColor) {
        this.clearColor = clearColor; 
    }
    
    public void setGearsColors(FloatBuffer gear1Color, FloatBuffer gear2Color, FloatBuffer gear3Color) {
        this.gear1Color = gear1Color;
        this.gear2Color = gear2Color;
        this.gear3Color = gear3Color;
    }
    
    public void setGears(GearsObjectES2 g1, GearsObjectES2 g2, GearsObjectES2 g3) {
        gear1 = g1;
        gear2 = g2;
        gear3 = g3;
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


    public void init(GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" GearsES2.init ...");
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if(verbose) {
            System.err.println("GearsES2 init on "+Thread.currentThread());
            System.err.println("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
            System.err.println("INIT GL IS: " + gl.getClass().getName());
            System.err.println(JoglVersion.getGLStrings(gl, null, false).toString());
        }        
        if( !gl.hasGLSL() ) {
            System.err.println("No GLSL available, no rendering.");
            return;
        }

        gl.glEnable(GL.GL_DEPTH_TEST);
        
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
        
        pmvMatrix = new PMVMatrix(pmvUseBackingArray);
        st.attachObject("pmvMatrix", pmvMatrix);
        pmvMatrixUniform = new GLUniformData("pmvMatrix", 4, 4, pmvMatrix.glGetPMvMvitMatrixf()); // P, Mv, Mvi and Mvit
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);

        GLUniformData lightU = new GLUniformData("lightPos", 3, lightPos);
        st.ownUniform(lightU);
        st.uniform(gl, lightU);

        colorU = new GLUniformData("color", 4, GearsObject.red);
        st.ownUniform(colorU);
        st.uniform(gl, colorU);

        if(null == gear1) {
            gear1 = new GearsObjectES2(st, gear1Color, 1.0f, 4.0f, 1.0f, 20, 0.7f, pmvMatrix, pmvMatrixUniform, colorU);
            if(verbose) {
                System.err.println("gear1 created: "+gear1);
            }
        } else {
            gear1 = new GearsObjectES2(gear1, st, pmvMatrix, pmvMatrixUniform, colorU);
            if(verbose) {
                System.err.println("gear1 reused: "+gear1);
            }
        }
                    
        if(null == gear2) {
            gear2 = new GearsObjectES2(st, gear2Color, 0.5f, 2.0f, 2.0f, 10, 0.7f, pmvMatrix, pmvMatrixUniform, colorU);
            if(verbose) {
                System.err.println("gear2 created: "+gear2);
            }
        } else {
            gear2 = new GearsObjectES2(gear2, st, pmvMatrix, pmvMatrixUniform, colorU);
            if(verbose) {
                System.err.println("gear2 reused: "+gear2);
            }
        }
                
        if(null == gear3) {
            gear3 = new GearsObjectES2(st, gear3Color, 1.3f, 2.0f, 0.5f, 10, 0.7f, pmvMatrix, pmvMatrixUniform, colorU);
            if(verbose) {
                System.err.println("gear3 created: "+gear3);
            }
        } else {
            gear3 = new GearsObjectES2(gear3, st, pmvMatrix, pmvMatrixUniform, colorU);
            if(verbose) {
                System.err.println("gear3 reused: "+gear3);
            }
        }
    
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.addMouseListener(gearsMouse);
            window.addKeyListener(gearsKeys);
        } else if (GLProfile.isAWTAvailable() && upstreamWidget instanceof java.awt.Component) {
            final java.awt.Component comp = (java.awt.Component) upstreamWidget;
            new com.jogamp.newt.event.awt.AWTMouseAdapter(gearsMouse).addTo(comp);
            new com.jogamp.newt.event.awt.AWTKeyAdapter(gearsKeys).addTo(comp);
        }
        st.useProgram(gl, false);
        
        System.err.println(Thread.currentThread()+" GearsES2.init FIN");
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        System.err.println(Thread.currentThread()+" GearsES2.reshape "+x+"/"+y+" "+width+"x"+height+", swapInterval "+swapInterval+", drawable 0x"+Long.toHexString(drawable.getHandle()));
        
        drawableHeight = height;
        
        // Thread.dumpStack();
        final GL2ES2 gl = drawable.getGL().getGL2ES2();

        if(-1 != swapInterval) {
            gl.setSwapInterval(swapInterval); // in case switching the drawable (impl. may bound attribute there)
        }
        if( !gl.hasGLSL() ) {
            return;
        }
        
        st.useProgram(gl, true);
        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        
        if(height>width) {
            float h = (float)height / (float)width;
            pmvMatrix.glFrustumf(-1.0f, 1.0f, -h, h, 5.0f, 200.0f);
        } else {
            float h = (float)width / (float)height;
            pmvMatrix.glFrustumf(-h, h, -1.0f, 1.0f, 5.0f, 200.0f);
        }

        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0.0f, 0.0f, -40.0f);
        st.uniform(gl, pmvMatrixUniform);
        st.useProgram(gl, false);
        
        // System.err.println(Thread.currentThread()+" GearsES2.reshape FIN");
    }
    // private boolean useAndroidDebug = false;

    public void dispose(GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" GearsES2.dispose ... ");
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {            
            final Window window = (Window) upstreamWidget;
            window.removeMouseListener(gearsMouse);
            window.removeKeyListener(gearsKeys);
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
        
        System.err.println(Thread.currentThread()+" GearsES2.dispose FIN");
    }

    public void display(GLAutoDrawable drawable) {
        GLAnimatorControl anim = drawable.getAnimator();
        if( verbose && ( null == anim || !anim.isAnimating() ) ) {
            System.err.println(Thread.currentThread()+" GearsES2.display "+drawable.getWidth()+"x"+drawable.getHeight()+", swapInterval "+swapInterval+", drawable 0x"+Long.toHexString(drawable.getHandle()));
        }
        // Turn the gears' teeth
        if(doRotate) {
            angle += 2.0f;
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
        
        if( clearBuffers ) {
            if( null != clearColor ) {
              gl.glClearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3]);
            } else if( ignoreFocus || hasFocus ) {
              gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            } else {
              gl.glClearColor(0.2f, 0.2f, 0.2f, 0.0f);
            }
            // Special handling for the case where the GLJPanel is translucent
            // and wants to be composited with other Java 2D content
            if (GLProfile.isAWTAvailable() && 
                (drawable instanceof javax.media.opengl.awt.GLJPanel) &&
                !((javax.media.opengl.awt.GLJPanel) drawable).isOpaque() &&
                ((javax.media.opengl.awt.GLJPanel) drawable).shouldPreserveColorBufferIfTranslucent()) {
              gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
            } else {
              gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
            }
        }        
        if( !gl.hasGLSL() ) {
            return;
        }

        gl.glEnable(GL.GL_CULL_FACE);
        
        st.useProgram(gl, true);
        pmvMatrix.glPushMatrix();
        pmvMatrix.glTranslatef(panX, panY, panZ);
        pmvMatrix.glRotatef(view_rotx, 1.0f, 0.0f, 0.0f);
        pmvMatrix.glRotatef(view_roty, 0.0f, 1.0f, 0.0f);
        pmvMatrix.glRotatef(view_rotz, 0.0f, 0.0f, 1.0f);

        gear1.draw(gl, -3.0f, -2.0f,  1f * angle -    0f);
        gear2.draw(gl,  3.1f, -2.0f, -2f * angle -  9.0f);
        gear3.draw(gl, -3.1f,  4.2f, -2f * angle - 25.0f);    
        pmvMatrix.glPopMatrix();
        st.useProgram(gl, false);
        
        gl.glDisable(GL.GL_CULL_FACE);
    }
    
    boolean confinedFixedCenter = false;
    
    public void setConfinedFixedCenter(boolean v) {
        confinedFixedCenter = v;
    }
    
    class GearsKeyAdapter extends KeyAdapter {      
        public void keyPressed(KeyEvent e) {
            int kc = e.getKeyCode();
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

    interface GestureHandler {
        /** Returns true if within the gesture */ 
        public boolean isWithinGesture();
        /** Returns true if within the gesture */
        public boolean onReleased(MouseEvent e);
        /** Returns true if within the gesture */
        public boolean onDragged(MouseEvent e);
    }
    final GestureHandler gesture2PtrZoom = new GestureHandler() {
        private int zoomLastYDist;
        private boolean zoomFirstTouch = true;
        private boolean zoomMode = false;
        
        @Override
        public boolean isWithinGesture() {
            return zoomMode;
        }

        @Override
        public boolean onReleased(MouseEvent e) {
            if( zoomMode && e.getPointerCount()==1 ) {
                zoomFirstTouch = true;
                zoomMode = false;
                System.err.println("panZ.X: "+e);
            }
            return zoomMode;
        }

        @Override
        public boolean onDragged(MouseEvent e) {
            if( e.getPointerCount() >=2 ) {
                // 2 pointers zoom .. [ -15 .. 15 ], range 30
                /** 
                // Simple 1:1 Zoom: finger-distance to screen-coord
                final int dy = Math.abs(e.getY(0)-e.getY(1));
                float scale =  (float)dy / (float)drawableHeight;
                panZ = 30f * scale - 15f; 
                System.err.println("panZ: scale "+scale+" ["+dy+"/"+drawableHeight+"] -> "+panZ);
                 */
                // Diff. 1:1 Zoom: finger-distance to screen-coord
                if(zoomFirstTouch) {
                    zoomLastYDist = Math.abs(e.getY(0)-e.getY(1));
                    zoomFirstTouch=false;
                    zoomMode = true;
                    System.err.println("panZ: 1st pinch "+zoomLastYDist+", "+e);
                } else if( zoomMode ) {
                    final int dy = Math.abs(e.getY(0)-e.getY(1));
                    final int ddy = dy - zoomLastYDist;
                    
                    final float incr =  ( (float)ddy / (float)drawableHeight ) * 15.0f;                    
                    panZ += incr; 
                    if( e.getPointerCount() > 2 ) {
                        panZ += incr;
                    }
                    System.err.println("panZ.1: ddy "+ddy+", incr "+incr+" ["+dy+"/"+drawableHeight+"], dblZoom "+(e.getPointerCount() > 2)+" -> "+panZ);
                    
                    zoomLastYDist = dy;
                }
            }
            return zoomMode;
        }        
    };
    
    class GearsMouseAdapter implements MouseListener{
        private int prevMouseX, prevMouseY;
        
        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseWheelMoved(MouseEvent e) {
            float[] rot = e.getRotation();
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
        
        public void mousePressed(MouseEvent e) {
            if( !gesture2PtrZoom.isWithinGesture() ) {
                if( e.getPointerCount()==1 ) {
                    prevMouseX = e.getX();
                    prevMouseY = e.getY();
                } else if( e.getPointerCount() == 4 ) {
                    final Object src = e.getSource();
                    if( e.getPressure(true) > 0.7f && src instanceof Window) { // show Keyboard
                       ((Window) src).setKeyboardVisible(true);
                    }
                }
            }
        }

        public void mouseReleased(MouseEvent e) {
            gesture2PtrZoom.onReleased(e);
        }

        public void mouseMoved(MouseEvent e) {
            if( !gesture2PtrZoom.isWithinGesture() && e.getPointerCount()==1 ) {
                if( e.isConfined() ) {
                    navigate(e);
                } else {
                    // track prev. position so we don't have 'jumps'
                    // in case we move to confined navigation.
                    prevMouseX = e.getX();
                    prevMouseY = e.getY();
                }
            }
        }
        
        public void mouseDragged(MouseEvent e) {
            if( !gesture2PtrZoom.onDragged(e) && e.getPointerCount()==1 ) {
                navigate(e);
            }
        }
        
        private void navigate(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            
            int width, height;
            Object source = e.getSource();
            Window window = null;
            if(source instanceof Window) {
                window = (Window) source;
                width=window.getWidth();
                height=window.getHeight();
            } else if (source instanceof GLAutoDrawable) {
                GLAutoDrawable glad = (GLAutoDrawable) source;
                width = glad.getWidth();
                height = glad.getHeight();
            } else if (GLProfile.isAWTAvailable() && source instanceof java.awt.Component) {
                java.awt.Component comp = (java.awt.Component) source;
                width=comp.getWidth();
                height=comp.getHeight();
            } else {
                throw new RuntimeException("Event source neither Window nor Component: "+source);
            }           
            final float thetaY = 360.0f * ( (float)(x-prevMouseX)/(float)width);
            final float thetaX = 360.0f * ( (float)(prevMouseY-y)/(float)height);
            view_rotx += thetaX;
            view_roty += thetaY;
            if(e.isConfined() && confinedFixedCenter && null!=window) {                
                x=window.getWidth()/2;
                y=window.getHeight()/2;
                window.warpPointer(x, y);
            }
            prevMouseX = x;
            prevMouseY = y;
            // System.err.println("rotXY.1: "+view_rotx+"/"+view_roty+", source "+e);
        }
    }
}
