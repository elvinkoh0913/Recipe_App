package com.example.recipeapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.list_recipe.view.*
import java.io.IOException


class MainActivity : AppCompatActivity() {
    private val database = FirebaseDatabase.getInstance()
    private lateinit var auth: FirebaseAuth

    lateinit var option :Spinner
    lateinit var addNewRecipe: Button
    private var SHARE_PREFS = "SharedPrefs"
    var recipeList = ArrayList<Recipe>()
    var adapter: RecipeAdapter? = null
    lateinit var signoutBtn: Button



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null){
            startActivity(Intent(this, LoginActivity::class.java))
        }
        option = findViewById(R.id.recipeSpinner)
        addNewRecipe = findViewById(R.id.addNew)
        signoutBtn = findViewById(R.id.logoutBtn)

        signoutBtn.setOnClickListener{
            showAlertDialog()
        }

        addNewRecipe.setOnClickListener{
            val intent = Intent(this, InsertRecipe::class.java)
            startActivity(intent)
        }

        val recipes: List<RecipeData>?
        try {
            val parser = XmlPullParserHandler()
            val istream = assets.open("recipetypes.xml")
            recipes = parser.parse(istream)

            val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, recipes)

            adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item)

            option.adapter = adapter

            option.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
                override fun onNothingSelected(p0: AdapterView<*>?) {
                }

                override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                    if (recipes[position].toString() == "Category"){
                        getAllRecipe()
                    }else {
                        getRecipe(recipes[position].toString())
                    }
                    //result.text = getLStorage("selected")
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if (auth.currentUser != null){
            finishAffinity()
            finish()
        }
    }

    class RecipeAdapter: BaseAdapter {
        var recipeList:ArrayList<Recipe>
        var context: Context? = null

        constructor(context: Context, productList: ArrayList<Recipe>) : super() {
            this.context = context
            this.recipeList = productList
        }

        override fun getCount(): Int {
            return recipeList.size
        }

        override fun getItem(position: Int): Any {
            return recipeList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {

            val recipe = this.recipeList[position]

            var inflater = context!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            var recipeView = inflater.inflate(R.layout.list_recipe, null)

            recipeView.recipeNameLV.text = recipe.name
            Picasso.get().load(recipe.image).into(recipeView.imageRecipe)
//            foodView.statustext1.text = recipe.ingredients
//            foodView.statustext2.text = recipe.steps
            return recipeView
        }

    }

    private fun getRecipe(recipeType: String){
        database.reference.child("recipe").child(recipeType).addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(baseContext, p0.toString(), Toast.LENGTH_LONG).show()
            }

            @SuppressLint("DefaultLocale")
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.value != null) {
                    val map = p0.value as Map<*, *>
                    recipeList.clear()
                    adapter = RecipeAdapter(baseContext!!, recipeList)
                    adapter!!.notifyDataSetChanged()
                    for ((keys, value) in map) {
                        val subMap = map[keys] as Map<*, *>
                        var name = subMap["recipeName"].toString()
                        var image = subMap["image"].toString()
                        recipeList.add(Recipe(name, image, keys.toString(), recipeType))
                    }

                    adapter = RecipeAdapter(baseContext!!, recipeList)
                    recipeListView.adapter = adapter
                    recipeListView.setOnItemClickListener { parent, view, position, id ->
                        val intent = Intent(baseContext, DetailActivity::class.java)
                        intent.putExtra("ID",recipeList[position].id)
                        startActivity(intent)
                    }
                    //setLStorage("selected", map[recipeType.toLowerCase()].toString())
                }
            }
        })
    }

    private fun getAllRecipe(){
        database.reference.child("recipe").addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            @SuppressLint("DefaultLocale")
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.value != null) {
                    val map = p0.value as Map<*, *>
                    recipeList.clear()
                    adapter = RecipeAdapter(baseContext!!, recipeList)
                    adapter!!.notifyDataSetChanged()
                    for ((keys, value) in map) {
                        val subMap = map[keys] as Map<*, *>
                        for ((childKeys, value) in subMap) {
                            val childMap = subMap[childKeys] as Map<*, *>
                            val name = childMap["recipeName"].toString()
                            var image = childMap["image"].toString()

                            recipeList.add(Recipe(name, image, childKeys.toString(), keys.toString()))
                        }
                    }

                    adapter = RecipeAdapter(baseContext!!, recipeList)
                    recipeListView.adapter = adapter
                    recipeListView.setOnItemClickListener { parent, view, position, id ->
                        val intent = Intent(baseContext, DetailActivity::class.java)
                        intent.putExtra("ID",recipeList[position].id)
                        intent.putExtra("Type",recipeList[position].type)
                        startActivity(intent)
                    }
                }
            }
        })
    }

    private fun showAlertDialog() {
        val alertDialog: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialog.setTitle("You are going to logout")
        alertDialog.setMessage("Do you want logout now?")

        alertDialog.setPositiveButton(
            "Yes"
        ) { _, _ ->
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
        }
        alertDialog.setNegativeButton(
            "No"
        ) { _, _ -> }
        val alert: AlertDialog = alertDialog.create()
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }

}
