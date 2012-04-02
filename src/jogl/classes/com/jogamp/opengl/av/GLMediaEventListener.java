
package com.jogamp.opengl.av;

import com.jogamp.opengl.av.GLMediaPlayer.TextureFrame;

public interface GLMediaEventListener {

    static final int EVENT_CHANGE_SIZE   = 1<<0;
    static final int EVENT_CHANGE_FPS    = 1<<1;
    static final int EVENT_CHANGE_BPS    = 1<<2;
    static final int EVENT_CHANGE_LENGTH = 1<<3;

    public void attributesChanges(GLMediaPlayer mp, int event_mask);
    public void newFrameAvailable(GLMediaPlayer mp, TextureFrame frame);

}

