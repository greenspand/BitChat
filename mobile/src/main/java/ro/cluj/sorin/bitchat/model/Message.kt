package ro.cluj.sorin.bitchat.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Message(val groupId: String, val userId: String, val userName: String?, var isSending: Boolean = false,
  val message: String, val time: Long
) : Parcelable