package system.software;

public class PCB {
    public final int pid; // id do
    // public final int ppid; // id do processo pai
    // public final int uid; // id do usuario que criou
    public int pc; // contador de programa
    public int[] reg = new int[10]; // registradores do processo, array de 10 posições
    public int status; // status do processo (running, ready, blocked) podemos realizar um ENUM
    public int priority;
    public int[] pageTable; // Alterado de Map para array de inteiros
    public String programName;  // nome do processo (programa)
    // public boolean allowInterrupt = true;

    // de alguma forma adicionar evento que o processo está
    // depois teremos que adicionar comunicação entre
    // informações de tempo executando e aguardando
    // recursos controlados pelo processo, como arquivos abertos

    public PCB(int _pid, int[] _pageTable, String _name) { // Alterado o tipo de _pageTable para int[]
        pid = _pid;
        pc = 0;
        for (int i = 0; i < reg.length; i++) {
            reg[i] = 0;
        }
        pageTable = _pageTable; // Atribuição direta do array
        status = 1; // 0 = running, 1 = ready, 2 = blocked
        priority = 0; // prioridade do processo
        programName = _name;
    }
}
