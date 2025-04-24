package system.os;

import system.hardware.HW;
import system.utils.Utilities;

public class SO {
    public InterruptHandling ih;
    public SysCallHandling sc;
    public Utilities utils;
    public MemoryManager mm;

    public SO(HW hw) {
        ih = new InterruptHandling(hw); // rotinas de tratamento de int
        sc = new SysCallHandling(hw); // chamadas de sistema
        hw.cpu.setAddressOfHandlers(ih, sc);
        mm = new MemoryManager(16, hw);
        utils = new Utilities(hw, mm); // utilitários do sistema

    }
}
