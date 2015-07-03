/*
 * Copyright (C) 1999-2001  Brian Paul   All Rights Reserved.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL
 * BRIAN PAUL BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

/*
 * Ported to GLES2.
 * Kristian Høgsberg <krh@bitplanet.net>
 * May 3, 2010
 * 
 * Improve GLES2 port:
 *   * Refactor gear drawing.
 *   * Use correct normals for surfaces.
 *   * Improve shader.
 *   * Use perspective projection transformation.
 *   * Add FPS count.
 *   * Add comments.
 * Alexandros Frantzis <alexandros.frantzis@linaro.org>
 * Jul 13, 2010
 */

#define GL_GLEXT_PROTOTYPES
#define EGL_EGLEXT_PROTOTYPES

#define _GNU_SOURCE

#include <math.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>
#include <assert.h>
#include <GLES2/gl2.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include "eglut.h"

static int demo_start_duration = 5000; // ms

#define OBJECT_VERTEX_STRIDE 3
#define OBJECT_COLOR_STRIDE 4

typedef GLfloat ObjectVertex[OBJECT_VERTEX_STRIDE];
typedef GLfloat ObjectColor[OBJECT_COLOR_STRIDE];

/**
 * Struct representing an object.
 */
struct object {
   ObjectVertex *vertices;
   int nvertices;
   ObjectColor *colors;
   int ncolors;

   GLuint vbo;
   GLuint cbo;
};

static struct object *obj;
/** The location of the shader uniforms */
static GLuint ModelViewProjectionMatrix_location;
/** The projection matrix */
static GLfloat ProjectionMatrix[16];

GLuint vertextCode=0, fragmentCode=0;
GLuint program = 0;

/** 
 * Fills an object vertex.
 * 
 * @param v the vertex to fill
 * @param x the x coordinate
 * @param y the y coordinate
 * @param z the z coortinate
 * 
 * @return the operation error code
 */
static ObjectVertex *
vert(ObjectVertex *v, GLfloat x, GLfloat y, GLfloat z)
{
   v[0][0] = x;
   v[0][1] = y;
   v[0][2] = z;

   return v + 1;
}

static ObjectColor *
color(ObjectColor *v, GLfloat r, GLfloat g, GLfloat b, GLfloat a)
{
   v[0][0] = r;
   v[0][1] = g;
   v[0][2] = b;
   v[0][3] = a;

   return v + 1;
}

static struct object *
create_object()
{
   ObjectVertex *v;
   ObjectColor *c;
   struct object *object;

   /* Allocate memory for the object */
   object = malloc(sizeof *object);
   if (object == NULL)
      return NULL;

   /* Allocate memory for the vertices */
   object->vertices = calloc(4, sizeof(*object->vertices));
   v = object->vertices;
   v = vert(v, -2,  2, 0);
   v = vert(v,  2,  2, 0);
   v = vert(v, -2, -2, 0);
   v = vert(v,  2, -2, 0);
   object->nvertices = (v - object->vertices);
   assert(4 == object->nvertices);

   object->colors = calloc(4, sizeof(*object->colors));
   c = object->colors;
   c = color(c, 1.0, 0.0, 0.0, 1.0);
   c = color(c, 0.0, 0.0, 1.0, 1.0);
   c = color(c, 1.0, 0.0, 0.0, 1.0);
   c = color(c, 1.0, 0.0, 0.0, 1.0);
   object->ncolors = (c - object->colors);
   assert(4 == object->ncolors);

   /* Store the vertices in a vertex buffer object (VBO) */
   glGenBuffers(1, &object->vbo);
   glBindBuffer(GL_ARRAY_BUFFER, object->vbo);
   glBufferData(GL_ARRAY_BUFFER, object->nvertices * sizeof(ObjectVertex),
         object->vertices, GL_STATIC_DRAW);
   glBindBuffer(GL_ARRAY_BUFFER, 0);

   glGenBuffers(1, &object->cbo);
   glBindBuffer(GL_ARRAY_BUFFER, object->cbo);
   glBufferData(GL_ARRAY_BUFFER, object->ncolors * sizeof(ObjectColor),
         object->colors, GL_STATIC_DRAW);
   glBindBuffer(GL_ARRAY_BUFFER, 0);

   return object;
}

static void destroy_object(struct object *object)
{
   glDeleteBuffers(1, &object->cbo);
   glDeleteBuffers(1, &object->vbo);
   free(object->colors);
   free(object->vertices);
   free(object);
}

/** 
 * Multiplies two 4x4 matrices.
 * 
 * The result is stored in matrix m.
 * 
 * @param m the first matrix to multiply
 * @param n the second matrix to multiply
 */
static void
multiply(GLfloat *m, const GLfloat *n)
{
   GLfloat tmp[16];
   const GLfloat *row, *column;
   div_t d;
   int i, j;

   for (i = 0; i < 16; i++) {
      tmp[i] = 0;
      d = div(i, 4);
      row = n + d.quot * 4;
      column = m + d.rem;
      for (j = 0; j < 4; j++)
         tmp[i] += row[j] * column[j * 4];
   }
   memcpy(m, &tmp, sizeof tmp);
}

/** 
 * Rotates a 4x4 matrix.
 * 
 * @param[in,out] m the matrix to rotate
 * @param angle the angle to rotate
 * @param x the x component of the direction to rotate to
 * @param y the y component of the direction to rotate to
 * @param z the z component of the direction to rotate to
 */
static void
rotate(GLfloat *m, GLfloat _angle, GLfloat x, GLfloat y, GLfloat z)
{
   double s, c;

   sincos(_angle, &s, &c);
   GLfloat r[16] = {
      x * x * (1 - c) + c,     y * x * (1 - c) + z * s, x * z * (1 - c) - y * s, 0,
      x * y * (1 - c) - z * s, y * y * (1 - c) + c,     y * z * (1 - c) + x * s, 0, 
      x * z * (1 - c) + y * s, y * z * (1 - c) - x * s, z * z * (1 - c) + c,     0,
      0, 0, 0, 1
   };

   multiply(m, r);
}


/** 
 * Translates a 4x4 matrix.
 * 
 * @param[in,out] m the matrix to translate
 * @param x the x component of the direction to translate to
 * @param y the y component of the direction to translate to
 * @param z the z component of the direction to translate to
 */
static void
translate(GLfloat *m, GLfloat x, GLfloat y, GLfloat z)
{
   GLfloat t[16] = { 1, 0, 0, 0,  0, 1, 0, 0,  0, 0, 1, 0,  x, y, z, 1 };

   multiply(m, t);
}

/** 
 * Creates an identity 4x4 matrix.
 * 
 * @param m the matrix make an identity matrix
 */
static void
identity(GLfloat *m)
{
   GLfloat t[16] = {
      1.0, 0.0, 0.0, 0.0,
      0.0, 1.0, 0.0, 0.0,
      0.0, 0.0, 1.0, 0.0,
      0.0, 0.0, 0.0, 1.0,
   };

   memcpy(m, t, sizeof(t));
}

/** 
 * Transposes a 4x4 matrix.
 *
 * @param m the matrix to transpose
 */
static void 
transpose(GLfloat *m)
{
   GLfloat t[16] = {
      m[0], m[4], m[8],  m[12],
      m[1], m[5], m[9],  m[13],
      m[2], m[6], m[10], m[14],
      m[3], m[7], m[11], m[15]};

   memcpy(m, t, sizeof(t));
}

/**
 * Inverts a 4x4 matrix.
 *
 * This function can currently handle only pure translation-rotation matrices.
 * Read http://www.gamedev.net/community/forums/topic.asp?topic_id=425118
 * for an explanation.
 */
static void
invert(GLfloat *m)
{
   GLfloat t[16];
   identity(t);

   // Extract and invert the translation part 't'. The inverse of a
   // translation matrix can be calculated by negating the translation
   // coordinates.
   t[12] = -m[12]; t[13] = -m[13]; t[14] = -m[14];

   // Invert the rotation part 'r'. The inverse of a rotation matrix is
   // equal to its transpose.
   m[12] = m[13] = m[14] = 0;
   transpose(m);

   // inv(m) = inv(r) * inv(t)
   multiply(m, t);
}

/** 
 * Calculate a perspective projection transformation.
 * 
 * @param m the matrix to save the transformation in
 * @param fovy the field of view in the y direction
 * @param aspect the view aspect ratio
 * @param zNear the near clipping plane
 * @param zFar the far clipping plane
 */
void perspective(GLfloat *m, GLfloat fovy, GLfloat aspect, GLfloat zNear, GLfloat zFar)
{
   GLfloat tmp[16];
   identity(tmp);

   double sine, cosine, cotangent, deltaZ;
   GLfloat radians = fovy / 2 * M_PI / 180;

   deltaZ = zFar - zNear;
   sincos(radians, &sine, &cosine);

   if ((deltaZ == 0) || (sine == 0) || (aspect == 0))
      return;

   cotangent = cosine / sine;

   tmp[0] = cotangent / aspect;
   tmp[5] = cotangent;
   tmp[10] = -(zFar + zNear) / deltaZ;
   tmp[11] = -1;
   tmp[14] = -2 * zNear * zFar / deltaZ;
   tmp[15] = 0;

   memcpy(m, tmp, sizeof(tmp));
}

/**
 * Draws
 *
 * @param transform the current transformation matrix
 * @param x the x position to draw the gear at
 * @param y the y position to draw the gear at
 * @param _angle the rotation angle
 */
static void
draw_object(GLfloat *transform)
{
   GLfloat model_view[16];
   GLfloat model_view_projection[16];
   int tms = eglutGet(EGLUT_ELAPSED_TIME);
   GLfloat angle = ( tms * 360.0 ) / 4000.0;
   GLfloat grad = 2 * M_PI * angle / 360.0;

   // fprintf(stderr, "td %d, angle %f\n", tms, angle);

   /* Translate and rotate the gear */
   memcpy(model_view, transform, sizeof (model_view));
   translate(model_view, 0, 0, -10);
   rotate(model_view, grad, 0.0, 0.0, 1.0);
   rotate(model_view, grad, 0.0, 1.0, 0.0);

   /* Create and set the ModelViewProjectionMatrix */
   memcpy(model_view_projection, ProjectionMatrix, sizeof(model_view_projection));
   multiply(model_view_projection, model_view);

   glUniformMatrix4fv(ModelViewProjectionMatrix_location, 1, GL_FALSE,
                      model_view_projection);

   /* Set the vertex buffer object to use */
   glBindBuffer(GL_ARRAY_BUFFER, obj->vbo);
   glVertexAttribPointer(0, 3, GL_FLOAT, GL_FALSE,
         3 * sizeof(GLfloat), NULL);
   glEnableVertexAttribArray(0);

   glBindBuffer(GL_ARRAY_BUFFER, obj->cbo);
   glVertexAttribPointer(1, 4, GL_FLOAT, GL_FALSE,
         4 * sizeof(GLfloat), NULL);
   glEnableVertexAttribArray(1);

   glDrawArrays(GL_TRIANGLE_STRIP, 0, 4);

   /* Disable the attributes */
   glDisableVertexAttribArray(1);
   glDisableVertexAttribArray(0);
   glBindBuffer(GL_ARRAY_BUFFER, 0);
}

/** 
 * Draws the object.
 */
static void
object_draw(void)
{
   const static GLfloat red[4] = { 0.8, 0.1, 0.0, 1.0 };
   const static GLfloat green[4] = { 0.0, 0.8, 0.2, 1.0 };
   const static GLfloat blue[4] = { 0.2, 0.2, 1.0, 1.0 };
   GLfloat transform[16];
   identity(transform);

   glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
   glUseProgram(program);

   /* Draw the object */
   draw_object(transform);

   glUseProgram(0);
}

/** 
 * Handles a new window size or exposure.
 * 
 * @param width the window width
 * @param height the window height
 */
static void
object_reshape(int width, int height)
{
   /* Update the projection matrix */
   perspective(ProjectionMatrix, 45.0, (float)width / (float)height, 1.0, 100.0);

   /* Set the viewport */
   glViewport(0, 0, (GLint) width, (GLint) height);

   glClearColor(0.0, 0.0, 0.0, 1.0);
}

/** 
 * Handles special eglut events.
 * 
 * @param special the event to handle.
 */
static void
object_special(int special)
{
   switch (special) {
      case EGLUT_KEY_LEFT:
         break;
      case EGLUT_KEY_RIGHT:
         break;
      case EGLUT_KEY_UP:
         break;
      case EGLUT_KEY_DOWN:
         break;
   }
}

static void
object_idle(void)
{
   static int frames = 0;
   static double tRate0 = -1.0;
   int tms = eglutGet(EGLUT_ELAPSED_TIME);
   double t = tms / 1000.0;

   if(tms>demo_start_duration) {
        eglutStopMainLoop();
        return;
   }

   eglutPostRedisplay();
   frames++;

   if (tRate0 < 0.0)
      tRate0 = t;
   if (t - tRate0 >= 1.0) {
      GLfloat seconds = t - tRate0;
      GLfloat fps = frames / seconds;
      printf("%d frames in %3.1f seconds = %6.3f FPS\n", frames, seconds,
            fps);
      tRate0 = t;
      frames = 0;
   }
}

static const char vertex_shader[] =
                " #ifdef GL_ES\n" 
                "  precision mediump float;\n" 
                "  precision mediump int;\n" 
                "#endif\n" 
                "\n" 
                "uniform mat4    mgl_PMVMatrix;\n" 
                "attribute vec3    mgl_Vertex;\n" 
                "attribute vec4    mgl_Color;\n" 
                "varying vec4    frontColor;\n" 
                "\n" 
                "void main(void)\n" 
                "{\n" 
                "  frontColor=mgl_Color;\n" 
                "  gl_Position = mgl_PMVMatrix * vec4(mgl_Vertex, 1.0);\n" 
                "}\n" ;

static const char fragment_shader[] =
                " #ifdef GL_ES\n" 
                "  precision mediump float;\n" 
                "  precision mediump int;\n" 
                "#endif\n" 
                "\n" 
                "varying   vec4    frontColor;\n" 
                "\n" 
                "void main (void)\n" 
                "{\n" 
                "    gl_FragColor = frontColor;\n" 
                "}\n" ;


static void
object_init(void)
{
   const char *p;
   char msg[512];

   glEnable(GL_DEPTH_TEST);

   /* Compile the vertex shader */
   p = vertex_shader;
   vertextCode = glCreateShader(GL_VERTEX_SHADER);
   glShaderSource(vertextCode, 1, &p, NULL);
   glCompileShader(vertextCode);
   glGetShaderInfoLog(vertextCode, sizeof msg, NULL, msg);
   printf("vertex shader info: %s\n", msg);

   /* Compile the fragment shader */
   p = fragment_shader;
   fragmentCode = glCreateShader(GL_FRAGMENT_SHADER);
   glShaderSource(fragmentCode, 1, &p, NULL);
   glCompileShader(fragmentCode);
   glGetShaderInfoLog(fragmentCode, sizeof msg, NULL, msg);
   printf("fragment shader info: %s\n", msg);

   /* Create and link the shader program */
   program = glCreateProgram();
   glAttachShader(program, vertextCode);
   glAttachShader(program, fragmentCode);
   glBindAttribLocation(program, 0, "mgl_Vertex");
   glBindAttribLocation(program, 1, "mgl_Color");

   glLinkProgram(program);
   glGetProgramInfoLog(program, sizeof msg, NULL, msg);
   printf("info: %s\n", msg);

   /* Enable the shaders */
   glUseProgram(program);

   /* Get the locations of the uniforms so we can access them */
   ModelViewProjectionMatrix_location = glGetUniformLocation(program, "mgl_PMVMatrix");

   /* make the object */
   obj = create_object();

   glUseProgram(0);
   // eglutSwapInterval(1);
}

static void
object_release(void)
{
   destroy_object(obj);
   obj = NULL;

   glDetachShader(program, vertextCode);
   glDeleteShader(vertextCode);
   glDetachShader(program, fragmentCode);
   glDeleteShader(fragmentCode);
   glDeleteProgram(program);
}

int
main(int argc, char *argv[])
{
   int demo_loops = 1;
   int i;
   for (i = 1; i < argc; i++) {
      if (strcmp(argv[i], "-time") == 0) {
         demo_start_duration = atoi(argv[++i]);
      } else if (strcmp(argv[i], "-loops") == 0) {
         demo_loops = atoi(argv[++i]);
      }
   }
   fprintf(stderr, "duration: %d\n", demo_start_duration);
   fprintf(stderr, "loops: %d\n", demo_loops);

   for(i=0; i<demo_loops; i++) {
       fprintf(stderr, "Loop: %d/%d\n", i, demo_loops);

       /* Initialize the window */
       eglutInitWindowSize(512, 512);
       eglutInitAPIMask(EGLUT_OPENGL_ES2_BIT);
       eglutInit(argc, argv);

       int winid = eglutCreateWindow("es2object");

       /* Set up eglut callback functions */
       eglutIdleFunc(object_idle);
       eglutReshapeFunc(object_reshape);
       eglutDisplayFunc(object_draw);
       eglutSpecialFunc(object_special);

       /* Initialize the object */
       object_init();

       eglutMainLoop();

       object_release();

       eglutDestroyWindow(winid);
       eglutTerminate();
   }

   return 0;
}
