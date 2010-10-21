package com.jogamp.newt.impl;

import com.jogamp.newt.ScreenMode;

public class ScreenModeStatus {
	private String screenFQN = null;
	private ScreenMode[] screenModes = null;
	
	private int currentScreenMode = -1;
	private short currentScreenRate = -1;
	private int currentScreenRotation = -1;
	
	private int originalScreenMode = -1;
	private short originalScreenRate = -1;
	private int originalScreenRotation = -1;
	
	public ScreenModeStatus(String screenFQN, int originalScreenMode,
			short originalScreenRate, int originalScreenRotation) {
		this.screenFQN = screenFQN;
		this.originalScreenMode = originalScreenMode;
		this.originalScreenRate = originalScreenRate;
		this.originalScreenRotation = originalScreenRotation;
		
		this.currentScreenMode = originalScreenMode;
		this.currentScreenRate = originalScreenRate;
		this.currentScreenRotation = originalScreenRotation;
	}

	public void setCurrentScreenRotation(int currentScreenRotation) {
		this.currentScreenRotation = currentScreenRotation;
	}

	public int getCurrentScreenRotation() {
		return currentScreenRotation;
	}

	public int getOriginalScreenRotation() {
		return originalScreenRotation;
	}

	public int getCurrentScreenMode() {
		return currentScreenMode;
	}

	public void setCurrentScreenMode(int currentScreenMode) {
		this.currentScreenMode = currentScreenMode;
	}

	public short getCurrentScreenRate() {
		return currentScreenRate;
	}

	public void setCurrentScreenRate(short currentRate) {
		this.currentScreenRate = currentRate;
	}

	public String getScreenFQN() {
		return screenFQN;
	}

	public void setScreenFQN(String screenFQN) {
		this.screenFQN = screenFQN;
	}

	public ScreenMode[] getScreenModes() {
		return screenModes;
	}

	public void setScreenModes(ScreenMode[] screenModes) {
		this.screenModes = screenModes;
	}
	public boolean isOriginalMode(){
		if(currentScreenMode == originalScreenMode
				&& currentScreenRate == originalScreenRate)
			return true;
		return false;
	}
	public boolean isOriginalRotation(){
		if(currentScreenRotation == originalScreenRotation)
			return true;
		return false;
	}
	
	public int getOriginalScreenMode() {
		return originalScreenMode;
	}

	public short getOriginalScreenRate() {
		return originalScreenRate;
	}
}
