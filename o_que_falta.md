- ProcessManagery
    - terminar metodo removeProcess
    
- classe Commands
    - implementar comandos
        - rm <id>
        - ps
        - dump <id>
        - dumpM <inicio, fim>
        - exec <id>
        - traceOn
        - traceOff
        - exit

- Na CPU, devemos implementar a função de Memory Mapping (MMU) para o nosso sistema executar o programa a partir das posições lógicas das instrução (feito?)
    - a entrada da função de MMU é tabela de páginas e a posição lógica da instrução

## Escalonamento com paginação e Round Robin

> consultar arquivo Sistema.java disponível no Discor=

### Chaveamento de Contexto
- na classe Sistema, instanciar semáforo de CPU e semáforo de Scheduler
- criar classe de Scheduler (instanciado pelo SO)
    - método roundRobin que é um while(true)
        - podemos implementar o relógio como uma thread em loop que após o quantum de tempo, gera uma interrupção
        - OU podemos implementar o relógio como um contador de ciclos da CPU
- criar função de handle na classe InterruptHandling com switch case para lidar com escalonador
    - switch case: tempo de RoundRobin, finalização de um processo e instrução inválida
- criar rotina de tratamento da interrupção do RoundRobin (relógio)
    - salvar o contexto (PC, registradores) em um campo no PCB
    - mudar o estado do processo de running para ready
    - escolher próximo processo