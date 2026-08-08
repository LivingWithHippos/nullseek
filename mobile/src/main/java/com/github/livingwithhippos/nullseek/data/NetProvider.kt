package com.github.livingwithhippos.nullseek.data

// todo: move to sealed objects
sealed class NetProvider(
    val name: String,
    val home: String,
    val referralLink: String,
    val referralCode: String,
    val tokenLink: String
) {
    class Torbox : NetProvider(
        "TorBox",
        "https://torbox.app",
        "https://torbox.app/subscription?referral=dba208d7-f53e-4711-96db-97c7051799bc",
        "dba208d7-f53e-4711-96db-97c7051799bc",
        "https://torbox.app/settings?section=account"
    )

}