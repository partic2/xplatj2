
#pragma once



#include "pxprpc_rtbridge.h"
#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_pipe.h>
#include <pxprpc_rtbridge_host.hpp>
#include <pxprpc_rtbridge_base/init.hpp>
#include <unordered_map>

extern "C"{
#include <pwart.h>
}
#include <vector>
#include <map>

namespace pxprpc_pwart{
    using string=std::string;

    int inited=0;
    using MemoryChunk=pxprpc_rtbridge_base::MemoryChunk;
    using AsyncReturn=pxprpc::NamedFunctionPPImpl1::AsyncReturn;

    std::vector<class PwartNamespaceWrap *> createdNs;
    
    
    class PwartFunctionWrap:public pxprpc::PxpObject{
        public:
        pwart_wasm_function func=nullptr;
        void callSync(MemoryChunk *stackBuffer,int32_t offset){
            pwart_call_wasm_function(func,(void *)((char *)stackBuffer->base+offset));
        }
        void callAsync(MemoryChunk *stackBuffer,int32_t offset,AsyncReturn *aret){
            pxprpc_rtbridge_host::runInNewThread([=]()->void {
                pwart_call_wasm_function(func,(void *)((char *)stackBuffer->base+offset));
                pxprpc_rtbridge_host::resolveTS(aret);
            });
        }

    };

    
    class CppHostModule:public pxprpc::PxpObject{
        public:
        std::unordered_map<string,pwart_wasm_memory> memSyms;
        std::unordered_map<string,pwart_wasm_function> funcSyms;
        std::unordered_map<string,pwart_wasm_table> tableSyms;
        struct {
            pwart_host_module c;
            CppHostModule *cpp;
        } resolver;
        struct {
            pwart_named_module c;
        } mod;
        std::function<void(CppHostModule *)> onDel;
        CppHostModule(string name){
            resolver.cpp=this;
            resolver.c.resolve=[](struct pwart_host_module *_this,struct pwart_symbol_resolve_request *req)-> void {
                auto cppThis=reinterpret_cast<decltype(resolver) *>(_this)->cpp;
                if(req->kind==PWART_KIND_MEMORY){
                    auto found=cppThis->memSyms.find(req->import_field);
                    if(found!=cppThis->memSyms.end()){
                        req->result=&found->second;
                        return;
                    }
                }
                req->result=nullptr;
            };
            
            mod.c.name=name.c_str();
            mod.c.type=PWART_MODULE_TYPE_HOST_MODULE;
            mod.c.val.host=&resolver.c;
        }
        void setMemeryChunkSymbol(std::string &name,MemoryChunk *chunk){
            pwart_wasm_memory mem;
            mem.bytes=reinterpret_cast<uint8_t *>(chunk->base);
            mem.fixed=1;
            mem.pages=chunk->size/(64*1024);
            memSyms[name]=mem;
        }
        ~CppHostModule(){
            if(onDel){
                onDel(this);
            }
        }
    };

    const char *pxprpc4pwartfuncnames[]={
        "pipe_connect","io_bsend","io_brecv","io_buf_free","io_close"
    };
    void __wasm_pipe_connect(void *fp);
    void __wasm_io_bsend(void *fp);
    void __wasm_io_brecv(void *fp);
    void __wasm_io_buf_free(void *fp);
    void __wasm_io_close(void *fp);

    pwart_host_function_c pxprpc4pwartfuncc[]={
        &__wasm_pipe_connect,&__wasm_io_bsend,&__wasm_io_brecv,&__wasm_io_buf_free,&__wasm_io_close
    };

    class PwartNamespaceWrap : public pxprpc::PxpObject{
        public:

        pwart_namespace cObj=nullptr;
        void *mainLoop=nullptr;
        pwart_host_module *hostmod=nullptr;

        PwartNamespaceWrap(){
            cObj=pwart_namespace_new();
            hostmod=pwart_namespace_new_host_module(pxprpc4pwartfuncnames, pxprpc4pwartfuncc, 5);
            pwart_namespace_define_host_module(cObj,"pxprpc", hostmod);
        }
        char *defineWasmModule(std::string &name,uint8_t *wasmBytes,int32_t length){
            char *errMsg=nullptr;
            pwart_namespace_define_wasm_module(cObj, name.c_str(), reinterpret_cast<char *>(wasmBytes), length, &errMsg);
            return errMsg;
        }
        PwartFunctionWrap *getFunction(std::string moduleName,std::string functionName){
            auto mod=pwart_namespace_find_module(cObj,const_cast<char *>(moduleName.c_str()));
            auto found=pwart_get_export_function(mod->val.wasm,const_cast<char *>(functionName.c_str()));
            if(found==nullptr)return nullptr;
            auto f=new PwartFunctionWrap();
            f->func=found;
            return f;
        }
        void getMemoryInfo(std::string moduleName,std::string memoryName,AsyncReturn *ret){
            auto mod=pwart_namespace_find_module(cObj,const_cast<char *>(moduleName.c_str()));
            auto found=pwart_get_export_memory(mod->val.wasm,const_cast<char *>(memoryName.c_str()));
            if(found==nullptr){
                ret->reject("Symbol not found.");
                return;
            }
            auto ser=new pxprpc::Serializer();
            ser->prepareSerializing(32)->putLong(reinterpret_cast<int64_t>(found->bytes))
                ->putInt(found->pages);
            ret->resolve(ser);
        }
        std::unordered_map<string,CppHostModule *> managedHostMod;
        CppHostModule *createHostModule(string &name){
            auto found=managedHostMod.find(name);
            if(found!=managedHostMod.end()){
                return found->second;
            }else{
                auto mod=new CppHostModule(name);
                mod->onDel=[this](CppHostModule* mod)-> void {
                    this->removeHostModule(mod->mod.c.name);
                };
                managedHostMod[name]=mod;
                return mod;
            }
        }
        //Note: Only called by CppHostModule.onDel
        void removeHostModule(const char *name){
            managedHostMod.erase(name);
        }
        virtual ~PwartNamespaceWrap(){
            if(cObj!=nullptr){
                pwart_namespace_delete(cObj);
            }
            if(hostmod!=nullptr){
                pwart_namespace_delete_host_module(hostmod);
            }
        }
    };

    void __wasm_io_bsend(void *fp){
        void *sp=fp;
        struct pxprpc_abstract_io *io=(struct pxprpc_abstract_io *)pwart_rstack_get_ref(&sp);
        void *buf=pwart_rstack_get_ref(&sp);
        int32_t length=pwart_rstack_get_i32(&sp);
        struct pxprpc_buffer_part sbuf;
        sbuf.bytes.base=buf;
        sbuf.bytes.length=length;
        sbuf.next_part=nullptr;
        char *err=pxprpc_rtbridge_bsend(io,&sbuf);
        sp=fp;
        pwart_rstack_put_ref(&sp, err);
    }
    void __wasm_io_brecv(void *fp){
        void *sp=fp;
        struct pxprpc_abstract_io *io=(struct pxprpc_abstract_io *)pwart_rstack_get_ref(&sp);
        struct pxprpc_buffer_part sbuf;
        sbuf.bytes.base=nullptr;
        sbuf.bytes.length=0;
        sbuf.next_part=nullptr;
        char *err=pxprpc_rtbridge_brecv(io,&sbuf);
        sp=fp;
        pwart_rstack_put_ref(&sp,err);
        pwart_rstack_put_ref(&sp,sbuf.bytes.base);
        pwart_rstack_put_i32(&sp,sbuf.bytes.length);
    }
    void __wasm_io_buf_free(void *fp){
        void *sp=fp;
        struct pxprpc_abstract_io *io=(struct pxprpc_abstract_io *)pwart_rstack_get_ref(&sp);
        void *buf=pwart_rstack_get_ref(&sp);
        io->buf_free(buf);
    }
    void __wasm_pipe_connect(void *fp){
        void *sp=fp;
        char *name=(char *)pwart_rstack_get_ref(&sp);
        pxprpc_abstract_io *io=pxprpc_pipe_connect(name);
        sp=fp;
        pwart_rstack_put_ref(&sp,io);
    }
    void __wasm_io_close(void *fp){
        void *sp=fp;
        struct pxprpc_abstract_io *io=(struct pxprpc_abstract_io *)pwart_rstack_get_ref(&sp);
        io->close(io);
    }
    
    void init(){
        if(inited)return;
        pxprpc::defaultFuncMap.add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_pwart.new_namespace", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                ret->resolve(new PwartNamespaceWrap());
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_pwart.define_wasm_module", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto ns=static_cast<PwartNamespaceWrap *>(para->nextObject());
                auto name=para->nextString();
                auto wasmBytes=para->nextBytes();
                auto err=ns->defineWasmModule(name,std::get<1>(wasmBytes),std::get<0>(wasmBytes));
                if(err==nullptr){
                    ret->resolve();
                }else{
                    ret->reject(err);
                }
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_pwart.get_function", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto ns=static_cast<PwartNamespaceWrap *>(para->nextObject());
                auto moduleName=para->nextString();
                auto functionName=para->nextString();
                auto func=ns->getFunction(moduleName,functionName);
                ret->resolve(func);
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_pwart.call_function_sync", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto func=static_cast<PwartFunctionWrap *>(para->nextObject());
                auto stackBuffer=static_cast<MemoryChunk *>(para->nextObject());
                auto offset=para->nextInt();
                func->callSync(stackBuffer,offset);
                ret->resolve();
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_pwart.call_function_async", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto func=static_cast<PwartFunctionWrap *>(para->nextObject());
                auto stackBuffer=static_cast<MemoryChunk *>(para->nextObject());
                auto offset=para->nextInt();
                func->callAsync(stackBuffer,offset,ret);
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_pwart.get_memory_info", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto ns=static_cast<PwartNamespaceWrap *>(para->nextObject());
                auto moduleName=para->nextString();
                auto memoryName=para->nextString();
                ns->getMemoryInfo(moduleName,memoryName,ret);
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_pwart.new_host_module", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto ns=static_cast<PwartNamespaceWrap *>(para->nextObject());
                auto moduleName=para->nextString();
                ret->resolve(ns->createHostModule(moduleName));

            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_pwart.host_module_set_memory_chunk", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto mod=static_cast<CppHostModule *>(para->nextObject());
                auto name=para->nextString();
                auto chunk=static_cast<MemoryChunk *>(para->nextObject());
                mod->setMemeryChunkSymbol(name, chunk);
            })
        );
        inited=1;
    }
}
