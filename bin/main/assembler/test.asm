.text "Hello World"

ldca $a .text
pushi $a
call :print_string

jmp :exit
printi #11

:print_string
        popi $a
    :print_loop
        ifb &$a #0
        jmp :print_end_loop
        printc &$a
        addi $a #1
        jmp :print_loop
    :print_end_loop
        ret

:exit