
#include <stdlib.h>
#include <stdio.h>

#ifdef __ANDROID__
#define SDL_DISABLE_IMMINTRIN_H 1
#endif


#include <SDL.h>
#include <SDL_main.h>



int main(int argc,char *argv[]){
	SDL_Init(SDL_INIT_EVERYTHING);
    
    void *rtbridgeDll=SDL_LoadObject("libpxprpc_rtbridge");
    if(rtbridgeDll==NULL){
        SDL_Log("pxprpc runtime bridge first try failed. %s",SDL_GetError());
        rtbridgeDll=SDL_LoadObject("libpxprpc_rtbridge.so");
    }
    if(rtbridgeDll==NULL){
        SDL_Log("pxprpc runtime bridge second try failed. %s",SDL_GetError());
        rtbridgeDll=SDL_LoadObject("libpxprpc_rtbridge.dll");
    }
    if(rtbridgeDll==NULL){
        SDL_Log("pxprpc runtime bridge load failed.maybe dependencies(libc++) missing? %s",SDL_GetError());
    }else{
        const char *(*pxprpc_rtbridge_host_ensureInited)();
        pxprpc_rtbridge_host_ensureInited=SDL_LoadFunction(rtbridgeDll,"pxprpc_rtbridge_host_ensureInited");
        if(pxprpc_rtbridge_host_ensureInited==NULL){
            SDL_Log("pxprpc runtime bridge load failed. No entry found.");
            SDL_UnloadObject(rtbridgeDll);
        }else{
            char *err=pxprpc_rtbridge_host_ensureInited();
            if(err!=NULL && strcmp(err,"inited")!=0){
                SDL_Log("pxprpc runtime bridge init failed. %s",err);
            }else{
                SDL_Log("pxprpc runtime bridge loaded.");
                void (*xplat_sdlloader_start)();
                xplat_sdlloader_start=SDL_LoadFunction(rtbridgeDll,"xplat_sdlloader_start");
                if(xplat_sdlloader_start!=NULL){
                    xplat_sdlloader_start();
                }
            }
        }
    }
    SDL_Quit();
    return 0;
}