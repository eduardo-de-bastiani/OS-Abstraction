package system.hardware;

import system.os.Interrupts;
import system.os.InterruptHandling;
import system.os.SysCallHandling;
import system.core.Sistema;

public class CPU {
    public int pc; // program counter
    public int[] reg; // registradores da CPU
    private Memory mem; // acesso a memoria
    private InterruptHandling ih; // rotinas de tratamento de interrupcoes
    private SysCallHandling sysCall; // chamadas de sistema
    private boolean debug; // para depuracao
    private Sistema sistema; // referencia ao sistema para acesso a utilitarios
    public boolean waitOnInstruction = false; // para depuracao
    
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
        
        while (true) {
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
            
            switch (w.opc) { // conforme o opcode
                // Instrucoes de Busca e Armazenamento em Memoria
                case 1: // LDI r,k
                    reg[w.ra] = w.p;
                    pc++;
                    break;
                    
                case 2: // STD k,r
                    try {
                        int enderecoFisicoSTD = sistema.so.mm.mmu(w.p);
                        
                        // Se retornou -1, houve page fault
                        if (enderecoFisicoSTD == -1) {
                            // Gera interrupção de page fault
                            ih.handle(Interrupts.intPageFault);
                            break;
                        }
                        
                        mem.pos[enderecoFisicoSTD].opc = Opcode.DATA.ordinal();
                        mem.pos[enderecoFisicoSTD].p = reg[w.ra];
                    } catch (Exception e) {
                        System.out.println("Erro ao acessar memória: " + e.getMessage());
                        ih.handle(Interrupts.intEnderecoInvalido);
                        break;
                    }
                    pc++;
                    break;
                    
                case 3: // ADD r,s
                    reg[w.ra] = reg[w.ra] + reg[w.rb];
                    pc++;
                    break;
                    
                case 4: // MULT r,s
                    reg[w.ra] = reg[w.ra] * reg[w.rb];
                    pc++;
                    break;
                    
                case 5: // CALL r,s
                    reg[w.ra] = pc + 1;
                    pc = w.p;
                    break;
                    
                case 6: // RET
                    pc = reg[w.ra];
                    break;
                    
                case 7: // JMP p
                    pc = w.p;
                    break;
                    
                case 8: // JMPI r
                    pc = reg[w.ra];
                    break;
                    
                case 9: // JMPIG r,s
                    if (reg[w.rb] > 0) {
                        pc = reg[w.ra];
                    } else {
                        pc++;
                    }
                    break;
                    
                case 10: // JMPIL r,s
                    if (reg[w.rb] < 0) {
                        pc = reg[w.ra];
                    } else {
                        pc++;
                    }
                    break;
                    
                case 11: // JMPIE r,s
                    if (reg[w.rb] == 0) {
                        pc = reg[w.ra];
                    } else {
                        pc++;
                    }
                    break;
                    
                case 12: // JMPIM r,s
                    try {
                        int enderecoFisicoJMPIM = sistema.so.mm.mmu(reg[w.ra]);
                        
                        // Se retornou -1, houve page fault
                        if (enderecoFisicoJMPIM == -1) {
                            // Gera interrupção de page fault
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
                    
                case 13: // JMPIGM r,s
                    if (reg[w.rb] > 0) {
                        try {
                            int enderecoFisicoJMPIGM = sistema.so.mm.mmu(reg[w.ra]);
                            
                            // Se retornou -1, houve page fault
                            if (enderecoFisicoJMPIGM == -1) {
                                // Gera interrupção de page fault
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
                    
                case 14: // JMPILM r,s
                    if (reg[w.rb] < 0) {
                        try {
                            int enderecoFisicoJMPILM = sistema.so.mm.mmu(reg[w.ra]);
                            
                            // Se retornou -1, houve page fault
                            if (enderecoFisicoJMPILM == -1) {
                                // Gera interrupção de page fault
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
                    
                case 15: // JMPIEM r,s
                    if (reg[w.rb] == 0) {
                        try {
                            int enderecoFisicoJMPIEM = sistema.so.mm.mmu(reg[w.ra]);
                            
                            // Se retornou -1, houve page fault
                            if (enderecoFisicoJMPIEM == -1) {
                                // Gera interrupção de page fault
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
                    
                case 16: // SWAP r,s
                    int aux = reg[w.ra];
                    reg[w.ra] = reg[w.rb];
                    reg[w.rb] = aux;
                    pc++;
                    break;
                    
                case 17: // STOP
                    ih.handle(Interrupts.intSTOP);
                    break;
                    
                case 18: // DATA
                    pc++;
                    break;
                    
                case 19: // LD r,s
                    try {
                        int enderecoFisicoLD = sistema.so.mm.mmu(reg[w.rb]);
                        
                        // Se retornou -1, houve page fault
                        if (enderecoFisicoLD == -1) {
                            // Gera interrupção de page fault
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
                    
                case 20: // SUB r,s
                    reg[w.ra] = reg[w.ra] - reg[w.rb];
                    pc++;
                    break;
                    
                case 21: // DIV r,s
                    if (reg[w.rb] == 0) {
                        ih.handle(Interrupts.intInstrucaoInvalida);
                        break;
                    }
                    reg[w.ra] = reg[w.ra] / reg[w.rb];
                    pc++;
                    break;
                    
                case 22: // TRAP
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
