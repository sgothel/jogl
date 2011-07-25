package jogamp.opengl.swt.legacy;

import org.eclipse.swt.opengl.GLData;
import org.eclipse.swt.widgets.Composite;

public class GLCanvas extends jogamp.opengl.swt.GLCanvas implements
		LegacySWTGLCanvasInterface {

	public GLCanvas(Composite parent, int style, GLData data) {
		super(parent, style, null, null);
	}

	@Override
	public GLData getGLData() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isCurrent() {
		return getContext().isCurrent();
	}

	@Override
	public void setCurrent() {
		getContext().makeCurrent();
	}

}
