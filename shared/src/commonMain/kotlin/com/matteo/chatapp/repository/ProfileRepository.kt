package com.matteo.chatapp.repository

import com.matteo.chatapp.model.UpdateProfileRequest
import com.matteo.chatapp.model.UserProfile
import com.matteo.chatapp.network.UserApi
import com.matteo.chatapp.platform.PickedAvatarFile

interface ProfileRepository {
    suspend fun loadMyProfile(): UserProfile

    suspend fun saveProfile(
        username: String,
        status: String,
        bio: String,
        avatarFile: PickedAvatarFile?,
        bannerFile: PickedAvatarFile?
    ): UserProfile

    suspend fun findUser(username: String): UserProfile

    suspend fun getUser(userId: String): UserProfile

    suspend fun getCurrentUserId(): String

    fun cacheCurrentProfile(profile: UserProfile)
}

class NetworkProfileRepository(
    private val userApi: UserApi,
    private val accessTokenProvider: AccessTokenProvider
) : ProfileRepository {
    override suspend fun loadMyProfile(): UserProfile {
        val profile = userApi.getMyProfile(accessTokenProvider.requireToken())
        SessionManager.saveUserId(profile.id)
        return profile
    }

    override suspend fun saveProfile(
        username: String,
        status: String,
        bio: String,
        avatarFile: PickedAvatarFile?,
        bannerFile: PickedAvatarFile?
    ): UserProfile {
        val token = accessTokenProvider.requireToken()
        var profile = userApi.updateMyProfile(
            UpdateProfileRequest(
                username = username.trim().takeIf(String::isNotBlank),
                avatarUrl = null,
                profileBannerUrl = null,
                status = status.trim().takeIf(String::isNotBlank),
                profileColor = null,
                bio = bio.trim()
            ),
            token
        )
        if (avatarFile != null) {
            profile = userApi.uploadAvatar(
                avatarFile.fileName,
                avatarFile.contentType,
                avatarFile.bytes,
                token
            )
        }
        if (bannerFile != null) {
            profile = userApi.uploadBanner(
                bannerFile.fileName,
                bannerFile.contentType,
                bannerFile.bytes,
                token
            )
        }
        cacheCurrentProfile(profile)
        return profile
    }

    override suspend fun findUser(username: String): UserProfile =
        userApi.getUserByUsername(username.trim(), accessTokenProvider.requireToken())

    override suspend fun getUser(userId: String): UserProfile =
        userApi.getUserById(userId, accessTokenProvider.requireToken())

    override suspend fun getCurrentUserId(): String =
        SessionManager.getUserId() ?: loadMyProfile().id

    override fun cacheCurrentProfile(profile: UserProfile) {
        val token = accessTokenProvider.currentToken() ?: return
        SessionManager.saveSession(token, profile.username, profile.email, profile.id)
    }
}
