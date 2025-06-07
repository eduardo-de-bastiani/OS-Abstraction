package system.software;

import system.core.Sistema;
import system.hardware.HW;

public class Scheduler implements Runnable {

    public final int QUANTUM_ROUNDS; // Quantidade de ciclos até gerar interrupção
    private int quantumCounter = 0; // Contador de quantums
    private boolean running = true;

    public Scheduler(int quantumRounds) {
        this.QUANTUM_ROUNDS = quantumRounds; // Inicializa a quantidade de ciclos
        // Inicialização do escalonador
        System.out.println("Scheduler inicializado.");
    }
   
    public synchronized boolean notifyInstructionExecuted() {
        quantumCounter++;
        if (quantumCounter >= QUANTUM_ROUNDS) {
            quantumCounter = 0;
            System.out.println("[Scheduler] Quantum completo! Gerando interrupção...");
            // Aqui você poderia setar alguma flag, ou fazer algo mais sofisticado
            // Ex: sinalizar o sistema operacional, escalar outro processo, etc.
            return true; // Indica que o quantum foi completado
        }

        return false; // Indica que o quantum não foi completado
    }

    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(100); // Evita busy-wait
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[Scheduler] Thread interrompida.");
            }
        }
    }

    public void stopScheduler() {
        running = false;
    }

    public void quantumCompleted() {
        quantumCounter = 0; // Reinicia o contador de quanta
    }

    public void handleStopInterrupt(HW hw){
        //zerar o quantum counter do scheduler
        this.quantumCompleted();

        //remove o processo em execução
        hw.sistema.so.pm.removeProcess(hw.sistema.so.pm.processRunning.pid);

        //chama o scheduler para pegar o próximo processo
        if (!hw.sistema.so.pm.processReady.isEmpty()) {
            PCB proximoProcesso = hw.sistema.so.pm.processReady.remove(0);
            hw.sistema.so.pm.processRunning = proximoProcesso;
            hw.cpu.pc = proximoProcesso.pc;
            hw.cpu.reg = proximoProcesso.reg.clone();
        } else {
            // Se não houver mais processos prontos, parar a CPU
            //hw.cpu.cpuStop = true;
        }
    }

    public void handleQuantumInterrupt(HW hw){
        // Obter o sistema a partir do hardware
        Sistema sistema = hw.sistema;

        // Atualizar o PCB do processo em execução com o contexto da CPU
        PCB processoAtual = sistema.so.pm.processRunning;
        if (processoAtual != null) {
            processoAtual.saveContext(hw.cpu.reg, hw.cpu.pc);
            // Mover o processo em execução para o final da lista de prontos
            sistema.so.pm.processReady.add(processoAtual);
        }

        if (sistema.so.pm.processReady.isEmpty()) {
            return;
        }

        // Selecionar o próximo processo da lista de prontos para execução
        PCB proximoProcesso = sistema.so.pm.processReady.remove(0);
        if (proximoProcesso != null) {
            // Atualizar a CPU com o contexto do próximo processo
            hw.cpu.pc = proximoProcesso.pc;
            hw.cpu.reg = proximoProcesso.reg.clone();

            // Definir o próximo processo como o processo em execução
            sistema.so.pm.processRunning = proximoProcesso;
        }
        //printa o processo em execução
        if (sistema.so.pm.processRunning == null) return;

        if (sistema.so.pm.processRunning == null) {
            System.out.println("Nenhum processo em execução.");
            return;
        }
        System.out.println("Processo em execução: " + sistema.so.pm.processRunning.pid + " - " + sistema.so.pm.processRunning.programName);
    }

    //ToDO: handleInvalidInstructionInterrupt(HW hw)

}

    






//como usar
// Scheduler scheduler = new Scheduler(5); // por exemplo, quantum de 5 instruções
// Thread schedulerThread = new Thread(scheduler);
// schedulerThread.start();

// CPU cpu = new CPU(mem, debug, sistema, scheduler);
// Thread cpuThread = new Thread(cpu);
// cpuThread.start();
