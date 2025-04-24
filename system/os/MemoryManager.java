package system.os;

import system.hardware.HW;
import system.hardware.Word;

import java.util.*;

public class MemoryManager {
    public boolean[] pages;
    private int pageSize;
    private HW hw;

    public MemoryManager(int pageSize, HW hw) {
        this.hw = hw;
        // Verifica se o tamanho da página é divisor do tamanho da memória
        if (hw.mem.pos.length % pageSize != 0) {
            throw new IllegalArgumentException("O tamanho da página deve ser um divisor do tamanho da memória.");
        }

        // Inicializa o array de páginas com false
        this.pages = new boolean[hw.mem.pos.length / pageSize];
        Arrays.fill(this.pages, false);

        // Salva o tamanho da página
        this.pageSize = pageSize;
    }

    public Map<Integer, Integer> jmAlloc(Word[] p) {
        int qtdWords = p.length; // Quantidade de palavras a serem alocadas
        int qtdPages = (int) Math.ceil((double) qtdWords / pageSize); // Calcula o número de páginas necessárias
        Map<Integer, Integer> pageTable = new HashMap<>(); // Tabela de páginas lógica -> física
        int allocatedCount = 0; // Contador de páginas alocadas

        // Percorre as páginas disponíveis para encontrar espaço usando "first fit"
        for (int i = 0; i < pages.length; i++) {
            if (pages[i]) continue; // Página indisponível

            pages[i] = true; // Marca a página como alocada
            pageTable.put(allocatedCount, i); // Mapeia página lógica para frame físico
            allocatedCount++;

            // Copia o conteúdo para a memória
            int startAddress = i * pageSize; // Endereço inicial da página
            for (int offset = 0; offset < pageSize; offset++) {
                int wordIndex = (allocatedCount - 1) * pageSize + offset;
                if (wordIndex >= qtdWords) {
                    break;
                }
                hw.mem.pos[startAddress + offset] = new Word(
                        p[wordIndex].opc,
                        p[wordIndex].ra,
                        p[wordIndex].rb,
                        p[wordIndex].p);
            }

            // Verifica se todas as palavras foram alocadas
            if (allocatedCount == qtdPages) {
                return pageTable; // Retorna a tabela de páginas
            }
        }

        // Caso não seja possível alocar todas as páginas, lança uma exceção
        throw new OutOfMemoryError("Não há memória suficiente para alocar o programa.");
    }

    // não limpa as palavras (memoria sera sobrescrita)
    public List<Integer> jmFree(Map<Integer, Integer> pageTable) {
        List<Integer> freedFrames = new ArrayList<>(); // Lista para armazenar os frames desalocados

        for (int frame : pageTable.values()) { // Itera sobre os frames físicos na tabela de páginas
            if (frame >= 0 && frame < pages.length && pages[frame]) { // Verifica se o frame é válido e está alocado
                pages[frame] = false; // Marca o frame como livre
                freedFrames.add(frame); // Adiciona o frame à lista de desalocados
            }
        }

        return freedFrames; // Retorna os frames desalocados
    }
}
