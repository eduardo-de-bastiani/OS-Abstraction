# Abstrações e Convenções das Operações

___

## Nossa CPU
- Um contador de programa (PC - program counter)
- Um registrador de instruções (IR - instruction register)
- oito registradores, de 0 a 7

___

### registradores R8 R9
- usados **apenas** para chamada de sistema

___

## Rs, Rd, Ra, Rb, Rc, P
- registrador Source
- registrador Destiny
- registrador A
- registrador B
- registrador Comparação
- parâmetro
	- K = constante
	- A = adress
___
## [A]
endereço
___
## OPCODE
instruções de chamada de sistema

### Stop:
todo programa tem uma instrução de **Stop**, que para o processo corrente

### SYSCALL: 
Desvia para sistema

### DATA:
nessa posição vai ter dado

### JMP:
> <span style="color:green">JMP k</span>

atribui o valor pulado ao program counter, que vai ser passado pelo parâmetro (**P** na tabela)


### JMPI:
> <span style="color:green">JMPI Ra</span>

Coloca o valor do registrador no program counter


### JMPIG: *(Greater)*
> <span style="color:green">JMPIG Ra, Rb</span>

Se Rb > 0, então o PC recebe Ra. Senão, PC++


### JMPIL: *(Less)*
> <span style="color:green">JMPIL Ra, Rb</span>

Se Rb < 0, então PC recebe Ra. Senão PC++


### JMPIE: *(Equals)*
> <span style="color:green">JMPIE Ra, Rb</span>

Se Rb == 0, então PC recebe Ra. Senão PC++


### JMPIM:
> <span style="color:green">JMPIM [A]</span>

Vai ao condicional de memória, pega seu valor e coloca no program counter


### JMPIGM:
> <span style="color:green">JMPIGM [A], Rb</span>

Se Rb > 0, então PC recebe [A]. Senão PC++


### JMPILM:
> <span style="color:green">JMPILM [A], Rb</span>

Se Rb < 0, então PC recebe [A]. Senão PC++


### JMPIEM:
> <span style="color:green">JMPIEM [A], Rb</span>

Se Rb == 0, então PC recebe [A]. Senão PC++


### JMPIGT:
> <span style="color:green">JMPIGT k, Rb, Ra</span>

Se Ra > Rb, então PC recebe k. Senão PC++


### JMPIGK:
> <span style="color:green">JMPIGK k, Rb</span>

Se Rb > 0, então PC recebe k. Senão PC++

### JMPILK:
> <span style="color:green">JMPILK k, Rb</span>

Se Rb < 0, então PC recebe k. Senão PC++

### JMPIEK:
> <span style="color:green">JMPIEK k, Rb</span>

Se Rb == 0, então PC recebe k. Senão PC++

---
### ADDI:
> <span style="color:red">ADDI Ra, k</span>

Ra = Ra + k

### SUBI:
> <span style="color:red">SUBI Ra, k</span>

Ra = Ra - k

### ADD:
> <span style="color:red">ADD Ra, Rb</span>

Ra = Ra + Rb

### SUB:
> <span style="color:red">SUB Ra, Rb</span>

Ra = Ra - Rb

### MULT:
> <span style="color:red">MULT Ra, Rb</span>

Ra = Ra * Rb

---
### LDI:(Load)
> <span style="color:blue">LID Ra, k</span>

Ra = k

### LDD:
> <span style="color:blue">LDD Ra, [A]</span>

Ra = [A]

### STD: (Store)
> <span style="color:blue">STD [A], Ra</span>

[A] = Ra

### LDX: (indirect load)
> <span style="color:blue">LDX Ra, [Rb]</span>

Ra = [Rb]

### STX: (indirect store)
> <span style="color:blue">STX [Ra], Rb</span>

[Ra] = Rb

### MOVE:
> <span style="color:blue">MOVE Ra, Rb</span>

Ra = Rb

___


### Código em Java

//valores -1 indicam campos não usados pela instrução

//				      Ra, Rb, P(k/A) 
new Word(Opcode.LDI,		0, -1, 4),
new Word(Opcode.LDI,		1, -1, 1),
new Word(Opcode.LDI,		2, -1, 6),