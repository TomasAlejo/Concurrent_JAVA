package main;

public class Exporter implements Runnable {

    private final Monitor monitor;
    private final int maxDatos;
    private int threadExports = 0;
    private String threadName;

    private static final int T11 = 11; // la transicion q consume exportBuffer y cierra invariante

    public Exporter(Monitor monitor, int maxDatos) {
        this.monitor = monitor;
        this.maxDatos = maxDatos;
    }

    @Override
    public void run() {
        threadName = Thread.currentThread().getName();
        System.out.println("Exporter " + threadName + " started");

        while (threadExports < maxDatos) {

            boolean valid = monitor.fireTransition(T11);
            if (!valid) break;

            // El dato removido del exportBuffer se encuentra en el Payload de este hilo
            Dato dato = Payload.get();
            if (dato == null) break;

            // Vemos como se procesó el dato
            System.out.println("[" + threadName + "] " + dato);

            // Limpieza
            Payload.clear();
            Payload.clearLastFired();

            threadExports++;
        }

        System.out.printf("[%s] Exporter finalizado. Exportó %d datos.%n",
                threadName, threadExports);
    }
}
