/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
 */

package javax.media.opengl.fixedfunc;

import java.nio.*;

import javax.media.opengl.*;

public interface GLPointerFunc { 
  public static final int GL_VERTEX_ARRAY = 0x8074;
  public static final int GL_NORMAL_ARRAY = 0x8075;
  public static final int GL_COLOR_ARRAY = 0x8076;
  public static final int GL_TEXTURE_COORD_ARRAY = 0x8078;

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

}

