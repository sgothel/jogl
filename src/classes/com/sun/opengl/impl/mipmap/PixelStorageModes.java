/*
 * License Applicability. Except to the extent portions of this file are
 * made subject to an alternative license as permitted in the SGI Free
 * Software License B, Version 1.1 (the "License"), the contents of this
 * file are subject only to the provisions of the License. You may not use
 * this file except in compliance with the License. You may obtain a copy
 * of the License at Silicon Graphics, Inc., attn: Legal Services, 1600
 * Amphitheatre Parkway, Mountain View, CA 94043-1351, or at:
 * 
 * http://oss.sgi.com/projects/FreeB
 * 
 * Note that, as provided in the License, the Software is distributed on an
 * "AS IS" basis, with ALL EXPRESS AND IMPLIED WARRANTIES AND CONDITIONS
 * DISCLAIMED, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES AND
 * CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A
 * PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
 * 
 * NOTE:  The Original Code (as defined below) has been licensed to Sun
 * Microsystems, Inc. ("Sun") under the SGI Free Software License B
 * (Version 1.1), shown above ("SGI License").   Pursuant to Section
 * 3.2(3) of the SGI License, Sun is distributing the Covered Code to
 * you under an alternative license ("Alternative License").  This
 * Alternative License includes all of the provisions of the SGI License
 * except that Section 2.2 and 11 are omitted.  Any differences between
 * the Alternative License and the SGI License are offered solely by Sun
 * and not by SGI.
 *
 * Original Code. The Original Code is: OpenGL Sample Implementation,
 * Version 1.2.1, released January 26, 2000, developed by Silicon Graphics,
 * Inc. The Original Code is Copyright (c) 1991-2000 Silicon Graphics, Inc.
 * Copyright in any portions created by third parties is as indicated
 * elsewhere herein. All Rights Reserved.
 * 
 * Additional Notice Provisions: The application programming interfaces
 * established by SGI in conjunction with the Original Code are The
 * OpenGL(R) Graphics System: A Specification (Version 1.2.1), released
 * April 1, 1999; The OpenGL(R) Graphics System Utility Library (Version
 * 1.3), released November 4, 1998; and OpenGL(R) Graphics with the X
 * Window System(R) (Version 1.3), released October 19, 1998. This software
 * was created using the OpenGL(R) version 1.2.1 Sample Implementation
 * published by SGI, but has not been independently verified as being
 * compliant with the OpenGL(R) version 1.2.1 Specification.
 */

package com.sun.opengl.impl.mipmap;

/**
 *
 * @author  Administrator
 */
public class PixelStorageModes {

  /**
   * Holds value of property packAlignment.
   */
  private int packAlignment;

  /**
   * Holds value of property packRowLength.
   */
  private int packRowLength;

  /**
   * Holds value of property packSkipRows.
   */
  private int packSkipRows;

  /**
   * Holds value of property packSkipPixels.
   */
  private int packSkipPixels;

  /**
   * Holds value of property packLsbFirst.
   */
  private boolean packLsbFirst;

  /**
   * Holds value of property packSwapBytes.
   */
  private boolean packSwapBytes;

  /**
   * Holds value of property packSkipImages.
   */
  private int packSkipImages;

  /**
   * Holds value of property packImageHeight.
   */
  private int packImageHeight;

  /**
   * Holds value of property unpackAlignment.
   */
  private int unpackAlignment;

  /**
   * Holds value of property unpackRowLength.
   */
  private int unpackRowLength;

  /**
   * Holds value of property unpackSkipRows.
   */
  private int unpackSkipRows;

  /**
   * Holds value of property unpackSkipPixels.
   */
  private int unpackSkipPixels;

  /**
   * Holds value of property unpackLsbFirst.
   */
  private boolean unpackLsbFirst;

  /**
   * Holds value of property unpackSwapBytes.
   */
  private boolean unpackSwapBytes;

  /**
   * Holds value of property unpackSkipImages.
   */
  private int unpackSkipImages;

  /**
   * Holds value of property unpackImageHeight.
   */
  private int unpackImageHeight;
  
  /** Creates a new instance of PixelStorageModes */
  public PixelStorageModes() {
  }

  /**
   * Getter for property packAlignment.
   * @return Value of property packAlignment.
   */
  public int getPackAlignment() {

    return this.packAlignment;
  }

  /**
   * Setter for property packAlignment.
   * @param packAlignment New value of property packAlignment.
   */
  public void setPackAlignment(int packAlignment) {

    this.packAlignment = packAlignment;
  }

  /**
   * Getter for property packRowLength.
   * @return Value of property packRowLength.
   */
  public int getPackRowLength() {

    return this.packRowLength;
  }

  /**
   * Setter for property packRowLength.
   * @param packRowLength New value of property packRowLength.
   */
  public void setPackRowLength(int packRowLength) {

    this.packRowLength = packRowLength;
  }

  /**
   * Getter for property packSkipRows.
   * @return Value of property packSkipRows.
   */
  public int getPackSkipRows() {

    return this.packSkipRows;
  }

  /**
   * Setter for property packSkipRows.
   * @param packSkipRows New value of property packSkipRows.
   */
  public void setPackSkipRows(int packSkipRows) {

    this.packSkipRows = packSkipRows;
  }

  /**
   * Getter for property packSkipPixels.
   * @return Value of property packSkipPixels.
   */
  public int getPackSkipPixels() {

    return this.packSkipPixels;
  }

  /**
   * Setter for property packSkipPixels.
   * @param packSkipPixels New value of property packSkipPixels.
   */
  public void setPackSkipPixels(int packSkipPixels) {

    this.packSkipPixels = packSkipPixels;
  }

  /**
   * Getter for property packLsbFirst.
   * @return Value of property packLsbFirst.
   */
  public boolean getPackLsbFirst() {

    return this.packLsbFirst;
  }

  /**
   * Setter for property packLsbFirst.
   * @param packLsbFirst New value of property packLsbFirst.
   */
  public void setPackLsbFirst(boolean packLsbFirst) {

    this.packLsbFirst = packLsbFirst;
  }

  /**
   * Getter for property packSwapBytes.
   * @return Value of property packSwapBytes.
   */
  public boolean getPackSwapBytes() {

    return this.packSwapBytes;
  }

  /**
   * Setter for property packSwapBytes.
   * @param packSwapBytes New value of property packSwapBytes.
   */
  public void setPackSwapBytes(boolean packSwapBytes) {

    this.packSwapBytes = packSwapBytes;
  }

  /**
   * Getter for property packSkipImages.
   * @return Value of property packSkipImages.
   */
  public int getPackSkipImages() {

    return this.packSkipImages;
  }

  /**
   * Setter for property packSkipImages.
   * @param packSkipImages New value of property packSkipImages.
   */
  public void setPackSkipImages(int packSkipImages) {

    this.packSkipImages = packSkipImages;
  }

  /**
   * Getter for property packImageHeight.
   * @return Value of property packImageHeight.
   */
  public int getPackImageHeight() {

    return this.packImageHeight;
  }

  /**
   * Setter for property packImageHeight.
   * @param packImageHeight New value of property packImageHeight.
   */
  public void setPackImageHeight(int packImageHeight) {

    this.packImageHeight = packImageHeight;
  }

  /**
   * Getter for property unpackAlignment.
   * @return Value of property unpackAlignment.
   */
  public int getUnpackAlignment() {

    return this.unpackAlignment;
  }

  /**
   * Setter for property unpackAlignment.
   * @param unpackAlignment New value of property unpackAlignment.
   */
  public void setUnpackAlignment(int unpackAlignment) {

    this.unpackAlignment = unpackAlignment;
  }

  /**
   * Getter for property unpackRowLength.
   * @return Value of property unpackRowLength.
   */
  public int getUnpackRowLength() {

    return this.unpackRowLength;
  }

  /**
   * Setter for property unpackRowLength.
   * @param unpackRowLength New value of property unpackRowLength.
   */
  public void setUnpackRowLength(int unpackRowLength) {

    this.unpackRowLength = unpackRowLength;
  }

  /**
   * Getter for property unpackSkipRows.
   * @return Value of property unpackSkipRows.
   */
  public int getUnpackSkipRows() {

    return this.unpackSkipRows;
  }

  /**
   * Setter for property unpackSkipRows.
   * @param unpackSkipRows New value of property unpackSkipRows.
   */
  public void setUnpackSkipRows(int unpackSkipRows) {

    this.unpackSkipRows = unpackSkipRows;
  }

  /**
   * Getter for property unpackSkipPixels.
   * @return Value of property unpackSkipPixels.
   */
  public int getUnpackSkipPixels() {

    return this.unpackSkipPixels;
  }

  /**
   * Setter for property unpackSkipPixels.
   * @param unpackSkipPixels New value of property unpackSkipPixels.
   */
  public void setUnpackSkipPixels(int unpackSkipPixels) {

    this.unpackSkipPixels = unpackSkipPixels;
  }

  /**
   * Getter for property unpackLsbFirst.
   * @return Value of property unpackLsbFirst.
   */
  public boolean getUnpackLsbFirst() {

    return this.unpackLsbFirst;
  }

  /**
   * Setter for property unpackLsbFirst.
   * @param unpackLsbFirst New value of property unpackLsbFirst.
   */
  public void setUnpackLsbFirst(boolean unpackLsbFirst) {

    this.unpackLsbFirst = unpackLsbFirst;
  }

  /**
   * Getter for property unpackSwapBytes.
   * @return Value of property unpackSwapBytes.
   */
  public boolean getUnpackSwapBytes() {

    return this.unpackSwapBytes;
  }

  /**
   * Setter for property unpackSwapBytes.
   * @param unpackSwapBytes New value of property unpackSwapBytes.
   */
  public void setUnpackSwapBytes(boolean unpackSwapBytes) {

    this.unpackSwapBytes = unpackSwapBytes;
  }

  /**
   * Getter for property unpackSkipImages.
   * @return Value of property unpackSkipImages.
   */
  public int getUnpackSkipImages() {

    return this.unpackSkipImages;
  }

  /**
   * Setter for property unpackSkipImages.
   * @param unpackSkipImages New value of property unpackSkipImages.
   */
  public void setUnpackSkipImages(int unpackSkipImages) {

    this.unpackSkipImages = unpackSkipImages;
  }

  /**
   * Getter for property unpackImageHeight.
   * @return Value of property unpackImageHeight.
   */
  public int getUnpackImageHeight() {

    return this.unpackImageHeight;
  }

  /**
   * Setter for property unpackImageHeight.
   * @param unpackImageHeight New value of property unpackImageHeight.
   */
  public void setUnpackImageHeight(int unpackImageHeight) {

    this.unpackImageHeight = unpackImageHeight;
  }
  
  
}
