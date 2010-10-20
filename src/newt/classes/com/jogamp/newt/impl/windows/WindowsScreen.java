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

package com.jogamp.newt.impl.windows;

import java.util.ArrayList;

import com.jogamp.newt.*;
import com.jogamp.newt.impl.ScreenImpl;
import com.jogamp.newt.impl.ScreenMode;
import com.jogamp.newt.impl.ScreenModeStatus;

import javax.media.nativewindow.*;

public class WindowsScreen extends ScreenImpl {
    static {
        WindowsDisplay.initSingleton();
    }


    public WindowsScreen() {
    }

    protected void createNativeImpl() {
        aScreen = new DefaultGraphicsScreen(getDisplay().getGraphicsDevice(), idx);
        setScreenSize(getWidthImpl0(idx), getHeightImpl0(idx));
    }

    protected void closeNativeImpl() { }
    
    public int getDesktopScreenModeIndex() {
    	int index = super.getDesktopScreenModeIndex();
    	if(index == -1) {
        	/** Set the current screen mode to refering to index zero
        	 *  dependent on the impl of getScreenModes which saves the 
        	 *  current screen mode at index 0 which is the original screen mode
        	 */
	    	ScreenMode[] screenModes = getScreenModes();
	    	if(screenModes != null) {
	    		if(screenModes[0] != null) {
	    			index = screenModes[0].getIndex();
	    		}
	    	}
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
    	ScreenMode sm = screenModes[selectedMode];
    	
    	if(selectedRate == -1){
    		selectedRate = sms.getOriginalScreenRate();
    	}
    	
    	boolean rateAvailable = false;
    	short[] rates = sm.getRates();
    	for(int i=0;i<rates.length;i++){
    		if(rates[i] == selectedRate){
    			rateAvailable = true;
    			break;
    		}
    	}
    	if(!rateAvailable){
    		selectedRate = rates[0];
    	}

    	if(0 == setScreenMode0(idx, sm.getWidth(), sm.getHeight(), sm.getBitsPerPixel(), selectedRate)){	
    		sms.setCurrentScreenMode(selectedMode);
    		sms.setCurrentScreenRate(selectedRate);
    	}
    }
    
    public short getCurrentScreenRate() {
    	short rate = super.getCurrentScreenRate();
    	if(rate == -1){
    		rate = (short)getCurrentScreenRate0(idx);
    	}
    	return rate;
	}
    
    public ScreenMode[] getScreenModes() {
    	ScreenMode[] screenModes = super.getScreenModes();
    	if(screenModes == null) {
    		ArrayList smTemp = new ArrayList();

    		int modeID = -1;
    		ScreenMode mySM = getScreenMode(modeID++);
    		int currentBitsPerPixel = mySM.getBitsPerPixel();
    		while(mySM != null){
    			//filter out modes with diff bits per pixel
    			if(mySM.getBitsPerPixel() == currentBitsPerPixel) {
    				smTemp.add(mySM);
    			}
    			mySM = getScreenMode(modeID++);
    		}
    		int numModes = smTemp.size();
    		if(numModes > 0) {
    			screenModes = new ScreenMode[numModes];
    			for(int i=0;i<numModes;i++) {
    				ScreenMode sm = (ScreenMode)smTemp.get(i);
    				sm.setIndex(i);
    				screenModes[i] = sm;
        		}
    		}
    	}
    	return screenModes;
    }
    private ScreenMode getScreenMode(int modeIndex) {
    	int[] modeProp = getScreenMode0(idx, modeIndex); 
    	if(modeProp == null){
    		return null;
    	}
    	int propIndex = 0;
    	int width = modeProp[propIndex++];
    	int height = modeProp[propIndex++];
    	int bits = modeProp[propIndex++];
    	short rate = (short)modeProp[propIndex++];
    	
    	ScreenMode screenMode = new ScreenMode(modeIndex+1, width, height);
    	screenMode.setRates(new short[]{rate});
    	screenMode.setBitsPerPixel(bits);
    	return screenMode;
    }
    
    public void setScreenRotation(int rot) {
    	if(!isRotationValid(rot)){
			return;
		}
		ScreenModeStatus sms = screensModeState.getScreenModeController(getScreenFQN());
		if(0 == setScreenRotation0(idx, rot)) {
			sms.setCurrentScreenRotation(rot);
		}
	}
    
    /** Check if this rotation is valid for platform
	 * @param rot user requested rotation angle
	 * @return true if is valid
	 */
	private boolean isRotationValid(int rot){
		if((rot == ScreenMode.ROTATE_0) || (rot == ScreenMode.ROTATE_90) || 
				(rot == ScreenMode.ROTATE_180) || (rot == ScreenMode.ROTATE_270)) {
			return true;
		}
		return false;
	}
	
	public int getCurrentScreenRotation() {
		int rot = super.getCurrentScreenRotation();
    	if(rot == -1){
    		return getCurrentScreenRotation0(idx);		
    	}
    	return rot;
	}
    
    // Native calls

    private native int getWidthImpl0(int scrn_idx);
    private native int getHeightImpl0(int scrn_idx);
    
    private native int getCurrentScreenRate0(int scrn_idx);
    private native int[] getScreenMode0(int screen_index, int mode_index);
    
    /** Change screen mode and return zero if successful
     */
    private native int setScreenMode0(int screen_index, int width, int height, int bits, short freq);
    
    private native int getCurrentScreenRotation0(int screen_index);
    
    /** Change screen mode and return zero if successful
     */
    private native int setScreenRotation0(int screen_index, int rot);
}
