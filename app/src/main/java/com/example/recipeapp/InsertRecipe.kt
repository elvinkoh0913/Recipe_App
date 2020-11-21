package com.example.recipeapp

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.IgnoreExtraProperties
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.io.IOException

@Suppress("DEPRECATION")
class InsertRecipe : AppCompatActivity() {
    lateinit var storageRef: StorageReference
    val REQUEST_CODE = 200
    private val database = FirebaseDatabase.getInstance()

    lateinit var option: Spinner
    lateinit var addButton: Button
    lateinit var insImage: ImageView
    lateinit var rName: EditText
    lateinit var ingred: EditText
    lateinit var sText: EditText
    lateinit var loadGif: ProgressBar

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insert_recipe)
        storageRef =  FirebaseStorage.getInstance().reference

        addButton = findViewById(R.id.insertBtn)
        option = findViewById(R.id.category)
        insImage = findViewById(R.id.insertImage)
        rName = findViewById(R.id.recipeName)
        ingred = findViewById(R.id.ingredient)
        sText = findViewById(R.id.Step)
        loadGif = findViewById(R.id.loadBar)

        loadGif.isVisible = false

        insImage.setOnClickListener {
            if (askForPermissions()) {
                capturePhoto()
            }else{
                Toast.makeText(baseContext, "false", Toast.LENGTH_LONG).show()
            }
        }

        addButton.setOnClickListener{
            val  isDefault = insImage.drawable.constantState == resources.getDrawable(R.drawable.ic_camera).constantState
            if (isDefault){
                Toast.makeText(baseContext, "please take a photo", Toast.LENGTH_LONG).show()
            }else{
                if (validate()){
                    loadGif.isVisible = true

                    uploadImage(rName.text.toString())

                    finish()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }
        }

        val recipes: List<RecipeData>?
        try {
            val parser = XmlPullParserHandler()
            val istream = assets.open("recipetypes.xml")
            recipes = parser.parse(istream)

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, recipes)

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            option.adapter = adapter

            option.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(p0: AdapterView<*>?) {
                }

                override fun onItemSelected(
                    p0: AdapterView<*>?,
                    p1: View?,
                    position: Int,
                    p3: Long
                ) {
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private  fun  validate():Boolean {
        val name = rName.text.toString().trim()
        val ingredient = ingred.text.toString().trim()
        val step =  sText.text.toString().trim()
        val type = option.selectedItem.toString()

        if (name.isEmpty()) {
            rName.error = "Please enter recipe name"
            rName.requestFocus()
            return false
        }

        if (ingredient.isEmpty()) {
            ingred.error = "Please enter ingredients"
            ingred.requestFocus()
            return false
        }
        if (step.isEmpty()) {
            sText.error =  "Please enter steps"
            sText.requestFocus()
            return false
        }
        if (type == "Category") {
            Toast.makeText(baseContext, "Please specify the recipe category", Toast.LENGTH_LONG).show()
            option.isFocusable = true

            option.isFocusableInTouchMode = true
            option.requestFocus()
            return false
        }

        return true
    }

    @IgnoreExtraProperties
    data class newRecipe(
        var RecipeName: String? = "",
        var Ingredients: String? = "",
        var Step: String? = "",
        var Image: String? = ""
    )

    private fun writeRecipe(URl: String){

        val name = rName.text.toString().trim()
        val ingredient = ingred.text.toString().trim()
        val step =  sText.text.toString().trim()
        val type = option.selectedItem.toString()

        //uploadImage(name)
        //Toast.makeText(baseContext, , Toast.LENGTH_LONG).show()
        val nRecipe = newRecipe(name, ingredient, step, URl)

        val id = database.reference.push().key


        database.reference.child("recipe").child(type).child(id!!).setValue(nRecipe)
        loadGif.isVisible = false

    }


    private fun uploadImage(picName: String) {
        var downloadurl = ""
        val bitmap = (insImage.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        var uploadTask = storageRef.child(picName).putBytes(data)

        uploadTask.addOnFailureListener {
            Toast.makeText(baseContext, it.toString(), Toast.LENGTH_LONG).show()
        }.addOnSuccessListener { task ->
            val du=task.uploadSessionUri.toString()
            val du3:String="&alt=media"
            val du1=du!!.substring(0,du!!.indexOf("&uploadType"))
            downloadurl = du1+du3
            writeRecipe(downloadurl)
            //Toast.makeText(baseContext, "url: $downloadurl", Toast.LENGTH_LONG).show()
        }

    }

    private fun capturePhoto() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE && data != null) {
            insImage.setImageBitmap(data.extras!!.get("data") as Bitmap)

            insImage.layoutParams.width = 600
            insImage.layoutParams.height = 600

        }
    }

    private fun isPermissionsAllowed(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

    }

    private fun askForPermissions(): Boolean {
        if (!isPermissionsAllowed()) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this as Activity,Manifest.permission.CAMERA)) {
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