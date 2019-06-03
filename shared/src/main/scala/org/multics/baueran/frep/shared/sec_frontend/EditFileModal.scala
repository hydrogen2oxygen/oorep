package org.multics.baueran.frep.shared.sec_frontend

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.{MultiPartBody, PlainTextBody}
import fr.hmil.roshttp.response.SimpleHttpResponse
import io.circe.parser.parse
import io.circe.syntax._
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.serverUrl
import org.multics.baueran.frep.shared.{Caze, FIle}
import org.multics.baueran.frep.shared.frontend.getCookieData
import org.scalajs.dom
import org.scalajs.dom.Event
import scalatags.JsDom.all.{onclick, _}
import rx.Var
import rx.Rx
import rx.Ctx.Owner.Unsafe._
import scalatags.rx.all._
import org.querki.jquery.$

import scalajs.js
import scala.math.{max, min}
import scala.util.{Failure, Success, Try}

object EditFileModal {

  private var currentlyOpenedFile: Option[FIle] = None
  private var currentlyActiveMemberId = -1
  private val fileName = Var("")
  private val cases: Var[List[Caze]] = Var(List())
  private val casesHeight = Rx(max(200, min(100, cases().length * 30)))
  private val caseAnchors = Rx {
    cases() match {
      case Nil =>
        List(a(cls := "list-group-item list-group-item-action", data.toggle := "list", id := "none", href := "#list-profile", role := "tab", "<no cases created yet>").render)
      case _ => cases().map { c =>
        a(cls:="list-group-item list-group-item-action", data.toggle:="list", id:="none", href:="#list-profile", role:="tab", c.header).render
      }
    }
  }

  private def mainModal() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="editFileModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", Rx(fileName())),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",

            div(cls:="form-group mb-2",
              div(cls:="mb-3",
                label(`for`:="fileDescr", "Description"),
                textarea(cls:="form-control", id:="fileDescrEditFileModal", rows:="8", placeholder:="A more verbose description of the file",
                  onkeyup:= { (event: Event) =>
                    currentlyOpenedFile match {
                      case Some(f) =>
                        if ($("#fileDescrEditFileModal").`val`().toString() != f.description)
                          $("#saveFileDescrEditFileModal").removeAttr("disabled")
                        else
                          $("#saveFileDescrEditFileModal").attr("disabled", true)
                      case None => ;
                    }
                  })
              ),
              div(cls:="form-row",
                div(cls:="col"),
                div(cls:="col-2",
                  button(cls:="btn mb-2 mr-2", id:="saveFileDescrEditFileModal", data.toggle:="modal", data.dismiss:="modal", disabled:=true,
                    onclick:= { (event: Event) =>
                      currentlyOpenedFile match {
                        case Some(f) =>
                          HttpRequest(serverUrl() + "/updateFileDescription")
                            .post(MultiPartBody(
                              "filedescr"  -> PlainTextBody($("#fileDescrEditFileModal").`val`().toString().trim()),
                              "fileheader" -> PlainTextBody(f.header),
                              "memberId"   -> PlainTextBody(currentlyActiveMemberId.toString())))
                        case None => ;
                      }
                      $("#saveFileDescrEditFileModal").attr("disabled", true)
                      js.eval("$('#editFileModal').modal('hide');") // TODO: This is ugly! No idea for an alternative :-(
                    },
                    "Save"),
                  button(cls:="btn mb-2", data.dismiss:="modal", "Cancel")
                ),
                div(cls:="col")
              )
            ),

            div(cls:="border-top my-3"),

            div(cls:="form-group",
              div(
                label(`for`:="editFileAvailableFilesList", "Cases"),
                div(
                  cls:="list-group", role:="tablist", id:="editFileAvailableCasesList", style:=Rx("height: " + casesHeight().toString() + "px; overflow-y: scroll;"),
                  caseAnchors
                )
              ),
              div(cls:="form-row",
                div(cls:="col"),
                div(cls:="col-2",
                  button(cls:="btn mb-2 mr-2", id:="openFileEditFileModal", data.toggle:="modal", data.dismiss:="modal", disabled:=true, "Open"),
                  button(cls:="btn mb-2", id:="deleteFileEditFileModal", data.dismiss:="modal", disabled:=true, "Delete")
                ),
                div(cls:="col")
              )
            )

          )
        )
      )
    )
  }

  def requestAndUpdateInformationForFile(fileHeader: String) = {
    // Update modal dialog...
    def updateModal(response: Try[SimpleHttpResponse]) = {
      response match {
        case response: Success[SimpleHttpResponse] => {
          parse(response.get.body) match {
            case Right(json) => {
              val cursor = json.hcursor
              cursor.as[FIle] match {
                case Right(f) =>
                  $("#fileDescrEditFileModal").`val`(f.description)
                  cases() = f.cazes
                  currentlyOpenedFile = Some(f)
                case Left(err) => println("Decoding of file failed: " + err)
              }
            }
            case Left(err) => println("Parsing of file failed (is it JSON?): " + err)
          }
        }
        case error: Failure[SimpleHttpResponse] => println("ERROR: " + error.get.body)
      }
    }
    fileName() = fileHeader

    // Request data from backend...
    getCookieData(dom.document.cookie, "oorep_member_id") match {
      case Some(memberId) => {
        currentlyActiveMemberId = memberId.toInt
        HttpRequest(serverUrl() + "/file")
          .withQueryParameters("memberId" -> memberId, "fileId" -> fileHeader)
          .withCrossDomainCookies(true)
          .send()
          .onComplete((r: Try[SimpleHttpResponse]) => updateModal(r))
      }
      case None => println("WARNING: getCasesForFile() failed. Could not get memberID from cookie."); -1
    }
  }

  def apply() = div(mainModal())

}