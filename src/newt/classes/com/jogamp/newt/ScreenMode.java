package com.jogamp.newt;

public class ScreenMode {
	public static final int ROTATE_0   = 0;
	public static final int ROTATE_90  = 90;
	public static final int ROTATE_180 = 180;
	public static final int ROTATE_270 = 270;
	
	private int index;
	private int width;
	private int height;
	private int bitsPerPixel = -1;
	
	private short[] rates = null;

	public ScreenMode(int index, int width, int height) {
		this.index = index;
		this.width = width;
		this.height = height;
	}
	/** Not safe to use this on platforms 
	 * other than windows. Since the mode ids
	 * on X11 match the native ids. unlike windows
	 * where the ids are generated .
	 * @param index
	 */
	public void setIndex(int index) {
		this.index = index;
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

	public int getBitsPerPixel() {
		return bitsPerPixel;
	}

	public void setBitsPerPixel(int bitsPerPixel) {
		this.bitsPerPixel = bitsPerPixel;
	}
	
	public short getHighestAvailableRate(){
		short highest = rates[0];
		if(rates.length > 1){
			for (int i = 1; i < rates.length; i++) {
				if(rates[i] > highest){
					highest = rates[i];
				}
			}
		}
		return highest;
	}
	
	public String toString() {
		return "ScreenMode: " + this.index + " - " + this.width + " x " 
			+ this.height + " " + getHighestAvailableRate() + " Hz";
	}
}
