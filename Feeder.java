package main;

public class Feeder implements Runnable {

    private final Monitor monitor;
    private final int maxDatos;
    private int produced;
    private String threadName;

    private static final int T12 = 12;

    public Feeder(Monitor monitor, int maxDatos) {
        this.monitor = monitor;
        this.maxDatos = maxDatos;
        this.produced = 0;
    }

    @Override
    public void run() {
        threadName = Thread.currentThread().getName();
        System.out.println("Feeder " + threadName + " started");

        while (produced < maxDatos) {

            // creamos nuestro dato
            Dato dato = new Dato();

            // lo dejamos en el payload de nuestro hilo
            Payload.set(dato);

            // disparar T12, el inicio de la invariante, añade ese dato a P0
            boolean valid = monitor.fireTransition(T12);

            if (valid) {
                produced++;
            }
        }

        System.out.printf("[%s] Feeder finalizado. Produjo %d datos.%n",
                threadName, produced);
    }
}
