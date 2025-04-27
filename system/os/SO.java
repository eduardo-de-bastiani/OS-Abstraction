package system.os;

import system.hardware.HW;
import system.software.ProcessManager;
import system.software.Scalonator;
import system.utils.Utilities;

public class SO {
    public InterruptHandling ih;
    public SysCallHandling sc;
    public Utilities utils;
    public MemoryManager mm; 
    public ProcessManager pm;
    public Scalonator sca;

    public SO(HW hw, int tamMem, int pageSize, int quantum) {
        ih = new InterruptHandling(hw); // rotinas de tratamento de int
        sc = new SysCallHandling(hw); // chamadas de sistema
        hw.cpu.setAddressOfHandlers(ih, sc);
        pm = new ProcessManager(quantum);
        sca = pm.scalonator;
        mm = new MemoryManager(tamMem, pageSize, hw.mem, pm);
        pm.memoryManager = mm; // gerenciador de processos tem acesso ao mm
        utils = new Utilities(hw, mm); // utilitários do sistema

    }
}
