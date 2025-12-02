
#pragma once




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

    //There may be multiple process use same pxseed-loader in same time, use tag to distinct these,Use pid if not set.
    //For example, On android ,there is a service process and an activity process. Use a valid filename character as process tag.
    string processTag("");

    string pxseedLoaderDataDir("");
    string pathPartSep="/";
    string hostFlags(" ");

    uv_sem_t exitRequested;

    void pxseedLoaderRequestExit(){
        uv_sem_post(&exitRequested);
    }
    void waitForPxseedLoaderExit(){
        uv_sem_wait(&exitRequested);
        uv_sem_post(&exitRequested);
    }

    //NOTE:Will leak TJS Runtime, So only call once for one process.
    void tjsstart(){
        std::cerr<<"[txikijs]:initializing."<<std::endl;
        pxprpc_txikijs::init();
        pxprpc_txikijs::SetTjsStartupDir(pxseedLoaderDataDir);
        
        auto tjsWrap=new pxprpc_txikijs::TjsRuntimeWrap();
        tjsWrap->init([tjsWrap]()->void {
            std::cerr<<"[txikijs]:initialize done.importing '"<<pxseedLoaderDataDir+pathPartSep+"boot0.js'"<<std::endl;
            tjsWrap->runJs(string{"import(String.raw`"}+(pxseedLoaderDataDir+pathPartSep+"boot0.js")+"`);undefined;");
        });
    }

    pxprpc_rtbridge_host::TcpPxpRpcServer *pxprpcServer=nullptr;
    void initPxseedEnviron(){
        if(pxprpcServer==nullptr){
            auto port=2048;
            pxprpcServer=new pxprpc_rtbridge_host::TcpPxpRpcServer("127.0.0.1",2048);
            for(;port<20000;port+=2048){
                pxprpcServer->port=port;
                const char *err=pxprpcServer->start();
                if(err==nullptr)break;
            }
        }
    }

    void init(){
        if(inited)return;
        processTag=std::to_string(uv_os_getpid());
        uv_sem_init(&exitRequested, 0);       
        pxprpc::defaultFuncMap.add((new pxprpc::NamedFunctionPPImpl1())->init("pxprpc_PxseedLoader.tjsstart",
            [](Parameter* para,AsyncReturn* ret)->void {
                pxprpc_PxseedLoader::tjsstart();
				ret->resolve();
            })
        ).add((new pxprpc::NamedFunctionPPImpl1())->init("pxprpc_PxseedLoader.getLoaderInfo",
            [](Parameter* para,AsyncReturn* ret)->void {
                pxprpc::TableSerializer tab;
                tab.setColumnsInfo("sss",{"pxseedLoaderDataDir","processTag","hostFlags"});
                tab.addValue(pxseedLoaderDataDir)->addValue(processTag)->addValue(hostFlags);
				ret->resolve(tab.buildSer());
            })
        );

        #ifdef __WIN32
        hostFlags+="__WIN32 ";
        #endif

        #ifdef __WIN64
        hostFlags+="__WIN64 ";
        #endif

        #ifdef __linux__
        hostFlags+="__linux__ ";
        #endif

        #ifdef __ANDROID__
        hostFlags+="__ANDROID__ ";
        pxprpc::defaultFuncMap.add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_PxseedLoader.setAndroidInitInfo", [](Parameter *para, AsyncReturn *ret) -> void {
                //Must called by Java side to initialize android environment before other call.
                androidDataDir=para->nextString();
                androidApiVersion=para->nextInt();
                auto ptag=para->nextString();
                if(ptag.size()>0){
                    processTag=ptag;
                }
                pxseedLoaderDataDir=androidDataDir;
                //init stdout/stderr
                string stdoutFile(pxseedLoaderDataDir+pathPartSep+"stdout_"+processTag+".txt");
                string stderrFile(pxseedLoaderDataDir+pathPartSep+"stderr_"+processTag+".txt");
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
                initPxseedEnviron();
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_PxseedLoader.getAndroidInitInfo", [](Parameter *para, AsyncReturn *ret) -> void {
                auto ser=new pxprpc::Serializer();
                ser->prepareSerializing(64)->putString(androidDataDir)->putInt(androidApiVersion)->putString(processTag);
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
            const char *nodejs__main__argv[]={"node",jsmain.c_str(),"pxseedServer2023/entry",nullptr};
            nodejs__main(3,nodejs__main__argv);
            ret->resolve();
        })
        );
        #endif

        #ifndef __ANDROID__
        {
            char *exepathC=new char[1024];
            size_t exepathSize=1024;
            uv_exepath(exepathC,&exepathSize);
            string exepathCpp(exepathC);
            delete[] exepathC;
            if(exepathCpp.find('\\')!=std::string::npos){
                pathPartSep="\\";
            }
            auto dirPartEndAt=exepathCpp.find_last_of(pathPartSep[0]);
            if(dirPartEndAt==std::string::npos){
                pxseedLoaderDataDir="";
            }else{
                pxseedLoaderDataDir=exepathCpp.substr(0,dirPartEndAt)+pathPartSep+"data";
            }
            initPxseedEnviron();
        }
        #endif
        
        inited=1;
    }

}

extern "C"{
    //For dll user. NOTE:Will leak TJS Runtime, So only call once for one process. 
    extern void xplat_tjsloader_start_once(int waitExitRequested){
        pxprpc_PxseedLoader::init();
        pxprpc_PxseedLoader::tjsstart();
        if(waitExitRequested){
            pxprpc_PxseedLoader::waitForPxseedLoaderExit();
        }
    }
    
}