#pragma once
#include <windows.h>
#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_host.hpp>

#include "codec.hpp"

namespace pxprpc_win32helpers{
    using pxprpc::NamedFunctionPPImpl1;

    bool openClipboardWithRetry(HWND owner){
        for (int i = 0; i < 10; i++) {
            if (OpenClipboard(owner)) {
                return true;
            }
            Sleep(10);
        }
        return false;
    }

    void getClipboardTextRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        pxprpc_rtbridge_host::threadPoolRun([ret]()->void {
            if (!IsClipboardFormatAvailable(CF_UNICODETEXT)) {
                pxprpc_rtbridge_host::resolveTS(ret, std::string(""));
                return;
            }
            if (!openClipboardWithRetry(GetForegroundWindow())) {
                pxprpc_rtbridge_host::rejectTS(ret, Win32ErrorString(GetLastError()));
                return;
            }
            std::string text;
            HANDLE data = GetClipboardData(CF_UNICODETEXT);
            if (data != nullptr) {
                auto wide = static_cast<const wchar_t *>(GlobalLock(data));
                if (wide != nullptr) {
                    text = WcharToUtf8(std::wstring(wide));
                    GlobalUnlock(data);
                }
            }
            CloseClipboard();
            pxprpc_rtbridge_host::resolveTS(ret, text);
        });
    }

    void setClipboardTextRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        auto text = para->nextString();
        pxprpc_rtbridge_host::threadPoolRun([text, ret]()->void {
            auto wide = Utf8ToWchar(text);
            SIZE_T bytes = (wide.size() + 1) * sizeof(wchar_t);
            HGLOBAL memory = GlobalAlloc(GMEM_MOVEABLE, bytes);
            if (memory == nullptr) {
                pxprpc_rtbridge_host::rejectTS(ret, "GlobalAlloc failed");
                return;
            }
            void *dest = GlobalLock(memory);
            if (dest == nullptr) {
                GlobalFree(memory);
                pxprpc_rtbridge_host::rejectTS(ret, "GlobalLock failed");
                return;
            }
            memcpy(dest, wide.c_str(), wide.size() * sizeof(wchar_t));
            static_cast<wchar_t *>(dest)[wide.size()] = L'\0';
            GlobalUnlock(memory);
            if (!openClipboardWithRetry(GetForegroundWindow())) {
                GlobalFree(memory);
                pxprpc_rtbridge_host::rejectTS(ret, Win32ErrorString(GetLastError()));
                return;
            }
            BOOL ok = EmptyClipboard() && SetClipboardData(CF_UNICODETEXT, memory) != nullptr;
            if (!ok) {
                DWORD err = GetLastError();
                //SetClipboardData did not take ownership on failure.
                GlobalFree(memory);
                CloseClipboard();
                pxprpc_rtbridge_host::rejectTS(ret, Win32ErrorString(err));
                return;
            }
            CloseClipboard();
            pxprpc_rtbridge_host::resolveTS(ret);
        });
    }

    void getClipboardSequenceNumberRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        ret->resolve((int64_t)GetClipboardSequenceNumber());
    }

    void registerClipboardFunctions(){
        pxprpc::defaultFuncMap.add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetClipboardText", getClipboardTextRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.SetClipboardText", setClipboardTextRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetClipboardSequenceNumber", getClipboardSequenceNumberRpc)
        );
    }
}