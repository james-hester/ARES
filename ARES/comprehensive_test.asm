# "Comprehensive" test program for ARES simulator.
.ktext 

eret


.data
store1: .space 4
.text

lw $a3, store1 #stall should occur here (lwstall)
addi $a3, $a3, 10 #stall should also occur here (branchstall)
beq $a3, $a2, address2
# Adapted from Patterson and Hennessy's example of
# "a sequence with many dependencies" (page 304, CO&D 5e).
# Should use all four forwarding paths.
sll $v0, $v0, 1
sub $v0, $v0, $a1
or $v1, $v0, $a2
and $v0, $t1, $v1
add $v1, $v0, $v0
and $v0, $v1, $v1
# Test multiplier unit, and stalls on mflo/hi.
addi $v0,$v0, 200
div $v0, $v1
mflo $v0 #stall for 35 cycles
mfhi $v1 #no stall
add $v0, $v0, $v1
add $v0, $v0, $a1
mult $v0, $v0
address2:
nop
nop
nop
nop
mflo $v0 #stall for 8 (12 cyc. delay - 4 nops) cycles
