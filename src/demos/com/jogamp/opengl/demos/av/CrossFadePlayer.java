/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.demos.av;

import com.jogamp.common.net.Uri;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.opengl.util.av.AudioSink;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

import java.io.File;

/**
 * Parallel media player that demonstrate CrossFade of audio volume during playback.
 * This also demonstrate audio only playback of the GLMediaPlayer.
 */
public class CrossFadePlayer
{
	static GLMediaPlayer[] player;
	static volatile boolean stop = false;

	public static void main(final String[] args)
	{

        if(args.length==0) {
            System.out.println("No files! \n" +
                    "pass as many media files you want\n" +
                    "to the CrossFadePlayer arguments \n" +
                    "and i will try CrossFade-play them all in parallel!");
        }

		final GLMediaEventListener mediaEventListener = new GLMediaEventListener()
		{
			@Override
			public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) { }

			@Override
			public void attributesChanged(final GLMediaPlayer mp, final int event_mask, final long when)
			{
				System.out.println("\n***\nEvent mask changed: " + event_mask);
				System.out.println("Timestamp: "+ when);
				System.out.println("State of player: " + mp.getState().toString() +"\n");

				if ((event_mask & GLMediaEventListener.EVENT_CHANGE_INIT) !=0) {
					System.out.println("Duration: " + mp.getDuration() + "ms");
					System.out.println("Volume: " + mp.getAudioVolume());
					System.out.println("player.initGL()...");
                    new InterruptSource.Thread() {
                        public void run() {
                            try {
                                mp.initGL(null);
                                if ( GLMediaPlayer.State.Paused == mp.getState() ) { // init OK
                                    mp.play();
                                }
                            } catch (final Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
				} else if ((event_mask & GLMediaEventListener.EVENT_CHANGE_PAUSE) !=0) {
					System.out.println("player.paused()...");
				} else if ((event_mask & GLMediaEventListener.EVENT_CHANGE_PLAY) !=0) {
					System.out.println("playing...");
					System.out.println(mp.toString());
					System.out.println(mp.getAudioSink().toString());
				} else if( 0 != ( GLMediaEventListener.EVENT_CHANGE_EOS & event_mask ) ) {
                    final StreamException se = mp.getStreamException();
                    if( null != se ) {
                        System.err.println("Player State: EOS + Exception");
                        stop = true;
                    } else {
                        System.err.println("Player State: EOS");
                        new InterruptSource.Thread() {
                            public void run() {
                                System.out.println("mp.setPlaySpeed(1f) returned: " + mp.setPlaySpeed(1f));
                                mp.seek(0);
                                mp.play();
                            }
                        }.start();
                    }
                }
				if( 0 != ( ( GLMediaEventListener.EVENT_CHANGE_ERR | GLMediaEventListener.EVENT_CHANGE_EOS ) & event_mask ) ) {
					final StreamException se = mp.getStreamException();
					if( null != se ) {
						se.printStackTrace();
					}
					new InterruptSource.Thread() {
						public void run() {
							System.out.println("terminating...");
							stop = true;
						}
					}.start();
				}

			}
		};

        // Initialize media players
        player = new GLMediaPlayer[args.length];
        int i=0;
        for( final String arg: args ) {
            player[i] = GLMediaPlayerFactory.createDefault();
            if(player[i]!=null){
                System.out.println("Created CrossFade player: "+ i + " " + player[i].getClass().getName());
                player[i].addEventListener(mediaEventListener);
                try {
                    final String filename = arg;
                    if(filename.equals("")){
                        System.out.println("No file selected: arg " + i +" = "+ filename);
                        player[i]=null;
                    } else {
                        final File file = new File(filename);
                        if(!file.exists()){
                            System.out.println("File do not exist");
                        } else {
                            final Uri uri = Uri.valueOf(file);
                            System.out.println("State of player "+ i +": " + player[i].getState().toString());
                            System.out.println("...initializing stream "+ i +"...");
                            player[i].initStream(uri, GLMediaPlayer.STREAM_ID_NONE, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.TEXTURE_COUNT_DEFAULT);

                        }
                    }
                } catch (final Exception e1) {
                    e1.printStackTrace();
                }
            } else {
                System.out.println("Failed to create player "+ i +"!");
            }
            i++;
        }


        // Main thread CrossFade until playback is done
		final long startTime = com.jogamp.common.os.Platform.currentTimeMillis();
        final double piPlayers = Math.PI*2.0f/args.length;
		StreamException se = null;
		while( null == se && stop == false ) {
				try {
					Thread.sleep(100);
				} catch (final InterruptedException e) { }

                // Find out the longest duration...
                float maxDuration = 1000.0f ;
                for(final GLMediaPlayer p: player) {
                    if(p!=null){
                        if( p.getDuration() > maxDuration) {
                            maxDuration = p.getDuration();
                        }
                    }
                }

				// tune the volume on players to crossfade!
				final float progress = (com.jogamp.common.os.Platform.currentTimeMillis()-startTime)/maxDuration;

                i = 0;
                for(final GLMediaPlayer p: player){
                    if(p!=null){
                        final AudioSink sink = p.getAudioSink();
				        if(sink != null){
                            final float volume = (float) (0.5f+(0.5f*(Math.cos(40.0f*progress+(piPlayers*i)))));
                            final float playbacktime = com.jogamp.common.os.Platform.currentTimeMillis()-startTime;
                            // System.out.println("player: "+ i +" volume = " + volume +" progress = "+ progress +" time = "+ playbacktime + " / duration = " + maxDuration);
                            sink.setVolume(volume);
				        }

                        se = p.getStreamException();
                        if( null != se) {
                            se.printStackTrace();
                            throw new RuntimeException(se);
                        }
                    }

                    i++;
                }
		}

        for(final GLMediaPlayer p: player) {
            if(p!=null)
                p.destroy(null);
        }
		System.out.println("...main exit...");
	}
}
