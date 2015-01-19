# "Comprehensive" test program for ARES simulator.
# Note: even with delayed branching turned on, I don't think this runs correctly on MARS.
# The addi after j address2 is executed, as is the ori to $a1.


.data
store1: .space 4
.text
la $t1, address1
sw $t1, store1
jr $t1 #first this jump will occur,
j address2 #then, immediately afterwards, this one will too
addi $t1, $t1, 1 #this instruction will not be executed
address1:
ori $v0, 10 #this instruction will be executed
ori $a1, 20 #this instruction will not be executed, because of jump #2
address2:
ori $a2, 30
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
nop
nop
nop
nop
mflo $v0 #stall for 8 (12 cyc. delay - 4 nops) cycles