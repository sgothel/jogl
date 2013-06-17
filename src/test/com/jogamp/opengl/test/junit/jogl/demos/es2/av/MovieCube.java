/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

package com.jogamp.opengl.test.junit.jogl.demos.es2.av;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.jogl.demos.es2.TextureSequenceCubeES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;

public class MovieCube implements GLEventListener, GLMediaEventListener {
    static boolean waitForKey = false;
    final URLConnection stream;
    final float zoom0, rotx, roty;
    TextureSequenceCubeES2 cube=null;
    GLMediaPlayer mPlayer=null;
    
    public MovieCube() throws IOException {
        this(new URL("http://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4").openConnection(), 
             -2.3f, 0f, 0f);        
    }
    
    public MovieCube(URLConnection stream, float zoom0, float rotx, float roty) throws IOException {
        this.stream = stream;
        this.zoom0 = zoom0;
        this.rotx = rotx;
        this.roty = roty;
    }

    private final KeyListener keyAction = new KeyAdapter() {
        public void keyReleased(KeyEvent e)  {
            if( !e.isPrintableKey() || e.isAutoRepeat() ) {
                return;
            }            
            System.err.println("MC "+e);
            int pts0 = mPlayer.getCurrentPosition();
            int pts1 = 0;
            switch(e.getKeyCode()) {
                case KeyEvent.VK_3:
                case KeyEvent.VK_RIGHT:      pts1 = pts0 +  1000; break;
                case KeyEvent.VK_4:
                case KeyEvent.VK_UP:         pts1 = pts0 + 10000; break;
                case KeyEvent.VK_2:
                case KeyEvent.VK_LEFT:       pts1 = pts0 -  1000; break;
                case KeyEvent.VK_1:
                case KeyEvent.VK_DOWN:       pts1 = pts0 - 10000; break;
                case KeyEvent.VK_ESCAPE:
                case KeyEvent.VK_DELETE:
                case KeyEvent.VK_BACK_SPACE: {
                    mPlayer.seek(0);
                    mPlayer.setPlaySpeed(1.0f);
                    mPlayer.start();
                    break;
                }
                case KeyEvent.VK_SPACE: {
                    if(GLMediaPlayer.State.Paused == mPlayer.getState()) {
                        mPlayer.start();
                    } else {
                        mPlayer.pause();
                    }
                    break;
                }
                case KeyEvent.VK_S: mPlayer.setPlaySpeed(mPlayer.getPlaySpeed()/2.0f); break;
                case KeyEvent.VK_F: mPlayer.setPlaySpeed(mPlayer.getPlaySpeed()*2.0f); break;
            }
            
            if( 0 != pts1 ) {
                mPlayer.seek(pts1);
            }
        }        
    };
    
    @Override
    public void attributesChanges(GLMediaPlayer mp, int event_mask, long when) {
        System.out.println("attributesChanges: "+mp+", 0x"+Integer.toHexString(event_mask)+", when "+when);        
    }

    @Override
    public void newFrameAvailable(GLMediaPlayer mp, long when) {
        // System.out.println("newFrameAvailable: "+mp+", when "+when);
    }

    public void init(GLAutoDrawable drawable) {
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.err.println(JoglVersion.getGLInfo(gl, null));

        mPlayer = GLMediaPlayerFactory.create();
        mPlayer.addEventListener(this);
        cube = new TextureSequenceCubeES2(mPlayer, false, zoom0, rotx, roty);        
        
        if(waitForKey) {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.err.println("Press enter to continue");
            try {
                System.err.println(stdin.readLine());
            } catch (IOException e) { }
        }
        try {
            System.out.println("p0 "+mPlayer);
            mPlayer.initGLStream(gl, stream);
            System.out.println("p1 "+mPlayer);
        } catch (Exception e) { 
            e.printStackTrace(); 
            if(null != mPlayer) {
                mPlayer.destroy(gl);
                mPlayer = null;
            }
            throw new GLException(e);
        }
        
        cube.init(drawable);
        mPlayer.start();

        boolean added;
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {            
            final Window window = (Window) upstreamWidget;
            window.addKeyListener(keyAction);
            added = true;
        } else { added = false; }       
        System.err.println("MC.init: kl-added "+added+", "+drawable.getClass().getName());
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        if(null == mPlayer) { return; }
        cube.reshape(drawable, x, y, width, height);
    }

    public void dispose(GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" MovieCube.dispose ... ");
        if(null == mPlayer) { return; }
        mPlayer.stop();
        GL2ES2 gl = drawable.getGL().getGL2ES2();
        mPlayer.destroy(gl);
        mPlayer=null;
        cube.dispose(drawable);
        cube=null;
    }

    public void display(GLAutoDrawable drawable) {
        if(null == mPlayer) { return; }
        cube.display(drawable);
    }

    public void displayChanged(javax.media.opengl.GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }
    
    public static void main(String[] args) throws MalformedURLException, IOException, InterruptedException {
        int width = 510;
        int height = 300;
        System.err.println("TexCubeES2.run()");

        String url_s="http://download.blender.org/peach/bigbuckbunny_movies/BigBuckBunny_320x180.mp4";        
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-width")) {
                i++;
                width = MiscUtils.atoi(args[i], width);
            } else if(args[i].equals("-height")) {
                i++;
                height = MiscUtils.atoi(args[i], height);
            } else if(args[i].equals("-url")) {
                i++;
                url_s = args[i];
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            }
        }
        final MovieCube mc = new MovieCube(new URL(url_s).openConnection(), -2.3f, 0f, 0f);
        
        final GLWindow window = GLWindow.create(new GLCapabilities(GLProfile.getGL2ES2()));
        // Size OpenGL to Video Surface
        window.setSize(width, height);
        window.setFullscreen(false);
        window.setSize(width, height);
        window.addGLEventListener(mc);
        final Animator anim = new Animator(window);
        window.addWindowListener(new WindowAdapter() {
            public void windowDestroyed(WindowEvent e) {
                anim.stop();
            }                
        });
        // anim.setUpdateFPSFrames(60, System.err);
        anim.start();
        window.setVisible(true);
    }
}

