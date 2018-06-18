package ro.cluj.sorin.bitchat.ui.groups

import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.EditText
import com.greenspand.kotlin_ext.alertDialog
import com.greenspand.kotlin_ext.init
import com.greenspand.kotlin_ext.snack
import kotlinx.android.synthetic.main.fragment_groups.contGroupsFragment
import kotlinx.android.synthetic.main.fragment_groups.fabCreateGroup
import kotlinx.android.synthetic.main.fragment_groups.rvGroups
import org.kodein.di.KodeinAware
import org.kodein.di.android.closestKodein
import org.kodein.di.generic.instance
import ro.cluj.sorin.bitchat.R
import ro.cluj.sorin.bitchat.model.ChatGroup
import ro.cluj.sorin.bitchat.ui.BaseFragment
import ro.cluj.sorin.bitchat.ui.chat.ChatActivity
import ro.cluj.sorin.bitchat.ui.chat.PARAM_CHAT_GROUP

class GroupsFragment : BaseFragment(), KodeinAware, GroupsView {
  override val kodein by closestKodein()
  private val presenter: GroupsPresenter by instance()
  private lateinit var groupsAdapter: GroupsAdapter
  override fun getLayoutId() = R.layout.fragment_groups
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    presenter.attachView(this)
    fabCreateGroup.setOnClickListener {
      showCreateGroupDialog()
    }
    rvGroups.apply {
      layoutManager = LinearLayoutManager(activity)
      groupsAdapter = GroupsAdapter({
        startActivity(Intent(activity, ChatActivity::class.java).apply {
          putExtra(PARAM_CHAT_GROUP, it)
        })
      }, {
        showDeleteGroupDialog(it)
      })
      adapter = groupsAdapter
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    presenter.detachView()
  }

  private fun showCreateGroupDialog() = context?.let {
    alertDialog(it) {
      val v = layoutInflater.init(R.layout.dialog_create_group)
      setView(v)
      setTitle(R.string.create_group_label)
      setMessage("${getString(R.string.create_group_message)} ${v.findViewById<EditText>(R.id.edtGroupName).text}")
      setPositiveButton(R.string.create_group) { _, _ ->
        val groupName = v.findViewById<EditText>(R.id.edtGroupName).text.toString()
        val group = ChatGroup(groupName)
        presenter.createChatGroup(group)
      }
    }
  }

  private fun showDeleteGroupDialog(group: ChatGroup) = context?.let {
    alertDialog(it) {
      setTitle(R.string.delete_group_label)
      setMessage("${getString(R.string.delete_group_label)} $group ?")
      setPositiveButton(R.string.create_group) { _, _ ->
        presenter.deleteChatGroup(group)
      }
      setNegativeButton(R.string.cancel) { _, _ -> }
    }
  }

  override fun showChatGroupCreated(group: ChatGroup) {
    snack(contGroupsFragment, "${group.name} ${getString(R.string.group_created_confirmation)}")
    groupsAdapter.addItem(group)
  }

  override fun showChatGroupDeleted(group: ChatGroup) {
    snack(contGroupsFragment, "${group.name} ${getString(R.string.group_deleted_confirmation)}")
    groupsAdapter.removeItem(group)
  }
}
