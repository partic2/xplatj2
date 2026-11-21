#pragma once
#include <pxprpc_rtbridge_host.hpp>
#include <windows.h>
#include <atomic>
#include <functional>

#include "event.hpp"

namespace pxprpc_win32helpers{
    const UINT WIN32HELPERS_POST_CALL = WM_APP + 1000;

    //Written by the message loop thread before the first post, so it is always
    //valid once StartMessageLoopThread() returns. Atomic to avoid stale reads
    //from RPC threads.
    std::atomic<DWORD> winMsgLoopNativeTid(0);
    uv_sem_t winMsgLoopReadySem;

    void WindowsMessageLoopThread(){
        //Force creation of the thread message queue, otherwise the first
        //PostThreadMessage from another thread can fail with ERROR_INVALID_THREAD_ID.
        MSG msg;
        PeekMessageW(&msg, nullptr, WM_USER, WM_USER, PM_NOREMOVE);
        winMsgLoopNativeTid.store(GetCurrentThreadId(), std::memory_order_release);
        uv_sem_post(&winMsgLoopReadySem);
        while (GetMessageW(&msg, nullptr, 0, 0) > 0) {
            if (msg.message == WIN32HELPERS_POST_CALL) {
                auto func = reinterpret_cast<std::function<void()> *>(msg.lParam);
                (*func)();
                delete func;
            } else if (msg.message == WM_HOTKEY) {
                int32_t hotkeyId = (int32_t)msg.wParam;
                pxprpc_rtbridge_host::postRunnable([hotkeyId]()->void {
                    emitEvent(hotkeySources, [hotkeyId](pxprpc::Serializer *ser)->void {
                        ser->putInt(hotkeyId);
                    });
                });
            } else {
                TranslateMessage(&msg);
                DispatchMessageW(&msg);
            }
        }
    }

    void StartMessageLoopThread(){
        uv_sem_init(&winMsgLoopReadySem, 0);
        pxprpc_rtbridge_host::runInNewThread([]()->void {
            WindowsMessageLoopThread();
        });
        uv_sem_wait(&winMsgLoopReadySem);
        uv_sem_destroy(&winMsgLoopReadySem);
    }

    const char *PostFunctionToDefaultMessageLoop(std::function<void()> fn){
        DWORD tid = winMsgLoopNativeTid.load(std::memory_order_acquire);
        if (tid == 0) {
            return "Message loop thread not inited";
        }
        auto func = new std::function<void()>(std::move(fn));
        if (!PostThreadMessageW(tid, WIN32HELPERS_POST_CALL, 0, reinterpret_cast<LPARAM>(func))) {
            delete func;
            return "PostThreadMessage failed";
        }
        return nullptr;
    }
}