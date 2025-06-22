package system.os;


public class InterruptInfo {
    Interrupts interrupt;
    int endereco;

    public InterruptInfo(Interrupts interrupt,  int endereco) {
        this.interrupt = interrupt;
        this.endereco = endereco;
    }
}
