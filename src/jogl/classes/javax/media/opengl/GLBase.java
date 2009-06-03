/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
 */

package javax.media.opengl;

import java.nio.*;

/**
 * The base interface from which all GL profiles derive, providing
 * checked conversion down to concrete profiles, and access to the
 * OpenGL context associated with the GL.
 */
public interface GLBase {
    
  /**
   * Indicates whether this GL object conforms to any of the common GL profiles.
   * @return whether this GL object conforms to any of the common GL profiles
   */
  public boolean isGL();

  /**
   * Indicates whether this GL object conforms to the GL3 profile.
   * The GL3 profile reflects OpenGL versions greater or equal 3.1
   * @return whether this GL object conforms to the GL3 profile
   */
  public boolean isGL3();

  /**
   * Indicates whether this GL object conforms to the GL2 profile.
   * The GL2 profile reflects OpenGL versions greater or equal 1.5
   * @return whether this GL object conforms to the GL2 profile
   */
  public boolean isGL2();

  /**
   * Indicates whether this GL object conforms to the GLES1 profile.
   * @return whether this GL object conforms to the GLES1 profile
   */
  public boolean isGLES1();

  /**
   * Indicates whether this GL object conforms to the GLES2 profile.
   * @return whether this GL object conforms to the GLES2 profile
   */
  public boolean isGLES2();

  /**
   * Indicates whether this GL object conforms to one of the OpenGL ES compatible profiles.
   * @return whether this GL object conforms to one of the OpenGL ES profiles
   */
  public boolean isGLES();

  /**
   * Indicates whether this GL object conforms to the GL2ES1 compatible profile.
   * @return whether this GL object conforms to the GL2ES1 profile
   */
  public boolean isGL2ES1();

  /**
   * Indicates whether this GL object conforms to the GL2ES2 compatible profile.
   * @return whether this GL object conforms to the GL2ES2 profile
   */
  public boolean isGL2ES2();

  /** Indicates whether this GL object supports GLSL. */
  public boolean hasGLSL();

  /**
   * Casts this object to the GL interface.
   * @return this object cast to the GL interface
   * @throws GLException if this GLObject is not a GL implementation
   */
  public GL getGL() throws GLException;

  /**
   * Casts this object to the GL3 interface.
   * @return this object cast to the GL3 interface
   * @throws GLException if this GLObject is not a GL3 implementation
   */
  public GL3 getGL3() throws GLException;

  /**
   * Casts this object to the GL2 interface.
   * @return this object cast to the GL2 interface
   * @throws GLException if this GLObject is not a GL2 implementation
   */
  public GL2 getGL2() throws GLException;

  /**
   * Casts this object to the GLES1 interface.
   * @return this object cast to the GLES1 interface
   * @throws GLException if this GLObject is not a GLES1 implementation
   */
  public GLES1 getGLES1() throws GLException;

  /**
   * Casts this object to the GLES2 interface.
   * @return this object cast to the GLES2 interface
   * @throws GLException if this GLObject is not a GLES2 implementation
   */
  public GLES2 getGLES2() throws GLException;

  /**
   * Casts this object to the GL2ES1 interface.
   * @return this object cast to the GL2ES1 interface
   * @throws GLException if this GLObject is not a GL2ES1 implementation
   */
  public GL2ES1 getGL2ES1() throws GLException;

  /**
   * Casts this object to the GL2ES2 interface.
   * @return this object cast to the GL2ES2 interface
   * @throws GLException if this GLObject is not a GL2ES2 implementation
   */
  public GL2ES2 getGL2ES2() throws GLException;

  /**
   * Returns the GLContext with which this GL object is associated.
   * @return the GLContext with which this GL object is associated
   */
  public GLContext getContext();

  /**
   * Returns the GLProfile with which this GL object is associated.
   * @return the GLProfile with which this GL object is associated
   */
  public GLProfile getGLProfile();
}
