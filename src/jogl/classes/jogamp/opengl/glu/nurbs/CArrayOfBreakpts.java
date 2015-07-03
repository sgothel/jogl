package jogamp.opengl.glu.nurbs;

/**
 * Class replacing C language pointer
 *
 * @author Tomas Hrasky
 *
 */
public class CArrayOfBreakpts {
  /**
   * Underlaying array
   */
  private final Breakpt[] pole;

  /**
   * Pointer to array member
   */
  private int pointer;

  /**
   * Makes new CArray
   *
   * @param array
   *            underlaying array
   * @param pointer
   *            pointer (index) to array
   */
  public CArrayOfBreakpts(final Breakpt[] array, final int pointer) {
    this.pole = array;
    this.pointer = pointer;
  }

  /**
   * Makes new CArray from other CArray
   *
   * @param carray
   *            reference array
   */
  public CArrayOfBreakpts(final CArrayOfBreakpts carray) {
    this.pole = carray.pole;
    this.pointer = carray.pointer;
  }

  /**
   * Returns element at pointer
   *
   * @return element at pointer
   */
  public Breakpt get() {
    return pole[pointer];
  }

  /**
   * Increases pointer by one (++)
   */
  public void pp() {
    pointer++;
  }

  /**
   * Sets element at pointer
   *
   * @param f
   *            desired value
   */
  public void set(final Breakpt f) {
    pole[pointer] = f;

  }

  /**
   * Returns array element at specified index
   *
   * @param i
   *            array index
   * @return element at index
   */
  public Breakpt get(final int i) {
    return pole[i];
  }

  /**
   * Lessens pointer by value
   *
   * @param i
   *            lessen by
   */
  public void lessenPointerBy(final int i) {
    pointer -= i;

  }

  /**
   * Returns pointer value
   *
   * @return pointer value
   */
  public int getPointer() {
    return pointer;
  }

  /**
   * Sets ponter value
   *
   * @param pointer
   *            pointer value to be set
   */
  public void setPointer(final int pointer) {
    this.pointer = pointer;
  }

  /**
   * Raises pointer by value
   *
   * @param i
   *            raise by
   */
  public void raisePointerBy(final int i) {
    pointer += i;

  }

  /**
   * Lessens ponter by one (--)
   */
  public void mm() {
    pointer--;

  }
}
