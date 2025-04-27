package system.os;

import java.util.*;
import system.hardware.Memory;
import system.hardware.Word;
import system.software.ProcessManager;

public class MemoryManager {
    public boolean[] pages; //Se true, frame já está alocado
    private int pageSize;
    private Memory mem; // Memória física
    private ProcessManager pm; // Gerenciador de processos

    public MemoryManager(int tamMem, int pageSize, Memory mem, ProcessManager pm) {
        this.pm = pm; // Inicializa o gerenciador de processos
        this.mem = mem; // Inicializa a memória
        // Verifica se o tamanho da página é divisor do tamanho da memória
        if (tamMem % pageSize != 0) {
            throw new IllegalArgumentException("O tamanho da página deve ser um divisor do tamanho da memória.");
        }

        // Inicializa o array de páginas com false
        this.pages = new boolean[tamMem / pageSize];
        Arrays.fill(this.pages, false);

        // Salva o tamanho da página
        this.pageSize = pageSize;
    }
 
    public int[] aloca(Word[] p) {
        int qtdWords = p.length; // Quantidade de palavras a serem alocadas
        int qtdPages = (int) Math.ceil((double) qtdWords / pageSize); // Calcula o número de páginas necessárias
        int[] allocatedPages = new int[qtdPages]; // Array para armazenar as páginas alocadas
        int allocatedCount = 0; // Contador de páginas alocadas

        // Percorre as páginas disponíveis para encontrar espaço usando "first fit"
        for (int i = 0; i < pages.length; i++) {
            if (pages[i]) continue; // Página indisponível

            pages[i] = true; // Marca a página como alocada
            allocatedPages[allocatedCount] = i; // Armazena a página alocada
            allocatedCount++;

            // Copia o conteúdo para a memória
            int startAddress = i * pageSize; // Endereço inicial da página
            for (int offset = 0; offset < pageSize; offset++) {
                int wordIndex = (allocatedCount - 1) * pageSize + offset;
                if (wordIndex >= qtdWords) {
                    break;
                }
                System.out.println("Alocando palavra " + wordIndex + " na página " + i);
                mem.pos[startAddress + offset] = new Word(
                        p[wordIndex].opc,
                        p[wordIndex].ra,
                        p[wordIndex].rb,
                        p[wordIndex].p);
            }

            // Verifica se todas as palavras foram alocadas
            if (allocatedCount == qtdPages) {
                return allocatedPages; // Retorna o array de páginas alocadas
            } else {
                // Se não foi possível alocar todas as páginas, desaloca as páginas alocadas
                for (int j = 0; j < allocatedCount; j++) {
                    pages[allocatedPages[j]] = false; // Marca a página como livre
                }
            }
        }

        // Caso não seja possível alocar todas as páginas, lança uma exceção
        throw new OutOfMemoryError("Não há memória suficiente para alocar o programa.");
    }

    public void desaloca(int[] pageTable) {
        for (int frame : pageTable) {
            if (frame >= 0 && frame < pages.length) {
                pages[frame] = false; // Marca o frame como livre
            }
        }
    }

    public int mmu(int enderecoLogico) {
        // Verifica se há um processo em execução no momento
        if (pm.processRunning == null) {
            throw new IllegalStateException("Nenhum processo está em execução no momento.");
        }

        // Obtém a tabela de páginas do processo em execução
        int[] tabelaDePaginas = pm.processRunning.pageTable;

        // Verifica se o endereço lógico está no range das tabelas alocadas
        if (enderecoLogico < 0 || enderecoLogico >= tabelaDePaginas.length * pageSize) {
            throw new IllegalArgumentException("Endereço lógico fora do intervalo das tabelas de páginas alocadas.");
        }

        // Calcula o índice da página e o deslocamento dentro da página
        int indicePagina = enderecoLogico / pageSize;
        int deslocamento = enderecoLogico % pageSize;

        // Obtém o frame físico correspondente
        int frameFisico = tabelaDePaginas[indicePagina];

        // Verifica se o frame físico está no range da memória física
        if (frameFisico < 0 || frameFisico >= pages.length) {
            throw new IllegalArgumentException("Frame físico fora do intervalo da memória física.");
        }

        // Transforma o endereço lógico em endereço físico
        int enderecoFisico = frameFisico * pageSize + deslocamento;

        // Retorna o endereço físico
        return enderecoFisico;
    }
}
