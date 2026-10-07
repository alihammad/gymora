package com.gymora.domain.model

import java.time.LocalDate

/** Steps counted on one local day. */
data class DaySteps(val date: LocalDate, val steps: Int)
