package navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.project.MainActivity
import com.example.project.databinding.FragmentGridBinding
import com.google.firebase.firestore.FirebaseFirestore
import model.ContentDTO

class GridFragment : Fragment() {
    private var _binding: FragmentGridBinding? = null
    private val binding get() = _binding!!
    var firestore: FirebaseFirestore? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGridBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        binding.gridfragmentRecyclerview.adapter = UserFragmentRecyclerViewAdapter()
        binding.gridfragmentRecyclerview.layoutManager = GridLayoutManager(activity, 3)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    inner class UserFragmentRecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        private var contentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {
            firestore?.collection("images")
                ?.addSnapshotListener { value, error ->

                    if (value == null) return@addSnapshotListener

                    for (snapshot in value.documents) {
                        contentDTOs.add(snapshot.toObject(ContentDTO::class.java)!!)
                    }
                    if (contentDTOs.isNullOrEmpty()) return@addSnapshotListener

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

            holder.imageView.setOnClickListener {
                (activity as MainActivity).goHome(position)
            }
        }

        override fun getItemCount(): Int {
            return contentDTOs.size
        }

    }
}