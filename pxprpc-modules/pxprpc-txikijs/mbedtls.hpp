#pragma once

// WIP For multi-thread issue

#include "mbedtls/md5.h"

#include "quickjs.h"
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

#include <pxprpc-txikijs/init.hpp>

namespace pxprpc_txikijs{

    namespace __embedtlsBinding{
    using string=std::string;
    using AsyncReturn=pxprpc::NamedFunctionPPImpl1::AsyncReturn;
    using Parameter=pxprpc::NamedFunctionPPImpl1::Parameter;

    int __mbedtls_ssl_send_client(void *ctx,const unsigned char *buf,size_t len);
    int __mbedtls_ssl_recv_client(void *ctx,unsigned char *buf,size_t len);

    
    //By deepseek
    class FIFOBuffer {
        private:
            std::deque<uint8_t> buffer;
            
        public:
            void write(const uint8_t* data, size_t len) {
                buffer.insert(buffer.end(), data, data + len);
            }
            
            size_t read(uint8_t* output, size_t max_len) {
                size_t len = std::min(max_len, buffer.size());
                for (size_t i = 0; i < len; ++i) {
                    output[i] = buffer[i];
                }
                buffer.erase(buffer.begin(), buffer.begin() + len);
                return len;
            }
            
            size_t peek(uint8_t* output, size_t max_len) const {
                size_t len = std::min(max_len, buffer.size());
                for (size_t i = 0; i < len; ++i) {
                    output[i] = buffer[i];
                }
                return len;
            }
            
            size_t available() const { return buffer.size(); }
            bool empty() const { return buffer.empty(); }
            void clear() { buffer.clear(); }
    };

    std::vector<std::function<JSValue(TjsRuntimeWrap *thisWrap,JSContext *ctx,int argc,JSValue *argv)>> embedtlsSslFunc2026List;
        
    static JSValue embedtlsSslFunc2026(JSContext *ctx,JSValue this_val, int argc, JSValue *argv){
        int32_t i32;
        int index=JS_ToInt32(ctx,&i32,argv[0]);
        if(index<embedtlsSslFunc2026List.size()){
            auto thisWrap=static_cast<TjsRuntimeWrap *>(JS_GetOpaque(this_val,__pxprpc4tjs::classId));
            return embedtlsSslFunc2026List[i32](thisWrap,ctx,argc,argv);
        }else{
            return JS_UNDEFINED;
        }
    }

    class SslClientAsync{
        mbedtls_ssl_context ssl;
        mbedtls_ssl_config conf;
        mbedtls_entropy_context entropy;
        mbedtls_ctr_drbg_context ctr_drbg;
        public:
        string hostname;
        FIFOBuffer C2SCipher;
        FIFOBuffer S2CCipher;
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
        virtual int tlsWriteCipherSendBuffer(const unsigned char *buf,size_t len){
            this->C2SCipher.write(buf,len);
            return len;
        }
        virtual int tlsReadCipherRecvBuffer(unsigned char *buf,size_t len){
            if(this->S2CCipher.empty()){
                return MBEDTLS_ERR_SSL_WANT_READ;
            }
            return this->S2CCipher.read(buf,len);
        }
        virtual int readCipherSendBuffer(unsigned char *buf,size_t len){
            if(this->C2SCipher.empty()){
                return 0;
            }
            return this->C2SCipher.read(buf,len);
        }
        virtual int writeCipherRecvBuffer(unsigned char *buf,size_t len){
            this->S2CCipher.write(buf,len);
            return len;
        }
        virtual int writePlain(const unsigned char *buf,int len){
            return mbedtls_ssl_write(&ssl,buf,len);
        }
        virtual int readPlain(unsigned char *buf,int len){
            return mbedtls_ssl_read(&ssl,buf,len);
        }
        virtual void deinitAndDelete(){
            //mbedtls_ssl_close_notify(&ssl);
            mbedtls_ssl_free(&ssl);
            mbedtls_ssl_config_free(&conf);
            mbedtls_ctr_drbg_free(&ctr_drbg);
            mbedtls_entropy_free(&entropy);
            delete this;
        }
    };

    int __mbedtls_ssl_send_client(void *ctx,const unsigned char *buf,size_t len){
        auto cppctx=static_cast<SslClientAsync *>(ctx);
        return cppctx->tlsWriteCipherSendBuffer(buf,len);
    }

    int __mbedtls_ssl_recv_client(void *ctx,unsigned char *buf,size_t len){
        auto cppctx=static_cast<SslClientAsync *>(ctx);
        return cppctx->tlsReadCipherRecvBuffer(buf,len);
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

    void init(){
        pxprpc::defaultFuncMap.add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.mbedtls.ssl.newSslClient", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
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
                ->init("pxprpc_txikijs.mbedtls.ssl.readCipherSendBuffer", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    auto ctx=static_cast<SslClient *>(para->nextObject());
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
                    auto ctx=static_cast<SslClient *>(para->nextObject());
                    auto buf=para->nextBytes();
                    ctx->value->writeCipherRecvBuffer(std::get<1>(buf),std::get<0>(buf));
                    pxprpc_rtbridge_host::resolveTS(ret);
                });
            })
        ).add(
            (new pxprpc::NamedFunctionPPImpl1())
                ->init("pxprpc_txikijs.mbedtls.ssl.writePlain", [](pxprpc::NamedFunctionPPImpl1::Parameter *para, AsyncReturn *ret) -> void {
                pxprpc_rtbridge_host::threadPoolRun([para,ret]()-> void {
                    auto ctx=static_cast<SslClient *>(para->nextObject());
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
                    auto ctx=static_cast<SslClient *>(para->nextObject());
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
        embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap,JSContext *ctx,int argc,JSValue *argv) -> JSValue{
            //0 getSizeOfFuncList
            return JS_NewInt32(ctx,embedtlsSslFunc2026List.size());
        });
        embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap,JSContext *ctx,int argc,JSValue *argv) -> JSValue{
            //1 mbedtls.newSslClient
            auto r=new SslClient();
            auto hostnameC=JS_ToCString(ctx,argv[1]);
            r->value->hostname=string(hostnameC);
            JS_FreeCString(ctx,hostnameC);
            auto err=r->value->configure();
            if(err!=nullptr){
                return JS_NewPlainError(ctx, "%s",err);
            }
            return JS_NewInt32(ctx,thisWrap->saveObject(r));
        });
        embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap,JSContext *ctx,int argc,JSValue *argv) -> JSValue{
            //2 mbedtls.readCipherSendBuffer
            int32_t i1;
            JS_ToInt32(ctx, &i1, argv[1]);
            auto ssl=static_cast<SslClient *>(thisWrap->objStore[i1]);
            JSValue ret=JS_UNDEFINED;
            size_t maxSize=0;
            auto buf=JS_GetUint8Array(ctx, &maxSize, argv[2]);
            ret=JS_NewInt32(ctx,ssl->value->readCipherSendBuffer(buf,maxSize));
            return ret;
        });
        embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap,JSContext *ctx,int argc,JSValue *argv) -> JSValue{
            //3 mbedtls.writeCipherRecvBuffer
            int32_t i1;
            JS_ToInt32(ctx, &i1, argv[1]);
            auto ssl=static_cast<SslClient *>(thisWrap->objStore[i1]);
            size_t size;
            auto buf=JS_GetUint8Array(ctx,&size,argv[2]);
            int32_t ret=ssl->value->writeCipherRecvBuffer(buf,size);
            return JS_NewInt32(ctx, ret);
        });
        embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap,JSContext *ctx,int argc,JSValue *argv) -> JSValue{
            //4 mbedtls.ssl.writePlain
            int32_t i1;
            JS_ToInt32(ctx, &i1, argv[1]);
            auto ssl=static_cast<SslClient *>(thisWrap->objStore[i1]);
            size_t size;
            auto buf=JS_GetUint8Array(ctx,&size,argv[2]);
            auto writeCount=ssl->value->writePlain(buf,size);
            if(writeCount==MBEDTLS_ERR_SSL_WANT_WRITE || writeCount==MBEDTLS_ERR_SSL_WANT_READ){
                return JS_NewInt32(ctx,0);
            }else if(writeCount>=0){
                return JS_NewInt32(ctx,writeCount);
            }else{
                return JS_NewPlainError(ctx, "mbedtls error code: %d",writeCount);
            }
        });
        embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap,JSContext *ctx,int argc,JSValue *argv) -> JSValue{
            //5 mbedtls.ssl.readPlain
            int32_t i1;
            JS_ToInt32(ctx, &i1, argv[1]);
            auto ssl=static_cast<SslClient *>(thisWrap->objStore[i1]);
            size_t size;
            auto buf=JS_GetUint8Array(ctx, &size, argv[2]);
            auto readCount=ssl->value->readPlain(buf,size);
            if(readCount==MBEDTLS_ERR_SSL_WANT_WRITE || readCount==MBEDTLS_ERR_SSL_WANT_READ){
                return JS_NewInt32(ctx,0);
            }else{
                return JS_NewInt32(ctx, readCount);
            }
        });
    }
}
}