- ProcessManagery
    - terminar metodo removeProcess ✅
    
- classe Commands ✅
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
    - a entrada da função de MMU é tabela de páginas e a posição lógica da  ✅

## Escalonamento com Round Robin  (Verificar a carga da memória)

> consultar arquivo Sistema.java disponível no Discord

1. Preparação de Threads e Semáforos

    - Na classe Sistema:

        - Instanciar um semáforo de CPU (para controlar acesso único à CPU).

        - Instanciar um semáforo do Scheduler (para sinalizar que houve interrupção por quantum ou fim de processo).

    - Garantir que a aplicação tenha, pelo menos, duas threads:

        - Thread de usuário: lê e executa comandos (create, remove, execAll etc.).

        - Thread de escalonamento: fica responsável por disparar o round-robin.

2. Criação da Classe Scheduler

    - Criar class Scheduler ✅

    - Implementar o método RoundRobin ✅

        - Fica em loop infinito (while (true)), aguardando o semáforo do relógio
        - A cada liberação, dispara a rotina de troca de contexto
    
    - Passar lógica das interrupções para o Scheduler


3. Implementação do “Relógio” (Quantum) ✅

Duas abordagens:

    - Thread de relógio: (não realizado)

        Dentro de Scheduler, criar uma thread que:

            Dorme por quantum milissegundos.

            Após acordar, libera o semáforo de scheduler.

    - Contador de instruções: (mais fácil) ✅

        No ciclo de execução de instruções da CPU, incrementar um contador. ✅

        Quando atingir quantum, acionar manualmente a interrupção (liberar semáforo). ✅


4. Manipulação de Interrupções

    - Na classe InterruptHandling:

        - Adicionar um switch para as causas de interrupção:

            TIMER (fim de quantum). ✅

            STOP (processo executou instrução STOP). ✅

            INVALID_INSTR (opcional, instrução inválida). A Fazer


5. Rotina de troca de contexto:

- A rotina de tratamento do fim de quantum deve:

    Salvar contexto do processo atual:

        PC, registradores → armazenar no próprio PCB.

    Atualizar estado
        RUNNING → READY, e adicioná-lo ao fim da fila de prontos.

    Selecionar próximo PCB da fila de prontos (FIFO).

    Atualizar estado
        READY → RUNNING.

    Restaurar contexto (PC, registradores) na CPU

    Liberar semáforo de CPU para que a thread de execução continue.


6. Rotina de Fim de Processo (handleStopInterrupt) ✅

    - Quando receber interrupção STOP: 

        chamar removeProcess para:

            desalocar memória do PCB.

            remover PCB de todas as filas e do allProcesses (se existir).

    Escolher novo processo da fila de prontos e executar troca de contexto (pode chamar a mesma lógica de TIMER).


7. Comando execAll -> o sor provavelmente vai querer ver essa execução!! 

    - Adicionar em Sistema o comando execAll que:

        Verifica se existem processos na fila de prontos.

        Se sim, sinaliza a thread de escalonamento para iniciar o round-robin.

    - Exibir no console:

        A cada quantum: qual PID saiu da CPU e qual entrou.

        Quando um processo termina: PID e status final.


8. Execução Contínua

    - Modificar o loop principal de leitura de comandos de forma não-bloqueante:

        A thread de escalonamento fica livre para disparar interrupts mesmo enquanto o usuário digita.

    - Garantir sincronização entre:

        Thread de comandos (criando/removendo PCBs).

        Thread de escalonamento (acessando filas e semáforos).


#### Movimentação dos processos para fila de bloqueados automaticamente

- quando um processo solicita acesso a um dispositivo de I/O, devem ocorrer as seguintes ações: 
1. O processo deve ser movido para bloqueado.
2. Outro processo deve ser escalonado.
3. O pedido de I/O deve ser despachado para fila do dispositivo e a operação realizada no mesmo.

As ações devem ser realizadas concorrentemente, com **multithreading**

#### Interrupção de finalização de I/O

- Devemos criar uma variável na CPU para que o dispositivo de I/O sinalize a interrupção.
- O processo que estava bloqueado deve ser movido para a fila de prontos para ser escalonado novamente.
- O processo que foi interrompido também volta para a fila de prontos.

#### Criação do DMA

- As operações de I/O são realizadas **sem envolver a CPU**
- precisaremos de uma Thread para o DMA fazer leitura e escrita **somente na memória principal**
- DMA é concorrente com a CPU. Executa pedidos de I/O enquanto a CPU executa instruções de outros processos.