package dev.inmo.navigation.core.tmp_utils

import dev.inmo.micro_utils.common.Diff
import dev.inmo.micro_utils.common.Warning
import dev.inmo.micro_utils.common.diff
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@Warning("This feature can be removed in any update without notice")
val <T> Flow<List<T>>.diffFlow: Flow<Diff<T>>
    get() = flow {
        var currentValue = first()

        map {
            val diff = it.diff(currentValue)

            currentValue = it

            emit(diff)
        }
    }
