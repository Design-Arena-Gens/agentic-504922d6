package com.agentic.notepad

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "notes_store")

class NotepadApp : Application() {
    lateinit var repository: NoteRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = NoteRepository(applicationContext)
    }
}

class NoteRepository(private val context: Context) {

    private val notesKey = stringPreferencesKey("notes_json")
    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    val notes: Flow<List<Note>> = context.dataStore.data.map { prefs ->
        prefs[notesKey]?.let { stored ->
            runCatching { json.decodeFromString<List<Note>>(stored) }.getOrElse { emptyList() }
        } ?: emptyList()
    }

    suspend fun upsert(note: Note) {
        val snapshot = notes.first().toMutableList()
        val index = snapshot.indexOfFirst { it.id == note.id }
        if (index >= 0) {
            snapshot[index] = note.copy(updatedAt = System.currentTimeMillis())
        } else {
            snapshot.add(note.copy(updatedAt = System.currentTimeMillis()))
        }
        persist(snapshot)
    }

    suspend fun delete(id: String) {
        val snapshot = notes.first().filterNot { it.id == id }
        persist(snapshot)
    }

    private suspend fun persist(notes: List<Note>) {
        context.dataStore.edit { prefs ->
            prefs[notesKey] = json.encodeToString(notes)
        }
    }
}
