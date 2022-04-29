package navigation

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.project.R
import com.google.firebase.firestore.FirebaseFirestore
import model.ContentDTO
import com.example.project.databinding.ItemDetailBinding
import com.example.project.databinding.FragmentDetailBinding
import com.google.firebase.auth.FirebaseAuth
import model.AlarmDTO
import util.FcmPush
import kotlin.collections.ArrayList

class DetailViewFragment : Fragment() {
    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser?.uid
        val pos = arguments?.getInt("pos")

        binding.detailviewfragmentRecyclerview.adapter = DetailViewRecyclerViewAdapter()
        binding.detailviewfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)

        if (pos != null) {
            Handler(Looper.getMainLooper()).postDelayed({
                binding.detailviewfragmentRecyclerview.smoothScrollToPosition(pos)
            }, 200)
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class DetailViewRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var contentDTOs: ArrayList<ContentDTO> = arrayListOf()
        private var contentUidList: ArrayList<String> = arrayListOf()

        init {
            firestore?.collection("images")?.orderBy("timestamp")
                ?.addSnapshotListener { value, error ->
                    contentDTOs.clear()
                    contentUidList.clear()

                    if (value == null) return@addSnapshotListener

                    for (snapshot in value.documents) {
                        val item = snapshot.toObject(ContentDTO::class.java)
                        contentDTOs.add(item!!)
                        contentUidList.add(snapshot.id)
                    }
                    contentDTOs.reverse()
                    contentUidList.reverse()
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding =
                ItemDetailBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(binding)
        }

        inner class CustomViewHolder(var binding: ItemDetailBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewholder = (holder as CustomViewHolder)
            var doubleClickLastTime = 0L
            if (uid != contentDTOs[position].uid)
                viewholder.binding.detailviewitemDelete.visibility = View.INVISIBLE

            viewholder.binding.detailviewitemDelete.setOnClickListener {
                deletePost(position, holder.itemView.context)
            }

            viewholder.binding.detailviewitemProfileTextview.text = contentDTOs[position].userId

            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .into(viewholder.binding.detailviewitemImageviewContent)

            viewholder.binding.detailviewitemImageviewContent.setOnClickListener {
                if (System.currentTimeMillis() - doubleClickLastTime < 300) {
                    doubleClickLastTime = 0
                    favoriteEvent(position)
                } else {
                    doubleClickLastTime = System.currentTimeMillis()
                }
            }

            viewholder.binding.detailviewitemIdTextview.text = contentDTOs[position].userId

            viewholder.binding.detailviewitemIdTextview.setTypeface(null, Typeface.BOLD)

            viewholder.binding.detailviewitemExplainTextview.text = contentDTOs[position].explain

            if (contentDTOs[position].favoriteCount == 0) {
                viewholder.binding.detailviewitemFavoriteLayout.visibility = View.INVISIBLE
            } else {
                viewholder.binding.detailviewitemFavoriteLayout.visibility = View.VISIBLE
            }

            viewholder.binding.detailviewitemFavoritecounterCount.text =
                contentDTOs[position].favoriteCount.toString() + "명"

            viewholder.binding.detailviewitemFavoritecounterCount.setTypeface(null, Typeface.BOLD)

            FirebaseFirestore.getInstance().collection("profileImages")
                .document(contentDTOs[position].uid!!).get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val url = task.result!!["image"]
                        if (url != null) {
                            Glide.with(holder.itemView.context).load(url)
                                .apply(RequestOptions().circleCrop())
                                .into(viewholder.binding.detailviewitemProfileImageview)
                        }
                    }
                }

            viewholder.binding.detailviewitemFavoriteImageview.setOnClickListener {
                favoriteEvent(position)
            }

            if (contentDTOs[position].favorites.containsKey(uid)) {
                viewholder.binding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite)
                viewholder.binding.detailviewitemFavoriteImageview.imageTintList =
                    ColorStateList.valueOf(
                        Color.parseColor("#DD4466")
                    )
            } else {
                viewholder.binding.detailviewitemFavoriteImageview.setImageResource(R.drawable.ic_favorite_border)
                viewholder.binding.detailviewitemFavoriteImageview.imageTintList =
                    ColorStateList.valueOf(
                        Color.parseColor("#000000")
                    )
            }

            viewholder.binding.detailviewitemProfileImageview.setOnClickListener {
                val fragment = UserFragment()
                val bundle = Bundle()
                bundle.putString("destinationUid", contentDTOs[position].uid)
                bundle.putString("userId", contentDTOs[position].userId)
                fragment.arguments = bundle
                activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.main_content, fragment)?.commit()
            }
            viewholder.binding.detailviewitemCommentImageview.setOnClickListener { v ->
                val intent = Intent(v.context, CommentActivity::class.java)
                intent.putExtra("contentUid", contentUidList[position])
                intent.putExtra("destinationUid", contentDTOs[position].uid)
                startActivity(intent)
            }
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

        private fun deletePost(position: Int, context: Context) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle("삭제하시겠습니까?")
            builder.setPositiveButton("삭제") { dialogInterface: DialogInterface, i: Int ->
                firestore?.collection("images")?.document(contentUidList[position])
                    ?.collection("comments")?.get()?.addOnSuccessListener {
                        for (document in it) {
                            firestore?.collection("images")?.document(contentUidList[position])
                                ?.collection("comments")?.document(document.id)?.delete()
                        }
                        firestore?.collection("images")?.document(contentUidList[position])
                            ?.delete()
                    }
                firestore?.collection("alarms")?.whereEqualTo("postId", contentUidList[position])
                    ?.get()?.addOnSuccessListener { result ->
                        for (documents in result) {
                            FirebaseFirestore.getInstance().collection("alarms")
                                .document(documents.id)
                                .delete()
                        }
                    }
            }
            builder.setNegativeButton("취소") { dialogInterface: DialogInterface, i: Int ->
            }
            builder.show()
        }

        private fun favoriteEvent(position: Int) {
            val tsDoc = firestore?.collection("images")?.document(contentUidList[position])
            firestore?.runTransaction { transaction ->

                val contentDTO = transaction.get(tsDoc!!).toObject(ContentDTO::class.java)
                val postId = transaction.get(tsDoc).id
                if (contentDTO!!.favorites.containsKey(uid)) {
                    contentDTO.favoriteCount = contentDTO.favoriteCount - 1
                    contentDTO.favorites.remove(uid)
                    favoriteCancel(contentDTOs[position].uid!!, postId)
                } else {
                    contentDTO.favoriteCount = contentDTO.favoriteCount + 1
                    contentDTO.favorites[uid!!] = true
                    favoriteAlarm(contentDTOs[position].uid!!, postId)
                }
                transaction.set(tsDoc, contentDTO)

            }
        }

        private fun favoriteCancel(destinationUid: String, postId: String) {
            FirebaseFirestore.getInstance().collection("alarms")
                .whereEqualTo("destinationUid", destinationUid)
                .whereEqualTo("uid", FirebaseAuth.getInstance().currentUser?.uid)
                .whereEqualTo("kind", 0)
                .whereEqualTo("postId", postId)
                .get()
                .addOnSuccessListener { result ->
                    for (documents in result) {
                        FirebaseFirestore.getInstance().collection("alarms").document(documents.id)
                            .delete()
                    }

                }
        }

        private fun favoriteAlarm(destinationUid: String, postId: String) {
            if (destinationUid == FirebaseAuth.getInstance().currentUser?.uid)
                return
            val alarmDTO = AlarmDTO()
            alarmDTO.destinationUid = destinationUid
            alarmDTO.userId = FirebaseAuth.getInstance().currentUser?.email
            alarmDTO.uid = FirebaseAuth.getInstance().currentUser?.uid
            alarmDTO.kind = 0
            alarmDTO.postId = postId
            alarmDTO.timestamp = System.currentTimeMillis()
            FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)
            FirebaseFirestore.getInstance().collection("images").document(postId).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val message =
                            FirebaseAuth.getInstance().currentUser?.email + " " + getString(R.string.alarm_favorite1) +
                                    " \"" + task.result.data?.get("explain") + "\" " + getString(R.string.alarm_favorite2)
                        FcmPush.instance.sendMessage(destinationUid, "stagram", message)
                    }
                }

        }
    }

}