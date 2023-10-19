#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>


char *flagfilepath="./data/xplat-flag.txt";

int main(int argc, char *argv[]) {
  char *thisPath = argv[0];
  char *pch;
  char *currentDir;
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
    currentDir = (char *)malloc(pch - thisPath + 2);
    memcpy(currentDir, thisPath, pch - thisPath + 1);
    currentDir[pch - thisPath + 1] = 0;
    chdir(currentDir);
    free(currentDir);
  }
  
  mkdir("./data");
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
}