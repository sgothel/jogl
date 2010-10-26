/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
package com.jogamp.newt;

import com.jogamp.newt.event.ScreenModeListener;
import com.jogamp.newt.impl.Debug;
import java.util.List;
import javax.media.nativewindow.AbstractGraphicsScreen;

public interface Screen {

    /**
     * A 10s timeout for screen mode change. It is observed, that some platforms
     * need a notable amount of time for this task, especially in case of rotation change.
     */
    public static final int SCREEN_MODE_CHANGE_TIMEOUT = 10000;

    public static final boolean DEBUG = Debug.debug("Screen");

    boolean isNativeValid();

    /**
     *
     * @return number of references by Window
     */
    int getReferenceCount();

    void destroy();

    /**
     * @return {@link Display#getDestroyWhenUnused()}
     *
     * @see #addReference()
     * @see #removeReference()
     * @see Display#setDestroyWhenUnused(boolean)
     */
    boolean getDestroyWhenUnused();

    /**
     * calls {@link Display#setDestroyWhenUnused(boolean)}.
     *
     * @see #addReference()
     * @see #removeReference()
     * @see Display#setDestroyWhenUnused(boolean)
     */
    void setDestroyWhenUnused(boolean v);

    /**
     * See {@link Display#addReference()}
     *
     * @see #removeReference()
     * @see #setDestroyWhenUnused(boolean)
     * @see #getDestroyWhenUnused()
     */
    int addReference();

    /**
     * See {@link Display#removeReference()}
     *
     * @see #addReference()
     * @see #setDestroyWhenUnused(boolean)
     * @see #getDestroyWhenUnused()
     */
    int removeReference();

    AbstractGraphicsScreen getGraphicsScreen();

    /**
     * @return this Screen index of all Screens of {@link #getDisplay()}.
     */
    int getIndex();

    /**
     * @return the current screen width
     */
    int getWidth();

    /**
     * @return the current screen height
     */
    int getHeight();

    /**
     * @return the associated Display
     */
    Display getDisplay();

    /** 
     * @return the screen fully qualified Screen name,
     * which is a key of {@link com.jogamp.newt.Display#getFQName()} + {@link #getIndex()}.
     */
    String getFQName();

    /**
     * @param sml ScreenModeListener to be added for ScreenMode change events
     */
    public void addScreenModeListener(ScreenModeListener sml);

    /**
     * @param sml ScreenModeListener to be removed from ScreenMode change events
     */
    public void removeScreenModeListener(ScreenModeListener sml);

    /** 
     * Return a list of available {@link com.jogamp.newt.ScreenMode}s.
     * @return a shallow copy of the internal immutable {@link com.jogamp.newt.ScreenMode}s,
     * or null if not implemented for this native type {@link com.jogamp.newt.Display#getType()}.
     */
    List/*<ScreenMode>*/ getScreenModes();

    /**
     * Return the original {@link com.jogamp.newt.ScreenMode}, as used at NEWT initialization.
     * @return null if functionality not implemented,
     * otherwise the original ScreenMode which is element of the list {@link #getScreenModes()}.
     *
     */
    ScreenMode getOriginalScreenMode();

    /**
     * Return the current {@link com.jogamp.newt.ScreenMode}.
     * @return null if functionality not implemented,
     * otherwise the current ScreenMode which is element of the list {@link #getScreenModes()}.
     */
    ScreenMode getCurrentScreenMode();

    /**
     * Set the current {@link com.jogamp.newt.ScreenMode}.
     * @param screenMode to be made current, must be element of the list {@link #getScreenModes()}.
     * @return true if successful, otherwise false
     */
    boolean setCurrentScreenMode(ScreenMode screenMode);
}
