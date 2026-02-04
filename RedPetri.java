package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedPetri {

    private final Policy policy;

    private StringBuilder sequence = new StringBuilder();

    private static final String[] transiciones = {
            "T0","T1","T2","T3","T4","T5","T6","T7","T8","T9","T10","T11","T12"
    };

    private static final String[] plazas = {
            "P0","P1","P2","P3","P4","P5","P6","P7","P8","P9","P10","P11"
    };


    // transición mapping >> condicion,operaion
    private final Map<Integer, BufferMapping> transitionBufferMapping = new HashMap<>();

    //  Matriz incidencia
    private final int[][] incidence_matrix = {
            //T0 T1  T2 T3 T4 T5 T6 T7 T8  T9  T10 T11 T12
            {-1,  0,  0, 0, 0, 0, 0, 0, 0,  0,  0,  0,  1}, //P0
            { 1, -1,  0, 0, 0, 0, 0, 0, 0,  0,  0,  0,  0}, //P1
            {-1,  1,  0, 0, 0, 0, 0, 0, 0,  0,  0,  0,  0}, //P2
            { 0,  1, -1, 0, 0,-1, 0,-1, 0,  0,  0,  0,  0}, //P3
            { 0,  0,  1,-1, 0, 0, 0, 0, 0,  0,  0,  0,  0}, //P4
            { 0,  0,  0, 1,-1, 0, 0, 0, 0,  0,  0,  0,  0}, //P5
            { 0,  0, -1, 0, 1,-1, 1,-1, 0,  0,  1,  0,  0}, //P6
            { 0,  0,  0, 0, 0, 1,-1, 0, 0,  0,  0,  0,  0}, //P7
            { 0,  0,  0, 0, 0, 0, 0, 1,-1,  0,  0,  0,  0}, //P8
            { 0,  0,  0, 0, 0, 0, 0, 0, 1, -1,  0,  0,  0}, //P9
            { 0,  0,  0, 0, 0, 0, 0, 0, 0,  1, -1,  0,  0}, //P10
            { 0,  0,  0, 0, 1, 0, 1, 0, 0,  0,  1, -1,  0}, //P11
    };


    //                                      P0 P1 P2 P3 P4 P5 P6 P7 P8 P9 P10 P11
    private final int[] startingMarking = {  1, 0, 1, 0, 0, 0, 1, 0, 0, 0,  0,  0 };
    private int[] marking = startingMarking.clone();

    //                                      T0 T1 T2 T3 T4 T5 T6 T7 T8 T9 T10 T11 T12
    private int[] transitionsFiredCount = {  0, 0, 0, 0, 0, 0, 0, 0, 0, 0,  0,  0,  0 };
    private static final int FINAL_TRANSITION = 11;


    private int M(String place) {
        switch (place) {
            case "P0":  return marking[0];
            case "P1":  return marking[1];
            case "P2":  return marking[2];
            case "P3":  return marking[3];
            case "P4":  return marking[4];
            case "P5":  return marking[5];
            case "P6":  return marking[6];
            case "P7":  return marking[7];
            case "P8":  return marking[8];
            case "P9":  return marking[9];
            case "P10": return marking[10];
            case "P11": return marking[11];
            default: throw new IllegalArgumentException("Plaza desconocida: " + place);
        }
    }


    //  TIEMPOS
    private final long[] alphaMs = new long[transiciones.length];
    private final int alphaTime;

    private final long[] enabledSince = new long[transiciones.length]; // ns
    private final boolean[] timerActive = new boolean[transiciones.length];


    //  CONDICIONES Y BUFFERS (para el monitor)
    private final Map<String, int[]> transitionsConditions = new HashMap<>();

    public RedPetri(Policy policy, int time) {
        this.policy = policy;
        this.alphaTime = time;

        for (int i = 0; i < transiciones.length; i++) {
            alphaMs[i] = 0L;
            enabledSince[i] = 0L;
            timerActive[i] = false;
        }

        // α = 60 ms para T12,T1,T3,T4,T6,T8,T9,T10
        setAlpha("T1",  alphaTime);
        setAlpha("T3",  alphaTime);
        setAlpha("T4",  alphaTime);
        setAlpha("T6",  alphaTime);
        setAlpha("T8",  alphaTime);
        setAlpha("T9",  alphaTime);
        setAlpha("T10", alphaTime);
        setAlpha("T12", alphaTime);


        //  Transiciones y su condicion asociadas
        transitionsConditions.put("canLoad", new int[]{12, 0}); //Desde entryBuffer, T12 produce, T0 consume
        transitionsConditions.put("canProcess", new int[]{1, 2, 5, 7}); //Desde loadBuffer, T1 produce, T2/T5/T7 consumen
        transitionsConditions.put("canExport", new int[]{4, 6, 10, 11}); //Desde exportBuffer, T4/T6/T10 producen, T11 consume

        //  Transiones asociadas a su buffer y operacion a ejecutar
        transitionBufferMapping.put(12, new BufferMapping("entryBuffer",  BufferMapping.Operation.ADD));
        transitionBufferMapping.put(1,  new BufferMapping("loadBuffer",   BufferMapping.Operation.ADD));
        transitionBufferMapping.put(4,  new BufferMapping("exportBuffer", BufferMapping.Operation.ADD));
        transitionBufferMapping.put(6,  new BufferMapping("exportBuffer", BufferMapping.Operation.ADD));
        transitionBufferMapping.put(10, new BufferMapping("exportBuffer", BufferMapping.Operation.ADD));
        //
        transitionBufferMapping.put(0,  new BufferMapping("entryBuffer",  BufferMapping.Operation.REMOVE));
        transitionBufferMapping.put(2,  new BufferMapping("loadBuffer",   BufferMapping.Operation.REMOVE));
        transitionBufferMapping.put(5,  new BufferMapping("loadBuffer",   BufferMapping.Operation.REMOVE));
        transitionBufferMapping.put(7,  new BufferMapping("loadBuffer",   BufferMapping.Operation.REMOVE));
        transitionBufferMapping.put(11, new BufferMapping("exportBuffer", BufferMapping.Operation.REMOVE));

    }

    private void setAlpha(String transition, long alphaMs) {
        for (int i = 0; i < transiciones.length; i++) {
            if (transiciones[i].equals(transition)) {
                this.alphaMs[i] = alphaMs;
                return;
            }
        }
        throw new IllegalArgumentException("Transición desconocida: " + transition);
    }

    public Map<String, int[]> getTransitionsConditions() {
        return Collections.unmodifiableMap(transitionsConditions);
    }


    //  Tiempo
    public void initTiming(int T) {
        if (!timerActive[T]) {
            timerActive[T]  = true;
            enabledSince[T] = System.nanoTime();
        }
    }

    public void resetTiming(int T) {
        timerActive[T]  = false;
        enabledSince[T] = 0L;
    }

    public long getRemainingTime(int T) {
        if (!timerActive[T]) return alphaMs[T];
        long now = System.nanoTime();
        long elapsedMs = (now - enabledSince[T]) / 1_000_000L;
        long remaining = alphaMs[T] - elapsedMs;
        return Math.max(0L, remaining);
    }

    //Sensibilizado
    public boolean canFire(int T) {
        for (int P = 0; P < incidence_matrix.length; P++) {
            int necessaryTokens = incidence_matrix[P][T];
            if (necessaryTokens < 0 && marking[P] < -necessaryTokens) return false;
        }
        return true;
    }

    public boolean canFireTemp(int T) {
        if (!timerActive[T]) return false;
        long now = System.nanoTime();
        long elapsedMs = (now - enabledSince[T]) / 1_000_000L;
        return elapsedMs >= alphaMs[T];
    }


    //  Uso de politica
    public int whichFire(int P) {
        int[] enabled = getAvailableTransitions(P);
        int chosen = policy.choose(P, enabled);

        if (chosen == -1) {
            return -1;
        } else if (chosen == -2) {
            return -2;
        }

        System.out.println("[" + Thread.currentThread().getName() + "] Desde "
                + plazas[P] + " (tokens = " + marking[P] + ") se disparará " + transiciones[chosen]);

        return chosen;
    }

    //Comprobacion de invariantes de plaza
    public boolean verifyPInvariants() {
        boolean valid = true;

        // I1: M(P1) + M(P2) = 1
        int I1 = M("P1") + M("P2");
        if (I1 != 1) {
            System.err.println("Invariante de Plaza I1 violada: valor = " + I1 + " (debería ser 1)");
            valid = false;
        }

        // I2: M(P10) + M(P4) + M(P5) + M(P6) + M(P7) + M(P8) + M(P9) = 1
        int I2 = M("P10") +M("P4") + M("P5") + M("P6") + M("P7") + M("P8") + M("P9");
        if (I2 != 1) {
            System.err.println("Invariante de Plaza I2 violada: valor = " + I2 + " (debería ser 1)");
            valid = false;
        }

        return valid;
    }


    //  Disparo
    public void fire(int T) {
        if (alphaMs[T] > 0 && enabledSince[T] != 0L) {
            long now = System.nanoTime();
            long elapsedMs = (now - enabledSince[T]) / 1_000_000L;

            System.out.printf(
                    "[%s] Dispara %s — tiempo transcurrido: %d ms (α = %d ms)%n",
                    Thread.currentThread().getName(),
                    transiciones[T],
                    elapsedMs,
                    alphaMs[T]
            );
        }

        for (int P = 0; P < incidence_matrix.length; P++) {
            marking[P] += incidence_matrix[P][T];
        }

        sequence.append(transiciones[T]);
        transitionsFiredCount[T]++;

        // Verificar invariantes
        if (!verifyPInvariants()) {
            System.err.println("Violacion de Invariantes de Plaza despues de disparar:" + transiciones[T]);
        }
    }

    // GETTERS

    public int[] getAvailableTransitions(int P) {
        List<Integer> out = new ArrayList<>();
        for (int T = 0; T < incidence_matrix[0].length; T++) {
            if (incidence_matrix[P][T] < 0 && canFire(T)) out.add(T);
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    public String getSequence() {
        return sequence.toString().trim();
    }

    public int[] getTransitionsFiredCount() {
        return transitionsFiredCount;
    }

    public int[] getTimedTransitions() {
        List<Integer> timed = new ArrayList<>();
        for (int i = 0; i < alphaMs.length; i++) {
            if (alphaMs[i] > 0) timed.add(i);
        }
        return timed.stream().mapToInt(Integer::intValue).toArray();
    }

    public int getPlacefromTransition(int T) {
        for (int P = 0; P < incidence_matrix.length; P++) {
            if (incidence_matrix[P][T] < 0) return P;
        }
        throw new IllegalArgumentException("La transición " + T + " no tiene plaza de entrada.");
    }

    public Map<Integer, BufferMapping> getTransitionBufferMapping() {
        return Collections.unmodifiableMap(transitionBufferMapping);
    }

    public int getFinalTransition(){
        return FINAL_TRANSITION;
    }

}
