//Widcc JIT rpc binding

#pragma once

extern "C"{
#include <pxprpc.h>
#include <pxprpc_pipe.h>
#include <pxprpc_rtbridge.h>
}
#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_host.hpp>
#include <pxprpc_rtbridge_base/init.hpp>
#include <widcc_jit.h>

#include <array>
#include <cstdint>
#include <cstring>
#include <memory>
#include <string>
#include <vector>

namespace pxprpc_widcc_jit{
    using string=std::string;
    using AsyncReturn=pxprpc::NamedFunctionPPImpl1::AsyncReturn;
    using MemoryChunk=pxprpc_rtbridge_base::MemoryChunk;

    int inited=0;

    /*
        The JIT functions that call into this binding directly. They use the native C ABI
        (widcc generated code is ordinary machine code), unlike pwart's stack frame ABI.
        Registered on every new runtime under the same names that the pwart host module used.
    */
    static void *host_pipe_connect(const char *name){
        return pxprpc_rtbridge_pipe_connect(name);
    }
    static const char *host_io_bsend(void *io,void *buf,int32_t length){
        struct pxprpc_buffer_part sbuf;
        sbuf.bytes.base=buf;
        sbuf.bytes.length=length;
        sbuf.next_part=nullptr;
        return pxprpc_rtbridge_bsend(static_cast<struct pxprpc_abstract_io *>(io),&sbuf);
    }
    //On success returns NULL and stores the received block in *buf / *length.
    //The block must be released with host_io_buf_free.
    static const char *host_io_brecv(void *io,void **buf,int32_t *length){
        struct pxprpc_bytes rbuf;
        rbuf.base=nullptr;
        rbuf.length=0;
        const char *err=pxprpc_rtbridge_brecv(static_cast<struct pxprpc_abstract_io *>(io),&rbuf);
        *buf=rbuf.base;
        *length=static_cast<int32_t>(rbuf.length);
        return err;
    }
    static void host_io_buf_free(void *io,void *buf){
        static_cast<struct pxprpc_abstract_io *>(io)->buf_free(buf);
    }
    static void host_io_close(void *io){
        pxprpc_rtbridge_io_close(static_cast<struct pxprpc_abstract_io *>(io));
    }

    static const char *registerHostSymbols(WidccRT *rt){
        static const struct {
            const char *name;
            void *fn;
        } syms[]={
            {"pipe_connect",reinterpret_cast<void *>(&host_pipe_connect)},
            {"io_bsend",reinterpret_cast<void *>(&host_io_bsend)},
            {"io_brecv",reinterpret_cast<void *>(&host_io_brecv)},
            {"io_buf_free",reinterpret_cast<void *>(&host_io_buf_free)},
            {"io_close",reinterpret_cast<void *>(&host_io_close)},
        };
        for(auto &sym:syms){
            const char *err=widcc_rt_add_func(rt,sym.name,sym.fn);
            if(err!=nullptr){
                return err;
            }
        }
        return nullptr;
    }

    //Shared between the runtime object, module objects and symbol objects, so deleting
    //any RPC handle in any order keeps the runtime alive until the last one is gone.
    struct RuntimeState{
        WidccRT *rt=nullptr;
        ~RuntimeState(){
            if(rt!=nullptr){
                widcc_rt_free(rt);
                rt=nullptr;
            }
        }
    };

    class WidccRuntimeWrap:public pxprpc::PxpObject{
        public:
        std::shared_ptr<RuntimeState> state;
        WidccRuntimeWrap(WidccRT *rt){
            state=std::make_shared<RuntimeState>();
            state->rt=rt;
        }
    };

    class WidccModuleWrap:public pxprpc::PxpObject{
        public:
        std::shared_ptr<RuntimeState> state;
        WidccCode *code=nullptr;
        WidccModuleWrap(std::shared_ptr<RuntimeState> state,WidccCode *code):state(state),code(code){
        }
        const char *unload(){
            if(code==nullptr){
                return nullptr;
            }
            const char *err=widcc_rt_unload_code(state->rt,code);
            if(err==nullptr){
                code=nullptr;
            }
            return err;
        }
        virtual ~WidccModuleWrap(){
            unload();
        }
    };

    class WidccSymbolWrap:public pxprpc::PxpObject{
        public:
        std::shared_ptr<RuntimeState> state;
        string name;
        void *address=nullptr;
        WidccSymbolWrap(std::shared_ptr<RuntimeState> state,string &name,void *address)
            :state(state),name(name),address(address){
        }
        //Collect pointer-sized arguments from the chunk. Function results and arguments
        //are passed through the native C ABI, so only integer and pointer types (and
        //anything else that fits in a pointer-sized slot) can be used here.
        static int collectArgs(MemoryChunk *chunk,int32_t offset,void **argv,const char **err){
            if(chunk==nullptr){
                *err="Argument chunk is null.";
                return -1;
            }
            if(offset<0 || offset>chunk->size){
                *err="Argument offset is out of range.";
                return -1;
            }
            int32_t bytes=chunk->size-offset;
            if(bytes%(int32_t)sizeof(void *)!=0){
                *err="Argument chunk size is not a multiple of pointer size.";
                return -1;
            }
            int32_t count=bytes/(int32_t)sizeof(void *);
            if(count>16){
                *err="Too many arguments, at most 16 are supported.";
                return -1;
            }
            if(count>0 && chunk->base==nullptr){
                *err="Argument chunk has no backing memory.";
                return -1;
            }
            memcpy(argv,reinterpret_cast<char *>(chunk->base)+offset,bytes);
            return count;
        }
        int64_t callSync(MemoryChunk *chunk,int32_t offset,const char **err){
            std::array<void *,16> argv{};
            int count=collectArgs(chunk,offset,argv.data(),err);
            if(count<0){
                return 0;
            }
            return reinterpret_cast<int64_t>(simpleffi_call(address,count,argv.data()));
        }
        //Runs on a new thread: code compiled by one runtime must not be entered from
        //another thread while the same runtime is in use.
        void callAsync(MemoryChunk *chunk,int32_t offset,AsyncReturn *ret){
            std::array<void *,16> argv{};
            const char *err=nullptr;
            int count=collectArgs(chunk,offset,argv.data(),&err);
            if(count<0){
                ret->reject(err);
                return;
            }
            void *fn=address;
            pxprpc_rtbridge_host::runInNewThread([fn,count,argv,ret]()->void {
                auto args=argv;
                auto result=reinterpret_cast<int64_t>(simpleffi_call(fn,count,args.data()));
                pxprpc_rtbridge_host::resolveTS(ret,result);
            });
        }
    };

    void init(){
        if(inited)return;
        pxprpc::defaultFuncMap.add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_widcc_jit.new_runtime", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto rt=widcc_rt_new();
                if(rt==nullptr){
                    ret->reject("widcc_rt_new failed.");
                    return;
                }
                const char *err=registerHostSymbols(rt);
                if(err!=nullptr){
                    widcc_rt_free(rt);
                    ret->reject(err);
                    return;
                }
                ret->resolve(new WidccRuntimeWrap(rt));
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_widcc_jit.set_include_paths", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto rt=static_cast<WidccRuntimeWrap *>(para->nextObject());
                auto count=para->nextInt();
                if(count<0){
                    ret->reject("Invalid include path count.");
                    return;
                }
                std::vector<string> paths(count);
                for(int i1=0;i1<count;i1++){
                    paths[i1]=para->nextString();
                }
                std::vector<const char *> cpaths;
                for(auto &path:paths){
                    cpaths.push_back(path.c_str());
                }
                const char *err=widcc_rt_set_include_paths(rt->state->rt,
                    count>0?cpaths.data():nullptr,count);
                if(err!=nullptr){
                    ret->reject(err);
                }else{
                    ret->resolve();
                }
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_widcc_jit.add_host_function", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto rt=static_cast<WidccRuntimeWrap *>(para->nextObject());
                auto name=para->nextString();
                auto address=para->nextLong();
                const char *err=widcc_rt_add_func(rt->state->rt,name.c_str(),reinterpret_cast<void *>(address));
                if(err!=nullptr){
                    ret->reject(err);
                }else{
                    ret->resolve();
                }
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_widcc_jit.add_host_variable", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto rt=static_cast<WidccRuntimeWrap *>(para->nextObject());
                auto name=para->nextString();
                auto address=para->nextLong();
                const char *err=widcc_rt_add_var(rt->state->rt,name.c_str(),reinterpret_cast<void *>(address));
                if(err!=nullptr){
                    ret->reject(err);
                }else{
                    ret->resolve();
                }
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_widcc_jit.load_code", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto rt=static_cast<WidccRuntimeWrap *>(para->nextObject());
                auto path=para->nextString();
                auto source=para->nextString();
                WidccCode *code=nullptr;
                const char *err=widcc_rt_load_code(rt->state->rt,path.c_str(),source.c_str(),&code);
                if(err!=nullptr){
                    ret->reject(err);
                }else{
                    ret->resolve(new WidccModuleWrap(rt->state,code));
                }
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_widcc_jit.load_code_file", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto rt=static_cast<WidccRuntimeWrap *>(para->nextObject());
                auto path=para->nextString();
                WidccCode *code=nullptr;
                const char *err=widcc_rt_load_code_file(rt->state->rt,path.c_str(),&code);
                if(err!=nullptr){
                    ret->reject(err);
                }else{
                    ret->resolve(new WidccModuleWrap(rt->state,code));
                }
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_widcc_jit.unload_code", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto mod=static_cast<WidccModuleWrap *>(para->nextObject());
                const char *err=mod->unload();
                if(err!=nullptr){
                    ret->reject(err);
                }else{
                    ret->resolve();
                }
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_widcc_jit.lookup", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto rt=static_cast<WidccRuntimeWrap *>(para->nextObject());
                auto name=para->nextString();
                void *address=widcc_rt_lookup(rt->state->rt,name.c_str());
                if(address==nullptr){
                    ret->resolve(static_cast<pxprpc::PxpObject *>(nullptr));
                }else{
                    ret->resolve(new WidccSymbolWrap(rt->state,name,address));
                }
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_widcc_jit.list_symbols", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto rt=static_cast<WidccRuntimeWrap *>(para->nextObject());
                int count=widcc_rt_list_symbols(rt->state->rt,nullptr,0);
                std::vector<char *> names(count>0?count:0);
                if(count>0){
                    widcc_rt_list_symbols(rt->state->rt,names.data(),count);
                }
                auto ser=new pxprpc::Serializer();
                ser->prepareSerializing(16+count*8);
                ser->putInt(count);
                for(auto name:names){
                    ser->putString(string(name));
                }
                ret->resolve(ser);
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_widcc_jit.symbol_address", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto sym=static_cast<WidccSymbolWrap *>(para->nextObject());
                ret->resolve(static_cast<int64_t>(reinterpret_cast<intptr_t>(sym->address)));
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_widcc_jit.call_function_sync", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto sym=static_cast<WidccSymbolWrap *>(para->nextObject());
                auto chunk=static_cast<MemoryChunk *>(para->nextObject());
                auto offset=para->nextInt();
                const char *err=nullptr;
                auto result=sym->callSync(chunk,offset,&err);
                if(err!=nullptr){
                    ret->reject(err);
                }else{
                    ret->resolve(result);
                }
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_widcc_jit.call_function_async", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                auto sym=static_cast<WidccSymbolWrap *>(para->nextObject());
                auto chunk=static_cast<MemoryChunk *>(para->nextObject());
                auto offset=para->nextInt();
                sym->callAsync(chunk,offset,ret);
            })
        );
        inited=1;
    }
}
