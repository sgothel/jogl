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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.jogamp.common.util.IntIntHashMap;
import com.jogamp.newt.event.InputEvent;

/**
 * SWT event translator to NEWT, inclusive dispatch listener.
 * <p>
 * <b>Disclaimer:</b> This code is merely tested and subject to change.
 * </p>
 */
public class SWTNewtEventFactory {

    protected static final IntIntHashMap eventTypeSWT2NEWT;

    static {
        IntIntHashMap map = new IntIntHashMap();
        map.setKeyNotFoundValue(0xFFFFFFFF);
        
        // map.put(SWT.MouseXXX, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_CLICKED);
        map.put(SWT.MouseDown, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_PRESSED);
        map.put(SWT.MouseUp, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_RELEASED);
        map.put(SWT.MouseMove, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_MOVED);
        map.put(SWT.MouseEnter, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_ENTERED);
        map.put(SWT.MouseExit, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_EXITED);
        // map.put(SWT.MouseXXX, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_DRAGGED);
        map.put(SWT.MouseVerticalWheel, com.jogamp.newt.event.MouseEvent.EVENT_MOUSE_WHEEL_MOVED);

        map.put(SWT.KeyDown, com.jogamp.newt.event.KeyEvent.EVENT_KEY_PRESSED);
        map.put(SWT.KeyUp, com.jogamp.newt.event.KeyEvent.EVENT_KEY_RELEASED);
        // map.put(SWT.KeyXXX, com.jogamp.newt.event.KeyEvent.EVENT_KEY_TYPED);

        eventTypeSWT2NEWT = map;
    }

    public static final int swtModifiers2Newt(int awtMods, boolean mouseHint) {
        int newtMods = 0;
        if ((awtMods & SWT.SHIFT) != 0)     newtMods |= com.jogamp.newt.event.InputEvent.SHIFT_MASK;
        if ((awtMods & SWT.CTRL) != 0)      newtMods |= com.jogamp.newt.event.InputEvent.CTRL_MASK;
        if ((awtMods & SWT.ALT) != 0)      newtMods |= com.jogamp.newt.event.InputEvent.ALT_MASK;
        return newtMods;
    }

    public static final com.jogamp.newt.event.InputEvent createInputEvent(org.eclipse.swt.widgets.Event event, Object source) {
        com.jogamp.newt.event.InputEvent res = createMouseEvent(event, source);
        if(null == res) {
            res = createKeyEvent(event, source);
        }
        return res;
    }
    
    public static final com.jogamp.newt.event.MouseEvent createMouseEvent(org.eclipse.swt.widgets.Event event, Object source) {
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
        int type = eventTypeSWT2NEWT.get(event.type);
        if(0xFFFFFFFF != type) {
            int rotation = 0;
            if (SWT.MouseVerticalWheel == event.type) {
                // SWT/NEWT rotation is reversed - AWT +1 is down, NEWT +1 is up.
                // rotation = -1 * (int) event.rotation;
                rotation = (int) event.rotation;
            }

            int mods = swtModifiers2Newt(event.stateMask, true);
            
            if( source instanceof com.jogamp.newt.Window) {
                final com.jogamp.newt.Window newtSource = (com.jogamp.newt.Window)source;
                if(newtSource.isPointerConfined()) {
                    mods |= InputEvent.CONFINED_MASK;
                }
                if(!newtSource.isPointerVisible()) {
                    mods |= InputEvent.INVISIBLE_MASK;
                }
            }
            
            return new com.jogamp.newt.event.MouseEvent(
                           type, (null==source)?(Object)event.data:source, (0xFFFFFFFFL & (long)event.time),
                           mods, event.x, event.y, event.count, event.button, rotation);
        }
        return null; // no mapping ..
    }

    public static final com.jogamp.newt.event.KeyEvent createKeyEvent(org.eclipse.swt.widgets.Event event, Object source) {
        switch(event.type) {
            case SWT.KeyDown:
            case SWT.KeyUp:
                break;
            default:
                return null;
        }
        int type = eventTypeSWT2NEWT.get(event.type);
        if(0xFFFFFFFF != type) {
            return new com.jogamp.newt.event.KeyEvent(
                           type, (null==source)?(Object)event.data:source, (0xFFFFFFFFL & (long)event.time),
                           swtModifiers2Newt(event.stateMask, false), 
                           event.keyCode, event.character);
        }
        return null; // no mapping ..
    }
    
    //
    //
    //
    
    int dragButtonDown = 0;
    
    public SWTNewtEventFactory() {
        resetButtonsDown();
    }
    
    final void resetButtonsDown() {
        dragButtonDown = 0;
    }
    
    public final boolean dispatchMouseEvent(org.eclipse.swt.widgets.Event event, Object source, com.jogamp.newt.event.MouseListener l) {
        com.jogamp.newt.event.MouseEvent res = createMouseEvent(event, source);
        if(null != res) {
            if(null != l) {
                switch(event.type) {
                    case SWT.MouseDown:               
                        dragButtonDown = event.button;
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
                                           res.getButton(), res.getWheelRotation() );
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
                                           dragButtonDown, res.getWheelRotation() );
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

    public final boolean dispatchKeyEvent(org.eclipse.swt.widgets.Event event, Object source, com.jogamp.newt.event.KeyListener l) {
        com.jogamp.newt.event.KeyEvent res = createKeyEvent(event, source);
        if(null != res) {
            if(null != l) {
                switch(event.type) {
                    case SWT.KeyDown:
                        l.keyPressed(res); 
                        break;
                    case SWT.KeyUp:
                        l.keyReleased(res);
                        l.keyTyped(res); 
                        break;
                }
            }
            return true;
        }
        return false;
    }  
    
    public final void attachDispatchListener(final org.eclipse.swt.widgets.Control ctrl, final Object source, 
                                             final com.jogamp.newt.event.MouseListener ml,
                                             final com.jogamp.newt.event.KeyListener kl) {
      final Listener listener = new Listener () {
          @Override
          public void handleEvent (Event event) {
              if( dispatchMouseEvent( event, source, ml ) ) {
                  return;
              }
              if( dispatchKeyEvent( event, source, kl ) ) {
                  return;
              }
          } };
      ctrl.addListener(SWT.MouseDown, listener);
      ctrl.addListener(SWT.MouseUp, listener);
      ctrl.addListener(SWT.MouseMove, listener);
      ctrl.addListener(SWT.MouseEnter, listener);
      ctrl.addListener(SWT.MouseExit, listener);
      ctrl.addListener(SWT.MouseVerticalWheel, listener);
      ctrl.addListener(SWT.KeyDown, listener);
      ctrl.addListener(SWT.KeyUp, listener);
    }
}

