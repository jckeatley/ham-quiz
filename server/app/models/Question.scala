package models

case class Question(name: String, text: String, imageURL: Option[String], section: Option[String],
    answers: List[Answer], correctAnswer: String, disabled: Boolean)
