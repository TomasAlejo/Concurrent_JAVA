package main;

public class TerminationPolicy implements TerminationInterface {

    private final int finalTransition;
    private final int maxInvariants;

    private int invariantsDone = 0;
    private volatile boolean finished = false;

    public TerminationPolicy(int finalTransition, int maxInvariants) {
        this.finalTransition = finalTransition;
        this.maxInvariants = maxInvariants;
    }

    @Override
    public void checkFire(int t) {
        if (finished) return;

        if (t == finalTransition) {
            invariantsDone++;
            if (invariantsDone >= maxInvariants) {
                finished = true;
            }
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }
}
