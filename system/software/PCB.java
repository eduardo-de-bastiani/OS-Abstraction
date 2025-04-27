package system.software;

public class PCB {
    public final int pid; // id do
    // public final int ppid; // id do processo pai
    // public final int uid; // id do usuario que criou
    public int pc; // contador de programa
    public int[] reg = new int[10]; // registradores do processo, array de 10 posições
    public int[] pageTable; // Alterado de Map para array de inteiros
    public String programName;  // nome do processo (programa)
    // public boolean allowInterrupt = true;

    public PCB(int _pid, int[] _pageTable, String _name) { // Alterado o tipo de _pageTable para int[]
        pid = _pid;
        pc = 0;
        for (int i = 0; i < reg.length; i++) {
            reg[i] = 0;
        }
        pageTable = _pageTable; // Atribuição direta do array
        programName = _name;
    }

    private void saveRegisters(int[] _reg) {
        reg = _reg.clone();
    }

    private void savePC(int _pc) {
        pc = _pc;
    }

    public void saveContext(int[] _reg, int _pc) {
        saveRegisters(_reg);
        savePC(_pc);
    }
}
