- Na classe da MemoryManager, precisamos implementar os métodos de **alocar** processos ✅
    - cada posição do programa é uma posição no array da memória
    - a saída deve ser uma tabela de páginas
    - cada palavra do programa deve ser **copiada** no frame da memória

- Na classe da MemoryManager, precisamos implementar os métodos de **desalocar** processos ✅
    - tem como entrada a tabela de páginas
    - deve liberar os frames da memória alocada
        - indicamos que os índices do array estão disponíveis para realocação de outro programa (sobrescrita)


- Na CPU, devemos implementar a função de Memory Mapping (MMU) para o nosso sistema executar o programa a partir das posições lógicas das instrução
    - a entrada da função de MMU é tabela de páginas e a posição lógica da instrução

- implementar classe ProcessManager
    - dentro tem o PCB (Process Control Block)
    - tem id e tabela de processos