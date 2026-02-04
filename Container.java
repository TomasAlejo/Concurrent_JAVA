package main;

import java.util.LinkedList;
import java.util.Queue;

public class Container<T> {

    private final int capacity;     //
    private final Queue<T> queue = new LinkedList<>();

    public Container() {
        this.capacity = 0; // sin límite
    }

    public int size() {
        return queue.size();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }


    // Intenta agregar un elemento al contenedor.

    public boolean add(T item) {
        return queue.offer(item);
    }

    //Extrae y devuelve el siguiente elemento del contenedor.

    public T remove() {
        return queue.poll();
    }


    @Override
    public String toString() {
        return "Container{" +
                "size=" + queue.size() +
                ", capacity=" + (capacity <= 0 ? "unbounded" : capacity) +
                ", items=" + queue +
                '}';
    }

}
