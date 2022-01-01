package xyz.quaver.pupil.sources.core

import java.io.File
import java.security.MessageDigest

fun sha256(data: ByteArray) : ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(data)
}

fun File.size(): Long =
    this.walk().fold(0L) { size, file -> size + file.length() }

