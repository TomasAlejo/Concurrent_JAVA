package main;

public class Main {
    public static void main(String[] args) {

        final int selectedPolicy = 2; // politica a aplicar
        final int NUM_IMAGES = 200;   // cantidad de datos a procesar / invariantes q voy a tener
        final int msTime = 60;        // alpha time

        final int NUM_FEEDERS = 1;
        final int NUM_LOADERS = 1;
        final int NUM_PROCESSORS = 3;
        final int NUM_EXPORTERS = 1;

        Policy policy = new Policy(selectedPolicy);
        RedPetri rdp  = new RedPetri(policy, msTime);
        TerminationPolicy termination = new TerminationPolicy(rdp.getFinalTransition(), NUM_IMAGES);
        Monitor monitor = new Monitor(rdp, termination);

        Thread[] feeders = new Thread[NUM_FEEDERS];
        for (int i = 0; i < NUM_FEEDERS; i++) {
            feeders[i] = new Thread(new Feeder(monitor, NUM_IMAGES), "Feeder-" + i);
        }

        Thread[] loaders = new Thread[NUM_LOADERS];
        for (int i = 0; i < NUM_LOADERS; i++) {
            loaders[i] = new Thread(new Loader(monitor, NUM_IMAGES), "Loader-" + i);
        }

        Thread[] processors = new Thread[NUM_PROCESSORS];
        for (int i = 0; i < NUM_PROCESSORS; i++) {
            processors[i] = new Thread(new Processor(monitor, NUM_IMAGES), "Processor-" + i);
        }

        Thread[] exporters = new Thread[NUM_EXPORTERS];
        for (int i = 0; i < NUM_EXPORTERS; i++) {
            exporters[i] = new Thread(new Exporter(monitor, NUM_IMAGES), "Exporter-" + i);
        }

        // Medición real
        long t0 = System.nanoTime();

        // Inicializar todos los hilos
        for (Thread t : feeders)   t.start();
        for (Thread t : loaders)   t.start();
        for (Thread t : processors) t.start();
        for (Thread t : exporters) t.start();

        // Esperar a que terminen todos los workers
        try {
            for (Thread t : feeders)   t.join();
            for (Thread t : loaders)   t.join();
            for (Thread t : processors) t.join();
            for (Thread t : exporters) t.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main interrumpido mientras esperaba a los hilos.");
            return;
        }

        long t1 = System.nanoTime();
        double totalMS = (t1 - t0) / 1_000_000.0;

        // Una vez que tengo el tiempo total, recien ahí llamo al constructor de mi log
        Log logger = new Log(policy, rdp, monitor, totalMS);
        logger.run();

    }
}
