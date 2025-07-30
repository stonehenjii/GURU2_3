import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.guru2_3.TodoItem
import com.example.guru2_3.R


/**
 * 투두리스트를 표시하는 RecyclerView 어댑터 (캘린더 연동 기능 포함)
 * 
 * @param items 표시할 투두 아이템 리스트 (선택된 날짜의 태스크들)
 * @param dbHelper 데이터베이스 헬퍼 (태스크 완료 상태 업데이트용)
 * @param onTaskStatusChanged 태스크 상태 변경 시 호출되는 콜백 함수 (캘린더 새로고침용)
 * 
 * 캘린더 연동 기능:
 * 1. 사용자가 체크박스 클릭 시 데이터베이스에 완료 상태 저장
 * 2. onTaskStatusChanged 콜백을 통해 MainActivity의 refreshCalendar() 호출
 * 3. 캘린더의 해당 날짜 상태 아이콘이 실시간으로 업데이트됨
 * 
 * 데이터 흐름:
 * 체크박스 클릭 → DB 업데이트 → onTaskStatusChanged() → refreshCalendar() → updateCalendarStatus()
 * → 캘린더 상태 아이콘 변경 (빨간색 원 ↔ 초록색 체크)
 */
class TodoAdapter(
    private val items: MutableList<TodoItem>,
    private val dbHelper: DatabaseHelper? = null,
    private val onTaskStatusChanged: (() -> Unit)? = null
) : RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    // 데이터 로딩 중인지 추적하는 플래그 (로딩 중에는 콜백 호출 안함)
    private var isLoadingData = false

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
        
        // 텍스트 설정
        holder.textView.text = "${position + 1}. ${item.tagName} : ${item.text}"
        
        // 체크박스 리스너 중복 호출 방지를 위해 먼저 제거
        holder.checkBox.setOnCheckedChangeListener(null)
        
        // 현재 태스크의 완료 상태를 체크박스에 반영 (리스너 없는 상태에서)
        holder.checkBox.isChecked = item.isDone
        
        /**
         * 체크박스 상태 변경 시 캘린더 연동 처리
         * 
         * 동작 순서:
         * 1. 메모리의 TodoItem 객체 상태 업데이트
         * 2. 데이터베이스에 변경된 완료 상태 저장
         * 3. onTaskStatusChanged 콜백 호출 → MainActivity.refreshCalendar() 실행
         * 4. 캘린더 상태 아이콘 실시간 업데이트
         * 
         * 캘린더 상태 변경 예시:
         * - 마지막 미완료 태스크 체크 시: 빨간색 원 → 초록색 체크
         * - 완료된 태스크 체크 해제 시: 초록색 체크 → 빨간색 원
         */
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            // 데이터 로딩 중이 아닌 경우에만 처리 (사용자가 직접 클릭한 경우)
            if (!isLoadingData) {
                // 1. 메모리의 TodoItem 상태 업데이트
                item.isDone = isChecked
                
                // 2. 데이터베이스에 변경 사항 저장
                dbHelper?.updateTaskCompletion(item.id, isChecked)
                
                // 3. 캘린더 새로고침을 위한 콜백 호출 (MainActivity.refreshCalendar())
                onTaskStatusChanged?.invoke()
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(todo: TodoItem) {
        items.add(todo)
        notifyItemInserted(items.size - 1)
    }

    fun setItems(newItems: List<TodoItem>) {
        // 데이터 로딩 시작을 표시 (체크박스 리스너 콜백 방지)
        isLoadingData = true
        
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
        
        // 데이터 로딩 완료를 표시 (체크박스 리스너 콜백 재활성화)
        isLoadingData = false
    }
}


