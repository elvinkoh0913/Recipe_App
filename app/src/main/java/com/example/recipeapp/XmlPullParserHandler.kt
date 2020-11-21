package com.example.recipeapp

import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.InputStream

class XmlPullParserHandler {
    private val Recipes = ArrayList<RecipeData>()
    private var recipe: RecipeData? = null
    private var text: String? = null

    fun parse(inputStream: InputStream): List<RecipeData> {
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val tagname = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> if (tagname.equals("recipe", ignoreCase = true)) {
                        // create a new instance of employee
                        recipe = RecipeData()
                    }
                    XmlPullParser.TEXT -> text = parser.text
                    XmlPullParser.END_TAG -> if (tagname.equals("recipe", ignoreCase = true)) {
                        // add employee object to list
                        recipe?.let { Recipes.add(it) }
                    } else if (tagname.equals("type", ignoreCase = true)) {
                        recipe!!.type = text!!
                    }
//                    else if (tagname.equals("name", ignoreCase = true)) {
//                        employee!!.name = text
//                    } else if (tagname.equals("salary", ignoreCase = true)) {
//                        employee!!.salary = java.lang.Float.parseFloat(text)
//                    }

                    else -> {
                    }
                }
                eventType = parser.next()
            }

        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Recipes
    }
}