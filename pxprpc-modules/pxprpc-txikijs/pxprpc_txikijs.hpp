
#pragma once

#include "uv.h"
extern "C"{
#include <pxprpc_pipe.h>
#include <quickjs.h>
#include <tjs.h>
}
#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_host.hpp>
#include <pxprpc_rtbridge_base/init.hpp>
#include <vector>


namespace pxprpc_txikijs{
    
    int inited=0;
    
    class TjsRuntimeWrap : public pxprpc::PxpObject{
        public:

        TJSRuntime *rt=nullptr;
        std::vector<std::string> args;
        std::vector<char *> argsC; 
        uv_mutex_t jobsMutex;
        std::vector<std::function<void()> *> jobs;
        uv_async_t *asyncReq;
        
        TjsRuntimeWrap(){
            uv_mutex_init(&jobsMutex);
        }
        void init(std::vector<std::string> &args){
            asyncReq=new uv_async_t();
            this->args=args;
            asyncReq->data=this;            
            pxprpc_rtbridge_host::runInNewThread([this]()->void {
                this->argsC.clear();
                for(auto& it:this->args){
                    this->argsC.push_back(const_cast<char *>(it.c_str()));
                }
                this->argsC.push_back(nullptr);
                TJS_Initialize(this->argsC.size()-1,this->argsC.data());
                this->rt=TJS_NewRuntime();
                uv_async_init(TJS_GetLoop(this->rt),this->asyncReq,[](uv_async_t *req)->void {
                    auto this2=reinterpret_cast<TjsRuntimeWrap *>(req->data);
                    this2->runPendingJobs();
                });
                auto rt=this->rt;
                auto asyncReq=this->asyncReq;
                auto isErr=TJS_Run(this->rt);
                if(isErr){
                    //FIXME: Potential thread race.
                    this->rt=nullptr;
                }
                //Here `this` may has been deleted. Don't use it.
                uv_close(reinterpret_cast<uv_handle_t *>(asyncReq),[](uv_handle_t *req)-> void {delete req;});
                TJS_FreeRuntime(rt);
            });
        }
        void runJs(const std::string &jsCode){
            std::string validJs=jsCode;
            if(jsCode.at(-1)!=0){
                validJs=jsCode+"\0";
            }
            JS_Eval(TJS_GetJSContext(rt),jsCode.c_str(),jsCode.length(),"<annomous>",JS_EVAL_TYPE_GLOBAL);
        }
        void runPendingJobs(){
            auto queueCopy=this->jobs;
            uv_mutex_lock(&jobsMutex);
            this->jobs.clear();
            uv_mutex_unlock(&jobsMutex);
            for(auto it : queueCopy){
                (*it)();
                delete it;
            }
        }
        void postRunnable(std::function<void()> cb){
            uv_mutex_lock(&jobsMutex);
            jobs.push_back(new decltype(cb)(cb));
            uv_mutex_unlock(&jobsMutex);
            uv_async_send(asyncReq);
        }
        virtual ~TjsRuntimeWrap(){
            if(this->rt!=nullptr){
                TJS_Stop(this->rt);
            }
            uv_mutex_destroy(&jobsMutex);
        }
    };
    void init(){
        if(inited)return;
        pxprpc::defaultFuncMap.add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.NewRuntime", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                auto argc=para->nextInt();
                std::vector<std::string> args;
                for(int i1=0;i1<argc;i1++){
                    args.push_back(para->nextString());
                }
                auto r=new TjsRuntimeWrap();
                r->init(args);
                ret->resolve(r);
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.RunJs", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                auto tjs=static_cast<TjsRuntimeWrap *>(para->nextObject());
                auto jsCode=para->nextString();
                tjs->postRunnable([jsCode,tjs,ret]()-> void {
                    tjs->runJs(jsCode);
                    pxprpc_rtbridge_host::resolveTS(ret);
                });
            })
        );
        inited=1;
    }
}
