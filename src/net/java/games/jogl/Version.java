/*
* Copyright (c) 2004 Sun Microsystems, Inc. All  Rights Reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*
* -Redistribution of source code must retain the above copyright notice,
* this list of conditions and the following disclaimer.
*
* -Redistribution in binary form must reproduce the above copyright notice,
* this list of conditions and the following disclaimer in the documentation
* and/or other materials provided with the distribution.
*
* Neither the name of Sun Microsystems, Inc. or the names of contributors may
* be used to endorse or promote products derived from this software without
* specific prior written permission.
*
* This software is provided "AS IS," without a warranty of any kind.
* ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
* ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
* NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS
* LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A
* RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
* IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT
* OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
* PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
* ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS
* BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
*
* You acknowledge that this software is not designed or intended for use in the
* design, construction, operation or maintenance of any nuclear facility.
*/

package net.java.games.jogl;

/**
 * The version and build number of this implementation.
 * Version numbers for a release are of the form: w.x.y[-a]-z, where:
 * <ul>
 *   <li>
 *     w - the major version number of the release.  This number should
 *         start at 1.  Typically, a bump in the major version number
 *         signifies that the release breaks backwards compatibility
 *         with some older release.
 *   </li>
 *   <li>
 *     x - minor version number.  This number starts at 0.  A bump in
 *         the minor version number signifies a release that has significant
 *         new functionality.
 *   </li>
 *   <li>
 *     y - minor-minor version number number.  This number starts at 0.  A
 *         bump in the minor-minor version number signifies that new bug
 *         fixes have been  added to the build.
 *  </li>
 *  <li>
 *    a - an optional build designator followed by a digit.  Valid build
 *        designators are:
 *    <ul>
 *      <li>alpha</li>
 *      <li>beta</li>
 *    </ul>
 *  </li>
 *  <li>
 *    z - build number.  This is used to specify the build number of the
 *        release.  This is usually only important to people that use
 *        the daily build of a project.  The format is the lower-case
 *        letter 'b' followed by a two digit number.
 *  </li>
 *  </ul>
 *
 * For example, the following are all valid version strings:
 * <ul>
 *   <li>1.1.2-b02</li>
 *   <li>1.3.5-alpha1-b19</li>
 *   <li>4.7.1-beta3-b20</li>
 * </ul>
 *
 */
public final class Version {

  /**
   * Private constructor - no need for user to create
   * an instance of this class.
   */
  private Version() {
  }

  /**
   * Version string of this build.
   */
  private static final String version = "1.1.0";

  /**
   * Returns the version string and build number of
   * this implementation.  See the class description
   * for the version string format. 
   *
   * @return The version string of this implementation.
   */
  public static String getVersion() {
    return version;
  }
}
