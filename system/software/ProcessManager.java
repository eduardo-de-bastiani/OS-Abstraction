package system.software;

import system.core.Sistema;
import system.os.MemoryManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProcessManager {
    public PCB processRunning;
    public List<PCB> processReady = new ArrayList<>(); // lista de processos prontos
    public List<PCB> processBlocked = new ArrayList<>(); // lista de processos bloqueados
    private int idCounter = 0;
    private Sistema sistema;
    private MemoryManager memoryManager;


    public ProcessManager(Sistema sistema) {
        this.sistema = sistema;
        this.memoryManager = new MemoryManager(sistema.pageSize, sistema.hw);
    }

    public boolean createProcess(Program p) {
        Map<Integer, Integer> pageTable = memoryManager.jmAlloc(p.image);
        idCounter++;
        PCB pcb = new PCB(idCounter, pageTable, p.name);
        processReady.add(pcb);
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

        // chamar metodo jmfree() com o map de pages do PCB que encontramos
        //memoryManager.jmFree()

    }
}
