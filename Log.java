package main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Log implements Runnable {

    private final Policy policy;
    private final RedPetri redPetri;
    private final Monitor monitor;

    private final String filenameResultados;
    private final String filenameSecuencia;
    double totalMS;

    public Log(Policy policy, RedPetri redPetri, Monitor monitor, double totalMS) {
        this.policy = policy;
        this.redPetri = redPetri;
        this.monitor = monitor;
        this.totalMS = totalMS;

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));

        this.filenameResultados = "resultados_" + timestamp + ".log";
        this.filenameSecuencia  = "secuencia_" + timestamp + ".log";
    }

    @Override
    public void run() {


        System.out.println("[Log] Invariantes completadas. Generando archivos de resultados...");

        writeResults(totalMS);
        writeSequence();

        System.out.println("[Log] Resultados escritos en:");
        System.out.println("  >> " + filenameResultados);
        System.out.println("  >> " + filenameSecuencia);
        System.out.println("[Log] Tiempo total medido: " + totalMS + "ms");
    }

    private void writeResults(double totalMs) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filenameResultados, true))) {

            String reporte = policyReport(policy, redPetri);
            writer.write("Política seleccionada: " + policy.getSelectedPolicy());
            writer.newLine();
            writer.newLine();
            writer.write(reporte);
            writer.newLine();
            writer.write("TIEMPO TOTAL DE EJECUCION: " + totalMs + "ms");

        } catch (IOException e) {
            System.err.println("[Log] Error escribiendo archivo de resultados: " + e.getMessage());
        }
    }

    private void writeSequence() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filenameSecuencia, true))) {
            writer.write(redPetri.getSequence());
        } catch (IOException e) {
            System.err.println("[Log] Error escribiendo archivo de secuencia: " + e.getMessage());
        }
    }

    public String policyReport(Policy policy, RedPetri redPetri) {

        int selected = policy.getSelectedPolicy();
        int[] t = redPetri.getTransitionsFiredCount();

        if (selected == 1) {

            int T2 = t[2];
            int T5 = t[5];
            int T7 = t[7];

            int total = T2 + T5 + T7;

            StringBuilder sb = new StringBuilder();

            sb.append("No priorizar ningun modo, selección ALEATORIA\n");
            sb.append("-------------------------------------------\n");
            sb.append(String.format("T2 (complejidad baja): %8d\n", T2));
            sb.append(String.format("T5 (complejidad media): %8d\n", T5));
            sb.append(String.format("T7 (complejidad alta): %8d\n", T7));
            sb.append("-------------------------------------------\n");
            sb.append(String.format("TOTAL DISPAROS:    %8d\n", total));

            return sb.toString();
        }


        if (selected == 2) {

            int T2 = t[2];
            int T5 = t[5];
            int T7 = t[7];

            int total = T2 + T5 + T7;

            int pct2 = 0, pct5 = 0, pct7 = 0;
            if (total > 0) {
                pct2 = (T2 * 100) / total;
                pct5 = (T5 * 100) / total;
                pct7 = (T7 * 100) / total;
            }

            StringBuilder sb = new StringBuilder();

            sb.append("   Priorizar modo SIMPLE de procesamiento\n");
            sb.append("-------------------------------------------\n");
            sb.append(String.format("T5 (SIMPLE): %8d (%3d%%)\n", T5, pct5));
            sb.append(String.format("T2 (MEDIO):  %8d (%3d%%)\n", T2, pct2));
            sb.append(String.format("T7 (ALTO):%7d (%3d%%)\n", T7, pct7));
            sb.append("-------------------------------------------\n");
            sb.append(String.format("TOTAL:        %8d\n", total));

            return sb.toString();
        }


        return "";
    }
}
