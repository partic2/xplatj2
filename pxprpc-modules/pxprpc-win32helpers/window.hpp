#pragma once
#include <windows.h>
#include <dwmapi.h>
#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_host.hpp>
#include <string>

#include "codec.hpp"
#include "msgloop.hpp"

#ifndef DWMWA_EXTENDED_FRAME_BOUNDS
#define DWMWA_EXTENDED_FRAME_BOUNDS 9
#endif
#ifndef WDA_NONE
#define WDA_NONE 0x00000000
#endif
#ifndef WDA_MONITOR
#define WDA_MONITOR 0x00000001
#endif
#ifndef WDA_EXCLUDEFROMCAPTURE
#define WDA_EXCLUDEFROMCAPTURE 0x00000011
#endif

namespace pxprpc_win32helpers{
    using pxprpc::NamedFunctionPPImpl1;
    using pxprpc_rtbridge_host::resolveTS;
    using pxprpc_rtbridge_host::rejectTS;

    HWND windowHandleFromLong(int64_t value){
        return reinterpret_cast<HWND>(static_cast<intptr_t>(value));
    }

    pxprpc::Serializer *serializeRect(const RECT &rect){
        auto ser = new pxprpc::Serializer();
        ser->prepareSerializing(32);
        ser->putInt((int32_t)rect.left);
        ser->putInt((int32_t)rect.top);
        ser->putInt((int32_t)rect.right);
        ser->putInt((int32_t)rect.bottom);
        return ser;
    }

    BOOL CALLBACK enumWindowsCallback(HWND hwnd, LPARAM lParam){
        auto result = reinterpret_cast<pxprpc::TableSerializer *>(lParam);
        if (!IsWindowVisible(hwnd)) {
            return TRUE;
        }
        int length = GetWindowTextLengthW(hwnd);
        std::wstring title(length + 1, L'\0');
        int written = 0;
        if (length > 0) {
            written = GetWindowTextW(hwnd, &title[0], length + 1);
        }
        if (written > 0) {
            title.resize(written);
        } else {
            title.clear();
        }
        RECT rect = {};
        GetWindowRect(hwnd, &rect);
        result->addValue((int64_t)(intptr_t)hwnd);
        result->addValue(WcharToUtf8(title));
        result->addValue((int32_t)rect.left);
        result->addValue((int32_t)rect.top);
        result->addValue((int32_t)rect.right);
        result->addValue((int32_t)rect.bottom);
        return TRUE;
    }

    void enumWindowsRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        auto result = new pxprpc::TableSerializer();
        result->setColumnsInfo("lsiiii", {"handle", "title", "left", "top", "right", "bottom"});
        if (!EnumWindows(enumWindowsCallback, (LPARAM)result)) {
            delete result;
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        ret->resolve(result->buildSer());
        delete result;
    }

    void getForegroundWindowRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        ret->resolve((int64_t)(intptr_t)GetForegroundWindow());
    }

    void getWindowTextRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        int length = GetWindowTextLengthW(hwnd);
        if (length <= 0) {
            ret->resolve(std::string(""));
            return;
        }
        std::wstring title(length + 1, L'\0');
        int written = GetWindowTextW(hwnd, &title[0], length + 1);
        if (written <= 0) {
            ret->resolve(std::string(""));
            return;
        }
        title.resize(written);
        ret->resolve(WcharToUtf8(title));
    }

    void setWindowTextRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        auto text = Utf8ToWchar(para->nextString());
        if (SetWindowTextW(hwnd, text.c_str())) {
            ret->resolve();
        } else {
            ret->reject(Win32ErrorString(GetLastError()));
        }
    }

    void getClassNameRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        wchar_t buffer[512];
        int written = GetClassNameW(hwnd, buffer, 512);
        ret->resolve(WcharToUtf8(std::wstring(buffer, written > 0 ? written : 0)));
    }

    void findWindowRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        auto className = Utf8ToWchar(para->nextString());
        auto windowTitle = Utf8ToWchar(para->nextString());
        HWND hwnd = FindWindowW(className.empty() ? nullptr : className.c_str(),
            windowTitle.empty() ? nullptr : windowTitle.c_str());
        ret->resolve((int64_t)(intptr_t)hwnd);
    }

    void setWindowPosRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        HWND insertAfter = windowHandleFromLong(para->nextLong());
        int32_t x = para->nextInt();
        int32_t y = para->nextInt();
        int32_t width = para->nextInt();
        int32_t height = para->nextInt();
        UINT flags = (UINT)para->nextInt();
        if (SetWindowPos(hwnd, insertAfter, x, y, width, height, flags)) {
            ret->resolve();
        } else {
            ret->reject(Win32ErrorString(GetLastError()));
        }
    }

    void moveWindowRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        int32_t x = para->nextInt();
        int32_t y = para->nextInt();
        int32_t width = para->nextInt();
        int32_t height = para->nextInt();
        int32_t repaint = para->nextInt();
        if (MoveWindow(hwnd, x, y, width, height, repaint != 0)) {
            ret->resolve();
        } else {
            ret->reject(Win32ErrorString(GetLastError()));
        }
    }

    void showWindowRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        int32_t cmd = para->nextInt();
        ret->resolve((int32_t)ShowWindow(hwnd, cmd));
    }

    void getWindowRectRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        RECT rect;
        if (!GetWindowRect(hwnd, &rect)) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        ret->resolve(serializeRect(rect));
    }

    //Visual bounds without the invisible resize border that GetWindowRect includes.
    void getWindowFrameBoundsRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        RECT rect;
        HRESULT hr = DwmGetWindowAttribute(hwnd, DWMWA_EXTENDED_FRAME_BOUNDS, &rect, sizeof(rect));
        if (FAILED(hr) && !GetWindowRect(hwnd, &rect)) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        ret->resolve(serializeRect(rect));
    }

    void getWindowLongRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        int32_t index = para->nextInt();
        SetLastError(0);
        LONG_PTR value = GetWindowLongPtrW(hwnd, index);
        if (value == 0 && GetLastError() != 0) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        ret->resolve((int64_t)value);
    }

    void setWindowLongRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        int32_t index = para->nextInt();
        LONG_PTR value = (LONG_PTR)para->nextLong();
        SetLastError(0);
        LONG_PTR previous = SetWindowLongPtrW(hwnd, index, value);
        if (previous == 0 && GetLastError() != 0) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        ret->resolve((int64_t)previous);
    }

    void setLayeredWindowAttributesRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        COLORREF colorKey = (COLORREF)para->nextInt();
        BYTE alpha = (BYTE)para->nextInt();
        DWORD flags = (DWORD)para->nextInt();
        if (SetLayeredWindowAttributes(hwnd, colorKey, alpha, flags)) {
            ret->resolve();
        } else {
            ret->reject(Win32ErrorString(GetLastError()));
        }
    }

    void setWindowDisplayAffinityRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        typedef BOOL (WINAPI *SetWindowDisplayAffinityFn)(HWND, DWORD);
        static SetWindowDisplayAffinityFn setAffinity = []()->SetWindowDisplayAffinityFn {
            HMODULE user32 = GetModuleHandleW(L"user32.dll");
            if (user32 == nullptr) {
                user32 = LoadLibraryW(L"user32.dll");
            }
            if (user32 == nullptr) {
                return nullptr;
            }
            return (SetWindowDisplayAffinityFn)GetProcAddress(user32, "SetWindowDisplayAffinity");
        }();
        if (setAffinity == nullptr) {
            ret->reject("SetWindowDisplayAffinity is not available");
            return;
        }
        HWND hwnd = windowHandleFromLong(para->nextLong());
        DWORD affinity = (DWORD)para->nextInt();
        if (setAffinity(hwnd, affinity)) {
            ret->resolve();
        } else {
            ret->reject(Win32ErrorString(GetLastError()));
        }
    }

    void getWindowThreadProcessIdRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        DWORD processId = 0;
        GetWindowThreadProcessId(hwnd, &processId);
        ret->resolve((int64_t)processId);
    }

    void getWindowProcessPathRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        DWORD processId = 0;
        GetWindowThreadProcessId(hwnd, &processId);
        if (processId == 0) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        HANDLE process = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, FALSE, processId);
        if (process == nullptr) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        std::wstring path(32768, L'\0');
        DWORD size = (DWORD)path.size();
        BOOL ok = QueryFullProcessImageNameW(process, 0, &path[0], &size);
        DWORD err = GetLastError();
        CloseHandle(process);
        if (!ok) {
            ret->reject(Win32ErrorString(err));
            return;
        }
        path.resize(size);
        ret->resolve(WcharToUtf8(path));
    }

    void postMessageRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        HWND hwnd = windowHandleFromLong(para->nextLong());
        UINT message = (UINT)para->nextInt();
        WPARAM wParam = (WPARAM)para->nextLong();
        LPARAM lParam = (LPARAM)para->nextLong();
        if (PostMessageW(hwnd, message, wParam, lParam)) {
            ret->resolve();
        } else {
            ret->reject(Win32ErrorString(GetLastError()));
        }
    }

    void forceForegroundWindow(HWND hwnd){
        HWND foreground = GetForegroundWindow();
        if (foreground == hwnd) {
            return;
        }
        DWORD foregroundThread = foreground != nullptr ? GetWindowThreadProcessId(foreground, nullptr) : 0;
        DWORD currentThread = GetCurrentThreadId();
        bool attached = false;
        if (foregroundThread != 0 && foregroundThread != currentThread) {
            attached = AttachThreadInput(currentThread, foregroundThread, TRUE) != 0;
        }
        SetForegroundWindow(hwnd);
        BringWindowToTop(hwnd);
        if (attached) {
            AttachThreadInput(currentThread, foregroundThread, FALSE);
        }
    }

    void setWindowZIndexRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        const char *err = PostFunctionToDefaultMessageLoop([para, ret]()->void {
            HWND hwnd = windowHandleFromLong(para->nextLong());
            auto position = para->nextString();
            if (!IsWindow(hwnd)) {
                rejectTS(ret, "Invalid window handle");
                return;
            }
            if (position == "topmost") {
                SetWindowPos(hwnd, HWND_TOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);
            } else if (position == "notopmost") {
                SetWindowPos(hwnd, HWND_NOTOPMOST, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);
            } else if (position == "top") {
                forceForegroundWindow(hwnd);
                SetWindowPos(hwnd, HWND_TOP, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);
            } else if (position == "bottom") {
                SetWindowPos(hwnd, HWND_BOTTOM, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE);
            } else {
                rejectTS(ret, "Unknown position: " + position);
                return;
            }
            resolveTS(ret);
        });
        if (err != nullptr) {
            ret->reject(err);
        }
    }

    void registerWindowFunctions(){
        pxprpc::defaultFuncMap.add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.EnumWindows", enumWindowsRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetForegroundWindow", getForegroundWindowRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetWindowText", getWindowTextRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.SetWindowText", setWindowTextRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetClassName", getClassNameRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.FindWindow", findWindowRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.SetWindowPos", setWindowPosRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.MoveWindow", moveWindowRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.ShowWindow", showWindowRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetWindowRect", getWindowRectRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetWindowFrameBounds", getWindowFrameBoundsRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetWindowLong", getWindowLongRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.SetWindowLong", setWindowLongRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.SetWindowZIndex", setWindowZIndexRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.SetLayeredWindowAttributes", setLayeredWindowAttributesRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.SetWindowDisplayAffinity", setWindowDisplayAffinityRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetWindowThreadProcessId", getWindowThreadProcessIdRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetWindowProcessPath", getWindowProcessPathRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.PostMessage", postMessageRpc)
        );
    }
}