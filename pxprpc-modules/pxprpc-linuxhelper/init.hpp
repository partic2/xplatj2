#pragma once

#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_host.hpp>
#include <pxprpc_rtbridge_base/init.hpp>

extern "C"{
#include <stdlib.h>
#include <unistd.h>
#include <uv.h>
}


namespace pxprpc_linuxpty{

    
    int inited=0;
    void init(){
        if(inited)return;

        pxprpc::defaultFuncMap.add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_c_posix.load_dll", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    if(winpty_error_msg!=nullptr){
                        pxprpc_rtbridge_host::resolveTS(ret);
                        return;
                    }
                    int err=uv_dlopen("winpty.dll",&winptydll);
                    if(err<0){
                        pxprpc_rtbridge_host::rejectTS(ret,uv_dlerror(&winptydll));
                        return;
                    }
                    for(int t1=0;t1<100;t1++){
                        if(_loadsymtab[t1].name==nullptr){
                            break;
                        }
                        err=uv_dlsym(&winptydll,_loadsymtab[t1].name,_loadsymtab[t1].sym);
                        if(err<0){
                            pxprpc_rtbridge_host::rejectTS(ret,uv_err_name(err));
                            uv_dlclose(&winptydll);
                            winpty_error_msg=nullptr;
                            return;
                        }
                    }
                    pxprpc_rtbridge_host::resolveTS(ret);
                });
            })
        ).add((new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_winpty.open", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                if(winpty_error_msg==nullptr){
                    ret->reject("dll not loaded, call load_dll first.");
                    return;
                }
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    int64_t agentFlag=para->nextLong();
                    winpty_error_ptr_t err=NULL;
                    auto config=(*winpty_config_new)(agentFlag,&err);
                    if(err!=NULL){
                        pxprpc_rtbridge_host::rejectTS(ret,pxprpc_win32helpers::WcharToUtf8((*winpty_error_msg)(err)));
                        (*winpty_error_free)(err);
                        err=NULL;
                        return;
                    }
                    auto npty=(*winpty_open)(config,&err);
                    if(err!=NULL){
                        pxprpc_rtbridge_host::rejectTS(ret,pxprpc_win32helpers::WcharToUtf8((*winpty_error_msg)(err)));
                        (*winpty_error_free)(err);
                        err=NULL;
                        (*winpty_config_free)(config);
                        return;
                    }
                    auto ptyhandle=new WinptyHandle();
                    ptyhandle->npty=npty;
                    pxprpc_rtbridge_host::resolveTS(ret,ptyhandle);
                    (*winpty_config_free)(config);
                });
            })
        ).add((new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_winpty.spawn", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                if(winpty_error_msg==nullptr){
                    ret->reject("dll not loaded, call load_dll first.");
                    return;
                }
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    auto pty=static_cast<WinptyHandle *>(para->nextObject());
                    auto spawnFlag=para->nextLong();
                    auto appName=pxprpc_win32helpers::Utf8ToWchar(para->nextString());
                    auto cmdline=pxprpc_win32helpers::Utf8ToWchar(para->nextString());
                    auto cwd=pxprpc_win32helpers::Utf8ToWchar(para->nextString());
                    winpty_error_ptr_t err=NULL;
                    auto config2=(*winpty_spawn_config_new)(spawnFlag,appName.c_str(),cmdline.c_str(),
                        cwd.length()>0?cwd.c_str():nullptr,nullptr,&err);
                    if(err!=NULL){
                        pxprpc_rtbridge_host::rejectTS(ret,pxprpc_win32helpers::WcharToUtf8((*winpty_error_msg)(err)));
                        (*winpty_error_free)(err);
                        err=NULL;
                        return;
                    }
                    DWORD err2=0;
                    (*winpty_spawn)(pty->npty,config2,nullptr,nullptr,&err2,&err);
                    if(err!=NULL){
                        auto errmsg=pxprpc_win32helpers::WcharToUtf8((*winpty_error_msg)(err));
                        pxprpc_rtbridge_host::rejectTS(ret,errmsg+","+std::to_string(err2));
                        (*winpty_error_free)(err);
                        err=NULL;
                        (*winpty_spawn_config_free)(config2);
                        return;
                    }
                    pxprpc_rtbridge_host::resolveTS(ret);
                    (*winpty_spawn_config_free)(config2);
                });
            })
        ).add((new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_winpty.constdio_name", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                if(winpty_error_msg==nullptr){
                    ret->reject("dll not loaded, call load_dll first.");
                    return;
                }
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    auto pty=static_cast<WinptyHandle *>(para->nextObject());
                    auto conin=(*winpty_conin_name)(pty->npty);
                    auto conout=(*winpty_conout_name)(pty->npty);
                    auto conerr=(*winpty_conerr_name)(pty->npty);
                    auto ser=new pxprpc::Serializer();
                    ser->prepareSerializing(36);
                    ser->putString(pxprpc_win32helpers::WcharToUtf8(conin));
                    ser->putString(pxprpc_win32helpers::WcharToUtf8(conout));
                    if(conerr!=nullptr){
                        ser->putString(pxprpc_win32helpers::WcharToUtf8(conin));
                    }else{
                        ser->putString("");
                    }
                    pxprpc_rtbridge_host::resolveTS(ret,ser);
                });
            })
        ).add((new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_winpty.set_size", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, pxprpc::NamedFunctionPPImpl1::AsyncReturn *ret) -> void {
                if(winpty_error_msg==nullptr){
                    ret->reject("dll not loaded, call load_dll first.");
                    return;
                }
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    winpty_error_ptr_t err=NULL;
                    auto pty=static_cast<WinptyHandle *>(para->nextObject());
                    auto cols=para->nextInt();
                    auto rows=para->nextInt();
                    if(err!=NULL){
                        pxprpc_rtbridge_host::rejectTS(ret,pxprpc_win32helpers::WcharToUtf8((*winpty_error_msg)(err)));
                        (*winpty_error_free)(err);
                        err=NULL;
                    }else{
                        pxprpc_rtbridge_host::resolveTS(ret);
                    }
                });
            })
        );
        inited=1;
    }
}