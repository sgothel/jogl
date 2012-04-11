
package com.jogamp.opengl.av;

import javax.media.opengl.GL;

public interface GLMediaEventListener {

    static final int EVENT_CHANGE_SIZE   = 1<<0;
    static final int EVENT_CHANGE_FPS    = 1<<1;
    static final int EVENT_CHANGE_BPS    = 1<<2;
    static final int EVENT_CHANGE_LENGTH = 1<<3;

    /**
     * @param mp the event source 
     * @param event_mask the changes attributes
     * @param when system time in msec. 
     */
    public void attributesChanges(GLMediaPlayer mp, int event_mask, long when);
    
    /** 
     * Signaling listeners that {@link GLMediaPlayer#getNextTexture(GL, boolean)} is able to deliver a new frame.
     * @param mp the event source 
     * @param when system time in msec. 
     **/
    public void newFrameAvailable(GLMediaPlayer mp, long when);

}

