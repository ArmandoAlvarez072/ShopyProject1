package com.example.shopyproject.chat

import com.example.shopyproject.entities.Message

interface OnChatListener {
    fun deleteMessage(message: Message)
}