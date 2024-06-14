package xyz.wagyourtail.jvmdg.maven

import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object Settings {
    val properties = Properties().also {
        Settings::class.java.getResourceAsStream("/server.properties").use { s ->
            it.load(s)
        }
    }

    val mavens: List<Pair<String, String>> = mutableListOf<Pair<String, String>>().also { list ->
        Settings::class.java.getResourceAsStream("/mavens.txt")?.bufferedReader()?.use {
            it.forEachLine { line ->
                val parts = line.split('=', limit = 2)
                if (parts.size != 2) throw IllegalArgumentException("Invalid line: $line")
                list.add(parts[0] to parts[1])
            }
        }
    }

    val port: Int = properties.getProperty("port").toInt()

    val maxSize = properties.getProperty("maxSize").parseSize()
    val maxAge = properties.getProperty("maxAge").parseAge()

    val contact = properties.getProperty("contact")

    val blacklist: Set<String> = mutableSetOf<String>().also { set ->
        Settings::class.java.getResourceAsStream("/blacklist.txt")?.bufferedReader()?.use {
            set.addAll(it.lines().toList())
        }
    }

    fun String.parseSize(): Long {
        if (this.last().isDigit()) {
            return this.toLong()
        }
        val size = this.substring(0, this.length - 1).toLong()
        return when (this.last()) {
            'K' -> size * 1024
            'M' -> size * 1024 * 1024
            'G' -> size * 1024 * 1024 * 1024
            'T' -> size * 1024 * 1024 * 1024 * 1024
            else -> throw IllegalArgumentException("Invalid size: $this")
        }
    }

    fun String.parseAge(): Duration {
        if (this.last().isDigit()) {
            return this.toLong().milliseconds
        }
        val age = this.substring(0, this.length - 1).toLong()
        return when (this.last()) {
            's' -> age.seconds
            'm' -> age.minutes
            'h' -> age.hours
            'd' -> age.days
            'w' -> age.days * 7
            'M' -> age.days * 30
            'y' -> age.days * 365
            else -> throw IllegalArgumentException("Invalid age: $this")
        }
    }

}