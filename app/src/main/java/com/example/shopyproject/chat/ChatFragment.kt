package com.example.shopyproject.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shopyproject.Constants
import com.example.shopyproject.R
import com.example.shopyproject.databinding.FragmentChatBinding
import com.example.shopyproject.entities.Message
import com.example.shopyproject.entities.Order
import com.example.shopyproject.order.OrderAux
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ChatFragment : Fragment() ,OnChatListener{
    private var binding : FragmentChatBinding? = null

    private lateinit var adapter: ChatAdapter
    private var order : Order? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChatBinding.inflate(layoutInflater, container, false)
        binding?.let{
            return it.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getOrder()
        setUpRecyclerView()
        setUpButtons()
    }


    private fun setUpRecyclerView() {
        adapter = ChatAdapter(mutableListOf(), this)
        binding?.let{
            it.recyclerView.apply {
                layoutManager = LinearLayoutManager(context).also {
                    it.stackFromEnd = true
                }
                adapter = this@ChatFragment.adapter
            }
        }

    }

    private fun setUpButtons() {
        binding?.let{ binding ->
            binding.ibSend.setOnClickListener{
                sendMessage()
            }
        }
    }

    private fun sendMessage() {

        order?.let {
            val database = Firebase.database
            val chatRef = database.getReference(Constants.PATH_CHAT)
                .child(it.id)
            val user = FirebaseAuth.getInstance().currentUser
            user?.let{
                val message = Message(message = binding?.etMessage?.text.toString().trim(),
                    sender = it.uid)

                binding?.ibSend?.isEnabled = false

                chatRef.push().setValue(message)
                    .addOnSuccessListener {
                        binding?.etMessage?.setText("")
                    }
                    .addOnCompleteListener {
                        binding?.ibSend?.isEnabled = true
                    }
            }
        }

    }


    private fun getOrder(){
        order = (activity as? OrderAux)?.getOrderSelecter()
        order?.let{
            setUpActionBar()
            setUpRealtimeDatabase()
        }
    }

    private fun setUpRealtimeDatabase() {
        order?.let{
            val database = Firebase.database
            val chatRef = database.getReference(Constants.PATH_CHAT).child(it.id)
            val childListener = object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    getMessage(snapshot)?.let{
                        adapter.add(it)
                        binding?.recyclerView?.scrollToPosition(adapter.itemCount - 1)
                    }

                }

                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    getMessage(snapshot)?.let{
                        adapter.update(it)
                    }
                }

                override fun onChildRemoved(snapshot: DataSnapshot) {
                    getMessage(snapshot)?.let{
                        adapter.delete(it)
                    }
                }

                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

                override fun onCancelled(error: DatabaseError) {
                    binding?.let{
                        Snackbar.make(it.root, "Error al cargar Chat", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }

            chatRef.addChildEventListener(childListener)
        }
    }

    private fun getMessage(snapshot: DataSnapshot) : Message?{
        snapshot.getValue(Message::class.java)?.let { message ->
            snapshot.key?.let{
                message.id = it
            }

            FirebaseAuth.getInstance().currentUser?.let{ id ->
                message.myUid = id.uid
            }
            return message
        }

        return null
    }

    private fun setUpActionBar() {
        (activity as? AppCompatActivity)?.let{
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.title = getString(R.string.chat_title)
            setHasOptionsMenu(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.let{
            it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            it.supportActionBar?.title = getString(R.string.order_title)
            setHasOptionsMenu(false)
        }
        binding = null
    }

    override fun deleteMessage(message: Message) {
        order?.let{
            val database = Firebase.database
            val messageRef = database.getReference(Constants.PATH_CHAT).child(it.id).child(message.id)
            messageRef.removeValue { error, ref ->
                binding?.let{ binding ->
                    if (error != null){
                        Snackbar.make(binding.root, "Error al eliminar mensaje", Snackbar.LENGTH_SHORT).show()
                    }else{
                        Snackbar.make(binding.root, "Mensaje eliminado", Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }
}