package system.software;

import java.util.HashMap;
import java.util.Map;

public class PCB {
    public final int pid; // id do
    // public final int ppid; // id do processo pai
    // public final int uid; // id do usuario que criou
    public int pc; // contador de programa
    public int[] reg = new int[10]; // registradores do processo, array de 10 posições
    public int status; // status do processo (running, ready, blocked) podemos realizar um ENUM
    public int priority;
    Map<Integer, Integer> pageTable = new HashMap<>();
    public String programName;
    public String name; // nome do processo (programa)
    // public boolean allowInterrupt = true;

    // de alguma forma adicionar evento que o processo está
    // depois teremos que adicionar comunicação entre
    // informações de tempo executando e aguardando
    // recursos controlados pelo processo, como arquivos abertos

    public PCB(int _pid, Map<Integer, Integer> _pageTable, String _name) { // pid é o id do processo
        pid = _pid;
        pc = 0;
        for (int i = 0; i < reg.length; i++) {
            reg[i] = 0;
        }
        name = _name;
        Map<Integer, Integer> pageTable = _pageTable; //TODO CLONAR
        status = 1; // 0 = running, 1 = ready, 2 = blocked
        priority = 0; // prioridade do processo
        programName = _name;

    }
}
