

#include <stdlib.h>
#include <stdio.h>

#ifdef __ANDROID__
#define SDL_DISABLE_IMMINTRIN_H 1
#define BOOT_DIR "/sdcard/xplat"
#define DATA_DIR "/data/data/project.xplat/files"
#else 
#define BOOT_DIR "res"
#define DATA_DIR "data"
#endif


#include <SDL.h>

#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <libtcc.h>
#include <string.h>

#include "sljit_allocator.c"



#define add_symbol(symbol_name) {.name=#symbol_name,.val=&symbol_name}

struct c_symbol{
    char *name;
    void *val;
};


FILE *logfile=NULL;

int log3(const char *str){
	if(logfile==NULL){
		logfile=fopen(DATA_DIR"/stdlog.txt","ab+");
	}
	fwrite(str,strlen(str),1,logfile);
	fflush(logfile);
	return 0;
}

struct c_symbol initSym[]={
    //memory
    add_symbol(malloc),add_symbol(realloc),add_symbol(free),add_symbol(memmove),add_symbol(memcpy),
    //basic IO 
    add_symbol(fopen),add_symbol(fclose),add_symbol(fread),add_symbol(fwrite),add_symbol(fseek),add_symbol(log3),
    //environ
    add_symbol(getenv),add_symbol(putenv),
    //TinyCC API
    add_symbol(tcc_new),add_symbol(tcc_delete),add_symbol(tcc_set_lib_path),
    add_symbol(tcc_set_error_func),add_symbol(tcc_get_error_func),add_symbol(tcc_get_error_opaque),
    add_symbol(tcc_set_options),add_symbol(tcc_add_include_path),add_symbol(tcc_add_sysinclude_path),
    add_symbol(tcc_define_symbol),add_symbol(tcc_undefine_symbol),add_symbol(tcc_add_file),
    add_symbol(tcc_compile_string),add_symbol(tcc_set_output_type),add_symbol(tcc_add_library_path),
    add_symbol(tcc_add_symbol),add_symbol(tcc_output_file),
    add_symbol(tcc_relocate),add_symbol(tcc_get_symbol),add_symbol(tcc_list_symbols),
    //SLJIT allocator
    add_symbol(sljit_malloc_exec),add_symbol(sljit_free_exec),add_symbol(sljit_free_unused_memory_exec),
    //SDL
    add_symbol(SDL_LoadFunction),add_symbol(SDL_LoadObject),add_symbol(SDL_UnloadObject),
    //end
    {"",NULL}
};

typedef void *(*entry_func)(void *);


int SDL_main(int argc,char *argv[]){
	
    struct stat fileStat;
    int boot0;
	
	log3("SDLLoader startup\n");
	FILE *redirecterr=freopen(DATA_DIR"/stderr.txt","ab+",stderr);
	
    boot0=open(BOOT_DIR"/boot0.c",O_RDONLY);
	
	fflush(stdout);
    #if DEBUG == 1
	log3("open file..."BOOT_DIR"/boot0.c\n");
    #endif
    if(boot0>=0){
        close(boot0);
        TCCState *tccStat=tcc_new();
        tcc_set_options(tccStat,"-nostdlib");
        tcc_add_file(tccStat,BOOT_DIR"/boot0.c");
        tcc_set_output_type(tccStat,TCC_OUTPUT_OBJ);
        tcc_output_file(tccStat,DATA_DIR"/boot0.o");
        tcc_delete(tccStat);
    }else{
		log3("failed\n");
	}
	log3("open file..."DATA_DIR"/boot0.o\n");
    boot0=open(DATA_DIR"/boot0.o",O_RDONLY);
    if(boot0>0){
        close(boot0);
        TCCState *tccStat=tcc_new();
        tcc_add_file(tccStat,DATA_DIR"/boot0.o");
        tcc_set_output_type(tccStat,TCC_OUTPUT_MEMORY);
        tcc_set_options(tccStat,"-nostdlib");
		log3("register symbol...\n");
		for(struct c_symbol *p=initSym;p->val!=NULL;p++){
            tcc_add_symbol(tccStat,p->name,p->val);
        }
		log3("allocate executable memory...\n");
        int memRequire=tcc_relocate(tccStat,NULL);
        void *mem=sljit_malloc_exec(memRequire);
		log3("relocate elf...\n");
        tcc_relocate(tccStat,mem);
		log3("find entry function _start");
		entry_func entry=(entry_func)tcc_get_symbol(tccStat,"_start");
        tcc_delete(tccStat);
		if(entry==NULL){
			log3("entry function not found...\n");
		}else{
			log3("run entry function...\n");
			entry(NULL);
		}
		log3("entry function returned...\n");
        sljit_free_exec(mem);
    }else{
        log3("failed\n");
    }
	log3("exit\n");
	if(logfile!=NULL){
		fclose(logfile);
		logfile=NULL;
	}
	fclose(redirecterr);
    return 0;
}