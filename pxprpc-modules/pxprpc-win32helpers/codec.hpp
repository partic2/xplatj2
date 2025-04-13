#pragma once
#include <memory>
#include <windows.h>
#include <string>

namespace pxprpc_win32helpers{
    std::wstring Utf8ToWchar(const std::string &utf8){
        int wideCharCount = MultiByteToWideChar(CP_UTF8, 0, utf8.c_str(), utf8.length()+1, nullptr, 0);
        if (wideCharCount == 0) {
            return std::wstring();
        }

        wchar_t* wideCharStr = new wchar_t[wideCharCount];

        if (MultiByteToWideChar(CP_UTF8, 0, utf8.c_str(), utf8.length()+1, wideCharStr, wideCharCount) == 0) {
            delete[] wideCharStr;
            return std::wstring();
        }
        std::wstring ret(wideCharStr);
        delete[] wideCharStr;
        return ret;
    }
    std::string WcharToUtf8(const std::wstring &wch){
        int utf8ByteCount = WideCharToMultiByte(CP_UTF8, 0, wch.c_str(), wch.length()+1, nullptr, 0,nullptr,nullptr);
        if (utf8ByteCount == 0) {
            return std::string();
        }

        char* utf8charStr = new char[utf8ByteCount];

        if (WideCharToMultiByte(CP_UTF8, 0, wch.c_str(), wch.length()+1, utf8charStr, utf8ByteCount,nullptr,nullptr) == 0) {
            delete[] utf8charStr;
            return std::string();
        }
        std::string ret(utf8charStr);
        delete[] utf8charStr;
        return ret;
    }
    //If successed, Caller should take response to delete the return string.
    //length should include tailing \0
}