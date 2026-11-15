#pragma once

#include "quickjs.h"
#include <string>
#include <vector>
#include <functional>
#include <cstring>
#include <cstdlib>
#include <cstdint>
extern "C" {
#include <pwart.h>
}

#include <pxprpc-txikijs/init.hpp>

namespace pxprpc_txikijs {

namespace __pwartBinding {
using string = std::string;
using AsyncReturn = pxprpc::NamedFunctionPPImpl1::AsyncReturn;
using Parameter = pxprpc::NamedFunctionPPImpl1::Parameter;


std::vector<std::function<JSValue(TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                  int argc, JSValue *argv)>>
    pwartFunc2026List;

static JSValue pwartFunc2026(JSContext *ctx, JSValue this_val, int argc,
                             JSValue *argv) {
  int32_t i32;
  JS_ToInt32(ctx, &i32, argv[0]);
  if (i32 >= 0 && i32 < (int)pwartFunc2026List.size()) {
    auto classId=JS_GetClassID(this_val);
    if(classId==JS_INVALID_CLASS_ID){
        return JS_EXCEPTION;
    }
    auto thisWrap = static_cast<TjsRuntimeWrap *>(
        JS_GetOpaque(this_val, classId));
    return pwartFunc2026List[i32](thisWrap, ctx, argc, argv);
  } else {
    return JS_UNDEFINED;
  }
}



class WrtNamespace : public pxprpc::PxpObject {
public:
  pwart_namespace ns;
  WrtNamespace() : ns(pwart_namespace_new()) {}
  virtual ~WrtNamespace() {
    if (ns) { pwart_namespace_delete(ns); ns = nullptr; }
  }
};


class WrtModuleState : public pxprpc::PxpObject {
public:
  pwart_module_state ms;
  WrtModuleState(pwart_module_state m) : ms(m) {}
};

class WrtFunction : public pxprpc::PxpObject {
public:
  pwart_wasm_function fn;
  WrtFunction(pwart_wasm_function f) : fn(f) {}
};

class WrtMemory : public pxprpc::PxpObject {
public:
  pwart_wasm_memory *mem;
  WrtMemory(pwart_wasm_memory *m) : mem(m) {}
};

class WrtTable : public pxprpc::PxpObject {
public:
  pwart_wasm_table *tb;
  WrtTable(pwart_wasm_table *t) : tb(t) {}
};

static inline uint64_t pwartMemBytes(struct pwart_wasm_memory *m) {
  return (uint64_t)m->pages * 65536ULL;
}


static JSValue pwartWrapSymbolResult(TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                     uint32_t kind, void *result) {
  if (!result) return JS_NULL;
  switch (kind) {
  case PWART_KIND_FUNCTION: {
    auto r = new WrtFunction((pwart_wasm_function)result);
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  }
  case PWART_KIND_MEMORY: {
    auto r = new WrtMemory((struct pwart_wasm_memory *)result);
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  }
  case PWART_KIND_TABLE: {
    auto r = new WrtTable((struct pwart_wasm_table *)result);
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  }
  case PWART_KIND_GLOBAL:
    return JS_NewBigInt64(ctx, (int64_t)(intptr_t)result);
  default:
    return JS_NULL;
  }
}


static JSValue pwartResolveSymbolImpl(TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                      pwart_namespace ns,
                                      const char *moduleName,
                                      const char *fieldName, uint32_t kind,
                                      const char *importerName,
                                      const char **errOut) {
  if (errOut) *errOut = nullptr;
  if (!ns) {
    if (errOut) *errOut = "invalid namespace";
    return JS_NULL;
  }
  struct pwart_symbol_resolver *resolver = pwart_namespace_resolver(ns);
  if (!resolver || !resolver->resolve) {
    if (errOut) *errOut = "namespace has no resolver";
    return JS_NULL;
  }
  struct pwart_symbol_resolve_request req;
  memset(&req, 0, sizeof(req));
  req.import_module = moduleName;
  req.import_field = fieldName;
  req.importer_name = importerName ? importerName : "";
  req.kind = kind;
  req.result = nullptr;
  resolver->resolve(resolver, &req);
  return pwartWrapSymbolResult(thisWrap, ctx, kind, req.result);
}

void init() {
  // 0: pwart.getSizeOfFuncList()
  pwartFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                 int argc, JSValue *argv) -> JSValue {
    return JS_NewInt32(ctx, (int32_t)pwartFunc2026List.size());
  });

  // 1: pwart.newNamespace() -> objId
  pwartFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                 int argc, JSValue *argv) -> JSValue {
    auto r = new WrtNamespace();
    if (!r->ns) {
      delete r;
      return JS_NewPlainError(ctx, "pwart_namespace_new failed");
    }
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  });

  // 2: pwart.namespaceDelete(nsId) -> void
  pwartFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                 int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_UNDEFINED;
    auto ns = static_cast<WrtNamespace *>(thisWrap->objStore[id]);
    if (ns && ns->ns) {
      pwart_namespace_delete(ns->ns);
      ns->ns = nullptr;
    }
    return JS_UNDEFINED;
  });

  // 3: pwart.namespaceDefineWasmModule(nsId, name, wasmBytes) -> moduleStateId
  pwartFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                 int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NULL;
    auto ns = static_cast<WrtNamespace *>(thisWrap->objStore[id]);
    if (!ns || !ns->ns) return JS_NewPlainError(ctx, "invalid namespace");
    const char *name = JS_ToCString(ctx, argv[2]);
    if (!name) return JS_NewPlainError(ctx, "invalid name");
    size_t len = 0;
    uint8_t *bytes = JS_GetUint8Array(ctx, &len, argv[3]);
    if (!bytes) {
      JS_FreeCString(ctx, name);
      return JS_NewPlainError(ctx, "wasmBytes must be a Uint8Array");
    }
    char *errMsg = nullptr;
    pwart_module_state *msPtr = pwart_namespace_define_wasm_module(
        ns->ns, name, (const char *)bytes, (int)len, &errMsg);
    JS_FreeCString(ctx, name);
    if (!msPtr || !*msPtr) {
      JSValue e = JS_NewPlainError(ctx, "%s",
                                   errMsg ? errMsg : "unknown error");
      if (errMsg) free(errMsg);
      return e;
    }
    auto r = new WrtModuleState(*msPtr);
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  });

  // 4: pwart.namespaceRemoveModule(nsId, name) -> bool
  pwartFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                 int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NewBool(ctx, 0);
    auto ns = static_cast<WrtNamespace *>(thisWrap->objStore[id]);
    if (!ns || !ns->ns) return JS_NewBool(ctx, 0);
    const char *name = JS_ToCString(ctx, argv[2]);
    if (!name) return JS_NewBool(ctx, 0);
    char *errMsg = pwart_namespace_remove_module(ns->ns, name);
    JS_FreeCString(ctx, name);
    bool ok = errMsg == nullptr;
    if (errMsg) free(errMsg);
    return JS_NewBool(ctx, ok);
  });

  // 5: pwart.getExportFunction(moduleStateId, name) -> functionId | null
  pwartFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                 int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NULL;
    auto ms = static_cast<WrtModuleState *>(thisWrap->objStore[id]);
    if (!ms || !ms->ms) return JS_NULL;
    const char *name = JS_ToCString(ctx, argv[2]);
    if (!name) return JS_NULL;
    pwart_wasm_function fn = pwart_get_export_function(ms->ms, name);
    JS_FreeCString(ctx, name);
    if (!fn) return JS_NULL;
    auto r = new WrtFunction(fn);
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  });

  // 6: pwart.callFunction(fnId, argTypes, argsArray, resultTypes) -> results[]
  pwartFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                 int argc, JSValue *argv) -> JSValue {
    int32_t fnId;
    if (JS_ToInt32(ctx, &fnId, argv[1]) < 0) return JS_NULL;
    auto fn = static_cast<WrtFunction *>(thisWrap->objStore[fnId]);
    if (!fn || !fn->fn) return JS_NewPlainError(ctx, "invalid function");

    const char *argTypes = JS_ToCString(ctx, argv[2]);
    const char *resultTypes = JS_ToCString(ctx, argv[4]);
    if (!argTypes || !resultTypes) {
      if (argTypes) JS_FreeCString(ctx, argTypes);
      if (resultTypes) JS_FreeCString(ctx, resultTypes);
      return JS_NewPlainError(ctx, "invalid type strings");
    }
    int argCount = (int)strlen(argTypes);
    int resultCount = (int)strlen(resultTypes);

    int stackSize = (argCount + resultCount + 16) * 16;
    if (stackSize < 4096) stackSize = 4096;
    void *stackBase = pwart_allocate_stack(stackSize);
    if (!stackBase) {
      JS_FreeCString(ctx, argTypes);
      JS_FreeCString(ctx, resultTypes);
      return JS_NewPlainError(ctx, "failed to allocate stack");
    }
    void *sp = stackBase;

    uint32_t argArrayLen = 0;
    JSValue lenVal = JS_GetPropertyStr(ctx, argv[3], "length");
    JS_ToUint32(ctx, &argArrayLen, lenVal);
    JS_FreeValue(ctx, lenVal);

    if ((int)argArrayLen != argCount) {
      pwart_free_stack(stackBase);
      JS_FreeCString(ctx, argTypes);
      JS_FreeCString(ctx, resultTypes);
      return JS_NewPlainError(ctx, "argument count mismatch: %d vs %u",
                              argCount, argArrayLen);
    }

    for (int i = 0; i < argCount; i++) {
      JSValue v = JS_GetPropertyUint32(ctx, argv[3], (uint32_t)i);
      switch (argTypes[i]) {
      case 'i': { int32_t x = 0; JS_ToInt32(ctx, &x, v);
                  pwart_rstack_put_i32(&sp, x); break; }
      case 'l': { int64_t x = 0; JS_ToInt64(ctx, &x, v);
                  pwart_rstack_put_i64(&sp, (long long)x); break; }
      case 'f': { double x = 0; JS_ToFloat64(ctx, &x, v);
                  pwart_rstack_put_f32(&sp, (float)x); break; }
      case 'd': { double x = 0; JS_ToFloat64(ctx, &x, v);
                  pwart_rstack_put_f64(&sp, x); break; }
      case 'r': { int64_t x = 0; JS_ToInt64(ctx, &x, v);
                  pwart_rstack_put_ref(&sp, (void *)(intptr_t)x); break; }
      default:
        JS_FreeValue(ctx, v);
        pwart_free_stack(stackBase);
        JS_FreeCString(ctx, argTypes);
        JS_FreeCString(ctx, resultTypes);
        return JS_NewPlainError(ctx, "unknown arg type: %c", argTypes[i]);
      }
      JS_FreeValue(ctx, v);
    }

    pwart_call_wasm_function(fn->fn, stackBase);

    JSValue results = JS_NewArray(ctx);
    for (int i = 0; i < resultCount; i++) {
      JSValue v = JS_UNDEFINED;
      switch (resultTypes[i]) {
      case 'i': v = JS_NewInt32(ctx, pwart_rstack_get_i32(&sp)); break;
      case 'l': v = JS_NewBigInt64(ctx, (int64_t)pwart_rstack_get_i64(&sp)); break;
      case 'f': v = JS_NewFloat64(ctx, (double)pwart_rstack_get_f32(&sp)); break;
      case 'd': v = JS_NewFloat64(ctx, pwart_rstack_get_f64(&sp)); break;
      case 'r': v = JS_NewBigInt64(ctx,
                                    (int64_t)(intptr_t)pwart_rstack_get_ref(&sp)); break;
      }
      JS_SetPropertyUint32(ctx, results, (uint32_t)i, v);
    }

    pwart_free_stack(stackBase);
    JS_FreeCString(ctx, argTypes);
    JS_FreeCString(ctx, resultTypes);
    return results;
  });

  // 7: pwart.getExportMemory(moduleStateId, name) -> memoryId | null
  pwartFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                 int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NULL;
    auto ms = static_cast<WrtModuleState *>(thisWrap->objStore[id]);
    if (!ms || !ms->ms) return JS_NULL;
    const char *name = JS_ToCString(ctx, argv[2]);
    if (!name) return JS_NULL;
    pwart_wasm_memory *mem = pwart_get_export_memory(ms->ms, name);
    JS_FreeCString(ctx, name);
    if (!mem) return JS_NULL;
    auto r = new WrtMemory(mem);
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  });

  // 8: pwart.readMemoryBytes(memId, offset, length) -> Uint8Array
  pwartFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                 int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NULL;
    auto mem = static_cast<WrtMemory *>(thisWrap->objStore[id]);
    if (!mem || !mem->mem || !mem->mem->bytes)
      return JS_NewPlainError(ctx, "invalid or native-imported memory");
    int32_t offset = 0, length = 0;
    JS_ToInt32(ctx, &offset, argv[2]);
    JS_ToInt32(ctx, &length, argv[3]);
    if (offset < 0 || length < 0 ||
        (uint64_t)offset + (uint64_t)length > pwartMemBytes(mem->mem))
      return JS_NewPlainError(ctx, "out of bounds");
    JSValue ab = JS_NewArrayBufferCopy(ctx, mem->mem->bytes + offset, length);
    JSValue global = JS_GetGlobalObject(ctx);
    JSValue ctor = JS_GetPropertyStr(ctx, global, "Uint8Array");
    JSValue args[1] = {ab};
    JSValue result = JS_CallConstructor(ctx, ctor, 1, args);
    JS_FreeValue(ctx, ab);
    JS_FreeValue(ctx, ctor);
    JS_FreeValue(ctx, global);
    return result;
  });

  // 9: pwart.writeMemoryBytes(memId, offset, data) -> void
  pwartFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                 int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_UNDEFINED;
    auto mem = static_cast<WrtMemory *>(thisWrap->objStore[id]);
    if (!mem || !mem->mem || !mem->mem->bytes)
      return JS_NewPlainError(ctx, "invalid or native-imported memory");
    int32_t offset = 0;
    JS_ToInt32(ctx, &offset, argv[2]);
    size_t dataLen = 0;
    uint8_t *data = JS_GetUint8Array(ctx, &dataLen, argv[3]);
    if (!data) return JS_NewPlainError(ctx, "data must be a Uint8Array");
    if (offset < 0 ||
        (uint64_t)offset + (uint64_t)dataLen > pwartMemBytes(mem->mem))
      return JS_NewPlainError(ctx, "out of bounds");
    memcpy(mem->mem->bytes + offset, data, dataLen);
    return JS_UNDEFINED;
  });

  // 10: pwart.getMemorySize(memId) -> BigInt (bytes)
  pwartFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                 int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NewBigInt64(ctx, 0);
    auto mem = static_cast<WrtMemory *>(thisWrap->objStore[id]);
    if (!mem || !mem->mem) return JS_NewBigInt64(ctx, 0);
    return JS_NewBigInt64(ctx, (int64_t)pwartMemBytes(mem->mem));
  });


  pwartFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                 int argc, JSValue *argv) -> JSValue {
    int32_t nsId;
    if (JS_ToInt32(ctx, &nsId, argv[1]) < 0) return JS_NULL;
    auto ns = static_cast<WrtNamespace *>(thisWrap->objStore[nsId]);
    if (!ns || !ns->ns) return JS_NewPlainError(ctx, "invalid namespace");
    const char *moduleName = JS_ToCString(ctx, argv[2]);
    const char *fieldName = JS_ToCString(ctx, argv[3]);
    if (!moduleName || !fieldName) {
      if (moduleName) JS_FreeCString(ctx, moduleName);
      if (fieldName) JS_FreeCString(ctx, fieldName);
      return JS_NewPlainError(ctx, "invalid module/field name");
    }
    uint32_t kind = 0;
    JS_ToUint32(ctx, &kind, argv[4]);
    const char *importerName = nullptr;
    if (argc > 5 && !JS_IsUndefined(argv[5]) && !JS_IsNull(argv[5]))
      importerName = JS_ToCString(ctx, argv[5]);
    const char *err = nullptr;
    JSValue ret = pwartResolveSymbolImpl(thisWrap, ctx, ns->ns, moduleName,
                                         fieldName, kind, importerName, &err);
    JS_FreeCString(ctx, moduleName);
    JS_FreeCString(ctx, fieldName);
    if (importerName) JS_FreeCString(ctx, importerName);
    if (err) return JS_NewPlainError(ctx, "%s", err);
    return ret;
  });

  // 12: pwart.namespaceResolveFunction(nsId, moduleName, fieldName[, importerName])
  // 13: pwart.namespaceResolveTable(nsId, moduleName, fieldName[, importerName])
  // 14: pwart.namespaceResolveMemory(nsId, moduleName, fieldName[, importerName])
  // 15: pwart.namespaceResolveGlobal(nsId, moduleName, fieldName[, importerName])
#define _TXIKIJS_PWART_PUSH_RESOLVE_SHORTCUT(kindVal)                                  \
  pwartFunc2026List.push_back(                                                \
      [](TjsRuntimeWrap *thisWrap, JSContext *ctx, int argc,                  \
         JSValue *argv) -> JSValue {                                          \
        int32_t nsId;                                                        \
        if (JS_ToInt32(ctx, &nsId, argv[1]) < 0) return JS_NULL;              \
        auto ns = static_cast<WrtNamespace *>(thisWrap->objStore[nsId]);      \
        if (!ns || !ns->ns)                                                   \
          return JS_NewPlainError(ctx, "invalid namespace");                  \
        const char *moduleName = JS_ToCString(ctx, argv[2]);                  \
        const char *fieldName = JS_ToCString(ctx, argv[3]);                   \
        if (!moduleName || !fieldName) {                                      \
          if (moduleName) JS_FreeCString(ctx, moduleName);                    \
          if (fieldName) JS_FreeCString(ctx, fieldName);                      \
          return JS_NewPlainError(ctx, "invalid module/field name");          \
        }                                                                     \
        const char *importerName = nullptr;                                   \
        if (argc > 4 && !JS_IsUndefined(argv[4]) && !JS_IsNull(argv[4]))      \
          importerName = JS_ToCString(ctx, argv[4]);                          \
        const char *err = nullptr;                                            \
        JSValue ret = pwartResolveSymbolImpl(thisWrap, ctx, ns->ns,            \
                                             moduleName, fieldName,           \
                                             (kindVal), importerName, &err);  \
        JS_FreeCString(ctx, moduleName);                                      \
        JS_FreeCString(ctx, fieldName);                                       \
        if (importerName) JS_FreeCString(ctx, importerName);                  \
        if (err) return JS_NewPlainError(ctx, "%s", err);                     \
        return ret;                                                           \
      });

  _TXIKIJS_PWART_PUSH_RESOLVE_SHORTCUT(PWART_KIND_FUNCTION);  // 12
  _TXIKIJS_PWART_PUSH_RESOLVE_SHORTCUT(PWART_KIND_TABLE);     // 13
  _TXIKIJS_PWART_PUSH_RESOLVE_SHORTCUT(PWART_KIND_MEMORY);    // 14
  _TXIKIJS_PWART_PUSH_RESOLVE_SHORTCUT(PWART_KIND_GLOBAL);    // 15
#undef _TXIKIJS_PWART_PUSH_RESOLVE_SHORTCUT
}
} // namespace __pwartBinding
} // namespace pxprpc_txikijs