package system.os;

import system.hardware.HW;
import system.software.InputDevice;
import system.software.PCB;

public class SysCallHandling {
    private HW hw; // referencia ao hw se tiver que setar algo

    public SysCallHandling(HW _hw) {
        hw = _hw;
    }

    public void stop() { // chamada de sistema indicando final de programa
        // nesta versao cpu simplesmente pára
        // vamos manter o print, pois o boolean cpuStop já encerra o programa
        System.out.println("                                               SYSCALL STOP");
    }

    public void handle() { // chamada de sistema
        //TODO: começa thread de IO
        //TODO: seta processo atual pra blocked

        //TODO: salva estado do processo atual e passa a mexer so nos regs dele, no lugar dos da cpu

        // suporta somente IO, com parametros
        // reg[8] = in ou out e reg[9] endereco do inteiro
        System.out.println("SYSCALL pars:  " + hw.cpu.reg[8] + " / " + hw.cpu.reg[9]);

        if (hw.cpu.reg[8] == 1) {
            PCB current = hw.sistema.so.pm.processRunning;

            hw.sistema.so.pm.setBlockedProcess(hw);

            // leitura - le a entrada do teclado do usuario e guarda em reg[9]
            // Cria uma thread para realizar a leitura de forma assíncrona

            Thread ioThread = new Thread(() -> {
                System.out.println("IN: Leitura do teclado (Use o comando IN, com o input em seguida): ");
                int input = InputDevice.getInstance().readFromQueue();
                hw.mem.pos[current.reg[9]].p = input; // armazena o valor lido na posicao de memoria indicada por reg[9]
                hw.sistema.so.pm.removeBlockedProcess(current);
                System.out.println("Entrada lida: " + input);
            });
            ioThread.start();

            // armazena a entrada no reg[9]
        } else if (hw.cpu.reg[8] == 2) {
            // escrita - escreve o conteudo da memoria na posicao dada em reg[9]
            System.out.println("OUT:   " + hw.mem.pos[hw.cpu.reg[9]].p);
        } else {
            System.out.println("  PARAMETRO INVALIDO");
        }

        //TODO: tira processo atual de blocked e coloca em ready
    }
}
