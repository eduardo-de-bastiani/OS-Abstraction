package system.core;

/**
 * Programa de teste para validar a memória virtual
 * Este programa acessa várias posições de memória para forçar page faults
 */
public class TestePaginacao {
    public static void main(String[] args) {
        // Cria um sistema com memória reduzida para forçar page faults
        Sistema s = new Sistema(128, 16, 4);
        
        // Inicia o sistema e cria vários processos para testar a memória virtual
        s.run();
    }
}
