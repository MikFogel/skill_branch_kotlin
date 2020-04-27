package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import org.intellij.lang.annotations.RegExp
import java.lang.IllegalArgumentException
import java.lang.StringBuilder
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString (" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
                 .map { it.first().toUpperCase() }
                 .joinToString( " ")

    private var phone: String? = null
        set(value) {
            field = value?.replace("[^+\\d]".toRegex(), "")
        }

    private var _login:String? = null
    private var salt: String? = null


    var login:String
        set(value) {
            _login = value.toLowerCase()
        }
        get() = _login!!

    private lateinit var passwordHasg: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null


    //for email
    constructor(
        firstName: String,
        lastName: String?,
        email: String,
        password: String
    ): this(firstName, lastName, email=email, meta = mapOf("auth" to "password")) {
        println("Secondary mail constructor")
        passwordHasg = encrypt(password)
    }

    //for phone
    constructor(
        firstName: String,
        lastName: String?,
        rawPhone: String
    ): this(firstName, lastName, rawPhone = rawPhone, meta = mapOf("auth" to "sms")) {
        println("Secondary phone constructor")
        val code = generateAccessCode()
        passwordHasg = encrypt(code)
        accessCode = code

        check(rawPhone.toValidNumber().matches("^(?:[+0])?[0-9]{11}".toRegex())) {
            throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")
        }
        sendAccessCodeToUser(rawPhone.toValidNumber(), code)
    }

    private fun String.toValidNumber(): String = this.replace("[^+\\d]".toRegex(), "")

    //fov csv
    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        phone: String?,
        salt: String,
        passwordHasg: String
    ): this(firstName, lastName, email = email, rawPhone = phone, meta = mapOf("src" to "csv")) {

        this.salt = salt
        this.passwordHasg = passwordHasg
    }


    init {

        println("First init block of primary constructor is called")

        check(!firstName.isBlank()) { "First name must not be blank" }
        check(!email.isNullOrBlank() || !rawPhone.isNullOrBlank()) {"Email or phone must not be blank"}

        phone = rawPhone
        login = email ?: phone!!


        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHasg

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHasg = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not match the current password")

    }


    private fun encrypt(password: String) : String {

        if (salt.isNullOrEmpty()) {
            salt = ByteArray(16).also {
                SecureRandom().nextBytes(it)
            }.toString()
        }

      return salt.plus(password).md5()
    }

    private fun generateAccessCode(): String {
        val possible = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        return StringBuilder().apply{
            repeat(6) {
                (possible.indices).random().also{
                    index -> append(possible[index])
                }
            }
        }.toString()
    }



    private fun sendAccessCodeToUser(phone: String?, code: String) {

        println("...sending access code: $code on $phone")

    }

    private fun String.md5():String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())

        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory{

        fun importUser(csv: String) : User {
            csv.split(";").run {
                val (firstName: String, lastName: String?) = this.first().fullNameToPair()

                val (salt: String, password: String) = this.get(2).saltPasswordToPair()

                val phone: String? = when {
                    this.last().isEmpty() -> null
                    else -> this.last()
                }

                val email: String? = when {
                    this.get(1).isEmpty() -> null
                    else -> get(1)
                }

                return when {
                    !phone.isNullOrBlank() -> User(
                        firstName = firstName,
                        lastName = lastName,
                        email = null,
                        phone = phone,
                        salt = salt,
                        passwordHasg = password
                    )
                    !email.isNullOrBlank() -> User(
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = null,
                        salt = salt,
                        passwordHasg = password
                    )
                    else -> throw IllegalArgumentException("Email or phone must be not null or blank")

                }
                }

            }


        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null) :
                User {

            val (firstName: String, lastName: String?) = fullName.fullNameToPair()

            return when{
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() -> User(firstName, lastName, email, password)
                else -> throw IllegalArgumentException("Email or phone must be not null or blank")
            }
        }

        private fun String.saltPasswordToPair(): Pair<String, String> {
            return this.split(":")
                .filter { it.isNotBlank()}
                .run {
                    when(size) {
                        1 -> throw IllegalArgumentException("wrong hash")
                        2 -> first() to last()
                        else -> throw IllegalArgumentException("wrong hash")
                    }
                }
        }


        private fun String.fullNameToPair(): Pair<String, String?> {
            return this.trim().split(" ")
                .filter { it.isNotBlank()}
                .run {
                    when(size) {
                        1 -> first().trim() to null
                        2 -> first().trim() to last().trim()
                        else -> throw IllegalArgumentException("firstName is empty")
                    }
                }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun changeAccessCode() {
        val code = generateAccessCode()
        accessCode = code
        passwordHasg = encrypt(code)
        sendAccessCodeToUser(phone, code)

    }

}