
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




static char *current_dir;
static char *flagfilepath="./data/pxseedloader-flags.txt";

/* free by caller*/
static char *strconcat2(const char *str1,const char *str2){
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
  return (void *)(GetProcAddress((HMODULE)dll, name));
}
static int dlclose(void *dll){
  return FreeLibrary((HMODULE)dll);
}
#endif




static void loadMainDll(){
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
        pxprpc_rtbridge_host_ensureInited=(const char *(*)())(dlsym(rtbridgeDll,"pxprpc_rtbridge_host_ensureInited"));
        if(pxprpc_rtbridge_host_ensureInited==NULL){
            printf("pxprpc runtime bridge load failed. No entry found.\n");
            dlclose(rtbridgeDll);
        }else{
            const char *err=pxprpc_rtbridge_host_ensureInited();
            if(err!=NULL && strcmp(err,"inited")!=0){
                printf("pxprpc runtime bridge init failed. %s",err);
            }else{
                printf("pxprpc runtime bridge loaded.\n");
                //int (*SDL_main_func)(int argc, char *argv[]);
                void (*xplat_tjsloader_start_once)(int waitExitRequested);
                xplat_tjsloader_start_once=(void (*)(int))(dlsym(rtbridgeDll,"xplat_tjsloader_start_once"));
                if(xplat_tjsloader_start_once!=NULL){
                    xplat_tjsloader_start_once(1);
                }
            }
        }
    }
}

#if defined __linux__
static char *ldpatheq=NULL;
#endif

int main(int argc, char *argv[]) {

    //Default launcher flow.
    char *thisPath = argv[0];
    char *pch;

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
    {
      const char *ldpath=getenv("LD_LIBRARY_PATH");
      char *temppath=NULL;
      if(ldpath==NULL){
        temppath=strconcat2("", ".");
      }else{
        temppath=strconcat2(ldpath, ":.");
      }
      ldpatheq=strconcat2("LD_LIBRARY_PATH=", temppath);
      free(temppath);
      putenv(ldpatheq);
      mkdir("./data",0777);
    }
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
      printf("flag file %s not found.\n",flagfilepath);
      return 1;
    }
    
    char *tbuf=malloc(1001);
    while(!feof(flagfile)){
      fscanf(flagfile,"%1000s",tbuf);
      /* check flag */
    }
    free(tbuf);
    fclose(flagfile);
    
    loadMainDll();

    return 0;
    
}