package system.software;

import java.util.ArrayList;
import java.util.List;

import system.os.MemoryManager;

public class ProcessManager {
    public PCB processRunning;
    public List<PCB> processReady = new ArrayList<>(); // lista de processos prontos
    public List<PCB> processBlocked = new ArrayList<>(); // lista de processos bloqueados
    private int idCounter = 0;
    public MemoryManager memoryManager;
    private Scalonator scalonator;

    private List<PCB> getAllProcesses() {
        List<PCB> all = new ArrayList<>();
        all.addAll(processReady);
        all.addAll(processBlocked);
        all.add(processRunning);
        return all;
    }
    public ProcessManager() {
        this.scalonator = new Scalonator();
    }

    public boolean createProcess(Program p) {
        int[] pageTable = memoryManager.aloca(p.image);
        
        idCounter++;
        PCB pcb = new PCB(idCounter, pageTable, p.name);
    
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

    public void scheduleNextProcess(Integer pid) {
        processRunning = scalonator.scheduleProcess(processRunning, processReady, pid);
    }
}
