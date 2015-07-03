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
package com.jogamp.opengl.test.android;

import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Arrays;

import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;

import jogamp.newt.driver.android.NewtBaseActivity;

import com.jogamp.common.net.Uri;
import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.av.MovieSimple;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;

public class MovieSimpleActivity1 extends NewtBaseActivity {
   static String TAG = "MovieSimpleActivity1";

   MouseAdapter toFrontMouseListener = new MouseAdapter() {
       public void mouseClicked(final MouseEvent e) {
           final Object src = e.getSource();
           if(src instanceof Window) {
               ((Window)src).requestFocus(false);
           }
       } };

   @Override
   public void onCreate(final Bundle savedInstanceState) {
       super.onCreate(savedInstanceState);

       final boolean mPlayerNoZoom = Boolean.valueOf(System.getProperty("jnlp.mplayer.nozoom"));
       final boolean mPlayerHUD = Boolean.valueOf(System.getProperty("jnlp.mplayer.hud"));
       final boolean mPlayerSharedHUD = mPlayerHUD && Boolean.valueOf(System.getProperty("jnlp.mplayer.hud.shared"));
       Log.d(TAG, "onCreate - 0 - mPlayerNoScale "+mPlayerNoZoom+", mPlayerHUD "+mPlayerHUD+", mPlayerSharedHUD "+mPlayerSharedHUD);

       final String[] streamLocs = new String[] {
               System.getProperty("jnlp.media0_url0"),
               System.getProperty("jnlp.media0_url1"),
               System.getProperty("jnlp.media0_url2") };
       final Uri streamLoc0 = getUri(streamLocs, 2, false);
       if(null == streamLoc0) { throw new RuntimeException("no media reachable: "+Arrays.asList(streamLocs)); }

       final Uri  streamLoc1;
       {
           Uri _streamLoc1 = null;
           if(mPlayerHUD && !mPlayerSharedHUD) {
               final String[] urls1 = new String[] { System.getProperty("jnlp.media1_url0") };
               _streamLoc1 = getUri(urls1, 1, false);
           }
           if(null == _streamLoc1) { _streamLoc1 = streamLoc0; }
           streamLoc1 = _streamLoc1;
       }

       setTransparencyTheme();
       setFullscreenFeature(getWindow(), true);

       final android.view.ViewGroup viewGroup = new android.widget.FrameLayout(getActivity().getApplicationContext());
       getWindow().setContentView(viewGroup);

       // also initializes JOGL
       final GLCapabilities capsMain = new GLCapabilities(GLProfile.getGL2ES2());
       capsMain.setNumSamples(4);
       capsMain.setSampleBuffers(true);
       capsMain.setBackgroundOpaque(!mPlayerHUD);

       // screen for layout params ..
       final com.jogamp.newt.Display dpy = NewtFactory.createDisplay(null);
       final com.jogamp.newt.Screen scrn = NewtFactory.createScreen(dpy, 0);
       scrn.addReference();

       final Animator anim = new Animator();

       // Main
       final GLWindow glWindowMain = GLWindow.create(scrn, capsMain);
       {
           final int padding = mPlayerHUD ? 32 : 0;
           final android.view.View androidView = ((jogamp.newt.driver.android.WindowDriver)glWindowMain.getDelegatedWindow()).getAndroidView();
           glWindowMain.setSurfaceSize(scrn.getWidth()-padding, scrn.getHeight()-padding);
           glWindowMain.setUndecorated(true);
           // setContentView(getWindow(), glWindowMain);
           viewGroup.addView(androidView, new android.widget.FrameLayout.LayoutParams(glWindowMain.getSurfaceWidth(), glWindowMain.getSurfaceHeight(), Gravity.BOTTOM|Gravity.RIGHT));
           registerNEWTWindow(glWindowMain);
       }
       anim.add(glWindowMain);
       glWindowMain.setVisible(true);

       final MovieSimple demoMain = new MovieSimple(null);
       final GLMediaPlayer mPlayerMain = demoMain.getGLMediaPlayer();
       if(mPlayerHUD) {
           demoMain.setEffects(MovieSimple.EFFECT_GRADIENT_BOTTOM2TOP);
           demoMain.setTransparency(0.9f);
       }
       demoMain.setScaleOrig(mPlayerNoZoom);
       mPlayerMain.addEventListener( new GLMediaPlayer.GLMediaEventListener() {
           @Override
           public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) { }

           @Override
           public void attributesChanged(final GLMediaPlayer mp, final int event_mask, final long when) {
               System.err.println("MovieSimpleActivity1 AttributesChanges: events_mask 0x"+Integer.toHexString(event_mask)+", when "+when);
               System.err.println("MovieSimpleActivity1 State: "+mp);
               if( 0 != ( GLMediaEventListener.EVENT_CHANGE_INIT & event_mask ) ) {
                   glWindowMain.addGLEventListener(demoMain);
                   anim.setUpdateFPSFrames(60*5, System.err);
                   anim.resetFPSCounter();
               }
               if( 0 != ( ( GLMediaEventListener.EVENT_CHANGE_ERR | GLMediaEventListener.EVENT_CHANGE_EOS ) & event_mask ) ) {
                   final StreamException se = mPlayerMain.getStreamException();
                   if( null != se ) {
                       se.printStackTrace();
                   }
                   getActivity().finish();
               }
           }
       });
       demoMain.initStream(streamLoc0, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO, 0);

       if(mPlayerHUD) {
            final GLMediaPlayer mPlayerShared = mPlayerSharedHUD ? mPlayerMain : null;
            final GLCapabilities capsHUD = new GLCapabilities(GLProfile.getGL2ES2());
            capsHUD.setNumSamples(4);
            capsHUD.setSampleBuffers(true);
            capsHUD.setBackgroundOpaque(false);
            final GLWindow glWindowHUD = GLWindow.create(scrn, capsHUD);
            glWindowMain.invoke(false, new GLRunnable() {
                @Override
                public boolean run(final GLAutoDrawable drawable) {
                    final GLMediaPlayer mPlayerSub;
                    final MovieSimple demoHUD;
                    final Rectangle windowBounds = (Rectangle) scrn.getViewportInWindowUnits().cloneMutable();
                    if(null != mPlayerShared) {
                       if(0 < mPlayerShared.getWidth() && mPlayerShared.getWidth()<scrn.getWidth()/2 &&
                          0 < mPlayerShared.getHeight() && mPlayerShared.getHeight()<scrn.getHeight()/2) {
                           final int[] wh = glWindowHUD.convertToWindowUnits(new int[]{mPlayerShared.getWidth(), mPlayerShared.getHeight()});
                           windowBounds.setWidth( wh[0] );
                           windowBounds.setHeight( wh[1] );
                       }
                       glWindowHUD.setSharedAutoDrawable(glWindowMain);
                       demoHUD = new MovieSimple(mPlayerShared);
                       mPlayerSub = mPlayerShared;
                    } else {
                       demoHUD = new MovieSimple(null);
                       mPlayerSub = demoHUD.getGLMediaPlayer();
                    }
                    mPlayerSub.addEventListener( new GLMediaPlayer.GLMediaEventListener() {
                       @Override
                       public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) { }

                       @Override
                       public void attributesChanged(final GLMediaPlayer mp, final int event_mask, final long when) {
                            if( 0 != ( GLMediaEventListener.EVENT_CHANGE_INIT & event_mask ) ) {
                                glWindowHUD.addGLEventListener(demoHUD);
                            }
                           if( 0 != ( ( GLMediaEventListener.EVENT_CHANGE_ERR | GLMediaEventListener.EVENT_CHANGE_EOS ) & event_mask ) ) {
                               final StreamException se = mPlayerMain.getStreamException();
                               if( null != se ) {
                                   se.printStackTrace();
                               }
                               getActivity().finish();
                           }
                       }
                    });
                    demoHUD.initStream(streamLoc1, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO, 0);

                    glWindowHUD.setPosition(windowBounds.getX(), windowBounds.getY());
                    glWindowHUD.setSize(windowBounds.getWidth(), windowBounds.getHeight());
                    System.err.println("HUD: "+mPlayerHUD);
                    System.err.println("HUD: "+windowBounds);
                    glWindowHUD.addMouseListener(toFrontMouseListener);

                    viewGroup.post(new Runnable() {
                        public void run() {
                            final android.view.View androidView = ((jogamp.newt.driver.android.WindowDriver)glWindowHUD.getDelegatedWindow()).getAndroidView();
                            // addContentView(getWindow(), glWindowHUD, new android.view.ViewGroup.LayoutParams(glWindowHUD.getWidth(), glWindowHUD.getHeight()));
                            viewGroup.addView(androidView, new android.widget.FrameLayout.LayoutParams(glWindowHUD.getSurfaceWidth(), glWindowHUD.getSurfaceHeight(), Gravity.TOP|Gravity.LEFT));
                            registerNEWTWindow(glWindowHUD);
                            anim.add(glWindowHUD);
                            glWindowHUD.setVisible(true);
                        } } );
                    return true;
                } } );
       }

       scrn.removeReference();

       Log.d(TAG, "onCreate - X");
   }

   static Uri getUri(final String path[], final int off, final boolean checkAvail) {
       Uri uri = null;
       for(int i=off; null==uri && i<path.length; i++) {
           if(null != path[i] && path[i].length()>0) {
               if( checkAvail ) {
                   final URLConnection uc = IOUtil.getResource(path[i], null);
                   if( null != uc ) {
                       try {
                           uri = Uri.valueOf(uc.getURL());
                       } catch (final URISyntaxException e) {
                           uri = null;
                       }
                       if( uc instanceof HttpURLConnection ) {
                           ((HttpURLConnection)uc).disconnect();
                       }
                   }
               } else {
                   try {
                       uri = Uri.cast(path[i]);
                   } catch (final URISyntaxException e) {
                       uri = null;
                   }
               }
               Log.d(TAG, "Stream: <"+path[i]+">: "+(null!=uri));
           }
       }
       return uri;
   }
}
