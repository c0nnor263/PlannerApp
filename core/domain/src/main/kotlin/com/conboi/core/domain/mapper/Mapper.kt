package com.conboi.core.domain.mapper

interface Mapper<S, D> {
    fun from(source: S): D

    fun to(destination: D): S
}