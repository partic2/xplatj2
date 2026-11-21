#pragma once
#include <windows.h>
#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <functional>
#include <set>
#include <vector>

namespace pxprpc_win32helpers{
    std::set<pxprpc::EventDispatcher *> keyboardHookSources;
    std::set<pxprpc::EventDispatcher *> mouseHookSources;
    std::set<pxprpc::EventDispatcher *> hotkeySources;

    //Win32 lock instead of std::mutex: MinGW's win32 thread model has no
    //std::mutex, and this module is Windows-only anyway.
    struct EventSourcesLock{
        CRITICAL_SECTION cs;
        EventSourcesLock(){ InitializeCriticalSection(&cs); }
        ~EventSourcesLock(){ DeleteCriticalSection(&cs); }
    };
    EventSourcesLock eventSourcesLock;

    struct EventSourcesGuard{
        EventSourcesGuard(){ EnterCriticalSection(&eventSourcesLock.cs); }
        ~EventSourcesGuard(){ LeaveCriticalSection(&eventSourcesLock.cs); }
    };

    void addEventSource(std::set<pxprpc::EventDispatcher *> &sources, pxprpc::EventDispatcher *dispatcher){
        dispatcher->ondelete = [&sources](pxprpc::NamedFunctionPPImpl1 *self)->void {
            EventSourcesGuard guard;
            sources.erase(static_cast<pxprpc::EventDispatcher *>(self));
        };
        EventSourcesGuard guard;
        sources.insert(dispatcher);
    }

    //Events are dropped when nobody is waiting on the dispatcher.
    void emitEvent(std::set<pxprpc::EventDispatcher *> &sources, const std::function<void(pxprpc::Serializer *)> &fill){
        std::vector<pxprpc::EventDispatcher *> targets;
        {
            EventSourcesGuard guard;
            targets.assign(sources.begin(), sources.end());
        }
        for (auto dispatcher : targets) {
            if (dispatcher->pendingReturn == nullptr) {
                continue;
            }
            auto ser = new pxprpc::Serializer();
            ser->prepareSerializing(32);
            fill(ser);
            dispatcher->pendingReturn->resolve(ser);
        }
    }
}