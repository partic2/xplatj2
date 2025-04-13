#pragma once

#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_host.hpp>
#include <windows.h>
#include <pxprpc_rtbridge_base/init.hpp>

#include "codec.hpp"

namespace pxprpc_win32helpers{
    
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
        
        pxprpc_rtbridge_host::resolveTS(ret,outputStream.pos);
    }

    int inited=0;
    void init(){
        if(inited)return;
        pxprpc::defaultFuncMap.add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_win32helpers.TakeScreenShot", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                auto saveTo=static_cast<pxprpc_rtbridge_base::MemoryChunk *>(para->nextObject());
                pxprpc_rtbridge_host::threadPoolRun([saveTo,ret]()-> void {
                    TakeScreenShot(saveTo,ret);
                });
            })
        );
        inited=1;
    }
}