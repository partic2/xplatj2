
#pragma once



#include "pxprpc_rtbridge.h"
#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
extern "C"{
    #include <pxprpc_pipe.h>
    #include <uv.h>
}
#include <pxprpc_rtbridge_host.hpp>
#include <pxprpc_rtbridge_base/init.hpp>
#include <pxprpc-txikijs/init.hpp>
#include <iostream>


//#define __ANDROID__

namespace pxprpc_PxseedLoader{
    using string=std::string;
    using Parameter=pxprpc::NamedFunctionPPImpl1::Parameter;
    using AsyncReturn=pxprpc::NamedFunctionPPImpl1::AsyncReturn;

    bool inited=false;
    
    #ifdef __ANDROID__
    string androidDataDir("");
    int32_t androidApiVersion=0;
    uv_lib_t *libnodeso=nullptr;
    #endif

    string pxseedLoaderDataDir("");
    string pathPartSep="/";

    void init(){
        if(inited)return;
        {
            char *exepathC=new char[1024];
            size_t exepathSize=1024;
            uv_exepath(exepathC,&exepathSize);
            string exepathCpp(exepathC);
            delete[] exepathC;
            if(exepathCpp.find('\\')!=std::string::npos){
                pathPartSep="\\";
            }
            pxseedLoaderDataDir=exepathCpp+pathPartSep+"data";
        }

        #ifdef __ANDROID__
        pxprpc::defaultFuncMap.add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_PxseedLoader.setAndroidInitInfo", [](Parameter *para, AsyncReturn *ret) -> void {
                //Must called by Java side to initialize android environment before other call.
                androidDataDir=para->nextString();
                androidApiVersion=para->nextInt();
                pxseedLoaderDataDir=androidDataDir;

                //init stdout/stderr
                string stdoutFile(pxseedLoaderDataDir+pathPartSep+"stdout.txt");
                string stderrFile(pxseedLoaderDataDir+pathPartSep+"stderr.txt");
                FILE *fh=freopen(stdoutFile.c_str(),"w",stdout);
                if(fh==NULL){
                    //Error
                }else{
                    setvbuf(fh, NULL, _IONBF, 0);
                }
                fh=freopen(stderrFile.c_str(),"w",stderr);
                if(fh==NULL){
                    //Error
                }else{
                    setvbuf(fh, NULL, _IONBF, 0);
                }
                ret->resolve();
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_PxseedLoader.getAndroidInitInfo", [](Parameter *para, AsyncReturn *ret) -> void {
                auto ser=new pxprpc::Serializer();
                ser->prepareSerializing(64)->putString(androidDataDir)->putInt(androidApiVersion);
                ret->resolve(ser);
            })
        ).add((new pxprpc::NamedFunctionPPImpl1())->init("pxprpc_PxseedLoader.loadNodeJSAndroid",
        [](Parameter* para,AsyncReturn* ret)->void {
            int err=-1;
            if(libnodeso==nullptr){
                libnodeso=new uv_lib_t();
                err=uv_dlopen("libnode.so", libnodeso);
            }
            if(err!=0){
                ret->reject(uv_dlerror(libnodeso));
                return;
            }
            void (*nodejs__main)(int argc,const char *argv[]);
            err=uv_dlsym(libnodeso, "nodejs__main", reinterpret_cast<void **>(&nodejs__main));
            if(err!=0){
                ret->reject("nodejs__main not found in libnode.so");
                return;
            }
            std::string jsmain=pxseedLoaderDataDir+pathPartSep+"pxseed"+pathPartSep+"www"+pathPartSep+"noderun.js";
            std::cout<<"jsmain:"<<jsmain<<std::endl;
            const char *nodejs__main__argv[]={"node",jsmain.c_str(),"pxseedServer2023/entry.js",nullptr};
            nodejs__main(3,nodejs__main__argv);
            ret->resolve();
        })
        );
        #endif
        
        inited=1;
    }

    //NOTE:Will leak TJS Runtime, So only call once for one process.
    void tjsstart(){
        pxprpc_txikijs::init();
        pxprpc_txikijs::SetTjsStartupDir(pxseedLoaderDataDir);
        auto tjsWrap=new pxprpc_txikijs::TjsRuntimeWrap();
        tjsWrap->init([tjsWrap]()->void {
            tjsWrap->runJs(string{"import('"}+pxseedLoaderDataDir+"/boot0.js');undefined;");
        });
    }

}
