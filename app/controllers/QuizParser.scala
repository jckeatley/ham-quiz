package controllers

import scala.xml.*

import models.{Answer, Group, Question}

object QuizParser {
  def parseQuiz(is: InputSource): Seq[Group] = {
    val root: Elem = XML.load(is)
    for {
      subelement <- root \ "subelement"
      group <- subelement \ "group"
      groupId = (group \ "@id").text
    } yield {
      val questions = for {
        question <- group \ "question"
        questionId = (question \ "@id").text
        corrAnsId = (question \ "@correct").text
        text = (question \ "text").text
        imageTag = (question \ "image").headOption
        disabledAtrb = (question \ "disabled").headOption
      } yield {
        val questSection = (question \ "@section").headOption.map(_.text)
        val imageUrl = imageTag.map(tag => (tag \ "@url").text)
        val disabled = disabledAtrb.exists(tag => tag.text == "true")

        val answers: Seq[Answer] = for {
          answer <- question \ "answer"
        } yield {
          val ansId = (answer \ "@id").head.text
          val ansText = answer.text
          val ansLast = (answer \ "@last").headOption.exists(l => l.text == "true")
          Answer(ansId, ansText, ansLast)
        }
        Question(questionId, text, imageUrl, questSection, answers.toList, corrAnsId, disabled)
      }
      Group(groupId, questions.toList)
    }
  }

  def loadQuiz(): Seq[Group] = {
    val inputSource = new InputSource(getClass.getClassLoader.getResourceAsStream("public/amateur_extra.xml"))
    parseQuiz(inputSource)
  }
}
