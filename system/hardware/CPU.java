package system.hardware;

import system.os.Interrupts;
import system.os.InterruptHandling;
import system.os.SysCallHandling;
import system.core.Sistema;
import system.software.Opcode;

public class CPU {
    public int pc; // program counter
    public int[] reg; // registradores da CPU
    private Memory mem; // acesso a memoria
    private InterruptHandling ih; // rotinas de tratamento de interrupcoes
    private SysCallHandling sysCall; // chamadas de sistema
    private boolean debug; // para depuracao
    private Sistema sistema; // referencia ao sistema para acesso a utilitarios
    public boolean waitOnInstruction = false; // para depuracao
    public boolean cpuStop = false;

    public CPU(Memory _mem, boolean _debug, Sistema sistema) {
        mem = _mem;
        debug = _debug;
        reg = new int[10]; // 10 registradores
        this.sistema = sistema;
    }

    public void setContext(int _pc) {
        pc = _pc;
    }

    public void setAddressOfHandlers(InterruptHandling _ih, SysCallHandling _sysCall) {
        ih = _ih;
        sysCall = _sysCall;
    }

    public void run() {
        // execucao da CPU supoe que o contexto da CPU, vindo do PCB, ja esta devidamente setado
        // pc e registradores já contém os valores corretos


        while (!cpuStop) {
            // --------------------------------------------------------------------------------------------------
            // FETCH

            if (pc < 0 || pc >= mem.pos.length) {
                ih.handle(Interrupts.intEnderecoInvalido);
                break; // ?
            }

            // Verifica se o endereço lógico está na memória usando o MMU
            int enderecoFisico;
            try {
                enderecoFisico = sistema.so.mm.mmu(pc);

                // Se retornou -1, houve page fault
                if (enderecoFisico == -1) {
                    // Gera interrupção de page fault
                    ih.handle(Interrupts.intPageFault);
                    break;
                }
            } catch (Exception e) {
                System.out.println("Erro ao acessar memória: " + e.getMessage());
                ih.handle(Interrupts.intEnderecoInvalido);
                break;
            }

            Word w = mem.pos[enderecoFisico];

            if (debug) {
                System.out.print("PC=" + pc + ", exec: ");
                System.out.print(w.opc + " ");
                System.out.print(w.ra + " ");
                System.out.print(w.rb + " ");
                System.out.println(w.p);
            }

            // --------------------------------------------------------------------------------------------------
            // EXECUTA INSTRUCAO NO ir

            switch (Opcode.values()[w.opc.ordinal()]) { // agora usando enum Opcode
                case LDI: // LDI r,k
                    reg[w.ra] = w.p;
                    pc++;
                    break;

                case STD: // STD k,r
                    try {
                        int enderecoFisicoSTD = sistema.so.mm.mmu(w.p);
                        if (enderecoFisicoSTD == -1) {
                            ih.handle(Interrupts.intPageFault);
                            break;
                        }
                        mem.pos[enderecoFisicoSTD].opc = Opcode.DATA;
                        mem.pos[enderecoFisicoSTD].p = reg[w.ra];
                    } catch (Exception e) {
                        System.out.println("Erro ao acessar memória: " + e.getMessage());
                        ih.handle(Interrupts.intEnderecoInvalido);
                        break;
                    }
                    pc++;
                    break;

                case ADD: // ADD r,s
                    reg[w.ra] = reg[w.ra] + reg[w.rb];
                    pc++;
                    break;

                case MULT: // MULT r,s
                    reg[w.ra] = reg[w.ra] * reg[w.rb];
                    pc++;
                    break;

                case CALL: // CALL r,s
                    reg[w.ra] = pc + 1;
                    pc = w.p;
                    break;

                case RET: // RET
                    pc = reg[w.ra];
                    break;

                case JMP: // JMP p
                    pc = w.p;
                    break;

                case JMPI: // JMPI r
                    pc = reg[w.ra];
                    break;

                case JMPIG: // JMPIG r,s
                    if (reg[w.rb] > 0) {
                        pc = reg[w.ra];
                    } else {
                        pc++;
                    }
                    break;

                case JMPIL: // JMPIL r,s
                    if (reg[w.rb] < 0) {
                        pc = reg[w.ra];
                    } else {
                        pc++;
                    }
                    break;

                case JMPIE: // JMPIE r,s
                    if (reg[w.rb] == 0) {
                        pc = reg[w.ra];
                    } else {
                        pc++;
                    }
                    break;

                case JMPIM: // JMPIM r,s
                    try {
                        int enderecoFisicoJMPIM = sistema.so.mm.mmu(reg[w.ra]);
                        if (enderecoFisicoJMPIM == -1) {
                            ih.handle(Interrupts.intPageFault);
                            break;
                        }
                        pc = mem.pos[enderecoFisicoJMPIM].p;
                    } catch (Exception e) {
                        System.out.println("Erro ao acessar memória: " + e.getMessage());
                        ih.handle(Interrupts.intEnderecoInvalido);
                        break;
                    }
                    break;

                case JMPIGM: // JMPIGM r,s
                    if (reg[w.rb] > 0) {
                        try {
                            int enderecoFisicoJMPIGM = sistema.so.mm.mmu(reg[w.ra]);
                            if (enderecoFisicoJMPIGM == -1) {
                                ih.handle(Interrupts.intPageFault);
                                break;
                            }
                            pc = mem.pos[enderecoFisicoJMPIGM].p;
                        } catch (Exception e) {
                            System.out.println("Erro ao acessar memória: " + e.getMessage());
                            ih.handle(Interrupts.intEnderecoInvalido);
                            break;
                        }
                    } else {
                        pc++;
                    }
                    break;

                case JMPILM: // JMPILM r,s
                    if (reg[w.rb] < 0) {
                        try {
                            int enderecoFisicoJMPILM = sistema.so.mm.mmu(reg[w.ra]);
                            if (enderecoFisicoJMPILM == -1) {
                                ih.handle(Interrupts.intPageFault);
                                break;
                            }
                            pc = mem.pos[enderecoFisicoJMPILM].p;
                        } catch (Exception e) {
                            System.out.println("Erro ao acessar memória: " + e.getMessage());
                            ih.handle(Interrupts.intEnderecoInvalido);
                            break;
                        }
                    } else {
                        pc++;
                    }
                    break;

                case JMPIEM: // JMPIEM r,s
                    if (reg[w.rb] == 0) {
                        try {
                            int enderecoFisicoJMPIEM = sistema.so.mm.mmu(reg[w.ra]);
                            if (enderecoFisicoJMPIEM == -1) {
                                ih.handle(Interrupts.intPageFault);
                                break;
                            }
                            pc = mem.pos[enderecoFisicoJMPIEM].p;
                        } catch (Exception e) {
                            System.out.println("Erro ao acessar memória: " + e.getMessage());
                            ih.handle(Interrupts.intEnderecoInvalido);
                            break;
                        }
                    } else {
                        pc++;
                    }
                    break;

                case SWAP: // SWAP r,s
                    int aux = reg[w.ra];
                    reg[w.ra] = reg[w.rb];
                    reg[w.rb] = aux;
                    pc++;
                    break;

                case STOP: // STOP
                    ih.handle(Interrupts.intSTOP);
                    break;

                case DATA: // DATA
                    pc++;
                    break;

                case LD: // LD r,s
                    try {
                        int enderecoFisicoLD = sistema.so.mm.mmu(reg[w.rb]);
                        if (enderecoFisicoLD == -1) {
                            ih.handle(Interrupts.intPageFault);
                            break;
                        }
                        reg[w.ra] = mem.pos[enderecoFisicoLD].p;
                    } catch (Exception e) {
                        System.out.println("Erro ao acessar memória: " + e.getMessage());
                        ih.handle(Interrupts.intEnderecoInvalido);
                        break;
                    }
                    pc++;
                    break;

                case SUB: // SUB r,s
                    reg[w.ra] = reg[w.ra] - reg[w.rb];
                    pc++;
                    break;

                case DIV: // DIV r,s
                    if (reg[w.rb] == 0) {
                        ih.handle(Interrupts.intInstrucaoInvalida);
                        break;
                    }
                    reg[w.ra] = reg[w.ra] / reg[w.rb];
                    pc++;
                    break;

                case TRAP: // TRAP
                    sysCall.handle();
                    pc++;
                    break;

                default:
                    ih.handle(Interrupts.intInstrucaoInvalida);
                    break;
            }

            if (waitOnInstruction) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
