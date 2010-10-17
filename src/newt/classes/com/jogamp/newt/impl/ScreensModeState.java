package com.jogamp.newt.impl;

import java.util.HashMap;

public class ScreensModeState {
	private static HashMap screenModes = new HashMap();
	private static Object lock = new Object();
	
	public ScreensModeState(){
		
	}
	public synchronized void setScreenModeController(ScreenModeStatus screenModeStatus){
		synchronized (lock) {
			screenModes.put(screenModeStatus.getScreenFQN(), screenModeStatus);	
		}
	}
	
	public synchronized ScreenModeStatus getScreenModeController(String screenFQN){
		return (ScreenModeStatus) screenModes.get(screenFQN);
	}
}
