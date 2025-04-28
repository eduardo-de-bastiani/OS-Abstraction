package system.software;

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

}

    






//como usar
// Scheduler scheduler = new Scheduler(5); // por exemplo, quantum de 5 instruções
// Thread schedulerThread = new Thread(scheduler);
// schedulerThread.start();

// CPU cpu = new CPU(mem, debug, sistema, scheduler);
// Thread cpuThread = new Thread(cpu);
// cpuThread.start();
