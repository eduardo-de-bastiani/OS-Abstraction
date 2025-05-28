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

            case intInstrucaoInvalida -> {
                System.out.println("Handling intInstrucaoInvalida interrupt");
                hw.sistema.so.sca.handleStopInterrupt(hw);
            }
            
            case intPageFault -> {
                System.out.println("Handling intPageFault interrupt");
                hw.sistema.so.mm.handlePageFault(hw.cpu.reg[8]); // Reg[8] contém o endereço lógico que causou o page fault
            }
            
            case intIOComplete -> {
                System.out.println("Handling intIOComplete interrupt");
                hw.sistema.so.mm.handleIOComplete();
            }
            
            case intDiskSaveComplete -> {
                System.out.println("Handling intDiskSaveComplete interrupt");
                hw.sistema.so.mm.handleDiskSaveComplete();
            }
            
            case intDiskLoadComplete -> {
                System.out.println("Handling intDiskLoadComplete interrupt");
                hw.sistema.so.mm.handleDiskLoadComplete();
            }

            default -> System.out.println("Unknown interrupt");
        }

        System.out.println("Interrupcao " + irpt + "   pc: " + hw.cpu.pc);
    }
}
