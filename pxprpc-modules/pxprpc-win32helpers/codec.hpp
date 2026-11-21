#pragma once
#ifndef NOMINMAX
#define NOMINMAX
#endif
//Vista+ APIs (QueryFullProcessImageNameW, SHGetKnownFolderPath, ...) are
//hidden when the SDK defaults to a lower target, e.g. MinGW's 0x0502.
#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0600
#endif
#ifndef WINVER
#define WINVER 0x0600
#endif
#include <windows.h>
#include <string>

namespace pxprpc_win32helpers{
    //Returns length-accurate string without trailing '\0'.
    std::wstring Utf8ToWchar(const std::string &utf8){
        if(utf8.empty()){
            return std::wstring();
        }
        int wideCharCount = MultiByteToWideChar(CP_UTF8, 0, utf8.data(), (int)utf8.size(), nullptr, 0);
        if (wideCharCount <= 0) {
            return std::wstring();
        }
        std::wstring ret(wideCharCount, L'\0');
        if (MultiByteToWideChar(CP_UTF8, 0, utf8.data(), (int)utf8.size(), &ret[0], wideCharCount) <= 0) {
            return std::wstring();
        }
        return ret;
    }
    //Returns length-accurate string without trailing '\0'.
    std::string WcharToUtf8(const std::wstring &wch){
        if(wch.empty()){
            return std::string();
        }
        int utf8ByteCount = WideCharToMultiByte(CP_UTF8, 0, wch.data(), (int)wch.size(), nullptr, 0, nullptr, nullptr);
        if (utf8ByteCount <= 0) {
            return std::string();
        }
        std::string ret(utf8ByteCount, '\0');
        if (WideCharToMultiByte(CP_UTF8, 0, wch.data(), (int)wch.size(), &ret[0], utf8ByteCount, nullptr, nullptr) <= 0) {
            return std::string();
        }
        return ret;
    }
    std::string Win32ErrorString(DWORD code){
        wchar_t *buf = nullptr;
        DWORD len = FormatMessageW(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM | FORMAT_MESSAGE_IGNORE_INSERTS,
            nullptr, code, MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT), (LPWSTR)&buf, 0, nullptr);
        std::string message;
        if (len > 0 && buf != nullptr) {
            std::wstring wbuf(buf, len);
            while (!wbuf.empty() && (wbuf.back() == L'\r' || wbuf.back() == L'\n' || wbuf.back() == L' ')) {
                wbuf.pop_back();
            }
            message = WcharToUtf8(wbuf);
        }
        if (buf != nullptr) {
            LocalFree(buf);
        }
        return message + " (" + std::to_string((unsigned long)code) + ")";
    }
}