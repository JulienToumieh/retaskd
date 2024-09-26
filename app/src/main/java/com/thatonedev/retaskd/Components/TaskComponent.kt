package com.thatonedev.retaskd.Components

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.content.res.TypedArray
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.thatonedev.retaskd.R
import org.json.JSONArray


class TaskComponent(private val activity: Activity, private val dataSet: JSONArray) : RecyclerView.Adapter<TaskComponent.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskData: CheckBox = view.findViewById(R.id.task)
        val taskCard: CardView = view.findViewById(R.id.taskCard)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.component_task, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.taskData.text = dataSet.getJSONObject(position).getString("title").toString()
        viewHolder.taskData.paint.isStrikeThruText = dataSet.getJSONObject(position).getBoolean("completed")
        viewHolder.taskData.isChecked = dataSet.getJSONObject(position).getBoolean("completed")

        if (dataSet.getJSONObject(position).getBoolean("completed")) viewHolder.taskData.setTextColor(Color.GRAY)
        else if (isDarkTheme(activity)) viewHolder.taskData.setTextColor(Color.parseColor("#ffe3ff"))
                                   else viewHolder.taskData.setTextColor(Color.parseColor("#24171d"))

        viewHolder.taskCard.setOnClickListener {
            //(activity as OnDataPass).toggleTask(position)
        }
        viewHolder.taskCard.setOnLongClickListener {
            (activity as OnDataPass).deleteTask(position)
            true
        }
        viewHolder.taskData.setOnCheckedChangeListener { _, isChecked ->
            viewHolder.taskData.isChecked = isChecked
            (activity as OnDataPass).toggleTask(position)
            viewHolder.taskData.paint.isStrikeThruText = isChecked


            if (isChecked) {
                viewHolder.taskData.setTextColor(Color.GRAY)
                (activity as OnDataPass).showConfetti()
            }
            else if (isDarkTheme(activity)) viewHolder.taskData.setTextColor(Color.parseColor("#ffe3ff"))
                                       else viewHolder.taskData.setTextColor(Color.parseColor("#24171d"))

        }
    }
    private fun isDarkTheme(context: Context): Boolean {
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    override fun getItemCount() = dataSet.length()

    interface OnDataPass {
        fun toggleTask(position: Int)
        fun deleteTask(position: Int)
        fun showConfetti()
    }
}