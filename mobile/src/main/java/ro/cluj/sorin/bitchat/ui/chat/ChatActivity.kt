package ro.cluj.sorin.bitchat.ui.chat

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.sorinirimies.kotlinx.animateGone
import com.sorinirimies.kotlinx.animateVisible
import com.sorinirimies.kotlinx.snack
import com.sorinirimies.kotlinx.visible
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import ro.cluj.sorin.bitchat.R
import ro.cluj.sorin.bitchat.model.ChatGroup
import ro.cluj.sorin.bitchat.model.ChatMessage
import ro.cluj.sorin.bitchat.model.State
import ro.cluj.sorin.bitchat.model.User
import ro.cluj.sorin.bitchat.ui.BaseActivity
import ro.cluj.sorin.bitchat.ui.nearby.NEARBY_SERVICE_ID
import ro.cluj.sorin.bitchat.utils.toBitChatUser
import java.util.*

internal const val PARAM_CHAT_GROUP = " ro.cluj.sorin.bitchat.CHAT_GROUP"

/**
 * Created by sorin on 12.05.18.
 */
class ChatActivity : BaseActivity(), ChatView {

    private val presenter: ChatPresenter by inject()
    private val channelFirebaseUser by lazy { BroadcastChannel<FirebaseUser>(1) }
    private val messageRef by lazy { db.collection("message") }
    private lateinit var conversationAdapter: ConversationAdapter
    private var user: User? = null
    private var group: ChatGroup? = null
    private var isNearbyChatEnabled = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        presenter.attachView(this)
        group = intent.getParcelableExtra(PARAM_CHAT_GROUP)
        group?.let { tvGroupNameTitle.text = "${getString(R.string.group_title_label)} ${it.name}" }
        rvConversation.apply {
            layoutManager = LinearLayoutManager(context)
            conversationAdapter = ConversationAdapter()
            adapter = conversationAdapter
        }
        GlobalScope.launch {
            channelFirebaseUser.openSubscription().consumeEach {
                user = it.toBitChatUser()
            }
        }
        firebaseAuth.addAuthStateListener(authStateListener)

        btnSend.setOnClickListener {
            user?.apply {
                group?.let { group ->
                    if (TextUtils.isEmpty(edtChatMsg.text.toString())) return@let
                    val msg = ChatMessage(UUID.randomUUID().toString(),
                            group.id,
                            id,
                            name,
                            true,
                            edtChatMsg.text.toString(),
                            Calendar.getInstance().timeInMillis)
                    if (isNearbyChatEnabled) {
                        presenter.sendMsg(msg)
                        presenter.addMessage(msg)
                    } else {
                        messageRef.document(msg.messageId).set(msg)
                    }
                    edtChatMsg.setText("")
                }
            }
        }
        if (group?.id != NEARBY_SERVICE_ID) {
            registerFirebaseChatListener()
            isNearbyChatEnabled = false
        } else {
            isNearbyChatEnabled = true
            tvConnectionStatus.visible()
            presenter.setState(State.SEARCHING)
            progressLoadChat.animateVisible()
            tvLookingForFriendsLabel.animateVisible()
        }
    }

    private fun registerFirebaseChatListener() = messageRef.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
        if (firebaseFirestoreException != null) return@addSnapshotListener
        querySnapshot?.forEach {
            val data = it.data
            val groupId = data["groupId"].toString()
            if (groupId == group?.id) {
                val isSending = data["userId"].toString() == user?.id
                val msg = ChatMessage(data["messageId"].toString(),
                        data["groupId"].toString(),
                        data["userId"].toString(),
                        data["userName"].toString(),
                        isSending,
                        data["message"].toString(),
                        data["time"].toString().toLong())
                presenter.addMessage(msg)
            }
        }
    }

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            presenter.userIsLoggedIn(user)
        } else {
            presenter.userIsLoggedOut()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
        registerFirebaseChatListener().remove()
        channelFirebaseUser.close()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    override fun showUserIsLoggedIn(user: FirebaseUser) {
        GlobalScope.launch {
            channelFirebaseUser.send(user)
        }
    }

    override fun showUserIsLoggedOut() {
        snack(contChatActivity, getString(R.string.user_is_logggedout))
    }

    override fun showNearbyChatConnected() {
        tvConnectionStatus.apply {
            text = getString(R.string.connected)
            setTextColor(getColor(R.color.colorConnected))
        }
        progressLoadChat.animateGone()
        tvLookingForFriendsLabel.animateGone()
    }

    override fun showNearbyChatDisconnected() {
        tvConnectionStatus.apply {
            text = getString(R.string.disconnected)
            setTextColor(getColor(R.color.colorDisconnected))
            snack(contChatActivity, getString(R.string.disconnected))
        }
        progressLoadChat.animateVisible()
        tvLookingForFriendsLabel.animateVisible()
    }

    override fun showAddedMessage(chatMessage: ChatMessage) {
        conversationAdapter.addItem(chatMessage)
    }
}
