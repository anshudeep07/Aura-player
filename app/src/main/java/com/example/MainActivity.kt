package com.example

import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.ui.theme.*
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels {
        PlayerViewModelFactory(this.applicationContext)
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val permissions = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
                    permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                
                val permissionState = com.google.accompanist.permissions.rememberMultiplePermissionsState(
                    permissions = permissions
                )
                
                // Allow proceeding if audio permission is granted, even if notifications are denied
                val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionState.permissions.any { it.permission == android.Manifest.permission.READ_MEDIA_AUDIO && it.status.isGranted }
                } else {
                    permissionState.permissions.any { it.permission == android.Manifest.permission.READ_EXTERNAL_STORAGE && it.status.isGranted }
                }

                LaunchedEffect(audioGranted) {
                    if (audioGranted) {
                        viewModel.loadTracks()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (audioGranted) {
                        val uiState by viewModel.uiState.collectAsState()
                        PlayerScreen(
                            uiState = uiState,
                            onPlayPause = { viewModel.togglePlayPause() },
                            onNext = { viewModel.nextTrack() },
                            onPrev = { viewModel.previousTrack() },
                            onCycleAccent = { viewModel.cycleAccent() },
                            onTrackSelect = { viewModel.playTrack(it) }
                        )
                    } else {
                        val uiState by viewModel.uiState.collectAsState()
                        PermissionScreen(
                            uiState = uiState,
                            onRequestPermission = { permissionState.launchMultiplePermissionRequest() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerScreen(
    uiState: PlayerState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onCycleAccent: () -> Unit,
    onTrackSelect: (AudioTrack) -> Unit
) {
    var currentTab by remember { mutableStateOf("Now Playing") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.systemBars.asPaddingValues())
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (currentTab == "Now Playing") {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AlbumArt(uiState = uiState)
                    }
            
                    Spacer(modifier = Modifier.height(40.dp))
            
                    TrackInfo(uiState = uiState)
            
                    Spacer(modifier = Modifier.height(32.dp))
            
                    ControlsSection(
                        uiState = uiState,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrev = onPrev
                    )
            
                    BottomTooltip(uiState = uiState)
                    Spacer(modifier = Modifier.weight(1f))
                }
            } else if (currentTab == "Search") {
                SearchScreen(
                    uiState = uiState,
                    onTrackSelect = onTrackSelect
                )
            }
        }

        BottomNavigationBar(
            uiState = uiState,
            currentTab = currentTab,
            onTabSelect = { currentTab = it },
            onCycleAccent = onCycleAccent
        )
    }
}

@Composable
fun AlbumArt(uiState: PlayerState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .widthIn(max = 320.dp)
    ) {
        // Blur background simulation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = 16.dp, y = 16.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            uiState.currentAccent.primary.copy(alpha = 0.2f),
                            uiState.currentAccent.secondary.copy(alpha = 0.2f)
                        )
                    )
                ) // We would normally use blur modifier here, but sticking to gradients
        )
        // Main Art Container
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(40.dp))
                .background(Color(0xFF1C1E21))
                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(40.dp))
        ) {
            // Abstract gradients inside
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(uiState.currentAccent.primary.copy(alpha = 0.2f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(200f, 200f),
                        radius = 400f
                    )
                )
            )
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.radialGradient(
                        colors = listOf(uiState.currentAccent.secondary.copy(alpha = 0.2f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(600f, 600f),
                        radius = 500f
                    )
                )
            )
            
            Icon(
                imageVector = Icons.Filled.Album,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.1f),
                modifier = Modifier.align(Alignment.Center).size(120.dp)
            )
            
            // HI-RES Badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(uiState.currentAccent.container)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "HI-RES AUDIO",
                    color = uiState.currentAccent.onContainer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
        }
    }
}

@Composable
fun TrackInfo(uiState: PlayerState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.currentTrack?.title ?: "Stargazing",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = uiState.currentTrack?.artist ?: "Lumina Project",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFFC4C6D0),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = { /* TODO */ },
                modifier = Modifier.padding(bottom = 4.dp).size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = uiState.currentAccent.container,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Output Device State
        var outputIndex by remember { mutableStateOf(0) }
        val outputIcons = listOf(Icons.Filled.Speaker, Icons.Filled.Headset, Icons.Filled.Bluetooth)
        
        // Simulated Bluetooth Codec Detection
        var codecIndex by remember { mutableStateOf(0) }
        val bluetoothCodecs = listOf("SBC", "AAC", "aptX", "LDAC")

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp), 
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState())
            ) {
                SpecBadge("FLAC", border = true)
                SpecBadge("24-bit / 192kHz", border = true)
                if (outputIndex == 2) {
                    SpecBadge(
                        text = bluetoothCodecs[codecIndex], 
                        border = true, 
                        textColor = uiState.currentAccent.primary,
                        modifier = Modifier.clickable { codecIndex = (codecIndex + 1) % bluetoothCodecs.size }
                    )
                }
                SpecBadge(
                    "TRUE PLAYBACK", 
                    border = false, 
                    bgColor = Color(0x3378350F), 
                    textColor = Color(0x99FDE68A)
                )
            }
            
            // Output Device Cube Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .clickable { outputIndex = (outputIndex + 1) % 3 },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = outputIcons[outputIndex],
                    contentDescription = "Output device",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SpecBadge(
    text: String, 
    border: Boolean, 
    bgColor: Color = Color.Transparent, 
    textColor: Color = Color.White.copy(alpha = 0.4f),
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .then(if (border) Modifier.border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp)) else Modifier)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            color = textColor,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (!border) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ControlsSection(
    uiState: PlayerState,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
    ) {
        // Progress Bar
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(4.dp)
                        .background(uiState.currentAccent.primary)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val formattedDuration = if (uiState.currentTrack != null) {
                    DateUtils.formatElapsedTime(uiState.currentTrack.duration / 1000)
                } else {
                    "02:44"
                }
                Text(text = "01:31", fontSize = 11.sp, color = Color(0xFFC4C6D0), fontFamily = FontFamily.Monospace)
                Text(text = "-$formattedDuration", fontSize = 11.sp, color = Color(0xFFC4C6D0), fontFamily = FontFamily.Monospace)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Playback Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Filled.Shuffle, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(uiState.currentAccent.container)
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (uiState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = uiState.currentAccent.onContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.Filled.Repeat, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
fun BottomTooltip(uiState: PlayerState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1E21))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
            Text(
                text = "System and device recordings are hidden. Tap the Engine icon below to change theme color.",
                fontSize = 11.sp,
                color = Color(0xFFC4C6D0),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    uiState: PlayerState, 
    currentTab: String, 
    onTabSelect: (String) -> Unit, 
    onCycleAccent: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xFF1C1E21))
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(32.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItem(
            icon = Icons.Filled.PlayCircle, 
            label = "Playing", 
            active = currentTab == "Now Playing", 
            activeColor = uiState.currentAccent.primary,
            onClick = { onTabSelect("Now Playing") }
        )
        NavItem(
            icon = Icons.Filled.Search, 
            label = "Search", 
            active = currentTab == "Search", 
            activeColor = uiState.currentAccent.primary,
            onClick = { onTabSelect("Search") }
        )
        NavItem(
            icon = Icons.Filled.Palette, 
            label = "Theme", 
            active = false, 
            activeColor = uiState.currentAccent.primary, 
            onClick = onCycleAccent
        )
    }
}

@Composable
fun NavItem(icon: ImageVector, label: String, active: Boolean, activeColor: Color, onClick: (() -> Unit)? = null) {
    val color = if (active) activeColor else Color(0xFFC4C6D0).copy(alpha = 0.6f)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally, 
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Text(text = label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PermissionScreen(uiState: PlayerState? = null, onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val primaryColor = uiState?.currentAccent?.primary ?: Color(0xFF22D3EE)
        val containerColor = uiState?.currentAccent?.container ?: Color(0xFFD1E4FF)
        val onContainerColor = uiState?.currentAccent?.onContainer ?: Color(0xFF00315C)
        
        Icon(
            imageVector = Icons.Default.Info, 
            contentDescription = "Permission Required",
            tint = primaryColor,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Storage Permission Needed",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Aura Player needs permission to read your high-fidelity audio files.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = onContainerColor)
        ) {
            Text("Grant Permission")
        }
    }
}

@Composable
fun SearchScreen(uiState: PlayerState, onTrackSelect: (AudioTrack) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredTracks = uiState.tracks.filter {
        it.title.contains(searchQuery, ignoreCase = true) ||
        it.artist.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search tracks or artists...", color = Color(0xFFC4C6D0).copy(alpha = 0.5f)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = uiState.currentAccent.primary) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = uiState.currentAccent.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = uiState.currentAccent.primary
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredTracks) { track ->
                TrackItem(
                    track = track,
                    isPlaying = track.id == uiState.currentTrack?.id,
                    accentColor = uiState.currentAccent.primary,
                    onClick = { onTrackSelect(track) }
                )
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun TrackItem(track: AudioTrack, isPlaying: Boolean, accentColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isPlaying) accentColor.copy(alpha = 0.1f) else Color(0xFF1C1E21))
            .border(1.dp, if (isPlaying) accentColor.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF111315)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = if (isPlaying) accentColor else Color.White.copy(alpha = 0.3f),
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                color = Color(0xFFC4C6D0),
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isPlaying) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
