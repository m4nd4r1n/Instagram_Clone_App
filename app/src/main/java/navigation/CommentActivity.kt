package navigation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.project.R
import com.example.project.databinding.ActivityCommentBinding
import com.example.project.databinding.ItemCommentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import model.AlarmDTO
import model.ContentDTO
import util.FcmPush

class CommentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCommentBinding
    var contentUid: String? = null
    private var destinationUid: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)
        binding = ActivityCommentBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        contentUid = intent.getStringExtra("contentUid")
        destinationUid = intent.getStringExtra("destinationUid")

        binding.commentRecyclerview.adapter = CommentRecyclerViewAdapter()
        binding.commentRecyclerview.layoutManager = LinearLayoutManager(this)

        binding.commentBtnSend.setOnClickListener {
            if (binding.commentEditMessage.text.toString().isEmpty()) {
                Toast.makeText(this, "내용을 입력하세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val comment = ContentDTO.Comment()
            comment.userId = FirebaseAuth.getInstance().currentUser?.email
            comment.uid = FirebaseAuth.getInstance().currentUser?.uid
            comment.comment = binding.commentEditMessage.text.toString()
            comment.timestamp = System.currentTimeMillis()

            FirebaseFirestore.getInstance().collection("images").document(contentUid!!)
                .collection("comments").document().set(comment)
            commentAlarm(destinationUid!!, binding.commentEditMessage.text.toString(), contentUid!!)
            binding.commentEditMessage.setText("")
        }
    }

    private fun commentAlarm(destinationUid: String, message: String, postId: String) {
        if (destinationUid == FirebaseAuth.getInstance().currentUser?.uid)
            return
        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
        alarmDTO.kind = 1
        alarmDTO.postId = postId
        alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
        alarmDTO.timestamp = System.currentTimeMillis()
        alarmDTO.message = message
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
        FirebaseFirestore.getInstance().collection("images").document(postId).get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val msg =
                        FirebaseAuth.getInstance().currentUser?.email + " " + getString(R.string.alarm_comment1) +
                                " \"" + task.result.data?.get("explain") + "\" " + getString(R.string.alarm_comment2) +
                                " \"" + message + "\""
                    FcmPush.instance.sendMessage(destinationUid, "stagram", msg)
                }
            }
    }

    inner class CommentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var comments: ArrayList<ContentDTO.Comment> = arrayListOf()

        init {
            FirebaseFirestore.getInstance().collection("images").document(contentUid!!)
                .collection("comments").orderBy("timestamp").addSnapshotListener { value, error ->
                    comments.clear()
                    if (value == null) return@addSnapshotListener

                    for (snapshot in value.documents) {
                        comments.add(snapshot.toObject(ContentDTO.Comment::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding =
                ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(binding)
        }

        private inner class CustomViewHolder(var binding: ItemCommentBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewholder = (holder as CustomViewHolder)
            viewholder.binding.commentviewitemTextviewComment.text = comments[position].comment
            viewholder.binding.commentviewitemTextviewProfile.text = comments[position].userId

            FirebaseFirestore.getInstance().collection("profileImages")
                .document(comments[position].uid!!).get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val url = task.result!!["image"]
                        Glide.with(holder.itemView.context).load(url)
                            .apply(RequestOptions().circleCrop())
                            .into(viewholder.binding.commentviewitemImageviewProfile)
                    }
                }
        }

        override fun getItemCount(): Int {
            return comments.size
        }

    }
}