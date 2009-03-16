package com.sun.opengl.impl.glu.nurbs;

/**
 * Class replacing C language pointer
 * 
 * @author Tomas Hrasky
 * 
 */
public class CArrayOfQuiltspecs {
  /**
   * Underlaying array
   */
  private Quiltspec[] array;

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
  public CArrayOfQuiltspecs(Quiltspec[] array, int pointer) {
    this.array = array;
    this.pointer = pointer;
  }

  /**
   * Makes new CArray from other CArray
   * 
   * @param carray
   *            reference array
   */
  public CArrayOfQuiltspecs(CArrayOfQuiltspecs carray) {
    this.array = carray.array;
    this.pointer = carray.pointer;
  }

  /**
   * Makes new CArray with pointer set to 0
   * 
   * @param array
   *            underlaying array
   */
  public CArrayOfQuiltspecs(Quiltspec[] array) {
    this.array = array;
    this.pointer = 0;
  }

  /**
   * Returns element at pointer
   * 
   * @return element at pointer
   */
  public Quiltspec get() {
    return array[pointer];
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
  public void set(Quiltspec f) {
    array[pointer] = f;

  }

  /**
   * Returns array element at specified index
   * 
   * @param i
   *            array index
   * @return element at index
   */
  public Quiltspec get(int i) {
    return array[i];
  }

  /**
   * Lessens pointer by value
   * 
   * @param i
   *            lessen by
   */
  public void lessenPointerBy(int i) {
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
  public void setPointer(int pointer) {
    this.pointer = pointer;
  }

  /**
   * Raises pointer by value
   * 
   * @param i
   *            raise by
   */
  public void raisePointerBy(int i) {
    pointer += i;

  }

  /**
   * Lessens ponter by one (--)
   */
  public void mm() {
    pointer--;

  }

  /**
   * Returns underlaying array
   * 
   * @return underlaying array
   */
  public Quiltspec[] getArray() {
    return array;
  }

  /**
   * Sets underlaying array
   * 
   * @param array
   *            underlaying array
   */
  public void setArray(Quiltspec[] array) {
    this.array = array;
  }
}
