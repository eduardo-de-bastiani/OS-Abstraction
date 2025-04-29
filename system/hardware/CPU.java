package system.hardware;

import system.core.Sistema;
import system.os.InterruptHandling;
import system.os.Interrupts;
import system.os.MemoryManager;
import system.os.SysCallHandling;
import system.software.Opcode;
import system.software.Scheduler;
import system.utils.Utilities;

public class CPU implements Runnable {
    private int maxInt; // valores maximo e minimo para inteiros nesta cpu
    private int minInt;
    // CONTEXTO da CPU ...
    public int pc; // ... composto de program counter,
    private Word ir; // instruction register,
    public int[] reg; // registradores da CPU
    private Interrupts irpt; // durante instrucao, interrupcao pode ser sinalizada
    // FIM CONTEXTO DA CPU: tudo que precisa sobre o estado de um processo para
    // executa-lo
    // nas proximas versoes isto pode modificar

    private Word[] m; // m é o array de memória "física", CPU tem uma ref a m para acessar

    private InterruptHandling ih; // significa desvio para rotinas de tratamento de Int - se int ligada, desvia
    private SysCallHandling sysCall; // significa desvio para tratamento de chamadas de sistema

    public boolean cpuStop; // flag para parar CPU - caso de interrupcao que acaba o processo, ou chamada
    // stop -
    // nesta versao acaba o sistema no fim do prog

    public boolean waitOnInstruction; // flag para esperar após cada instrução

    // auxilio aa depuração
    private Sistema sys;
    private boolean debug; // se true entao mostra cada instrucao em execucao
    private Utilities u; // para debug (dump)
    public MemoryManager mm; 
    public Scheduler scheduler;

    public CPU(Memory _mem, boolean _debug, Sistema sys) {
        this.sys = sys;
        maxInt = 32767;
        minInt = -32767;
        m = _mem.pos;
        reg = new int[10];
        debug = _debug;
    }

    // TODO: implementar TLB (Translation Lookaside Buffer)
    // local para armazenar as traduções dos endereços virtuais para físicos

    public void setAddressOfHandlers(InterruptHandling _ih, SysCallHandling _sysCall) {
        ih = _ih; // aponta para rotinas de tratamento de int
        sysCall = _sysCall; // aponta para rotinas de tratamento de chamadas de sistema
    }

    public void setUtilities(Utilities _u) {
        u = _u; // aponta para rotinas utilitárias - fazer dump da memória na tela
    }

    //TODO: isso
    // private boolean isLogicAdressValid(int e) { // verifica se o endereco é valido
    //     for (int page : sys.so.pm.processRunning.pageTable) {
    //         se e ta aq entao retorna true
    //     }
    //     return false;
    // }

    // verificação de enderecamento
    private boolean legal(int e) { // todo acesso a memoria tem que ser verificado se é válido -
        // aqui no caso se o endereco é um endereco valido em toda memoria
        if (e >= 0 && e < m.length) {
            return true;
        } else {
            irpt = Interrupts.intEnderecoInvalido; // se nao for liga interrupcao no meio da exec da instrucao
            return false;
        }
    }

    private boolean testOverflow(int v) { // toda operacao matematica deve avaliar se ocorre overflow
        if ((v < minInt) || (v > maxInt)) {
            irpt = Interrupts.intOverflow; // se houver liga interrupcao no meio da exec da instrucao
            return false;
        }
        ;
        return true;
    }

    public void setContext(int _pc) { // usado para setar o contexto da cpu para rodar um processo
        // [ nesta versao é somente colocar o PC na posicao 0 ]
        pc = _pc; // pc cfe endereco logico
        irpt = Interrupts.noInterrupt; // reset da interrupcao registrada
    }

    @Override
    public void run() {
        cpuStop = false;
        while (!cpuStop) {
            if (legal(pc)) {
                ir = m[pc];
                if (debug) {
                    System.out.print("                                              regs: ");
                    for (int i = 0; i < 10; i++) {
                        System.out.print(" r[" + i + "]:" + reg[i]);
                    }
                    System.out.println();
                }
                if (debug) {
                    System.out.print("                      pc: " + pc + "       exec: ");
                    u.dump(ir);
                }

                switch (ir.opc) {
                    case LDI:
                        reg[ir.ra] = ir.p;
                        pc++;
                        break;
                    case LDD:
                        int enderecoFisicoLDD = mm.mmu(ir.p);
                        if (legal(enderecoFisicoLDD)) {
                            reg[ir.ra] = m[enderecoFisicoLDD].p;
                            pc++;
                        }
                        break;
                    case LDX: // RD <- [RS] // NOVA
                        int enderecoFisicoLDX = mm.mmu(reg[ir.rb]);
                        if (legal(enderecoFisicoLDX)) {
                            reg[ir.ra] = m[enderecoFisicoLDX].p;
                            pc++;
                        }
                        break;
                    case STD: // [A] ← Rs
                        int enderecoFisicoSTD = mm.mmu(ir.p);
                        if (legal(enderecoFisicoSTD)) {
                            m[enderecoFisicoSTD].opc = Opcode.DATA;
                            m[enderecoFisicoSTD].p = reg[ir.ra];
                            pc++;
                            if (debug) {
                                System.out.print("                                  ");
                                u.dump(enderecoFisicoSTD, enderecoFisicoSTD + 1);
                            }
                        }
                        break;
                    case STX: // [Rd] ←Rs
                        int enderecoFisicoSTX = mm.mmu(reg[ir.ra]);
                        if (legal(enderecoFisicoSTX)) {
                            m[enderecoFisicoSTX].opc = Opcode.DATA;
                            m[enderecoFisicoSTX].p = reg[ir.rb];
                            pc++;
                        }
                        break;
                    case MOVE: // RD <- RS
                        reg[ir.ra] = reg[ir.rb];
                        pc++;
                        break;
                    // Instrucoes Aritmeticas
                    case ADD: // Rd ← Rd + Rs
                        reg[ir.ra] = reg[ir.ra] + reg[ir.rb];
                        testOverflow(reg[ir.ra]);
                        pc++;
                        break;
                    case ADDI: // Rd ← Rd + k
                        reg[ir.ra] = reg[ir.ra] + ir.p;
                        testOverflow(reg[ir.ra]);
                        pc++;
                        break;
                    case SUB: // Rd ← Rd - Rs
                        reg[ir.ra] = reg[ir.ra] - reg[ir.rb];
                        testOverflow(reg[ir.ra]);
                        pc++;
                        break;
                    case SUBI: // RD <- RD - k // NOVA
                        reg[ir.ra] = reg[ir.ra] - ir.p;
                        testOverflow(reg[ir.ra]);
                        pc++;
                        break;
                    case MULT: // Rd <- Rd * Rs
                        reg[ir.ra] = reg[ir.ra] * reg[ir.rb];
                        testOverflow(reg[ir.ra]);
                        pc++;
                        break;

                    // Instrucoes JUMP
                    case JMP: // PC <- k
                        pc = ir.p;
                        break;
                    case JMPIM: // PC <- [A]
                        pc = m[ir.p].p;
                        break;
                    case JMPIG: // If Rc > 0 Then PC ← Rs Else PC ← PC +1
                        if (reg[ir.rb] > 0) {
                            pc = reg[ir.ra];
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIGK: // If RC > 0 then PC <- k else PC++
                        if (reg[ir.rb] > 0) {
                            pc = ir.p;
                        } else {
                            pc++;
                        }
                        break;
                    case JMPILK: // If RC < 0 then PC <- k else PC++
                        if (reg[ir.rb] < 0) {
                            pc = ir.p;
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIEK: // If RC = 0 then PC <- k else PC++
                        if (reg[ir.rb] == 0) {
                            pc = ir.p;
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIL: // if Rc < 0 then PC <- Rs Else PC <- PC +1
                        if (reg[ir.rb] < 0) {
                            pc = reg[ir.ra];
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIE: // If Rc = 0 Then PC <- Rs Else PC <- PC +1
                        if (reg[ir.rb] == 0) {
                            pc = reg[ir.ra];
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIGM: // If RC > 0 then PC <- [A] else PC++
                        if (legal(ir.p)) {
                            if (reg[ir.rb] > 0) {
                                pc = m[ir.p].p;
                            } else {
                                pc++;
                            }
                        }
                        break;
                    case JMPILM: // If RC < 0 then PC <- k else PC++
                        if (reg[ir.rb] < 0) {
                            pc = m[ir.p].p;
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIEM: // If RC = 0 then PC <- k else PC++
                        if (reg[ir.rb] == 0) {
                            pc = m[ir.p].p;
                        } else {
                            pc++;
                        }
                        break;
                    case JMPIGT: // If RS>RC then PC <- k else PC++
                        if (reg[ir.ra] > reg[ir.rb]) {
                            pc = ir.p;
                        } else {
                            pc++;
                        }
                        break;

                    case DATA: // pc está sobre área supostamente de dados
                        System.out.println("pc está sobre área supostamente de dados");
                        irpt = Interrupts.intInstrucaoInvalida;
                        break;

                    // Chamadas de sistema
                    case SYSCALL:
                        sysCall.handle(); // <<<<< aqui desvia para rotina de chamada de sistema, no momento so
                        // temos IO
                        pc++;
                        break;

                    case STOP: // por enquanto, para execucao
                        sysCall.stop();
                        irpt = Interrupts.intSTOP;
                        break;
                    // Inexistente
                    default:
                        System.out.println("Instrução inexistente: " + ir.opc);
                        irpt = Interrupts.intInstrucaoInvalida;
                        break;
                }
            }

            if (scheduler.notifyInstructionExecuted()){
                // Se o quantum foi completado, gera uma interrupção
                irpt = Interrupts.quantumTime;
            }

            if (irpt != Interrupts.noInterrupt) {
                ih.handle(irpt);
                irpt = Interrupts.noInterrupt; // reset da interrupcao
            }

        
            if (waitOnInstruction) {
                try {
                    Thread.sleep(1000); // espera 1 segundo entre as instruções
                } catch (InterruptedException e) {
                    System.out.println("Erro ao pausar a execução: " + e.getMessage());
                }
            }
        }
    }
}