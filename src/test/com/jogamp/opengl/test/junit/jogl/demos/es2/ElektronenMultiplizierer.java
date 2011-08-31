/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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


package com.jogamp.opengl.test.junit.jogl.demos.es2;

import java.nio.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;

import com.jogamp.common.nio.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;

import static javax.media.opengl.GL2ES2.*;

/**
 **   __ __|_  ___________________________________________________________________________  ___|__ __
 **  //    /\                                           _                                  /\    \\  
 ** //____/  \__     __ _____ _____ _____ _____ _____  | |     __ _____ _____ __        __/  \____\\ 
 **  \    \  / /  __|  |     |   __|  _  |     |  _  | | |  __|  |     |   __|  |      /\ \  /    /  
 **   \____\/_/  |  |  |  |  |  |  |     | | | |   __| | | |  |  |  |  |  |  |  |__   "  \_\/____/   
 **  /\    \     |_____|_____|_____|__|__|_|_|_|__|    | | |_____|_____|_____|_____|  _  /    /\     
 ** /  \____\                       http://jogamp.org  |_|                              /____/  \    
 ** \  /   "' _________________________________________________________________________ `"   \  /    
 **  \/____.                                                                             .____\/     
 **
 ** JOGL2 port of my PC 4k intro competition entry for Revision 2011. Sure it got a little bigger 
 ** while porting but the shader and control code remained more or less untouched. The intro renders
 ** a fullscreen billboard using a single fragment shader. The shader encapsulates basically two 
 ** different routines: A sphere-tracing based raymarcher for a single fractal formula and a bitmap
 ** orbit trap julia+mandelbrot fractal renderer. Additionally an inline-processing analog-distortion
 ** filter is applied to all rendered fragments to make the overall look more interesting.
 **
 ** The different intro parts are all parameter variations of the two routines in the fragment shader 
 ** synched to the music: Parts 3+5 are obviously the mandelbrot and julia bitmap orbit traps, and parts
 ** 1,2,4 and 6 are pure fractal sphere tracing.
 **
 ** During the development of the intro it turned out that perfectly raymarching every pixel of the orbit
 ** trapped julia+mandelbrot fractal was way to slow even on highend hardware. So I inserted a lowres 
 ** intermediate FBO to be used by the bitmap based orbit trap routine wich was ofcourse way faster, but
 ** had the obvious upscaling artefacts. Maybe I'll produce a perfect quality version for very patient 
 ** people with insane hardware :)
 **
 ** Papers and articles you should be familiar with before trying to understand the code:
 **
 ** Distance rendering for fractals: http://www.iquilezles.org/www/articles/distancefractals/distancefractals.htm
 ** Geometric orbit traps: http://www.iquilezles.org/www/articles/ftrapsgeometric/ftrapsgeometric.htm
 ** Bitmap orbit traps: http://www.iquilezles.org/www/articles/ftrapsbitmap/ftrapsbitmap.htm
 ** Ambient occlusion techniques: http://www.iquilezles.org/www/articles/ao/ao.htm
 ** Sphere tracing: A geometric method for the antialiased ray tracing of implicit surfaces: http://graphics.cs.uiuc.edu/~jch/papers/zeno.pdf
 ** Rendering fractals with distance estimation function: http://www.iquilezles.org/www/articles/mandelbulb/mandelbulb.htm
 **
 ** For an impression how this routine looks like see here: http://www.youtube.com/watch?v=lvC8maVHh8Q
 ** Original release from the Revision can be found here: http://www.pouet.net/prod.php?which=56860
 **/
public class ElektronenMultiplizierer implements GLEventListener {

//BEGIN --- BaseGlobalEnvironment replacement ---

    private final GLCapabilities mCaps; 
    private final GLU mGlu;

    private String      mCommandLineParameter_BaseRoutineClassName;
    private boolean     mCommandLineParameter_MultiSampling;
    private int         mCommandLineParameter_NumberOfSampleBuffers;
    private boolean     mCommandLineParameter_AnisotropicFiltering;
    private float       mCommandLineParameter_AnisotropyLevel;
    private boolean     mCommandLineParameter_FrameCapture;
    private boolean     mCommandLineParameter_FrameSkip;
    private boolean     mUsesFullScreenMode;
    private int         mFrameCounter;
    private int         mCommandLineParameter_FrameRate;
    private long        mFrameSkipAverageFramerateTimeStart;
    private boolean     mFrameSkipAverageFrameStartTimeInitialized;
    private long        mFrameSkipAverageFramerateTimeEnd; 
    private double      mFrameCounterDifference;
    private double      mFrameCounterTargetValue;
    private int         mSkippedFramesCounter;
//    private BaseMusic mBaseMusic;
    boolean             mMusicSyncStartTimeInitialized = false;

    private ShaderState st;
    private PMVMatrix pmvMatrix;
    private GLUniformData pmvMatrixUniform;
    private GLUniformData mScreenDimensionUniform;
    private GLArrayDataServer vertices0;
    private GLArrayDataServer texCoords0;
    
    public String   getBaseRoutineClassName()       { return mCommandLineParameter_BaseRoutineClassName; }
    public boolean  preferMultiSampling()           { return mCommandLineParameter_MultiSampling; }
    public int      getNumberOfSamplingBuffers()    { return mCommandLineParameter_NumberOfSampleBuffers; }
    public boolean  preferAnisotropicFiltering()    { return mCommandLineParameter_AnisotropicFiltering; }
    public float    getAnisotropyLevel()            { return mCommandLineParameter_AnisotropyLevel; }
    public boolean  wantsFrameCapture()             { return mCommandLineParameter_FrameCapture; }
    public int      getDesiredFramerate()           { return mCommandLineParameter_FrameRate; }
    public boolean  wantsFrameSkip()                { return mCommandLineParameter_FrameSkip; }
    public boolean  usesFullScreenMode()            { return mUsesFullScreenMode; }

    public ElektronenMultiplizierer (
            String inBaseRoutineClassName,
            boolean inMultiSampling,
            int inNumberOfSampleBuffers,
            boolean inAnisotropicFiltering,
            float inAnisotropyLevel,
            boolean inFrameCapture,
            boolean inFrameSkip, int desiredFrameRate
    ) {
        mGlu = new GLU();
        mCommandLineParameter_BaseRoutineClassName = inBaseRoutineClassName;
        mCommandLineParameter_MultiSampling = inMultiSampling;
        mCommandLineParameter_NumberOfSampleBuffers = (inNumberOfSampleBuffers==-1) ? 2 : inNumberOfSampleBuffers;
        mCommandLineParameter_AnisotropicFiltering = inAnisotropicFiltering;
        mCommandLineParameter_AnisotropyLevel = (inAnisotropyLevel==-1.0f) ? 2.0f : inAnisotropyLevel;
        mCommandLineParameter_FrameCapture = inFrameCapture;
        mCommandLineParameter_FrameSkip = inFrameSkip;
        mCommandLineParameter_FrameRate = desiredFrameRate;
        mCaps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2));
        if (preferMultiSampling()) {
            // enable/configure multisampling support ...
            mCaps.setSampleBuffers(true);
            mCaps.setNumSamples(getNumberOfSamplingBuffers());
            mCaps.setAccumAlphaBits(1);
            mCaps.setAccumBlueBits(1);
            mCaps.setAccumGreenBits(1);
            mCaps.setAccumRedBits(1);
            // turns out we need to have alpha, otherwise no AA will be visible
            mCaps.setAlphaBits(1); 
        }
    }
    
    public GLCapabilitiesImmutable getGLCapabilities() {
        return mCaps;
    }

    public void init(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        gl.setSwapInterval(1);
        mFrameCounter = 0;        

        st = new ShaderState();        
        final ShaderCode vp0 = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, 1, this.getClass(),
                "shader", "shader/bin", "default");
        final ShaderCode fp0 = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, 1, this.getClass(),
           //   "shader", "shader/bin", "elektronenmultiplizierer_development");
                "shader", "shader/bin", "elektronenmultiplizierer_port");
        final ShaderProgram sp0 = new ShaderProgram();
        sp0.add(gl, vp0, System.err);
        sp0.add(gl, fp0, System.err);       
        st.attachShaderProgram(gl, sp0);
        st.useProgram(gl, true);
                
        final float XRESf = (float) drawable.getWidth();
        final float YRESf = (float) drawable.getHeight();

        mScreenDimensionUniform = new GLUniformData("resolution", 2, Buffers.newDirectFloatBuffer(2));
        final FloatBuffer mScreenDimensionV = (FloatBuffer) mScreenDimensionUniform.getBuffer();
        mScreenDimensionV.put(0, XRESf);
        mScreenDimensionV.put(1, YRESf);
        st.ownUniform(mScreenDimensionUniform);       
        st.uniform(gl, mScreenDimensionUniform);        
        
        pmvMatrix = new PMVMatrix();
        pmvMatrixUniform = new GLUniformData("gcu_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
        st.ownUniform(pmvMatrixUniform);
        st.uniform(gl, pmvMatrixUniform);
                 
        vertices0 = GLArrayDataServer.createGLSL("gca_Vertices", 2, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        vertices0.putf(0);     vertices0.putf(YRESf);
        vertices0.putf(XRESf); vertices0.putf(YRESf);
        vertices0.putf(0);     vertices0.putf(0);
        vertices0.putf(XRESf); vertices0.putf(0);
        vertices0.seal(gl, true);        
        st.ownAttribute(vertices0, true);
        vertices0.enableBuffer(gl, false);
        
        texCoords0 = GLArrayDataServer.createGLSL("gca_TexCoords", 2, GL.GL_FLOAT, false, 4, GL.GL_STATIC_DRAW);
        texCoords0.putf(0f); texCoords0.putf(1f);
        texCoords0.putf(1f);  texCoords0.putf(1f);
        texCoords0.putf(0f);  texCoords0.putf(0f);        
        texCoords0.putf(1f); texCoords0.putf(0f);
        texCoords0.seal(gl, true);
        st.ownAttribute(texCoords0, true);
        texCoords0.enableBuffer(gl, false);

        //generate framebufferobject
        int[] result = new int[1];
        gl.glGenTextures(1, result, 0);
        mFrameBufferTextureID = result[0];
        gl.glBindTexture(GL_TEXTURE_2D, mFrameBufferTextureID);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, 384, 384, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);

        //allocate the framebuffer object ...
        gl.glGenFramebuffers(1, result, 0);
        mFrameBufferObjectID = result[0];
        gl.glBindFramebuffer(GL_FRAMEBUFFER, mFrameBufferObjectID);
        gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, mFrameBufferTextureID, 0);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        st.uniform(gl, new GLUniformData("fb", 0));

        // will be changed in display(..)
        st.uniform(gl, new GLUniformData("en", 0));
        st.uniform(gl, new GLUniformData("tm", 0.0f));
        st.uniform(gl, new GLUniformData("br", 0.0f));
        st.uniform(gl, new GLUniformData("et", 0.0f));

        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

//        mBaseMusic = new BaseMusic(BaseGlobalEnvironment.getInstance().getMusicFileName());
//        mBaseMusic.init();
//        mBaseMusic.play();
    }

    public void display(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        final int XRES = drawable.getWidth();
        final int YRES = drawable.getHeight();

        //if NO music is used sync to mainloop start ...
        if (!mFrameSkipAverageFrameStartTimeInitialized) {
            mFrameSkipAverageFrameStartTimeInitialized = true;
            mFrameSkipAverageFramerateTimeStart = System.nanoTime();
        }
//        if (!getBaseMusic().isOffline()) {
//            //if music IS used sync to first second of music ...
//            if (BaseRoutineRuntime.getInstance().getBaseMusic().getPositionInMilliseconds()>0 && !mMusicSyncStartTimeInitialized) {
//                BaseLogging.getInstance().info("Synching to BaseMusic ...");
//                mFrameSkipAverageFramerateTimeStart = (long)(System.nanoTime()-((double)BaseRoutineRuntime.getInstance().getBaseMusic().getPositionInMilliseconds()*1000000.0d));
//                mMusicSyncStartTimeInitialized = true;
//            }
//        }
        //allow music DSP's to synchronize with framerate ...
//        mBaseMusic.synchonizeMusic();

        //use this for offline rendering/capture ...
        int MMTime_u_ms = (int)((((double)mFrameCounter)*44100.0f)/60.0f);
        //use this for music synched rendering ...
        //int MMTime_u_ms = (int)(BaseRoutineRuntime.getInstance().getBaseMusic().getPositionInMilliseconds()*(44100.0f/1000.0f));
        //dedicated sync variable for each event ... kinda lame but who cares X-)
        if (MMTime_u_ms>=522240  && !mSyncEvent_01) { mSyncEvent_01 = true; handleSyncEvent(MMTime_u_ms); }
        if (MMTime_u_ms>=1305480 && !mSyncEvent_02) { mSyncEvent_02 = true; handleSyncEvent(MMTime_u_ms); }
        if (MMTime_u_ms>=1827720 && !mSyncEvent_03) { mSyncEvent_03 = true; handleSyncEvent(MMTime_u_ms); }
        if (MMTime_u_ms>=2349960 && !mSyncEvent_04) { mSyncEvent_04 = true; handleSyncEvent(MMTime_u_ms); }
        if (MMTime_u_ms>=3394440 && !mSyncEvent_05) { mSyncEvent_05 = true; handleSyncEvent(MMTime_u_ms); }
        if (MMTime_u_ms>=3916680 && !mSyncEvent_06) { mSyncEvent_06 = true; handleSyncEvent(MMTime_u_ms); }
        if (MMTime_u_ms>=4438408 && !mSyncEvent_07) { mSyncEvent_07 = true; handleSyncEvent(MMTime_u_ms); }
        if (MMTime_u_ms>=5482831 && !mSyncEvent_08) { mSyncEvent_08 = true; handleSyncEvent(MMTime_u_ms); }
        //calculate current time based on 60fps reference framerate ...
        MMTime_u_ms = (int)((((double)mFrameCounter)*44100.0)/60.0);
        gl.glDisable(GL_CULL_FACE);
        gl.glDisable(GL_DEPTH_TEST);

        st.useProgram(gl, true);

        vertices0.enableBuffer(gl, true);
        texCoords0.enableBuffer(gl, true);

        pmvMatrix.glMatrixMode(PMVMatrix.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glOrthof(0f, XRES, YRES, 0f, -1f, 1f);
        pmvMatrix.glMatrixMode(PMVMatrix.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        st.uniform(gl, pmvMatrixUniform);
        
        gl.glActiveTexture(GL_TEXTURE0);

        //gogogo! O-)
        float tBrightnessSync = 40.0f-((MMTime_u_ms-mSyncTime)/1000.0f);
        if (tBrightnessSync<1) {
            tBrightnessSync=1;
        }
        mEffectTime = (float)((MMTime_u_ms-mEffectSyncTime)/100000.0f);
        
        if (mSyncEventNumber==0 && mEffectTime<4.0f) {
            //fadein and fullscreen rotate
            tBrightnessSync = mEffectTime/4.0f;
        } else if (mSyncEventNumber==8 && mEffectTime>12.0f) {
             //fullscrenn mushroom transform
             tBrightnessSync = 1.0f-((mEffectTime-12.0f)/3.5f);
        }
        
        if (mSyncEventNumber==0 || mSyncEventNumber==1) {
             //zoomin from fog
             mEffectNumber = 3;
             mEffectTime *= 1.75;
             float tEffectTimeMax = 9.3f; 
             if (mEffectTime>=tEffectTimeMax) {
                 mEffectTime=tEffectTimeMax;
             }
        } else if(mSyncEventNumber==2 || mSyncEventNumber==3) {
             //transform big after zoomin
             mEffectNumber = 4;
             mEffectTime *= 0.25f;
        } else if(mSyncEventNumber==4) {
             //mandelbrot orbit-trap zoomout
             mEffectNumber = 1;
             mEffectTime *= 0.0002f;
        } else if(mSyncEventNumber==5 || mSyncEventNumber==6) {
             //inside fractal
             mEffectNumber = 5;
             mEffectTime *= 0.02f;
        } else if(mSyncEventNumber==7) {
             //spiral orbit-trap
             mEffectNumber = 0;
             mEffectTime *= 0.02f;
        } else if(mSyncEventNumber==8) {
             //fadeout fractal
             mEffectNumber = 6;
             mEffectTime *= 0.364f;
        }
         
        gl.glBindFramebuffer(GL_FRAMEBUFFER, mFrameBufferObjectID);
        // gl.glViewport(0, 0, drawable.getWidth(), drawable.getHeight());                
        GLUniformData en = st.getUniform("en");
        if(mSyncEventNumber==7) {
             en.setData(2);
        }
        if(mSyncEventNumber==4) {
             en.setData(7);
        } else {
             en.setData(0);
        }
        st.uniform(gl, en);
         
        GLUniformData et = st.getUniform("et");
        st.uniform(gl, et.setData(9.1f));

        st.uniform(gl, st.getUniform("tm").setData(MMTime_u_ms/40000.0f));
        st.uniform(gl, st.getUniform("br").setData(tBrightnessSync));

        if(mSyncEventNumber==4 || mSyncEventNumber==7) {
           //render to fbo only when using julia/mandel orbittrap ...
           // gl.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
           gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);        
        }
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
        st.uniform(gl, en.setData(mEffectNumber));
        st.uniform(gl, et.setData(mEffectTime));

        gl.glEnable(GL_TEXTURE_2D);
        gl.glBindTexture(GL_TEXTURE_2D, mFrameBufferTextureID);

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);        

        vertices0.enableBuffer(gl, false);
        texCoords0.enableBuffer(gl, false);         
        st.useProgram(gl, false);

        //---
        mFrameCounter++;
        if (wantsFrameSkip()) {
            mFrameSkipAverageFramerateTimeEnd = System.nanoTime();
            double tDesiredFrameRate = (float)getDesiredFramerate();
            double tSingleFrameTime = 1000000000.0f/tDesiredFrameRate;
            double tElapsedTime = mFrameSkipAverageFramerateTimeEnd - mFrameSkipAverageFramerateTimeStart;
            mFrameCounterTargetValue = tElapsedTime/tSingleFrameTime;
            mFrameCounterDifference = mFrameCounterTargetValue-mFrameCounter;
            if (mFrameCounterDifference>2) {
                mFrameCounter+=mFrameCounterDifference;
                mSkippedFramesCounter+=mFrameCounterDifference;
            } else if (mFrameCounterDifference<-2) {
                //hold framecounter advance ...
                mFrameCounter--;
            }
        }
       
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        
        st.useProgram(gl, true);
        vertices0.seal(false);
        vertices0.rewind();
        vertices0.putf(0);     vertices0.putf(height);
        vertices0.putf(width); vertices0.putf(height);
        vertices0.putf(0);     vertices0.putf(0);
        vertices0.putf(width); vertices0.putf(0);
        vertices0.seal(gl, true);        
        st.ownAttribute(vertices0, true);
        vertices0.enableBuffer(gl, false);
        
        final FloatBuffer mScreenDimensionV = (FloatBuffer) mScreenDimensionUniform.getBuffer();
        mScreenDimensionV.put(0, (float) width);
        mScreenDimensionV.put(1, (float) height);
        st.uniform(gl, mScreenDimensionUniform);

        st.useProgram(gl, false);
        gl.glViewport(0, 0, width, height);                
    }

    public void dispose(GLAutoDrawable inDrawable) {
        GL2ES2 gl = inDrawable.getGL().getGL2ES2();
        gl.glDeleteFramebuffers(1, new int[] { mFrameBufferObjectID }, 0);
        gl.glDeleteTextures(1, new int[] { mFrameBufferTextureID }, 0);
        st.destroy(gl);
    }

//    public BaseMusic getBaseMusic() {
//        return mBaseMusic;
//    }
    
    public void resetFrameCounter() {
        mFrameCounter = 0;
    }

//END --- BaseRoutineRuntime ---

    protected int mFrameBufferTextureID;
    protected int mFrameBufferObjectID;
    protected int mSyncTime;
    protected int mSyncEventNumber;
    protected float mEffectTime;
    protected int mEffectNumber;
    protected int mEffectSyncTime;

    protected boolean mSyncEvent_01;
    protected boolean mSyncEvent_02;
    protected boolean mSyncEvent_03;
    protected boolean mSyncEvent_04;
    protected boolean mSyncEvent_05;
    protected boolean mSyncEvent_06;
    protected boolean mSyncEvent_07;
    protected boolean mSyncEvent_08;

    public void handleSyncEvent(int inMMTime_u_ms) {
        mSyncTime = inMMTime_u_ms;
        mSyncEventNumber++;
        System.out.println("NEW SYNC EVENT! tSyncEventNumber="+mSyncEventNumber+" tSyncTime="+mSyncTime);
        if (mSyncEventNumber==0 || mSyncEventNumber==2 || mSyncEventNumber==5 || mSyncEventNumber==8) {
            mEffectSyncTime = inMMTime_u_ms;
        }
    }


//BEGIN --- main entry point ---

    public static void main(String args[]) throws Exception {
        String tRoutineClassName = null;
        boolean tMultiSampling = false;
        int tNumberOfSampleBuffers = -1;
        boolean tAnisotropicFiltering = false;
        float tAnisotropyLevel = -1.0f;
        boolean tFrameCapture = false;
        boolean tFrameSkip = true;
        int desiredFrameRate = 30;
        
        final ElektronenMultiplizierer demo = new ElektronenMultiplizierer(
                tRoutineClassName,
                tMultiSampling,tNumberOfSampleBuffers,
                tAnisotropicFiltering,tAnisotropyLevel,
                tFrameCapture,
                tFrameSkip, desiredFrameRate
        );
        GLCapabilitiesImmutable caps = demo.getGLCapabilities();
        
        GLWindow glWindow = GLWindow.create(caps);
        glWindow.setSize(640, 480);
        glWindow.setTitle("Jogamp.org - ElektronenMultiplizierer - GL2ES2/NEWT");   
        glWindow.addGLEventListener(demo);
        final Animator animator = new Animator(glWindow);
        animator.setUpdateFPSFrames(60, System.err);
        glWindow.addWindowListener(new com.jogamp.newt.event.WindowAdapter() {
            public void windowDestroyNotify(com.jogamp.newt.event.WindowEvent e) {
                animator.stop();
                System.exit(0);
            }
        });
        glWindow.setVisible(true);
        animator.start();
    }
    
//END --- main entry point ---
    
}
