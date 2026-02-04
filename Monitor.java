package main;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Monitor implements MonitorInterface {

    private final RedPetri Rdp;
    private final ReentrantLock mutex = new ReentrantLock(true);

    private final int[] timedTransitions;

    // CONDICIONES
    private final Map<String, Condition> conditionsByName = new HashMap<>();
    private final Map<Integer, Condition> conditionByTransition = new HashMap<>();

    // BUFFERS
    private final Map<String, Container<Dato>> buffers = new HashMap<>();

    // Mapeo para la transicion: Transicion >> buffer asociado y operacion a realizar
    private final Map<Integer, BufferMapping> transitionBufferMapping = new HashMap<>();


    // FIN GLOBAL

    private final TerminationPolicy termination;
    private volatile boolean programFinished = false;

    // CONSTRUCTOR
    public Monitor(RedPetri rdp, TerminationPolicy termination) {
        this.Rdp = rdp;
        this.timedTransitions = rdp.getTimedTransitions();
        this.termination = termination;

        initConditions();
        initBufferMapping();
        initBuffers();
    }

    private void initConditions() {
        for (var i : Rdp.getTransitionsConditions().entrySet()) {
            Condition condition = mutex.newCondition();
            conditionsByName.put(i.getKey(), condition);
            for (int t : i.getValue()) {
                conditionByTransition.put(t, condition);
            }
        }
    }

    private void initBufferMapping() {
        transitionBufferMapping.clear();
        transitionBufferMapping.putAll(Rdp.getTransitionBufferMapping());
    }

    private void initBuffers() {
        buffers.clear();

        for (BufferMapping mapping : transitionBufferMapping.values()) {
            String name = mapping.getBufferName();
            buffers.putIfAbsent(name, new Container<>());
        }
    }

    private Condition getBufferCondition(String bufferName, BufferMapping.Operation expectedOperation) {
        for (var e : transitionBufferMapping.entrySet()) {
            int t = e.getKey();
            BufferMapping m = e.getValue();

            if (bufferName.equals(m.getBufferName()) && m.getOperation() == expectedOperation) {
                return getCondition(t);
            }
        }
        return null;
    }


    private Condition getCondition(int t) {
        return conditionByTransition.get(t);
    }

    private BufferMapping getMapping(int t) {
        return transitionBufferMapping.get(t);
    }

    private boolean needsData(int t) { // La transición necesita obtener el dato del buffer?
        BufferMapping buffer = getMapping(t);
        return buffer != null && buffer.getOperation() == BufferMapping.Operation.REMOVE;
    }


    private void isEndInvariant(int T) {

        termination.checkFire(T);

        if (termination.isFinished()) {
            programFinished = true;
            // Despertar a todos los que estén esperando
            for (Condition c : conditionsByName.values()) {
                c.signalAll();
            }
        }
    }

    private void moveData(int T) {
        BufferMapping map = getMapping(T);
        if (map == null) return;

        String bufferName = map.getBufferName();
        Container<Dato> buffer = buffers.get(bufferName);

        if (map.getOperation() == BufferMapping.Operation.REMOVE) { // remove y lo guardo en payload
            Dato data = buffer.remove();
            if (data == null) {
                throw new IllegalStateException(
                        "GET desde buffer pero buffer vacío (T=" + T + ", buffer=" + bufferName + ")"
                );
            }
            Payload.set(data);

        } else { // ADD
            Dato data = Payload.get();
            if (data == null) {
                throw new IllegalStateException(
                        "ADD hacia el buffer pero el elemento == null (T=" + T + ", buffer=" + bufferName + ")"
                );
            }

            buffer.add(data);
            Payload.clear();
        }
    }



    //UNICO METODO PUBLICO

    @Override
    public boolean fireTransition(int T) {
        for (int i : timedTransitions) {
            if (i == T) return fireTimedTransition(T);
        }
        return fireInstantTransition(T);
    }

    private boolean fireInstantTransition(int Tref) {
        mutex.lock();
        try {
            while (!programFinished) {

                int chosen = Rdp.whichFire(Rdp.getPlacefromTransition(Tref));

                    if (chosen < 0) {
                        awaitOnCondition(Tref);
                        continue;
                    }

                //Si la transicion consume del buffer
                if (needsData(chosen)) {
                    String bufferName = getMapping(chosen).getBufferName();

                    if (buffers.get(bufferName).isEmpty()) {
                        awaitBufferHasData(bufferName);
                        continue;
                    }
                }


                //Puedo disparar por tokens
                if (!Rdp.canFire(chosen)) {
                    awaitOnCondition(chosen);
                    continue;
                }

                Rdp.fire(chosen);
                Payload.setLastFired(chosen);
                moveData(chosen);
                isEndInvariant(chosen);
                return true;
            }
            return false;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;

        } finally {
            mutex.unlock();
        }
    }

    private boolean fireTimedTransition(int T) {

        boolean hasCondition = getCondition(T) != null; //Falso si no tiene condicion
        boolean hasMapping = getMapping(T) != null; //Falso si no tiene mapping

        // Si la transición tiene condición o mapping es una transición temporal final
        if (hasCondition || hasMapping) {
            return fireFinalTimedTransition(T);
        }

        // Si no, es una transición intermedia
        return fireMidTimedTransition(T);
    }


    private boolean fireMidTimedTransition(int T) {
        while (!programFinished) {
            mutex.lock();
            try {
                if (!Rdp.canFire(T)) {
                    awaitOnCondition(T);
                    continue;
                }

                Rdp.initTiming(T);

                if (Rdp.canFireTemp(T)) {

                    Rdp.fire(T);
                    Payload.setLastFired(T);
                    Rdp.resetTiming(T);

                    return true;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;

            } finally {
                mutex.unlock();
            }

            try {
                sleepRemainingTime(T);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean fireFinalTimedTransition(int T) {
        while (!programFinished) {
            mutex.lock();
            try {
                if (needsData(T)) {
                    String bufferName = getMapping(T).getBufferName();

                    if (buffers.get(bufferName).isEmpty()) {
                        awaitBufferHasData(bufferName);
                        continue;
                    }
                }

                if (!Rdp.canFire(T)) {
                    awaitOnCondition(T);
                    continue;
                }

                Rdp.initTiming(T);

                if (Rdp.canFireTemp(T)) {

                    Rdp.fire(T);
                    Payload.setLastFired(T);
                    Rdp.resetTiming(T);

                    moveData(T);
                    isEndInvariant(T);

                    signalAllOnCondition(T);

                    return true;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;

            } finally {
                mutex.unlock();
            }

            try {
                sleepRemainingTime(T);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }


    // METODOS AUXILIARES
    private void awaitOnCondition(int t) throws InterruptedException {
        Condition c = getCondition(t);
        if (c != null) {
            c.await(); // se asume que tenemos el lock
        }
    }

    private void awaitBufferHasData(String bufferName) throws InterruptedException {
        // se asume que tenemos el lock del mutex
        Condition c = getBufferCondition(bufferName, BufferMapping.Operation.ADD);
        while (buffers.get(bufferName).isEmpty() && !programFinished) {
            if (c == null) return;   // si no hay condition asociada, no puedo esperar
            c.await();
        }
    }


    private void signalAllOnCondition(int t) {
        Condition c = getCondition(t);
        if (c != null) {
            c.signalAll();
        }
    }


    private void sleepRemainingTime(int t) throws InterruptedException {
        long remaining = Rdp.getRemainingTime(t);
        if (remaining > 0) {
            Thread.sleep(remaining);
        }
    }



}
