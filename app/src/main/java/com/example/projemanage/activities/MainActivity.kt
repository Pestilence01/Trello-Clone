package com.example.projemanage.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.projemanage.R
import com.example.projemanage.adapters.BoardItemsAdapter
import com.example.projemanage.firebase.FirestoreClass
import com.example.projemanage.models.Board
import com.example.projemanage.models.User
import com.example.projemanage.utils.Constants
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.main_content.*

class MainActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mUsername: String
    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupActionBar()
        nav_view.setNavigationItemSelectedListener(this)

        FirestoreClass().loadUserData(this, true)

        fab_create_board.setOnClickListener {
            val intent = Intent(this, CreateBoardActivity::class.java)
            intent.putExtra(Constants.NAME, mUsername)
            startActivityForResult(intent, CREATE_BOARD_REQUEST_CODE)
        }

        mSharedPreferences = this.getSharedPreferences(Constants.PROJEMANAGE_PREFERENCES, Context.MODE_PRIVATE)

        val tokenUpdated = mSharedPreferences.getBoolean(Constants.FCM_TOKEN_UPDATED, false)

        if (tokenUpdated) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().loadUserData(this@MainActivity, true)
        } else {
            FirebaseInstanceId.getInstance()
                .instanceId.addOnSuccessListener(this@MainActivity) { instanceIdResult ->
                    updateFCMToken(instanceIdResult.token)
                }
        }
    }

    fun tokenUpdateSuccess() {

        hideProgressDialog()

        val editor: SharedPreferences.Editor = mSharedPreferences.edit()
        editor.putBoolean(Constants.FCM_TOKEN_UPDATED, true)
        editor.apply()

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().loadUserData(this@MainActivity, true)
    }

    private fun updateFCMToken(token: String) {

        val userHashMap = HashMap<String, Any>()
        userHashMap[Constants.FCM_TOKEN] = token

        showProgressDialog(resources.getString(R.string.please_wait))
        FirestoreClass().updateUserProfileData(this@MainActivity, userHashMap)
    }

    fun populateBoardsListToUI(boardsList: ArrayList<Board>) {

        hideProgressDialog()

        if (boardsList.size > 0) {

            rv_boards_list.visibility = View.VISIBLE
            tv_no_boards_available.visibility = View.GONE

            rv_boards_list.layoutManager = LinearLayoutManager(this@MainActivity)
            rv_boards_list.setHasFixedSize(true)

            val adapter = BoardItemsAdapter(this@MainActivity, boardsList)
            rv_boards_list.adapter = adapter

            adapter.setOnClick(object: BoardItemsAdapter.OnClickListener{
                override fun onClick(position: Int, model: Board) {
                    val intent = Intent(this@MainActivity, TaskListActivity::class.java)
                    intent.putExtra(Constants.DOCUMENT_ID, model.documentId)
                    startActivity(intent)
                }
            })
        } else {
            rv_boards_list.visibility = View.GONE
            tv_no_boards_available.visibility = View.VISIBLE
        }
    }

    private fun setupActionBar(){
        setSupportActionBar(toolbar_main_activity)
        toolbar_main_activity.setNavigationIcon(R.drawable.ic_action_navigation_menu)

        toolbar_main_activity.setNavigationOnClickListener {
            toggleDrawer()
        }
    }

    fun updateNavigationUserDetails(user: User, readBoardsList: Boolean) {
        hideProgressDialog()
        mUsername = user.name
        val headerView = nav_view.getHeaderView(0)

        val navUserImage = headerView.findViewById<ImageView>(R.id.nav_user_image)

        Glide.with(this@MainActivity)
            .load(user.image)
            .centerCrop()
            .placeholder(R.drawable.ic_user_place_holder)
            .into(navUserImage)

        val navUsername = headerView.findViewById<TextView>(R.id.tv_username)
        navUsername.text = user.name

        if (readBoardsList) {
            showProgressDialog(resources.getString(R.string.please_wait))
            FirestoreClass().getBoardsList(this@MainActivity)
        }
    }

    private fun toggleDrawer() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            drawer_layout.openDrawer(GravityCompat.START)
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            doubleBackToExit()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == MY_PROFILE_REQUEST_CODE) {
            FirestoreClass().loadUserData(this@MainActivity)
        }else if(resultCode == Activity.RESULT_OK && requestCode == CREATE_BOARD_REQUEST_CODE){
            FirestoreClass().getBoardsList(this)
        } else {
            Log.e("Cancelled", "Cancelled")
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_my_profile -> {
                startActivityForResult(Intent(this, MyProfileActivity::class.java), MY_PROFILE_REQUEST_CODE)
            }

            R.id.nav_sign_out -> {
                FirebaseAuth.getInstance().signOut()

                mSharedPreferences.edit().clear().apply()

                val intent = Intent(this, IntroActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                finish()
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    companion object {
        const val MY_PROFILE_REQUEST_CODE: Int = 11
        const val CREATE_BOARD_REQUEST_CODE: Int = 12
    }
}