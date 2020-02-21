import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;

public class Bug1398macOSContextOpsOnMainThread extends JFrame implements GLEventListener {

	protected GLCanvas canvas;

	static {
		GLProfile.initSingleton();
	}

	public Bug1398macOSContextOpsOnMainThread() throws Exception {
		System.out.println("Java version: " + Runtime.class.getPackage().getSpecificationVersion() + " (" + Runtime.class.getPackage().getImplementationVersion() + ")");
		System.out.println("classloader:" + Thread.currentThread().getContextClassLoader());
		System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));

		setTitle("Bug1398macOSContextOpsOnMainThread");
		//setUndecorated(true);
		//setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setBackground(Color.WHITE);
		Dimension dim = new Dimension(800, 600);
		GraphicsDevice device = getGraphicsConfiguration().getDevice();
		DisplayMode dm = device.getDisplayMode();
		System.out.println("w:" + dm.getWidth() + " h:" + dm.getHeight() + " rr:" + dm.getRefreshRate() + " bits:" + dm.getBitDepth() + " dim.w:" + dim.width + " dim.h:" + dim.height);
		GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2));
		canvas = new GLCanvas(caps);
        canvas.addGLEventListener(new RedSquareES2());
		// canvas.setBounds(0, 0, 1, 1);
		canvas.setBounds(0, 0, 800, 600);

		JPanel panel = new JPanel();
		panel.setLayout(null);
		panel.setPreferredSize(dim);
		panel.add(canvas);

		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		c.add(panel, BorderLayout.CENTER);

		pack();

		// center window
		GraphicsConfiguration gc = getGraphicsConfiguration();
		Rectangle bounds = gc.getBounds();
		System.out.println("gc.bounds: " + bounds);
		dim = Toolkit.getDefaultToolkit().getScreenSize();
		System.out.println("dim: " + dim);
		int w = getSize().width;
		int h = getSize().height;
		int x = (dim.width  - w) / 2;
		int y = (dim.height - h) / 2;
		setLocation(x, y);
		setVisible(true);

		final FPSAnimator animator = new FPSAnimator(canvas, 30, true);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// Run this on another thread than the AWT event queue to
				// make sure the call to Animator.stop() completes before
				// exiting
				new Thread(new Runnable() {
					public void run() {
						animator.stop();
					}
				}).start();
			}
		});
		animator.start();
	}

	/**
	 * OpenGL funcs
	 */
	private void initExtension(GL2 gl, String glExtensionName) {
		if (!gl.isExtensionAvailable(glExtensionName)) {
			final String message = "OpenGL extension \"" + glExtensionName + "\" not available.\n\nPlease update your display driver to the latest version.";
			throw new RuntimeException(message);
		}
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();

		int[] arg = new int[1];
		gl.glGetIntegerv(GL2.GL_MAX_TEXTURE_SIZE, arg, 0);
		System.out.println("GL_MAX_TEXTURE_SIZE:" + arg[0]);

		System.out.println("Available GL Extensions: " + gl.glGetString(GL2.GL_EXTENSIONS));

		initExtension(gl, "GL_ARB_texture_non_power_of_two");
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl = drawable.getGL().getGL2();
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		GL2 gl = drawable.getGL().getGL2();
	}
}

