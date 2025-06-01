# About
Implementation of the first pass of the two-pass assembler in Java.  

<b>Pass 1</b> generates:
- Literal table
- Symbol table  
- Pool table
- Intermediate code

A few assumptions are made for the assembly code input. Read them [here](https://github.com/athkarandikar/two-pass-assembler-pass-one/blob/main/assumptions.txt).

# Input and Output
Input is an assembly file (ending with asm extension) located in the [input](https://github.com/athkarandikar/two-pass-assembler-pass-one/blob/main/input) directory.  
Output consists of 4 files in the [output](https://github.com/athkarandikar/two-pass-assembler-pass-one/blob/main/output) directory: literal table, symbol table, pool table, and intermediate code.

### Sample Input

```assembly
START 200
MOVER AREG, ='5'
MOVEM AREG, A
LOOP MOVER AREG, A  
MOVER CREG, B  
ADD CREG, ='1'
BC ANY, NEXT
LTORG
NEXT SUB AREG, ='1'
BC LT, BACK
LAST STOP
ORIGIN LOOP+2
MULT CREG, B
ORIGIN LAST+1
A DS 1
BACK EQU LOOP
B DS 1
END
    ='1'
```

### Sample Output
1. #### Literal Table (literal_table.txt)
```
Literal Table:
ID   Literal    Address
1    ='5'       206    
2    ='1'       207    
3    ='1'       213

```

2. #### Symbol Table (symbol_table.txt)
```
Symbol Table:
ID   Symbol     Address
1    A          211    
2    LOOP       202    
3    B          212    
4    NEXT       208    
5    BACK       202    
6    LAST       210    
 
```

3. #### Pool Table (pool_table.txt)
```
Pool Table:
ID   Lit. ID  Pool Length
1    1        2          
2    3        1          

```

4. #### Intermediate Code (intermediate_code.txt)
```
(AD, 1) (C, 200) 
(IS, 4) (1) (L, 1) 
(IS, 5) (1) (S, 1) 
(IS, 4) (1) (S, 1) 
(IS, 4) (3) (S, 3) 
(IS, 1) (3) (L, 2) 
(IS, 7) (CC, 6) (S, 4) 
(AD, 5) 
(IS, 2) (1) (L, 3) 
(IS, 7) (CC, 2) (S, 5) 
(IS, 0) 
(AD, 3) 
(IS, 3) (3) (S, 3) 
(AD, 3) 
(DL, 1) (C, 1) 
(AD, 4) (S, 2) 
(DL, 1) (C, 1) 
(AD, 2) 
```
