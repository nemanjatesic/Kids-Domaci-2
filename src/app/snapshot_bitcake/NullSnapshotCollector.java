package app.snapshot_bitcake;

public class NullSnapshotCollector implements SnapshotCollector{
    @Override
    public void stop() {
    }

    @Override
    public BitcakeManager getBitcakeManager() {
        return null;
    }

    @Override
    public void addNaiveSnapshotInfo(String snapshotSubject, int amount) {

    }

    @Override
    public void startCollecting() {

    }

    @Override
    public void run() {

    }
}
