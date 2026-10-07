package com.freshsave.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.freshsave.app.data.model.UserRole
import com.freshsave.app.data.repository.Session
import com.freshsave.app.data.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// `by preferencesDataStore` creates one DataStore file ("session.preferences_pb") per app.
private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** The only real persistence in the app for now: survives restarts so you stay logged in. */
class DataStoreSessionRepository(context: Context) : SessionRepository {
    private val store = context.applicationContext.sessionStore

    private object Keys {
        val onboardingSeen = booleanPreferencesKey("onboarding_seen")
        val loggedIn = booleanPreferencesKey("logged_in")
        val role = stringPreferencesKey("role")
    }

    override val session: Flow<Session> = store.data.map { prefs ->
        Session(
            onboardingSeen = prefs[Keys.onboardingSeen] ?: false,
            loggedIn = prefs[Keys.loggedIn] ?: false,
            role = prefs[Keys.role]?.let { runCatching { UserRole.valueOf(it) }.getOrNull() },
        )
    }

    override suspend fun setOnboardingSeen() {
        store.edit { it[Keys.onboardingSeen] = true }
    }

    override suspend fun logIn() {
        store.edit {
            it[Keys.onboardingSeen] = true
            it[Keys.loggedIn] = true
        }
    }

    override suspend fun setRole(role: UserRole) {
        store.edit { it[Keys.role] = role.name }
    }

    override suspend fun logOut() {
        store.edit {
            it[Keys.loggedIn] = false
            it.remove(Keys.role)
        }
    }
}
