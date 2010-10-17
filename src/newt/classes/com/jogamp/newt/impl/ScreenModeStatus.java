package com.jogamp.newt.impl;

public class ScreenModeStatus {
	private String screenFQN = null;
	private ScreenMode[] screenModes = null;
	
	private int currentScreenMode = -1;
	private short currentScreenRate = -1;

	private int originalScreenMode = -1;
	private short originalScreenRate = -1;
	
	public ScreenModeStatus(String screenFQN, int originalScreenMode,
			short originalScreenRate) {
		this.screenFQN = screenFQN;
		this.originalScreenMode = originalScreenMode;
		this.originalScreenRate = originalScreenRate;
		
		this.currentScreenMode = originalScreenMode;
		this.currentScreenRate = originalScreenRate;
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
	
	public int getOriginalScreenMode() {
		return originalScreenMode;
	}

	public short getOriginalScreenRate() {
		return originalScreenRate;
	}
}
