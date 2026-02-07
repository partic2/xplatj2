#pragma once

// WIP For multi-thread issue

#include <string>
extern "C"{
#include "pxprpc.h"
#include <pxprpc_pipe.h>
#include <mbedtls/ssl.h>
#include <mbedtls/entropy.h>
#include <mbedtls/ctr_drbg.h>
}

#include <pxprpc_pp.hpp>
#include <pxprpc_ext.hpp>
#include <pxprpc_rtbridge_host.hpp>
#include <pxprpc_rtbridge_base/init.hpp>
#include <list>


namespace pxprpc_txikijs{

    using string=std::string;
    using AsyncReturn=pxprpc::NamedFunctionPPImpl1::AsyncReturn;
    using Parameter=pxprpc::NamedFunctionPPImpl1::Parameter;

    int __mbedtls_ssl_send_client(void *ctx,const unsigned char *buf,size_t len);
    int __mbedtls_ssl_recv_client(void *ctx,unsigned char *buf,size_t len);

    

    class SslClientAsync{
        mbedtls_ssl_context ssl;
        mbedtls_ssl_config conf;
        mbedtls_entropy_context entropy;
        mbedtls_ctr_drbg_context ctr_drbg;
        public:
        string hostname;
        std::list<std::pair<int,unsigned char *>> C2SCipher;
        std::list<std::pair<int,unsigned char *>> S2CCipher;
        SslClientAsync(){
            mbedtls_ssl_init(&ssl);
            mbedtls_ssl_config_init(&conf);
            mbedtls_entropy_init(&entropy);
            mbedtls_ctr_drbg_init(&ctr_drbg);
        }
        const char *configure(){
            int ret;

            if ((ret = mbedtls_ctr_drbg_seed(&ctr_drbg, mbedtls_entropy_func, &entropy,
                                            nullptr, 0)) != 0) {
                return "Failed in mbedtls_ctr_drbg_seed";
            }

            if ((ret = mbedtls_ssl_config_defaults(&conf,
                            MBEDTLS_SSL_IS_CLIENT,  
                            MBEDTLS_SSL_TRANSPORT_STREAM, 
                            MBEDTLS_SSL_PRESET_DEFAULT)) != 0) {
                return "Failed in mbedtls_ssl_config_defaults";
            }

            mbedtls_ssl_conf_authmode(&conf, MBEDTLS_SSL_VERIFY_NONE);

            mbedtls_ssl_conf_rng(&conf, mbedtls_ctr_drbg_random, &ctr_drbg);

            if ((ret = mbedtls_ssl_setup(&ssl, &conf)) != 0) {
                return "Failed in mbedtls_ssl_setup";
            }
            
            if ((ret = mbedtls_ssl_set_hostname(&ssl, hostname.c_str())) != 0) {
                return "Failed to set hostname";
            }

            mbedtls_ssl_set_bio(&ssl, this, __mbedtls_ssl_send_client, __mbedtls_ssl_recv_client, NULL);
            return nullptr;
        }
        virtual int cipherSend(const unsigned char *buf,size_t len){
            std::pair<int,unsigned char *> pack;
            pack.second=new unsigned char[len];
            pack.first=len;
            memcpy(pack.second,buf,len);
            this->C2SCipher.push_back(pack);
            return len;
        }
        virtual int cipherRecv(unsigned char *buf,size_t len){
            if(this->S2CCipher.size()==0){
                return MBEDTLS_ERR_SSL_WANT_READ;
            }
            auto& pack=this->S2CCipher.front();
            if(len<pack.first){
                memmove(buf,pack.second,len);
                auto newbuf=new unsigned char[pack.first-len];
                memmove(pack.second,pack.second+len,pack.first-len);
                pack.first=pack.first-len;
                return len;
            }else{
                memmove(buf,pack.second,pack.first);
                len=pack.first;
                delete[] pack.second;
                this->S2CCipher.pop_front();
                return len;
            }
        }
        virtual int plainSend(const unsigned char *buf,int len){
            return mbedtls_ssl_write(&ssl,buf,len);
        }
        virtual int plainRecv(unsigned char *buf,int len){
            return mbedtls_ssl_read(&ssl,buf,len);
        }
        virtual void deinitAndDelete(){
            //mbedtls_ssl_close_notify(&ssl);
            mbedtls_ssl_free(&ssl);
            mbedtls_ssl_config_free(&conf);
            mbedtls_ctr_drbg_free(&ctr_drbg);
            mbedtls_entropy_free(&entropy);
            for(auto it : this->C2SCipher){
                delete[] it.second;
            }
            for(auto it : this->S2CCipher){
                delete[] it.second;
            }
            delete this;
        }
    };

    int __mbedtls_ssl_send_client(void *ctx,const unsigned char *buf,size_t len){
        auto cppctx=static_cast<SslClientAsync *>(ctx);
        return cppctx->cipherSend(buf,len);
    }

    int __mbedtls_ssl_recv_client(void *ctx,unsigned char *buf,size_t len){
        auto cppctx=static_cast<SslClientAsync *>(ctx);
        return cppctx->cipherRecv(buf,len);
    }

    class SslClient:public pxprpc::PxpObject{
        public:
        SslClientAsync *value=nullptr;
        SslClient(){
            value=new SslClientAsync();
        }
        virtual ~SslClient(){
            if(value!=nullptr){
                value->deinitAndDelete();
            }
        }
    };

    void mbedtlsInit(){
        pxprpc::defaultFuncMap.add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.NewSslClientContext", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    auto hostname=para->nextString();
                    auto ctx=new SslClient();
                    ctx->value->hostname=hostname;
                    auto err=ctx->value->configure();
                    if(err==nullptr){
                        pxprpc_rtbridge_host::resolveTS(ret,ctx);
                    }else{
                        pxprpc_rtbridge_host::rejectTS(ret,err);
                        delete ctx;
                    }
                });
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.SslClientPopCipherSend", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    auto ctx=static_cast<SslClient *>(para->nextObject());
                    if(ctx->value->C2SCipher.size()>0){
                        auto front=ctx->value->C2SCipher.front();
                        ctx->value->C2SCipher.pop_front();
                        pxprpc_rtbridge_host::resolveTS(ret,(void *)front.second,front.first);
                        delete[] front.second;
                    }else{
                        char buf[1];
                        pxprpc_rtbridge_host::resolveTS(ret,buf,0);
                    }
                });
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.SslClientPushCipherRecv", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    auto ctx=static_cast<SslClient *>(para->nextObject());
                    auto buf=para->nextBytes();
                    auto copy=new uint8_t[std::get<0>(buf)];
                    memmove(copy,std::get<1>(buf),std::get<0>(buf));
                    ctx->value->S2CCipher.push_back({std::get<0>(buf),copy});
                    pxprpc_rtbridge_host::resolveTS(ret);
                });
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.SslClientWritePlain", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    auto ctx=static_cast<SslClient *>(para->nextObject());
                    auto buf=para->nextBytes();
                    auto writeCount=ctx->value->plainSend(std::get<1>(buf),std::get<0>(buf));
                    if(writeCount==MBEDTLS_ERR_SSL_WANT_WRITE || writeCount==MBEDTLS_ERR_SSL_WANT_READ){
                        pxprpc_rtbridge_host::resolveTS(ret,0);
                    }else if(writeCount>=0){
                        pxprpc_rtbridge_host::resolveTS(ret,writeCount);
                    }else{
                        pxprpc_rtbridge_host::rejectTS(ret,"mbedtls error code:"+std::to_string(writeCount));
                    }
                });
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.SslClientReadPlain", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()->void {
                    auto ctx=static_cast<SslClient *>(para->nextObject());
                    auto buf=new uint8_t[4096];
                    auto readCount=ctx->value->plainRecv(buf,4096);
                    if(readCount==MBEDTLS_ERR_SSL_WANT_READ || readCount==MBEDTLS_ERR_SSL_WANT_WRITE){
                        pxprpc_rtbridge_host::resolveTS(ret,buf,0);
                    }else if(readCount>=0){
                        pxprpc_rtbridge_host::resolveTS(ret,buf,readCount);
                    }else{
                        pxprpc_rtbridge_host::rejectTS(ret,"mbedtls error code:"+std::to_string(readCount));
                    }
                    delete[] buf;
                });
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.MbedtlsDigest", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()->void {
                    auto alg=para->nextString();
                    auto input=para->nextBytes();
                    if(alg=="SHA1"){
                        uint8_t buf[20];
                        int r=mbedtls_sha1(std::get<1>(input),std::get<0>(input),buf);
                        if(r==0){
                            pxprpc_rtbridge_host::resolveTS(ret,buf,20);
                        }else{
                            pxprpc_rtbridge_host::rejectTS(ret,"mbedtls error code:"+std::to_string(r));
                        }
                    }else if(alg=="SHA256"){
                        uint8_t buf[32];
                        int r=mbedtls_sha256(std::get<1>(input),std::get<0>(input),buf,0);
                        if(r==0){
                            pxprpc_rtbridge_host::resolveTS(ret,buf,32);
                        }else{
                            pxprpc_rtbridge_host::rejectTS(ret,"mbedtls error code:"+std::to_string(r));
                        }
                    }else if(alg=="SHA512"){
                        uint8_t buf[32];
                        int r=mbedtls_sha256(std::get<1>(input),std::get<0>(input),buf,0);
                        if(r==0){
                            pxprpc_rtbridge_host::resolveTS(ret,buf,48);
                        }else{
                            pxprpc_rtbridge_host::rejectTS(ret,"mbedtls error code:"+std::to_string(r));
                        }
                    }else{
                        pxprpc_rtbridge_host::rejectTS(ret,"Unsupported algorithm.");
                    }
                });
            })
        );
    }
}