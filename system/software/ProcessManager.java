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

    public void removeProcess(int id){
        
        // iterar sobre lista de processos prontos e remover o PCB == id
        for(int i = 0; i < processReady.size(); i++){
            if(id == processReady.get(i).pid){
                processReady.remove(i);
                break;
            }
        }

        // iterar sobre lista de processos bloqueados e remover o PCB == id
        for(int i = 0; i < processBlocked.size(); i++){
            if(id == processBlocked.get(i).pid){
                processBlocked.remove(i);
                break;
            }
        }

        // chamar metodo desaloca() com o map de pages do PCB que encontramos
        //memoryManager.desaloca()

    }

    public void scheduleNextProcess(Integer pid) {
        processRunning = scalonator.scheduleProcess(processRunning, processReady, pid);
    }
}
