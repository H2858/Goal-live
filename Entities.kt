package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leagues")
data class LeagueEntity(
    @PrimaryKey val id: String,
    val name: String,
    val country: String,
    val logoUrl: String
)

@Entity(tableName = "teams")
data class TeamEntity(
    @PrimaryKey val id: String,
    val name: String,
    val leagueId: String,
    val logoUrl: String
)

@Entity(tableName = "players")
data class PlayerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val teamId: String,
    val photoUrl: String,
    val number: Int
)

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey val id: String,
    val homeTeamId: String,
    val awayTeamId: String,
    val leagueId: String,
    val homeScore: Int,
    val awayScore: Int,
    val status: String, // MatchStatus name
    val minute: Int,
    val startTime: Long
)

@Entity(tableName = "live_events")
data class LiveEventEntity(
    @PrimaryKey val id: String,
    val matchId: String,
    val type: String, // EventType name
    val minute: Int,
    val scoringTeamId: String,
    val scorerPlayerId: String,
    val assistPlayerId: String?
)

@Entity(tableName = "app_config")
data class AppConfigEntity(
    @PrimaryKey val id: Int = 1,
    val isApiMode: Boolean = false,
    val baseUrl: String = "https://api.sportscore.com/v1",
    val apiKey: String = "gs_live_abc123xyz",
    val serializedMappings: String = "fullName:player_name;score_home:homeScore;score_away:awayScore;elapsed_min:minute;team_logo:logoUrl",
    val isPremium: Boolean = false
)
