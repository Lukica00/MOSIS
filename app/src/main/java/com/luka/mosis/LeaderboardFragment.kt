package com.luka.mosis
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import coil.load
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.luka.mosis.databinding.FragmentLeaderboardBinding


class LeaderboardFragment : Fragment() {
    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val user : User by activityViewModels()
    private lateinit var db: FirebaseFirestore
    private var users: List<DocumentSnapshot>? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        binding.leadNavView.setCheckedItem(R.id.leaderboard)
        binding.leadToolbar.setupWithNavController(findNavController(), AppBarConfiguration(setOf(R.id.leaderboardFragment),binding.leadDrawerLayout))
        db = Firebase.firestore
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db.collection("users").get().addOnSuccessListener {
            users = it.documents
            val adapter = object: ArrayAdapter<DocumentSnapshot>(requireContext(),R.layout.list_item, users!!){
                var items: List<DocumentSnapshot>
                init {
                    items = users!!.sortedBy {
                        it.data!!["score"] as Long
                    }.reversed()
                }
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    var listItem = convertView
                    if(listItem==null)
                        listItem = LayoutInflater.from(context).inflate(R.layout.list_item,parent,false)
                    listItem?.findViewById<ImageView>(R.id.icon)?.load(Uri.parse(items[position].data!!["photoUri"] as String))
                    listItem?.findViewById<TextView>(R.id.firstLine)?.text = items[position].data!!["displayName"] as String
                    listItem?.findViewById<TextView>(R.id.secondLine)?.text = (items[position].data!!["score"] as Long).toString()
                    listItem?.findViewById<TextView>(R.id.position)?.text = (position + 1).toString()
                    return listItem!!
                }
            }
            requireActivity().findViewById<ListView>(R.id.lead_linear).adapter = adapter
        }
        user.email.observe(viewLifecycleOwner){
            binding.leadNavView.getHeaderView(0).findViewById<TextView>(R.id.menu_header_bottom).text =
                it
        }
        user.displayName.observe(viewLifecycleOwner){
            binding.leadNavView.getHeaderView(0).findViewById<TextView>(R.id.menu_header_top).text = it
        }
        user.photoUri.observe(viewLifecycleOwner){
            binding.leadNavView.getHeaderView(0).findViewById<ImageView>(R.id.menu_header_image)
                .load(it)
        }
        binding.leadNavView.setNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.logout_item -> {
                    Firebase.auth.signOut()
                    user.setFirebaseUser(Firebase.auth.currentUser)
                    findNavController().navigate(R.id.action_leaderboardFragment_to_loginFragment)
                }
                R.id.explore -> {
                    findNavController().navigate(R.id.action_leaderboardFragment_to_mainFragment)
                }
                else -> {
                }
            }
            true
        }
    }
}