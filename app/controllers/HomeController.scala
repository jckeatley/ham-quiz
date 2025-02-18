package controllers

import scala.annotation.tailrec
import scala.compiletime.uninitialized
import scala.util.Random

import java.time.{Duration, Instant}
import javax.inject.*

import controllers.QuestionForm.*
import models.{Answer, Group, Question}
import play.api.*
import play.api.data.Form
import play.api.mvc.*
import views.html

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(messagesAction: MessagesActionBuilder, cc: ControllerComponents)
  extends AbstractController(cc) {
  private val random = new Random()
  private var startTime: Instant = uninitialized
  private var stopTime: Instant = uninitialized
  var questions: List[Question] = Nil
  var total = 0
  var current = 0
  var correct = 0

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index: Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def startTechQuiz: Action[AnyContent] = startQuiz("public/technician.xml")

  def startGeneralQuiz: Action[AnyContent] = startQuiz("public/general.xml")

  def startExtraQuiz: Action[AnyContent] = startQuiz("public/amateur_extra.xml")

  def startQuiz(path: String): Action[AnyContent] = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    val quiz = QuizParser.loadQuiz(path)
    val groups = quiz.groups
    questions = groups.map(generateQuestion).toList
    total = questions.length
    current = 1
    correct = 0
    startTime = Instant.now()
    Ok(views.html.question(questions.head, form, current, total))
  }

  def continue: Action[AnyContent] = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    questions = questions.tail
    current += 1
    if (questions.isEmpty) {
      val score = 100 * correct.toDouble / total
      stopTime = Instant.now()
      val et = Duration.between(startTime, stopTime)
      val elapsedTime = f"${et.toHoursPart}%d:${et.toMinutesPart}%02d:${et.toSecondsPart}%02d"
      Ok(views.html.score(total, correct, score, elapsedTime))
    } else {
      Ok(views.html.question(questions.head, form, current, total))
    }
  }

  def quit: Action[AnyContent] = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.index())
  }

  def scoreQuestion: Action[AnyContent] = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    val errorFunction = { (formWithErrors: Form[Data]) =>
      Ok(views.html.explanation(questions.head, form, current, total, ""))
    }

    val successFunction = { (data: Data) =>
      if (data.answerId == questions.head.correctAnswer) {
        // Answer is correct -- iterate to next:
        correct += 1
        current += 1
        questions = questions.tail

        if (questions.isEmpty) {
          val score = 100 * correct.toDouble / total
          stopTime = Instant.now()
          val et = Duration.between(startTime, stopTime)
          val elapsedTime = f"${et.toHoursPart}%d:${et.toMinutesPart}%02d:${et.toSecondsPart}%02d"
          Ok(html.score(total, correct, score, elapsedTime))
        } else {
          Ok(views.html.question(questions.head, form, current, total))
        }
      } else {
        // Answer is wrong -- reshow question with answer:
        Ok(views.html.explanation(questions.head, form, current, total, data.answerId))
      }
    }

    val formValidationResult = form.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

  private def selectRandomFromList[T](items: List[T]): (T, List[T]) = {
    if (items.isEmpty) {
      throw new IllegalStateException("Items list is empty.")
    }
    if (items.length > 1) {
      val n = random.nextInt(items.length)
      (items(n), items.take(n) ++ items.drop(n + 1))
    } else {
      (items.head, Nil)
    }
  }

  @tailrec private def randomizeAnswers(answers: List[Answer], accum: List[Answer] = Nil): List[Answer] = {
    if (answers == Nil) {
      accum
    } else {
      val (item, rest) = selectRandomFromList(answers)
      randomizeAnswers(rest, item :: accum)
    }
  }

  private def generateQuestion(group: Group): Question = {
    val activeQuestions = group.questions.filter(q => !q.disabled)
    val question = activeQuestions(random.nextInt(activeQuestions.length))
    val keys = question.answers.map(a => a.id)
    val (answers, last) = question.answers.partition(a => !a.last)
    val randAnswers = randomizeAnswers(answers) ++ last
    var newCorrect = ""
    val newAnswers = for {
      (key, answer) <- keys.zip(randAnswers)
    } yield {
      if (answer.id == question.correctAnswer) {
        newCorrect = key
      }
      answer.copy(id = key)
    }
    question.copy(answers = newAnswers, correctAnswer = newCorrect)
  }
}
