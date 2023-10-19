

#include <stdlib.h>
#include <stdio.h>

#ifdef __ANDROID__
#define SDL_DISABLE_IMMINTRIN_H 1
#endif


#include <SDL.h>

#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <pwart.h>
#include <pwart_syslib.h>
#include <string.h>




#define add_symbol(symbol_name) {.name=#symbol_name,.val=&symbol_name}

struct c_symbol{
    char *name;
    void *val;
};

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


int SDL_main(int argc,char *argv[]){
	SDL_Init(SDL_INIT_EVERYTHING);
    pwart_namespace ns=NULL;
    FILE *boot0=NULL;
    void *wasmbuf=NULL;
    void *stackbase=NULL;
    int nread=0;
    char *errmsg=NULL;
    pwart_module_state modstat=NULL;
    pwart_wasm_function startfn=NULL;
	SDL_Log("SDLLoader startup");

    {
        freopen(get_data_path("/stdout.txt"),"w",stdout);
        freopen(get_data_path("/stderr.txt"),"w",stderr);
        char *modpath=get_data_path("/boot0.wasm");
        void *stackbase = pwart_allocate_stack(64 * 1024);
        char *err=NULL;
        void *sp;
        long filesize;
        FILE *f;
        int len;
        int returncode;

        pwart_wasi_module_set_wasiargs(argc,argv);
        f = fopen(modpath, "rb");

        fseek(f,0,SEEK_END);
        filesize=ftell(f);

        if(filesize>1024*1024*1024){
            SDL_Log(".wasm file too large(>1GB)");
            return 1;
        }
        fseek(f,0,SEEK_SET);

        uint8_t *data = malloc(filesize);

        pwart_namespace *ns=pwart_namespace_new();
        pwart_syslib_load(ns);
        len = fread(data, 1, filesize, f);
        fclose(f);
        pwart_module_state stat=pwart_namespace_define_wasm_module(ns,"__main__",data,len,&err);
        free(data);
        if(err!=NULL){
            SDL_Log("error occur:%s\n",err);
            return 1;
        }
        struct pwart_wasm_memory *mem=pwart_get_export_memory(stat,"memory");
        pwart_wasi_module_set_wasimemory(mem);
        err=pwart_wasi_module_init();
        if(err!=NULL){
            SDL_Log("warning:%s uvwasi will not load",err);
        }
        pwart_wasm_function fn=pwart_get_start_function(stat);
        if(fn!=NULL){
            pwart_call_wasm_function(fn,stackbase);
        }
        fn=pwart_get_export_function(stat,"_start");
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
    fclose(stdout);
    fclose(stderr);
    SDL_Quit();
    return 0;
}