package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting

object UserHolder {
    private var map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User = User.makeUser(fullName, email=email, password = password)
        .also {
                user -> if (!map.containsKey(user.login))map[user.login] = user
                        else throw IllegalArgumentException("A user with this email already exists")

        }

    fun loginUser(login: String, password: String): String? =

            map[login.proveLogin()]?.let {
                if (it.checkPassword(password)) it.userInfo
                else null
            }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun requestAccessCode(login: String) : Unit {
        map[login.proveLogin()].let {
            it?.changeAccessCode()
        }

    }


    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder(){
        map.clear()
    }

    fun registerUserByPhone(fullName: String, rawPhone: String): User =

        User.makeUser(fullName, phone=rawPhone).also {
            user -> if(!map.containsKey(user.login)) map[user.login] = user
                    else throw IllegalArgumentException("A user with this phone already exists")
        }

    fun importUsers(csv: List<String>): MutableList<String>{

        val iter = csv.iterator()

        return mutableListOf<String>().apply {
            while (iter.hasNext())
                this.add(User.importUser(iter.next()).userInfo)
        }
    }


    fun String.proveLogin(): String {
        if (this.matches("^([a-zA-Z0-9_/-/.]+)@([a-zA-Z0-9_/-/.]+).([a-zA-Z]{2,5})$".toRegex())) return this
        else return this.replace("[^+\\d]".toRegex(), "").trim()
    }


}