libuv is a multi-platform support library with a focus on asynchronous I/O. It was primarily developed for use by Nodejs, but it's also used by Luvit, Julia, pyuv, and others.

**this repo is aim to patch libuv to keep compatible with some old os, like android api 21 (android 5.0). windows7, etc.**

This repo use standalone file patch(compat.c/compat.h), to make tracking with lastest libuv source more easier.
