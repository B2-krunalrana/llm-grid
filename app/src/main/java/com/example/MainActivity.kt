package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.ChatRepository
import com.example.data.local.AppDatabase
import com.example.data.local.KeyManager
import com.example.data.network.RetrofitClient
import com.example.ui.ChatScreen
import com.example.ui.ChatViewModel
import com.example.ui.ChatViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    companion object {
        @Volatile
        private var instance: MainActivity? = null

        fun getAppContext(): Context {
            return instance!!.applicationContext
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        enableEdgeToEdge()

        // 1. Initialize local sandboxed modules
        val database = AppDatabase.getDatabase(applicationContext)
        val keyManager = KeyManager(applicationContext)
        val repository = ChatRepository(
            chatDao = database.chatDao(),
            modelDao = database.modelDao(),
            openRouterApi = RetrofitClient.openRouterApi
        )

        // 2. Instantiate View Model bound with state logic factory
        val viewModel: ChatViewModel by viewModels {
            ChatViewModelFactory(repository, keyManager)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
    }
}
