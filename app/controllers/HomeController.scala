package controllers

import scala.annotation.tailrec
import scala.util.Random
import java.time.format.DateTimeFormatter
import java.time.{Duration, Instant}

import javax.inject.*

import models.{Answer, Question}
import play.api.*
import play.api.mvc.*
import views.html

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(messagesAction: MessagesActionBuilder, cc: ControllerComponents)
  extends AbstractController(cc) {

  import play.api.data.Form
  import QuestionForm.*

  var questions: List[Question] = Nil
  var total = 0
  var correct = 0
  var startTime: Instant = _
  var stopTime: Instant = _

  val postUrl: Call = routes.HomeController.scoreQuestion()

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }

  def startQuiz: Action[AnyContent] = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    questions = generateQuestions
    total = questions.length
    correct = 0
    startTime = Instant.now()
    Ok(views.html.question(questions.head, form, postUrl, false))
  }

  def continue: Action[AnyContent] = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    questions = questions.tail
    if (questions.isEmpty) {
      val score = 100 * correct.toDouble / total
      stopTime = Instant.now()
      val et = Duration.between(startTime, stopTime)
      val elapsedTime = f"${et.toHoursPart}%d:${et.toMinutesPart}%02d:${et.toSecondsPart}%02d"
      Ok(views.html.score(total, correct, score, elapsedTime))
    } else {
      Ok(views.html.question(questions.head, form, postUrl, false))
    }
  }

  def scoreQuestion: Action[AnyContent] = messagesAction { implicit request: MessagesRequest[AnyContent] =>
    val errorFunction = { (formWithErrors: Form[Data]) =>
      Ok(views.html.question(questions.head, form, postUrl, true))
    }

    val successFunction = { (data: Data) =>
      if (data.answerId == questions.head.correctAnswer) {
        // Answer is correct -- iterate to next:
        correct += 1
        questions = questions.tail

        if (questions.isEmpty) {
          val score = 100 * correct.toDouble / total
          stopTime = Instant.now()
          val et = Duration.between(startTime, stopTime)
          val elapsedTime = f"${et.toHoursPart}%d:${et.toMinutesPart}%02d:${et.toSecondsPart}%02d"
          Ok(html.score(total, correct, score, elapsedTime))
        } else {
          Ok(views.html.question(questions.head, form, postUrl, false))
        }
      } else {
        // Answer is wrong -- reshow question with answer:
        Ok(views.html.question(questions.head, form, postUrl, true))
      }
    }

    val formValidationResult = form.bindFromRequest()
    formValidationResult.fold(errorFunction, successFunction)
  }

  def selectRandomFromList[T](random: Random, items: List[T]): (T, List[T]) = {
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

  @tailrec private def randomizeAnswers(random: Random, answers: List[Answer], accum: List[Answer] = Nil): List[Answer] = {
    if (answers == Nil) {
      accum
    } else {
      val (item, rest) = selectRandomFromList(random, answers)
      randomizeAnswers(random, rest, item :: accum)
    }
  }

  def generateQuestions: List[Question] = {
    val groups = QuizParser.loadQuiz()
    val random = new Random()

    val questions = for {
      group <- groups
      activeQuestions = group.questions.filter(q => !q.disabled)
      question = activeQuestions(random.nextInt(activeQuestions.length))
    } yield {
      val keys = question.answers.map(a => a.id)
      val (answers, last) = question.answers.partition(a => !a.last)
      val randAnswers = randomizeAnswers(random, answers) ++ last
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
    questions.toList
  }
}
