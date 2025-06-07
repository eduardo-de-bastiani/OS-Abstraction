package system.software;

import java.util.LinkedList;
import java.util.Queue;

public class InputDevice {
    private static final InputDevice instance = new InputDevice();
    private Queue<Integer> queue = new LinkedList<>();

    private InputDevice() {
    }

    public static InputDevice getInstance() {
        return instance;
    }

    public synchronized Integer readFromQueue() {
        while (queue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return queue.poll();
    }

    public synchronized void addToQueue(int value) {
        queue.add(value);
        notifyAll();
    }
}
