#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char *argv[]) {
  char *thisPath = argv[0];
  char *pch;
  char *currentDir;
  char *buf;
  int opType = 0;
  int firstStartup=1;
  FILE *flatConfigFile;
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
  

  do {
    flatConfigFile = fopen("./res/flat", "r");
    if (flatConfigFile == NULL) {
      printf("./res/flat not Found.");
      return 1;
    }
    buf = (char *)malloc(0x100);
    fscanf(flatConfigFile, "%s", buf);
    if (strcmp(buf, "gdx") == 0) {
      opType = 1;
    } else if (strcmp(buf, "sdl") == 0) {
      opType = 2;
    } else if (strcmp(buf,"webapp") == 0) {
      opType = 1;
    }
    fscanf(flatConfigFile, "%s", buf);
    if (strcmp(buf, "reboot") == 0 || firstStartup) {
    } else if (strcmp(buf, "shutdown") == 0) {
      opType = 0;
    } else {
      opType = 0;
    }
	firstStartup=0;
    if (opType == 1) {
      free(buf);
      fclose(flatConfigFile);
#ifdef __WIN32
      system("cmd.exe /c \".\\bin\\xplatj.bat\"");
#elif defined __linux
      system("sh -c ./bin/xplatj");
#endif
    } else if (opType == 2) {
      free(buf);
      fclose(flatConfigFile);
	  
#ifdef __WIN32
      system(".\\bin\\SDLLoader");
#elif defined __linux
      system("./bin/SDLLoader");
#endif
    } else {
	  free(buf);
      fclose(flatConfigFile);
      opType = 0;
    }
	
  } while (opType != 0);
}