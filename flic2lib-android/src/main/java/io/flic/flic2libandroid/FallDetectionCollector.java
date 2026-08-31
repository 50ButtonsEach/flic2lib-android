package io.flic.flic2libandroid;

import java.util.AbstractList;

class FallDetectionCollector {
    static class ArraySnapshotList<E> extends AbstractList<E> {
        private final E[] storage;
        private final int offset;
        private final int size;

        ArraySnapshotList(E[] storage, int offset, int size) {
            this.storage = storage;
            this.offset = offset;
            this.size = size;
        }

        @Override
        public E get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("index: " + index + ", size: " + size);
            }
            return storage[offset + index];
        }

        @Override
        public int size() {
            return size;
        }
    }

    int phase0SampleRate;
    int phase0NumSamples;
    int phase1SampleRate;
    int phase1NumSamples;
    AccelerometerDataPoint[] samples;
    int numSamplesReceived;
    boolean didSendPreFallDataCollected;

    void addSamples(short[] source, byte scale) {
        int pos = numSamplesReceived;
        int len = source.length / 3;
        for (int i = 0; i < len; i++) {
            samples[pos + i] = new AccelerometerDataPoint(source[3 * i], source[3 * i + 1], source[3 * i + 2], scale);
        }
        numSamplesReceived += len;
    }

    FallDetectionEvent getEvent(int state) {
        return new FallDetectionEvent(
                state,
                phase0SampleRate,
                phase0NumSamples,
                new ArraySnapshotList<>(samples, 0, state == FallDetectionEvent.STATE_TRIGGERED ? 0 : Math.min(phase0NumSamples, numSamplesReceived)),
                phase1SampleRate,
                phase1NumSamples,
                new ArraySnapshotList<>(samples, phase0NumSamples, state != FallDetectionEvent.STATE_COMPLETED ? 0 : numSamplesReceived - phase0NumSamples));
    }
}
