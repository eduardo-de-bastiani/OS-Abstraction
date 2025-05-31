package system.os;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Classe que representa um dispositivo de IO para paginação
 * Implementa uma thread que processa requisições de IO
 */
public class DiskDevice implements Runnable {
    private final BlockingQueue<DiskRequest> requestQueue;
    private final MemoryManager mm;
    private boolean running;
    private Thread deviceThread;

    public DiskDevice(MemoryManager mm) {
        this.mm = mm;
        this.requestQueue = new LinkedBlockingQueue<>();
        this.running = true;
    }

    public void start() {
        deviceThread = new Thread(this);
        deviceThread.setDaemon(true);
        deviceThread.start();
        System.out.println("Dispositivo de disco iniciado");
    }

    public void stop() {
        running = false;
        deviceThread.interrupt();
    }

    @Override
    public void run() {
        while (running) {
            try {
                // Espera por uma requisição na fila
                DiskRequest request = requestQueue.take();
                
                // Simula o tempo de acesso ao disco
                Thread.sleep(100);
                
                // Processa a requisição
                switch (request.type) {
                    case LOAD_PAGE:
                        System.out.println("Carregando página " + request.pageNumber + " do disco para o frame " + request.frameNumber);
                        mm.loadPageFromDisk(request.pageNumber, request.frameNumber, request.processId);
                        break;
                    case SAVE_PAGE:
                        System.out.println("Salvando página " + request.pageNumber + " do frame " + request.frameNumber + " para o disco");
                        mm.savePageToDisk(request.pageNumber, request.frameNumber, request.processId);
                        break;
                }
            } catch (InterruptedException e) {
                if (!running) {
                    break;
                }
            }
        }
    }

    public void queueRequest(DiskRequest request) {
        try {
            requestQueue.put(request);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static class DiskRequest {
        public enum RequestType {
            LOAD_PAGE,
            SAVE_PAGE
        }

        public final RequestType type;
        public final int pageNumber;
        public final int frameNumber;
        public final int processId;

        public DiskRequest(RequestType type, int pageNumber, int frameNumber, int processId) {
            this.type = type;
            this.pageNumber = pageNumber;
            this.frameNumber = frameNumber;
            this.processId = processId;
        }
    }
}
