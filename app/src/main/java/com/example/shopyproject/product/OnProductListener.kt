package com.example.shopyproject.product

import com.example.shopyproject.entities.Product

interface OnProductListener {
    fun onClick(product: Product)
    fun onLongClick(product: Product)
}