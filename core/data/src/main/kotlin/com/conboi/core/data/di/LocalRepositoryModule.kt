package com.conboi.core.data.di

import com.conboi.core.data.source.local.repo.UserTaskRepositoryImpl
import com.conboi.core.domain.source.local.repo.UserTaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface LocalRepositoryModule {
    @Binds
    fun bindTaskRepository(userTaskRepositoryImpl: UserTaskRepositoryImpl): UserTaskRepository
}