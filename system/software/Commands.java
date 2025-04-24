package system.software;

import system.hardware.Word;
import system.os.SO;

import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;

public class Commands {
    public interface Comando {
        public void execute(String[] args);
    }

    private Programs progs;
    private SO so;
    public Commands(Programs programs, SO so) {
        this.progs = programs;
        this.so = so;

    }

    private Comando newCmd = new Comando() {
        public void execute(String[] args) {
            String processName = args[0];
            Word[] program = progs.retrieveProgram(processName.strip());
            if (program == null) {
                System.out.println("Programa não encontrado: " + processName);

                for (Program p : progs.progs) {
                    System.out.println("Programa disponível: " + p.name);
                }

                return;
            }
            try {
                Map<Integer, Integer> pageTable = so.mm.jmAlloc(program);
                PCB newProcess = new PCB(so.mm.pages.length, pageTable, "sdkgsdhkgs");
                newProcess.programName = processName;
                so.utils.dump(0, program.length);
                so.utils.loadAndExec(program);
                System.out.println("Processo criado: " + processName);
            } catch (OutOfMemoryError e) {
                System.out.println("Erro ao criar processo: memória insuficiente.");
            }
        }
    };

    public void waitForCommands() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("Digite um comando: ");
            String typed = scanner.nextLine();
            String[] args = typed.split(" ");
            String command = args[0].toLowerCase(); // Comando em minúsculas para comparação
            args = Arrays.copyOfRange(args, 1, args.length); // Ignora o primeiro argumento (comando)
            if (command.equals("exit")) {
                System.out.println("Saindo do programa.");
                break;
            } else if (command.equals("new")) {
                newCmd.execute(args); // Executa o comando 1
            } else {
                System.out.println("Comando inválido. Tente novamente.");
            }
        }
        scanner.close(); // Fecha o scanner quando terminar
    }
}

