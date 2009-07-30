
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_AND = 0x1501;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_AND_INVERTED = 0x1504;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_AND_REVERSE = 0x1502;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_BLEND_DST = 0x0BE0;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_BLEND_SRC = 0x0BE1;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_BUFFER_ACCESS = 0x88BB;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_CLEAR = 0x1500;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_COLOR_LOGIC_OP = 0x0BF2;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_COPY = 0x1503;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_COPY_INVERTED = 0x150C;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_DEPTH_COMPONENT24 = 0x81A6;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_DEPTH_COMPONENT32 = 0x81A7;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_EQUIV = 0x1509;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_LINE_SMOOTH = 0x0B20;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_LINE_SMOOTH_HINT = 0x0C52;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_LOGIC_OP_MODE = 0x0BF0;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_MULTISAMPLE = 0x809D;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_NAND = 0x150E;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_NOOP = 0x1505;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_NOR = 0x1508;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_OR = 0x1507;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_OR_INVERTED = 0x150D;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_OR_REVERSE = 0x150B;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_POINT_FADE_THRESHOLD_SIZE = 0x8128;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_POINT_SIZE = 0x0B11;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_SAMPLE_ALPHA_TO_ONE = 0x809F;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_SET = 0x150F;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_SMOOTH_LINE_WIDTH_RANGE = 0x0B22;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_SMOOTH_POINT_SIZE_RANGE = 0x0B12;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_STENCIL_INDEX1 = 0x8D46;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_STENCIL_INDEX4 = 0x8D47;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_WRITE_ONLY = 0x88B9;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_XOR = 0x1506;

  public void glClearDepth( double depth );

  public void glDepthRange(double zNear, double zFar);

  public int glGetBoundBuffer(int target);

  public boolean glIsVBOArrayEnabled();
  public boolean glIsVBOElementEnabled();

