package main;

public class Loader implements Runnable {

    private final Monitor monitor;
    private final int maxDatos;
    private int threadLoads = 0;
    private String threadName;

    // Transiciones asociadas a la etapa de cargado "loader"
    private static final int T0 = 0; // entryBuffer REMOVE
    private static final int T1 = 1; // loadBuffer  ADD

    public Loader(Monitor monitor, int maxDatos) {
        this.monitor = monitor;
        this.maxDatos = maxDatos;
    }

    @Override
    public void run() {
        threadName = Thread.currentThread().getName();
        System.out.println("Loader " + threadName + " started");

        while (threadLoads < maxDatos) {

            // Disparo T0, se consume tokens desde entryBuffer y ese elemento sacado de entryBuffer lo guardo en Payload
            boolean valid = monitor.fireTransition(T0);
            if (!valid) {
                // Si no pudo disparar (en caso de que se termine el programa)
                break;
            }

            Dato dato = Payload.get();
            if (dato == null) break; // no deberia pasar pero por las dudas...

            // Ya con mi dato obtenido, acá simulo que el dato está en "P1"
            Payload.set(dato);

            // Disparo T1, añade elemento a loadBuffer
            // y el monitor limpia el payload ya que acá termina nuestra etapa y por ende, la responsabilidad del hilo
            valid = monitor.fireTransition(T1);
            if (!valid) {
                break;
            }

            threadLoads++;
        }

        System.out.printf("[%s] Loader finalizado. Procesó %d datos.%n",
                threadName, threadLoads);
    }
}
