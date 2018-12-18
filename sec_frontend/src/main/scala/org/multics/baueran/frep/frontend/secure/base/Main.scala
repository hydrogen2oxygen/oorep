package org.multics.baueran.frep.frontend.secure.base

import fr.hmil.roshttp.HttpRequest
import fr.hmil.roshttp.response.SimpleHttpResponse
import org.scalajs.dom.{XMLHttpRequest, document, html}
import monix.execution.Scheduler.Implicits.global
import org.multics.baueran.frep.shared.Defs.serverUrl
import org.scalajs.dom
import org.scalajs.dom.raw._

import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.annotation._
import org.querki.jquery._
import scalatags.JsDom.all._

import scala.util.{Failure, Success}
import org.multics.baueran.frep.shared.frontend.Repertorise
import org.multics.baueran.frep.shared.frontend.Disclaimer

@JSExportTopLevel("MainSecure")
object Main {

  def main(args: Array[String]): Unit = {
    $(dom.document.body).append(div(style:="width:100%;", id:="nav_bar").render)
    $(dom.document.body).append(div(style:="width:100%;", id:="content").render)
    $(dom.document.body).append(div(style:="width:100%;", id:="content_bottom").render)

    // No access without valid cookies!
    HttpRequest("http://localhost:9000/authenticate")
      .withCrossDomainCookies(true)
      .send()
      .onComplete({
        case response: Success[SimpleHttpResponse] => {
          $("#nav_bar").empty()
          $("#nav_bar").append(NavBar.apply().render)
          $("#content").append(Repertorise.applySecure().render)
          $("#content_bottom").append(Disclaimer.toHTML().render)
        }
        case error: Failure[SimpleHttpResponse] => {
          $("#content").append(p("Not authorized.").render)
        }
      })

    // Stuff to make the NavBar (dis)appear dynamically
    var navBarDark = false
    $(dom.window).scroll(() => {
      if (Repertorise.results.size == 0) {
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
}
