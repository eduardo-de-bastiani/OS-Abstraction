package system.os;

import system.hardware.HW;

public class InterruptHandling {
    private HW hw; // referencia ao hw se tiver que setar algo

    public InterruptHandling(HW _hw) {
        hw = _hw;
    }

    public void handle(Interrupts irpt) {
        // apenas avisa - todas interrupcoes neste momento finalizam o programa
        System.out.println("Interrupcao " + irpt + "   pc: " + hw.cpu.pc);
    }
}
