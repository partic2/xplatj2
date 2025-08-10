#pragma once

#include <libloaderapi.h>
#include <processthreadsapi.h>
#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_host.hpp>
#include <set>
#include <windows.h>
#include <pxprpc_rtbridge_base/init.hpp>
#include <iostream>
#include "codec.hpp"
#include "uv.h"

namespace pxprpc_win32helpers{
    using pxprpc_rtbridge_host::resolveTS;
    using pxprpc_rtbridge_host::rejectTS;
    uv_thread_t winMsgLoop;
    DWORD winMsgLoopNativeTid=0;
    void WindowsMessageLoopThread(){
        MSG msg;
        winMsgLoopNativeTid=GetCurrentThreadId();
        while (GetMessage(&msg, NULL, 0, 0)) {
            if(msg.message==WM_APP+1000){
                auto func=reinterpret_cast<std::function<void()> *>(msg.lParam);
                (*func)();
                delete func;
            }else{
                TranslateMessage(&msg);
                DispatchMessage(&msg);
            }
        }
    }

    const char *PostFunctionToDefaultMessageLoop(std::function<void()> fn){
        if(winMsgLoopNativeTid==0){
            return "Thread not inited";
        }else{
            auto func=new std::function<void()>(fn);
            PostThreadMessage(winMsgLoopNativeTid, WM_APP+1000, 0,reinterpret_cast<LPARAM>(func));
            return nullptr;
        }
    }
    
    class FileOutputStream:public pxprpc_rtbridge_base::OutputStream{
        public:
        HANDLE hf=INVALID_HANDLE_VALUE;
        const char *open(const std::string& path){
            auto filename=Utf8ToWchar(path);
            if(filename.length()==0){
                return "utf8towchar failed";
            }
            hf = CreateFileW(filename.c_str(), GENERIC_READ | GENERIC_WRITE, (DWORD)0, NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, (HANDLE)NULL);
            if (hf == INVALID_HANDLE_VALUE)
                return "CreateFile failed";
            return nullptr;
        }
        virtual const char *write(void *data,int length){
            DWORD nwrite=0;
            if(!WriteFile(hf,data,length,&nwrite,NULL)){
                return "WriteFile failed";
            }
            return nullptr;
        }
        ~FileOutputStream(){
            if(hf!=INVALID_HANDLE_VALUE){
                CloseHandle(hf);
            }
        }
    };
    

    //Copilot generate

    void TakeScreenShot(pxprpc_rtbridge_base::MemoryChunk *saveTo,pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret){
        pxprpc_rtbridge_base::MemoryChunkOutputStream outputStream;
        outputStream.target(saveTo);

        // Get the device context of the screen
        HDC hScreenDC = GetDC(NULL);
        HDC hMemoryDC = CreateCompatibleDC(hScreenDC);

        int width = GetDeviceCaps(hScreenDC, HORZRES);
        int height = GetDeviceCaps(hScreenDC, VERTRES);

        // Create a compatible bitmap from the Screen DC
        HBITMAP hBitmap = CreateCompatibleBitmap(hScreenDC, width, height);

        // Select the compatible bitmap into the compatible memory DC.
        HBITMAP hOldBitmap = (HBITMAP)SelectObject(hMemoryDC, hBitmap);

        // Bit block transfer into our compatible memory DC.
        BitBlt(hMemoryDC, 0, 0, width, height, hScreenDC, 0, 0, SRCCOPY);
        hBitmap = (HBITMAP)SelectObject(hMemoryDC, hOldBitmap);

        // Open a file to save the screenshot
        BITMAPFILEHEADER bfHeader;
        BITMAPINFOHEADER biHeader;
        BITMAP bmpScreen;

        GetObject(hBitmap, sizeof(BITMAP), &bmpScreen);

        DWORD dwBmpSize = ((bmpScreen.bmWidth * bmpScreen.bmBitsPixel + 31) / 32) * 4 * bmpScreen.bmHeight;

        HANDLE hDIB = GlobalAlloc(GHND, dwBmpSize + sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER));
        char* lpbitmap = (char*)GlobalLock(hDIB);

        // Fill the file header
        bfHeader.bfType = 0x4D42; // BM
        bfHeader.bfSize = dwBmpSize + sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER);
        bfHeader.bfReserved1 = 0;
        bfHeader.bfReserved2 = 0;
        bfHeader.bfOffBits = sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER);

        // Fill the info header
        biHeader.biSize = sizeof(BITMAPINFOHEADER);
        biHeader.biWidth = bmpScreen.bmWidth;
        biHeader.biHeight = bmpScreen.bmHeight;
        biHeader.biPlanes = 1;
        biHeader.biBitCount = 32;
        biHeader.biCompression = BI_RGB;
        biHeader.biSizeImage = dwBmpSize;
        biHeader.biXPelsPerMeter = 0;
        biHeader.biYPelsPerMeter = 0;
        biHeader.biClrUsed = 0;
        biHeader.biClrImportant = 0;

        // Copy the headers into the DIB
        memcpy(lpbitmap, &bfHeader, sizeof(BITMAPFILEHEADER));
        memcpy(lpbitmap + sizeof(BITMAPFILEHEADER), &biHeader, sizeof(BITMAPINFOHEADER));

        // Get the actual bitmap data
        GetDIBits(hMemoryDC, hBitmap, 0, (UINT)bmpScreen.bmHeight, lpbitmap + sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER), (BITMAPINFO*)&biHeader, DIB_RGB_COLORS);

        // Write the bitmap to file
        outputStream.write(lpbitmap,dwBmpSize + sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER));

        // Cleanup
        GlobalUnlock(hDIB);
        GlobalFree(hDIB);

        DeleteObject(hBitmap);
        DeleteDC(hMemoryDC);
        ReleaseDC(NULL, hScreenDC);
        
        resolveTS(ret,outputStream.pos);
    }

    HHOOK g_hKeyboardHook = NULL;
    class KeyboardHookEventListener;
    std::set<KeyboardHookEventListener *> allKbHookEventSource;
    LRESULT CALLBACK LowLevelKeyboardProc(int nCode, WPARAM wParam, LPARAM lParam);

    class KeyboardHookEventListener:public pxprpc::PxpObject{
        public:
        std::vector<pxprpc::NamedFunctionPPImpl1::AsyncReturn *> pullRequests;
        KeyboardHookEventListener(){
            allKbHookEventSource.insert(this);
        }
        void pull(pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret){
            pullRequests.push_back(ret);
        }
        void notify(int32_t keyEv){
            for(auto t1:pullRequests){
                t1->resolve(t1, keyEv);
            }
            pullRequests.clear();
        }
        ~KeyboardHookEventListener(){
            allKbHookEventSource.erase(this);
            if(allKbHookEventSource.size()==0){
                PostFunctionToDefaultMessageLoop([]()-> void {
                    if(g_hKeyboardHook){
                        UnhookWindowsHookEx(g_hKeyboardHook);
                        g_hKeyboardHook=NULL;
                    }
                });
            }
            for(auto it:pullRequests){
                it->reject("closed");
            }
        }
    };

    LRESULT CALLBACK LowLevelKeyboardProc(int nCode, WPARAM wParam, LPARAM lParam) {
        LRESULT res=CallNextHookEx(g_hKeyboardHook, nCode, wParam, lParam);;
        if (nCode == HC_ACTION) {
            KBDLLHOOKSTRUCT* pKeyInfo = (KBDLLHOOKSTRUCT*)lParam;
            if (wParam == WM_KEYDOWN || wParam == WM_SYSKEYDOWN || wParam == WM_KEYUP || wParam == WM_SYSKEYUP) {
                int32_t keyEv=pKeyInfo->vkCode;
                if(GetAsyncKeyState(VK_CONTROL)){
                    keyEv|=0x10000000;
                }
                if(GetAsyncKeyState(VK_SHIFT)){
                    keyEv|=0x20000000;
                }
                if(GetAsyncKeyState(VK_MENU)){
                    keyEv|=0x40000000;
                }
                if(wParam == WM_KEYUP || wParam == WM_SYSKEYUP){
                    keyEv|=0x80000000;
                }
                pxprpc_rtbridge_host::postRunnable([keyEv]()->void {
                    for(auto t1:allKbHookEventSource){
                        t1->notify(keyEv);
                    }
                });
            }
        }
        return res;
    }

    pxprpc::TableSerializer *windowEnumResult=nullptr;
    
    BOOL CALLBACK EnumWindowsProc(HWND hwnd, LPARAM lParam) {
        if (!IsWindowVisible(hwnd)) {
            return TRUE;
        }
        const int titleLength = GetWindowTextLengthW(hwnd) + 1;
        std::wstring title(titleLength, L'\0');
        GetWindowTextW(hwnd, &title[0], titleLength);
        
        windowEnumResult->addValue((int64_t)hwnd);
        windowEnumResult->addValue(WcharToUtf8(title.c_str()));

        RECT windowRect;
        GetWindowRect(hwnd, &windowRect);
        windowEnumResult->addValue((int32_t)windowRect.left);
        windowEnumResult->addValue((int32_t)windowRect.top);
        windowEnumResult->addValue((int32_t)windowRect.right);
        windowEnumResult->addValue((int32_t)windowRect.bottom);
        return TRUE;
    }

    int inited=0;
    void init(){
        if(inited)return;
        pxprpc_rtbridge_host::runInNewThread([]()->void {
            WindowsMessageLoopThread();
        });
        pxprpc::defaultFuncMap.add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_win32helpers.TakeScreenShot", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                auto saveTo=static_cast<pxprpc_rtbridge_base::MemoryChunk *>(para->nextObject());
                pxprpc_rtbridge_host::threadPoolRun([saveTo,ret]()-> void {
                    TakeScreenShot(saveTo,ret);
                });
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_win32helpers.GetKeyState", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                auto rawPara=para->asRaw();
                uint8_t *pKey=reinterpret_cast<uint8_t *>(rawPara->base);
                int32_t count=rawPara->length;
                auto buf=new uint8_t[count]();
                for(auto t1=0;t1<count;t1++){
                    if(GetAsyncKeyState(*(pKey+t1))&0x8000){
                        buf[t1]=1;
                    }else{
                        buf[t1]=0;
                    }
                }
                ret->resolve(buf,count);
                delete[] buf;
            })
        ).add((new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_win32helpers.CreateKeyboardEventListener", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                PostFunctionToDefaultMessageLoop([ret]()->void {
                    if(g_hKeyboardHook==NULL){
                        g_hKeyboardHook=SetWindowsHookEx(WH_KEYBOARD_LL,LowLevelKeyboardProc,GetModuleHandle(NULL),0);
                    }
                    if(g_hKeyboardHook!=NULL){
                        resolveTS(ret,new KeyboardHookEventListener());
                    }else{
                        rejectTS(ret,"SetWindowsHookEx failed");
                    }
                });
            })
        ).add((new pxprpc::NamedFunctionPPImpl1())
            ->init("pxprpc_win32helpers.PullKeyboardEvent", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                auto kbListener=static_cast<KeyboardHookEventListener *>(para->nextObject());
                kbListener->pull(ret);
            })
        ).add((new pxprpc::NamedFunctionPPImpl1())
            ->init("pxprpc_win32helpers.EnumWindows", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                PostFunctionToDefaultMessageLoop([ret]()->void {
                    if(windowEnumResult!=NULL){
                        rejectTS(ret,"overlapped call is not allowed.");
                    }else{
                        windowEnumResult=new pxprpc::TableSerializer();
                        windowEnumResult->setColumnInfo("lsiiii",{"handle","title","left","top","right","bottom"});
                        EnumWindows(EnumWindowsProc, 0);
                        resolveTS(ret,windowEnumResult->buildSer());
                        delete windowEnumResult;
                        windowEnumResult=NULL;
                    }
                });
            })
        ).add((new pxprpc::NamedFunctionPPImpl1())
            ->init("pxprpc_win32helpers.SetWindowZIndex", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                PostFunctionToDefaultMessageLoop([para,ret]()-> void {
                    auto hwnd=reinterpret_cast<HWND>(para->nextLong());
                    auto pos=para->nextString();
                    if(pos=="topmost"){
                        SetWindowPos(hwnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);
                    }else if(pos=="notopmost"){
                        SetWindowPos(hwnd, HWND_NOTOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);
                    }else if(pos=="top"){
                        SetForegroundWindow(hwnd);
                        SetWindowPos(hwnd, HWND_TOP, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);
                    }else if(pos=="bottom"){
                        SetWindowPos(hwnd, HWND_BOTTOM, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);
                    }
                    resolveTS(ret);
                });
            })
        );

        
        inited=1;
    }
}