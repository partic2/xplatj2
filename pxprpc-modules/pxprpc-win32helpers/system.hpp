#pragma once
#include <windows.h>
#include <lmcons.h>
#include <shellapi.h>
#include <shlobj.h>
#include <knownfolders.h>
#include <objbase.h>
#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_host.hpp>
#include <cctype>
#include <cstdio>
#include <string>

#include "codec.hpp"

namespace pxprpc_win32helpers{
    using pxprpc::NamedFunctionPPImpl1;

    void getSystemMetricsRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        int32_t index = para->nextInt();
        ret->resolve((int32_t)GetSystemMetrics(index));
    }

    void getMemoryStatusRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        MEMORYSTATUSEX status = {};
        status.dwLength = sizeof(status);
        if (!GlobalMemoryStatusEx(&status)) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        auto ser = new pxprpc::Serializer();
        ser->prepareSerializing(64);
        ser->putLong((int64_t)status.ullTotalPhys);
        ser->putLong((int64_t)status.ullAvailPhys);
        ser->putLong((int64_t)status.ullTotalPageFile);
        ser->putLong((int64_t)status.ullAvailPageFile);
        ser->putLong((int64_t)status.ullTotalVirtual);
        ser->putLong((int64_t)status.ullAvailVirtual);
        ser->putInt((int32_t)status.dwMemoryLoad);
        ret->resolve(ser);
    }

    void getIdleTimeRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        LASTINPUTINFO info = {};
        info.cbSize = sizeof(info);
        if (!GetLastInputInfo(&info)) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        //32 bit unsigned subtraction remains correct across the ~49 day tick wrap.
        DWORD elapsed = GetTickCount() - info.dwTime;
        ret->resolve((int64_t)elapsed);
    }

    void getPowerStatusRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        SYSTEM_POWER_STATUS status = {};
        if (!GetSystemPowerStatus(&status)) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        auto ser = new pxprpc::Serializer();
        ser->prepareSerializing(32);
        ser->putInt((int32_t)status.ACLineStatus);
        ser->putInt((int32_t)status.BatteryFlag);
        ser->putInt((int32_t)status.BatteryLifePercent);
        ser->putInt((int32_t)status.BatteryLifeTime);
        ret->resolve(ser);
    }

    void shellExecuteRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        auto verb = Utf8ToWchar(para->nextString());
        auto file = Utf8ToWchar(para->nextString());
        auto parameters = Utf8ToWchar(para->nextString());
        auto directory = Utf8ToWchar(para->nextString());
        int32_t show = para->nextInt();
        pxprpc_rtbridge_host::threadPoolRun([verb, file, parameters, directory, show, ret]()->void {
            HINSTANCE result = ShellExecuteW(nullptr,
                verb.empty() ? nullptr : verb.c_str(),
                file.c_str(),
                parameters.empty() ? nullptr : parameters.c_str(),
                directory.empty() ? nullptr : directory.c_str(),
                show);
            intptr_t code = (intptr_t)result;
            if (code > 32) {
                pxprpc_rtbridge_host::resolveTS(ret, (int64_t)code);
            } else {
                pxprpc_rtbridge_host::rejectTS(ret, "ShellExecute failed: code " + std::to_string((int)code));
            }
        });
    }

    void getUserNameRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        wchar_t buffer[UNLEN + 1];
        DWORD size = UNLEN + 1;
        if (!GetUserNameW(buffer, &size)) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        ret->resolve(WcharToUtf8(std::wstring(buffer)));
    }

    void getComputerNameRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        wchar_t buffer[MAX_COMPUTERNAME_LENGTH + 1];
        DWORD size = MAX_COMPUTERNAME_LENGTH + 1;
        if (!GetComputerNameW(buffer, &size)) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        ret->resolve(WcharToUtf8(std::wstring(buffer)));
    }

    void getTempPathRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        std::wstring path(32768, L'\0');
        DWORD written = GetTempPathW((DWORD)path.size(), &path[0]);
        if (written == 0 || written >= path.size()) {
            ret->reject(Win32ErrorString(GetLastError()));
            return;
        }
        path.resize(written);
        ret->resolve(WcharToUtf8(path));
    }

    void getKnownFolderRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        std::string name = para->nextString();
        for (auto &c : name) {
            c = (char)tolower((unsigned char)c);
        }
        struct KnownFolderEntry{
            const char *name;
            const GUID *guid;
        };
        static const KnownFolderEntry entries[] = {
            {"desktop", &FOLDERID_Desktop},
            {"documents", &FOLDERID_Documents},
            {"downloads", &FOLDERID_Downloads},
            {"pictures", &FOLDERID_Pictures},
            {"music", &FOLDERID_Music},
            {"videos", &FOLDERID_Videos},
            {"appdata", &FOLDERID_RoamingAppData},
            {"localappdata", &FOLDERID_LocalAppData},
            {"programdata", &FOLDERID_ProgramData},
            {"public", &FOLDERID_Public},
            {"profile", &FOLDERID_Profile},
            {"savedgames", &FOLDERID_SavedGames},
        };
        const GUID *guid = nullptr;
        for (auto &entry : entries) {
            if (name == entry.name) {
                guid = entry.guid;
                break;
            }
        }
        if (guid == nullptr) {
            ret->reject("Unknown known folder: " + name);
            return;
        }
        PWSTR path = nullptr;
        HRESULT hr = SHGetKnownFolderPath(*guid, 0, nullptr, &path);
        if (FAILED(hr) || path == nullptr) {
            if (path != nullptr) {
                CoTaskMemFree(path);
            }
            char buffer[64];
            snprintf(buffer, sizeof(buffer), "SHGetKnownFolderPath failed: 0x%08lX", (unsigned long)hr);
            ret->reject(buffer);
            return;
        }
        std::string result = WcharToUtf8(std::wstring(path));
        CoTaskMemFree(path);
        ret->resolve(result);
    }

    void messageBoxRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        auto title = Utf8ToWchar(para->nextString());
        auto text = Utf8ToWchar(para->nextString());
        int32_t type = para->nextInt();
        //MessageBox owns a nested message loop, keep it off the shared loop thread.
        pxprpc_rtbridge_host::runInNewThread([title, text, type, ret]()->void {
            int result = MessageBoxW(GetForegroundWindow(), text.c_str(), title.c_str(), (UINT)type);
            pxprpc_rtbridge_host::resolveTS(ret, (int32_t)result);
        });
    }

    void registerSystemFunctions(){
        pxprpc::defaultFuncMap.add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetSystemMetrics", getSystemMetricsRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetMemoryStatus", getMemoryStatusRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetIdleTime", getIdleTimeRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetPowerStatus", getPowerStatusRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.ShellExecute", shellExecuteRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetUserName", getUserNameRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetComputerName", getComputerNameRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetTempPath", getTempPathRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetKnownFolder", getKnownFolderRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.MessageBox", messageBoxRpc)
        );
    }
}