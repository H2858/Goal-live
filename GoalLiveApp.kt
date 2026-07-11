package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.AudioSynthesizer
import com.example.data.model.*
import com.example.ui.viewmodel.GoalLiveViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun GoalLiveApp(
    viewModel: GoalLiveViewModel = viewModel()
) {
    val leagues by viewModel.leagues.collectAsStateWithLifecycle()
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val isApiMode by viewModel.isApiMode.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val selectedMatch by viewModel.selectedMatch.collectAsStateWithLifecycle()
    val liveEvents by viewModel.liveEvents.collectAsStateWithLifecycle()
    val activeCelebration by viewModel.activeGoalCelebration.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf("dashboard") }
    var showAdminLoginDialog by remember { mutableStateOf(false) }
    var isAdminLoggedIn by remember { mutableStateOf(false) }
    var adminEmail by remember { mutableStateOf("") }
    var adminPassword by remember { mutableStateOf("") }
    var adminLoginError by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0F172A),
                contentColor = Color(0xFF64748B)
            ) {
                NavigationBarItem(
                    selected = currentTab == "dashboard",
                    onClick = { currentTab = "dashboard" },
                    icon = { Icon(Icons.Default.SportsSoccer, contentDescription = "Matches") },
                    label = { Text("Matches", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00FF66),
                        selectedTextColor = Color(0xFF00FF66),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = Color(0x2200FF66)
                    ),
                    modifier = Modifier.testTag("nav_dashboard")
                )
                if (isAdminLoggedIn) {
                    NavigationBarItem(
                        selected = currentTab == "admin",
                        onClick = { currentTab = "admin" },
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                        label = { Text("Admin Panel", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF00FF66),
                            selectedTextColor = Color(0xFF00FF66),
                            unselectedIconColor = Color(0xFF64748B),
                            unselectedTextColor = Color(0xFF64748B),
                            indicatorColor = Color(0x2200FF66)
                        ),
                        modifier = Modifier.testTag("nav_admin")
                    )
                }
                NavigationBarItem(
                    selected = currentTab == "premium",
                    onClick = { currentTab = "premium" },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Premium") },
                    label = { Text("Go Premium", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF00FF66),
                        selectedTextColor = Color(0xFF00FF66),
                        unselectedIconColor = Color(0xFF64748B),
                        unselectedTextColor = Color(0xFF64748B),
                        indicatorColor = Color(0x2200FF66)
                    ),
                    modifier = Modifier.testTag("nav_premium")
                )
            }
        },
        containerColor = Color(0xFF090D16)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "dashboard" -> UserDashboardScreen(
                    matches = matches,
                    selectedMatch = selectedMatch,
                    liveEvents = liveEvents,
                    teams = teams,
                    players = players,
                    isPremium = isPremium,
                    isApiMode = isApiMode,
                    onSelectMatch = { viewModel.selectMatch(it) },
                    onAdminClick = { showAdminLoginDialog = true }
                )
                "admin" -> {
                    if (isAdminLoggedIn) {
                        AdminPanelScreen(
                            viewModel = viewModel,
                            onExitAdmin = {
                                isAdminLoggedIn = false
                                currentTab = "dashboard"
                            }
                        )
                    } else {
                        currentTab = "dashboard"
                    }
                }
                "premium" -> PremiumPaywallScreen(
                    isPremium = isPremium,
                    onTogglePremium = { viewModel.togglePremium(it) }
                )
            }

            // Secure Admin Login Dialog
            if (showAdminLoginDialog) {
                Dialog(onDismissRequest = { showAdminLoginDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Login",
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Admin Access Portal",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Please verify your administrator credentials.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = adminEmail,
                                onValueChange = { adminEmail = it },
                                label = { Text("Gmail / Email") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00FF66),
                                    focusedLabelColor = Color(0xFF00FF66),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            OutlinedTextField(
                                value = adminPassword,
                                onValueChange = { adminPassword = it },
                                label = { Text("Password") },
                                singleLine = true,
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF00FF66),
                                    focusedLabelColor = Color(0xFF00FF66),
                                    unfocusedBorderColor = Color(0xFF334155),
                                    unfocusedLabelColor = Color(0xFF64748B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            adminLoginError?.let { error ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    color = Color.Red,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showAdminLoginDialog = false }) {
                                    Text("Cancel", color = Color(0xFF64748B))
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        scope.launch {
                                            isLoggingIn = true
                                            adminLoginError = null
                                            try {
                                                val authResult = com.example.data.remote.SupabaseClient.api.loginWithEmail(
                                                    apiKey = com.example.data.remote.SupabaseClient.API_KEY,
                                                    body = com.example.data.remote.SupabaseLoginRequest(
                                                        email = adminEmail,
                                                        password = adminPassword
                                                    )
                                                )
                                                val userId = authResult.user.id
                                                
                                                val roles = com.example.data.remote.SupabaseClient.api.getUserRoles(
                                                    apiKey = com.example.data.remote.SupabaseClient.API_KEY,
                                                    auth = "Bearer ${authResult.accessToken}",
                                                    userIdFilter = "eq.$userId",
                                                    roleFilter = "eq.admin"
                                                )
                                                
                                                if (roles.isNotEmpty()) {
                                                    isAdminLoggedIn = true
                                                    showAdminLoginDialog = false
                                                    currentTab = "admin"
                                                } else {
                                                    adminLoginError = "Access denied: This account is not designated as an admin in user_roles."
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                adminLoginError = "Authentication failed. Make sure the user is created in Supabase Auth, and the user_roles table is set up in Supabase with this user's UUID."
                                            } finally {
                                                isLoggingIn = false
                                            }
                                        }
                                    },
                                    enabled = !isLoggingIn && adminEmail.isNotBlank() && adminPassword.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00FF66),
                                        contentColor = Color.Black
                                    )
                                ) {
                                    if (isLoggingIn) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.Black, strokeWidth = 2.dp)
                                    } else {
                                        Text("Verify", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // High-Energy 60fps Neon GOOOAL Overlay Dialog
            activeCelebration?.let { celebration ->
                GoalCelebrationOverlay(
                    event = celebration,
                    teams = teams,
                    players = players,
                    onDismiss = { viewModel.dismissCelebration() }
                )
            }
        }
    }
}

@Composable
fun UserDashboardScreen(
    matches: List<Match>,
    selectedMatch: Match?,
    liveEvents: List<LiveEvent>,
    teams: List<Team>,
    players: List<Player>,
    isPremium: Boolean,
    isApiMode: Boolean,
    onSelectMatch: (Match) -> Unit,
    onAdminClick: () -> Unit
) {
    var isMuted by remember { mutableStateOf(AudioSynthesizer.isMuted) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GOAL LIVE",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        shadow = Shadow(Color(0xFF00FF66), Offset(0f, 0f), 12f)
                    )
                )
                Text(
                    text = if (isApiMode) "API Live Sports Source Active" else "Supabase Realtime Mode Active",
                    fontSize = 12.sp,
                    color = Color(0xFF00FF66),
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onAdminClick
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Admin Login",
                        tint = Color(0xFF64748B)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = {
                        isMuted = !isMuted
                        AudioSynthesizer.isMuted = isMuted
                    }
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        contentDescription = "Mute Toggle",
                        tint = if (isMuted) Color.Red else Color(0xFF00FF66)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(
                                if (isPremium) listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                                else listOf(Color(0xFF475569), Color(0xFF334155))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isPremium) "👑 PREMIUM ACTIVE" else "FREE TIER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Live Matches Header
        Text(
            text = "LIVE MATCHES",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF94A3B8),
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (matches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(Color(0xFF1E293B), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SportsSoccer, contentDescription = null, tint = Color(0xFF475569), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No live matches active currently.", color = Color(0xFF94A3B8), fontSize = 13.sp)
                }
            }
        } else {
            // Horizontal Match Slider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                matches.forEach { match ->
                    val homeTeam = teams.find { it.id == match.homeTeamId }
                    val awayTeam = teams.find { it.id == match.awayTeamId }

                    MatchCard(
                        match = match,
                        homeTeam = homeTeam,
                        awayTeam = awayTeam,
                        isSelected = selectedMatch?.id == match.id,
                        onClick = { onSelectMatch(match) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Selected Match Details Hub
        if (selectedMatch != null) {
            val homeTeam = teams.find { it.id == selectedMatch.homeTeamId }
            val awayTeam = teams.find { it.id == selectedMatch.awayTeamId }

            MatchDetailsHub(
                match = selectedMatch,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                liveEvents = liveEvents,
                players = players,
                teams = teams,
                isPremium = isPremium
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .background(Color(0xFF0F172A), RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, Color(0x11FFFFFF)), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color(0xFF334155), modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SELECT A MATCH",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap on any active card above to tune in to the live stream and enable real-time crowd celebrations.",
                        color = Color(0xFF64748B),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: Match,
    homeTeam: Team?,
    awayTeam: Team?,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(220.dp)
            .clickable { onClick() }
            .testTag("match_card_${match.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF0F172A)
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF00FF66) else Color(0x11FFFFFF)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFDC2626), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "LIVE",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "${match.minute}'",
                    color = Color(0xFF00FF66),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    TeamLogoIcon(teamName = homeTeam?.name ?: "Home", size = 40.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = homeTeam?.name ?: "Home",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }

                Text(
                    text = "${match.homeScore} - ${match.awayScore}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    TeamLogoIcon(teamName = awayTeam?.name ?: "Away", size = 40.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = awayTeam?.name ?: "Away",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun MatchDetailsHub(
    match: Match,
    homeTeam: Team?,
    awayTeam: Team?,
    liveEvents: List<LiveEvent>,
    players: List<Player>,
    teams: List<Team>,
    isPremium: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(24.dp))
            .border(BorderStroke(1.dp, Color(0x22FFFFFF)), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        // Glassmorphic Scoreboard Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0x11FFFFFF), RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, Color(0x11FFFFFF)), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    TeamLogoIcon(teamName = homeTeam?.name ?: "Home", size = 56.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(homeTeam?.name ?: "Home", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${match.homeScore} - ${match.awayScore}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            shadow = Shadow(Color.Black, Offset(0f, 4f), 8f)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF00FF66), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE ${match.minute}'",
                            fontSize = 11.sp,
                            color = Color(0xFF00FF66),
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    TeamLogoIcon(teamName = awayTeam?.name ?: "Away", size = 56.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(awayTeam?.name ?: "Away", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Match Stream Event Logs
        Text(
            text = "LIVE COMMENTARY & EVENTS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF64748B),
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (liveEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Waiting for kick-off. Live events will stream here.", color = Color(0xFF475569), fontSize = 13.sp)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                liveEvents.forEach { event ->
                    val scorer = players.find { it.id == event.scorerPlayerId }
                    val team = teams.find { it.id == event.scoringTeamId }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0x2200FF66), CircleShape)
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsSoccer,
                                contentDescription = null,
                                tint = Color(0xFF00FF66),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GOOOOAL! ${team?.name ?: "Team"}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Scored by ${scorer?.name ?: "Unknown Player"}",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        }

                        Text(
                            text = "${event.minute}'",
                            color = Color(0xFF00FF66),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

// Custom High-Quality Vector Logos Generated in Kotlin
@Composable
fun TeamLogoIcon(teamName: String, size: androidx.compose.ui.unit.Dp) {
    val logoColor = when (teamName) {
        "Real Madrid" -> Color(0xFFECEFF1)
        "Manchester City" -> Color(0xFF81D4FA)
        "Bayern Munich" -> Color(0xFFEF5350)
        "Paris SG" -> Color(0xFF1565C0)
        "Arsenal" -> Color(0xFFE53935)
        "Chelsea" -> Color(0xFF1E88E5)
        "Liverpool" -> Color(0xFFC62828)
        "Manchester Utd" -> Color(0xFFD84315)
        else -> Color(0xFF00FF66)
    }

    Box(
        modifier = Modifier
            .size(size)
            .background(logoColor.copy(alpha = 0.2f), CircleShape)
            .border(BorderStroke(2.dp, logoColor), CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = teamName.take(2).uppercase(),
            color = logoColor,
            fontWeight = FontWeight.Black,
            fontSize = (size.value * 0.35f).sp
        )
    }
}

@Composable
fun PlayerAvatarIcon(playerName: String, size: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .background(Color(0xFF1E293B), CircleShape)
            .border(BorderStroke(2.dp, Color(0xFF00FF66)), CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Player photo",
            tint = Color(0xFF00FF66),
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

// ADVANCED 4-SECTION ADMIN PANEL
@Composable
fun AdminPanelScreen(viewModel: GoalLiveViewModel, onExitAdmin: () -> Unit) {
    var adminSection by remember { mutableStateOf(1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ADMIN CONTROLLER PANEL",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Manage squads, matches, active goal injections, and external APIs",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
            IconButton(
                onClick = onExitAdmin
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Logout",
                    tint = Color.Red
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Multi-Section Segmented Tab Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(1, 2, 3, 4).forEach { section ->
                val label = when (section) {
                    1 -> "Squads"
                    2 -> "Scheduler"
                    3 -> "Live Hub"
                    else -> "API Mode"
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { adminSection = section }
                        .background(
                            color = if (adminSection == section) Color(0xFF00FF66) else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (adminSection == section) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Sections Switch
        when (adminSection) {
            1 -> AdminSquadsSection(viewModel)
            2 -> AdminSchedulerSection(viewModel)
            3 -> AdminLiveHubSection(viewModel)
            4 -> AdminApiModeSection(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSquadsSection(viewModel: GoalLiveViewModel) {
    var name by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    var teamName by remember { mutableStateOf("") }
    val leagues by viewModel.leagues.collectAsStateWithLifecycle()
    var selectedLeagueId by remember { mutableStateOf("") }

    var playerName by remember { mutableStateOf("") }
    var playerNum by remember { mutableStateOf("") }
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    var selectedTeamId by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Add New Football League", fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("League Name (e.g. Premier League)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF66),
                    focusedLabelColor = Color(0xFF00FF66),
                    unfocusedBorderColor = Color(0xFF334155),
                    unfocusedLabelColor = Color(0xFF64748B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_league_name")
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = country,
                onValueChange = { country = it },
                label = { Text("Country") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF66),
                    focusedLabelColor = Color(0xFF00FF66),
                    unfocusedBorderColor = Color(0xFF334155),
                    unfocusedLabelColor = Color(0xFF64748B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (name.isNotEmpty()) {
                        viewModel.addLeague(name, country)
                        name = ""
                        country = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_league")
            ) {
                Text("Create League", fontWeight = FontWeight.Bold)
            }

            Divider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 16.dp))

            Text("Add New Team", fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = teamName,
                onValueChange = { teamName = it },
                label = { Text("Team Name (e.g. Manchester City)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF66),
                    focusedLabelColor = Color(0xFF00FF66),
                    unfocusedBorderColor = Color(0xFF334155),
                    unfocusedLabelColor = Color(0xFF64748B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Select League", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                leagues.forEach { league ->
                    FilterChip(
                        selected = selectedLeagueId == league.id,
                        onClick = { selectedLeagueId = league.id },
                        label = { Text(league.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00FF66),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (teamName.isNotEmpty() && selectedLeagueId.isNotEmpty()) {
                        viewModel.addTeam(teamName, selectedLeagueId)
                        teamName = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Team", fontWeight = FontWeight.Bold)
            }

            Divider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 16.dp))

            Text("Add New Player", fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = playerName,
                onValueChange = { playerName = it },
                label = { Text("Player Name (e.g. Kylian Mbappé)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF66),
                    focusedLabelColor = Color(0xFF00FF66),
                    unfocusedBorderColor = Color(0xFF334155),
                    unfocusedLabelColor = Color(0xFF64748B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = playerNum,
                onValueChange = { playerNum = it },
                label = { Text("Shirt Number") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF66),
                    focusedLabelColor = Color(0xFF00FF66),
                    unfocusedBorderColor = Color(0xFF334155),
                    unfocusedLabelColor = Color(0xFF64748B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text("Select Team", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                teams.forEach { team ->
                    FilterChip(
                        selected = selectedTeamId == team.id,
                        onClick = { selectedTeamId = team.id },
                        label = { Text(team.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00FF66),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = {
                    if (playerName.isNotEmpty() && selectedTeamId.isNotEmpty()) {
                        viewModel.addPlayer(playerName, selectedTeamId, playerNum.toIntOrNull() ?: 10)
                        playerName = ""
                        playerNum = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create Player Profile & Photo", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSchedulerSection(viewModel: GoalLiveViewModel) {
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    val leagues by viewModel.leagues.collectAsStateWithLifecycle()

    var homeTeamId by remember { mutableStateOf("") }
    var awayTeamId by remember { mutableStateOf("") }
    var selectedLeagueId by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Match Scheduler Console", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Select League Context", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                leagues.forEach { league ->
                    FilterChip(
                        selected = selectedLeagueId == league.id,
                        onClick = { selectedLeagueId = league.id },
                        label = { Text(league.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00FF66),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Select Team A (Home)", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                teams.forEach { team ->
                    FilterChip(
                        selected = homeTeamId == team.id,
                        onClick = { homeTeamId = team.id },
                        label = { Text(team.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00FF66),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Select Team B (Away)", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                teams.filter { it.id != homeTeamId }.forEach { team ->
                    FilterChip(
                        selected = awayTeamId == team.id,
                        onClick = { awayTeamId = team.id },
                        label = { Text(team.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00FF66),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (homeTeamId.isNotEmpty() && awayTeamId.isNotEmpty() && selectedLeagueId.isNotEmpty()) {
                        viewModel.scheduleMatch(homeTeamId, awayTeamId, selectedLeagueId)
                        homeTeamId = ""
                        awayTeamId = ""
                    }
                },
                enabled = homeTeamId.isNotEmpty() && awayTeamId.isNotEmpty() && selectedLeagueId.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00FF66),
                    contentColor = Color.Black,
                    disabledContainerColor = Color(0xFF1E293B)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_schedule_match")
            ) {
                Text("Schedule & Kick Off Match", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLiveHubSection(viewModel: GoalLiveViewModel) {
    val matches by viewModel.matches.collectAsStateWithLifecycle()
    val teams by viewModel.teams.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()

    var selectedAdminMatchId by remember { mutableStateOf("") }
    var selectedGoalscorerId by remember { mutableStateOf("") }
    var scoringTeamId by remember { mutableStateOf("") }
    var goalMinute by remember { mutableStateOf("45") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Live Events Real-Time Injector", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Text("Select Active Match to Control", color = Color(0xFF94A3B8), fontSize = 12.sp)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                matches.forEach { match ->
                    val home = teams.find { it.id == match.homeTeamId }
                    val away = teams.find { it.id == match.awayTeamId }
                    FilterChip(
                        selected = selectedAdminMatchId == match.id,
                        onClick = {
                            selectedAdminMatchId = match.id
                            scoringTeamId = ""
                            selectedGoalscorerId = ""
                        },
                        label = { Text("${home?.name ?: "Home"} vs ${away?.name ?: "Away"}") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF00FF66),
                            selectedLabelColor = Color.Black,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color.White
                        ),
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            selectedAdminMatchId.let { matchId ->
                val activeMatch = matches.find { it.id == matchId }
                if (activeMatch != null) {
                    val homeTeam = teams.find { it.id == activeMatch.homeTeamId }
                    val awayTeam = teams.find { it.id == activeMatch.awayTeamId }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Step 1: Select Scoring Team", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        homeTeam?.let {
                            FilterChip(
                                selected = scoringTeamId == it.id,
                                onClick = {
                                    scoringTeamId = it.id
                                    selectedGoalscorerId = ""
                                },
                                label = { Text(it.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00FF66),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                        awayTeam?.let {
                            FilterChip(
                                selected = scoringTeamId == it.id,
                                onClick = {
                                    scoringTeamId = it.id
                                    selectedGoalscorerId = ""
                                },
                                label = { Text(it.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF00FF66),
                                    selectedLabelColor = Color.Black,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }
                    }

                    if (scoringTeamId.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Step 2: Select Scorer from Team Roster", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        val roster = players.filter { it.teamId == scoringTeamId }
                        if (roster.isEmpty()) {
                            Text("No players preloaded for this team. Please add squad members first in Squads section.", color = Color.Red, fontSize = 12.sp)
                        } else {
                            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                roster.forEach { player ->
                                    FilterChip(
                                        selected = selectedGoalscorerId == player.id,
                                        onClick = { selectedGoalscorerId = player.id },
                                        label = { Text("${player.number}. ${player.name}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF00FF66),
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF1E293B),
                                            labelColor = Color.White
                                        ),
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = goalMinute,
                        onValueChange = { goalMinute = it },
                        label = { Text("Goal Minute") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00FF66),
                            focusedLabelColor = Color(0xFF00FF66),
                            unfocusedBorderColor = Color(0xFF334155),
                            unfocusedLabelColor = Color(0xFF64748B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (selectedGoalscorerId.isNotEmpty() && scoringTeamId.isNotEmpty()) {
                                viewModel.triggerLiveGoal(
                                    matchId = matchId,
                                    scoringTeamId = scoringTeamId,
                                    scorerPlayerId = selectedGoalscorerId,
                                    assistPlayerId = null,
                                    minute = goalMinute.toIntOrNull() ?: 45
                                )
                            }
                        },
                        enabled = selectedGoalscorerId.isNotEmpty() && scoringTeamId.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF1E293B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("btn_trigger_goal")
                    ) {
                        Text(
                            text = "⚽ GOOOOOAL!!!",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.labelLarge.copy(
                                shadow = Shadow(Color.Black, Offset(0f, 2f), 4f)
                            )
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Please schedule and activate a match in the Scheduler tab to start injecting live goals.", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AdminApiModeSection(viewModel: GoalLiveViewModel) {
    val isApiMode by viewModel.isApiMode.collectAsStateWithLifecycle()
    val apiConfig by viewModel.externalApiConfig.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTestingConnection.collectAsStateWithLifecycle()
    val testSuccess by viewModel.apiTestSuccess.collectAsStateWithLifecycle()

    var baseUrl by remember { mutableStateOf(apiConfig.baseUrl) }
    var apiKey by remember { mutableStateOf(apiConfig.apiKey) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dynamic Data Source Master Switch", fontWeight = FontWeight.Bold, color = Color.White)
                Switch(
                    checked = isApiMode,
                    onCheckedChange = { viewModel.toggleApiMode(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00FF66),
                        checkedTrackColor = Color(0x6600FF66),
                        uncheckedThumbColor = Color(0xFF64748B),
                        uncheckedTrackColor = Color(0xFF1E293B)
                    ),
                    modifier = Modifier.testTag("toggle_api_mode")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isApiMode) "STATUS: Bypassing local Supabase match tables and fetching from mapped API schema"
                else "STATUS: Manual Supabase simulation active",
                color = if (isApiMode) Color(0xFF00FF66) else Color(0xFF94A3B8),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Divider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 16.dp))

            Text("External Live Sports Web API Integration", fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base API URL") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF66),
                    focusedLabelColor = Color(0xFF00FF66),
                    unfocusedBorderColor = Color(0xFF334155),
                    unfocusedLabelColor = Color(0xFF64748B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Authorization Key") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF66),
                    focusedLabelColor = Color(0xFF00FF66),
                    unfocusedBorderColor = Color(0xFF334155),
                    unfocusedLabelColor = Color(0xFF64748B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.testApiConnection(baseUrl, apiKey) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(color = Color(0xFF00FF66), modifier = Modifier.size(20.dp))
                    } else {
                        Text("Test Connection")
                    }
                }

                Button(
                    onClick = { viewModel.saveApiConfig(baseUrl, apiKey, apiConfig.mappings) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66), contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Config", fontWeight = FontWeight.Bold)
                }
            }

            testSuccess?.let { success ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (success) Color(0x2200FF66) else Color(0x22EF4444), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (success) Color(0xFF00FF66) else Color.Red
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (success) "Ping Successful: 200 OK Connection Established" else "Ping Failed: Endpoint Connection Error",
                        color = if (success) Color(0xFF00FF66) else Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider(color = Color(0xFF1E293B), modifier = Modifier.padding(vertical = 16.dp))

            // Data Mapping Table UI
            Text("Dynamic Data Mapping Matrix", fontWeight = FontWeight.Bold, color = Color.White)
            Text("Align external sport schema attributes with app internal identifiers", fontSize = 11.sp, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                apiConfig.mappings.forEach { mapping ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(mapping.internalField, color = Color(0xFF00FF66), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(mapping.description, color = Color(0xFF64748B), fontSize = 10.sp)
                        }

                        // Editable external key representation
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                .border(BorderStroke(1.dp, Color(0x22FFFFFF)), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(mapping.externalKey, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// PREMIUM MONETIZATION SUBSCRIPTION SCREEN (RevenueCat integration)
@Composable
fun PremiumPaywallScreen(isPremium: Boolean, onTogglePremium: (Boolean) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(1.dp, Color(0xFF00FF66))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = "Premium Crown",
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GOAL LIVE GOLD",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge.copy(
                    shadow = Shadow(Color(0xFFFFD700), Offset(0f, 0f), 16f)
                )
            )

            Text(
                text = "Cinematic Stadium Experience Unlocked",
                fontSize = 14.sp,
                color = Color(0xFFFFD700),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Subscription status card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ACCOUNTLESS BILLING VERIFICATION",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RevenueCat SDK Server Receipt Secured",
                            fontSize = 12.sp,
                            color = Color(0xFF00FF66),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Benefits checklist
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(
                    "Instant Realtime Audio: 3D stadium crowd cheers generated natively upon goal scored events.",
                    "Kinetic 60fps Celebrations: Fullscreen neon animations accompanied by synchronized haptic soundscape vibrations.",
                    "Sleek Scorer Transition: Scorecards morphing to spotlight player full portraits, numbers, and team crest badges."
                ).forEach { benefit ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF00FF66), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(benefit, color = Color(0xFFE2E8F0), fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Toggle Buy button
            Button(
                onClick = { onTogglePremium(!isPremium) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPremium) Color(0xFF1E293B) else Color(0xFFFFD700),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("toggle_premium_btn")
            ) {
                Text(
                    text = if (isPremium) "ACTIVE — RESTORE / CANCEL" else "SUBSCRIBE NOW — $2.99 / Month",
                    color = if (isPremium) Color.White else Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// CINEMATIC KINETIC GOAL CELEBRATION OVERLAY
@Composable
fun GoalCelebrationOverlay(
    event: LiveEvent,
    teams: List<Team>,
    players: List<Player>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scorer = players.find { it.id == event.scorerPlayerId }
    val team = teams.find { it.id == event.scoringTeamId }

    var stage by remember { mutableStateOf(1) } // 1: Neon text overlay, 2: Morphing card showcase

    // Auto advancement timer
    LaunchedEffect(Unit) {
        delay(2200) // Neon overlay shows for 2.2 seconds
        stage = 2
    }

    Dialog(
        onDismissRequest = { onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xEE090D16))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = stage,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500)) togetherWith fadeOut(animationSpec = tween(400))
                }
            ) { targetStage ->
                if (targetStage == 1) {
                    // Stage 1: Full-screen neon 'GOOOOOAL' kinetic scale pop
                    NeonGoalTextStage(teamName = team?.name ?: "GOAL")
                } else {
                    // Stage 2: Smoothly morph into player spotlight showcase card
                    PlayerSpotlightCard(
                        playerName = scorer?.name ?: "Kylian Mbappé",
                        shirtNum = scorer?.number ?: 9,
                        teamName = team?.name ?: "Real Madrid",
                        goalMinute = event.minute,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
fun NeonGoalTextStage(teamName: String) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(24.dp)
    ) {
        Text(
            text = "GOOOOOAL!!!",
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF00FF66),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayMedium.copy(
                shadow = Shadow(
                    color = Color(0xFF00FF66),
                    offset = Offset(0f, 0f),
                    blurRadius = 24f * glowScale
                )
            ),
            modifier = Modifier.scale(glowScale)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = teamName.uppercase(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 2.sp
        )
    }
}

@Composable
fun PlayerSpotlightCard(
    playerName: String,
    shirtNum: Int,
    teamName: String,
    goalMinute: Int,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Card(
        modifier = Modifier
            .width(320.dp)
            .padding(16.dp)
            .scale(pulseScale)
            .clickable(enabled = false) {},
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = BorderStroke(2.dp, Color(0xFF00FF66)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header crest + goal minute badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamLogoIcon(teamName = teamName, size = 36.dp)
                Box(
                    modifier = Modifier
                        .background(Color(0xFF00FF66), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$goalMinute'",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Player Avatar Photo spotlight frame
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                // Pulse halo ring
                Box(
                    modifier = Modifier
                        .size(110.dp * pulseScale)
                        .background(Color(0x1100FF66), CircleShape)
                        .border(BorderStroke(2.dp, Color(0xFF00FF66)), CircleShape)
                )
                PlayerAvatarIcon(playerName = playerName, size = 96.dp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = playerName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "NUMBER $shirtNum • ${teamName.uppercase()}",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dismiss Celebration")
            }
        }
    }
}
