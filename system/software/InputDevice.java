package system.software;

import java.util.LinkedList;
import java.util.Queue;

public class InputDevice {
    private Queue<Integer> queue = new LinkedList<>();

    public void addToQueue(int value) {
        queue.add(value);
    }

    public Integer readFromQueue() {
        return queue.poll();
    }
}
