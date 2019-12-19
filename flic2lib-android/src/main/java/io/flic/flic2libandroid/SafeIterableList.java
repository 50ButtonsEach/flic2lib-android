package io.flic.flic2libandroid;

import java.util.Iterator;
import java.util.NoSuchElementException;

class SafeIterableList<E> implements Iterable<E> {
    private SafeIterableList<E> prev;
    private SafeIterableList<E> next;
    private E element;
    private boolean removed;

    private SafeIterableList(E element) {
        this.element = element;
    }

    public SafeIterableList() {
    }

    public void add(E element) {
        SafeIterableList<E> node = new SafeIterableList<>(element);
        SafeIterableList<E> cur = this;
        for (;;) {
            if (cur.next == null) {
                cur.next = node;
                node.prev = cur;
                return;
            }
            cur = cur.next;
            if (cur.element == element) {
                return;
            }
        }
    }

    public void remove(E element) {
        SafeIterableList<E> prev = this;
        SafeIterableList<E> cur = next;
        for (;;) {
            if (cur == null) {
                return;
            }
            if (cur.element == element) {
                cur.removed = true;
                prev.next = cur.next;
                if (cur.next != null) {
                    cur.next.prev = prev;
                }
                return;
            }
            prev = cur;
            cur = cur.next;
        }
    }

    public void clear() {
        SafeIterableList<E> cur = next;
        while (cur != null) {
            cur.removed = true;
            cur = cur.next;
        }
        next = null;
    }

    @Override
    public Iterator<E> iterator() {
        return new SafeIterableListIterator(this);
    }

    class SafeIterableListIterator implements Iterator<E> {
        private SafeIterableList<E> node;

        public SafeIterableListIterator(SafeIterableList<E> node) {
            this.node = node;
        }

        @Override
        public boolean hasNext() {
            while (node.removed) {
                node = node.prev;
            }
            return node.next != null;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            node = node.next;
            return node.element;
        }
    }
}
