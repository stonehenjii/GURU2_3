import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guru2_3.TodoItem
import com.example.guru2_3.R


class TodoAdapter(
    private val items: MutableList<TodoItem>,
    private val dbHelper: DatabaseHelper? = null,
    private val onTaskStatusChanged: (() -> Unit)? = null
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.textView)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val item = items[position]
        // position은 0부터 시작하니까 +1 해서 1부터 시작하는 번호로 표시
        holder.textView.text = "${position + 1}. ${item.tagName} : ${item.text}"
        holder.checkBox.isChecked = item.isDone

        holder.checkBox.setOnCheckedChangeListener(null) // 리스너 초기화
        holder.checkBox.isChecked = item.isDone
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isDone = isChecked
            // 데이터베이스 업데이트
            dbHelper?.updateTaskCompletion(item.id, isChecked)
            // 태스크 상태 변경 시 콜백 호출
            onTaskStatusChanged?.invoke()
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(todo: TodoItem) {
        items.add(todo)
        notifyItemInserted(items.size - 1)
    }

    fun setItems(newItems: List<TodoItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}


