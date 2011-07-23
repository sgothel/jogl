package jogamp.opengl.swt;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.ProxySurface;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLProfile;

import jogamp.nativewindow.swt.SWTAccessor;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class GLCanvas extends Canvas implements SWTGLCanvasInterface {

	private Canvas canvas;

	private AbstractGraphicsDevice device;
	private long nativeWindowHandle;
	private GLCapabilities caps;
	private GLDrawableFactory factory;
	private ProxySurface proxySurface;
	private final GLDrawable drawable;
	private final GLContext glcontext;

	public GLCanvas(Composite parent, int style, GLProfile glprofile) {
		super(parent, style | SWT.NO_BACKGROUND);

		canvas = this;

		SWTAccessor.setRealized(canvas, true);

		device = SWTAccessor.getDevice(canvas);
		nativeWindowHandle = SWTAccessor.getWindowHandle(canvas);

		caps = new GLCapabilities(glprofile);
		caps.setSampleBuffers(true);
		factory = GLDrawableFactory.getFactory(glprofile);

		proxySurface = factory.createProxySurface(device, nativeWindowHandle,
				caps, null);
		proxySurface.setSize(640, 480);

		drawable = factory.createGLDrawable(proxySurface);

		drawable.setRealized(true);
		glcontext = drawable.createContext(null);

	}

	@Override
	public boolean isCurrent() {
		return glcontext.isCurrent();
	}

	@Override
	public int makeCurrent() {
		return glcontext.makeCurrent();

	}

	@Override
	public void releaseContext() {
		glcontext.release();
	}

	@Override
	public GLContext getContext() {
		return glcontext;
	}
	
	@Override
	public void swapBuffers() {
		drawable.swapBuffers();
	}


}
