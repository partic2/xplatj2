#pragma once

// WIP For multi-thread issue

#include "mbedtls/md5.h"


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

#include <pxprpc-txikijs/init.hpp>
#include <pxprpc-txikijs/mbedtls.hpp>

namespace pxprpc_txikijs{

    namespace __embedtlsRpc{
    

    void init(){
        pxprpc::defaultFuncMap.add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.mbedtls.ssl.newpxprpc_txikijs::__embedtlsBinding::SslClient", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    auto hostname=para->nextString();
                    auto ctx=new pxprpc_txikijs::__embedtlsBinding::SslClient();
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
                ->init("pxprpc_txikijs.mbedtls.ssl.readCipherSendBuffer", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    auto ctx=static_cast<pxprpc_txikijs::__embedtlsBinding::SslClient *>(para->nextObject());
                    struct pxprpc_buffer_part buf;
                    buf.next_part=nullptr;
                    buf.bytes.length=ctx->value->C2SCipher.available();
                    buf.bytes.base=new uint8_t[buf.bytes.length];
                    ctx->value->readCipherSendBuffer(static_cast<uint8_t*>(buf.bytes.base),buf.bytes.length);
                    pxprpc_rtbridge_host::resolveTS(ret,buf,[buf]()-> void {
                        delete[] static_cast<uint8_t *>(buf.bytes.base);
                    });
                });
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.mbedtls.ssl.writeCipherRecvBuffer", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    auto ctx=static_cast<pxprpc_txikijs::__embedtlsBinding::SslClient *>(para->nextObject());
                    auto buf=para->nextBytes();
                    ctx->value->writeCipherRecvBuffer(std::get<1>(buf),std::get<0>(buf));
                    pxprpc_rtbridge_host::resolveTS(ret);
                });
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.mbedtls.ssl.writePlain", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    auto ctx=static_cast<pxprpc_txikijs::__embedtlsBinding::SslClient *>(para->nextObject());
                    auto buf=para->nextBytes();
                    auto writeCount=ctx->value->writePlain(std::get<1>(buf),std::get<0>(buf));
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
                ->init("pxprpc_txikijs.mbedtls.ssl.readPlain", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()->void {
                    auto ctx=static_cast<pxprpc_txikijs::__embedtlsBinding::SslClient *>(para->nextObject());
                    auto buf=new uint8_t[4096];
                    auto readCount=ctx->value->readPlain(buf,4096);
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
                ->init("pxprpc_txikijs.mbedtls.digest", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
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
                        int r=mbedtls_sha512(std::get<1>(input),std::get<0>(input),buf,0);
                        if(r==0){
                            pxprpc_rtbridge_host::resolveTS(ret,buf,48);
                        }else{
                            pxprpc_rtbridge_host::rejectTS(ret,"mbedtls error code:"+std::to_string(r));
                        }
                    }else if(alg=="MD5"){
                        uint8_t buf[16];
                        int r=mbedtls_md5(std::get<1>(input),std::get<0>(input),buf);
                        if(r==0){
                            pxprpc_rtbridge_host::resolveTS(ret,buf,16);
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
}