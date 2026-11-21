#pragma once
#include <windows.h>
#include <pxprpc_pp.hpp>
#include <pxprpc_rtbridge_host.hpp>
#include <string>

#include "codec.hpp"
#include "event.hpp"
#include "msgloop.hpp"

namespace pxprpc_win32helpers{
    using pxprpc::NamedFunctionPPImpl1;
    using pxprpc_rtbridge_host::resolveTS;
    using pxprpc_rtbridge_host::rejectTS;

    HHOOK keyboardHookHandle = nullptr;
    HHOOK mouseHookHandle = nullptr;

    LRESULT CALLBACK lowLevelKeyboardProc(int nCode, WPARAM wParam, LPARAM lParam){
        LRESULT res = CallNextHookEx(keyboardHookHandle, nCode, wParam, lParam);
        if (nCode == HC_ACTION) {
            auto keyInfo = reinterpret_cast<KBDLLHOOKSTRUCT *>(lParam);
            if (wParam == WM_KEYDOWN || wParam == WM_SYSKEYDOWN || wParam == WM_KEYUP || wParam == WM_SYSKEYUP) {
                int32_t keyEvent = keyInfo->vkCode;
                if (GetAsyncKeyState(VK_CONTROL)) keyEvent |= 0x100000;
                if (GetAsyncKeyState(VK_SHIFT)) keyEvent |= 0x200000;
                if (GetAsyncKeyState(VK_MENU)) keyEvent |= 0x400000;
                if (wParam == WM_KEYUP || wParam == WM_SYSKEYUP) keyEvent |= 0x800000;
                pxprpc_rtbridge_host::postRunnable([keyEvent]()->void {
                    emitEvent(keyboardHookSources, [keyEvent](pxprpc::Serializer *ser)->void {
                        ser->putInt(INPUT_KEYBOARD);
                        ser->putInt(keyEvent);
                    });
                });
            }
        }
        return res;
    }

    LRESULT CALLBACK lowLevelMouseProc(int nCode, WPARAM wParam, LPARAM lParam){
        LRESULT res = CallNextHookEx(mouseHookHandle, nCode, wParam, lParam);
        if (nCode == HC_ACTION) {
            auto mouseInfo = reinterpret_cast<MSLLHOOKSTRUCT *>(lParam);
            int32_t message = (int32_t)wParam;
            int32_t x = mouseInfo->pt.x;
            int32_t y = mouseInfo->pt.y;
            int32_t data = (int32_t)mouseInfo->mouseData;
            pxprpc_rtbridge_host::postRunnable([message, x, y, data]()->void {
                emitEvent(mouseHookSources, [message, x, y, data](pxprpc::Serializer *ser)->void {
                    ser->putInt(INPUT_MOUSE);
                    ser->putInt(message);
                    ser->putInt(x);
                    ser->putInt(y);
                    ser->putInt(data);
                });
            });
        }
        return res;
    }

    //Must run on the message loop thread.
    const char *ensureKeyboardHook(){
        if (keyboardHookHandle == nullptr) {
            keyboardHookHandle = SetWindowsHookExW(WH_KEYBOARD_LL, lowLevelKeyboardProc, GetModuleHandleW(nullptr), 0);
        }
        return keyboardHookHandle != nullptr ? nullptr : "SetWindowsHookExW(WH_KEYBOARD_LL) failed";
    }

    //Must run on the message loop thread.
    const char *ensureMouseHook(){
        if (mouseHookHandle == nullptr) {
            mouseHookHandle = SetWindowsHookExW(WH_MOUSE_LL, lowLevelMouseProc, GetModuleHandleW(nullptr), 0);
        }
        return mouseHookHandle != nullptr ? nullptr : "SetWindowsHookExW(WH_MOUSE_LL) failed";
    }

    void createKeyboardEventListenerRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        const char *err = PostFunctionToDefaultMessageLoop([ret]()->void {
            const char *hookErr = ensureKeyboardHook();
            if (hookErr == nullptr) {
                auto dispatcher = new pxprpc::EventDispatcher();
                addEventSource(keyboardHookSources, dispatcher);
                resolveTS(ret, static_cast<pxprpc::NamedFunctionPP *>(dispatcher));
            } else {
                DWORD code = GetLastError();
                rejectTS(ret, code != 0 ? Win32ErrorString(code) : std::string(hookErr));
            }
        });
        if (err != nullptr) {
            ret->reject(err);
        }
    }

    void createMouseEventListenerRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        const char *err = PostFunctionToDefaultMessageLoop([ret]()->void {
            const char *hookErr = ensureMouseHook();
            if (hookErr == nullptr) {
                auto dispatcher = new pxprpc::EventDispatcher();
                addEventSource(mouseHookSources, dispatcher);
                resolveTS(ret, static_cast<pxprpc::NamedFunctionPP *>(dispatcher));
            } else {
                DWORD code = GetLastError();
                rejectTS(ret, code != 0 ? Win32ErrorString(code) : std::string(hookErr));
            }
        });
        if (err != nullptr) {
            ret->reject(err);
        }
    }

    void createHotkeyEventListenerRpc(NamedFunctionPPImpl1::Parameter *, NamedFunctionPPImpl1::AsyncReturn *ret){
        auto dispatcher = new pxprpc::EventDispatcher();
        addEventSource(hotkeySources, dispatcher);
        ret->resolve(static_cast<pxprpc::NamedFunctionPP *>(dispatcher));
    }

    void registerHotKeyRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        const char *err = PostFunctionToDefaultMessageLoop([para, ret]()->void {
            int32_t id = para->nextInt();
            int32_t modifiers = para->nextInt();
            int32_t vk = para->nextInt();
            if (RegisterHotKey(nullptr, id, modifiers, vk)) {
                resolveTS(ret);
            } else {
                rejectTS(ret, Win32ErrorString(GetLastError()));
            }
        });
        if (err != nullptr) {
            ret->reject(err);
        }
    }

    void unregisterHotKeyRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        const char *err = PostFunctionToDefaultMessageLoop([para, ret]()->void {
            int32_t id = para->nextInt();
            if (UnregisterHotKey(nullptr, id)) {
                resolveTS(ret);
            } else {
                rejectTS(ret, Win32ErrorString(GetLastError()));
            }
        });
        if (err != nullptr) {
            ret->reject(err);
        }
    }

    void getKeyStateRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        auto rawPara = para->asRaw();
        int32_t count = rawPara->length;
        if (count <= 0) {
            ret->resolve();
            return;
        }
        auto keys = reinterpret_cast<uint8_t *>(rawPara->base);
        auto buf = new uint8_t[count]();
        for (int32_t i = 0; i < count; i++) {
            buf[i] = (GetAsyncKeyState(keys[i]) & 0x8000) != 0 ? 1 : 0;
        }
        ret->resolve(buf, count);
        delete[] buf;
    }

    void sendInputRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        const char *err = PostFunctionToDefaultMessageLoop([para, ret]()->void {
            int32_t inputCount = para->nextInt();
            if (inputCount <= 0 || inputCount > 4096) {
                rejectTS(ret, "Invalid input count");
                return;
            }
            auto inputs = new INPUT[inputCount]();
            for (int32_t i = 0; i < inputCount; i++) {
                int32_t inputType = para->nextInt();
                if (inputType == INPUT_KEYBOARD) {
                    inputs[i].type = INPUT_KEYBOARD;
                    inputs[i].ki.dwFlags = (DWORD)para->nextInt();
                    inputs[i].ki.wVk = (WORD)para->nextInt();
                } else if (inputType == INPUT_MOUSE) {
                    inputs[i].type = INPUT_MOUSE;
                    inputs[i].mi.dwFlags = (DWORD)para->nextInt();
                    inputs[i].mi.dx = para->nextInt();
                    inputs[i].mi.dy = para->nextInt();
                    inputs[i].mi.mouseData = (DWORD)para->nextInt();
                } else {
                    delete[] inputs;
                    rejectTS(ret, "Unsupported INPUT type: " + std::to_string(inputType));
                    return;
                }
            }
            UINT sent = SendInput((UINT)inputCount, inputs, sizeof(INPUT));
            DWORD code = GetLastError();
            delete[] inputs;
            if (sent == 0) {
                rejectTS(ret, code != 0 ? Win32ErrorString(code) : "SendInput failed");
            } else {
                resolveTS(ret);
            }
        });
        if (err != nullptr) {
            ret->reject(err);
        }
    }

    void setCursorPosRpc(NamedFunctionPPImpl1::Parameter *para, NamedFunctionPPImpl1::AsyncReturn *ret){
        int32_t x = para->nextInt();
        int32_t y = para->nextInt();
        if (SetCursorPos(x, y)) {
            ret->resolve();
        } else {
            ret->reject(Win32ErrorString(GetLastError()));
        }
    }

    void registerInputFunctions(){
        pxprpc::defaultFuncMap.add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.CreateKeyboardEventListener", createKeyboardEventListenerRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.CreateMouseEventListener", createMouseEventListenerRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.CreateHotkeyEventListener", createHotkeyEventListenerRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.RegisterHotKey", registerHotKeyRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.UnregisterHotKey", unregisterHotKeyRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.GetKeyState", getKeyStateRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.SendInput", sendInputRpc)
        ).add(
            (new NamedFunctionPPImpl1())->init("pxprpc_win32helpers.SetCursorPos", setCursorPosRpc)
        );
    }
}