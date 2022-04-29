package navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.project.R
import com.example.project.databinding.FragmentAlarmBinding
import com.example.project.databinding.ItemCommentBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import model.AlarmDTO


class AlarmFragment : Fragment() {
    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        binding.alarmfragmentRecyclerview.adapter = AlarmRecyclerViewAdapter()
        binding.alarmfragmentRecyclerview.layoutManager = LinearLayoutManager(activity)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class AlarmRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var alarmDTOList: ArrayList<AlarmDTO> = arrayListOf()

        init {
            val uid = FirebaseAuth.getInstance().currentUser?.uid

            FirebaseFirestore.getInstance().collection("alarms").whereEqualTo("destinationUid", uid)
                .addSnapshotListener { value, error ->
                    alarmDTOList.clear()
                    if (value == null) return@addSnapshotListener
                    for (snapshot in value.documents) {
                        alarmDTOList.add(snapshot.toObject(AlarmDTO::class.java)!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val binding =
                ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return CustomViewHolder(binding)
        }

        inner class CustomViewHolder(var binding: ItemCommentBinding) :
            RecyclerView.ViewHolder(binding.root)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val viewholder = (holder as CustomViewHolder)

            FirebaseFirestore.getInstance().collection("profileImages")
                .document(alarmDTOList[position].uid!!).get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val url = task.result!!["image"]
                        Glide.with(holder.itemView.context).load(url)
                            .apply(RequestOptions().circleCrop())
                            .into(viewholder.binding.commentviewitemImageviewProfile)
                    }
                }
            FirebaseFirestore.getInstance().collection("images")
                .document(alarmDTOList[position].postId.toString()).get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        when (alarmDTOList[position].kind) {
                            0 -> {
                                val str0 =
                                    alarmDTOList[position].userId + " " + getString(R.string.alarm_favorite1) +
                                            " \"" + task.result.data?.get("explain") + "\" " + getString(
                                        R.string.alarm_favorite2
                                    )
                                viewholder.binding.commentviewitemTextviewProfile.text = str0
                            }
                            1 -> {
                                val str1 =
                                    alarmDTOList[position].userId + " " + getString(R.string.alarm_comment1) +
                                            " \"" + task.result.data?.get("explain") + "\" " + getString(
                                        R.string.alarm_comment2
                                    ) +
                                            " \"" + alarmDTOList[position].message + "\""
                                viewholder.binding.commentviewitemTextviewProfile.text = str1
                            }
                            2 -> {
                                val str2 =
                                    alarmDTOList[position].userId + " " + getString(R.string.alarm_follow)
                                viewholder.binding.commentviewitemTextviewProfile.text = str2
                            }
                        }
                        viewholder.binding.commentviewitemTextviewComment.visibility =
                            View.INVISIBLE
                    }
                }

        }

        override fun getItemCount(): Int {
            return alarmDTOList.size
        }

    }
}