package com.zg.carbonapp.Tool

import android.content.Context
import androidx.core.content.ContextCompat
//import com.google.android.material.datepicker.DayViewDecorator
import com.prolificinteractive.materialcalendarview.CalendarDay
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.zg.carbonapp.R

class CalendarDecorator(context: Context, dates: List<CalendarDay>) : DayViewDecorator {
    private val drawable = ContextCompat.getDrawable(context, R.drawable.sign_marker)
    private val dates = HashSet(dates)

    override fun shouldDecorate(day: CalendarDay) = dates.contains(day)

    override fun decorate(view: DayViewFacade) {
        drawable?.let {
            view.setBackgroundDrawable(it)
        }
    }
}