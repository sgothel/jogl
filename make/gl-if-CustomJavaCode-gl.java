
  public static final int GL_LIGHT0 = 0x4000;
  public static final int GL_LIGHT1 = 0x4001;
  public static final int GL_LIGHT2 = 0x4002;
  public static final int GL_LIGHT3 = 0x4003;
  public static final int GL_LIGHT4 = 0x4004;
  public static final int GL_LIGHT5 = 0x4005;
  public static final int GL_LIGHT6 = 0x4006;
  public static final int GL_LIGHT7 = 0x4007;
  public static final int GL_LIGHTING = 0xB50;
  public static final int GL_AMBIENT = 0x1200;
  public static final int GL_DIFFUSE = 0x1201;
  public static final int GL_SPECULAR = 0x1202;
  public static final int GL_POSITION = 0x1203;
  public static final int GL_SPOT_DIRECTION = 0x1204;
  public static final int GL_SPOT_EXPONENT = 0x1205;
  public static final int GL_SPOT_CUTOFF = 0x1206;
  public static final int GL_CONSTANT_ATTENUATION = 0x1207;
  public static final int GL_LINEAR_ATTENUATION = 0x1208;
  public static final int GL_QUADRATIC_ATTENUATION = 0x1209;
  public static final int GL_EMISSION = 0x1600;
  public static final int GL_SHININESS = 0x1601;
  public static final int GL_AMBIENT_AND_DIFFUSE = 0x1602;
  public static final int GL_COLOR_MATERIAL = 0xB57;
  public static final int GL_NORMALIZE = 0xBA1;

  public static final int GL_FLAT = 0x1D00;
  public static final int GL_SMOOTH = 0x1D01;

  public static final int GL_MODELVIEW = 0x1700;
  public static final int GL_PROJECTION = 0x1701;

  public static final int GL_VERTEX_ARRAY = 0x8074;
  public static final int GL_NORMAL_ARRAY = 0x8075;
  public static final int GL_COLOR_ARRAY = 0x8076;
  public static final int GL_TEXTURE_COORD_ARRAY = 0x8078;

  public boolean isGL2();

  public boolean isGLES1();

  public boolean isGLES2();

  public boolean isGLES();

  public boolean isGL2ES1();

  public boolean isGL2ES2();

  public GL2 getGL2() throws GLException;

  public GLES1 getGLES1() throws GLException;

  public GLES2 getGLES2() throws GLException;

  public GL2ES1 getGL2ES1() throws GLException;

  public GL2ES2 getGL2ES2() throws GLException;

  public boolean matchesProfile();

  public boolean matchesProfile(String test_profile);

  public String toString();

  public GLContext getContext();

  public void glClearDepth( double depth );

  public void glDepthRange(double zNear, double zFar);

  public void glPopMatrix();

  public void glPushMatrix();

  public void glLoadIdentity() ;

  public void glLoadMatrixf(java.nio.FloatBuffer m) ;
  public void glLoadMatrixf(float[] m, int m_offset);

  public void glMatrixMode(int mode) ;

  public void glMultMatrixf(java.nio.FloatBuffer m) ;
  public void glMultMatrixf(float[] m, int m_offset);

  public void glTranslatef(float x, float y, float z) ;

  public void glRotatef(float angle, float x, float y, float z);

  public void glScalef(float x, float y, float z) ;

  public void glOrthof(float left, float right, float bottom, float top, float zNear, float zFar) ;

  public void glFrustumf(float left, float right, float bottom, float top, float zNear, float zFar);

  public void glEnableClientState(int arrayName);
  public void glDisableClientState(int arrayName);

  public void glVertexPointer(GLArrayData array);
  public void glVertexPointer(int size, int type, int stride, java.nio.Buffer pointer);
  public void glVertexPointer(int size, int type, int stride, long pointer_buffer_offset);

  public void glColorPointer(GLArrayData array);
  public void glColorPointer(int size, int type, int stride, java.nio.Buffer pointer);
  public void glColorPointer(int size, int type, int stride, long pointer_buffer_offset);
  public void glColor4f(float red, float green, float blue, float alpha);

  public void glNormalPointer(GLArrayData array);
  public void glNormalPointer(int type, int stride, java.nio.Buffer pointer);
  public void glNormalPointer(int type, int stride, long pointer_buffer_offset);

  public void glTexCoordPointer(GLArrayData array);
  public void glTexCoordPointer(int size, int type, int stride, java.nio.Buffer pointer);
  public void glTexCoordPointer(int size, int type, int stride, long pointer_buffer_offset);

  public void glLightfv(int light, int pname, java.nio.FloatBuffer params);
  public void glLightfv(int light, int pname, float[] params, int params_offset);
  public void glMaterialfv(int face, int pname, java.nio.FloatBuffer params);
  public void glMaterialfv(int face, int pname, float[] params, int params_offset);

  public void glShadeModel(int mode);

