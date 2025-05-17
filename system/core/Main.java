package system.core;

public class Main {
    public static void main(String[] args) {
        // Reduzindo o tamanho da memória principal para facilitar testes de page fault
        // Tamanho original: 1024
        // Novo tamanho: 256 (1/4 do original)
        Sistema s = new Sistema(256, 16, 4);
        s.run();
    }
}
