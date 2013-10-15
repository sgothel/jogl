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
            final com.jogamp.newt.event.MouseEvent newtEvent = factory.createMouseEvents(isOnTouchEvent, event, newtWindow);
            if(null != newtEvent) {
                switch( event.getActionMasked() ) {
                    case android.view.MotionEvent.ACTION_DOWN: 
                    case android.view.MotionEvent.ACTION_POINTER_DOWN: 
                        newtWindow.focusChanged(false, true);
                        break;
                }
                newtWindow.enqueueEvent(false, newtEvent);
                try { Thread.sleep((long) (100.0F/3.0F)); } // 33 ms
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
            final com.jogamp.newt.event.KeyEvent newtEvent = AndroidNewtEventFactory.createKeyEvent(event, newtWindow, false /* no system keys */);
            if(null != newtEvent) {
                newtWindow.enqueueEvent(false, newtEvent);
                return true;
            }
            return false;
        }
        
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            newtWindow.focusChanged(false, hasFocus);
        }
}
