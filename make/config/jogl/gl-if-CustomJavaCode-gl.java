
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
  public static final int GL_CLEAR = 0x1500;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_COLOR_LOGIC_OP = 0x0BF2;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_COPY = 0x1503;
  /** Common in ES1, GL2 and GL3 */
  public static final int GL_COPY_INVERTED = 0x150C;
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
  public static final int GL_XOR = 0x1506;

  public void glClearDepth( double depth );

  public void glDepthRange(double zNear, double zFar);

  /**
   * @param target a GL buffer (VBO) target as used in {@link GL#glBindBuffer(int, int)}, ie {@link GL#GL_ELEMENT_ARRAY_BUFFER}, {@link GL#GL_ARRAY_BUFFER}, ..
   * @return the GL buffer (VBO) name bound to a target via {@link GL#glBindBuffer(int, int)} or 0 if unbound.
   */
  public int glGetBoundBuffer(int target);

  /**
   * @param buffer a GL buffer name, generated with {@link GL#glGenBuffers(int, int[], int)} and used in {@link GL#glBindBuffer(int, int)}, {@link GL#glBufferData(int, long, java.nio.Buffer, int)} or {@link GL2#glNamedBufferDataEXT(int, long, java.nio.Buffer, int)} for example.
   * @return the size of the given GL buffer
   */
  public long glGetBufferSize(int buffer);

  /**
   * @return true if a VBO is bound to {@link GL.GL_ARRAY_BUFFER} via {@link GL#glBindBuffer(int, int)}, otherwise false
   */
  public boolean glIsVBOArrayEnabled();

  /**
   * @return true if a VBO is bound to {@link GL.GL_ELEMENT_ARRAY_BUFFER} via {@link GL#glBindBuffer(int, int)}, otherwise false
   */
  public boolean glIsVBOElementArrayEnabled();

