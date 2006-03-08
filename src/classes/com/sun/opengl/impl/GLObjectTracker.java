/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.impl;

import java.nio.*;
import javax.media.opengl.*;

/** 
 * Tracks the creation of server-side OpenGL objects which can be
 * shared between contexts. Ordinarily, when an OpenGL context is
 * deleted and no other contexts are sharing server-side objects with
 * it, all of the server-side objects are automatically deleted by the
 * OpenGL implementation. It is not necessary for the end user to
 * explicitly delete these objects. However, when the Java2D/OpenGL
 * pipeline is active and frame buffer objects are being used for
 * rendering, it is necessary for all OpenGL contexts created by JOGL
 * to share server-side objects with the Java2D OpenGL context. This
 * means that these objects "leak" into the namespace used by Java2D.
 * In order to prevent memory leaks and to present the same
 * programming model to the end user, it is necessary to track the
 * creation and destruction of all of these server-side OpenGL objects
 * and to explicitly release them when all of the JOGL-created
 * contexts which can see them have been released. <P>
 *
 * The {@link #ref ref} and {@link #unref unref} methods should be
 * used during the creation and destruction of OpenGL contexts by JOGL
 * in order to update the liveness of the objects being tracked. The
 * various other methods should be called by the OpenGL binding in the
 * various named methods.
 */

public class GLObjectTracker {
  private static final boolean DEBUG = Debug.debug("GLObjectTracker");

  //----------------------------------------------------------------------
  // Adders
  //

  // glGenBuffers
  public synchronized void addBuffers(int n, IntBuffer ids) {
    add(getList(BUFFERS), n, ids);
  }

  // glGenBuffers
  public synchronized void addBuffers(int n, int[] ids, int ids_offset) {
    add(getList(BUFFERS), n, ids, ids_offset);
  }

  // glGenBuffersARB
  public synchronized void addBuffersARB(int n, IntBuffer ids) {
    add(getList(BUFFERS_ARB), n, ids);
  }

  // glGenBuffersARB
  public synchronized void addBuffersARB(int n, int[] ids, int ids_offset) {
    add(getList(BUFFERS_ARB), n, ids, ids_offset);
  }

  // glGenFencesAPPLE
  public synchronized void addFencesAPPLE(int n, IntBuffer ids) {
    add(getList(FENCES_APPLE), n, ids);
  }

  // glGenFencesAPPLE
  public synchronized void addFencesAPPLE(int n, int[] ids, int ids_offset) {
    add(getList(FENCES_APPLE), n, ids, ids_offset);
  }

  // glGenFencesNV
  public synchronized void addFencesNV(int n, IntBuffer ids) {
    add(getList(FENCES_NV), n, ids);
  }

  // glGenFencesNV
  public synchronized void addFencesNV(int n, int[] ids, int ids_offset) {
    add(getList(FENCES_NV), n, ids, ids_offset);
  }

  // glGenFragmentShadersATI
  public synchronized void addFragmentShadersATI(int start, int n) {
    add(getList(FRAGMENT_SHADERS_ATI), start, n);
  }

  // glGenFramebuffersEXT
  public synchronized void addFramebuffersEXT(int n, IntBuffer ids) {
    add(getList(FRAMEBUFFERS_EXT), n, ids);
  }

  // glGenFramebuffersEXT
  public synchronized void addFramebuffersEXT(int n, int[] ids, int ids_offset) {
    add(getList(FRAMEBUFFERS_EXT), n, ids, ids_offset);
  }
  
  // glGenLists
  public synchronized void addLists(int start, int n) {
    add(getList(LISTS), start, n);
  }

  // glGenOcclusionQueriesNV
  public synchronized void addOcclusionQueriesNV(int n, IntBuffer ids) {
    add(getList(OCCLUSION_QUERIES_NV), n, ids);
  }

  // glGenOcclusionQueriesNV
  public synchronized void addOcclusionQueriesNV(int n, int[] ids, int ids_offset) {
    add(getList(OCCLUSION_QUERIES_NV), n, ids, ids_offset);
  }

  // glCreateProgram
  public synchronized void addProgramObject(int obj) {
    add(getList(PROGRAM_OBJECTS), obj, 1);
  }

  // glCreateProgramObjectARB
  public synchronized void addProgramObjectARB(int obj) {
    add(getList(PROGRAM_AND_SHADER_OBJECTS_ARB), obj, 1);
  }

  // glGenProgramsARB
  public synchronized void addProgramsARB(int n, IntBuffer ids) {
    add(getList(PROGRAMS_ARB), n, ids);
  }

  // glGenProgramsARB
  public synchronized void addProgramsARB(int n, int[] ids, int ids_offset) {
    add(getList(PROGRAMS_ARB), n, ids, ids_offset);
  }

  // glGenProgramsNV
  public synchronized void addProgramsNV(int n, IntBuffer ids) {
    add(getList(PROGRAMS_NV), n, ids);
  }

  // glGenProgramsNV
  public synchronized void addProgramsNV(int n, int[] ids, int ids_offset) {
    add(getList(PROGRAMS_NV), n, ids, ids_offset);
  }

  // glGenQueries
  public synchronized void addQueries(int n, IntBuffer ids) {
    add(getList(QUERIES), n, ids);
  }

  // glGenQueries
  public synchronized void addQueries(int n, int[] ids, int ids_offset) {
    add(getList(QUERIES), n, ids, ids_offset);
  }

  // glGenQueriesARB
  public synchronized void addQueriesARB(int n, IntBuffer ids) {
    add(getList(QUERIES_ARB), n, ids);
  }

  // glGenQueriesARB
  public synchronized void addQueriesARB(int n, int[] ids, int ids_offset) {
    add(getList(QUERIES_ARB), n, ids, ids_offset);
  }

  // glGenRenderbuffersEXT
  public synchronized void addRenderbuffersEXT(int n, IntBuffer ids) {
    add(getList(RENDERBUFFERS_EXT), n, ids);
  }

  // glGenRenderbuffersEXT
  public synchronized void addRenderbuffersEXT(int n, int[] ids, int ids_offset) {
    add(getList(RENDERBUFFERS_EXT), n, ids, ids_offset);
  }

  // glCreateShader
  public synchronized void addShaderObject(int obj) {
    add(getList(SHADER_OBJECTS), obj, 1);
  }

  // glCreateShaderObjectARB
  public synchronized void addShaderObjectARB(int obj) {
    add(getList(PROGRAM_AND_SHADER_OBJECTS_ARB), obj, 1);
  }

  // glGenTextures
  public synchronized void addTextures(int n, IntBuffer ids) {
    add(getList(TEXTURES), n, ids);
  }

  // glGenTextures
  public synchronized void addTextures(int n, int[] ids, int ids_offset) {
    add(getList(TEXTURES), n, ids, ids_offset);
  }

  // glGenVertexArraysAPPLE
  public synchronized void addVertexArraysAPPLE(int n, IntBuffer ids) {
    add(getList(VERTEX_ARRAYS_APPLE), n, ids);
  }

  // glGenVertexArraysAPPLE
  public synchronized void addVertexArraysAPPLE(int n, int[] ids, int ids_offset) {
    add(getList(VERTEX_ARRAYS_APPLE), n, ids, ids_offset);
  }

  // glGenVertexShadersEXT
  public synchronized void addVertexShadersEXT(int start, int n) {
    add(getList(VERTEX_SHADERS_EXT), start, n);
  }

  //----------------------------------------------------------------------
  // Removers
  //

  // glDeleteBuffers
  public synchronized void removeBuffers(int n, IntBuffer ids) {
    remove(getList(BUFFERS), n, ids);
  }

  // glDeleteBuffers
  public synchronized void removeBuffers(int n, int[] ids, int ids_offset) {
    remove(getList(BUFFERS), n, ids, ids_offset);
  }

  // glDeleteBuffersARB
  public synchronized void removeBuffersARB(int n, IntBuffer ids) {
    remove(getList(BUFFERS_ARB), n, ids);
  }

  // glDeleteBuffersARB
  public synchronized void removeBuffersARB(int n, int[] ids, int ids_offset) {
    remove(getList(BUFFERS_ARB), n, ids, ids_offset);
  }

  // glDeleteFencesAPPLE
  public synchronized void removeFencesAPPLE(int n, IntBuffer ids) {
    remove(getList(FENCES_APPLE), n, ids);
  }

  // glDeleteFencesAPPLE
  public synchronized void removeFencesAPPLE(int n, int[] ids, int ids_offset) {
    remove(getList(FENCES_APPLE), n, ids, ids_offset);
  }

  // glDeleteFencesNV
  public synchronized void removeFencesNV(int n, IntBuffer ids) {
    remove(getList(FENCES_NV), n, ids);
  }

  // glDeleteFencesNV
  public synchronized void removeFencesNV(int n, int[] ids, int ids_offset) {
    remove(getList(FENCES_NV), n, ids, ids_offset);
  }

  // glDeleteFragmentShaderATI
  public synchronized void removeFragmentShaderATI(int obj) {
    remove(getList(FRAGMENT_SHADERS_ATI), obj, 1);
  }

  // glDeleteFramebuffersEXT
  public synchronized void removeFramebuffersEXT(int n, IntBuffer ids) {
    remove(getList(FRAMEBUFFERS_EXT), n, ids);
  }

  // glDeleteFramebuffersEXT
  public synchronized void removeFramebuffersEXT(int n, int[] ids, int ids_offset) {
    remove(getList(FRAMEBUFFERS_EXT), n, ids, ids_offset);
  }
  
  // glDeleteLists
  public synchronized void removeLists(int start, int n) {
    remove(getList(LISTS), start, n);
  }

  // glDeleteOcclusionQueriesNV
  public synchronized void removeOcclusionQueriesNV(int n, IntBuffer ids) {
    remove(getList(OCCLUSION_QUERIES_NV), n, ids);
  }

  // glDeleteOcclusionQueriesNV
  public synchronized void removeOcclusionQueriesNV(int n, int[] ids, int ids_offset) {
    remove(getList(OCCLUSION_QUERIES_NV), n, ids, ids_offset);
  }

  // glDeleteProgram
  public synchronized void removeProgramObject(int obj) {
    remove(getList(PROGRAM_OBJECTS), obj, 1);
  }

  // glDeleteObjectARB
  public synchronized void removeProgramOrShaderObjectARB(int obj) {
    remove(getList(PROGRAM_AND_SHADER_OBJECTS_ARB), obj, 1);
  }

  // glDeleteProgramsARB
  public synchronized void removeProgramsARB(int n, IntBuffer ids) {
    remove(getList(PROGRAMS_ARB), n, ids);
  }

  // glDeleteProgramsARB
  public synchronized void removeProgramsARB(int n, int[] ids, int ids_offset) {
    remove(getList(PROGRAMS_ARB), n, ids, ids_offset);
  }

  // glDeleteProgramsNV
  public synchronized void removeProgramsNV(int n, IntBuffer ids) {
    remove(getList(PROGRAMS_NV), n, ids);
  }

  // glDeleteProgramsNV
  public synchronized void removeProgramsNV(int n, int[] ids, int ids_offset) {
    remove(getList(PROGRAMS_NV), n, ids, ids_offset);
  }

  // glDeleteQueries
  public synchronized void removeQueries(int n, IntBuffer ids) {
    remove(getList(QUERIES), n, ids);
  }

  // glDeleteQueries
  public synchronized void removeQueries(int n, int[] ids, int ids_offset) {
    remove(getList(QUERIES), n, ids, ids_offset);
  }

  // glDeleteQueriesARB
  public synchronized void removeQueriesARB(int n, IntBuffer ids) {
    remove(getList(QUERIES_ARB), n, ids);
  }

  // glDeleteQueriesARB
  public synchronized void removeQueriesARB(int n, int[] ids, int ids_offset) {
    remove(getList(QUERIES_ARB), n, ids, ids_offset);
  }

  // glDeleteRenderbuffersEXT
  public synchronized void removeRenderbuffersEXT(int n, IntBuffer ids) {
    remove(getList(RENDERBUFFERS_EXT), n, ids);
  }

  // glDeleteRenderbuffersEXT
  public synchronized void removeRenderbuffersEXT(int n, int[] ids, int ids_offset) {
    remove(getList(RENDERBUFFERS_EXT), n, ids, ids_offset);
  }

  // glDeleteShader
  public synchronized void removeShaderObject(int obj) {
    remove(getList(SHADER_OBJECTS), obj, 1);
  }

  // glDeleteTextures
  public synchronized void removeTextures(int n, IntBuffer ids) {
    remove(getList(TEXTURES), n, ids);
  }

  // glDeleteTextures
  public synchronized void removeTextures(int n, int[] ids, int ids_offset) {
    remove(getList(TEXTURES), n, ids, ids_offset);
  }

  // glDeleteVertexArraysAPPLE
  public synchronized void removeVertexArraysAPPLE(int n, IntBuffer ids) {
    remove(getList(VERTEX_ARRAYS_APPLE), n, ids);
  }

  // glDeleteVertexArraysAPPLE
  public synchronized void removeVertexArraysAPPLE(int n, int[] ids, int ids_offset) {
    remove(getList(VERTEX_ARRAYS_APPLE), n, ids, ids_offset);
  }

  // glDeleteVertexShaderEXT
  public synchronized void removeVertexShaderEXT(int obj) {
    remove(getList(VERTEX_SHADERS_EXT), obj, 1);
  }

  //----------------------------------------------------------------------
  // Reference count maintenance and manual deletion
  //

  public synchronized void transferAll(GLObjectTracker other) {
    for (int i = 0; i < lists.length; i++) {
      getList(i).addAll(other.lists[i]);
      if (other.lists[i] != null) {
        other.lists[i].clear();
      }
    }
    dirty = true;
  }

  public synchronized void ref() {
    ++refCount;
  }

  public void unref(GLObjectTracker deletedObjectPool) {
    boolean tryDelete = false;
    synchronized (this) {
      if (--refCount == 0) {
        tryDelete = true;
      }
    }
    if (tryDelete) {
      // See whether we should try to do the work now or whether we
      // have to postpone
      GLContext cur = GLContext.getCurrent();
      if ((cur != null) &&
          (cur instanceof GLContextImpl)) {
        GLContextImpl curImpl = (GLContextImpl) cur;
        if (deletedObjectPool != null &&
            deletedObjectPool == curImpl.getDeletedObjectTracker()) {
          // Should be safe to delete these objects now
          try {
            delete(curImpl.getGL());
            return;
          } catch (GLException e) {
            // Shouldn't happen, but if it does, transfer all objects
            // to the deleted object pool hoping we can later clean
            // them up
            deletedObjectPool.transferAll(this);
            throw(e);
          }
        }
      }
      // If we get here, we couldn't attempt to delete the objects
      // right now; instead try to transfer them to the
      // deletedObjectPool for later cleanup (FIXME: should consider
      // throwing an exception if deletedObjectPool is null, since
      // that shouldn't happen)
      if (DEBUG) {
        String s = null;
        if (cur == null) {
          s = "current context was null";
        } else if (!(cur instanceof GLContextImpl)) {
          s = "current context was not a GLContextImpl";
        } else if (deletedObjectPool == null) {
          s = "no current deletedObjectPool";
        } else if (deletedObjectPool != ((GLContextImpl) cur).getDeletedObjectTracker()) {
          s = "deletedObjectTracker didn't match";
          if (((GLContextImpl) cur).getDeletedObjectTracker() == null) {
            s += " (other was null)";
          }
        } else {
          s = "unknown reason";
        }
        System.err.println("Deferred destruction of server-side OpenGL objects into " + deletedObjectPool + ": " + s);
      }

      if (deletedObjectPool != null) {
        deletedObjectPool.transferAll(this);
      }
    }
  }

  public void clean(GL gl) {
    if (dirty) {
      try {
        delete(gl);
        dirty = false;
      } catch (GLException e) {
        // FIXME: not sure what to do here; probably a bad idea to be
        // throwing exceptions during an otherwise-successful makeCurrent
      }
    }
  }


  //----------------------------------------------------------------------
  // Internals only below this point
  //

  // Kinds of sharable server-side OpenGL objects this class tracks
  private static final int BUFFERS                        = 0;
  private static final int BUFFERS_ARB                    = 1;
  private static final int FENCES_APPLE                   = 2;
  private static final int FENCES_NV                      = 3;
  private static final int FRAGMENT_SHADERS_ATI           = 4;
  private static final int FRAMEBUFFERS_EXT               = 5;
  private static final int LISTS                          = 6;
  private static final int OCCLUSION_QUERIES_NV           = 7;
  private static final int PROGRAM_AND_SHADER_OBJECTS_ARB = 8;
  private static final int PROGRAM_OBJECTS                = 9;
  private static final int PROGRAMS_ARB                   = 10;
  private static final int PROGRAMS_NV                    = 11;
  private static final int QUERIES                        = 12;
  private static final int QUERIES_ARB                    = 13;
  private static final int RENDERBUFFERS_EXT              = 14;
  private static final int SHADER_OBJECTS                 = 15;
  private static final int TEXTURES                       = 16;
  private static final int VERTEX_ARRAYS_APPLE            = 17;
  private static final int VERTEX_SHADERS_EXT             = 18;
  private static final int NUM_OBJECT_TYPES               = 19;

  static abstract class Deleter {
    public abstract void delete(GL gl, int obj);
  }

  static class ObjectList {
    private static final int MIN_CAPACITY = 4;

    private int size;
    private int capacity;
    private int[] data;
    private Deleter deleter;
    private String name;

    public ObjectList(Deleter deleter) {
      this.deleter = deleter;
      clear();
    }

    public void add(int obj) {
      if (size == capacity) {
        int newCapacity = 2 * capacity;
        int[] newData = new int[newCapacity];
        System.arraycopy(data, 0, newData, 0, size);
        data = newData;
        capacity = newCapacity;
      }

      data[size++] = obj;
    }

    public void addAll(ObjectList other) {
      if (other == null) {
        return;
      }
      for (int i = 0; i < other.size; i++) {
        add(other.data[i]);
      }
    }

    public boolean remove(int value) {
      for (int i = 0; i < size; i++) {
        if (data[i] == value) {
          if (i < size - 1) {
            System.arraycopy(data, i+1, data, i, size - i - 1);
          }
          --size;
          if ((size < capacity / 4) &&
              (capacity > MIN_CAPACITY)) {
            int newCapacity = capacity / 4;
            if (newCapacity < MIN_CAPACITY) {
              newCapacity = MIN_CAPACITY;
            }
            int[] newData = new int[newCapacity];
            System.arraycopy(data, 0, newData, 0, size);
            data = newData;
            capacity = newCapacity;
          }
          return true;
        }
      }
      return false;
    }

    public void setName(String name) {
      if (DEBUG) {
        this.name = name;
      }
    }

    public void delete(GL gl) {
      // Just in case we start throwing exceptions during deletion,
      // make sure we make progress rather than going into an infinite
      // loop
      while (size > 0) {
        int obj = data[size - 1];
        --size;
        if (DEBUG) {
          System.err.println("Deleting server-side OpenGL object " + obj +
                             ((name != null) ? (" (" + name + ")") : ""));
        }
        deleter.delete(gl, obj);
      }
    }

    public void clear() {
      size = 0;
      capacity = MIN_CAPACITY;
      data = new int[capacity];
    }
  }

  private ObjectList[] lists = new ObjectList[NUM_OBJECT_TYPES];
  private int refCount;
  private boolean dirty;

  private void add(ObjectList list, int n, IntBuffer ids) {
    int pos = ids.position();
    for (int i = 0; i < n; i++) {
      list.add(ids.get(pos + i));
    }
  }

  private void add(ObjectList list, int n, int[] ids, int ids_offset) {
    for (int i = 0; i < n; i++) {
      list.add(ids[i + ids_offset]);
    }
  }

  private void add(ObjectList list, int start, int n) {
    for (int i = 0; i < n; i++) {
      list.add(start + i);
    }
  }

  private void remove(ObjectList list, int n, IntBuffer ids) {
    int pos = ids.position();
    for (int i = 0; i < n; i++) {
      list.remove(ids.get(pos + i));
    }
  }

  private void remove(ObjectList list, int n, int[] ids, int ids_offset) {
    for (int i = 0; i < n; i++) {
      list.remove(ids[i + ids_offset]);
    }
  }

  private void remove(ObjectList list, int start, int n) {
    for (int i = 0; i < n; i++) {
      list.remove(start + i);
    }
  }

  private ObjectList getList(int which) {
    ObjectList list = lists[which];
    if (list == null) {
      Deleter deleter = null;
      String name = null;
      // Figure out which deleter we need
      switch (which) {
        case BUFFERS:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteBuffers(1, new int[] { obj }, 0);
              }
            };
          name = "buffer";
          break;
        case BUFFERS_ARB:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteBuffersARB(1, new int[] { obj }, 0);
              }
            };
          name = "ARB buffer";
          break;
        case FENCES_APPLE:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteFencesAPPLE(1, new int[] { obj }, 0);
              }
            };
          name = "APPLE fence";
          break;
        case FENCES_NV:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteFencesNV(1, new int[] { obj }, 0);
              }
            };
          name = "NV fence";
          break;
        case FRAGMENT_SHADERS_ATI:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteFragmentShaderATI(obj);
              }
            };
          name = "ATI fragment shader";
          break;
        case FRAMEBUFFERS_EXT:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteFramebuffersEXT(1, new int[] { obj }, 0);
              }
            };
          name = "EXT framebuffer";
          break;
        case LISTS:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteLists(obj, 1);
              }
            };
          name = "display list";
          break;
        case OCCLUSION_QUERIES_NV:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteOcclusionQueriesNV(1, new int[] { obj }, 0);
              }
            };
          name = "NV occlusion query";
          break;
        case PROGRAM_AND_SHADER_OBJECTS_ARB:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteObjectARB(obj);
              }
            };
          name = "ARB program or shader object";
          break;
        case PROGRAM_OBJECTS:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteProgram(obj);
              }
            };
          name = "program object";
          break;
        case PROGRAMS_ARB:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteProgramsARB(1, new int[] { obj }, 0);
              }
            };
          name = "ARB program object";
          break;
        case PROGRAMS_NV:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteProgramsNV(1, new int[] { obj }, 0);
              }
            };
          name = "NV program";
          break;
        case QUERIES:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteQueries(1, new int[] { obj }, 0);
              }
            };
          name = "query";
          break;
        case QUERIES_ARB:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteQueriesARB(1, new int[] { obj }, 0);
              }
            };
          name = "ARB query";
          break;
        case RENDERBUFFERS_EXT:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteRenderbuffersEXT(1, new int[] { obj }, 0);
              }
            };
          name = "EXT renderbuffer";
          break;
        case SHADER_OBJECTS:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteShader(obj);
              }
            };
          name = "shader object";
          break;
        case TEXTURES:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteTextures(1, new int[] { obj }, 0);
              }
            };
          name = "texture";
          break;
        case VERTEX_ARRAYS_APPLE:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteVertexArraysAPPLE(1, new int[] { obj }, 0);
              }
            };
          name = "APPLE vertex array";
          break;
        case VERTEX_SHADERS_EXT:
          deleter = new Deleter() {
              public void delete(GL gl, int obj) {
                gl.glDeleteVertexShaderEXT(obj);
              }
            };
          name = "EXT vertex shader";
          break;
        default:
          throw new InternalError("Unexpected OpenGL object type " + which);
      }

      list = new ObjectList(deleter);
      list.setName(name);
      lists[which] = list;
    }
    return list;
  }

  private void delete(GL gl) {
    for (int i = 0; i < lists.length; i++) {
      ObjectList list = lists[i];
      if (list != null) {
        list.delete(gl);
        lists[i] = null;
      }
    }
  }
}
