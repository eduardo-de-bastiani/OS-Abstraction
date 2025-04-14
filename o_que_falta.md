- MemoryManager 
    - trocar a saída do JmAlloc para lista de índices de páginas

- ProcessManager
    - corrigir metodo createProcess (nao podemos criar um novo MemoryManager para cada processo)
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

- Na CPU, devemos implementar a função de Memory Mapping (MMU) para o nosso sistema executar o programa a partir das posições **físicas** das instrução
    - a entrada da função de MMU é tabela de páginas e a posição lógica da instrução