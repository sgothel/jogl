#include <stdio.h>

LRESULT	CALLBACK WndProc(HWND, UINT, WPARAM, LPARAM);
ATOM oglClass = 0;

HWND CreateDummyWindow( int x, int y, int width, int height ) {
  RECT rect;
  HINSTANCE hInstance;
  DWORD     dwExStyle;
  DWORD     dwStyle;
  HWND      hWnd;
  ZeroMemory( &rect, sizeof( rect ) );
  // I don't know if we need this but it can't hurt
  if( width < 0 ) {
    rect.left = x + width;
    rect.right = x;
  } else {
    rect.left = x;
    rect.right = x + width;
  }
  if( height < 0 ) {
    rect.top = y + height;
    rect.bottom = y;
  } else {
    rect.top = y;
    rect.bottom = y + height;
  }
  hInstance = GetModuleHandle(NULL);
  
  if( !oglClass ) {
    WNDCLASS  wc;
    ZeroMemory( &wc, sizeof( wc ) );
    wc.style = CS_HREDRAW | CS_VREDRAW | CS_OWNDC;
    wc.lpfnWndProc = (WNDPROC) WndProc;
    wc.cbClsExtra = 0;
    wc.cbWndExtra = 0;
    wc.hInstance = hInstance;
    wc.hIcon = NULL;
    wc.hCursor = NULL;
    wc.hbrBackground = NULL;
    wc.lpszMenuName = NULL;
    wc.lpszClassName	= "OpenGL";
    if( !(oglClass = RegisterClass( &wc )) ) {
      printf( "RegisterClass Failed: %d\n", GetLastError() );
      return( 0 );
    }
  }
  
  dwExStyle = WS_EX_APPWINDOW | WS_EX_WINDOWEDGE;
  dwStyle = WS_OVERLAPPEDWINDOW;
  if( !(hWnd=CreateWindowEx( dwExStyle, "OpenGL", "OpenGL",
                             dwStyle | WS_CLIPSIBLINGS | WS_CLIPCHILDREN,
                             rect.left, rect.top, rect.right - rect.left, rect.bottom - rect.top,
                             NULL, NULL, hInstance, NULL ) ) ) {
    return( 0 );
  }
  return( hWnd );
}

void NativeEventLoop() {
  MSG msg;
  BOOL ret;
  // Grab windows system messages from queue
  while( ( ret = GetMessage( &msg, NULL, 0, 0 ) ) != 0 ) {
    if( ret == -1 ) {
      printf( "Error GetMessage: %d", GetLastError() );
    } else {
      DispatchMessage( &msg ); 
    }
  }
}

void DestroyDummyWindow(HWND handle, HDC hdc) {
  // Post a close window message from shutdown hook thread to
  // window message pump thread
  if( !PostMessage( handle, WM_CLOSE, 0, (LPARAM) hdc ) ) {
    printf( "PostMessage Failed: %d\n", GetLastError() );
  }
}

LRESULT CALLBACK WndProc( HWND   hWnd, UINT   uMsg, WPARAM wParam, LPARAM lParam) {
  switch( uMsg ) {
    case WM_CLOSE:
      // Destroy HDC
      if( ReleaseDC( hWnd, (HDC) lParam ) != 1 ) {
        printf( "Error Releasing DC: %d\n", GetLastError() );
      }
      // Destroy HWND
      if( DestroyWindow( hWnd ) == 0 ) {
        printf( "Error Destroying Window: %d\n", GetLastError() );
      }
      break;
    case WM_DESTROY:
      // Terminate Dummy Window
      PostQuitMessage(0);
      return(0);
  }
  return DefWindowProc(hWnd,uMsg,wParam,lParam);
}
