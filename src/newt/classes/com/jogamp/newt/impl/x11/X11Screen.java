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

package com.jogamp.newt.impl.x11;

import com.jogamp.newt.impl.ScreenImpl;
import com.jogamp.newt.impl.ScreenMode;
import com.jogamp.newt.impl.ScreenModeStatus;

import javax.media.nativewindow.x11.*;

public class X11Screen extends ScreenImpl {

    static {
        X11Display.initSingleton();
    }


    public X11Screen() {
    }

    protected void createNativeImpl() {
        long handle = GetScreen0(display.getHandle(), idx);
        if (handle == 0 ) {
            throw new RuntimeException("Error creating screen: "+idx);
        }
        aScreen = new X11GraphicsScreen((X11GraphicsDevice)getDisplay().getGraphicsDevice(), idx);
        setScreenSize(getWidth0(display.getHandle(), idx),
                      getHeight0(display.getHandle(), idx));
    }

    protected void closeNativeImpl() { }
    
    public int getDesktopScreenModeIndex() {
    	int index = super.getDesktopScreenModeIndex();
    	if(index == -1){
    		return getDesktopScreenModeIndex0(display.getHandle(), idx);
    	}
    	return index;
    }
    
    public void setScreenMode(int modeIndex, short rate) {
    	ScreenModeStatus sms = screensModeState.getScreenModeController(getScreenFQN());
    	ScreenMode[] screenModes = sms.getScreenModes();
    	
    	short selectedRate = rate;
    	int selectedMode = modeIndex;
    	
    	if(modeIndex < 0 || (modeIndex > screenModes.length)){
    		selectedMode = sms.getOriginalScreenMode();
    	}
    	ScreenMode screenMode = screenModes[selectedMode];
    	
    	if(selectedRate == -1){
    		selectedRate = sms.getOriginalScreenRate();
    	}
    	
    	boolean rateAvailable = false;
    	short[] rates = screenMode.getRates();
    	for(int i=0;i<rates.length;i++){
    		if(rates[i] == selectedRate){
    			rateAvailable = true;
    			break;
    		}
    	}
    	if(!rateAvailable){
    		selectedRate = rates[0];
    	}

    	
    	setScreenMode0(display.getHandle(), idx, selectedMode, selectedRate);	
		sms.setCurrentScreenMode(selectedMode);
    	sms.setCurrentScreenRate(selectedRate);
    }
    
    public short getCurrentScreenRate() {
    	short rate = super.getCurrentScreenRate();
    	if(rate == -1){
    		return getCurrentScreenRate0(display.getHandle(), idx);		
    	}
    	return rate;
	}
    
    public ScreenMode[] getScreenModes() {
    	ScreenMode[] screenModes = super.getScreenModes();
    	if(screenModes == null){
    		int numModes = getNumScreenModes0(display.getHandle(), idx);
    		screenModes = new ScreenMode[numModes];
	    	for(int i=0; i< numModes; i++){
	    		screenModes[i] = getScreenMode(i);
	    	}
    	}
    	return screenModes;
    }
    
    private ScreenMode getScreenMode(int modeIndex){
    	int[] modeProp = getScreenMode0(display.getHandle(), idx, modeIndex);
    	
    	if(modeProp == null){
    		return null;
    	}
    	int propIndex = 0;
    	int index = modeProp[propIndex++];
    	int width = modeProp[propIndex++];
    	int height = modeProp[propIndex++];
    	
    	ScreenMode screenMode = new ScreenMode(index, width, height);
    	
    	short[] rates = new short[modeProp.length - propIndex];
    	for(int i= propIndex; i < modeProp.length; i++)
    	{
    		rates[i-propIndex] = (short) modeProp[i];
    	}
    	screenMode.setRates(rates);
    	return screenMode;
    }
    
    //----------------------------------------------------------------------
    // Internals only
    //

    private native long GetScreen0(long dpy, int scrn_idx);
    private native int  getWidth0(long display, int scrn_idx);
    private native int  getHeight0(long display, int scrn_idx);

    private native int getDesktopScreenModeIndex0(long display, int screen_index);
    private native short getCurrentScreenRate0(long display, int screen_index);
    
    private native void setScreenMode0(long display, int screen_index, int mode_index, short freq);
    
    private native int[] getScreenMode0(long display, int screen_index, int mode_index);
    private native int getNumScreenModes0(long display, int screen_index);

}

