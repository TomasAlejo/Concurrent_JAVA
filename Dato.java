package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Dato {

    private static final AtomicInteger idx = new AtomicInteger(0);
    private final int id;

    private final List<String> processing = new ArrayList<>();

    public Dato() {
        this.id = idx.incrementAndGet();
    }

    //public int getId() {
    //    return id;
    //}

    // marcas
    public void markSimple() {
        processing.add("simple");
    }

    public void markMedium() {
        processing.add("medio");
    }

    public void markHigh() {
        processing.add("alto");
    }

    @Override
    public String toString() {
        return "Dato{id=" + id +
                ", procesado=" + processing +
                '}';
    }
}
