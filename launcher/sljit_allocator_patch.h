
#ifndef SLJIT_ALLOCATOR_PATCH_H_
#define SLJIT_ALLOCATOR_PATCH_H_

#ifdef _WIN32
#include <windows.h>
#endif

#define SLJIT_ALLOCATOR_LOCK() 
#define SLJIT_ALLOCATOR_UNLOCK()

#ifdef __linux__
#include <stdlib.h>
#include <unistd.h>
#include <sys/mman.h>
#endif

#include "sljit_allocator.h"

#endif

