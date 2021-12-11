package simpledb;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LockManager {
    /**
     * Store and link given PageId to PageLock instance.
     */
    private ConcurrentHashMap<PageId, PageLocks> lockMap;

    /**
     * Store and link given TransactionId to the PageId it locks.
     */
    private ConcurrentHashMap<TransactionId, HashSet<PageId>> transactionMap;

    /**
     * Directed graph of waiting transactions for deadlock detection.
     */
    private ConcurrentHashMap<TransactionId, HashSet<TransactionId>> waitMap;

    /**
     * Manage locks on page given by pid.
     */
    private class PageLocks {
        private final PageId pid;
        private final HashSet<TransactionId> lockedBy;
        private Permissions perm;
        private final LockManager lockManager;
        private final HashSet<TransactionId> waitedBy;

        PageLocks(PageId pid, TransactionId tid, Permissions perm, LockManager lockManager) {
            this.pid = pid;
            this.perm = perm;
            this.lockedBy = new HashSet<>();
            this.lockedBy.add(tid);
            this.lockManager = lockManager;
            this.waitedBy = new HashSet<>();
        }

        private boolean canAcquire(TransactionId tid, Permissions p) {
            if (perm == Permissions.READ_ONLY && p == Permissions.READ_WRITE) {
                    return lockedBy.size() == 1 && lockedBy.contains(tid);
            } else if (perm == Permissions.READ_WRITE) {
                return lockedBy.contains(tid);
            }
            return true;
        }

        synchronized void lock(TransactionId tid, Permissions p) throws InterruptedException, TransactionAbortedException {
            while (!canAcquire(tid, p)) {
                lockManager.waitMapAdd(tid, this);
                waitedBy.add(tid);
                if (lockManager.isDeadlocked()) {
                    lockedBy.remove(tid);
                    waitedBy.remove(tid);
                    lockManager.waitMapRemove(tid, waitedBy);
                    notifyAll();
                    throw new TransactionAbortedException();
                }
                wait();
            }

            perm = p;
            lockedBy.add(tid);
            waitedBy.remove(tid);

            for (TransactionId t : waitedBy) {
                this.lockManager.waitMapAdd(t, this);
            }
            lockManager.registerTransaction(tid, pid);
        }

        synchronized void unlock(TransactionId tid) {
            lockedBy.remove(tid);
            if (lockedBy.isEmpty()) {
                perm = null;
            }
            lockManager.waitMapRemove(tid, waitedBy);
            lockManager.removeTransaction(tid, pid);
            notifyAll();
        }

        public HashSet<TransactionId> getLockedBy() {
            return this.lockedBy;
        }
    }

    public LockManager() {
        this.lockMap = new ConcurrentHashMap<>();
        this.transactionMap = new ConcurrentHashMap<>();
        this.waitMap = new ConcurrentHashMap<>();
    }

    public void acquire(TransactionId tid, PageId pid, Permissions perm) throws TransactionAbortedException {
        PageLocks pl = lockMap.get(pid);
        if (pl == null) {
            pl = new PageLocks(pid, tid, perm, this);
            PageLocks prev = lockMap.putIfAbsent(pid, pl);
            if (prev != null) {
                pl = prev;
            }
        }

        try {
            pl.lock(tid, perm);
        } catch (InterruptedException e) {
            throw new TransactionAbortedException();
        }
    }

    public void release(TransactionId tid, PageId pid) {
        PageLocks pl = lockMap.get(pid);
        if (pl != null) {
            pl.unlock(tid);
        }
    }

    public synchronized boolean isLocked(TransactionId tid, PageId pid) {
        return transactionMap.get(tid).contains(pid);
    }

    public void releaseLocks(TransactionId tid) {
        Iterator<PageId> iter;
        synchronized (this) {
            HashSet<PageId> ps = transactionMap.remove(tid);
            if (ps == null) {
                return;
            }
            iter = ps.iterator();
        }

        while (iter.hasNext()) {
            PageId pid = iter.next();
            iter.remove();
            release(tid, pid);
        }
    }

    public synchronized void waitMapAdd(TransactionId tid, PageLocks pl) {
        HashSet<TransactionId> waiting = waitMap.computeIfAbsent(tid, k -> new HashSet<>());
        for (TransactionId tid2 : pl.getLockedBy()) {
            if (!tid.equals(tid2)) {
                waiting.add(tid2);
            }
        }
    }

    public synchronized void waitMapRemove(TransactionId tid, Iterable<TransactionId> waitList) {
        waitMap.remove(tid);
        for (TransactionId tid2 : waitList) {
            HashSet<TransactionId> inLine = waitMap.get(tid2);
            if (inLine != null) {
                inLine.remove(tid);
            }
        }
    }

    public synchronized boolean isDeadlocked() {
        HashMap<TransactionId, Integer> indegree = new HashMap<>();
        Deque<TransactionId> dq = new LinkedList<>();

        for (TransactionId tid: transactionMap.keySet()){
            indegree.putIfAbsent(tid, 0);
        }

        for (TransactionId tid: transactionMap.keySet()){
            if (waitMap.containsKey(tid)){
                for (TransactionId tid2: waitMap.get(tid)){
                    indegree.replace(tid2, indegree.get(tid2) + 1);
                }
            }
        }

        for (TransactionId tid: transactionMap.keySet()){
            if(indegree.get(tid) == 0){
                dq.offer(tid);
            }
        }

        int count = 0;
        while (!dq.isEmpty()){
            TransactionId tid = dq.poll();
            count += 1;
            if (waitMap.containsKey(tid) ) {
                for (TransactionId tid2 : waitMap.get(tid)) {
                    if (indegree.get(tid2) == 0) {
                        dq.offer(tid2);
                    } else {
                        indegree.replace(tid2, indegree.get(tid2) - 1);
                    }
                }
            }
        }
        return count != transactionMap.size();
    }

    private synchronized void registerTransaction(TransactionId tid, PageId pid) {
        HashSet<PageId> pageSet = transactionMap.computeIfAbsent(tid, k -> new HashSet<>());
        pageSet.add(pid);
    }

    private synchronized void removeTransaction(TransactionId tid, PageId pid) {
        HashSet<PageId> pageSet = transactionMap.get(tid);
        if (pageSet != null) {
            pageSet.remove(pid);
            if (pageSet.isEmpty()) {
                transactionMap.remove(tid);
            }
        }
    }

    public ConcurrentHashMap<TransactionId, HashSet<PageId>> getTransactionMap() {
        return this.transactionMap;
    }
}