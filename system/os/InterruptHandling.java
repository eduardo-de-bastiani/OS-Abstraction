package system.os;

import system.core.Sistema;
import system.hardware.HW;
import system.software.PCB;

public class InterruptHandling {
    private HW hw; // referencia ao hw se tiver que setar algo

    public InterruptHandling(HW _hw) {
        hw = _hw;
    }

    public void handle(Interrupts irpt) {
        switch (irpt) {
            case Interrupts.intSTOP -> {
                System.out.println("Handling intSTOP interrupt");
                //zerar o quantum counter do scheduler
                hw.sistema.so.sca.quantumCompleted();
                //remove o processo em execução
                hw.sistema.so.pm.removeProcess(hw.sistema.so.pm.processRunning.pid);
                //chama o scheduler para pegar o próximo processo
                if (!hw.sistema.so.pm.processReady.isEmpty()) {
                    PCB proximoProcesso = hw.sistema.so.pm.processReady.remove(0);
                    hw.sistema.so.pm.processRunning = proximoProcesso;
                    hw.cpu.pc = proximoProcesso.pc;
                    hw.cpu.reg = proximoProcesso.reg.clone();
                } else {
                    // Se não houver mais processos prontos, parar a CPU
                    hw.cpu.cpuStop = true;
                }
            }
            
            case Interrupts.quantumTime -> {
                System.out.println("Handling quantumTime interrupt");
                // Obter o sistema a partir do hardware
                Sistema sistema = hw.sistema;

                // Atualizar o PCB do processo em execução com o contexto da CPU
                PCB processoAtual = sistema.so.pm.processRunning;
                if (processoAtual != null) {
                    processoAtual.saveContext(hw.cpu.reg, hw.cpu.pc);
                    // Mover o processo em execução para o final da lista de prontos
                    sistema.so.pm.processReady.add(processoAtual);
                }

                // Selecionar o próximo processo da lista de prontos para execução
                PCB proximoProcesso = sistema.so.pm.processReady.remove(0);
                if (proximoProcesso != null) {
                    // Atualizar a CPU com o contexto do próximo processo
                    hw.cpu.pc = proximoProcesso.pc;
                    hw.cpu.reg = proximoProcesso.reg.clone();

                    // Definir o próximo processo como o processo em execução
                    sistema.so.pm.processRunning = proximoProcesso;
                }
                //printa o processo em execução
                System.out.println("Processo em execução: " + sistema.so.pm.processRunning.pid + " - " + sistema.so.pm.processRunning.programName);
            }

            case Interrupts.intInstrucaoInvalida -> System.out.println("Handling intInstrucaoInvalida interrupt");
            // Add logic to handle intInstrucaoInvalida

            default -> System.out.println("Unknown interrupt");
        }

        System.out.println("Interrupcao " + irpt + "   pc: " + hw.cpu.pc);
    }
}
