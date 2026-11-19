#pragma once

#include "quickjs.h"
#include <string>
#include <vector>
#include <functional>
#include <cstring>
#include <cstdlib>
#include <cstdint>
#include <memory>
extern "C" {
#include <widcc_jit.h>
}

#include <pxprpc-txikijs/init.hpp>

namespace pxprpc_txikijs {

namespace __widccjitBinding {
using string = std::string;

std::vector<std::function<JSValue(TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                  int argc, JSValue *argv)>>
    widccjitFunc2026List;

static JSValue widccjitFunc2026(JSContext *ctx, JSValue this_val, int argc,
                                JSValue *argv) {
  int32_t i32;
  JS_ToInt32(ctx, &i32, argv[0]);
  if (i32 >= 0 && i32 < (int)widccjitFunc2026List.size()) {
    auto classId = JS_GetClassID(this_val);
    if (classId == JS_INVALID_CLASS_ID) {
      return JS_EXCEPTION;
    }
    auto thisWrap = static_cast<TjsRuntimeWrap *>(
        JS_GetOpaque(this_val, classId));
    return widccjitFunc2026List[i32](thisWrap, ctx, argc, argv);
  } else {
    return JS_UNDEFINED;
  }
}

static pxprpc::PxpObject *widccObjectAt(TjsRuntimeWrap *thisWrap, int32_t id) {
  if (!thisWrap || id < 0 || (size_t)id >= thisWrap->objStore.size())
    return nullptr;
  return thisWrap->objStore[id];
}

static JSValue widccAddressValue(JSContext *ctx, void *address) {
  return JS_NewBigInt64(ctx, (int64_t)(intptr_t)address);
}

// Shared by runtime, module and symbol handles so that freeing the runtime
// handle first does not invalidate modules/symbols which still reference it.
struct WidccRuntimeState {
  WidccRT *rt = nullptr;
  ~WidccRuntimeState() {
    if (rt) { widcc_rt_free(rt); rt = nullptr; }
  }
};

class WrtRuntime : public pxprpc::PxpObject {
public:
  std::shared_ptr<WidccRuntimeState> state;
  WrtRuntime(WidccRT *rt) {
    state = std::make_shared<WidccRuntimeState>();
    state->rt = rt;
  }
};

class WrtModule : public pxprpc::PxpObject {
public:
  std::shared_ptr<WidccRuntimeState> state;
  WidccCode *code = nullptr;
  WrtModule(std::shared_ptr<WidccRuntimeState> state, WidccCode *code)
      : state(state), code(code) {}
  void unload() {
    if (code && state->rt) {
      widcc_rt_unload_code(state->rt, code);
    }
    code = nullptr;
  }
  virtual ~WrtModule() { unload(); }
};

class WrtSymbol : public pxprpc::PxpObject {
public:
  std::shared_ptr<WidccRuntimeState> state;
  string name;
  void *address = nullptr;
  WrtSymbol(std::shared_ptr<WidccRuntimeState> state, const string &name,
            void *address)
      : state(state), name(name), address(address) {}
};

// widcc generates ordinary host machine code, so calls go through the C ABI.
// Only pointer-sized values (integers and pointers) can be passed here.
static void *widccCallNative(void *fn, int argc, void **argv) {
  if (argc <= 4) {
    return ((void *(*)(void *, void *, void *, void *))fn)(
        argv[0], argv[1], argv[2], argv[3]);
  } else {
    return ((void *(*)(void *, void *, void *, void *, void *, void *, void *,
                       void *, void *, void *, void *, void *, void *, void *,
                       void *, void *))fn)(
        argv[0], argv[1], argv[2], argv[3], argv[4], argv[5], argv[6],
        argv[7], argv[8], argv[9], argv[10], argv[11], argv[12], argv[13],
        argv[14], argv[15]);
  }
}

void init() {
  // 0: widccjit.getSizeOfFuncList()
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    return JS_NewInt32(ctx, (int32_t)widccjitFunc2026List.size());
  });

  // 1: widccjit.newRuntime() -> runtimeId
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    WidccRT *rt = widcc_rt_new();
    if (!rt) {
      return JS_NewPlainError(ctx, "widcc_rt_new failed");
    }
    auto r = new WrtRuntime(rt);
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  });

  // 2: widccjit.deleteRuntime(runtimeId) -> void
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_UNDEFINED;
    auto rt = static_cast<WrtRuntime *>(widccObjectAt(thisWrap, id));
    if (rt && rt->state->rt) {
      widcc_rt_free(rt->state->rt);
      rt->state->rt = nullptr;
    }
    return JS_UNDEFINED;
  });

  // 3: widccjit.addHostFunction(runtimeId, name, address) -> bool
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NewBool(ctx, 0);
    auto rt = static_cast<WrtRuntime *>(widccObjectAt(thisWrap, id));
    if (!rt || !rt->state->rt) {
      return JS_NewPlainError(ctx, "invalid runtime");
    }
    const char *name = JS_ToCString(ctx, argv[2]);
    if (!name) return JS_NewPlainError(ctx, "invalid name");
    int64_t address = 0;
    if (JS_ToInt64Ext(ctx, &address, argv[3]) < 0) {
      JS_FreeCString(ctx, name);
      return JS_NewPlainError(ctx, "invalid address");
    }
    const char *err =
        widcc_rt_add_func(rt->state->rt, name, (void *)(intptr_t)address);
    JS_FreeCString(ctx, name);
    if (err) return JS_NewPlainError(ctx, "%s", err);
    return JS_NewBool(ctx, 1);
  });

  // 4: widccjit.addHostVariable(runtimeId, name, address) -> bool
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NewBool(ctx, 0);
    auto rt = static_cast<WrtRuntime *>(widccObjectAt(thisWrap, id));
    if (!rt || !rt->state->rt) {
      return JS_NewPlainError(ctx, "invalid runtime");
    }
    const char *name = JS_ToCString(ctx, argv[2]);
    if (!name) return JS_NewPlainError(ctx, "invalid name");
    int64_t address = 0;
    if (JS_ToInt64Ext(ctx, &address, argv[3]) < 0) {
      JS_FreeCString(ctx, name);
      return JS_NewPlainError(ctx, "invalid address");
    }
    const char *err =
        widcc_rt_add_var(rt->state->rt, name, (void *)(intptr_t)address);
    JS_FreeCString(ctx, name);
    if (err) return JS_NewPlainError(ctx, "%s", err);
    return JS_NewBool(ctx, 1);
  });

  // 5: widccjit.lookup(runtimeId, name) -> symbolId | null
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NULL;
    auto rt = static_cast<WrtRuntime *>(widccObjectAt(thisWrap, id));
    if (!rt || !rt->state->rt) return JS_NULL;
    const char *name = JS_ToCString(ctx, argv[2]);
    if (!name) return JS_NULL;
    void *address = widcc_rt_lookup(rt->state->rt, name);
    string symbolName(name);
    JS_FreeCString(ctx, name);
    if (!address) return JS_NULL;
    auto r = new WrtSymbol(rt->state, symbolName, address);
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  });

  // 6: widccjit.listSymbols(runtimeId) -> string[]
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NULL;
    auto rt = static_cast<WrtRuntime *>(widccObjectAt(thisWrap, id));
    if (!rt || !rt->state->rt) {
      return JS_NewPlainError(ctx, "invalid runtime");
    }
    int count = widcc_rt_list_symbols(rt->state->rt, nullptr, 0);
    std::vector<char *> names(count > 0 ? count : 0);
    if (count > 0) {
      widcc_rt_list_symbols(rt->state->rt, names.data(), count);
    }
    JSValue arr = JS_NewArray(ctx);
    for (int i = 0; i < count; i++) {
      JS_SetPropertyUint32(ctx, arr, (uint32_t)i,
                          JS_NewString(ctx, names[i]));
    }
    return arr;
  });

  // 7: widccjit.setIncludePaths(runtimeId, paths[]) -> void
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_UNDEFINED;
    auto rt = static_cast<WrtRuntime *>(widccObjectAt(thisWrap, id));
    if (!rt || !rt->state->rt) {
      return JS_NewPlainError(ctx, "invalid runtime");
    }
    if (!JS_IsArray(argv[2])) {
      return JS_NewPlainError(ctx, "include paths must be an array");
    }
    uint32_t count = 0;
    JSValue lenVal = JS_GetPropertyStr(ctx, argv[2], "length");
    JS_ToUint32(ctx, &count, lenVal);
    JS_FreeValue(ctx, lenVal);
    std::vector<string> paths(count);
    for (uint32_t i = 0; i < count; i++) {
      JSValue v = JS_GetPropertyUint32(ctx, argv[2], i);
      const char *path = JS_ToCString(ctx, v);
      if (!path) {
        JS_FreeValue(ctx, v);
        return JS_NewPlainError(ctx, "invalid include path");
      }
      paths[i] = path;
      JS_FreeCString(ctx, path);
      JS_FreeValue(ctx, v);
    }
    std::vector<const char *> cpaths;
    for (auto &path : paths) {
      cpaths.push_back(path.c_str());
    }
    const char *err = widcc_rt_set_include_paths(
        rt->state->rt, count > 0 ? cpaths.data() : nullptr, (int)count);
    if (err) return JS_NewPlainError(ctx, "%s", err);
    return JS_UNDEFINED;
  });

  // 8: widccjit.loadCode(runtimeId, path, source) -> moduleId
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NULL;
    auto rt = static_cast<WrtRuntime *>(widccObjectAt(thisWrap, id));
    if (!rt || !rt->state->rt) {
      return JS_NewPlainError(ctx, "invalid runtime");
    }
    const char *path = JS_ToCString(ctx, argv[2]);
    const char *source = JS_ToCString(ctx, argv[3]);
    if (!path || !source) {
      if (path) JS_FreeCString(ctx, path);
      if (source) JS_FreeCString(ctx, source);
      return JS_NewPlainError(ctx, "invalid path or source");
    }
    WidccCode *code = nullptr;
    const char *err = widcc_rt_load_code(rt->state->rt, path, source, &code);
    JS_FreeCString(ctx, path);
    JS_FreeCString(ctx, source);
    if (err) return JS_NewPlainError(ctx, "%s", err);
    auto r = new WrtModule(rt->state, code);
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  });

  // 9: widccjit.loadCodeFile(runtimeId, path) -> moduleId
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NULL;
    auto rt = static_cast<WrtRuntime *>(widccObjectAt(thisWrap, id));
    if (!rt || !rt->state->rt) {
      return JS_NewPlainError(ctx, "invalid runtime");
    }
    const char *path = JS_ToCString(ctx, argv[2]);
    if (!path) return JS_NewPlainError(ctx, "invalid path");
    WidccCode *code = nullptr;
    const char *err = widcc_rt_load_code_file(rt->state->rt, path, &code);
    JS_FreeCString(ctx, path);
    if (err) return JS_NewPlainError(ctx, "%s", err);
    auto r = new WrtModule(rt->state, code);
    return JS_NewInt32(ctx, thisWrap->saveObject(r));
  });

  // 10: widccjit.unloadCode(moduleId) -> bool
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NewBool(ctx, 0);
    auto mod = static_cast<WrtModule *>(widccObjectAt(thisWrap, id));
    if (!mod || !mod->code) return JS_NewBool(ctx, 0);
    mod->unload();
    return JS_NewBool(ctx, 1);
  });

  // 11: widccjit.symbolAddress(symbolId) -> BigInt
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NewBigInt64(ctx, 0);
    auto sym = static_cast<WrtSymbol *>(widccObjectAt(thisWrap, id));
    if (!sym) return JS_NewBigInt64(ctx, 0);
    return widccAddressValue(ctx, sym->address);
  });

  // 12: widccjit.callFunction(symbolId, argTypes, argsArray, resultTypes)
  //     argTypes/resultTypes use one char per value: 'i' int32, 'l' int64,
  //     'p' pointer. Results are returned as an array.
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int32_t id;
    if (JS_ToInt32(ctx, &id, argv[1]) < 0) return JS_NULL;
    auto sym = static_cast<WrtSymbol *>(widccObjectAt(thisWrap, id));
    if (!sym || !sym->address) {
      return JS_NewPlainError(ctx, "invalid symbol");
    }

    const char *argTypes = JS_ToCString(ctx, argv[2]);
    const char *resultTypes = JS_ToCString(ctx, argv[4]);
    if (!argTypes || !resultTypes) {
      if (argTypes) JS_FreeCString(ctx, argTypes);
      if (resultTypes) JS_FreeCString(ctx, resultTypes);
      return JS_NewPlainError(ctx, "invalid type strings");
    }
    int argCount = (int)strlen(argTypes);
    int resultCount = (int)strlen(resultTypes);
    if (argCount > 16 || resultCount > 1) {
      JS_FreeCString(ctx, argTypes);
      JS_FreeCString(ctx, resultTypes);
      return JS_NewPlainError(
          ctx, "at most 16 arguments and 1 result are supported");
    }
    if (!JS_IsArray(argv[3])) {
      JS_FreeCString(ctx, argTypes);
      JS_FreeCString(ctx, resultTypes);
      return JS_NewPlainError(ctx, "arguments must be an array");
    }

    uint32_t argArrayLen = 0;
    JSValue lenVal = JS_GetPropertyStr(ctx, argv[3], "length");
    JS_ToUint32(ctx, &argArrayLen, lenVal);
    JS_FreeValue(ctx, lenVal);
    if ((int)argArrayLen != argCount) {
      JS_FreeCString(ctx, argTypes);
      JS_FreeCString(ctx, resultTypes);
      return JS_NewPlainError(ctx, "argument count mismatch: %d vs %u",
                              argCount, argArrayLen);
    }

    void *slots[16] = {nullptr};
    for (int i = 0; i < argCount; i++) {
      JSValue v = JS_GetPropertyUint32(ctx, argv[3], (uint32_t)i);
      int64_t x = 0;
      switch (argTypes[i]) {
      case 'i':
      case 'l':
      case 'p':
        if (JS_ToInt64Ext(ctx, &x, v) < 0) {
          JS_FreeValue(ctx, v);
          JS_FreeCString(ctx, argTypes);
          JS_FreeCString(ctx, resultTypes);
          return JS_EXCEPTION;
        }
        slots[i] = (void *)(intptr_t)x;
        break;
      default:
        JS_FreeValue(ctx, v);
        JS_FreeCString(ctx, argTypes);
        JS_FreeCString(ctx, resultTypes);
        return JS_NewPlainError(ctx, "unknown arg type: %c", argTypes[i]);
      }
      JS_FreeValue(ctx, v);
    }

    void *raw = widccCallNative(sym->address, argCount, slots);

    JSValue results = JS_NewArray(ctx);
    if (resultCount == 1) {
      JSValue v = JS_UNDEFINED;
      switch (resultTypes[0]) {
      case 'v':
        break;
      case 'i':
        v = JS_NewInt32(ctx, (int32_t)(intptr_t)raw);
        break;
      case 'l':
      case 'p':
        v = JS_NewBigInt64(ctx, (int64_t)(intptr_t)raw);
        break;
      default:
        JS_FreeValue(ctx, results);
        JS_FreeCString(ctx, argTypes);
        JS_FreeCString(ctx, resultTypes);
        return JS_NewPlainError(ctx, "unknown result type: %c",
                                resultTypes[0]);
      }
      JS_SetPropertyUint32(ctx, results, 0, v);
    }

    JS_FreeCString(ctx, argTypes);
    JS_FreeCString(ctx, resultTypes);
    return results;
  });

  // 13: widccjit.readMemory(address, length) -> Uint8Array
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int64_t address = 0;
    int32_t length = 0;
    if (JS_ToInt64Ext(ctx, &address, argv[1]) < 0) return JS_NULL;
    JS_ToInt32(ctx, &length, argv[2]);
    if (!address || length < 0) {
      return JS_NewPlainError(ctx, "invalid address or length");
    }
    return JS_NewUint8ArrayCopy(
        ctx, reinterpret_cast<const uint8_t *>((intptr_t)address), length);
  });

  // 14: widccjit.writeMemory(address, data) -> void
  widccjitFunc2026List.push_back([](TjsRuntimeWrap *thisWrap, JSContext *ctx,
                                    int argc, JSValue *argv) -> JSValue {
    int64_t address = 0;
    if (JS_ToInt64Ext(ctx, &address, argv[1]) < 0) return JS_NULL;
    size_t dataLen = 0;
    uint8_t *data = JS_GetUint8Array(ctx, &dataLen, argv[2]);
    if (!data) return JS_NewPlainError(ctx, "data must be a Uint8Array");
    if (dataLen == 0) return JS_UNDEFINED;
    if (!address) {
      return JS_NewPlainError(ctx, "invalid address");
    }
    memcpy(reinterpret_cast<void *>((intptr_t)address), data, dataLen);
    return JS_UNDEFINED;
  });
}
} // namespace __widccjitBinding
} // namespace pxprpc_txikijs
