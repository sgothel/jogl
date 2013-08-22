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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLES2;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLUniformData;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureSequence;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

/**
 * Simple planar movie player w/ orthogonal 1:1 projection. 
 */
public class MovieSimple implements GLEventListener, GLMediaEventListener {
    public static final int EFFECT_NORMAL                  =    0;
    public static final int EFFECT_GRADIENT_BOTTOM2TOP     = 1<<1;
    public static final int EFFECT_TRANSPARENT             = 1<<3; 

    private static boolean waitForKey = false;
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
    private int swapInterval = 1;

    private GLMediaPlayer mPlayer;
    private boolean mPlayerExternal;
    private boolean mPlayerShared;
    private boolean mPlayerScaleOrig;
    private GLArrayDataServer interleavedVBO;

    private ShaderState st;
    private PMVMatrix pmvMatrix;
    private GLUniformData pmvMatrixUniform;
    private static final String shaderBasename = "texsequence_xxx";
    private static final String myTextureLookupName = "myTexture2D";
    
    /** Blender's Big Buck Bunny Trailer: 24f 640p VP8, Vorbis 44100Hz mono, WebM/Matroska Stream. */
    public static final URI defURI;
    static {
        URI _defURI = null;
        try {
            _defURI = new URI("http://video.webmfiles.org/big-buck-bunny_trailer.webm");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        defURI = _defURI;
    }
    
    private final MouseListener mouseAction = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            if(e.getY()<=winHeight/2 && null!=mPlayer && 1 == e.getClickCount()) {
                if(GLMediaPlayer.State.Playing == mPlayer.getState()) {
                    mPlayer.pause();
                } else {
                    mPlayer.play();
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
                mPlayer.seek(mPlayer.getVideoPTS() + (int) (mPlayer.getDuration() * dp));                
            } else {
                mPlayer.play();
                rotate = 1;                
                zoom = zoom1;
            }
            
            prevMouseX = x;
            // prevMouseY = y;
        }
        public void mouseWheelMoved(MouseEvent e) {
            if( !e.isShiftDown() ) {
                zoom += e.getRotation()[1]/10f; // vertical: wheel
                System.err.println("zoom: "+zoom);
            }
        }
    };
    
    private final KeyListener keyAction = new KeyAdapter() {
        public void keyReleased(KeyEvent e)  {
            if( e.isAutoRepeat() ) {
                return;
            }            
            System.err.println("MC "+e);
            int pts0 = mPlayer.getVideoPTS();
            int pts1 = 0;
            switch(e.getKeyCode()) {
                case KeyEvent.VK_RIGHT:      pts1 = pts0 +  1000; break;
                case KeyEvent.VK_UP:         pts1 = pts0 + 10000; break;
                case KeyEvent.VK_PAGE_UP:    pts1 = pts0 + 30000; break;
                case KeyEvent.VK_LEFT:       pts1 = pts0 -  1000; break;
                case KeyEvent.VK_DOWN:       pts1 = pts0 - 10000; break;
                case KeyEvent.VK_PAGE_DOWN:  pts1 = pts0 - 30000; break;
                case KeyEvent.VK_ESCAPE:
                case KeyEvent.VK_HOME:
                case KeyEvent.VK_BACK_SPACE: {
                    mPlayer.seek(0);
                    break;
                }
                case KeyEvent.VK_SPACE: {
                    if(GLMediaPlayer.State.Paused == mPlayer.getState()) {
                        mPlayer.play();
                    } else {
                        mPlayer.pause();
                    }
                    break;
                }
                case KeyEvent.VK_MULTIPLY:
                      mPlayer.setPlaySpeed(1.0f); 
                      break;
                case KeyEvent.VK_SUBTRACT: {
                      float playSpeed = mPlayer.getPlaySpeed();
                      if( e.isShiftDown() ) {
                          playSpeed /= 2.0f;
                      } else {
                          playSpeed -= 0.1f;
                      }
                      mPlayer.setPlaySpeed(playSpeed); 
                    } break;
                case KeyEvent.VK_ADD: {
                      float playSpeed = mPlayer.getPlaySpeed();
                      if( e.isShiftDown() ) {
                          playSpeed *= 2.0f;
                      } else {
                          playSpeed += 0.1f;
                      }
                      mPlayer.setPlaySpeed(playSpeed); 
                    } break;
            }
            
            if( 0 != pts1 ) {
                mPlayer.seek(pts1);
            }
        }        
    };
    
    public MovieSimple() {
        mPlayerScaleOrig = false;
        mPlayerShared = false;
        mPlayerExternal = false;
        mPlayer = GLMediaPlayerFactory.createDefault();
        System.out.println("pC.1a "+mPlayer);
    }

    public MovieSimple(GLMediaPlayer sharedMediaPlayer) throws IllegalStateException {
        mPlayerScaleOrig = false;
        mPlayerShared = true;
        mPlayerExternal = true;
        mPlayer = sharedMediaPlayer;
        mPlayer.addEventListener(this);
        System.out.println("pC.2 shared "+mPlayerShared+", "+mPlayer);
    }
    
    public void initStream(URI streamLoc, int vid, int aid, int textureCount) {
        mPlayer.addEventListener(this);
        mPlayer.initStream(streamLoc, vid, aid, textureCount);
        System.out.println("pC.1b "+mPlayer);
    }
    
    public void setSwapInterval(int v) { this.swapInterval = v; }
    
    public GLMediaPlayer getGLMediaPlayer() { return mPlayer; }
    
    public void setScaleOrig(boolean v) {
        mPlayerScaleOrig = v;
    }
    
    /** defaults to true */
    public void setOrthoProjection(boolean v) { orthoProjection=v; }
    public boolean getOrthoProjection() { return orthoProjection; }
    
    public boolean hasEffect(int e) { return 0 != ( effects & e ) ; }
    public void setEffects(int e) { effects = e; };
    public void setTransparency(float alpha) {
        this.effects |= EFFECT_TRANSPARENT;
        this.alpha = alpha;
    }    

    @Override
    public void attributesChanged(GLMediaPlayer mp, int event_mask, long when) {
        System.out.println("attributesChanges: "+mp+", 0x"+Integer.toHexString(event_mask)+", when "+when);        
    }

    @Override
    public void newFrameAvailable(GLMediaPlayer mp, TextureFrame newFrame, long when) {
        // System.out.println("newFrameAvailable: "+mp+", when "+when);        
    }

    private void initShader(GL2ES2 gl) {
        // Create & Compile the shader objects
        ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, MovieSimple.class, 
                                            "../shader", "../shader/bin", shaderBasename, true);
        ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, MovieSimple.class, 
                                            "../shader", "../shader/bin", shaderBasename, true);
        rsVp.defaultShaderCustomization(gl, true, true);
        int rsFpPos = rsFp.addGLSLVersion(gl);

        rsFpPos = rsFp.insertShaderSource(0, rsFpPos, mPlayer.getRequiredExtensionsShaderStub());
        rsFpPos = rsFp.addDefaultShaderPrecision(gl, rsFpPos);
        
        final String texLookupFuncName = mPlayer.getTextureLookupFunctionName(myTextureLookupName);        
        rsFp.replaceInShaderSource(myTextureLookupName, texLookupFuncName);
        
        // Inject TextureSequence shader details
        final StringBuilder sFpIns = new StringBuilder();
        sFpIns.append("uniform ").append(mPlayer.getTextureSampler2DType()).append(" mgl_ActiveTexture;\n");
        sFpIns.append(mPlayer.getTextureLookupFragmentShaderImpl());
        rsFp.insertShaderSource(0, "TEXTURE-SEQUENCE-CODE-BEGIN", 0, sFpIns);

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

    @Override
    public void init(GLAutoDrawable drawable) {
        if(null == mPlayer) { 
            throw new InternalError("mPlayer null");
        }
        if( GLMediaPlayer.State.Initialized != mPlayer.getState() ) {
            throw new IllegalStateException("mPlayer not in state initialized: "+mPlayer);
        }
        if( GLMediaPlayer.STREAM_ID_NONE == mPlayer.getVID() ) {
            System.err.println("MovieSimple: No VID/stream selected - no GL: "+mPlayer);
            return;
        }
        zoom0 =  orthoProjection ? 0f : -2.5f;
        zoom1 = orthoProjection ? 0f : -5f;
        zoom = zoom0;        

        GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.err.println(JoglVersion.getGLInfo(gl, null));
        System.err.println("Alpha: "+alpha+", opaque "+drawable.getChosenGLCapabilities().isBackgroundOpaque()+
                           ", "+drawable.getClass().getName()+", "+drawable);
        
        if(waitForKey) {
            UITestCase.waitForKey("Init>");
        }        
        final Texture tex;
        boolean useExternalTexture = false;
        try {
            System.out.println("p0 "+mPlayer+", shared "+mPlayerShared);
            if(!mPlayerShared) {
                mPlayer.initGL(gl);
            }
            tex = mPlayer.getLastTexture().getTexture();
            System.out.println("p1 "+mPlayer+", shared "+mPlayerShared);
            useExternalTexture = GLES2.GL_TEXTURE_EXTERNAL_OES == tex.getTarget();
            if(useExternalTexture && !gl.isExtensionAvailable("GL_OES_EGL_image_external")) {
                throw new GLException("GL_OES_EGL_image_external requested but not available");
            }
            if(!mPlayerShared) {
                mPlayer.setTextureMinMagFilter( new int[] { GL.GL_NEAREST, GL.GL_LINEAR } );
            }
        } catch (Exception glex) {
            glex.printStackTrace();
            if(!mPlayerShared && null != mPlayer) {
                mPlayer.destroy(gl);
                mPlayer = null;
            }
            throw new GLException(glex);
        }
        
        initShader(gl);

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
        System.err.println("XXX0: mov aspect: "+mAspect);
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

        interleavedVBO = GLArrayDataServer.createGLSLInterleaved(3+4+2, GL.GL_FLOAT, false, 3*4, GL.GL_STATIC_DRAW);
        {        
            interleavedVBO.addGLSLSubArray("mgl_Vertex",        3, GL.GL_ARRAY_BUFFER);            
            interleavedVBO.addGLSLSubArray("mgl_Color",         4, GL.GL_ARRAY_BUFFER);            
            interleavedVBO.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);
            
            final FloatBuffer ib = (FloatBuffer)interleavedVBO.getBuffer();
            final TextureCoords tc = tex.getImageTexCoords();                                   
            final float aspect = tex.getAspectRatio();
            System.err.println("XXX0: "+tc);
            System.err.println("XXX0: tex aspect: "+aspect);
            System.err.println("XXX0: tex y-flip: "+tex.getMustFlipVertically());
            
             // left-bottom
            ib.put(verts[0]);  ib.put(verts[1]);  ib.put(verts[2]);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                ib.put( 0);    ib.put( 0);     ib.put( 0);    ib.put(alpha);
            } else {
                ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            }
            ib.put( tc.left()   *ss);  ib.put( tc.bottom() *ts);
            
             // right-bottom
            ib.put(verts[3]);  ib.put(verts[1]);  ib.put(verts[2]);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                ib.put( 0);    ib.put( 0);     ib.put( 0);    ib.put(alpha); 
            } else {
                ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            }
            ib.put( tc.right()  *ss);  ib.put( tc.bottom() *ts);

             // left-top
            ib.put(verts[0]);  ib.put(verts[4]);  ib.put(verts[2]);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            } else {
                ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            }
            ib.put( tc.left()   *ss);  ib.put( tc.top()    *ts);
            
             // right-top
            ib.put(verts[3]);  ib.put(verts[4]);  ib.put(verts[2]);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            } else {
                ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            } 
            ib.put( tc.right()  *ss);  ib.put( tc.top()    *ts);            
        }
        interleavedVBO.seal(gl, true);
        interleavedVBO.enableBuffer(gl, false);
        st.ownAttribute(interleavedVBO, true);
        gl.glClearColor(0.3f, 0.3f, 0.3f, 0.3f);
        
        gl.glEnable(GL2ES2.GL_DEPTH_TEST);

        st.useProgram(gl, false);

        // Let's show the completed shader state ..
        System.out.println("iVBO: "+interleavedVBO);
        System.out.println(st);

        if(!mPlayerShared) {
            mPlayer.play();
            System.out.println("pStart "+mPlayer);
        }        
        startTime = System.currentTimeMillis();
        
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {            
            final Window window = (Window) upstreamWidget;
            window.addMouseListener(mouseAction);
            window.addKeyListener(keyAction);
            winWidth = window.getWidth();
            winHeight = window.getHeight();
        }
    }
    
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(-1 != swapInterval) {
            gl.setSwapInterval(swapInterval); // in case switching the drawable (impl. may bound attribute there)
        }
        if(null == mPlayer) { return; }
        if( GLMediaPlayer.STREAM_ID_NONE == mPlayer.getVID() ) {
            return;
        }
        winWidth = width;
        winHeight = height;
                
        if(null != st) {
            reshapePMV(width, height);
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

    @Override
    public void dispose(GLAutoDrawable drawable) {
        if(null == mPlayer) { return; }
        
        System.out.println("pD.1 "+mPlayer);        
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        if( null != mPlayer ) {
            mPlayer.removeEventListener(this);
            if(!mPlayerExternal) {
                mPlayer.destroy(gl);
            }
        }
        System.out.println("pD.X "+mPlayer);
        mPlayer=null;
        pmvMatrixUniform = null;
        if(null != pmvMatrix) {
            pmvMatrix.destroy();
            pmvMatrix=null;
        }
        if(null != st) {
            st.destroy(gl);
            st=null;
        }
    }

    long lastPerfPos = 0;
    
    @Override
    public void display(GLAutoDrawable drawable) {
        if(null == mPlayer) { return; }
        
        final long currentPos = System.currentTimeMillis();
        if( currentPos - lastPerfPos > 2000 ) {
            System.err.println( mPlayer.getPerfString() );
            lastPerfPos = currentPos;  
        }
        
        if( GLMediaPlayer.STREAM_ID_NONE == mPlayer.getVID() ) {
            return;
        }
                
        GL2ES2 gl = drawable.getGL().getGL2ES2();        

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        if(null == st) {
            return;
        }
        
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
        interleavedVBO.enableBuffer(gl, true);
        Texture tex = null; 
        if(null!=mPlayer) {
            final TextureSequence.TextureFrame texFrame;
            if(mPlayerShared) {
                texFrame=mPlayer.getLastTexture();
            } else {
                texFrame=mPlayer.getNextTexture(gl);
            }
            if(null != texFrame) {
                tex = texFrame.getTexture();
                gl.glActiveTexture(GL.GL_TEXTURE0+mPlayer.getTextureUnit());
                tex.enable(gl);
                tex.bind(gl);
            }
        }
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        if(null != tex) {
            tex.disable(gl);
        }
        interleavedVBO.enableBuffer(gl, false);
        st.useProgram(gl, false);
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        int swapInterval = 1;
        int width = 640;
        int height = 600;
        int textureCount = 3; // default - threaded
        boolean ortho = true;
        boolean zoom = false;
        
        boolean forceES2 = false;
        boolean forceES3 = false;
        boolean forceGL3 = false;
        boolean forceGLDef = false;
        int vid = GLMediaPlayer.STREAM_ID_AUTO;
        int aid = GLMediaPlayer.STREAM_ID_AUTO;
        final boolean origSize;
        
        String url_s=null;
        {
            boolean _origSize = false;        
            for(int i=0; i<args.length; i++) {
                if(args[i].equals("-vid")) {
                    i++;
                    vid = MiscUtils.atoi(args[i], vid);
                } else if(args[i].equals("-aid")) {
                    i++;
                    aid = MiscUtils.atoi(args[i], aid);
                } else if(args[i].equals("-width")) {
                    i++;
                    width = MiscUtils.atoi(args[i], width);
                } else if(args[i].equals("-height")) {
                    i++;
                    height = MiscUtils.atoi(args[i], height);
                } else if(args[i].equals("-osize")) {
                    _origSize = true;
                } else if(args[i].equals("-textureCount")) {
                    i++;
                    textureCount = MiscUtils.atoi(args[i], textureCount);
                } else if(args[i].equals("-es2")) {
                    forceES2 = true;
                } else if(args[i].equals("-es3")) {
                    forceES3 = true;
                } else if(args[i].equals("-gl3")) {
                    forceGL3 = true;
                } else if(args[i].equals("-gldef")) {
                    forceGLDef = true;
                } else if(args[i].equals("-vsync")) {
                    i++;
                    swapInterval = MiscUtils.atoi(args[i], swapInterval);
                } else if(args[i].equals("-projection")) {
                    ortho=false;
                } else if(args[i].equals("-zoom")) {
                    zoom=true;
                } else if(args[i].equals("-url")) {
                    i++;
                    url_s = args[i];
                } else if(args[i].equals("-wait")) {
                    waitForKey = true;
                }
            }
            origSize = _origSize;
        }
        final URI streamLoc;
        if( null == url_s ) {
            streamLoc = defURI;
        } else {
            streamLoc = new URI(url_s);
        }
        System.err.println("stream "+streamLoc);
        System.err.println("vid "+vid+", aid "+aid);
        System.err.println("textureCount "+textureCount);
        System.err.println("forceES2   "+forceES2);
        System.err.println("forceES3   "+forceES3);
        System.err.println("forceGL3   "+forceGL3);
        System.err.println("forceGLDef "+forceGLDef);
        System.err.println("swapInterval "+swapInterval);
        
        final MovieSimple ms = new MovieSimple();
        ms.setSwapInterval(swapInterval);
        ms.setScaleOrig(!zoom);
        ms.setOrthoProjection(ortho);
        
        try {
            final GLProfile glp;
            if(forceGLDef) {
                glp = GLProfile.getDefault();
            } else if(forceGL3) {
                glp = GLProfile.get(GLProfile.GL3);
            } else if(forceES3) {
                glp = GLProfile.get(GLProfile.GLES3);
            } else if(forceES2) {
                glp = GLProfile.get(GLProfile.GLES2);
            } else {
                glp = GLProfile.getGL2ES2();
            }        
            System.err.println("GLProfile: "+glp);
            GLCapabilities caps = new GLCapabilities(glp);
            final GLWindow window = GLWindow.create(caps);            
            final Animator anim = new Animator(window);
            window.addWindowListener(new WindowAdapter() {
                public void windowDestroyed(WindowEvent e) {
                    anim.stop();
                }                
            });
            window.setSize(width, height);
            window.setVisible(true);
            anim.start();

            ms.mPlayer.addEventListener(new GLMediaEventListener() {
                @Override
                public void newFrameAvailable(GLMediaPlayer ts, TextureFrame newFrame, long when) {
                }
    
                @Override
                public void attributesChanged(final GLMediaPlayer mp, int event_mask, long when) {
                    System.err.println("Player AttributesChanges: events_mask 0x"+Integer.toHexString(event_mask)+", when "+when);
                    System.err.println("Player State: "+mp);
                    if( 0 != ( GLMediaEventListener.EVENT_CHANGE_SIZE & event_mask ) && origSize ) {
                        window.setSize(mp.getWidth(), mp.getHeight());
                    }
                    if( 0 != ( GLMediaEventListener.EVENT_CHANGE_INIT & event_mask ) ) {
                        window.addGLEventListener(ms);
                        anim.setUpdateFPSFrames(60, System.err);
                        anim.resetFPSCounter();
                    }
                    if( 0 != ( ( GLMediaEventListener.EVENT_CHANGE_ERR | GLMediaEventListener.EVENT_CHANGE_EOS ) & event_mask ) ) {
                        final StreamException se = ms.mPlayer.getStreamException();
                        if( null != se ) {
                            se.printStackTrace();                        
                        }
                        new Thread() {
                            public void run() {
                                window.destroy();
                            } }.start();
                    }
                }
            });
            ms.initStream(streamLoc, vid, aid, textureCount);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
