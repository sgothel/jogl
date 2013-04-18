package jogamp.newt.driver.x11;

import com.jogamp.newt.ScreenMode;

public interface ScreenRandR {

    int[] getScreenModeFirstImpl(final long dpy, final int screen_idx);
    int[] getScreenModeNextImpl(final long dpy, final int screen_idx);
    ScreenMode getCurrentScreenModeImpl(final long dpy, final int screen_idx);
    boolean setCurrentScreenModeImpl(final long dpy, final int screen_idx, final ScreenMode screenMode, final int screenModeIdx, final int resolutionIdx);
    
}
