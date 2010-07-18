(ns ^{:doc "This namespace provides page templates for use by teglon.web."
       :author "David Edgar Liebke"}
    teglon.web.pages)

(defn header [title]
  (str "<?xml version=\"1.0\" encoding=\"iso-8859-1\"?>"
       "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\""
       "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
       "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">"
       " <head>"
       "  <title>" title "</title>"
       " </head>"
       " <body>"))

(defn footer []
  (str "</body>"
       "</html>"))

(defn search-form []
  (str "<form method=\"GET\" action=\"/api/v1/json/models/search\">"
       "<input type=\"text\" size=\"30\" maxlength=\"35\" name=\"q\" />"
       "<input type=\"submit\" value=\"Search\">"
       "</form>"))

(defn index-page [] 
  (str (header "Teglon")
       "  <img src=\"http://incanter.org/images/teglon/teglon.png\" height=\"100\" alt=\"Teglon\" />"
       (search-form)
       "  <ul>"
       "    <li><a href=\"/api/v1/json/models/show\">Models</a></li>"
       "  </ul>"
       (footer)))

(defn status-404
  ([] (status-404 ""))
  ([uri]
     (str (header "404 - Not Found")
	  "  <h1>404 - Not Found</h1>"
	  "<p>No such page: " uri "</p>"
	  (footer))))
