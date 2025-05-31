package system.os;

import java.util.ArrayList;
import java.util.List;
import system.hardware.Memory;
import system.hardware.Word;
import system.software.PCB;
import system.software.ProcessManager;
import system.software.Program;

public class MemoryManager {
    public boolean[] pages; // Se true, frame já está alocado
    public int pageSize;
    private Memory mem; // Memória física
    private Memory secMem; // Memória secundária (virtual)
    private ProcessManager pm; // Gerenciador de processos
    private int allocMethod = 1; // Método de alocação (0 = first fit, 1 = random fit)
    private DiskDevice diskDevice; // Dispositivo de disco para paginação
    
    // Variáveis para o algoritmo do relógio
    private int clockHand = 0; // Ponteiro do relógio
    private List<Integer> allocatedFrames = new ArrayList<>(); // Lista de frames alocados para o algoritmo do relógio
    
    // Estruturas para controle de requisições pendentes
    private int pendingPageFaultPID = -1; // PID do processo que gerou page fault
    private int pendingPageNumber = -1; // Número da página que gerou page fault
    private int pendingFrameNumber = -1; // Frame alocado para a página
    private int victimProcessId = -1; // PID do processo dono da página vítima
    private int victimPageNumber = -1; // Número da página vítima
    private int victimFrameNumber = -1; // Frame da página vítima

    public MemoryManager(int tamMem, int pageSize, Memory mem, Memory secMem, ProcessManager pm) {
        this.pm = pm; // Inicializa o gerenciador de processos
        this.mem = mem; // Inicializa a memória principal
        this.secMem = secMem; // Inicializa a memória secundária
        
        // Verifica se o tamanho da página é divisor do tamanho da memória
        if (tamMem % pageSize != 0) {
            throw new IllegalArgumentException("O tamanho da página deve ser um divisor do tamanho da memória.");
        }

        // Inicializa o array de páginas com false
        this.pages = new boolean[tamMem / pageSize];
        for (int i = 0; i < this.pages.length; i++) {
            this.pages[i] = false;
        }

        // Salva o tamanho da página
        this.pageSize = pageSize;
        
        // Inicializa o dispositivo de disco
        this.diskDevice = new DiskDevice(this);
        this.diskDevice.start();
    }
 
    // Método modificado para alocar apenas a primeira página do programa
    public int[] alocaFirstPage(Program p) {
        Word[] programImage = p.image;
        int qtdWords = programImage.length; // Quantidade de palavras a serem alocadas
        int qtdPages = (int) Math.ceil((double) qtdWords / pageSize); // Calcula o número de páginas necessárias
        int[] pageTable = new int[qtdPages]; // Array para armazenar as páginas alocadas
        
        // Inicializa a tabela de páginas com -1 (não alocada)
        for (int i = 0; i < pageTable.length; i++) {
            pageTable[i] = -1;
        }
        
        // Verifica se há pelo menos um frame livre
        boolean frameFound = false;
        int frameIndex = -1;
        
        for (int i = 0; i < pages.length; i++) {
            if (!pages[i]) {
                frameIndex = i;
                frameFound = true;
                break;
            }
        }
        
        if (!frameFound) {
            throw new OutOfMemoryError("Não há memória suficiente para alocar a primeira página do programa.");
        }
        
        // Aloca apenas a primeira página
        pages[frameIndex] = true;
        pageTable[0] = frameIndex;
        allocatedFrames.add(frameIndex); // Adiciona o frame à lista de frames alocados para o algoritmo do relógio
        
        System.out.println("Alocando primeira página do programa no frame " + frameIndex);
        
        // Copia o conteúdo da primeira página para a memória
        int startAddress = frameIndex * pageSize;
        int wordsInFirstPage = Math.min(pageSize, qtdWords);
        
        for (int offset = 0; offset < wordsInFirstPage; offset++) {
            mem.pos[startAddress + offset] = new Word(
                    programImage[offset].opc,
                    programImage[offset].ra,
                    programImage[offset].rb,
                    programImage[offset].p);
        }
        
        // Copia o restante do programa para a memória secundária
        for (int page = 1; page < qtdPages; page++) {
            int secMemStartAddr = page * pageSize;
            int programStartAddr = page * pageSize;
            
            for (int offset = 0; offset < pageSize; offset++) {
                int wordIndex = programStartAddr + offset;
                if (wordIndex >= qtdWords) {
                    break;
                }
                
                secMem.pos[secMemStartAddr + offset] = new Word(
                        programImage[wordIndex].opc,
                        programImage[wordIndex].ra,
                        programImage[wordIndex].rb,
                        programImage[wordIndex].p);
            }
        }
        
        return pageTable;
    }

    // Método original para compatibilidade
    public int[] aloca(Word[] p) {
        int qtdWords = p.length; // Quantidade de palavras a serem alocadas
        int qtdPages = (int) Math.ceil((double) qtdWords / pageSize); // Calcula o número de páginas necessárias
        int[] allocatedPages = new int[qtdPages]; // Array para armazenar as páginas alocadas
        int allocatedCount = 0; // Contador de páginas alocadas

        //Verifica a quantidade de páginas disponíveis
        int qtdAvailablePages = 0;
        for (boolean page : pages) {
            if (!page) {
                qtdAvailablePages++;
            }
        }

        if (qtdAvailablePages < qtdPages) {
            throw new OutOfMemoryError("Não há memória suficiente para alocar o programa.");
        }

        switch (allocMethod) {
            case 0 -> {
                // Percorre as páginas disponíveis para encontrar espaço usando "first fit"
                for (int i = 0; i < pages.length; i++) {
                    if (pages[i]) { // Página indisponível
                        System.out.println("Página " + i + " indisponível");
                        continue;
                    };
                    
                    System.out.println("Alocando página " + i + " na memória.");
                    
                    pages[i] = true;
                    allocatedPages[allocatedCount] = i;
                    allocatedCount++;
                    allocatedFrames.add(i); // Adiciona o frame à lista para o algoritmo do relógio
                    
                    // Copia o conteúdo para a memória
                    int startAddress = i * pageSize;
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
                        return allocatedPages;
                    }
                }
                
                // Se não foi possível alocar todas as páginas, desaloca as páginas alocadas
                for (int j = 0; j < allocatedCount; j++) {
                    pages[allocatedPages[j]] = false;
                    allocatedFrames.remove(Integer.valueOf(allocatedPages[j]));
                }  

                throw new OutOfMemoryError("Não há memória suficiente para alocar o programa.");
            }
            case 1 -> {
                int[] freePages = new int[qtdAvailablePages];

                for (int i = 0, j = 0; i < pages.length; i++) {
                    if (!pages[i]) { // Página livre
                        freePages[j] = i;
                        j++;
                    }
                }
                //Embaralha as páginas livres
                freePages = embaralhaArray(freePages);

                //Pegas as primeiras páginas livres do array embaralhado e aloca nelas
                for (int i = 0; i < freePages.length; i++) {
                    if (allocatedCount == qtdPages) {
                        return allocatedPages;
                    }

                    int pageIndex = freePages[i];
                    pages[pageIndex] = true;
                    allocatedPages[allocatedCount] = pageIndex;
                    allocatedCount++;
                    allocatedFrames.add(pageIndex); // Adiciona o frame à lista para o algoritmo do relógio

                    System.out.println("Alocando página " + pageIndex + " na memória.");

                    // Copia o conteúdo para a memória
                    int startAddress = pageIndex * pageSize;
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
                    return allocatedPages;
                }
            }
        }

        throw new OutOfMemoryError("Não há memória suficiente para alocar o programa.");
    }

    public void desaloca(int[] pageTable) {
        for (int frame : pageTable) {
            if (frame >= 0 && frame < pages.length) {
                pages[frame] = false;
                allocatedFrames.remove(Integer.valueOf(frame)); // Remove o frame da lista do algoritmo do relógio
                System.out.println("Desalocando página " + frame + " da memória.");
            }
        }
    }

    // Método MMU modificado para verificar se a página está na memória
    public int mmu(int enderecoLogico) {
        // Verifica se há um processo em execução no momento
        if (pm.processRunning == null) {
            throw new IllegalStateException("Nenhum processo está em execução no momento.");
        }

        // Obtém o PCB do processo em execução
        PCB pcb = pm.processRunning;

        // Calcula o índice da página e o deslocamento dentro da página
        int indicePagina = enderecoLogico / pageSize;
        int deslocamento = enderecoLogico % pageSize;

        // Verifica se o endereço lógico está no range das tabelas alocadas
        if (indicePagina < 0 || indicePagina >= pcb.pageTable.length) {
            throw new IllegalArgumentException(
                "Endereço lógico " + enderecoLogico + " fora do intervalo das tabelas de páginas alocadas (" + (pcb.pageTable.length * pageSize) + ")"
            );
        }

        // Marca a página como acessada (para o algoritmo do relógio)
        pcb.pageAccessed[indicePagina] = true;

        // Verifica se a página está presente na memória
        if (!pcb.pagePresent[indicePagina]) {
            // Página não está na memória, gera page fault
            System.out.println("Page fault! Página " + indicePagina + " não está na memória.");
            
            // Salva informações para o tratamento do page fault
            pm.processRunning.reg[8] = enderecoLogico; // Salva o endereço que causou o page fault
            
            // Gera interrupção de page fault
            pm.processRunning.saveContext(pm.processRunning.reg, pm.processRunning.pc);
            pm.processBlocked.add(pm.processRunning);
            pm.processRunning = null;
            
            // Inicia o tratamento do page fault
            handlePageFault(enderecoLogico);
            
            // Retorna -1 para indicar que houve page fault
            return -1;
        }

        // Obtém o frame físico correspondente
        int frameFisico = pcb.pageTable[indicePagina];

        // Verifica se o frame físico está no range da memória física
        if (frameFisico < 0 || frameFisico >= pages.length) {
            throw new IllegalArgumentException("Frame físico fora do intervalo da memória física.");
        }

        // Transforma o endereço lógico em endereço físico
        int enderecoFisico = frameFisico * pageSize + deslocamento;

        // Retorna o endereço físico
        return enderecoFisico;
    }

    // Método para tratar page fault
    public void handlePageFault(int enderecoLogico) {
        // Obtém o PCB do processo que gerou o page fault (último processo bloqueado)
        PCB pcb = pm.processBlocked.get(pm.processBlocked.size() - 1);
        
        // Calcula o índice da página que gerou o page fault
        int indicePagina = enderecoLogico / pageSize;
        
        System.out.println("Tratando page fault para processo " + pcb.pid + ", página " + indicePagina);
        
        // Tenta encontrar um frame livre
        int frameIndex = -1;
        for (int i = 0; i < pages.length; i++) {
            if (!pages[i]) {
                frameIndex = i;
                break;
            }
        }
        
        // Se não encontrou frame livre, precisa escolher uma vítima usando o algoritmo do relógio
        if (frameIndex == -1) {
            System.out.println("Não há frames livres. Usando algoritmo do relógio para escolher vítima.");
            frameIndex = clockAlgorithm();
            
            // Identifica o processo e a página que ocupam o frame vítima
            for (PCB process : pm.getAllProcesses()) {
                if (process == null) continue;
                
                for (int i = 0; i < process.pageTable.length; i++) {
                    if (process.pagePresent[i] && process.pageTable[i] == frameIndex) {
                        victimProcessId = process.pid;
                        victimPageNumber = i;
                        victimFrameNumber = frameIndex;
                        
                        System.out.println("Vítima escolhida: processo " + victimProcessId + ", página " + victimPageNumber + ", frame " + victimFrameNumber);
                        
                        // Marca a página como não presente e salva o frame para restauração posterior
                        process.pagePresent[i] = false;
                        process.pageFrames[i] = i; // Endereço no disco
                        
                        // Envia requisição para salvar a página no disco
                        DiskDevice.DiskRequest request = new DiskDevice.DiskRequest(
                            DiskDevice.DiskRequest.RequestType.SAVE_PAGE,
                            i,
                            frameIndex,
                            process.pid
                        );
                        
                        diskDevice.queueRequest(request);
                        
                        // Salva informações do page fault pendente
                        pendingPageFaultPID = pcb.pid;
                        pendingPageNumber = indicePagina;
                        pendingFrameNumber = frameIndex;
                        
                        return; // Aguarda a conclusão da operação de salvamento
                    }
                }
            }
        } else {
            // Se encontrou um frame livre, marca como ocupado
            pages[frameIndex] = true;
            allocatedFrames.add(frameIndex);
            
            // Atualiza a tabela de páginas do processo
            pcb.pageTable[indicePagina] = frameIndex;
            pcb.pagePresent[indicePagina] = true;
            pcb.pageFrames[indicePagina] = frameIndex;
            pcb.pageAccessed[indicePagina] = true;
            
            // Envia requisição para carregar a página do disco
            DiskDevice.DiskRequest request = new DiskDevice.DiskRequest(
                DiskDevice.DiskRequest.RequestType.LOAD_PAGE,
                indicePagina,
                frameIndex,
                pcb.pid
            );
            
            diskDevice.queueRequest(request);
            
            // Salva informações do page fault pendente
            pendingPageFaultPID = pcb.pid;
            pendingPageNumber = indicePagina;
            pendingFrameNumber = frameIndex;
        }
    }
    
    // Método para tratar a conclusão do salvamento de uma página no disco
    public void handleDiskSaveComplete() {
        System.out.println("Salvamento de página no disco concluído.");
        
        // Libera o frame da vítima
        pages[victimFrameNumber] = true; // Marca como ocupado para o novo processo
        
        // Envia requisição para carregar a página do processo que gerou o page fault
        DiskDevice.DiskRequest request = new DiskDevice.DiskRequest(
            DiskDevice.DiskRequest.RequestType.LOAD_PAGE,
            pendingPageNumber,
            victimFrameNumber,
            pendingPageFaultPID
        );
        
        diskDevice.queueRequest(request);
    }
    
    // Método para tratar a conclusão do carregamento de uma página do disco
    public void handleDiskLoadComplete() {
        System.out.println("Carregamento de página do disco concluído.");
        
        // Encontra o PCB do processo que gerou o page fault
        PCB pcb = null;
        for (PCB process : pm.processBlocked) {
            if (process.pid == pendingPageFaultPID) {
                pcb = process;
                break;
            }
        }
        
        if (pcb != null) {
            // Atualiza a tabela de páginas do processo
            pcb.pageTable[pendingPageNumber] = pendingFrameNumber;
            pcb.pagePresent[pendingPageNumber] = true;
            pcb.pageFrames[pendingPageNumber] = pendingFrameNumber;
            pcb.pageAccessed[pendingPageNumber] = true;
            
            // Move o processo de bloqueado para pronto
            pm.processBlocked.remove(pcb);
            pm.processReady.add(pcb);
            
            System.out.println("Processo " + pcb.pid + " movido de bloqueado para pronto após page fault.");
            
            // Limpa as informações pendentes
            pendingPageFaultPID = -1;
            pendingPageNumber = -1;
            pendingFrameNumber = -1;
            victimProcessId = -1;
            victimPageNumber = -1;
            victimFrameNumber = -1;
        }

    }
    
    // Método para tratar a conclusão de uma operação de IO
    public void handleIOComplete() {
        System.out.println("Operação de IO concluída.");
        // Implementação específica para IO não relacionada à paginação
    }
    
    // Método para salvar uma página da memória principal para o disco
    public void savePageToDisk(int pageNumber, int frameNumber, int processId) {
        // Simula a cópia da página da memória principal para a memória secundária
        int memStartAddr = frameNumber * pageSize;
        int diskStartAddr = pageNumber * pageSize;

        for (int i = 0; i < pageSize; i++) {
            secMem.pos[diskStartAddr + i] = new Word(
                    mem.pos[memStartAddr + i].opc,
                    mem.pos[memStartAddr + i].ra,
                    mem.pos[memStartAddr + i].rb,
                    mem.pos[memStartAddr + i].p
            );
        }

        System.out.println("Página " + pageNumber + " do processo " + processId + " salva no disco.");

        // Em vez de usar pm.processRunning, vamos buscar o PCB correto pelo PID:
        PCB pcb = null;
        for (PCB p : pm.processBlocked) {
            if (p.pid == processId) {
                pcb = p;
                break;
            }
        }
        if (pcb != null) {
            // Salva o contexto do PCB bloqueado (ele já não está em execução)
            pcb.saveContext(pcb.reg, pcb.pc);
        }

        // Simula a interrupção (chama o handler de "disk save complete")
        handleDiskSaveComplete();
    }
    
    // Método para carregar uma página do disco para a memória principal
    public void loadPageFromDisk(int pageNumber, int frameNumber, int processId) {
        // Simula a cópia da página da memória secundária para a memória principal
        int diskStartAddr = pageNumber * pageSize;
        int memStartAddr = frameNumber * pageSize;

        for (int i = 0; i < pageSize; i++) {
            mem.pos[memStartAddr + i] = new Word(
                    secMem.pos[diskStartAddr + i].opc,
                    secMem.pos[diskStartAddr + i].ra,
                    secMem.pos[diskStartAddr + i].rb,
                    secMem.pos[diskStartAddr + i].p
            );
        }

        System.out.println("Página " + pageNumber + " do processo " + processId + " carregada do disco.");

        // Em vez de usar pm.processRunning, buscamos o PCB correspondente ao processId:
        PCB pcb = null;
        for (PCB p : pm.processBlocked) {
            if (p.pid == processId) {
                pcb = p;
                break;
            }
        }
        if (pcb != null) {
            // Salva o contexto do PCB (ele estava bloqueado aguardando esta página)
            pcb.saveContext(pcb.reg, pcb.pc);
        }

        // Simula a interrupção de load completo
        handleDiskLoadComplete();
    }
    
    // Implementação do algoritmo do relógio para escolha de página vítima
    private int clockAlgorithm() {
        if (allocatedFrames.isEmpty()) {
            throw new IllegalStateException("Não há frames alocados para aplicar o algoritmo do relógio.");
        }
        
        System.out.println("Executando algoritmo do relógio para escolher vítima.");
        
        while (true) {
            // Avança o ponteiro do relógio
            if (clockHand >= allocatedFrames.size()) {
                clockHand = 0;
            }
            
            int frameToCheck = allocatedFrames.get(clockHand);
            
            // Encontra o processo e a página que ocupam este frame
            for (PCB process : pm.getAllProcesses()) {
                if (process == null) continue;
                
                for (int i = 0; i < process.pageTable.length; i++) {
                    if (process.pagePresent[i] && process.pageTable[i] == frameToCheck) {
                        // Verifica o bit de acesso
                        if (process.pageAccessed[i]) {
                            // Página foi acessada recentemente, reseta o bit e continua
                            System.out.println("Frame " + frameToCheck + " (processo " + process.pid + ", página " + i + ") foi acessado recentemente. Resetando bit.");
                            process.pageAccessed[i] = false;
                            clockHand++;
                            break;
                        } else {
                            // Página não foi acessada recentemente, escolhe como vítima
                            System.out.println("Frame " + frameToCheck + " (processo " + process.pid + ", página " + i + ") escolhido como vítima.");
                            clockHand++;
                            return frameToCheck;
                        }
                    }
                }
            }
            
            // Se chegou aqui, avança para o próximo frame
            clockHand++;
        }
    }

    private int[] embaralhaArray(int[] array) {
        // Converte o array para uma lista
        Integer[] boxedArray = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            boxedArray[i] = array[i];
        }
        
        List<Integer> list = new ArrayList<>();
        for (Integer value : boxedArray) {
            list.add(value);
        }

        // Embaralha a lista
        java.util.Collections.shuffle(list);

        // Converte a lista de volta para um array
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }

        return array;
    }
}
