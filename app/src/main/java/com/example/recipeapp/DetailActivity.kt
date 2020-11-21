package com.example.recipeapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.Image
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_detail.*
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

class DetailActivity : AppCompatActivity() {
    private val db  = FirebaseDatabase.getInstance().reference
    lateinit var storageRef: StorageReference

    private val database = FirebaseDatabase.getInstance().reference.child("recipe")
    val REQUEST_CODE = 200
    private var SHARE_PREFS = "SharedPrefs"

    lateinit var displayName: EditText
    lateinit var displayIngredient: EditText
    lateinit var displayStep: EditText
    lateinit var displayCategory: Spinner
    lateinit var displayImage: ImageView
    lateinit var enableUpdate: Button
    lateinit var UpdateButton: Button
    lateinit var loadGif: ProgressBar


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        storageRef =  FirebaseStorage.getInstance().reference

        displayName = findViewById(R.id.editName)
        displayIngredient = findViewById(R.id.editIngredient)
        displayStep = findViewById(R.id.editStep)
        displayCategory = findViewById(R.id.editCategory)
        displayImage = findViewById(R.id.editImage)
        enableUpdate = findViewById(R.id.enableEdit)
        UpdateButton = findViewById(R.id.Update)
        loadGif = findViewById(R.id.loadProgress)

        displayImage.setOnClickListener {
            if (askForPermissions()) {
                capturePhoto()
            }else{
                Toast.makeText(baseContext, "false", Toast.LENGTH_LONG).show()
            }
        }

        // default disabled
        displayCategory.isEnabled = false
        displayImage.isEnabled = false

        var recipes: List<RecipeData>? = null
        try {
            val parser = XmlPullParserHandler()
            val istream = assets.open("recipetypes.xml")
            recipes = parser.parse(istream)

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, recipes)

            adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item)

            displayCategory.adapter = adapter

            displayCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(p0: AdapterView<*>?) {
                }

                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                    if (recipes[position].toString() == "Category"){
                    }else {
                    }
                    //result.text = getLStorage("selected")
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }

        enableUpdate.setOnClickListener{
            displayName.isEnabled = true
            displayIngredient.isEnabled = true
            displayStep.isEnabled = true
            displayCategory.isEnabled = true
            displayImage.isEnabled = true
            enableUpdate.isVisible = false
            UpdateButton.isVisible = true
        }

        UpdateButton.setOnClickListener{
            displayName.isEnabled = false
            displayIngredient.isEnabled = false
            displayStep.isEnabled = false
            displayCategory.isEnabled = false
            displayImage.isEnabled = false
            UpdateButton.isVisible = false
            enableUpdate.isVisible = true
            if (validateEdit()){
                loadGif.isVisible = true
                val getType = intent.getStringExtra("Type").toString()
                val getId = intent.getStringExtra("ID").toString()
                if (getType != displayCategory.selectedItem.toString()){
                    db.child("recipe").child(getType).child(getId).removeValue()
                    Toast.makeText(baseContext, "remove $getType $getId", Toast.LENGTH_LONG).show()
                }
                uploadImage(displayName.text.toString())

            }
        }


        getSingleRecipe(recipes)
    }

    data class newRecipe(
        var RecipeName: String? = "",
        var Ingredients: String? = "",
        var Step: String? = "",
        var Image: String? = ""
    )

    private fun writeRecipe(URl: String){
        val name = displayName.text.toString().trim()
        val ingredient = displayIngredient.text.toString().trim()
        val step =  displayStep.text.toString().trim()
        val type = displayCategory.selectedItem.toString()

        val nRecipe = newRecipe(name, ingredient, step, URl)
        val id = intent.getStringExtra("ID")


        if (id != null) {
            loadGif.isVisible = false
            db.child("recipe").child(type).child(id).setValue(nRecipe)
            Toast.makeText(baseContext, "Update Successfully", Toast.LENGTH_LONG).show()
            reload()
        }else{
            loadGif.isVisible = false

            Toast.makeText(baseContext, "ID not found", Toast.LENGTH_LONG).show()
        }
    }

    private fun uploadImage(picName: String) {
        var downloadurl = ""
        val bitmap = (editImage.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        var uploadTask = storageRef.child(picName).putBytes(data)

        uploadTask.addOnFailureListener {
            loadGif.isVisible = false
            Toast.makeText(baseContext, it.toString(), Toast.LENGTH_LONG).show()
        }.addOnSuccessListener { task ->

            val du=task.uploadSessionUri.toString()
            val du3:String="&alt=media"
            val du1=du!!.substring(0,du!!.indexOf("&uploadType"))
            downloadurl = du1+du3
            writeRecipe(downloadurl)
        }

    }

    private fun reload(){
        finish()
        val intent = Intent(baseContext, MainActivity::class.java)
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private  fun  validateEdit():Boolean {
        val name = displayName.text.toString().trim()
        val ingredient = displayIngredient.text.toString().trim()
        val step =  displayStep.text.toString().trim()
        val type = displayCategory.selectedItem.toString()

        if (name.isEmpty()) {
            displayName.error = "Please enter recipe name"
            displayName.requestFocus()
            return false
        }

        if (ingredient.isEmpty()) {
            displayIngredient.error = "Please enter ingredients"
            displayIngredient.requestFocus()
            return false
        }
        if (step.isEmpty()) {
            displayStep.error =  "Please enter steps"
            displayStep.requestFocus()
            return false
        }
        if (type == "Category") {
            Toast.makeText(baseContext, "Please specify the recipe category", Toast.LENGTH_LONG).show()
            displayCategory.isFocusable = true

            displayCategory.isFocusableInTouchMode = true
            displayCategory.requestFocus()
            return false
        }

        return true
    }

    private fun capturePhoto() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE && data != null) {
            displayImage.setImageBitmap(data.extras!!.get("data") as Bitmap)

            displayImage.layoutParams.width = 600
            displayImage.layoutParams.height = 600

        }
    }

    private fun getSingleRecipe(list:List<RecipeData>?){
        database.addValueEventListener(object : ValueEventListener {
                override fun onCancelled(p0: DatabaseError) {
                    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                }

                override fun onDataChange(p0: DataSnapshot) {
                    if (p0.value != null) {
                        var map = p0.value as Map<*, *>
                        for ((keys, value) in map) {
                            val subMap = map[keys] as Map<*, *>
                            for ((childKeys, value) in subMap) {
                                if (subMap[intent.getStringExtra("ID")] != null){
                                    val childMap = subMap[intent.getStringExtra("ID")] as Map<*, *>

                                    displayName.setText(childMap["recipeName"].toString())
                                    displayIngredient.setText(childMap["ingredients"].toString())
                                    displayStep.setText(childMap["step"].toString())
                                    Picasso.get().load(childMap["image"].toString()).into(displayImage)
                                    displayImage.layoutParams.width = 600
                                    displayImage.layoutParams.height = 600
                                    list!!.forEachIndexed { i, element ->
                                        if (element.type == keys) {
                                            displayCategory.setSelection(i)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            })
    }

    fun setLStorage(name: String, data: String){
        val sharedPrefer = getSharedPreferences(SHARE_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPrefer.edit()
        editor.putString(name, data)
        editor.apply()
        editor.commit()
    }

    fun getLStorage(name:String): String?{
        val sharedPrefer = getSharedPreferences(SHARE_PREFS, Context.MODE_PRIVATE)
        return sharedPrefer.getString(name, "null")
    }

    private fun isPermissionsAllowed(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    }

    private fun askForPermissions(): Boolean {
        if (!isPermissionsAllowed()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this as Activity, Manifest.permission.CAMERA)) {
                showPermissionDeniedDialog()
            } else {
                ActivityCompat.requestPermissions(this as Activity,arrayOf(Manifest.permission.CAMERA),REQUEST_CODE)
            }
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<String>,grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission is granted, you can perform your operation here
                } else {
                    // permission is denied, you can ask for permission again, if you want
                    askForPermissions()
                }
                return
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Denied")
            .setMessage("Permission is denied, Please allow permissions from App Settings.")
            .setPositiveButton("App Settings",
                DialogInterface.OnClickListener { dialogInterface, i ->
                    // send to app settings if permission is denied permanently
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", getPackageName(), null)
                    intent.data = uri
                    startActivity(intent)
                })
            .setNegativeButton("Cancel",null)
            .show()
    }
}
