package jogamp.opengl.swt.legacy;

import javax.media.opengl.GLContext;

public interface SWTGLCanvasInterface {
	boolean isCurrent();

	int makeCurrent();
	
	void releaseContext();
	
	GLContext getContext();

	void swapBuffers();
}
