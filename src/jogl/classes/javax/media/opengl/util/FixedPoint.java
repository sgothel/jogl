
package javax.media.opengl.util;

public class FixedPoint {
  public static final int toFixed(int value) {
    if (value < -32768) value = -32768;
    if (value > 32767) value = 32767;
    return value * 65536;
  }

  public static final int toFixed(float value) {
    if (value < -32768) value = -32768;
    if (value > 32767) value = 32767;
    return (int)(value * 65536.0f);
  }

  public static final float toFloat(int value) {
    return (float)value/65536.0f;
  }

  public static final int mult(int x1, int x2) {
    return (int) ( ((long)x1*(long)x2)/65536 );
  }

  public static final int div(int x1, int x2) {
    return (int) ( (((long)x1)<<16)/x2 );
  }
}

