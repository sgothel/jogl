package net.java.games.jogl.impl.macosx;

import net.java.games.jogl.*;
import net.java.games.jogl.impl.*;

public class MacOSXPbufferGLContext extends MacOSXGLContext {
  private static final boolean DEBUG = false;

  private int  initWidth;
  private int  initHeight;

  private long pBuffer;
  private int pBufferTextureName;
  
  private int  width;
  private int  height;

  // FIXME: kept around because we create the OpenGL context lazily to
  // better integrate with the MacOSXGLContext framework
  private long nsContextOfParent;

  public MacOSXPbufferGLContext(GLCapabilities capabilities, int initialWidth, int initialHeight) {
    super(null, capabilities, null, null);
    this.initWidth  = initialWidth;
    this.initHeight = initialHeight;
    if (initWidth <= 0 || initHeight <= 0) {
      throw new GLException("Initial width and height of pbuffer must be positive (were (" +
			    initWidth + ", " + initHeight + "))");
    }
  }

  public boolean canCreatePbufferContext() {
    return false;
  }

  public GLContext createPbufferContext(GLCapabilities capabilities,
                                        int initialWidth,
                                        int initialHeight) {
    throw new GLException("Not supported");
  }

  public void bindPbufferToTexture() {
	pBufferTextureName = CGL.bindPBuffer(nsContextOfParent, pBuffer);
  }

  public void releasePbufferFromTexture() {
	CGL.unbindPBuffer(nsContextOfParent, pBuffer, pBufferTextureName);
  }

  public void createPbuffer(long parentView, long parentContext) {
    GL gl = getGL();
    // Must initally grab OpenGL function pointers while parent's
    // context is current because otherwise we don't have the cgl
    // extensions available to us
    resetGLFunctionAvailability();
		
	this.pBuffer = CGL.createPBuffer(nsContext, initWidth, initHeight);
    if (this.pBuffer == 0) {
      throw new GLException("pbuffer creation error: CGL.createPBuffer() failed");
    }
	
	nsContextOfParent = parentContext;
	
	width = getNextPowerOf2(initWidth);
	height = getNextPowerOf2(initHeight);
	
    if (DEBUG) {
      System.err.println("Created pbuffer " + width + " x " + height);
    }
  }

  public void handleModeSwitch(long parentView, long parentContext) {
    throw new GLException("Not yet implemented");
  }

  protected boolean isOffscreen() {
    // FIXME: currently the only caller of this won't cause proper
    // resizing of the pbuffer anyway.
    return false;
  }

  public int getOffscreenContextBufferedImageType() {
    throw new GLException("Should not call this");
  }

  public int getOffscreenContextReadBuffer() {
    throw new GLException("Should not call this");
  }

  public boolean offscreenImageNeedsVerticalFlip() {
    throw new GLException("Should not call this");
  }

  protected void swapBuffers() throws GLException {
    // FIXME: do we need to do anything if the pbuffer is double-buffered?
  }

  int getNextPowerOf2(int number)
  {
	if (((number-1) & number) == 0)
	{
		//ex: 8 -> 0b1000; 8-1=7 -> 0b0111; 0b1000&0b0111 == 0
		return number;
	}	
	int power = 0;
	while (number > 0)
	{
      number = number>>1;
      power++;
	}	
	return (1<<power);
  }
}
