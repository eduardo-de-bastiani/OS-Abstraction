package system.core;

import system.hardware.HW;
import system.software.Commands;
import system.software.Programs;

public class Sistema {
    public HW hw;
    public system.os.SO so;
    public Commands cmd;
    public Programs progs;

    public Sistema(int tamMem, int pageSize, int quantum) {
        hw = new HW(tamMem, this);
        so = new system.os.SO(hw, tamMem, pageSize, quantum);
        progs = new Programs();
        cmd = new Commands(progs, this);
    }

    public void run() {
        cmd.waitForCommands();
    }
}
