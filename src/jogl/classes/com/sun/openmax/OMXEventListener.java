
package com.sun.openmax;

public interface OMXEventListener {

    static final int EVENT_CHANGE_SIZE = 1<<0;
    static final int EVENT_CHANGE_FPS  = 1<<1;
    static final int EVENT_CHANGE_BPS  = 1<<2;
    static final int EVENT_CHANGE_LENGTH  = 1<<3;

    public void changedAttributes(OMXInstance omx, int event_mask);

}

