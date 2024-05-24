package com.chromia.build.tools.model

class Brid private constructor(private val value: String) {

    init {
        require(value.length == 64 || value.matches(Regex("^(x\"[0-9A-Fa-f]{64}\")"))) { "String length must be exactly 64 characters" }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        fun of(value: String): Brid {
            return Brid(value)
        }
    }

    fun toByteArray(): ByteArray {
        //Some case to convert from the two different representations
        return ByteArray(11)
    }
}

fun main() {
    val myString = Brid.of("a".repeat(64))
    val myString2 = Brid.of("x\"${"a".repeat(64)}\"")
    val myString3 = Brid.of("615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5")
    println(myString)
    println(myString2)
    println(myString3)
}