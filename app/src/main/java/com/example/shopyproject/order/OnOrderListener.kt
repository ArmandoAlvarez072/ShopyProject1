package com.example.shopyproject.order

import com.example.shopyproject.entities.Order

interface OnOrderListener {
    fun onStartChat(order : Order)
    fun onStatusChanged(order: Order)
}