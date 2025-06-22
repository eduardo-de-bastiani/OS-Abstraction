package system.software;

public class Clock {
    private final boolean[] referenceBits;
    private final int frameCount;
    private int pointer;

    /**
     * Construtor da classe Clock.
     * 
     * @param frameCount Número de frames (quadros) disponíveis.
     */
    public Clock(int frameCount) {
        if (frameCount <= 0) {
            throw new IllegalArgumentException("frameCount deve ser maior que zero.");
        }
        this.frameCount = frameCount;
        this.referenceBits = new boolean[frameCount];
        this.pointer = 0;
    }

    /**
     * Marca um frame como acessado (bit de referência = true).
     * 
     * @param frameIndex Índice do frame a ser marcado como acessado.
     */
    public void markFrameAccessed(int frameIndex) {
        if (frameIndex < 0 || frameIndex >= frameCount) {
            throw new IndexOutOfBoundsException("Índice de frame inválido.");
        }
        referenceBits[frameIndex] = true;
    }

    /**
     * Retorna o índice do próximo frame que deve ser substituído,
     * aplicando o algoritmo do relógio.
     * 
     * @return Índice do frame a ser vitimado.
     */
    public int getNextFrame() {
        while (true) {
            if (!referenceBits[pointer]) {
                int frameToReplace = pointer;
                pointer = (pointer + 1) % frameCount;
                return frameToReplace;
            } else {
                referenceBits[pointer] = false;
                pointer = (pointer + 1) % frameCount;
            }
        }
    }
// 

}
