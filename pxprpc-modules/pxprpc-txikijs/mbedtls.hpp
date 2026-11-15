#pragma once
//mbedtls.hpp
#include "quickjs.h"
#include <string>
extern "C" {
#include <mbedtls/ctr_drbg.h>
#include <mbedtls/entropy.h>
#include <mbedtls/ssl.h>
#include <pxprpc_pipe.h>
#include <mbedtls/pk.h>
}

#include <pxprpc_ext.hpp>
#include <pxprpc_pp.hpp>
#include <pxprpc_rtbridge_base/init.hpp>
#include <pxprpc_rtbridge_host.hpp>

#include <pxprpc-txikijs/init.hpp>

namespace pxprpc_txikijs {

namespace __embedtlsBinding {
using string = std::string;
using AsyncReturn = pxprpc::NamedFunctionPPImpl1::AsyncReturn;
using Parameter = pxprpc::NamedFunctionPPImpl1::Parameter;

int __mbedtls_ssl_send_client(void *ctx, const unsigned char *buf, size_t len);
int __mbedtls_ssl_recv_client(void *ctx, unsigned char *buf, size_t len);

// By deepseek
class FIFOBuffer {
private:
  std::deque<uint8_t> buffer;

public:
  void write(const uint8_t *data, size_t len) {
    buffer.insert(buffer.end(), data, data + len);
  }

  size_t read(uint8_t *output, size_t max_len) {
    size_t len = std::min(max_len, buffer.size());
    for (size_t i = 0; i < len; ++i) {
      output[i] = buffer[i];
    }
    buffer.erase(buffer.begin(), buffer.begin() + len);
    return len;
  }

  size_t peek(uint8_t *output, size_t max_len) const {
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

std::vector<std::function<JSValue(TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                  int argc, JSValue *argv)>>
    embedtlsSslFunc2026List;

static JSValue embedtlsSslFunc2026(JSContext *ctx, JSValue this_val, int argc,
                                   JSValue *argv) {
  int32_t index=0;
  int ok = JS_ToInt32(ctx, &index, argv[0]);
  auto classId=JS_GetClassID(this_val);
            if(classId==JS_INVALID_CLASS_ID){
                return JS_EXCEPTION;
            }
  if (index < embedtlsSslFunc2026List.size()) {
    auto thisWrap = static_cast<TjsRuntimeWrap *>(
        JS_GetOpaque(this_val, classId));
    return embedtlsSslFunc2026List[index](thisWrap, ctx, argc, argv);
  } else {
    return JS_UNDEFINED;
  }
}

class SslClientAsync {
  mbedtls_ssl_context ssl;
  mbedtls_ssl_config conf;
  mbedtls_entropy_context entropy;
  mbedtls_ctr_drbg_context ctr_drbg;

public:
  string hostname;
  FIFOBuffer C2SCipher;
  FIFOBuffer S2CCipher;
  SslClientAsync() {
    mbedtls_ssl_init(&ssl);
    mbedtls_ssl_config_init(&conf);
    mbedtls_entropy_init(&entropy);
    mbedtls_ctr_drbg_init(&ctr_drbg);
  }
  const char *configure() {
    int ret;

    if ((ret = mbedtls_ctr_drbg_seed(&ctr_drbg, mbedtls_entropy_func, &entropy,
                                     nullptr, 0)) != 0) {
      return "Failed in mbedtls_ctr_drbg_seed";
    }

    if ((ret = mbedtls_ssl_config_defaults(&conf, MBEDTLS_SSL_IS_CLIENT,
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

    mbedtls_ssl_set_bio(&ssl, this, __mbedtls_ssl_send_client,
                        __mbedtls_ssl_recv_client, NULL);
    return nullptr;
  }
  virtual int tlsWriteCipherSendBuffer(const unsigned char *buf, size_t len) {
    this->C2SCipher.write(buf, len);
    return len;
  }
  virtual int tlsReadCipherRecvBuffer(unsigned char *buf, size_t len) {
    if (this->S2CCipher.empty()) {
      return MBEDTLS_ERR_SSL_WANT_READ;
    }
    return this->S2CCipher.read(buf, len);
  }
  virtual int readCipherSendBuffer(unsigned char *buf, size_t len) {
    if (this->C2SCipher.empty()) {
      return 0;
    }
    return this->C2SCipher.read(buf, len);
  }
  virtual int writeCipherRecvBuffer(unsigned char *buf, size_t len) {
    this->S2CCipher.write(buf, len);
    return len;
  }
  virtual int writePlain(const unsigned char *buf, int len) {
    return mbedtls_ssl_write(&ssl, buf, len);
  }
  virtual int readPlain(unsigned char *buf, int len) {
    return mbedtls_ssl_read(&ssl, buf, len);
  }
  virtual void deinitAndDelete() {
    // mbedtls_ssl_close_notify(&ssl);
    mbedtls_ssl_free(&ssl);
    mbedtls_ssl_config_free(&conf);
    mbedtls_ctr_drbg_free(&ctr_drbg);
    mbedtls_entropy_free(&entropy);
    delete this;
  }
};

class SslServerAsync {
  mbedtls_ssl_context ssl;
  mbedtls_ssl_config conf;
  mbedtls_entropy_context entropy;
  mbedtls_ctr_drbg_context ctr_drbg;
  mbedtls_x509_crt srvcert;
  mbedtls_pk_context pkey;

public:
  FIFOBuffer C2SCipher;
  FIFOBuffer S2CCipher;

  SslServerAsync() {
    mbedtls_ssl_init(&ssl);
    mbedtls_ssl_config_init(&conf);
    mbedtls_entropy_init(&entropy);
    mbedtls_ctr_drbg_init(&ctr_drbg);
    mbedtls_x509_crt_init(&srvcert);
    mbedtls_pk_init(&pkey);
  }

  const char *configure(const char *crtPem, const char *keyPem) {
    int ret;
    if ((ret = mbedtls_ctr_drbg_seed(&ctr_drbg, mbedtls_entropy_func, &entropy,
                                     nullptr, 0)) != 0) {
      return "Failed in mbedtls_ctr_drbg_seed";
    }

    if ((ret = mbedtls_x509_crt_parse(&srvcert, (const unsigned char *)crtPem,
                                      strlen(crtPem) + 1)) != 0) {
      return "Failed to parse certificate";
    }
    if ((ret = mbedtls_pk_parse_key(&pkey, (const unsigned char *)keyPem,
                     strlen(keyPem) + 1, nullptr, 0,
                     mbedtls_ctr_drbg_random, &ctr_drbg)) != 0) {
      return "Failed to parse private key";
    }

    if ((ret = mbedtls_ssl_config_defaults(&conf, MBEDTLS_SSL_IS_SERVER,
                                           MBEDTLS_SSL_TRANSPORT_STREAM,
                                           MBEDTLS_SSL_PRESET_DEFAULT)) != 0) {
      return "Failed in mbedtls_ssl_config_defaults";
    }

    mbedtls_ssl_conf_authmode(&conf, MBEDTLS_SSL_VERIFY_NONE);
    mbedtls_ssl_conf_rng(&conf, mbedtls_ctr_drbg_random, &ctr_drbg);

    if ((ret = mbedtls_ssl_conf_own_cert(&conf, &srvcert, &pkey)) != 0) {
      return "Failed to set own certificate";
    }

    if ((ret = mbedtls_ssl_setup(&ssl, &conf)) != 0) {
      return "Failed in mbedtls_ssl_setup";
    }

    mbedtls_ssl_set_bio(
        &ssl, this,
        [](void *ctx, const unsigned char *buf, size_t len) -> int {
          auto s = static_cast<SslServerAsync *>(ctx);
          s->S2CCipher.write(buf, len);
          return len;
        },
        [](void *ctx, unsigned char *buf, size_t len) -> int {
          auto s = static_cast<SslServerAsync *>(ctx);
          if (s->C2SCipher.empty())
            return MBEDTLS_ERR_SSL_WANT_READ;
          return s->C2SCipher.read(buf, len);
        },
        NULL);

    return nullptr;
  }

  int readCipherFromClient(unsigned char *buf, size_t len) {
    if (C2SCipher.empty())
      return 0;
    return C2SCipher.read(buf, len);
  }

  int writeCipherToClient(const unsigned char *buf, size_t len) {
    S2CCipher.write(buf, len);
    return len;
  }

  int writePlain(const unsigned char *buf, int len) {
    return mbedtls_ssl_write(&ssl, buf, len);
  }

  int readPlain(unsigned char *buf, int len) {
    return mbedtls_ssl_read(&ssl, buf, len);
  }

  int handshake() { return mbedtls_ssl_handshake(&ssl); }

  int readCipherToClient(unsigned char *buf, size_t len) {
    if (S2CCipher.empty())
      return 0;
    return S2CCipher.read(buf, len);
  }

  int writeCipherFromClient(const unsigned char *buf, size_t len) {
    C2SCipher.write(buf, len);
    return len;
  }

  void deinitAndDelete() {
    mbedtls_ssl_free(&ssl);
    mbedtls_ssl_config_free(&conf);
    mbedtls_ctr_drbg_free(&ctr_drbg);
    mbedtls_entropy_free(&entropy);
    mbedtls_x509_crt_free(&srvcert);
    mbedtls_pk_free(&pkey);
    delete this;
  }
};

class SslServer : public pxprpc::PxpObject {
public:
  SslServerAsync *value = nullptr;
  SslServer() { value = new SslServerAsync(); }
  virtual ~SslServer() {
    if (value)
      value->deinitAndDelete();
  }
};

int __mbedtls_ssl_send_client(void *ctx, const unsigned char *buf, size_t len) {
  auto cppctx = static_cast<SslClientAsync *>(ctx);
  return cppctx->tlsWriteCipherSendBuffer(buf, len);
}

int __mbedtls_ssl_recv_client(void *ctx, unsigned char *buf, size_t len) {
  auto cppctx = static_cast<SslClientAsync *>(ctx);
  return cppctx->tlsReadCipherRecvBuffer(buf, len);
}

class SslClient : public pxprpc::PxpObject {
public:
  SslClientAsync *value = nullptr;
  SslClient() { value = new SslClientAsync(); }
  virtual ~SslClient() {
    if (value != nullptr) {
      value->deinitAndDelete();
    }
  }
};

void init() {
  embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                       int argc, JSValue *argv) -> JSValue {
    // 0 getSizeOfFuncList
    return JS_NewInt32(ctx, embedtlsSslFunc2026List.size());
  });
  embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                       int argc, JSValue *argv) -> JSValue {
    // 1 mbedtls.newSslClient
    auto r = new SslClient();
    auto hostnameC = JS_ToCString(ctx, argv[1]);
    r->value->hostname = string(hostnameC);
    JS_FreeCString(ctx, hostnameC);
    auto err = r->value->configure();
    if (err != nullptr) {
      delete r;
      return JS_NewPlainError(ctx, "%s", err);
    }
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  });
  embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                       int argc, JSValue *argv) -> JSValue {
    // 2 mbedtls.readCipherSendBuffer
    int32_t i1;
    JS_ToInt32(ctx, &i1, argv[1]);
    auto ssl = static_cast<SslClient *>(thisWrap->objStore[i1]);
    if(ssl==nullptr)return JS_EXCEPTION;
    JSValue ret = JS_UNDEFINED;
    size_t maxSize = 0;
    auto buf = JS_GetUint8Array(ctx, &maxSize, argv[2]);
    ret = JS_NewInt32(ctx, ssl->value->readCipherSendBuffer(buf, maxSize));
    return ret;
  });
  embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                       int argc, JSValue *argv) -> JSValue {
    // 3 mbedtls.writeCipherRecvBuffer
    int32_t i1;
    JS_ToInt32(ctx, &i1, argv[1]);
    auto ssl = static_cast<SslClient *>(thisWrap->objStore[i1]);
    if(ssl==nullptr)return JS_EXCEPTION;
    size_t size;
    auto buf = JS_GetUint8Array(ctx, &size, argv[2]);
    int32_t ret = ssl->value->writeCipherRecvBuffer(buf, size);
    return JS_NewInt32(ctx, ret);
  });
  embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                       int argc, JSValue *argv) -> JSValue {
    // 4 mbedtls.ssl.writePlain
    int32_t i1;
    JS_ToInt32(ctx, &i1, argv[1]);
    auto ssl = static_cast<SslClient *>(thisWrap->objStore[i1]);
    if(ssl==nullptr)return JS_EXCEPTION;
    size_t size;
    auto buf = JS_GetUint8Array(ctx, &size, argv[2]);
    auto writeCount = ssl->value->writePlain(buf, size);
    if (writeCount == MBEDTLS_ERR_SSL_WANT_WRITE ||
        writeCount == MBEDTLS_ERR_SSL_WANT_READ) {
      return JS_NewInt32(ctx, 0);
    } else if (writeCount >= 0) {
      return JS_NewInt32(ctx, writeCount);
    } else {
      return JS_NewPlainError(ctx, "mbedtls error code: %d", writeCount);
    }
  });
  embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                       int argc, JSValue *argv) -> JSValue {
    // 5 mbedtls.ssl.readPlain
    int32_t i1;
    JS_ToInt32(ctx, &i1, argv[1]);
    auto ssl = static_cast<SslClient *>(thisWrap->objStore[i1]);
    if(ssl==nullptr)return JS_EXCEPTION;
    size_t size;
    auto buf = JS_GetUint8Array(ctx, &size, argv[2]);
    auto readCount = ssl->value->readPlain(buf, size);
    if (readCount == MBEDTLS_ERR_SSL_WANT_WRITE ||
        readCount == MBEDTLS_ERR_SSL_WANT_READ) {
      return JS_NewInt32(ctx, 0);
    } else {
      return JS_NewInt32(ctx, readCount);
    }
  });
  // 6 mbedtls.newSslServer(crtPem, keyPem) -> objId
  embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                       int argc, JSValue *argv) -> JSValue {
    auto r = new SslServer();
    auto crt = JS_ToCString(ctx, argv[1]);
    auto key = JS_ToCString(ctx, argv[2]);
    if (crt == nullptr || key == nullptr) {
      JS_FreeCString(ctx, crt);
      JS_FreeCString(ctx, key);
      delete r;
      return JS_NewPlainError(ctx, "Invalid certificate or key string");
    }
    auto err = r->value->configure(crt, key);
    JS_FreeCString(ctx, crt);
    JS_FreeCString(ctx, key);
    if (err != nullptr) {
      delete r;
      return JS_NewPlainError(ctx, "%s", err);
    }
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  });

  // 7 mbedtls.server.readCipherFromClient(objId, buf) -> int
  embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                       int argc, JSValue *argv) -> JSValue {
    int32_t i1;
    JS_ToInt32(ctx, &i1, argv[1]);
    auto ssl = static_cast<SslServer *>(thisWrap->objStore[i1]);
    if(ssl==nullptr)return JS_EXCEPTION;
    size_t maxSize = 0;
    auto buf = JS_GetUint8Array(ctx, &maxSize, argv[2]);
    int ret = ssl->value->readCipherToClient(buf, maxSize);
    return JS_NewInt32(ctx, ret);
  });

  // 8 mbedtls.server.writeCipherToClient(objId, buf) -> int
  embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                       int argc, JSValue *argv) -> JSValue {
    int32_t i1;
    JS_ToInt32(ctx, &i1, argv[1]);
    auto ssl = static_cast<SslServer *>(thisWrap->objStore[i1]);
    if(ssl==nullptr)return JS_EXCEPTION;
    size_t size = 0;
    auto buf = JS_GetUint8Array(ctx, &size, argv[2]);
    int ret = ssl->value->writeCipherFromClient(buf, size);
    return JS_NewInt32(ctx, ret);
  });

  // 9 mbedtls.server.writePlain(objId, buf) -> int
  embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                       int argc, JSValue *argv) -> JSValue {
    int32_t i1;
    JS_ToInt32(ctx, &i1, argv[1]);
    auto ssl = static_cast<SslServer *>(thisWrap->objStore[i1]);
    if(ssl==nullptr)return JS_EXCEPTION;
    size_t size = 0;
    auto buf = JS_GetUint8Array(ctx, &size, argv[2]);
    auto writeCount = ssl->value->writePlain(buf, size);
    if (writeCount == MBEDTLS_ERR_SSL_WANT_WRITE ||
        writeCount == MBEDTLS_ERR_SSL_WANT_READ) {
      return JS_NewInt32(ctx, 0);
    } else if (writeCount >= 0) {
      return JS_NewInt32(ctx, writeCount);
    } else {
      return JS_NewPlainError(ctx, "mbedtls error code: %d", writeCount);
    }
  });

  // 10 mbedtls.server.readPlain(objId, buf) -> int
  embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                       int argc, JSValue *argv) -> JSValue {
    int32_t i1;
    JS_ToInt32(ctx, &i1, argv[1]);
    auto ssl = static_cast<SslServer *>(thisWrap->objStore[i1]);
    if(ssl==nullptr)return JS_EXCEPTION;
    size_t size = 0;
    auto buf = JS_GetUint8Array(ctx, &size, argv[2]);
    auto readCount = ssl->value->readPlain(buf, size);
    if (readCount == MBEDTLS_ERR_SSL_WANT_WRITE ||
        readCount == MBEDTLS_ERR_SSL_WANT_READ) {
      return JS_NewInt32(ctx, 0);
    } else {
      return JS_NewInt32(ctx, readCount);
    }
  });

  // 11 mbedtls.server.handshake(objId) -> int
  embedtlsSslFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                       int argc, JSValue *argv) -> JSValue {
    int32_t i1;
    JS_ToInt32(ctx, &i1, argv[1]);
    auto ssl = static_cast<SslServer *>(thisWrap->objStore[i1]);
    if(ssl==nullptr)return JS_EXCEPTION;
    int ret = ssl->value->handshake();
    if (ret == MBEDTLS_ERR_SSL_WANT_READ || ret == MBEDTLS_ERR_SSL_WANT_WRITE) {
      return JS_NewInt32(ctx, 0);
    }
    return JS_NewInt32(ctx, ret);
  });
}
} // namespace __embedtlsBinding
} // namespace pxprpc_txikijs