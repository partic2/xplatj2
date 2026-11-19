

#pragma once

#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_base/init.hpp>

extern "C"{
#include <uv.h>
#include <widcc_jit.h>
#ifdef __ANDROID__
#define SDL_DISABLE_IMMINTRIN_H 1
#endif

#include <stdlib.h>
#include <stdio.h>
#define SDL_MAIN_HANDLED
#include <SDL3/SDL.h>
#include "SDL3/SDL_main.h"
#include <tjs.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <string.h>
}


#include <pxprpc_rtbridge_host.hpp>

#include <pxprpc-pxseedloader/init.hpp>


namespace pxseed_sdlloader{

const char *data_dir=NULL;
std::string get_data_path(const char *subpath){
    #ifdef __ANDROID__
    if(data_dir==NULL){
        data_dir=SDL_GetAndroidInternalStoragePath();
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



const char *wasmnames[]={"set_sdl_event_handler"};

void (*sdlEventHandle)(SDL_Event *)=nullptr;

WidccRT *wcrt=nullptr;

void start(){
    SDL_Log("pxseed_sdlloader:SDLLoader startup");
    {
        //to init wasi module on windows, maybe changed in future.
        FILE *fh=NULL;
        fh=freopen(get_data_path("/stdout.txt").c_str(),"w",stdout);
        if(fh==NULL){
            SDL_Log("pxseed_sdlloader:freopen stdout error %s",strerror(errno));
        }else{
            setvbuf(fh, NULL, _IONBF, 0);
        }
        fh=freopen(get_data_path("/stderr.txt").c_str(),"w",stderr);
        if(fh==NULL){
            SDL_Log("pxseed_sdlloader:freopen stderr error %s",strerror(errno));
        }else{
            setvbuf(fh, NULL, _IONBF, 0);
        }
        std::string modpath=get_data_path("/boot0.c");
        const char *err=NULL;

        wcrt=widcc_rt_new();
        widcc_rt_add_var(wcrt,"pxseed_sdlloader_crt",wcrt);
        err=widcc_rt_load_code_file(wcrt,modpath.c_str(),nullptr);
        if(err!=NULL){
            SDL_Log("pxseed_sdlloader:warning:%s\n",err);
            return;
        }
        
        void (*pxseed_main)();
        pxseed_main=reinterpret_cast<void (*)()>(widcc_rt_lookup(wcrt,"pxseedloader_sdlmain"));

        if(pxseed_main==nullptr){
            SDL_Log("pxseed_sdlloader:warning:%s\n","'pxseedloader_sdlmain' function not found. ");
        }

    }
    
    SDL_Log("pxseed_sdlloader:.wasm exit\n");
    if(logfile!=NULL){
        fclose(logfile);
        logfile=NULL;
    }
}

void sdlloop(){
    SDL_Event event;
    while (SDL_WaitEvent(&event) != 0) {
        if(sdlEventHandle==nullptr){
            if (event.type == SDL_EVENT_QUIT) {
                break;
            }
        }else{
            sdlEventHandle(&event);
        }
    }
}

int inited=0;




using Parameter=pxprpc::NamedFunctionPPImpl1::Parameter;
using AsyncReturn=pxprpc::NamedFunctionPPImpl1::AsyncReturn;



void init(){
    if(inited)return;
    inited=1;
    pxprpc::defaultFuncMap.add((new pxprpc::NamedFunctionPPImpl1())->init("pxseed_sdlloader.RunBoot0Wasm",
        [](Parameter* para,AsyncReturn* ret)->void{
            pxprpc_rtbridge_host::runInNewThread([ret]()-> void {
                start();
                ret->resolve();
            });
        })
    ).add((new pxprpc::NamedFunctionPPImpl1())->init("pxseed_sdlloader.SDL_RunMain",
        [](Parameter* para,AsyncReturn* ret)->void{
            //On android , We just send event, handled by Java side, before we implement pxprpc client for C.
            #ifndef __ANDROID__
            pxprpc_rtbridge_host::runInNewThread([ret]()-> void {
                SDL_RunApp(0,nullptr,&SDL_main,nullptr);
            });
            #endif
            uv_timeval64_t tv;
            uv_gettimeofday(&tv);
            pxprpc_rtbridge_base::rtbvarsSet("pxseed_sdlloader.event.SDL_RunMain", std::to_string(tv.tv_sec*1000+tv.tv_usec/1000));
            ret->resolve();
        })
    ).add((new pxprpc::NamedFunctionPPImpl1())->init("pxseed_sdlloader.SetWasmSdlEventHandler",
        [](Parameter* para,AsyncReturn* ret)->void {
            auto handler=para->nextLong();
            sdlEventHandle=reinterpret_cast<void (*)(SDL_Event *)>(handler);
            ret->resolve();
        })
    );
    //sdlEventHandle

}

}

extern "C"{
    //Use SDL_main instead of replaced main.
    extern int SDL_main(int argc, char *argv[]){
        SDL_Init(SDL_INIT_AUDIO|SDL_INIT_VIDEO|SDL_INIT_EVENTS|SDL_INIT_CAMERA|SDL_INIT_SENSOR);
        pxseed_sdlloader::sdlloop();
        SDL_Quit();
        return 0;
    }
}