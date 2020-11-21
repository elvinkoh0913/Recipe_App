package com.example.recipeapp

class Recipe {
    var type:String = ""
    var name: String = ""
    var ingredients: String = ""
    var steps: String = ""
    var image: String = ""
    var id: String = ""

    constructor(foodName: String, img: String, newId:String, getType:String) {//,ingredient: String,step: String
        this.name = foodName
        this.image = img
        this.id = newId
        this.type = getType
//        this.ingredients = ingredient
//        this.steps = step
    }

    override fun toString(): String {
        return "$type"
    }
}

