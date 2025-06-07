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
        processRunning.saveContext(hw.cpu.reg, hw.cpu.pc);
        this.processBlocked.add(processRunning);
        System.out.println("Processo " + processRunning.pid + " foi bloqueado.");
        this.processRunning = null;
        Scheduler.handleQuantumInterrupt(hw);
    }

    public void removeBlockedProcess(PCB pcb) {
        synchronized (processBlocked) {
            if (processBlocked.remove(pcb)) {
                synchronized (processReady) {
                    processReady.add(pcb);
                    System.out.println("Processo " + pcb.pid + " removido de bloqueado e movido para ready.");
                }
            }
        }
    }
}
