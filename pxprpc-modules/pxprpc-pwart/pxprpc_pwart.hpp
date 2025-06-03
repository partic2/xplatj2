
#pragma once



#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_pipe.h>
#include <pxprpc_rtbridge_host.hpp>
#include <pxprpc_rtbridge_base/init.hpp>
#include <unordered_map>

extern "C"{
#include <pwart.h>
}
#include <iterator>
#include <vector>
#include <map>

namespace pxprpc_pwart{
    using string=std::string;

    int inited=0;
    using MemoryChunk=pxprpc_rtbridge_base::MemoryChunk;
    using AsyncReturn=pxprpc::NamedFunctionPPImpl1::AsyncReturn;
    
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

    class PwartNamespaceWrap : public pxprpc::PxpObject{
        public:

        pwart_namespace cObj=nullptr;

        PwartNamespaceWrap(){
            cObj=pwart_namespace_new();
        }
        char *defineWasmModule(std::string &name,uint8_t *wasmBytes,int32_t length){
            char *errMsg=nullptr;
            pwart_namespace_define_wasm_module(cObj,const_cast<char *>(name.c_str()),(char *)wasmBytes,length,&errMsg);
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
        }
    };
    
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
