package com.luka.mosis

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.luka.mosis.databinding.FragmentAddObjectBinding
import com.luka.mosis.databinding.FragmentMainBinding

class AddObject : Fragment() {
    private var _binding: FragmentAddObjectBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddObjectBinding.inflate(inflater,container,false)
        binding.toolbar.setupWithNavController(findNavController())
        return binding.root
    }
}