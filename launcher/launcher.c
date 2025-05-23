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
    int firstStartup=1;
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
    
    do {
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
      }
      fscanf(flagfile, "%s", buf);
      if (strcmp(buf, "reboot") == 0 || firstStartup) {
      } else if (strcmp(buf, "shutdown") == 0) {
        opType = 0;
      } else {
        opType = 0;
      }
    firstStartup=0;
      if (opType == 1) {
        free(buf);
        fclose(flagfile);
  #ifdef __WIN32
        system("\".\\bin\\xplatj.bat\"");
  #elif defined __linux__
        system("./bin/xplatj");
  #endif
      } else if (opType == 2) {
        free(buf);
        fclose(flagfile);
      
  #ifdef __WIN32
        system(".\\SDLLoader");
  #elif defined __linux__
        system("./SDLLoader");
  #endif
      } else {
      free(buf);
        fclose(flagfile);
        opType = 0;
      }
    
    } while (opType != 0);
  }else{
    //As a DLL Host.
    void (*entryFunc)(int argc,char *argv[]);
  #ifdef __WIN32
    HANDLE dll1=LoadLibrary(argv[1]);
    if(dll1==NULL){
      fprintf(stderr,"Load dll failed.\n");
      return 1;
    }
    entryFunc=(void (*)(int argc,char *argv[]))GetProcAddress(dll1,argv[2]);
    if(entryFunc==NULL){
      fprintf(stderr,"Entry function not found.\n");
      return 1;
    }
    entryFunc(argc,argv);
  #elif defined __linux__
    void *dll1=dlopen(argv[1],RTLD_LOCAL);
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
  #endif

  }
}