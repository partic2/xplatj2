
#pragma once


extern "C"{
#include "pxprpc.h"
#include "pxprpc_rtbridge.h"
#include "uv.h"
#include <pxprpc_pipe.h>
#include <quickjs.h>
#include <tjs.h>
}
#include <pxprpc_pp.hpp >
#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_host.hpp>
#include <pxprpc_rtbridge_base/init.hpp>
#include <vector>
#include <set>

namespace pxprpc_txikijs{
    
    int inited=0;

    class TjsRuntimeWrap;

    using AsyncReturn=pxprpc::NamedFunctionPPImpl1::AsyncReturn;

    namespace __pxprpc4tjs{
        static JSClassID classId=0;
        static const JSClassDef classDef={
            .class_name="__pxprpc4tjs__",
            .gc_mark=[](JSRuntime *rt, JSValueConst val,
                           JS_MarkFunc *mark_func)-> void {}
        };

        static JSValue pipeConnect(JSContext *ctx, JSValue this_val, int argc, JSValue *argv) ;
        static JSValue ioSend(JSContext *ctx, JSValue this_val, int argc, JSValue *argv) ;
        static JSValue ioReceive(JSContext *ctx, JSValue this_val, int argc, JSValue *argv) ;
        static JSValue ioClose(JSContext *ctx, JSValue this_val, int argc, JSValue *argv) ;
        static JSValue accessMemory(JSContext *ctx, JSValue this_val, int argc, JSValue *argv) ;
        static const JSCFunctionListEntry props[] = {
            {
                "pipeConnect", JS_PROP_C_W_E, JS_DEF_CFUNC, 0, {                                            
                    .func = { 1, JS_CFUNC_generic, { .generic = pipeConnect } }        
                }
            },
            {
                "ioSend", JS_PROP_C_W_E, JS_DEF_CFUNC, 0, {                                            
                    .func = { 2, JS_CFUNC_generic, { .generic = ioSend } }        
                }
            },
            {
                "ioReceive", JS_PROP_C_W_E, JS_DEF_CFUNC, 0, {                                            
                    .func = { 2, JS_CFUNC_generic, { .generic = ioReceive } }        
                }
            },
            {
                "ioClose", JS_PROP_C_W_E, JS_DEF_CFUNC, 0, {                                            
                    .func = { 1, JS_CFUNC_generic, { .generic = ioClose } }        
                }
            },
            {
                "accessMemory", JS_PROP_C_W_E, JS_DEF_CFUNC, 0, {                                            
                    .func = { 2, JS_CFUNC_generic, { .generic = accessMemory } }        
                }
            }
        };
        void bindRpcBridge(JSContext *ctx,void *opaque){
            JS_NewClassID(JS_GetRuntime(ctx),&classId);
            JS_NewClass(JS_GetRuntime(ctx),classId,&classDef);
            auto inst = JS_NewObjectClass(ctx,classId);
            JS_SetPropertyFunctionList(ctx,inst,props,5);
            JSValue gobj=JS_GetGlobalObject(ctx);
            JS_DefinePropertyValueStr(ctx, gobj, "__pxprpc4tjs__", inst, JS_PROP_C_W_E);
            JS_FreeValue(ctx,gobj);
            JS_SetOpaque(inst, opaque);
        }
    }


    class TjsRuntimeWrap{
        public:

        TJSRuntime *rt=nullptr;
        uv_mutex_t jobsMutex;
        std::vector<std::function<void()> *> jobs;
        uv_async_t asyncReq;
        JSValue jsonevent=JS_UNDEFINED;
        int32_t refCount=1;
        std::set<struct pxprpc_abstract_io *> openedConn;
        
        TjsRuntimeWrap(){
            uv_mutex_init(&jobsMutex);
        }
        void init(std::function<void()> done){
            asyncReq.data=this;            
            pxprpc_rtbridge_host::runInNewThread([this,done]()->void {
                this->addRef();
                this->rt=TJS_NewRuntime();
                auto ctx=TJS_GetJSContext(this->rt);
                __pxprpc4tjs::bindRpcBridge(ctx,this);
                uv_async_init(TJS_GetLoop(this->rt),&this->asyncReq,[](uv_async_t *req)->void {
                    auto this2=reinterpret_cast<TjsRuntimeWrap *>(req->data);
                    this2->runPendingJobs();
                });
                done();
                TJS_Run(this->rt);
                uv_close(reinterpret_cast<uv_handle_t *>(&asyncReq),[](uv_handle_t *req)-> void {});
                for(int i1=0;i1<5;i1++){
                    uv_run(TJS_GetLoop(this->rt),UV_RUN_ONCE);
                }
                TJS_FreeRuntime(this->rt);
                this->rt=nullptr;
                this->freeRef();
            });
        }
        void runJs(const std::string &jsCode){
            JSValue jv=JS_Eval(TJS_GetJSContext(rt),jsCode.c_str(),jsCode.length(),"<annomous>",JS_EVAL_TYPE_GLOBAL);
            JS_FreeValue(TJS_GetJSContext(rt),jv);
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
            uv_async_send(&asyncReq);
        }
        //XXX:Thread racing
        virtual void deinitAndDelete(){
            if(this->rt!=nullptr){
                this->freeRef();
                TJS_Stop(this->rt);
            }else{
                this->freeRef();
            };
        }
        virtual ~TjsRuntimeWrap(){
            uv_mutex_destroy(&jobsMutex);
            for(auto it:this->openedConn){
                it->close(it);
            }
            this->openedConn.clear();
        }
        virtual void addRef(){
            refCount++;
        }
        virtual void freeRef(){
            refCount--;
            if(refCount<=0){
                delete this;
            }
        }
    };

    class TjsRuntimeWrapAsyncFree: public pxprpc::PxpObject{
        public:
        TjsRuntimeWrap *p;
        TjsRuntimeWrapAsyncFree(){
            p=new TjsRuntimeWrap();
        }
        virtual ~TjsRuntimeWrapAsyncFree(){
            p->deinitAndDelete();
        }
    };

    namespace __pxprpc4tjs{
        static JSValue pipeConnect(JSContext *ctx, JSValue this_val, int argc, JSValue *argv){
            if(JS_IsUndefined(this_val)){
                return JS_EXCEPTION;
            }
            auto thisWrap=static_cast<TjsRuntimeWrap *>(JS_GetOpaque(this_val,classId));
            if(thisWrap==nullptr){
                return JS_EXCEPTION;
            }
            auto name=JS_ToCString(ctx, argv[0]);
            auto r=pxprpc_rtbridge_pipe_connect(name);
            JS_FreeCString(ctx, name);
            if(r!=nullptr){
                thisWrap->openedConn.insert(r);
            }
            return JS_NewBigInt64(ctx, reinterpret_cast<int64_t>(r));
        }
        static JSValue ioSend(JSContext *ctx, JSValue this_val, int argc, JSValue *argv){
            if(JS_IsUndefined(this_val)){
                return JS_EXCEPTION;
            }
            auto thisWrap=static_cast<TjsRuntimeWrap *>(JS_GetOpaque(this_val,classId));
            if(thisWrap==nullptr){
                return JS_EXCEPTION;
            }
            int64_t i64;
            JS_ToBigInt64(ctx, &i64, argv[0]);
            auto io1=reinterpret_cast<struct pxprpc_abstract_io *>(i64);
            struct pxprpc_buffer_part buf;
            size_t size=0;
            buf.bytes.base=reinterpret_cast<void *>(JS_GetArrayBuffer(ctx, &size,argv[1]));
            buf.bytes.length=size;
            buf.next_part=nullptr;
            const char *err=pxprpc_rtbridge_bsend(io1,&buf);
            if(err==nullptr){
                return JS_UNDEFINED;
            }else{
                return JS_NewString(ctx,err);
            }
        }
        static JSValue ioReceive(JSContext *ctx, JSValue this_val, int argc, JSValue *argv) {
            if(JS_IsUndefined(this_val)){
                return JS_EXCEPTION;
            }
            auto thisWrap=static_cast<TjsRuntimeWrap *>(JS_GetOpaque(this_val,classId));
            if(thisWrap==nullptr){
                return JS_EXCEPTION;
            }
            int64_t i64;
            JS_ToBigInt64(ctx, &i64, argv[0]);
            auto io1=reinterpret_cast<struct pxprpc_abstract_io *>(i64);
            thisWrap->addRef();
            auto jsCb=JS_DupValue(ctx,argv[1]);
            pxprpc_rtbridge_host::postRunnable([thisWrap,io1,jsCb,ctx]()->void {
                pxprpc::iopp::receive(io1,
                [thisWrap,jsCb,ctx](const char *err,std::tuple<int32_t,void *> buf,std::function<void()> freebuf)-> void {
                    thisWrap->postRunnable([thisWrap,buf,freebuf,ctx,jsCb,err]()-> void {
                        if(err!=nullptr){
                            auto errStr=JS_NewString(ctx, err);
                            JS_Call(ctx,jsCb,JS_UNDEFINED,1,&errStr);
                            JS_FreeValue(ctx,errStr);
                        }else{
                            auto reJs=JS_NewArrayBufferCopy(ctx,reinterpret_cast<uint8_t *>(std::get<1>(buf)),std::get<0>(buf));
                            freebuf();
                            JS_Call(ctx,jsCb,JS_UNDEFINED,1,&reJs);
                            JS_FreeValue(ctx,reJs);
                        }
                        JS_FreeValue(ctx,jsCb);
                        thisWrap->freeRef();
                    });
                });
            });
            return JS_UNDEFINED;
        }
        static JSValue ioClose(JSContext *ctx, JSValue this_val, int argc, JSValue *argv) {
            if(JS_IsUndefined(this_val)){
                return JS_EXCEPTION;
            }
            auto thisWrap=static_cast<TjsRuntimeWrap *>(JS_GetOpaque(this_val,0));
            if(thisWrap==nullptr)return JS_EXCEPTION;
            int64_t i64;
            JS_ToBigInt64(ctx, &i64, argv[0]);
            auto io1=reinterpret_cast<struct pxprpc_abstract_io *>(i64);
            thisWrap->openedConn.erase(io1);
            io1->close(io1);
            return JS_EXCEPTION;
        }
        static JSValue accessMemory(JSContext *ctx, JSValue this_val, int argc, JSValue *argv){
            int64_t base;
            int32_t len;
            JS_ToBigInt64(ctx,&base,argv[0]);
            JS_ToInt32(ctx, &len, argv[1]);
            return JS_NewArrayBuffer(ctx, reinterpret_cast<uint8_t*>(base), len,
                 [](JSRuntime *rt, void *opaque, void *ptr)-> void {}, nullptr, true);
        }
    }

    void SetTjsStartupDir(const std::string &startupDir){
        TJS_SetTjsStartupDir(startupDir.c_str());
    }
    
    void init(){
        if(inited)return;
        pxprpc::defaultFuncMap.add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.NewRuntime", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto argc=para->nextInt();
                std::vector<std::string> args;
                for(int i1=0;i1<argc;i1++){
                    args.push_back(para->nextString());
                }
                auto r=new TjsRuntimeWrapAsyncFree();
                r->p->init([ret,r]()->void {
                    pxprpc_rtbridge_host::resolveTS(ret,r);
                });
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.RunJs", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto tjs=static_cast<TjsRuntimeWrapAsyncFree *>(para->nextObject());
                auto jsCode=para->nextString();
                tjs->p->postRunnable([jsCode,tjs,ret]()-> void {
                    tjs->p->runJs(jsCode);
                    pxprpc_rtbridge_host::resolveTS(ret);
                });
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.SetStartupDir", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto startupDir=para->nextString();
                SetTjsStartupDir(startupDir);
                ret->resolve();
            })
        );
        inited=1;
    }
}
