
#pragma once

#include <webview.h>
#include <pxprpc_pp.hpp>
#include <uv.h>
#include <pxprpc_ext.hpp>
#include <pxprpc_pipe.h>
#include <pxprpc_rtbridge_host.hpp>

namespace pxprpc_webview{
    void postRunnableToWebview(webview_t webview,std::function<void()> runnable);
    class WebViewObject:public pxprpc::PxpObject{
        public:
        volatile webview_t nativeWebview=nullptr;
        std::function<void()> onWebviewReady=[]()->void{};
        uv_thread_t thread;
        virtual ~WebViewObject(){
            if(nativeWebview!=nullptr){
                webview_t wv=this->nativeWebview;
                postRunnableToWebview(wv,[wv]()->void{
                    webview_terminate(wv);
                });
            }
        }
    };
    void __runwebview(void *inptr){
        WebViewObject *webviewObj=static_cast<WebViewObject *>(inptr);
        webview_t wvr=webview_create(true,nullptr);
        webviewObj->nativeWebview=wvr;
        pxprpc_rtbridge_host::postRunnable(webviewObj->onWebviewReady);
        webview_run(wvr);
        //Here webviewObj has been free,Do NOT access it.
        webview_destroy(wvr);
    };
    void __runCppFunction(webview_t webview,void *cppFunc){
        auto fn=static_cast<std::function<void()> *>(cppFunc);
        (*fn)();
    };
    void postRunnableToWebview(webview_t webview,std::function<void()> runnable){
        auto pFn=new std::function<void()>();
        *pFn=runnable;
        webview_dispatch(webview,__runCppFunction,pFn);
    }
    void init(){
        pxprpc::defaultFuncMap.add((new pxprpc::NamedFunctionPPImpl1())->init("pxprpc_webview.create",
        [](auto para,auto ret)->void{
            auto webviewObj=new WebViewObject();
            webviewObj->onWebviewReady=[ret,webviewObj]()->void{
                ret->resolve(webviewObj);
            };
            uv_thread_create(&webviewObj->thread,&__runwebview,webviewObj);
        }));
        pxprpc::defaultFuncMap.add((new pxprpc::NamedFunctionPPImpl1())->init("pxprpc_webview.navigate",
        [](auto para,auto ret)->void{
            auto webviewObj=static_cast<WebViewObject *>(para->nextObject());
            auto url=para->nextString();
            postRunnableToWebview(webviewObj->nativeWebview,[webviewObj,url,ret]()->void{
                auto r=static_cast<int>(webview_navigate(webviewObj->nativeWebview,url.c_str()));
                pxprpc_rtbridge_host::postRunnable([ret,r]()->void{
                    ret->resolve(r);
                });
            });
        }));
        //Injects JavaScript code to be executed immediately upon loading a page.
        pxprpc::defaultFuncMap.add((new pxprpc::NamedFunctionPPImpl1())->init("pxprpc_webview.init_js",
        [](auto para,auto ret)->void{
            auto webviewObj=static_cast<WebViewObject *>(para->nextObject());
            auto js=para->nextString();
            postRunnableToWebview(webviewObj->nativeWebview,[webviewObj,js,ret]()->void{
                auto r=static_cast<int>(webview_init(webviewObj->nativeWebview,js.c_str()));
                pxprpc_rtbridge_host::postRunnable([ret,r]()->void{
                    ret->resolve(r);
                });
            });
        }));
        pxprpc::defaultFuncMap.add((new pxprpc::NamedFunctionPPImpl1())->init("pxprpc_webview.set_title",
        [](auto para,auto ret)->void{
            auto webviewObj=static_cast<WebViewObject *>(para->nextObject());
            auto title=para->nextString();
            postRunnableToWebview(webviewObj->nativeWebview,[webviewObj,title,ret]()->void{
                auto r=static_cast<int>(webview_set_title(webviewObj->nativeWebview,title.c_str()));
                pxprpc_rtbridge_host::postRunnable([ret,r]()->void{
                    ret->resolve(r);
                });
            });
        }));
    }
}
