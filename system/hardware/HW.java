package system.hardware;

import system.core.Sistema;

public class HW {
    public Memory mem;
    public CPU cpu;
    public Sistema sistema; // referencia ao sistema para acesso a utilitarios

    public HW(int tamMem, Sistema sistema) {
        this.sistema = sistema;
        mem = new Memory(tamMem);
        cpu = new CPU(mem, true, sistema); // true liga debug
    }
}
