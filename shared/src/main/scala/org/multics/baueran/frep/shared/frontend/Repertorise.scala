package org.multics.baueran.frep.shared.frontend

import scalatags.JsDom.all._
import scalatags.JsDom.tags.input
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLInputElement
import io.circe.parser.parse
import rx.Var

import scala.collection.mutable
import scala.util.{Failure, Success}
import org.querki.jquery._
import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared._
import org.multics.baueran.frep.shared.Defs.{ serverUrl, CookieFields }
import org.multics.baueran.frep.shared.sec_frontend.FileModalCallbacks.updateMemberFiles

object Repertorise {

  private var symptomQuery = ""
  private var selectedRepertory = ""
  private val remedyFilter = Var("")
  private val remedyFormat = Var(RemedyFormat.NotFormatted)
  val results: Var[List[CaseRubric]] = Var(List())

  remedyFilter.triggerLater(if (results.now.size > 0) showResults())
  results.triggerLater(if (results.now.size > 0) showResults())
  remedyFormat.triggerLater(if (results.now.size > 0) showResults())

  // ------------------------------------------------------------------------------------------------------------------
  // Render HTML for the results of a repertory lookup directly to page.
  //
  // containingRemedyAbbrev, if not empty, will lead to only those rows drawn, that contain remedy
  // with abbrev. containingRemedyAbbrev.
  def showResults(): Unit = {

    def resetContentView() = {
      $("#content").empty()
      $("#content").append(apply().render)

      getCookieData(dom.document.cookie, CookieFields.oorep_member_id.toString) match {
        case Some(id) => updateMemberFiles(id.toInt)
        case None => ;
      }
    }

    // This method is just to display the remedy-summary at the top of the results table.
    // It is not strictly necessary for displaying the results themselves.
    def resultingRemedies() = {
      var remedies = mutable.HashMap[Remedy, (Integer, Integer)]()

      for (cr <- results.now) {
        for (WeightedRemedy(r,w) <- cr.weightedRemedies) {
          if (remedies.contains(r)) {
            val weight = remedies.get(r).get._1 + w
            val occurrence = remedies.get(r).get._2 + 1
            remedies.put(r, (weight, occurrence))
          }
          else
            remedies.put(r, (w, 1))
        }
      }

      remedies
    }

    val numberOfMultiOccurrences = resultingRemedies().filter(rr => rr._2._2 > 1).size

    def resultRow(result: CaseRubric) = {

      implicit def crToCR(cr: CaseRubric) = new BetterCaseRubric(cr)

      val remedies =
        if (remedyFormat.now == RemedyFormat.NotFormatted)
          result.getRawRemedies()
        else
          result.getFormattedRemedies()

      tr(
        td(result.rubric.fullPath, style:="min-width:400px;"),
        td(remedies.take(remedies.size - 1).map(l => span(l, ", ")) ::: List(remedies.last)),
        td(cls := "text-right",
          button(cls:="btn btn-sm", `type`:="button", id:=("button_" + result.repertoryAbbrev + "_" + result.rubric.id),
            style:="vertical-align: middle; display: inline-block",
            (if (Case.cRubrics.contains(result)) attr("disabled"):="disabled" else ""),
            onclick:={ (event: Event) => {
              event.stopPropagation()
              Case.addRepertoryLookup(result)
              Case.updateCaseViewAndDataStructures()
              $("#button_" + result.repertoryAbbrev + "_" + result.rubric.id).attr("disabled", 1)
              showCase()
            }
            }, "Add")
        )
      )
    }

    resetContentView()

    if (results.now.size > 0) {
      $("#resultStatus").empty()
      $("#resultStatus").append(
        div(cls := "alert alert-secondary", role := "alert",
          b(a(href := "#", onclick := { (event: Event) => remedyFilter() = "" },
            results.now.size + " result(s) for '" + symptomQuery + "'. ")),
          if (numberOfMultiOccurrences > 1) {
            val relevantMultiRemedies = resultingRemedies().toList
              .sortBy(-_._2._2)
              .filter(rr => rr._2._2 > 1)

            span("(Multi-occurrences in search results: ",
              relevantMultiRemedies
                .take(relevantMultiRemedies.size - 1)
                .map(rr => {
                  span(
                    (rr._2._2 + "x"),
                    a(href := "#",
                      onclick := { (event: Event) =>
                        remedyFilter() = rr._1.nameAbbrev
                      },
                      rr._1.nameAbbrev),
                    ("(" + rr._2._1 + "), "),
                  )
                }),
              span(
                (relevantMultiRemedies.last._2._2 + "x"),
                a(href := "#", onclick := { (event: Event) =>
                  remedyFilter() = relevantMultiRemedies.last._1.nameAbbrev
                }, relevantMultiRemedies.last._1.nameAbbrev),
                ("(" + relevantMultiRemedies.last._2._1 + ")")),
              ")")
          }
          else {
            span(" ")
          }
        ).render

      )

      $("#resultDiv").empty()
      $("#resultDiv").append(
        div(cls := "table-responsive",
          table(cls := "table table-striped table-sm table-bordered",
            thead(cls := "thead-dark", scalatags.JsDom.attrs.id := "resultsTHead",
              th(attr("scope") := "col", "Symptom"),
              th(attr("scope") := "col",
                a(scalatags.JsDom.attrs.id := "remediesFormatButton",
                  cls := "underline", href := "#", style := "color:white;",
                  onclick := ((event: Event) => toggleRemedyFormat()),
                  "Remedies")
              ),
              th(attr("scope") := "col", " ")
            ),
            tbody(scalatags.JsDom.attrs.id := "resultsTBody")
          )
        ).render
      )
    }

    if (remedyFilter.now.length == 0)
      results.now.foreach(result => $("#resultsTBody").append(resultRow(result).render))
    else
      results.now.filter(_.containsRemedyAbbrev(remedyFilter.now)).foreach(result => $("#resultsTBody").append(resultRow(result).render))
  }

  // ------------------------------------------------------------------------------------------------------------------
  def toggleRemedyFormat() = {
    if (remedyFormat.now == RemedyFormat.NotFormatted)
      remedyFormat() = RemedyFormat.Formatted
    else
      remedyFormat() = RemedyFormat.NotFormatted

    if (Case.size() > 0)
      showCase()
    else
      println("Repertorise: toggleRemedyFormat: Case.size() == 0.")
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def showCase() = {
    Case.rmCaseDiv()
    $("#caseDiv").append(Case.toHTML(remedyFormat.now).render)
    Case.updateCaseViewAndDataStructures()
    Case.updateCaseHeaderView()
  }

  // ------------------------------------------------------------------------------------------------------------------
  private def onSubmitSymptom(event: Event): Unit = {
    val symptom = dom.document.getElementById("inputField").asInstanceOf[HTMLInputElement].value
    val repertory = selectedRepertory

    if (repertory.length == 0 || symptom.trim.replaceAll("[^A-Za-z0-9]", "").length <= 3) {
      $("#resultStatus").empty()
      $("#resultStatus").append(
        div(cls:="alert alert-danger", role:="alert",
          b("No results returned for '" + symptom + "'. " +
            "Either no repertory selected or symptom input too short.")).render)
      return
    }

    $("body").css("cursor", "wait")

    HttpRequest(serverUrl() + "/lookup")
      .withQueryParameters(("symptom", symptom), ("repertory", repertory))
      .withCrossDomainCookies(true)
      .send()
      .onComplete({
        case response: Success[SimpleHttpResponse] => {
          $("body").css("cursor", "default")
          parse(response.get.body) match {
            case Right(json) => {
              val cursor = json.hcursor
              cursor.as[List[CaseRubric]] match {
                case Right(newResults) => {
                  symptomQuery = symptom
                  results() = newResults

                  if (Case.size() > 0)
                    showCase()
                }
                case Left(t) => println("Parsing of lookup as RepertoryLookup failed: " + t)
              }
            }
            case Left(_) => println("Parsing of lookup failed (is it JSON?).")
          }
        }
        case error: Failure[SimpleHttpResponse] => {
          $("body").css("cursor", "default")
          $("#resultStatus").empty()
          $("#resultStatus").append(
            div(cls:="alert alert-danger", role:="alert",
              b("No results returned for '" + symptom + "'. " +
                "Either symptom not in repertory or server error.")).render)
        }
      })
  }

  // ------------------------------------------------------------------------------------------------------------------
  def apply() = {
    val ulRepertorySelection =
      div(cls:="dropdown col-sm-2",
        button(`type`:="button",
          style:="overflow: hidden;",
          cls:="btn btn-block dropdown-toggle",
          data("toggle"):="dropdown",
          `id`:="repSelectionDropDownButton",
          "Repertories"),
        div(cls:="dropdown-menu", `id`:="repSelectionDropDown")
      )

    def updateAvailableRepertories() = {
      HttpRequest(serverUrl() + "/availableReps")
        .send()
        .onComplete({
          case response: Success[SimpleHttpResponse] => {
            parse(response.get.body) match {
              case Right(json) => {
                val cursor = json.hcursor
                cursor.as[List[Info]] match {
                  case Right(infos) => {
                    infos.foreach(info => {
                      $("#repSelectionDropDown")
                        .append(a(cls:="dropdown-item", href:="#", data.value:=info.abbrev,
                          onclick := { (event: Event) =>
                            selectedRepertory = info.abbrev
                            $("#repSelectionDropDownButton").text("Repertory: " + selectedRepertory)
                          }, info.abbrev).render)

                      if (selectedRepertory.length > 0)
                        $("#repSelectionDropDownButton").text("Repertory: " + selectedRepertory)
                      else if (info.access == RepAccess.Default) {
                        selectedRepertory = info.abbrev
                        $("#repSelectionDropDownButton").text("Repertory: " + selectedRepertory)
                      }
                    })
                  }
                  case Left(t) => println("Parsing of available repertories failed: " + t)
                }
              }
              case Left(_) => println("Parsing of available repertories failed (is it JSON?).")
            }
          }
          case error: Failure[SimpleHttpResponse] => println("Available repertories failed: " + error.toString)
        })
    }

    val myHTML =
      // Fresh page...
      if (results.now.size == 0 && Case.size() == 0) {
        div(cls := "container-fluid",
          div(cls := "container-fluid text-center",
            div(cls:="col-sm-12 text-center", img(src:="img/logo_small.png")),
            div(cls := "row", style := "margin-top:20px;",
              div(cls := "col-sm-1"),
              div(cls := "row col-sm-10",
                ulRepertorySelection,
                div(cls := "col-sm-9",
                  input(cls := "form-control", `id` := "inputField",
                    onkeydown := { (event: dom.KeyboardEvent) =>
                      if (event.keyCode == 13) {
                        remedyFilter() = ""
                        event.stopImmediatePropagation()
                        onSubmitSymptom(event)
                      }
                    },
                    `placeholder` := "Enter a symptom (for example: head, pain, left)")
                ),
              ),
              div(cls := "col-sm-1")
            ),
            div(cls := "col-sm-12 text-center", style := "margin-top:20px;",
              button(cls := "btn btn-primary", style := "width: 120px; margin-right:5px;", `type` := "button",
                onclick := { (event: Event) => remedyFilter() = ""; onSubmitSymptom(event); event.stopPropagation() }, "Find"),
              button(cls := "btn", style := "width: 100px;", `type` := "button",
                onclick := { (event: Event) => $("#inputField").value(""); event.stopPropagation() }, "Clear")
            )
          ),
          div(cls := "container-fluid", style := "margin-top: 23px;", id := "resultStatus"),
          div(cls := "container-fluid", id := "resultDiv"),
          div(cls := "span12", id := "caseDiv", {
            if (Case.size() > 0)
              Case.toHTML(remedyFormat.now)
            else ""
          })
        )
      }
      // Page with some results after search...
      else {
        div(cls := "container-fluid",
          div(cls := "container-fluid text-center",
            div(cls := "row", style := "margin-top:20px;",
              div(cls := "col-sm-1"),
              div(cls := "row col-sm-10",
                ulRepertorySelection,
                div(cls := "col-sm-7",
                  input(cls := "form-control", `id` := "inputField",
                    onkeydown := { (event: dom.KeyboardEvent) =>
                      if (event.keyCode == 13) {
                        remedyFilter() = ""
                        event.stopImmediatePropagation()
                        onSubmitSymptom(event)
                      }
                    },
                    `placeholder` := "Enter a symptom (for example: head, pain, left)")
                ),
                span(button(cls := "btn btn-primary", style := "width: 80px; margin-right:5px;", `type` := "button",
                  onclick := { (event: Event) => remedyFilter() = ""; onSubmitSymptom(event); event.stopPropagation() },
                  span(cls := "oi oi-magnifying-glass", title := "Find", aria.hidden := "true")),
                  button(cls := "btn", style := "width: 80px;", `type` := "button",
                    onclick := { (event: Event) => $("#inputField").value(""); event.stopPropagation() },
                    span(cls := "oi oi-trash", title := "Clear", aria.hidden := "true"))
                )
              ),
              div(cls := "col-sm-1")
            )
          ),
          div(cls := "container-fluid", style := "margin-top: 23px;", id := "resultStatus"),
          div(cls := "container-fluid", id := "resultDiv"),
          div(cls := "span12", id := "caseDiv", {
            if (Case.size() > 0)
              Case.toHTML(remedyFormat.now)
            else ""
          })
        )
      }

    updateAvailableRepertories()

    // If initial page, then vertically center search form
    if (results.now.size == 0 && Case.size() == 0) {
      div(cls := "introduction", div(cls := "vertical-align", myHTML))
    }
    // If there are already some search results, do without center and fix nav bar prior to rendering
    else {
      if (dom.document.getElementById("nav_bar_logo").innerHTML.length() == 0) {
        $("#public_nav_bar").addClass("bg-dark navbar-dark shadow p-3 mb-5")
        $("#nav_bar_logo").append(a(cls := "navbar-brand py-0", href := serverUrl(), "OOREP").render)
      }
      div(style:="margin-top:100px;", myHTML)
    }
  }
}
