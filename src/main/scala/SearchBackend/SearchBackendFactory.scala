package br.ufrj.ner.SearchBackend 

import com.codahale.logula.Logging
import scala.util.parsing.combinator.JavaTokenParsers


/** This class is a factory for the SearchBackend class.
  * It provides an two methods to create a SearchBackend from
  * a configuration file.
  */
class SearchBackendFactory extends JavaTokenParsers with Logging {

  private var backend = new SearchBackend
  
  /*** Grammar ****/

  private def backendConfig= config ~ endPoint ~ predicates

  private def config = "%%Config" ~ configLine.*
  
  private def configLine = "name" ~ ":=" ~ stringLiteral ^^ 
        { case "name" ~ _ ~ value => backend.name = value }
               
  private def endPoint = "%%EndPoint" ~ endPointLine
  
  private def endPointLine = "url" ~ ":=" ~ uri ^^ 
        { case "url" ~ _ ~ uri => backend.url = uri.drop(1).dropRight(1) }
  
  private def uri: Parser[String] =
    ("<"+"""([^"\p{Cntrl}\\]|\\[\\/bfnrt]|\\u[a-fA-F0-9]{4})*"""+">").r
    
  private def predicates = "%%Predicates" ~ predicateLine.*
  
  private def predicateLine = uri ~ decimalNumber ^^
        { case uri ~ weight => {
          val p = new Predicate(uri, weight.toInt)
          backend.predicates += (p.key -> p) }
        }
  
  
  /**** Public interface ****/
    
  /** This method returns the SearchBackend corresponding to a configuration file
    *
    * @param file Path to a config file
    * @return Some[SearchBackend] on success ; None otherwise
    */
  def createFromFile(file : String) : SearchBackend = {
    log.info("Reading configuration from file : \"%s\"", file)
    val source = io.Source.fromFile(file)
    val ret = parse(source.mkString)
    source.close
    return ret
  }
  
  /** This method returns the SearchBackend corresponding to a configuration string
    *
    * @param str a ''String'' containing the configuration 
    * @return Some[SearchBackend] on success ; None otherwise
    */
  def parse(str :String) : SearchBackend = {
    backend = new SearchBackend
    parseAll(backendConfig, str) match {
      case Success(res, _) =>
        backend
      case NoSuccess(msg, _) =>
        log.error("Error while parsing the configuration : %s", msg)
        throw new Exception("Unable to parse configuration")
    }
  }

}


