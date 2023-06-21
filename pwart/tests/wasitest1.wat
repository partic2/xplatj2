(module
  (type $t0 (func (param i32) (result i32)))
  (type $t1 (func (param i32 i32 i32) (result i32)))
  (type $t2 (func (param i32 i32) (result i32)))
  (type $t3 (func (param i32)))
  (type $t4 (func (param i32 i32 i32)))
  (type $t5 (func (param i32 i32 i32 i32) (result i32)))
  (type $t6 (func))
  (type $t7 (func (param i32 i64 i32) (result i64)))
  (type $t8 (func (result i32)))
  (type $t9 (func (param i32 i32 i32 i32 i32)))
  (import "wasi_snapshot_preview1" "args_sizes_get" (func $wasi_snapshot_preview1.args_sizes_get (type $t2)))
  (import "wasi_snapshot_preview1" "args_get" (func $wasi_snapshot_preview1.args_get (type $t2)))
  (import "wasi_snapshot_preview1" "proc_exit" (func $wasi_snapshot_preview1.proc_exit (type $t3)))
  (import "wasi_snapshot_preview1" "fd_write" (func $wasi_snapshot_preview1.fd_write (type $t5)))
  (import "pwart_builtin" "version" (func $getver (result i32)))
  (;0;)
  (func $f4 (type $t6)
    i32.const 2904
    i32.const 2792
    i32.store
    i32.const 2832
    i32.const 42
    i32.store)
	(;1;)
  (func $__main_argc_argv (type $t2) (param $p0 i32) (param $p1 i32) (result i32)
    (local $l2 i32) (local $l3 i32) (local $l4 i32) (local $l5 i32) (local $l6 i32)
    global.get $g0
    i32.const 16
    i32.sub
    local.tee $p0
    global.set $g0
    local.get $p0
    i32.const 1034
    i32.store
    global.get $g0
    i32.const 16
    i32.sub
    local.tee $p1
    global.set $g0
    local.get $p1
    local.get $p0
    i32.store offset=12
    global.get $g0
    i32.const 208
    i32.sub
    local.tee $l2
    global.set $g0
    local.get $l2
    local.get $p0
    i32.store offset=204
    local.get $l2
    i32.const 160
    i32.add
    i32.const 0
    i32.const 40
    call $f10
    local.get $l2
    local.get $l2
    i32.load offset=204
    i32.store offset=200
    block $B0
      i32.const 0
      local.get $l2
      i32.const 200
      i32.add
      local.get $l2
      i32.const 80
      i32.add
      local.get $l2
      i32.const 160
      i32.add
      call $f15
      i32.const 0
      i32.lt_s
      br_if $B0
      i32.const 1628
      i32.load
      i32.const 0
      i32.ge_s
      local.set $l5
      i32.const 1552
      i32.load
      local.set $l3
      i32.const 1624
      i32.load
      i32.const 0
      i32.le_s
      if $I1
        i32.const 1552
        local.get $l3
        i32.const -33
        i32.and
        i32.store
      end
      block $B2 (result i32)
        block $B3
          block $B4
            i32.const 1600
            i32.load
            i32.eqz
            if $I5
              i32.const 1600
              i32.const 80
              i32.store
              i32.const 1580
              i32.const 0
              i32.store
              i32.const 1568
              i64.const 0
              i64.store
              i32.const 1596
              i32.load
              local.set $l4
              i32.const 1596
              local.get $l2
              i32.store
              br $B4
            end
            i32.const 1568
            i32.load
            br_if $B3
          end
          i32.const -1
          i32.const 1552
          call $f13
          br_if $B2
          drop
        end
        i32.const 1552
        local.get $l2
        i32.const 200
        i32.add
        local.get $l2
        i32.const 80
        i32.add
        local.get $l2
        i32.const 160
        i32.add
        call $f15
      end
      local.set $l6
      local.get $l3
      i32.const 32
      i32.and
      local.set $l3
      local.get $l4
      if $I6 (result i32)
        i32.const 1552
        i32.const 0
        i32.const 0
        i32.const 1588
        i32.load
        call_indirect $__indirect_function_table (type $t1)
        drop
        i32.const 1600
        i32.const 0
        i32.store
        i32.const 1596
        local.get $l4
        i32.store
        i32.const 1580
        i32.const 0
        i32.store
        i32.const 1572
        i32.load
        drop
        i32.const 1568
        i64.const 0
        i64.store
        i32.const 0
      else
        local.get $l6
      end
      drop
      i32.const 1552
      i32.const 1552
      i32.load
      local.get $l3
      i32.or
      i32.store
      local.get $l5
      i32.eqz
      br_if $B0
    end
    local.get $l2
    i32.const 208
    i32.add
    global.set $g0
    local.get $p1
    i32.const 16
    i32.add
    global.set $g0
    local.get $p0
    i32.const 16
    i32.add
    global.set $g0
    i32.const 0)
	(;2;)
  (func $_start (type $t6)
    (local $l0 i32) (local $l1 i32) (local $l2 i32) (local $l3 i32)
    call $f4
    block $B0 (result i32)
      global.get $g0
      i32.const 16
      i32.sub
      local.tee $l0
      global.set $g0
      block $B1
        local.get $l0
        local.tee $l1
        i32.const 12
        i32.add
        local.get $l0
        i32.const 8
        i32.add
        call $wasi_snapshot_preview1.args_sizes_get
        i32.eqz
        if $I2
          local.get $l0
          local.get $l1
          i32.load offset=12
          i32.const 2
          i32.shl
          local.tee $l3
          i32.const 19
          i32.add
          i32.const -16
          i32.and
          i32.sub
          local.tee $l0
          local.tee $l2
          global.set $g0
          local.get $l2
          local.get $l1
          i32.load offset=8
          i32.const 15
          i32.add
          i32.const -16
          i32.and
          i32.sub
          local.tee $l2
          global.set $g0
          local.get $l0
          local.get $l3
          i32.add
          i32.const 0
          i32.store
          local.get $l0
          local.get $l2
          call $wasi_snapshot_preview1.args_get
          br_if $B1
          local.get $l1
          i32.load offset=12
          local.get $l0
          call $__main_argc_argv
          local.set $l0
          local.get $l1
          i32.const 16
          i32.add
          global.set $g0
          local.get $l0
          br $B0
        end
        i32.const 71
        call $wasi_snapshot_preview1.proc_exit
        unreachable
      end
      i32.const 71
      call $wasi_snapshot_preview1.proc_exit
      unreachable
    end
    local.set $l1
    i32.const 2752
    i32.load
    local.tee $l0
    if $I3
      loop $L4
        local.get $l0
        call $f12
        local.get $l0
        i32.load offset=56
        local.tee $l0
        br_if $L4
      end
    end
    i32.const 2756
    i32.load
    call $f12
    i32.const 1696
    i32.load
    call $f12
    i32.const 2756
    i32.load
    call $f12
    local.get $l1
    call $wasi_snapshot_preview1.proc_exit
    unreachable)
	(;3;)
  (func $f7 (type $t1) (param $p0 i32) (param $p1 i32) (param $p2 i32) (result i32)
    (local $l3 i32) (local $l4 i32) (local $l5 i32) (local $l6 i32) (local $l7 i32) (local $l8 i32) (local $l9 i32)
	call $getver
	drop
    global.get $g0
    i32.const 32
    i32.sub
    local.tee $l3
    global.set $g0
    local.get $l3
    local.get $p0
    i32.load offset=28
    local.tee $l4
    i32.store offset=16
    local.get $p0
    i32.load offset=20
    local.set $l5
    local.get $l3
    local.get $p2
    i32.store offset=28
    local.get $l3
    local.get $p1
    i32.store offset=24
    local.get $l3
    local.get $l5
    local.get $l4
    i32.sub
    local.tee $p1
    i32.store offset=20
    local.get $p1
    local.get $p2
    i32.add
    local.set $l6
    local.get $l3
    i32.const 16
    i32.add
    local.set $l4
    i32.const 2
    local.set $l7
    block $B0 (result i32)
      block $B1
        block $B2
          block $B3
            local.get $p0
            i32.load offset=60
            local.get $l3
            i32.const 16
            i32.add
            i32.const 2
            local.get $l3
            i32.const 12
            i32.add
            call $wasi_snapshot_preview1.fd_write
            call $f20
            if $I4
              local.get $l4
              local.set $l5
              br $B3
            end
            loop $L5
              local.get $l6
              local.get $l3
              i32.load offset=12
              local.tee $p1
              i32.eq
              br_if $B2
              local.get $p1
              i32.const 0
              i32.lt_s
              if $I6
                local.get $l4
                local.set $l5
                br $B1
              end
              local.get $l4
              local.get $p1
              local.get $l4
              i32.load offset=4
              local.tee $l8
              i32.gt_u
              local.tee $l9
              i32.const 3
              i32.shl
              i32.add
              local.tee $l5
              local.get $p1
              local.get $l8
              i32.const 0
              local.get $l9
              select
              i32.sub
              local.tee $l8
              local.get $l5
              i32.load
              i32.add
              i32.store
              local.get $l4
              i32.const 12
              i32.const 4
              local.get $l9
              select
              i32.add
              local.tee $l4
              local.get $l4
              i32.load
              local.get $l8
              i32.sub
              i32.store
              local.get $l6
              local.get $p1
              i32.sub
              local.set $l6
              local.get $p0
              i32.load offset=60
              local.get $l5
              local.tee $l4
              local.get $l7
              local.get $l9
              i32.sub
              local.tee $l7
              local.get $l3
              i32.const 12
              i32.add
              call $wasi_snapshot_preview1.fd_write
              call $f20
              i32.eqz
              br_if $L5
            end
          end
          local.get $l6
          i32.const -1
          i32.ne
          br_if $B1
        end
        local.get $p0
        local.get $p0
        i32.load offset=44
        local.tee $p1
        i32.store offset=28
        local.get $p0
        local.get $p1
        i32.store offset=20
        local.get $p0
        local.get $p1
        local.get $p0
        i32.load offset=48
        i32.add
        i32.store offset=16
        local.get $p2
        br $B0
      end
      local.get $p0
      i32.const 0
      i32.store offset=28
      local.get $p0
      i64.const 0
      i64.store offset=16
      local.get $p0
      local.get $p0
      i32.load
      i32.const 32
      i32.or
      i32.store
      i32.const 0
      local.get $l7
      i32.const 2
      i32.eq
      br_if $B0
      drop
      local.get $p2
      local.get $l5
      i32.load offset=4
      i32.sub
    end
    local.set $p1
    local.get $l3
    i32.const 32
    i32.add
    global.set $g0
    local.get $p1)
  (func $f8 (type $t0) (param $p0 i32) (result i32)
    i32.const 0)
  (func $f9 (type $t7) (param $p0 i32) (param $p1 i64) (param $p2 i32) (result i64)
    i64.const 0)
  (func $f10 (type $t4) (param $p0 i32) (param $p1 i32) (param $p2 i32)
    (local $l3 i32) (local $l4 i32) (local $l5 i64)
    block $B0
      local.get $p2
      i32.eqz
      br_if $B0
      local.get $p0
      local.get $p1
      i32.store8
      local.get $p0
      local.get $p2
      i32.add
      local.tee $l3
      i32.const 1
      i32.sub
      local.get $p1
      i32.store8
      local.get $p2
      i32.const 3
      i32.lt_u
      br_if $B0
      local.get $p0
      local.get $p1
      i32.store8 offset=2
      local.get $p0
      local.get $p1
      i32.store8 offset=1
      local.get $l3
      i32.const 3
      i32.sub
      local.get $p1
      i32.store8
      local.get $l3
      i32.const 2
      i32.sub
      local.get $p1
      i32.store8
      local.get $p2
      i32.const 7
      i32.lt_u
      br_if $B0
      local.get $p0
      local.get $p1
      i32.store8 offset=3
      local.get $l3
      i32.const 4
      i32.sub
      local.get $p1
      i32.store8
      local.get $p2
      i32.const 9
      i32.lt_u
      br_if $B0
      local.get $p0
      i32.const 0
      local.get $p0
      i32.sub
      i32.const 3
      i32.and
      local.tee $l4
      i32.add
      local.tee $l3
      local.get $p1
      i32.const 255
      i32.and
      i32.const 16843009
      i32.mul
      local.tee $p1
      i32.store
      local.get $l3
      local.get $p2
      local.get $l4
      i32.sub
      i32.const -4
      i32.and
      local.tee $l4
      i32.add
      local.tee $p2
      i32.const 4
      i32.sub
      local.get $p1
      i32.store
      local.get $l4
      i32.const 9
      i32.lt_u
      br_if $B0
      local.get $l3
      local.get $p1
      i32.store offset=8
      local.get $l3
      local.get $p1
      i32.store offset=4
      local.get $p2
      i32.const 8
      i32.sub
      local.get $p1
      i32.store
      local.get $p2
      i32.const 12
      i32.sub
      local.get $p1
      i32.store
      local.get $l4
      i32.const 25
      i32.lt_u
      br_if $B0
      local.get $l3
      local.get $p1
      i32.store offset=24
      local.get $l3
      local.get $p1
      i32.store offset=20
      local.get $l3
      local.get $p1
      i32.store offset=16
      local.get $l3
      local.get $p1
      i32.store offset=12
      local.get $p2
      i32.const 16
      i32.sub
      local.get $p1
      i32.store
      local.get $p2
      i32.const 20
      i32.sub
      local.get $p1
      i32.store
      local.get $p2
      i32.const 24
      i32.sub
      local.get $p1
      i32.store
      local.get $p2
      i32.const 28
      i32.sub
      local.get $p1
      i32.store
      local.get $l4
      local.get $l3
      i32.const 4
      i32.and
      i32.const 24
      i32.or
      local.tee $l4
      i32.sub
      local.tee $p2
      i32.const 32
      i32.lt_u
      br_if $B0
      local.get $p1
      i64.extend_i32_u
      i64.const 4294967297
      i64.mul
      local.set $l5
      local.get $l3
      local.get $l4
      i32.add
      local.set $p1
      loop $L1
        local.get $p1
        local.get $l5
        i64.store offset=24
        local.get $p1
        local.get $l5
        i64.store offset=16
        local.get $p1
        local.get $l5
        i64.store offset=8
        local.get $p1
        local.get $l5
        i64.store
        local.get $p1
        i32.const 32
        i32.add
        local.set $p1
        local.get $p2
        i32.const 32
        i32.sub
        local.tee $p2
        i32.const 31
        i32.gt_u
        br_if $L1
      end
    end)
  (func $__errno_location (type $t8) (result i32)
    i32.const 2744)
  (func $f12 (type $t3) (param $p0 i32)
    (local $l1 i32) (local $l2 i32)
    block $B0
      local.get $p0
      i32.eqz
      br_if $B0
      local.get $p0
      i32.load offset=76
      drop
      local.get $p0
      i32.load offset=20
      local.get $p0
      i32.load offset=28
      i32.ne
      if $I1
        local.get $p0
        i32.const 0
        i32.const 0
        local.get $p0
        i32.load offset=36
        call_indirect $__indirect_function_table (type $t1)
        drop
      end
      local.get $p0
      i32.load offset=4
      local.tee $l1
      local.get $p0
      i32.load offset=8
      local.tee $l2
      i32.eq
      br_if $B0
      local.get $p0
      local.get $l1
      local.get $l2
      i32.sub
      i64.extend_i32_s
      i32.const 1
      local.get $p0
      i32.load offset=40
      call_indirect $__indirect_function_table (type $t7)
      drop
    end)
  (func $f13 (type $t0) (param $p0 i32) (result i32)
    (local $l1 i32)
    local.get $p0
    local.get $p0
    i32.load offset=72
    local.tee $l1
    i32.const 1
    i32.sub
    local.get $l1
    i32.or
    i32.store offset=72
    local.get $p0
    i32.load
    local.tee $l1
    i32.const 8
    i32.and
    if $I0
      local.get $p0
      local.get $l1
      i32.const 32
      i32.or
      i32.store
      i32.const -1
      return
    end
    local.get $p0
    i64.const 0
    i64.store offset=4 align=4
    local.get $p0
    local.get $p0
    i32.load offset=44
    local.tee $l1
    i32.store offset=28
    local.get $p0
    local.get $l1
    i32.store offset=20
    local.get $p0
    local.get $l1
    local.get $p0
    i32.load offset=48
    i32.add
    i32.store offset=16
    i32.const 0)
  (func $f14 (type $t0) (param $p0 i32) (result i32)
    local.get $p0
    i32.const 48
    i32.sub
    i32.const 10
    i32.lt_u)
  (func $f15 (type $t5) (param $p0 i32) (param $p1 i32) (param $p2 i32) (param $p3 i32) (result i32)
    (local $l4 i32) (local $l5 i32) (local $l6 i32) (local $l7 i32) (local $l8 i32) (local $l9 i32) (local $l10 i32) (local $l11 i32) (local $l12 i32) (local $l13 i32) (local $l14 i32) (local $l15 i32) (local $l16 i32) (local $l17 i32) (local $l18 i32) (local $l19 i32) (local $l20 i32) (local $l21 i32) (local $l22 i32) (local $l23 i64) (local $l24 i64)
    i32.const 1048
    local.set $l11
    global.get $g0
    i32.const 80
    i32.sub
    local.tee $l6
    global.set $g0
    local.get $l6
    i32.const 1048
    i32.store offset=76
    local.get $l6
    i32.const 55
    i32.add
    local.set $l22
    local.get $l6
    i32.const 56
    i32.add
    local.set $l17
    block $B0
      block $B1
        block $B2
          block $B3
            loop $L4
              local.get $l11
              local.set $l9
              local.get $l4
              local.get $l13
              i32.const 2147483647
              i32.xor
              i32.gt_s
              br_if $B3
              local.get $l4
              local.get $l13
              i32.add
              local.set $l13
              block $B5
                block $B6
                  block $B7
                    local.get $l9
                    local.tee $l4
                    i32.load8_u
                    local.tee $l12
                    if $I8
                      loop $L9
                        block $B10
                          block $B11
                            local.get $l12
                            i32.const 255
                            i32.and
                            local.tee $l12
                            i32.eqz
                            if $I12
                              local.get $l4
                              local.set $l11
                              br $B11
                            end
                            local.get $l12
                            i32.const 37
                            i32.ne
                            br_if $B10
                            local.get $l4
                            local.set $l12
                            loop $L13
                              local.get $l12
                              i32.load8_u offset=1
                              i32.const 37
                              i32.ne
                              if $I14
                                local.get $l12
                                local.set $l11
                                br $B11
                              end
                              local.get $l4
                              i32.const 1
                              i32.add
                              local.set $l4
                              local.get $l12
                              i32.load8_u offset=2
                              local.set $l5
                              local.get $l12
                              i32.const 2
                              i32.add
                              local.tee $l11
                              local.set $l12
                              local.get $l5
                              i32.const 37
                              i32.eq
                              br_if $L13
                            end
                          end
                          local.get $l4
                          local.get $l9
                          i32.sub
                          local.tee $l4
                          local.get $l13
                          i32.const 2147483647
                          i32.xor
                          local.tee $l12
                          i32.gt_s
                          br_if $B3
                          local.get $p0
                          if $I15
                            local.get $p0
                            local.get $l9
                            local.get $l4
                            call $f16
                          end
                          local.get $l4
                          br_if $L4
                          local.get $l6
                          local.get $l11
                          i32.store offset=76
                          local.get $l11
                          i32.const 1
                          i32.add
                          local.set $l4
                          i32.const -1
                          local.set $l14
                          block $B16
                            local.get $l11
                            i32.load8_s offset=1
                            call $f14
                            i32.eqz
                            br_if $B16
                            local.get $l11
                            i32.load8_u offset=2
                            i32.const 36
                            i32.ne
                            br_if $B16
                            local.get $l11
                            i32.const 3
                            i32.add
                            local.set $l4
                            local.get $l11
                            i32.load8_s offset=1
                            i32.const 48
                            i32.sub
                            local.set $l14
                            i32.const 1
                            local.set $l19
                          end
                          local.get $l6
                          local.get $l4
                          i32.store offset=76
                          i32.const 0
                          local.set $l7
                          block $B17
                            local.get $l4
                            i32.load8_s
                            local.tee $l10
                            i32.const 32
                            i32.sub
                            local.tee $l11
                            i32.const 31
                            i32.gt_u
                            if $I18
                              local.get $l4
                              local.set $l5
                              br $B17
                            end
                            local.get $l4
                            local.set $l5
                            i32.const 1
                            local.get $l11
                            i32.shl
                            local.tee $l11
                            i32.const 75913
                            i32.and
                            i32.eqz
                            br_if $B17
                            loop $L19
                              local.get $l6
                              local.get $l4
                              i32.const 1
                              i32.add
                              local.tee $l5
                              i32.store offset=76
                              local.get $l7
                              local.get $l11
                              i32.or
                              local.set $l7
                              local.get $l4
                              i32.load8_s offset=1
                              local.tee $l10
                              i32.const 32
                              i32.sub
                              local.tee $l11
                              i32.const 32
                              i32.ge_u
                              br_if $B17
                              local.get $l5
                              local.set $l4
                              i32.const 1
                              local.get $l11
                              i32.shl
                              local.tee $l11
                              i32.const 75913
                              i32.and
                              br_if $L19
                            end
                          end
                          block $B20
                            local.get $l10
                            i32.const 42
                            i32.eq
                            if $I21
                              block $B22 (result i32)
                                block $B23
                                  local.get $l5
                                  i32.load8_s offset=1
                                  call $f14
                                  i32.eqz
                                  br_if $B23
                                  local.get $l5
                                  i32.load8_u offset=2
                                  i32.const 36
                                  i32.ne
                                  br_if $B23
                                  local.get $l5
                                  i32.load8_s offset=1
                                  i32.const 2
                                  i32.shl
                                  local.get $p3
                                  i32.add
                                  i32.const 192
                                  i32.sub
                                  i32.const 10
                                  i32.store
                                  local.get $l5
                                  i32.const 3
                                  i32.add
                                  local.set $l10
                                  i32.const 1
                                  local.set $l19
                                  local.get $l5
                                  i32.load8_s offset=1
                                  i32.const 3
                                  i32.shl
                                  local.get $p2
                                  i32.add
                                  i32.const 384
                                  i32.sub
                                  i32.load
                                  br $B22
                                end
                                local.get $l19
                                br_if $B7
                                local.get $l5
                                i32.const 1
                                i32.add
                                local.set $l10
                                local.get $p0
                                i32.eqz
                                if $I24
                                  local.get $l6
                                  local.get $l10
                                  i32.store offset=76
                                  i32.const 0
                                  local.set $l19
                                  i32.const 0
                                  local.set $l15
                                  br $B20
                                end
                                local.get $p1
                                local.get $p1
                                i32.load
                                local.tee $l4
                                i32.const 4
                                i32.add
                                i32.store
                                i32.const 0
                                local.set $l19
                                local.get $l4
                                i32.load
                              end
                              local.set $l15
                              local.get $l6
                              local.get $l10
                              i32.store offset=76
                              local.get $l15
                              i32.const 0
                              i32.ge_s
                              br_if $B20
                              i32.const 0
                              local.get $l15
                              i32.sub
                              local.set $l15
                              local.get $l7
                              i32.const 8192
                              i32.or
                              local.set $l7
                              br $B20
                            end
                            local.get $l6
                            i32.const 76
                            i32.add
                            call $f17
                            local.tee $l15
                            i32.const 0
                            i32.lt_s
                            br_if $B3
                            local.get $l6
                            i32.load offset=76
                            local.set $l10
                          end
                          i32.const 0
                          local.set $l4
                          i32.const -1
                          local.set $l8
                          block $B25 (result i32)
                            local.get $l10
                            i32.load8_u
                            i32.const 46
                            i32.ne
                            if $I26
                              local.get $l10
                              local.set $l11
                              i32.const 0
                              br $B25
                            end
                            local.get $l10
                            i32.load8_u offset=1
                            i32.const 42
                            i32.eq
                            if $I27
                              block $B28 (result i32)
                                block $B29
                                  local.get $l10
                                  i32.load8_s offset=2
                                  call $f14
                                  i32.eqz
                                  br_if $B29
                                  local.get $l10
                                  i32.load8_u offset=3
                                  i32.const 36
                                  i32.ne
                                  br_if $B29
                                  local.get $l10
                                  i32.load8_s offset=2
                                  i32.const 2
                                  i32.shl
                                  local.get $p3
                                  i32.add
                                  i32.const 192
                                  i32.sub
                                  i32.const 10
                                  i32.store
                                  local.get $l10
                                  i32.const 4
                                  i32.add
                                  local.set $l11
                                  local.get $l10
                                  i32.load8_s offset=2
                                  i32.const 3
                                  i32.shl
                                  local.get $p2
                                  i32.add
                                  i32.const 384
                                  i32.sub
                                  i32.load
                                  br $B28
                                end
                                local.get $l19
                                br_if $B7
                                local.get $l10
                                i32.const 2
                                i32.add
                                local.set $l11
                                i32.const 0
                                local.get $p0
                                i32.eqz
                                br_if $B28
                                drop
                                local.get $p1
                                local.get $p1
                                i32.load
                                local.tee $l5
                                i32.const 4
                                i32.add
                                i32.store
                                local.get $l5
                                i32.load
                              end
                              local.set $l8
                              local.get $l6
                              local.get $l11
                              i32.store offset=76
                              local.get $l8
                              i32.const -1
                              i32.xor
                              i32.const 31
                              i32.shr_u
                              br $B25
                            end
                            local.get $l6
                            local.get $l10
                            i32.const 1
                            i32.add
                            i32.store offset=76
                            local.get $l6
                            i32.const 76
                            i32.add
                            call $f17
                            local.set $l8
                            local.get $l6
                            i32.load offset=76
                            local.set $l11
                            i32.const 1
                          end
                          local.set $l20
                          loop $L30
                            local.get $l4
                            local.set $l5
                            i32.const 28
                            local.set $l16
                            local.get $l11
                            local.tee $l10
                            i32.load8_s
                            local.tee $l4
                            i32.const 123
                            i32.sub
                            i32.const -58
                            i32.lt_u
                            br_if $B2
                            local.get $l10
                            i32.const 1
                            i32.add
                            local.set $l11
                            local.get $l4
                            local.get $l5
                            i32.const 58
                            i32.mul
                            i32.add
                            i32.load8_u offset=1007
                            local.tee $l4
                            i32.const 1
                            i32.sub
                            i32.const 8
                            i32.lt_u
                            br_if $L30
                          end
                          local.get $l6
                          local.get $l11
                          i32.store offset=76
                          block $B31
                            block $B32
                              local.get $l4
                              i32.const 27
                              i32.ne
                              if $I33
                                local.get $l4
                                i32.eqz
                                br_if $B2
                                local.get $l14
                                i32.const 0
                                i32.ge_s
                                if $I34
                                  local.get $p3
                                  local.get $l14
                                  i32.const 2
                                  i32.shl
                                  i32.add
                                  local.get $l4
                                  i32.store
                                  local.get $l6
                                  local.get $p2
                                  local.get $l14
                                  i32.const 3
                                  i32.shl
                                  i32.add
                                  i64.load
                                  i64.store offset=64
                                  br $B32
                                end
                                local.get $p0
                                i32.eqz
                                br_if $B5
                                local.get $l6
                                i32.const -64
                                i32.sub
                                local.get $l4
                                local.get $p1
                                call $f18
                                br $B31
                              end
                              local.get $l14
                              i32.const 0
                              i32.ge_s
                              br_if $B2
                            end
                            i32.const 0
                            local.set $l4
                            local.get $p0
                            i32.eqz
                            br_if $L4
                          end
                          local.get $l7
                          i32.const -65537
                          i32.and
                          local.tee $l18
                          local.get $l7
                          local.get $l7
                          i32.const 8192
                          i32.and
                          select
                          local.set $l7
                          i32.const 0
                          local.set $l14
                          i32.const 1024
                          local.set $l21
                          local.get $l17
                          local.set $l16
                          block $B35
                            block $B36
                              block $B37
                                block $B38 (result i32)
                                  block $B39
                                    block $B40
                                      block $B41
                                        block $B42
                                          block $B43 (result i32)
                                            block $B44
                                              block $B45
                                                block $B46
                                                  block $B47
                                                    block $B48
                                                      block $B49
                                                        block $B50
                                                          local.get $l10
                                                          i32.load8_s
                                                          local.tee $l4
                                                          i32.const -33
                                                          i32.and
                                                          local.get $l4
                                                          local.get $l4
                                                          i32.const 15
                                                          i32.and
                                                          i32.const 3
                                                          i32.eq
                                                          select
                                                          local.get $l4
                                                          local.get $l5
                                                          select
                                                          local.tee $l4
                                                          i32.const 88
                                                          i32.sub
                                                          br_table $B46 $B6 $B6 $B6 $B6 $B6 $B6 $B6 $B6 $B36 $B6 $B35 $B44 $B36 $B36 $B36 $B6 $B44 $B6 $B6 $B6 $B6 $B48 $B45 $B47 $B6 $B6 $B41 $B6 $B49 $B6 $B6 $B46 $B50
                                                        end
                                                        block $B51
                                                          local.get $l4
                                                          i32.const 65
                                                          i32.sub
                                                          br_table $B36 $B6 $B39 $B6 $B36 $B36 $B36 $B51
                                                        end
                                                        local.get $l4
                                                        i32.const 83
                                                        i32.eq
                                                        br_if $B40
                                                        br $B6
                                                      end
                                                      local.get $l6
                                                      i64.load offset=64
                                                      local.set $l23
                                                      i32.const 1024
                                                      br $B43
                                                    end
                                                    i32.const 0
                                                    local.set $l4
                                                    block $B52
                                                      block $B53
                                                        block $B54
                                                          block $B55
                                                            block $B56
                                                              block $B57
                                                                block $B58
                                                                  local.get $l5
                                                                  i32.const 255
                                                                  i32.and
                                                                  br_table $B58 $B57 $B56 $B55 $B54 $L4 $B53 $B52 $L4
                                                                end
                                                                local.get $l6
                                                                i32.load offset=64
                                                                local.get $l13
                                                                i32.store
                                                                br $L4
                                                              end
                                                              local.get $l6
                                                              i32.load offset=64
                                                              local.get $l13
                                                              i32.store
                                                              br $L4
                                                            end
                                                            local.get $l6
                                                            i32.load offset=64
                                                            local.get $l13
                                                            i64.extend_i32_s
                                                            i64.store
                                                            br $L4
                                                          end
                                                          local.get $l6
                                                          i32.load offset=64
                                                          local.get $l13
                                                          i32.store16
                                                          br $L4
                                                        end
                                                        local.get $l6
                                                        i32.load offset=64
                                                        local.get $l13
                                                        i32.store8
                                                        br $L4
                                                      end
                                                      local.get $l6
                                                      i32.load offset=64
                                                      local.get $l13
                                                      i32.store
                                                      br $L4
                                                    end
                                                    local.get $l6
                                                    i32.load offset=64
                                                    local.get $l13
                                                    i64.extend_i32_s
                                                    i64.store
                                                    br $L4
                                                  end
                                                  i32.const 8
                                                  local.get $l8
                                                  local.get $l8
                                                  i32.const 8
                                                  i32.le_u
                                                  select
                                                  local.set $l8
                                                  local.get $l7
                                                  i32.const 8
                                                  i32.or
                                                  local.set $l7
                                                  i32.const 120
                                                  local.set $l4
                                                end
                                                local.get $l17
                                                local.set $l9
                                                local.get $l4
                                                i32.const 32
                                                i32.and
                                                local.set $l5
                                                local.get $l6
                                                i64.load offset=64
                                                local.tee $l23
                                                i64.const 0
                                                i64.ne
                                                if $I59
                                                  loop $L60
                                                    local.get $l9
                                                    i32.const 1
                                                    i32.sub
                                                    local.tee $l9
                                                    local.get $l23
                                                    i32.wrap_i64
                                                    i32.const 15
                                                    i32.and
                                                    i32.const 1536
                                                    i32.add
                                                    i32.load8_u
                                                    local.get $l5
                                                    i32.or
                                                    i32.store8
                                                    local.get $l23
                                                    i64.const 15
                                                    i64.gt_u
                                                    local.set $l18
                                                    local.get $l23
                                                    i64.const 4
                                                    i64.shr_u
                                                    local.set $l23
                                                    local.get $l18
                                                    br_if $L60
                                                  end
                                                end
                                                local.get $l6
                                                i64.load offset=64
                                                i64.eqz
                                                br_if $B42
                                                local.get $l7
                                                i32.const 8
                                                i32.and
                                                i32.eqz
                                                br_if $B42
                                                local.get $l4
                                                i32.const 4
                                                i32.shr_u
                                                i32.const 1024
                                                i32.add
                                                local.set $l21
                                                i32.const 2
                                                local.set $l14
                                                br $B42
                                              end
                                              local.get $l17
                                              local.set $l4
                                              local.get $l6
                                              i64.load offset=64
                                              local.tee $l23
                                              i64.const 0
                                              i64.ne
                                              if $I61
                                                loop $L62
                                                  local.get $l4
                                                  i32.const 1
                                                  i32.sub
                                                  local.tee $l4
                                                  local.get $l23
                                                  i32.wrap_i64
                                                  i32.const 7
                                                  i32.and
                                                  i32.const 48
                                                  i32.or
                                                  i32.store8
                                                  local.get $l23
                                                  i64.const 7
                                                  i64.gt_u
                                                  local.set $l9
                                                  local.get $l23
                                                  i64.const 3
                                                  i64.shr_u
                                                  local.set $l23
                                                  local.get $l9
                                                  br_if $L62
                                                end
                                              end
                                              local.get $l4
                                              local.set $l9
                                              local.get $l7
                                              i32.const 8
                                              i32.and
                                              i32.eqz
                                              br_if $B42
                                              local.get $l8
                                              local.get $l17
                                              local.get $l9
                                              i32.sub
                                              local.tee $l4
                                              i32.const 1
                                              i32.add
                                              local.get $l4
                                              local.get $l8
                                              i32.lt_s
                                              select
                                              local.set $l8
                                              br $B42
                                            end
                                            local.get $l6
                                            i64.load offset=64
                                            local.tee $l23
                                            i64.const 0
                                            i64.lt_s
                                            if $I63
                                              local.get $l6
                                              i64.const 0
                                              local.get $l23
                                              i64.sub
                                              local.tee $l23
                                              i64.store offset=64
                                              i32.const 1
                                              local.set $l14
                                              i32.const 1024
                                              br $B43
                                            end
                                            local.get $l7
                                            i32.const 2048
                                            i32.and
                                            if $I64
                                              i32.const 1
                                              local.set $l14
                                              i32.const 1025
                                              br $B43
                                            end
                                            i32.const 1026
                                            i32.const 1024
                                            local.get $l7
                                            i32.const 1
                                            i32.and
                                            local.tee $l14
                                            select
                                          end
                                          local.set $l21
                                          local.get $l17
                                          local.set $l9
                                          block $B65
                                            local.get $l23
                                            i64.const 4294967296
                                            i64.lt_u
                                            if $I66
                                              local.get $l23
                                              local.set $l24
                                              br $B65
                                            end
                                            loop $L67
                                              local.get $l9
                                              i32.const 1
                                              i32.sub
                                              local.tee $l9
                                              local.get $l23
                                              local.get $l23
                                              i64.const 10
                                              i64.div_u
                                              local.tee $l24
                                              i64.const 10
                                              i64.mul
                                              i64.sub
                                              i32.wrap_i64
                                              i32.const 48
                                              i32.or
                                              i32.store8
                                              local.get $l23
                                              i64.const 42949672959
                                              i64.gt_u
                                              local.set $l5
                                              local.get $l24
                                              local.set $l23
                                              local.get $l5
                                              br_if $L67
                                            end
                                          end
                                          local.get $l24
                                          i32.wrap_i64
                                          local.tee $l5
                                          if $I68
                                            loop $L69
                                              local.get $l9
                                              i32.const 1
                                              i32.sub
                                              local.tee $l9
                                              local.get $l5
                                              local.get $l5
                                              i32.const 10
                                              i32.div_u
                                              local.tee $l4
                                              i32.const 10
                                              i32.mul
                                              i32.sub
                                              i32.const 48
                                              i32.or
                                              i32.store8
                                              local.get $l5
                                              i32.const 9
                                              i32.gt_u
                                              local.set $l18
                                              local.get $l4
                                              local.set $l5
                                              local.get $l18
                                              br_if $L69
                                            end
                                          end
                                        end
                                        local.get $l20
                                        i32.const 0
                                        local.get $l8
                                        i32.const 0
                                        i32.lt_s
                                        select
                                        br_if $B3
                                        local.get $l7
                                        i32.const -65537
                                        i32.and
                                        local.get $l7
                                        local.get $l20
                                        select
                                        local.set $l7
                                        block $B70
                                          local.get $l6
                                          i64.load offset=64
                                          local.tee $l23
                                          i64.const 0
                                          i64.ne
                                          br_if $B70
                                          local.get $l8
                                          br_if $B70
                                          local.get $l17
                                          local.tee $l9
                                          local.set $l16
                                          i32.const 0
                                          local.set $l8
                                          br $B6
                                        end
                                        local.get $l8
                                        local.get $l23
                                        i64.eqz
                                        local.get $l17
                                        local.get $l9
                                        i32.sub
                                        i32.add
                                        local.tee $l4
                                        local.get $l4
                                        local.get $l8
                                        i32.lt_s
                                        select
                                        local.set $l8
                                        br $B6
                                      end
                                      block $B71 (result i32)
                                        i32.const 2147483647
                                        local.get $l8
                                        local.get $l8
                                        i32.const 2147483647
                                        i32.ge_u
                                        select
                                        local.tee $l10
                                        local.tee $l5
                                        i32.const 0
                                        i32.ne
                                        local.set $l7
                                        block $B72
                                          block $B73
                                            block $B74
                                              local.get $l6
                                              i32.load offset=64
                                              local.tee $l4
                                              i32.const 1041
                                              local.get $l4
                                              select
                                              local.tee $l9
                                              local.tee $l4
                                              i32.const 3
                                              i32.and
                                              i32.eqz
                                              br_if $B74
                                              local.get $l5
                                              i32.eqz
                                              br_if $B74
                                              loop $L75
                                                local.get $l4
                                                i32.load8_u
                                                i32.eqz
                                                br_if $B73
                                                local.get $l5
                                                i32.const 1
                                                i32.sub
                                                local.tee $l5
                                                i32.const 0
                                                i32.ne
                                                local.set $l7
                                                local.get $l4
                                                i32.const 1
                                                i32.add
                                                local.tee $l4
                                                i32.const 3
                                                i32.and
                                                i32.eqz
                                                br_if $B74
                                                local.get $l5
                                                br_if $L75
                                              end
                                            end
                                            local.get $l7
                                            i32.eqz
                                            br_if $B72
                                            block $B76
                                              local.get $l4
                                              i32.load8_u
                                              i32.eqz
                                              br_if $B76
                                              local.get $l5
                                              i32.const 4
                                              i32.lt_u
                                              br_if $B76
                                              loop $L77
                                                local.get $l4
                                                i32.load
                                                local.tee $l7
                                                i32.const -1
                                                i32.xor
                                                local.get $l7
                                                i32.const 16843009
                                                i32.sub
                                                i32.and
                                                i32.const -2139062144
                                                i32.and
                                                br_if $B73
                                                local.get $l4
                                                i32.const 4
                                                i32.add
                                                local.set $l4
                                                local.get $l5
                                                i32.const 4
                                                i32.sub
                                                local.tee $l5
                                                i32.const 3
                                                i32.gt_u
                                                br_if $L77
                                              end
                                            end
                                            local.get $l5
                                            i32.eqz
                                            br_if $B72
                                          end
                                          loop $L78
                                            local.get $l4
                                            local.get $l4
                                            i32.load8_u
                                            i32.eqz
                                            br_if $B71
                                            drop
                                            local.get $l4
                                            i32.const 1
                                            i32.add
                                            local.set $l4
                                            local.get $l5
                                            i32.const 1
                                            i32.sub
                                            local.tee $l5
                                            br_if $L78
                                          end
                                        end
                                        i32.const 0
                                      end
                                      local.tee $l4
                                      local.get $l9
                                      i32.sub
                                      local.get $l10
                                      local.get $l4
                                      select
                                      local.tee $l4
                                      local.get $l9
                                      i32.add
                                      local.set $l16
                                      local.get $l8
                                      i32.const 0
                                      i32.ge_s
                                      if $I79
                                        local.get $l18
                                        local.set $l7
                                        local.get $l4
                                        local.set $l8
                                        br $B6
                                      end
                                      local.get $l18
                                      local.set $l7
                                      local.get $l4
                                      local.set $l8
                                      local.get $l16
                                      i32.load8_u
                                      br_if $B3
                                      br $B6
                                    end
                                    local.get $l8
                                    if $I80
                                      local.get $l6
                                      i32.load offset=64
                                      br $B38
                                    end
                                    i32.const 0
                                    local.set $l4
                                    local.get $p0
                                    i32.const 32
                                    local.get $l15
                                    i32.const 0
                                    local.get $l7
                                    call $f19
                                    br $B37
                                  end
                                  local.get $l6
                                  i32.const 0
                                  i32.store offset=12
                                  local.get $l6
                                  local.get $l6
                                  i64.load offset=64
                                  i64.store32 offset=8
                                  local.get $l6
                                  local.get $l6
                                  i32.const 8
                                  i32.add
                                  i32.store offset=64
                                  i32.const -1
                                  local.set $l8
                                  local.get $l6
                                  i32.const 8
                                  i32.add
                                end
                                local.set $l12
                                i32.const 0
                                local.set $l4
                                block $B81
                                  loop $L82
                                    local.get $l12
                                    i32.load
                                    local.tee $l5
                                    i32.eqz
                                    br_if $B81
                                    block $B83
                                      local.get $l6
                                      i32.const 4
                                      i32.add
                                      local.get $l5
                                      call $f21
                                      local.tee $l5
                                      i32.const 0
                                      i32.lt_s
                                      local.tee $l9
                                      br_if $B83
                                      local.get $l5
                                      local.get $l8
                                      local.get $l4
                                      i32.sub
                                      i32.gt_u
                                      br_if $B83
                                      local.get $l12
                                      i32.const 4
                                      i32.add
                                      local.set $l12
                                      local.get $l8
                                      local.get $l4
                                      local.get $l5
                                      i32.add
                                      local.tee $l4
                                      i32.gt_u
                                      br_if $L82
                                      br $B81
                                    end
                                  end
                                  local.get $l9
                                  br_if $B1
                                end
                                i32.const 61
                                local.set $l16
                                local.get $l4
                                i32.const 0
                                i32.lt_s
                                br_if $B2
                                local.get $p0
                                i32.const 32
                                local.get $l15
                                local.get $l4
                                local.get $l7
                                call $f19
                                local.get $l4
                                i32.eqz
                                if $I84
                                  i32.const 0
                                  local.set $l4
                                  br $B37
                                end
                                i32.const 0
                                local.set $l5
                                local.get $l6
                                i32.load offset=64
                                local.set $l12
                                loop $L85
                                  local.get $l12
                                  i32.load
                                  local.tee $l9
                                  i32.eqz
                                  br_if $B37
                                  local.get $l6
                                  i32.const 4
                                  i32.add
                                  local.get $l9
                                  call $f21
                                  local.tee $l9
                                  local.get $l5
                                  i32.add
                                  local.tee $l5
                                  local.get $l4
                                  i32.gt_u
                                  br_if $B37
                                  local.get $p0
                                  local.get $l6
                                  i32.const 4
                                  i32.add
                                  local.get $l9
                                  call $f16
                                  local.get $l12
                                  i32.const 4
                                  i32.add
                                  local.set $l12
                                  local.get $l4
                                  local.get $l5
                                  i32.gt_u
                                  br_if $L85
                                end
                              end
                              local.get $p0
                              i32.const 32
                              local.get $l15
                              local.get $l4
                              local.get $l7
                              i32.const 8192
                              i32.xor
                              call $f19
                              local.get $l15
                              local.get $l4
                              local.get $l4
                              local.get $l15
                              i32.lt_s
                              select
                              local.set $l4
                              br $L4
                            end
                            local.get $l20
                            i32.const 0
                            local.get $l8
                            i32.const 0
                            i32.lt_s
                            select
                            br_if $B3
                            i32.const 61
                            local.set $l16
                            local.get $p0
                            drop
                            local.get $l6
                            f64.load offset=64
                            drop
                            local.get $l15
                            drop
                            local.get $l8
                            drop
                            local.get $l7
                            drop
                            local.get $l4
                            drop
                            unreachable
                          end
                          local.get $l6
                          local.get $l6
                          i64.load offset=64
                          i64.store8 offset=55
                          i32.const 1
                          local.set $l8
                          local.get $l22
                          local.set $l9
                          local.get $l18
                          local.set $l7
                          br $B6
                        end
                        local.get $l4
                        i32.load8_u offset=1
                        local.set $l12
                        local.get $l4
                        i32.const 1
                        i32.add
                        local.set $l4
                        br $L9
                      end
                      unreachable
                    end
                    local.get $p0
                    br_if $B0
                    local.get $l19
                    i32.eqz
                    br_if $B5
                    i32.const 1
                    local.set $l4
                    loop $L86
                      local.get $p3
                      local.get $l4
                      i32.const 2
                      i32.shl
                      i32.add
                      i32.load
                      local.tee $l12
                      if $I87
                        local.get $p2
                        local.get $l4
                        i32.const 3
                        i32.shl
                        i32.add
                        local.get $l12
                        local.get $p1
                        call $f18
                        i32.const 1
                        local.set $l13
                        local.get $l4
                        i32.const 1
                        i32.add
                        local.tee $l4
                        i32.const 10
                        i32.ne
                        br_if $L86
                        br $B0
                      end
                    end
                    i32.const 1
                    local.set $l13
                    local.get $l4
                    i32.const 10
                    i32.ge_u
                    br_if $B0
                    loop $L88
                      local.get $p3
                      local.get $l4
                      i32.const 2
                      i32.shl
                      i32.add
                      i32.load
                      br_if $B7
                      local.get $l4
                      i32.const 1
                      i32.add
                      local.tee $l4
                      i32.const 10
                      i32.ne
                      br_if $L88
                    end
                    br $B0
                  end
                  i32.const 28
                  local.set $l16
                  br $B2
                end
                local.get $l8
                local.get $l16
                local.get $l9
                i32.sub
                local.tee $l10
                local.get $l8
                local.get $l10
                i32.gt_s
                select
                local.tee $l8
                local.get $l14
                i32.const 2147483647
                i32.xor
                i32.gt_s
                br_if $B3
                i32.const 61
                local.set $l16
                local.get $l15
                local.get $l8
                local.get $l14
                i32.add
                local.tee $l5
                local.get $l5
                local.get $l15
                i32.lt_s
                select
                local.tee $l4
                local.get $l12
                i32.gt_s
                br_if $B2
                local.get $p0
                i32.const 32
                local.get $l4
                local.get $l5
                local.get $l7
                call $f19
                local.get $p0
                local.get $l21
                local.get $l14
                call $f16
                local.get $p0
                i32.const 48
                local.get $l4
                local.get $l5
                local.get $l7
                i32.const 65536
                i32.xor
                call $f19
                local.get $p0
                i32.const 48
                local.get $l8
                local.get $l10
                i32.const 0
                call $f19
                local.get $p0
                local.get $l9
                local.get $l10
                call $f16
                local.get $p0
                i32.const 32
                local.get $l4
                local.get $l5
                local.get $l7
                i32.const 8192
                i32.xor
                call $f19
                br $L4
              end
            end
            i32.const 0
            local.set $l13
            br $B0
          end
          i32.const 61
          local.set $l16
        end
        i32.const 2744
        local.get $l16
        i32.store
      end
      i32.const -1
      local.set $l13
    end
    local.get $l6
    i32.const 80
    i32.add
    global.set $g0
    local.get $l13)
  (func $f16 (type $t4) (param $p0 i32) (param $p1 i32) (param $p2 i32)
    (local $l3 i32) (local $l4 i32) (local $l5 i32) (local $l6 i32)
    local.get $p0
    i32.load8_u
    i32.const 32
    i32.and
    i32.eqz
    if $I0
      block $B1
        local.get $p1
        local.set $l3
        block $B2
          local.get $p2
          local.get $p0
          local.tee $l5
          i32.load offset=16
          local.tee $l4
          if $I3 (result i32)
            local.get $l4
          else
            local.get $l5
            call $f13
            br_if $B2
            local.get $l5
            i32.load offset=16
          end
          local.get $l5
          i32.load offset=20
          local.tee $p1
          i32.sub
          i32.gt_u
          if $I4
            local.get $l5
            local.get $l3
            local.get $p2
            local.get $l5
            i32.load offset=36
            call_indirect $__indirect_function_table (type $t1)
            drop
            br $B1
          end
          block $B5
            local.get $l5
            i32.load offset=80
            i32.const 0
            i32.lt_s
            br_if $B5
            local.get $p2
            local.set $p0
            loop $L6
              local.get $p0
              local.tee $l4
              i32.eqz
              br_if $B5
              local.get $l3
              local.get $l4
              i32.const 1
              i32.sub
              local.tee $p0
              i32.add
              i32.load8_u
              i32.const 10
              i32.ne
              br_if $L6
            end
            local.get $l5
            local.get $l3
            local.get $l4
            local.get $l5
            i32.load offset=36
            call_indirect $__indirect_function_table (type $t1)
            local.get $l4
            i32.lt_u
            br_if $B2
            local.get $l3
            local.get $l4
            i32.add
            local.set $l3
            local.get $p2
            local.get $l4
            i32.sub
            local.set $p2
            local.get $l5
            i32.load offset=20
            local.set $p1
          end
          local.get $p1
          local.tee $p0
          local.get $p2
          local.tee $p1
          i32.add
          local.set $l4
          block $B7
            block $B8
              block $B9
                local.get $p0
                local.get $l3
                i32.xor
                i32.const 3
                i32.and
                i32.eqz
                if $I10
                  local.get $p0
                  i32.const 3
                  i32.and
                  i32.eqz
                  br_if $B9
                  local.get $p1
                  i32.const 0
                  i32.le_s
                  br_if $B9
                  local.get $p0
                  local.set $p1
                  loop $L11
                    local.get $p1
                    local.get $l3
                    i32.load8_u
                    i32.store8
                    local.get $l3
                    i32.const 1
                    i32.add
                    local.set $l3
                    local.get $p1
                    i32.const 1
                    i32.add
                    local.tee $p1
                    i32.const 3
                    i32.and
                    i32.eqz
                    br_if $B8
                    local.get $p1
                    local.get $l4
                    i32.lt_u
                    br_if $L11
                  end
                  br $B8
                end
                block $B12
                  local.get $l4
                  i32.const 4
                  i32.lt_u
                  br_if $B12
                  local.get $l4
                  i32.const 4
                  i32.sub
                  local.tee $l6
                  local.get $p0
                  i32.lt_u
                  br_if $B12
                  local.get $p0
                  local.set $p1
                  loop $L13
                    local.get $p1
                    local.get $l3
                    i32.load8_u
                    i32.store8
                    local.get $p1
                    local.get $l3
                    i32.load8_u offset=1
                    i32.store8 offset=1
                    local.get $p1
                    local.get $l3
                    i32.load8_u offset=2
                    i32.store8 offset=2
                    local.get $p1
                    local.get $l3
                    i32.load8_u offset=3
                    i32.store8 offset=3
                    local.get $l3
                    i32.const 4
                    i32.add
                    local.set $l3
                    local.get $p1
                    i32.const 4
                    i32.add
                    local.tee $p1
                    local.get $l6
                    i32.le_u
                    br_if $L13
                  end
                  br $B7
                end
                local.get $p0
                local.set $p1
                br $B7
              end
              local.get $p0
              local.set $p1
            end
            block $B14
              local.get $l4
              i32.const -4
              i32.and
              local.tee $l6
              i32.const 64
              i32.lt_u
              br_if $B14
              local.get $p1
              local.get $l6
              i32.const -64
              i32.add
              local.tee $p0
              i32.gt_u
              br_if $B14
              loop $L15
                local.get $p1
                local.get $l3
                i32.load
                i32.store
                local.get $p1
                local.get $l3
                i32.load offset=4
                i32.store offset=4
                local.get $p1
                local.get $l3
                i32.load offset=8
                i32.store offset=8
                local.get $p1
                local.get $l3
                i32.load offset=12
                i32.store offset=12
                local.get $p1
                local.get $l3
                i32.load offset=16
                i32.store offset=16
                local.get $p1
                local.get $l3
                i32.load offset=20
                i32.store offset=20
                local.get $p1
                local.get $l3
                i32.load offset=24
                i32.store offset=24
                local.get $p1
                local.get $l3
                i32.load offset=28
                i32.store offset=28
                local.get $p1
                local.get $l3
                i32.load offset=32
                i32.store offset=32
                local.get $p1
                local.get $l3
                i32.load offset=36
                i32.store offset=36
                local.get $p1
                local.get $l3
                i32.load offset=40
                i32.store offset=40
                local.get $p1
                local.get $l3
                i32.load offset=44
                i32.store offset=44
                local.get $p1
                local.get $l3
                i32.load offset=48
                i32.store offset=48
                local.get $p1
                local.get $l3
                i32.load offset=52
                i32.store offset=52
                local.get $p1
                local.get $l3
                i32.load offset=56
                i32.store offset=56
                local.get $p1
                local.get $l3
                i32.load offset=60
                i32.store offset=60
                local.get $l3
                i32.const -64
                i32.sub
                local.set $l3
                local.get $p1
                i32.const -64
                i32.sub
                local.tee $p1
                local.get $p0
                i32.le_u
                br_if $L15
              end
            end
            local.get $p1
            local.get $l6
            i32.ge_u
            br_if $B7
            loop $L16
              local.get $p1
              local.get $l3
              i32.load
              i32.store
              local.get $l3
              i32.const 4
              i32.add
              local.set $l3
              local.get $p1
              i32.const 4
              i32.add
              local.tee $p1
              local.get $l6
              i32.lt_u
              br_if $L16
            end
          end
          local.get $p1
          local.get $l4
          i32.lt_u
          if $I17
            loop $L18
              local.get $p1
              local.get $l3
              i32.load8_u
              i32.store8
              local.get $l3
              i32.const 1
              i32.add
              local.set $l3
              local.get $p1
              i32.const 1
              i32.add
              local.tee $p1
              local.get $l4
              i32.ne
              br_if $L18
            end
          end
          local.get $l5
          local.get $l5
          i32.load offset=20
          local.get $p2
          i32.add
          i32.store offset=20
        end
      end
    end)
  (func $f17 (type $t0) (param $p0 i32) (result i32)
    (local $l1 i32) (local $l2 i32) (local $l3 i32)
    local.get $p0
    i32.load
    i32.load8_s
    call $f14
    i32.eqz
    if $I0
      i32.const 0
      return
    end
    loop $L1
      local.get $p0
      i32.load
      local.set $l3
      i32.const -1
      local.set $l1
      local.get $l2
      i32.const 214748364
      i32.le_u
      if $I2
        i32.const -1
        local.get $l3
        i32.load8_s
        i32.const 48
        i32.sub
        local.tee $l1
        local.get $l2
        i32.const 10
        i32.mul
        local.tee $l2
        i32.add
        local.get $l1
        local.get $l2
        i32.const 2147483647
        i32.xor
        i32.gt_s
        select
        local.set $l1
      end
      local.get $p0
      local.get $l3
      i32.const 1
      i32.add
      i32.store
      local.get $l1
      local.set $l2
      local.get $l3
      i32.load8_s offset=1
      call $f14
      br_if $L1
    end
    local.get $l1)
  (func $f18 (type $t4) (param $p0 i32) (param $p1 i32) (param $p2 i32)
    block $B0
      block $B1
        block $B2
          block $B3
            block $B4
              block $B5
                block $B6
                  block $B7
                    block $B8
                      block $B9
                        block $B10
                          block $B11
                            block $B12
                              block $B13
                                block $B14
                                  block $B15
                                    block $B16
                                      block $B17
                                        block $B18
                                          local.get $p1
                                          i32.const 9
                                          i32.sub
                                          br_table $B18 $B17 $B16 $B13 $B15 $B14 $B12 $B11 $B10 $B9 $B8 $B7 $B6 $B5 $B4 $B3 $B2 $B1 $B0
                                        end
                                        local.get $p2
                                        local.get $p2
                                        i32.load
                                        local.tee $p1
                                        i32.const 4
                                        i32.add
                                        i32.store
                                        local.get $p0
                                        local.get $p1
                                        i32.load
                                        i32.store
                                        return
                                      end
                                      local.get $p2
                                      local.get $p2
                                      i32.load
                                      local.tee $p1
                                      i32.const 4
                                      i32.add
                                      i32.store
                                      local.get $p0
                                      local.get $p1
                                      i64.load32_s
                                      i64.store
                                      return
                                    end
                                    local.get $p2
                                    local.get $p2
                                    i32.load
                                    local.tee $p1
                                    i32.const 4
                                    i32.add
                                    i32.store
                                    local.get $p0
                                    local.get $p1
                                    i64.load32_u
                                    i64.store
                                    return
                                  end
                                  local.get $p2
                                  local.get $p2
                                  i32.load
                                  local.tee $p1
                                  i32.const 4
                                  i32.add
                                  i32.store
                                  local.get $p0
                                  local.get $p1
                                  i64.load32_s
                                  i64.store
                                  return
                                end
                                local.get $p2
                                local.get $p2
                                i32.load
                                local.tee $p1
                                i32.const 4
                                i32.add
                                i32.store
                                local.get $p0
                                local.get $p1
                                i64.load32_u
                                i64.store
                                return
                              end
                              local.get $p2
                              local.get $p2
                              i32.load
                              i32.const 7
                              i32.add
                              i32.const -8
                              i32.and
                              local.tee $p1
                              i32.const 8
                              i32.add
                              i32.store
                              local.get $p0
                              local.get $p1
                              i64.load
                              i64.store
                              return
                            end
                            local.get $p2
                            local.get $p2
                            i32.load
                            local.tee $p1
                            i32.const 4
                            i32.add
                            i32.store
                            local.get $p0
                            local.get $p1
                            i64.load16_s
                            i64.store
                            return
                          end
                          local.get $p2
                          local.get $p2
                          i32.load
                          local.tee $p1
                          i32.const 4
                          i32.add
                          i32.store
                          local.get $p0
                          local.get $p1
                          i64.load16_u
                          i64.store
                          return
                        end
                        local.get $p2
                        local.get $p2
                        i32.load
                        local.tee $p1
                        i32.const 4
                        i32.add
                        i32.store
                        local.get $p0
                        local.get $p1
                        i64.load8_s
                        i64.store
                        return
                      end
                      local.get $p2
                      local.get $p2
                      i32.load
                      local.tee $p1
                      i32.const 4
                      i32.add
                      i32.store
                      local.get $p0
                      local.get $p1
                      i64.load8_u
                      i64.store
                      return
                    end
                    local.get $p2
                    local.get $p2
                    i32.load
                    i32.const 7
                    i32.add
                    i32.const -8
                    i32.and
                    local.tee $p1
                    i32.const 8
                    i32.add
                    i32.store
                    local.get $p0
                    local.get $p1
                    i64.load
                    i64.store
                    return
                  end
                  local.get $p2
                  local.get $p2
                  i32.load
                  local.tee $p1
                  i32.const 4
                  i32.add
                  i32.store
                  local.get $p0
                  local.get $p1
                  i64.load32_u
                  i64.store
                  return
                end
                local.get $p2
                local.get $p2
                i32.load
                i32.const 7
                i32.add
                i32.const -8
                i32.and
                local.tee $p1
                i32.const 8
                i32.add
                i32.store
                local.get $p0
                local.get $p1
                i64.load
                i64.store
                return
              end
              local.get $p2
              local.get $p2
              i32.load
              i32.const 7
              i32.add
              i32.const -8
              i32.and
              local.tee $p1
              i32.const 8
              i32.add
              i32.store
              local.get $p0
              local.get $p1
              i64.load
              i64.store
              return
            end
            local.get $p2
            local.get $p2
            i32.load
            local.tee $p1
            i32.const 4
            i32.add
            i32.store
            local.get $p0
            local.get $p1
            i64.load32_s
            i64.store
            return
          end
          local.get $p2
          local.get $p2
          i32.load
          local.tee $p1
          i32.const 4
          i32.add
          i32.store
          local.get $p0
          local.get $p1
          i64.load32_u
          i64.store
          return
        end
        local.get $p2
        local.get $p2
        i32.load
        i32.const 7
        i32.add
        i32.const -8
        i32.and
        local.tee $p1
        i32.const 8
        i32.add
        i32.store
        local.get $p0
        local.get $p1
        f64.load
        f64.store
        return
      end
      local.get $p0
      drop
      local.get $p2
      drop
      unreachable
    end)
  (func $f19 (type $t9) (param $p0 i32) (param $p1 i32) (param $p2 i32) (param $p3 i32) (param $p4 i32)
    (local $l5 i32)
    global.get $g0
    i32.const 256
    i32.sub
    local.tee $l5
    global.set $g0
    block $B0
      local.get $p2
      local.get $p3
      i32.le_s
      br_if $B0
      local.get $p4
      i32.const 73728
      i32.and
      br_if $B0
      local.get $l5
      local.get $p1
      i32.const 255
      i32.and
      local.get $p2
      local.get $p3
      i32.sub
      local.tee $p3
      i32.const 256
      local.get $p3
      i32.const 256
      i32.lt_u
      local.tee $p2
      select
      call $f10
      local.get $p2
      i32.eqz
      if $I1
        loop $L2
          local.get $p0
          local.get $l5
          i32.const 256
          call $f16
          local.get $p3
          i32.const 256
          i32.sub
          local.tee $p3
          i32.const 255
          i32.gt_u
          br_if $L2
        end
      end
      local.get $p0
      local.get $l5
      local.get $p3
      call $f16
    end
    local.get $l5
    i32.const 256
    i32.add
    global.set $g0)
  (func $f20 (type $t0) (param $p0 i32) (result i32)
    local.get $p0
    i32.eqz
    if $I0
      i32.const 0
      return
    end
    i32.const 2744
    local.get $p0
    i32.store
    i32.const -1)
  (func $f21 (type $t2) (param $p0 i32) (param $p1 i32) (result i32)
    local.get $p0
    i32.eqz
    if $I0
      i32.const 0
      return
    end
    block $B1 (result i32)
      block $B2
        local.get $p0
        if $I3 (result i32)
          local.get $p1
          i32.const 127
          i32.le_u
          br_if $B2
          block $B4
            i32.const 2904
            i32.load
            i32.load
            i32.eqz
            if $I5
              local.get $p1
              i32.const -128
              i32.and
              i32.const 57216
              i32.eq
              br_if $B2
              i32.const 2744
              i32.const 25
              i32.store
              br $B4
            end
            local.get $p1
            i32.const 2047
            i32.le_u
            if $I6
              local.get $p0
              local.get $p1
              i32.const 63
              i32.and
              i32.const 128
              i32.or
              i32.store8 offset=1
              local.get $p0
              local.get $p1
              i32.const 6
              i32.shr_u
              i32.const 192
              i32.or
              i32.store8
              i32.const 2
              br $B1
            end
            local.get $p1
            i32.const -8192
            i32.and
            i32.const 57344
            i32.ne
            local.get $p1
            i32.const 55296
            i32.ge_u
            i32.and
            i32.eqz
            if $I7
              local.get $p0
              local.get $p1
              i32.const 63
              i32.and
              i32.const 128
              i32.or
              i32.store8 offset=2
              local.get $p0
              local.get $p1
              i32.const 12
              i32.shr_u
              i32.const 224
              i32.or
              i32.store8
              local.get $p0
              local.get $p1
              i32.const 6
              i32.shr_u
              i32.const 63
              i32.and
              i32.const 128
              i32.or
              i32.store8 offset=1
              i32.const 3
              br $B1
            end
            local.get $p1
            i32.const 65536
            i32.sub
            i32.const 1048575
            i32.le_u
            if $I8
              local.get $p0
              local.get $p1
              i32.const 63
              i32.and
              i32.const 128
              i32.or
              i32.store8 offset=3
              local.get $p0
              local.get $p1
              i32.const 18
              i32.shr_u
              i32.const 240
              i32.or
              i32.store8
              local.get $p0
              local.get $p1
              i32.const 6
              i32.shr_u
              i32.const 63
              i32.and
              i32.const 128
              i32.or
              i32.store8 offset=2
              local.get $p0
              local.get $p1
              i32.const 12
              i32.shr_u
              i32.const 63
              i32.and
              i32.const 128
              i32.or
              i32.store8 offset=1
              i32.const 4
              br $B1
            end
            i32.const 2744
            i32.const 25
            i32.store
          end
          i32.const -1
        else
          i32.const 1
        end
        br $B1
      end
      local.get $p0
      local.get $p1
      i32.store8
      i32.const 1
    end)
  (func $stackSave (type $t8) (result i32)
    global.get $g0)
  (func $stackRestore (type $t3) (param $p0 i32)
    local.get $p0
    global.set $g0)
  (func $stackAlloc (type $t0) (param $p0 i32) (result i32)
    global.get $g0
    local.get $p0
    i32.sub
    i32.const -16
    i32.and
    local.tee $p0
    global.set $g0
    local.get $p0)
  (table $__indirect_function_table 5 5 funcref)
  (memory $memory 256 256)
  (global $g0 (mut i32) (i32.const 5245808))
  (export "memory" (memory $memory))
  (export "__main_argc_argv" (func $__main_argc_argv))
  (export "__indirect_function_table" (table $__indirect_function_table))
  (export "_start" (func $_start))
  (export "__errno_location" (func $__errno_location))
  (export "stackSave" (func $stackSave))
  (export "stackRestore" (func $stackRestore))
  (export "stackAlloc" (func $stackAlloc))
  (elem $e0 (i32.const 1) func $f4 $f8 $f7 $f9)
  (data $d0 (i32.const 1024) "-+   0X0x\00printf\00(null)\00Hello %s\0a")
  (data $d1 (i32.const 1072) "\19\00\0a\00\19\19\19\00\00\00\00\05\00\00\00\00\00\00\09\00\00\00\00\0b\00\00\00\00\00\00\00\00\19\00\11\0a\19\19\19\03\0a\07\00\01\00\09\0b\18\00\00\09\06\0b\00\00\0b\00\06\19\00\00\00\19\19\19")
  (data $d2 (i32.const 1153) "\0e\00\00\00\00\00\00\00\00\19\00\0a\0d\19\19\19\00\0d\00\00\02\00\09\0e\00\00\00\09\00\0e\00\00\0e")
  (data $d3 (i32.const 1211) "\0c")
  (data $d4 (i32.const 1223) "\13\00\00\00\00\13\00\00\00\00\09\0c\00\00\00\00\00\0c\00\00\0c")
  (data $d5 (i32.const 1269) "\10")
  (data $d6 (i32.const 1281) "\0f\00\00\00\04\0f\00\00\00\00\09\10\00\00\00\00\00\10\00\00\10")
  (data $d7 (i32.const 1327) "\12")
  (data $d8 (i32.const 1339) "\11\00\00\00\00\11\00\00\00\00\09\12\00\00\00\00\00\12\00\00\12\00\00\1a\00\00\00\1a\1a\1a")
  (data $d9 (i32.const 1394) "\1a\00\00\00\1a\1a\1a\00\00\00\00\00\00\09")
  (data $d10 (i32.const 1443) "\14")
  (data $d11 (i32.const 1455) "\17\00\00\00\00\17\00\00\00\00\09\14\00\00\00\00\00\14\00\00\14")
  (data $d12 (i32.const 1501) "\16")
  (data $d13 (i32.const 1513) "\15\00\00\00\00\15\00\00\00\00\09\16\00\00\00\00\00\16\00\00\16\00\000123456789ABCDEF")
  (data $d14 (i32.const 1552) "\05")
  (data $d15 (i32.const 1564) "\02")
  (data $d16 (i32.const 1588) "\03\00\00\00\04\00\00\00\b8\06\00\00\00\04")
  (data $d17 (i32.const 1612) "\01")
  (data $d18 (i32.const 1628) "\ff\ff\ff\ff\0a")
  (data $d19 (i32.const 1696) "\10\06"))
