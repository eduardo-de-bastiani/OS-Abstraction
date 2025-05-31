package system.os;

// Adicionando novas interrupções para memória virtual
public enum Interrupts {
    noInterrupt, intEnderecoInvalido, intInstrucaoInvalida, intOverflow, intSTOP, quantumTime, 
    intPageFault, intIOComplete, intDiskSaveComplete, intDiskLoadComplete;
}
