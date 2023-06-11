package com.iggydev.quicksignwizard.di

import com.iggydev.quicksignwizard.presentation.viewmodels.GenerationViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel {
        GenerationViewModel()
    }
}