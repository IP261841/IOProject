package com.example.dlmproject

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ProfileActivity : AppCompatActivity() {

    val db = Firebase.firestore

    private lateinit var userName: EditText
    private lateinit var userSurname: EditText
    private lateinit var userAge: NumberPicker
    private lateinit var userGender: Spinner
    private lateinit var saveData: Button
    private lateinit var switchProfile: ImageView


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var formattedDateTime: String = ""

        userName = findViewById(R.id.editTextProfileName)
        userSurname = findViewById(R.id.editTextProfileSurname)
        userAge = findViewById(R.id.ProfileAgePicker)
        userGender = findViewById(R.id.spinnerProfileGender)
        saveData = findViewById(R.id.buttonSave)
        switchProfile = findViewById(R.id.switchProfile)

        userAge.minValue = 16
        userAge.maxValue = 100
        userAge.value = 25

        switchProfile.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        saveData.setOnClickListener{

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            val email = FirebaseAuth.getInstance().currentUser?.email.toString()

            val dataName = userName.text.toString()
            val dataSurname = userSurname.text.toString()
            val dataAge = userAge.value
            val dataGender = userGender.selectedItem.toString()

            val listOfData = listOf(dataName, dataSurname, dataAge.toString(), dataGender)

            val currentDate = LocalDateTime.now()

            val firebaseData = FireStoreData(email, listOfData)

            formattedDateTime = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            // dodanie danych do bazy Firestore
            GlobalScope.launch(Dispatchers.IO) {
                userId?.let {
                    db.collection(FirebaseAuth.getInstance().currentUser?.email.toString())
                        .document(formattedDateTime)
                        .set(firebaseData).await()
                }
            }


        }
    }
}