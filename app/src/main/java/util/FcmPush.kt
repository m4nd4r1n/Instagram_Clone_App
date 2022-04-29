package util

import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.squareup.okhttp.*
import model.PushDTO
import java.io.IOException

class FcmPush {
    private var JSON: MediaType = MediaType.parse("application/json; charset=utf-8")
    private var url = "https://fcm.googleapis.com/fcm/send"
    private var serverKey = ""
    private var gson: Gson? = null
    private var okHttpClient: OkHttpClient? = null

    companion object {
        var instance = FcmPush()
    }

    init {
        gson = Gson()
        okHttpClient = OkHttpClient()
    }

    fun sendMessage(destinationUid: String, title: String, message: String) {
        FirebaseFirestore.getInstance().collection("pushtokens").document(destinationUid).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result?.get("pushToken").toString()

                    val pushDTO = PushDTO()
                    pushDTO.to = token
                    pushDTO.notification.title = title
                    pushDTO.notification.body = message

                    val body = RequestBody.create(JSON, gson?.toJson(pushDTO))
                    val request = Request.Builder()
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Authorization", "key=$serverKey")
                        .url(url)
                        .post(body)
                        .build()
                    okHttpClient?.newCall(request)?.enqueue(object : Callback {
                        override fun onFailure(request: Request?, e: IOException?) {

                        }

                        override fun onResponse(response: Response?) {

                        }
                    })
                }
            }
    }
}