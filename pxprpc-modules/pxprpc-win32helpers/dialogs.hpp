#pragma once
#include <windows.h>
#include <commdlg.h>
#include <objbase.h>
#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_host.hpp>
#include <string>

#include "codec.hpp"

namespace pxprpc_win32helpers{
    using pxprpc::NamedFunctionPPImpl1;
    using pxprpc_rtbridge_host::resolveTS;
    using pxprpc_rtbridge_host::rejectTS;

    //Accepts "Description|*.ext;*.ext2|Description2|*.ext3". The OPENFILENAME
    //filter needs embedded '\0' separators and a double '\0' terminator.
    std::wstring buildDialogFilter(const std::wstring &filter){
        if (filter.empty()) {
            return std::wstring(L"All Files\0*.*\0", 15);
        }
        std::wstring result;
        result.reserve(filter.size() + 2);
        for (auto c : filter) {
            result.push_back(c == L'|' ? L'\0' : c);
        }
        result.push_back(L'\0');
        return result;
    }

    void openFileDialogRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        auto title = Utf8ToWchar(para->nextString());
        auto filter = Utf8ToWchar(para->nextString());
        auto initialDir = Utf8ToWchar(para->nextString());
        //The dialog owns a nested message loop, so it must not run on the shared
        //message loop thread: posted functions would be swallowed by that loop.
        pxprpc_rtbridge_host::runInNewThread([title, filter, initialDir, ret]()->void {
            HRESULT coInit = CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
            auto filterBuffer = buildDialogFilter(filter);
            std::wstring fileBuffer(4096, L'\0');
            OPENFILENAMEW ofn = {};
            ofn.lStructSize = sizeof(ofn);
            ofn.hwndOwner = GetForegroundWindow();
            ofn.lpstrFilter = filterBuffer.c_str();
            ofn.lpstrFile = &fileBuffer[0];
            ofn.nMaxFile = (DWORD)fileBuffer.size();
            ofn.lpstrInitialDir = initialDir.empty() ? nullptr : initialDir.c_str();
            ofn.lpstrTitle = title.empty() ? nullptr : title.c_str();
            ofn.Flags = OFN_EXPLORER | OFN_FILEMUSTEXIST | OFN_PATHMUSTEXIST | OFN_NOCHANGEDIR;
            if (GetOpenFileNameW(&ofn)) {
                resolveTS(ret, WcharToUtf8(std::wstring(fileBuffer.c_str())));
            } else {
                DWORD code = CommDlgExtendedError();
                if (code != 0) {
                    rejectTS(ret, "GetOpenFileNameW failed: " + std::to_string((unsigned long)code));
                } else {
                    resolveTS(ret, std::string(""));
                }
            }
            if (SUCCEEDED(coInit)) {
                CoUninitialize();
            }
        });
    }

    void saveFileDialogRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        auto title = Utf8ToWchar(para->nextString());
        auto filter = Utf8ToWchar(para->nextString());
        auto initialDir = Utf8ToWchar(para->nextString());
        auto defaultName = Utf8ToWchar(para->nextString());
        pxprpc_rtbridge_host::runInNewThread([title, filter, initialDir, defaultName, ret]()->void {
            HRESULT coInit = CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED);
            auto filterBuffer = buildDialogFilter(filter);
            std::wstring fileBuffer(4096, L'\0');
            if (!defaultName.empty()) {
                size_t copySize = defaultName.size() < fileBuffer.size() - 1 ? defaultName.size() : fileBuffer.size() - 1;
                defaultName.copy(&fileBuffer[0], copySize);
            }
            OPENFILENAMEW ofn = {};
            ofn.lStructSize = sizeof(ofn);
            ofn.hwndOwner = GetForegroundWindow();
            ofn.lpstrFilter = filterBuffer.c_str();
            ofn.lpstrFile = &fileBuffer[0];
            ofn.nMaxFile = (DWORD)fileBuffer.size();
            ofn.lpstrInitialDir = initialDir.empty() ? nullptr : initialDir.c_str();
            ofn.lpstrTitle = title.empty() ? nullptr : title.c_str();
            ofn.Flags = OFN_EXPLORER | OFN_OVERWRITEPROMPT | OFN_PATHMUSTEXIST | OFN_NOCHANGEDIR;
            if (GetSaveFileNameW(&ofn)) {
                resolveTS(ret, WcharToUtf8(std::wstring(fileBuffer.c_str())));
            } else {
                DWORD code = CommDlgExtendedError();
                if (code != 0) {
                    rejectTS(ret, "GetSaveFileNameW failed: " + std::to_string((unsigned long)code));
                } else {
                    resolveTS(ret, std::string(""));
                }
            }
            if (SUCCEEDED(coInit)) {
                CoUninitialize();
            }
        });
    }

    void registerDialogFunctions(){
        pxprpc::defaultFuncMap.add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.OpenFileDialog", openFileDialogRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.SaveFileDialog", saveFileDialogRpc)
        );
    }
}