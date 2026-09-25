package com.example.edu_ai.ui.screens.student

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.OfflineBolt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.edu_ai.data.local.BookmarkEntity
import com.example.edu_ai.data.local.UserEntity
import com.example.edu_ai.utils.TactileFeedback
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

data class Peer(
    val id: String,
    val name: String,
    val role: String,
    val status: String,
    val isOnline: Boolean
)

data class PeerMsg(
    val id: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val sharedBookmark: BookmarkEntity? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    user: UserEntity,
    viewModel: StudentViewModel,
    onNavigateToLearn: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Dynamic lists of active connected peers
    val peers = remember {
        mutableStateListOf(
            Peer("2", "Dr. Neema Ongaga", "Faculty Mentor", "Online", true),
            Peer("102", "Grace Naliaka", "Clinical Peer", "Study Mode", true),
            Peer("103", "Rayvins Otieno", "Pre-med Peer", "In Assessment", false)
        )
    }

    // Unconnected Searchable Catalog
    val userCatalog = remember {
        listOf(
            Peer("104", "Julius Gachoki", "Cardiology Peer", "Reviewing Biochem", true),
            Peer("105", "Mercy Wanjiku", "Research Assistant", "Online", true),
            Peer("106", "Kevin Kiprop", "Anatomy Instructor", "Consultation", true),
            Peer("107", "John Kamau", "Paediatric Resident", "Offline", false)
        )
    }

    var selectedPeer by remember { mutableStateOf(peers[0]) }
    var inputText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showSearchResults by remember { mutableStateOf(false) }
    
    // In-memory chat storage for peer connection
    val chatHistory = remember { mutableStateMapOf<String, List<PeerMsg>>() }
    
    // Seed initial greetings
    LaunchedEffect(selectedPeer.id) {
        if (!chatHistory.containsKey(selectedPeer.id)) {
            chatHistory[selectedPeer.id] = listOf(
                PeerMsg(
                    id = "init-1",
                    senderId = selectedPeer.id,
                    senderName = selectedPeer.name,
                    content = "Hello ${user.username}! Let me know if you want to collaborate on any medical topics or share bookmarks today.",
                    timestamp = System.currentTimeMillis() - 600000
                )
            )
        }
    }

    val currentMessages = chatHistory[selectedPeer.id] ?: emptyList()
    val localBookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())
    var showShareBookmarkDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        // Upper Peer/Mentors Horizon Selection
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Study Connect & Mentors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // FIND OTHER USERS Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                showSearchResults = it.isNotBlank()
            },
            placeholder = { Text("Find other users by name...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                focusedBorderColor = MaterialTheme.colorScheme.primary
            )
        )

        // Dropdown Search Results Overlay
        if (showSearchResults) {
            val filteredCatalog = userCatalog.filter { 
                it.name.contains(searchQuery, ignoreCase = true) 
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(max = 180.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                if (filteredCatalog.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No matching peers found.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn {
                        items(filteredCatalog) { peer ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        TactileFeedback.triggerSubtleClick(context)
                                        // If peer is not in dynamic active lists, append them
                                        if (peers.none { it.id == peer.id }) {
                                            peers.add(peer)
                                        }
                                        selectedPeer = peer
                                        searchQuery = ""
                                        showSearchResults = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(peer.name.split(" ").last().take(1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(peer.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(peer.role, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Add, contentDescription = "Add & Chat", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }
        
        // Peer selector list (horizontal)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            peers.forEach { peer ->
                val isSelected = peer.id == selectedPeer.id
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            TactileFeedback.triggerSubtleClick(context)
                            selectedPeer = peer
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ),
                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (peer.isOnline) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    peer.name.split(" ").last().take(1),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (peer.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            peer.name.split(" ").last(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            peer.status,
                            fontSize = 8.sp,
                            color = if (peer.isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }

        // Direct Messaging Section
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Active Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(selectedPeer.name.take(1), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                             }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(selectedPeer.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(selectedPeer.role, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("STUDY NETWORK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Chat Messages Scroll list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(currentMessages) { m ->
                        val isMe = m.senderId == user.id
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isMe) 16.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 16.dp
                                ),
                                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (m.sharedBookmark != null) {
                                        // Display beautifully shared bookmark card
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                                            ),
                                            modifier = Modifier.clickable {
                                                TactileFeedback.triggerSubtleClick(context)
                                                val subId = m.sharedBookmark.target.toLongOrNull()
                                                if (subId != null) {
                                                    onNavigateToLearn(subId)
                                                }
                                            }
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Bookmark, contentDescription = null, size20Modifier(), tint = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Shared Highlight", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(m.sharedBookmark.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                                Text(m.sharedBookmark.context, fontSize = 10.sp, maxLines = 2, color = MaterialTheme.colorScheme.outline)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("TAP TO LEARN 📚", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                    
                                    Text(m.content, fontSize = 12.sp, lineHeight = 16.sp)
                                    
                                    Text(
                                        text = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(m.timestamp)),
                                        fontSize = 8.sp,
                                        color = if (isMe) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Chat Input Controllers
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            TactileFeedback.triggerSubtleClick(context)
                            showShareBookmarkDialog = true
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share Highlight", tint = MaterialTheme.colorScheme.primary)
                        }
                        
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Collaborate on curriculum...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 2,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    TactileFeedback.triggerSubtleClick(context)
                                    val newMsg = PeerMsg(
                                        id = "msg-${System.currentTimeMillis()}",
                                        senderId = user.id,
                                        senderName = user.username,
                                        content = inputText,
                                        timestamp = System.currentTimeMillis()
                                    )
                                    chatHistory[selectedPeer.id] = (chatHistory[selectedPeer.id] ?: emptyList()) + newMsg
                                    inputText = ""
                                }
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }

    // Share Bookmarks Dialog Selection
    if (showShareBookmarkDialog) {
        AlertDialog(
            onDismissRequest = { showShareBookmarkDialog = false },
            title = { Text("Select Bookmark to Share") },
            text = {
                Column {
                    Text("Select one of your saved highlights/bookmarks to share directly with ${selectedPeer.name}.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(10.dp))
                    if (localBookmarks.isEmpty()) {
                        Text("You don't have any bookmarks to share yet. Save some highlights first!", textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 240.dp)
                        ) {
                            items(localBookmarks) { b ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            TactileFeedback.triggerSubtleClick(context)
                                            val sharedMsg = PeerMsg(
                                                id = "msg-bm-${System.currentTimeMillis()}",
                                                senderId = user.id,
                                                senderName = user.username,
                                                content = "I shared a medical highlight with you: '${b.title}'",
                                                timestamp = System.currentTimeMillis(),
                                                sharedBookmark = b
                                            )
                                            chatHistory[selectedPeer.id] = (chatHistory[selectedPeer.id] ?: emptyList()) + sharedMsg
                                            showShareBookmarkDialog = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text(b.title, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        Text(b.context, fontSize = 9.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showShareBookmarkDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

private fun size20Modifier(): Modifier = Modifier.size(20.dp)
