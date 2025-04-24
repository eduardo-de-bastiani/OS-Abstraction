package system.os;

import system.hardware.HW;

import java.util.Scanner;

public class SysCallHandling {
    private HW hw; // referencia ao hw se tiver que setar algo
    private Scanner in;

    public SysCallHandling(HW _hw) {
        hw = _hw;
        in = new Scanner(System.in);
    }

    public void stop() { // chamada de sistema indicando final de programa
        // nesta versao cpu simplesmente pára
        // vamos manter o print, pois o boolean cpuStop já encerra o programa
        System.out.println("                                               SYSCALL STOP");
    }

    public void handle() { // chamada de sistema
        // suporta somente IO, com parametros
        // reg[8] = in ou out e reg[9] endereco do inteiro
        System.out.println("SYSCALL pars:  " + hw.cpu.reg[8] + " / " + hw.cpu.reg[9]);

        if (hw.cpu.reg[8] == 1) {
            // leitura - le a entrada do teclado do usuario e guarda em reg[9]
            System.out.println("IN: Leitura do teclado (apenas valores inteiros): ");
            int input = in.nextInt();

            // armazena a entrada no reg[9]
            hw.mem.pos[hw.cpu.reg[9]].p = input;

        } else if (hw.cpu.reg[8] == 2) {
            // escrita - escreve o conteudo da memoria na posicao dada em reg[9]
            System.out.println("OUT:   " + hw.mem.pos[hw.cpu.reg[9]].p);
        } else {
            System.out.println("  PARAMETRO INVALIDO");
        }
    }
}
