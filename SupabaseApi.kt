package com.example.data.remote

import com.squareup.moshi.Json
import retrofit2.Response
import retrofit2.http.*

interface SupabaseApi {

    @GET("leagues")
    suspend fun getLeagues(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): List<SupabaseLeagueDto>

    @POST("leagues")
    suspend fun insertLeague(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body league: SupabaseLeagueDto
    ): Response<Unit>

    @GET("teams")
    suspend fun getTeams(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): List<SupabaseTeamDto>

    @POST("teams")
    suspend fun insertTeam(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body team: SupabaseTeamDto
    ): Response<Unit>

    @GET("players")
    suspend fun getPlayers(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): List<SupabasePlayerDto>

    @POST("players")
    suspend fun insertPlayer(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body player: SupabasePlayerDto
    ): Response<Unit>

    @GET("matches")
    suspend fun getMatches(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String
    ): List<SupabaseMatchDto>

    @POST("matches")
    suspend fun insertMatch(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body match: SupabaseMatchDto
    ): Response<Unit>

    @GET("live_events")
    suspend fun getLiveEvents(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("matchId") matchId: String? = null
    ): List<SupabaseLiveEventDto>

    @POST("live_events")
    suspend fun insertLiveEvent(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Body event: SupabaseLiveEventDto
    ): Response<Unit>

    @POST("https://eyiqjuhioxrpqixmovfs.supabase.co/auth/v1/token")
    suspend fun loginWithEmail(
        @Header("apikey") apiKey: String,
        @Query("grant_type") grantType: String = "password",
        @Body body: SupabaseLoginRequest
    ): SupabaseLoginResponse

    @GET("user_roles")
    suspend fun getUserRoles(
        @Header("apikey") apiKey: String,
        @Header("Authorization") auth: String,
        @Query("user_id") userIdFilter: String? = null,
        @Query("role") roleFilter: String? = null
    ): List<SupabaseUserRoleDto>
}

data class SupabaseLoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

data class SupabaseLoginResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "user") val user: SupabaseUserInfoDto
)

data class SupabaseUserInfoDto(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String
)

data class SupabaseUserRoleDto(
    @Json(name = "id") val id: Long?,
    @Json(name = "user_id") val userId: String,
    @Json(name = "role") val role: String
)

data class SupabaseLeagueDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "country") val country: String,
    @Json(name = "logoUrl") val logoUrl: String
)

data class SupabaseTeamDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "leagueId") val leagueId: String,
    @Json(name = "logoUrl") val logoUrl: String
)

data class SupabasePlayerDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "teamId") val teamId: String,
    @Json(name = "photoUrl") val photoUrl: String,
    @Json(name = "number") val number: Int
)

data class SupabaseMatchDto(
    @Json(name = "id") val id: String,
    @Json(name = "homeTeamId") val homeTeamId: String,
    @Json(name = "awayTeamId") val awayTeamId: String,
    @Json(name = "leagueId") val leagueId: String,
    @Json(name = "homeScore") val homeScore: Int,
    @Json(name = "awayScore") val awayScore: Int,
    @Json(name = "status") val status: String,
    @Json(name = "minute") val minute: Int,
    @Json(name = "startTime") val startTime: Long
)

data class SupabaseLiveEventDto(
    @Json(name = "id") val id: String,
    @Json(name = "matchId") val matchId: String,
    @Json(name = "type") val type: String,
    @Json(name = "minute") val minute: Int,
    @Json(name = "scoringTeamId") val scoringTeamId: String,
    @Json(name = "scorerPlayerId") val scorerPlayerId: String,
    @Json(name = "assistPlayerId") val assistPlayerId: String? = null
)
