package com.zg.carbonapp.Activity

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zg.carbonapp.MMKV.SignInManager
import com.zg.carbonapp.MMKV.UserMMKV
import com.zg.carbonapp.R
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import java.util.*

class SignInActivity : AppCompatActivity() {

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var signButton: Button
    private lateinit var streakCount: TextView
    private lateinit var totalCount: TextView
    private lateinit var cha: ImageView
    private lateinit var monthYearText: TextView
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // 当前显示的月份
    private var currentMonth = Calendar.getInstance().get(Calendar.MONTH)
    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // 初始化控件
        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
        signButton = findViewById(R.id.signButton)
        streakCount = findViewById(R.id.streakCount)
        totalCount = findViewById(R.id.totalCount)
        cha = findViewById(R.id.cha)
        monthYearText = findViewById(R.id.monthYearText)

        // 配置日历
        setupCalendar()

        // 初始化UI
        updateUI()

        // 关闭按钮
        cha.setOnClickListener {
            finish()
        }

        // 签到按钮逻辑
        signButton.setOnClickListener {
            if (SignInManager.signIn()) {
                Toast.makeText(this, "签到成功！+5碳积分", Toast.LENGTH_SHORT).show()
                updateUserCarbonPoints() // 更新积分
                updateUI() // 刷新界面
                setupCalendar() // 刷新日历
            } else {
                Toast.makeText(this, "今日已签到", Toast.LENGTH_SHORT).show()
            }
        }

        // 上个月按钮
        findViewById<ImageView>(R.id.prevMonthBtn).setOnClickListener {
            if (currentMonth == Calendar.JANUARY) {
                currentMonth = Calendar.DECEMBER
                currentYear--
            } else {
                currentMonth--
            }
            setupCalendar()
        }

        // 下个月按钮
        findViewById<ImageView>(R.id.nextMonthBtn).setOnClickListener {
            if (currentMonth == Calendar.DECEMBER) {
                currentMonth = Calendar.JANUARY
                currentYear++
            } else {
                currentMonth++
            }
            setupCalendar()
        }
    }

    // 配置日历
    private fun setupCalendar() {
        // 设置月份标题
        val monthNames = resources.getStringArray(R.array.months)
        monthYearText.text = "${monthNames[currentMonth]} $currentYear"

        // 计算该月的第一天和最后一天
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        // 获取该月的天数
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // 获取该月第一天是星期几（周日=1, 周一=2, ..., 周六=7）
        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // 创建日期列表（包括空白项）
        val days = mutableListOf<DateItem>()

        // 添加空白项（用于对齐）
        for (i in 1 until firstDayOfWeek) {
            days.add(DateItem("", false))
        }

        // 添加日期项
        for (day in 1..daysInMonth) {
            val dateStr = String.format("%04d-%02d-%02d", currentYear, currentMonth + 1, day)
            val isSigned = SignInManager.getSignedDates().contains(dateStr)
            days.add(DateItem(dateStr, isSigned))
        }

        // 设置适配器
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarRecyclerView.adapter = CalendarAdapter(days)
    }

    // 更新用户碳积分
    private fun updateUserCarbonPoints() {
        val user = UserMMKV.getUser()
        user?.let {
            val newCarbonCount = it.carbonCount + 5
            val updatedUser = it.copy(carbonCount = newCarbonCount)
            UserMMKV.saveUser(updatedUser)
        }
    }

    // 更新UI显示
    private fun updateUI() {
        // 更新统计数据
        streakCount.text = SignInManager.getStreak().toString()
        totalCount.text = SignInManager.getSignedDates().size.toString()

        // 禁用已签到按钮
        val today = LocalDate.now().format(dateFormatter)
        signButton.isEnabled = !SignInManager.getSignedDates().contains(today)

        // 已签到时修改按钮文字
        if (!signButton.isEnabled) {
            signButton.text = "今日已签到"
            signButton.setBackgroundColor(resources.getColor(R.color.text_secondary))
        } else {
            signButton.text = "今日签到 +5碳积分"
            signButton.setBackgroundColor(resources.getColor(R.color.primary))
        }
    }

    // 从后台返回时刷新UI
    override fun onResume() {
        super.onResume()
        updateUI()
        setupCalendar()
    }

    // 日期项数据类
    data class DateItem(val date: String, val isSigned: Boolean)

    // 日历适配器
    inner class CalendarAdapter(private val dates: List<DateItem>) :
        RecyclerView.Adapter<CalendarAdapter.DateViewHolder>() {

        inner class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val dayText: TextView = itemView.findViewById(R.id.dayText)
            val signedIndicator: View = itemView.findViewById(R.id.signedIndicator)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_calendar_day, parent, false)
            return DateViewHolder(view)
        }

        override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
            val item = dates[position]

            if (item.date.isNotEmpty()) {
                val day = item.date.split("-")[2].toInt()
                holder.dayText.text = day.toString()
                holder.dayText.visibility = View.VISIBLE

                // 高亮当前日期
                val today = LocalDate.now().format(dateFormatter)
                if (item.date == today) {
                    holder.dayText.setTextColor(Color.WHITE)
                    holder.dayText.setBackgroundResource(R.drawable.bg_current_day)
                } else {
                    holder.dayText.setTextColor(ContextCompat.getColor(this@SignInActivity, R.color.eco_text))
                    holder.dayText.background = null
                }

                // 显示签到标记
                holder.signedIndicator.visibility = if (item.isSigned) View.VISIBLE else View.INVISIBLE
            } else {
                holder.dayText.visibility = View.INVISIBLE
                holder.signedIndicator.visibility = View.INVISIBLE
            }
        }

        override fun getItemCount() = dates.size
    }
}