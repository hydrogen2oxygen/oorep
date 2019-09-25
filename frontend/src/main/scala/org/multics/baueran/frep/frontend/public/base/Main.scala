package org.multics.baueran.frep.frontend.public.base

import org.scalajs.dom
import dom.{Event, document}
import org.multics.baueran.frep.shared.Defs.CookieFields
import org.querki.jquery.$

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExportTopLevel
import org.querki.jquery._
import scalatags.JsDom.all._
import org.multics.baueran.frep.shared.frontend.{Case, Disclaimer, Repertorise, getCookieData, serverUrl}
import org.multics.baueran.frep.shared.sec_frontend.RepertoryModal

@JSExportTopLevel("Main")
object Main {

  def main(args: Array[String]): Unit = {
    cookiePopup()

    dom.document.body.appendChild(div(style:="width:100%;", id:="nav_bar").render)
    dom.document.body.appendChild(div(style:="width:100%;", id:="content").render)
    dom.document.body.appendChild(div(style:="width:100%;", id:="content_bottom").render)

    dom.document.body.appendChild(RepertoryModal().render)
    dom.document.body.appendChild(Case.analysisModalDialogHTML().render)

    $("#nav_bar").empty()
    $("#nav_bar").append(NavBar.apply().render)
    $("#content").append(Repertorise.apply().render)
    $("#content").append(About.toHTML().render)
    $("#content").append(Features.toHTML().render)
    $("#content_bottom").append(Disclaimer.toHTML().render)

    // Stuff to make the NavBar (dis)appear dynamically
    var navBarDark = false
    $(dom.window).scroll(() => {
      if (Repertorise.results.now.size == 0) {
        if ($(document).scrollTop() > 150) {
          if (!navBarDark) {
            $("#public_nav_bar").addClass("bg-dark navbar-dark shadow p-3 mb-5")
            $("#nav_bar_logo").append(a(cls := "navbar-brand py-0", href := serverUrl(), "OOREP").render)
            navBarDark = true
          }
        }
        else {
          $("#public_nav_bar").removeClass("bg-dark navbar-dark shadow p-3 mb-5")
          $("#nav_bar_logo").empty()
          navBarDark = false
        }
      }
    })
  }

  // /////////////////////////////////////////////////////////////////////////////////////////
  // This whole popup is ugly as sin.  In an ideal world, it shouldn't be here.  But it's
  // technical uglyness doesn't matter for now, because it is displayed only once
  // when the user visits the site for the first time, or if the cookie has been removed.
  //
  // The whole dialog should not be necessary and instead of refactoring it, I'm hoping that
  // legislation will change to make it no longer necessary one day.  (But, I am realistic
  // and am not holding my breath.)
  // /////////////////////////////////////////////////////////////////////////////////////////

  def cookiePopup() = {
    // TODO: Using JQuery here, because don't know how else to do it. Not nice, I know!
    def loadAndScroll(file: String) = {
      $("#content").load(serverUrl() + "/assets/html/" + file)
      js.eval("$('html, body').animate({ scrollTop: 0 }, 'fast');")
    }

    getCookieData(dom.document.cookie, CookieFields.cookiePopupAccepted.toString) match {
      case None =>
        val dialog =
          div(cls:="modal fade modalless", data.backdrop:="static", data.keyboard:="false", tabindex:="-1", role:="dialog", id:="cookiePopup",
            div(cls:="modal-dialog", role:="document", style:="min-width: 50%;",
              div(cls:="modal-content",
                div(cls:="modal-header",
                  h5(cls:="modal-title", "Cookie-Einverständniserklärung / Agreement to use cookies")
                  // button(`type`:="button", cls:="close", data.dismiss:="modal", aria.label:="Close", span(aria.hidden:="true", "\u00d7"))
                ),
                div(cls:="modal-body",
                  p("Diese Website benötigt Cookies als technische Grundvoraussetzung. Durch Verwendung dieser Website erklären Sie, dass sie unsere ",
                    a(href:="#",
                      onclick:= { () =>
                        loadAndScroll("datenschutz.html")
                        js.eval("$('#cookiePopup').modal('hide');")  // TODO: Ugly as sin
                      }, "Datenschutzrichtline"),
                    " gelesen und verstanden haben und einverstanden mit der Vewendung der übertragenen Cookies sind."),
                  p("The basic functionality of this web site depends on the use of cookies. By using our web site, you acknowledge that you have read and understand our ",
                    a(href:="#",
                      onclick:= { () =>
                        loadAndScroll("cookies.html")
                        js.eval("$('#cookiePopup').modal('hide');")  // TODO: Ugly as sin
                      }, "privacy policy"),
                    ", and consent to the use and transmission of cookies.")
                ),
                div(cls:="modal-footer",
                  button(`type`:="button", cls:="btn btn-secondary", data.dismiss:="modal",
                    onclick:= { (event: Event) =>
                      event.stopPropagation()
                      dom.window.location.assign("https://www.europarl.europa.eu/")
                    }, "Ablehnen / Refuse"),
                  button(`type`:="button", cls:="btn btn-primary", data.dismiss:="modal",
                    onclick:= { (event: Event) =>
                      event.stopPropagation()
                      dom.window.location.replace(serverUrl())
                    }, "Annehmen / Accept"),
                )
              )
            )
          )

        dom.document.body.appendChild(dialog.render)
        js.eval("$('#cookiePopup').modal('show');")  // TODO: Ugly as sin
      case Some(_) =>
        println("Initial cookie already present. Not the first visit of site!")
    }
  }

}
