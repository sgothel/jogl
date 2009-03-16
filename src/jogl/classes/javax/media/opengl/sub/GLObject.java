/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
 */

package javax.media.opengl.sub;

import java.nio.*;

import javax.media.opengl.*;

/**
 * GLObject specifies the GL profile related implementations
 * and it's composition with GLContext, which is a lifetime one.
 */
public interface GLObject {

  public boolean isGL();

  public boolean isGL2();

  public boolean isGLES1();

  public boolean isGLES2();

  public boolean isGLES();

  public boolean isGL2ES1();

  public boolean isGL2ES2();

  /**
   * @return This object cast to GL
   * @throws GLException is this GLObject is not a GL implementation
   */
  public GL getGL() throws GLException;

  /**
   * @return This object cast to GL2
   * @throws GLException is this GLObject is not a GL2 implementation
   */
  public GL2 getGL2() throws GLException;

  /**
   * @return This object cast to GLES1
   * @throws GLException is this GLObject is not a GLES1 implementation
   */
  public GLES1 getGLES1() throws GLException;

  /**
   * @return This object cast to GLES2
   * @throws GLException is this GLObject is not a GLES2 implementation
   */
  public GLES2 getGLES2() throws GLException;

  /**
   * @return This object cast to GL2ES1
   * @throws GLException is this GLObject is not a GL2ES1 implementation
   */
  public GL2ES1 getGL2ES1() throws GLException;

  /**
   * @return This object cast to GL2ES2
   * @throws GLException is this GLObject is not a GL2ES2 implementation
   */
  public GL2ES2 getGL2ES2() throws GLException;

  public String toString();

  /**
   * @return This GL object's bound GLContext
   */
  public GLContext getContext();

}

