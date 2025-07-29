import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guru2_3.TodoItem

class TodoAdapter(
    private val items: MutableList<TodoItem>
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    inner class TodoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 만약 텍스트뷰만 필요하면 선언
        val textView: TextView = itemView.findViewById(android.R.id.text1)
        // 체크박스 삭제
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        // 체크박스 없는 단순한 레이아웃 사용 (simple_list_item_1)
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return TodoViewHolder(view)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val item = items[position]
        holder.textView.text = "${item.tagName} : ${item.text}"
        // 체크박스 없으니 아래 코드 삭제
        // holder.checkbox.isChecked = item.isDone
        // holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
        //     item.isDone = isChecked
        // }
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

