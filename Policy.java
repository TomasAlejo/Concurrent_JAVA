package main;

import java.util.HashMap;
import java.util.Map;

public class Policy {

    private final int selectedPolicy;

    // Round robin con peso para política 2 (4-1 -> T11, T12)
    private final Map<Integer, Integer> weightCursor = new HashMap<>();

    // Índices de transiciones

    private static final int T2 = 2;
    private static final int T5 = 5;
    private static final int T7 = 7;



    public Policy(int selectedPolicy) {
        this.selectedPolicy = selectedPolicy;
    }

    /**
     * Política 1: Elige al azar entre los 3 modos de procesamiento disponibles
     * Elige al azar entre T2, T5 y T7,
     * luego no hay mas conflictos y se elige la unica transicion habilitada disponible.
     * Política 2: Round Robin 80/20/20 (T5/T2/T7) para priorizar procesamiento simple
     */
    public int choose(int place, int[] enabledTransitions) {
        if (enabledTransitions == null || enabledTransitions.length == 0) return -1;

        if (selectedPolicy == 1) {
            return policy1(place, enabledTransitions);
        } else if (selectedPolicy == 2) {
            return policy2(place, enabledTransitions);
        }

        // Política no reconocida
        return -2;
    }


    // POLÍTICA 1
    private int policy1(int place, int[] enabledTransitions) {

        if (enabledTransitions == null || enabledTransitions.length == 0) {
            return -1;
        }

        // Una sola transición habilitada
        if (enabledTransitions.length == 1) {
            return enabledTransitions[0];
        }

        // Si hay varias transiciones, se asigna un random
        double r = Math.random(); // [0.0, 1.0)

        double step = 1.0 / enabledTransitions.length;
        int idx = (int) (r / step); //Asigna el indice segun r y el paso

        // Protección por redondeos
        if (idx >= enabledTransitions.length) {
            idx = enabledTransitions.length - 1;
        }

        return enabledTransitions[idx];
    }





    //   POLÍTICA 2
    private int policy2(int place, int[] enabledTransitions) {

        // Si no hay ninguna transición habilitada
        if (enabledTransitions == null || enabledTransitions.length == 0) {
            return -1;
        }

        // Vemos si están T2, T5 y T7 habilitadas
        boolean hasT2 = false;
        boolean hasT5 = false;
        boolean hasT7 = false;

        for (int T : enabledTransitions) {
            if (T == T2) hasT2 = true;
            if (T == T5) hasT5 = true;
            if (T == T7) hasT7 = true;
        }


        // CONFLICTO T2/T5/T7
        if (hasT2 && hasT5 && hasT7) {
            /*
             * RR ponderado 60/20/20:
             * Cursor de 5 estados
             * 0,1,2: T5
             * 3    : T2
             * 4    : T7
             */

            int counter = weightCursor.getOrDefault(place, 0); // 0..4
            int chosen;

            if (counter < 3) {          // 0,1,2
                chosen = T5;        // 60%
            } else if (counter == 3) {  // 3
                chosen = T2;        // 20%
            } else {                    // 4
                chosen = T7;        // 20%
            }

            weightCursor.put(place, (counter + 1) % 5);
            return chosen;
        }


        // Sin conflicto
        // Se elije la primera habilitada
        return enabledTransitions[0];
    }


    public int getSelectedPolicy(){
        return selectedPolicy;
    }
}
