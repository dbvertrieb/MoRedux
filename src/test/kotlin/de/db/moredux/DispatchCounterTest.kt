package de.db.moredux

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test


class DispatchCounterTest {
    @Test
    fun `test incrementAndGet and get`() {
        val sut = DispatchCounter()

        assertThat(sut.incrementAndGet()).isEqualTo(1)
        assertThat(sut.get()).isEqualTo(1)
        assertThat(sut.get()).isEqualTo(1)

        assertThat(sut.incrementAndGet()).isEqualTo(2)
        assertThat(sut.get()).isEqualTo(2)
        assertThat(sut.get()).isEqualTo(2)

        assertThat(sut.incrementAndGet()).isEqualTo(3)
        assertThat(sut.get()).isEqualTo(3)
        assertThat(sut.get()).isEqualTo(3)
    }
}