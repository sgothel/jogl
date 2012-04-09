/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.demos.es2.av;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLArrayData;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLES2;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.av.GLMediaPlayer;
import com.jogamp.opengl.av.GLMediaEventListener;
import com.jogamp.opengl.av.GLMediaPlayerFactory;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;

public class MovieSimple implements GLEventListener, GLMediaEventListener {
    private int winWidth, winHeight;
    private int prevMouseX; // , prevMouseY;
    private int rotate = 0;
    private boolean  orthoProjection = true;
    private float nearPlaneNormalized;
    private float zoom0;
    private float zoom1;
    private float zoom;
    private long startTime;
    private int effects = EFFECT_NORMAL;
    private float alpha = 1.0f;

    public static final int EFFECT_NORMAL                  =    0;
    public static final int EFFECT_GRADIENT_BOTTOM2TOP     = 1<<1;
    public static final int EFFECT_TRANSPARENT             = 1<<3; 

    /** defaults to true */
    public void setOrthoProjection(boolean v) { orthoProjection=v; }
    public boolean getOrthoProjection() { return orthoProjection; }
    
    public boolean hasEffect(int e) { return 0 != ( effects & e ) ; }
    public void setEffects(int e) { effects = e; };
    public void setTransparency(float alpha) {
        this.effects |= EFFECT_TRANSPARENT;
        this.alpha = alpha;
    }
    
    private final MouseListener mouseAction = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            if(e.getY()<=winHeight/2 && null!=mPlayer && 1 == e.getClickCount()) {
                if(GLMediaPlayer.State.Playing == mPlayer.getState()) {
                    mPlayer.pause();
                } else {
                    mPlayer.start();
                }
            }
        }
        public void mouseReleased(MouseEvent e) {
            if(e.getY()<=winHeight/2) {
                rotate = -1;
                zoom = zoom0;
                System.err.println("zoom: "+zoom);
            }
        }
        public void mouseMoved(MouseEvent e) {
            prevMouseX = e.getX();
            // prevMouseY = e.getY();
        }
        public void mouseDragged(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            
            if(y>winHeight/2) {
                final float dp  = (float)(x-prevMouseX)/(float)winWidth;
                mPlayer.seek(mPlayer.getCurrentPosition() + (long) (mPlayer.getDuration() * dp));                
            } else {
                mPlayer.start();
                rotate = 1;                
                zoom = zoom1;
            }
            
            prevMouseX = x;
            // prevMouseY = y;
        }
        public void mouseWheelMoved(MouseEvent e) {
            int r = e.getWheelRotation();
            if(r>0) {
                zoom += 0.1;
            } else if(r<0) {
                zoom -= 0.1;
            }
            System.err.println("zoom: "+zoom);
        }
    };

    GLMediaPlayer mPlayer;
    URLConnection stream = null;
    boolean mPlayerExternal;
    boolean mPlayerShared;
    boolean mPlayerScaleOrig;

    public MovieSimple(URLConnection stream) throws IOException {
        mPlayerScaleOrig = false;
        mPlayerShared = false;
        mPlayerExternal = false;
        mPlayer = GLMediaPlayerFactory.create();
        mPlayer.addEventListener(this);
        this.stream = stream;
        System.out.println("pC.1 "+mPlayer);
    }

    public MovieSimple(GLMediaPlayer sharedMediaPlayer) throws IllegalStateException {
        mPlayerScaleOrig = false;
        mPlayerShared = true;
        mPlayerExternal = true;
        mPlayer = sharedMediaPlayer;
        mPlayer.addEventListener(this);
        this.stream = null;
        System.out.println("pC.2 shared "+mPlayerShared+", "+mPlayer);
    }
    
    public GLMediaPlayer getGLMediaPlayer() { return mPlayer; }
    
    public void setScaleOrig(boolean v) {
        mPlayerScaleOrig = v;
    }
    
    @Override
    public void attributesChanges(GLMediaPlayer mp, int event_mask, long when) {
        System.out.println("attributesChanges: "+mp+", 0x"+Integer.toHexString(event_mask)+", when "+when);        
    }

    @Override
    public void newFrameAvailable(GLMediaPlayer mp, long when) {
        // System.out.println("newFrameAvailable: "+mp+", when "+when);
    }

    public void start() {
        if(null!=mPlayer) {
            mPlayer.start();
            System.out.println("pStart "+mPlayer);
        }        
    }

    public void stop() {
        if(null!=mPlayer) {
            mPlayer.stop();
            System.out.println("pStop "+mPlayer);
        }        
    }
    
    ShaderState st;
    PMVMatrix pmvMatrix;
    GLUniformData pmvMatrixUniform;
    
    private void initShader(GL2ES2 gl, boolean useExternalTexture) {
        // Create & Compile the shader objects
        final String vShaderBasename = gl.isGLES2() ? "moviesimple_es2" : "moviesimple_gl2" ;
        final String fShaderBasename = gl.isGLES2() ? ( useExternalTexture ? "moviesimple_es2_exttex" : "moviesimple_es2" ) : "moviesimple_gl2";
        
        ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, MovieSimple.class,
                                            "../shader", "../shader/bin", vShaderBasename);
        ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, MovieSimple.class,
                                            "../shader", "../shader/bin", fShaderBasename);

        // Create & Link the shader program
        ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);
        if(!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: "+sp);
        }

        // Let's manage all our states using ShaderState.
        st = new ShaderState();
        st.attachShaderProgram(gl, sp, false);
    }

    public void init(GLAutoDrawable drawable) {
        zoom0 =  orthoProjection ? 0f : -2.5f;
        zoom1 = orthoProjection ? 0f : -5f;
        zoom = zoom0;        

        GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.err.println(JoglVersion.getGLInfo(gl, null));
        System.err.println("Alpha: "+alpha+", opaque "+drawable.getChosenGLCapabilities().isBackgroundOpaque()+
                           ", "+drawable.getClass().getName()+", "+drawable);
        
        gl.glFinish();

        boolean useExternalTexture = false;
        try {
            System.out.println("p0 "+mPlayer+", shared "+mPlayerShared);
            if(!mPlayerShared) {
                mPlayer.initGLStream(gl, stream);
            }
            System.out.println("p1 "+mPlayer+", shared "+mPlayerShared);
            useExternalTexture = GLES2.GL_TEXTURE_EXTERNAL_OES == mPlayer.getTextureTarget();
            if(useExternalTexture && !gl.isExtensionAvailable("GL_OES_EGL_image_external")) {
                throw new GLException("GL_OES_EGL_image_external requested but not available");
            }
            if(!mPlayerShared) {
                mPlayer.setTextureMinMagFilter( new int[] { GL.GL_NEAREST, GL.GL_LINEAR } );
            }
        } catch (Exception glex) { 
            if(!mPlayerShared && null != mPlayer) {
                mPlayer.destroy(gl);
                mPlayer = null;
            }
            throw new GLException(glex);
        }
        
        initShader(gl, useExternalTexture);

        // Push the 1st uniform down the path 
        st.useProgram(gl, true);

        int[] viewPort = new int[] { 0, 0, drawable.getWidth(), drawable.getHeight()};
        pmvMatrix = new PMVMatrix();
        reshapePMV(viewPort[2], viewPort[3]);        
        pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        if(!st.uniform(gl, pmvMatrixUniform)) {
            throw new GLException("Error setting PMVMatrix in shader: "+st);
        }
        if(!st.uniform(gl, new GLUniformData("mgl_ActiveTexture", mPlayer.getTextureUnit()))) {
            throw new GLException("Error setting mgl_ActiveTexture in shader: "+st);
        }
        
        float dWidth = drawable.getWidth();
        float dHeight = drawable.getHeight();
        float mWidth = mPlayer.getWidth();
        float mHeight = mPlayer.getHeight();        
        float mAspect = mWidth/mHeight;
        float[] verts;
        float xs, ys;
        if(orthoProjection) {
            if(mPlayerScaleOrig && mWidth < dWidth && mHeight < dHeight) {
                xs   = mWidth/2f;                ys   = xs / mAspect;                                
            } else {
                xs   = dWidth/2f;                ys   = xs / mAspect; // w>h
            }
        } else {
            if(mPlayerScaleOrig && mWidth < dWidth && mHeight < dHeight) {
                xs   = mAspect * ( mWidth / dWidth ) ; ys   =  xs / mAspect ;
            } else {
                xs   = mAspect; ys   = 1f; // b>h
            }
        }
        verts = new float[] { -1f*xs, -1f*ys, 0f, // LB
                               1f*xs,  1f*ys, 0f  // RT
                            };
        {
            System.err.println("XXX0: pixel  LB: "+verts[0]+", "+verts[1]+", "+verts[2]);
            System.err.println("XXX0: pixel  RT: "+verts[3]+", "+verts[4]+", "+verts[5]);
            float[] winLB = new float[3];
            float[] winRT = new float[3];
            pmvMatrix.gluProject(verts[0], verts[1], verts[2], viewPort, 0, winLB, 0);
            pmvMatrix.gluProject(verts[3], verts[4], verts[5], viewPort, 0, winRT, 0);
            System.err.println("XXX0: win   LB: "+winLB[0]+", "+winLB[1]+", "+winLB[2]);
            System.err.println("XXX0: win   RT: "+winRT[0]+", "+winRT[1]+", "+winRT[2]);
        }
        final float ss = 1f, ts = 1f; // scale tex-coord

        final GLArrayDataServer interleaved = GLArrayDataServer.createGLSLInterleaved(9, GL.GL_FLOAT, false, 12, GL.GL_STATIC_DRAW);
        {        
            GLArrayData vertices = interleaved.addGLSLSubArray("mgl_Vertex", 3, GL.GL_ARRAY_BUFFER);
            FloatBuffer verticeb = (FloatBuffer)vertices.getBuffer();
            
            GLArrayData texcoord = interleaved.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);
            TextureCoords tc = mPlayer.getTextureCoords();
            FloatBuffer texcoordb = (FloatBuffer)texcoord.getBuffer();
            
            GLArrayData colors = interleaved.addGLSLSubArray("mgl_Color",  4, GL.GL_ARRAY_BUFFER);
            FloatBuffer colorb = (FloatBuffer)colors.getBuffer();
                        
             // left-bottom
            verticeb.put(verts[0]);  verticeb.put(verts[1]);  verticeb.put(verts[2]);
            texcoordb.put( tc.left()   *ss);  texcoordb.put( tc.bottom() *ts);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                colorb.put( 0);    colorb.put( 0);     colorb.put( 0);    colorb.put(alpha);
            } else {
                colorb.put( 1);    colorb.put( 1);     colorb.put( 1);    colorb.put(alpha);
            }
            
             // right-bottom
            verticeb.put(verts[3]);  verticeb.put(verts[1]);  verticeb.put(verts[2]);
            texcoordb.put( tc.right()  *ss);  texcoordb.put( tc.bottom() *ts);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                colorb.put( 0);    colorb.put( 0);     colorb.put( 0);    colorb.put(alpha); 
            } else {
                colorb.put( 1);    colorb.put( 1);     colorb.put( 1);    colorb.put(alpha);
            }

             // left-top
            verticeb.put(verts[0]);  verticeb.put(verts[4]);  verticeb.put(verts[2]);
            texcoordb.put( tc.left()   *ss);  texcoordb.put( tc.top()    *ts);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                colorb.put( 1);    colorb.put( 1);     colorb.put( 1);    colorb.put(alpha);
            } else {
                colorb.put( 1);    colorb.put( 1);     colorb.put( 1);    colorb.put(alpha);
            }
            
             // right-top
            verticeb.put(verts[3]);  verticeb.put(verts[4]);  verticeb.put(verts[2]);
            texcoordb.put( tc.right()  *ss);  texcoordb.put( tc.top()    *ts);            
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                colorb.put( 1);    colorb.put( 1);     colorb.put( 1);    colorb.put(alpha);
            } else {
                colorb.put( 1);    colorb.put( 1);     colorb.put( 1);    colorb.put(alpha);
            } 
        }
        interleaved.seal(gl, true);
        
        gl.glClearColor(0.3f, 0.3f, 0.3f, 0.3f);
        
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);

        st.useProgram(gl, false);

        // Let's show the completed shader state ..
        System.out.println(st);

        if(null!=mPlayer) {
            start();
            System.out.println("p2 "+mPlayer);
        }
        
        startTime = System.currentTimeMillis();
        
        if (drawable instanceof Window) {
            Window window = (Window) drawable;
            window.addMouseListener(mouseAction);
            winWidth = window.getWidth();
            winHeight = window.getHeight();
        }
    }
    
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        if(null == mPlayer) { return; }
        winWidth = width;
        winHeight = height;
                
        if(null != st) {
            reshapePMV(width, height);
            GL2ES2 gl = drawable.getGL().getGL2ES2();
            st.useProgram(gl, true);
            st.uniform(gl, pmvMatrixUniform);
            st.useProgram(gl, false);
        }
        
        System.out.println("pR "+mPlayer);
    }
    
    private void reshapePMV(int width, int height) {
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        if(orthoProjection) {
            final float fw = (float) width / 2f;
            final float fh = (float) height/ 2f;
            pmvMatrix.glOrthof(-fw, fw, -fh, fh, -1.0f, 1.0f);
            nearPlaneNormalized = 0f;
        } else {
            pmvMatrix.gluPerspective(45.0f, (float)width / (float)height, 1f, 10.0f);
            nearPlaneNormalized = 1f/(10f-1f);
        }
        System.err.println("XXX0: Perspective nearPlaneNormalized: "+nearPlaneNormalized);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, zoom0);        
    }

    public void dispose(GLAutoDrawable drawable) {
        if(null == mPlayer) { return; }
        
        stop();
        System.out.println("pD.1 "+mPlayer);
        
        GL2ES2 gl = drawable.getGL().getGL2ES2();

        mPlayer.removeEventListener(this);
        if(!mPlayerExternal) {
            mPlayer.destroy(gl);
        }
        System.out.println("pD.X "+mPlayer);
        mPlayer=null;
        pmvMatrixUniform = null;
        pmvMatrix.destroy();
        pmvMatrix=null;
        st.destroy(gl);
        st=null;
    }

    public void display(GLAutoDrawable drawable) {
        if(null == mPlayer) { return; }
        
        GL2ES2 gl = drawable.getGL().getGL2ES2();        

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        st.useProgram(gl, true);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, zoom);
        if(rotate > 0) {
            final float ang = ((float) (System.currentTimeMillis() - startTime) * 360.0f) / 8000.0f;
            pmvMatrix.glRotatef(ang, 0, 0, 1);
        } else {
            rotate = 0;
        }
        st.uniform(gl, pmvMatrixUniform);

        if(null!=mPlayer) {
            final GLMediaPlayer.TextureFrame texFrame;
            if(mPlayerShared) {
                texFrame=mPlayer.getLastTexture();
            } else {
                texFrame=mPlayer.getNextTexture(gl, true);
            }
            if(null != texFrame) {
                final Texture tex = texFrame.getTexture();
                gl.glActiveTexture(GL.GL_TEXTURE0+mPlayer.getTextureUnit());
                tex.enable(gl);
                tex.bind(gl);
                gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
                tex.disable(gl);                    
            }
        }

        st.useProgram(gl, false);
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

    public static void main(String[] args) throws IOException, MalformedURLException {
        int w = 640;
        int h = 480;
        boolean ortho = true;
        boolean zoom = false;
        
        String url_s="file:///mnt/sdcard/Movies/BigBuckBunny_320x180.mp4";        
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-width")) {
                i++;
                w = MiscUtils.atoi(args[i], w);
            } else if(args[i].equals("-height")) {
                i++;
                h = MiscUtils.atoi(args[i], h);
            } else if(args[i].equals("-projection")) {
                ortho=false;
            } else if(args[i].equals("-zoom")) {
                zoom=true;
            } else if(args[i].equals("-url")) {
                i++;
                url_s = args[i];
            }
        }
        MovieSimple ms = new MovieSimple(new URL(url_s).openConnection());
        ms.setScaleOrig(!zoom);
        ms.setOrthoProjection(ortho);
        
        try {
            GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
            GLWindow window = GLWindow.create(caps);            

            window.addGLEventListener(ms);

            window.setSize(w, h);
            window.setVisible(true);
            final Animator anim = new Animator(window);
            anim.start();
            window.addWindowListener(new WindowAdapter() {
                public void windowDestroyed(WindowEvent e) {
                    anim.stop();
                }                
            });
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
