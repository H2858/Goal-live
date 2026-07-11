package com.example.data.repository

import com.example.data.model.League
import com.example.data.model.Team
import com.example.data.model.Player
import com.example.data.model.Match
import com.example.data.model.LiveEvent
import com.example.data.model.ExternalApiConfig
import kotlinx.coroutines.flow.Flow

interface GoalLiveRepository {
    fun getLeagues(): Flow<List<League>>
    fun getTeams(): Flow<List<Team>>
    fun getPlayers(): Flow<List<Player>>
    fun getMatches(): Flow<List<Match>>
    fun getLiveEvents(matchId: String): Flow<List<LiveEvent>>
    
    suspend fun insertLeague(league: League)
    suspend fun insertTeam(team: Team)
    suspend fun insertPlayer(player: Player)
    suspend fun scheduleMatch(match: Match)
    suspend fun insertLiveEvent(event: LiveEvent)
    
    // Config management
    fun isApiMode(): Flow<Boolean>
    suspend fun setApiMode(active: Boolean)
    
    fun getExternalApiConfig(): Flow<ExternalApiConfig>
    suspend fun updateExternalApiConfig(config: ExternalApiConfig)
    
    fun isPremium(): Flow<Boolean>
    suspend fun setPremium(active: Boolean)
    
    suspend fun testExternalApiConnection(baseUrl: String, apiKey: String): Boolean
}
