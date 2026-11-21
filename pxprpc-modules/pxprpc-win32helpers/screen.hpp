#pragma once
#include <windows.h>
#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_host.hpp>
#include <pxprpc_rtbridge_base/init.hpp>

#include "codec.hpp"

#ifndef PW_RENDERFULLCONTENT
#define PW_RENDERFULLCONTENT 0x00000002
#endif

namespace pxprpc_win32helpers{
    using pxprpc::NamedFunctionPPImpl1;

    typedef HRESULT (WINAPI *GetDpiForMonitorFn)(HMONITOR, int, UINT *, UINT *);

    GetDpiForMonitorFn loadGetDpiForMonitor(){
        static GetDpiForMonitorFn fn = []()->GetDpiForMonitorFn {
            HMODULE module = LoadLibraryW(L"shcore.dll");
            if (module != nullptr) {
                return (GetDpiForMonitorFn)GetProcAddress(module, "GetDpiForMonitor");
            }
            return nullptr;
        }();
        return fn;
    }

    void EnableProcessDpiAwareness(){
        HMODULE user32 = GetModuleHandleW(L"user32.dll");
        if (user32 == nullptr) {
            user32 = LoadLibraryW(L"user32.dll");
        }
        if (user32 == nullptr) {
            return;
        }
        //DPI_AWARENESS_CONTEXT_PER_MONITOR_AWARE_V2, only available since Win10 1703.
        typedef BOOL (WINAPI *SetProcessDpiAwarenessContextFn)(HANDLE);
        auto setContext = (SetProcessDpiAwarenessContextFn)GetProcAddress(user32, "SetProcessDpiAwarenessContext");
        if (setContext != nullptr && setContext((HANDLE)-4)) {
            return;
        }
        typedef BOOL (WINAPI *SetProcessDPIAwareFn)();
        auto setAware = (SetProcessDPIAwareFn)GetProcAddress(user32, "SetProcessDPIAware");
        if (setAware != nullptr) {
            setAware();
        }
    }

    HBITMAP create32BitDib(HDC dc, int width, int height, void **bits){
        BITMAPINFO info = {};
        info.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
        info.bmiHeader.biWidth = width;
        info.bmiHeader.biHeight = -height;
        info.bmiHeader.biPlanes = 1;
        info.bmiHeader.biBitCount = 32;
        info.bmiHeader.biCompression = BI_RGB;
        return CreateDIBSection(dc, &info, DIB_RGB_COLORS, bits, nullptr, 0);
    }

    //Writes a bottom-up 32bpp BMP. Returns required byte count.
    int writeDibToBmp(void *bits, int width, int height, pxprpc_rtbridge_base::OutputStream &output){
        int stride = width * 4;
        int dataSize = stride * height;
        BITMAPFILEHEADER fileHeader = {};
        fileHeader.bfType = 0x4D42;
        fileHeader.bfOffBits = sizeof(BITMAPFILEHEADER) + sizeof(BITMAPINFOHEADER);
        fileHeader.bfSize = fileHeader.bfOffBits + dataSize;
        BITMAPINFOHEADER infoHeader = {};
        infoHeader.biSize = sizeof(BITMAPINFOHEADER);
        infoHeader.biWidth = width;
        infoHeader.biHeight = height;
        infoHeader.biPlanes = 1;
        infoHeader.biBitCount = 32;
        infoHeader.biCompression = BI_RGB;
        infoHeader.biSizeImage = dataSize;
        output.write(&fileHeader, sizeof(fileHeader));
        output.write(&infoHeader, sizeof(infoHeader));
        for (int y = height - 1; y >= 0; y--) {
            output.write(((uint8_t *)bits) + (size_t)y * stride, stride);
        }
        return (int)fileHeader.bfSize;
    }

    //Captures the whole virtual desktop (all monitors). Returns required byte
    //count or -1 on failure. The chunk receives at most chunk->size bytes.
    int captureScreenToChunk(pxprpc_rtbridge_base::MemoryChunk *chunk, int includeCursor){
        int x = GetSystemMetrics(SM_XVIRTUALSCREEN);
        int y = GetSystemMetrics(SM_YVIRTUALSCREEN);
        int width = GetSystemMetrics(SM_CXVIRTUALSCREEN);
        int height = GetSystemMetrics(SM_CYVIRTUALSCREEN);
        if (width <= 0 || height <= 0) {
            return -1;
        }
        HDC screenDC = GetDC(nullptr);
        if (screenDC == nullptr) {
            return -1;
        }
        HDC memoryDC = CreateCompatibleDC(screenDC);
        void *bits = nullptr;
        HBITMAP bitmap = create32BitDib(screenDC, width, height, &bits);
        if (memoryDC == nullptr || bitmap == nullptr || bits == nullptr) {
            if (bitmap != nullptr) DeleteObject(bitmap);
            if (memoryDC != nullptr) DeleteDC(memoryDC);
            ReleaseDC(nullptr, screenDC);
            return -1;
        }
        HGDIOBJ oldObject = SelectObject(memoryDC, bitmap);
        BOOL ok = BitBlt(memoryDC, 0, 0, width, height, screenDC, x, y, SRCCOPY);
        if (ok && includeCursor) {
            CURSORINFO cursorInfo = {};
            cursorInfo.cbSize = sizeof(cursorInfo);
            if (GetCursorInfo(&cursorInfo) && (cursorInfo.flags & CURSOR_SHOWING)) {
                DrawIconEx(memoryDC, cursorInfo.ptScreenPos.x - x, cursorInfo.ptScreenPos.y - y,
                    cursorInfo.hCursor, 0, 0, 0, nullptr, DI_NORMAL);
            }
        }
        int required = -1;
        if (ok) {
            pxprpc_rtbridge_base::MemoryChunkOutputStream output;
            output.target(chunk);
            required = writeDibToBmp(bits, width, height, output);
        }
        SelectObject(memoryDC, oldObject);
        DeleteObject(bitmap);
        DeleteDC(memoryDC);
        ReleaseDC(nullptr, screenDC);
        return required;
    }

    //flags: 0 or PW_RENDERFULLCONTENT(2). PrintWindow works for many
    //hardware accelerated windows where BitBlt returns black.
    int captureWindowToChunk(HWND hwnd, pxprpc_rtbridge_base::MemoryChunk *chunk, UINT flags){
        RECT rect;
        if (!GetWindowRect(hwnd, &rect)) {
            return -1;
        }
        int width = rect.right - rect.left;
        int height = rect.bottom - rect.top;
        if (width <= 0 || height <= 0) {
            return -1;
        }
        HDC windowDC = GetWindowDC(hwnd);
        if (windowDC == nullptr) {
            return -1;
        }
        HDC memoryDC = CreateCompatibleDC(windowDC);
        void *bits = nullptr;
        HBITMAP bitmap = create32BitDib(windowDC, width, height, &bits);
        if (memoryDC == nullptr || bitmap == nullptr || bits == nullptr) {
            if (bitmap != nullptr) DeleteObject(bitmap);
            if (memoryDC != nullptr) DeleteDC(memoryDC);
            ReleaseDC(hwnd, windowDC);
            return -1;
        }
        HGDIOBJ oldObject = SelectObject(memoryDC, bitmap);
        BOOL ok = PrintWindow(hwnd, memoryDC, flags);
        if (!ok) {
            ok = BitBlt(memoryDC, 0, 0, width, height, windowDC, 0, 0, SRCCOPY);
        }
        int required = -1;
        if (ok) {
            pxprpc_rtbridge_base::MemoryChunkOutputStream output;
            output.target(chunk);
            required = writeDibToBmp(bits, width, height, output);
        }
        SelectObject(memoryDC, oldObject);
        DeleteObject(bitmap);
        DeleteDC(memoryDC);
        ReleaseDC(hwnd, windowDC);
        return required;
    }

    struct MonitorEnumContext{
        pxprpc::TableSerializer *result;
        GetDpiForMonitorFn getDpi;
    };

    BOOL CALLBACK enumMonitorCallback(HMONITOR monitor, HDC, LPRECT, LPARAM lParam){
        auto context = reinterpret_cast<MonitorEnumContext *>(lParam);
        MONITORINFOEXW info = {};
        info.cbSize = sizeof(info);
        if (!GetMonitorInfoW(monitor, &info)) {
            return TRUE;
        }
        UINT dpiX = 96;
        UINT dpiY = 96;
        if (context->getDpi != nullptr) {
            //MDT_EFFECTIVE_DPI
            context->getDpi(monitor, 0, &dpiX, &dpiY);
        }
        context->result->addValue((int64_t)(intptr_t)monitor);
        context->result->addValue((int32_t)((info.dwFlags & MONITORINFOF_PRIMARY) != 0));
        context->result->addValue((int32_t)info.rcMonitor.left);
        context->result->addValue((int32_t)info.rcMonitor.top);
        context->result->addValue((int32_t)info.rcMonitor.right);
        context->result->addValue((int32_t)info.rcMonitor.bottom);
        context->result->addValue((int32_t)info.rcWork.left);
        context->result->addValue((int32_t)info.rcWork.top);
        context->result->addValue((int32_t)info.rcWork.right);
        context->result->addValue((int32_t)info.rcWork.bottom);
        context->result->addValue((int32_t)dpiX);
        context->result->addValue((int32_t)dpiY);
        return TRUE;
    }

    void takeScreenShotRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        auto chunk = static_cast<pxprpc_rtbridge_base::MemoryChunk *>(para->nextObject());
        if (chunk == nullptr) {
            ret->reject("Invalid memory chunk");
            return;
        }
        pxprpc_rtbridge_host::threadPoolRun([chunk, ret]()->void {
            int required = captureScreenToChunk(chunk, 0);
            if (required < 0) {
                pxprpc_rtbridge_host::rejectTS(ret, "Capture screen failed");
            } else {
                pxprpc_rtbridge_host::resolveTS(ret, (int32_t)required);
            }
        });
    }

    void captureScreenRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        auto chunk = static_cast<pxprpc_rtbridge_base::MemoryChunk *>(para->nextObject());
        int includeCursor = para->nextInt();
        if (chunk == nullptr) {
            ret->reject("Invalid memory chunk");
            return;
        }
        pxprpc_rtbridge_host::threadPoolRun([chunk, includeCursor, ret]()->void {
            int required = captureScreenToChunk(chunk, includeCursor);
            if (required < 0) {
                pxprpc_rtbridge_host::rejectTS(ret, "Capture screen failed");
            } else {
                pxprpc_rtbridge_host::resolveTS(ret, (int32_t)required);
            }
        });
    }

    void captureWindowRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = reinterpret_cast<HWND>(static_cast<intptr_t>(para->nextLong()));
        auto chunk = static_cast<pxprpc_rtbridge_base::MemoryChunk *>(para->nextObject());
        UINT flags = (UINT)para->nextInt();
        if (chunk == nullptr) {
            ret->reject("Invalid memory chunk");
            return;
        }
        if (!IsWindow(hwnd)) {
            ret->reject("Invalid window handle");
            return;
        }
        pxprpc_rtbridge_host::threadPoolRun([hwnd, chunk, flags, ret]()->void {
            int required = captureWindowToChunk(hwnd, chunk, flags);
            if (required < 0) {
                pxprpc_rtbridge_host::rejectTS(ret, "Capture window failed");
            } else {
                pxprpc_rtbridge_host::resolveTS(ret, (int32_t)required);
            }
        });
    }

    void getMonitorsRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        auto result = new pxprpc::TableSerializer();
        result->setColumnsInfo("liiiiiiiiiii",
            {"handle", "primary", "left", "top", "right", "bottom",
             "workLeft", "workTop", "workRight", "workBottom", "dpiX", "dpiY"});
        MonitorEnumContext context{result, loadGetDpiForMonitor()};
        if (!EnumDisplayMonitors(nullptr, nullptr, enumMonitorCallback, (LPARAM)&context)) {
            delete result;
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        ret->resolve(result->buildSer());
        delete result;
    }

    void getCursorPosRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        POINT pt;
        if (!GetCursorPos(&pt)) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        auto ser = new pxprpc::Serializer();
        ser->prepareSerializing(16);
        ser->putInt(pt.x);
        ser->putInt(pt.y);
        ret->resolve(ser);
    }

    void registerScreenFunctions(){
        pxprpc::defaultFuncMap.add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.TakeScreenShot", takeScreenShotRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.CaptureScreen", captureScreenRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.CaptureWindow", captureWindowRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetMonitors", getMonitorsRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetCursorPos", getCursorPosRpc)
        );
    }
}