package navigation


import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.project.R
import com.example.project.databinding.ActivityAddPhotoBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import model.ContentDTO
import java.text.SimpleDateFormat
import java.util.*

class AddPhotoActivity : AppCompatActivity() {
    private var storage: FirebaseStorage? = null
    private var photoUri: Uri? = null
    private var auth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore? = null
    private lateinit var binding: ActivityAddPhotoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_photo)
        binding = ActivityAddPhotoBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val photoPickerIntent = Intent(Intent.ACTION_PICK)
        photoPickerIntent.type = "image/*"
        startForResult.launch(photoPickerIntent)

        binding.addphotoBtnUpload.setOnClickListener { contentUpload() }
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                photoUri = result.data?.data
                binding.addphotoImage.setImageURI(photoUri)
            } else {
                finish()
            }
        }

    private fun contentUpload() {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "IMAGE_" + timestamp + "_.png"

        val storageRef = storage?.reference?.child("images")?.child(imageFileName)

        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                val contentDTO = ContentDTO()

                contentDTO.imageUrl = uri.toString()

                contentDTO.uid = auth?.currentUser?.uid

                contentDTO.userId = auth?.currentUser?.email

                contentDTO.explain = binding.addphotoEditExplain.text.toString()

                contentDTO.timestamp = System.currentTimeMillis()

                firestore?.collection("images")?.document()?.set(contentDTO)

                setResult(Activity.RESULT_OK)

                finish()
            }
        }
    }
}