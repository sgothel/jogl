/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.newt.impl;

import com.jogamp.newt.*;

import javax.media.nativewindow.*;

import java.security.*;

public abstract class ScreenImpl implements Screen {
	protected DisplayImpl display;
	protected int idx;
	protected AbstractGraphicsScreen aScreen;
	protected int refCount; // number of Screen references by Window
	protected int width=-1, height=-1; // detected values: set using setScreenSize
	protected static int usrWidth=-1, usrHeight=-1; // property values: newt.ws.swidth and newt.ws.sheight
	private static AccessControlContext localACC = AccessController.getContext();


	protected static ScreensModeState screensModeState = new ScreensModeState(); // hold all screen mode controllers
	private String screenFQN = null; // string fully qualified name

	private static Class getScreenClass(String type) 
	throws ClassNotFoundException 
	{
		Class screenClass = NewtFactory.getCustomClass(type, "Screen");
		if(null==screenClass) {
			if (NativeWindowFactory.TYPE_EGL.equals(type)) {
				screenClass = Class.forName("com.jogamp.newt.impl.opengl.kd.KDScreen");
			} else if (NativeWindowFactory.TYPE_WINDOWS.equals(type)) {
				screenClass = Class.forName("com.jogamp.newt.impl.windows.WindowsScreen");
			} else if (NativeWindowFactory.TYPE_MACOSX.equals(type)) {
				screenClass = Class.forName("com.jogamp.newt.impl.macosx.MacScreen");
			} else if (NativeWindowFactory.TYPE_X11.equals(type)) {
				screenClass = Class.forName("com.jogamp.newt.impl.x11.X11Screen");
			} else if (NativeWindowFactory.TYPE_AWT.equals(type)) {
				screenClass = Class.forName("com.jogamp.newt.impl.awt.AWTScreen");
			} else {
				throw new RuntimeException("Unknown window type \"" + type + "\"");
			}
		}
		return screenClass;
	}

	public static ScreenImpl create(String type, Display display, final int idx) {
		try {
			if(usrWidth<0 || usrHeight<0) {
				usrWidth  = Debug.getIntProperty("newt.ws.swidth", true, localACC);
				usrHeight = Debug.getIntProperty("newt.ws.sheight", true, localACC);
				if(usrWidth>0 || usrHeight>0) {
					System.err.println("User screen size "+usrWidth+"x"+usrHeight);
				}
			}
			Class screenClass = getScreenClass(type);
			ScreenImpl screen  = (ScreenImpl) screenClass.newInstance();
			screen.display = (DisplayImpl) display;
			screen.idx = idx;

			return screen;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	protected  synchronized final void createNative() {
		if(null == aScreen) {
			if(DEBUG) {
				System.err.println("Screen.createNative() START ("+DisplayImpl.getThreadName()+", "+this+")");
			}
			display.addReference();
			createNativeImpl();
			if(null == aScreen) {
				throw new RuntimeException("Screen.createNative() failed to instanciate an AbstractGraphicsScreen");
			}
			if(DEBUG) {
				System.err.println("Screen.createNative() END ("+DisplayImpl.getThreadName()+", "+this+")");
			}
		}

		initScreenModes();
	}

	/** Retrieve screen modes
	 * and screen rate initializing the status
	 * of the screen mode
	 */
	private void initScreenModes(){
		ScreenMode[] screenModes = getScreenModes();
		String screenFQN = display.getFQName()+idx;
		setScreenFQN(screenFQN);
		ScreenModeStatus screenModeStatus = new ScreenModeStatus(screenFQN , 
				getDesktopScreenModeIndex(), getCurrentScreenRate());
		screenModeStatus.setScreenModes(screenModes);

		screensModeState.setScreenModeController(screenModeStatus);
	}

	private void resetScreenMode() {
		ScreenModeStatus sms = screensModeState.getScreenModeController(getScreenFQN());
		/**Revert the screen mode and rate 
		 * to original state of creation
		 */
		if(!sms.isOriginalMode()) {
			setScreenMode(sms.getOriginalScreenMode(), 
					sms.getOriginalScreenRate());
		}
	}

	public synchronized final void destroy() {
		resetScreenMode();

		if ( null != aScreen ) {
			closeNativeImpl();
			aScreen = null;
		}
		refCount = 0;
		display.removeReference();
	}

	protected synchronized final int addReference() {
		if(DEBUG) {
			System.err.println("Screen.addReference() ("+DisplayImpl.getThreadName()+"): "+refCount+" -> "+(refCount+1));
		}
		if ( 0 == refCount ) {
			createNative();
		}
		if(null == aScreen) {
			throw new RuntimeException("Screen.addReference() (refCount "+refCount+") null AbstractGraphicsScreen");
		}
		return ++refCount;
	}

	protected synchronized final int removeReference() {
		if(DEBUG) {
			System.err.println("Screen.removeReference() ("+DisplayImpl.getThreadName()+"): "+refCount+" -> "+(refCount-1));
		}
		refCount--; // could become < 0, in case of forced destruction without actual creation/addReference
		if(0>=refCount && getDestroyWhenUnused()) {
			destroy();
		}
		return refCount;
	}

	public synchronized final int getReferenceCount() {
		return refCount;
	}

	public final boolean getDestroyWhenUnused() { 
		return display.getDestroyWhenUnused(); 
	}
	public final void setDestroyWhenUnused(boolean v) { 
		display.setDestroyWhenUnused(v); 
	}

	protected abstract void createNativeImpl();
	protected abstract void closeNativeImpl();

	public int getDesktopScreenModeIndex() {
		ScreenModeStatus sms = screensModeState.getScreenModeController(getScreenFQN());
		if(sms != null){
			return sms.getCurrentScreenMode();
		}
		return -1;
	}

	/** 
	 * Get list of available screen modes
	 * null if not implemented for screen platform
	 */
	public ScreenMode[] getScreenModes(){
		ScreenModeStatus sms = screensModeState.getScreenModeController(getScreenFQN());
		if(sms != null) {
			return sms.getScreenModes();
		}
		return null;
	}

	public void setScreenMode(int modeIndex, short rate) {
	}

	public short getCurrentScreenRate() {
		ScreenModeStatus sms = screensModeState.getScreenModeController(getScreenFQN());
		if(sms != null){
			return sms.getCurrentScreenRate();
		}
		return -1;
	}

	/** get the screens mode state handler
	 * which contain the screen mode controller of each screen
	 * @return the ScreensModeState static object
	 */
	protected ScreensModeState getScreensModeState() {
		return screensModeState;
	}

	public String getScreenFQN() {
		return screenFQN;
	}

	private void setScreenFQN(String screenFQN) {
		this.screenFQN = screenFQN;
	}

	protected void setScreenSize(int w, int h) {
		System.err.println("Detected screen size "+w+"x"+h);
		width=w; height=h;
	}

	public final Display getDisplay() {
		return display;
	}

	public final int getIndex() {
		return idx;
	}

	public final AbstractGraphicsScreen getGraphicsScreen() {
		return aScreen;
	}

	public final boolean isNativeValid() {
		return null != aScreen;
	}

	public final int getWidth() {
		return (usrWidth>0) ? usrWidth : (width>0) ? width : 480;
	}

	public final int getHeight() {
		return (usrHeight>0) ? usrHeight : (height>0) ? height : 480;
	}

	public String toString() {
		return "NEWT-Screen[idx "+idx+", refCount "+refCount+", "+getWidth()+"x"+getHeight()+", "+aScreen+", "+display+"]";
	}
}

