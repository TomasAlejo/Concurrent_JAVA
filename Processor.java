package main;

public class Processor implements Runnable {

    private final Monitor monitor;
    private final int maxImages;
    private int threadProcessed = 0;
    private String threadName;

    // Transiciones asociadas a esta etapa
    private static final int T2 = 2;
    private static final int T3 = 3;
    private static final int T4 = 4;

    private static final int T5 = 5;
    private static final int T6 = 6;

    private static final int T7  = 7;
    private static final int T8  = 8;
    private static final int T9  = 9;
    private static final int T10 = 10;

    // Transicion de referencia para resolver los conflictos de esta etapa (P3)
    // A partir de esta, se puede disparar T2/T5/T7 (según politica)
    private static final int TREF = T2;

    public Processor(Monitor monitor, int maxImages) {
        this.monitor = monitor;
        this.maxImages = maxImages;
    }

    @Override
    public void run() {
        threadName = Thread.currentThread().getName();
        System.out.println("Processor " + threadName + " started");

        while (threadProcessed < maxImages) {

            // Paso la transicion de referencia para que el monitor resuelva el conflicto
            boolean valid = monitor.fireTransition(TREF);
            if (!valid) break;

            //Luego de que el monitor resuelva y dispare, necesito saber cual fué esa transicion
            Integer lastFired = Payload.getLastFired();
            if (lastFired == null) break;
            Payload.clearLastFired();

            Dato dato = Payload.get();
            if (dato == null) break;
            Payload.clear();

            // Ejecutamos el modo de procesamiento según el ultimo disparo
            switch (lastFired) {

                case T2:
                    // Entra primer etapa de procesado (Simple)
                    dato.markSimple();

                    // Se dispara T3 (intermedia)
                    valid = monitor.fireTransition(T3);
                    if (!valid) break;
                    dato.markMedium();

                    // Guardamos de vuelta el dato para que el monitor lo tome y lo pueda agregar a exportBuffer (T4)
                    Payload.set(dato);
                    valid = monitor.fireTransition(T4);
                    if (!valid) break;
                    break;

                case T5:
                    // Pasa por la unica etapa de procesado
                    dato.markSimple();

                    // Se dispara T6 (añade el dato a exportBuffer)
                    Payload.set(dato);
                    valid = monitor.fireTransition(T6);
                    if (!valid) break;
                    break;

                case T7:
                    // Se procesa por primera vez
                    dato.markSimple();
                    // Se dispara T8 (intermedia)
                    valid = monitor.fireTransition(T8);
                    if (!valid) break;
                    dato.markMedium();

                    // T9 (intermedia)
                    valid = monitor.fireTransition(T9);
                    if (!valid) break;
                    dato.markHigh();


                    // Procesado completo, T10 (dato a exportBuffer)
                    Payload.set(dato);
                    valid = monitor.fireTransition(T10);
                    if (!valid) break;
                    break;

                default:
                    valid = false;
                    break;
            }

            if (!valid) break;

            threadProcessed++;

            // Limpieza;
            Payload.clearLastFired();
        }

        // Limpieza de seguridad
        Payload.clearLastFired();

        System.out.printf("[%s] Processor finalizado. Procesó %d datos.%n",
                threadName, threadProcessed);
    }
}
