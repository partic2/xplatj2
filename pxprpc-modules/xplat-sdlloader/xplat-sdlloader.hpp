



extern "C"{
#ifdef __ANDROID__
#define SDL_DISABLE_IMMINTRIN_H 1
#endif

#include <stdlib.h>
#include <stdio.h>

#include <SDL.h>

#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <pwart.h>
#include <pwart_syslib.h>
#include <string.h>
}


#include <pxprpc_rtbridge_host.hpp>

namespace xplat_sdlloader{

char path_buf[256];
const char *data_dir=NULL;
char *get_data_path(const char *subpath){
    #ifdef __ANDROID__
    if(data_dir==NULL){
        data_dir=SDL_AndroidGetInternalStoragePath();
    }
    #else
    if(data_dir==NULL){
        data_dir="./data";
    }
    #endif
    strcpy(path_buf,data_dir);
    strcat(path_buf,subpath);
    return path_buf;
}

FILE *logfile=NULL;



typedef void *(*entry_func)(void *);

char* cmd_argv[]={NULL};
void start(){
    SDL_Init(SDL_INIT_EVERYTHING);
    SDL_Log("SDLLoader startup");
    {
        //to init wasi module on windows, maybe changed in future.
        freopen(get_data_path("/xplat-flag.txt"),"r",stdin);
        freopen(get_data_path("/stdout.txt"),"w",stdout);
        freopen(get_data_path("/stderr.txt"),"w",stderr);
        char *modpath=get_data_path("/boot0.wasm");
        void *stackbase = pwart_allocate_stack(64 * 1024);
        char *err=NULL;
        long filesize;
        FILE *f;
        int len;

        pwart_wasi_module_set_wasiargs(0,cmd_argv);
        f = fopen(modpath, "rb");
        
        if(f==NULL){
            SDL_Log(".wasm file open failed.");
            return;
        }

        fseek(f,0,SEEK_END);
        filesize=ftell(f);

        if(filesize>1024*1024*1024){
            SDL_Log(".wasm file too large(>1GB)");
            return;
        }
        fseek(f,0,SEEK_SET);

        char *data = (char *)malloc(filesize);

        pwart_namespace ns=pwart_namespace_new();
        pwart_syslib_load(ns);
        len = fread(data, 1, filesize, f);
        fclose(f);
        pwart_module_state stat=pwart_namespace_define_wasm_module(ns,const_cast<char *>("__main__"),data,len,&err);
        free(data);
        if(err!=NULL){
            SDL_Log("error occur:%s\n",err);
            return;
        }
        struct pwart_wasm_memory *mem=pwart_get_export_memory(stat,const_cast<char *>("memory"));
        pwart_wasi_module_set_wasimemory(mem);
        err=pwart_wasi_module_init();
        if(err!=NULL){
            SDL_Log("warning:%s uvwasi will not load",err);
        }
        pwart_wasm_function fn=pwart_get_start_function(stat);
        if(fn!=NULL){
            pwart_call_wasm_function(fn,stackbase);
        }
        fn=pwart_get_export_function(stat,const_cast<char *>("_start"));
        if(fn!=NULL){
            pwart_call_wasm_function(fn,stackbase);
        }else{
            SDL_Log("%s\n","'_start' function not found. ");
        }
        pwart_free_stack(stackbase);
        pwart_namespace_delete(ns);
    }
    
    SDL_Log("exit\n");
    if(logfile!=NULL){
        fclose(logfile);
        logfile=NULL;
    }
    SDL_Quit();
}
void init(){
    pxprpc::defaultFuncMap.add((new pxprpc::NamedFunctionPPImpl1())->init("xplat_sdlloader.start",
        [](auto para,auto ret)->void{
            start();
            ret->resolve();
        }));
}

}

extern "C"{
    //For dll user.
    extern void xplat_sdlloader_start(){
        xplat_sdlloader::start();
    }
}