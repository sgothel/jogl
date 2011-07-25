package jogamp.opengl.swt.legacy;

import org.eclipse.swt.opengl.GLData;

public interface LegacySWTGLCanvasInterface {

	GLData getGLData();
	
	boolean isCurrent();
	
	void setCurrent();
	
	void swapBuffers();
	
}
