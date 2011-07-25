package jogamp.opengl.swt;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.opengl.GL;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;

import jogamp.nativewindow.swt.SWTAccessor;
import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableHelper;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.jogamp.common.util.locks.RecursiveLock;

public class GLCanvas extends Canvas implements SWTGLAutoDrawable,
		WindowClosingProtocol {

	private static final boolean DEBUG;

	static {
		DEBUG = Debug.debug("GLCanvas");
	}

	private Canvas canvas;

	private GLDrawableHelper drawableHelper = new GLDrawableHelper();
	private AbstractGraphicsDevice device;
	private long nativeWindowHandle;
	private GLCapabilitiesImmutable capsReqUser;
	private GLDrawableFactory factory;
	private ProxySurface proxySurface;
	private final GLDrawable drawable;
	private int additionalCtxCreationFlags = 0;
	private GLContext context;

	private PaintListener paintListener = new PaintListener() {

		@Override
		public void paintControl(PaintEvent arg0) {
			if ( ! drawableHelper.isExternalAnimatorAnimating() ) {
				display();
			}
		}
	};

	private RecursiveLock drawableSync = new RecursiveLock();
	
	public GLCanvas(Composite parent, int style,
			GLCapabilitiesImmutable capsReqUser, GLContext shareWith)
			throws GLException {

		super(parent, style | SWT.NO_BACKGROUND);

		canvas = this;

		if (null == capsReqUser) {
			capsReqUser = new GLCapabilities(GLProfile.getDefault(GLProfile
					.getDefaultDesktopDevice()));
		} else {
			// don't allow the user to change data
			capsReqUser = (GLCapabilitiesImmutable) capsReqUser.cloneMutable();
		}

		SWTAccessor.setRealized(canvas, true);

		this.capsReqUser = capsReqUser;

		device = SWTAccessor.getDevice(canvas);
		nativeWindowHandle = SWTAccessor.getWindowHandle(canvas);

		factory = GLDrawableFactory.getFactory(capsReqUser.getGLProfile());

		proxySurface = factory.createProxySurface(device, nativeWindowHandle,
				capsReqUser, null);
		proxySurface.setSize(640, 480);

		drawable = factory.createGLDrawable(proxySurface);

		drawable.setRealized(true);
		context = drawable.createContext(shareWith);
		
		canvas.addPaintListener(paintListener);

	}

	@Override
	public GLContext getContext() {
		return context;
	}

	@Override
	public void setContext(GLContext ctx) {
		context = (GLContextImpl) ctx;
		if (null != context) {
			context.setContextCreationFlags(additionalCtxCreationFlags);
		}
	}

	@Override
	public void addGLEventListener(GLEventListener listener) {
		drawableHelper.addGLEventListener(listener);
	}

	@Override
	public void addGLEventListener(int index, GLEventListener listener)
			throws IndexOutOfBoundsException {
		drawableHelper.addGLEventListener(index, listener);
	}

	@Override
	public void removeGLEventListener(GLEventListener listener) {
		drawableHelper.removeGLEventListener(listener);
	}

	@Override
	public void setAnimator(GLAnimatorControl animatorControl)
			throws GLException {
		drawableHelper.setAnimator(animatorControl);
	}

	@Override
	public GLAnimatorControl getAnimator() {
		return drawableHelper.getAnimator();
	}

	@Override
	public void invoke(boolean wait, GLRunnable glRunnable) {
		drawableHelper.invoke(this, wait, glRunnable);
	}

	@Override
	public void destroy() {
		drawable.setRealized(false);
		dispose();
	}

	@Override
	public void display() {
		// check if drawable is realized
		// check if we are in the correct thread
		// check for reshape request
		 drawableHelper.display(this);
	}

	@Override
	public void setAutoSwapBufferMode(boolean onOrOff) {
		drawableHelper.setAutoSwapBufferMode(onOrOff);

	}

	@Override
	public boolean getAutoSwapBufferMode() {
		return drawableHelper.getAutoSwapBufferMode();
	}

	@Override
	public void setContextCreationFlags(int flags) {
		additionalCtxCreationFlags = flags;
	}

	@Override
	public int getContextCreationFlags() {
		return additionalCtxCreationFlags;
	}

	@Override
	public GL getGL() {
		GLContext ctx = getContext();
		return (ctx == null) ? null : ctx.getGL();
	}

	@Override
	public GL setGL(GL gl) {
		GLContext ctx = getContext();
		if (ctx != null) {
			ctx.setGL(gl);
			return gl;
		}
		return null;
	}

	@Override
	public GLContext createContext(GLContext shareWith) {
		drawableSync.lock();
		try {
			return (null != drawable) ? drawable.createContext(shareWith)
					: null;
		} finally {
			drawableSync.unlock();
		}
	}

	@Override
	public void setRealized(boolean realized) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isRealized() {
		drawableSync.lock();
		try {
			return (null != drawable) ? drawable.isRealized() : false;
		} finally {
			drawableSync.unlock();
		}
	}

	@Override
	public int getWidth() {
		return canvas.getClientArea().width;
	}

	@Override
	public int getHeight() {
		return canvas.getClientArea().height;
	}

	@Override
	public void swapBuffers() throws GLException {
		drawable.swapBuffers();
	}

	@Override
	public GLCapabilitiesImmutable getChosenGLCapabilities() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public GLProfile getGLProfile() {
		return capsReqUser.getGLProfile();
	}

	@Override
	public NativeSurface getNativeSurface() {
		drawableSync.lock();
		try {
			return (null != drawable) ? drawable.getNativeSurface() : null;
		} finally {
			drawableSync.unlock();
		}
	}

	@Override
	public long getHandle() {
		drawableSync.lock();
		try {
			return (null != drawable) ? drawable.getHandle() : 0;
		} finally {
			drawableSync.unlock();
		}
	}

	@Override
	public GLDrawableFactory getFactory() {
		drawableSync.lock();
		try {
			return (null != drawable) ? drawable.getFactory() : null;
		} finally {
			drawableSync.unlock();
		}
	}

	@Override
	public int getDefaultCloseOperation() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int setDefaultCloseOperation(int op) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String toString() {
		final int dw = (null != drawable) ? drawable.getWidth() : -1;
		final int dh = (null != drawable) ? drawable.getHeight() : -1;

		return "SWT-GLCanvas[Realized "
				+ isRealized()
				+ ",\n\t"
				+ ((null != drawable) ? drawable.getClass().getName()
						: "null-drawable") + ",\n\tRealized " + isRealized()
				+ ",\n\tFactory   " + getFactory() + ",\n\thandle    0x"
				+ Long.toHexString(getHandle()) + ",\n\tDrawable size " + dw
				+ "x" + dh + ",\n\tSWT pos " + getLocation().x + "/"
				+ getLocation().y + ", size " + getWidth() + "x" + getHeight()
				+ ",\n\tvisible " + isVisible() + "]";
	}
}
