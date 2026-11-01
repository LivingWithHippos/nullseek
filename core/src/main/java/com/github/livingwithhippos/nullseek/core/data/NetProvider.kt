package com.github.livingwithhippos.nullseek.core.data

import com.github.livingwithhippos.nullseek.core.domain.ProviderKind

enum class NullProviderType(
    val value: Int,
) {
    TORBOX(ProviderKind.TORBOX.dbType),
    REAL_DEBRID(ProviderKind.REAL_DEBRID.dbType),
}

sealed class NetProvider(
    val kind: ProviderKind,
    val home: String,
    val referralLink: String,
    val referralCode: String,
    val tokenLink: String,
) {
    val type: Int get() = kind.dbType
    val name: String get() = kind.displayName

    class Torbox :
        NetProvider(
            ProviderKind.TORBOX,
            "https://torbox.app",
            "https://torbox.app/subscription?referral=dba208d7-f53e-4711-96db-97c7051799bc",
            "dba208d7-f53e-4711-96db-97c7051799bc",
            "https://torbox.app/settings?section=account",
        )

    class RealDebrid :
        NetProvider(
            ProviderKind.REAL_DEBRID,
            "https://real-debrid.com",
            "https://real-debrid.com/premium",
            "",
            "https://real-debrid.com/apitoken",
        )

    val hasReferral: Boolean get() = referralCode.isNotBlank()

    companion object {
        fun all(): List<NetProvider> = listOf(Torbox(), RealDebrid())

        fun of(kind: ProviderKind): NetProvider =
            when (kind) {
                ProviderKind.TORBOX -> Torbox()
                ProviderKind.REAL_DEBRID -> RealDebrid()
            }

        fun fromType(type: Int): NetProvider = of(ProviderKind.fromDbType(type))
    }
}
