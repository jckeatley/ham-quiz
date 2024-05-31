package controllers

object QuestionForm {
  import play.api.data.Forms._
  import play.api.data.Form

  case class Data(answerId: String)
 
  object Data {
    def unapply(data: Data): Option[String] = {
      Some(data.answerId)
    }
  }

  val form: Form[Data] = Form(
    mapping(
      "answerId" -> nonEmptyText
    )(Data.apply)(Data.unapply)
  )
}
