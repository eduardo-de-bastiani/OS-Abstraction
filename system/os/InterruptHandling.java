package system.os;

import system.hardware.HW;

public class InterruptHandling {
    private HW hw; // referencia ao hw se tiver que setar algo

    public InterruptHandling(HW _hw) {
        hw = _hw;
    }

    public void handle(Interrupts irpt) {
        switch (irpt) {
            case intSTOP -> {
                System.out.println("Handling intSTOP interrupt");
                hw.sistema.so.sca.handleStopInterrupt(hw);
            }
            
            case quantumTime -> {
                System.out.println("Handling quantumTime interrupt");
                hw.sistema.so.sca.handleQuantumInterrupt(hw);
            }

            case intInstrucaoInvalida -> System.out.println("Handling intInstrucaoInvalida interrupt");
            // Add logic to handle intInstrucaoInvalida

            default -> System.out.println("Unknown interrupt");
        }

        System.out.println("Interrupcao " + irpt + "   pc: " + hw.cpu.pc);
    }
}
