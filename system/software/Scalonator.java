package system.software;

import java.util.List;

public class Scalonator {
    public Scalonator() {
        // Inicialização do escalonador
        System.out.println("Scalonator inicializado.");
    }

    // Métodos do escalonador podem ser adicionados aqui
    public PCB scheduleProcess(PCB runningProcess, List<PCB> readyProcesses, Integer pid) {
        if (runningProcess != null) {
            readyProcesses.add(runningProcess); // Adiciona o processo rodando ao final da fila de prontos
        }

        if (pid != null) {
            for (PCB pcb : readyProcesses) {
                if (pcb.pid == pid) {
                    readyProcesses.remove(pcb);
                    System.out.println("Processo com PID " + pid + " escalonado como running.");
                    return pcb;
                }
            }
            System.out.println("Processo com PID " + pid + " não encontrado na lista de prontos.");
        }

        if (!readyProcesses.isEmpty()) {
            PCB nextProcess = readyProcesses.remove(0); // Remove o primeiro processo da fila de prontos
            System.out.println("Processo com PID " + nextProcess.pid + " escalonado como running (FIFO).");
            return nextProcess;
        } else {
            System.out.println("Nenhum processo disponível na lista de prontos para escalonamento.");
            return null;
        }
    }
}