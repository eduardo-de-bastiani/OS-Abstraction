package system.os;

import system.hardware.HW;
import system.software.ProcessManager;
import system.utils.Utilities;

public class SO {
    public InterruptHandling ih;
    public SysCallHandling sc;
    public Utilities utils;
    public MemoryManager mm; 
    public ProcessManager pm;

    public SO(HW hw, int tamMem, int pageSize) {
        ih = new InterruptHandling(hw); // rotinas de tratamento de int
        sc = new SysCallHandling(hw); // chamadas de sistema
        hw.cpu.setAddressOfHandlers(ih, sc);
        mm = new MemoryManager(tamMem, pageSize, hw.mem);
        pm = new ProcessManager(mm);
        utils = new Utilities(hw, mm); // utilitários do sistema

    }
}
