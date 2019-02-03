package org.multics.baueran.frep.shared.sec_frontend

import org.querki.jquery.$

import org.multics.baueran.frep.shared.Defs.serverUrl
import org.multics.baueran.frep.shared.FIle
import org.multics.baueran.frep.shared.frontend.getCookieData

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.body.PlainTextBody
import fr.hmil.roshttp.response.SimpleHttpResponse
import scalatags.JsDom.all.{id, input, _}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.raw.HTMLInputElement

import monix.execution.Scheduler.Implicits.global

import scala.scalajs.js
import scala.util.Success

import io.circe.syntax._

object NewFileModal {

  def apply() = {
    div(cls:="modal fade", tabindex:="-1", role:="dialog", id:="newFileModal",
      div(cls:="modal-dialog modal-dialog-centered", role:="document", style:="min-width: 80%;",
        div(cls:="modal-content",
          div(cls:="modal-header",
            h5(cls:="modal-title", "Create a new file"),
            button(`type`:="button", cls:="close", data.dismiss:="modal", "\u00d7")
          ),
          div(cls:="modal-body",
            div(cls:="table-responsive",
              form(
                div(cls:="form-group",
                  label(`for`:="fileHeader", "ID"),
                  input(cls:="form-control",
                    id:="fileHeader",
                    placeholder:="A simple, unique file identifier")
                  // required)
                ),
                div(cls:="form-group",
                  label(`for`:="fileDescr", "Description"),
                  textarea(cls:="form-control", id:="fileDescr", rows:="3", placeholder:="A more verbose description of the file")
                ),
                div(
                  button(data.dismiss:="modal", cls:="btn mb-2",
                    "Cancel",
                    onclick:={ (event: Event) =>
                      $("#fileHeader").`val`("")
                      $("#fileDescr").`val`("")
                    }),
                  button(cls:="btn btn-primary mb-2", `type`:="button",
                    "Submit",
                    onclick:={(event: Event) =>
                      event.stopPropagation()

                      val header = dom.document.getElementById("fileHeader").asInstanceOf[HTMLInputElement].value
                      val descr = dom.document.getElementById("fileDescr").asInstanceOf[HTMLInputElement].value
                      val memberId = getCookieData(dom.document.cookie, "oorep_member_id") match {
                        case Some(id) => id.toInt
                        case None => -1 // TODO: Force user to relogin; the identification cookie has disappeared!!!!!!!!!!
                      }
                      val newFIle = FIle(header, memberId, (new js.Date()).toISOString(), descr, List.empty)

                      HttpRequest(serverUrl() + "/savefile")
                        .post(PlainTextBody(newFIle.asJson.toString()))
                        .onComplete({
                          case response: Success[SimpleHttpResponse] => println("Received: " + response.toString())
                          case _ => println("Failure!")
                        })

                      // js.eval("$('#newFileModal').modal('hide');")
                    })
                )
              )
            )
          )
        )
      )
    )

  }
}
