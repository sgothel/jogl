package com.jogamp.newt.impl;

public class ScreenMode {
	public static final int ROTATE_0   = 0;
	public static final int ROTATE_90  = 90;
	public static final int ROTATE_180 = 180;
	public static final int ROTATE_270 = 270;
	
	private int index;
	private int width;
	private int height;
	private short[] rates = null;

	public ScreenMode(int index, int width, int height) {
		this.index = index;
		this.width = width;
		this.height = height;
	}
	
	public int getIndex() {
		return index;
	}
	public int getWidth() {
		return width;
	}
	public void setWidth(int width) {
		this.width = width;
	}
	public int getHeight() {
		return height;
	}
	public void setHeight(int height) {
		this.height = height;
	}
	public short[] getRates() {
		return rates;
	}
	public void setRates(short[] rates) {
		this.rates = rates;
	}
}
