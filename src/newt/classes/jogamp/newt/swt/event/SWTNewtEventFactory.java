/**
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package jogamp.newt.swt.event;

import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeSurfaceHolder;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.MouseEvent;

/**
 * SWT event translator to NEWT, inclusive dispatch listener.
 * <p>
 * <b>Disclaimer:</b> This code is merely tested and subject to change.
 * </p>
 */
public class SWTNewtEventFactory {

    public static final short eventTypeSWT2NEWT(final int swtType) {
        switch( swtType ) {
            // case SWT.MouseXXX: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_CLICKED;
            case SWT.MouseDown: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_PRESSED;
            case SWT.MouseUp: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED;
            case SWT.MouseMove: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_MOVED;
            case SWT.MouseEnter: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_ENTERED;
            case SWT.MouseExit: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_EXITED;
            // case SWT.MouseXXX: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_DRAGGED;
            case SWT.MouseVerticalWheel: return com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_WHEEL_MOVED;

            case SWT.KeyDown: return com.jogamp.newt.event.KeyEvent.EVENT_KEY_PRESSED;
            case SWT.KeyUp: return com.jogamp.newt.event.KeyEvent.EVENT_KEY_RELEASED;
        }
        return (short)0;
    }

    public static final int swtModifiers2Newt(final int awtMods, final boolean mouseHint) {
        int newtMods = 0;
        if ((awtMods & SWT.SHIFT) != 0)     newtMods |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
        if ((awtMods & SWT.CTRL) != 0)      newtMods |= com.jogamp.newt.event.InputEvent.CTRL_MASK;
        if ((awtMods & SWT.ALT) != 0)      newtMods |= com.jogamp.newt.event.InputEvent.ALT_MASK;
        return newtMods;
    }

    public static short swtKeyCode2NewtKeyCode(final int swtKeyCode) {
        final short defNEWTKeyCode = (short)swtKeyCode;
        switch (swtKeyCode) {
            case SWT.HOME          : return com.jogamp.newt.event.KeyEvent.VK_HOME;
            case SWT.END           : return com.jogamp.newt.event.KeyEvent.VK_END;
            case SWT.PRINT_SCREEN  : return com.jogamp.newt.event.KeyEvent.VK_PRINTSCREEN;
            case SWT.BS            : return com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE;
            case SWT.TAB           : return com.jogamp.newt.event.KeyEvent.VK_TAB;
            case SWT.LF            : return com.jogamp.newt.event.KeyEvent.VK_ENTER;
            case SWT.PAGE_DOWN     : return com.jogamp.newt.event.KeyEvent.VK_PAGE_DOWN;
            case SWT.PAGE_UP       : return com.jogamp.newt.event.KeyEvent.VK_PAGE_UP;
            case SWT.CONTROL       : return com.jogamp.newt.event.KeyEvent.VK_CONTROL;
            case SWT.CAPS_LOCK     : return com.jogamp.newt.event.KeyEvent.VK_CAPS_LOCK;
            case SWT.PAUSE         : return com.jogamp.newt.event.KeyEvent.VK_PAUSE;
            case SWT.SCROLL_LOCK   : return com.jogamp.newt.event.KeyEvent.VK_SCROLL_LOCK;
            case SWT.CANCEL        : return com.jogamp.newt.event.KeyEvent.VK_CANCEL;
            case SWT.INSERT        : return com.jogamp.newt.event.KeyEvent.VK_INSERT;
            case SWT.ESC           : return com.jogamp.newt.event.KeyEvent.VK_ESCAPE;
            case SWT.SPACE         : return com.jogamp.newt.event.KeyEvent.VK_SPACE;
            case SWT.F1            : return com.jogamp.newt.event.KeyEvent.VK_F1;
            case SWT.F2            : return com.jogamp.newt.event.KeyEvent.VK_F2;
            case SWT.F3            : return com.jogamp.newt.event.KeyEvent.VK_F3;
            case SWT.F4            : return com.jogamp.newt.event.KeyEvent.VK_F4;
            case SWT.F5            : return com.jogamp.newt.event.KeyEvent.VK_F5;
            case SWT.F6            : return com.jogamp.newt.event.KeyEvent.VK_F6;
            case SWT.F7            : return com.jogamp.newt.event.KeyEvent.VK_F7;
            case SWT.F8            : return com.jogamp.newt.event.KeyEvent.VK_F8;
            case SWT.F9            : return com.jogamp.newt.event.KeyEvent.VK_F9;
            case SWT.F10           : return com.jogamp.newt.event.KeyEvent.VK_F10;
            case SWT.F11           : return com.jogamp.newt.event.KeyEvent.VK_F11;
            case SWT.F12           : return com.jogamp.newt.event.KeyEvent.VK_F12;
            case SWT.F13           : return com.jogamp.newt.event.KeyEvent.VK_F13;
            case SWT.F14           : return com.jogamp.newt.event.KeyEvent.VK_F14;
            case SWT.F15           : return com.jogamp.newt.event.KeyEvent.VK_F15;
            case SWT.F16           : return com.jogamp.newt.event.KeyEvent.VK_F16;
            case SWT.F17           : return com.jogamp.newt.event.KeyEvent.VK_F17;
            case SWT.F18           : return com.jogamp.newt.event.KeyEvent.VK_F18;
            case SWT.F19           : return com.jogamp.newt.event.KeyEvent.VK_F19;
            case SWT.F20           : return com.jogamp.newt.event.KeyEvent.VK_F20;
            case SWT.DEL           : return com.jogamp.newt.event.KeyEvent.VK_DELETE;
            case SWT.KEYPAD_0      : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD0;
            case SWT.KEYPAD_1      : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD1;
            case SWT.KEYPAD_2      : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD2;
            case SWT.KEYPAD_3      : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD3;
            case SWT.KEYPAD_4      : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD4;
            case SWT.KEYPAD_5      : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD5;
            case SWT.KEYPAD_6      : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD6;
            case SWT.KEYPAD_7      : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD7;
            case SWT.KEYPAD_8      : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD8;
            case SWT.KEYPAD_9      : return com.jogamp.newt.event.KeyEvent.VK_NUMPAD9;
            case SWT.KEYPAD_DECIMAL: return com.jogamp.newt.event.KeyEvent.VK_DECIMAL;
            case SWT.KEYPAD_ADD    : return com.jogamp.newt.event.KeyEvent.VK_ADD;
            case SWT.KEYPAD_SUBTRACT: return com.jogamp.newt.event.KeyEvent.VK_SUBTRACT;
            case SWT.KEYPAD_MULTIPLY: return com.jogamp.newt.event.KeyEvent.VK_MULTIPLY;
            case SWT.KEYPAD_DIVIDE : return com.jogamp.newt.event.KeyEvent.VK_DIVIDE;
            case SWT.NUM_LOCK      : return com.jogamp.newt.event.KeyEvent.VK_NUM_LOCK;
            case SWT.ARROW_LEFT    : return com.jogamp.newt.event.KeyEvent.VK_LEFT;
            case SWT.ARROW_UP      : return com.jogamp.newt.event.KeyEvent.VK_UP;
            case SWT.ARROW_RIGHT   : return com.jogamp.newt.event.KeyEvent.VK_RIGHT;
            case SWT.ARROW_DOWN    : return com.jogamp.newt.event.KeyEvent.VK_DOWN;
            case SWT.HELP          : return com.jogamp.newt.event.KeyEvent.VK_HELP;
        }
        return defNEWTKeyCode;
    }

    public static int newtKeyCode2SWTKeyCode(final short newtKeyCode) {
        final int defSWTKeyCode = 0xFFFF & newtKeyCode;
        switch (newtKeyCode) {
            case com.jogamp.newt.event.KeyEvent.VK_HOME          : return SWT.HOME;
            case com.jogamp.newt.event.KeyEvent.VK_END           : return SWT.END;
            case com.jogamp.newt.event.KeyEvent.VK_PRINTSCREEN   : return SWT.PRINT_SCREEN;
            case com.jogamp.newt.event.KeyEvent.VK_BACK_SPACE    : return SWT.BS;
            case com.jogamp.newt.event.KeyEvent.VK_TAB           : return SWT.TAB;
            case com.jogamp.newt.event.KeyEvent.VK_ENTER         : return SWT.LF;
            case com.jogamp.newt.event.KeyEvent.VK_PAGE_DOWN     : return SWT.PAGE_DOWN;
            case com.jogamp.newt.event.KeyEvent.VK_PAGE_UP       : return SWT.PAGE_UP;
            case com.jogamp.newt.event.KeyEvent.VK_CONTROL       : return SWT.CONTROL;
            case com.jogamp.newt.event.KeyEvent.VK_CAPS_LOCK     : return SWT.CAPS_LOCK;
            case com.jogamp.newt.event.KeyEvent.VK_PAUSE         : return SWT.PAUSE;
            case com.jogamp.newt.event.KeyEvent.VK_SCROLL_LOCK   : return SWT.SCROLL_LOCK;
            case com.jogamp.newt.event.KeyEvent.VK_CANCEL        : return SWT.CANCEL;
            case com.jogamp.newt.event.KeyEvent.VK_INSERT        : return SWT.INSERT;
            case com.jogamp.newt.event.KeyEvent.VK_ESCAPE        : return SWT.ESC;
            case com.jogamp.newt.event.KeyEvent.VK_SPACE         : return SWT.SPACE;
            case com.jogamp.newt.event.KeyEvent.VK_F1            : return SWT.F1;
            case com.jogamp.newt.event.KeyEvent.VK_F2            : return SWT.F2;
            case com.jogamp.newt.event.KeyEvent.VK_F3            : return SWT.F3;
            case com.jogamp.newt.event.KeyEvent.VK_F4            : return SWT.F4;
            case com.jogamp.newt.event.KeyEvent.VK_F5            : return SWT.F5;
            case com.jogamp.newt.event.KeyEvent.VK_F6            : return SWT.F6;
            case com.jogamp.newt.event.KeyEvent.VK_F7            : return SWT.F7;
            case com.jogamp.newt.event.KeyEvent.VK_F8            : return SWT.F8;
            case com.jogamp.newt.event.KeyEvent.VK_F9            : return SWT.F9;
            case com.jogamp.newt.event.KeyEvent.VK_F10           : return SWT.F10;
            case com.jogamp.newt.event.KeyEvent.VK_F11           : return SWT.F11;
            case com.jogamp.newt.event.KeyEvent.VK_F12           : return SWT.F12;
            case com.jogamp.newt.event.KeyEvent.VK_F13           : return SWT.F13;
            case com.jogamp.newt.event.KeyEvent.VK_F14           : return SWT.F14;
            case com.jogamp.newt.event.KeyEvent.VK_F15           : return SWT.F15;
            case com.jogamp.newt.event.KeyEvent.VK_F16           : return SWT.F16;
            case com.jogamp.newt.event.KeyEvent.VK_F17           : return SWT.F17;
            case com.jogamp.newt.event.KeyEvent.VK_F18           : return SWT.F18;
            case com.jogamp.newt.event.KeyEvent.VK_F19           : return SWT.F19;
            case com.jogamp.newt.event.KeyEvent.VK_F20           : return SWT.F20;
            case com.jogamp.newt.event.KeyEvent.VK_DELETE        : return SWT.DEL;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD0       : return SWT.KEYPAD_0;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD1       : return SWT.KEYPAD_1;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD2       : return SWT.KEYPAD_2;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD3       : return SWT.KEYPAD_3;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD4       : return SWT.KEYPAD_4;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD5       : return SWT.KEYPAD_5;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD6       : return SWT.KEYPAD_6;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD7       : return SWT.KEYPAD_7;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD8       : return SWT.KEYPAD_8;
            case com.jogamp.newt.event.KeyEvent.VK_NUMPAD9       : return SWT.KEYPAD_9;
            case com.jogamp.newt.event.KeyEvent.VK_DECIMAL       : return SWT.KEYPAD_DECIMAL;
            case com.jogamp.newt.event.KeyEvent.VK_ADD           : return SWT.KEYPAD_ADD;
            case com.jogamp.newt.event.KeyEvent.VK_SUBTRACT      : return SWT.KEYPAD_SUBTRACT;
            case com.jogamp.newt.event.KeyEvent.VK_MULTIPLY      : return SWT.KEYPAD_MULTIPLY;
            case com.jogamp.newt.event.KeyEvent.VK_DIVIDE        : return SWT.KEYPAD_DIVIDE;
            case com.jogamp.newt.event.KeyEvent.VK_NUM_LOCK      : return SWT.NUM_LOCK;
            case com.jogamp.newt.event.KeyEvent.VK_LEFT          : return SWT.ARROW_LEFT;
            case com.jogamp.newt.event.KeyEvent.VK_UP            : return SWT.ARROW_UP;
            case com.jogamp.newt.event.KeyEvent.VK_RIGHT         : return SWT.ARROW_RIGHT;
            case com.jogamp.newt.event.KeyEvent.VK_DOWN          : return SWT.ARROW_DOWN;
            case com.jogamp.newt.event.KeyEvent.VK_HELP          : return SWT.HELP;
        }
        return defSWTKeyCode;
    }


    public static final com.jogamp.newt.event.InputEvent createInputEvent(final org.eclipse.swt.widgets.Event event, final NativeSurfaceHolder sourceHolder) {
        com.jogamp.newt.event.InputEvent res = createMouseEvent(event, sourceHolder);
        if(null == res) {
            res = createKeyEvent(event, sourceHolder);
        }
        return res;
    }

    public static final com.jogamp.newt.event.MouseEvent createMouseEvent(final org.eclipse.swt.widgets.Event event, final NativeSurfaceHolder sourceHolder) {
        switch(event.type) {
            case SWT.MouseDown:
            case SWT.MouseUp:
            case SWT.MouseMove:
            case SWT.MouseEnter:
            case SWT.MouseExit:
            case SWT.MouseVerticalWheel:
                break;
            default:
                return null;
        }
        final short type = eventTypeSWT2NEWT(event.type);
        if( (short)0 != type ) {
            float rotation = 0;
            if (SWT.MouseVerticalWheel == event.type) {
                // SWT/NEWT rotation is reversed - AWT +1 is down, NEWT +1 is up.
                // rotation = -1 * (int) event.rotation;
                rotation = (float) event.rotation;
            }

            int mods = swtModifiers2Newt(event.stateMask, true);

            final NativeSurface source = sourceHolder.getNativeSurface();
            final int[] pixelPos;
            if( null != source ) {
                if( source instanceof com.jogamp.newt.Window) {
                    final com.jogamp.newt.Window newtSource = (com.jogamp.newt.Window)source;
                    if(newtSource.isPointerConfined()) {
                        mods |= InputEvent.CONFINED_MASK;
                    }
                    if(!newtSource.isPointerVisible()) {
                        mods |= InputEvent.INVISIBLE_MASK;
                    }
                }
                pixelPos = source.convertToPixelUnits(new int[] { event.x, event.y });
            } else {
                pixelPos = new int[] { event.x, event.y };
            }

            return new com.jogamp.newt.event.MouseEvent(
                           type, sourceHolder, (0xFFFFFFFFL & event.time),
                           mods, pixelPos[0], pixelPos[1], (short)event.count, (short)event.button, MouseEvent.getRotationXYZ(rotation, mods), 1f);
        }
        return null; // no mapping ..
    }

    public static final com.jogamp.newt.event.KeyEvent createKeyEvent(final org.eclipse.swt.widgets.Event event, final NativeSurfaceHolder sourceHolder) {
        switch(event.type) {
            case SWT.KeyDown:
            case SWT.KeyUp:
                break;
            default:
                return null;
        }
        final short type = eventTypeSWT2NEWT(event.type);
        if( (short)0 != type ) {
            final short newtKeyCode = swtKeyCode2NewtKeyCode( event.keyCode );
            return com.jogamp.newt.event.KeyEvent.create(
                           type, sourceHolder, (0xFFFFFFFFL & event.time),
                           swtModifiers2Newt(event.stateMask, false),
                           newtKeyCode, newtKeyCode, event.character);
        }
        return null; // no mapping ..
    }

    //
    //
    //

    short dragButtonDown = 0;

    public SWTNewtEventFactory() {
        resetButtonsDown();
    }

    final void resetButtonsDown() {
        dragButtonDown = 0;
    }

    public final boolean dispatchMouseEvent(final org.eclipse.swt.widgets.Event event, final NativeSurfaceHolder sourceHolder, final com.jogamp.newt.event.MouseListener l) {
        final com.jogamp.newt.event.MouseEvent res = createMouseEvent(event, sourceHolder);
        if(null != res) {
            if(null != l) {
                switch(event.type) {
                    case SWT.MouseDown:
                        dragButtonDown = (short) event.button;
                        l.mousePressed(res); break;
                    case SWT.MouseUp:
                        dragButtonDown = 0;
                        l.mouseReleased(res);
                        {
                            final com.jogamp.newt.event.MouseEvent res2 = new com.jogamp.newt.event.MouseEvent(
                                           com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_CLICKED,
                                           res.getSource(),
                                           res.getWhen(), res.getModifiers(),
                                           res.getX(), res.getY(), res.getClickCount(),
                                           res.getButton(), res.getRotation(), res.getRotationScale());
                            l.mouseClicked(res2);
                        }
                        break;
                    case SWT.MouseMove:
                        if( 0 < dragButtonDown ) {
                            final com.jogamp.newt.event.MouseEvent res2 = new com.jogamp.newt.event.MouseEvent(
                                           com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_DRAGGED,
                                           res.getSource(),
                                           res.getWhen(), res.getModifiers(),
                                           res.getX(), res.getY(), res.getClickCount(),
                                           dragButtonDown, res.getRotation(), res.getRotationScale());
                            l.mouseDragged( res2 );
                        } else {
                            l.mouseMoved(res);
                        }
                        break;
                    case SWT.MouseEnter:
                        l.mouseEntered(res);
                        break;
                    case SWT.MouseExit:
                        resetButtonsDown();
                        l.mouseExited(res);
                        break;
                    case SWT.MouseVerticalWheel:
                        l.mouseWheelMoved(res);
                        break;
                }
            }
            return true;
        }
        return false;
    }

    public final boolean dispatchKeyEvent(final org.eclipse.swt.widgets.Event event, final NativeSurfaceHolder sourceHolder, final com.jogamp.newt.event.KeyListener l) {
        final com.jogamp.newt.event.KeyEvent res = createKeyEvent(event, sourceHolder);
        if(null != res) {
            if(null != l) {
                switch(event.type) {
                    case SWT.KeyDown:
                        l.keyPressed(res);
                        break;
                    case SWT.KeyUp:
                        l.keyReleased(res);
                        break;
                }
            }
            return true;
        }
        return false;
    }

    public final void attachDispatchListener(final org.eclipse.swt.widgets.Control ctrl, final NativeSurfaceHolder sourceHolder,
                                             final com.jogamp.newt.event.MouseListener ml,
                                             final com.jogamp.newt.event.KeyListener kl) {
      if(null==ctrl) {
          throw new IllegalArgumentException("Argument ctrl is null");
      }
      if(null==sourceHolder) {
          throw new IllegalArgumentException("Argument source is null");
      }

      if( null != ml ) {
          final Listener listener = new Listener () {
              @Override
              public void handleEvent (final Event event) {
                  dispatchMouseEvent( event, sourceHolder, ml );
              } };
          ctrl.addListener(SWT.MouseDown, listener);
          ctrl.addListener(SWT.MouseUp, listener);
          ctrl.addListener(SWT.MouseMove, listener);
          ctrl.addListener(SWT.MouseEnter, listener);
          ctrl.addListener(SWT.MouseExit, listener);
          ctrl.addListener(SWT.MouseVerticalWheel, listener);
      }
      if( null != kl ) {
          final Listener listener = new Listener () {
              @Override
              public void handleEvent (final Event event) {
                  dispatchKeyEvent( event, sourceHolder, kl );
              } };
          ctrl.addListener(SWT.KeyDown, listener);
          ctrl.addListener(SWT.KeyUp, listener);
      }
    }
}

