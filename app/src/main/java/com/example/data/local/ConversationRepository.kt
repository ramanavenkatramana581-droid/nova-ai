package com.example.data.local

import kotlinx.coroutines.flow.Flow

class ConversationRepository(private val conversationDao: ConversationDao) {
    fun getMessagesForSession(sessionId: String): Flow<List<ConversationEntity>> =
        conversationDao.getMessagesForSession(sessionId)

    fun getAllMessages(): Flow<List<ConversationEntity>> =
        conversationDao.getAllMessages()

    fun getSessions(): Flow<List<String>> =
        conversationDao.getDistinctSessions()

    suspend fun addMessage(message: ConversationEntity): Long =
        conversationDao.insertMessage(message)

    suspend fun deleteSession(sessionId: String) =
        conversationDao.deleteSession(sessionId)

    suspend fun clearAll() =
        conversationDao.clearAllMessages()

    suspend fun getCount(): Int =
        conversationDao.getMessageCount()
}
