package com.example.asazna

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.realm.Realm
import io.realm.kotlin.createObject
import io.realm.kotlin.where
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SpotBottomSheet(private var nameinfo:String,val geotext:String,val spotgenre:String?,
                      val mLat:Double,val mLng:Double)
    : BottomSheetDialogFragment()  {

    private val db = FirebaseFirestore.getInstance()
    private  val auth = FirebaseAuth.getInstance()
    private val realm : Realm = Realm.getDefaultInstance()

    private lateinit var bottomsheetArea: View
    private lateinit var bottomsheetAreaArea: View
    private lateinit var bottomsheetBehavior: BottomSheetBehavior<View>

    val firestoreRef = db.collection("smokeStores")
        .whereEqualTo("latitude",mLat.toString())
        .whereEqualTo("longitude",mLng.toString())

    private val realmRef = realm.where(RealmData::class.java)
        .equalTo("lat", mLat)
        .equalTo("lng", mLng)
        .findFirst()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        val bottomsheetview = inflater.inflate(R.layout.fragment_bottom_sheet, container, false)
        val etspotname = bottomsheetview.findViewById<TextView>(R.id.etspotname)
        val spotimage = bottomsheetview.findViewById<ImageView>(R.id.spotimage)
        val spotlocation = bottomsheetview.findViewById<TextView>(R.id.spotlocation)
        val spotnameEdit = bottomsheetview.findViewById<ImageView>(R.id.spotnameedit)
        val infoEdit = bottomsheetview.findViewById<ImageView>(R.id.infoedit)
        val buttonFollow = bottomsheetview.findViewById<ToggleButton>(R.id.button)
        val edittername = bottomsheetview.findViewById<TextView>(R.id.EditUser)
        val btprivate = bottomsheetview.findViewById<Button>(R.id.btprivate)
        val btpublic = bottomsheetview.findViewById<Button>(R.id.btpublic)
        val infoText = bottomsheetview.findViewById<TextView>(R.id.textView)
        val btfollow = bottomsheetview.findViewById<Button>(R.id.button)
        val userImage = bottomsheetview.findViewById<ImageView>(R.id.edituserimage)

        val user = auth.currentUser?.uid.toString()

//ボトムシートに表示する
        if(realmRef != null) { //Realmに保存されている場合

            indicateRealmInfo(etspotname, spotlocation, infoText)

            spotnameEdit.visibility = View.INVISIBLE
            infoEdit.visibility = View.INVISIBLE
            btprivate.visibility = View.INVISIBLE
            btpublic.visibility = View.INVISIBLE
            btfollow.visibility = View.GONE
            edittername.visibility = View.INVISIBLE
            userImage.visibility = View.INVISIBLE
            btpublic.text = "公開する"
            btprivate.text = "更新して保存"

        }else{ //Firebaseに保存されている場合

            indicateFirebaseInfo(etspotname, spotlocation, infoText,edittername)

            spotnameEdit.visibility = View.INVISIBLE
            infoEdit.visibility = View.INVISIBLE
            btprivate.visibility = View.INVISIBLE
            btpublic.visibility = View.INVISIBLE
            btpublic.text = "更新する"
            btprivate.text = "非公開にする"
        }

//ボトムシートの挙動
        bottomsheetArea = bottomsheetview.findViewById<CoordinatorLayout>(R.id.blayout)
        bottomsheetAreaArea = bottomsheetview.findViewById<ConstraintLayout>(R.id.bottomsheet)
        bottomsheetBehavior = BottomSheetBehavior.from(bottomsheetAreaArea)

        bottomSheetAction()

//ユーザー名をタップしてマイページへ移動
        edittername.setOnClickListener {
            val intent = Intent(bottomsheetview.context,Mypage::class.java)
            intent.putExtra("editorname",edittername.text.toString())
            startActivity(intent)
            Log.d("AsaznaLog","$intent")

        }

//”編集”の表示・非表示
        val editTextView = bottomsheetview.findViewById<TextView>(R.id.editTextView)
        showEdit(editTextView)


        //既にフォローしている場合のフォローボタンについて
        val editorname = edittername.text.toString()

        firestoreRef.get()
            .addOnSuccessListener {result ->
                for (document in result){
                    val eid = document.data["editorUid"].toString()
                    Log.d("Asazna","このSPOTの編集者は${eid}です")
                  followButtonAction(eid,buttonFollow)
                }
            }

        //”編集”をタップした際の動作
        editTextView.setOnClickListener{
            if(spotnameEdit.visibility == View.INVISIBLE &&infoEdit.visibility == View.INVISIBLE){
                editTextView.text ="完了"
                spotnameEdit.visibility = View.VISIBLE
                infoEdit.visibility = View.VISIBLE
                buttonFollow.visibility = View.INVISIBLE
                btprivate.visibility = View.VISIBLE
                btpublic.visibility = View.VISIBLE
            }else if(spotnameEdit.visibility == View.VISIBLE &&infoEdit.visibility == View.VISIBLE){
                editTextView.text = "編集"
                spotnameEdit.visibility = View.INVISIBLE
                infoEdit.visibility = View.INVISIBLE
                buttonFollow.visibility = View.VISIBLE
                btprivate.visibility = View.INVISIBLE
                btpublic.visibility = View.INVISIBLE
            }
        }

        //以下編集
        //①名前
        spotnameEdit.setOnClickListener {
            val dialoglayout = LayoutInflater.from(context)
                .inflate(R.layout.marker_edit_alartdialog, null)
            val adedittext = dialoglayout.findViewById<EditText>(R.id.adedittext)
            adedittext.setText(etspotname.text)
            AlertDialog.Builder(requireContext())
                .setTitle("名前の編集")
                .setView(dialoglayout)
                .setPositiveButton("OK"){dialog,_->
                    val newshopname = adedittext.text.toString()
                    etspotname.text = newshopname
                }
                .setNegativeButton("キャンセル") { dialog, which ->
                    dialog.dismiss()
                }
                .create().show()
        }
        //②情報
        infoEdit.setOnClickListener {
            val dialoglayout2 = LayoutInflater.from(context)
                .inflate(R.layout.marker_edit_alartdialog, null)
            val adedittext2 = dialoglayout2.findViewById<EditText>(R.id.adedittext)
            adedittext2.setText(infoText.text)
            AlertDialog.Builder(requireContext())
                .setTitle("情報の編集")
                .setView(dialoglayout2)
                .setPositiveButton("OK"){dialog,_->
                    val newinfo = adedittext2.text.toString()
                    infoText.text = newinfo
                }
                .setNegativeButton("キャンセル") { dialog, which ->
                    dialog.dismiss()
                }
                .create().show()
        }
        btprivate.setOnClickListener {
            val newshopname = etspotname.text.toString()
            val newinfo = infoText.text.toString()
            setRealm(newshopname, newinfo)
        }

        btpublic.setOnClickListener{
            val newshopname = etspotname.text.toString()
            val newinfo = infoText.text.toString()
            setFireStore(newshopname,newinfo)
        }

        //フォローボタンを押した際の挙動（フォロー&フォロー解除、被フォローと被フォロー解除）
        buttonFollow.setOnCheckedChangeListener { buttonView, isChecked ->
            val user = auth.currentUser?.uid.toString()
            val editorname = edittername.text.toString()

            if(isChecked){
                firestoreRef.get()
                    .addOnSuccessListener {result ->
                        for(document in result) {
                            val eid = document.data["editorUid"].toString()
                            toggleButtonIsTrue(eid)
                        }
                }
            }else{
                firestoreRef.get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            val eid = document.data["editorUid"].toString()
                            toggleButtonIsFales(eid)
                        }
                    }
            }
        }
        return bottomsheetview
    }

    //以下メソッド
    //EditUserをフォローする
    @RequiresApi(Build.VERSION_CODES.O)
    private fun followUser(followNum:Int, now: LocalDateTime) {
        val follow1 = followNum + 1
        val user = auth.currentUser?.uid.toString()

        firestoreRef.get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val editor = document.data["editorUid"].toString()
                    val followat = ZonedDateTime.of(now, ZoneId.of("Asia/Tokyo"))
                    val followinfo = mapOf(
                        "followingAt" to "$followat"
                    )

                    db.collection("users")
                        .document(user)
                        .collection("followingUsers")
                        .document(editor)
                        .set(followinfo)

                    Log.d("AsaznaLog", "${followat}にフォローしたで")

                }
            }
        db.collection("users")
            .document(user).update("followingCount", follow1)
        Log.d("AsaznaLog","フォローしてる人が${follow1}人になりました")
    }

    //フォローされようとしているユーザのリファレンス（この後フォロー）
    @RequiresApi(Build.VERSION_CODES.O)
    private fun refFollowedUser(eid:String,now: LocalDateTime){

        db.collection("users")
            .document(eid)
            .get()
            .addOnCompleteListener {
                val getFresult = it.result
                val followedNumber = getFresult.data?.get("beingFollowedCount").toString().toInt()
                Log.d("AsaznaLog","${eid}はんのフォロワーは${followedNumber}人だったんや")
                beFollowed(eid,now,followedNumber)
            }
    }
    //フォローされる
    @RequiresApi(Build.VERSION_CODES.O)
    private fun beFollowed(eid:String, now: LocalDateTime, followedNum: Int) {
        val user = auth.currentUser?.uid.toString()
        val followed1 = followedNum + 1
        val befollowedat = ZonedDateTime.of(now, ZoneId.of("Asia/Tokyo"))
        val befollowedinfo = mapOf(
            "followedAt" to "$befollowedat"
        )
        db.collection("users")
            .document(eid)
            .collection("beingFollowedUsers")
            .document(user)
            .set(befollowedinfo)

        db.collection("users")
            .document(eid).update("beingFollowedCount",followed1)

        Log.d("AsaznaLog","${user}にフォローされたで")
    }

    //フォローを解除する（フォローしていた側）
    private fun unFollowUser(followNum:Int){
        val followm1 = followNum - 1
        val user = auth.currentUser?.uid.toString()

        firestoreRef.get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val editor = document.data["editorUid"].toString()

                    val reffollowing = db.collection("users")
                        .document(user)
                        .collection("followingUsers")
                        .document(editor)

                    reffollowing.delete()
                    Log.d("AsaznaLog","フォロー解除完了！")
                }
            }
        db.collection("users")
            .document(user).update("followingCount", followm1)

        Log.d("AsaznaLog","フォローしてる人が${followm1}人になりました")

    }

    //フォローされているユーザーのリファレンス（この後解除）
    private fun refUnfollowedUser(eid:String){
        db.collection("users")
            .document(eid)
            .get()
            .addOnCompleteListener {
                val getFresult = it.result
                val followedNumber = getFresult.data?.get("beingFollowedCount").toString().toInt()
                Log.d("AsaznaLog","フォロワーが。。。")
                beUnfollowed(eid,followedNumber)
            }
    }

    //フォロー解除される
    private fun beUnfollowed(eid:String,followedNumber:Int){
        val followedm1 = followedNumber - 1
        val user = auth.currentUser?.uid.toString()

        val followeduser = db.collection("users")
            .document(eid)
            .collection("beingFollowedUsers")
            .document(user)

        followeduser.delete()

        db.collection("users")
            .document(eid).update("beingFollowedCount", followedm1)

        Log.d("AsaznaLog","${followedm1}人に減ってもうた。。。")

    }

    //「編集」の表示・非表示について
    private fun showEdit(editTextView: TextView){
        val user = auth.currentUser?.uid.toString()

        val refRealm = realm.where(RealmData::class.java)
            .equalTo("name", nameinfo)
            .findAll()
        refRealm.forEach {

            val rMarkerEID = it.editterID

            if(user == rMarkerEID){
                editTextView.visibility = View.VISIBLE
            }else{
                editTextView.visibility = View.INVISIBLE
            }
        }

        firestoreRef.get()
            .addOnSuccessListener{ result ->
                for (document in result) {
                    val eid = document.data["editorUid"].toString()

                    if(user == eid){
                        editTextView.visibility = View.VISIBLE
                    }else{
                        editTextView.visibility = View.INVISIBLE
                    }
                }
            }
    }

    // Realm情報の書き換えorRealmに登録
    fun setRealm(newshopname:String,newinfo:String) {

        val refRealm = realm.where(RealmData::class.java)
            .equalTo("name", nameinfo)
            .findFirst()
        if(refRealm != null){ //Realm情報の更新
            realm.executeTransaction {
                Log.d("AsaznaLog", "$refRealm")
                refRealm.name = newshopname
                refRealm.info = newinfo
                Log.d("AsaznaLog", "newname:${refRealm.name} + newinfo:${refRealm.info}")
            }
        }else{//Firestore情報を削除しRealmに保存
            firestoreRef.get()
                .addOnSuccessListener{ result ->
                    Log.d("AsaznaLog","さてこの取得したデータをどうするんや？")
                    for (document in result) {
                        val rMarkerName = document.data["storeName"].toString()
                        val rMarkerInfo = document.data["description"].toString()
                        val rMarkerAdress = document.data["adress"].toString()
                        val eid = document.data["editorUid"].toString()
                        val genre = document.data["genre"].toString()
                        val lat:Double = document.data["latitude"] as Double
                        val lng:Double = document.data["longitude"] as Double

                        realm.executeTransaction{
                            val rCurrentId = realm.where<RealmData>().max("id")
                            val nextId = (rCurrentId?.toLong() ?: 0L) + 1L
                            val myModel = realm.createObject<RealmData>(nextId)

                            myModel.name = rMarkerName
                            myModel.genre = genre
                            myModel.lat = lat
                            myModel.lng = lng
                            myModel.adress = rMarkerAdress
                            myModel.info = rMarkerInfo
                            myModel.editterID = eid

                            Log.d("AsaznaLog","Firestoreから引っ越し完了")
                        }
                    }
                    db.collection("smokeStores").document(geotext).delete()
                }
        }
    }


    // Firestoreに登録orFirestore情報の書き換え
    private fun setFireStore(spname:String,spinfo:String){

        val refRealm = realm.where(RealmData::class.java)
            .equalTo("name", nameinfo)
            .findFirst()

        if(refRealm != null){//Realm情報を削除しFireStoreに登録
            val spotinfo = mapOf(
                "genre" to refRealm.genre,
                "storeName" to spname,
                "latitude" to refRealm.lat.toString(),
                "longitude" to refRealm.lng.toString(),
                "address" to refRealm.adress,
                "description" to spinfo,
                "editorUid" to refRealm.editterID,
                "phoneNumber" to ""
            )

            db.collection("smokeStores")
                .document(refRealm.adress)
                .set(spotinfo)
                .addOnSuccessListener {
                    Log.d("AsaznaLog", "Firebaseに引っ越し完了")
                    realm.executeTransaction{
                        val refRealm2 = realm.where(RealmData::class.java)
                            .equalTo("name",nameinfo)
                            .findFirst()
                        refRealm2?.deleteFromRealm()
                        if (refRealm2 == null){
                            Log.d("AsaznaLog","Firestoreにいっても元気でね！")
                        }else{
                            Log.d("AsaznaLog","$refRealm2")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("AsaznaLog", "なんか知らんけどミスっとるで",e)
                }
        }else{//Firestore情報の更新

            if(nameinfo != spname) {
                db.collection("smokeStores")
                    .document(geotext)
                    .update("storeName", spname, "description", spinfo)
            }
        }
    }

    //非公開の情報（Realmに登録されているもの）を表示するメソッド
    private fun indicateRealmInfo(spotname: TextView, adress: TextView, info: TextView){
        Log.d("AsaznaLog","Realmから持ってきやす")
        val rMarkerName = realmRef?.name
        val rMarkerInfo = realmRef?.info
        val rMarkerAdress = realmRef?.adress

        spotname.text = rMarkerName
        adress.text = rMarkerAdress
        if(rMarkerInfo == "") {
            info.text = "情報がありません"
        }else{
            info.text = rMarkerInfo
        }
    }
    //公開されている情報（Firebaseに登録されているもの）を表示するメソッド
    private fun indicateFirebaseInfo(spotname: TextView, adress: TextView,
                                     info: TextView, editter: TextView
    ){
        Log.d("AsaznaLog","firestoreから持ってきやす")
        firestoreRef.get()
            .addOnSuccessListener{ result ->
                Log.d("AsaznaLog","${result}")
                    for (document in result) {
                        Log.d("AsaznaLog", "${document}を持ってきました！")
                        val rMarkerName = document.data["storeName"].toString()
                        val rMarkerInfo = document.data["description"].toString()
                        val rMarkerAdress = document.data["adress"].toString()
                        val eid = document.data["editorUid"].toString()

                        Log.d("AsaznaLog", "$rMarkerInfo,$rMarkerAdress,$eid")

                        spotname.text = rMarkerName
                        adress.text = rMarkerAdress
                        showEID(eid, editter)
                        if (rMarkerInfo == "") {
                            info.text = "情報がありません"
                        } else {
                            info.text = rMarkerInfo
                        }
                    }

            }
            .addOnFailureListener {
                Log.d("AsaznaLog","あかん！持ってこれないわ")
            }
    }

    //ボトムシートにEIDの表示
    private fun showEID(eid:String,editterTextView: TextView){
        db.collection("users")
            .whereEqualTo("uid", eid)
            .get()
            .addOnSuccessListener{ result ->
                for (document in result) {
                    val name = document.data["username"]
                    editterTextView.text = name.toString()
                }
            }
    }

    //ボトムシートの表示についてのメソッド
    private fun bottomSheetAction() {
        bottomsheetAreaArea.setOnClickListener {
            if (bottomsheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomsheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else if (bottomsheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                bottomsheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
    }

    private fun followButtonAction(eid: String,btfollow:ToggleButton){
        val user = auth.currentUser?.uid.toString()
        db.collection("users").document(user)
            .collection("followingUsers")
            .document(eid)
            .get()
            .addOnSuccessListener{document ->
            if(document.data != null){
                Log.d("AsaznaLog","${document.data}")
                btfollow.isChecked = true
              }
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun toggleButtonIsTrue(eid: String) {
        val user = auth.currentUser?.uid.toString()
        db.collection("users")
            .document(user)
            .collection("followingUsers")
            .document(eid)
            .get()
            .addOnSuccessListener {document->

                if (document.data == null) {
                    val now = LocalDateTime.now()
                    //フォローした側
                    db.collection("users")
                        .document(user)
                        .get()
                        .addOnCompleteListener {
                            val getFresult = it.result
                            val followNumber =
                                getFresult.data?.get("followingCount").toString()
                                    .toInt()
                            followUser(followNumber, now)
                        }
                    //フォローされた側
                    firestoreRef.get()
                        .addOnSuccessListener { result ->
                            for (document in result) {
                                val editor = document.data["editorUid"].toString()
                                Log.d("AsaznaLog", "${editor}はん！誰かにフォロ－されましたよ！")
                                refFollowedUser(editor, now)
                            }
                        }
                } else {
                    Log.d("AsaznaLog", "既にフォロー中でっせ")
                    return@addOnSuccessListener
                }
            }

    }
    private fun toggleButtonIsFales(eid: String) {
        val user = auth.currentUser?.uid.toString()
        db.collection("users")
            .document(user)
            .collection("followingUsers")
            .document(eid)
            .get()
            .addOnSuccessListener { document->
                if(document != null){
                Log.d("AsaznaLog", "フォロー外したで")
                val user = auth.currentUser?.uid.toString()
                //フォロー外した側
                db.collection("users")
                    .document(user)
                    .get()
                    .addOnCompleteListener {
                        val getFresult = it.result
                        val followNumber =
                            getFresult.data?.get("followingCount").toString()
                                .toInt()
                        unFollowUser(followNumber)
                        Log.d("AsaznaLog", "フォロー外したで")
                    }
                //フォロー外された側
                firestoreRef.get()
                    .addOnSuccessListener { result ->
                        for (document in result) {
                            val editor = document.data["editorUid"].toString()
                            Log.d("AsaznaLog", "${editor}はん！")
                            refUnfollowedUser(editor)
                        }
                    }
            }else{
                    Log.d("AsaznaLog", "まだフォローもしてないで")
                    return@addOnSuccessListener
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realm.close()
    }
}