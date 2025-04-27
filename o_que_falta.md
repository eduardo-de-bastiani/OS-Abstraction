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

## Escalonamento com Round Robin

> consultar arquivo Sistema.java disponível no Discord

1. Preparação de Threads e Semáforos

    - Na classe Sistema:

        - Instanciar um semáforo de CPU (para controlar acesso único à CPU).

        - Instanciar um semáforo do Scheduler (para sinalizar que houve interrupção por quantum ou fim de processo).

    - Garantir que a aplicação tenha, pelo menos, duas threads:

        - Thread de usuário: lê e executa comandos (create, remove, execAll etc.).

        - Thread de escalonamento: fica responsável por disparar o round-robin.

2. Criação da Classe Scheduler

    - Criar class Scheduler

    - No construtor, receber referência ao ProcessManager e ao semáforo de scheduler.

    - Implementar o método RoundRobin
        - Fica em loop infinito (while (true)), aguardando o semáforo do relógio
        - A cada liberação, dispara a rotina de troca de contexto


3. Implementação do “Relógio” (Quantum)

Duas abordagens:

    - Thread de relógio:

        Dentro de Scheduler, criar uma thread que:

            Dorme por quantum milissegundos.

            Após acordar, libera o semáforo de scheduler.

    - Contador de instruções: (mais fácil)

        No ciclo de execução de instruções da CPU, incrementar um contador.

        Quando atingir quantum, acionar manualmente a interrupção (liberar semáforo).


4. Manipulação de Interrupções

    - Na classe InterruptHandling:

        - Adicionar um switch para as causas de interrupção:

            TIMER (fim de quantum).

            STOP (processo executou instrução STOP).

            INVALID_INSTR (opcional, instrução inválida).


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


6. Rotina de Fim de Processo (handleStopInterrupt)

    - Quando receber interrupção STOP: 

        chamar removeProcess para:

            desalocar memória do PCB.

            remover PCB de todas as filas e do allProcesses (se existir).

    Escolher novo processo da fila de prontos e executar troca de contexto (pode chamar a mesma lógica de TIMER).


7. Comando execAll

    Adicionar em Sistema o comando execAll que:

        Verifica se existem processos na fila de prontos.

        Se sim, sinaliza a thread de escalonamento para iniciar o round-robin.

    Exibir no console:

        A cada quantum: qual PID saiu da CPU e qual entrou.

        Quando um processo termina: PID e status final.


### Chaveamento de Contexto
- na classe Sistema, instanciar semáforo de CPU e semáforo de Scheduler
- criar classe de Scheduler (instanciado pelo SO)
    - método roundRobin que é um while(true)
        - podemos implementar o relógio como uma thread em loop que após o quantum de tempo, gera uma interrupção
        - OU podemos implementar o relógio como um contador de ciclos da CPU
- criar função de handle na classe InterruptHandling com switch case para lidar com escalonador
    - switch case: tempo de RoundRobin, finalização de um processo e instrução inválida
- criar rotina de tratamento da interrupção do RoundRobin (relógio)
    1. salvar o contexto do processo (PC, registradores) em um campo no PCB
    2. mudar o estado do processo de running para ready
    3. escolher próximo processo a ser executado
    4. mudar seu estado de ready para running
    5. restaurar o contexto dele
    6. faz tudo de novo (loop)