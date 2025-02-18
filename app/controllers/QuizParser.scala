package controllers

import scala.xml.*
import java.time.LocalDate

import models.{Answer, Group, Question, Quiz}

object QuizParser {
  private def parseQuiz(is: InputSource): Quiz = {
    val root: Elem = XML.load(is)
    val title = (root \ "title").head.text
    val effDate = LocalDate.parse((root \ "effectiveDate").head.text)
    val groups = for {
      subElement <- root \ "subelement"
      group <- subElement \ "group"
      groupId = (group \ "@id").text
    } yield {
      val questions = for {
        question <- group \ "question"
        questionId = (question \ "@id").text
        corrAnsId = (question \ "@correct").text
        text = (question \ "text").text
        imageTag = (question \ "image").headOption
        disabledAttrib = (question \ "@disabled").headOption
      } yield {
        val questSection = (question \ "@section").headOption.map(_.text)
        val imageUrl = imageTag.map(tag => (tag \ "@url").text)
        val disabled = disabledAttrib.exists(tag => tag.text == "true")

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
    Quiz(title, effDate, groups)
  }

  def loadQuiz(path: String): Quiz = {
    val inputSource = new InputSource(getClass.getClassLoader.getResourceAsStream(path))
    parseQuiz(inputSource)
  }
}
