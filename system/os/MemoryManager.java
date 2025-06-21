package system.os;

import java.util.*;
import system.hardware.Memory;
import system.hardware.Word;
import system.software.Clock; //vlw
import system.software.ProcessManager;

public class MemoryManager {
    public boolean[][] pages; // Array de páginas, modificado, cada linha [X][] representa uma página, a primeria coluna [X][0] representa se a pagina está alocada ou não, a segunda coluna [X][1] representa se a página foi ou não acessada, essa informação será utilizada para implementar o algoritmo de substituição de páginas do relógio (clock)
    public boolean[] pagesSec; // Se true, frame já está alocado na memória secundária
    public int pageSize;
    private Memory mem; // Memória física
    private Memory secMem;
    private ProcessManager pm; // Gerenciador de 
    private int allocMethod = 1; // Método de alocação (0 = first fit, 1 = random fit, 2 = on demand)
    private Clock clock; // Algoritmo de substituição de páginas do relógio

    public MemoryManager(int tamMem, int pageSize, Memory mem, Memory secMem, ProcessManager pm) {
        this.pm = pm; 
        this.mem = mem; 
        this.secMem = secMem;
        // Verifica se o tamanho da página é divisor do tamanho da memória
        if (tamMem % pageSize != 0) {
            throw new IllegalArgumentException("O tamanho da página deve ser um divisor do tamanho da memória.");
        }
        if( secMem.pos.length % pageSize != 0) {
            throw new IllegalArgumentException("O tamanho da página deve ser um divisor do tamanho da memória secundária.");
        }

        // Inicializa o array de páginas com false
        this.pages = new boolean[tamMem / pageSize][2]; // Cada página tem duas colunas: [0] para alocação e [1] para acesso
        for (boolean[] page : this.pages) {
            page[0] = false; // Marca a página como não alocada
            page[1] = false; // Marca a página como não acessada
        }

        this.clock = new Clock(tamMem / pageSize); // Inicializa o algoritmo de substituição de páginas do relógio

        this.pagesSec = new boolean[secMem.pos.length / pageSize];
        Arrays.fill(this.pagesSec, false);

        // Salva o tamanho da página
        this.pageSize = pageSize;
    }
 
    public int[][] alocaPrograma(Word[] p) {
        int qtdWords = p.length; // Quantidade de palavras a serem alocadas
        int qtdPages = (int) Math.ceil((double) qtdWords / pageSize); // Calcula o número de páginas necessárias
        int[][] allocatedPages = new int[qtdPages][2]; // Array para armazenar as páginas alocadas
        int allocatedCount = 0; // Contador de páginas alocadas

        //Verifica a quantidade de páginas disponíveis
        int qtdAvailablePages = 0;
        for (boolean[] page : this.pages) {
            if (!page[0]) { // Se a página não está alocada
                qtdAvailablePages++;
            }
        }

        if (qtdAvailablePages < qtdPages && allocMethod != 2) {
            throw new OutOfMemoryError("Não há memória suficiente para alocar o programa.");
        }

        switch (allocMethod) {
            case 0 -> { // Percorre as páginas disponíveis para encontrar espaço usando "first fit"
                
                for (int i = 0; i < pages.length; i++) {
                    if (pages[i][0]) { // Página indisponível
                        System.out.println("Página " + i + " indisponível");
                        continue;
                    };
                    
                    System.out.println("Alocando página " + i + " na memória.");
                    
                    pages[i][0] = true; // Marca a página como alocada
                    allocatedPages[allocatedCount][0] = i; // Armazena a página alocada
                    allocatedCount++;
                    
                    // Copia o conteúdo para a memória
                    int startAddress = i * pageSize; // Endereço inicial da página
                    for (int offset = 0; offset < pageSize; offset++) {
                        int wordIndex = (allocatedCount - 1) * pageSize + offset;
                        if (wordIndex >= qtdWords) {
                            break;
                        }
                        mem.pos[startAddress + offset] = new Word(
                                p[wordIndex].opc,
                                p[wordIndex].ra,
                                p[wordIndex].rb,
                                p[wordIndex].p);
                    }
                    
                    // Verifica se todas as páginas foram alocadas
                    if (allocatedCount == qtdPages) {
                        return allocatedPages; // Retorna o array de páginas alocadas
                    }
                }
                
                // Se não foi possível alocar todas as páginas, desaloca as páginas alocadas
                for (int j = 0; j < allocatedCount; j++) {
                    pages[allocatedPages[j][0]][0] = false; // Marca a página como livre
                }  

                throw new OutOfMemoryError("Não há memória suficiente para alocar o programa.");
            }
            case 1 -> { // Aloca as páginas de forma aleatória
                int[] freePages = new int[qtdAvailablePages]; // Array para armazenar as páginas livres

                for (int i = 0, j = 0; i < pages.length; i++) {
                    if (!pages[i][0]) { // Página livre
                        freePages[j] = i;
                        j++;
                    }
                }
                //Embaralha as páginas livres
                freePages = embaralhaArray(freePages);

                //Pegas as primeiras páginas livres do array embaralhado e aloca nelas

                for (int i = 0; i < freePages.length; i++) {
                    if (allocatedCount == qtdPages) {
                        return allocatedPages; // Retorna o array de páginas alocadas
                    }

                    int pageIndex = freePages[i];
                    pages[pageIndex][0] = true; // Marca a página como alocada
                    allocatedPages[allocatedCount][0] = pageIndex; // Armazena a página alocada
                    allocatedCount++;

                    System.out.println("Alocando página " + pageIndex + " na memória.");

                    // Copia o conteúdo para a memória
                    int startAddress = pageIndex * pageSize; // Endereço inicial da página
                    for (int offset = 0; offset < pageSize; offset++) {
                        int wordIndex = (allocatedCount - 1) * pageSize + offset;
                        if (wordIndex >= qtdWords) {
                            break;
                        }
                        mem.pos[startAddress + offset] = new Word(
                                p[wordIndex].opc,
                                p[wordIndex].ra,
                                p[wordIndex].rb,
                                p[wordIndex].p);
                    }
                }
                if (allocatedCount == qtdPages) {
                    return allocatedPages; // Retorna o array de páginas alocadas
                }
            }
            case 2 -> { // Aloca apenas a primeira página do processo na memória principal, as demais serão carregadas para a memoria secundária

                //Todo alterar a lógica ainda para carregar as páginas para a memória secundária
                if(qtdAvailablePages < 1) {
                    throw new OutOfMemoryError("Não há memória suficiente para alocar o programa.");
                }
                for (int i = 0; i < pages.length; i++) {
                    if (!pages[i][0]) { // Página livre
                        pages[i][0] = true; // Marca a página como alocada
                        allocatedPages[allocatedCount][0] = i; // Armazena a página alocada
                        allocatedPages[allocatedCount][1] = -1; // Marca a página como não alocada na memória secundária
                        allocatedCount++;

                        System.out.println("Alocando página " + i + " na memória.");

                        // Copia o conteúdo para a memória
                        int startAddress = i * pageSize; // Endereço inicial da página
                        for (int offset = 0; offset < pageSize && offset < qtdWords; offset++) {
                            mem.pos[startAddress + offset] = new Word(
                                    p[offset].opc,
                                    p[offset].ra,
                                    p[offset].rb,
                                    p[offset].p);
                        }

                        return allocatedPages; // Retorna o array de páginas alocadas
                    }
                }
                
            }
        }

        throw new OutOfMemoryError("Não há memória suficiente para alocar o programa.");
    }

    public void desaloca(int[][] pageTable) {

        for (int i = 0; i < pageTable.length; i++) {
            int frame = pageTable[i][0]; // Obtém o frame físico da memória principal
            int secFrame = pageTable[i][1]; // Obtém o frame físico da memória secundária
            if (frame >= 0 && frame < pages.length) {
                pages[frame][0] = false; // Marca o frame como livre
                pages[frame][1] = false; // Marca a página como não acessada
                System.out.println("Desalocando página " + i + " da memória.");
            }
            if (secFrame >= 0 && secFrame < pagesSec.length) {
                pagesSec[secFrame] = false; // Marca o frame como livre na memória secundária
                System.out.println("Desalocando página " + i + " da memória secundária.");
            }
        }
    }

    public int mmu(int enderecoLogico) {
        // Verifica se há um processo em execução no momento
        if (pm.processRunning == null) {
            throw new IllegalStateException("Nenhum processo está em execução no momento.");
        }

        // Obtém a tabela de páginas do processo em execução
        int[][] tabelaDePaginas = pm.processRunning.pageTable;

        // Verifica se o endereço lógico está no range das tabelas alocadass
        if (enderecoLogico < 0 || enderecoLogico >= tabelaDePaginas.length * pageSize) {
            throw new IllegalArgumentException(
                "Endereço lógico " + enderecoLogico + " fora do intervalo das tabelas de páginas (" + (tabelaDePaginas.length * pageSize) + ")"
                );
        }

        // Calcula o índice da página e o deslocamento dentro da página
        int indicePagina = enderecoLogico / pageSize;
        int deslocamento = enderecoLogico % pageSize;

        // Obtém o frame físico correspondente
        int frameFisico = tabelaDePaginas[indicePagina][0];

        if (frameFisico == -1) {
            //Aciona interrupção por page fault
            return -1;
        }

        // Verifica se o frame físico está no range da memória física
        if (frameFisico < 0 || frameFisico >= pages.length) {
            throw new IllegalArgumentException("Frame físico fora do intervalo da memória física.");
        }
        
        this.markPageAccessed(enderecoLogico); // Marca a página como acessada

        // Transforma o endereço lógico em endereço físico
        int enderecoFisico = frameFisico * pageSize + deslocamento;

        // Retorna o endereço físico
        return enderecoFisico;
    }

    public boolean  markPageAccessed(int enderecoLogico) {
        if (enderecoLogico >= 0 && enderecoLogico < mem.pos.length){
            int pageIndex = enderecoLogico / pageSize; // Calcula o índice da página
            pages[pageIndex][1] = true; // Marca a página como acessada
        } else {
            throw new IllegalArgumentException("Endereço de programa inválido: " + enderecoLogico);
        }
        return true;
    }

    private int[] embaralhaArray(int[] array) {
        // Converte o array para uma lista
        Integer[] boxedArray = Arrays.stream(array).boxed().toArray(Integer[]::new);
        List<Integer> list = Arrays.asList(boxedArray);

        // Embaralha a lista
        Collections.shuffle(list);

        // Converte a lista de volta para um array
        array = list.stream().mapToInt(Integer::intValue).toArray();

        // Exibe o array embaralhado
        return array;
    }

    /**
     * Retorna o conjunto de frames atualmente alocados.
     */
    public Set<Integer> getAllocatedFrames() {
        Set<Integer> allocated = new TreeSet<>(); // TreeSet para já ordenar
        for (int i = 0; i < pages.length; i++) {
            if (pages[i][0]) {
                allocated.add(i);
            }
        }
        return allocated;
    }
}
