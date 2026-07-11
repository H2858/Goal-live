package com.example.data.model

enum class MatchStatus {
    SCHEDULED,
    LIVE,
    FINISHED
}

enum class EventType {
    GOAL,
    YELLOW_CARD,
    RED_CARD,
    SUBSTITUTION
}

data class League(
    val id: String,
    val name: String,
    val country: String,
    val logoUrl: String
)

data class Team(
    val id: String,
    val name: String,
    val leagueId: String,
    val logoUrl: String
)

data class Player(
    val id: String,
    val name: String,
    val teamId: String,
    val photoUrl: String,
    val number: Int
)

data class Match(
    val id: String,
    val homeTeamId: String,
    val awayTeamId: String,
    val leagueId: String,
    val homeScore: Int,
    val awayScore: Int,
    val status: MatchStatus,
    val minute: Int,
    val startTime: Long
)

data class LiveEvent(
    val id: String,
    val matchId: String,
    val type: EventType,
    val minute: Int,
    val scoringTeamId: String,
    val scorerPlayerId: String,
    val assistPlayerId: String? = null
)

data class ApiKeyMapping(
    val externalKey: String,
    val internalField: String,
    val description: String
)

data class ExternalApiConfig(
    val baseUrl: String = "https://api.sportscore.com/v1",
    val apiKey: String = "gs_live_abc123xyz",
    val mappings: List<ApiKeyMapping> = listOf(
        ApiKeyMapping("fullName", "player_name", "Scorer's full name field"),
        ApiKeyMapping("score_home", "homeScore", "Home team's goals"),
        ApiKeyMapping("score_away", "awayScore", "Away team's goals"),
        ApiKeyMapping("elapsed_min", "minute", "Current match minute"),
        ApiKeyMapping("team_logo", "logoUrl", "Team logo visual asset")
    )
)
