/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- * 
 * This software is published under the terms of the Apache Software License * 
 * version 1.1, a copy of which has been included with this distribution in  * 
 * the LICENSE file.                                                         * 
 *****************************************************************************/

package jogamp.graph.font.typecast.ot.table;

/**
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class Panose {

  private byte bFamilyType;
  private byte bSerifStyle;
  private byte bWeight;
  private byte bProportion;
  private byte bContrast;
  private byte bStrokeVariation;
  private byte bArmStyle;
  private byte bLetterform;
  private byte bMidline;
  private byte bXHeight;

  /** Creates new Panose */
  public Panose(byte[] panose) {
    bFamilyType = panose[0];
    bSerifStyle = panose[1];
    bWeight = panose[2];
    bProportion = panose[3];
    bContrast = panose[4];
    bStrokeVariation = panose[5];
    bArmStyle = panose[6];
    bLetterform = panose[7];
    bMidline = panose[8];
    bXHeight = panose[9];
  }

  public byte getFamilyType() {
    return bFamilyType;
  }
  
  public byte getSerifStyle() {
    return bSerifStyle;
  }
  
  public byte getWeight() {
    return bWeight;
  }

  public byte getProportion() {
    return bProportion;
  }
  
  public byte getContrast() {
    return bContrast;
  }
  
  public byte getStrokeVariation() {
    return bStrokeVariation;
  }
  
  public byte getArmStyle() {
    return bArmStyle;
  }
  
  public byte getLetterForm() {
    return bLetterform;
  }
  
  public byte getMidline() {
    return bMidline;
  }
  
  public byte getXHeight() {
    return bXHeight;
  }
  
  public String toString() {
    String sb = String.valueOf(bFamilyType) + " " +
            String.valueOf(bSerifStyle) + " " +
            String.valueOf(bWeight) + " " +
            String.valueOf(bProportion) + " " +
            String.valueOf(bContrast) + " " +
            String.valueOf(bStrokeVariation) + " " +
            String.valueOf(bArmStyle) + " " +
            String.valueOf(bLetterform) + " " +
            String.valueOf(bMidline) + " " +
            String.valueOf(bXHeight);
    return sb;
  }
}
