package models

import java.time.LocalDate

case class Quiz(title: String, effDate: LocalDate, groups: Seq[Group])
