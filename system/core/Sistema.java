package system.core;

import system.hardware.HW;
import system.os.MemoryManager;
import system.os.SO;
import system.software.Commands;
import system.software.ProcessManager;
import system.software.Programs;

public class Sistema {
    public int pageSize = 16;
    public HW hw;
    public SO so;
    public Programs progs;
    public MemoryManager mm;
    public ProcessManager pm;

    public Sistema(int tamMem) {
        hw = new HW(tamMem); // memoria do HW tem tamMem palavras
        so = new SO(hw);
        hw.cpu.setUtilities(so.utils); // permite cpu fazer dump de memoria ao avancar
        progs = new Programs();
        mm = new MemoryManager(pageSize, hw); // gerenciador de memoria
        pm = new ProcessManager(this); // gerenciador de processos
    }

    public void run() {

        //so.utils.loadAndExec(progs.retrieveProgram("fatorialV2"));
        Commands cmds = new Commands(progs, so);

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
