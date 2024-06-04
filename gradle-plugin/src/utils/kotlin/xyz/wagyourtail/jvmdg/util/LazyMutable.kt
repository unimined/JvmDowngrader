package xyz.wagyourtail.jvmdg.util

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// https://stackoverflow.com/questions/47947841/kotlin-var-lazy-init :)
class LazyMutable<T>(val initializer: () -> T) : ReadWriteProperty<Any?, T> {

    @Suppress("ClassName")
    private object UNINITIALIZED_VALUE

    private var prop: Any? = UNINITIALIZED_VALUE

    @Suppress("UNCHECKED_CAST")
    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return if (prop == UNINITIALIZED_VALUE) {
            synchronized(this) {
                return if (prop == UNINITIALIZED_VALUE) initializer().also { prop = it } else prop as T
            }
        } else prop as T
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        synchronized(this) {
            prop = value
        }
    }
}