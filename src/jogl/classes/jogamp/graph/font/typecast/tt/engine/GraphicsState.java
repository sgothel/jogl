/*
 * $Id: GraphicsState.java,v 1.1.1.1 2004-12-05 23:15:01 davidsch Exp $
 *
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004 David Schweinsberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jogamp.graph.font.typecast.tt.engine;

/**
 * Maintains the graphics state whilst interpreting hinting instructions.
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: GraphicsState.java,v 1.1.1.1 2004-12-05 23:15:01 davidsch Exp $
 */
class GraphicsState {

    public boolean auto_flip = true;
    public int control_value_cut_in = 0;
    public int delta_base = 9;
    public int delta_shift = 3;
    public int dual_projection_vectors;
    public int[] freedom_vector = new int[2];
    public int zp0 = 1;
    public int zp1 = 1;
    public int zp2 = 1;
    public int instruction_control = 0;
    public int loop = 1;
    public int minimum_distance = 1;
    public int[] projection_vector = new int[2];
    public int round_state = 1;
    public int rp0 = 0;
    public int rp1 = 0;
    public int rp2 = 0;
    public int scan_control = 0;
    public int single_width_cut_in = 0;
    public int single_width_value = 0;
}
