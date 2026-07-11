package com.example.data.repository

import android.content.Context
import com.example.data.local.*
import com.example.data.model.*
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseLeagueDto
import com.example.data.remote.SupabaseTeamDto
import com.example.data.remote.SupabasePlayerDto
import com.example.data.remote.SupabaseMatchDto
import com.example.data.remote.SupabaseLiveEventDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class DynamicGoalLiveRepository(
    context: Context
) : GoalLiveRepository {

    private val db = AppDatabase.getDatabase(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            try {
                // Check if we can reach Supabase and if it has any leagues
                val supabaseLeagues = SupabaseClient.api.getLeagues(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER)
                if (supabaseLeagues.isEmpty()) {
                    // Supabase is empty, prepopulate local database and seed Supabase!
                    prepopulateDatabase()
                } else {
                    // Supabase has data! Sync everything to Room cache.
                    supabaseLeagues.forEach {
                        db.leagueDao().insertLeague(LeagueEntity(it.id, it.name, it.country, it.logoUrl))
                    }
                    try {
                        val teams = SupabaseClient.api.getTeams(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER)
                        teams.forEach {
                            db.teamDao().insertTeam(TeamEntity(it.id, it.name, it.leagueId, it.logoUrl))
                        }
                    } catch (te: Exception) { te.printStackTrace() }

                    try {
                        val players = SupabaseClient.api.getPlayers(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER)
                        players.forEach {
                            db.playerDao().insertPlayer(PlayerEntity(it.id, it.name, it.teamId, it.photoUrl, it.number))
                        }
                    } catch (pe: Exception) { pe.printStackTrace() }

                    try {
                        val matches = SupabaseClient.api.getMatches(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER)
                        matches.forEach {
                            db.matchDao().insertMatch(MatchEntity(it.id, it.homeTeamId, it.awayTeamId, it.leagueId, it.homeScore, it.awayScore, it.status, it.minute, it.startTime))
                        }
                    } catch (me: Exception) { me.printStackTrace() }
                }
            } catch (e: Exception) {
                // If offline, check if local Room is empty. If so, prepopulate locally.
                e.printStackTrace()
                try {
                    db.leagueDao().getAllLeagues().first().let { list ->
                        if (list.isEmpty()) {
                            prepopulateDatabaseLocallyOnly()
                        }
                    }
                } catch (le: Exception) { le.printStackTrace() }
            }
        }
    }

    override fun getLeagues(): Flow<List<League>> {
        // Trigger background fetch to sync from Supabase
        scope.launch {
            try {
                val supabaseLeagues = SupabaseClient.api.getLeagues(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER)
                supabaseLeagues.forEach {
                    db.leagueDao().insertLeague(LeagueEntity(it.id, it.name, it.country, it.logoUrl))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return db.leagueDao().getAllLeagues().map { entities ->
            entities.map { League(it.id, it.name, it.country, it.logoUrl) }
        }
    }

    override fun getTeams(): Flow<List<Team>> {
        scope.launch {
            try {
                val teams = SupabaseClient.api.getTeams(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER)
                teams.forEach {
                    db.teamDao().insertTeam(TeamEntity(it.id, it.name, it.leagueId, it.logoUrl))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return db.teamDao().getAllTeams().map { entities ->
            entities.map { Team(it.id, it.name, it.leagueId, it.logoUrl) }
        }
    }

    override fun getPlayers(): Flow<List<Player>> {
        scope.launch {
            try {
                val players = SupabaseClient.api.getPlayers(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER)
                players.forEach {
                    db.playerDao().insertPlayer(PlayerEntity(it.id, it.name, it.teamId, it.photoUrl, it.number))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return db.playerDao().getAllPlayers().map { entities ->
            entities.map { Player(it.id, it.name, it.teamId, it.photoUrl, it.number) }
        }
    }

    override fun getMatches(): Flow<List<Match>> {
        return isApiMode().flatMapLatest { apiModeActive ->
            if (apiModeActive) {
                getExternalMatchesFlow()
            } else {
                flow {
                    val syncJob = scope.launch {
                        while (true) {
                            try {
                                val supabaseMatches = SupabaseClient.api.getMatches(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER)
                                supabaseMatches.forEach { m ->
                                    db.matchDao().insertMatch(
                                        MatchEntity(
                                            id = m.id,
                                            homeTeamId = m.homeTeamId,
                                            awayTeamId = m.awayTeamId,
                                            leagueId = m.leagueId,
                                            homeScore = m.homeScore,
                                            awayScore = m.awayScore,
                                            status = m.status,
                                            minute = m.minute,
                                            startTime = m.startTime
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            delay(4000)
                        }
                    }
                    try {
                        db.matchDao().getAllMatches().map { entities ->
                            entities.map {
                                Match(
                                    id = it.id,
                                    homeTeamId = it.homeTeamId,
                                    awayTeamId = it.awayTeamId,
                                    leagueId = it.leagueId,
                                    homeScore = it.homeScore,
                                    awayScore = it.awayScore,
                                    status = MatchStatus.valueOf(it.status),
                                    minute = it.minute,
                                    startTime = it.startTime
                                )
                            }
                        }.collect {
                            emit(it)
                        }
                    } finally {
                        syncJob.cancel()
                    }
                }
            }
        }
    }

    override fun getLiveEvents(matchId: String): Flow<List<LiveEvent>> {
        return isApiMode().flatMapLatest { apiModeActive ->
            if (apiModeActive) {
                getExternalEventsFlow(matchId)
            } else {
                flow {
                    val syncJob = scope.launch {
                        while (true) {
                            try {
                                val supabaseEvents = SupabaseClient.api.getLiveEvents(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, matchId)
                                supabaseEvents.forEach { ev ->
                                    db.liveEventDao().insertLiveEvent(
                                        LiveEventEntity(
                                            id = ev.id,
                                            matchId = ev.matchId,
                                            type = ev.type,
                                            minute = ev.minute,
                                            scoringTeamId = ev.scoringTeamId,
                                            scorerPlayerId = ev.scorerPlayerId,
                                            assistPlayerId = ev.assistPlayerId
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            delay(4000)
                        }
                    }
                    try {
                        db.liveEventDao().getLiveEventsForMatch(matchId).map { entities ->
                            entities.map {
                                LiveEvent(
                                    id = it.id,
                                    matchId = it.matchId,
                                    type = EventType.valueOf(it.type),
                                    minute = it.minute,
                                    scoringTeamId = it.scoringTeamId,
                                    scorerPlayerId = it.scorerPlayerId,
                                    assistPlayerId = it.assistPlayerId
                                )
                            }
                        }.collect {
                            emit(it)
                        }
                    } finally {
                        syncJob.cancel()
                    }
                }
            }
        }
    }

    override suspend fun insertLeague(league: League) {
        db.leagueDao().insertLeague(
            LeagueEntity(league.id, league.name, league.country, league.logoUrl)
        )
        scope.launch {
            try {
                SupabaseClient.api.insertLeague(
                    SupabaseClient.API_KEY,
                    SupabaseClient.AUTH_HEADER,
                    SupabaseLeagueDto(league.id, league.name, league.country, league.logoUrl)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun insertTeam(team: Team) {
        db.teamDao().insertTeam(
            TeamEntity(team.id, team.name, team.leagueId, team.logoUrl)
        )
        scope.launch {
            try {
                SupabaseClient.api.insertTeam(
                    SupabaseClient.API_KEY,
                    SupabaseClient.AUTH_HEADER,
                    SupabaseTeamDto(team.id, team.name, team.leagueId, team.logoUrl)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun insertPlayer(player: Player) {
        db.playerDao().insertPlayer(
            PlayerEntity(player.id, player.name, player.teamId, player.photoUrl, player.number)
        )
        scope.launch {
            try {
                SupabaseClient.api.insertPlayer(
                    SupabaseClient.API_KEY,
                    SupabaseClient.AUTH_HEADER,
                    SupabasePlayerDto(player.id, player.name, player.teamId, player.photoUrl, player.number)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun scheduleMatch(match: Match) {
        db.matchDao().insertMatch(
            MatchEntity(
                id = match.id,
                homeTeamId = match.homeTeamId,
                awayTeamId = match.awayTeamId,
                leagueId = match.leagueId,
                homeScore = match.homeScore,
                awayScore = match.awayScore,
                status = match.status.name,
                minute = match.minute,
                startTime = match.startTime
            )
        )
        scope.launch {
            try {
                SupabaseClient.api.insertMatch(
                    SupabaseClient.API_KEY,
                    SupabaseClient.AUTH_HEADER,
                    SupabaseMatchDto(
                        id = match.id,
                        homeTeamId = match.homeTeamId,
                        awayTeamId = match.awayTeamId,
                        leagueId = match.leagueId,
                        homeScore = match.homeScore,
                        awayScore = match.awayScore,
                        status = match.status.name,
                        minute = match.minute,
                        startTime = match.startTime
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun insertLiveEvent(event: LiveEvent) {
        db.liveEventDao().insertLiveEvent(
            LiveEventEntity(
                id = event.id,
                matchId = event.matchId,
                type = event.type.name,
                minute = event.minute,
                scoringTeamId = event.scoringTeamId,
                scorerPlayerId = event.scorerPlayerId,
                assistPlayerId = event.assistPlayerId
            )
        )

        // Also update match score in Manual/Supabase mode
        var updatedMatch: MatchEntity? = null
        if (event.type == EventType.GOAL) {
            db.matchDao().getAllMatches().first().find { it.id == event.matchId }?.let { match ->
                val isHome = match.homeTeamId == event.scoringTeamId
                val newHomeScore = if (isHome) match.homeScore + 1 else match.homeScore
                val newAwayScore = if (!isHome) match.awayScore + 1 else match.awayScore
                updatedMatch = match.copy(
                    homeScore = newHomeScore,
                    awayScore = newAwayScore,
                    status = MatchStatus.LIVE.name,
                    minute = event.minute
                )
                db.matchDao().insertMatch(updatedMatch!!)
            }
        }

        scope.launch {
            try {
                // First push event
                SupabaseClient.api.insertLiveEvent(
                    SupabaseClient.API_KEY,
                    SupabaseClient.AUTH_HEADER,
                    SupabaseLiveEventDto(
                        id = event.id,
                        matchId = event.matchId,
                        type = event.type.name,
                        minute = event.minute,
                        scoringTeamId = event.scoringTeamId,
                        scorerPlayerId = event.scorerPlayerId,
                        assistPlayerId = event.assistPlayerId
                    )
                )
                
                // If the event updated the score, push the updated match object to Supabase too!
                updatedMatch?.let { m ->
                    SupabaseClient.api.insertMatch(
                        SupabaseClient.API_KEY,
                        SupabaseClient.AUTH_HEADER,
                        SupabaseMatchDto(
                            id = m.id,
                            homeTeamId = m.homeTeamId,
                            awayTeamId = m.awayTeamId,
                            leagueId = m.leagueId,
                            homeScore = m.homeScore,
                            awayScore = m.awayScore,
                            status = m.status,
                            minute = m.minute,
                            startTime = m.startTime
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    override fun isApiMode(): Flow<Boolean> {
        return db.appConfigDao().getConfig().map { it?.isApiMode ?: false }
    }

    override suspend fun setApiMode(active: Boolean) {
        val current = db.appConfigDao().getConfigDirect() ?: AppConfigEntity()
        db.appConfigDao().insertConfig(current.copy(isApiMode = active))
    }

    override fun getExternalApiConfig(): Flow<ExternalApiConfig> {
        return db.appConfigDao().getConfig().map { entity ->
            if (entity == null) {
                ExternalApiConfig()
            } else {
                val mappings = entity.serializedMappings.split(";").mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        ApiKeyMapping(parts[0], parts[1], getMappingDesc(parts[1]))
                    } else null
                }
                ExternalApiConfig(
                    baseUrl = entity.baseUrl,
                    apiKey = entity.apiKey,
                    mappings = mappings
                )
            }
        }
    }

    override suspend fun updateExternalApiConfig(config: ExternalApiConfig) {
        val current = db.appConfigDao().getConfigDirect() ?: AppConfigEntity()
        val serialized = config.mappings.joinToString(";") { "${it.externalKey}:${it.internalField}" }
        db.appConfigDao().insertConfig(
            current.copy(
                baseUrl = config.baseUrl,
                apiKey = config.apiKey,
                serializedMappings = serialized
            )
        )
    }

    override fun isPremium(): Flow<Boolean> {
        return db.appConfigDao().getConfig().map { it?.isPremium ?: false }
    }

    override suspend fun setPremium(active: Boolean) {
        val current = db.appConfigDao().getConfigDirect() ?: AppConfigEntity()
        db.appConfigDao().insertConfig(current.copy(isPremium = active))
    }

    override suspend fun testExternalApiConnection(baseUrl: String, apiKey: String): Boolean {
        // Simulate real network request ping with a small delay
        delay(800)
        return baseUrl.startsWith("http") && apiKey.isNotEmpty()
    }

    private fun getMappingDesc(internal: String): String {
        return when (internal) {
            "player_name" -> "Scorer's full name field"
            "homeScore" -> "Home team's goals"
            "awayScore" -> "Away team's goals"
            "minute" -> "Current match minute"
            "logoUrl" -> "Team logo visual asset"
            else -> "Custom field mapping"
        }
    }

    // --- MOCKED EXTERNAL LIVE SPORTS API SIMULATION ENGINE ---
    // This allows the app to fetch live matches simulated from an external API,
    // using mapping configurations defined by the admin.
    private fun getExternalMatchesFlow(): Flow<List<Match>> = flow {
        while (true) {
            // Emulate an external sports API response
            val config = db.appConfigDao().getConfigDirect() ?: AppConfigEntity()
            val mappingPairs = config.serializedMappings.split(";").associate {
                val parts = it.split(":")
                if (parts.size == 2) parts[1] to parts[0] else "" to ""
            }

            // Read custom mapped keys
            val keyHomeScore = mappingPairs["homeScore"] ?: "score_home"
            val keyAwayScore = mappingPairs["awayScore"] ?: "score_away"
            val keyMinute = mappingPairs["minute"] ?: "elapsed_min"

            // Construct dynamic JSON response matching the external API schema!
            val rawResponse = """
                [
                    {
                        "id": "ext_match_1",
                        "homeTeam": "Real Madrid",
                        "awayTeam": "Bayern Munich",
                        "league": "Champions League",
                        "$keyHomeScore": 2,
                        "$keyAwayScore": 1,
                        "status": "LIVE",
                        "$keyMinute": 82,
                        "startTime": 1720618000
                    },
                    {
                        "id": "ext_match_2",
                        "homeTeam": "Manchester City",
                        "awayTeam": "Liverpool",
                        "league": "Premier League",
                        "$keyHomeScore": 0,
                        "$keyAwayScore": 0,
                        "status": "LIVE",
                        "$keyMinute": 14,
                        "startTime": 1720619000
                    }
                ]
            """.trimIndent()

            // Parse raw response based on the dynamically configured mappings!
            val parsedMatches = mutableListOf<Match>()
            try {
                val jsonArray = JSONArray(rawResponse)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val homeName = obj.getString("homeTeam")
                    val awayName = obj.getString("awayTeam")
                    
                    // Match to local teams
                    val homeTeamId = when (homeName) {
                        "Real Madrid" -> "T1"
                        "Manchester City" -> "T2"
                        else -> "T1"
                    }
                    val awayTeamId = when (awayName) {
                        "Bayern Munich" -> "T3"
                        "Liverpool" -> "T7"
                        else -> "T3"
                    }

                    parsedMatches.add(
                        Match(
                            id = id,
                            homeTeamId = homeTeamId,
                            awayTeamId = awayTeamId,
                            leagueId = if (id == "ext_match_1") "L1" else "L2",
                            homeScore = obj.getInt(keyHomeScore),
                            awayScore = obj.getInt(keyAwayScore),
                            status = MatchStatus.LIVE,
                            minute = obj.getInt(keyMinute),
                            startTime = obj.getLong("startTime")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            emit(parsedMatches)
            delay(4000) // Poll every 4 seconds to simulate active livestreaming
        }
    }

    private fun getExternalEventsFlow(matchId: String): Flow<List<LiveEvent>> = flow {
        while (true) {
            val config = db.appConfigDao().getConfigDirect() ?: AppConfigEntity()
            val mappingPairs = config.serializedMappings.split(";").associate {
                val parts = it.split(":")
                if (parts.size == 2) parts[1] to parts[0] else "" to ""
            }

            val keyPlayerName = mappingPairs["player_name"] ?: "fullName"

            // Construct sample external JSON events with custom mapping
            val rawEventsResponse = when (matchId) {
                "ext_match_1" -> """
                    [
                        {
                            "id": "ext_evt_1",
                            "matchId": "ext_match_1",
                            "type": "GOAL",
                            "minute": 81,
                            "teamId": "T1",
                            "player": {
                                "$keyPlayerName": "Kylian Mbappé",
                                "id": "P1"
                            }
                        },
                        {
                            "id": "ext_evt_2",
                            "matchId": "ext_match_1",
                            "type": "GOAL",
                            "minute": 60,
                            "teamId": "T3",
                            "player": {
                                "$keyPlayerName": "Harry Kane",
                                "id": "P5"
                            }
                        },
                        {
                            "id": "ext_evt_3",
                            "matchId": "ext_match_1",
                            "type": "GOAL",
                            "minute": 15,
                            "teamId": "T1",
                            "player": {
                                "$keyPlayerName": "Vinicius Jr",
                                "id": "P2"
                            }
                        }
                    ]
                """.trimIndent()
                else -> "[]"
            }

            val parsedEvents = mutableListOf<LiveEvent>()
            try {
                val jsonArray = JSONArray(rawEventsResponse)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.getString("id")
                    val type = obj.getString("type")
                    val minute = obj.getInt("minute")
                    val teamId = obj.getString("teamId")
                    val playerObj = obj.getJSONObject("player")
                    val playerName = playerObj.getString(keyPlayerName)
                    val playerId = playerObj.getString("id")

                    parsedEvents.add(
                        LiveEvent(
                            id = id,
                            matchId = matchId,
                            type = EventType.valueOf(type),
                            minute = minute,
                            scoringTeamId = teamId,
                            scorerPlayerId = playerId, // Maps to scorer ID
                            assistPlayerId = null
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            emit(parsedEvents)
            delay(5000)
        }
    }

    private suspend fun prepopulateDatabaseLocallyOnly() {
        // UCL League
        db.leagueDao().insertLeague(
            LeagueEntity("L1", "Champions League", "Europe", "ucl")
        )
        // EPL League
        db.leagueDao().insertLeague(
            LeagueEntity("L2", "Premier League", "England", "epl")
        )

        // Teams
        db.teamDao().insertTeam(TeamEntity("T1", "Real Madrid", "L1", "real_madrid"))
        db.teamDao().insertTeam(TeamEntity("T2", "Manchester City", "L1", "man_city"))
        db.teamDao().insertTeam(TeamEntity("T3", "Bayern Munich", "L1", "bayern"))
        db.teamDao().insertTeam(TeamEntity("T4", "Paris SG", "L1", "psg"))

        db.teamDao().insertTeam(TeamEntity("T5", "Arsenal", "L2", "arsenal"))
        db.teamDao().insertTeam(TeamEntity("T6", "Chelsea", "L2", "chelsea"))
        db.teamDao().insertTeam(TeamEntity("T7", "Liverpool", "L2", "liverpool"))
        db.teamDao().insertTeam(TeamEntity("T8", "Manchester Utd", "L2", "man_utd"))

        // Players
        // Real Madrid
        db.playerDao().insertPlayer(PlayerEntity("P1", "Kylian Mbappé", "T1", "mbappe", 9))
        db.playerDao().insertPlayer(PlayerEntity("P2", "Vinicius Jr", "T1", "vinicius", 7))
        db.playerDao().insertPlayer(PlayerEntity("P3", "Jude Bellingham", "T1", "bellingham", 5))

        // Man City
        db.playerDao().insertPlayer(PlayerEntity("P4", "Erling Haaland", "T2", "haaland", 9))
        db.playerDao().insertPlayer(PlayerEntity("P4_2", "Kevin De Bruyne", "T2", "debruyne", 17))

        // Bayern Munich
        db.playerDao().insertPlayer(PlayerEntity("P5", "Harry Kane", "T3", "kane", 9))
        db.playerDao().insertPlayer(PlayerEntity("P6", "Jamal Musiala", "T3", "musiala", 42))

        // Arsenal
        db.playerDao().insertPlayer(PlayerEntity("P7", "Bukayo Saka", "T5", "saka", 7))
        db.playerDao().insertPlayer(PlayerEntity("P8", "Martin Ødegaard", "T5", "odegaard", 8))

        // App Config
        db.appConfigDao().insertConfig(
            AppConfigEntity(
                id = 1,
                isApiMode = false,
                baseUrl = "https://api.sportscore.com/v1",
                apiKey = "gs_live_abc123xyz",
                serializedMappings = "fullName:player_name;score_home:homeScore;score_away:awayScore;elapsed_min:minute;team_logo:logoUrl",
                isPremium = false
            )
        )

        // Schedule one default match to make user screen gorgeous immediately
        db.matchDao().insertMatch(
            MatchEntity(
                id = "m_default_1",
                homeTeamId = "T1",
                awayTeamId = "T2",
                leagueId = "L1",
                homeScore = 0,
                awayScore = 0,
                status = MatchStatus.LIVE.name,
                minute = 45,
                startTime = System.currentTimeMillis() - 2700000
            )
        )
    }

    private suspend fun prepopulateDatabase() {
        // First populate locally so UI works instantly
        prepopulateDatabaseLocallyOnly()

        // Push everything to Supabase to initialize the user's remote database!
        scope.launch {
            try {
                // Seeding Leagues
                SupabaseClient.api.insertLeague(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabaseLeagueDto("L1", "Champions League", "Europe", "ucl"))
                SupabaseClient.api.insertLeague(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabaseLeagueDto("L2", "Premier League", "England", "epl"))

                // Seeding Teams
                SupabaseClient.api.insertTeam(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabaseTeamDto("T1", "Real Madrid", "L1", "real_madrid"))
                SupabaseClient.api.insertTeam(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabaseTeamDto("T2", "Manchester City", "L1", "man_city"))
                SupabaseClient.api.insertTeam(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabaseTeamDto("T3", "Bayern Munich", "L1", "bayern"))
                SupabaseClient.api.insertTeam(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabaseTeamDto("T4", "Paris SG", "L1", "psg"))
                SupabaseClient.api.insertTeam(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabaseTeamDto("T5", "Arsenal", "L2", "arsenal"))
                SupabaseClient.api.insertTeam(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabaseTeamDto("T6", "Chelsea", "L2", "chelsea"))
                SupabaseClient.api.insertTeam(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabaseTeamDto("T7", "Liverpool", "L2", "liverpool"))
                SupabaseClient.api.insertTeam(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabaseTeamDto("T8", "Manchester Utd", "L2", "man_utd"))

                // Seeding Players
                SupabaseClient.api.insertPlayer(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabasePlayerDto("P1", "Kylian Mbappé", "T1", "mbappe", 9))
                SupabaseClient.api.insertPlayer(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabasePlayerDto("P2", "Vinicius Jr", "T1", "vinicius", 7))
                SupabaseClient.api.insertPlayer(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabasePlayerDto("P3", "Jude Bellingham", "T1", "bellingham", 5))
                SupabaseClient.api.insertPlayer(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabasePlayerDto("P4", "Erling Haaland", "T2", "haaland", 9))
                SupabaseClient.api.insertPlayer(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabasePlayerDto("P4_2", "Kevin De Bruyne", "T2", "debruyne", 17))
                SupabaseClient.api.insertPlayer(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabasePlayerDto("P5", "Harry Kane", "T3", "kane", 9))
                SupabaseClient.api.insertPlayer(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabasePlayerDto("P6", "Jamal Musiala", "T3", "musiala", 42))
                SupabaseClient.api.insertPlayer(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabasePlayerDto("P7", "Bukayo Saka", "T5", "saka", 7))
                SupabaseClient.api.insertPlayer(SupabaseClient.API_KEY, SupabaseClient.AUTH_HEADER, SupabasePlayerDto("P8", "Martin Ødegaard", "T5", "odegaard", 8))

                // Seeding Default Match
                SupabaseClient.api.insertMatch(
                    SupabaseClient.API_KEY,
                    SupabaseClient.AUTH_HEADER,
                    SupabaseMatchDto(
                        id = "m_default_1",
                        homeTeamId = "T1",
                        awayTeamId = "T2",
                        leagueId = "L1",
                        homeScore = 0,
                        awayScore = 0,
                        status = MatchStatus.LIVE.name,
                        minute = 45,
                        startTime = System.currentTimeMillis() - 2700000
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
