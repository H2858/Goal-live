package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioSynthesizer
import com.example.data.model.*
import com.example.data.repository.DynamicGoalLiveRepository
import com.example.data.repository.GoalLiveRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class GoalLiveViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GoalLiveRepository = DynamicGoalLiveRepository(application)
    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    val leagues = repository.getLeagues().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val teams = repository.getTeams().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val players = repository.getPlayers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val matches = repository.getMatches().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val isApiMode = repository.isApiMode().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val externalApiConfig = repository.getExternalApiConfig().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExternalApiConfig())
    val isPremium = repository.isPremium().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val selectedMatch = MutableStateFlow<Match?>(null)
    
    val liveEvents = selectedMatch.flatMapLatest { match ->
        if (match != null) {
            repository.getLiveEvents(match.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeGoalCelebration = MutableStateFlow<LiveEvent?>(null)
    
    private val processedEventIds = mutableSetOf<String>()

    val isTestingConnection = MutableStateFlow(false)
    val apiTestSuccess = MutableStateFlow<Boolean?>(null)

    init {
        viewModelScope.launch {
            liveEvents.collect { events ->
                val currentMatch = selectedMatch.value ?: return@collect
                val premiumActive = isPremium.value
                
                val newGoals = events.filter { it.type == EventType.GOAL && !processedEventIds.contains(it.id) }
                
                if (newGoals.isNotEmpty()) {
                    val latestGoal = newGoals.first()
                    newGoals.forEach { processedEventIds.add(it.id) }
                    
                    if (premiumActive) {
                        triggerPremiumCelebration(latestGoal)
                    }
                }
            }
        }
    }

    private fun triggerPremiumCelebration(event: LiveEvent) {
        AudioSynthesizer.playGoalCheer()

        activeGoalCelebration.value = event

        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createWaveform(
                            longArrayOf(0, 150, 80, 250, 80, 400),
                            intArrayOf(0, 255, 0, 255, 0, 255),
                            -1
                        )
                    )
                } else {
                    vibrator.vibrate(600)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun dismissCelebration() {
        activeGoalCelebration.value = null
        AudioSynthesizer.stopAll()
    }

    fun selectMatch(match: Match) {
        selectedMatch.value = match
        viewModelScope.launch {
            val existing = repository.getLiveEvents(match.id).first()
            existing.forEach { processedEventIds.add(it.id) }
        }
    }

    fun addLeague(name: String, country: String) {
        viewModelScope.launch {
            repository.insertLeague(League(UUID.randomUUID().toString(), name, country, "league_default"))
        }
    }

    fun addTeam(name: String, leagueId: String) {
        viewModelScope.launch {
            repository.insertTeam(Team(UUID.randomUUID().toString(), name, leagueId, "team_default"))
        }
    }

    fun addPlayer(name: String, teamId: String, number: Int) {
        viewModelScope.launch {
            repository.insertPlayer(Player(UUID.randomUUID().toString(), name, teamId, "player_default", number))
        }
    }

    fun scheduleMatch(homeTeamId: String, awayTeamId: String, leagueId: String) {
        viewModelScope.launch {
            val match = Match(
                id = UUID.randomUUID().toString(),
                homeTeamId = homeTeamId,
                awayTeamId = awayTeamId,
                leagueId = leagueId,
                homeScore = 0,
                awayScore = 0,
                status = MatchStatus.LIVE,
                minute = 1,
                startTime = System.currentTimeMillis()
            )
            repository.scheduleMatch(match)
        }
    }

    fun triggerLiveGoal(matchId: String, scoringTeamId: String, scorerPlayerId: String, assistPlayerId: String?, minute: Int) {
        viewModelScope.launch {
            val event = LiveEvent(
                id = UUID.randomUUID().toString(),
                matchId = matchId,
                type = EventType.GOAL,
                minute = minute,
                scoringTeamId = scoringTeamId,
                scorerPlayerId = scorerPlayerId,
                assistPlayerId = assistPlayerId
            )
            repository.insertLiveEvent(event)
        }
    }

    fun toggleApiMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setApiMode(enabled)
            selectedMatch.value = null
            processedEventIds.clear()
        }
    }

    fun testApiConnection(baseUrl: String, apiKey: String) {
        viewModelScope.launch {
            isTestingConnection.value = true
            apiTestSuccess.value = null
            val success = repository.testExternalApiConnection(baseUrl, apiKey)
            apiTestSuccess.value = success
            isTestingConnection.value = false
        }
    }

    fun saveApiConfig(baseUrl: String, apiKey: String, mappings: List<ApiKeyMapping>) {
        viewModelScope.launch {
            repository.updateExternalApiConfig(
                ExternalApiConfig(baseUrl = baseUrl, apiKey = apiKey, mappings = mappings)
            )
        }
    }

    fun togglePremium(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPremium(enabled)
        }
    }
}
