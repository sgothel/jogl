
  /**
   * The following enumeration are common in GL2ES1 and GL2GL3
   */
  public static final int GL_AND = 0x1501;
  public static final int GL_AND_INVERTED = 0x1504;
  public static final int GL_AND_REVERSE = 0x1502;
  public static final int GL_BLEND_DST = 0x0BE0;
  public static final int GL_BLEND_SRC = 0x0BE1;
  public static final int GL_BUFFER_ACCESS = 0x88BB;
  public static final int GL_CLEAR = 0x1500;
  public static final int GL_COLOR_LOGIC_OP = 0x0BF2;
  public static final int GL_COPY = 0x1503;
  public static final int GL_COPY_INVERTED = 0x150C;
  public static final int GL_DEPTH_COMPONENT24 = 0x81A6;
  public static final int GL_DEPTH_COMPONENT32 = 0x81A7;
  public static final int GL_EQUIV = 0x1509;
  public static final int GL_LINE_SMOOTH = 0x0B20;
  public static final int GL_LINE_SMOOTH_HINT = 0x0C52;
  public static final int GL_LOGIC_OP_MODE = 0x0BF0;
  public static final int GL_MULTISAMPLE = 0x809D;
  public static final int GL_NAND = 0x150E;
  public static final int GL_NOOP = 0x1505;
  public static final int GL_NOR = 0x1508;
  public static final int GL_OR = 0x1507;
  public static final int GL_OR_INVERTED = 0x150D;
  public static final int GL_OR_REVERSE = 0x150B;
  public static final int GL_POINT_FADE_THRESHOLD_SIZE = 0x8128;
  public static final int GL_POINT_SIZE = 0x0B11;
  public static final int GL_SAMPLE_ALPHA_TO_ONE = 0x809F;
  public static final int GL_SET = 0x150F;
  public static final int GL_SMOOTH_LINE_WIDTH_RANGE = 0x0B22;
  public static final int GL_SMOOTH_POINT_SIZE_RANGE = 0x0B12;
  public static final int GL_STENCIL_INDEX1 = 0x8D46;
  public static final int GL_STENCIL_INDEX4 = 0x8D47;
  public static final int GL_WRITE_ONLY = 0x88B9;
  public static final int GL_XOR = 0x1506;

  public void glClearDepth( double depth );

  public void glDepthRange(double zNear, double zFar);

  public int glGetBoundBuffer(int target);

  public boolean glIsVBOArrayEnabled();
  public boolean glIsVBOElementEnabled();

