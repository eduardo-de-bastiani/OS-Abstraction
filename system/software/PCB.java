package system.software;

public class PCB {
    public final int pid; // id do
    // public final int ppid; // id do processo pai
    // public final int uid; // id do usuario que criou
    public int pc; // contador de programa
    public int[] reg = new int[10]; // registradores do processo, array de 10 posições
    public int[][] pageTable; 
    //Page table, agora devera ser uma tabela, onde cada linha representa uma página logica do programa, a primeira coluna representa a memória pricipal e a segunda coluna representa a memória secundária, o numero armazenado será o frame fisico onde a página está alocada, ou -1 se não estiver alocada.
    // Exemplos
    // pageTable[0][0] = 2; // Página lógica 0 está no frame físico 2 da memória principal.
    // pageTable[0][1] = -1; // Página lógica 0 não está alocada na memória secundária.
    // pageTable[1][0] = 3; // Página lógica 1 está no frame físico 3 da memória principal.
    // pageTable[1][1] = 5; // Página lógica 1 está no frame físico 5 da memória secundária.
    
    public String programName;  // nome do processo (programa)
    // public boolean allowInterrupt = true;

    public PCB(int _pid, int[][] _pageTable, String _name, int pageSize) {
        pid = _pid;
        this.pc = 0;
        for (int i = 0; i < reg.length; i++) {
            reg[i] = 0;
        }
        pageTable = _pageTable; 
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
