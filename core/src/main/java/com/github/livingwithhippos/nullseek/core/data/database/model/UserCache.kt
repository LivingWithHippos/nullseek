package com.github.livingwithhippos.nullseek.core.data.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.PrimaryKey
import com.github.livingwithhippos.torbox.model.UserDataDto

@Entity(
    tableName = "torbox_user_cache",
    foreignKeys = [
        ForeignKey(
            entity = NullProvider::class,
            parentColumns = ["id"],
            childColumns = ["provider_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class TorboxUserCache(
    @PrimaryKey @ColumnInfo(name = "provider_id") val providerId: Long,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "auth_id") val authId: String,
    @ColumnInfo(name = "created_at") val createdAt: String,
    @ColumnInfo(name = "updated_at") val updatedAt: String,
    @ColumnInfo(name = "plan") val plan: Int,
    @ColumnInfo(name = "total_downloaded") val totalDownloaded: Int,
    @ColumnInfo(name = "customer") val customer: String,
    @ColumnInfo(name = "is_subscribed") val isSubscribed: Boolean,
    @ColumnInfo(name = "premium_expires_at") val premiumExpiresAt: String?,
    @ColumnInfo(name = "cooldown_until") val cooldownUntil: String?,
    @ColumnInfo(name = "email") val email: String,
    @ColumnInfo(name = "user_referral") val userReferral: String,
    @ColumnInfo(name = "base_email") val baseEmail: String,
    @ColumnInfo(name = "total_bytes_downloaded") val totalBytesDownloaded: Long,
    @ColumnInfo(name = "total_bytes_uploaded") val totalBytesUploaded: Long,
    @ColumnInfo(name = "torrents_downloaded") val torrentsDownloaded: Int,
    @ColumnInfo(name = "web_downloads_downloaded") val webDownloadsDownloaded: Int,
    @ColumnInfo(name = "usenet_downloads_downloaded") val usenetDownloadsDownloaded: Int,
    @ColumnInfo(name = "additional_concurrent_slots") val additionalConcurrentSlots: Int,
    @ColumnInfo(name = "long_term_seeding") val longTermSeeding: Boolean,
    @ColumnInfo(name = "long_term_storage") val longTermStorage: Boolean,
    @ColumnInfo(name = "is_vendor") val isVendor: Boolean,
    @ColumnInfo(name = "vendor_id") val vendorId: Int?,
    @ColumnInfo(name = "purchases_referred") val purchasesReferred: Int
) {
    fun toUserDataDto(): UserDataDto = UserDataDto(
        id = userId,
        authId = authId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        plan = plan,
        totalDownloaded = totalDownloaded,
        customer = customer,
        isSubscribed = isSubscribed,
        premiumExpiresAt = premiumExpiresAt,
        cooldownUntil = cooldownUntil,
        email = email,
        userReferral = userReferral,
        baseEmail = baseEmail,
        totalBytesDownloaded = totalBytesDownloaded,
        totalBytesUploaded = totalBytesUploaded,
        torrentsDownloaded = torrentsDownloaded,
        webDownloadsDownloaded = webDownloadsDownloaded,
        usenetDownloadsDownloaded = usenetDownloadsDownloaded,
        additionalConcurrentSlots = additionalConcurrentSlots,
        longTermSeeding = longTermSeeding,
        longTermStorage = longTermStorage,
        isVendor = isVendor,
        vendorId = vendorId,
        purchasesReferred = purchasesReferred,
    )

    companion object {
        fun fromUserDataDto(user: UserDataDto, providerId: Long) = TorboxUserCache(
            providerId = providerId,
            userId = user.id,
            authId = user.authId,
            createdAt = user.createdAt,
            updatedAt = user.updatedAt,
            plan = user.plan,
            totalDownloaded = user.totalDownloaded,
            customer = user.customer,
            isSubscribed = user.isSubscribed,
            premiumExpiresAt = user.premiumExpiresAt,
            cooldownUntil = user.cooldownUntil,
            email = user.email,
            userReferral = user.userReferral,
            baseEmail = user.baseEmail,
            totalBytesDownloaded = user.totalBytesDownloaded,
            totalBytesUploaded = user.totalBytesUploaded,
            torrentsDownloaded = user.torrentsDownloaded,
            webDownloadsDownloaded = user.webDownloadsDownloaded,
            usenetDownloadsDownloaded = user.usenetDownloadsDownloaded,
            additionalConcurrentSlots = user.additionalConcurrentSlots,
            longTermSeeding = user.longTermSeeding,
            longTermStorage = user.longTermStorage,
            isVendor = user.isVendor,
            vendorId = user.vendorId,
            purchasesReferred = user.purchasesReferred
        )
    }
}