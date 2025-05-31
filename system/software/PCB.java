package system.software;

import java.util.Arrays;

public class PCB {
    public final int pid; // id do processo
    public int pc; // contador de programa
    public int[] reg = new int[10]; // registradores do processo, array de 10 posições
    public int[] pageTable; // Tabela de páginas
    public boolean[] pagePresent; // Indica se a página está presente na memória (true) ou no disco (false)
    public int[] pageFrames; // Armazena o frame físico ou o endereço no disco
    public boolean[] pageAccessed; // Bit de acesso para algoritmo do relógio
    public String programName;  // nome do processo (programa)

    public PCB(int _pid, int[] _pageTable, String _name, int pageSize) {
        pid = _pid;
        this.pc = 0;
        for (int i = 0; i < reg.length; i++) {
            reg[i] = 0;
        }
        
        // Inicializa a tabela de páginas com informações estendidas
        pageTable = _pageTable;
        pagePresent = new boolean[_pageTable.length];
        pageFrames = new int[_pageTable.length];
        pageAccessed = new boolean[_pageTable.length];
        
        // Inicialmente, apenas a primeira página está presente na memória
        pagePresent[0] = true;
        pageFrames[0] = _pageTable[0]; // Frame da primeira página
        
        // As demais páginas não estão presentes (estão no disco)
        for (int i = 1; i < _pageTable.length; i++) {
            pagePresent[i] = false;
            pageFrames[i] = i; // Endereço no disco (simplificado)
            pageAccessed[i] = false;
        }
        
        programName = _name;
    }

    private void saveRegisters(int[] _reg) {
        for (int i = 0; i < reg.length; i++) {
            System.out.println("Saving register " + i + ": " + _reg[i]);
        }
        reg = _reg.clone();
    }

    private void savePC(int _pc) {
        pc = _pc;
    }

    public void saveContext(int[] _reg, int _pc) {
        saveRegisters(_reg);
        savePC(_pc);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PCB [pid=").append(pid)
          .append(", programName=").append(programName)
          .append(", pc=").append(pc)
          .append(", reg=").append(Arrays.toString(reg))
          .append("]\n");
        
        sb.append("Tabela de Páginas:\n");
        for (int i = 0; i < pageTable.length; i++) {
            sb.append("  Página ").append(i)
              .append(": Presente=").append(pagePresent[i])
              .append(", Frame/Disco=").append(pageFrames[i])
              .append(", Acessada=").append(pageAccessed[i])
              .append("\n");
        }
        
        return sb.toString();
    }
}
