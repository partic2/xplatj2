

#pragma once

#include "pxprpc_ext.hpp"
#include "uv.h"
extern "C"{
#ifdef __ANDROID__
#define SDL_DISABLE_IMMINTRIN_H 1
#endif

#include <stdlib.h>
#include <stdio.h>
#define SDL_MAIN_HANDLED
#include <SDL.h>
#include <tjs.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <pwart.h>
#include <pwart_syslib.h>
#include <string.h>
}


#include <pxprpc_rtbridge_host.hpp>

#include <pxprpc-pxseedloader/init.hpp>


namespace xplat_sdlloader{

const char *data_dir=NULL;
std::string get_data_path(const char *subpath){
    #ifdef __ANDROID__
    if(data_dir==NULL){
        data_dir=SDL_AndroidGetInternalStoragePath();
    }
    #else
    if(data_dir==NULL){
        data_dir="./data";
    }
    #endif
    std::string datadirpp(data_dir);
    return datadirpp+subpath;
}

FILE *logfile=NULL;



typedef void *(*entry_func)(void *);

char* cmd_argv[]={NULL,NULL};

const char *wasmnames[]={"set_sdl_event_handler"};

pwart_wasm_function sdlEventHandle=nullptr;

void __wasm_set_sdl_event_handler(void *fp){
    void *sp=fp;
    sdlEventHandle=pwart_rstack_get_ref(&sp);
}

const pwart_host_function_c wasmfuncs[]={&__wasm_set_sdl_event_handler};

pwart_namespace ns=nullptr;
void start(){
    if(ns!=nullptr){
        //Only start once.
        return;
    }
    cmd_argv[0]=const_cast<char *>(get_data_path("/wasi-stub").c_str());
    SDL_Log("xplat:SDLLoader startup");
    {
        //to init wasi module on windows, maybe changed in future.
        FILE *fh=NULL;
        fh=freopen(get_data_path("/stdout.txt").c_str(),"w",stdout);
        if(fh==NULL){
            SDL_Log("xplat:freopen stdout error %s",strerror(errno));
        }else{
            setvbuf(fh, NULL, _IONBF, 0);
        }
        fh=freopen(get_data_path("/stderr.txt").c_str(),"w",stderr);
        if(fh==NULL){
            SDL_Log("xplat:freopen stderr error %s",strerror(errno));
        }else{
            setvbuf(fh, NULL, _IONBF, 0);
        }
        std::string modpath=get_data_path("/boot0.wasm");
        void *stackbase = pwart_allocate_stack(4096);
        char *err=NULL;
        long filesize;
        FILE *f;
        int len;

        pwart_wasi_module_set_wasiargs(1,cmd_argv);
        f = fopen(modpath.c_str(), "rb");
        
        if(f==NULL){
            SDL_Log("xplat:.wasm file open failed.");
            return;
        }else{
            SDL_Log("xplat:.wasm file opened.");
        }

        fseek(f,0,SEEK_END);
        filesize=ftell(f);

        if(filesize>1024*1024*1024){
            SDL_Log("xplat:.wasm file too large(>1GB)");
            fclose(f);
            return;
        }
        fseek(f,0,SEEK_SET);

        char *data = (char *)malloc(filesize);

        ns=pwart_namespace_new();
        auto hostmod=pwart_namespace_new_host_module(&wasmnames[0],const_cast<pwart_host_function_c *>(&wasmfuncs[0]),1);
        pwart_namespace_define_host_module(ns,"xplat-sdlloader",hostmod);
        SDL_Log("xplat:pwart namespace created.");
        pwart_syslib_load(ns);
        SDL_Log("xplat:syslib initialized.");
        len = fread(data, 1, filesize, f);
        SDL_Log("xplat:.wasm data readed.");
        fclose(f);
        pwart_module_state stat=pwart_namespace_define_wasm_module(ns,const_cast<char *>("__main__"),data,len,&err);
        free(data);
        if(err!=NULL){
            SDL_Log("xplat:error occur:%s\n",err);
            return;
        }
        SDL_Log("xplat:.wasm module compiled.");
        struct pwart_wasm_memory *mem=pwart_get_export_memory(stat,const_cast<char *>("memory"));
        pwart_wasi_module_set_wasimemory(mem);
        SDL_Log("xplat:wasi initialzed.");
        err=pwart_wasi_module_init();
        if(err!=NULL){
            SDL_Log("xplat:warning:%s uvwasi will not load",err);
        }
        pwart_wasm_function fn=pwart_get_start_function(stat);
        if(fn!=NULL){
            pwart_call_wasm_function(fn,stackbase);
        }
        fn=pwart_get_export_function(stat,const_cast<char *>("_start"));
        if(fn!=NULL){
            pwart_call_wasm_function(fn,stackbase);
        }else{
            SDL_Log("xplat:%s\n","'_start' function not found. ");
        }
        SDL_Log("xplat:%s\n",".wasm._start exited. ");
        pwart_free_stack(stackbase);
    }
    
    SDL_Log("xplat:.wasm exit\n");
    if(logfile!=NULL){
        fclose(logfile);
        logfile=NULL;
    }
}

void sdlloop(){
    int quit=0;
    SDL_Event event;
    void *fp=pwart_allocate_stack(4096);
    //Wait for 5 secs, for sdlEventHandle
    for(int i1=0;i1<5;i1++){
        if(sdlEventHandle!=nullptr){
            break;
        }
        uv_sleep(1000);
    }
    if(sdlEventHandle==nullptr){
        return;
    }
    while (SDL_WaitEvent(&event) != 0) {
        if(sdlEventHandle==nullptr){
            if (event.type == SDL_QUIT) {
                break;
            }
        }else{
            void *sp=fp;
            pwart_rstack_put_ref(&sp,&event);
            pwart_call_wasm_function(sdlEventHandle,fp);
        }
    }
    pwart_free_stack(fp);
}

int inited=0;




using Parameter=pxprpc::NamedFunctionPPImpl1::Parameter;
using AsyncReturn=pxprpc::NamedFunctionPPImpl1::AsyncReturn;



void init(){
    if(inited)return;
    inited=1;
    pxprpc::defaultFuncMap.add((new pxprpc::NamedFunctionPPImpl1())->init("xplat_sdlloader.start",
        [](Parameter* para,AsyncReturn* ret)->void{
            pxprpc_rtbridge_host::runInNewThread([ret]()-> void {
                start();
                ret->resolve();
            });
        })
    ).add((new pxprpc::NamedFunctionPPImpl1())->init("xplat_sdlloader.tjsstart",
        [](Parameter* para,AsyncReturn* ret)->void {
            pxprpc_PxseedLoader::tjsstart();
        })
    );
    

}

}

extern "C"{
    //For dll user. NOTE:Will leak TJS Runtime, So only call once for one process. 
    extern void xplat_tjsloader_start_once(){
        pxprpc_PxseedLoader::init();
        pxprpc_PxseedLoader::tjsstart();
    }
    //Use SDL_main instead of replaced main.
    extern int SDL_main(int argc, char *argv[]){
        SDL_Init(SDL_INIT_EVERYTHING);
        xplat_sdlloader::init();
        pxprpc_rtbridge_host::runInNewThread([]()-> void {
            xplat_sdlloader::start();
        });
        xplat_sdlloader::sdlloop();
        SDL_Quit();
        return 0;
    }
}