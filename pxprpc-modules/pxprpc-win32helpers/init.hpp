#pragma once

#include "codec.hpp"
#include "event.hpp"
#include "msgloop.hpp"
#include "screen.hpp"
#include "clipboard.hpp"
#include "input.hpp"
#include "window.hpp"
#include "system.hpp"
#include "dialogs.hpp"

namespace pxprpc_win32helpers{
    int inited = 0;

    void init(){
        if (inited) return;
        EnableProcessDpiAwareness();
        StartMessageLoopThread();
        registerScreenFunctions();
        registerClipboardFunctions();
        registerInputFunctions();
        registerWindowFunctions();
        registerSystemFunctions();
        registerDialogFunctions();
        inited = 1;
    }
}