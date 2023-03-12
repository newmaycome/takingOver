package com.example.asazna

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

open class RealmData : RealmObject() {
    @PrimaryKey var id : Long = 0L
    var name :String = ""
    var genre:String = ""
    var lat : Double = 0.0
    var lng :Double = 0.0
    var adress:String = ""
    var info:String = ""
    var editterID : String = ""

}