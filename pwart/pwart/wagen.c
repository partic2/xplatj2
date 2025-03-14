#ifndef _PWART_WAGEN_C
#define _PWART_WAGEN_C

#include "def.h"

#include "extfunc.c"
#include "opgen_utils.c"
#include "opgen_ctl.c"
#include "opgen_mem.c"
#include "opgen_num.c"
#include "opgen_misc.c"


static void wasm_SkipImmediates(uint8_t *bytes, uint32_t *pos) {
  uint32_t count, opcode = bytes[*pos];
  *pos = *pos + 1;
  switch (opcode) {
  // varuint1
  case 0x3f: case 0x40: // current_memory, memory.grow
    read_LEB(bytes, pos, 1);
    break;
  // varuint32, varint32
  case 0x0c: case 0x0d: // br, br_if
  case 0x10:          // call
    read_LEB(bytes, pos, 32);
    break;
  case 0x20: //local.get
    read_LEB(bytes, pos, 32);
    break;
  case 0x21: //local.set
    read_LEB(bytes, pos, 32);
    break;
  case 0x22: //local.tee
    read_LEB(bytes, pos, 32);
    break;
  case 0x23: //global.get
  case 0x24: //global.set
  case 0xd0: //ref.null
  case 0xd2: //ref.func
    read_LEB(bytes, pos, 32);
    break;
  case 0x41:          // i32.const
    read_LEB(bytes, pos, 32);
    break;
  // varuint32 + varuint1
  case 0x11: // call_indirect
    read_LEB(bytes, pos, 32);
    read_LEB(bytes, pos, 1);
    break;
  case 0x1c: // select t
    read_LEB(bytes, pos, 32);
    break;
  // varint64
  case 0x42: // i64.const
    read_LEB(bytes, pos, 64);
    break;
  // uint32
  case 0x43: // f32.const
    *pos += 4;
    break;
  // uint64
  case 0x44: // f64.const
    *pos += 8;
    break;
  // block_type
  case 0x02: case 0x03: case 0x04: // block, loop, if
    read_LEB_signed(bytes, pos, 33);
    break;
  // memory_immediate
  case 0x28:  case 0x29:  case 0x2a:  case 0x2b:  case 0x2c:  case 0x2d:  case 0x2e:  case 0x2f:  case 0x30:  case 0x31:  case 0x32:  case 0x33:  case 0x34:  case 0x35:  case 0x36:  case 0x37:  case 0x38:  case 0x39:  case 0x3a:  case 0x3b:  case 0x3c:  case 0x3d: case 0x3e:
  // *.load*, *.store*
  {
    uint32_t align=read_LEB(bytes, pos, 32);
    if(align&0x40){read_LEB(bytes,pos,32);}//memory index
    read_LEB(bytes, pos, 32);
    break;
  }
  // br_table
  case 0x0e:                          // br_table
    count = read_LEB(bytes, pos, 32); // target count
    for (uint32_t i = 0; i < count; i++) {
      read_LEB(bytes, pos, 32);
    }
    read_LEB(bytes, pos, 32); // default target
    break;
  case 0xfc: //misc op.
  {
    int opc2=bytes[*pos];
    *pos = *pos + 1;
    switch(opc2){
      case 0xa:
      case 0xe:
      read_LEB(bytes,pos,32); 
      read_LEB(bytes,pos,32); 
      break;
      case 0x0b:
      case 0x0f: case 0x10: case 0x11:
      read_LEB(bytes,pos,32); 
      break;
    }
  }
    break;
  default: // no immediates
    break;
  }
}



static char *pwart_PrepareFunc(ModuleCompiler *m) {
  uint8_t *bytes = m->bytes;
  StackValue *stack = m->stack;
  StackValue *sv=NULL;

  uint32_t cur_pc;

  uint32_t arg, val, fidx, tidx, depth, count,tabidx,midx;
  uint32_t flags, offset, addr;
  uint8_t opcode, eof=0;
  uint32_t len = 0;
  Block *blk = NULL;
  m->locals = NULL;
  dynarr_init(&m->locals, sizeof(StackValue));
  m->blocks=NULL;
  dynarr_init(&m->blocks,sizeof(Block));
  m->cached_midx=-1;
  int i;

  int paramCnt=0;
  for (i = 0; m->function_type->params[i] != 0; i++) {
    sv = dynarr_push_type(&m->locals, StackValue);
    sv->wasm_type = m->function_type->params[i];
  }
  paramCnt=i;

  for (i = 0; m->function_locals_type[i] != 0; i++) {
    sv = dynarr_push_type(&m->locals, StackValue);
    sv->wasm_type = m->function_locals_type[i];
    //XXX: use sv->jit_type as locals flags. 1:Use before Set detected.
    sv->jit_type=0;
    //XXX: use sv->val.op as the most top level the local.set take effect.
    sv->val.op=0xffff;
  }
  // initialize to -1
  // if local variable required, set to -2 and set to real value(>=0) after code
  // scan.
  m->mem_base_local = -1;
  m->table_entries_local = -1;

  while (!eof && m->pc < m->byte_count) {
    opcode = bytes[m->pc];
    cur_pc = m->pc;
    m->pc += 1;

    #if DEBUG_BUILD
    m->insnCnt++;
    #endif

    switch (opcode) {
    //
    // Control flow operators
    //
    case 0x02: // block
      read_LEB_signed(bytes, &m->pc, 33);
      blk=dynarr_push_type(&m->blocks,Block);
      blk->block_type=0x02;
      break;
    case 0x03: // loop
      read_LEB_signed(bytes, &m->pc, 33);
      blk=dynarr_push_type(&m->blocks,Block);
      blk->block_type=0x03;
      break;
    case 0x04: // if
      read_LEB_signed(bytes, &m->pc, 33);
      blk=dynarr_push_type(&m->blocks,Block);
      blk->block_type=0x04;
      break;
    case 0x05: // else
      for(i=0;i<m->locals->len;i++){
        sv=dynarr_get(m->locals,StackValue,i);
        if(sv->val.op>=m->blocks->len)sv->val.op=0xffff;
      }
      break;
    case WASMOPC_br:
    case WASMOPC_br_if: 
      read_LEB(bytes, &m->pc, 32);
      break;
    case 0x0b: // end
      if (m->blocks->len <= 0) {
        eof = 1;
      }else{
        blk=dynarr_pop_type(&m->blocks,Block);
        for(i=0;i<m->locals->len;i++){
          sv=dynarr_get(m->locals,StackValue,i);
          if(sv->val.op>m->blocks->len)sv->val.op=0xffff;
        }
      }
      break;
    case WASMOPC_call:
    case WASMOPC_return_call:
      fidx = read_LEB(bytes, &m->pc, 32);
      break;
    case WASMOPC_call_indirect:
    case WASMOPC_return_call_indirect:
      tidx = read_LEB(bytes, &m->pc, 32); //type
      tabidx=read_LEB(bytes, &m->pc, 1); //table index
      if(tabidx==0)m->table_entries_local = -2;
      break;
    case 0x23: // global.get
      arg = read_LEB(bytes, &m->pc, 32);
      break;
    case 0x24: // global.set
      arg = read_LEB(bytes, &m->pc, 32);
      break;
    case 0x25:                            // table.get
      tidx = read_LEB(bytes, &m->pc, 32); // table index
      if(tidx==0)m->table_entries_local = -2;
      break;
    case 0x26:                            // table.set
      tidx = read_LEB(bytes, &m->pc, 32); // table index
      if(tidx==0)m->table_entries_local = -2;
      break;
    // Memory load/store operators
    case 0x28:  case 0x29:  case 0x2a:  case 0x2b:  case 0x2c:  case 0x2d:  case 0x2e:  case 0x2f:  case 0x30:  case 0x31:  case 0x32:  case 0x33:  case 0x34:  case 0x35:  case 0x36:  case 0x37:  case 0x38:  case 0x39:  case 0x3a:  case 0x3b:  case 0x3c:  case 0x3d:  case 0x3e: 
      midx=0;
      arg = read_LEB(m->bytes, &m->pc, 32);
      offset = read_LEB(m->bytes, &m->pc, 32);
      if(arg&0x40)midx=read_LEB(m->bytes, &m->pc, 32);
      if(m->cached_midx<0 && (*dynarr_get(m->context->memories,Memory *,midx))->bytes!=NULL){
        m->mem_base_local = -2;
        m->cached_midx=midx;
      }
      break;
    case 0xd2: // ref.func
      fidx = read_LEB(bytes, &m->pc, 32);
      break;
    case 0x20: //local.get
      arg=read_LEB(bytes, &m->pc, 32);
      sv=dynarr_get(m->locals,StackValue,arg);
      if(pwart_gcfg.misc_flags&PWART_MISC_FLAGS_LOCALS_ZERO_INIT){
        if(sv->val.op>m->blocks->len)sv->jit_type|=1;
      }
      break;
    case 0x21: //local.set
      arg=read_LEB(bytes, &m->pc, 32);
      sv=dynarr_get(m->locals,StackValue,arg);
      if(pwart_gcfg.misc_flags&PWART_MISC_FLAGS_LOCALS_ZERO_INIT){
        if(sv->val.op>m->blocks->len)sv->val.op=m->blocks->len;
      }
      break;
    case 0x22: //local.tee
      arg=read_LEB(bytes, &m->pc, 32);
      sv=dynarr_get(m->locals,StackValue,arg);
      if(pwart_gcfg.misc_flags&PWART_MISC_FLAGS_LOCALS_ZERO_INIT){
        if(sv->val.op>m->blocks->len)sv->val.op=m->blocks->len;
      }
      break;
    default:
      m->pc--;
      wasm_SkipImmediates(m->bytes, &m->pc);
      break;
    }
  }
  if(m->bytes[m->pc - 1] != 0xb){return "Function not end with 0x0b, unsupported feature may used.";}
  m->locals_need_zero=NULL;
  if(pwart_gcfg.misc_flags&PWART_MISC_FLAGS_LOCALS_ZERO_INIT){
    dynarr_init(&m->locals_need_zero,sizeof(int16_t));
    for(int i1=paramCnt;i1<m->locals->len;i1++){
      sv=dynarr_get(m->locals,StackValue,i1);
      if(sv->jit_type&1){
        *dynarr_push_type(&m->locals_need_zero,int16_t)=(int16_t)i1;
      }
    }
  }
  
  if (m->mem_base_local == -2) {
    m->mem_base_local = m->locals->len;
    sv = dynarr_push_type(&m->locals, StackValue);
    sv->wasm_type = WVT_REF;
  }
  if (m->table_entries_local == -2) {
    m->table_entries_local = m->locals->len;
    sv = dynarr_push_type(&m->locals, StackValue);
    sv->wasm_type = WVT_REF;
  }
  dynarr_free(&m->blocks);
  return NULL;
}

/*
r0,r1,r2 is scratch registers(at least three registers are required.).
s0(arg0) contains stack frame pointer.
*/
static char *pwart_GenCode(ModuleCompiler *m) {
  Block *block;
  int opcode,cur_pc;

  #if DEBUG_BUILD
  int insnCnt2=0;
  #endif

  m->jitc = sljit_create_compiler(NULL);
#if DEBUG_BUILD
  m->jitc->verbose = stdout;
#endif

  pwart_EmitFuncEnter(m);

  m->blocks = NULL;
  dynarr_init(&m->blocks, sizeof(Block));
  m->br_table = NULL;
  dynarr_init(&m->br_table, sizeof(uint32_t));

  block = dynarr_push_type(&m->blocks, Block);
  block->block_type = 0x00;
  m->eof=0;
  m->block_returned=0;

  while (!m->eof && m->pc < m->byte_count) {
    opcode = m->bytes[m->pc];
    cur_pc = m->pc;
    m->pc += 1;

#if DEBUG_BUILD
    if(opcode<=0xd2){
      wa_debug("op %x:%s\n", m->pc, OPERATOR_INFO[opcode]);
    }
    m->insnCnt--;
#endif

    // XXX: save flag if next op is not if, br_if or i32.eqz(not).
    if (m->sp>=0 && m->stack[m->sp].jit_type == SVT_CMP && 
    opcode != WASMOPC_if && opcode != WASMOPC_br_if && opcode != WASMOPC_i32_eqz && opcode !=WASMOPC_select && opcode != WASMOPC_select_t) {
      pwart_EmitStackValueLoadReg(m, &m->stack[m->sp]);
    }

    if (opcode >= 0 && opcode <= 0x1b) {
      ReturnIfErr(opgen_GenCtlOp(m, opcode));
    } else if (opcode <= 0x44) {
      ReturnIfErr(opgen_GenMemOp(m, opcode));
    } else if (opcode <= 0xc4) {
      ReturnIfErr(opgen_GenNumOp(m, opcode));
    } else {
      ReturnIfErr(opgen_GenMiscOp(m, opcode));
    }
  }

  #if DEBUG_BUILD
  if(m->insnCnt!=0){
    wa_debug("instruction count is not matched(prepare phase-generate phase=%d).\n",m->insnCnt);
    abort();
  }
  #endif

  dynarr_free(&m->br_table);
  dynarr_free(&m->blocks);
  return NULL;
}

static char *pwart_EmitFunction(ModuleCompiler *m, WasmFunction *func) {
  WasmFunctionEntry code;
  uint32_t savepos = m->pc;

  // Empty stacks
  m->sp = -1;

  m->function_type = dynarr_get(m->types, Type, func->tidx);
  m->function_locals_type = func->locals_type;

  m->locals = NULL;
  dynarr_init(&m->locals, sizeof(StackValue));

  ReturnIfErr(pwart_PrepareFunc(m));
  m->pc = savepos;
  ReturnIfErr(pwart_GenCode(m));
  code = (WasmFunctionEntry)sljit_generate_code(m->jitc,0,NULL);
  sljit_free_compiler(m->jitc);
  m->jitc=NULL;
  dynarr_free(&m->locals);
  func->func_ptr = code;
  return NULL;
}

static void pwart_FreeFunction(WasmFunctionEntry code) {
  sljit_free_code(code, NULL);
}

#endif