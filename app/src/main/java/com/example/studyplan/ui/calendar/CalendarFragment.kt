package com.example.studyplanner.ui.calendar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.studyplanner.databinding.FragmentCalendarBinding
import com.kizitonwose.calendarview.model.CalendarDay
import com.kizitonwose.calendarview.model.DayOwner
import com.kizitonwose.calendarview.ui.DayBinder
import java.time.LocalDate
import java.time.YearMonth

class CalendarFragment : Fragment() {

    private lateinit var binding: FragmentCalendarBinding
    private lateinit var viewModel: CalendarViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(CalendarViewModel::class.java)

        setupCalendar()
        observeViewModel()
        viewModel.loadCurrentMonth()
    }

    private fun setupCalendar() {
        val today = LocalDate.now()
        val currentMonth = YearMonth.now()
        val startMonth = currentMonth.minusMonths(100)
        val endMonth = currentMonth.plusMonths(100)

        binding.calendarView.setup(startMonth, endMonth, firstDayOfWeek = java.time.DayOfWeek.MONDAY)
        binding.calendarView.scrollToMonth(currentMonth)

        binding.calendarView.dayBinder = object : DayBinder<CalendarDayViewContainer> {
            override fun create(parent: ViewGroup) = CalendarDayViewContainer(parent)
            override fun bind(container: CalendarDayViewContainer, data: CalendarDay) {
                container.day = data
                if (data.owner == DayOwner.THIS_MONTH) {
                    val dayStatus = viewModel.getDayStatus(data.date)
                    container.bind(data.date, dayStatus)
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.calendarData.observe(viewLifecycleOwner) { data ->
            binding.calendarView.notifyCalendarChanged()
        }
    }
}

class CalendarDayViewContainer(parent: ViewGroup) : androidx.recyclerview.widget.RecyclerView.ViewHolder(parent) {
    lateinit var day: CalendarDay

    fun bind(date: LocalDate, status: String?) {
        // UI 업데이트
    }
}