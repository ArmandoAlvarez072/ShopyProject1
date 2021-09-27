package com.example.shopyproject.order

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopyproject.Constants
import com.example.shopyproject.R
import com.example.shopyproject.chat.ChatFragment
import com.example.shopyproject.databinding.ActivityOrderBinding
import com.example.shopyproject.entities.Order
import com.example.shopyproject.fcm.NotificationRS
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase

class OrderActivity : AppCompatActivity() ,OnOrderListener ,OrderAux{

    private lateinit var binding : ActivityOrderBinding
    private lateinit var adapter: OrderAdapter
    private lateinit var orderSelected : Order

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val arrayValues : Array<String> by lazy {
        resources.getStringArray(R.array.status_value)
    }
    private val arrayKeys : Array<Int> by lazy {
        resources.getIntArray(R.array.status_key).toTypedArray()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpRecyclerView()
        setUpFirestore()
        configAnalytics()
    }

    private fun setUpRecyclerView() {
        adapter = OrderAdapter(mutableListOf(), this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@OrderActivity)
            adapter = this@OrderActivity.adapter
        }
    }

    private fun setUpFirestore(){
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_REQUEST)
            .get()
            .addOnSuccessListener {
                for (document in it){
                    val order = document.toObject(Order::class.java)
                    order.id = document.id
                    adapter.add(order)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al consultar datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun configAnalytics() {
        firebaseAnalytics = Firebase.analytics
    }

    private fun notifyClient(order: Order){
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_USERS)
            .document(order.clientId)
            .collection(Constants.COLL_TOKENS)
            .get()
            .addOnSuccessListener {
                var tokensStr = ""
                for (document in it){
                    val tokenMap = document.data
                    tokensStr += "${tokenMap.getValue(Constants.PROP_TOKEN)}, "
                }
                if (tokensStr.length > 0) {
                    tokensStr = tokensStr.dropLast(1)

                    var names = ""
                    order.products.forEach {
                        names += "${it.value.name}, "
                    }
                    names = names.dropLast(2)

                    val index = arrayKeys.indexOf(order.status)

                    val notificationRS = NotificationRS()
                    notificationRS.sendNotification(
                        "Tu pedido ha sido ${arrayValues[index]}",
                        names,
                        tokensStr
                    )

                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al enviar mensaje", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onStartChat(order: Order) {
        orderSelected = order

        val fragment = ChatFragment()

        supportFragmentManager
            .beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onStatusChanged(order: Order) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_REQUEST)
            .document(order.id)
            .update(Constants.PROP_STATUS, order.status)
            .addOnSuccessListener {
                Toast.makeText(this, "Orden Actualizada", Toast.LENGTH_SHORT).show()
                notifyClient(order)

                //Analytics
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_SHIPPING_INFO){
                    val product = mutableListOf<Bundle>()
                    order.products.forEach{
                        val bundle = Bundle()
                        bundle.putString("id_prodcut", it.key)
                        product.add(bundle)
                    }
                    param(FirebaseAnalytics.Param.SHIPPING, product.toTypedArray())
                    param(FirebaseAnalytics.Param.PRICE, order.totalPrice)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getOrderSelecter(): Order = orderSelected
}