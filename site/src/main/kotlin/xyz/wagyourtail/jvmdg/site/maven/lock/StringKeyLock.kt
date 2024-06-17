package xyz.wagyourtail.jvmdg.site.maven.lock

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

class StringKeyLock {
    @PublishedApi
    internal val locks = ConcurrentHashMap<String, LockEntry?>()

    class LockEntry {
        private val lock = ReentrantLock()
        private var count = AtomicInteger(0)

        fun incrementAndLock(beforeLock: (Int) -> Unit = {}) {
            beforeLock(count.incrementAndGet())
            lock.lock()
        }

        fun unlockAndDecrement(): Int {
            lock.unlock()
            return count.decrementAndGet()
        }
    }

    inline fun <T> locked(key: String, action: () -> T): T {
        val lock = locks.compute(key) { _, v -> v ?: LockEntry() }
        lock!!.incrementAndLock()
        try {
            return action()
        } finally {
            locks.compute(key) { _, v -> if (v!!.unlockAndDecrement() == 0) null else v }
            locks.remove(key, null)
        }
    }
}
