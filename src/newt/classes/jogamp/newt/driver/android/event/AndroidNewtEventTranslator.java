package jogamp.newt.driver.android.event;

import jogamp.newt.driver.android.WindowDriver;
import android.view.View;

public class AndroidNewtEventTranslator implements View.OnKeyListener, View.OnTouchListener, View.OnFocusChangeListener, View.OnGenericMotionListener {
        private final WindowDriver newtWindow;
        private final AndroidNewtEventFactory factory;
        
        public AndroidNewtEventTranslator(WindowDriver newtWindow, android.content.Context context, android.os.Handler handler) {
            this.newtWindow = newtWindow;
            this.factory = new AndroidNewtEventFactory(context, handler); 
        }
        
        private final boolean processTouchMotionEvents(View v, android.view.MotionEvent event, boolean isOnTouchEvent) {
            final com.jogamp.newt.event.MouseEvent[] newtEvents = factory.createMouseEvents(isOnTouchEvent, event, newtWindow);
            if(null != newtEvents) {
                newtWindow.focusChanged(false, true);
                for(int i=0; i<newtEvents.length; i++) {
                    newtWindow.enqueueEvent(false, newtEvents[i]);
                }
                try { Thread.sleep((long) (1000.0F/30.0F)); }
                catch(InterruptedException e) { }
                return true; // consumed/handled, further interest in events
            }
            return false; // no mapping, no further interest in the event!            
        }
        
        @Override
        public boolean onTouch(View v, android.view.MotionEvent event) {
            return processTouchMotionEvents(v, event, true);
        }

        @Override
        public boolean onGenericMotion(View v, android.view.MotionEvent event) {
            return processTouchMotionEvents(v, event, false);
        }
        
        @Override
        public boolean onKey(View v, int keyCode, android.view.KeyEvent event) {
            final com.jogamp.newt.event.KeyEvent[] newtEvents = factory.createKeyEvents(keyCode, event, newtWindow);
            if(null != newtEvents) {
                for(int i=0; i<newtEvents.length; i++) {
                    newtWindow.enqueueEvent(false, newtEvents[i]);
                }
                return true;
            }
            return false;
        }
        
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            newtWindow.focusChanged(false, hasFocus);
        }
}
