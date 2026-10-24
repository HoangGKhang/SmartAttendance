package com.example.smartattendance.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartattendance.data.repository.CourseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CourseViewModel(
    private val repository: CourseRepository = CourseRepository()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<CourseUiState>(CourseUiState.Idle)

    val uiState: StateFlow<CourseUiState> =
        _uiState.asStateFlow()

    fun loadCourses() {

        viewModelScope.launch {

            _uiState.value = CourseUiState.Loading

            repository
                .getCourses()
                .onSuccess { courses ->

                    _uiState.value =
                        CourseUiState.Success(courses)
                }
                .onFailure { exception ->

                    _uiState.value =
                        CourseUiState.Error(
                            exception.message
                                ?: "Không thể tải danh sách lớp học"
                        )
                }
        }
    }
}