- Na classe da memória, precisamos implementar os métodos de **alocar** processos
    - cada posição do programa é uma posição no array da memória
    - a saída deve ser uma tabela de páginas
    - cada palavra do programa deve ser **copiada** no frame da memória

- Na classe da memória, precisamos implementar os métodos de **desalocar** processos
    - tem como entrada a tabela de páginas
    - deve liberar os frames da memória alocada
        - podemos colocar null ou fazer como o gerenciador de memória real, indicando que os índices do array estão disponíveis para realocação de outro programa (sobrescrita)


- Na CPU, devemos implementar a função de Memory Mapping (MMU) para o nosso sistema executar o programa a partir das posições lógicas das instrução
    - a entrada da função de MMU é tabela de páginas e a posição lógica da instrução