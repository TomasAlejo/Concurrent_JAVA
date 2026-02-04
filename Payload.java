package main;

public final class Payload {

    private static final ThreadLocal<Dato> payload =
            ThreadLocal.withInitial(() -> null);

    private static final ThreadLocal<Integer> lastFired =
            ThreadLocal.withInitial(() -> null);


    // Metodos para manipular el Dato
    public static void set(Dato d) { payload.set(d); }
    public static Dato get() { return payload.get(); }
    public static void clear() { payload.remove(); }

    // Metodos para obtener informacion de disparos
    public static void setLastFired(int t) { lastFired.set(t); }
    public static Integer getLastFired() { return lastFired.get(); }
    public static void clearLastFired() { lastFired.remove(); }
}
