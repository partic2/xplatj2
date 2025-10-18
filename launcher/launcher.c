
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>



#ifdef __WIN32
#include <windows.h>
#elif defined __linux__
#include <dlfcn.h>
#endif

char *flagfilepath="./data/xplat-flag.txt";

static char *current_dir;

/* free by caller*/
static char *strconcat2(char *str1,char *str2){
  int len1=strlen(str1);
  int len2=strlen(str2);
  char *r=(char *)malloc(len1+len2+1);
  memmove(r,str1,len1);
  memmove(r+len1,str2,len2);
  r[len1+len2]=0;
  return r;
}

#ifdef __WIN32
static void *dlopen(const char *name,int flag){
  return LoadLibrary(name);
}
static void *dlsym(void *dll,const char *name){
  return GetProcAddress((HMODULE)dll, name);
}
static int dlclose(void *dll){
  return FreeLibrary((HMODULE)dll);
}
#endif




static void loadXplatDll(int opType){
   void *rtbridgeDll=dlopen("libpxprpc_rtbridge",0);
    if(rtbridgeDll==NULL){
        printf("pxprpc runtime bridge first try failed.\n");
        rtbridgeDll=dlopen("libpxprpc_rtbridge.so",0);
    }
    if(rtbridgeDll==NULL){
        printf("pxprpc runtime bridge second try failed.\n");
        rtbridgeDll=dlopen("libpxprpc_rtbridge.dll",0);
    }
    if(rtbridgeDll==NULL){
        printf("pxprpc runtime bridge load failed.maybe dependencies(libc++) missing?\n");
    }else{
        const char *(*pxprpc_rtbridge_host_ensureInited)();
        pxprpc_rtbridge_host_ensureInited=dlsym(rtbridgeDll,"pxprpc_rtbridge_host_ensureInited");
        if(pxprpc_rtbridge_host_ensureInited==NULL){
            printf("pxprpc runtime bridge load failed. No entry found.\n");
            dlclose(rtbridgeDll);
        }else{
            const char *err=pxprpc_rtbridge_host_ensureInited();
            if(err!=NULL && strcmp(err,"inited")!=0){
                printf("pxprpc runtime bridge init failed. %s",err);
            }else{
                printf("pxprpc runtime bridge loaded.\n");
                int (*SDL_main_func)(int argc, char *argv[]);
                void (*xplat_tjsloader_start_once)();
                if(opType==3){
                  xplat_tjsloader_start_once=dlsym(rtbridgeDll,"xplat_tjsloader_start_once");
                  if(xplat_tjsloader_start_once!=NULL){
                      xplat_tjsloader_start_once();
                  }
                }else if(opType==2){
                  SDL_main_func=dlsym(rtbridgeDll,"SDL_main");
                  if(SDL_main_func!=NULL){
                      SDL_main_func(0,NULL);
                  }
                }
            }
        }
    }
}

/*
  This is also a DLLHOST, Use as ./launcher dllpath function_name ...other_command
  The entry is: void (*function_name)(argc, argv);
*/
int main(int argc, char *argv[]) {
  if(argc==1){
    //Default launcher flow.
    char *thisPath = argv[0];
    char *pch;
    char *buf;
    int opType = 0;
    FILE *flagfile;
    for (pch = thisPath + strlen(thisPath); pch >= thisPath; pch--) {
      if (*pch == '/' || *pch == '\\') {
        break;
      }
    }
    if(pch>=thisPath){
      current_dir = (char *)malloc(pch - thisPath + 2);
      memcpy(current_dir, thisPath, pch - thisPath + 1);
      current_dir[pch - thisPath + 1] = 0;
      chdir(current_dir);
    }
    #if defined __linux__
      char *ldpath=getenv("LD_LIBRARY_PATH");
      char *newldpath;
      if(ldpath!=NULL){
        newldpath=strconcat2(ldpath,":.");
      }else{
        newldpath=".";
      }
      char *newldpatheq=strconcat2("LD_LIBRARY_PATH=",newldpath);
      if(ldpath!=NULL){
        free(newldpath);
      }
      putenv(newldpatheq);
      mkdir("./data",0777);
    #endif

    #if defined _WIN32
      mkdir("./data");
    #endif
    
    flagfile = fopen(flagfilepath, "r");
    if(flagfile==NULL){
      //implement copy tree by hand?
      #ifdef __WIN32
        system("xcopy .\\res .\\data /S /I /Y");
      #elif defined __linux__
        system("cp -r -T ./res ./data");
      #endif
      flagfile = fopen(flagfilepath, "r");
    }
    if (flagfile == NULL) {
      printf("%s not Found.",flagfilepath);
      return 1;
    }
    buf = (char *)malloc(0x100);
    fscanf(flagfile, "%s", buf);
    if (strcmp(buf, "sdl") == 0) {
      opType = 2;
    } else if (strcmp(buf,"webapp") == 0) {
      opType = 1;
    } else if (strcmp(buf,"tjs")==0){
      opType=3;
    }
    free(buf);
    fclose(flagfile);
    
    if(opType==1){
      #ifdef __WIN32
        system("\".\\bin\\xplatj.bat\"");
      #elif defined __linux__
            system("./bin/xplatj");
      #endif
    }else{
      loadXplatDll(opType);
    }
    
  }else{
    //As a DLL Host.
    void (*entryFunc)(int argc,char *argv[]);

    void *dll1=dlopen(argv[1],0);
    if(dll1==NULL){
      fprintf(stderr,"Load dll failed.\n");
      return 1;
    }
    entryFunc=dlsym(dll1,argv[2]);
    if(entryFunc==NULL){
      fprintf(stderr,"Entry function not found.\n");
      return 1;
    }
    entryFunc(argc,argv);
  }
}