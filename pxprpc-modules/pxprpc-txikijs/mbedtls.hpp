
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

    class SslClient{
        public:
        mbedtls_ssl_context ssl;
        mbedtls_ssl_config conf;
        mbedtls_entropy_context entropy;
        mbedtls_ctr_drbg_context ctr_drbg;
        string hostname;
        std::list<std::pair<int,unsigned char *>> C2SCipher;
        std::list<std::pair<int,unsigned char *>> S2CCipher;
        std::list<std::pair<int,unsigned char *>> C2SPlain;
        std::list<std::pair<int,unsigned char *>> S2CPlain;
        SslClient(){
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
        virtual int mbedtlsCipherSend(const unsigned char *buf,size_t len){
            std::pair<int,unsigned char *> pack;
            pack.second=new unsigned char[len];
            pack.first=len;
            memcpy(pack.second,buf,len);
            this->C2SCipher.push_back(pack);
            return len;
        }
        virtual int mbedtlsCipherRecv(unsigned char *buf,size_t len){
            if(this->S2CCipher.size()==0){
                return MBEDTLS_ERR_SSL_WANT_READ;
            }
            auto pack=this->S2CCipher.front();
            if(len<pack.first){
                memcpy(buf,pack.second,len);
                auto newbuf=new unsigned char[pack.first-len];
                memcpy(newbuf,pack.second+len,pack.first-len);
                pack.first=pack.first-len;
                delete[] pack.second;
                pack.second=newbuf;
                return len;
            }else{
                memcpy(buf,pack.second,pack.first);
                len=pack.first;
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
        ~SslClient(){
            //mbedtls_ssl_close_notify(&ssl);
            mbedtls_ssl_free(&ssl);
            mbedtls_ssl_config_free(&conf);
            mbedtls_ctr_drbg_free(&ctr_drbg);
            mbedtls_entropy_free(&entropy);
        }
    };

    int __mbedtls_ssl_send_client(void *ctx,const unsigned char *buf,size_t len){
        auto cppctx=static_cast<SslClient *>(ctx);
        return cppctx->mbedtlsCipherSend(buf,len);
    }

    int __mbedtls_ssl_recv_client(void *ctx,unsigned char *buf,size_t len){
        auto cppctx=static_cast<SslClient *>(ctx);
        return cppctx->mbedtlsCipherRecv(buf,len);
    }


}