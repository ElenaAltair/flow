package ru.netology.nmedia.activity

import android.Manifest
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.navigation.findNavController
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import ru.netology.nmedia.R
import ru.netology.nmedia.activity.NewPostFragment.Companion.textArg
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.viewmodel.AuthViewModel

class AppActivity : AppCompatActivity(R.layout.activity_app) {

    var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationsPermission()

        intent?.let {
            if (it.action != Intent.ACTION_SEND) {
                return@let
            }

            val text = it.getStringExtra(Intent.EXTRA_TEXT)
            if (text?.isNotBlank() != true) {
                return@let
            }

            intent.removeExtra(Intent.EXTRA_TEXT)
            findNavController(R.id.nav_host_fragment)
                .navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply {
                        textArg = text
                    }
                )
        }

        val viewModel by viewModels<AuthViewModel>()

        addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_auth, menu) // инициализируем меню menu_auth

                    // подпишемся на изменения данных авторизации
                    viewModel.data.observe(this@AppActivity) {
                        menu.setGroupVisible(R.id.authorized, viewModel.isAuthorized)
                        menu.setGroupVisible(R.id.unauthorized, !viewModel.isAuthorized)
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                    when (menuItem.itemId) {

                        R.id.signIn -> {
                            findNavController(R.id.nav_host_fragment) //
                                .navigate(
                                    R.id.action_feedFragment_to_signInFragment
                                )
                            true
                        }

                        R.id.signUp -> {

                            findNavController(R.id.nav_host_fragment) //
                                .navigate(
                                    R.id.action_feedFragment_to_signUpFragment
                                )
                            true
                        }

                        R.id.logout -> {
                            createDialogSignOut()
                            alertDialog?.show()
                            true
                        }

                        else -> false
                    }
            }
        )

        checkGoogleApiAvailability()
    }

    private fun requestNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }

        val permission = Manifest.permission.POST_NOTIFICATIONS

        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
            return
        }

        requestPermissions(arrayOf(permission), 1)
    }

    private fun checkGoogleApiAvailability() {
        with(GoogleApiAvailability.getInstance()) {
            val code = isGooglePlayServicesAvailable(this@AppActivity)
            if (code == ConnectionResult.SUCCESS) {
                return@with
            }
            if (isUserResolvableError(code)) {
                getErrorDialog(this@AppActivity, code, 9000)?.show()
                return
            }
            Toast.makeText(this@AppActivity, R.string.google_play_unavailable, Toast.LENGTH_LONG)
                .show()
        }

        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            println(it)
        }
    }

    fun createDialogSignOut() {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Sign out")
        alertDialogBuilder.setMessage("Sign out?")
        alertDialogBuilder.setPositiveButton("Yes") { _: DialogInterface, _: Int ->
            AppAuth.getInstance().clear()
        }
        alertDialogBuilder.setNegativeButton(
            "Cancel",
            { dialogInterface: DialogInterface, i: Int -> })
        alertDialog = alertDialogBuilder.create()
    }
}