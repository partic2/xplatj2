(module
  (type (;0;) (func (param i32 i32 i32)))
  (type (;1;) (func))
  (type (;2;) (func (result i32)))
  (type (;3;) (func (param i32)))
  (type (;4;) (func (param i32) (result i32)))
  (func $emscripten_memcpy_big (type 0)
  (memory.copy (local.get 0)(local.get 1)(local.get 2))
  )
  (func (;1;) (type 1)
    nop)
  (func (;2;) (type 0) (param i32 i32 i32)
    (local i32 i32 i32 i32 i32 i32 i32 i32)
    local.get 1
    local.get 2
    i32.lt_s
    if  ;; label = @1
      i32.const 0
      local.get 2
      i32.sub
      local.set 10
      local.get 0
      local.get 2
      i32.const 2
      i32.shl
      i32.add
      local.set 7
      loop  ;; label = @2
        local.get 7
        i32.load
        local.set 6
        block  ;; label = @3
          local.get 1
          local.get 2
          i32.ge_s
          if  ;; label = @4
            local.get 1
            local.set 3
            br 1 (;@3;)
          end
          local.get 1
          i32.const -1
          i32.xor
          local.set 5
          local.get 1
          local.set 4
          local.get 2
          local.get 1
          local.tee 3
          i32.sub
          i32.const 1
          i32.and
          if  ;; label = @4
            local.get 1
            i32.const 1
            i32.add
            local.tee 4
            local.get 1
            local.get 0
            local.get 1
            i32.const 2
            i32.shl
            i32.add
            i32.load
            local.get 6
            i32.lt_s
            select
            local.set 3
          end
          local.get 5
          local.get 10
          i32.ne
          if  ;; label = @4
            loop  ;; label = @5
              local.get 6
              local.get 0
              local.get 4
              i32.const 2
              i32.shl
              i32.add
              local.tee 5
              i32.load
              local.tee 8
              i32.gt_s
              if  ;; label = @6
                local.get 3
                local.get 4
                i32.ne
                if  ;; label = @7
                  local.get 5
                  local.get 0
                  local.get 3
                  i32.const 2
                  i32.shl
                  i32.add
                  local.tee 9
                  i32.load
                  i32.store
                  local.get 9
                  local.get 8
                  i32.store
                end
                local.get 3
                i32.const 1
                i32.add
                local.set 3
              end
              local.get 6
              local.get 0
              local.get 4
              i32.const 1
              i32.add
              local.tee 5
              i32.const 2
              i32.shl
              i32.add
              local.tee 8
              i32.load
              local.tee 9
              i32.gt_s
              if  ;; label = @6
                local.get 3
                local.get 5
                i32.ne
                if  ;; label = @7
                  local.get 8
                  local.get 0
                  local.get 3
                  i32.const 2
                  i32.shl
                  i32.add
                  local.tee 5
                  i32.load
                  i32.store
                  local.get 5
                  local.get 9
                  i32.store
                end
                local.get 3
                i32.const 1
                i32.add
                local.set 3
              end
              local.get 4
              i32.const 2
              i32.add
              local.tee 4
              local.get 2
              i32.ne
              br_if 0 (;@5;)
            end
          end
          local.get 7
          i32.load
          local.set 6
        end
        local.get 7
        local.get 0
        local.get 3
        i32.const 2
        i32.shl
        i32.add
        local.tee 4
        i32.load
        i32.store
        local.get 4
        local.get 6
        i32.store
        local.get 0
        local.get 1
        local.get 3
        i32.const 1
        i32.sub
        call 2
        local.get 3
        i32.const 1
        i32.add
        local.tee 1
        local.get 2
        i32.lt_s
        br_if 0 (;@2;)
      end
    end)
  (func (;3;) (type 1)
    (local i32)
    loop  ;; label = @1
      i32.const 5024
      i32.const 1024
      i32.const 4000
      call 0
      i32.const 5024
      i32.const 0
      i32.const 999
      call 2
      local.get 0
      i32.const 1
      i32.add
      local.tee 0
      i32.const 100000
      i32.ne
      br_if 0 (;@1;)
    end)
  (func (;4;) (type 2) (result i32)
    global.get 0)
  (func (;5;) (type 3) (param i32)
    local.get 0
    global.set 0)
  (func (;6;) (type 4) (param i32) (result i32)
    global.get 0
    local.get 0
    i32.sub
    i32.const -16
    i32.and
    local.tee 0
    global.set 0
    local.get 0)
  (func (;7;) (type 2) (result i32)
    i32.const 9024)
  (table (;0;) 1 1 funcref)
  (memory (;0;) 256 256)
  (global (;0;) (mut i32) (i32.const 5251920))
  (export "memory" (memory 0))
  (export "__wasm_call_ctors" (func 1))
  (export "mainloop" (func 3))
  (export "__indirect_function_table" (table 0))
  (export "__errno_location" (func 7))
  (export "stackSave" (func 4))
  (export "stackRestore" (func 5))
  (export "stackAlloc" (func 6))
  (data (;0;) (i32.const 1024) "8\11\00\00\19\1a\00\00<\01\00\00\e1\01\00\00z\12\00\00\05\14\00\00g\19\00\00\cc\0e\00\003\0c\00\00\a1!\00\00\a6\08\00\00\c3\11\00\00\a7\1b\00\00E$\00\00v\0e\00\00W\1c\00\00/\0d\00\00\b1\19\00\00d&\00\00T\07\00\00\f3\0d\00\00\0f\1f\00\00\ed\03\00\00y\0a\00\00\ed\14\00\00\5c\0c\00\00\8e\1e\00\00\b2\0b\00\00\95\18\00\00\e6\0d\00\00%\07\00\00l\0d\00\004\0b\00\00=\05\00\00v\11\00\00'\10\00\00&\0d\00\00j\00\00\00<\10\00\00\e0\0f\00\00\e0\22\00\00s\17\00\00\15\1d\00\00\8b\00\00\00\8e\13\00\00\13\0f\00\00\a3\1c\00\00(\14\00\00\9a\12\00\00\87$\00\00\d4\1e\00\00\b0\0e\00\00\f1%\00\00\a8!\00\00\b3\02\00\00)\11\00\00\c7\0a\00\00\07\14\00\00\96\15\00\00\14 \00\00Z\12\00\00c\07\00\00W!\00\007\02\00\00z#\00\00\bb\16\00\00e\01\00\00T\1d\00\00\f0\0a\00\00~\08\00\00\f1\12\00\001\15\00\00\f7\00\00\00k\14\00\00\c4\11\00\00r\06\00\00\1d\1a\00\00B\22\00\00\81\1a\00\00\9e\1e\00\00O\15\00\00\a6\1f\00\00\f4 \00\00\91\00\00\00C$\00\00\cd\06\00\00j\15\00\00\95&\00\00\c8\0a\00\00\9b\18\00\00\a1$\00\00A\17\00\00\ae\09\00\00\b4\01\00\00!\0b\00\00\1f\12\00\00\8e\06\00\00\08\1b\00\00L\1a\00\00\8c!\00\00\be\10\00\00\b9\14\00\00\12\13\00\00L \00\00\b9\1f\00\00\22\06\00\00\17\07\00\00&\16\00\006\0b\00\00\98\12\00\00\a6\14\00\00x\0b\00\00l\22\00\00u\04\00\00\0b\1c\00\00t\0e\00\00j\18\00\00\f3\09\00\00\d3\03\00\00,\18\00\00\f7\0c\00\00\d4\16\00\00\0d$\00\00\f5\12\00\00\b5\1f\00\00@\0f\00\008\18\00\00\f6\0b\00\00\b9\16\00\00\b1\19\00\00p\04\00\00\d9\12\00\009\07\00\00\94&\00\00\ac\1e\00\00\01\0c\00\00\1d&\00\00\0d\0f\00\00\81\10\00\00I\22\00\00\e1\1d\00\00X\08\00\00f\22\00\00H \00\00\07\0b\00\00T\0b\00\00\95\1d\00\00B \00\00i \00\001\09\00\00\ac&\00\00F\0f\00\00\f4\00\00\00B\0f\00\00H\1c\00\00\b4\10\00\00\1b\18\00\004\05\00\00\e9\11\00\00\c6\10\00\00\b3\06\00\00z\17\00\00\f2!\00\00\c7%\00\00\15\15\00\00q\0b\00\00\a4\1a\00\00\1a\12\00\00\0b\17\00\00c\1a\00\00\1e\13\00\00\91\0a\00\00#\0b\00\00\b6\10\00\00\fb\14\00\00K\0b\00\00\ca\15\00\00Y\09\00\00\22 \00\00\f4\09\00\00\8e \00\00\a9\1d\00\00\da\06\00\00\07\04\00\00l\0a\00\00\db\1c\00\00G\0b\00\00\1f\11\00\00\7f\09\00\00C\1f\00\00\bb%\00\000\18\00\00\db\05\00\00\fb%\00\00\96\0b\00\00\f8\0a\00\00\c1\07\00\00]\0c\00\00`\13\00\00\ef\22\00\00 !\00\00n\0a\00\00d\11\00\00\f1\01\00\00\d3\05\00\00\81!\00\00\d8\1f\00\00\fe#\00\00\07#\00\00\c4\09\00\00~\0b\00\00\8d#\00\00\c6$\00\00\88!\00\00V\12\00\00\f2\0b\00\00\ec\0f\00\00\0e\00\00\00\1b\16\00\00h\11\00\00\89\14\00\00\85\15\00\00\ab\10\00\00$\1d\00\00\87\0f\00\00\cd\1c\00\00\b8\1b\00\00\92\1d\00\00\ea\1a\00\00e\18\00\00\cf\0e\00\00\22\1a\00\00-\03\00\00\1d\0f\00\00I\22\00\00\a1\0b\00\00\b0\0d\00\00\14\12\00\00\da\00\00\00[\00\00\00+#\00\00\1c\15\00\00\c7\0a\00\00\19\03\00\00\15\11\00\00\ae\06\00\00{\15\00\002\0d\00\002\0e\00\00d\13\00\00\9f\1c\00\00\fb\12\00\00\b8\19\00\009\03\00\00\13\1c\00\00\ec\10\00\00\b2\02\00\00\c4\1a\00\00P\16\00\00b\0b\00\00\81\0e\00\00=\04\00\004\00\00\00G\22\00\00 $\00\00\98\0a\00\00\03\1e\00\00\d9\0e\00\00\fe\19\00\00\d1\1f\00\00\b8 \00\00\b6\05\00\00\1c\07\00\00\ac\0a\00\00\fe \00\00`\10\00\00\cd \00\00i$\00\00\04\19\00\00\1c#\00\00\ec\00\00\00\fe\10\00\00Y\1a\00\00\b6\10\00\00W\0b\00\00\fe\0c\00\00\e6\08\00\00\a3!\00\00\a9\17\00\00\9c\0c\00\00D\14\00\00\a2 \00\00%\22\00\00*\04\00\00V&\00\00a\1c\00\00h\1d\00\00\0c\0e\00\00\a0\1e\00\00\95$\00\00\db\07\00\00/\10\00\00\82\1d\00\00\dd\0d\00\00\d6\1c\00\00\08\19\00\00\e3\1f\00\00\d8\03\00\00\92\11\00\00\dd\1c\00\003\05\00\00\f4\04\00\00##\00\00B\14\00\00S\09\00\00\88#\00\00\0f\04\00\00t\0e\00\00\c2#\00\00\ed!\00\00\90\0e\00\00\07\18\00\00\de\11\00\00\91\13\00\00d\13\00\00m$\00\00\ad\0a\00\00v\18\00\00s\1b\00\00.\04\00\00\e4\03\00\00\fd\12\00\00\99\0d\00\00S\02\00\00\df\03\00\00\00\19\00\00\f9\10\00\00>\14\00\00\5c\0e\00\00\f8\06\00\00N\1a\00\00\b9\1d\00\00=\11\00\00/\04\00\00g\07\00\00\03!\00\00u \00\00=\02\00\00\eb#\00\00\83\13\00\00\d1\13\00\00\f0\04\00\00D\14\00\00\11\0d\00\00\89\14\00\00\ac\04\00\00Q\03\00\00\dd\01\00\00\e3\05\00\00:\13\00\00t\0e\00\00#\14\00\00\12\05\00\00\de\13\00\00z\17\00\00(\0b\00\00\1a\16\00\00.#\00\00\ee#\00\00a%\00\00C\0c\00\00j\0a\00\00\cb\0b\00\007\1d\00\00\f0&\00\005\1e\00\00\7f!\00\00\db\1f\00\009\16\00\00\ae\1c\00\00\d9\1f\00\00\14\12\00\00\a3\11\00\00\fa\14\00\00\1f\01\00\00\09 \00\00\1e\19\00\00_\0a\00\00\bb$\00\00[\13\00\008#\00\00\ae%\00\00g\07\00\00\e8\0f\00\00\d1!\00\00=\0d\00\00\db\03\00\00d\05\00\00\aa\00\00\00\af%\00\00Z\1c\00\00\0f&\00\00i\15\00\00\a1\19\00\00>\07\00\00\d9\1a\00\00@\14\00\00i\1b\00\00\a0\1e\00\00\14\05\00\00>\12\00\00\b2\0d\00\00\f2\0e\00\00\bc%\00\00Q\1f\00\00'!\00\00\09\0f\00\00\86\1e\00\00\95\12\00\00\e1 \00\00\f8\03\00\00\cb\0c\00\00\14\08\00\00^&\00\00`\08\00\00o\12\00\00\15\11\00\00\fc#\00\00\fb\05\00\00\0e\06\00\00\b0%\00\00z\0f\00\00\16\0b\00\00\0a\0f\00\00h%\00\00\ed\11\00\00\87\09\00\004\1e\00\00\9e%\00\00:\13\00\000!\00\00A\14\00\00A\11\00\00\b4\02\00\00c\22\00\00\b7\1e\00\00\f0\05\00\00l\07\00\00\c1\1f\00\00\ec\1f\00\00)\05\00\00H\17\00\00!\17\00\00\11\17\00\00g\1a\00\00\e4&\00\00\c3\01\00\00\b8\02\00\00\15\1e\00\00?\0a\00\00\08\19\00\00\7f\0e\00\00\a3\0a\00\00z\19\00\00\ca\17\00\00&\01\00\00U\13\00\00V\02\00\00\87\10\00\00\9f\1f\00\00\af\0f\00\00\bb\1b\00\00w\0c\00\00\8b\13\00\00\17\16\00\00\e2\0e\00\00\eb\0a\00\00k\22\00\00\7f\0f\00\009!\00\00\83\06\00\00\ea\07\00\00\dc\1c\00\00\b3\10\00\00\f7\10\00\00\ea\0d\00\00M\00\00\00;%\00\00G\13\00\00\ae\08\00\00\a5\0c\00\00\c7\0f\00\00>\1f\00\00e\0f\00\00U%\00\00\c8\13\00\00a\08\00\00\1f\15\00\00%\01\00\00)\1b\00\00;\08\00\00\a0\0b\00\00^\0a\00\00\cf\1a\00\00\dd\00\00\00\f7\1d\00\00;\1b\00\00g\18\00\00\94\19\00\00B\0d\00\00/\1d\00\00\91\0e\00\00\d8\1d\00\00\8c\22\00\00\b0\1f\00\00\d4\0e\00\00\9f\0e\00\00\eb\00\00\00\f7\17\00\00\b8#\00\00\a7\0b\00\004&\00\00d\19\00\00\b8\04\00\002\0b\00\00f\13\00\00R\10\00\00=\06\00\00_\11\00\00O\1e\00\00\a3!\00\00#\02\00\00\ee\1e\00\00~\16\00\00Y#\00\00\a1\17\00\00.\22\00\00j\22\00\00S\1c\00\00F\1f\00\00\a1\14\00\00\a5\18\00\00\5c\10\00\00\dc\1e\00\00h\16\00\00R\04\00\00\bc\0a\00\00\81\10\00\00q\04\00\00\92\22\00\00\9b\17\00\00\08\0f\00\00\07\11\00\00\87\04\00\00\88\1d\00\002\01\00\00%#\00\00O\14\00\00d\02\00\00`\08\00\00\ca\14\00\00\a1\09\00\00\17\0a\00\00\b1\1e\00\00V\19\00\00\12\10\00\00\d7&\00\00m\07\00\00\fb\04\00\00\e8\01\00\00h\1c\00\00\b2%\00\00\d6\16\00\00*\0d\00\00\8f\0d\00\00\d7!\00\00\88\07\00\00\02\15\00\00\93\18\00\00\83\18\00\00\83\22\00\00\d8\10\00\00\fe\01\00\00P\16\00\00\e6\06\00\00\cb\10\00\00~\10\00\00=\09\00\00\ca\05\00\00i\1a\00\00%\1d\00\00k\18\00\00\7f\18\00\00\19\1b\00\00\ad\0e\00\00t\14\00\00\f7\18\00\00\0d\1b\00\00\ec\02\00\00g\1d\00\00\8a\0e\00\00\80\0d\00\00\81\22\00\00\5c\01\00\00 \04\00\00Y\12\00\009\03\00\00\ff\22\00\00S\14\00\00\fa\19\00\00\fa\02\00\00\ee\05\00\00\cf%\00\00\c6\14\00\00\94\17\00\00\ff\10\00\00\c1\07\00\00\04\1f\00\00\f3 \00\00\10\15\00\00\19\22\00\00\09\04\00\00\a8\03\00\00]\10\00\00i\02\00\00\ad\11\00\00K\0d\00\00\ba\1e\00\00\80\1c\00\00\f0\02\00\00\cc\09\00\00\7f\0a\00\00\af%\00\00=\0a\00\00G\02\00\00\db\16\00\00X\1c\00\00\d8\05\00\00$ \00\00^\11\00\004\1b\00\00{\06\00\00{\15\00\00B\1d\00\00q\15\00\00\e5\05\00\00\15\1b\00\00\d6\16\00\00&\10\00\00\aa\0b\00\00\95#\00\00(\0d\00\00\db\19\00\00\ac\0d\00\00}\09\00\00\ff\22\00\00\a7\0d\00\00'\0a\00\00\c9\0d\00\00\cb\0c\00\00\f8\1f\00\00\ac\19\00\001\03\00\00]\13\00\00I\1a\00\00\8c\06\00\00S\18\00\00\a5\1a\00\00g\18\00\00^\0d\00\00@\17\00\00\ad&\00\007\18\00\00\1d\1b\00\00\80\08\00\00\89\0e\00\00\c0\04\00\00(\0e\00\00\a9\16\00\00\f2\03\00\00Y\15\00\00\ad\0e\00\00\d3\1d\00\006#\00\00\be\22\00\00\f6 \00\00\86\16\00\00\d3\05\00\00\c1\02\00\00\e3\0c\00\00h$\00\00\5c\1b\00\008\1a\00\00?\0a\00\00\fc\12\00\00\10\0e\00\00\c0\04\00\00\b7\09\00\00b\1d\00\00\06&\00\00\1e\09\00\00{\00\00\00\bd \00\00\d1#\00\00*\1b\00\00\dd#\00\00\08\08\00\00\7f\07\00\00\e9\0e\00\00&$\00\00J\1a\00\00\08\10\00\00n\1a\00\00B\1c\00\00A$\00\00\f6%\00\00\c2\05\00\008\13\00\00\7f\04\00\00\ce\09\00\00\81&\00\00c\07\00\00\84\12\00\00-\19\00\00\bc\06\00\00\eb\1b\00\00\f3\06\00\00>%\00\00\14\12\00\000\09\00\00t\1f\00\00\d0\0d\00\00\c3\02\00\00\e0\1e\00\00\ad\0c\00\00H\0c\00\00u\22\00\00\d3\17\00\00\83\10\00\00@\1c\00\00\19 \00\00*\18\00\00\85\0b\00\00\f6\1c\00\00\f8\1b\00\00\cd\0f\00\00\f5\0c\00\00\ed\22\00\00\8c\0a\00\00{!\00\00\93\1e\00\00\85\10\00\00\0d\16\00\00\17\18\00\001\1d\00\00\11&\00\00{\05\00\00\d5\01\00\00\e7\11\00\00m#\00\00\90\04\00\00\03\22\00\00\98\1c\00\00x\17\00\00\11\10\00\000\15\00\00\cf\1a\00\00X\0b\00\00\d2\04\00\00\a2\09\00\00\e3\06\00\00~\10\00\00\ea\18\00\00\c6\1d\00\00l\14\00\00\82\12\00\00.\03\00\005\00\00\00\ec\19\00\00:\00\00\00c\05\00\00\15\1d\00\00q\14\00\00!\12\00\00\cd\03\00\00X\0b\00\00\9b\0b\00\00Q#\00\00\b5!\00\00\cf\1b\00\00i\00\00\00\11\15\00\00\0d\18\00\00\1f\14\00\00\f4\1b\00\00C\03\00\00\f8\07\00\00\86 \00\00\8c\0d\00\00\aa\0f\00\00\f8\14\00\00B\12\00\00\bc%\00\00\cd!\00\00\a8\0b\00\00\a0\08\00\00g\15\00\00\9d\00\00\00\19\16\00\00\98\09\00\00S\0b\00\00\f6&\00\00\95\0b\00\00L\1c\00\00W\12\00\00\ec\02\00\00\0a\10\00\00\ec\11\00\00\c1\1f\00\00\0b\0b\00\00\1f\0a\00\00\af\01\00\00]\0a\00\00>\0f\00\00\88\11\00\00\dd\17\00\00\94$\00\00P\0b\00\00e\14\00\00V#\00\00\88&\00\00\c0\17\00\00\12!\00\00\11\12\00\00%\22\00\00\06#\00\00\82\06\00\00\b5\04\00\00\02\15\00\00o\0b\00\000\07\00\00\fd\22\00\00\94\1d\00\00\14\09\00\00\1c\04\00\006\1b\00\00;\22\00\00M\08\00\00\85\0d\00\00\da\16\00\00\d9\0c\00\00\a6&\00\00\92\11\00\003\1d\00\00\22\15\00\00\d2\1f\00\00\87\08\00\00\96\07\00\00L\0e\00\00\10\1f\00\00Z\04\00\00:#\00\00\d0\0f\00\00\aa\1a\00\00\92\0b\00\00\c4\14\00\00^ \00\00\f2\11\00\00\f5\16\00\00\e9\0b\00\00W\16\00\00\db!\00\00\d5\08\00\00\08\0a\00\00@!\00\00M$\00\00\8b\00\00\00Y\0d\00\00\97\07\00\00\c4$\00\00\ac\0e\00\00\95!\00\00E\1b\00\00Z$\00\00\cd\06\00\00\90%\00\00 \15\00\00\d5\1c\00\00\c4!\00\00-\14\00\00q\0f\00\00\06\1d\00\00\00\1a\00\00\10!\00\00\98\00\00\00\97\1b\00\00\1d\06\00\00Q\0f\00\00\f0\04\00\00\d1\08\00\007\18\00\00\0d#\00\00\bb\0d\00\00V\06\00\00`\12\00\00]\12\00\00A%\00\00\06$\00\00\9a\0d\00\00u\10\00\00`\06\00\002#\00\00c\09\00\00,\0f\00\00F$\00\00K\16\00\00\b7\0c\00\00\dd\1d\00\00g\16\00\00\b5\08\00\00\01\07\00\00\e6$\00\00\ed\03\00\00\fd\18\00\00\f9\03\00\00?\13\00\00\85\10\00\00\aa\14\00\00\9e\0f\00\00\16\01\00\00\a9\0f\00\00\bb\0a\00\00\86\10\00\00\c4\16\00\00#$\00\00/$\00\00\05\16\00\00\b7\03\00\00\f3\01\00\00\d3&\00\00= \00\00\85\09\00\00\bb\01\00\00!#\00\00\d3\05\00\00r\01\00\00H\08\00\00d\19\00\00\b3 \00\00\e9\04\00\00\f2 \00\00\e4$\00\00\d9$\00\00\02\1a\00\00\93\0b\00\00\9a\02\00\007\17\00\00(\15\00\00\c3\1e\00\00\13\1c\00\00\f9\0d\00\00\9d\18\00\00\05 \00\00\a6\10\00\00q&\00\00*\06\00\00\c7\12\00\00y\0c\00\00\99\19\00\00\e7\18\00\00\c9#\00\00>\0d\00\00;\14\00\00\1f\0a\00\00&\1e\00\00\ea\22\00\00\c4\15\00\00\cd\22\00\00\8b\1f\00\00`\11\00\00'\18\00\00\e3\15\00\00[\0c\00\00_\1e\00\00\d9\19\00\00Q\02\00\00\d6\0a\00\00-\19\00\00F!\00\00\86\14\00\00(\10\00\00\9d\14\00\007\0a\00\00\aa\01\00\00/\17\00\00H\01\00\00r\19\00\00\9f\0d\00\00\b4\0d\00\00\da\07"))
