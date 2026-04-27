package com.example.smartattendance.ui.lecturer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class SessionListAdapter(
    context: Context,
    private val list: List<SessionItem>
) : ArrayAdapter<SessionItem>(context, 0, list) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)

        val item = list[position]

        view.findViewById<TextView>(android.R.id.text1).text =
            item.className.ifEmpty { "Đang tải..." }

        view.findViewById<TextView>(android.R.id.text2).text =
            if (item.isOpen) "Đang mở" else "Đã đóng"

        return view
    }
}
