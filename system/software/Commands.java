package system.software;

import java.util.*;
import system.hardware.Word;
import system.os.SO;
import system.core.Sistema;

/**
 * Gerencia os comandos de usuário para o SO fictício.
 */
public class Commands {
    @FunctionalInterface
    public interface Comando {
        void execute(String[] args);
    }

    private final Programs progs;
    private final Sistema sys;
    private final Map<String, Comando> commands = new HashMap<>();

    public Commands(Programs programs, Sistema sys) {
        this.progs = programs;
        this.sys = sys;

        commands.put("new",      this::cmdNew);
        commands.put("rm",       this::cmdRm);
        commands.put("ps",       this::cmdPs);
        commands.put("dump",     this::cmdDump);
        commands.put("dumpM",    this::cmdDumpM);
        commands.put("exec",     this::cmdExec);
        commands.put("traceOn",  args -> cmdTraceOn());
        commands.put("traceOff", args -> cmdTraceOff());
        commands.put("help",     args -> cmdHelp());
    }

    public void waitForCommands() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Bem-vindo ao SO fictício. Digite 'help' para ver os comandos.");
        while (true) {
            System.out.print("SO> ");
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) continue;

            String[] tokens = line.split("\\s+");
            String cmd = tokens[0].toLowerCase();
            String[] args = Arrays.copyOfRange(tokens, 1, tokens.length);

            if ("exit".equals(cmd)) {
                System.out.println("Saindo do sistema.");
                break;
            }

            Comando action = commands.get(cmd);
            if (action != null) {
                action.execute(args);
            } else {
                System.out.println("Comando inválido: '" + cmd + "'. Digite 'help' para listar comandos.");
            }
        }
        scanner.close();
    }

    private void cmdNew(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: new <programName>");
            return;
        }
        String name = args[0].strip();
        Word[] image = progs.retrieveProgram(name);
        if (image == null) {
            System.out.println("Programa não encontrado: " + name);
            System.out.println("Programas disponíveis:");
            for (Program p : progs.progs) {
                System.out.println("  - " + p.name);
            }
            return;
        }
        try {
            PCB ok = sys.so.pm.createProcess(new Program(name, image));
            if (ok != null) {
                System.out.println("Processo " + name + " criado com sucesso com pid " + ok.pid);
            }
        } catch (OutOfMemoryError e) {
            System.out.println("Erro ao criar processo: memória insuficiente.");
        }
    }

    private void cmdRm(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: rm <pid>");
            return;
        }
        try {
            int pid = Integer.parseInt(args[0]);
            sys.so.pm.removeProcess(pid);
            System.out.println("Processo " + pid + " removido do sistema.");
        } catch (NumberFormatException e) {
            System.out.println("PID inválido: " + args[0]);
        }
    }

    private void cmdPs(String[] args) {
        System.out.println("=== Processos Prontos ===");
        for (PCB pcb : sys.so.pm.processReady) {
            System.out.printf("PID: %d\tpid: %s%n", pcb.pid, pcb.pid);
        }
        System.out.println("=== Processos Bloqueados ===");
        for (PCB pcb : sys.so.pm.processBlocked) {
            System.out.printf("PID: %d\tpid: %s%n", pcb.pid, pcb.pid);
        }
        if (sys.so.pm.processRunning != null) {
            System.out.println("=== Executando ===");
            PCB run = sys.so.pm.processRunning;
            System.out.printf("PID: %d\tpid: %s%n", run.pid, run.pid);
        }
    }

    private void cmdDump(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: dump <pid>");
            return;
        }
        try {
            int pid = Integer.parseInt(args[0]);
            PCB pcb = findPCB(pid);
            if (pcb == null) {
                System.out.println("Processo não encontrado: " + pid);
                return;
            }
            System.out.println(pcb);
            
            // Exibe memória de todas as páginas alocadas ao processo
            if (pcb.pageTable != null && pcb.pageTable.length > 0) {
                int pageSize = sys.so.mm.pages.length > 0 ? sys.hw.mem.pos.length / sys.so.mm.pages.length : 16;
                System.out.println("Memória alocada ao processo (páginas: " + pcb.pageTable.length + "):");
                
                for (int i = 0; i < pcb.pageTable.length; i++) {
                    int frameNumber = pcb.pageTable[i];
                    int startAddr = frameNumber * pageSize;
                    int endAddr = startAddr + pageSize;
                    
                    System.out.println("------ Página Lógica " + i + " -> Frame Físico " + frameNumber + " ------");
                    sys.so.utils.dump(startAddr, endAddr);
                }
            } else {
                System.out.println("Processo não possui memória alocada.");
            }
        } catch (NumberFormatException e) {
            System.out.println("PID inválido: " + args[0]);
        }
    }

    private void cmdDumpM(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: dumpM <start> <end>");
            return;
        }
        try {
            int start = Integer.parseInt(args[0]);
            int end = Integer.parseInt(args[1]);
            if (start < 0 || end < start) {
                System.out.println("Intervalo inválido.");
                return;
            }
            sys.so.utils.dump(start, end - start + 1);
        } catch (NumberFormatException e) {
            System.out.println("Argumento inválido: " + Arrays.toString(args));
        }
    }

    private void cmdExec(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: exec <pid>");
            return;
        }
        try {
            // int pid = Integer.parseInt(args[0]);
            // PCB pcb = findPCB(pid);
            // if (pcb == null) {
            //     System.out.println("Processo não encontrado: " + pid);
            //     return;
            // }
            // Word[] image = progs.retrieveProgram(pcb.programName);
            // sys.so.utils.loadAndExec(image);

            sys.hw.cpu.setContext(0); // seta pc para endereço 0 - ponto de entrada dos programas
            System.out.println("---------------------------------- inicia execucao ");
            sys.hw.cpu.run(); // cpu roda programa ate parar
        } catch (NumberFormatException e) {
            System.out.println("PID inválido: " + args[0]);
        }
    }

    private void cmdTraceOn() {
        //sys.hw.cpu.setDebug(true);
        System.out.println("Modo trace ligado.");
    }

    private void cmdTraceOff() {
        //sys.hw.cpu.setDebug(false);
        System.out.println("Modo trace desligado.");
    }

    private void cmdHelp() {
        System.out.println("Comandos disponíveis:");
        System.out.println("  new <programName>   - cria um processo na memória");
        System.out.println("  rm <pid>            - remove processo (executado ou não)");
        System.out.println("  ps                  - lista processos existentes");
        System.out.println("  dump <pid>          - exibe PCB e memória do processo");
        System.out.println("  dumpM <start> <end> - exibe memória do sistema entre posições");
        System.out.println("  exec <pid>          - executa o processo com o ID informado");
        System.out.println("  traceOn             - habilita modo de trace da CPU");
        System.out.println("  traceOff            - desabilita modo de trace da CPU");
        System.out.println("  help                - mostra esta ajuda");
        System.out.println("  exit                - sai do sistema");
    }

    /**
     * Busca um PCB por ID em prontos, bloqueados e executando.
     */
    private PCB findPCB(int pid) {
        if (sys.so.pm.processRunning != null && sys.so.pm.processRunning.pid == pid) {
            return sys.so.pm.processRunning;
        }
        for (PCB pcb : sys.so.pm.processReady) {
            if (pcb.pid == pid) return pcb;
        }
        for (PCB pcb : sys.so.pm.processBlocked) {
            if (pcb.pid == pid) return pcb;
        }
        return null;
    }
}
