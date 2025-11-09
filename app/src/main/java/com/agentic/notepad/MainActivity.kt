package com.agentic.notepad

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agentic.notepad.ui.theme.AgenticNotepadTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NotepadViewModel by viewModels {
        val app = application as NotepadApp
        NotepadViewModelFactory(app.repository)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgenticNotepadTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = "Agentic Notepad") },
                            actions = {
                                if (state.activeNote != null) {
                                    IconButton(onClick = { viewModel.deleteNote(state.activeNote.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete note"
                                        )
                                    }
                                }
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(onClick = viewModel::createNote) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "New note")
                        }
                    }
                ) { innerPadding ->
                    NotepadContent(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        state = state,
                        onSelectNote = viewModel::selectNote,
                        onSearch = viewModel::onSearch,
                        onUpdate = viewModel::updateNote
                    )
                }
            }
        }
    }
}

@Composable
fun NotepadContent(
    modifier: Modifier = Modifier,
    state: NotepadUiState,
    onSelectNote: (String?) -> Unit,
    onSearch: (String) -> Unit,
    onUpdate: (Note) -> Unit
) {
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = modifier) {
        if (maxWidth < 600.dp) {
            Column(modifier = Modifier.fillMaxSize()) {
                NotesSidebar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f),
                    state = state,
                    onSelectNote = onSelectNote,
                    onSearch = onSearch
                )
                Divider(modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp))
                NoteEditor(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    note = state.activeNote,
                    onUpdate = onUpdate
                )
            }
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                NotesSidebar(
                    modifier = Modifier
                        .width(280.dp)
                        .fillMaxHeight(),
                    state = state,
                    onSelectNote = onSelectNote,
                    onSearch = onSearch
                )
                Divider(modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight())
                NoteEditor(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    note = state.activeNote,
                    onUpdate = onUpdate
                )
            }
        }
    }
}

@Composable
fun NotesSidebar(
    modifier: Modifier,
    state: NotepadUiState,
    onSelectNote: (String?) -> Unit,
    onSearch: (String) -> Unit
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 16.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.Top
    ) {
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            value = state.searchQuery,
            onValueChange = onSearch,
            label = { Text("Search") },
            singleLine = true
        )
        if (state.filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(text = "No notes yet. Tap + to create one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.filteredNotes, key = { it.id }) { note ->
                    NoteListItem(
                        note = note,
                        isActive = state.activeNote?.id == note.id,
                        onClick = { onSelectNote(note.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun NoteListItem(
    note: Note,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        tonalElevation = if (isActive) 6.dp else 0.dp,
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = if (note.title.isBlank()) "Untitled" else note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = note.content.ifBlank { "No content yet" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NoteEditor(
    modifier: Modifier,
    note: Note?,
    onUpdate: (Note) -> Unit
) {
    if (note == null) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Create a note to get started")
        }
        return
    }

    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }

    LaunchedEffect(note.id, note.updatedAt) {
        title = note.title
        content = note.content
    }

    Column(
        modifier = modifier
            .padding(20.dp),
        verticalArrangement = Arrangement.Top
    ) {
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = title,
            onValueChange = {
                title = it
                onUpdate(note.copy(title = it))
            },
            textStyle = MaterialTheme.typography.headlineSmall,
            singleLine = true,
            placeholder = { Text("Title") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            value = content,
            onValueChange = {
                content = it
                onUpdate(note.copy(content = it))
            },
            placeholder = { Text("Start typing your notes...") }
        )
    }
}

@Preview(widthDp = 800, heightDp = 600)
@Composable
fun NotepadPreview() {
    AgenticNotepadTheme {
        val notes = listOf(
            Note(title = "Sample note", content = "This is a preview note.")
        )
        NotepadContent(
            state = NotepadUiState(
                notes = notes,
                filteredNotes = notes,
                activeNote = notes.first()
            ),
            onSelectNote = {},
            onSearch = {},
            onUpdate = {}
        )
    }
}
++ End Patch
