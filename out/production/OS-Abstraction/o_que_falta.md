- MemoryManager 
    - trocar a saída do JmAlloc para lista de índices de 

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

- Na CPU, devemos implementar a função de Memory Mapping (MMU) para o nosso sistema executar o programa a partir das posições lógicas das instrução
    - a entrada da função de MMU é tabela de páginas e a posição lógica da instrução