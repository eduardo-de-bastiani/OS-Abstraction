package system.software;

import java.util.ArrayList;
import java.util.List;

import system.hardware.HW;
import system.os.MemoryManager;

public class ProcessManager {
    public PCB processRunning;
    public List<PCB> processReady = new ArrayList<>(); // lista de processos prontos
    public List<PCB> processBlocked = new ArrayList<>(); // lista de processos bloqueados
    private int idCounter = 0;
    public MemoryManager memoryManager;
    public Scheduler Scheduler;
    private final Thread blockedManagerThread;

    //retorna a juncao de todas as listas de processos (bloqueando, pronto e rodando)
    private List<PCB> getAllProcesses() {
        List<PCB> all = new ArrayList<>();
        all.addAll(processReady);
        all.addAll(processBlocked);
        all.add(processRunning);
        return all;
    }
    
    public ProcessManager(int quantum) {
        this.Scheduler = new Scheduler(quantum);
        blockedManagerThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(3000); // tempo de espera entre verificações

                    synchronized (processBlocked) {
                        if (!processBlocked.isEmpty()) {
                            PCB p = processBlocked.remove(0);

                            synchronized (processReady) {
                                processReady.add(p);
                                System.out.println("[Desbloqueio] Processo " + p.pid + " movido para ready.");
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        blockedManagerThread.setDaemon(true); // encerra com o sistema
        blockedManagerThread.start(); // inicia a thread
    }

    public boolean createProcess(Program p) {
        int[] pageTable = memoryManager.aloca(p.image);
        
        idCounter++;
        PCB pcb = new PCB(idCounter, pageTable, p.name, memoryManager.pageSize);
    
        processReady.add(pcb);

        System.out.println("Processo " + p.name + " criado com PID: " + idCounter);
        return true;
    }

    public boolean removeProcess(int pid) {
        PCB pcb = getAllProcesses().stream()
                .filter(p -> p.pid == pid)
                .findFirst()
                .orElse(null);

        if (pcb == null) {
            System.out.println("PID " + pid + " não encontrado.");
            return false;
        }
        
        // 1) liberar memória
        memoryManager.desaloca(pcb.pageTable);

        // 2) retirar de filas de ready e blocked
        processReady.removeIf(p -> p.pid == pid);
        processBlocked.removeIf(p -> p.pid == pid);

        // 3) se for o que está rodando, encerra-o
        if (processRunning != null && processRunning.pid == pid) {
            processRunning = null;
        }
        
        System.out.println("Processo PID " + pid + " removido com sucesso.");
        return true;
    }

    public boolean setFirstProcessRunning() {
        if (processReady.isEmpty()) return false; // se não houver processos prontos, não faz nada
        this.processRunning = processReady.remove(0); // remove o primeiro da lista de prontos
        return true;
    }

    public void setBlockedProcess(HW hw) {
        this.processBlocked.add(processRunning);
        System.out.println("Processo " + processRunning.pid + " foi bloqueado.");
        Scheduler.handleQuantumInterrupt(hw);
    }
}
