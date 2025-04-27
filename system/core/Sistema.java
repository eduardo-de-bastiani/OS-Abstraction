package system.core;

import system.hardware.HW;
import system.os.SO;
import system.software.Commands;
import system.software.Programs;

public class Sistema {
    public HW hw;
    public SO so;
    public Programs progs;

    public Sistema(int tamMem, int pageSize, int quantum) {
        hw = new HW(tamMem, this); // memoria do HW tem tamMem palavras
        so = new SO(hw, tamMem, pageSize, quantum);
        hw.cpu.mm = so.mm; // mm do HW é a mesma do SO
        hw.cpu.setUtilities(so.utils); // permite cpu fazer dump de memoria ao avancar
        progs = new Programs();
    }

    public void run() {

        //so.utils.loadAndExec(progs.retrieveProgram("fatorialV2"));
        Commands cmds = new Commands(progs, this);

        cmds.waitForCommands();

        // so.utils.loadAndExec(progs.retrieveProgram("fatorial"));
        // fibonacci10,
        // fibonacci10v2,
        // progMinimo,
        // fatorialWRITE, // saida
        // fibonacciREAD, // entrada
        // PB
        // PC, // bubble sort
    }
}
