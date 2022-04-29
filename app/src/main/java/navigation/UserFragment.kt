package navigation


import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.project.LoginActivity
import com.example.project.MainActivity
import com.example.project.R
import com.example.project.databinding.FragmentUserBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import model.AlarmDTO
import model.ContentDTO
import model.FollowDTO
import util.FcmPush

class UserFragment : Fragment() {
    private var _binding: FragmentUserBinding? = null
    private val binding get() = _binding!!
    var firestore: FirebaseFirestore? = null
    var uid: String? = null
    private var auth: FirebaseAuth? = null
    private var currentUserUid: String? = null
    private var registration: ListenerRegistration? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentUserBinding.inflate(inflater, container, false)
        uid = arguments?.getString("destinationUid")
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserUid = auth?.currentUser?.uid

        if (uid == currentUserUid) {
            binding.accountBtnFollowSignout.text = getString(R.string.signout)
            binding.accountBtnFollowSignout.setOnClickListener {
                activity?.finish()
                startActivity(Intent(activity, LoginActivity::class.java))
                auth?.signOut()
            }
            binding.accountIvProfile.setOnClickListener {
                val photoPickerIntent = Intent(Intent.ACTION_PICK)
                photoPickerIntent.type = "image/*"

                startForResult.launch(photoPickerIntent)
            }
        } else {
            binding.accountBtnFollowSignout.text = view?.context?.getString(R.string.follow)
            val mainactivity = (activity as MainActivity)
            mainactivity.setToolbar(arguments?.getString("userId"))
            binding.accountBtnFollowSignout.setOnClickListener {
                requestFollow()
            }
        }

        binding.accountRecyclerview.adapter = UserFragmentRecyclerViewAdapter()
        binding.accountRecyclerview.layoutManager = GridLayoutManager(activity, 3)

        getProfileImage()
        getFollowerAndFollowing()
        return binding.root
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                val imageUri = result.data?.data
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val storageRef =
                    FirebaseStorage.getInstance().reference.child("userProfileImages").child(uid!!)
                storageRef.putFile(imageUri!!)
                    .continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                        return@continueWithTask storageRef.downloadUrl
                    }.addOnSuccessListener { uri ->
                        val map = HashMap<String, Any>()
                        map["image"] = uri.toString()
                        FirebaseFirestore.getInstance().collection("profileImages").document(uid)
                            .set(map)
                    }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        registration!!.remove()
    }

    private fun getFollowerAndFollowing() {
        var originalBackgroundColor: ColorStateList? = null
        var originalTextColor: ColorStateList? = null
        registration = firestore?.collection("users")?.document(uid!!)
            ?.addSnapshotListener { value, error ->
                if (value == null) return@addSnapshotListener
                val followDTO = value.toObject(FollowDTO::class.java)
                if (followDTO?.followingCount != null) {
                    binding.accountTvFollowingCount.text = followDTO.followingCount.toString()
                }
                if (followDTO?.followerCount != null) {
                    binding.accountTvFollowerCount.text = followDTO.followerCount.toString()
                    if (followDTO.followers.containsKey(currentUserUid!!)) {
                        binding.accountBtnFollowSignout.text =
                            view?.context?.getString(R.string.following)
                        originalBackgroundColor = binding.accountBtnFollowSignout.backgroundTintList
                        originalTextColor = binding.accountBtnFollowSignout.textColors
                        binding.accountBtnFollowSignout.backgroundTintList = ColorStateList.valueOf(
                            Color.parseColor("#f1f1f1")
                        )
                        binding.accountBtnFollowSignout.setTextColor(
                            ColorStateList.valueOf(
                                Color.parseColor("#000000")
                            )
                        )
                    } else {
                        if (uid != currentUserUid) {
                            binding.accountBtnFollowSignout.text =
                                view?.context?.getString(R.string.follow)
                            if (originalBackgroundColor != null && originalTextColor != null) {
                                binding.accountBtnFollowSignout.backgroundTintList =
                                    originalBackgroundColor
                                binding.accountBtnFollowSignout.setTextColor(
                                    originalTextColor
                                )
                            }
                        }
                    }
                }
            }

    }

    private fun requestFollow() {
        val tsDocFollowing = firestore?.collection("users")?.document(currentUserUid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollowing!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO.followingCount = 1
                followDTO.followings[uid!!] = true
                transaction.set(tsDocFollowing, followDTO)
                return@runTransaction
            }

            if (followDTO.followings.containsKey(uid)) {
                followDTO.followingCount = followDTO.followingCount - 1
                followDTO.followings.remove(uid)
            } else {
                followDTO.followingCount = followDTO.followingCount + 1
                followDTO.followings[uid!!] = true
            }
            transaction.set(tsDocFollowing, followDTO)
            return@runTransaction
        }
        val tsDocFollower = firestore?.collection("users")?.document(uid!!)
        firestore?.runTransaction { transaction ->
            var followDTO = transaction.get(tsDocFollower!!).toObject(FollowDTO::class.java)
            if (followDTO == null) {
                followDTO = FollowDTO()
                followDTO!!.followerCount = 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
                transaction.set(tsDocFollower, followDTO!!)
                return@runTransaction
            }

            if (followDTO!!.followers.containsKey(currentUserUid)) {
                followDTO!!.followerCount = followDTO!!.followerCount - 1
                followDTO!!.followers.remove(currentUserUid)
                followerCancel(uid!!)
            } else {
                followDTO!!.followerCount = followDTO!!.followerCount + 1
                followDTO!!.followers[currentUserUid!!] = true
                followerAlarm(uid!!)
            }
            transaction.set(tsDocFollower, followDTO!!)
            return@runTransaction
        }
    }

    private fun followerCancel(destinationUid: String) {
        FirebaseFirestore.getInstance().collection("alarms")
            .whereEqualTo("destinationUid", destinationUid)
            .whereEqualTo("uid", auth?.currentUser?.uid)
            .whereEqualTo("kind", 2)
            .get()
            .addOnSuccessListener { result ->
                for (documents in result) {
                    FirebaseFirestore.getInstance().collection("alarms").document(documents.id)
                        .delete()
                }

            }
    }

    private fun followerAlarm(destinationUid: String) {
        if (destinationUid == auth?.currentUser?.uid)
            return

        val alarmDTO = AlarmDTO()
        alarmDTO.destinationUid = destinationUid
        alarmDTO.userId = auth?.currentUser?.email
        alarmDTO.uid = auth?.currentUser?.uid
        alarmDTO.kind = 2
        alarmDTO.timestamp = System.currentTimeMillis()
        FirebaseFirestore.getInstance().collection("alarms").document().set(alarmDTO)

        val message = auth?.currentUser?.email + " " + getString(R.string.alarm_follow)
        FcmPush.instance.sendMessage(destinationUid, "stagram", message)
    }

    private fun getProfileImage() {
        firestore?.collection("profileImages")?.document(uid!!)
            ?.addSnapshotListener { value, error ->
                if (value == null) return@addSnapshotListener
                if (value.data != null) {
                    val url = value.data!!["image"]
                    if (isAdded) {
                        Glide.with(requireActivity()).load(url).apply(RequestOptions().circleCrop())
                            .into(binding.accountIvProfile)
                    }
                }
            }
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {

            firestore?.collection("images")?.whereEqualTo("uid", uid)
                ?.addSnapshotListener { value, error ->

                    if (value == null) return@addSnapshotListener

                    for (snapshot in value.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
                    if (contentDTOs.isNullOrEmpty()) return@addSnapshotListener
                    binding.accountTvPostCount.text = contentDTOs.size.toString()
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val width = resources.displayMetrics.widthPixels / 3
            val imageView = ImageView(parent.context)
            imageView.layoutParams = LinearLayoutCompat.LayoutParams(width, width)
            return CustomViewHolder(imageView)
        }

        inner class CustomViewHolder(var imageView: ImageView) :
            RecyclerView.ViewHolder(imageView)

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val imageView = (holder as CustomViewHolder).imageView
            Glide.with(holder.itemView.context).load(contentDTOs[position].imageUrl)
                .apply(RequestOptions().centerCrop()).into(imageView)
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }
}